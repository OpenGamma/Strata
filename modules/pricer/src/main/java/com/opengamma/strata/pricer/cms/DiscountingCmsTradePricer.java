/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.cms.ResolvedCms;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;
import com.opengamma.strata.product.swap.Swap;

/**
 * Pricer for CMS trade by simple forward estimation.
 *  <p>
 *  This is an overly simplistic approach to CMS coupon pricer. It is provided only for testing and comparison 
 *  purposes. It is not recommended to use this for valuation or risk management purposes.
 */
public class DiscountingCmsTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingCmsTradePricer DEFAULT = new DiscountingCmsTradePricer(
      DiscountingSwapProductPricer.DEFAULT, DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedCms}.
   */
  private final DiscountingCmsProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public DiscountingCmsTradePricer(DiscountingSwapProductPricer swapPricer, DiscountingPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
    this.productPricer = new DiscountingCmsProductPricer(swapPricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the CMS trade by simple forward estimation.
   * 
   * @param trade  the CMS trade
   * @param ratesProvider  the rates provider
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider) {

    MultiCurrencyAmount pvCms = productPricer.presentValue(trade.getProduct(), ratesProvider);
    if (!trade.getPremium().isPresent()) {
      return pvCms;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(trade.getPremium().get(), ratesProvider);
    return pvCms.plus(pvPremium);
  }

  /**
   * Calculates the present value curve sensitivity of the CMS trade by simple forward estimation.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to the underlying curves.
   * 
   * @param trade  the CMS trade
   * @param ratesProvider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivityBuilder pvSensiCms =
        productPricer.presentValueSensitivity(trade.getProduct(), ratesProvider);
    if (!trade.getPremium().isPresent()) {
      return pvSensiCms.build();
    }
    PointSensitivityBuilder pvSensiPremium = paymentPricer.presentValueSensitivity(trade.getPremium().get(), ratesProvider);
    return pvSensiCms.combinedWith(pvSensiPremium).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the trade.
   * 
   * @param trade  the CMS trade
   * @param ratesProvider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider) {

    MultiCurrencyAmount ceCms = productPricer.currencyExposure(trade.getProduct(), ratesProvider);
    if (!trade.getPremium().isPresent()) {
      return ceCms;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(trade.getPremium().get(), ratesProvider);
    return ceCms.plus(pvPremium);
  }

  /**
   * Calculates the current cash of the trade.
   * 
   * @param trade  the CMS trade
   * @param ratesProvider  the rates provider
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider) {

    MultiCurrencyAmount ccCms = productPricer.currentCash(trade.getProduct(), ratesProvider);
    if (!trade.getPremium().isPresent()) {
      return ccCms;
    }
    Payment premium = trade.getPremium().get();
    if (premium.getDate().equals(ratesProvider.getValuationDate())) {
      ccCms = ccCms.plus(premium.getCurrency(), premium.getAmount());
    }
    return ccCms;
  }

}
