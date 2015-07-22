/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.fx.FxProduct;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.FxForwardRates;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange transaction products.
 * <p>
 * This function provides the ability to price an {@link FxProduct}.
 */
public class DiscountingFxProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxProductPricer DEFAULT = new DiscountingFxProductPricer(
      DiscountingFxPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link FxPayment}.
   */
  private final DiscountingFxPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPricer  the pricer for {@link FxPayment}
   */
  public DiscountingFxProductPricer(
      DiscountingFxPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value of the FX product by discounting each payment in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    if (provider.getValuationDate().isAfter(fx.getPaymentDate())) {
      return MultiCurrencyAmount.empty();
    }
    CurrencyAmount pv1 = paymentPricer.presentValue(fx.getBaseCurrencyPayment(), provider);
    CurrencyAmount pv2 = paymentPricer.presentValue(fx.getCounterCurrencyPayment(), provider);
    return MultiCurrencyAmount.of(pv1, pv2);
  }

  /**
   * Compute the present value curve sensitivity of the FX product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    if (provider.getValuationDate().isAfter(fx.getPaymentDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivityBuilder pvcs1 = paymentPricer.presentValueSensitivity(fx.getBaseCurrencyPayment(), provider);
    PointSensitivityBuilder pvcs2 = paymentPricer.presentValueSensitivity(fx.getCounterCurrencyPayment(), provider);
    return pvcs1.combinedWith(pvcs2).build();
  }

  //-------------------------------------------------------------------------
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
   * The par spread is the spread that should be added to the FX points to have a zero value.
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
    double dfEnd = provider.discountFactor(counterPayment.getCurrency(), fx.getPaymentDate());
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
    FxForwardRates fxForwardRates = provider.fxForwardRates(fx.getCurrencyPair());
    FxPayment basePayment = fx.getBaseCurrencyPayment();
    FxPayment counterPayment = fx.getCounterCurrencyPayment();
    double forwardRate = fxForwardRates.rate(basePayment.getCurrency(), fx.getPaymentDate());
    return FxRate.of(basePayment.getCurrency(), counterPayment.getCurrency(), forwardRate);
  }

  /**
   * Computes the forward exchange rate point sensitivity.
   * <p>
   * The returned value is based on the direction of the FX product.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the point sensitivity
   */
  public PointSensitivityBuilder forwardFxRatePointSensitivity(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    FxForwardRates fxForwardRates = provider.fxForwardRates(fx.getCurrencyPair());
    PointSensitivityBuilder forwardFxRatePointSensitivity = fxForwardRates.ratePointSensitivity(
        fx.getReceiveCurrencyAmount().getCurrency(), fx.getPaymentDate());
    return forwardFxRatePointSensitivity;
  }

  /**
   * Computes the sensitivity of the forward exchange rate to the spot rate.
   * <p>
   * The returned value is based on the direction of the FX product.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the sensitivity to spot
   */
  public double forwardFxRateSpotSensitivity(FxProduct product, RatesProvider provider) {
    ExpandedFx fx = product.expand();
    FxForwardRates fxForwardRates = provider.fxForwardRates(fx.getCurrencyPair());
    double forwardRateSpotSensitivity = fxForwardRates.rateFxSpotSensitivity(
        fx.getReceiveCurrencyAmount().getCurrency(), fx.getPaymentDate());
    return forwardRateSpotSensitivity;
  }
}
