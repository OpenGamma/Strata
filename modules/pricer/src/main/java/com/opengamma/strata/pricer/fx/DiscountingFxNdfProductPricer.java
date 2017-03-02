/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxNdf;

/**
 * Pricer for FX non-deliverable forward (NDF) products.
 * <p>
 * This provides the ability to price an {@link ResolvedFxNdf}.
 * The product is priced using forward curves for the currency pair.
 */
public class DiscountingFxNdfProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxNdfProductPricer DEFAULT = new DiscountingFxNdfProductPricer();

  /**
   * Creates an instance.
   */
  public DiscountingFxNdfProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the NDF product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The present value is returned in the settlement currency.
   * 
   * @param ndf  the product
   * @param provider  the rates provider
   * @return the present value of the product in the settlement currency
   */
  public CurrencyAmount presentValue(ResolvedFxNdf ndf, RatesProvider provider) {
    Currency ccySettle = ndf.getSettlementCurrency();
    if (provider.getValuationDate().isAfter(ndf.getPaymentDate())) {
      return CurrencyAmount.zero(ccySettle);
    }
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    CurrencyAmount notionalSettle = ndf.getSettlementCurrencyNotional();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle, ccyOther);
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ndf.getObservation(), ccySettle);
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
    return notionalSettle.multipliedBy(dfSettle * (1d - agreedRate / forwardRate));
  }

  /**
   * Calculates the present value curve sensitivity of the NDF product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param ndf  the product
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedFxNdf ndf, RatesProvider provider) {
    if (provider.getValuationDate().isAfter(ndf.getPaymentDate())) {
      return PointSensitivities.empty();
    }
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    double notionalSettle = ndf.getSettlementNotional();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle, ccyOther);
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ndf.getObservation(), ccySettle);
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
    double ratio = agreedRate / forwardRate;
    double dscBar = (1d - ratio) * notionalSettle;
    PointSensitivityBuilder sensiDsc =
        provider.discountFactors(ccySettle).zeroRatePointSensitivity(ndf.getPaymentDate()).multipliedBy(dscBar);
    double forwardRateBar = dfSettle * notionalSettle * ratio / forwardRate;
    PointSensitivityBuilder sensiFx = provider.fxIndexRates(ndf.getIndex())
        .ratePointSensitivity(ndf.getObservation(), ccySettle).withCurrency(ccySettle).multipliedBy(forwardRateBar);
    return sensiDsc.combinedWith(sensiFx).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure by discounting each payment in its own currency.
   * 
   * @param ndf  the product
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFxNdf ndf, RatesProvider provider) {
    if (provider.getValuationDate().isAfter(ndf.getPaymentDate())) {
      return MultiCurrencyAmount.empty();
    }
    Currency ccySettle = ndf.getSettlementCurrency();
    CurrencyAmount notionalSettle = ndf.getSettlementCurrencyNotional();
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle, ccyOther);
    double dfOther = provider.discountFactor(ccyOther, ndf.getPaymentDate());
    return MultiCurrencyAmount.of(notionalSettle.multipliedBy(dfSettle))
        .plus(CurrencyAmount.of(ccyOther, -notionalSettle.getAmount() * agreedRate * dfOther));
  }

  /**
   * Calculates the current cash of the NDF product.
   * 
   * @param ndf  the product
   * @param provider  the rates provider
   * @return the current cash of the product in the settlement currency
   */
  public CurrencyAmount currentCash(ResolvedFxNdf ndf, RatesProvider provider) {
    Currency ccySettle = ndf.getSettlementCurrency();
    if (provider.getValuationDate().isEqual(ndf.getPaymentDate())) {
      Currency ccyOther = ndf.getNonDeliverableCurrency();
      CurrencyAmount notionalSettle = ndf.getSettlementCurrencyNotional();
      double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle, ccyOther);
      double rate = provider.fxIndexRates(ndf.getIndex()).rate(ndf.getObservation(), ccySettle);
      return notionalSettle.multipliedBy(1d - agreedRate / rate);
    }
    return CurrencyAmount.zero(ccySettle);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward exchange rate.
   * 
   * @param ndf  the product
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxNdf ndf, RatesProvider provider) {
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ndf.getObservation(), ccySettle);
    return FxRate.of(ccySettle, ccyOther, forwardRate);
  }

}
