/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.IborAveragedRateObservation;
import com.opengamma.strata.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Rate observation implementation using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingRateObservationFn
    implements RateObservationFn<RateObservation> {

  /**
   * Default implementation.
   */
  public static final DispatchingRateObservationFn DEFAULT = new DispatchingRateObservationFn(
      ForwardIborRateObservationFn.DEFAULT,
      ForwardIborInterpolatedRateObservationFn.DEFAULT,
      ForwardIborAveragedRateObservationFn.DEFAULT,
      ForwardOvernightCompoundedRateObservationFn.DEFAULT,
      ApproxForwardOvernightAveragedRateObservationFn.DEFAULT);

  /**
   * Rate provider for {@link IborRateObservation}.
   */
  private final RateObservationFn<IborRateObservation> iborRateObservationFn;
  /**
   * Rate provider for {@link IborInterpolatedRateObservation}.
   */
  private final RateObservationFn<IborInterpolatedRateObservation> iborInterpolatedRateObservationFn;
  /**
   * Rate provider for {@link IborAveragedRateObservation}.
   */
  private final RateObservationFn<IborAveragedRateObservation> iborAveragedRateObservationFn;
  /**
   * Rate provider for {@link OvernightCompoundedRateObservation}.
   */
  private final RateObservationFn<OvernightCompoundedRateObservation> overnightCompoundedRateObservationFn;
  /**
   * Rate provider for {@link OvernightAveragedRateObservation}.
   */
  private final RateObservationFn<OvernightAveragedRateObservation> overnightAveragedRateObservationFn;

  /**
   * Creates an instance.
   *
   * @param iborRateObservationFn  the rate provider for {@link IborRateObservation}
   * @param iborInterpolatedRateObservationFn  the rate observation for {@link IborInterpolatedRateObservation}
   * @param iborAveragedRateObservationFn  the rate observation for {@link IborAveragedRateObservation}
   * @param overnightCompoundedRateObservationFn  the rate observation for {@link OvernightCompoundedRateObservation}
   * @param overnightAveragedRateObservationFn  the rate observation for {@link OvernightAveragedRateObservation}
   */
  public DispatchingRateObservationFn(
      RateObservationFn<IborRateObservation> iborRateObservationFn,
      RateObservationFn<IborInterpolatedRateObservation> iborInterpolatedRateObservationFn,
      RateObservationFn<IborAveragedRateObservation> iborAveragedRateObservationFn,
      RateObservationFn<OvernightCompoundedRateObservation> overnightCompoundedRateObservationFn,
      RateObservationFn<OvernightAveragedRateObservation> overnightAveragedRateObservationFn) {

    this.iborRateObservationFn =
        ArgChecker.notNull(iborRateObservationFn, "iborRateObservationFn");
    this.iborInterpolatedRateObservationFn =
        ArgChecker.notNull(iborInterpolatedRateObservationFn, "iborInterpolatedRateObservationFn");
    this.iborAveragedRateObservationFn =
        ArgChecker.notNull(iborAveragedRateObservationFn, "iborAverageRateObservationFn");
    this.overnightCompoundedRateObservationFn =
        ArgChecker.notNull(overnightCompoundedRateObservationFn, "overnightCompoundedRateObservationFn");
    this.overnightAveragedRateObservationFn =
        ArgChecker.notNull(overnightAveragedRateObservationFn, "overnightAveragedRateObservationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      RatesProvider provider,
      RateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {

    // dispatch by runtime type
    if (observation instanceof FixedRateObservation) {
      // inline code (performance) avoiding need for FixedRateObservationFn implementation
      return ((FixedRateObservation) observation).getRate();
    } else if (observation instanceof IborRateObservation) {
      return iborRateObservationFn.rate(
          provider, (IborRateObservation) observation, startDate, endDate);
    } else if (observation instanceof IborInterpolatedRateObservation) {
      return iborInterpolatedRateObservationFn.rate(
          provider, (IborInterpolatedRateObservation) observation, startDate, endDate);
    } else if (observation instanceof IborAveragedRateObservation) {
      return iborAveragedRateObservationFn.rate(
          provider, (IborAveragedRateObservation) observation, startDate, endDate);
    } else if (observation instanceof OvernightAveragedRateObservation) {
      return overnightAveragedRateObservationFn.rate(
          provider, (OvernightAveragedRateObservation) observation, startDate, endDate);
    } else if (observation instanceof OvernightCompoundedRateObservation) {
      return overnightCompoundedRateObservationFn.rate(
          provider, (OvernightCompoundedRateObservation) observation, startDate, endDate);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + observation.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      RatesProvider provider,
      RateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {

    // dispatch by runtime type
    if (observation instanceof FixedRateObservation) {
      // inline code (performance) avoiding need for FixedRateObservationFn implementation
      return PointSensitivityBuilder.none();
    } else if (observation instanceof IborRateObservation) {
      return iborRateObservationFn.rateSensitivity(
          provider, (IborRateObservation) observation, startDate, endDate);
    } else if (observation instanceof IborInterpolatedRateObservation) {
      return iborInterpolatedRateObservationFn.rateSensitivity(
          provider, (IborInterpolatedRateObservation) observation, startDate, endDate);
    } else if (observation instanceof IborAveragedRateObservation) {
      return iborAveragedRateObservationFn.rateSensitivity(
          provider, (IborAveragedRateObservation) observation, startDate, endDate);
    } else if (observation instanceof OvernightAveragedRateObservation) {
      return overnightAveragedRateObservationFn.rateSensitivity(
          provider, (OvernightAveragedRateObservation) observation, startDate, endDate);
    } else if (observation instanceof OvernightCompoundedRateObservation) {
      return overnightCompoundedRateObservationFn.rateSensitivity(
          provider, (OvernightCompoundedRateObservation) observation, startDate, endDate);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + observation.getClass().getSimpleName());
    }
  }

}
