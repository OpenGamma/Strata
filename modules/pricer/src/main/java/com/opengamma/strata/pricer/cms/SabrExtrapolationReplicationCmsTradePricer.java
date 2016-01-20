/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsProduct;
import com.opengamma.strata.product.cms.CmsTrade;

/**
 * Pricer for CMS trade by swaption replication on a SABR formula with extrapolation.
 * <p>
 * This function provides the ability to price {@link CmsTrade}. 
 */
public class SabrExtrapolationReplicationCmsTradePricer {

  /**
   * Pricer for {@link CmsProduct}.
   */
  private final SabrExtrapolationReplicationCmsProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance. 
   * 
   * @param productPricer  the pricer for {@link CmsProduct}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public SabrExtrapolationReplicationCmsTradePricer(
      SabrExtrapolationReplicationCmsProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the CMS trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param cms  the CMS trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      CmsTrade cms,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    MultiCurrencyAmount pvCms = productPricer.presentValue(cms.getProduct(), ratesProvider, swaptionVolatilities);
    if (!cms.getPremium().isPresent()) {
      return pvCms;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(cms.getPremium().get(), ratesProvider);
    return pvCms.plus(pvPremium);
  }

  /**
   * Calculates the present value curve sensitivity of the CMS trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to the underlying curves.
   * 
   * @param cms  the CMS trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      CmsTrade cms,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    PointSensitivityBuilder pvSensiCms =
        productPricer.presentValueSensitivity(cms.getProduct(), ratesProvider, swaptionVolatilities);
    if (!cms.getPremium().isPresent()) {
      return pvSensiCms;
    }
    PointSensitivityBuilder pvSensiPremium = paymentPricer.presentValueSensitivity(cms.getPremium().get(), ratesProvider);
    return pvSensiCms.combinedWith(pvSensiPremium);
  }

  /**
   * Calculates the present value sensitivity to the SABR model parameters.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to the SABR model parameters, 
   * alpha, beta, rho and nu.
   * 
   * @param cms  the CMS trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public SwaptionSabrSensitivities presentValueSensitivitySabrParameter(
      CmsTrade cms,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    return productPricer.presentValueSensitivitySabrParameter(cms.getProduct(), ratesProvider, swaptionVolatilities);
  }

  /**
   * Calculates the present value sensitivity to the strike value.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to the strike value. 
   * 
   * @param cms  the CMS trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public double presentValueSensitivityStrike(
      CmsTrade cms,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    return productPricer.presentValueSensitivityStrike(cms.getProduct(), ratesProvider, swaptionVolatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the trade.
   * 
   * @param cms  the CMS trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      CmsTrade cms,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    MultiCurrencyAmount ceCms = productPricer.currencyExposure(cms.getProduct(), ratesProvider, swaptionVolatilities);
    if (!cms.getPremium().isPresent()) {
      return ceCms;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(cms.getPremium().get(), ratesProvider);
    return ceCms.plus(pvPremium);
  }

  /**
   * Calculates the current cash of the trade.
   * 
   * @param cms  the CMS trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      CmsTrade cms,
      RatesProvider ratesProvider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    MultiCurrencyAmount ccCms = productPricer.currentCash(cms.getProduct(), ratesProvider, swaptionVolatilities);
    if (!cms.getPremium().isPresent()) {
      return ccCms;
    }
    Payment premium = cms.getPremium().get();
    if (premium.getDate().equals(ratesProvider.getValuationDate())) {
      ccCms = ccCms.plus(premium.getCurrency(), premium.getAmount());
    }
    return ccCms;
  }

}
