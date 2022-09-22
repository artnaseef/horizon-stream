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
 *     http://www.gnu.org/licenses/
 * 
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.syslog.config;


import org.opennms.core.xml.ValidateUsing;
import org.opennms.horizon.core.lib.ConfigUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * String against which to match the hostname; interpreted
 *  as a regular expression.
 */
@XmlRootElement(name = "hostname-match")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("syslog.xsd")
public class HostnameMatch implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * The regular expression
     */
    @XmlAttribute(name = "expression", required = true)
    private String m_expression;

    public HostnameMatch() {
    }

    public String getExpression() {
        return m_expression;
    }

    public void setExpression(final String expression) {
        m_expression = ConfigUtils.assertNotEmpty(expression, "expression");
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_expression);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }

        if (obj instanceof HostnameMatch) {
            final HostnameMatch that = (HostnameMatch)obj;
            return Objects.equals(this.m_expression, that.m_expression);
        }
        return false;
    }

}
