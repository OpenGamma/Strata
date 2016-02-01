/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * The standard set of measures which can be calculated by Strata.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public final class Measures {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<Measure> ENUM_LOOKUP = ExtendedEnum.of(Measure.class);

  //--------------------------------------------------------------------------------------------------

  /**
   * Measure representing the accrued interest of the calculation target.
   */
  public static final Measure ACCRUED_INTEREST = Measure.of(StandardMeasures.ACCRUED_INTEREST.getName());
  /**
   * Measure representing the Bucketed Gamma PV01 of the calculation target.
   */
  public static final Measure BUCKETED_GAMMA_PV01 = Measure.of(StandardMeasures.BUCKETED_GAMMA_PV01.getName());
  /**
   * Measure representing the cash flows of the calculation target.
   */
  public static final Measure CASH_FLOWS = Measure.of(StandardMeasures.CASH_FLOWS.getName());
  /**
   * Measure representing the currency exposure of the calculation target.
   */
  public static final Measure CURRENCY_EXPOSURE = Measure.of(StandardMeasures.CURRENCY_EXPOSURE.getName());
  /**
   * Measure representing the current cash of the calculation target.
   */
  public static final Measure CURRENT_CASH = Measure.of(StandardMeasures.CURRENT_CASH.getName());
  /**
   * Measure representing a break-down of the present value calculation on the target.
   */
  public static final Measure EXPLAIN_PRESENT_VALUE = Measure.of(StandardMeasures.EXPLAIN_PRESENT_VALUE.getName());
  /**
   * Measure representing the forward FX rate of the calculation target.
   */
  public static final Measure FORWARD_FX_RATE = Measure.of(StandardMeasures.FORWARD_FX_RATE.getName());
  /**
   * Measure representing the initial notional amount of each leg of the calculation target.
   */
  public static final Measure LEG_INITIAL_NOTIONAL = Measure.of(StandardMeasures.LEG_INITIAL_NOTIONAL.getName());
  /**
   * Measure representing the par rate of the calculation target.
   */
  public static final Measure PAR_RATE = Measure.of(StandardMeasures.PAR_RATE.getName());
  /**
   * Measure representing the par spread of the calculation target.
   */
  public static final Measure PAR_SPREAD = Measure.of(StandardMeasures.PAR_SPREAD.getName());
  /**
   * Measure representing the present value of the calculation target.
   */
  public static final Measure PRESENT_VALUE = Measure.of(StandardMeasures.PRESENT_VALUE.getName());
  /**
   * Measure representing the present value of the calculation target.
   * <p>
   * Calculated values are not converted to the reporting currency and may contain values in multiple currencies
   * if the target contains multiple currencies.
   */
  public static final Measure PRESENT_VALUE_MULTI_CCY = Measure.of(StandardMeasures.PRESENT_VALUE_MULTI_CURRENCY.getName());
  /**
   * Measure representing the present value of each leg of the calculation target.
   */
  public static final Measure LEG_PRESENT_VALUE = Measure.of(StandardMeasures.LEG_PRESENT_VALUE.getName());
  /**
   * Measure representing the PV01 of the calculation target.
   */
  public static final Measure PV01 = Measure.of(StandardMeasures.PV01.getName());
  /**
   * Measure representing the Bucketed PV01 of the calculation target.
   */
  public static final Measure BUCKETED_PV01 = Measure.of(StandardMeasures.BUCKETED_PV01.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in par interest rates.
   */
  public static final Measure IR01_PARALLEL_PAR = Measure.of(StandardMeasures.IR01_PARALLEL_PAR.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in par interest rates at each curve node.
   */
  public static final Measure IR01_BUCKETED_PAR = Measure.of(StandardMeasures.IR01_BUCKETED_PAR.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in zero interest rates of calibrated curve.
   */
  public static final Measure IR01_PARALLEL_ZERO = Measure.of(StandardMeasures.IR01_PARALLEL_ZERO.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in zero interest rates at each curve node.
   */
  public static final Measure IR01_BUCKETED_ZERO = Measure.of(StandardMeasures.IR01_BUCKETED_ZERO.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in par credit spread rates.
   */
  public static final Measure CS01_PARALLEL_PAR = Measure.of(StandardMeasures.CS01_PARALLEL_PAR.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in par credit rates at each curve node.
   */
  public static final Measure CS01_BUCKETED_PAR = Measure.of(StandardMeasures.CS01_BUCKETED_PAR.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in hazard rates of calibrated curve.
   */
  public static final Measure CS01_PARALLEL_HAZARD = Measure.of(StandardMeasures.CS01_PARALLEL_HAZARD.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in hazard rates at each curve node.
   */
  public static final Measure CS01_BUCKETED_HAZARD = Measure.of(StandardMeasures.CS01_BUCKETED_HAZARD.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in recovery rate.
   */
  public static final Measure RECOVERY01 = Measure.of(StandardMeasures.RECOVERY01.getName());
  /**
   * Measure representing the the risk of default as opposed to the the risk of change in credit spreads.
   */
  public static final Measure JUMP_TO_DEFAULT = Measure.of(StandardMeasures.JUMP_TO_DEFAULT.getName());

  //-------------------------------------------------------------------------

  private Measures() {
  }
}
