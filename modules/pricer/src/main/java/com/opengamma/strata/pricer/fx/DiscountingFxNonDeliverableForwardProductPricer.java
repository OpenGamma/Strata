/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForwardProduct;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange transaction products.
 * <p>
 * This function provides the ability to price an {@link FxProduct}.
 */
public class DiscountingFxNonDeliverableForwardProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxNonDeliverableForwardProductPricer DEFAULT =
      new DiscountingFxNonDeliverableForwardProductPricer();

  /**
   * Creates an instance.
   */
  public DiscountingFxNonDeliverableForwardProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value.
   * <p>
   * The present value is returned in the settlement currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value in the settlement currency
   */
  public CurrencyAmount presentValue(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    CurrencyAmount notionalSettle = ndf.getSettlementCurrencyNotional();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle);
    LocalDate fixingDate = ndf.getIndex().calculateFixingFromMaturity(ndf.getPaymentDate());
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ccySettle, fixingDate);
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
    return notionalSettle.multipliedBy(dfSettle * (1d - agreedRate / forwardRate));
  }

  // TODO requires implementation of fx rate sensitivity to spot
  //  /**
  //   * Calculates the currency exposure.
  //   * 
  //   * @param product  the product to prices
  //   * @param provider  the rates provider
  //   * @return the currency exposure in the two natural currencies
  //   */
  //  public MultiCurrencyAmount currencyExposure(FxNonDeliverableForwardProduct product, RatesProvider provider) {
  //    ExpandedFxNonDeliverableForward ndf = product.expand();
  //    Currency ccySettle = ndf.getSettlementCurrency();
  //    Currency ccyOther = ndf.getNonDeliverableCurrency();
  //    CurrencyAmount notionalSettle = ndf.getSettlementCurrencyNotional();
  //    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle);
  //    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
  //    LocalDate fixingDate = ndf.getIndex().calculateFixingFromMaturity(ndf.getPaymentDate());
  //    double spot = provider.fxRate(ccySettle, ccyOther);
  //    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ccySettle, fixingDate);
  //    double forwardRateDelta = provider.fxIndexRates(ndf.getIndex()).rateDelta(ccySettle, fixingDate);
  //    double delta = -agreedRate * forwardRateDelta / forwardRate / forwardRate;
  //    CurrencyAmount exposureSettle =
  //        notionalSettle.multipliedBy(dfSettle * (1d - agreedRate / forwardRate - delta * spot));
  //    CurrencyAmount exposureOther =
  //        notionalSettle.multipliedBy(dfSettle * delta * spot).convertedTo(ccyOther, spot);
  //    return MultiCurrencyAmount.of(exposureOther, exposureSettle);
  //  }

  /**
   * Calculates the present value curve sensitivity.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    double notionalSettle = ndf.getSettlementNotional();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle);
    LocalDate fixingDate = ndf.getIndex().calculateFixingFromMaturity(ndf.getPaymentDate());
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ccySettle, fixingDate);
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());

    double ratio = agreedRate / forwardRate;
    double dscBar = (1d - ratio) * notionalSettle;
    PointSensitivityBuilder sensiDsc =
        provider.discountFactors(ccySettle).pointSensitivity(ndf.getPaymentDate()).multipliedBy(dscBar);
    double fxBar = dfSettle * ratio / forwardRate * notionalSettle;
    PointSensitivityBuilder sensiFx =
        provider.fxIndexRates(ndf.getIndex()).pointSensitivity(ccySettle, fixingDate).multipliedBy(fxBar);
    return sensiDsc.combinedWith(sensiFx).build();
  }

}
