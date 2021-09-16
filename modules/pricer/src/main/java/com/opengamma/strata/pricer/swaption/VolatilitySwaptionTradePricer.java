/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;

/**
 * Pricer for swaptions handling physical and cash par yield settlement based on volatilities.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * <p>
 * The present value and sensitivities of the premium, if in the future, are also taken into account.
 */
public class VolatilitySwaptionTradePricer {

  /**
   * Default implementation.
   */
  public static final VolatilitySwaptionTradePricer DEFAULT = new VolatilitySwaptionTradePricer(
      VolatilitySwaptionProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedSwaption}.
   */
  private final VolatilitySwaptionProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedSwaption}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public VolatilitySwaptionTradePricer(
      VolatilitySwaptionProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption trade.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    // product
    ResolvedSwaption product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(product, ratesProvider, swaptionVolatilities);
    // premium
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    // total
    return pvProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky strike" style, i.e. the sensitivity to the 
   * curve nodes with the volatility at the swaption strike unchanged. This sensitivity does not include a potential 
   * change of volatility due to the implicit change of forward rate or moneyness.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    // product
    ResolvedSwaption product = trade.getProduct();
    PointSensitivityBuilder pvcsProduct =
        productPricer.presentValueSensitivityRatesStickyStrike(product, ratesProvider, swaptionVolatilities);
    // premium
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = paymentPricer.presentValueSensitivity(premium, ratesProvider);
    // total
    return pvcsProduct.combinedWith(pvcsPremium).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption trade.
   * <p>
   * The sensitivity to the implied volatility is also called vega.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the implied volatility
   */
  public PointSensitivities presentValueSensitivityModelParamsVolatility(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ResolvedSwaption product = trade.getProduct();
    SwaptionSensitivity pointSens =
        productPricer.presentValueSensitivityModelParamsVolatility(product, ratesProvider, swaptionVolatilities);
    return PointSensitivities.of(pointSens);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption trade.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider, swaptionVolatilities));
  }

  /**
   * Computes the implied volatility of the swaption.
   *
   * @param swaptionTrade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the implied volatility
   */
  public double impliedVolatility(
      ResolvedSwaptionTrade swaptionTrade,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    return productPricer.impliedVolatility(swaptionTrade.getProduct(), ratesProvider, swaptionVolatilities);
  }

  /**
   * Provides the forward rate.
   * <p>
   * This is the par rate for the forward starting swap that is the underlying of the swaption.
   *
   * @param swaptionTrade  the swaption trade
   * @param ratesProvider  the rates provider
   * @return the forward rate
   */
  public double forwardRate(ResolvedSwaptionTrade swaptionTrade, RatesProvider ratesProvider) {
    return productPricer.forwardRate(swaptionTrade.getProduct(), ratesProvider);
  }

  /**
   * Calculates the current cash of the swaption trade.
   * <p>
   * Only the premium is contributing to the current cash for non-cash settle swaptions.
   * 
   * @param trade  the swaption trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedSwaptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

}
