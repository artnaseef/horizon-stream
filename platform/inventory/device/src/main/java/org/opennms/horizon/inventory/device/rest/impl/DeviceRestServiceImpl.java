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

import javax.ws.rs.core.Response;

import com.google.common.base.Strings;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventForwarder;
import org.opennms.horizon.inventory.device.rest.api.DeviceRestService;
import org.opennms.horizon.inventory.device.service.DeviceService;
import org.opennms.horizon.shared.dto.device.DeviceCreateDTO;
import org.opennms.horizon.shared.dto.device.DeviceDTO;

public class DeviceRestServiceImpl implements DeviceRestService {
  private DeviceService service;

  private EventForwarder eventForwarder;

  public Response getById(Integer id) {
      DeviceDTO device = service.getDevice(id);
      if(device == null) {
          return Response.noContent().build();
      }
    return Response.ok(device).build();
  }

  public Response findAll() {
    return Response.ok(service.searchDevices()).build();
  }

  @Override
  public Response createDevice(DeviceCreateDTO newDevice) {
    try {
        if (Strings.isNullOrEmpty(newDevice.getLabel())) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "label can't be empty").build();
        }
        Integer nodeId = service.createDevice(newDevice);
        eventForwarder.sendNow(new EventBuilder(EventConstants.NODE_ADDED_EVENT_UEI, "Device-Rest-Service")
            .setNodeid(nodeId).getEvent());
        return Response.ok(nodeId).build();
    } catch (Exception e){
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

    public void setService(DeviceService service) {
        this.service = service;
    }

    public void setEventForwarder(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }
}
