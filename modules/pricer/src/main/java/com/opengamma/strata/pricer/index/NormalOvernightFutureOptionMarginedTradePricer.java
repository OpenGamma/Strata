/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradedPrice;
import com.opengamma.strata.product.index.OvernightFutureOption;
import com.opengamma.strata.product.index.ResolvedOvernightFuture;
import com.opengamma.strata.product.index.ResolvedOvernightFutureOption;
import com.opengamma.strata.product.index.ResolvedOvernightFutureOptionTrade;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Pricer implementation for Overnight future option.
 * <p>
 * This provides the ability to price an Overnight future option.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 *
 * <h4>Price</h4>
 * The price of an Overnight future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Overnight future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
 */
public class NormalOvernightFutureOptionMarginedTradePricer {

  /**
   * Default implementation.
   */
  public static final NormalOvernightFutureOptionMarginedTradePricer DEFAULT =
      new NormalOvernightFutureOptionMarginedTradePricer(NormalOvernightFutureOptionMarginedProductPricer.DEFAULT);

  /**
   * Underlying option pricer.
   */
  private final NormalOvernightFutureOptionMarginedProductPricer futureOptionPricer;

  /**
   * Creates an instance.
   *
   * @param futureOptionPricer  the pricer for {@link OvernightFutureOption}
   */
  public NormalOvernightFutureOptionMarginedTradePricer(
      NormalOvernightFutureOptionMarginedProductPricer futureOptionPricer) {
    this.futureOptionPricer = ArgChecker.notNull(futureOptionPricer, "futureOptionPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Overnight future option trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * The price is calculated using the volatility model.
   *
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    return futureOptionPricer.price(trade.getProduct(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Overnight future option trade from the current option price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is specified, not calculated.
   * <p>
   * This method calculates based on the difference between the specified current price and the
   * last settlement price, or the trade price if traded on the valuation date.
   *
   * @param trade  the trade
   * @param valuationDate  the valuation date; required to asses if the trade or last closing price should be used
   * @param currentOptionPrice  the current price for the option, in decimal form
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedOvernightFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastOptionSettlementPrice) {

    ResolvedOvernightFutureOption option = trade.getProduct();
    double referencePrice = referencePrice(trade, valuationDate, lastOptionSettlementPrice);
    double priceIndex = futureOptionPricer.marginIndex(option, currentOptionPrice);
    double referenceIndex = futureOptionPricer.marginIndex(option, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(option.getUnderlyingFuture().getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Overnight future option trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is calculated using the volatility model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   *
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities,
      double lastOptionSettlementPrice) {

    double price = price(trade, ratesProvider, volatilities);
    return presentValue(trade, ratesProvider.getValuationDate(), price, lastOptionSettlementPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Overnight future option trade from the underlying future price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is calculated using the volatility model with a known future price.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   *
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities,
      double futurePrice,
      double lastOptionSettlementPrice) {

    double optionPrice = futureOptionPricer.price(trade.getProduct(), volatilities, futurePrice);
    return presentValue(trade, ratesProvider.getValuationDate(), optionPrice, lastOptionSettlementPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the Overnight future option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   *
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityRates(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    ResolvedOvernightFutureOption product = trade.getProduct();
    PointSensitivities priceSensi =
        futureOptionPricer.priceSensitivityRatesStickyStrike(product, ratesProvider, volatilities);
    PointSensitivities marginIndexSensi = futureOptionPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the normal volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * The volatility is associated with the expiry/delay/strike/future price key combination.
   * <p>
   * This calculates the underlying future price using the future pricer.
   *
   * @param futureOptionTrade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the price sensitivity
   */
  public OvernightFutureOptionSensitivity presentValueSensitivityModelParamsVolatility(
      ResolvedOvernightFutureOptionTrade futureOptionTrade,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    ResolvedOvernightFuture future = futureOptionTrade.getProduct().getUnderlyingFuture();
    double futurePrice = futureOptionPricer.getFuturePricer().price(future, ratesProvider);
    return presentValueSensitivityModelParamsVolatility(futureOptionTrade, volatilities, futurePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the normal volatility used in the pricing
   * based on the price of the underlying future.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * The volatility is associated with the expiry/delay/strike/future price key combination.
   *
   * @param futureOptionTrade  the trade
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @return the price sensitivity
   */
  public OvernightFutureOptionSensitivity presentValueSensitivityModelParamsVolatility(
      ResolvedOvernightFutureOptionTrade futureOptionTrade,
      NormalOvernightFutureOptionVolatilities volatilities,
      double futurePrice) {

    ResolvedOvernightFutureOption product = futureOptionTrade.getProduct();
    OvernightFutureOptionSensitivity priceSensitivity =
        futureOptionPricer.priceSensitivityModelParamsVolatility(product, volatilities, futurePrice);
    double factor = futureOptionPricer.marginIndex(product, 1) * futureOptionTrade.getQuantity();
    return priceSensitivity.multipliedBy(factor);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the reference price for the trade.
   * <p>
   * If the valuation date equals the trade date, then the reference price is the trade price.
   * Otherwise, the reference price is the last settlement price used for margining.
   *
   * @param trade  the trade
   * @param valuationDate  the date for which the reference price should be calculated
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the reference price, in decimal form
   */
  private double referencePrice(ResolvedOvernightFutureOptionTrade trade, LocalDate valuationDate, double lastSettlementPrice) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    return trade.getTradedPrice()
        .filter(tp -> tp.getTradeDate().equals(valuationDate))
        .map(TradedPrice::getPrice)
        .orElse(lastSettlementPrice);
  }

}
