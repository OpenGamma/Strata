/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.FxVanillaOption;
import com.opengamma.strata.product.fx.FxVanillaOptionTrade;

/**
 * Pricer for foreign exchange vanilla option trades with a lognormal model.
 * <p>
 * This function provides the ability to price an {@link FxVanillaOptionTrade}.
 */
public class BlackFxVanillaOptionTradePricer {

  /**
   * Default implementation. 
   */
  public static final BlackFxVanillaOptionTradePricer DEFAULT = new BlackFxVanillaOptionTradePricer();

  /**
   * Pricer for {@link FxVanillaOption}.
   */
  private static final BlackFxVanillaOptionProductPricer PRICER_PRODUCT = BlackFxVanillaOptionProductPricer.DEFAULT;
  /** 
   * Pricer for {@link Payment} which is used to described the premium. 
   */
  private static final DiscountingPaymentPricer PRICER_PREMIUM = DiscountingPaymentPricer.DEFAULT;

  /**
   * Calculates the present value of the foreign exchange vanilla option trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the option trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the trade
   */
  public MultiCurrencyAmount presentValue(
      FxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    FxVanillaOption product = trade.getProduct();
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(product, ratesProvider, volatilityProvider);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, ratesProvider);
    return MultiCurrencyAmount.of(pvProduct, pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   * 
   * @param trade  the option trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      FxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    FxVanillaOption product = trade.getProduct();
    PointSensitivities pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivity(product, ratesProvider, volatilityProvider);
    Payment premium = trade.getPremium();
    PointSensitivities pvcsPremium = PRICER_PREMIUM.presentValueSensitivity(premium, ratesProvider).build();
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * 
   * @param trade  the option trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityBlackVolatility(
      FxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    FxVanillaOption product = trade.getProduct();
    return PRICER_PRODUCT.presentValueSensitivityBlackVolatility(product, ratesProvider, volatilityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the foreign exchange vanilla option trade.
   * 
   * @param trade  the option trade to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      FxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, ratesProvider);
    FxVanillaOption product = trade.getProduct();
    return PRICER_PRODUCT.currencyExposure(product, ratesProvider, volatilityProvider).plus(pvPremium);
  }

  /**
   * Calculates the current of the foreign exchange vanilla option trade.
   * 
   * @param trade  the option trade to price
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(FxVanillaOptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }
}
