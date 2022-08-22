/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019-2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.horizon.graph.api.renderer;

import org.opennms.horizon.graph.api.ImmutableGraph;
import org.opennms.horizon.graph.api.ImmutableGraphContainer;
import org.opennms.horizon.graph.api.Vertex;
import org.opennms.horizon.shared.dto.graph.GraphContainerInfo;

import java.util.List;


public interface GraphRenderer {
    String getContentType();
    String render(int identation, List<GraphContainerInfo> containerInfos);
    String render(int identation, ImmutableGraphContainer<?> graphContainer);
    String render(int identation, ImmutableGraph<?, ?> graph);
    String render(int identation, Vertex vertex);

    default String render(List<GraphContainerInfo> containerInfos) {
        return render(0, containerInfos);
    }

    default String render(ImmutableGraphContainer<?> graphContainer) {
        return render(0, graphContainer);
    }

    default String render(ImmutableGraph<?, ?> graph) {
        return render(0, graph);
    }

    default String render(Vertex vertex) {
        return render(0, vertex);
    }
}
