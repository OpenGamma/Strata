/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxCollar;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Pricer for fx collar transaction products.
 * <p>
 * This provides the ability to price an {@link ResolvedFxCollar}.
 */
public class DiscountingFxCollarProductPricer {
  /**
   * Default implementation.
   */
  public static final DiscountingFxCollarProductPricer DEFAULT =
      new DiscountingFxCollarProductPricer(BlackFxVanillaOptionProductPricer.DEFAULT);
  /**
   * Underlying option FX pricer.
   */
  private final BlackFxVanillaOptionProductPricer fxPricer;
  /**
   * Creates an instance.
   *
   * @param fxPricer  the pricer for {@link ResolvedFxVanillaOption}
   */
  public DiscountingFxCollarProductPricer(
      BlackFxVanillaOptionProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  /**
   * Calculates the price of the foreign exchange collar product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency
   * and is expressed in the counter currency. The price does not take into account the long/short flag.
   * See {@link #presentValue} for scaling and currency.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    double option1 = fxPricer.price(collar.getOption1(), provider, volatilities);
    double option2 = fxPricer.price(collar.getOption1(), provider, volatilities);
    return option1 + option2;
  }

  /**
   * Calculates the present value of the foreign exchange collar product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * It is expressed in the counter currency.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedFxCollar collar, RatesProvider provider, BlackFxOptionVolatilities volatilities) {
    CurrencyAmount option1 = fxPricer.presentValue(collar.getOption1(), provider, volatilities);
    CurrencyAmount option2 = fxPricer.presentValue(collar.getOption2(), provider, volatilities);
    return option1.plus(option2);
  }

  /**
   * Calculates the delta of the foreign exchange collar product.
   * <p>
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the delta of the product
   */
  public double delta(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {
    double option1 = fxPricer.delta(collar.getOption1(), provider, volatilities);
    double option2 = fxPricer.delta(collar.getOption2(), provider, volatilities);
    return option1 + option2;
  }

  /**
   * Calculates the present value delta of the foreign exchange collar product.
   * <p>
   * The present value delta is the first derivative of {@link #presentValue} with respect to spot.
   *
   * @param collar  the collar product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value delta of the product
   */
  public CurrencyAmount presentValueDelta(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    CurrencyAmount option1 = fxPricer.presentValueDelta(collar.getOption1(), provider, volatilities);
    CurrencyAmount option2 = fxPricer.presentValueDelta(collar.getOption2(), provider, volatilities);
    return option1.plus(option2);
  }

  /**
   * Calculates the present value sensitivity of the foreign exchange collar product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    PointSensitivities option1 = fxPricer.presentValueSensitivityRatesStickyStrike(collar.getOption1(), provider, volatilities);
    PointSensitivities option2 = fxPricer.presentValueSensitivityRatesStickyStrike(collar.getOption2(), provider, volatilities);
    return option1.combinedWith(option2);
  }

  /**
   * Calculates the gamma of the foreign exchange collar product.
   * <p>
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the gamma of the product
   */
  public double gamma(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    double option1 = fxPricer.gamma(collar.getOption1(), provider, volatilities);
    double option2 = fxPricer.gamma(collar.getOption2(), provider, volatilities);
    return option1 + option2;
  }

  /**
   * Calculates the present value delta of the foreign exchange collar product.
   * <p>
   * The present value gamma is the second derivative of the {@link #presentValue} with respect to spot.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value gamma of the product
   */
  public CurrencyAmount presentValueGamma(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    CurrencyAmount option1 = fxPricer.presentValueGamma(collar.getOption1(), provider, volatilities);
    CurrencyAmount option2 = fxPricer.presentValueGamma(collar.getOption2(), provider, volatilities);
    return option1.plus(option2);
  }

  /**
   * Calculates the vega of the foreign exchange collar product.
   * <p>
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the vega of the product
   */
  public double vega(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    double option1 = fxPricer.vega(collar.getOption1(), provider, volatilities);
    double option2 = fxPricer.vega(collar.getOption2(), provider, volatilities);
    return option1 + option2;
  }

  /**
   * Calculates the present value vega of the foreign exchange collar product.
   * <p>
   * The present value vega is the first derivative of the {@link #presentValue} with respect to volatility.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueVega(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    CurrencyAmount option1 = fxPricer.presentValueVega(collar.getOption1(), provider, volatilities);
    CurrencyAmount option2 = fxPricer.presentValueVega(collar.getOption2(), provider, volatilities);
    return option1.plus(option2);
  }

  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    PointSensitivityBuilder option1 = fxPricer.presentValueSensitivityModelParamsVolatility(collar.getOption1(), provider, volatilities);
    PointSensitivityBuilder option2 = fxPricer.presentValueSensitivityModelParamsVolatility(collar.getOption2(), provider, volatilities);
    return option1.combinedWith(option2);
  }

  /**
   * Calculates the Black theta of the foreign exchange collar product.
   * <p>
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the theta of the product
   */
  public double theta(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    double option1 = fxPricer.theta(collar.getOption1(), provider, volatilities);
    double option2 = fxPricer.theta(collar.getOption2(), provider, volatilities);
    return option1 + option2;
  }

  /**
   * Calculates the present value theta of the foreign exchange collar product.
   * <p>
   * The present value theta is the negative of the first derivative of {@link #presentValue} with time parameter
   * in Black formula, i.e., the driftless theta of the present value.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueTheta(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    CurrencyAmount option1 = fxPricer.presentValueTheta(collar.getOption1(), provider, volatilities);
    CurrencyAmount option2 = fxPricer.presentValueTheta(collar.getOption2(), provider, volatilities);
    return option1.plus(option2);
  }

  /**
   * Calculates the forward exchange rate.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxCollar collar, RatesProvider provider) {
    return fxPricer.forwardFxRate(collar.getOption1(), provider);
  }

  /**
   * Calculates the implied Black volatility of the foreign exchange collar product.
   *
   * @param collar  the option product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the implied volatility of the product
   * @throws IllegalArgumentException if the option has expired
   */
  public double impliedVolatility(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {
    return fxPricer.impliedVolatility(collar.getOption1(), provider, volatilities);
  }

  /**
   * Calculates the present value sensitivity of the FX collar product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   *
   * @param collar  the product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    PointSensitivities option1 = fxPricer.presentValueSensitivityRatesStickyStrike(collar.getOption1(), provider, volatilities);
    PointSensitivities option2 = fxPricer.presentValueSensitivityRatesStickyStrike(collar.getOption2(), provider, volatilities);
    return option1.combinedWith(option2);
  }

  /**
   * Calculates the currency exposure of the foreign exchange collar product.
   *
   * @param collar  the collar product
   * @param provider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxCollar collar,
      RatesProvider provider,
      BlackFxOptionVolatilities volatilities) {

    MultiCurrencyAmount option1 = fxPricer.currencyExposure(collar.getOption1(), provider, volatilities);
    MultiCurrencyAmount option2 = fxPricer.currencyExposure(collar.getOption2(), provider, volatilities);
    return option1.plus(option2);
  }
}
