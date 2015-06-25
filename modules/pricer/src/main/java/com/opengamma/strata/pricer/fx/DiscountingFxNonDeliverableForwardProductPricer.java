/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForwardProduct;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for FX non-deliverable forward (NDF) products.
 * <p>
 * This function provides the ability to price an {@link FxNonDeliverableForwardProduct}.
 * The product is priced using forward curves for the currency pair.
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
   * Calculates the present value of the NDF product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The present value is returned in the settlement currency.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value of the product in the settlement currency
   */
  public CurrencyAmount presentValue(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    if (provider.getValuationDate().isAfter(ndf.getPaymentDate())) {
      return CurrencyAmount.zero(ccySettle);
    }
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    CurrencyAmount notionalSettle = ndf.getSettlementCurrencyNotional();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle, ccyOther);
    LocalDate fixingDate = ndf.getIndex().calculateFixingFromMaturity(ndf.getPaymentDate());
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ccySettle, fixingDate);
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
    return notionalSettle.multipliedBy(dfSettle * (1d - agreedRate / forwardRate));
  }

  /**
   * Calculates the present value curve sensitivity of the NDF product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    ExpandedFxNonDeliverableForward ndf = product.expand();
    if (provider.getValuationDate().isAfter(ndf.getPaymentDate())) {
      return PointSensitivities.empty();
    }
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    double notionalSettle = ndf.getSettlementNotional();
    double agreedRate = ndf.getAgreedFxRate().fxRate(ccySettle, ccyOther);
    LocalDate fixingDate = ndf.getIndex().calculateFixingFromMaturity(ndf.getPaymentDate());
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ccySettle, fixingDate);
    double dfSettle = provider.discountFactor(ccySettle, ndf.getPaymentDate());
    double ratio = agreedRate / forwardRate;
    double dscBar = (1d - ratio) * notionalSettle;
    PointSensitivityBuilder sensiDsc =
        provider.discountFactors(ccySettle).zeroRatePointSensitivity(ndf.getPaymentDate()).multipliedBy(dscBar);
    double fxBar = dfSettle * ratio / forwardRate * notionalSettle;
    PointSensitivityBuilder sensiFx = provider.fxIndexRates(ndf.getIndex())
        .ratePointSensitivity(ccySettle, fixingDate).withCurrency(ccySettle).multipliedBy(fxBar);
    return sensiDsc.combinedWith(sensiFx).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward exchange rate.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    LocalDate fixingDate = ndf.getIndex().calculateFixingFromMaturity(ndf.getPaymentDate());
    double forwardRate = provider.fxIndexRates(ndf.getIndex()).rate(ccySettle, fixingDate);
    return FxRate.of(ccySettle, ccyOther, forwardRate);
  }

}
