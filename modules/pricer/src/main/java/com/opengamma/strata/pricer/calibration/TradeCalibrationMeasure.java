/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.deposit.IborFixingDepositTrade;
import com.opengamma.strata.finance.rate.deposit.TermDepositTrade;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.rate.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;

/**
 * Provides calibration measures for a single type of trade based on functions.
 * <p>
 * This is initialized using functions that typically refer to pricers.
 * 
 * @param <T> the trade type
 */
public class TradeCalibrationMeasure<T extends Trade>
    implements CalibrationMeasure<T> {

  /**
   * The calibrator for {@link FraTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<FraTrade> FRA_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "FraParSpreadDiscounting",
          FraTrade.class,
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct(), p));

  /**
   * The calibrator for {@link SwapTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<SwapTrade> SWAP_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "SwapParSpreadDiscounting",
          SwapTrade.class,
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct(), p).build());

  /**
   * The calibrator for {@link IborFixingDepositTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<IborFixingDepositTrade> IBOR_FIXING_DEPOSIT_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "IborFixingDepositParSpreadDiscounting",
          IborFixingDepositTrade.class,
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct(), p));

  /**
   * The calibrator for {@link TermDepositTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<TermDepositTrade> TERM_DEPOSIT_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "TermDepositParSpreadDiscounting",
          TermDepositTrade.class,
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct(), p));

  //-------------------------------------------------------------------------
  /**
   * The name.
   */
  private final String name;
  /**
   * The trade type.
   */
  private final Class<T> tradeType;
  /**
   * The value measure.
   */
  private final ToDoubleBiFunction<T, ImmutableRatesProvider> valueFn;
  /**
   * The sensitivity measure.
   */
  private final BiFunction<T, ImmutableRatesProvider, PointSensitivities> sensitivityFn;

  //-------------------------------------------------------------------------
  /**
   * Obtains a calibrator for a specific type of trade.
   * <p>
   * The functions typically refer to pricers.
   * 
   * @param <R>  the trade type
   * @param name  the name
   * @param tradeType  the trade type
   * @param valueFn  the function for calculating the value
   * @param sensitivityFn  the function for calculating the sensitivity
   * @return the calibrator
   */
  public static <R extends Trade> TradeCalibrationMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, ImmutableRatesProvider> valueFn,
      BiFunction<R, ImmutableRatesProvider, PointSensitivities> sensitivityFn) {

    return new TradeCalibrationMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private TradeCalibrationMeasure(
      String name,
      Class<T> tradeType,
      ToDoubleBiFunction<T, ImmutableRatesProvider> valueFn,
      BiFunction<T, ImmutableRatesProvider, PointSensitivities> sensitivityFn) {

    this.name = name;
    this.tradeType = tradeType;
    this.valueFn = ArgChecker.notNull(valueFn, "valueFn");
    this.sensitivityFn = ArgChecker.notNull(sensitivityFn, "sensitivityFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<T> getTradeType() {
    return tradeType;
  }

  //-------------------------------------------------------------------------
  @Override
  public double value(T trade, ImmutableRatesProvider provider) {
    return valueFn.applyAsDouble(trade, provider);
  }

  @Override
  public CurveCurrencyParameterSensitivities sensitivities(T trade, ImmutableRatesProvider provider) {
    PointSensitivities pts = sensitivityFn.apply(trade, provider);
    return provider.curveParameterSensitivity(pts);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}
