/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * The name of a {@link FunctionGroup}.
 */
public final class FunctionGroupName
    extends TypedString<FunctionGroupName> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Pattern for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code FunctionGroupName} by name.
   * <p>
   * Group names must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the group
   * @return a group with the specified name
   */
  @FromString
  public static FunctionGroupName of(String name) {
    return new FunctionGroupName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the group
   */
  private FunctionGroupName(String name) {
    super(name, NAME_PATTERN, "Group name must only contain the characters A-Z, a-z, 0-9 and -");
  }

}
