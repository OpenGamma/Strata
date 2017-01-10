/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Pricing method for vanilla Forex option transactions with Vanna-Volga method.
 * <p>
 * The volatilities are expressed using {@code BlackFxOptionSmileVolatilities}. 
 * Each smile of the term structure consists of 3 data points, where the middle point corresponds to ATM volatility.
 * <p>
 * Reference: The vanna-volga method for implied volatilities (2007), A. Castagna and F. Mercurio, Risk, 106-111, January 2007.
 * OG implementation: Vanna-volga method for Forex options, version 1.0, June 2012.
 */
public class VannaVolgaFxVanillaOptionProductPricer {

  /**
   * Default implementation.
   */
  public static final VannaVolgaFxVanillaOptionProductPricer DEFAULT = new VannaVolgaFxVanillaOptionProductPricer(
      DiscountingFxSingleProductPricer.DEFAULT);

  /**
   * Underlying FX pricer.
   */
  private final DiscountingFxSingleProductPricer fxPricer;

  /**
   * Creates an instance.
   * 
   * @param fxPricer  the pricer for {@link ResolvedFxSingle}
   */
  public VannaVolgaFxVanillaOptionProductPricer(DiscountingFxSingleProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the foreign exchange vanilla option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency 
   * and is expressed in the counter currency. The price does not take into account the long/short flag.
   * See {@link #presentValue} for scaling and currency.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    ResolvedFxSingle underlyingFx = option.getUnderlying();
    Currency ccyCounter = option.getCounterCurrency();
    double df = ratesProvider.discountFactor(ccyCounter, underlyingFx.getPaymentDate());
    FxRate forward = fxPricer.forwardFxRate(underlyingFx, ratesProvider);
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double forwardRate = forward.fxRate(currencyPair);
    double strikeRate = option.getStrike();
    boolean isCall = option.getPutCall().isCall();
    SmileDeltaParameters smileAtTime = volatilities.getSmile().smileForExpiry(timeToExpiry);
    double[] strikes = smileAtTime.strike(forwardRate).toArray();
    double[] vols = smileAtTime.getVolatility().toArray();
    double volAtm = vols[1];
    double[] x = vannaVolgaWeights(forwardRate, strikeRate, timeToExpiry, volAtm, strikes);
    double priceFwd = BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, volAtm, isCall);
    for (int i = 0; i < 3; i += 2) {
      double priceFwdAtm = BlackFormulaRepository.price(forwardRate, strikes[i], timeToExpiry, volAtm, isCall);
      double priceFwdSmile = BlackFormulaRepository.price(forwardRate, strikes[i], timeToExpiry, vols[i], isCall);
      priceFwd += x[i] * (priceFwdSmile - priceFwdAtm);
    }
    return df * priceFwd;
  }

