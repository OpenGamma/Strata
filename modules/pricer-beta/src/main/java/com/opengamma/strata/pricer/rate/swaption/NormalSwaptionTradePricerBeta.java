/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.rate.swaption.Swaption;
import com.opengamma.strata.finance.rate.swaption.SwaptionTrade;
import com.opengamma.strata.pricer.fx.DiscountingFxProductPricerBeta;
import com.opengamma.strata.pricer.provider.NormalVolatilitySwaptionProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for swaption trade in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption should have a fixed leg on which the forward rate is computed. The underlying swap
 * should be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap conventions. The volatilities from the provider
 * are taken as such.
 * <p>
 * The present value and sensitivities of the premium, if in the future, are also taken into account.
 */
public class NormalSwaptionTradePricerBeta {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionTradePricerBeta DEFAULT = new NormalSwaptionTradePricerBeta();

  /** Pricer for {@link Swaption}. */
  private static final NormalSwaptionProductPricerBeta PRICER_PRODUCT = NormalSwaptionProductPricerBeta.DEFAULT;
  /** Pricer for {@link FxPayment} which is used to described the premium. **/
  private static final DiscountingFxProductPricerBeta PRICER_PREMIUM = DiscountingFxProductPricerBeta.DEFAULT;
  
  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param tradeSwaption  the swaption trade to price
   * @param rates  the rates provider
   * @param volatilities  the normal volatility parameters
   * @return the present value of the swap product
   */
  public CurrencyAmount presentValue(SwaptionTrade tradeSwaption, RatesProvider rates, 
      NormalVolatilitySwaptionProvider volatilities) {
    Swaption product = tradeSwaption.getProduct();
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(product, rates, volatilities);
    FxPayment premium = tradeSwaption.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, rates);
    return pvProduct.plus(pvPremium);
  }

}
