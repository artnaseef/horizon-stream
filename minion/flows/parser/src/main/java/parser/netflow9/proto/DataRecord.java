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


import static listeners.utils.BufferUtils.slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import parser.InvalidPacketException;
import parser.MissingTemplateException;
import parser.ie.Value;
import parser.session.Field;
import parser.session.Session;
import parser.session.Template;

public final class DataRecord implements Record {

    /*
     +--------------------------------------------------+
     | Field Value                                      |
     +--------------------------------------------------+
     | Field Value                                      |
     +--------------------------------------------------+
      ...
     +--------------------------------------------------+
     | Field Value                                      |
     +--------------------------------------------------+
    */

    public final DataSet set;  // Enclosing set

    public final Template template;

    public final List<Value<?>> scopes;
    public final List<Value<?>> fields;
    public final List<Value<?>> options;

    public DataRecord(final DataSet set,
                      final Session.Resolver resolver,
                      final Template template,
                      final ByteBuf buffer) throws InvalidPacketException, MissingTemplateException {
        this.set = Objects.requireNonNull(set);

        this.template = Objects.requireNonNull(template);

        final List<Value<?>> scopes = new ArrayList<>(this.template.scopes.size());
        for (final Field scope : template.scopes) {
            scopes.add(scope.parse(resolver, slice(buffer, scope.length())));
        }

        final List<Value<?>> fields = new ArrayList<>(this.template.fields.size());
        for (final Field field : template.fields) {
            fields.add(field.parse(resolver, slice(buffer, field.length())));
        }

        this.scopes = Collections.unmodifiableList(scopes);
        this.fields = Collections.unmodifiableList(fields);

        // Expand the data record by appending values from
        this.options = resolver.lookupOptions(ScopeFieldSpecifier.buildScopeValues(this));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scopes", this.scopes)
                .add("fields", this.fields)
                .add("options", this.options)
                .toString();
    }
}
