/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.UnitParameterSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.ResolvedTrade;

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
   * The present value instance, which is the default used in present value sensitivity to market quote stored during 
   * curve calibration.
   * <p>
   * This computes present value for Term Deposits, IborFixingDeposit, FRA and Swap by discounting,
   * and price Ibor Futures by discounting; the derivative is the derivative with respect to the market quotes.
   */
  public static final CalibrationMeasures PRESENT_VALUE = CalibrationMeasures.of(
      "PresentValue",
      PresentValueCalibrationMeasure.FRA_PV,
      PresentValueCalibrationMeasure.IBOR_FIXING_DEPOSIT_PV,
      PresentValueCalibrationMeasure.IBOR_FUTURE_PV,
      PresentValueCalibrationMeasure.SWAP_PV,
      PresentValueCalibrationMeasure.TERM_DEPOSIT_PV);

  /**
   * The name of the set of measures.
   */
  private final String name;
  /**
   * The calibration measure providers keyed by type.
   */
  private final ImmutableMap<Class<?>, CalibrationMeasure<? extends ResolvedTrade>> measuresByTrade;

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
  public static CalibrationMeasures of(String name, List<? extends CalibrationMeasure<? extends ResolvedTrade>> measures) {
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
  public static CalibrationMeasures of(String name, CalibrationMeasure<? extends ResolvedTrade>... measures) {
    return new CalibrationMeasures(name, ImmutableList.copyOf(measures));
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private CalibrationMeasures(String name, List<? extends CalibrationMeasure<? extends ResolvedTrade>> measures) {
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
  public double value(ResolvedTrade trade, RatesProvider provider) {
    CalibrationMeasure<ResolvedTrade> measure = getMeasure(trade);
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
  public DoubleArray derivative(ResolvedTrade trade, RatesProvider provider, List<CurveParameterSize> curveOrder) {
    UnitParameterSensitivities unitSens = extractSensitivities(trade, provider);

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
  private UnitParameterSensitivities extractSensitivities(ResolvedTrade trade, RatesProvider provider) {
    CalibrationMeasure<ResolvedTrade> measure = getMeasure(trade);
    CurrencyParameterSensitivities paramSens = measure.sensitivities(trade, provider);
    UnitParameterSensitivities unitSens = UnitParameterSensitivities.empty();
    for (CurrencyParameterSensitivity ccySens : paramSens.getSensitivities()) {
      unitSens = unitSens.combinedWith(ccySens.toUnitParameterSensitivity());
    }
    return unitSens;
  }

  //-------------------------------------------------------------------------
  // finds the correct measure implementation
  @SuppressWarnings("unchecked")
  private <T extends ResolvedTrade> CalibrationMeasure<ResolvedTrade> getMeasure(ResolvedTrade trade) {
    Class<? extends ResolvedTrade> tradeType = trade.getClass();
    CalibrationMeasure<? extends ResolvedTrade> measure = measuresByTrade.get(tradeType);
    if (measure == null) {
      throw new IllegalArgumentException(Messages.format(
          "Trade type '{}' is not supported for calibration", tradeType.getSimpleName()));
    }
    // cast makes life easier for the code using this method
    return (CalibrationMeasure<ResolvedTrade>) measure;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}
