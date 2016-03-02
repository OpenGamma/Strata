/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivities;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.pricer.impl.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;

/**
 * Pricer for for CMS legs by swaption replication on a SABR formula with extrapolation.
 * <p>
 * This function provides the ability to price {@link ResolvedCmsLeg}. 
 * One must apply {@code expand()} in order to price {@link CmsLeg}. 
 */
public class SabrExtrapolationReplicationCmsLegPricer {
  
  /**
   * The pricer for {@link CmsPeriod}.
   */
  private final SabrExtrapolationReplicationCmsPeriodPricer cmsPeriodPricer;

  /**
   * Creates an instance.
   * 
   * @param cmsPeriodPricer  the pricer for {@link CmsPeriod}
   */
  public SabrExtrapolationReplicationCmsLegPricer(SabrExtrapolationReplicationCmsPeriodPricer cmsPeriodPricer) {
    this.cmsPeriodPricer = ArgChecker.notNull(cmsPeriodPricer, "cmsPeriodPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the CMS leg.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    validate(ratesProvider, swaptionVolatilities);
    return cmsLeg.getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValue(cmsPeriod, ratesProvider, swaptionVolatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  /**
   * Calculates the present value curve sensitivity of the CMS leg.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    validate(ratesProvider, swaptionVolatilities);
    return cmsLeg.getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValueSensitivity(cmsPeriod, ratesProvider, swaptionVolatilities))
        .reduce((p1, p2) -> p1.combinedWith(p2))
        .get();
  }

  /**
   * Calculates the present value sensitivity to the SABR model parameters.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public SwaptionSabrSensitivities presentValueSensitivitySabrParameter(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    validate(ratesProvider, swaptionVolatilities);
    List<SwaptionSabrSensitivity> sensitivities = cmsLeg
        .getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValueSensitivitySabrParameter(cmsPeriod, ratesProvider, swaptionVolatilities))
        .collect(Collectors.toList());
    return SwaptionSabrSensitivities.of(sensitivities).normalize();
  }

  /**
   * Calculates the present value sensitivity to the strike value.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to the strike value. 
   * This is not relevant for CMS coupons and an exception is thrown in the underlying pricer.
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public double presentValueSensitivityStrike(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    validate(ratesProvider, swaptionVolatilities);
    return cmsLeg
        .getCmsPeriods()
        .stream()
        .map(cmsPeriod -> cmsPeriodPricer.presentValueSensitivityStrike(cmsPeriod, ratesProvider, swaptionVolatilities))
        .collect(Collectors.summingDouble(Double::doubleValue));
  }

  /**
   * Calculates the current cash of the leg. 
   * 
   * @param cmsLeg  the CMS leg
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedCmsLeg cmsLeg,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    validate(ratesProvider, swaptionVolatilities);
    return cmsLeg.getCmsPeriods()
        .stream()
        .filter(x -> x.getPaymentDate().equals(ratesProvider.getValuationDate()))
        .map(x -> cmsPeriodPricer.presentValue(x, ratesProvider, swaptionVolatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .orElse(CurrencyAmount.zero(cmsLeg.getCurrency()));
  }

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, SabrParametersSwaptionVolatilities swaptionVolatilities) {
    ArgChecker.isTrue(swaptionVolatilities.getValuationDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data must be for the same date");
  }

}
