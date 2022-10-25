/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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
 ******************************************************************************/

package org.opennms.horizon.dockerit;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * IT runner that kicks off Cucumber.
 *
 * NOTE: glue lists the packages that Cucumber scans for Step Definitions (i.e. the Java classes that define the
 *  code executed for steps in the features file).  The minion-gateway wiremock package is needed to make use of the
 *  steps defined in the package from the minion gateway mock.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    glue = {"org.opennms.horizon.dockerit", "org.opennms.horizon.testtool.miniongateway.wiremock.client"},
    plugin = {"json:target/cucumber-report.json", "html:target/cucumber.html", "pretty"}
    )
public class CucumberRunnerIT {
}
