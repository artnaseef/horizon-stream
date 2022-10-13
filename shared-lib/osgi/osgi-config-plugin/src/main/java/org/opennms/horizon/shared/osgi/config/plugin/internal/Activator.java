package org.opennms.horizon.shared.osgi.config.plugin.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationPlugin;

public class Activator implements BundleActivator {

    private ServiceRegistration<ConfigurationPlugin> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        EnvVarsConfigurationPlugin karafConfigurationPlugin = new EnvVarsConfigurationPlugin();
        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(ConfigurationPlugin.CM_RANKING, EnvVarsConfigurationPlugin.PLUGIN_RANKING);
        serviceProps.put("config.plugin.id", EnvVarsConfigurationPlugin.PLUGIN_ID);
        registration = context.registerService(ConfigurationPlugin.class, karafConfigurationPlugin, serviceProps);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
        }
    }

}
