/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Test {@link BlackFxVanillaOptionProductPricer}.
 */
@Test
public class BlackFxVanillaOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2014, 5, 9, 13, 10, 0, 0, ZONE);
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final ZonedDateTime VAL_DATETIME_AFTER = EXPIRY.plusDays(1);
  private static final LocalDate VAL_DATE_AFTER = VAL_DATETIME_AFTER.toLocalDate();
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atTime(VAL_TIME).atZone(ZONE);
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE);
  private static final RatesProvider RATES_PROVIDER_EXPIRY =
      RatesProviderFxDataSets.createProviderEURUSD(EXPIRY.toLocalDate());
  private static final RatesProvider RATES_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE_AFTER);
  private static final BlackFxOptionSmileVolatilities VOLS =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(VAL_DATETIME);
  private static final BlackFxOptionSmileVolatilities VOLS_EXPIRY =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(EXPIRY);
  private static final BlackFxOptionSmileVolatilities VOLS_AFTER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(VAL_DATETIME_AFTER);
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      FxVolatilitySmileDataSet.getSmileDeltaTermStructure6();

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final double NOTIONAL = 1.0e6;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double STRIKE_RATE_HIGH = 1.44;
  private static final double STRIKE_RATE_LOW = 1.36;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_HIGH = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_HIGH);
  private static final CurrencyAmount USD_AMOUNT_LOW = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_LOW);
  private static final ResolvedFxSingle FX_PRODUCT_HIGH = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT_HIGH, PAYMENT_DATE);
  private static final ResolvedFxSingle FX_PRODUCT_LOW = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT_LOW, PAYMENT_DATE);
  private static final ResolvedFxVanillaOption CALL_OTM = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH)
      .build();
  private static final ResolvedFxVanillaOption CALL_ITM = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_LOW)
      .build();
  private static final ResolvedFxVanillaOption PUT_OTM = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_LOW.inverse())
      .build();
  private static final ResolvedFxVanillaOption PUT_ITM = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH.inverse())
      .build();
  private static final BlackFxVanillaOptionProductPricer PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;
  private static final double FD_EPS = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  public void test_price_presentValue() {
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvPutOtm = PRICER.presentValue(PUT_OTM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    double volHigh = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double volLow = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_LOW, forward);
    double expectedPriceCallOtm =
        df * BlackFormulaRepository.price(forward, STRIKE_RATE_HIGH, timeToExpiry, volHigh, true);
    double expectedPricePutOtm =
        df * BlackFormulaRepository.price(forward, STRIKE_RATE_LOW, timeToExpiry, volLow, false);
    double expectedPvCallOtm = -NOTIONAL * df *
        BlackFormulaRepository.price(forward, STRIKE_RATE_HIGH, timeToExpiry, volHigh, true);
    double expectedPvPutOtm = -NOTIONAL * df *
        BlackFormulaRepository.price(forward, STRIKE_RATE_LOW, timeToExpiry, volLow, false);
    assertEquals(priceCallOtm, expectedPriceCallOtm, TOL);
    assertEquals(pvCallOtm.getCurrency(), USD);
    assertEquals(pvCallOtm.getAmount(), expectedPvCallOtm, NOTIONAL * TOL);
    assertEquals(pricePutOtm, expectedPricePutOtm, TOL);
    assertEquals(pvPutOtm.getCurrency(), USD);
    assertEquals(pvPutOtm.getAmount(), expectedPvPutOtm, NOTIONAL * TOL);
  }

  public void test_price_presentValue_atExpiry() {
    double df = RATES_PROVIDER_EXPIRY.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER_EXPIRY).fxRate(CURRENCY_PAIR);
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(priceCallOtm, 0d, TOL);
    assertEquals(pvCallOtm.getAmount(), 0d, NOTIONAL * TOL);
    double priceCallItm = PRICER.price(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvCallItm = PRICER.presentValue(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(priceCallItm, df * (forward - STRIKE_RATE_LOW), TOL);
    assertEquals(pvCallItm.getAmount(), df * (forward - STRIKE_RATE_LOW) * NOTIONAL, NOTIONAL * TOL);
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvPutOtm = PRICER.presentValue(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(pricePutOtm, 0d, TOL);
    assertEquals(pvPutOtm.getAmount(), 0d, NOTIONAL * TOL);
    double pricePutItm = PRICER.price(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvPutItm = PRICER.presentValue(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(pricePutItm, df * (STRIKE_RATE_HIGH - forward), TOL);
    assertEquals(pvPutItm.getAmount(), df * (STRIKE_RATE_HIGH - forward) * NOTIONAL, NOTIONAL * TOL);
  }

  public void test_price_presentValue_afterExpiry() {
    double price = PRICER.price(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pv = PRICER.presentValue(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(price, 0d, NOTIONAL * TOL);
    assertEquals(pv.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_price_presentValue_parity() {
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutItm = PRICER.price(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvPutItm = PRICER.presentValue(PUT_ITM, RATES_PROVIDER, VOLS);
    assertEquals(priceCallOtm - pricePutItm, df * (forward - STRIKE_RATE_HIGH), TOL);
    assertEquals(-pvCallOtm.getAmount() - pvPutItm.getAmount(),
        df * (forward - STRIKE_RATE_HIGH) * NOTIONAL, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_delta_presentValueDelta() {
    double deltaCall = PRICER.delta(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvDeltaCall = PRICER.presentValueDelta(CALL_OTM, RATES_PROVIDER, VOLS);
    double deltaPut = PRICER.delta(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPut = PRICER.presentValueDelta(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfFor = RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedDeltaCall = dfFor * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true);
    double expectedDeltaPut = dfFor * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false);
    double expectedPvDeltaCall = -NOTIONAL * dfFor
        * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true);
    double expectedPvDeltaPut = NOTIONAL * dfFor
        * BlackFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false);
    assertEquals(deltaCall, expectedDeltaCall, TOL);
    assertEquals(pvDeltaCall.getCurrency(), USD);
    assertEquals(pvDeltaCall.getAmount(), expectedPvDeltaCall, NOTIONAL * TOL);
    assertEquals(deltaPut, expectedDeltaPut, TOL);
    assertEquals(pvDeltaPut.getCurrency(), USD);
    assertEquals(pvDeltaPut.getAmount(), expectedPvDeltaPut, NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_atExpiry() {
    double dfFor = RATES_PROVIDER_EXPIRY.discountFactor(EUR, PAYMENT_DATE);
    double deltaCallOtm = PRICER.delta(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaCallOtm = PRICER.presentValueDelta(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(deltaCallOtm, 0d, TOL);
    assertEquals(pvDeltaCallOtm.getAmount(), 0d, NOTIONAL * TOL);
    double deltaCallItm = PRICER.delta(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaCallItm = PRICER.presentValueDelta(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(deltaCallItm, dfFor, TOL);
    assertEquals(pvDeltaCallItm.getAmount(), NOTIONAL * dfFor, NOTIONAL * TOL);
    double deltaPutItm = PRICER.delta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaPutItm = PRICER.presentValueDelta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(deltaPutItm, -dfFor, TOL);
    assertEquals(pvDeltaPutItm.getAmount(), -NOTIONAL * dfFor, NOTIONAL * TOL);
    double deltaPutOtm = PRICER.delta(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvDeltaPutOtm = PRICER.presentValueDelta(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(deltaPutOtm, 0d, TOL);
    assertEquals(pvDeltaPutOtm.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_delta_presentValueDelta_afterExpiry() {
    double delta = PRICER.delta(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvDelta = PRICER.presentValueDelta(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(delta, 0d, TOL);
    assertEquals(pvDelta.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    // call
    PointSensitivities pointCall = PRICER.presentValueSensitivityRates(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedCall = RATES_PROVIDER.parameterSensitivity(pointCall);
    CurrencyParameterSensitivities expectedCall = FD_CAL.sensitivity(
        RATES_PROVIDER, (p) -> PRICER.presentValue(CALL_OTM, (p), VOLS));
    // contribution via implied volatility, to be subtracted.
    CurrencyAmount pvVegaCall = PRICER.presentValueVega(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities impliedVolSenseCall =
        FD_CAL.sensitivity(RATES_PROVIDER,
            (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(CALL_OTM, (p), VOLS)))
            .multipliedBy(-pvVegaCall.getAmount());
    assertTrue(computedCall.equalWithTolerance(expectedCall.combinedWith(impliedVolSenseCall), NOTIONAL * FD_EPS));
    // put
    PointSensitivities pointPut = PRICER.presentValueSensitivityRates(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedPut = RATES_PROVIDER.parameterSensitivity(pointPut);
    CurrencyParameterSensitivities expectedPut = FD_CAL.sensitivity(
        RATES_PROVIDER, (p) -> PRICER.presentValue(PUT_OTM, (p), VOLS));
    // contribution via implied volatility, to be subtracted.
    CurrencyAmount pvVegaPut = PRICER.presentValueVega(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyParameterSensitivities impliedVolSensePut = FD_CAL.sensitivity(
        RATES_PROVIDER, (p) -> CurrencyAmount.of(USD, PRICER.impliedVolatility(PUT_OTM, (p), VOLS)))
        .multipliedBy(-pvVegaPut.getAmount());
    assertTrue(computedPut.equalWithTolerance(expectedPut.combinedWith(impliedVolSensePut), NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivity_atExpiry() {
    // call
    PointSensitivities pointCall = PRICER.presentValueSensitivityRates(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedCall = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointCall);
    CurrencyParameterSensitivities expectedCall = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(CALL_OTM, (p), VOLS_EXPIRY));
    assertTrue(computedCall.equalWithTolerance(expectedCall, NOTIONAL * FD_EPS));
    // put
    PointSensitivities pointPut = PRICER.presentValueSensitivityRates(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedPut = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointPut);
    CurrencyParameterSensitivities expectedPut = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(PUT_OTM, (p), VOLS_EXPIRY));
    assertTrue(computedPut.equalWithTolerance(expectedPut, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivity_afterExpiry() {
    PointSensitivities point = PRICER.presentValueSensitivityRates(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(point, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_gamma_presentValueGamma() {
    double gammaCall = PRICER.gamma(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvGammaCall = PRICER.presentValueGamma(CALL_OTM, RATES_PROVIDER, VOLS);
    double gammaPut = PRICER.gamma(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvGammaPut = PRICER.presentValueGamma(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double dfFor = RATES_PROVIDER.discountFactor(EUR, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedGamma = dfFor * dfFor / dfDom *
        BlackFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    double expectedPvGamma = -NOTIONAL * dfFor * dfFor / dfDom *
        BlackFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertEquals(gammaCall, expectedGamma, TOL);
    assertEquals(pvGammaCall.getCurrency(), USD);
    assertEquals(pvGammaCall.getAmount(), expectedPvGamma, NOTIONAL * TOL);
    assertEquals(gammaPut, expectedGamma, TOL);
    assertEquals(pvGammaPut.getCurrency(), USD);
    assertEquals(pvGammaPut.getAmount(), -expectedPvGamma, NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_atExpiry() {
    double gamma = PRICER.gamma(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(gamma, 0d, TOL);
    assertEquals(pvGamma.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_gamma_presentValueGamma_afterExpiry() {
    double gamma = PRICER.gamma(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvGamma = PRICER.presentValueGamma(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(gamma, 0d, TOL);
    assertEquals(pvGamma.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_vega_presentValueVega() {
    double vegaCall = PRICER.vega(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvVegaCall = PRICER.presentValueVega(CALL_OTM, RATES_PROVIDER, VOLS);
    double vegaPut = PRICER.vega(PUT_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvVegaPut = PRICER.presentValueVega(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedVega = dfDom * BlackFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    double expectedPvVega = -NOTIONAL * dfDom *
        BlackFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertEquals(vegaCall, expectedVega, TOL);
    assertEquals(pvVegaCall.getCurrency(), USD);
    assertEquals(pvVegaCall.getAmount(), expectedPvVega, NOTIONAL * TOL);
    assertEquals(vegaPut, expectedVega, TOL);
    assertEquals(pvVegaPut.getCurrency(), USD);
    assertEquals(pvVegaPut.getAmount(), -expectedPvVega, NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_atExpiry() {
    double vega = PRICER.vega(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvVega = PRICER.presentValueVega(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(vega, 0d, TOL);
    assertEquals(pvVega.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_vega_presentValueVega_afterExpiry() {
    double vega = PRICER.vega(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvVega = PRICER.presentValueVega(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(vega, 0d, TOL);
    assertEquals(pvVega.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityBlackVolatility() {
    FxOptionSensitivity computedCall = (FxOptionSensitivity)
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_OTM, RATES_PROVIDER, VOLS);
    FxOptionSensitivity computedPut = (FxOptionSensitivity)
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    FxOptionSensitivity expected = FxOptionSensitivity.of(
        VOLS.getName(), CURRENCY_PAIR, timeToExpiry, STRIKE_RATE_HIGH, forward, USD,
        -NOTIONAL * df * BlackFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol));
    assertTrue(computedCall.build().equalWithTolerance(expected.build(), NOTIONAL * TOL));
    assertTrue(computedPut.build().equalWithTolerance(expected.build().multipliedBy(-1d), NOTIONAL * TOL));
  }

  public void test_presentValueSensitivityBlackVolatility_atExpiry() {
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(point, PointSensitivityBuilder.none());
  }

  public void test_presentValueSensitivityBlackVolatility_afterExpiry() {
    PointSensitivityBuilder point =
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(point, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_theta_presentValueTheta() {
    double theta = PRICER.theta(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(CALL_OTM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double dfDom = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double expectedTheta = dfDom * BlackFormulaRepository.driftlessTheta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertEquals(theta, expectedTheta, TOL);
    double expectedPvTheta = -NOTIONAL * dfDom *
        BlackFormulaRepository.driftlessTheta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol);
    assertEquals(pvTheta.getCurrency(), USD);
    assertEquals(pvTheta.getAmount(), expectedPvTheta, NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_atExpiry() {
    double theta = PRICER.theta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(theta, 0d, TOL);
    assertEquals(pvTheta.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_theta_presentValueTheta_afterExpiry() {
    double theta = PRICER.theta(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pvTheta = PRICER.presentValueTheta(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(theta, 0d, TOL);
    assertEquals(pvTheta.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double computedCall = PRICER.impliedVolatility(CALL_OTM, RATES_PROVIDER, VOLS);
    double computedPut = PRICER.impliedVolatility(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double expected = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    assertEquals(computedCall, expected);
    assertEquals(computedPut, expected);
  }

  public void test_impliedVolatility_atExpiry() {
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY));
  }

  public void test_impliedVolatility_afterExpiry() {
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computedPricer = PRICER.currencyExposure(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pv = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    PointSensitivities point = PRICER.presentValueSensitivityRates(CALL_OTM, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount computedPoint = RATES_PROVIDER.currencyExposure(point).plus(pv);
    assertEquals(computedPricer.getAmount(EUR).getAmount(), computedPoint.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(computedPricer.getAmount(USD).getAmount(), computedPoint.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

}
