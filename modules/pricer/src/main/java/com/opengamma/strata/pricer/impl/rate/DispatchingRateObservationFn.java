/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborAveragedRateObservation;
import com.opengamma.strata.product.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.product.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.product.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.product.rate.RateObservation;

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
      ApproxForwardOvernightAveragedRateObservationFn.DEFAULT,
      ForwardInflationMonthlyRateObservationFn.DEFAULT,
      ForwardInflationInterpolatedRateObservationFn.DEFAULT);

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
   * Rate provider for {@link InflationMonthlyRateObservation}.
   */
  private final RateObservationFn<InflationMonthlyRateObservation> inflationMonthlyRateObservationFn;
  /**
   * Rate provider for {@link InflationInterpolatedRateObservation}.
   */
  private final RateObservationFn<InflationInterpolatedRateObservation> inflationInterpolatedRateObservationFn;

  /**
   * Creates an instance.
   *
   * @param iborRateObservationFn  the rate provider for {@link IborRateObservation}
   * @param iborInterpolatedRateObservationFn  the rate observation for {@link IborInterpolatedRateObservation}
   * @param iborAveragedRateObservationFn  the rate observation for {@link IborAveragedRateObservation}
   * @param overnightCompoundedRateObservationFn  the rate observation for {@link OvernightCompoundedRateObservation}
   * @param overnightAveragedRateObservationFn  the rate observation for {@link OvernightAveragedRateObservation}
   * @param inflationMonthlyRateObservationFn  the rate observation for {@link InflationMonthlyRateObservation}
   * @param inflationInterpolatedRateObservationFn  the rate observation for {@link InflationInterpolatedRateObservation}
   */
  public DispatchingRateObservationFn(
      RateObservationFn<IborRateObservation> iborRateObservationFn,
      RateObservationFn<IborInterpolatedRateObservation> iborInterpolatedRateObservationFn,
      RateObservationFn<IborAveragedRateObservation> iborAveragedRateObservationFn,
      RateObservationFn<OvernightCompoundedRateObservation> overnightCompoundedRateObservationFn,
      RateObservationFn<OvernightAveragedRateObservation> overnightAveragedRateObservationFn,
      RateObservationFn<InflationMonthlyRateObservation> inflationMonthlyRateObservationFn,
      RateObservationFn<InflationInterpolatedRateObservation> inflationInterpolatedRateObservationFn) {

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
    this.inflationMonthlyRateObservationFn =
        ArgChecker.notNull(inflationMonthlyRateObservationFn, "inflationMonthlyRateObservationFn");
    this.inflationInterpolatedRateObservationFn =
        ArgChecker.notNull(inflationInterpolatedRateObservationFn, "inflationInterpolatedRateObservationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      RateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // dispatch by runtime type
    if (observation instanceof FixedRateObservation) {
      // inline code (performance) avoiding need for FixedRateObservationFn implementation
      return ((FixedRateObservation) observation).getRate();
    } else if (observation instanceof IborRateObservation) {
      return iborRateObservationFn.rate(
          (IborRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof IborInterpolatedRateObservation) {
      return iborInterpolatedRateObservationFn.rate(
          (IborInterpolatedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof IborAveragedRateObservation) {
      return iborAveragedRateObservationFn.rate(
          (IborAveragedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof OvernightAveragedRateObservation) {
      return overnightAveragedRateObservationFn.rate(
          (OvernightAveragedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof OvernightCompoundedRateObservation) {
      return overnightCompoundedRateObservationFn.rate(
          (OvernightCompoundedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof InflationMonthlyRateObservation) {
      return inflationMonthlyRateObservationFn.rate(
          (InflationMonthlyRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof InflationInterpolatedRateObservation) {
      return inflationInterpolatedRateObservationFn.rate(
          (InflationInterpolatedRateObservation) observation, startDate, endDate, provider);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + observation.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      RateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // dispatch by runtime type
    if (observation instanceof FixedRateObservation) {
      // inline code (performance) avoiding need for FixedRateObservationFn implementation
      return PointSensitivityBuilder.none();
    } else if (observation instanceof IborRateObservation) {
      return iborRateObservationFn.rateSensitivity(
          (IborRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof IborInterpolatedRateObservation) {
      return iborInterpolatedRateObservationFn.rateSensitivity(
          (IborInterpolatedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof IborAveragedRateObservation) {
      return iborAveragedRateObservationFn.rateSensitivity(
          (IborAveragedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof OvernightAveragedRateObservation) {
      return overnightAveragedRateObservationFn.rateSensitivity(
          (OvernightAveragedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof OvernightCompoundedRateObservation) {
      return overnightCompoundedRateObservationFn.rateSensitivity(
          (OvernightCompoundedRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof InflationMonthlyRateObservation) {
      return inflationMonthlyRateObservationFn.rateSensitivity(
          (InflationMonthlyRateObservation) observation, startDate, endDate, provider);
    } else if (observation instanceof InflationInterpolatedRateObservation) {
      return inflationInterpolatedRateObservationFn.rateSensitivity(
          (InflationInterpolatedRateObservation) observation, startDate, endDate, provider);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + observation.getClass().getSimpleName());
    }
  }

  @Override
  public double explainRate(
      RateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    // dispatch by runtime type
    if (observation instanceof FixedRateObservation) {
      // inline code (performance) avoiding need for FixedRateObservationFn implementation
      double rate = ((FixedRateObservation) observation).getRate();
      builder.put(ExplainKey.FIXED_RATE, rate);
      builder.put(ExplainKey.COMBINED_RATE, rate);
      return rate;
    } else if (observation instanceof IborRateObservation) {
      return iborRateObservationFn.explainRate(
          (IborRateObservation) observation, startDate, endDate, provider, builder);
    } else if (observation instanceof IborInterpolatedRateObservation) {
      return iborInterpolatedRateObservationFn.explainRate(
          (IborInterpolatedRateObservation) observation, startDate, endDate, provider, builder);
    } else if (observation instanceof IborAveragedRateObservation) {
      return iborAveragedRateObservationFn.explainRate(
          (IborAveragedRateObservation) observation, startDate, endDate, provider, builder);
    } else if (observation instanceof OvernightAveragedRateObservation) {
      return overnightAveragedRateObservationFn.explainRate(
          (OvernightAveragedRateObservation) observation, startDate, endDate, provider, builder);
    } else if (observation instanceof OvernightCompoundedRateObservation) {
      return overnightCompoundedRateObservationFn.explainRate(
          (OvernightCompoundedRateObservation) observation, startDate, endDate, provider, builder);
    } else if (observation instanceof InflationMonthlyRateObservation) {
      return inflationMonthlyRateObservationFn.explainRate(
          (InflationMonthlyRateObservation) observation, startDate, endDate, provider, builder);
    } else if (observation instanceof InflationInterpolatedRateObservation) {
      return inflationInterpolatedRateObservationFn.explainRate(
          (InflationInterpolatedRateObservation) observation, startDate, endDate, provider, builder);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + observation.getClass().getSimpleName());
    }
  }

}
