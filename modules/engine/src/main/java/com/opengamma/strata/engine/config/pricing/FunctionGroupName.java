/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config.pricing;

import java.util.regex.Pattern;

import com.opengamma.strata.collect.type.TypedString;

/**
 * The name of a {@link FunctionGroup}.
 */
public final class FunctionGroupName extends TypedString<FunctionGroupName> {

  /** Pattern for checking the measure name. It must only contains the characters A-Z, a-z, 0-9 and -. */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  /**
   * Returns a function group name.
   *
   * @return a function group name
   */
  public static FunctionGroupName of(String name) {
    return new FunctionGroupName(name);
  }

  /**
   * Creates an instance.
   *
   * @param name the name, not empty
   */
  protected FunctionGroupName(String name) {
    super(name);
    validateName(name);
  }

  /**
   * Checks the name matches {@link #NAME_PATTERN}.
   *
   * @param name  the name
   * @throws IllegalArgumentException if the name doesn't match the pattern
   */
  private static void validateName(String name) {
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("Function group names must only contains the characters A-Z, a-z, 0-9 and -");
    }
  }
}
