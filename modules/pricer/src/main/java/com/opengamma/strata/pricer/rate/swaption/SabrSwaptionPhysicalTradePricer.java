/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.finance.rate.swaption.Swaption;
import com.opengamma.strata.finance.rate.swaption.SwaptionTrade;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for swaption trade with physical settlement in SABR model.
 * <p>
 * The swap underlying the swaption should have a fixed leg on which the forward rate is computed. 
 * The underlying swap should be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap conventions. The volatilities from the provider
 * are taken as such.
 * <p>
 * The present value and sensitivities of the premium, if in the future, are also taken into account.
 */
public class SabrSwaptionPhysicalTradePricer {

  /**
   * Default implementation.
   */
  public static final SabrSwaptionPhysicalTradePricer DEFAULT = new SabrSwaptionPhysicalTradePricer();
  /** 
   * Pricer for {@link Swaption}. 
   */
  private static final SabrSwaptionPhysicalProductPricer PRICER_PRODUCT =
      SabrSwaptionPhysicalProductPricer.DEFAULT;
  /** 
   * Pricer for {@link Payment} which is used to described the premium. 
   */
  private static final DiscountingPaymentPricer PRICER_PREMIUM = DiscountingPaymentPricer.DEFAULT;

  /**
   * Private constructor. 
   * <p>
   * Use {@link #DEFAULT} for the default implementation.
   */
  private SabrSwaptionPhysicalTradePricer() {
  }

  /**
   * Calculates the present value of the swaption trade.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the swap product
   */
  public CurrencyAmount presentValue(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrVolatilitySwaptionProvider volatilityProvider) {

    Swaption product = trade.getProduct();
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(product, ratesProvider, volatilityProvider);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, ratesProvider);
    return pvProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption trade
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrVolatilitySwaptionProvider volatilityProvider) {

    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider, volatilityProvider));
  }

  /**
   * Calculates the current of the swaption trade.
   * <p>
   * Only the premium is contributing to the current cash for non-cash settle swaptions.
   * 
   * @param trade  the swaption trade to price
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(SwaptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0.0);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value curve sensitivity of the swap trade
   */
  public PointSensitivityBuilder presentValueSensitivity(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrVolatilitySwaptionProvider volatilityProvider) {

    Swaption product = trade.getProduct();
    PointSensitivityBuilder pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivity(product, ratesProvider, volatilityProvider);
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = PRICER_PREMIUM.presentValueSensitivity(premium, ratesProvider);
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the SABR model parameters of the swaption trade.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the sensitivity to the SABR model parameters 
   */
  public SwaptionSabrSensitivity presentValueSensitivitySabrParameter(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrVolatilitySwaptionProvider volatilityProvider) {

    Swaption product = trade.getProduct();
    return PRICER_PRODUCT.presentValueSensitivitySabrParameter(product, ratesProvider, volatilityProvider);
  }

}
