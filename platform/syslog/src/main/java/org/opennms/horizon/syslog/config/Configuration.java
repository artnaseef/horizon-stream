/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.syslog.config;

import com.google.common.base.Strings;
import org.opennms.core.xml.ValidateUsing;
import org.opennms.horizon.core.lib.ConfigUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.beans.Transient;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Top-level element for the syslogd-configuration.xml configuration file.
 */
@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("syslog.xsd")
public class Configuration implements Serializable {
    private static final long serialVersionUID = 2L;

    private static final String DEFAULT_PARSER = "org.opennms.netmgt.syslogd.CustomSyslogParser";
    private static final String DEFAULT_DISCARD_UEI = "DISCARD-MATCHING-MESSAGES";

    /**
     * The address on which Syslogd listens for SYSLOG Messages. The
     * default is to listen on all addresses.
     */
    @XmlAttribute(name = "listen-address")
    private String listenAddress;

    /**
     * The port on which Syslogd listens for SYSLOG Messages. The
     * standard port is 514.
     */
    @XmlAttribute(name = "syslog-port", required = true)
    private Integer syslogPort;

    /**
     * Whether messages from devices unknown to OpenNMS should
     * generate newSuspect events.
     */
    @XmlAttribute(name = "new-suspect-on-message")
    private Boolean newSuspectOnMessage;

    /**
     * The regular expression used to separate message and host.
     */
    @XmlAttribute(name = "forwarding-regexp")
    private String forwardingRegexp;

    /**
     * The matching group for the host
     */
    @XmlAttribute(name = "matching-group-host")
    private Integer matchingGroupHost;

    /**
     * The matching group for the message
     */
    @XmlAttribute(name = "matching-group-message")
    private Integer matchingGroupMessage;

    /**
     * A string which defines the class to use when parsing syslog messages.
     * The default is the "CustomSyslogParser", which honors the forwarding-regexp,
     * matching-group-host, and matching-group-message attributes, and can parse
     * most BSD-style Syslog messages, including Syslog-NG's default format.
     * Other options include "org.opennms.netmgt.syslogd.SyslogNGParser" which is a
     * slightly more strict version of the CustomSyslogParser, and
     * "org.opennms.netmgt.syslogd.Rfc5424SyslogParser" which can handle the
     * recent (2009) RFC for syslog messages.
     */
    @XmlAttribute(name = "parser")
    private String parser;

    /**
     * A string which, when used as the value of a "uei"
     * element inside a "ueiMatch" element, results in all
     * matching messages to be discarded without an event
     * ever being created
     */
    @XmlAttribute(name = "discard-uei")
    private String discardUei;

    /**
     * Number of threads used for consuming/dispatching messages.
     * Defaults to 2 x the number of available processors.
     */
    @XmlAttribute(name = "threads")
    private int threads;

    /**
     * Maximum number of messages to keep in memory while waiting
     * to be dispatched.
     */
    @XmlAttribute(name = "queue-size")
    private Integer queueSize;

    /**
     * Messages are aggregated in batches before being dispatched.
     * When the batch reaches this size, it will be dispatched.
     */
    @XmlAttribute(name = "batch-size")
    private Integer batchSize;

    /**
     * Messages are aggregated in batches before being dispatched.
     * When the batch has been created for longer than this interval (ms)
     * it will be dispatched, regardless of the current size.
     */
    @XmlAttribute(name = "batch-interval")
    private Integer batchInterval;

    @XmlAttribute(name = "timezone")
    private String timeZone;

    @XmlAttribute(name = "includeRawSyslogmessage")
    private Boolean includeRawSyslogmessage;

