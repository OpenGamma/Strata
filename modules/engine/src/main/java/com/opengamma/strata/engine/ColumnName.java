/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import com.opengamma.strata.collect.type.TypedString;

/**
 * The name of a column in the grid of calculation results.
 */
public final class ColumnName extends TypedString<ColumnName> {

  /**
   * Returns a column name.
   *
   * @return a column name
   */
  public static ColumnName of(String name) {
    return new ColumnName(name);
  }

  /**
   * Creates an instance.
   *
   * @param name the name, not empty
   */
  protected ColumnName(String name) {
    super(name);
  }
}
