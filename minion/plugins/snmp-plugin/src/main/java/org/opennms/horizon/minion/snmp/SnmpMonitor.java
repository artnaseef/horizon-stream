/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.snmp;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import lombok.Setter;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl.ServiceMonitorResponseImplBuilder;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.StrategyResolver;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * TBD888: is there lost logic here?  For example, counting
 *
 * <P>
 * This class is designed to be used by the service poller framework to test the
 * availability of the SNMP service on remote interfaces. The class implements
 * the ServiceMonitor interface that allows it to be used along with other
 * plug-ins by the service poller framework.
 * </P>
 * <p>
 * This does SNMP and therefore relies on the SNMP configuration so it is not distributable.
 * </p>
 *
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="mailto:mike@opennms.org">Mike Davidson </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
public class SnmpMonitor extends SnmpMonitorStrategy {

    public static final long NANOSECOND_PER_MILLISECOND = 1_000_000;
    
    public static final Logger LOG = LoggerFactory.getLogger(SnmpMonitor.class);

    /**
     * Default object to collect if "oid" property not available.
     */
    private static final String DEFAULT_OBJECT_IDENTIFIER = ".1.3.6.1.2.1.1.2.0"; // MIB-II
                                                                                // System
                                                                                // Object
                                                                                // Id

    private static final String DEFAULT_REASON_TEMPLATE = "Observed value '${observedValue}' does not meet criteria '${operator} ${operand}'";
    private final StrategyResolver strategyResolver;
    private final SnmpHelper snmpHelper;

