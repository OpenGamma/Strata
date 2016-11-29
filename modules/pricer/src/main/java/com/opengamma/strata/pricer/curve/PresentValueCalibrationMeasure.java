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
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Provides calibration measures for a single type of trade based on functions.
 * <p>
 * This set of measures return the present value of the product. For multi-currency instruments, the present value
 * is converted into the currency of the first leg.
 * The sensitivities are with respect to the market quote sensitivities and are also converted in the currency of the 
 * first leg when necessary.
 * 
 * @param <T> the trade type
 */
public final class PresentValueCalibrationMeasure<T extends ResolvedTrade>
    implements CalibrationMeasure<T> {

  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  /**
   * The measure for {@link FraTrade} using present value discounting.
   */
  public static final PresentValueCalibrationMeasure<ResolvedFraTrade> FRA_PV =
      PresentValueCalibrationMeasure.of(
          "FraPresentValueDiscounting",
          ResolvedFraTrade.class,
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.presentValue(trade.getProduct(), p).getAmount(),
          (trade, p) -> DiscountingFraProductPricer.DEFAULT.presentValueSensitivity(trade.getProduct(), p));

  /**
   * The calibrator for {@link IborFutureTrade} using par spread discounting.
   */
  public static final PresentValueCalibrationMeasure<ResolvedIborFutureTrade> IBOR_FUTURE_PV =
      PresentValueCalibrationMeasure.of(
          "IborFutureParSpreadDiscounting",
          ResolvedIborFutureTrade.class,
          (trade, p) -> DiscountingIborFutureTradePricer.DEFAULT.presentValue(trade, p, 0.0).getAmount(),
          (trade, p) -> DiscountingIborFutureTradePricer.DEFAULT.presentValueSensitivity(trade, p));

  /**
   * The calibrator for {@link SwapTrade} using par spread discounting.
   */
  public static final PresentValueCalibrationMeasure<ResolvedSwapTrade> SWAP_PV =
      PresentValueCalibrationMeasure.of(
          "SwapParSpreadDiscounting",
          ResolvedSwapTrade.class,
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.presentValue(trade.getProduct(), p)
              .convertedTo(trade.getProduct().getLegs().get(0).getCurrency(), p).getAmount(),
          (trade, p) -> DiscountingSwapProductPricer.DEFAULT.presentValueSensitivity(trade.getProduct(), p).build()
              .convertedTo(trade.getProduct().getLegs().get(0).getCurrency(), p));

  /**
   * The calibrator for {@link IborFixingDepositTrade} using par spread discounting.
   */
  public static final PresentValueCalibrationMeasure<ResolvedIborFixingDepositTrade> IBOR_FIXING_DEPOSIT_PV =
      PresentValueCalibrationMeasure.of(
          "IborFixingDepositParSpreadDiscounting",
          ResolvedIborFixingDepositTrade.class,
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.presentValue(trade.getProduct(), p).getAmount(),
          (trade, p) -> DiscountingIborFixingDepositProductPricer.DEFAULT.presentValueSensitivity(trade.getProduct(), p));

  /**
   * The calibrator for {@link TermDepositTrade} using par spread discounting.
   */
  public static final PresentValueCalibrationMeasure<ResolvedTermDepositTrade> TERM_DEPOSIT_PV =
      PresentValueCalibrationMeasure.of(
          "TermDepositParSpreadDiscounting",
          ResolvedTermDepositTrade.class,
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.presentValue(trade.getProduct(), p).getAmount(),
          (trade, p) -> DiscountingTermDepositProductPricer.DEFAULT.presentValueSensitivity(trade.getProduct(), p));

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
  public static <R extends ResolvedTrade> PresentValueCalibrationMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, RatesProvider> valueFn,
      BiFunction<R, RatesProvider, PointSensitivities> sensitivityFn) {

    return new PresentValueCalibrationMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private PresentValueCalibrationMeasure(
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
    CurrencyParameterSensitivities ps = provider.parameterSensitivity(pts);
    return MQC.sensitivity(ps, provider);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}
