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

package parser.netflow9.proto;

import static listeners.utils.BufferUtils.uint16;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import parser.InvalidPacketException;
import parser.MissingTemplateException;
import parser.Protocol;
import parser.ie.InformationElement;
import parser.ie.InformationElementDatabase;
import parser.ie.Value;
import parser.ie.values.UndeclaredValue;
import parser.session.Field;
import parser.session.Session;

public final class FieldSpecifier implements Field {
    private static final Logger LOG = LoggerFactory.getLogger(FieldSpecifier.class);

    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |        Field Type N           |         Field Length N        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    */

    public final static int SIZE = 4;

    public final int fieldType; // uint16
    public final int fieldLength; // uint16

    public final InformationElement informationElement;

    public FieldSpecifier(final ByteBuf buffer) throws InvalidPacketException {
        this.fieldType = uint16(buffer);
        this.fieldLength = uint16(buffer);

        this.informationElement = InformationElementDatabase.instance
                .lookup(Protocol.NETFLOW9, this.fieldType).orElseGet(() -> {
                    LOG.warn("Undeclared field type: {}", UndeclaredValue.nameFor(Optional.empty(), this.fieldType));
                    return UndeclaredValue.parser(this.fieldType);
                });

        if (this.fieldLength > this.informationElement.getMaximumFieldLength() || this.fieldLength < this.informationElement.getMinimumFieldLength()) {
            throw new InvalidPacketException(buffer, "Template field '%s' has illegal size: %d (min=%d, max=%d)",
                    this.informationElement.getName(),
                    this.fieldLength,
                    this.informationElement.getMinimumFieldLength(),
                    this.informationElement.getMaximumFieldLength());
        }
    }

    @Override
    public Value<?> parse(final Session.Resolver resolver, final ByteBuf buffer) throws InvalidPacketException, MissingTemplateException {
        return this.informationElement.parse(resolver, buffer);
    }

    @Override
    public int length() {
        return this.fieldLength;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fieldType", fieldType)
                .add("fieldLength", fieldLength)
                .toString();
    }
}
