package org.opennms.horizon.shared.osgi.config.plugin.internal;

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class EnvVarsConfigurationPluginTest {

    @Mock
    ServiceReference serviceReference;

    @Test
    public void checkBasicLookup() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "a.b");
        EnvVarsConfigurationPlugin envVarsConfigurationPlugin = new EnvVarsConfigurationPlugin(() -> Map.of("A_B__C", "10.0"));
        envVarsConfigurationPlugin.modifyConfiguration(
            serviceReference, properties
        );
        assertEquals("10.0", properties.get("c"));

        properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "a");
        envVarsConfigurationPlugin.modifyConfiguration(
            serviceReference, properties
        );
        assertNull(properties.get("c"));
    }

}
