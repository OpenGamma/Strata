/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.util.function.Function;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.fx.Fx;
import com.opengamma.strata.finance.fx.FxDigitalOption;
import com.opengamma.strata.finance.fx.FxDigitalOptionProduct;
import com.opengamma.strata.finance.fx.FxVanillaOption;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange digital option transaction products by static replication with foreign exchange 
 * vanilla options.
 * <p>
 * This function provides the ability to price an {@link FxDigitalOptionProduct}.
 */
public class StaticReplicationFxDigitalOptionProductPricer extends FxDigitalOptionProductPricer {

  /**
   * Default foreign exchange vanilla options pricer. 
   */
  private static final FxVanillaOptionProductPricer DEFAULT_VANILLA_PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;
  
  /**
   * Default value of strike spread. 
   */
  private static final double DEFAULT_SPREAD = 1E-4;

  /**
   * Default implementation.
   */
  public static final StaticReplicationFxDigitalOptionProductPricer DEFAULT =
      new StaticReplicationFxDigitalOptionProductPricer();

  /**
   * Foreign exchange vanilla options pricer. 
   */
  private final FxVanillaOptionProductPricer vanillaPricer;

  /**
   * Value of strike spread. 
   */
  private final double spread;

  /**
   * Creates an instance using default vanilla option pricer and default spread value. 
   */
  public StaticReplicationFxDigitalOptionProductPricer() {
    this(DEFAULT_VANILLA_PRICER, DEFAULT_SPREAD);
  }

  /**
   * Creates an instance specifying vanilla option pricer and strike spread value. 
   * 
   * @param vanillaPricer  the vanilla option pricer
   * @param spread  the spread value
   */
  public StaticReplicationFxDigitalOptionProductPricer(FxVanillaOptionProductPricer vanillaPricer, double spread) {
    this.spread = ArgChecker.notNull(spread, "spread");
    this.vanillaPricer = ArgChecker.notNull(vanillaPricer, "vanillaPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns vanilla option pricer.
   * 
   * @return the vanilla option pricer
   */
  public FxVanillaOptionProductPricer getFxVanillaOptionProductPricer() {
    return vanillaPricer;
  }

  /**
   * Returns the strike spread value. 
   * 
   * @return the spread
   */
  public double getSpread() {
    return spread;
  }

  //-------------------------------------------------------------------------
  @Override
  double undiscountedPrice(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxVanillaOption vanillaOption = createVanillaOption(option, ratesProvider);
    double undiscountedPrice = computeUndiscountedValue(option,
        (p) -> vanillaPricer.undiscountedPrice(vanillaOption, ratesProvider, volatilityProvider, (p)));
    return undiscountedPrice;
  }

  @Override
  double undiscountedDelta(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxVanillaOption vanillaOption = createVanillaOption(option, ratesProvider);
    double undiscountedPrice = computeUndiscountedValue(option,
        (p) -> vanillaPricer.undiscountedDelta(vanillaOption, ratesProvider, volatilityProvider, (p)));
    return undiscountedPrice;
  }

  @Override
  double undiscountedGamma(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxVanillaOption vanillaOption = createVanillaOption(option, ratesProvider);
    double undiscountedPrice = computeUndiscountedValue(option,
        (p) -> vanillaPricer.undiscountedGamma(vanillaOption, ratesProvider, volatilityProvider, (p)));
    return undiscountedPrice;
  }

  @Override
  double undiscountedVega(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxVanillaOption vanillaOption = createVanillaOption(option, ratesProvider);
    double undiscountedPrice = computeUndiscountedValue(option,
        (p) -> vanillaPricer.undiscountedVega(vanillaOption, ratesProvider, volatilityProvider, (p)));
    return undiscountedPrice;
  }

  @Override
  double undiscountedTheta(
      FxDigitalOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxVanillaOption vanillaOption = createVanillaOption(option, ratesProvider);
    double undiscountedPrice = computeUndiscountedValue(option,
        (p) -> vanillaPricer.undiscountedTheta(vanillaOption, ratesProvider, volatilityProvider, (p)));
    return undiscountedPrice;
  }

  //-------------------------------------------------------------------------
  // compute undiscounted value based on replication
  private double computeUndiscountedValue(FxDigitalOption option, Function<Double, Double> func) {
    FxRate strike = option.getStrike();
    double strikeRate = strike.fxRate(strike.getPair());
    double sign = option.getPutCall().isCall() ? 1d : -1d;
    double valueStrikeUp = func.apply(spread);
    double valueStrikeDown = func.apply(-spread);
    double valueCash = 0.5 * (valueStrikeDown - valueStrikeUp) / spread;
    double undiscountedPrice = option.getStrikeCounterCurrency().equals(option.getPayoffCurrency()) ?
        valueCash : valueCash * strikeRate + func.apply(0d);
    return sign * undiscountedPrice;
  }

  // create vanilla option from digital option
  private FxVanillaOption createVanillaOption(FxDigitalOption digitalOption, RatesProvider ratesProvider) {
    FxRate strike = digitalOption.getStrike();
    CurrencyPair strikePair = strike.getPair();
    Fx underlying = Fx.of(CurrencyAmount.of(digitalOption.getStrikeBaseCurrency(), digitalOption.getNotional()),
        FxRate.of(strikePair, ratesProvider.fxRate(strikePair)), digitalOption.getPaymentDate());
    FxVanillaOption vanillaOption = FxVanillaOption.builder()
        .expiryDate(digitalOption.getExpiryDate())
        .expiryTime(digitalOption.getExpiryTime())
        .expiryZone(digitalOption.getExpiryZone())
        .longShort(digitalOption.getLongShort())
        .putCall(digitalOption.getPutCall())
        .strike(strike)
        .underlying(underlying)
        .build();
    return vanillaOption;
  }

}
