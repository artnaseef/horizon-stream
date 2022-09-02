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

package org.opennms.horizon.inventory.device.rest.impl;

import com.google.common.base.Strings;
import org.opennms.horizon.db.dao.api.MonitoringLocationDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsMonitoringLocation;
import org.opennms.horizon.inventory.device.rest.api.LocationRestService;
import org.opennms.horizon.inventory.device.utils.LocationMapper;
import org.opennms.horizon.shared.dto.device.LocationCollectionDTO;
import org.opennms.horizon.shared.dto.device.LocationDTO;

import javax.ws.rs.core.Response;
import java.util.List;


public class LocationRestServiceImpl implements LocationRestService {

    private final MonitoringLocationDao monitoringLocationDao;

    private final LocationMapper locationMapper;

    private final SessionUtils sessionUtils;

    public LocationRestServiceImpl(MonitoringLocationDao monitoringLocationDao,
                                   LocationMapper locationMapper,
                                   SessionUtils sessionUtils) {
        this.monitoringLocationDao = monitoringLocationDao;
        this.locationMapper = locationMapper;
        this.sessionUtils = sessionUtils;
    }

    @Override
    public Response getByLocation(String location) {
        if (Strings.isNullOrEmpty(location)) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "location can't be empty").build();
        }
        OnmsMonitoringLocation monitoringLocation = sessionUtils.withReadOnlyTransaction(() -> monitoringLocationDao.get(location));
        LocationDTO locationDTO = locationMapper.toDto(monitoringLocation);
        return Response.ok(locationDTO).build();
    }

    @Override
    public Response findAll() {
        List<OnmsMonitoringLocation> locations = sessionUtils.withReadOnlyTransaction(monitoringLocationDao::findAll);
        List<LocationDTO> locationDTOList = locationMapper.listToDto(locations);
        return Response.ok(new LocationCollectionDTO(locationDTOList)).build();
    }
}
