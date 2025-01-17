/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.jicmp.standalone;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.minion.jicmp.jna.NativeDatagramSocket;
import org.opennms.horizon.minion.jicmp.ipv6.ICMPv6EchoPacket;
import org.opennms.horizon.minion.jicmp.ipv6.ICMPv6Packet;
import org.opennms.horizon.minion.jicmp.ipv6.ICMPv6Packet.Type;
import org.opennms.horizon.minion.jicmp.jna.NativeDatagramPacket;

import com.sun.jna.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PingListener
 *
 * @author brozow
 */
public class V6Pinger extends AbstractPinger<Inet6Address> {
    private static final Logger log = LoggerFactory.getLogger(V6Pinger.class);

    public V6Pinger(int id) throws Exception {
        super(NativeDatagramSocket.create(NativeDatagramSocket.PF_INET6, NativeDatagramSocket.IPPROTO_ICMPV6, id));
        
        // Windows requires at least one packet sent before a receive call can be made without error
        // so we send a packet here to make sure...  This one should not match the normal ping requests
        // since it does not contain the cookie so it won't interface.
        if (Platform.isWindows()) {
            final ICMPv6EchoPacket packet = new ICMPv6EchoPacket(64);
            packet.setCode(0);
            packet.setType(Type.EchoRequest);
            packet.getContentBuffer().putLong(System.nanoTime());
            packet.getContentBuffer().putLong(System.nanoTime());
            getPingSocket().send(packet.toDatagramPacket(InetAddress.getByName("::1")));
        }
    }
    
    @Override
    public void run() {
        try {
            final NativeDatagramPacket datagram = new NativeDatagramPacket(65535);
            while (!isFinished()) {
                getPingSocket().receive(datagram);
                final long received = System.nanoTime();
    
                final ICMPv6Packet icmpPacket = new ICMPv6Packet(getIPPayload(datagram));
                final V6PingReply echoReply = icmpPacket.getType() == Type.EchoReply ? new V6PingReply(icmpPacket, received) : null;
            
                if (echoReply != null && echoReply.isValid()) {
                    // 64 bytes from 127.0.0.1: icmp_seq=0 time=0.069 ms
                    System.out.printf("%d bytes from [%s]: tid=%d icmp_seq=%d time=%.3f ms%n", 
                        echoReply.getPacketLength(),
                        datagram.getAddress().getHostAddress(),
                        echoReply.getIdentifier(),
                        echoReply.getSequenceNumber(),
                        echoReply.elapsedTime(TimeUnit.MILLISECONDS)
                    );
                    for (PingReplyListener listener : getListeners()) {
                        listener.onPingReply(datagram.getAddress(), echoReply);
                    }
                }
            }
        } catch(final Throwable e) {
            m_throwable.set(e);
            log.error("Failed to run v6 pinger", e);
        }
    }

    private ByteBuffer getIPPayload(final NativeDatagramPacket datagram) {
        return datagram.getContent();
    }
    
    @Override
    public PingReplyMetric ping(final Inet6Address addr, final int id, final int sequenceNumber, final int count, final long interval) throws InterruptedException {
        final PingReplyMetric metric = new PingReplyMetric(count, interval);
        addPingReplyListener(metric);
        final NativeDatagramSocket socket = getPingSocket();
        for(int i = sequenceNumber; i < sequenceNumber + count; i++) {
            final V6PingRequest request = new V6PingRequest(id, i);
            request.send(socket, addr);
            Thread.sleep(interval);
        }
        return metric;
    }

}
