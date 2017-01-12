/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.impl.option.BlackDigitalPriceFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxDigitalOption;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test {@link BlackFxDigitalOptionProductPricer}.
 */
@Test
public class BlackFxDigitalOptionProductPricerTest {

  // define inputs
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2014, 5, 9, 13, 10, 0, 0, ZONE);
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final ZonedDateTime VAL_DATETIME_AFTER = EXPIRY.plusDays(1);
  private static final LocalDate VAL_DATE_AFTER = VAL_DATETIME_AFTER.toLocalDate();
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atTime(VAL_TIME).atZone(ZONE);
  // fx market data fixed valdate
  private static final RatesProvider RATES_PROVIDER = RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE);
  // fx market data fixed expirydate
  private static final RatesProvider RATES_PROVIDER_EXPIRY =
      RatesProviderFxDataSets.createProviderEURUSD(EXPIRY.toLocalDate());
  // fx market data fixed expirydate + 1
  private static final RatesProvider RATES_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE_AFTER);
  // fx volsmile 6 slices @ valdate
  private static final BlackFxOptionSmileVolatilities VOLS =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(VAL_DATETIME);
  // fx volsmile 6 slices @ Expirydate
  private static final BlackFxOptionSmileVolatilities VOLS_EXPIRY =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(EXPIRY);
  // fx volsmile 6 slices @ Expirydate + 1
  private static final BlackFxOptionSmileVolatilities VOLS_AFTER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider6(VAL_DATETIME_AFTER);
  // fx volsmile 6 delta based slices @ testdate
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      FxVolatilitySmileDataSet.getSmileDeltaTermStructure6();
  // USD PER EUR
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final double NOTIONAL = 1.0e6;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double STRIKE_RATE_HIGH = 1.70;
  private static final double STRIKE_RATE_LOW = 1.15;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_HIGH = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_HIGH);
  private static final CurrencyAmount USD_AMOUNT_LOW = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_LOW);
  private static final ResolvedFxSingle FX_PRODUCT_HIGH = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT_HIGH, PAYMENT_DATE);
  private static final ResolvedFxSingle FX_PRODUCT_LOW = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT_LOW, PAYMENT_DATE);


  /////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////
  // Short + High spot
  private static final ResolvedFxDigitalOption CALL_OTM = ResolvedFxDigitalOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH)
      .build();
  CurrencyPair CP1 = CALL_OTM.getCurrencyPair();
  LocalDate expirydate1 = CALL_OTM.getExpiryDate();
  ResolvedFxSingle spot1 = CALL_OTM.getUnderlying();
  LongShort LS1 = CALL_OTM.getLongShort();


  // Long + Low Spot
  private static final ResolvedFxDigitalOption CALL_ITM = ResolvedFxDigitalOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_LOW)
      .build();
  CurrencyPair CP2 = CALL_ITM.getCurrencyPair();
  LocalDate expirydate2 = CALL_ITM.getExpiryDate();
  ResolvedFxSingle spot2 = CALL_ITM.getUnderlying();
  LongShort LS2 = CALL_ITM.getLongShort();

  // Short + Low spot
  private static final ResolvedFxDigitalOption PUT_OTM = ResolvedFxDigitalOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_LOW.inverse())
      .build();
  CurrencyPair CP3 = PUT_OTM.getCurrencyPair();
  LocalDate expirydate3 = PUT_OTM.getExpiryDate();
  ResolvedFxSingle spot3 = PUT_OTM.getUnderlying();
  LongShort LS3 = PUT_OTM.getLongShort();


  // Long + High spot
  private static final ResolvedFxDigitalOption PUT_ITM = ResolvedFxDigitalOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY)
      .underlying(FX_PRODUCT_HIGH.inverse())
      .build();
  CurrencyPair CP4 = PUT_ITM.getCurrencyPair();
  LocalDate expirydate4 = PUT_ITM.getExpiryDate();
  ResolvedFxSingle spot4 = PUT_ITM.getUnderlying();
  LongShort LS4 = PUT_ITM.getLongShort();


  // default pricer Black Digital
  private static final BlackFxDigitalOptionProductPricer PRICER = BlackFxDigitalOptionProductPricer.DEFAULT;
  private static final double TOL = 1.0e-13;
  private static final double FD_EPS = 1.0e-7;

  // Curve PV01
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);




  // Test the PV
  public void test_price_presentValue() {
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvPutOtm = PRICER.presentValue(PUT_OTM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    CurveInterpolator strikeinterp = SMILE_TERM.getStrikeInterpolator();
    CurveInterpolator timeinterp = SMILE_TERM.getTimeInterpolator();
    ImmutableList<SmileDeltaParameters> volterm = SMILE_TERM.getVolatilityTerm();
    SmileDeltaParameters time = SMILE_TERM.smileForExpiry(timeToExpiry);


    double volHigh = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    double volLow = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_LOW, forward);

    double expectedPriceCallOtm =
        df * BlackDigitalPriceFormulaRepository.price(forward, STRIKE_RATE_HIGH, timeToExpiry, volHigh, true, 1.0);
    double expectedPricePutOtm =
        df * BlackDigitalPriceFormulaRepository.price(forward, STRIKE_RATE_LOW, timeToExpiry, volLow, false, 1.0);
    double expectedPvCallOtm = -NOTIONAL * df *
        BlackDigitalPriceFormulaRepository.price(forward, STRIKE_RATE_HIGH, timeToExpiry, volHigh, true, 1.0);
    double expectedPvPutOtm = -NOTIONAL * df *
        BlackDigitalPriceFormulaRepository.price(forward, STRIKE_RATE_LOW, timeToExpiry, volLow, false, 1.0);


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
    double CallOtmStrike = CALL_OTM.getStrike();

    // Check Deep OTM Call
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(priceCallOtm, 0d, TOL);
    assertEquals(pvCallOtm.getAmount(), 0d, NOTIONAL * TOL);

    // Check Deep ITM Call
    double priceCallItm = PRICER.price(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvCallItm = PRICER.presentValue(CALL_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(priceCallItm,  df, TOL);
    assertEquals(pvCallItm.getAmount(),  df * NOTIONAL, NOTIONAL * TOL);

    // Check Deep OTM Put
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvPutOtm = PRICER.presentValue(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(pricePutOtm, 0d, TOL);
    assertEquals(pvPutOtm.getAmount(), 0d, NOTIONAL * TOL);

    // Check Deep ITM Put
    double pricePutItm = PRICER.price(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount pvPutItm = PRICER.presentValue(PUT_ITM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertEquals(pricePutItm,  df , TOL);
    assertEquals(pvPutItm.getAmount(), df  * NOTIONAL, NOTIONAL * TOL);
  }

  public void test_price_presentValue_afterExpiry() {
    double price = PRICER.price(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount pv = PRICER.presentValue(CALL_OTM, RATES_PROVIDER_AFTER, VOLS_AFTER);
    assertEquals(price, 0d, NOTIONAL * TOL);
    assertEquals(pv.getAmount(), 0d, NOTIONAL * TOL);
  }


  public void test_price_presentValue_parity1() {
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    double priceCallItm = PRICER.price(CALL_ITM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutOtm = PRICER.price(PUT_OTM, RATES_PROVIDER, VOLS);
    double discountfactor = RATES_PROVIDER.discountFactor(CURRENCY_PAIR.getBase(), EXPIRY.toLocalDate());
    boolean parity = false;
    double totalPV = priceCallItm + pricePutOtm;
    double totalFV = totalPV / discountfactor;
    totalFV = totalFV - 1.0;
    if (totalFV < 0.001) {
      parity = true;
    }
  }

  public void test_price_presentValue_parity2() {
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer()
        .forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER).fxRate(CURRENCY_PAIR);
    double priceCallOtm = PRICER.price(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pvCallOtm = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    double pricePutItm = PRICER.price(PUT_ITM, RATES_PROVIDER, VOLS);
    double discountfactor = RATES_PROVIDER.discountFactor(CURRENCY_PAIR.getBase(), EXPIRY.toLocalDate());
    boolean parity = false;
    double totalPV = priceCallOtm + pricePutItm;
    double totalFV = totalPV / discountfactor;
    totalFV = totalFV - 1.0;
    if (totalFV < 0.001) {
      parity = true;
    }

  }


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
    double expectedDeltaCall = dfFor * BlackDigitalPriceFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1.0);
    double expectedDeltaPut = dfFor * BlackDigitalPriceFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1.0);
    double expectedPvDeltaCall = -NOTIONAL * dfFor
        * BlackDigitalPriceFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1.0);
    double expectedPvDeltaPut = NOTIONAL * dfFor
        * BlackDigitalPriceFormulaRepository.delta(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1.0);
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



  public void test_presentValueSensitivity_atExpiry() {
    // call
    PointSensitivities pointCall = PRICER.presentValueSensitivityRatesStickyStrike(CALL_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedCall = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointCall);
    CurrencyParameterSensitivities expectedCall = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(CALL_OTM, (p), VOLS_EXPIRY));
    assertTrue(computedCall.equalWithTolerance(expectedCall, NOTIONAL * FD_EPS));
    // put
    PointSensitivities pointPut = PRICER.presentValueSensitivityRatesStickyStrike(PUT_OTM, RATES_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyParameterSensitivities computedPut = RATES_PROVIDER_EXPIRY.parameterSensitivity(pointPut);
    CurrencyParameterSensitivities expectedPut = FD_CAL.sensitivity(
        RATES_PROVIDER_EXPIRY, (p) -> PRICER.presentValue(PUT_OTM, (p), VOLS_EXPIRY));
    assertTrue(computedPut.equalWithTolerance(expectedPut, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivity_afterExpiry() {
    PointSensitivities point = PRICER.presentValueSensitivityRatesStickyStrike(CALL_ITM, RATES_PROVIDER_AFTER, VOLS_AFTER);
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

    double expectedGammaCall = dfFor * dfFor / dfDom *
        BlackDigitalPriceFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1);
    double expectedGammaPut = dfFor * dfFor / dfDom *
        BlackDigitalPriceFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1);

    // L
    double expectedPvGammaCall = -NOTIONAL * dfFor * dfFor / dfDom *
        BlackDigitalPriceFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1);
    // S
    double expectedPvGammaPut =  NOTIONAL * dfFor * dfFor / dfDom *
        BlackDigitalPriceFormulaRepository.gamma(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1);

    // OTMC gamma
    assertEquals(gammaCall, expectedGammaCall, TOL);
    assertEquals(pvGammaCall.getCurrency(), USD);
    assertEquals(pvGammaCall.getAmount(), expectedPvGammaCall, NOTIONAL * TOL);

    // ITMP Gamma
    assertEquals(gammaPut, expectedGammaPut, TOL);
    assertEquals(pvGammaPut.getCurrency(), USD);
    assertEquals(pvGammaPut.getAmount(), expectedPvGammaPut, NOTIONAL * TOL);

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

    double expectedVegaC = dfDom * BlackDigitalPriceFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1.0);
    double expectedVegaP = dfDom * BlackDigitalPriceFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1.0);
    double expectedPvVegaC = -NOTIONAL * dfDom *
        BlackDigitalPriceFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1.0);
    double expectedPvVegaP = NOTIONAL * dfDom *
        BlackDigitalPriceFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1.0);

    assertEquals(vegaCall, expectedVegaC, TOL);
    assertEquals(pvVegaCall.getCurrency(), USD);
    assertEquals(pvVegaCall.getAmount(), expectedPvVegaC, NOTIONAL * TOL);
    assertEquals(vegaPut, expectedVegaP, TOL);
    assertEquals(pvVegaPut.getCurrency(), USD);
    assertEquals(pvVegaPut.getAmount(), expectedPvVegaP, NOTIONAL * TOL);
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
    // OTMC sensitivity
    FxOptionSensitivity computedCall = (FxOptionSensitivity)
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_OTM, RATES_PROVIDER, VOLS);
    // ITMP sens
    FxOptionSensitivity computedPut = (FxOptionSensitivity)
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_ITM, RATES_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY);
    double df = RATES_PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double forward = PRICER.getDiscountingFxSingleProductPricer().forwardFxRate(FX_PRODUCT_HIGH, RATES_PROVIDER)
        .fxRate(CURRENCY_PAIR);
    double vol = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE_HIGH, forward);
    //call vega sensitivity
    FxOptionSensitivity expectedcall = FxOptionSensitivity.of(
        VOLS.getName(), CURRENCY_PAIR, timeToExpiry, STRIKE_RATE_HIGH, forward, USD,
        -NOTIONAL * df * BlackDigitalPriceFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, true, 1.0));
    FxOptionSensitivity expectedput = FxOptionSensitivity.of(
        VOLS.getName(), CURRENCY_PAIR, timeToExpiry, STRIKE_RATE_HIGH, forward, USD,
        -NOTIONAL * df * BlackDigitalPriceFormulaRepository.vega(forward, STRIKE_RATE_HIGH, timeToExpiry, vol, false, 1.0));

    assertTrue(computedCall.build().equalWithTolerance(expectedcall.build(), NOTIONAL * TOL));
    assertTrue(computedPut.build().equalWithTolerance(expectedput.build().multipliedBy(-1d), NOTIONAL * TOL));
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
  public void test_currencyExposure() {
    MultiCurrencyAmount computedPricer = PRICER.currencyExposure(CALL_OTM, RATES_PROVIDER, VOLS);
    CurrencyAmount pv = PRICER.presentValue(CALL_OTM, RATES_PROVIDER, VOLS);
    PointSensitivities point = PRICER.presentValueSensitivityRatesStickyStrike(CALL_OTM, RATES_PROVIDER, VOLS);
    MultiCurrencyAmount computedPoint = RATES_PROVIDER.currencyExposure(point).plus(pv);
    assertEquals(computedPricer.getAmount(EUR).getAmount(), computedPoint.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    assertEquals(computedPricer.getAmount(USD).getAmount(), computedPoint.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }
}
