/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.calculation.function.CalculationSingleFunction;

/**
 * Abstract function calculating a result for each of a set of scenarios.
 * 
 * @param <T>  the type of target handled by this function
 * @param <R>  the return type of this function
 */
public abstract class AbstractCalculationFunction<T extends CalculationTarget, R>
    implements CalculationSingleFunction<T, R> {

  /**
   * One basis point, expressed as a {@code double}.
   */
  public static final double ONE_BASIS_POINT = 1e-4;

  /**
   * If this is true the value returned by the {@code execute} method will support automatic currency
   * conversion if the underlying results support it.
   */
  private final boolean convertCurrencies;

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractCalculationFunction() {
    this(true);
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractCalculationFunction(boolean convertCurrencies) {
    this.convertCurrencies = convertCurrencies;
  }

  /**
   * Gets whether currencies in the result should be automatically converted.
   * 
   * @return the convert currencies flag
   */
  protected boolean isConvertCurrencies() {
    return convertCurrencies;
  }

}
