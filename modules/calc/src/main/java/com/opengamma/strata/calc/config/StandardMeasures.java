/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

/**
 * The standard set of measures which can be calculated by Strata.
 */
final class StandardMeasures {
  /**
   * Measure representing the accrued interest of the calculation target.
   */
  public static final Measure ACCRUED_INTEREST = ImmutableMeasure.of("AccruedInterest");
  /**
   * Measure representing the Bucketed Gamma PV01 of the calculation target.
   */
  public static final Measure BUCKETED_GAMMA_PV01 = ImmutableMeasure.of("BucketedGammaPV01");
  /**
   * Measure representing the cash flows of the calculation target.
   */
  public static final Measure CASH_FLOWS = ImmutableMeasure.of("CashFlows");
  /**
   * Measure representing the currency exposure of the calculation target.
   */
  public static final Measure CURRENCY_EXPOSURE = ImmutableMeasure.of("CurrencyExposure");
  /**
   * Measure representing the current cash of the calculation target.
   */
  public static final Measure CURRENT_CASH = ImmutableMeasure.of("CurrentCash");
  /**
   * Measure representing a break-down of the present value calculation on the target.
   */
  public static final Measure EXPLAIN_PRESENT_VALUE = ImmutableMeasure.of("ExplainPresentValue");
  /**
   * Measure representing the forward FX rate of the calculation target.
   */
  public static final Measure FORWARD_FX_RATE = ImmutableMeasure.of("ForwardFxRate");
  /**
   * Measure representing the initial notional amount of each leg of the calculation target.
   */
  public static final Measure LEG_INITIAL_NOTIONAL = ImmutableMeasure.of("LegInitialNotional");
  /**
   * Measure representing the par rate of the calculation target.
   */
  public static final Measure PAR_RATE = ImmutableMeasure.of("ParRate");
  /**
   * Measure representing the par spread of the calculation target.
   */
  public static final Measure PAR_SPREAD = ImmutableMeasure.of("ParSpread");
  /**
   * Measure representing the present value of the calculation target.
   */
  public static final Measure PRESENT_VALUE = ImmutableMeasure.of("PresentValue");
  /**
   * Measure representing the present value of the calculation target.
   * <p>
   * Calculated values are not converted to the reporting currency and may contain values in multiple currencies
   * if the target contains multiple currencies.
   */
  public static final Measure PRESENT_VALUE_MULTI_CURRENCY = ImmutableMeasure.of("PresentValueMultiCurrency", false);
  /**
   * Measure representing the present value of each leg of the calculation target.
   */
  public static final Measure LEG_PRESENT_VALUE = ImmutableMeasure.of("LegPresentValue");
  /**
   * Measure representing the PV01 of the calculation target.
   */
  public static final Measure PV01 = ImmutableMeasure.of("PV01");
  /**
   * Measure representing the Bucketed PV01 of the calculation target.
   */
  public static final Measure BUCKETED_PV01 = ImmutableMeasure.of("BucketedPV01");
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in par interest rates.
   */
  public static final Measure IR01_PARALLEL_PAR = ImmutableMeasure.of("IR01ParallelPar");
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in par interest rates at each curve node.
   */
  public static final Measure IR01_BUCKETED_PAR = ImmutableMeasure.of("IR01BucketedPar");
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in zero interest rates of calibrated curve.
   */
  public static final Measure IR01_PARALLEL_ZERO = ImmutableMeasure.of("IR01ParallelZero");
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in zero interest rates at each curve node.
   */
  public static final Measure IR01_BUCKETED_ZERO = ImmutableMeasure.of("IR01BucketedZero");
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in par credit spread rates.
   */
  public static final Measure CS01_PARALLEL_PAR = ImmutableMeasure.of("CS01ParallelPar");
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in par credit rates at each curve node.
   */
  public static final Measure CS01_BUCKETED_PAR = ImmutableMeasure.of("CS01BucketedPar");
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in hazard rates of calibrated curve.
   */
  public static final Measure CS01_PARALLEL_HAZARD = ImmutableMeasure.of("CS01ParallelHazard");
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in hazard rates at each curve node.
   */
  public static final Measure CS01_BUCKETED_HAZARD = ImmutableMeasure.of("CS01BucketedHazard");
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in recovery rate.
   */
  public static final Measure RECOVERY01 = ImmutableMeasure.of("Recovery01");
  /**
   * Measure representing the the risk of default as opposed to the the risk of change in credit spreads.
   */
  public static final Measure JUMP_TO_DEFAULT = ImmutableMeasure.of("JumpToDefault");
}
