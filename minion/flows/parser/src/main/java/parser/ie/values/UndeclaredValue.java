/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package parser.ie.values;

import static listeners.utils.BufferUtils.bytes;

import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import parser.ie.InformationElement;
import parser.ie.Value;
import parser.session.Session;

public class UndeclaredValue extends Value<byte[]> {
    public final byte[] value;

    public UndeclaredValue(final Optional<Long> enterpriseNumber,
                           final int informationElementId,
                           final byte[] value) {
        super(nameFor(enterpriseNumber, informationElementId), Optional.empty());
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", getName())
                .add("data", value)
                .toString();
    }

    @Override
    public byte[] getValue() {
        return this.value;
    }

    @Override
    public void visit(final Visitor visitor) {
        visitor.accept(this);
    }

    public static InformationElement parser(final int informationElementId) {
        return parser(Optional.empty(), informationElementId);
    }

    public static InformationElement parser(final Optional<Long> enterpriseNumber,
                                            final int informationElementId) {
        return new InformationElement() {
            @Override
            public Value<?> parse(final Session.Resolver resolver, final ByteBuf buffer) {
                return new UndeclaredValue(enterpriseNumber, informationElementId, bytes(buffer, buffer.readableBytes()));
            }

            @Override
            public String getName() {
                return nameFor(enterpriseNumber, informationElementId);
            }

            @Override
            public int getMinimumFieldLength() {
                return 0;
            }

            @Override
            public int getMaximumFieldLength() {
                return 0xFFFF;
            }
        };
    }

    public static String nameFor(final Optional<Long> enterpriseNumber,
                                 final int informationElementId) {
        return enterpriseNumber.map(en -> Long.toString(en) + ':').orElse("") + informationElementId;
    }
}
