/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: netflow.proto

package parser.flowmessage;

/**
 * Protobuf enum {@code NetflowVersion}
 */
public enum NetflowVersion
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>V5 = 0;</code>
   */
  V5(0),
  /**
   * <code>V9 = 1;</code>
   */
  V9(1),
  /**
   * <code>IPFIX = 2;</code>
   */
  IPFIX(2),
  UNRECOGNIZED(-1),
  ;

  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @Deprecated
  public static NetflowVersion valueOf(int value) {
    return forNumber(value);
  }

  public static NetflowVersion forNumber(int value) {
    switch (value) {
      case 0: return V5;
      case 1: return V9;
      case 2: return IPFIX;
      default: return null;
    }
  }

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return FlowProtos.getDescriptor().getEnumTypes().get(2);
  }

  private final int value;

  NetflowVersion(int value) {
    this.value = value;
  }
}
