package org.opennms.horizon.shared.osgi.config.plugin.internal;

import java.util.Dictionary;
import java.util.Map;
import java.util.function.Supplier;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;

/**
 * Configuration plugin which attempts to map environment variables with configuration properties.
 *
 * Matching rules are environment variable of ORG_APACHE_KARAF_FEATURES_BOOT_FEATURE will fit:
 * - pid org.apache.karaf.features, property boot.feature
 * - pid org.apache.karaf, property features.boot.feature
 *
 * Properties are always lower case with dots as separator.
 */
public class EnvVarsConfigurationPlugin implements ConfigurationPlugin {

    public static final int PLUGIN_RANKING = 510;
    public static final String PLUGIN_ID = "org.opennms.horizon.shared.osgi.config.plugin";
    private final Map<String, String> environment;

    public EnvVarsConfigurationPlugin() {
        this(System::getenv);
    }

    EnvVarsConfigurationPlugin(Supplier<Map<String, String>> environment) {
        this.environment = environment.get();
    }

    @Override
    public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
        final Object pid = properties.get(Constants.SERVICE_PID);
        if (pid == null) {
            return;
        }

        String prefix = (pid.toString()).toUpperCase().replaceAll("\\.", "_");
        Map<String, String> variables = environment;
        for (String key : variables.keySet()) {
            if (key.startsWith(prefix + "__")) {
                // assume pid part is separated by _, so we strip one more character, then replace underscore with dots
                // to construct desired key
                String property = key.substring(prefix.length() + 2)
                    .replaceAll("_", ".").toLowerCase();

                String value = variables.get(key);
                if (value != null) {
                    properties.put(property, value.trim());
                }
            }
        }

    }

}
