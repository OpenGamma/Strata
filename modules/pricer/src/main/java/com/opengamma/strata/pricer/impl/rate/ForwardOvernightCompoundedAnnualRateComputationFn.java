/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.util.OptionalDouble;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.OvernightCompoundedAnnualRateComputation;

/**
 * Rate computation implementation for a rate based on a single overnight index that is compounded using an annual rate.
 * <p>
 * Rates that are already fixed are retrieved from the time series of the {@link RatesProvider}.
 * Rates that are in the future are computed as a unique forward rate
 * in the full future period.
 */
public class ForwardOvernightCompoundedAnnualRateComputationFn
    implements RateComputationFn<OvernightCompoundedAnnualRateComputation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightCompoundedAnnualRateComputationFn DEFAULT =
      new ForwardOvernightCompoundedAnnualRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightCompoundedAnnualRateComputationFn() {
  }

  @Override
  public double rate(
      OvernightCompoundedAnnualRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndexRates rates = provider.overnightIndexRates(computation.getIndex());
    ForwardOvernightCompoundedAnnualRateComputationFn.ObservationDetails details =
        new ForwardOvernightCompoundedAnnualRateComputationFn.ObservationDetails(computation, rates);
    return details.calculateRate();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightCompoundedAnnualRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndexRates rates = provider.overnightIndexRates(computation.getIndex());
    ForwardOvernightCompoundedAnnualRateComputationFn.ObservationDetails details =
        new ForwardOvernightCompoundedAnnualRateComputationFn.ObservationDetails(computation, rates);
    return details.calculateRateSensitivity();
  }

  @Override
  public double explainRate(
      OvernightCompoundedAnnualRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

  //-------------------------------------------------------------------------
  // Internal class. Observation details stored in a separate class to clarify the construction.
  private static final class ObservationDetails {

    private final OvernightCompoundedAnnualRateComputation computation;
    private final OvernightIndexRates rates;
    private final LocalDateDoubleTimeSeries indexFixingDateSeries;
    private final DayCount dayCount;
    private final LocalDate firstFixing; // The date of the first fixing
    private final LocalDate lastFixingP1; // The date after the last fixing
    private final LocalDate lastFixing; // The date of the last fixing
    private LocalDate nextFixing; // Running variable through the different methods: next fixing date to be analyzed
    private final double accrualFactorTotal;

    private ObservationDetails(OvernightCompoundedAnnualRateComputation computation, OvernightIndexRates rates) {
      this.computation = computation;
      this.rates = rates;
      this.indexFixingDateSeries = rates.getFixings();
      this.dayCount = computation.getIndex().getDayCount();
      this.firstFixing = computation.getStartDate();
      this.lastFixingP1 = computation.getEndDate();
      this.lastFixing = computation.getFixingCalendar().previous(lastFixingP1);
      LocalDate startUnderlyingPeriod = computation.calculateEffectiveFromFixing(firstFixing);
      LocalDate endUnderlyingPeriod = computation.calculateMaturityFromFixing(lastFixing);
      this.accrualFactorTotal = dayCount.yearFraction(startUnderlyingPeriod, endUnderlyingPeriod);
    }

    // Composition - publication strictly before valuation date: try accessing fixing time-series
    private double pastCompositionFactor() {
      double compositionFactor = 1.0d;
      LocalDate currentFixing = firstFixing;
      LocalDate currentPublication = computation.calculatePublicationFromFixing(currentFixing);
      while (!(currentFixing.isAfter(lastFixing)) && rates.getValuationDate().isAfter(currentPublication)) {
        LocalDate effectiveDate = computation.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = computation.calculateMaturityFromEffective(effectiveDate);
        double accrualFactor = dayCount.yearFraction(effectiveDate, maturityDate);
        double rate = checkedFixing(currentFixing, indexFixingDateSeries, computation.getIndex());
        compositionFactor *= Math.pow(1.0d + rate, accrualFactor);
        currentFixing = computation.getFixingCalendar().next(currentFixing);
        currentPublication = computation.calculatePublicationFromFixing(currentFixing);
      }
      nextFixing = currentFixing;
      return compositionFactor;
    }

    // Composition - publication on valuation date: Check if a fixing is available on current date
    private double valuationCompositionFactor() {
      LocalDate currentFixing = nextFixing;
      LocalDate currentPublication = computation.calculatePublicationFromFixing(currentFixing);
      if (rates.getValuationDate().equals(currentPublication) && !(currentFixing.isAfter(lastFixing))) {
        OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixing);
        if (fixedRate.isPresent()) {
          nextFixing = computation.getFixingCalendar().next(nextFixing);
          LocalDate effectiveDate = computation.calculateEffectiveFromFixing(currentFixing);
          LocalDate maturityDate = computation.calculateMaturityFromEffective(effectiveDate);
          double accrualFactor = dayCount.yearFraction(effectiveDate, maturityDate);
          return Math.pow(1.0d + fixedRate.getAsDouble(), accrualFactor);
        }
      }
      return 1.0d;
    }

    // Composition - forward part in future period; past/valuation date case dealt with in previous methods
    private double futureCompositionFactor() {
      if (!nextFixing.isAfter(lastFixing)) {
        OvernightIndexObservation obs = computation.observeOn(nextFixing);
        LocalDate startDate = obs.getEffectiveDate();
        LocalDate endDate = computation.getEndDate();
        double accrualFactor = dayCount.yearFraction(startDate, endDate);
        double rate = rates.periodRate(obs, endDate);
        return 1.0d + accrualFactor * rate;
      }
      return 1.0d;
    }

    // Check that the fixing is present. Throws an exception if not and return the rate as double.
    private static double checkedFixing(
        LocalDate currentFixingTs,
        LocalDateDoubleTimeSeries indexFixingDateSeries,
        OvernightIndex index) {

      OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixingTs);
      return fixedRate.orElseThrow(() -> new PricingException(
          "Could not get fixing value of index " + index.getName() + " for date " + currentFixingTs));
    }

    // Calculate the total rate
    private double calculateRate() {
      return (pastCompositionFactor() * valuationCompositionFactor() * futureCompositionFactor() - 1.0d) /
          accrualFactorTotal;
    }

    // Calculate the total rate sensitivity
    private PointSensitivityBuilder calculateRateSensitivity() {
      double factor = pastCompositionFactor() * valuationCompositionFactor() / accrualFactorTotal;
      if (!nextFixing.isAfter(lastFixing)) {
        OvernightIndexObservation obs = computation.observeOn(nextFixing);
        LocalDate startDate = obs.getEffectiveDate();
        LocalDate endDate = computation.calculateMaturityFromFixing(lastFixing);
        double accrualFactor = dayCount.yearFraction(startDate, endDate);
        PointSensitivityBuilder rateSensitivity = rates.periodRatePointSensitivity(obs, endDate);
        return rateSensitivity.multipliedBy(factor * accrualFactor);
      }
      return PointSensitivityBuilder.none();
    }
  }

}
