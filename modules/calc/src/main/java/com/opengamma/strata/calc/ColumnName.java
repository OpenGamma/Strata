/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * The name of a column in the grid of calculation results.
 */
public final class ColumnName
    extends TypedString<ColumnName> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code ColumnName} by name.
   * <p>
   * Column names may contain any character, but must not be empty.
   *
   * @param name  the name of the column
   * @return a column with the specified name
   */
  @FromString
  public static ColumnName of(String name) {
    return new ColumnName(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the column
   */
  private ColumnName(String name) {
    super(name);
  }

}
