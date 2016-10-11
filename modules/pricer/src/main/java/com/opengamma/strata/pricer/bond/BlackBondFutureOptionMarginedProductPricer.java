/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.ResolvedBondFuture;
import com.opengamma.strata.product.bond.ResolvedBondFutureOption;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Pricer of options on bond future with a log-normal model on the underlying future price.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link BondFuture}.
 */
public final class BlackBondFutureOptionMarginedProductPricer {

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
   * @param futurePricer  the pricer for {@link ResolvedBondFutureOption}
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
   * Calculates the number related to bond futures product on which the daily margin is computed.
   * <p>
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   *    
   * @param option  the option product
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  double marginIndex(ResolvedBondFutureOption option, double price) {
    double notional = option.getUnderlyingFuture().getNotional();
    return price * notional;
  }

  /**
   * Calculates the margin index sensitivity of the bond future product.
   * <p>
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   * The margin index sensitivity if the sensitivity of the margin index to the underlying curves.
   * 
   * @param option  the option product
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  PointSensitivities marginIndexSensitivity(ResolvedBondFutureOption option, PointSensitivities priceSensitivity) {
    double notional = option.getUnderlyingFuture().getNotional();
    return priceSensitivity.multipliedBy(notional);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future option product.
   * <p>
   * The price of the option is the price on the valuation date.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * <p>
   * Strata uses <i>decimal prices</i> for bond futures. This is coherent with the pricing of {@link BondFuture}.
   * For example, a price of 1.32% is represented in Strata by 0.0132.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, discountingProvider);
    return price(futureOption, discountingProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the price of the bond future option product
   * based on the price of the underlying future.
   * <p>
   * The price of the option is the price on the valuation date.
   * <p>
   * Strata uses <i>decimal prices</i> for bond futures. This is coherent with the pricing of {@link BondFuture}.
   * For example, a price of 1.32% is represented in Strata by 0.0132.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double strike = futureOption.getStrikePrice();
    ResolvedBondFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(
        futureOption.getExpiry(), future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
    double price = BlackFormulaRepository.price(
        futurePrice, strike, timeToExpiry, volatility, futureOption.getPutCall().isCall());
    return price;
  }

  double price(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    ArgChecker.isTrue(volatilities instanceof BlackBondFutureVolatilities,
        "Provider must be of type BlackVolatilityBondFutureProvider");
    return price(futureOption, discountingProvider, (BlackBondFutureVolatilities) volatilities);
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
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, discountingProvider);
    return deltaStickyStrike(futureOption, discountingProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the delta of the bond future option product based on the price of the underlying future.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double strike = futureOption.getStrikePrice();
    ResolvedBondFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
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
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price curve sensitivity of the product
   */
  public double gammaStickyStrike(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, discountingProvider);
    return gammaStickyStrike(futureOption, discountingProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the gamma of the bond future option product based on the price of the underlying future.
   * <p>
   * The gamma of the product is the sensitivity of the option delta to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double gammaStickyStrike(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double strike = futureOption.getStrikePrice();
    ResolvedBondFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
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
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price curve sensitivity of the product
   */
  public double theta(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, discountingProvider);
    return theta(futureOption, discountingProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the theta of the bond future option product based on the price of the underlying future.
   * <p>
   * The theta of the product is minus of the option price sensitivity to the time to expiry.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double theta(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double strike = futureOption.getStrikePrice();
    ResolvedBondFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
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
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityRatesStickyStrike(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double futurePrice = futurePrice(futureOption, discountingProvider);
    return priceSensitivityRatesStickyStrike(futureOption, discountingProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the price sensitivity of the bond future option product based on the price of the underlying future.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityRatesStickyStrike(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    double delta = deltaStickyStrike(futureOption, discountingProvider, volatilities, futurePrice);
    PointSensitivities futurePriceSensitivity =
        futurePricer.priceSensitivity(futureOption.getUnderlyingFuture(), discountingProvider);
    return futurePriceSensitivity.multipliedBy(delta);
  }

  PointSensitivities priceSensitivity(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    ArgChecker.isTrue(volatilities instanceof BlackBondFutureVolatilities,
        "Provider must be of type BlackVolatilityBondFutureProvider");
    return priceSensitivityRatesStickyStrike(
        futureOption, discountingProvider, (BlackBondFutureVolatilities) volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity to the Black volatility used for the pricing of the bond future option.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @return the sensitivity
   */
  public BondFutureOptionSensitivity priceSensitivityModelParamsVolatility(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, discountingProvider);
    return priceSensitivityModelParamsVolatility(futureOption, discountingProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the price sensitivity to the Black volatility used for the pricing of the bond future option
   * based on the price of the underlying future.
   * 
   * @param futureOption  the option product
   * @param discountingProvider  the discounting provider
   * @param volatilities  the volatilities
   * @param futurePrice  the underlying future price
   * @return the sensitivity
   */
  public BondFutureOptionSensitivity priceSensitivityModelParamsVolatility(
      ResolvedBondFutureOption futureOption,
      LegalEntityDiscountingProvider discountingProvider,
      BlackBondFutureVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double strike = futureOption.getStrikePrice();
    ResolvedBondFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(futureOption.getExpiry(),
        future.getLastTradeDate(), strike, futurePrice);
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
    double vega = BlackFormulaRepository.vega(futurePrice, strike, timeToExpiry, volatility);
    return BondFutureOptionSensitivity.of(
        volatilities.getName(), timeToExpiry, future.getLastTradeDate(), strike, futurePrice, future.getCurrency(), vega);
  }

  //-------------------------------------------------------------------------
  // calculate the price of the underlying future
  private double futurePrice(ResolvedBondFutureOption futureOption, LegalEntityDiscountingProvider discountingProvider) {
    ResolvedBondFuture future = futureOption.getUnderlyingFuture();
    return futurePricer.price(future, discountingProvider);
  }

}
