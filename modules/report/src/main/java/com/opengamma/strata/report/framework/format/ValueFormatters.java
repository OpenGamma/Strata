/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;

/**
 * Provides standard formatters.
 * <p>
 * Each formatter implements {@link ValueFormatter}.
 */
public final class ValueFormatters {

  /**
   * The default formatter that returns the value of the {@code toString()} method.
   */
  public static final ValueFormatter<Object> TO_STRING = ToStringValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code AdjustableDate}, printing the unadjusted date.
   */
  public static final ValueFormatter<AdjustableDate> ADJUSTABLE_DATE = AdjustableDateValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code CurrencyAmount}.
   */
  public static final ValueFormatter<CurrencyAmount> CURRENCY_AMOUNT = CurrencyAmountValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code CurrencyParameterSensitivity}.
   */
  public static final ValueFormatter<CurrencyParameterSensitivity> CURRENCY_PARAMETER_SENSITIVITY =
      CurrencyParameterSensitivityValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code double[]}.
   */
  public static final ValueFormatter<double[]> DOUBLE_ARRAY = DoubleArrayValueFormatter.INSTANCE;
  /**
   * The formatter to be used for {@code double}.
   */
  public static final ValueFormatter<Double> DOUBLE = DoubleValueFormatter.INSTANCE;
  /**
   * The formatter to be used when no specific formatter exists for the object.
   */
  public static final ValueFormatter<Object> UNSUPPORTED = UnsupportedValueFormatter.INSTANCE;

  // restricted constructor
  private ValueFormatters() {

  }

}
