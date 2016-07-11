/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Pricer for Ibor future option trades with daily margin.
 * <p>
 * This function provides the ability to price an {@link IborFutureOptionTrade}.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * 
 * <h4>Price</h4>
 * The price of an Ibor future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
 */
public abstract class IborFutureOptionMarginedTradePricer {

  /**
   * Creates an instance.
   */
  protected IborFutureOptionMarginedTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract IborFutureOptionMarginedProductPricer getProductPricer();

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future option trade.
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
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return getProductPricer().price(trade.getProduct(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor future option trade from the current option price.
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
      ResolvedIborFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastOptionSettlementPrice) {

    ResolvedIborFutureOption option = trade.getProduct();
    double referencePrice = referencePrice(trade, valuationDate, lastOptionSettlementPrice);
    double priceIndex = getProductPricer().marginIndex(option, currentOptionPrice);
    double referenceIndex = getProductPricer().marginIndex(option, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(option.getUnderlyingFuture().getCurrency(), pv);
  }

  /**
   * Calculates the present value of the Ibor future option trade.
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
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities,
      double lastOptionSettlementPrice) {

    double price = price(trade, ratesProvider, volatilities);
    return presentValue(trade, ratesProvider.getValuationDate(), price, lastOptionSettlementPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the Ibor future option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    ResolvedIborFutureOption product = trade.getProduct();
    PointSensitivities priceSensi = getProductPricer().priceSensitivity(product, ratesProvider, volatilities);
    PointSensitivities marginIndexSensi = getProductPricer().marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
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
  public double referencePrice(ResolvedIborFutureOptionTrade trade, LocalDate valuationDate, double lastSettlementPrice) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    return (trade.getTradeDate().equals(valuationDate) ? trade.getPrice() : lastSettlementPrice);
  }

}
