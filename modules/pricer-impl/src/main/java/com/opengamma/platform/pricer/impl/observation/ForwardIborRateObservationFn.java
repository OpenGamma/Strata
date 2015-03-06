/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MulticurveSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.observation.IborRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.Legacy;
import com.opengamma.platform.pricer.impl.sensitivity.multicurve.ForwardRateSensitivity;
import com.opengamma.platform.pricer.impl.sensitivity.multicurve.ForwardRateSensitivityLD;
import com.opengamma.platform.pricer.impl.sensitivity.multicurve.MulticurveSensitivity3;
import com.opengamma.platform.pricer.impl.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
* Rate observation implementation for an IBOR-like index.
* <p>
* The implementation simply returns the rate from the {@code PricingEnvironment}.
*/
public class ForwardIborRateObservationFn
    implements RateObservationFn<IborRateObservation> {

  /**
   * Default implementation.
   */
  public static final ForwardIborRateObservationFn DEFAULT = new ForwardIborRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    return env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
  }

  public Pair<Double, MulticurveSensitivity> rateMulticurveSensitivity(
      PricingEnvironment env,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    LocalDate fixingDate = observation.getFixingDate();
    IborIndex index = observation.getIndex();
    // historic rate
    if (!fixingDate.isAfter(env.getValuationDate())) {
      OptionalDouble fixedRate = env.timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        return Pair.of(fixedRate.getAsDouble(), new MulticurveSensitivity());
      } else if (fixingDate.isBefore(env.getValuationDate())) { // the fixing is required
        throw new OpenGammaRuntimeException("Could not get fixing value for date " + fixingDate);
      }
    }
    // forward rate
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    double fixingAccrualFactor = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    double fixingStartTime = env.relativeTime(fixingStartDate);
    double fixingEndTime = env.relativeTime(fixingEndDate);
    double forwardRate = env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
    final Map<String, List<ForwardSensitivity>> mapFwd = new HashMap<>();
    final List<ForwardSensitivity> listForward = new ArrayList<>();
    listForward.add(new SimplyCompoundedForwardSensitivity(fixingStartTime, fixingEndTime, fixingAccrualFactor, 1.0d));
    mapFwd.put(env.rawData(MulticurveProviderInterface.class).getName(Legacy.iborIndex(index)), listForward);
    return Pair.of(forwardRate, MulticurveSensitivity.ofForward(mapFwd));
  }

  public Pair<Double, MulticurveSensitivity3> rateMulticurveSensitivity3(
      PricingEnvironment env,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      Currency ccy) {
    LocalDate fixingDate = observation.getFixingDate();
    IborIndex index = observation.getIndex();
    // historic rate
    if (!fixingDate.isAfter(env.getValuationDate())) {
      OptionalDouble fixedRate = env.timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        return Pair.of(fixedRate.getAsDouble(), new MulticurveSensitivity3());
      } else if (fixingDate.isBefore(env.getValuationDate())) { // the fixing is required
        throw new OpenGammaRuntimeException("Could not get fixing value for date " + fixingDate);
      }
    }
    // forward rate
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    double fixingAccrualFactor = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    double fixingTime = env.relativeTime(fixingDate);
    double fixingStartTime = env.relativeTime(fixingStartDate);
    double fixingEndTime = env.relativeTime(fixingEndDate);
    double forwardRate = env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
    final List<ForwardRateSensitivity> forwardRateSensi = new ArrayList<>();
    String curveName = env.rawData(MulticurveProviderInterface.class).getName(Legacy.iborIndex(index));
    forwardRateSensi.add(new ForwardRateSensitivity(curveName, fixingTime, fixingStartTime, fixingEndTime,
        fixingAccrualFactor, 1.0d, ccy));
    return Pair.of(forwardRate, MulticurveSensitivity3.ofForwardRate(forwardRateSensi));
  }

  public Pair<Double, MulticurveSensitivity3LD> rateMulticurveSensitivity3LD(
      PricingEnvironment env,
      LocalDate valuationDate,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      Currency ccy) {
    LocalDate fixingDate = observation.getFixingDate();
    IborIndex index = observation.getIndex();
    // historic rate
    if (!fixingDate.isAfter(valuationDate)) {
      OptionalDouble fixedRate = env.timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        return Pair.of(fixedRate.getAsDouble(), new MulticurveSensitivity3LD());
      } else if (fixingDate.isBefore(valuationDate)) { // the fixing is required
        throw new OpenGammaRuntimeException("Could not get fixing value for date " + fixingDate);
      }
    }
    // forward rate
    double forwardRate = env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
    final List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(index, fixingDate, 1.0d, ccy));
    return Pair.of(forwardRate, MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
  }

}
