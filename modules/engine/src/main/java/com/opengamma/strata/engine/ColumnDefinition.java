/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.config.Measure;

/**
 * A column definition specifies the name of the column and the measure displayed in the column for each target.
 */
public interface ColumnDefinition {

  /**
   * Returns the column name
   *
   * @return the column name
   */
  public abstract String getName();

  /**
   * Returns the measure displayed in the column for the target
   *
   * @param target  a calculation target
   * @return the measure displayed in the column for the target
   */
  public abstract Measure getMeasure(CalculationTarget target);
}
