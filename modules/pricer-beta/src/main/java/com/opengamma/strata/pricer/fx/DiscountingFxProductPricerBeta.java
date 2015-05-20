/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.fx.FxProduct;
import com.opengamma.strata.market.curve.DiscountFactors;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange transaction products.
 * <p>
 * This function provides the ability to price an {@link FxProduct}.
 */
public class DiscountingFxProductPricerBeta {
  // copied/modified from ForexDiscountingMethod
  // TODO: check valuation date vs payment date (pv of zero?)

  /**
   * Default implementation.
   */
  public static final DiscountingFxProductPricerBeta DEFAULT = new DiscountingFxProductPricerBeta();

  /**
   * Creates an instance.
   */
  public DiscountingFxProductPricerBeta() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value by discounting each payment in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    CurrencyAmount pv1 = presentValue(fx.getBaseCurrencyPayment(), provider);
    CurrencyAmount pv2 = presentValue(fx.getCounterCurrencyPayment(), provider);
    return MultiCurrencyAmount.of(pv1, pv2);
  }

  // from PaymentFixedDiscountingMethod
  public CurrencyAmount presentValue(FxPayment payment, RatesProvider provider) {
    return payment.getValue().multipliedBy(provider.discountFactor(payment.getCurrency(), payment.getDate()));
  }

  /**
   * Computes the currency exposure by discounting each payment in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(FxProduct product, RatesProvider provider) {
    return presentValue(product, provider);
  }

  /**
   * The par spread is the spread that should be added to the forex forward points to have a zero value.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    FxPayment basePayment = fx.getBaseCurrencyPayment();
    FxPayment counterPayment = fx.getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(fx, provider);
    double pvCounterCcy = pv.convertedTo(counterPayment.getCurrency(), provider).getAmount();
    double dfEnd = provider.discountFactor(counterPayment.getCurrency(), fx.getValueDate());
    double notionalBaseCcy = basePayment.getAmount();
    return pvCounterCcy / (notionalBaseCcy * dfEnd);
  }

  /**
   * Computes the forward exchange rate.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    FxPayment basePayment = fx.getBaseCurrencyPayment();
    FxPayment counterPayment = fx.getCounterCurrencyPayment();
    // TODO: domestic/foreign vs base/counter?
    double dfDomestic = provider.discountFactor(counterPayment.getCurrency(), counterPayment.getDate());
    double dfForeign = provider.discountFactor(basePayment.getCurrency(), basePayment.getDate());
    double spot = provider.fxRate(basePayment.getCurrency(), counterPayment.getCurrency());
    return FxRate.of(basePayment.getCurrency(), counterPayment.getCurrency(), spot * dfForeign / dfDomestic);
  }

  //-------------------------------------------------------------------------
  /**
   * Compute the present value sensitivity.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    PointSensitivityBuilder pvcs1 = presentValueSensitivity(fx.getBaseCurrencyPayment(), provider);
    PointSensitivityBuilder pvcs2 = presentValueSensitivity(fx.getCounterCurrencyPayment(), provider);
    return pvcs1.combinedWith(pvcs2).build();
  }

  // from PaymentFixedDiscountingMethod
  public PointSensitivityBuilder presentValueSensitivity(FxPayment payment, final RatesProvider provider) {
    DiscountFactors discountFactors = provider.discountFactors(payment.getCurrency());
    return discountFactors.pointSensitivity(payment.getDate())
        .multipliedBy(payment.getAmount());
  }

}
