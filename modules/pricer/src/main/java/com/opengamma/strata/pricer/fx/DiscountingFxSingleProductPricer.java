/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

/**
 * Pricer for foreign exchange transaction products.
 * <p>
 * This provides the ability to price an {@link ResolvedFxSingle}.
 */
public class DiscountingFxSingleProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxSingleProductPricer DEFAULT = new DiscountingFxSingleProductPricer(
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public DiscountingFxSingleProductPricer(
      DiscountingPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX product by discounting each payment in its own currency.
   * 
   * @param fx  the product
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(ResolvedFxSingle fx, RatesProvider provider) {
    if (provider.getValuationDate().isAfter(fx.getPaymentDate())) {
      return MultiCurrencyAmount.empty();
    }
    CurrencyAmount pv1 = paymentPricer.presentValue(fx.getBaseCurrencyPayment(), provider);
    CurrencyAmount pv2 = paymentPricer.presentValue(fx.getCounterCurrencyPayment(), provider);
    return MultiCurrencyAmount.of(pv1, pv2);
  }

  /**
   * Calculates the present value curve sensitivity of the FX product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param fx  the product
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedFxSingle fx, RatesProvider provider) {
    if (provider.getValuationDate().isAfter(fx.getPaymentDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivityBuilder pvcs1 = paymentPricer.presentValueSensitivity(fx.getBaseCurrencyPayment(), provider);
    PointSensitivityBuilder pvcs2 = paymentPricer.presentValueSensitivity(fx.getCounterCurrencyPayment(), provider);
    return pvcs1.combinedWith(pvcs2).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread.
   * <p>
   * This is the spread that should be added to the FX points to have a zero value.
   * 
   * @param fx  the product
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(ResolvedFxSingle fx, RatesProvider provider) {
    Payment basePayment = fx.getBaseCurrencyPayment();
    Payment counterPayment = fx.getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(fx, provider);
    double pvCounterCcy = pv.convertedTo(counterPayment.getCurrency(), provider).getAmount();
    double dfEnd = provider.discountFactor(counterPayment.getCurrency(), fx.getPaymentDate());
    double notionalBaseCcy = basePayment.getAmount();
    return pvCounterCcy / (notionalBaseCcy * dfEnd);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure by discounting each payment in its own currency.
   * 
   * @param product  the product
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFxSingle product, RatesProvider provider) {
    return presentValue(product, provider);
  }

  /**
   * Calculates the current cash.
   * 
   * @param fx  the product
   * @param valuationDate  the valuation date
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(ResolvedFxSingle fx, LocalDate valuationDate) {
    if (valuationDate.isEqual(fx.getPaymentDate())) {
      return MultiCurrencyAmount.of(fx.getBaseCurrencyPayment().getValue(), fx.getCounterCurrencyPayment().getValue());
    }
    return MultiCurrencyAmount.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward exchange rate.
   * 
   * @param fx  the product
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxSingle fx, RatesProvider provider) {
    FxForwardRates fxForwardRates = provider.fxForwardRates(fx.getCurrencyPair());
    Payment basePayment = fx.getBaseCurrencyPayment();
    Payment counterPayment = fx.getCounterCurrencyPayment();
    double forwardRate = fxForwardRates.rate(basePayment.getCurrency(), fx.getPaymentDate());
    return FxRate.of(basePayment.getCurrency(), counterPayment.getCurrency(), forwardRate);
  }

  /**
   * Calculates the forward exchange rate point sensitivity.
   * <p>
   * The returned value is based on the direction of the FX product.
   * 
   * @param fx  the product
   * @param provider  the rates provider
   * @return the point sensitivity
   */
  public PointSensitivityBuilder forwardFxRatePointSensitivity(ResolvedFxSingle fx, RatesProvider provider) {
    FxForwardRates fxForwardRates = provider.fxForwardRates(fx.getCurrencyPair());
    PointSensitivityBuilder forwardFxRatePointSensitivity = fxForwardRates.ratePointSensitivity(
        fx.getReceiveCurrencyAmount().getCurrency(), fx.getPaymentDate());
    return forwardFxRatePointSensitivity;
  }

  /**
   * Calculates the sensitivity of the forward exchange rate to the spot rate.
   * <p>
   * The returned value is based on the direction of the FX product.
   * 
   * @param fx  the product
   * @param provider  the rates provider
   * @return the sensitivity to spot
   */
  public double forwardFxRateSpotSensitivity(ResolvedFxSingle fx, RatesProvider provider) {
    FxForwardRates fxForwardRates = provider.fxForwardRates(fx.getCurrencyPair());
    double forwardRateSpotSensitivity = fxForwardRates.rateFxSpotSensitivity(
        fx.getReceiveCurrencyAmount().getCurrency(), fx.getPaymentDate());
    return forwardRateSpotSensitivity;
  }

}
