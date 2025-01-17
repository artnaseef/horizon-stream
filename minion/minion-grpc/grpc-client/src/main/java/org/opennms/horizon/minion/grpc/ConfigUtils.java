/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.grpc;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigUtils {

    public static Properties getPropertiesFromConfig(ConfigurationAdmin configAdmin, String pid) {
        Properties properties = new Properties();
        try {
            final Dictionary<String, Object> config = configAdmin.getConfiguration(pid).getProperties();
            if (config != null) {
                final Enumeration<String> keys = config.keys();
                while (keys.hasMoreElements()) {
                    final String key = keys.nextElement();
                    properties.put(key, config.get(key));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config", e);
        }
        return properties;
    }
}
