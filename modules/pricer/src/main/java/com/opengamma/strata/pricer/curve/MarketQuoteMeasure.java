/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.pricer.index.DiscountingIborFutureProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Provides market quote measures for a single type of trade based on functions.
 * <p>
 * This is initialized using functions that typically refer to pricers.
 * 
 * @param <T> the trade type
 */
public final class MarketQuoteMeasure<T extends ResolvedTrade>
    implements CalibrationMeasure<T> {

  /**
   * The measure for {@link ResolvedFraTrade} using par rate discounting.
   */
  public static final MarketQuoteMeasure<ResolvedFraTrade> FRA_MQ =
      MarketQuoteMeasure.of(
          "FraParRateDiscounting",
          ResolvedFraTrade.class,
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parRate(trade.getProduct(), p),
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.parRateSensitivity(trade.getProduct(), p));

  /**
   * The measure for {@link ResolvedIborFutureTrade} using price discounting.
   */
  public static final MarketQuoteMeasure<ResolvedIborFutureTrade> IBOR_FUTURE_MQ =
      MarketQuoteMeasure.of(
          "IborFuturePriceDiscounting",
          ResolvedIborFutureTrade.class,
          (trade, p) -> DiscountingIborFutureProductPricer.DEFAULT.price(trade.getProduct(), p),
          (trade, p) -> DiscountingIborFutureProductPricer.DEFAULT.priceSensitivity(trade.getProduct(), p));

  /**
   * The measure for {@link ResolvedSwapTrade} using par rate discounting. Apply only to swap with a fixed leg.
   */
  public static final MarketQuoteMeasure<ResolvedSwapTrade> SWAP_MQ =
      MarketQuoteMeasure.of(
          "SwapParRateDiscounting",
          ResolvedSwapTrade.class,
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parRate(trade.getProduct(), p),
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.parRateSensitivity(
              trade.getProduct(), p).build());

  /**
   * The measure for {@link ResolvedIborFixingDepositTrade} using par rate discounting.
   */
  public static final MarketQuoteMeasure<ResolvedIborFixingDepositTrade> IBOR_FIXING_DEPOSIT_MQ =
      MarketQuoteMeasure.of(
          "IborFixingDepositParRateDiscounting",
          ResolvedIborFixingDepositTrade.class,
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parRate(trade.getProduct(), p),
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.parRateSensitivity(
              trade.getProduct(), p));

  /**
   * The measure for {@link ResolvedTermDepositTrade} using par rate discounting.
   */
  public static final MarketQuoteMeasure<ResolvedTermDepositTrade> TERM_DEPOSIT_MQ =
      MarketQuoteMeasure.of(
          "TermDepositParRateDiscounting",
          ResolvedTermDepositTrade.class,
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parRate(trade.getProduct(), p),
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.parRateSensitivity(trade.getProduct(), p));

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
  public static <R extends ResolvedTrade> MarketQuoteMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, RatesProvider> valueFn,
      BiFunction<R, RatesProvider, PointSensitivities> sensitivityFn) {

    return new MarketQuoteMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private MarketQuoteMeasure(
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
