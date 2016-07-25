/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The name of a column in the grid of calculation results.
 */
public final class ColumnName
    extends TypedString<ColumnName> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
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
   * Obtains an instance from the specified measure.
   * <p>
   * The column name will be the same as the name of the measure.
   *
   * @param measure  the measure to extract the name from
   * @return a column with the same name as the measure
   */
  public static ColumnName of(Measure measure) {
    return new ColumnName(measure.getName());
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
