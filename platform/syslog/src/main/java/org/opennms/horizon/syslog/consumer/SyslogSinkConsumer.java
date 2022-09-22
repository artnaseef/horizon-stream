/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.syslog.consumer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.horizon.config.service.api.ConfigConstants;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.core.lib.InetAddressUtils;
import org.opennms.horizon.core.lib.Logging;
import org.opennms.horizon.db.dao.api.DistPollerDao;
import org.opennms.horizon.db.dao.api.InterfaceToNodeCache;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsDistPoller;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventForwarder;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.events.xml.Parm;
import org.opennms.horizon.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.syslog.api.SyslogConnection;
import org.opennms.horizon.syslog.api.SyslogMessageDTO;
import org.opennms.horizon.syslog.api.SyslogMessageLogDTO;
import org.opennms.horizon.syslog.config.HideMatch;
import org.opennms.horizon.syslog.config.SyslogConfigBean;
import org.opennms.horizon.syslog.config.SyslogdConfiguration;
import org.opennms.horizon.syslog.config.SyslogdConfigurationGroup;
import org.opennms.horizon.syslog.config.UeiMatch;
import org.opennms.horizon.syslog.parser.MessageDiscardedException;
import org.opennms.netmgt.provision.LocationAwareDnsLookupClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.opennms.horizon.core.lib.InetAddressUtils.addr;


