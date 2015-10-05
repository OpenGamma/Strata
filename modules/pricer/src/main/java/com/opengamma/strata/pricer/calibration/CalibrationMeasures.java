/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static java.util.stream.Collectors.joining;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Provides access to the measures needed to perform curve calibration.
 * <p>
 * The most commonly used measures are par spread and converted present value.
 */
public final class CalibrationMeasures {

  /**
   * The default instance.
   * This computes par spread for IborFixingDeposit, Fra and Swap by discounting.
   */
  public static final CalibrationMeasures DEFAULT = CalibrationMeasures.of(
      TradeCalibrationMeasure.FRA_PAR_SPREAD,
      TradeCalibrationMeasure.IBOR_FIXING_DEPOSIT_PAR_SPREAD,
      TradeCalibrationMeasure.SWAP_PAR_SPREAD,
      TradeCalibrationMeasure.TERM_DEPOSIT_PAR_SPREAD);

  /**
   * The calibration measure providers keyed by type.
   */
  private final ImmutableMap<Class<?>, CalibrationMeasure<? extends Trade>> measuresByTrade;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a list of individual trade-specific measures.
   * <p>
   * Each measure must be for a different trade type.
   * 
   * @param measures  the list of measures
   * @return the calibration measures
   * @throws IllegalArgumentException if a trade type is specified more than once
   */
  public static CalibrationMeasures of(List<? extends CalibrationMeasure<? extends Trade>> measures) {
    return new CalibrationMeasures(measures);
  }

  /**
   * Obtains an instance from a list of individual trade-specific measures.
   * <p>
   * Each measure must be for a different trade type.
   * 
   * @param measures  the list of measures
   * @return the calibration measures
   * @throws IllegalArgumentException if a trade type is specified more than once
   */
  @SafeVarargs
  public static CalibrationMeasures of(CalibrationMeasure<? extends Trade>... measures) {
    return new CalibrationMeasures(ImmutableList.copyOf(measures));
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private CalibrationMeasures(List<? extends CalibrationMeasure<? extends Trade>> measures) {
    measuresByTrade = measures.stream()
        .collect(toImmutableMap(CalibrationMeasure::getTradeType, m -> m));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the supported trade types.
   * 
   * @return the supported trade types
   */
  public ImmutableSet<Class<?>> getTradeTypes() {
    return measuresByTrade.keySet();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the value, such as par spread.
   * <p>
   * The value must be calculated using the specified rates provider.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the sensitivity
   * @throws IllegalArgumentException if the trade cannot be valued
   */
  public double value(Trade trade, ImmutableRatesProvider provider) {
    CalibrationMeasure<Trade> measure = getMeasure(trade.getClass());
    return measure.value(trade, provider);
  }

  /**
   * Calculates the sensitivity with respect to the rates provider.
   * <p>
   * The result array is composed of the concatenated curve sensitivities from
   * all curves currently being processed.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @param curveOrder  the order of the curves
   * @return the sensitivity derivative
   */
  public double[] derivative(Trade trade, ImmutableRatesProvider provider, List<CurveParameterSize> curveOrder) {
    CurveUnitParameterSensitivities unitSens = extractSensitivities(trade, provider);

    // expand to a concatenated array
    double[] result = DoubleArrayMath.EMPTY_DOUBLE_ARRAY;
    for (CurveParameterSize curveParams : curveOrder) {
      double[] sens = unitSens.findSensitivity(curveParams.getName())
          .map(s -> s.getSensitivity())
          .orElse(new double[curveParams.getParameterCount()]);
      result = Doubles.concat(result, sens);
    }
    return result;
  }

  // determine the curve parameter sensitivities, removing the curency
  private CurveUnitParameterSensitivities extractSensitivities(Trade trade, ImmutableRatesProvider provider) {
    CalibrationMeasure<Trade> measure = getMeasure(trade.getClass());
    CurveCurrencyParameterSensitivities paramSens = measure.sensitivities(trade, provider);
    CurveUnitParameterSensitivities unitSens = CurveUnitParameterSensitivities.empty();
    for (CurveCurrencyParameterSensitivity ccySens : paramSens.getSensitivities()) {
      unitSens = unitSens.combinedWith(CurveUnitParameterSensitivity.of(ccySens.getMetadata(), ccySens.getSensitivity()));
    }
    return unitSens;
  }

  //-------------------------------------------------------------------------
  // finds the correct measure implementation
  @SuppressWarnings("unchecked")
  private <T extends Trade> CalibrationMeasure<Trade> getMeasure(Class<?> tradeType) {
    CalibrationMeasure<? extends Trade> measure = measuresByTrade.get(tradeType);
    if (measure == null) {
      throw new IllegalArgumentException("Trade type " + tradeType.getName() + " is not supported for calibration");
    }
    // cast makes life easier for the code using this method
    return (CalibrationMeasure<Trade>) measure;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return measuresByTrade.values().stream()
        .map(CalibrationMeasure::toString)
        .collect(joining(", ", "[", "]"));
  }

}
