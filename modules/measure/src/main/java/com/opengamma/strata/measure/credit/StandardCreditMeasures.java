/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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

  // scalar PV change to a 1 bps shift in par interest rates
  public static final Measure IR01_PARALLEL_PAR = ImmutableMeasure.of("IR01ParallelPar");
  // vector PV change to a series of 1 bps shifts in par interest rates at each curve node
  public static final Measure IR01_BUCKETED_PAR = ImmutableMeasure.of("IR01BucketedPar");
  // scalar PV change to a 1 bps shift in zero interest rates of calibrated curve
  public static final Measure IR01_PARALLEL_ZERO = ImmutableMeasure.of("IR01ParallelZero");
  // vector PV change to a series of 1 bps shifts in zero interest rates at each curve node
  public static final Measure IR01_BUCKETED_ZERO = ImmutableMeasure.of("IR01BucketedZero");
  // scalar PV change to a 1 bps shift in par credit spread rates
  public static final Measure CS01_PARALLEL_PAR = ImmutableMeasure.of("CS01ParallelPar");
  // vector PV change to a series of 1 bps shifts in par credit rates at each curve node
  public static final Measure CS01_BUCKETED_PAR = ImmutableMeasure.of("CS01BucketedPar");
  // scalar PV change to a 1 bps shift in hazard rates of calibrated curve
  public static final Measure CS01_PARALLEL_HAZARD = ImmutableMeasure.of("CS01ParallelHazard");
  // vector PV change to a series of 1 bps shifts in hazard rates at each curve node
  public static final Measure CS01_BUCKETED_HAZARD = ImmutableMeasure.of("CS01BucketedHazard");
  // scalar PV change to a 1 bps shift in recovery rate
  public static final Measure RECOVERY01 = ImmutableMeasure.of("Recovery01");
  // risk of default as opposed to the risk of change in credit spreads
  public static final Measure JUMP_TO_DEFAULT = ImmutableMeasure.of("JumpToDefault");

}
