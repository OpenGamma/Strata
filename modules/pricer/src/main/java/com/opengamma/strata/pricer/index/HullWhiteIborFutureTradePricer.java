/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Pricer for for Ibor future trades.
 * <p>
 * This function provides the ability to price a {@link IborFutureTrade} based on
 * Hull-White one-factor model with piecewise constant volatility.
 * <p> 
 * Reference: Henrard M., Eurodollar Futures and Options: Convexity Adjustment in HJM One-Factor Model. March 2005.
 * Available at <a href="http://ssrn.com/abstract=682343">http://ssrn.com/abstract=682343</a>
 * 
 * <h4>Price</h4>
 * The price of an Ibor future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
public class HullWhiteIborFutureTradePricer
    extends AbstractIborFutureTradePricer {

  /**
   * Default implementation.
   */
  public static final HullWhiteIborFutureTradePricer DEFAULT =
      new HullWhiteIborFutureTradePricer(HullWhiteIborFutureProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final HullWhiteIborFutureProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link IborFuture}
   */
  public HullWhiteIborFutureTradePricer(HullWhiteIborFutureProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  protected HullWhiteIborFutureProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * The price is calculated using the Hull-White model.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the price of the trade, in decimal form
   */
  public double price(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    return productPricer.price(trade.getProduct(), ratesProvider, hwProvider);
  }

  /**
   * Calculates the present value of the Ibor future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is calculated using the Hull-White model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double lastSettlementPrice) {

    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), lastSettlementPrice);
    double price = price(trade, ratesProvider, hwProvider);
    return presentValue(trade, price, referencePrice);
  }

  /**
   * Calculates the present value sensitivity of the Ibor future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    ResolvedIborFuture product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, ratesProvider, hwProvider);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  /**
   * Calculates the present value sensitivity to piecewise constant volatility parameters of the Hull-White model.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the present value parameter sensitivity of the trade
   */
  public DoubleArray presentValueSensitivityHullWhiteParameter(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    ResolvedIborFuture product = trade.getProduct();
    DoubleArray hwSensi = productPricer.priceSensitivityHullWhiteParameter(product, ratesProvider, hwProvider);
    hwSensi = hwSensi.multipliedBy(product.getNotional() * product.getAccrualFactor() * trade.getQuantity());
    return hwSensi;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the Ibor future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * The current price is calculated using the Hull-White model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the par spread.
   */
  public double parSpread(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double lastSettlementPrice) {

    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), lastSettlementPrice);
    return price(trade, ratesProvider, hwProvider) - referencePrice;
  }

  /**
   * Calculates the par spread sensitivity of the Ibor future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivity(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    return productPricer.priceSensitivity(trade.getProduct(), ratesProvider, hwProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the Ibor future trade.
   * <p>
   * Since the Ibor future is based on a single currency, the trade is exposed to only this currency.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedIborFutureTrade trade,
      RatesProvider provider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double lastSettlementPrice) {

    return MultiCurrencyAmount.of(presentValue(trade, provider, hwProvider, lastSettlementPrice));
  }

}
