/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.FxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOptionTrade;

/**
 * Pricer for FX barrier option trades in Black-Scholes world.
 * <p>
 * This function provides the ability to price an {@link ResolvedFxSingleBarrierOptionTrade}.
 */
public class BlackFxSingleBarrierOptionTradePricer {

  /**
   * Default implementation. 
   */
  public static final BlackFxSingleBarrierOptionTradePricer DEFAULT = new BlackFxSingleBarrierOptionTradePricer();

  /**
   * Pricer for {@link FxSingleBarrierOption}.
   */
  private static final BlackFxSingleBarrierOptionProductPricer PRICER_PRODUCT = BlackFxSingleBarrierOptionProductPricer.DEFAULT;
  /** 
   * Pricer for {@link Payment} which is used to described the premium. 
   */
  private static final DiscountingPaymentPricer PRICER_PREMIUM = DiscountingPaymentPricer.DEFAULT;

  /**
   * Calculates the present value of the FX barrier option trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the trade
   */
  public MultiCurrencyAmount presentValue(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    ResolvedFxSingleBarrierOption product = trade.getProduct();
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(product, ratesProvider, volatilityProvider);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, ratesProvider);
    return MultiCurrencyAmount.of(pvProduct, pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX barrier option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    ResolvedFxSingleBarrierOption product = trade.getProduct();
    PointSensitivityBuilder pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivity(product, ratesProvider, volatilityProvider);
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = PRICER_PREMIUM.presentValueSensitivity(premium, ratesProvider);
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityBlackVolatility(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    ResolvedFxSingleBarrierOption product = trade.getProduct();
    return PRICER_PRODUCT.presentValueSensitivityVolatility(product, ratesProvider, volatilityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX barrier option trade.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, ratesProvider);
    ResolvedFxSingleBarrierOption product = trade.getProduct();
    return PRICER_PRODUCT.currencyExposure(product, ratesProvider, volatilityProvider).plus(pvPremium);
  }

  /**
   * Calculates the current of the FX barrier option trade.
   * 
   * @param trade  the option trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedFxSingleBarrierOptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

}
