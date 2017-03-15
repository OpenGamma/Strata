/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.ResolvedCms;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for CMS products by swaption replication on a SABR formula with extrapolation.
 * <p>
 * This function provides the ability to price {@link ResolvedCms}. 
 */
public class SabrExtrapolationReplicationCmsProductPricer {

  /**
   * The pricer for {@link CmsLeg}.
   */
  private final SabrExtrapolationReplicationCmsLegPricer cmsLegPricer;
  /**
   * The pricer for {@link SwapLeg}.
   */
  private final DiscountingSwapLegPricer payLegPricer;

  /**
   * Creates an instance using the default pay leg pricer.
   * 
   * @param cmsLegPricer  the pricer for {@link CmsLeg}
   */
  public SabrExtrapolationReplicationCmsProductPricer(
      SabrExtrapolationReplicationCmsLegPricer cmsLegPricer) {

    this(cmsLegPricer, DiscountingSwapLegPricer.DEFAULT);
  }

  /**
   * Creates an instance.
   * 
   * @param cmsLegPricer  the pricer for {@link CmsLeg}
   * @param payLegPricer  the pricer for {@link SwapLeg}
   */
  public SabrExtrapolationReplicationCmsProductPricer(
      SabrExtrapolationReplicationCmsLegPricer cmsLegPricer,
      DiscountingSwapLegPricer payLegPricer) {

    this.cmsLegPricer = ArgChecker.notNull(cmsLegPricer, "cmsLegPricer");
    this.payLegPricer = ArgChecker.notNull(payLegPricer, "payLegPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the CMS product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * CMS leg and pay leg are typically in the same currency. Thus the present value is expressed as a 
   * single currency amount in most cases.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    CurrencyAmount pvCmsLeg = cmsLegPricer.presentValue(cms.getCmsLeg(), ratesProvider, swaptionVolatilities);
    if (!cms.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(pvCmsLeg);
    }
    CurrencyAmount pvPayLeg = payLegPricer.presentValue(cms.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(pvCmsLeg).plus(pvPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Explains the present value of the CMS product.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the explain PV map
   */
  public ExplainMap explainPresentValue(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {
    ExplainMapBuilder builder = ExplainMap.builder();
    builder.put(ExplainKey.ENTRY_TYPE, "CmsSwap");
    List<ExplainMap> legsExplain = new ArrayList<>();
    legsExplain.add(cmsLegPricer.explainPresentValue(cms.getCmsLeg(), ratesProvider, swaptionVolatilities));
    if (cms.getPayLeg().isPresent()) {
      legsExplain.add(payLegPricer.explainPresentValue(cms.getPayLeg().get(), ratesProvider));
    }
    builder.put(ExplainKey.LEGS, legsExplain);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value curve sensitivity of the CMS product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to the underlying curves.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRates(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    PointSensitivityBuilder pvSensiCmsLeg =
        cmsLegPricer.presentValueSensitivityRates(cms.getCmsLeg(), ratesProvider, swaptionVolatilities);
    if (!cms.getPayLeg().isPresent()) {
      return pvSensiCmsLeg;
    }
    PointSensitivityBuilder pvSensiPayLeg = payLegPricer.presentValueSensitivity(cms.getPayLeg().get(), ratesProvider);
    return pvSensiCmsLeg.combinedWith(pvSensiPayLeg);
  }

  /**
   * Calculates the present value sensitivity to the SABR model parameters.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value
   * to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    return cmsLegPricer.presentValueSensitivityModelParamsSabr(cms.getCmsLeg(), ratesProvider, swaptionVolatilities);
  }

  /**
   * Calculates the present value sensitivity to the strike value.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to the strike value.
   * This is not relevant for CMS coupons and an exception is thrown in the underlying pricer.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public double presentValueSensitivityStrike(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    return cmsLegPricer.presentValueSensitivityStrike(cms.getCmsLeg(), ratesProvider, swaptionVolatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the product.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    CurrencyAmount ceCmsLeg = cmsLegPricer.presentValue(cms.getCmsLeg(), ratesProvider, swaptionVolatilities);
    if (!cms.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ceCmsLeg);
    }
    MultiCurrencyAmount cePayLeg = payLegPricer.currencyExposure(cms.getPayLeg().get(), ratesProvider);
    return cePayLeg.plus(ceCmsLeg);
  }

  /**
   * Calculates the current cash of the product.
   * 
   * @param cms  the CMS product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedCms cms,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    CurrencyAmount ccCmsLeg = cmsLegPricer.currentCash(cms.getCmsLeg(), ratesProvider, swaptionVolatilities);
    if (!cms.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ccCmsLeg);
    }
    CurrencyAmount ccPayLeg = payLegPricer.currentCash(cms.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(ccPayLeg).plus(ccCmsLeg);
  }

}
