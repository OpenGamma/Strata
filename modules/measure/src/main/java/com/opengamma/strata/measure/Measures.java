/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.calc.Measure;

/**
 * The standard set of measures that can be calculated by Strata.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public final class Measures {

  /**
   * Measure representing the present value of the calculation target.
   * <p>
   * The result is a single currency monetary amount in the reporting currency.
   */
  public static final Measure PRESENT_VALUE = Measure.of(StandardMeasures.PRESENT_VALUE.getName());
  /**
   * Measure representing a break-down of the present value calculation on the target.
   * <p>
   * No currency conversion is performed on the monetary amounts.
   */
  public static final Measure EXPLAIN_PRESENT_VALUE = Measure.of(StandardMeasures.EXPLAIN_PRESENT_VALUE.getName());

  //-------------------------------------------------------------------------
  /**
   * Measure representing the calibrated sum PV01 on the calculation target.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated data structure.
   * The result is the sum of the sensitivities of all affected curves.
   * It is expressed in the reporting currency.
   */
  public static final Measure PV01_CALIBRATED_SUM = Measure.of(StandardMeasures.PV01_CALIBRATED_SUM.getName());
  /**
   * Measure representing the calibrated bucketed PV01 on the calculation target.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated data structure.
   * The result is provided for each affected curve and currency, bucketed by parameter.
   * It is expressed in the reporting currency.
   */
  public static final Measure PV01_CALIBRATED_BUCKETED = Measure.of(StandardMeasures.PV01_CALIBRATED_BUCKETED.getName());
  /**
   * Measure representing the market quote sum PV01 on the calculation target.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the
   * market quotes used to calibrate the data structure.
   * The result is the sum of the sensitivities of all affected curves.
   * It is expressed in the reporting currency.
   */
  public static final Measure PV01_MARKET_QUOTE_SUM = Measure.of(StandardMeasures.PV01_MARKET_QUOTE_SUM.getName());
  /**
   * Measure representing the market quote bucketed PV01 on the calculation target.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the
   * market quotes used to calibrate the data structure.
   * The result is provided for each affected curve and currency, bucketed by parameter.
   * It is expressed in the reporting currency.
   */
  public static final Measure PV01_MARKET_QUOTE_BUCKETED = Measure.of(StandardMeasures.PV01_MARKET_QUOTE_BUCKETED.getName());

  //-------------------------------------------------------------------------
  /**
   * Measure representing the par rate of the calculation target.
   */
  public static final Measure PAR_RATE = Measure.of(StandardMeasures.PAR_RATE.getName());
  /**
   * Measure representing the par spread of the calculation target.
   */
  public static final Measure PAR_SPREAD = Measure.of(StandardMeasures.PAR_SPREAD.getName());

  //-------------------------------------------------------------------------
  /**
   * Measure representing the present value of each leg of the calculation target.
   * <p>
   * The result is expressed in the reporting currency.
   */
  public static final Measure LEG_PRESENT_VALUE = Measure.of(StandardMeasures.LEG_PRESENT_VALUE.getName());
  /**
   * Measure representing the initial notional amount of each leg of the calculation target.
   * <p>
   * The result is expressed in the reporting currency.
   */
  public static final Measure LEG_INITIAL_NOTIONAL = Measure.of(StandardMeasures.LEG_INITIAL_NOTIONAL.getName());
  /**
   * Measure representing the accrued interest of the calculation target.
   */
  public static final Measure ACCRUED_INTEREST = Measure.of(StandardMeasures.ACCRUED_INTEREST.getName());
  /**
   * Measure representing the cash flows of the calculation target.
   * <p>
   * Cash flows provide details about the payments of the target.
   * The result is expressed in the reporting currency.
   */
  public static final Measure CASH_FLOWS = Measure.of(StandardMeasures.CASH_FLOWS.getName());
  /**
   * Measure representing the currency exposure of the calculation target.
   * <p>
   * Currency exposure is the currency risk, expressed as the equivalent amount in each currency.
   * Calculated values are not converted to the reporting currency and may contain values in multiple currencies
   * if the target contains multiple currencies.
   */
  public static final Measure CURRENCY_EXPOSURE = Measure.of(StandardMeasures.CURRENCY_EXPOSURE.getName());
  /**
   * Measure representing the current cash of the calculation target.
   * <p>
   * Current cash is the sum of all cash flows paid on the valuation date.
   * The result is expressed in the reporting currency.
   */
  public static final Measure CURRENT_CASH = Measure.of(StandardMeasures.CURRENT_CASH.getName());
  /**
   * Measure representing the forward FX rate of the calculation target.
   */
  public static final Measure FORWARD_FX_RATE = Measure.of(StandardMeasures.FORWARD_FX_RATE.getName());
  /**
   * Measure representing the unit price of the instrument.
   * <p>
   * This is the price of a single unit of a security using Strata market conventions.
   * The price is represented as a {@code double}, even if it is actually a currency amount.
   */
  public static final Measure UNIT_PRICE = Measure.of(StandardMeasures.UNIT_PRICE.getName());
  /**
   * Measure representing the resolved form of the calculation target.
   * <p>
   * Many calculation targets have a {@linkplain Resolvable resolved} form that is optimized for pricing.
   * This measure allows the resolved form to be obtained.
   */
  public static final Measure RESOLVED_TARGET = Measure.of(StandardMeasures.RESOLVED_TARGET.getName());

  //-------------------------------------------------------------------------
  private Measures() {
  }

}
