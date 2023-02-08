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

package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.Int64Value;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class TagGrpcItTest extends GrpcTestBase {
    private static final String TEST_NODE_LABEL_1 = "node-label-1";
    private static final String TEST_NODE_LABEL_2 = "node-label-2";
    private static final String TEST_LOCATION = "test-location";
    private static final String TEST_TAG_NAME_1 = "tag-name-1";
    private static final String TEST_TAG_NAME_2 = "tag-name-2";

    private TagServiceGrpc.TagServiceBlockingStub serviceStub;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private MonitoringLocationRepository locationRepository;

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = TagServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        tagRepository.deleteAll();
        nodeRepository.deleteAll();
        locationRepository.deleteAll();
        afterTest();
    }

    @Test
    void testCreateTag() throws Exception {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO);


        List<Tag> allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        Tag savedTag = allTags.get(0);
        assertEquals(createDTO.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateTagAlreadyCreatedOnce() throws Exception {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO)).setNodeId(nodeId).build();

        for (int index = 0; index < 2; index++) {
            serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO);
        }

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        Tag savedTag = allTags.get(0);
        assertEquals(createDTO.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateTwoTagsOnNode() throws Exception {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        //tag 1
        Tag savedTag = allTags.get(0);
        assertEquals(createDTO1.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());

        //tag 2
        savedTag = allTags.get(1);
        assertEquals(createDTO2.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateTagThenRemoveTag() throws Exception {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        Tag savedTag = allTags.get(0);
        assertEquals(createDTO1.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());

        List<Int64Value> removeTagIds = Collections.singletonList(Int64Value.of(savedTag.getId()));
        TagRemoveListDTO removeListDTO = TagRemoveListDTO.newBuilder()
            .setNodeId(nodeId).addAllTagIds(removeTagIds).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).removeTags(removeListDTO);

        allTags = tagRepository.findAll();
        assertEquals(0, allTags.size());

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testMultipleCreateTagThenRemoveOne() throws Exception {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        long lastSavedTagId = -1;

        for (Tag savedTag : allTags) {
            assertNotNull(savedTag.getName());
            assertEquals(tenantId, savedTag.getTenantId());

            List<Node> nodes = savedTag.getNodes();
            assertEquals(1, nodes.size());

            Node node = nodes.get(0);
            assertEquals(nodeId, node.getId());
            assertEquals(tenantId, node.getTenantId());

            lastSavedTagId = savedTag.getId();
        }

        List<Int64Value> removeTagIds = Collections.singletonList(Int64Value.of(lastSavedTagId));
        TagRemoveListDTO removeListDTO = TagRemoveListDTO.newBuilder()
            .setNodeId(nodeId).addAllTagIds(removeTagIds).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).removeTags(removeListDTO);

        allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        verify(spyInterceptor, times(3)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(3)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateTwoTagsTwoNodes() throws Exception {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(tenantId);
        location = locationRepository.saveAndFlush(location);

        Node node1 = new Node();
        node1.setNodeLabel(TEST_NODE_LABEL_1);
        node1.setCreateTime(LocalDateTime.now());
        node1.setTenantId(tenantId);
        node1.setMonitoringLocation(location);
        node1.setMonitoringLocationId(location.getId());
        node1 = nodeRepository.saveAndFlush(node1);

        Node node2 = new Node();
        node2.setNodeLabel(TEST_NODE_LABEL_2);
        node2.setCreateTime(LocalDateTime.now());
        node2.setTenantId(tenantId);
        node2.setMonitoringLocation(location);
        node2.setMonitoringLocationId(location.getId());
        node2 = nodeRepository.saveAndFlush(node2);

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(List.of(createDTO1, createDTO2)).setNodeId(node1.getId()).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO3 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO3 = TagCreateListDTO.newBuilder()
            .addAllTags(List.of(createDTO3)).setNodeId(node2.getId()).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO3);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        List<Node> allNodes = nodeRepository.findAll();
        assertEquals(2, allNodes.size());

        Node savedNode1 = allNodes.get(0);
        assertEquals(node1.getId(), savedNode1.getId());
        assertEquals(2, savedNode1.getTags().size());
        assertEquals(TEST_TAG_NAME_1, savedNode1.getTags().get(0).getName());
        assertEquals(TEST_TAG_NAME_2, savedNode1.getTags().get(1).getName());

        Node savedNode2 = allNodes.get(1);
        assertEquals(node2.getId(), savedNode2.getId());
        assertEquals(1, savedNode2.getTags().size());
        assertEquals(TEST_TAG_NAME_2, savedNode2.getTags().get(0).getName());

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testGetTagListForNode() throws Exception {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByNodeId(Int64Value.newBuilder().setValue(nodeId).build());
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());

        verify(spyInterceptor, times(3)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(3)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));

    }

    private long setupDatabase() {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(tenantId);
        location = locationRepository.saveAndFlush(location);

        Node node = new Node();
        node.setNodeLabel(TEST_NODE_LABEL_1);
        node.setCreateTime(LocalDateTime.now());
        node.setTenantId(tenantId);
        node.setMonitoringLocation(location);
        node.setMonitoringLocationId(location.getId());
        node = nodeRepository.saveAndFlush(node);
        return node.getId();
    }
}