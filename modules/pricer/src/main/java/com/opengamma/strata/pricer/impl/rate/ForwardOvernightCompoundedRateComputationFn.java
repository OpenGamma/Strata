/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.collect.tuple.ObjDoublePair;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

/**
* Rate computation implementation for a rate based on a single overnight index that is compounded.
* <p>
* Rates that are already fixed are retrieved from the time series of the {@link RatesProvider}.
* Rates that are in the future and not in the cut-off period are computed as unique forward rate in the full future period.
* Rates that are in the cut-off period (already fixed or forward) are compounded.
*/
public class ForwardOvernightCompoundedRateComputationFn
    implements RateComputationFn<OvernightCompoundedRateComputation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightCompoundedRateComputationFn DEFAULT =
      new ForwardOvernightCompoundedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightCompoundedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      OvernightCompoundedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndexRates rates = provider.overnightIndexRates(computation.getIndex());
    ObservationDetails details = new ObservationDetails(computation, rates);
    return details.calculateRate();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightCompoundedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndexRates rates = provider.overnightIndexRates(computation.getIndex());
    ObservationDetails details = new ObservationDetails(computation, rates);
    return details.calculateRateSensitivity();
  }

  @Override
  public double explainRate(
      OvernightCompoundedRateComputation computation,
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

    private final OvernightCompoundedRateComputation computation;
    private final OvernightIndexRates rates;
    private final LocalDateDoubleTimeSeries indexFixingDateSeries;
    private final DayCount dayCount;
    private final int cutoffOffset;
    private final LocalDate firstFixing; // The date of the first fixing
    private final LocalDate lastFixingP1; // The date after the last fixing
    private final LocalDate lastFixing; // The date of the last fixing
    private final LocalDate lastFixingNonCutoff; // The last fixing not in the cutoff period.
    private final double accrualFactorTotal; // Total accrual factor
    private final double[] accrualFactorCutoff; // Accrual factors for the sub-periods using the cutoff rate.
    private LocalDate nextFixing; // Running variable through the different methods: next fixing date to be analyzed

    private ObservationDetails(OvernightCompoundedRateComputation computation, OvernightIndexRates rates) {
      this.computation = computation;
      this.rates = rates;
      this.indexFixingDateSeries = rates.getFixings();
      this.dayCount = computation.getIndex().getDayCount();
      // Details of the cutoff period
      this.firstFixing = computation.getStartDate();
      this.lastFixingP1 = computation.getEndDate();
      this.lastFixing = computation.getFixingCalendar().previous(lastFixingP1);
      this.cutoffOffset = Math.max(computation.getRateCutOffDays(), 1);
      this.accrualFactorCutoff = new double[cutoffOffset - 1];
      LocalDate currentFixing = lastFixing;
      for (int i = 0; i < cutoffOffset - 1; i++) {
        currentFixing = computation.getFixingCalendar().previous(currentFixing);
        LocalDate effectiveDate = computation.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = computation.calculateMaturityFromEffective(effectiveDate);
        accrualFactorCutoff[i] = dayCount.yearFraction(effectiveDate, maturityDate);
      }
      this.lastFixingNonCutoff = currentFixing;
      LocalDate startUnderlyingPeriod = computation.calculateEffectiveFromFixing(firstFixing);
      LocalDate endUnderlyingPeriod = computation.calculateMaturityFromFixing(lastFixing);
      this.accrualFactorTotal = dayCount.yearFraction(startUnderlyingPeriod, endUnderlyingPeriod);
    }

    // Composition - publication strictly before valuation date: try accessing fixing time-series
    private double pastCompositionFactor() {
      double compositionFactor = 1.0d;
      LocalDate currentFixing = firstFixing;
      LocalDate currentPublication = computation.calculatePublicationFromFixing(currentFixing);
      while ((currentFixing.isBefore(lastFixingNonCutoff)) && // fixing in the non-cutoff period
          rates.getValuationDate().isAfter(currentPublication)) { // publication before valuation
        LocalDate effectiveDate = computation.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = computation.calculateMaturityFromEffective(effectiveDate);
        double accrualFactor = dayCount.yearFraction(effectiveDate, maturityDate);
        compositionFactor *= 1.0d + accrualFactor * checkedFixing(currentFixing, indexFixingDateSeries, computation.getIndex());
        currentFixing = computation.getFixingCalendar().next(currentFixing);
        currentPublication = computation.calculatePublicationFromFixing(currentFixing);
      }
      if (currentFixing.equals(lastFixingNonCutoff) && // fixing is on the last non-cutoff date, cutoff period known
          rates.getValuationDate().isAfter(currentPublication)) { // publication before valuation
        double rate = checkedFixing(currentFixing, indexFixingDateSeries, computation.getIndex());
        LocalDate effectiveDate = computation.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = computation.calculateMaturityFromEffective(effectiveDate);
        double accrualFactor = dayCount.yearFraction(effectiveDate, maturityDate);
        compositionFactor *= 1.0d + accrualFactor * rate;
        for (int i = 0; i < cutoffOffset - 1; i++) {
          compositionFactor *= 1.0d + accrualFactorCutoff[i] * rate;
        }
        currentFixing = computation.getFixingCalendar().next(currentFixing);
      }
      nextFixing = currentFixing;
      return compositionFactor;
    }

    // Composition - publication on valuation date: Check if a fixing is available on current date
    private double valuationCompositionFactor() {
      LocalDate currentFixing = nextFixing;
      LocalDate currentPublication = computation.calculatePublicationFromFixing(currentFixing);
      if (rates.getValuationDate().equals(currentPublication) &&
          !(currentFixing.isAfter(lastFixingNonCutoff))) { // If currentFixing > lastFixingNonCutoff, everything fixed
        OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixing);
        if (fixedRate.isPresent()) {
          nextFixing = computation.getFixingCalendar().next(nextFixing);
          LocalDate effectiveDate = computation.calculateEffectiveFromFixing(currentFixing);
          LocalDate maturityDate = computation.calculateMaturityFromEffective(effectiveDate);
          double accrualFactor = dayCount.yearFraction(effectiveDate, maturityDate);
          if (currentFixing.isBefore(lastFixingNonCutoff)) {
            return 1.0d + accrualFactor * fixedRate.getAsDouble();
          }
          double compositionFactor = 1.0d + accrualFactor * fixedRate.getAsDouble();
          for (int i = 0; i < cutoffOffset - 1; i++) {
            compositionFactor *= 1.0d + accrualFactorCutoff[i] * fixedRate.getAsDouble();
          }
          return compositionFactor;
        }
      }
      return 1.0d;
    }

    // Composition - forward part in non-cutoff period; past/valuation date case dealt with in previous methods
    private double compositionFactorNonCutoff() {
      if (nextFixing.isBefore(lastFixingNonCutoff)) {
        OvernightIndexObservation obs = computation.observeOn(nextFixing);
        LocalDate startDate = obs.getEffectiveDate();
        LocalDate endDate = computation.calculateMaturityFromFixing(lastFixingNonCutoff);
        double accrualFactor = dayCount.yearFraction(startDate, endDate);
        double rate = rates.periodRate(obs, endDate);
        return 1.0d + accrualFactor * rate;
      }
      return 1.0d;
    }

    // Composition - forward part in non-cutoff period; past/valuation date case dealt with in previous methods
    private ObjDoublePair<PointSensitivityBuilder> compositionFactorAndSensitivityNonCutoff() {
      if (nextFixing.isBefore(lastFixingNonCutoff)) {
        OvernightIndexObservation obs = computation.observeOn(nextFixing);
        LocalDate startDate = obs.getEffectiveDate();
        LocalDate endDate = computation.calculateMaturityFromFixing(lastFixingNonCutoff);
        double accrualFactor = dayCount.yearFraction(startDate, endDate);
        double rate = rates.periodRate(obs, endDate);
        PointSensitivityBuilder rateSensitivity = rates.periodRatePointSensitivity(obs, endDate);
        rateSensitivity = rateSensitivity.multipliedBy(accrualFactor);
        return ObjDoublePair.of(rateSensitivity, 1.0d + accrualFactor * rate);
      }
      return ObjDoublePair.of(PointSensitivityBuilder.none(), 1.0d);
    }

    // Composition - forward part in the cutoff period; past/valuation date case dealt with in previous methods
    private double compositionFactorCutoff() {
      if (nextFixing.isBefore(lastFixingNonCutoff)) {
        OvernightIndexObservation obs = computation.observeOn(lastFixingNonCutoff);
        double rate = rates.rate(obs);
        double compositionFactor = 1.0d;
        for (int i = 0; i < cutoffOffset - 1; i++) {
          compositionFactor *= 1.0d + accrualFactorCutoff[i] * rate;
        }
        return compositionFactor;
      }
      return 1.0d;
    }

    // Composition - forward part in the cutoff period; past/valuation date case dealt with in previous methods
    private ObjDoublePair<PointSensitivityBuilder> compositionFactorAndSensitivityCutoff() {
      OvernightIndexObservation obs = computation.observeOn(lastFixingNonCutoff);
      if (nextFixing.isBefore(lastFixingNonCutoff)) {
        double rate = rates.rate(obs);
        double compositionFactor = 1.0d;
        double compositionFactorDerivative = 0.0;
        for (int i = 0; i < cutoffOffset - 1; i++) {
          compositionFactor *= 1.0d + accrualFactorCutoff[i] * rate;
          compositionFactorDerivative += accrualFactorCutoff[i] / (1.0d + accrualFactorCutoff[i] * rate);
        }
        compositionFactorDerivative *= compositionFactor;
        PointSensitivityBuilder rateSensitivity =
            cutoffOffset <= 1 ? PointSensitivityBuilder.none() : rates.ratePointSensitivity(obs);
        rateSensitivity = rateSensitivity.multipliedBy(compositionFactorDerivative);
        return ObjDoublePair.of(rateSensitivity, compositionFactor);
      }
      return ObjDoublePair.of(PointSensitivityBuilder.none(), 1.0d);
    }

    // Calculate the total rate
    private double calculateRate() {
      return (pastCompositionFactor() * valuationCompositionFactor() *
          compositionFactorNonCutoff() * compositionFactorCutoff() - 1.0d) / accrualFactorTotal;
    }

    // Calculate the total rate sensitivity
    private PointSensitivityBuilder calculateRateSensitivity() {
      double factor = pastCompositionFactor() * valuationCompositionFactor() / accrualFactorTotal;
      ObjDoublePair<PointSensitivityBuilder> compositionFactorAndSensitivityNonCutoff =
          compositionFactorAndSensitivityNonCutoff();
      ObjDoublePair<PointSensitivityBuilder> compositionFactorAndSensitivityCutoff = compositionFactorAndSensitivityCutoff();

      PointSensitivityBuilder combinedPointSensitivity = compositionFactorAndSensitivityNonCutoff.getFirst()
          .multipliedBy(compositionFactorAndSensitivityCutoff.getSecond() * factor);
      combinedPointSensitivity = combinedPointSensitivity.combinedWith(compositionFactorAndSensitivityCutoff
          .getFirst().multipliedBy(compositionFactorAndSensitivityNonCutoff.getSecond() * factor));

      return combinedPointSensitivity;
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
  }

}
