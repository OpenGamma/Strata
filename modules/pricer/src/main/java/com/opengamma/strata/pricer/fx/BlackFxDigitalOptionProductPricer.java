/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;


import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.AssetOrNothingOptionPriceFunction;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.CashOrNothingOptionPriceFunction;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.finance.fx.FxDigitalOption;
import com.opengamma.strata.finance.fx.FxDigitalOptionProduct;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange digital option transaction products with a lognormal model.
 * <p>
 * This function provides the ability to price an {@link FxDigitalOptionProduct}.
 */
public class BlackFxDigitalOptionProductPricer extends FxDigitalOptionProductPricer {
  /**
   * Pricer for asset-or-nothing options.
   * <p>
   * This pricer is used if payoff currency is the same as base currency of strike pair. 
   */
  private static final AssetOrNothingOptionPriceFunction PRICER_ASSET = new AssetOrNothingOptionPriceFunction();
  /**
   * Pricer for cash-or-nothing options. 
   * <p>
   * This pricer is used if payoff currency is the same as the counter currency of strike pair.
   */
  private static final CashOrNothingOptionPriceFunction PRICER_CASH = new CashOrNothingOptionPriceFunction();

  /**
   * Default implementation.
   */
  public static final BlackFxDigitalOptionProductPricer DEFAULT =
      new BlackFxDigitalOptionProductPricer();


  /**
   * Creates an instance.
   */
  public BlackFxDigitalOptionProductPricer() {
  }

  @Override
  double undiscountedPrice(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double strikeRate = strike.fxRate(strikePair);
    double forwardRate = ratesProvider.fxIndexRates(option.getIndex())
        .rate(strikePair.getBase(), option.getExpiryDate());
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    if (strikePair.getCounter().equals(option.getPayoffCurrency())) {
      return PRICER_CASH.price(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    }
    return PRICER_ASSET.price(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
  }

  @Override
  double undiscountedDelta(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double strikeRate = strike.fxRate(strikePair);
    double forwardRate = ratesProvider.fxIndexRates(
        option.getIndex()).rate(strikePair.getBase(), option.getExpiryDate());
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    if (strikePair.getCounter().equals(option.getPayoffCurrency())) {
      return PRICER_CASH.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    }
    return PRICER_ASSET.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
  }

  @Override
  double undiscountedGamma(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double strikeRate = strike.fxRate(strikePair);
    double forwardRate = ratesProvider.fxIndexRates(
        option.getIndex()).rate(strikePair.getBase(), option.getExpiryDate());
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    if (strikePair.getCounter().equals(option.getPayoffCurrency())) {
      return PRICER_CASH.gamma(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    }
    return PRICER_ASSET.gamma(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
  }

  @Override
  double undiscountedVega(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double strikeRate = strike.fxRate(strikePair);
    double forwardRate = ratesProvider.fxIndexRates(
        option.getIndex()).rate(strikePair.getBase(), option.getExpiryDate());
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    if (strikePair.getCounter().equals(option.getPayoffCurrency())) {
      return PRICER_CASH.vega(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    }
    return PRICER_ASSET.vega(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
  }

  @Override
  double undiscountedTheta(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double strikeRate = strike.fxRate(strikePair);
    double forwardRate = ratesProvider.fxIndexRates(
        option.getIndex()).rate(strikePair.getBase(), option.getExpiryDate());
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    if (strikePair.getCounter().equals(option.getPayoffCurrency())) {
      return PRICER_CASH.theta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    }
    return PRICER_ASSET.theta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
  }
}
