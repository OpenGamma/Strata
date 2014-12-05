/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.convert.FromString;

import com.opengamma.collect.type.TypedString;

/**
 * The type of a security.
 * <p>
 * This identifies the type of a security at the data model level.
 * It is not intended for general use in classifying securities.
 */
public final class SecurityType
    extends TypedString<SecurityType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains a {@code SecurityType} by name.
   * 
   * @param name  the name to lookup
   * @return the type matching the name
   */
  @FromString
  public static SecurityType of(String name) {
    return new SecurityType(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private SecurityType(String name) {
    super(name);
  }

}
