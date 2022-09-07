/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.snmp.proxy.common;

import java.util.Collections;
import java.util.List;

import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpResult;

public class SNMPWalkBuilder extends AbstractSNMPRequestBuilder<List<SnmpResult>> {

    public SNMPWalkBuilder(LocationAwareSnmpClientRpcImpl client, SnmpAgentConfig agent, List<SnmpObjId> oids) {
        super(client, agent, Collections.emptyList(), buildWalkRequests(oids));
    }

    private static List<SnmpWalkRequestDTO> buildWalkRequests(List<SnmpObjId> oids) {
        final SnmpWalkRequestDTO walkRequest = new SnmpWalkRequestDTO();
        walkRequest.setOids(oids);
        return Collections.singletonList(walkRequest);
    }

    @Override
    protected List<SnmpResult> processResponse(SnmpMultiResponseDTO response) {
        return response.getResponses().stream()
                .findFirst()
                .map(SnmpResponseDTO::getResults)
                .orElse(Collections.emptyList());
    }
}