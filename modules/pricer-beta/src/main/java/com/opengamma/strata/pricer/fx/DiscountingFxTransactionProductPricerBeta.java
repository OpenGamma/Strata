/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.fx.FxTransaction;
import com.opengamma.strata.finance.fx.FxTransactionProduct;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer for for foreign exchange transaction products.
 * <p>
 * This function provides the ability to price an {@link FxTransaction}.
 */
public class DiscountingFxTransactionProductPricerBeta {
  // copied/modified from ForexDiscountingMethod
  // TODO: check valuation date vs payment date (pv of zero?)

  /**
   * Default implementation.
   */
  public static final DiscountingFxTransactionProductPricerBeta DEFAULT = new DiscountingFxTransactionProductPricerBeta();

  /**
   * Creates an instance.
   */
  public DiscountingFxTransactionProductPricerBeta() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value by discounting each payment in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(FxTransactionProduct product, RatesProvider provider) {
    FxTransaction fx = product.expand();
    CurrencyAmount pv1 = presentValue(fx.getBaseCurrencyPayment(), provider);
    CurrencyAmount pv2 = presentValue(fx.getCounterCurrencyPayment(), provider);
    return MultiCurrencyAmount.of(pv1, pv2);
  }

  // from PaymentFixedDiscountingMethod
  private CurrencyAmount presentValue(FxPayment payment, RatesProvider provider) {
    return payment.getValue().multipliedBy(provider.discountFactor(payment.getCurrency(), payment.getDate()));
  }

  /**
   * Computes the currency exposure by discounting each payment in its own currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(FxTransactionProduct product, RatesProvider provider) {
    return presentValue(product, provider);
  }

  /**
   * The par spread is the spread that should be added to the forex forward points to have a zero value.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(FxTransactionProduct product, RatesProvider provider) {
    FxTransaction fx = product.expand();
    FxPayment basePayment = fx.getBaseCurrencyPayment();
    FxPayment counterPayment = fx.getCounterCurrencyPayment();
    double pv2 = provider.fxConvert(presentValue(fx, provider), counterPayment.getCurrency()).getAmount();
    // TODO: counterPayment.date or basePayment.date?
    double dfEnd = provider.discountFactor(counterPayment.getCurrency(), counterPayment.getDate());
    double notional1 = basePayment.getAmount();
    return pv2 / (notional1 * dfEnd);
  }

  /**
   * Computes the forward exchange rate associated to the FxExchangeProduct instrument (1 Cyy1 = fwd Cyy2).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the forward rate
   */
  public double forwardFxRate(FxTransactionProduct product, RatesProvider provider) {
    FxTransaction fx = product.expand();
    FxPayment basePayment = fx.getBaseCurrencyPayment();
    FxPayment counterPayment = fx.getCounterCurrencyPayment();
    // TODO: domestic/foreign vs base/counter?
    double dfDomestic = provider.discountFactor(counterPayment.getCurrency(), counterPayment.getDate());
    double dfForeign = provider.discountFactor(basePayment.getCurrency(), basePayment.getDate());
    double spot = provider.fxRate(basePayment.getCurrency(), counterPayment.getCurrency());
    return spot * dfForeign / dfDomestic;
  }

  //-------------------------------------------------------------------------
  /**
   * Compute the present value sensitivity to rates of a forex transaction.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(FxTransactionProduct product, RatesProvider provider) {
    FxTransaction fx = product.expand();
    PointSensitivityBuilder pvcs1 = presentValueSensitivity(fx.getBaseCurrencyPayment(), provider);
    PointSensitivityBuilder pvcs2 = presentValueSensitivity(fx.getCounterCurrencyPayment(), provider);
    return pvcs1.combinedWith(pvcs2).build();
  }

  // from PaymentFixedDiscountingMethod
  private PointSensitivityBuilder presentValueSensitivity(FxPayment payment, final RatesProvider provider) {
    return provider.discountFactorZeroRateSensitivity(payment.getCurrency(), payment.getDate())
        .multipliedBy(payment.getAmount());
  }

//  /**
//   * Computes the par spread curve sensitivity.
//   * 
//   * @param product  the product to price
//   * @param provider  the rates provider
//   * @return the par spread sensitivity
//   */
//  public MulticurveSensitivity parSpreadCurveSensitivity(FxExchangeProduct product, RatesProvider provider) {
//    FxExchange fx = product.expand();
//    FxPayment basePayment = fx.getBaseCurrencyPayment();
//    FxPayment counterPayment = fx.getCounterCurrencyPayment();
//    Currency counterCcy = counterPayment.getCurrency();
//    double payTime = provider.relativeTime(counterPayment.getDate());
//    double pv2 = provider.fxConvert(presentValue(fx, provider), counterCcy).getAmount();
//    double dfEnd = provider.discountFactor(counterCcy, counterPayment.getDate());
//    double notional1 = basePayment.getAmount();
//    // Backward sweep
//    double spreadBar = 1.0;
//    double dfEndBar = -pv2 / (notional1 * dfEnd * dfEnd) * spreadBar;
//    double pv2Bar = spreadBar / (notional1 * dfEnd);
//    PointSensitivities pv2DrMC = presentValueSensitivity(fx, provider);
//    // TODO: this converts sensitivity to a single currency
//    MulticurveSensitivity pv2Dr = pv2DrMC.converted(counterCcy, provider.getFxRates()).getSensitivity(counterCcy);
//
//    List<DoublesPair> list = new ArrayList<>();
//    list.add(DoublesPair.of(payTime, -payTime * dfEnd * dfEndBar));
//    Map<String, List<DoublesPair>> result = new HashMap<>();
//    result.put(provider.getName(counterCcy), list);
//    MulticurveSensitivity dfEndDr = MulticurveSensitivity.ofYieldDiscounting(result);
//    return pv2Dr.multipliedBy(pv2Bar).plus(dfEndDr);
//  }

}
