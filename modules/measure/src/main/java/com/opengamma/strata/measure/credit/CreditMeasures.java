/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import com.opengamma.strata.calc.Measure;

/**
 * The standard set of credit measures that can be calculated by Strata.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * NOTE: These measure names are subject to change.
 */
public final class CreditMeasures {

  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in par interest rates.
   */
  public static final Measure IR01_PARALLEL_PAR = Measure.of(StandardCreditMeasures.IR01_PARALLEL_PAR.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in par interest rates at each curve node.
   */
  public static final Measure IR01_BUCKETED_PAR = Measure.of(StandardCreditMeasures.IR01_BUCKETED_PAR.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in zero interest rates of calibrated curve.
   */
  public static final Measure IR01_PARALLEL_ZERO = Measure.of(StandardCreditMeasures.IR01_PARALLEL_ZERO.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in zero interest rates at each curve node.
   */
  public static final Measure IR01_BUCKETED_ZERO = Measure.of(StandardCreditMeasures.IR01_BUCKETED_ZERO.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in par credit spread rates.
   */
  public static final Measure CS01_PARALLEL_PAR = Measure.of(StandardCreditMeasures.CS01_PARALLEL_PAR.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in par credit rates at each curve node.
   */
  public static final Measure CS01_BUCKETED_PAR = Measure.of(StandardCreditMeasures.CS01_BUCKETED_PAR.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in hazard rates of calibrated curve.
   */
  public static final Measure CS01_PARALLEL_HAZARD = Measure.of(StandardCreditMeasures.CS01_PARALLEL_HAZARD.getName());
  /**
   * Measure representing the (vector) PV change to a series of 1 bps shifts in hazard rates at each curve node.
   */
  public static final Measure CS01_BUCKETED_HAZARD = Measure.of(StandardCreditMeasures.CS01_BUCKETED_HAZARD.getName());
  /**
   * Measure representing the (scalar) PV change to a 1 bps shift in recovery rate.
   */
  public static final Measure RECOVERY01 = Measure.of(StandardCreditMeasures.RECOVERY01.getName());
  /**
   * Measure representing the risk of default as opposed to the risk of change in credit spreads.
   */
  public static final Measure JUMP_TO_DEFAULT = Measure.of(StandardCreditMeasures.JUMP_TO_DEFAULT.getName());

  //-------------------------------------------------------------------------
  private CreditMeasures() {
  }

}