    private final Descriptors.FieldDescriptor communityFieldDescriptor;
    private final Descriptors.FieldDescriptor hostFieldDescriptor;
    private final Descriptors.FieldDescriptor hexFieldDescriptor;
    private final Descriptors.FieldDescriptor oidFieldDescriptor;
    private final Descriptors.FieldDescriptor operatorFieldDescriptor;
    private final Descriptors.FieldDescriptor operandFieldDescriptor;
    private final Descriptors.FieldDescriptor reasonTemplateFieldDescriptor;
    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public SnmpMonitor(StrategyResolver strategyResolver, SnmpHelper snmpHelper) {
        this.strategyResolver = strategyResolver;
        this.snmpHelper = snmpHelper;

        Descriptors.Descriptor snmpMonitorRequestDescriptor = SnmpMonitorRequest.getDefaultInstance().getDescriptorForType();

        communityFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.COMMUNITY_FIELD_NUMBER);
        hexFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.HEX_FIELD_NUMBER);
        hostFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.HOST_FIELD_NUMBER);
        oidFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.OID_FIELD_NUMBER);
        operandFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.OPERAND_FIELD_NUMBER);
        operatorFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.OPERATOR_FIELD_NUMBER);
        reasonTemplateFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.REASON_TEMPLATE_FIELD_NUMBER);
        retriesFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor = snmpMonitorRequestDescriptor.findFieldByNumber(SnmpMonitorRequest.TIMEOUT_FIELD_NUMBER);
    }

    /**
     * {@inheritDoc}
     *
     * <P>
     * The poll() method is responsible for polling the specified address for
     * SNMP service availability.
     * </P>
     * @exception RuntimeException
     *                Thrown for any unrecoverable errors.
     */

    @Override
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config) {

        CompletableFuture<ServiceMonitorResponse> future = null;
        String hostAddress = null;

        // Establish SNMP session with interface
        //
        try {
            if (! config.is(SnmpMonitorRequest.class)) {
                throw new IllegalArgumentException("config must be an SnmpRequest; type-url=" + config.getTypeUrl());
            }

            SnmpMonitorRequest snmpMonitorRequest = config.unpack(SnmpMonitorRequest.class);
            SnmpMonitorRequest effectiveSnmpMonitorRequest = populateDefaultsAsNeeded(snmpMonitorRequest);

            // Retrieve this interface's SNMP peer object
            //
            SnmpAgentConfig agentConfig = getAgentConfig(svc, effectiveSnmpMonitorRequest);
            hostAddress = effectiveSnmpMonitorRequest.getHost();

            // Get configuration parameters
            //
            String oid = effectiveSnmpMonitorRequest.getOid();
            String operator = protobufDefaultNullHelper(effectiveSnmpMonitorRequest, operatorFieldDescriptor);
            String operand = protobufDefaultNullHelper(effectiveSnmpMonitorRequest, operandFieldDescriptor);
            String reasonTemplate = effectiveSnmpMonitorRequest.getReasonTemplate();
            boolean hex = effectiveSnmpMonitorRequest.getHex();

            agentConfig.setTimeout(effectiveSnmpMonitorRequest.getTimeout());
            agentConfig.setRetries(effectiveSnmpMonitorRequest.getRetries());

            // TBD888
            // String walkstr = ParameterMap.getKeyedString(config, "walk", "false");
            // String matchstr = ParameterMap.getKeyedString(config, "match-all", "true");
            // int countMin = ParameterMap.getKeyedInteger(config, "minimum", 0);
            // int countMax = ParameterMap.getKeyedInteger(config, "maximum", 0);
            // String reasonTemplate = ParameterMap.getKeyedString(config, "reason-template", DEFAULT_REASON_TEMPLATE);

            // set timeout and retries on SNMP peer object
            //
            // agentConfig.setPort(ParameterMap.getKeyedInteger(config, "port", agentConfig.getPort()));

            // Squirrel the configuration parameters away in a Properties for later expansion if service is down
            // Properties svcParams = new Properties();
            // svcParams.setProperty("oid", oid);
            // svcParams.setProperty("operator", String.valueOf(operator));
            // svcParams.setProperty("operand", String.valueOf(operand));
            // svcParams.setProperty("walk", walkstr);
            // svcParams.setProperty("matchAll", matchstr);
            // svcParams.setProperty("minimum", String.valueOf(countMin));
            // svcParams.setProperty("maximum", String.valueOf(countMax));
            // svcParams.setProperty("timeout", String.valueOf(agentConfig.getTimeout()));
            // svcParams.setProperty("retry", String.valueOf(agentConfig.getRetries()));
            // svcParams.setProperty("retries", svcParams.getProperty("retry"));
            // svcParams.setProperty("ipaddr", hostAddress);
            // svcParams.setProperty("port", String.valueOf(agentConfig.getPort()));
            // svcParams.setProperty("hex", hexstr);


//            TODO: Removing to decouple from horizon core
//            TimeoutTracker tracker = new TimeoutTracker(parameters, agentConfig.getRetries(), agentConfig.getTimeout());
//            tracker.reset();
//            tracker.startAttempt();


            final String finalHostAddress = hostAddress;
            SnmpObjId snmpObjectId = SnmpObjId.get(oid);

            long startTimestamp = System.nanoTime();

            future =
                snmpHelper.getAsync(agentConfig, new SnmpObjId[]{ snmpObjectId })
                    .thenApply(result -> processSnmpResponse(result, finalHostAddress, snmpObjectId, operator, operand, startTimestamp))
                    .completeOnTimeout(this.createTimeoutResponse(finalHostAddress), agentConfig.getTimeout(), TimeUnit.MILLISECONDS)
                    .exceptionally((thrown) -> this.createExceptionResponse(thrown, finalHostAddress))
            ;

            return future;
        } catch (NumberFormatException e) {
            LOG.debug("Number operator used in a non-number evaluation", e);
            return CompletableFuture.completedFuture(ServiceMonitorResponseImpl.builder().reason(e.getMessage()).status(Status.Unknown).build());
        } catch (IllegalArgumentException e) {
            LOG.debug("Invalid SNMP Criteria", e);
            return CompletableFuture.completedFuture(ServiceMonitorResponseImpl.builder().reason(e.getMessage()).status(Status.Unknown).build());
        } catch (Throwable t) {
            LOG.debug("Unexpected exception during SNMP poll of interface {}", hostAddress, t);
            return CompletableFuture.completedFuture(ServiceMonitorResponseImpl.builder().reason(t.getMessage()).status(Status.Unknown).build());
        }
    }

