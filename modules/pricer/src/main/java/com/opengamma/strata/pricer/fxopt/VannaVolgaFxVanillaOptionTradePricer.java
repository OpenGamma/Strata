/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOptionTrade;

/**
 * Pricer for FX vanilla option trades with a Vanna-Volga method.
 * <p>
 * The volatilities are expressed using {@code BlackFxOptionSmileVolatilities}. 
 * Each smile of the term structure consists of 3 data points, where the middle point corresponds to ATM volatility.
 */
public class VannaVolgaFxVanillaOptionTradePricer {

  /**
   * Default implementation.
   */
  public static final VannaVolgaFxVanillaOptionTradePricer DEFAULT = new VannaVolgaFxVanillaOptionTradePricer(
      VannaVolgaFxVanillaOptionProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSingleBarrierOption}.
   */
  private final VannaVolgaFxVanillaOptionProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFxVanillaOption}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public VannaVolgaFxVanillaOptionTradePricer(
      VannaVolgaFxVanillaOptionProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX vanilla option trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the trade
   */
  public MultiCurrencyAmount presentValue(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    ResolvedFxVanillaOption product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(product, ratesProvider, volatilities);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    return MultiCurrencyAmount.of(pvProduct, pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX vanilla option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    ResolvedFxVanillaOption product = trade.getProduct();
    PointSensitivities pvcsProduct =
        productPricer.presentValueSensitivityRatesStickyStrike(product, ratesProvider, volatilities).build();
    Payment premium = trade.getPremium();
    PointSensitivities pvcsPremium = paymentPricer.presentValueSensitivity(premium, ratesProvider).build();
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityModelParamsVolatility(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    ResolvedFxVanillaOption product = trade.getProduct();
    return productPricer.presentValueSensitivityModelParamsVolatility(product, ratesProvider, volatilities).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX vanilla option trade.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    ResolvedFxVanillaOption product = trade.getProduct();
    return productPricer.currencyExposure(product, ratesProvider, volatilities).plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current of the FX vanilla option trade.
   * 
   * @param trade  the option trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedFxVanillaOptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

}
