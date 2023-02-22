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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.VerificationException;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;

public abstract class GrpcTestBase {
    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("grpc.server.port", ()->6767);
    }

    protected final String tenantId = "test-tenant";
    protected final String invalidTenantId = "invalid-tenant";
    protected final String authHeader = "Bearer esgs12345";
    protected final String headerWithoutTenant = "Bearer esgs12345invalid";
    protected final String differentTenantHeader = "Bearer esgs12345different";
    protected ManagedChannel channel;
    @SpyBean
    protected  InventoryServerInterceptor spyInterceptor;

    protected void prepareServer() throws VerificationException {
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
                .usePlaintext().build();
        doReturn(Optional.of(tenantId)).when(spyInterceptor).verifyAccessToken(authHeader);
        doReturn(Optional.of(invalidTenantId)).when(spyInterceptor).verifyAccessToken(differentTenantHeader);
        doReturn(Optional.empty()).when(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        doThrow(new VerificationException()).when(spyInterceptor).verifyAccessToken(null);
    }

    protected void afterTest() throws InterruptedException {
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        verifyNoMoreInteractions(spyInterceptor);
        reset(spyInterceptor);
    }

    protected Metadata createAuthHeader(String value) {
        Metadata headers = new Metadata();
        headers.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, value);
        return headers;
    }
}
