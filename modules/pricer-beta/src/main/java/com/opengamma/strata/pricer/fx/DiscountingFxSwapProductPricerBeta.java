/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.fx.ExpandedFxSwap;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.fx.FxProduct;
import com.opengamma.strata.finance.fx.FxSwapProduct;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange swap transaction products.
 * <p>
 * This function provides the ability to price an {@link FxSwapProduct}.
 */
public class DiscountingFxSwapProductPricerBeta {
  // copied/modified from ForexSwapDiscountingMethod
  // TODO: check valuation date vs payment date (pv of zero?)

  /**
   * Default implementation.
   */
  public static final DiscountingFxSwapProductPricerBeta DEFAULT =
      new DiscountingFxSwapProductPricerBeta(DiscountingFxProductPricerBeta.DEFAULT);

  /**
   * Underlying single FX pricer.
   */
  private final DiscountingFxProductPricerBeta fxPricer;

  /**
   * Creates an instance.
   * 
   * @param fxPricer  the pricer for {@link FxProduct}
   */
  public DiscountingFxSwapProductPricerBeta(
      DiscountingFxProductPricerBeta fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value.
   * <p>
   * This discounts each payment on each leg in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    if (provider.getValuationDate().isAfter(fx.getFarLeg().getValueDate())) {
      return MultiCurrencyAmount.empty();
    }
    MultiCurrencyAmount farPv = fxPricer.presentValue(fx.getFarLeg(), provider);
    if (provider.getValuationDate().isAfter(fx.getNearLeg().getValueDate())) {
      return farPv;
    }
    MultiCurrencyAmount nearPv = fxPricer.presentValue(fx.getNearLeg(), provider);
    return nearPv.plus(farPv);
  }

  /**
   * Calculates the currency exposure.
   * <p>
   * This discounts each payment on each leg in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(FxSwapProduct product, RatesProvider provider) {
    return presentValue(product, provider);
  }

  /**
   * Calculates the par spread is the spread that should be added to the forex forward points to have a zero value.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    if (provider.getValuationDate().isAfter(fx.getNearLeg().getValueDate())) {
      return fxPricer.parSpread(fx.getFarLeg(), provider);
    }
    FxPayment basePaymentNear = fx.getNearLeg().getBaseCurrencyPayment();
    FxPayment counterPaymentNear = fx.getNearLeg().getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(fx, provider);
    double pvCounterCcy = pv.convertedTo(counterPaymentNear.getCurrency(), provider).getAmount();
    // TODO: is basePaymentNear.getCurrency() correct?
    double dfEnd = provider.discountFactor(basePaymentNear.getCurrency(), fx.getFarLeg().getValueDate());
    double notionalBaseCcy = basePaymentNear.getAmount();
    return -pvCounterCcy / (notionalBaseCcy * dfEnd);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(FxSwapProduct product, RatesProvider provider) {
    ExpandedFxSwap fx = product.expand();
    PointSensitivities nearSens = fxPricer.presentValueSensitivity(fx.getNearLeg(), provider);
    PointSensitivities farSens = fxPricer.presentValueSensitivity(fx.getFarLeg(), provider);
    return nearSens.combinedWith(farSens);
  }

}
