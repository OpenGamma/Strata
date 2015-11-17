/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxVanillaOption;

/**
 * Test {@link BlackFxVanillaOptionProductPricer}.
 */
@Test
public class BlackFxVanillaOptionProductPricerTest {

  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD();

  private static final double[] TIME_TO_EXPIRY = new double[] {0.01, 0.252, 0.501, 1.0, 2.0, 5.0 };
  private static final double[] ATM = {0.175, 0.185, 0.18, 0.17, 0.16, 0.16 };
  private static final double[] DELTA = new double[] {0.10, 0.25 };
  private static final double[][] RISK_REVERSAL = new double[][] {
      {-0.010, -0.0050}, {-0.011, -0.0060}, {-0.012, -0.0070},
      {-0.013, -0.0080}, {-0.014, -0.0090}, {-0.014, -0.0090}};
  private static final double[][] STRANGLE = new double[][] {
      {0.0300, 0.0100}, {0.0310, 0.0110}, {0.0320, 0.0120},
      {0.0330, 0.0130}, {0.0340, 0.0140}, {0.0340, 0.0140}};
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM =
      new SmileDeltaTermStructureParametersStrikeInterpolation(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE);

  private static final LocalDate VALUATION_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * FX_MATRIX.fxRate(EUR, USD));
  private static final FxSingle FX_PRODUCT = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  private static final double STRIKE_RATE = 1.45;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, STRIKE_RATE);
  private static final PutCall CALL = PutCall.CALL;
  private static final LongShort SHORT = LongShort.SHORT;
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 5, 9);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(13, 10);
  private static final ZonedDateTime EXPIRY_DATE_TIME = EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(ZONE);
  private static final FxVanillaOption OPTION_PRODUCT = FxVanillaOption.builder()
      .putCall(CALL)
      .longShort(SHORT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .underlying(FX_PRODUCT)
      .strike(STRIKE)
      .build();

  private static final BlackFxVanillaOptionProductPricer PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;

  //-------------------------------------------------------------------------
  public void test_price_presentValue() {
    double price = PRICER.price(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pv = PRICER.presentValue(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    double expectedPrice = df * BlackFormulaRepository.price(forward, STRIKE_RATE, timeToExpiry, vol, CALL.isCall());
    double expectedPv = -NOTIONAL * df
        * BlackFormulaRepository.price(forward, STRIKE_RATE, timeToExpiry, vol, CALL.isCall());
    assertEquals(price, expectedPrice, TOL);
    assertEquals(pv.getCurrency(), USD);
    assertEquals(pv.getAmount(), expectedPv, NOTIONAL * TOL);
    // The direction of strike will be modified, thus the same result is expected
    FxVanillaOption option1 = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(FX_PRODUCT)
        .strike(STRIKE.inverse())
        .build();
    CurrencyAmount pv1 = PRICER.presentValue(option1, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pv1.getCurrency(), pv.getCurrency());
    assertEquals(pv1.getAmount(), pv.getAmount(), NOTIONAL * TOL);
    // long option 
    FxVanillaOption option2 = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LongShort.LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(FX_PRODUCT)
        .strike(STRIKE)
        .build();
    CurrencyAmount pv2 = PRICER.presentValue(option2, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(pv2.getCurrency(), pv.getCurrency());
    assertEquals(pv2.getAmount(), -pv.getAmount(), NOTIONAL * TOL);
  }

  public void test_price_presentValue_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    double price = PRICER.price(callItm, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pv = PRICER.presentValue(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(price, 0d, NOTIONAL * TOL);
    assertEquals(pv.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta() {
    double delta = PRICER.delta(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDelta = PRICER.presentValueDelta(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double dfFor = RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    double expectedDelta = dfFor * BlackFormulaRepository.delta(forward, STRIKE_RATE, timeToExpiry, vol, CALL.isCall());
    assertEquals(delta, expectedDelta, TOL);
    double expectedPvDelta = -NOTIONAL * dfFor
        * BlackFormulaRepository.delta(forward, STRIKE_RATE, timeToExpiry, vol, CALL.isCall());
    assertEquals(pvDelta.getCurrency(), USD);
    assertEquals(pvDelta.getAmount(), expectedPvDelta, NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    double delta = PRICER.delta(callItm, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDelta = PRICER.presentValueDelta(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(delta, 0d, NOTIONAL * TOL);
    assertEquals(pvDelta.getAmount(), 0d, NOTIONAL * TOL);
    PointSensitivities point = PRICER.presentValueSensitivity(callItm, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities sensi = RATES_PROVIDER.curveParameterSensitivity(point);
    assertEquals(sensi, CurveCurrencyParameterSensitivities.empty());
  }

  public void test_presentValueSensitivity() {
    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator cal = new RatesFiniteDifferenceSensitivityCalculator(eps);
    PointSensitivities point = PRICER.presentValueSensitivity(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computed = RATES_PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = cal.sensitivity((ImmutableRatesProvider) RATES_PROVIDER,
        (p) -> PRICER.presentValue(OPTION_PRODUCT, (p), VOL_PROVIDER));
    // contribution via implied volatility, to be subtracted.
    CurrencyAmount pvVega = PRICER.presentValueVega(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities impliedVolSense =
        cal.sensitivity((ImmutableRatesProvider) RATES_PROVIDER,
            (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(OPTION_PRODUCT, (p), VOL_PROVIDER)))
            .multipliedBy(-pvVega.getAmount());
    assertTrue(computed.equalWithTolerance(expected.combinedWith(impliedVolSense), NOTIONAL * eps));
  }

  public void test_presentValueSensitivity_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    PointSensitivities point = PRICER.presentValueSensitivity(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(point, PointSensitivities.empty());
  }

  public void test_gamma_presentValueGamma() {
    double gamma = PRICER.gamma(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double dfFor = RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    double expectedGamma = dfFor * dfFor / dfDom *
        BlackFormulaRepository.gamma(forward, STRIKE_RATE, timeToExpiry, vol);
    assertEquals(gamma, expectedGamma, TOL);
    double expectedPvGamma = -NOTIONAL * dfFor * dfFor / dfDom *
        BlackFormulaRepository.gamma(forward, STRIKE_RATE, timeToExpiry, vol);
    assertEquals(pvGamma.getCurrency(), USD);
    assertEquals(pvGamma.getAmount(), expectedPvGamma, NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    double gamma = PRICER.gamma(callItm, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGamma1 = PRICER.presentValueGamma(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(gamma, 0d, NOTIONAL * TOL);
    assertEquals(pvGamma1.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega() {
    double vega = PRICER.vega(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVega = PRICER.presentValueVega(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    double expectedVega = dfDom * BlackFormulaRepository.vega(forward, STRIKE_RATE, timeToExpiry, vol);
    assertEquals(vega, expectedVega, TOL);
    double expectedPvVega = -NOTIONAL * dfDom * BlackFormulaRepository.vega(forward, STRIKE_RATE, timeToExpiry, vol);
    assertEquals(pvVega.getCurrency(), USD);
    assertEquals(pvVega.getAmount(), expectedPvVega, NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    double vega = PRICER.vega(callItm, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvVega = PRICER.presentValueVega(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(vega, 0d, NOTIONAL * TOL);
    assertEquals(pvVega.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivityBlackVolatility() {
    FxOptionSensitivity computed = (FxOptionSensitivity)
        PRICER.presentValueSensitivityBlackVolatility(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    FxOptionSensitivity expected = FxOptionSensitivity.of(CURRENCY_PAIR, EXPIRY_DATE_TIME, STRIKE_RATE, forward, USD,
        -NOTIONAL * df * BlackFormulaRepository.vega(forward, STRIKE_RATE, timeToExpiry, vol));
    assertTrue(computed.build().equalWithTolerance(expected.build(), NOTIONAL * TOL));
  }

  public void test_presentValueSensitivityBlackVolatility_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityBlackVolatility(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(point, PointSensitivityBuilder.none());
  }

  public void test_theta_presentValueTheta() {
    double theta = PRICER.theta(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    double expectedTheta = dfDom * BlackFormulaRepository.driftlessTheta(forward, STRIKE_RATE, timeToExpiry, vol);
    assertEquals(theta, expectedTheta, TOL);
    double expectedPvTheta = -NOTIONAL * dfDom *
        BlackFormulaRepository.driftlessTheta(forward, STRIKE_RATE, timeToExpiry, vol);
    assertEquals(pvTheta.getCurrency(), USD);
    assertEquals(pvTheta.getAmount(), expectedPvTheta, NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    double theta = PRICER.theta(callItm, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(callItm, RATES_PROVIDER, VOL_PROVIDER);
    assertEquals(theta, 0d, NOTIONAL * TOL);
    assertEquals(pvTheta.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_impliedVolatility() {
    double computed = PRICER.impliedVolatility(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATE_TIME);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double expected = SMILE_TERM.getVolatility(timeToExpiry, STRIKE_RATE, forward);
    assertEquals(computed, expected);
  }

  public void test_impliedVolatility_afterExpiry() {
    LocalDate paymentDate = LocalDate.of(2014, 1, 23);
    FxSingle fx = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, paymentDate);
    LocalDate expiryDate = LocalDate.of(2014, 1, 21);
    double strikeRate = 1.30;
    FxRate strike = FxRate.of(EUR, USD, strikeRate);
    FxVanillaOption callItm = FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(SHORT)
        .expiryDate(expiryDate)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .underlying(fx)
        .strike(strike)
        .build();
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(callItm, RATES_PROVIDER, VOL_PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computedPricer = PRICER.currencyExposure(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pv = PRICER.presentValue(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    PointSensitivities point = PRICER.presentValueSensitivity(OPTION_PRODUCT, RATES_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedPoint = RATES_PROVIDER.currencyExposure(point).plus(pv);
    assertEquals(computedPricer.getAmount(EUR).getAmount(), computedPoint.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(computedPricer.getAmount(USD).getAmount(), computedPoint.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

}
