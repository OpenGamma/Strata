/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Identifies a measure that can be produced by the system.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * Some measures represent aspects of the calculation target rather than a calculation.
 * For example, the target identifier, counterparty and trade date.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public final class Measure
    extends TypedString<Measure> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Pattern for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  //-------------------------------------------------------------------------
  /** Measure representing the accrued interest of the calculation target. */
  public static final Measure ACCRUED_INTEREST = Measure.of("AccruedInterest");
  
  /** Measure representing the Bucketed Gamma PV01 of the calculation target. */
  public static final Measure BUCKETED_GAMMA_PV01 = Measure.of("BucketedGammaPV01");
  
  /** Measure representing a break-down of the present value calculation on the target. */
  public static final Measure EXPLAIN_PRESENT_VALUE = Measure.of("ExplainPresentValue");

  /** Measure representing the initial notional amount of each leg of the calculation target. */
  public static final Measure LEG_INITIAL_NOTIONAL = Measure.of("LegInitialNotional");
  
  /** Measure representing the par rate of the calculation target. */
  public static final Measure PAR_RATE = Measure.of("ParRate");
  
  /** Measure representing the par spread of the calculation target. */
  public static final Measure PAR_SPREAD = Measure.of("ParSpread");

  /** Measure representing the present value of the calculation target. */
  public static final Measure PRESENT_VALUE = Measure.of("PresentValue");

  /** Measure representing the present value of each leg of the calculation target. */
  public static final Measure LEG_PRESENT_VALUE = Measure.of("LegPresentValue");

  /** Measure representing the PV01 of the calculation target. */
  public static final Measure PV01 = Measure.of("PV01");
  
  /** Measure representing the Bucketed PV01 of the calculation target. */
  public static final Measure BUCKETED_PV01 = Measure.of("BucketedPV01");

  /** Measure representing the (scalar) PV change to a 1 bps shift in par interest rates */
  public static final Measure IR01_PARALLEL_PAR = Measure.of("Ir01ParallelPar");

  /** Measure representing the (vector) PV change to series of 1 bps shift in par interest rates at each curve point */
  public static final Measure IR01_BUCKETED_PAR = Measure.of("Ir01BucketedPar");

  /** Measure representing the (scalar) PV change to a 1 bps shift in zero interest rates of calibrated curve */
  public static final Measure IR01_PARALLEL_ZERO = Measure.of("Ir01ParallelZero");

  /** Measure representing the (vector) PV change to series of 1 bps shift in zero interest rates at each curve point */
  public static final Measure IR01_BUCKETED_ZERO = Measure.of("Ir01BucketedZero");

  /** Measure representing the (scalar) PV change to a 1 bps shift in par credit spread rates */
  public static final Measure CS01_PARALLEL_PAR = Measure.of("Cs01ParallelPar");

  /** Measure representing the (vector) PV change to a series of 1 bps shift in par credit rates at each curve point */
  public static final Measure CS01_BUCKETED_PAR = Measure.of("Cs01BucketedPar");

  /** Measure representing the (scalar) PV change to a 1 bps shift in hazard rates of calibrated curve */
  public static final Measure CS01_PARALLEL_HAZARD = Measure.of("Cs01ParallelHazard");

  /** Measure representing the (vector) PV change to a series of 1 bps shift in hazard rates at each curve point */
  public static final Measure CS01_BUCKETED_HAZARD = Measure.of("Cs01BucketedHazard");

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code Measure} by name.
   * <p>
   * Measure names must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the measure
   * @return a measure with the specified name
   */
  @FromString
  public static Measure of(String name) {
    return new Measure(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the measure
   */
  private Measure(String name) {
    super(name, NAME_PATTERN, "Measure name must only contain the characters A-Z, a-z, 0-9 and -");
  }

}
