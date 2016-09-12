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
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;
import com.opengamma.strata.product.rate.IborInterpolatedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Rate computation implementation using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingRateComputationFn
    implements RateComputationFn<RateComputation> {

  /**
   * Default implementation.
   */
  public static final DispatchingRateComputationFn DEFAULT = new DispatchingRateComputationFn(
      ForwardIborRateComputationFn.DEFAULT,
      ForwardIborInterpolatedRateComputationFn.DEFAULT,
      ForwardIborAveragedRateComputationFn.DEFAULT,
      ForwardOvernightCompoundedRateComputationFn.DEFAULT,
      ApproxForwardOvernightAveragedRateComputationFn.DEFAULT,
      ForwardInflationMonthlyRateComputationFn.DEFAULT,
      ForwardInflationInterpolatedRateComputationFn.DEFAULT,
      ForwardInflationEndMonthRateComputationFn.DEFAULT,
      ForwardInflationEndInterpolatedRateComputationFn.DEFAULT);

  /**
   * Rate provider for {@link IborRateComputation}.
   */
  private final RateComputationFn<IborRateComputation> iborRateComputationFn;
  /**
   * Rate provider for {@link IborInterpolatedRateComputation}.
   */
  private final RateComputationFn<IborInterpolatedRateComputation> iborInterpolatedRateComputationFn;
  /**
   * Rate provider for {@link IborAveragedRateComputation}.
   */
  private final RateComputationFn<IborAveragedRateComputation> iborAveragedRateComputationFn;
  /**
   * Rate provider for {@link OvernightCompoundedRateComputation}.
   */
  private final RateComputationFn<OvernightCompoundedRateComputation> overnightCompoundedRateComputationFn;
  /**
   * Rate provider for {@link OvernightAveragedRateComputation}.
   */
  private final RateComputationFn<OvernightAveragedRateComputation> overnightAveragedRateComputationFn;
  /**
   * Rate provider for {@link InflationMonthlyRateComputation}.
   */
  private final RateComputationFn<InflationMonthlyRateComputation> inflationMonthlyRateComputationFn;
  /**
   * Rate provider for {@link InflationInterpolatedRateComputation}.
   */
  private final RateComputationFn<InflationInterpolatedRateComputation> inflationInterpolatedRateComputationFn;
  /**
   * Rate provider for {@link InflationEndMonthRateComputation}.
   */
  private final RateComputationFn<InflationEndMonthRateComputation> inflationEndMonthRateComputationFn;
  /**
   * Rate provider for {@link InflationEndInterpolatedRateComputation}.
   */
  private final RateComputationFn<InflationEndInterpolatedRateComputation> inflationEndInterpolatedRateComputationFn;

  /**
   * Creates an instance.
   *
   * @param iborRateComputationFn  the rate provider for {@link IborRateComputation}
   * @param iborInterpolatedRateComputationFn  the rate computation for {@link IborInterpolatedRateComputation}
   * @param iborAveragedRateComputationFn  the rate computation for {@link IborAveragedRateComputation}
   * @param overnightCompoundedRateComputationFn  the rate computation for {@link OvernightCompoundedRateComputation}
   * @param overnightAveragedRateComputationFn  the rate computation for {@link OvernightAveragedRateComputation}
   * @param inflationMonthlyRateComputationFn  the rate computation for {@link InflationMonthlyRateComputation}
   * @param inflationInterpolatedRateComputationFn  the rate computation for {@link InflationInterpolatedRateComputation}
   * @param inflationEndMonthRateComputationFn  the rate computation for {@link InflationEndMonthRateComputation}
   * @param inflationEndInterpolatedRateComputationFn  the rate computation for {@link InflationEndInterpolatedRateComputation}
   */
  public DispatchingRateComputationFn(
      RateComputationFn<IborRateComputation> iborRateComputationFn,
      RateComputationFn<IborInterpolatedRateComputation> iborInterpolatedRateComputationFn,
      RateComputationFn<IborAveragedRateComputation> iborAveragedRateComputationFn,
      RateComputationFn<OvernightCompoundedRateComputation> overnightCompoundedRateComputationFn,
      RateComputationFn<OvernightAveragedRateComputation> overnightAveragedRateComputationFn,
      RateComputationFn<InflationMonthlyRateComputation> inflationMonthlyRateComputationFn,
      RateComputationFn<InflationInterpolatedRateComputation> inflationInterpolatedRateComputationFn,
      RateComputationFn<InflationEndMonthRateComputation> inflationEndMonthRateComputationFn,
      RateComputationFn<InflationEndInterpolatedRateComputation> inflationEndInterpolatedRateComputationFn) {

    this.iborRateComputationFn =
        ArgChecker.notNull(iborRateComputationFn, "iborRateComputationFn");
    this.iborInterpolatedRateComputationFn =
        ArgChecker.notNull(iborInterpolatedRateComputationFn, "iborInterpolatedRateComputationFn");
    this.iborAveragedRateComputationFn =
        ArgChecker.notNull(iborAveragedRateComputationFn, "iborAverageRateComputationFn");
    this.overnightCompoundedRateComputationFn =
        ArgChecker.notNull(overnightCompoundedRateComputationFn, "overnightCompoundedRateComputationFn");
    this.overnightAveragedRateComputationFn =
        ArgChecker.notNull(overnightAveragedRateComputationFn, "overnightAveragedRateComputationFn");
    this.inflationMonthlyRateComputationFn =
        ArgChecker.notNull(inflationMonthlyRateComputationFn, "inflationMonthlyRateComputationFn");
    this.inflationInterpolatedRateComputationFn =
        ArgChecker.notNull(inflationInterpolatedRateComputationFn, "inflationInterpolatedRateComputationFn");
    this.inflationEndMonthRateComputationFn =
        ArgChecker.notNull(inflationEndMonthRateComputationFn, "inflationEndMonthRateComputationFn");
    this.inflationEndInterpolatedRateComputationFn =
        ArgChecker.notNull(inflationEndInterpolatedRateComputationFn, "inflationEndInterpolatedRateComputationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      RateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // dispatch by runtime type
    if (computation instanceof FixedRateComputation) {
      // inline code (performance) avoiding need for FixedRateComputationFn implementation
      return ((FixedRateComputation) computation).getRate();
    } else if (computation instanceof IborRateComputation) {
      return iborRateComputationFn.rate(
          (IborRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof IborInterpolatedRateComputation) {
      return iborInterpolatedRateComputationFn.rate(
          (IborInterpolatedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof IborAveragedRateComputation) {
      return iborAveragedRateComputationFn.rate(
          (IborAveragedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof OvernightAveragedRateComputation) {
      return overnightAveragedRateComputationFn.rate(
          (OvernightAveragedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof OvernightCompoundedRateComputation) {
      return overnightCompoundedRateComputationFn.rate(
          (OvernightCompoundedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationMonthlyRateComputation) {
      return inflationMonthlyRateComputationFn.rate(
          (InflationMonthlyRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationInterpolatedRateComputation) {
      return inflationInterpolatedRateComputationFn.rate(
          (InflationInterpolatedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationEndMonthRateComputation) {
      return inflationEndMonthRateComputationFn.rate(
          (InflationEndMonthRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationEndInterpolatedRateComputation) {
      return inflationEndInterpolatedRateComputationFn.rate(
          (InflationEndInterpolatedRateComputation) computation, startDate, endDate, provider);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + computation.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      RateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // dispatch by runtime type
    if (computation instanceof FixedRateComputation) {
      // inline code (performance) avoiding need for FixedRateComputationFn implementation
      return PointSensitivityBuilder.none();
    } else if (computation instanceof IborRateComputation) {
      return iborRateComputationFn.rateSensitivity(
          (IborRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof IborInterpolatedRateComputation) {
      return iborInterpolatedRateComputationFn.rateSensitivity(
          (IborInterpolatedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof IborAveragedRateComputation) {
      return iborAveragedRateComputationFn.rateSensitivity(
          (IborAveragedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof OvernightAveragedRateComputation) {
      return overnightAveragedRateComputationFn.rateSensitivity(
          (OvernightAveragedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof OvernightCompoundedRateComputation) {
      return overnightCompoundedRateComputationFn.rateSensitivity(
          (OvernightCompoundedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationMonthlyRateComputation) {
      return inflationMonthlyRateComputationFn.rateSensitivity(
          (InflationMonthlyRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationInterpolatedRateComputation) {
      return inflationInterpolatedRateComputationFn.rateSensitivity(
          (InflationInterpolatedRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationEndMonthRateComputation) {
      return inflationEndMonthRateComputationFn.rateSensitivity(
          (InflationEndMonthRateComputation) computation, startDate, endDate, provider);
    } else if (computation instanceof InflationEndInterpolatedRateComputation) {
      return inflationEndInterpolatedRateComputationFn.rateSensitivity(
          (InflationEndInterpolatedRateComputation) computation, startDate, endDate, provider);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + computation.getClass().getSimpleName());
    }
  }

  @Override
  public double explainRate(
      RateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    // dispatch by runtime type
    if (computation instanceof FixedRateComputation) {
      // inline code (performance) avoiding need for FixedRateComputationFn implementation
      double rate = ((FixedRateComputation) computation).getRate();
      builder.put(ExplainKey.FIXED_RATE, rate);
      builder.put(ExplainKey.COMBINED_RATE, rate);
      return rate;
    } else if (computation instanceof IborRateComputation) {
      return iborRateComputationFn.explainRate(
          (IborRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof IborInterpolatedRateComputation) {
      return iborInterpolatedRateComputationFn.explainRate(
          (IborInterpolatedRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof IborAveragedRateComputation) {
      return iborAveragedRateComputationFn.explainRate(
          (IborAveragedRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof OvernightAveragedRateComputation) {
      return overnightAveragedRateComputationFn.explainRate(
          (OvernightAveragedRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof OvernightCompoundedRateComputation) {
      return overnightCompoundedRateComputationFn.explainRate(
          (OvernightCompoundedRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof InflationMonthlyRateComputation) {
      return inflationMonthlyRateComputationFn.explainRate(
          (InflationMonthlyRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof InflationInterpolatedRateComputation) {
      return inflationInterpolatedRateComputationFn.explainRate(
          (InflationInterpolatedRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof InflationEndMonthRateComputation) {
      return inflationEndMonthRateComputationFn.explainRate(
          (InflationEndMonthRateComputation) computation, startDate, endDate, provider, builder);
    } else if (computation instanceof InflationEndInterpolatedRateComputation) {
      return inflationEndInterpolatedRateComputationFn.explainRate(
          (InflationEndInterpolatedRateComputation) computation, startDate, endDate, provider, builder);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + computation.getClass().getSimpleName());
    }
  }

}
