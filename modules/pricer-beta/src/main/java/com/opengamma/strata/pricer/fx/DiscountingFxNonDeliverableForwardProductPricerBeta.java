/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForwardProduct;
import com.opengamma.strata.finance.fx.FxProduct;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange transaction products.
 * <p>
 * This function provides the ability to price an {@link FxProduct}.
 */
public class DiscountingFxNonDeliverableForwardProductPricerBeta {
  // copied/modified from ForexNonDeliverableForwardDiscountingMethod
  // TODO: check valuation date vs payment date (pv of zero?)

  /**
   * Default implementation.
   */
  public static final DiscountingFxNonDeliverableForwardProductPricerBeta DEFAULT =
      new DiscountingFxNonDeliverableForwardProductPricerBeta();

  /**
   * Creates an instance.
   */
  public DiscountingFxNonDeliverableForwardProductPricerBeta() {
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
    // TODO: use the FxIndex, not the spot rate
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    double notionalSettle = ndf.getSettlementCurrencyNotional().getAmount();
    double agreedRateSettleToOther = ndf.getAgreedFxRate();

    double dfSettle = provider.discountFactor(ccySettle, ndf.getValueDate());
    double dfOther = provider.discountFactor(ccyOther, ndf.getValueDate());
    double spot = provider.fxRate(ccySettle, ccyOther);
    double pv2 = notionalSettle * (dfSettle - agreedRateSettleToOther / spot * dfOther);
    return CurrencyAmount.of(ccySettle, pv2);
  }

  /**
   * Calculates the currency exposure.
   * 
   * @param product  the product to prices
   * @param provider  the rates provider
   * @return the currency exposure in the two natural currencies
   */
  public MultiCurrencyAmount currencyExposure(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    // TODO: use the FxIndex, not the spot rate
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();

    double dfSettle = provider.discountFactor(ccySettle, ndf.getValueDate());
    double dfOther = provider.discountFactor(ccyOther, ndf.getValueDate());
    CurrencyAmount pvSettle = ndf.getSettlementCurrencyNotional().multipliedBy(dfSettle);
    CurrencyAmount pvOther = ndf.getNonDeliverableCurrencyNotional().multipliedBy(dfOther);
    return MultiCurrencyAmount.of(pvOther, pvSettle);
  }

  /**
   * Calculates the forward exchange rate.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    // TODO: use the FxIndex, not the spot rate
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();

    double dfDelivery = provider.discountFactor(ccySettle, ndf.getValueDate());
    double dfNonDelivery = provider.discountFactor(ccyOther, ndf.getValueDate());
    double spot = provider.fxRate(ccySettle, ccyOther);
    return FxRate.of(ccySettle, ccyOther, spot * dfDelivery / dfNonDelivery);
  }

  /**
   * Calculates the present value curve sensitivity.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(FxNonDeliverableForwardProduct product, RatesProvider provider) {
    // TODO: use the FxIndex, not the spot rate
    ExpandedFxNonDeliverableForward ndf = product.expand();
    Currency ccySettle = ndf.getSettlementCurrency();
    Currency ccyOther = ndf.getNonDeliverableCurrency();
    double notionalSettle = ndf.getSettlementCurrencyNotional().getAmount();
    double agreedRateSettleToOther = ndf.getAgreedFxRate();

    double spot = provider.fxRate(ccySettle, ccyOther);
    // Backward sweep
    double pvBar = 1.0;
    double dfSettleBar = notionalSettle * pvBar;
    double dfOtherBar = -notionalSettle * agreedRateSettleToOther / spot * pvBar;
    // TODO: check this
    PointSensitivityBuilder sensSettle = provider.discountFactors(ccySettle).pointSensitivity(ndf.getValueDate())
        .multipliedBy(dfSettleBar);
    PointSensitivityBuilder sensOther = provider.discountFactors(ccyOther).pointSensitivity(ndf.getValueDate())
        .multipliedBy(dfOtherBar).withCurrency(ccySettle);
    return sensSettle.combinedWith(sensOther).build();
  }

}
