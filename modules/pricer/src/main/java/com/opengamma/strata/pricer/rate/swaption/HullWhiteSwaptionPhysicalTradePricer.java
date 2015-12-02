/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.index.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Pricer for swaption with physical settlement in Hull-White one factor model with piecewise constant volatility.
 * <p>
 * Reference: Henrard, M. "The Irony in the derivatives discounting Part II: the crisis", Wilmott Journal, 2010, 2, 301-316
 */
public class HullWhiteSwaptionPhysicalTradePricer {

  /**
   * Default implementation.
   */
  public static final HullWhiteSwaptionPhysicalTradePricer DEFAULT = new HullWhiteSwaptionPhysicalTradePricer();

  /** 
   * Pricer for {@link Swaption}.  
   */
  private static final HullWhiteSwaptionPhysicalProductPricer PRICER_PRODUCT = HullWhiteSwaptionPhysicalProductPricer.DEFAULT;
  /** 
   * Pricer for {@link Payment} which is used to described the premium.  
   */
  private static final DiscountingPaymentPricer PRICER_PREMIUM = DiscountingPaymentPricer.DEFAULT;

  /**
   * Calculates the present value of the swaption trade.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter trade
   * @return the present value of the swaption trade
   */
  public CurrencyAmount presentValue(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    Swaption product = trade.getProduct();
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(product, ratesProvider, hwProvider);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = PRICER_PREMIUM.presentValue(premium, ratesProvider);
    return pvProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption trade
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the currency exposure of the swaption trade
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider, hwProvider));
  }

  /**
   * Calculates the current of the swaption trade.
   * <p>
   * Only the premium is contributing to the current cash for non-cash settle swaptions.
   * 
   * @param trade  the swaption trade to price
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(SwaptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0.0);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the present value curve sensitivity of the swaption trade
   */
  public PointSensitivityBuilder presentValueSensitivity(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    Swaption product = trade.getProduct();
    PointSensitivityBuilder pvcsProduct =
        PRICER_PRODUCT.presentValueSensitivity(product, ratesProvider, hwProvider);
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = PRICER_PREMIUM.presentValueSensitivity(premium, ratesProvider);
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity piecewise constant volatility parameters of the Hull-White model.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the present value Hull-White model parameter sensitivity of the swaption trade
   */
  public DoubleArray presentValueSensitivityHullWhiteParameter(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    Swaption product = trade.getProduct();
    return PRICER_PRODUCT.presentValueSensitivityHullWhiteParameter(product, ratesProvider, hwProvider);
  }

}
