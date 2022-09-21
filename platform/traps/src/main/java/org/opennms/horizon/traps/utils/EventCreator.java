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

package org.opennms.horizon.traps.utils;

import org.opennms.horizon.core.lib.InetAddressUtils;
import org.opennms.horizon.db.dao.api.InterfaceToNodeCache;
import org.opennms.horizon.db.dao.api.MonitoringLocationDao;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.traps.dto.TrapDTO;
import org.opennms.horizon.traps.dto.TrapIdentityDTO;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpResult;
import org.opennms.netmgt.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static org.opennms.horizon.core.lib.InetAddressUtils.str;

public class EventCreator {

    private static final Logger LOG = LoggerFactory.getLogger(EventCreator.class);

    private final InterfaceToNodeCache cache;
    private final EventConfDao eventConfDao;

    public EventCreator(InterfaceToNodeCache cache, EventConfDao eventConfDao) {
        this.cache = Objects.requireNonNull(cache);
        this.eventConfDao = Objects.requireNonNull(eventConfDao);
    }

    public Event createEventFrom(final TrapDTO trapDTO, final String systemId, final String location, final InetAddress trapAddress) {
        LOG.debug("{} trap - trapInterface: {}", trapDTO.getVersion(), trapDTO.getAgentAddress());

        // Set event data
        final EventBuilder eventBuilder = new EventBuilder(null, "trapd");
        eventBuilder.setTime(new Date(trapDTO.getCreationTime()));
        eventBuilder.setCommunity(trapDTO.getCommunity());
        eventBuilder.setSnmpTimeStamp(trapDTO.getTimestamp());
        eventBuilder.setSnmpVersion(trapDTO.getVersion());
        eventBuilder.setSnmpHost(str(trapAddress));
        eventBuilder.setInterface(trapAddress);
        eventBuilder.setHost(InetAddressUtils.toIpAddrString(trapDTO.getAgentAddress()));

        // Handle trap identity
        final TrapIdentityDTO trapIdentity = trapDTO.getTrapIdentity();
        if (trapIdentity != null) {
            LOG.debug("Trap Identity {}", trapIdentity);
            eventBuilder.setGeneric(trapIdentity.getGeneric());
            eventBuilder.setSpecific(trapIdentity.getSpecific());
            eventBuilder.setEnterpriseId(trapIdentity.getEnterpriseId());
            eventBuilder.setTrapOID(trapIdentity.getTrapOID());
        }

        // Handle var bindings
        for (SnmpResult eachResult : trapDTO.getResults()) {
            final SnmpObjId name = eachResult.getBase();
            final SnmpValue value = eachResult.getValue();
            eventBuilder.addParam(SyntaxToEvent.processSyntax(name.toString(), value));
            if (EventConstants.OID_SNMP_IFINDEX.isPrefixOf(name)) {
                eventBuilder.setIfIndex(value.toInt());
            }
        }

        // Resolve Node id and set, if known by OpenNMS
        resolveNodeId(location, trapAddress)
            .ifPresent(eventBuilder::setNodeid);

        // If there was no systemId in the trap message, assume that
        // it was generated by this system. Eventd will fill in the
        // systemId of the local system if it remains null here.
        if (systemId != null) {
            eventBuilder.setDistPoller(systemId);
        }

        // Get event template and set uei, if unknown
        final Event event = eventBuilder.getEvent();
        final org.opennms.horizon.events.conf.xml.Event econf = eventConfDao.findByEvent(event);
        if (econf == null || econf.getUei() == null) {
            event.setUei("uei.opennms.org/default/trap");
        } else {
            event.setUei(econf.getUei());
        }
        return event;
    }

    private Optional<Integer> resolveNodeId(String location, InetAddress trapAddress) {
        // If there was no location in the trap message, assume that
        // it was generated in the default location
        if (location == null) {
            return cache.getFirstNodeId(MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID, trapAddress);
        }
        return cache.getFirstNodeId(location, trapAddress);
    }
}
