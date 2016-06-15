/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.cms.DiscountingCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.ResolvedCms;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 *  Computes the price of a CMS product by simple forward estimation.
 *  <p>
 *  This is an overly simplistic approach to CMS coupon pricer. It is provided only for testing and comparison 
 *  purposes. It is not recommended to use this for valuation or risk management purposes.
 */
public class DiscountingCmsProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingCmsProductPricer DEFAULT = new DiscountingCmsProductPricer(
      DiscountingSwapProductPricer.DEFAULT);

  /** The pricer for {@link SwapLeg}. */
  private final DiscountingSwapProductPricer swapPricer;
  /** The pricer for {@link CmsLeg}. */
  private final DiscountingCmsLegPricer cmsLegPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public DiscountingCmsProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
    this.cmsLegPricer = new DiscountingCmsLegPricer(new DiscountingCmsPeriodPricer(swapPricer));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the CMS product by simple forward estimation.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedCms cms,
      RatesProvider ratesProvider) {

    CurrencyAmount pvCmsLeg = cmsLegPricer.presentValue(cms.getCmsLeg(), ratesProvider);
    if (!cms.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(pvCmsLeg);
    }
    CurrencyAmount pvPayLeg = swapPricer.getLegPricer().presentValue(cms.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(pvCmsLeg).plus(pvPayLeg);
  }

  /**
   * Calculates the present value curve sensitivity of the CMS product by simple forward estimation.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to the underlying curves.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCms cms,
      RatesProvider ratesProvider) {

    PointSensitivityBuilder pvSensiCmsLeg =
        cmsLegPricer.presentValueSensitivity(cms.getCmsLeg(), ratesProvider);
    if (!cms.getPayLeg().isPresent()) {
      return pvSensiCmsLeg;
    }
    PointSensitivityBuilder pvSensiPayLeg =
        swapPricer.getLegPricer().presentValueSensitivity(cms.getPayLeg().get(), ratesProvider);
    return pvSensiCmsLeg.combinedWith(pvSensiPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the product.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCms cms,
      RatesProvider ratesProvider) {

    return presentValue(cms, ratesProvider);
  }

  /**
   * Calculates the current cash of the product.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedCms cms,
      RatesProvider ratesProvider) {

    CurrencyAmount ccCmsLeg = cmsLegPricer.currentCash(cms.getCmsLeg(), ratesProvider);
    if (!cms.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ccCmsLeg);
    }
    CurrencyAmount ccPayLeg = swapPricer.getLegPricer().currentCash(cms.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(ccPayLeg).plus(ccCmsLeg);
  }

}
