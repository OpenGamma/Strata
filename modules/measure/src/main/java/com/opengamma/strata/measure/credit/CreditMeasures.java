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
 */
public final class CreditMeasures {

  /**
   * Measure representing the PV change under a 1 bps shift to market quotes.
   */
  public static final Measure IR01_MARKET_QUOTE_PARALLEL = Measure.of(StandardCreditMeasures.IR01_MARKET_QUOTE_PARALLEL.getName());
  /**
   * Measure representing the PV change under a series of 1 bps shifts in market quotes at each curve node.
   */
  public static final Measure IR01_MARKET_QUOTE_BUCKETED = Measure.of(StandardCreditMeasures.IR01_MARKET_QUOTE_BUCKETED.getName());
  /**
   * Measure representing the PV change under a 1 bps shift in calibrated curve.
   */
  public static final Measure IR01_CALIBRATED__PARALLEL = Measure.of(StandardCreditMeasures.IR01_CALIBRATED__PARALLEL.getName());
  /**
   * Measure representing the PV change under a series of 1 bps shifts in calibrated curve at each curve node.
   */
  public static final Measure IR01_CALIBRATED__BUCKETED = Measure.of(StandardCreditMeasures.IR01_CALIBRATED__BUCKETED.getName());

  //-------------------------------------------------------------------------
  /**
   * Measure representing the PV change under a 1 bps shift in credit spread.
   */
  public static final Measure CS01_PARALLEL = Measure.of(StandardCreditMeasures.CS01_PARALLEL.getName());
  /**
   * Measure representing the PV change under a series of 1 bps shifts in credit spread at each curve node.
   */
  public static final Measure CS01_BUCKETED = Measure.of(StandardCreditMeasures.CS01_BUCKETED.getName());

  //-------------------------------------------------------------------------
  /**
   * Measure representing the PV change under a 1 bps shift in recovery rate.
   */
  public static final Measure RECOVERY01 = Measure.of(StandardCreditMeasures.RECOVERY01.getName());
  /**
   * Measure representing the PV change in case of immediate default.
   */
  public static final Measure JUMP_TO_DEFAULT = Measure.of(StandardCreditMeasures.JUMP_TO_DEFAULT.getName());
  /**
   * Measure representing the expected value of protection settlement.
   */
  public static final Measure EXPECTED_LOSS = Measure.of(StandardCreditMeasures.EXPECTED_LOSS.getName());

  //-------------------------------------------------------------------------
  private CreditMeasures() {
  }

}
