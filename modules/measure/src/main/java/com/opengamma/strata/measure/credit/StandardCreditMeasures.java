/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import com.opengamma.strata.calc.ImmutableMeasure;
import com.opengamma.strata.calc.Measure;

/**
 * The standard set of credit measures that can be calculated by Strata.
 */
final class StandardCreditMeasures {

  // principal
  public static final Measure PRINCIPAL = ImmutableMeasure.of("Principal");
  //-------------------------------------------------------------------------
  // PV change under a 1 bps shift in calibrated curve
  public static final Measure IR01_CALIBRATED_PARALLEL = ImmutableMeasure.of("IR01CalibratedParallel");
  // PV change under a series of 1 bps shifts in calibrated curve at each curve node
  public static final Measure IR01_CALIBRATED_BUCKETED = ImmutableMeasure.of("IR01CalibratedBucketed");
  // PV change under a 1 bps shift to market quotes
  public static final Measure IR01_MARKET_QUOTE_PARALLEL = ImmutableMeasure.of("IR01MarketQuoteParallel");
  // PV change under a series of 1 bps shifts in market quotes at each curve node
  public static final Measure IR01_MARKET_QUOTE_BUCKETED = ImmutableMeasure.of("IR01MarketQuoteBucketed");

  //-------------------------------------------------------------------------
  // PV change under a 1 bps shift in credit spread
  public static final Measure CS01_PARALLEL = ImmutableMeasure.of("CS01Parallel");
  // PV change under a series of 1 bps shifts in credit spread at each curve node
  public static final Measure CS01_BUCKETED = ImmutableMeasure.of("CS01Bucketed");

  //-------------------------------------------------------------------------
  // PV change under a 1 bps shift in recovery rate
  public static final Measure RECOVERY01 = ImmutableMeasure.of("Recovery01");
  // PV change in case of immediate default
  public static final Measure JUMP_TO_DEFAULT = ImmutableMeasure.of("JumpToDefault");
  // expected value of protection settlement
  public static final Measure EXPECTED_LOSS = ImmutableMeasure.of("ExpectedLoss");

}
