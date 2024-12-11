/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.FxCollarTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxCollar;
import com.opengamma.strata.product.fxopt.ResolvedFxCollarTrade;

/**
 * Pricer for FX collar trades with a lognormal model.
 * <p>
 * This function provides the ability to price an {@link FxCollarTrade}.
 */
public class DiscountingFxCollarTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxCollarTradePricer DEFAULT = new DiscountingFxCollarTradePricer(
      DiscountingFxCollarProductPricer.DEFAULT, DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxCollar}.
   */
  private final DiscountingFxCollarProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   *
   * @param productPricer  the pricer for {@link ResolvedFxCollar}
   * @param paymentPricer  the pricer for {@link DiscountingPaymentPricer}
   */
  public DiscountingFxCollarTradePricer(
      DiscountingFxCollarProductPricer productPricer, DiscountingPaymentPricer paymentPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  /**
   * Calculates the present value of the trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The present value is returned in the settlement currency.
   *
   * @param trade  the trade
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the trade in the settlement currency
   */
  public MultiCurrencyAmount presentValue(ResolvedFxCollarTrade trade, RatesProvider provider, BlackFxOptionVolatilities volatilities) {
    ResolvedFxCollar product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(product, provider, volatilities);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, provider);
    return MultiCurrencyAmount.of(pvProduct).plus(pvPremium);
  }

  /**
   * Calculates the present value sensitivity of the FX collar trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   *
   * @param trade  the option trade
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedFxCollarTrade trade,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxCollar product = trade.getProduct();
    PointSensitivities pvcsProduct = productPricer.presentValueSensitivityRatesStickyStrike(product, provider, volatilities);
    Payment premium = trade.getPremium();
    PointSensitivities pvcsPremium = paymentPricer.presentValueSensitivity(premium, provider).build();
    return pvcsProduct.combinedWith(pvcsPremium);
  }

  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   *
   * @param trade  the option trade
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityModelParamsVolatility(
      ResolvedFxCollarTrade trade,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxCollar product = trade.getProduct();
    return productPricer.presentValueSensitivityModelParamsVolatility(product, provider, volatilities).build();
  }

  /**
   * Calculates the currency exposure of the FX collar trade.
   *
   * @param trade  the option trade
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxCollarTrade trade,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, provider);
    ResolvedFxCollar product = trade.getProduct();
    return productPricer.currencyExposure(product, provider, volatilities).plus(pvPremium);
  }

  /**
   * Calculates the current of the FX collar trade.
   *
   * @param trade  the option trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedFxCollarTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

  /**
   * Calculates the forward exchange rate.
   *
   * @param trade  the option trade
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxCollarTrade trade, RatesProvider provider) {
    ResolvedFxCollar product = trade.getProduct();
    return productPricer.forwardFxRate(product, provider);
  }

  /**
   * Calculates the implied Black volatility of the foreign exchange collar trade.
   *
   * @param trade  the option trade
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the implied volatility of the product
   * @throws IllegalArgumentException if the option has expired
   */
  public double impliedVolatility(
      ResolvedFxCollarTrade trade,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {
    ResolvedFxCollar product = trade.getProduct();
    return productPricer.impliedVolatility(product, provider, volatilities);
  }
}
