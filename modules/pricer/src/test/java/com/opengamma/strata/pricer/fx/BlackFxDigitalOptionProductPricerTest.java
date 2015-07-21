/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_USD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.volatility.surface.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.analytics.math.statistics.distribution.NormalDistribution;
import com.opengamma.analytics.math.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.FxDigitalOption;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

@Test
public class BlackFxDigitalOptionProductPricerTest {
  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final ImmutableRatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD();
  private static final double[] TIME_TO_EXPIRY = new double[] {0.01, 0.252, 0.501, 1.0, 2.0, 5.0 };
  private static final double[] ATM = {0.175, 0.185, 0.18, 0.17, 0.16, 0.16 };
  private static final double[] DELTA = new double[] {0.10, 0.25 };
  private static final double[][] RISK_REVERSAL = new double[][] { {-0.010, -0.0050 }, {-0.011, -0.0060 },
    {-0.012, -0.0070 }, {-0.013, -0.0080 }, {-0.014, -0.0090 }, {-0.014, -0.0090 } };
  private static final double[][] STRANGLE = new double[][] { {0.0300, 0.0100 }, {0.0310, 0.0110 }, {0.0320, 0.0120 },
    {0.0330, 0.0130 }, {0.0340, 0.0140 }, {0.0340, 0.0140 } };
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM =
      new SmileDeltaTermStructureParametersStrikeInterpolation(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE);

