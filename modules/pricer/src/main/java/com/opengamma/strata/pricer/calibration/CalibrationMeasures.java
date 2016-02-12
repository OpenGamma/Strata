/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Provides access to the measures needed to perform curve calibration.
 * <p>
 * The most commonly used measures are par spread and converted present value.
 */
public final class CalibrationMeasures {

  /**
   * The par spread instance, which is the default used in curve calibration.
   * <p>
   * This computes par spread for Term Deposits, IborFixingDeposit, FRA, Ibor Futures
   * Swap and FX Swap by discounting.
   */
  public static final CalibrationMeasures PAR_SPREAD = CalibrationMeasures.of(
      "ParSpread",
      TradeCalibrationMeasure.FRA_PAR_SPREAD,
      TradeCalibrationMeasure.FX_SWAP_PAR_SPREAD,
      TradeCalibrationMeasure.IBOR_FIXING_DEPOSIT_PAR_SPREAD,
      TradeCalibrationMeasure.IBOR_FUTURE_PAR_SPREAD,
      TradeCalibrationMeasure.SWAP_PAR_SPREAD,
      TradeCalibrationMeasure.TERM_DEPOSIT_PAR_SPREAD);
  /**
   * The market quote instance, which is the default used in synthetic curve calibration.
   * <p>
   * This computes par rate for Term Deposits, IborFixingDeposit, FRA and Swap by discounting,
   * and price Ibor Futures by discounting.
   */
  public static final CalibrationMeasures MARKET_QUOTE = CalibrationMeasures.of(
      "MarketQuote",
      MarketQuoteMeasure.FRA_MQ,
      MarketQuoteMeasure.IBOR_FIXING_DEPOSIT_MQ,
      MarketQuoteMeasure.IBOR_FUTURE_MQ,
      MarketQuoteMeasure.SWAP_MQ,
      MarketQuoteMeasure.TERM_DEPOSIT_MQ);

  /**
   * The name of the set of measures.
   */
  private final String name;
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
   * @param name  the name of the set of measures
   * @param measures  the list of measures
   * @return the calibration measures
   * @throws IllegalArgumentException if a trade type is specified more than once
   */
  public static CalibrationMeasures of(String name, List<? extends CalibrationMeasure<? extends Trade>> measures) {
    return new CalibrationMeasures(name, measures);
  }

  /**
   * Obtains an instance from a list of individual trade-specific measures.
   * <p>
   * Each measure must be for a different trade type.
   * 
   * @param name  the name of the set of measures
   * @param measures  the list of measures
   * @return the calibration measures
   * @throws IllegalArgumentException if a trade type is specified more than once
   */
  @SafeVarargs
  public static CalibrationMeasures of(String name, CalibrationMeasure<? extends Trade>... measures) {
    return new CalibrationMeasures(name, ImmutableList.copyOf(measures));
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private CalibrationMeasures(String name, List<? extends CalibrationMeasure<? extends Trade>> measures) {
    this.name = ArgChecker.notEmpty(name, "name");
    this.measuresByTrade = measures.stream()
        .collect(toImmutableMap(CalibrationMeasure::getTradeType, m -> m));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name of the set of measures.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

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
  public double value(Trade trade, RatesProvider provider) {
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
  public DoubleArray derivative(Trade trade, RatesProvider provider, List<CurveParameterSize> curveOrder) {
    CurveUnitParameterSensitivities unitSens = extractSensitivities(trade, provider);

    // expand to a concatenated array
    DoubleArray result = DoubleArray.EMPTY;
    for (CurveParameterSize curveParams : curveOrder) {
      DoubleArray sens = unitSens.findSensitivity(curveParams.getName())
          .map(s -> s.getSensitivity())
          .orElseGet(() -> DoubleArray.filled(curveParams.getParameterCount()));
      result = result.concat(sens);
    }
    return result;
  }

  // determine the curve parameter sensitivities, removing the curency
  private CurveUnitParameterSensitivities extractSensitivities(Trade trade, RatesProvider provider) {
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
    return name;
  }

}