    @Transient
    public Optional<String> getOptionalListenAddress() {
        return Optional.ofNullable(listenAddress);
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(final String listenAddress) {
        this.listenAddress = ConfigUtils.normalizeString(listenAddress);
    }

    public Integer getSyslogPort() {
        return syslogPort;
    }

    public void setSyslogPort(final Integer syslogPort) {
        this.syslogPort = ConfigUtils.assertMinimumInclusive(ConfigUtils.assertNotNull(syslogPort, "syslog-port"), 1, "syslog-port");
    }

    public Boolean getNewSuspectOnMessage() {
        return newSuspectOnMessage != null ? newSuspectOnMessage : Boolean.FALSE;
    }

    public void setNewSuspectOnMessage(final Boolean newSuspectOnMessage) {
        this.newSuspectOnMessage = newSuspectOnMessage;
    }

    @Transient
    public Optional<String> getOptionalForwardingRegexp() {
        return Optional.ofNullable(forwardingRegexp);
    }

    public String getForwardingRegexp() {
        return forwardingRegexp;
    }

    public void setForwardingRegexp(final String forwardingRegexp) {
        this.forwardingRegexp = ConfigUtils.normalizeString(forwardingRegexp);
    }

    @Transient
    public Optional<Integer> getOptionalMatchingGroupHost() {
        return Optional.ofNullable(matchingGroupHost);
    }

    public Integer getMatchingGroupHost() {
        return matchingGroupHost;
    }

    public void setMatchingGroupHost(final Integer matchingGroupHost) {
        this.matchingGroupHost = ConfigUtils.assertMinimumInclusive(matchingGroupHost, 1, "matching-group-host");
    }

    @Transient
    public Optional<Integer> getOptionalMatchingGroupMessage() {
        return Optional.ofNullable(matchingGroupMessage);
    }

    public Integer getMatchingGroupMessage() {
        return matchingGroupMessage;
    }

    public void setMatchingGroupMessage(final Integer matchingGroupMessage) {
        this.matchingGroupMessage = ConfigUtils.assertMinimumInclusive(matchingGroupMessage, 1, "matching-group-message");
    }

    public String getParser() {
        return parser != null ? parser : DEFAULT_PARSER;
    }

    public void setParser(final String parser) {
        this.parser = ConfigUtils.normalizeString(parser);
    }

    public String getDiscardUei() {
        return discardUei != null ? discardUei : DEFAULT_DISCARD_UEI;
    }

    public void setDiscardUei(final String discardUei) {
        this.discardUei = ConfigUtils.normalizeString(discardUei);
    }

    @Transient
    public Optional<Integer> getOptionalThreads() {
        return Optional.ofNullable(threads);
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(final Integer threads) {
        this.threads = ConfigUtils.assertMinimumInclusive(threads, 1, "threads");
    }

    public int getQueueSize() {
        return queueSize != null ? queueSize : 10000;
    }

    public void setQueueSize(final Integer queueSize) {
        this.queueSize = ConfigUtils.assertMinimumInclusive(queueSize, 1, "queue-size");
    }

    public int getBatchSize() {
        return batchSize != null ? batchSize : 1000;
    }

    public void setBatchSize(final Integer batchSize) {
        this.batchSize = ConfigUtils.assertMinimumInclusive(batchSize, 1, "batch-size");
    }

    public int getBatchInterval() {
        return batchInterval != null ? batchInterval : 500;
    }

    public void setBatchInterval(final Integer batchInterval) {
        this.batchInterval = ConfigUtils.assertMinimumInclusive(batchInterval, 1, "batch-interval");
    }

    @Transient
    public Optional<TimeZone> getOptionalTimeZone() {
        if (Strings.emptyToNull(this.timeZone) == null) {
            return Optional.empty();
        }
        return Optional.of(TimeZone.getTimeZone(ZoneId.of(timeZone)));
    }

    public TimeZone getTimeZone() {
        if (Strings.emptyToNull(this.timeZone) == null) {
            return null;
        }
        return TimeZone.getTimeZone(ZoneId.of(timeZone));
    }

    public void setTimeZone(String timeZone) {
        if (Strings.emptyToNull(timeZone) == null) {
            this.timeZone = null;
            return;
        }
        // test if zone is valid:
        ZoneId.of(timeZone);
        this.timeZone = timeZone;
    }

    public boolean shouldIncludeRawSyslogmessage() {
        return includeRawSyslogmessage == null ? false : includeRawSyslogmessage;
    }

    public void setIncludeRawSyslogmessage(boolean includeRawSyslogmessage) {
        this.includeRawSyslogmessage = includeRawSyslogmessage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(listenAddress,
            syslogPort,
            newSuspectOnMessage,
            forwardingRegexp,
            matchingGroupHost,
            matchingGroupMessage,
            parser,
            discardUei,
            threads,
            queueSize,
            batchSize,
            batchInterval,
            timeZone,
            includeRawSyslogmessage);
    }

    /**
     * Overrides the Object.equals method.
     *
     * @param obj
     * @return true if the objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Configuration) {
            final Configuration that = (Configuration) obj;
            return Objects.equals(this.listenAddress, that.listenAddress)
                && Objects.equals(this.syslogPort, that.syslogPort)
                && Objects.equals(this.newSuspectOnMessage, that.newSuspectOnMessage)
                && Objects.equals(this.forwardingRegexp, that.forwardingRegexp)
                && Objects.equals(this.matchingGroupHost, that.matchingGroupHost)
                && Objects.equals(this.matchingGroupMessage, that.matchingGroupMessage)
                && Objects.equals(this.parser, that.parser)
                && Objects.equals(this.discardUei, that.discardUei)
                && Objects.equals(this.threads, that.threads)
                && Objects.equals(this.queueSize, that.queueSize)
                && Objects.equals(this.batchSize, that.batchSize)
                && Objects.equals(this.batchInterval, that.batchInterval)
                && Objects.equals(this.timeZone, that.timeZone)
                && Objects.equals(this.includeRawSyslogmessage, that.includeRawSyslogmessage);
        }
        return false;
    }

}

