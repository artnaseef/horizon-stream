/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.detector.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opennms.horizon.ipc.rpc.api.RemoteExecutionException;
import org.opennms.horizon.ipc.rpc.api.RpcResponse;
import org.opennms.netmgt.provision.DetectResults;

@Data
@NoArgsConstructor
public class DetectorResponseDTO implements DetectResults, RpcResponse {

    private String error;
    private boolean detected;
    private List<DetectorAttributeDTO> attributes = new ArrayList<>();

    public DetectorResponseDTO(DetectResults results) {
        setDetected(results.isServiceDetected());
        if (results.getServiceAttributes() != null) {
            addAttributes(results.getServiceAttributes());
        }
    }

    public DetectorResponseDTO(Throwable t) {
        setDetected(false);
        error = RemoteExecutionException.toErrorMessage(t);
    }

    public void addAttribute(String key, String value) {
        attributes.add(new DetectorAttributeDTO(key, value));
    }

    public void addAttributes(Map<String, String> attributes) {
        attributes.entrySet().stream()
            .forEach(e -> addAttribute(e.getKey(), e.getValue()));
    }

    public Map<String, String> getAttributesMap() {
        return attributes.stream().collect(Collectors.toMap(DetectorAttributeDTO::getKey,
                DetectorAttributeDTO::getValue));
    }

    @Override
    public boolean isServiceDetected() {
        return isDetected();
    }

    @Override
    public Map<String, String> getServiceAttributes() {
        return getAttributesMap();
    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(detected, error, attributes);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        DetectorResponseDTO other = (DetectorResponseDTO) obj;
//        return Objects.equals(this.detected, other.detected) &&
//                Objects.equals(this.error, other.error) &&
//                Objects.equals(this.attributes, other.attributes);
//    }

    @Override
    public String getErrorMessage() {
        return error;
    }

}