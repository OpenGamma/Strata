/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOptionTrade;

/**
 * Pricer for foreign exchange vanilla option trades with a lognormal model.
 * <p>
 * This function provides the ability to price an {@link FxVanillaOptionTrade}.
 */
public class BlackFxVanillaOptionTradePricer {

  /**
   * Default implementation.
   */
  public static final BlackFxVanillaOptionTradePricer DEFAULT = new BlackFxVanillaOptionTradePricer(
      BlackFxVanillaOptionProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSingleBarrierOption}.
   */
  private final BlackFxVanillaOptionProductPricer productPricer;
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
  public BlackFxVanillaOptionTradePricer(
      BlackFxVanillaOptionProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the foreign exchange vanilla option trade.
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
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxVanillaOption product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(product, ratesProvider, volatilities);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    return MultiCurrencyAmount.of(pvProduct, pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option trade.
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
  public PointSensitivities presentValueSensitivityRates(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxVanillaOption product = trade.getProduct();
    PointSensitivities pvcsProduct =
        productPricer.presentValueSensitivityRates(product, ratesProvider, volatilities);
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
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxVanillaOption product = trade.getProduct();
    return productPricer.presentValueSensitivityModelParamsVolatility(product, ratesProvider, volatilities).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the foreign exchange vanilla option trade.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    ResolvedFxVanillaOption product = trade.getProduct();
    return productPricer.currencyExposure(product, ratesProvider, volatilities).plus(pvPremium);
  }

  /**
   * Calculates the current of the foreign exchange vanilla option trade.
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
