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
   * <p>
   * The calculation is performed against a reference price. On the trade date, the reference price
   * is the trade price, otherwise it is the settlement price.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @param settlementPrice  the last settlement price used for margining
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double settlementPrice) {

    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), settlementPrice);
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
   * <p>
   * The calculation is performed against a reference price. On the trade date, the reference price
   * is the trade price, otherwise it is the settlement price.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @param settlementPrice  the last settlement price used for margining
   * @return the par spread.
   */
  public double parSpread(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double settlementPrice) {

    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), settlementPrice);
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
   * The calculation is performed against a reference price. On the trade date, the reference price
   * is the trade price, otherwise it is the settlement price.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @param settlementPrice  the last settlement price used for margining
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedIborFutureTrade trade,
      RatesProvider provider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double settlementPrice) {

    return MultiCurrencyAmount.of(presentValue(trade, provider, hwProvider, settlementPrice));
  }

}
