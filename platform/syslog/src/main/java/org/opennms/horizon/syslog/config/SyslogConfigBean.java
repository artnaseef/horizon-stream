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
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.syslog.config;

import java.io.IOException;
import java.util.List;
import java.util.TimeZone;

public class SyslogConfigBean implements SyslogdConfig{

    private final SyslogdConfiguration syslogConfig;

    public SyslogConfigBean(SyslogdConfiguration syslogConfig) {
        this.syslogConfig = syslogConfig;
    }


    @Override
    public int getSyslogPort() {
        return syslogConfig.getConfiguration().getSyslogPort();
    }

    @Override
    public String getListenAddress() {
        return syslogConfig.getConfiguration().getListenAddress();
    }

    @Override
    public boolean getNewSuspectOnMessage() {
        return syslogConfig.getConfiguration().getNewSuspectOnMessage();
    }

    @Override
    public String getForwardingRegexp() {
        return syslogConfig.getConfiguration().getForwardingRegexp();
    }

    @Override
    public Integer getMatchingGroupHost() {
        return syslogConfig.getConfiguration().getMatchingGroupHost();
    }

    @Override
    public Integer getMatchingGroupMessage() {
        return syslogConfig.getConfiguration().getMatchingGroupMessage();
    }

    @Override
    public String getParser() {
        return syslogConfig.getConfiguration().getParser();
    }

    @Override
    public List<UeiMatch> getUeiList() {
        return syslogConfig.getUeiMatches();
    }

    @Override
    public List<HideMatch> getHideMessages() {
        return syslogConfig.getHideMatches();
    }

    @Override
    public String getDiscardUei() {
        return syslogConfig.getConfiguration().getDiscardUei();
    }

    @Override
    public int getNumThreads() {
        return syslogConfig.getConfiguration().getThreads();
    }

    @Override
    public int getQueueSize() {
        return syslogConfig.getConfiguration().getQueueSize();
    }

    @Override
    public int getBatchSize() {
        return syslogConfig.getConfiguration().getBatchSize();
    }

    @Override
    public int getBatchIntervalMs() {
        return syslogConfig.getConfiguration().getBatchInterval();
    }

    @Override
    public TimeZone getTimeZone() {
        return syslogConfig.getConfiguration().getTimeZone();
    }

    @Override
    public boolean shouldIncludeRawSyslogmessage() {
        return syslogConfig.getConfiguration().shouldIncludeRawSyslogmessage();
    }

    @Override
    public void reload() throws IOException {

    }
}
