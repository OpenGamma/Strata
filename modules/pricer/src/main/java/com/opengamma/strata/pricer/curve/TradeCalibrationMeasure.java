/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.fx.ResolvedFxSwapTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Provides calibration measures for a single type of trade based on functions.
 * <p>
 * This is initialized using functions that typically refer to pricers.
 * 
 * @param <T> the trade type
 */
public final class TradeCalibrationMeasure<T extends ResolvedTrade>
    implements CalibrationMeasure<T> {

  /**
   * The calibrator for {@link ResolvedFraTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<ResolvedFraTrade> FRA_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "FraParSpreadDiscounting",
          ResolvedFraTrade.class,
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct(), p));

  /**
   * The calibrator for {@link ResolvedIborFutureTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<ResolvedIborFutureTrade> IBOR_FUTURE_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "IborFutureParSpreadDiscounting",
          ResolvedIborFutureTrade.class,
          (trade, p) -> DiscountingIborFutureTradePricer.DEFAULT.parSpread(trade, p, 0.0),
          (trade, p) -> DiscountingIborFutureTradePricer.DEFAULT.parSpreadSensitivity(trade, p));

  /**
   * The calibrator for {@link ResolvedSwapTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<ResolvedSwapTrade> SWAP_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "SwapParSpreadDiscounting",
          ResolvedSwapTrade.class,
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parSpreadSensitivity(
              trade.getProduct(), p).build());

  /**
   * The calibrator for {@link ResolvedIborFixingDepositTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<ResolvedIborFixingDepositTrade> IBOR_FIXING_DEPOSIT_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "IborFixingDepositParSpreadDiscounting",
          ResolvedIborFixingDepositTrade.class,
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parSpreadSensitivity(
              trade.getProduct(), p));

  /**
   * The calibrator for {@link ResolvedTermDepositTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<ResolvedTermDepositTrade> TERM_DEPOSIT_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "TermDepositParSpreadDiscounting",
          ResolvedTermDepositTrade.class,
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parSpreadSensitivity(
              trade.getProduct(), p));

  /**
   * The calibrator for {@link ResolvedFxSwapTrade} using par spread discounting.
   */
  public static final TradeCalibrationMeasure<ResolvedFxSwapTrade> FX_SWAP_PAR_SPREAD =
      TradeCalibrationMeasure.of(
          "FxSwapParSpreadDiscounting",
          ResolvedFxSwapTrade.class,
          (trade, p) -> DiscountingFxSwapProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingFxSwapProductPricer.DEFAULT.parSpreadSensitivity(trade.getProduct(), p));

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
  private final ToDoubleBiFunction<T, RatesProvider> valueFn;
  /**
   * The sensitivity measure.
   */
  private final BiFunction<T, RatesProvider, PointSensitivities> sensitivityFn;

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
  public static <R extends ResolvedTrade> TradeCalibrationMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, RatesProvider> valueFn,
      BiFunction<R, RatesProvider, PointSensitivities> sensitivityFn) {

    return new TradeCalibrationMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private TradeCalibrationMeasure(
      String name,
      Class<T> tradeType,
      ToDoubleBiFunction<T, RatesProvider> valueFn,
      BiFunction<T, RatesProvider, PointSensitivities> sensitivityFn) {

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
  public double value(T trade, RatesProvider provider) {
    return valueFn.applyAsDouble(trade, provider);
  }

  @Override
  public CurrencyParameterSensitivities sensitivities(T trade, RatesProvider provider) {
    PointSensitivities pts = sensitivityFn.apply(trade, provider);
    return provider.parameterSensitivity(pts);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}
