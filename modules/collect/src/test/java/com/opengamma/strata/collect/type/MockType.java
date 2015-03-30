/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.type;

import org.joda.convert.FromString;

/**
 * The mock type.
 */
public final class MockType
    extends TypedString<MockType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains a {@code MockType} by name.
   * 
   * @param name  the name to lookup, not null
   * @return the type matching the name, not null
   */
  @FromString
  public static MockType of(String name) {
    return new MockType(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the name, not null
   */
  private MockType(String name) {
    super(name);
  }

}
