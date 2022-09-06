package org.opennms.horizon.minion.snmp;

import java.util.function.Consumer;
import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.shared.snmp.SnmpStrategy;
import org.opennms.horizon.shared.snmp.StrategyResolver;

public class SnmpMonitorManager implements ServiceMonitorManager {

    private final StrategyResolver strategyResolver;

    public SnmpMonitorManager(StrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    @Override
    public ServiceMonitor create(Consumer<ServiceMonitorResponse> resultProcessor) {
        return new SnmpMonitor(strategyResolver);
    }
}