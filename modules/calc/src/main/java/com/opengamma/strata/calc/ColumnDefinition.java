/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import com.opengamma.strata.basics.market.ScenarioValuesList;
import com.opengamma.strata.calc.config.Measure;

/**
 * A column definition specifies the name of the column and the measure displayed in the column for each target.
 */
public interface ColumnDefinition<T, U extends ScenarioValuesList<T>> {

  /**
   * Returns a definition of a column that contains the same measure in all rows and whose name is the measure name.
   *
   * @param measure  the measure to base the column on
   * @return a definition of a column that contains the same measure in all rows and whose name is the measure name
   */
  public static <T, U extends ScenarioValuesList<T>> ColumnDefinition<T, U> of(Measure<T, U> measure) {
    return SimpleColumnDefinition.builder()
        .measure(measure)
        .name(ColumnName.of(measure.toString()))
        .build();
  }

  /**
   * Returns a definition of a column with the specified name that contains the same measure in all rows.
   *
   * @param measure  the measure to base the column on
   * @param name  the name of the column
   * @return a definition of a column with the specified name that contains the same measure in all rows
   */
  public static <T, U extends ScenarioValuesList<T>> ColumnDefinition<T, U> of(Measure<T, U> measure, String name) {
    return SimpleColumnDefinition.builder()
        .measure(measure)
        .name(ColumnName.of(name))
        .build();
  }

  /**
   * Returns the column name
   *
   * @return the column name
   */
  public abstract ColumnName getName();

  /**
   * Returns the measure displayed in the column for the target.
   *
   * @return the measure displayed in the column for the target
   */
  public abstract Measure<T, U> getMeasure();
}
