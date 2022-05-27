package org.opennms.netmgt.provision.service.scan;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.opennms.netmgt.provision.persistence.dto.RequisitionDTO;
import org.opennms.netmgt.provision.persistence.dto.RequisitionNodeDTO;
import org.opennms.netmgt.provision.service.Provisioner;

@AllArgsConstructor
@Slf4j
public class NodeScanner {

    public static final String DIRECT_SCAN = "direct:scan";

    private final CamelContext context;
    private final Provisioner provisioner;

    public void init() {
        try {
            context.addRoutes(createRoutes());
        } catch (Exception e) {
            throw new RuntimeException("Exception while creating scanner route", e);
        }
    }

    private RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                from(DIRECT_SCAN).
                        routeId("provision::scan").
                        log(LoggingLevel.INFO, "Provision :: Scan Request").
                        process(new SimpleScanner());
            }
        };
    }

    @NoArgsConstructor
    private class SimpleScanner implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            List<RequisitionDTO> requisitions = provisioner.read();
            log.info("Found {} requisitions for scanning", requisitions.size());
            requisitions.forEach(req -> {log.info("requisition: {}", req);req.getNodes().values().forEach(node -> scanNode(node));});
        }

        private void scanNode(RequisitionNodeDTO node) {
            node.getInterfaces().values().forEach(intrfc -> log.info("Interface {}", intrfc));
            //TODO: put in gRPC call to the minions here?
        }
    }
}