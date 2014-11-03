/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.convert.FromString;

import com.opengamma.collect.type.TypedString;

/**
 * The type of a security data entity.
 * <p>
 * This identifies the type of a trade at the data model level.
 */
public final class SecurityType
    extends TypedString<SecurityType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains a {@code SecurityType} by name.
   * 
   * @param name  the name to lookup, not null
   * @return the type matching the name, not null
   */
  @FromString
  public static SecurityType of(String name) {
    return new SecurityType(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the name, not null
   */
  private SecurityType(String name) {
    super(name);
  }

}
