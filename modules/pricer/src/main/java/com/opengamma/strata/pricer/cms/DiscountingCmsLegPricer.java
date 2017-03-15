/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.cms.DiscountingCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;

/**
 * Pricer for CMS legs by simple forward estimation.
 * <p>
 * This is an overly simplistic approach to CMS coupon pricer. It is provided only for testing and comparison 
 * purposes. It is not recommended to use this for valuation or risk management purposes.
 * <p>
 * This function provides the ability to price {@link ResolvedCmsLeg}. 
 * One must apply {@code resolved()} in order to price {@link CmsLeg}. 
 */
public class DiscountingCmsLegPricer {

  /**
   * The pricer for {@link CmsPeriod}.
   */
  private final DiscountingCmsPeriodPricer cmsPeriodPricer;

  /**
   * Creates an instance.
   * 
   * @param cmsPeriodPricer  the pricer for {@link CmsPeriod}
   */
  public DiscountingCmsLegPricer(DiscountingCmsPeriodPricer cmsPeriodPricer) {
    this.cmsPeriodPricer = ArgChecker.notNull(cmsPeriodPricer, "cmsPeriodPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value of CMS leg by simple forward rate estimation.
   * <p>
   * The result is returned using the payment currency of the leg.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider) {

    return cmsLeg.getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValue(cmsPeriod, ratesProvider))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  /**
   * Calculates the present value curve sensitivity of the CMS leg by simple forward rate estimation.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider) {

    return cmsLeg.getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValueSensitivity(cmsPeriod, ratesProvider))
        .reduce((p1, p2) -> p1.combinedWith(p2))
        .get();
  }

  /**
   * Calculates the current cash of the leg.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider) {

    return cmsLeg.getCmsPeriods()
        .stream()
        .filter(x -> x.getPaymentDate().equals(ratesProvider.getValuationDate()))
        .map(x -> cmsPeriodPricer.presentValue(x, ratesProvider))
        .reduce((c1, c2) -> c1.plus(c2))
        .orElse(CurrencyAmount.zero(cmsLeg.getCurrency()));
  }

}