  /**
   * Calculates the present value of the foreign exchange vanilla option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * It is expressed in the counter currency.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    double price = price(option, ratesProvider, volatilities);
    return CurrencyAmount.of(option.getCounterCurrency(), signedNotional(option) * price);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curves.
   * <p>
   * The implied strikes and weights are fixed in this sensitivity computation.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyStrike(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return PointSensitivityBuilder.none();
    }
    ResolvedFxSingle underlyingFx = option.getUnderlying();
    Currency ccyCounter = option.getCounterCurrency();
    double df = ratesProvider.discountFactor(ccyCounter, underlyingFx.getPaymentDate());
    FxRate forward = fxPricer.forwardFxRate(underlyingFx, ratesProvider);
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double forwardRate = forward.fxRate(currencyPair);
    double strikeRate = option.getStrike();
    boolean isCall = option.getPutCall().isCall();
    SmileDeltaParameters smileAtTime = volatilities.getSmile().smileForExpiry(timeToExpiry);
    double[] strikes = smileAtTime.strike(forwardRate).toArray();
    double[] vols = smileAtTime.getVolatility().toArray();
    double volAtm = vols[1];
    double[] x = vannaVolgaWeights(forwardRate, strikeRate, timeToExpiry, volAtm, strikes);
    double priceFwd = BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, volAtm, isCall);
    double deltaFwd = BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volAtm, isCall);
    for (int i = 0; i < 3; i += 2) {
      double priceFwdAtm = BlackFormulaRepository.price(forwardRate, strikes[i], timeToExpiry, volAtm, isCall);
      double priceFwdSmile = BlackFormulaRepository.price(forwardRate, strikes[i], timeToExpiry, vols[i], isCall);
      priceFwd += x[i] * (priceFwdSmile - priceFwdAtm);
      double deltaFwdAtm = BlackFormulaRepository.delta(forwardRate, strikes[i], timeToExpiry, volAtm, isCall);
      double deltaFwdSmile = BlackFormulaRepository.delta(forwardRate, strikes[i], timeToExpiry, vols[i], isCall);
      deltaFwd += x[i] * (deltaFwdSmile - deltaFwdAtm);
    }
    double signedNotional = signedNotional(option);
    PointSensitivityBuilder dfSensi = ratesProvider.discountFactors(ccyCounter)
        .zeroRatePointSensitivity(underlyingFx.getPaymentDate()).multipliedBy(priceFwd * signedNotional);
    PointSensitivityBuilder fwdSensi = fxPricer.forwardFxRatePointSensitivity(
        option.getPutCall().isCall() ? underlyingFx : underlyingFx.inverse(), ratesProvider)
        .multipliedBy(df * deltaFwd * signedNotional);
    return dfSensi.combinedWith(fwdSensi);
  }

  /**
   * Computes the present value sensitivity to the black volatilities used in the pricing.
   * <p>
   * The implied strikes and weights are fixed in this sensitivity computation.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return PointSensitivityBuilder.none();
    }
    ResolvedFxSingle underlyingFx = option.getUnderlying();
    Currency ccyCounter = option.getCounterCurrency();
    double df = ratesProvider.discountFactor(ccyCounter, underlyingFx.getPaymentDate());
    FxRate forward = fxPricer.forwardFxRate(underlyingFx, ratesProvider);
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double forwardRate = forward.fxRate(currencyPair);
    double strikeRate = option.getStrike();
    SmileDeltaParameters smileAtTime = volatilities.getSmile().smileForExpiry(timeToExpiry);
    double[] strikes = smileAtTime.strike(forwardRate).toArray();
    double[] vols = smileAtTime.getVolatility().toArray();
    double volAtm = vols[1];
    double[] x = vannaVolgaWeights(forwardRate, strikeRate, timeToExpiry, volAtm, strikes);
    double vegaAtm = BlackFormulaRepository.vega(forwardRate, strikeRate, timeToExpiry, volAtm);
    double signedNotional = signedNotional(option);
    PointSensitivityBuilder sensiSmile = PointSensitivityBuilder.none();
    for (int i = 0; i < 3; i += 2) {
      double vegaFwdAtm = BlackFormulaRepository.vega(forwardRate, strikes[i], timeToExpiry, volAtm);
      vegaAtm -= x[i] * vegaFwdAtm;
      double vegaFwdSmile = BlackFormulaRepository.vega(forwardRate, strikes[i], timeToExpiry, vols[i]);
      sensiSmile = sensiSmile.combinedWith(
          FxOptionSensitivity.of(
              volatilities.getName(),
              currencyPair,
              timeToExpiry,
              strikes[i],
              forwardRate,
              ccyCounter,
              df * signedNotional * x[i] * vegaFwdSmile));
    }
    FxOptionSensitivity sensiAtm = FxOptionSensitivity.of(
        volatilities.getName(),
        currencyPair,
        timeToExpiry,
        strikes[1],
        forwardRate,
        ccyCounter,
        df * signedNotional * vegaAtm);
    return sensiAtm.combinedWith(sensiSmile);
  }

  /**
   * Calculates the currency exposure of the foreign exchange vanilla option product.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionSmileVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return MultiCurrencyAmount.empty();
    }
    ResolvedFxSingle underlyingFx = option.getUnderlying();
    Currency ccyCounter = option.getCounterCurrency();
    double df = ratesProvider.discountFactor(ccyCounter, underlyingFx.getPaymentDate());
    FxRate forward = fxPricer.forwardFxRate(underlyingFx, ratesProvider);
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double spot = ratesProvider.fxRate(currencyPair);
    double forwardRate = forward.fxRate(currencyPair);
    double fwdRateSpotSensitivity = fxPricer.forwardFxRateSpotSensitivity(
        option.getPutCall().isCall() ? underlyingFx : underlyingFx.inverse(), ratesProvider);
    double strikeRate = option.getStrike();
    boolean isCall = option.getPutCall().isCall();
    SmileDeltaParameters smileAtTime = volatilities.getSmile().smileForExpiry(timeToExpiry);
    double[] strikes = smileAtTime.strike(forwardRate).toArray();
    double[] vols = smileAtTime.getVolatility().toArray();
    double volAtm = vols[1];
    double[] x = vannaVolgaWeights(forwardRate, strikeRate, timeToExpiry, volAtm, strikes);
    double priceFwd = BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, volAtm, isCall);
    double deltaFwd = BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volAtm, isCall);
    for (int i = 0; i < 3; i += 2) {
      double priceFwdAtm = BlackFormulaRepository.price(forwardRate, strikes[i], timeToExpiry, volAtm, isCall);
      double priceFwdSmile = BlackFormulaRepository.price(forwardRate, strikes[i], timeToExpiry, vols[i], isCall);
      priceFwd += x[i] * (priceFwdSmile - priceFwdAtm);
      double deltaFwdAtm = BlackFormulaRepository.delta(forwardRate, strikes[i], timeToExpiry, volAtm, isCall);
      double deltaFwdSmile = BlackFormulaRepository.delta(forwardRate, strikes[i], timeToExpiry, vols[i], isCall);
      deltaFwd += x[i] * (deltaFwdSmile - deltaFwdAtm);
    }
    double price = df * priceFwd;
    double delta = df * deltaFwd * fwdRateSpotSensitivity;
    double signedNotional = signedNotional(option);
    CurrencyAmount domestic = CurrencyAmount.of(currencyPair.getCounter(), (price - delta * spot) * signedNotional);
    CurrencyAmount foreign = CurrencyAmount.of(currencyPair.getBase(), delta * signedNotional);
    return MultiCurrencyAmount.of(domestic, foreign);
  }

  //-------------------------------------------------------------------------
  // signed notional amount to computed present value and value Greeks
  private double signedNotional(ResolvedFxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) *
        Math.abs(option.getUnderlying().getBaseCurrencyPayment().getAmount());
  }

  private double[] vannaVolgaWeights(
      double forward,
      double strike,
      double timeToExpiry,
      double volATM,
      double[] strikesReference) {

    double lnk21 = Math.log(strikesReference[1] / strikesReference[0]);
    double lnk31 = Math.log(strikesReference[2] / strikesReference[0]);
    double lnk32 = Math.log(strikesReference[2] / strikesReference[1]);
    double[] lnk = new double[3];
    for (int loopvv = 0; loopvv < 3; loopvv++) {
      lnk[loopvv] = Math.log(strikesReference[loopvv] / strike);
    }
    double[] x = new double[3];
    double vega0 = BlackFormulaRepository.vega(forward, strikesReference[0], timeToExpiry, volATM);
    double vegaFlat = BlackFormulaRepository.vega(forward, strike, timeToExpiry, volATM);
    double vega2 = BlackFormulaRepository.vega(forward, strikesReference[2], timeToExpiry, volATM);
    x[0] = vegaFlat * lnk[1] * lnk[2] / (vega0 * lnk21 * lnk31);
    x[2] = vegaFlat * lnk[0] * lnk[1] / (vega2 * lnk31 * lnk32);
    return x;
  }

  private void validate(RatesProvider ratesProvider, BlackFxOptionSmileVolatilities volatilities) {
    ArgChecker.isTrue(volatilities.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data must be for the same date");
    ArgChecker.isTrue(volatilities.getSmile().getStrikeCount() == 3, "the number of data points must be 3");
  }
}
