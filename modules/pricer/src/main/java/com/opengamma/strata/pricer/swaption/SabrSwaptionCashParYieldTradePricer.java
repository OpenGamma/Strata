/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionProduct;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Pricer for swaption trade with par yield curve method of cash settlement in SABR model.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed. 
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * The volatilities from the provider are taken as such.
 * <p>
 * The present value and sensitivities of the premium, if in the future, are also taken into account.
 */
public class SabrSwaptionCashParYieldTradePricer {

  /**
   * Default implementation.
   */
  public static final SabrSwaptionCashParYieldTradePricer DEFAULT = new SabrSwaptionCashParYieldTradePricer(
      SabrSwaptionCashParYieldProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link SwaptionProduct}.
   */
  private final SabrSwaptionCashParYieldProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link SwaptionProduct}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public SabrSwaptionCashParYieldTradePricer(
      SabrSwaptionCashParYieldProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption trade.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swap product
   */
  public CurrencyAmount presentValue(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    Swaption product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(product, ratesProvider, swaptionVolatilities);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    return pvProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption trade
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider, swaptionVolatilities));
  }

  /**
   * Calculates the current of the swaption trade.
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
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption trade.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value curve sensitivity of the swap trade
   */
  public PointSensitivityBuilder presentValueSensitivity(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    Swaption product = trade.getProduct();
    PointSensitivityBuilder pvcsProduct =
        productPricer.presentValueSensitivity(product, ratesProvider, swaptionVolatilities);
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = paymentPricer.presentValueSensitivity(premium, ratesProvider);
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the SABR model parameters of the swaption trade.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param trade  the swaption trade to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the Black volatility
   */
  public SwaptionSabrSensitivity presentValueSensitivitySabrParameter(
      SwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    Swaption product = trade.getProduct();
    return productPricer.presentValueSensitivitySabrParameter(product, ratesProvider, swaptionVolatilities);
  }

}
