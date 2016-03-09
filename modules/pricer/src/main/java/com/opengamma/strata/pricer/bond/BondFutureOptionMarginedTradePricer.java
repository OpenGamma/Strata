/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.ResolvedBondFutureOption;
import com.opengamma.strata.product.bond.ResolvedBondFutureOptionTrade;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Pricer for bond future option trades with daily margin.
 * <p>
 * This function provides the ability to price an {@link ResolvedBondFutureOptionTrade}.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public abstract class BondFutureOptionMarginedTradePricer {

  /**
   * Creates an instance.
   */
  protected BondFutureOptionMarginedTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract BondFutureOptionMarginedProductPricer getProductPricer();

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future option trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider futureProvider) {

    return getProductPricer().price(trade.getProduct(), ratesProvider, futureProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond future option trade from the current option price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param valuationDate  the valuation date; required to asses if the trade or last closing price should be used
   * @param currentOptionPrice  the option price on the valuation date
   * @param lastClosingPrice  the last closing price
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastClosingPrice) {

    ResolvedBondFutureOption option = trade.getProduct();
    double priceIndex = getProductPricer().marginIndex(option, currentOptionPrice);
    double marginReferencePrice = lastClosingPrice;
    if (trade.getTradeDate().equals(valuationDate)) {
      marginReferencePrice = trade.getPrice();
    }
    double referenceIndex = getProductPricer().marginIndex(option, marginReferencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(option.getUnderlyingFuture().getCurrency(), pv);
  }

  /**
   * Calculates the present value of the bond future option trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @param lastClosingPrice  the last closing price
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider futureProvider,
      double lastClosingPrice) {

    double price = price(trade, ratesProvider, futureProvider);
    return presentValue(trade, ratesProvider.getValuationDate(), price, lastClosingPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond future option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider futureProvider) {

    ResolvedBondFutureOption product = trade.getProduct();
    PointSensitivities priceSensi = getProductPricer().priceSensitivity(product, ratesProvider, futureProvider);
    PointSensitivities marginIndexSensi = getProductPricer().marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond future option trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @param lastClosingPrice  the last closing price
   * @return the currency exposure of the bond future option trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider futureProvider,
      double lastClosingPrice) {

    double price = price(trade, ratesProvider, futureProvider);
    return currencyExposure(trade, ratesProvider.getValuationDate(), price, lastClosingPrice);
  }

  /**
   * Calculates the currency exposure of the bond future option trade from the current option price.
   * 
   * @param trade  the trade
   * @param valuationDate  the valuation date; required to asses if the trade or last closing price should be used
   * @param currentOptionPrice  the option price on the valuation date
   * @param lastClosingPrice  the last closing price
   * @return the currency exposure of the bond future option trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastClosingPrice) {

    return MultiCurrencyAmount.of(presentValue(trade, valuationDate, currentOptionPrice, lastClosingPrice));
  }

}
