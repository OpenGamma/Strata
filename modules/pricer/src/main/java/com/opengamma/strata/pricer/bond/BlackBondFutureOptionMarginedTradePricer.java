/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.ResolvedBondFuture;
import com.opengamma.strata.product.bond.ResolvedBondFutureOption;
import com.opengamma.strata.product.bond.ResolvedBondFutureOptionTrade;

/**
 * Pricer implementation for bond future option.
 * <p>
 * The bond future option is priced based on Black model.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link BondFuture}.
 */
public final class BlackBondFutureOptionMarginedTradePricer {

  /**
   * Default implementation.
   */
  public static final BlackBondFutureOptionMarginedTradePricer DEFAULT =
      new BlackBondFutureOptionMarginedTradePricer(BlackBondFutureOptionMarginedProductPricer.DEFAULT);

  /**
   * Underlying option pricer.
   */
  private final BlackBondFutureOptionMarginedProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedBondFutureOption}
   */
  public BlackBondFutureOptionMarginedTradePricer(
      BlackBondFutureOptionMarginedProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future option trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    return productPricer.price(trade.getProduct(), discountingProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond future option trade from the current option price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is specified, not calculated.
   * <p>
   * This method calculates based on the difference between the specified current price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param valuationDate  the valuation date; required to asses if the trade or last closing price should be used
   * @param currentOptionPrice  the option price on the valuation date
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastOptionSettlementPrice) {

    ResolvedBondFutureOption option = trade.getProduct();
    double referencePrice = referencePrice(trade, valuationDate, lastOptionSettlementPrice);
    double priceIndex = productPricer.marginIndex(option, currentOptionPrice);
    double referenceIndex = productPricer.marginIndex(option, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(option.getUnderlyingFuture().getCurrency(), pv);
  }

  /**
   * Calculates the present value of the bond future option trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is calculated using the volatility model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities,
      double lastOptionSettlementPrice) {

    double price = price(trade, discountingProvider, volatilities);
    return presentValue(trade, discountingProvider.getValuationDate(), price, lastOptionSettlementPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond future option trade from the underlying future price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is calculated using the volatility model with a known future price.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice,
      double lastOptionSettlementPrice) {

    double optionPrice = productPricer.price(trade.getProduct(), discountingProvider, volatilities, futurePrice);
    return presentValue(trade, discountingProvider.getValuationDate(), optionPrice, lastOptionSettlementPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond future option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityRates(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    ResolvedBondFutureOption product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, discountingProvider, volatilities);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the Black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * The volatility is associated with the expiry/delay/strike/future price key combination.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOptionTrade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price sensitivity
   */
  public BondFutureOptionSensitivity presentValueSensitivityModelParamsVolatility(
      ResolvedBondFutureOptionTrade futureOptionTrade,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    ResolvedBondFuture future = futureOptionTrade.getProduct().getUnderlyingFuture();
    double futurePrice = productPricer.getFuturePricer().price(future, discountingProvider);
    return presentValueSensitivityModelParamsVolatility(futureOptionTrade, discountingProvider, volatilities, futurePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the Black volatility used in the pricing
   * based on the price of the underlying future.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * The volatility is associated with the expiry/delay/strike/future price key combination.
   * 
   * @param futureOptionTrade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @return the price sensitivity
   */
  public BondFutureOptionSensitivity presentValueSensitivityModelParamsVolatility(
      ResolvedBondFutureOptionTrade futureOptionTrade,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    ResolvedBondFutureOption product = futureOptionTrade.getProduct();
    BondFutureOptionSensitivity priceSensitivity =
        productPricer.priceSensitivityModelParamsVolatility(product, discountingProvider, volatilities, futurePrice);
    double factor = productPricer.marginIndex(product, 1) * futureOptionTrade.getQuantity();
    return priceSensitivity.multipliedBy(factor);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond future option trade.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the currency exposure of the bond future option trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities,
      double lastOptionSettlementPrice) {

    double price = price(trade, discountingProvider, volatilities);
    return currencyExposure(trade, discountingProvider.getValuationDate(), price, lastOptionSettlementPrice);
  }

  /**
   * Calculates the currency exposure of the bond future option trade from the current option price.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param valuationDate  the valuation date; required to asses if the trade or last closing price should be used
   * @param currentOptionPrice  the option price on the valuation date
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the currency exposure of the bond future option trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastOptionSettlementPrice) {

    return MultiCurrencyAmount.of(presentValue(trade, valuationDate, currentOptionPrice, lastOptionSettlementPrice));
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
  private double referencePrice(ResolvedBondFutureOptionTrade trade, LocalDate valuationDate, double lastSettlementPrice) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    return (trade.getTradeDate().equals(valuationDate) ? trade.getPrice() : lastSettlementPrice);
  }

}
