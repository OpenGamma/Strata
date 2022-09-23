/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.OvernightFutureOption;
import com.opengamma.strata.product.index.ResolvedOvernightFuture;
import com.opengamma.strata.product.index.ResolvedOvernightFutureOption;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Pricer of options on overnight future with a normal model on the underlying future price.
 * <p>
 * This provides the ability to price an overnight future option.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * <p>
 * Strata uses <i>decimal prices</i> for overnight future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
 */
public class NormalOvernightFutureOptionMarginedProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalOvernightFutureOptionMarginedProductPricer DEFAULT =
      new NormalOvernightFutureOptionMarginedProductPricer(DiscountingOvernightFutureProductPricer.DEFAULT);

  /**
   * The underlying future pricer.
   * The pricer take only the curves as inputs, no model parameters.
   */
  private final DiscountingOvernightFutureProductPricer futurePricer;

  /**
   * Creates an instance.
   * 
   * @param futurePricer  the pricer for {@link OvernightFutureOption}
   */
  public NormalOvernightFutureOptionMarginedProductPricer(
      DiscountingOvernightFutureProductPricer futurePricer) {
    this.futurePricer = ArgChecker.notNull(futurePricer, "futurePricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the underlying future pricer function.
   * 
   * @return the future pricer
   */
  DiscountingOvernightFutureProductPricer getFuturePricer() {
    return futurePricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to overnight futures product on which the daily margin is computed.
   * <p>
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   *    
   * @param option  the option product
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  double marginIndex(ResolvedOvernightFutureOption option, double price) {
    double notional = option.getUnderlyingFuture().getNotional();
    double accrualFactor = option.getUnderlyingFuture().getAccrualFactor();
    return price * notional * accrualFactor;
  }

  /**
   * Calculates the margin index sensitivity of the overnight future product.
   * <p>
   * The margin index sensitivity if the sensitivity of the margin index to the underlying curves.
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   * 
   * @param option  the option product
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  PointSensitivities marginIndexSensitivity(
      ResolvedOvernightFutureOption option,
      PointSensitivities priceSensitivity) {

    double notional = option.getUnderlyingFuture().getNotional();
    double accrualFactor = option.getUnderlyingFuture().getAccrualFactor();
    return priceSensitivity.multipliedBy(notional * accrualFactor);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the overnight future option product.
   * <p>
   * The price of the option is the price on the valuation date.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedOvernightFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return price(futureOption, volatilities, futurePrice);
  }

  /**
   * Calculates the price of the Overnight future option product
   * based on the price of the underlying future.
   * <p>
   * The price of the option is the price on the valuation date.
   * 
   * @param futureOption  the option product
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @return the price of the product, in decimal form
   */
  public double price(
      ResolvedOvernightFutureOption futureOption,
      NormalOvernightFutureOptionVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlyingFuture().getIndex().equals(volatilities.getIndex()),
        "Future index should be the same as data index");
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
    double strike = futureOption.getStrikePrice();
    ResolvedOvernightFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(timeToExpiry, future.getLastTradeDate(), strike, futurePrice);
    return NormalFormulaRepository.price(futurePrice, strike, timeToExpiry, volatility, futureOption.getPutCall());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the delta of the Overnight future option product.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      ResolvedOvernightFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return deltaStickyStrike(futureOption, volatilities, futurePrice);
  }

  /**
   * Calculates the delta of the Overnight future option product
   * based on the price of the underlying future.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      ResolvedOvernightFutureOption futureOption,
      NormalOvernightFutureOptionVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
    double strike = futureOption.getStrikePrice();
    ResolvedOvernightFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(timeToExpiry, future.getLastTradeDate(), strike, futurePrice);
    return NormalFormulaRepository.delta(futurePrice, strike, timeToExpiry, volatility, futureOption.getPutCall());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the Overnight future option product based on curves.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityRatesStickyStrike(
      ResolvedOvernightFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return priceSensitivityRatesStickyStrike(futureOption, ratesProvider, volatilities, futurePrice);
  }

  /**
   * Calculates the price sensitivity of the Overnight future option product
   * based on the price of the underlying future.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityRatesStickyStrike(
      ResolvedOvernightFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities,
      double futurePrice) {

    double delta = deltaStickyStrike(futureOption, volatilities, futurePrice);
    PointSensitivities futurePriceSensitivity =
        futurePricer.priceSensitivity(futureOption.getUnderlyingFuture(), ratesProvider);
    return futurePriceSensitivity.multipliedBy(delta);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Overnight future option.
   * <p>
   * This sensitivity is also called the <i>price normal vega</i>.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOption  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the sensitivity
   */
  public OvernightFutureOptionSensitivity priceSensitivityModelParamsVolatility(
      ResolvedOvernightFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalOvernightFutureOptionVolatilities volatilities) {

    double futurePrice = futurePrice(futureOption, ratesProvider);
    return priceSensitivityModelParamsVolatility(futureOption, volatilities, futurePrice);
  }

  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Overnight future option
   * based on the price of the underlying future.
   * <p>
   * This sensitivity is also called the <i>price normal vega</i>.
   * 
   * @param futureOption  the option product
   * @param volatilities  the volatilities
   * @param futurePrice  the underlying future price, in decimal form
   * @return the sensitivity
   */
  public OvernightFutureOptionSensitivity priceSensitivityModelParamsVolatility(
      ResolvedOvernightFutureOption futureOption,
      NormalOvernightFutureOptionVolatilities volatilities,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    double timeToExpiry = volatilities.relativeTime(futureOption.getExpiry());
    double strike = futureOption.getStrikePrice();
    ResolvedOvernightFuture future = futureOption.getUnderlyingFuture();
    double volatility = volatilities.volatility(timeToExpiry, future.getLastTradeDate(), strike, futurePrice);
    double vega = NormalFormulaRepository.vega(futurePrice, strike, timeToExpiry, volatility, futureOption.getPutCall());
    return OvernightFutureOptionSensitivity.of(
        volatilities.getName(), timeToExpiry, future.getLastTradeDate(), strike, futurePrice, future.getCurrency(), vega);
  }

  //-------------------------------------------------------------------------
  // calculate the price of the underlying future
  private double futurePrice(ResolvedOvernightFutureOption futureOption, RatesProvider ratesProvider) {
    ResolvedOvernightFuture future = futureOption.getUnderlyingFuture();
    return futurePricer.price(future, ratesProvider);
  }

}