public class SyslogSinkConsumer implements MessageConsumer<SyslogConnection, SyslogMessageLogDTO>, EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogSinkConsumer.class);

    private static final String SYSLOG_LOG4J = "syslog";

    private static final String defaultCacheConfig = "maximumSize=1000,expireAfterWrite=8h";
    private static final String dnsCacheConfigProperty = "org.opennms.netmgt.syslogd.dnscache.config";

    private MessageConsumerManager messageConsumerManager;

    private SyslogConfigBean syslogdConfig;

    private DistPollerDao distPollerDao;

    private EventForwarder eventForwarder;

    private SessionUtils sessionUtils;

    // TODO: This is not wired in, May be hostname can be resolved in Minion itself.
    private LocationAwareDnsLookupClient m_locationAwareDnsLookupClient;

    private InterfaceToNodeCache interfaceToNodeCache;

    private final Cache<HostNameWithLocationKey, String> dnsCache;

    private ConfigService configService;

    private EventSubscriptionService eventSubscriptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String localAddr;
    private final Timer consumerTimer;
    private final Timer toEventTimer;
    private final Timer broadcastTimer;

    public SyslogSinkConsumer(MetricRegistry registry) {
        consumerTimer = registry.timer("consumer");
        toEventTimer = registry.timer("consumer.toevent");
        broadcastTimer = registry.timer("consumer.broadcast");
        String cacheConfig = System.getProperty(dnsCacheConfigProperty, defaultCacheConfig);
        dnsCache = CacheBuilder.from(cacheConfig).recordStats().build();
        registry.register("dnsCacheSize", (Gauge<Long>) dnsCache::size);
        registry.register("dnsCacheHitRate", (Gauge<Double>) () -> dnsCache.stats().hitRate());
        localAddr = InetAddressUtils.getLocalHostName();
    }

    @Override
    public SyslogSinkModule getModule() {
        OnmsDistPoller distPoller = sessionUtils.withReadOnlyTransaction(() -> distPollerDao.whoami());
        return new SyslogSinkModule(syslogdConfig, distPoller);
    }

    @Override
    public void handleMessage(SyslogMessageLogDTO syslogDTO) {
        try (Context consumerCtx = consumerTimer.time()) {
            try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(SYSLOG_LOG4J)) {
                // Convert the Syslog UDP messages to Events
                final Log eventLog;
                try (Context toEventCtx = toEventTimer.time()) {
                    eventLog = toEventLog(syslogDTO);
                }
                // Broadcast the Events to the event bus
                try (Context broadCastCtx = broadcastTimer.time()) {
                    broadcast(eventLog);
                }
            }
        }
    }

    public Log toEventLog(SyslogMessageLogDTO messageLog) {
        final Log elog = new Log();
        final Events events = new Events();
        elog.setEvents(events);
        for (SyslogMessageDTO message : messageLog.getMessages()) {
            try {
                LOG.debug("Converting syslog message into event.");
                ConvertToEvent re = new ConvertToEvent(
                        messageLog.getSystemId(),
                        messageLog.getLocation(),
                        messageLog.getSourceAddress(),
                        messageLog.getSourcePort(),
                        message.getBytes(),
                        message.getTimestamp(),
                        syslogdConfig,
                        m_locationAwareDnsLookupClient,
                        dnsCache,
                        interfaceToNodeCache);
                events.addEvent(re.getEvent());
            } catch (final MessageDiscardedException e) {
                LOG.info("Message discarded, returning without enqueueing event.", e);
            } catch (final Throwable e) {
                LOG.error("Unexpected exception while processing SyslogConnection", e);
            }
        }
        return elog;
    }

    private void broadcast(Log eventLog)  {
        if (LOG.isTraceEnabled())  {
            for (Event event : eventLog.getEvents().getEventCollection()) {
                LOG.trace("Processing a syslog to event dispatch {}", event.toString());
                String uuid = event.getUuid();
                LOG.trace("Event {");
                LOG.trace("  uuid  = {}", (uuid != null && uuid.length() > 0 ? uuid : "<not-set>"));
                LOG.trace("  uei   = {}", event.getUei());
                LOG.trace("  src   = {}", event.getSource());
                LOG.trace("  iface = {}", event.getInterface());
                LOG.trace("  time  = {}", event.getTime());
                LOG.trace("  Msg   = {}", event.getLogmsg().getContent());
                LOG.trace("  Dst   = {}", event.getLogmsg().getDest());
                List<Parm> parms = (event.getParmCollection() == null ? null : event.getParmCollection());
                if (parms != null) {
                    LOG.trace("  parms {");
                    for (Parm parm : parms) {
                        if ((parm.getParmName() != null)
                                && (parm.getValue().getContent() != null)) {
                            LOG.trace("    ({}, {})", parm.getParmName().trim(), parm.getValue().getContent().trim());
                        }
                    }
                    LOG.trace("  }");
                }
                LOG.trace("}");
            }
        }
        eventForwarder.sendNowSync(eventLog);

        if (syslogdConfig.getNewSuspectOnMessage()) {
            eventLog.getEvents().getEventCollection().stream()
                .filter(e -> !e.hasNodeid())
                .filter(e -> !Strings.isNullOrEmpty(e.getInterface()))
                .forEach(e -> {
                    LOG.trace("Syslogd: Found a new suspect {}", e.getInterface());
                    sendNewSuspectEvent(localAddr, e.getInterface(), e.getDistPoller());
                });
        }
    }

    private void sendNewSuspectEvent(String localAddr, String syslogInterface, String distPoller) {
        EventBuilder bldr = new EventBuilder(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI, "syslogd");
        bldr.setInterface(addr(syslogInterface));
        bldr.setHost(localAddr);
        bldr.setDistPoller(distPoller);
        eventForwarder.sendNow(bldr.getEvent());
    }

    public void init() throws Exception {
        initializeConfig();
        messageConsumerManager.registerConsumer(this);
        eventSubscriptionService.addEventListener(this, EventConstants.CONFIG_UPDATED_UEI);
    }

    void initializeConfig() throws IOException {
        SyslogdConfiguration syslogdConfiguration = null;
        Optional<String> optionalConfig = configService.getConfig(ConfigConstants.SYSLOG_CONFIG);
        if (optionalConfig.isEmpty()) {
            // Load initial config from resource.
            URL url = this.getClass().getResource("/syslog-config.json");
            // Validate and store config.
            syslogdConfiguration = objectMapper.readValue(url, SyslogdConfiguration.class);
            parseIncludedFiles(syslogdConfiguration);
            configService.addConfig(ConfigConstants.SYSLOG_CONFIG, objectMapper.writeValueAsString(syslogdConfiguration), "syslog-consumer");
        } else {
            try {
                syslogdConfiguration = objectMapper.readValue(optionalConfig.get(), SyslogdConfiguration.class);
            } catch (JsonProcessingException e) {
                LOG.error("Error while mapping json config to SyslogConfig", e);
            }
        }
        if (syslogdConfiguration != null) {
            syslogdConfig = new SyslogConfigBean(syslogdConfiguration);
        } else {
            throw new IOException("Not able to load default syslog config");
        }
    }

    private void parseIncludedFiles(SyslogdConfiguration config) {

        String path = "src/main/resources/syslog/";
        File file = new File(path);
        File[] listFiles = file.listFiles();
        if (listFiles == null) {
            return;
        }

        for (final File configFile : listFiles) {
            final SyslogdConfigurationGroup includeCfg = JaxbUtils.unmarshal(SyslogdConfigurationGroup.class, new FileSystemResource(configFile));
            if (includeCfg.getUeiMatches() != null) {
                for (final UeiMatch ueiMatch : includeCfg.getUeiMatches())  {
                    if (config.getUeiMatches() == null) {
                        config.setUeiMatches(new ArrayList<>());
                    }
                    config.addUeiMatch(ueiMatch);
                }
            }
            if (includeCfg.getHideMatches() != null) {
                for (final HideMatch hideMatch : includeCfg.getHideMatches()) {
                    if (config.getHideMatches() == null) {
                        config.setHideMatches(new ArrayList<>());
                    }
                    config.addHideMatch(hideMatch);
                }
            }
        }
    }

    public void setEventForwarder(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }

    public void setMessageConsumerManager(MessageConsumerManager messageConsumerManager) {
        this.messageConsumerManager = messageConsumerManager;
    }


    public void setDistPollerDao(DistPollerDao distPollerDao) {
        this.distPollerDao = distPollerDao;
    }

    public void setLocationAwareDnsLookupClient(LocationAwareDnsLookupClient locationAwareDnsLookupClient) {
        this.m_locationAwareDnsLookupClient = locationAwareDnsLookupClient;
    }

    public InterfaceToNodeCache getInterfaceToNodeCache() {
        return interfaceToNodeCache;
    }

    public void setInterfaceToNodeCache(InterfaceToNodeCache interfaceToNodeCache) {
        this.interfaceToNodeCache = interfaceToNodeCache;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    public SyslogConfigBean getSyslogdConfig() {
        return syslogdConfig;
    }


    public void setSessionUtils(SessionUtils sessionUtils) {
        this.sessionUtils = sessionUtils;
    }

    public void setEventSubscriptionService(EventSubscriptionService eventSubscriptionService) {
        this.eventSubscriptionService = eventSubscriptionService;
    }

    @Override
    public String getName() {
        return "syslog-consumer";
    }

    @Override
    public void onEvent(IEvent event) {
        if (event.getUei().equals(EventConstants.CONFIG_UPDATED_UEI) &&
            event.getParm(EventConstants.PARM_CONFIG_NAME).isValid() &&
            event.getParm(EventConstants.PARM_CONFIG_NAME).getValue().getContent().equals(ConfigConstants.SYSLOG_CONFIG) &&
            !event.getSource().equals("syslog-consumer")) {

            try {
                initializeConfig();
            } catch (IOException e) {
                LOG.error("Exception while initializing Trap config", e);
            }
        }
    }
}
