/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.BondFutureOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureOption;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Pricer of options on bond future with a lognormal model on the underlying future price.
 */
public final class BlackBondFutureOptionMarginedProductPricer extends BondFutureOptionMarginedProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackBondFutureOptionMarginedProductPricer DEFAULT =
      new BlackBondFutureOptionMarginedProductPricer(DiscountingBondFutureProductPricer.DEFAULT);

  /**
   * The underlying future pricer.
   * The pricer take only the curves as inputs, no model parameters.
   */
  private final DiscountingBondFutureProductPricer futurePricer;

  /**
   * Creates an instance.
   * 
   * @param futurePricer  the pricer for {@link BondFutureOption}
   */
  public BlackBondFutureOptionMarginedProductPricer(
      DiscountingBondFutureProductPricer futurePricer) {
    this.futurePricer = ArgChecker.notNull(futurePricer, "futurePricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the underlying future pricer function.
   * 
   * @return the future pricer
   */
  DiscountingBondFutureProductPricer getFuturePricer() {
    return futurePricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future option product.
   * <p>
   * The price of the option is the price on the valuation date.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @return the price of the product, in decimal form
   */
  public double price(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return price(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the price of the bond future option product
   * based on the price of the underlying future.
   * <p>
   * The price of the option is the price on the valuation date.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @param futurePrice  the price of the underlying future
   * @return the price of the product, in decimal form
   */
  public double price(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlyingLink().getStandardId().equals(volatilityProvider.getFutureSecurityId()),
        "Underlying future security ID should be the same as security ID of data");
    double strike = futureOption.getStrikePrice();
    BondFuture future = futureOption.getUnderlying();
    double volatility = volatilityProvider.getVolatility(
        futureOption.getExpiry(), future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilityProvider.relativeTime(futureOption.getExpiry());
    double price = BlackFormulaRepository.price(
        futurePrice, strike, timeToExpiry, volatility, futureOption.getPutCall().isCall());
    return price;
  }

  @Override
  double price(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider volatilityProvider) {

    ArgChecker.isTrue(volatilityProvider instanceof BlackVolatilityBondFutureProvider,
        "Provider must be of type BlackVolatilityBondFutureProvider");
    return price(futureOption, ratesProvider, (BlackVolatilityBondFutureProvider) volatilityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the delta of the bond future option product.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return deltaStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the delta of the bond future option product based on the price of the underlying future.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlyingLink().getStandardId().equals(volatilityProvider.getFutureSecurityId()),
        "Underlying future security ID should be the same as security ID of data");
    double strike = futureOption.getStrikePrice();
    BondFuture future = futureOption.getUnderlying();
    double volatility = volatilityProvider.getVolatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilityProvider.relativeTime(futureOption.getExpiry());
    double delta = BlackFormulaRepository.delta(
        futurePrice, strike, timeToExpiry, volatility, futureOption.getPutCall().isCall());
    return delta;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the gamma of the bond future option product.
   * <p>
   * The gamma of the product is the sensitivity of the option delta to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @return the price curve sensitivity of the product
   */
  public double gammaStickyStrike(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return gammaStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the gamma of the bond future option product based on the price of the underlying future.
   * <p>
   * The gamma of the product is the sensitivity of the option delta to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double gammaStickyStrike(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlyingLink().getStandardId().equals(volatilityProvider.getFutureSecurityId()),
        "Underlying future security ID should be the same as security ID of data");
    double strike = futureOption.getStrikePrice();
    BondFuture future = futureOption.getUnderlying();
    double volatility = volatilityProvider.getVolatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilityProvider.relativeTime(futureOption.getExpiry());
    double gamma = BlackFormulaRepository.gamma(futurePrice, strike, timeToExpiry, volatility);
    return gamma;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the theta of the bond future option product.
   * <p>
   * The theta of the product is the minus of the option price sensitivity to the time to expiry.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @return the price curve sensitivity of the product
   */
  public double theta(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return theta(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the theta of the bond future option product based on the price of the underlying future.
   * <p>
   * The theta of the product is minus of the option price sensitivity to the time to expiry.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double theta(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlyingLink().getStandardId().equals(volatilityProvider.getFutureSecurityId()),
        "Underlying future security ID should be the same as security ID of data");
    double strike = futureOption.getStrikePrice();
    BondFuture future = futureOption.getUnderlying();
    double volatility = volatilityProvider.getVolatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilityProvider.relativeTime(futureOption.getExpiry());
    double theta = BlackFormulaRepository.driftlessTheta(futurePrice, strike, timeToExpiry, volatility);
    return theta;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the bond future option product based on curves.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double futurePrice = futurePrice(futureOption, ratesProvider);
    return priceSensitivityStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the price sensitivity of the bond future option product based on the price of the underlying future.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name. 
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider,
      double futurePrice) {

    double delta = deltaStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
    PointSensitivities futurePriceSensitivity =
        futurePricer.priceSensitivity(futureOption.getUnderlying(), ratesProvider);
    return futurePriceSensitivity.multipliedBy(delta);
  }

  @Override
  PointSensitivities priceSensitivity(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider volatilityProvider) {

    ArgChecker.isTrue(volatilityProvider instanceof BlackVolatilityBondFutureProvider,
        "Provider must be of type BlackVolatilityBondFutureProvider");
    return priceSensitivityStickyStrike(
        futureOption, ratesProvider, (BlackVolatilityBondFutureProvider) volatilityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity to the Black volatility used for the pricing of the bond future option.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @return the sensitivity
   */
  public BondFutureOptionSensitivity priceSensitivityBlackVolatility(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return priceSensitivityBlackVolatility(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the price sensitivity to the Black volatility used for the pricing of the bond future option
   * based on the price of the underlying future.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of Black volatility
   * @param futurePrice  the underlying future price
   * @return the sensitivity
   */
  public BondFutureOptionSensitivity priceSensitivityBlackVolatility(
      BondFutureOption futureOption,
      LegalEntityDiscountingProvider ratesProvider,
      BlackVolatilityBondFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlyingLink().getStandardId().equals(volatilityProvider.getFutureSecurityId()),
        "Underlying future security ID should be the same as security ID of data");
    double strike = futureOption.getStrikePrice();
    BondFuture future = futureOption.getUnderlying();
    double volatility = volatilityProvider.getVolatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilityProvider.relativeTime(futureOption.getExpiry());
    double vega = BlackFormulaRepository.vega(futurePrice, strike, timeToExpiry, volatility);
    return BondFutureOptionSensitivity.of(futureOption.getUnderlyingLink().getStandardId(),
        futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice, future.getCurrency(), vega);
  }

  //-------------------------------------------------------------------------
  // calculate the price of the underlying future
  private double futurePrice(BondFutureOption futureOption, LegalEntityDiscountingProvider ratesProvider) {
    BondFuture future = futureOption.getUnderlying();
    return futurePricer.price(future, ratesProvider);
  }

}