//========================================
// Internal Methods
//----------------------------------------

    private SnmpMonitorRequest populateDefaultsAsNeeded(SnmpMonitorRequest snmpMonitorRequest) {
        SnmpMonitorRequest.Builder resultBuilder = SnmpMonitorRequest.newBuilder(snmpMonitorRequest);


        if (! snmpMonitorRequest.hasField(communityFieldDescriptor)) {
            resultBuilder.setCommunity(SnmpConfiguration.DEFAULT_READ_COMMUNITY);
        }

        if (! snmpMonitorRequest.hasField(hexFieldDescriptor)) {
            resultBuilder.setHex(false);
        }

        if (! snmpMonitorRequest.hasField(oidFieldDescriptor)) {
            resultBuilder.setOid(DEFAULT_OBJECT_IDENTIFIER);
        }

        if (! snmpMonitorRequest.hasField(reasonTemplateFieldDescriptor)) {
            resultBuilder.setReasonTemplate(DEFAULT_REASON_TEMPLATE);
        }

        if (! snmpMonitorRequest.hasField(retriesFieldDescriptor)) {
            resultBuilder.setRetries(SnmpConfiguration.DEFAULT_RETRIES);
        }

        if (! snmpMonitorRequest.hasField(timeoutFieldDescriptor)) {
            resultBuilder.setTimeout(SnmpConfiguration.DEFAULT_TIMEOUT);
        }

        return resultBuilder.build();
    }

    private String protobufDefaultNullHelper(Message msg, Descriptors.FieldDescriptor fieldDescriptor) {
        if (! msg.hasField(fieldDescriptor)) {
            return null;
        }

        return (String) msg.getField(fieldDescriptor);
    }

    private ServiceMonitorResponse
    processSnmpResponse(
        SnmpValue[] result,
        String hostAddress,
        SnmpObjId oid,
        String operator,
        String operand,
        long startTimestamp
    ) {
        long endTimestamp = System.nanoTime();
        long elapsedTimeNs = ( endTimestamp - startTimestamp );
        double elapsedTimeMs = (double) elapsedTimeNs / NANOSECOND_PER_MILLISECOND;

        ServiceMonitorResponseImplBuilder builder = ServiceMonitorResponseImpl.builder()
            .monitorType(MonitorType.SNMP)
            .status(Status.Unknown)
            .responseTime(elapsedTimeMs)
            .ipAddress(hostAddress)
            ;

        Map<String, Number> metrics = new HashMap<>();

        if (result[0] != null) {
            LOG.debug("poll: SNMP poll succeeded, addr={} oid={} value={}", hostAddress, oid, result);

            if (result[0].isNumeric()) {
                metrics.put("observedValue", result[0].toLong());
            }

            if (meetsCriteria(result[0], operator, operand)) {
                builder.status(Status.Up);
            } else {
                builder.status(Status.Down);
            }

            // if (DEFAULT_REASON_TEMPLATE.equals(reasonTemplate)) {
            //     if (operator != null) {
            //         reasonTemplate = "Observed value '${observedValue}' does not meet criteria '${operator} ${operand}'";
            //     } else {
            //         reasonTemplate = "Observed value '${observedValue}' was null";
            //     }
            // }
        } else {
            String reason = "SNMP poll failed, addr=" + hostAddress + " oid=" + oid;
            builder.reason(reason);

            LOG.debug(reason);
        }

        builder.properties(metrics);
        return builder.build();
    }

    // NOTE: this is called at call-setup time, not after the timeout.
    private ServiceMonitorResponse createTimeoutResponse(String hostAddress) {
        ServiceMonitorResponse response =
            ServiceMonitorResponseImpl.builder()
                .monitorType(MonitorType.SNMP)
                .status(Status.Unknown)
                .ipAddress(hostAddress)
                .reason("timeout")
                .responseTime(-1)
                .build()
            ;

        return response;
    }

    private ServiceMonitorResponse createExceptionResponse(Throwable thrown, String hostAddress) {
        LOG.debug("SNMP poll failed", thrown);

        ServiceMonitorResponse response =
            ServiceMonitorResponseImpl.builder()
                .monitorType(MonitorType.SNMP)
                .status(Status.Unknown)
                .ipAddress(hostAddress)
                .reason(thrown.getMessage())
                .build()
            ;

        return response;
    }
}