  private static final LocalDate VALUATION_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
  private static final double NOTIONAL = 1.0e6;
  private static final double STRIKE_RATE = 1.45;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, STRIKE_RATE);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 5, 9);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(13, 10);

  private static final FxDigitalOption OPTION_EURUSD_USD = FxDigitalOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(USD)
      .strike(STRIKE)
      .notional(NOTIONAL)
      .build();
  private static final FxDigitalOption OPTION_EURUSD_EUR = FxDigitalOption.builder()
      .putCall(PUT)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(EUR)
      .strike(STRIKE)
      .notional(NOTIONAL)
      .build();
  private static final FxDigitalOption OPTION_USDEUR_USD = FxDigitalOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(USD)
      .strike(STRIKE.inverse())
      .notional(NOTIONAL)
      .build();
  private static final FxDigitalOption OPTION_USDEUR_EUR = FxDigitalOption.builder()
      .putCall(PUT)
      .longShort(LONG)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .index(ECB_EUR_USD)
      .payoffCurrency(EUR)
      .strike(STRIKE.inverse())
      .notional(NOTIONAL)
      .build();

  private static final BlackFxDigitalOptionProductPricer PRICER = BlackFxDigitalOptionProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  //-------------------------------------------------------------------------
  public void test_price_presentValue() {
    double priceDom = PRICER.price(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double priceFor = PRICER.price(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(EUR, EXPIRY_DATE);
    double k = STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATE, STRIKE_RATE, f);
    double df = RATES_PROVIDER.discountFactor(USD, OPTION_EURUSD_USD.getPaymentDate());
    double priceAsset = df * f * NORMAL.getCDF(-(Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t));
    double priceCash = df * NORMAL.getCDF((Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t));
    assertEquals(priceDom, priceCash, TOL);
    assertEquals(priceFor, priceAsset, TOL);
    CurrencyAmount pvDom = PRICER.presentValue(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvFor = PRICER.presentValue(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double pvAsset = -NOTIONAL * priceAsset;
    double pvCash = -NOTIONAL * priceCash;
    assertEquals(pvDom.getCurrency(), USD);
    assertEquals(pvFor.getCurrency(), USD);
    assertEquals(pvDom.getAmount(), pvCash, NOTIONAL * TOL);
    assertEquals(pvFor.getAmount(), pvAsset, NOTIONAL * TOL);
  }

  public void test_price_presentValue_inverse() {
    double priceDom = PRICER.price(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double priceFor = PRICER.price(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(USD, EXPIRY_DATE);
    double k = 1d / STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(EUR, OPTION_EURUSD_USD.getPaymentDate());
    double priceAsset = df * f * NORMAL.getCDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t));
    double priceCash = df * NORMAL.getCDF(-(Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t));
    assertEquals(priceDom, priceCash, TOL);
    assertEquals(priceFor, priceAsset, TOL);
    CurrencyAmount pvDom = PRICER.presentValue(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvFor = PRICER.presentValue(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double pvAsset = -NOTIONAL * priceAsset;
    double pvCash = NOTIONAL * priceCash;
    assertEquals(pvDom.getCurrency(), EUR);
    assertEquals(pvFor.getCurrency(), EUR);
    assertEquals(pvDom.getAmount(), pvCash, NOTIONAL * TOL);
    assertEquals(pvFor.getAmount(), pvAsset, NOTIONAL * TOL);
  }

  public void test_price_presentValue_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 3);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(USD)
        .strike(STRIKE)
        .notional(NOTIONAL)
        .build();
    double price = PRICER.price(expired, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pv = PRICER.presentValue(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(price, 0d);
    assertEquals(pv.getAmount(), -0d);
  }

  //-------------------------------------------------------------------------
  public void test_delta_presentValueDelta() {
    double deltaDom = PRICER.delta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double deltaFor = PRICER.delta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(EUR, EXPIRY_DATE);
    double fs = f / RATES_PROVIDER.fxRate(CURRENCY_PAIR);
    double k = STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATE, STRIKE_RATE, f);
    double df = RATES_PROVIDER.discountFactor(USD, OPTION_EURUSD_USD.getPaymentDate());
    double deltaAsset = df * fs * (NORMAL.getCDF(-(Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t))
        - NORMAL.getPDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t)) / v / Math.sqrt(t));
    double deltaCash = df * fs * NORMAL.getPDF(-(Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t))
        / f / v / Math.sqrt(t);
    assertEquals(deltaDom, deltaCash, TOL);
    assertEquals(deltaFor, deltaAsset, TOL);
    CurrencyAmount pvDeltaDom = PRICER.presentValueDelta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaFor = PRICER.presentValueDelta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double pvDeltaAsset = -NOTIONAL * deltaAsset;
    double pvDeltaCash = -NOTIONAL * deltaCash;
    assertEquals(pvDeltaDom.getCurrency(), USD);
    assertEquals(pvDeltaFor.getCurrency(), USD);
    assertEquals(pvDeltaDom.getAmount(), pvDeltaCash, NOTIONAL * TOL);
    assertEquals(pvDeltaFor.getAmount(), pvDeltaAsset, NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_inverse() {
    double deltaDom = PRICER.delta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double deltaFor = PRICER.delta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(USD, EXPIRY_DATE);
    double fs = f / RATES_PROVIDER.fxRate(CURRENCY_PAIR.inverse());
    double k = 1d / STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(EUR, OPTION_EURUSD_USD.getPaymentDate());
    double deltaAsset = df * fs * (NORMAL.getCDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t))
        + NORMAL.getPDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t)) / v / Math.sqrt(t));
    double deltaCash = -df * fs * NORMAL.getPDF(-(Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t))
        / f / v / Math.sqrt(t);
    assertEquals(deltaDom, deltaCash, TOL);
    assertEquals(deltaFor, deltaAsset, TOL);
    CurrencyAmount pvDeltaDom = PRICER.presentValueDelta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaFor = PRICER.presentValueDelta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double pvDeltaAsset = -NOTIONAL * deltaAsset;
    double pvDeltaCash = NOTIONAL * deltaCash;
    assertEquals(pvDeltaDom.getCurrency(), EUR);
    assertEquals(pvDeltaFor.getCurrency(), EUR);
    assertEquals(pvDeltaDom.getAmount(), pvDeltaCash, NOTIONAL * TOL);
    assertEquals(pvDeltaFor.getAmount(), pvDeltaAsset, NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(PUT)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(EUR)
        .strike(STRIKE.inverse())
        .notional(NOTIONAL)
        .build();
    double delta = PRICER.delta(expired, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDelta = PRICER.presentValueDelta(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(delta, 0d);
    assertEquals(pvDelta.getAmount(), -0d);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivities pointDom = PRICER.presentValueSensitivity(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities sensiDom = RATES_PROVIDER.curveParameterSensitivity(pointDom);
    PointSensitivities pointFor = PRICER.presentValueSensitivity(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities sensiFor = RATES_PROVIDER.curveParameterSensitivity(pointFor);

    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    CurveCurrencyParameterSensitivities expectedDom = cal.sensitivity(RATES_PROVIDER,
        (p) -> PRICER.presentValue(OPTION_EURUSD_USD, (p), VOL_PROVIDER));
    CurveCurrencyParameterSensitivities expectedFor = cal.sensitivity(RATES_PROVIDER,
        (p) -> PRICER.presentValue(OPTION_EURUSD_EUR, (p), VOL_PROVIDER));
    CurrencyAmount pvVegaDom = PRICER.presentValueVega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities impliedVolSenseDom = cal.sensitivity(RATES_PROVIDER,
            (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(OPTION_EURUSD_USD, (p), VOL_PROVIDER)))
            .multipliedBy(-pvVegaDom.getAmount());
    CurrencyAmount pvVegaFor = PRICER.presentValueVega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities impliedVolSenseFor = cal.sensitivity(RATES_PROVIDER,
            (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(OPTION_EURUSD_EUR, (p), VOL_PROVIDER)))
            .multipliedBy(-pvVegaFor.getAmount());

    assertTrue(sensiDom.equalWithTolerance(expectedDom.combinedWith(impliedVolSenseDom), NOTIONAL * eps));
    assertTrue(sensiFor.equalWithTolerance(expectedFor.combinedWith(impliedVolSenseFor), NOTIONAL * eps));
  }

  public void test_presentValueSensitivity_inverse() {
    PointSensitivities pointDom = PRICER.presentValueSensitivity(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities sensiDom = RATES_PROVIDER.curveParameterSensitivity(pointDom);
    PointSensitivities pointFor = PRICER.presentValueSensitivity(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities sensiFor = RATES_PROVIDER.curveParameterSensitivity(pointFor);

    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    CurveCurrencyParameterSensitivities expectedDom = cal.sensitivity(RATES_PROVIDER,
        (p) -> PRICER.presentValue(OPTION_USDEUR_EUR, (p), VOL_PROVIDER));
    CurveCurrencyParameterSensitivities expectedFor = cal.sensitivity(RATES_PROVIDER,
        (p) -> PRICER.presentValue(OPTION_USDEUR_USD, (p), VOL_PROVIDER));
    CurrencyAmount pvVegaDom = PRICER.presentValueVega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities impliedVolSenseDom = cal.sensitivity(RATES_PROVIDER,
        (p) -> CurrencyAmount.of(EUR, PRICER.impliedVolatility(OPTION_USDEUR_EUR, (p), VOL_PROVIDER)))
        .multipliedBy(-pvVegaDom.getAmount());
    CurrencyAmount pvVegaFor = PRICER.presentValueVega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities impliedVolSenseFor = cal.sensitivity(RATES_PROVIDER,
        (p) -> CurrencyAmount.of(EUR, PRICER.impliedVolatility(OPTION_USDEUR_USD, (p), VOL_PROVIDER)))
        .multipliedBy(-pvVegaFor.getAmount());

    assertTrue(sensiDom.equalWithTolerance(expectedDom.combinedWith(impliedVolSenseDom), NOTIONAL * eps));
    assertTrue(sensiFor.equalWithTolerance(expectedFor.combinedWith(impliedVolSenseFor), NOTIONAL * eps));
  }

  public void test_presentValueSensitivity_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(PUT)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(EUR)
        .strike(STRIKE.inverse())
        .notional(NOTIONAL)
        .build();
    PointSensitivities computed = PRICER.presentValueSensitivity(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_gamma_presentValueGamma() {
    double gammaDom = PRICER.gamma(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double gammaFor = PRICER.gamma(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(EUR, EXPIRY_DATE);
    double fs = f / RATES_PROVIDER.fxRate(CURRENCY_PAIR);
    double k = STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(USD, OPTION_EURUSD_USD.getPaymentDate());
    double d1 = (Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t);
    double d2 = (Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t);
    double gammaAsset = df * fs * fs * NORMAL.getPDF(d1) * d2 / f / v / v / t;
    double gammaCash = -df * fs * fs * NORMAL.getPDF(d2) * d1 / f / f / v / v / t;
    assertEquals(gammaDom, gammaCash, TOL);
    assertEquals(gammaFor, gammaAsset, TOL);
    CurrencyAmount pvGammaDom = PRICER.presentValueGamma(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaFor = PRICER.presentValueGamma(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double pvGammaAsset = -NOTIONAL * gammaAsset;
    double pvGammaCash = -NOTIONAL * gammaCash;
    assertEquals(pvGammaDom.getCurrency(), USD);
    assertEquals(pvGammaFor.getCurrency(), USD);
    assertEquals(pvGammaDom.getAmount(), pvGammaCash, NOTIONAL * TOL);
    assertEquals(pvGammaFor.getAmount(), pvGammaAsset, NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_inverse() {
    double gammaDom = PRICER.gamma(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double gammaFor = PRICER.gamma(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(USD, EXPIRY_DATE);
    double fs = f / RATES_PROVIDER.fxRate(CURRENCY_PAIR.inverse());
    double k = 1d / STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(EUR, OPTION_EURUSD_USD.getPaymentDate());
    double d1 = (Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t);
    double d2 = (Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t);
    double gammaAsset = -df * fs * fs * NORMAL.getPDF(d1) * d2 / f / v / v / t;
    double gammaCash = df * fs * fs * NORMAL.getPDF(d2) * d1 / f / f / v / v / t;
    assertEquals(gammaDom, gammaCash, TOL);
    assertEquals(gammaFor, gammaAsset, TOL);
    CurrencyAmount pvGammaDom = PRICER.presentValueGamma(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaFor = PRICER.presentValueGamma(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double pvGammaAsset = -NOTIONAL * gammaAsset;
    double pvGammaCash = NOTIONAL * gammaCash;
    assertEquals(pvGammaDom.getCurrency(), EUR);
    assertEquals(pvGammaFor.getCurrency(), EUR);
    assertEquals(pvGammaDom.getAmount(), pvGammaCash, NOTIONAL * TOL);
    assertEquals(pvGammaFor.getAmount(), pvGammaAsset, NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(PUT)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(EUR)
        .strike(STRIKE.inverse())
        .notional(NOTIONAL)
        .build();
    double gamma = PRICER.gamma(expired, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(gamma, 0d);
    assertEquals(pvGamma.getAmount(), -0d);
  }

  //-------------------------------------------------------------------------
  public void test_vega_presentValueVega() {
    double vegaDom = PRICER.vega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double vegaFor = PRICER.vega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(EUR, EXPIRY_DATE);
    double k = STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(USD, OPTION_EURUSD_USD.getPaymentDate());
    double d1 = (Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t);
    double d2 = (Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t);
    double vegaAsset = df * NORMAL.getPDF(d1) * d2 * f / v;
    double vegaCash = -df * NORMAL.getPDF(d2) * d1 / v;
    assertEquals(vegaDom, vegaCash, TOL);
    assertEquals(vegaFor, vegaAsset, TOL);
    CurrencyAmount pvVegaDom = PRICER.presentValueVega(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaFor = PRICER.presentValueVega(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double pvVegaAsset = -NOTIONAL * vegaAsset;
    double pvVegaCash = -NOTIONAL * vegaCash;
    assertEquals(pvVegaDom.getCurrency(), USD);
    assertEquals(pvVegaFor.getCurrency(), USD);
    assertEquals(pvVegaDom.getAmount(), pvVegaCash, NOTIONAL * TOL);
    assertEquals(pvVegaFor.getAmount(), pvVegaAsset, NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_inverse() {
    double vegaDom = PRICER.vega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double vegaFor = PRICER.vega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(USD, EXPIRY_DATE);
    double k = 1d / STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(EUR, OPTION_EURUSD_USD.getPaymentDate());
    double d1 = (Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t);
    double d2 = (Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t);
    double vegaAsset = -df * NORMAL.getPDF(d1) * d2 * f / v;
    double vegaCash = df * NORMAL.getPDF(d2) * d1 / v;
    assertEquals(vegaDom, vegaCash, TOL);
    assertEquals(vegaFor, vegaAsset, TOL);
    CurrencyAmount pvVegaDom = PRICER.presentValueVega(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVegaFor = PRICER.presentValueVega(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double pvVegaAsset = -NOTIONAL * vegaAsset;
    double pvVegaCash = NOTIONAL * vegaCash;
    assertEquals(pvVegaDom.getCurrency(), EUR);
    assertEquals(pvVegaFor.getCurrency(), EUR);
    assertEquals(pvVegaDom.getAmount(), pvVegaCash, NOTIONAL * TOL);
    assertEquals(pvVegaFor.getAmount(), pvVegaAsset, NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(PUT)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(EUR)
        .strike(STRIKE.inverse())
        .notional(NOTIONAL)
        .build();
    double vega = PRICER.vega(expired, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVega = PRICER.presentValueVega(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(vega, 0d);
    assertEquals(pvVega.getAmount(), -0d);
  }

  //-------------------------------------------------------------------------
  public void test_theta_presentValueTheta() {
    double thetaDom = PRICER.theta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    double thetaFor = PRICER.theta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(EUR, EXPIRY_DATE);
    double k = STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(USD, OPTION_EURUSD_USD.getPaymentDate());
    double thetaAsset = -df * NORMAL.getPDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t)) * f *
        (0.5 * Math.log(f / k) / v / t / Math.sqrt(t) - 0.25 * v / Math.sqrt(t));
    double thetaCash = df * NORMAL.getPDF((Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t)) *
        (0.5 * Math.log(f / k) / v / t / Math.sqrt(t) + 0.25 * v / Math.sqrt(t));
    assertEquals(thetaDom, thetaCash, TOL);
    assertEquals(thetaFor, thetaAsset, TOL);
    CurrencyAmount pvThetaDom = PRICER.presentValueTheta(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaFor = PRICER.presentValueTheta(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double pvThetaAsset = -NOTIONAL * thetaAsset;
    double pvThetaCash = -NOTIONAL * thetaCash;
    assertEquals(pvThetaDom.getCurrency(), USD);
    assertEquals(pvThetaFor.getCurrency(), USD);
    assertEquals(pvThetaDom.getAmount(), pvThetaCash, NOTIONAL * TOL);
    assertEquals(pvThetaFor.getAmount(), pvThetaAsset, NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_inverse() {
    double thetaDom = PRICER.theta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    double thetaFor = PRICER.theta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double f = RATES_PROVIDER.fxIndexRates(ECB_EUR_USD).rate(USD, EXPIRY_DATE);
    double k = 1d / STRIKE_RATE;
    double t = VOL_PROVIDER.relativeTime(EXPIRY_DATE, EXPIRY_TIME, ZONE);
    double v = VOL_PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), EXPIRY_DATE, k, f);
    double df = RATES_PROVIDER.discountFactor(EUR, OPTION_EURUSD_USD.getPaymentDate());
    double thetaAsset = df * NORMAL.getPDF((Math.log(f / k) + 0.5 * v * v * t) / v / Math.sqrt(t)) * f *
        (0.5 * Math.log(f / k) / v / t / Math.sqrt(t) - 0.25 * v / Math.sqrt(t));
    double thetaCash = -df * NORMAL.getPDF((Math.log(f / k) - 0.5 * v * v * t) / v / Math.sqrt(t)) *
        (0.5 * Math.log(f / k) / v / t / Math.sqrt(t) + 0.25 * v / Math.sqrt(t));
    assertEquals(thetaDom, thetaCash, TOL);
    assertEquals(thetaFor, thetaAsset, TOL);
    CurrencyAmount pvThetaDom = PRICER.presentValueTheta(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaFor = PRICER.presentValueTheta(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    double pvThetaAsset = -NOTIONAL * thetaAsset;
    double pvThetaCash = NOTIONAL * thetaCash;
    assertEquals(pvThetaDom.getCurrency(), EUR);
    assertEquals(pvThetaFor.getCurrency(), EUR);
    assertEquals(pvThetaDom.getAmount(), pvThetaCash, NOTIONAL * TOL);
    assertEquals(pvThetaFor.getAmount(), pvThetaAsset, NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(PUT)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(EUR)
        .strike(STRIKE.inverse())
        .notional(NOTIONAL)
        .build();
    double theta = PRICER.theta(expired, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(theta, 0d);
    assertEquals(pvTheta.getAmount(), -0d);
  }

  //-------------------------------------------------------------------------
  // TODO complete this
  public void test_currencyExposure() {
    MultiCurrencyAmount computedDom = PRICER.currencyExposure(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedFor = PRICER.currencyExposure(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);

    PointSensitivities pointDom = PRICER.presentValueSensitivity(OPTION_EURUSD_USD, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedDom = RATES_PROVIDER.currencyExposure(pointDom);
    PointSensitivities pointFor = PRICER.presentValueSensitivity(OPTION_EURUSD_EUR, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedFor = RATES_PROVIDER.currencyExposure(pointFor);

    assertEquals(computedDom.getAmount(USD).getAmount(), expectedDom.getAmount(USD).getAmount(), NOTIONAL * TOL);
    assertEquals(computedDom.getAmount(EUR).getAmount(), expectedDom.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(computedFor.getAmount(USD).getAmount(), expectedFor.getAmount(USD).getAmount(), NOTIONAL * TOL);
    assertEquals(computedFor.getAmount(EUR).getAmount(), expectedFor.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_inverse() {
    MultiCurrencyAmount computedDom = PRICER.currencyExposure(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedFor = PRICER.currencyExposure(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);

    PointSensitivities pointDom = PRICER.presentValueSensitivity(OPTION_USDEUR_EUR, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedDom = RATES_PROVIDER.currencyExposure(pointDom);
    PointSensitivities pointFor = PRICER.presentValueSensitivity(OPTION_USDEUR_USD, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedFor = RATES_PROVIDER.currencyExposure(pointFor);

    assertEquals(computedDom.getAmount(USD).getAmount(), expectedDom.getAmount(USD).getAmount(), NOTIONAL * TOL);
    assertEquals(computedDom.getAmount(EUR).getAmount(), expectedDom.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(computedFor.getAmount(USD).getAmount(), expectedFor.getAmount(USD).getAmount(), NOTIONAL * TOL);
    assertEquals(computedFor.getAmount(EUR).getAmount(), expectedFor.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_expired() {
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    FxDigitalOption expired = FxDigitalOption.builder()
        .putCall(PUT)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .index(ECB_EUR_USD)
        .payoffCurrency(USD)
        .strike(STRIKE.inverse())
        .notional(NOTIONAL)
        .build();
    MultiCurrencyAmount ce = PRICER.currencyExposure(expired, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(ce, MultiCurrencyAmount.empty());
  }

  //-------------------------------------------------------------------------
  //TODO bucketed vega
  //TODO implied vol
}
