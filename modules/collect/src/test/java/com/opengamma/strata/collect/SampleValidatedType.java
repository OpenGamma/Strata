/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

/**
 * The sample type.
 */
public final class SampleValidatedType
    extends TypedString<SampleValidatedType> {

  /** Validation of name. */
  private static final Pattern PATTERN = Pattern.compile("[A-Z]+");
  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains an instance from the specified name.
   * 
   * @param name  the name to lookup, not null
   * @return the type matching the name, not null
   */
  @FromString
  public static SampleValidatedType of(String name) {
    return new SampleValidatedType(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the name, not null
   */
  private SampleValidatedType(String name) {
    super(name, PATTERN, "Name must be letters");
  }

}
