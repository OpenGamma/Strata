/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;

/**
 * Provides standard formatters.
 * <p>
 * Each formatter implements {@link ValueFormatter}.
 */
public final class ValueFormatters {

  /**
   * The default formatter that returns the value of the {@code toString()} method.
   */
  public static ValueFormatter<Object> TO_STRING = ToStringValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code CurrencyAmount}.
   */
  public static ValueFormatter<CurrencyAmount> CURRENCY_AMOUNT = CurrencyAmountValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code CurveCurrencyParameterSensitivity}.
   */
  public static ValueFormatter<CurveCurrencyParameterSensitivity> CURVE_CURRENCY_PARAMETER_SENSITIVITY =
      CurveCurrencyParameterSensitivityValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code double[]}.
   */
  public static ValueFormatter<double[]> DOUBLE_ARRAY = DoubleArrayValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code double}.
   */
  public static ValueFormatter<Double> DOUBLE = DoubleValueFormatter.INSTANCE;
  /**
   * The formatter to be used when no specific formatter exists for the object.
   */
  public static ValueFormatter<Object> UNSUPPORTED = UnsupportedValueFormatter.INSTANCE;

  // restricted constructor
  private ValueFormatters() {

  }

}
