/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package parser.factory;

import java.util.Objects;

import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;

import com.codahale.metrics.MetricRegistry;

import listeners.Parser;
import listeners.factory.ParserDefinition;
import listeners.factory.UdpListenerMessage;
import parser.Netflow5UdpParser;
import parser.UdpListenerModule;

public class Netflow5UdpParserFactory implements ParserFactory {


    private final DnsResolver dnsResolver;

    private final UdpListenerModule udpListenerModule;

    private final MessageDispatcherFactory messageDispatcherFactory;


    public Netflow5UdpParserFactory(final MessageDispatcherFactory messageDispatcherFactory, final DnsResolver dnsResolver,
                                    final UdpListenerModule udpListenerModule) {
        this.dnsResolver = Objects.requireNonNull(dnsResolver);
        this.udpListenerModule = Objects.requireNonNull(udpListenerModule);
        this.messageDispatcherFactory = Objects.requireNonNull(messageDispatcherFactory);
    }

    @Override
    public Class<? extends Parser> getBeanClass() {
        return Netflow5UdpParser.class;
    }

    @Override
    public Parser createBean(final ParserDefinition parserDefinition) {
        final AsyncDispatcher<UdpListenerMessage> dispatcher = messageDispatcherFactory.createAsyncDispatcher(udpListenerModule);
        return new Netflow5UdpParser(parserDefinition.getFullName(), dispatcher, dnsResolver, new MetricRegistry());
    }
}
