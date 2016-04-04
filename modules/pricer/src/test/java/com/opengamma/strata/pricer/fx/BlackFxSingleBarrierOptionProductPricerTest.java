/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchAssetPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchCashPriceFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.BarrierType;
import com.opengamma.strata.product.fx.KnockType;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fx.SimpleConstantContinuousBarrier;

/**
 * Test {@link BlackFxVanillaOptionProductPricer}.
 */
@Test
public class BlackFxSingleBarrierOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2014, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  // providers
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(VAL_DATETIME);
  private static final ImmutableRatesProvider RATE_PROVIDER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(VAL_DATE);
  // providers - valuation at expiry
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER_EXPIRY =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(EXPIRY_DATETIME);
  private static final ImmutableRatesProvider RATE_PROVIDER_EXPIRY =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(EXPIRY_DATE);
  // provider - valuation after expiry
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER_AFTER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(EXPIRY_DATETIME.plusDays(1));
  private static final ImmutableRatesProvider RATE_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(EXPIRY_DATE.plusDays(1));

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final double SPOT = RATE_PROVIDER.fxRate(CURRENCY_PAIR);
  private static final double NOTIONAL = 100_000_000d;
  private static final double LEVEL_LOW = 1.35;
  private static final double LEVEL_HIGH = 1.5;
  private static final SimpleConstantContinuousBarrier BARRIER_DKI =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, LEVEL_LOW);
  private static final SimpleConstantContinuousBarrier BARRIER_DKO =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, LEVEL_LOW);
  private static final SimpleConstantContinuousBarrier BARRIER_UKI =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, LEVEL_HIGH);
  private static final SimpleConstantContinuousBarrier BARRIER_UKO =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, LEVEL_HIGH);
  private static final double REBATE_AMOUNT = 50_000d;
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, REBATE_AMOUNT);
  private static final CurrencyAmount REBATE_BASE = CurrencyAmount.of(EUR, REBATE_AMOUNT);
  private static final double STRIKE_RATE = 1.45;
  // call
  private static final CurrencyAmount EUR_AMOUNT_REC = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_PAY = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE);
  private static final ResolvedFxSingle FX_PRODUCT = ResolvedFxSingle.of(EUR_AMOUNT_REC, USD_AMOUNT_PAY, PAY_DATE);
  private static final ResolvedFxVanillaOption CALL = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT)
      .build();
  private static final ResolvedFxSingleBarrierOption CALL_DKI =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKI, REBATE);
  private static final ResolvedFxSingleBarrierOption CALL_DKI_BASE =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKI, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption CALL_DKO =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKO, REBATE);
  private static final ResolvedFxSingleBarrierOption CALL_DKO_BASE =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKO, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption CALL_UKI =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKI, REBATE);
  private static final ResolvedFxSingleBarrierOption CALL_UKI_BASE =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKI, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption CALL_UKO =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKO, REBATE);
  private static final ResolvedFxSingleBarrierOption CALL_UKO_BASE =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKO, REBATE_BASE);
  // put
  private static final CurrencyAmount EUR_AMOUNT_PAY = CurrencyAmount.of(EUR, -NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_REC = CurrencyAmount.of(USD, NOTIONAL * STRIKE_RATE);
  private static final ResolvedFxSingle FX_PRODUCT_INV = ResolvedFxSingle.of(EUR_AMOUNT_PAY, USD_AMOUNT_REC, PAY_DATE);
  private static final ResolvedFxVanillaOption PUT = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT_INV)
      .build();
  private static final ResolvedFxSingleBarrierOption PUT_DKI =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKI, REBATE);
  private static final ResolvedFxSingleBarrierOption PUT_DKI_BASE =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKI, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption PUT_DKO =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKO, REBATE);
  private static final ResolvedFxSingleBarrierOption PUT_DKO_BASE =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKO, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption PUT_UKI =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKI, REBATE);
  private static final ResolvedFxSingleBarrierOption PUT_UKI_BASE =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKI, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption PUT_UKO =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKO, REBATE);
  private static final ResolvedFxSingleBarrierOption PUT_UKO_BASE =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKO, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption[] OPTION_ALL = new ResolvedFxSingleBarrierOption[] {
    CALL_DKI, CALL_DKI_BASE, CALL_DKO, CALL_DKO_BASE, CALL_UKI, CALL_UKI_BASE, CALL_UKO, CALL_UKO_BASE,
    PUT_DKI, PUT_DKI_BASE, PUT_DKO, PUT_DKO_BASE, PUT_UKI, PUT_UKI_BASE, PUT_UKO, PUT_UKO_BASE };

  private static final BlackFxSingleBarrierOptionProductPricer PRICER = BlackFxSingleBarrierOptionProductPricer.DEFAULT;
  private static final BlackFxVanillaOptionProductPricer VANILLA_PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;
  private static final BlackBarrierPriceFormulaRepository BARRIER_PRICER = new BlackBarrierPriceFormulaRepository();
  private static final BlackOneTouchAssetPriceFormulaRepository ASSET_REBATE_PRICER = new BlackOneTouchAssetPriceFormulaRepository();
  private static final BlackOneTouchCashPriceFormulaRepository CASH_REBATE_PRICER = new BlackOneTouchCashPriceFormulaRepository();
  private static final double TOL = 1.0e-12;
  private static final double FD_EPS = 1.0e-6;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL = new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  public void test_price_presentValue() {
    double computedPriceCall = PRICER.price(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    double computedPricePut = PRICER.price(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvCall = PRICER.presentValue(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvPut = PRICER.presentValue(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash =
        CASH_REBATE_PRICER.price(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO);
    double expectedAsset =
        ASSET_REBATE_PRICER.price(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI);
    double expectedPriceCall = BARRIER_PRICER.price(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI) + rebateRate * expectedCash;
    double expectedPricePut = BARRIER_PRICER.price(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO) + rebateRate * expectedAsset;
    assertEquals(computedPriceCall, expectedPriceCall, TOL);
    assertEquals(computedPricePut, expectedPricePut, TOL);
    assertEquals(computedPvCall.getCurrency(), USD);
    assertEquals(computedPvPut.getCurrency(), USD);
    assertEquals(computedPvCall.getAmount(), expectedPriceCall * NOTIONAL, TOL);
    assertEquals(computedPvPut.getAmount(), -expectedPricePut * NOTIONAL, TOL);
  }

  public void test_price_presentValue_atExpiry() {
    double computedPriceCall = PRICER.price(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double computedPriceCallZero = PRICER.price(CALL_UKO, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double computedPricePut = PRICER.price(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvCall = PRICER.presentValue(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvCallZero = PRICER.presentValue(CALL_UKO, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvPut = PRICER.presentValue(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double expectedPriceCall = REBATE_AMOUNT / NOTIONAL;
    double expectedPricePut = STRIKE_RATE - SPOT;
    assertEquals(computedPriceCall, expectedPriceCall, TOL);
    assertEquals(computedPriceCallZero, 0d, TOL);
    assertEquals(computedPricePut, expectedPricePut, TOL);
    assertEquals(computedPvCall.getAmount(), expectedPriceCall * NOTIONAL, TOL);
    assertEquals(computedPvCallZero.getAmount(), 0d * NOTIONAL, TOL);
    assertEquals(computedPvPut.getAmount(), -expectedPricePut * NOTIONAL, TOL);
  }

  public void test_price_presentValue_afterExpiry() {
    double computedPriceCall = PRICER.price(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    double computedPricePut = PRICER.price(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvCall = PRICER.presentValue(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvPut = PRICER.presentValue(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    assertEquals(computedPriceCall, 0d, TOL);
    assertEquals(computedPricePut, 0d, TOL);
    assertEquals(computedPvCall.getAmount(), 0d, TOL);
    assertEquals(computedPvPut.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_inOutParity() {
    ResolvedFxSingleBarrierOption callDKI = ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKI);
    ResolvedFxSingleBarrierOption callDKO = ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKO);
    ResolvedFxSingleBarrierOption callUKI = ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKI);
    ResolvedFxSingleBarrierOption callUKO = ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKO);
    ResolvedFxSingleBarrierOption putDKI = ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKI);
    ResolvedFxSingleBarrierOption putDKO = ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKO);
    ResolvedFxSingleBarrierOption putUKI = ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKI);
    ResolvedFxSingleBarrierOption putUKO = ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKO);
    //pv
    CurrencyAmount pvCall = VANILLA_PRICER.presentValue(CALL, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvCallUp = PRICER.presentValue(callUKO, RATE_PROVIDER, VOL_PROVIDER)
        .plus(PRICER.presentValue(callUKI, RATE_PROVIDER, VOL_PROVIDER));
    CurrencyAmount computedPvCallDw = PRICER.presentValue(callDKO, RATE_PROVIDER, VOL_PROVIDER)
        .plus(PRICER.presentValue(callDKI, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedPvCallUp.getAmount(), pvCall.getAmount(), NOTIONAL * TOL);
    assertEquals(computedPvCallDw.getAmount(), pvCall.getAmount(), NOTIONAL * TOL);
    CurrencyAmount pvPut = VANILLA_PRICER.presentValue(PUT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvPutUp = PRICER.presentValue(putUKO, RATE_PROVIDER, VOL_PROVIDER)
        .plus(PRICER.presentValue(putUKI, RATE_PROVIDER, VOL_PROVIDER));
    CurrencyAmount computedPvPutDw = PRICER.presentValue(putDKO, RATE_PROVIDER, VOL_PROVIDER)
        .plus(PRICER.presentValue(putDKI, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedPvPutUp.getAmount(), pvPut.getAmount(), NOTIONAL * TOL);
    assertEquals(computedPvPutDw.getAmount(), pvPut.getAmount(), NOTIONAL * TOL);
    // curve sensitivity
    PointSensitivities pvSensiCall = VANILLA_PRICER.presentValueSensitivity(CALL, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivities computedPvSensiCallUp = PRICER.presentValueSensitivity(callUKO, RATE_PROVIDER, VOL_PROVIDER)
        .combinedWith(PRICER.presentValueSensitivity(callUKI, RATE_PROVIDER, VOL_PROVIDER)).build();
    PointSensitivities computedPvSensiCallDw = PRICER.presentValueSensitivity(callDKO, RATE_PROVIDER, VOL_PROVIDER)
        .combinedWith(PRICER.presentValueSensitivity(callDKI, RATE_PROVIDER, VOL_PROVIDER)).build();
    assertTrue(RATE_PROVIDER.curveParameterSensitivity(pvSensiCall).equalWithTolerance(
        RATE_PROVIDER.curveParameterSensitivity(computedPvSensiCallUp), TOL * NOTIONAL));
    assertTrue(RATE_PROVIDER.curveParameterSensitivity(pvSensiCall).equalWithTolerance(
        RATE_PROVIDER.curveParameterSensitivity(computedPvSensiCallDw), TOL * NOTIONAL));
    PointSensitivities pvSensiPut = VANILLA_PRICER.presentValueSensitivity(PUT, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivities computedPvSensiPutUp = PRICER.presentValueSensitivity(putUKO, RATE_PROVIDER, VOL_PROVIDER)
        .combinedWith(PRICER.presentValueSensitivity(putUKI, RATE_PROVIDER, VOL_PROVIDER)).build();
    PointSensitivities computedPvSensiPutDw = PRICER.presentValueSensitivity(putDKO, RATE_PROVIDER, VOL_PROVIDER)
        .combinedWith(PRICER.presentValueSensitivity(putDKI, RATE_PROVIDER, VOL_PROVIDER)).build();
    assertTrue(RATE_PROVIDER.curveParameterSensitivity(pvSensiPut).equalWithTolerance(
        RATE_PROVIDER.curveParameterSensitivity(computedPvSensiPutUp), TOL * NOTIONAL));
    assertTrue(RATE_PROVIDER.curveParameterSensitivity(pvSensiPut).equalWithTolerance(
        RATE_PROVIDER.curveParameterSensitivity(computedPvSensiPutDw), TOL * NOTIONAL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      PointSensitivityBuilder point = PRICER.presentValueSensitivity(option, RATE_PROVIDER, VOL_PROVIDER);
      CurveCurrencyParameterSensitivities computed = RATE_PROVIDER.curveParameterSensitivity(point.build());
      CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER,
          p -> PRICER.presentValue(option, p, VOL_PROVIDER));
      double pvVega = ((FxOptionSensitivity)
          PRICER.presentValueSensitivityVolatility(option, RATE_PROVIDER, VOL_PROVIDER)).getSensitivity();
      CurveCurrencyParameterSensitivities sensiViaFwd = FD_CAL.sensitivity(RATE_PROVIDER,
          p -> CurrencyAmount.of(USD, VANILLA_PRICER.impliedVolatility(CALL, p, VOL_PROVIDER))).multipliedBy(-pvVega);
      expected = expected.combinedWith(sensiViaFwd);
      assertTrue(computed.equalWithTolerance(expected, FD_EPS * NOTIONAL * 10d));
    }
  }

  public void test_presentValueSensitivity_atExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      PointSensitivityBuilder point = PRICER.presentValueSensitivity(option, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
      CurveCurrencyParameterSensitivities computed = RATE_PROVIDER_EXPIRY.curveParameterSensitivity(point.build());
      CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER_EXPIRY,
          p -> PRICER.presentValue(option, p, VOL_PROVIDER_EXPIRY));
      assertTrue(computed.equalWithTolerance(expected, FD_EPS * NOTIONAL * 10d));
    }
  }

  public void test_presentValueSensitivity_afterExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      PointSensitivityBuilder point = PRICER.presentValueSensitivity(option, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
      assertEquals(point, PointSensitivityBuilder.none());
    }
  }

  //-------------------------------------------------------------------------
  public void test_vega_presentValueSensitivityVolatility() {
    double computedVegaCall = PRICER.vega(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    FxOptionSensitivity computedCall =
        (FxOptionSensitivity) PRICER.presentValueSensitivityVolatility(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    double computedVegaPut = PRICER.vega(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    FxOptionSensitivity computedPut =
        (FxOptionSensitivity) PRICER.presentValueSensitivityVolatility(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(3);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(3);
    double expectedCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(4) + rebateRate * expectedCash;
    double expectedPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(4) + rebateRate * expectedAsset;
    assertEquals(computedVegaCall, expectedCall, TOL);
    assertEquals(computedCall.getSensitivity(), expectedCall * NOTIONAL, TOL * NOTIONAL);
    assertEquals(computedCall.getCurrency(), USD);
    assertEquals(computedCall.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(computedCall.getStrike(), STRIKE_RATE);
    assertEquals(computedCall.getForward(), forward, TOL);
    assertEquals(computedCall.getExpiryDateTime(), EXPIRY_DATETIME);
    assertEquals(computedVegaPut, expectedPut, TOL);
    assertEquals(computedPut.getSensitivity(), -expectedPut * NOTIONAL, TOL * NOTIONAL);
    assertEquals(computedPut.getCurrency(), USD);
    assertEquals(computedPut.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(computedPut.getStrike(), STRIKE_RATE);
    assertEquals(computedPut.getForward(), forward, TOL);
    assertEquals(computedPut.getExpiryDateTime(), EXPIRY_DATETIME);
  }

  public void test_vega_presentValueSensitivityVolatility_atExpiry() {
    double computedVegaCall = PRICER.vega(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    PointSensitivityBuilder computedCall =
        PRICER.presentValueSensitivityVolatility(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double computedVegaPut = PRICER.vega(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    PointSensitivityBuilder computedPut =
        PRICER.presentValueSensitivityVolatility(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    assertEquals(computedVegaCall, 0d);
    assertEquals(computedCall, PointSensitivityBuilder.none());
    assertEquals(computedVegaPut, 0d);
    assertEquals(computedPut, PointSensitivityBuilder.none());
  }

  public void test_vega_presentValueSensitivityVolatility_afterExpiry() {
    double computedVegaCall = PRICER.vega(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    PointSensitivityBuilder computedCall =
        PRICER.presentValueSensitivityVolatility(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    double computedVegaPut = PRICER.vega(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    PointSensitivityBuilder computedPut =
        PRICER.presentValueSensitivityVolatility(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    assertEquals(computedVegaCall, 0d);
    assertEquals(computedCall, PointSensitivityBuilder.none());
    assertEquals(computedVegaPut, 0d);
    assertEquals(computedPut, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      CurrencyAmount pv = PRICER.presentValue(option, RATE_PROVIDER, VOL_PROVIDER);
      MultiCurrencyAmount computed = PRICER.currencyExposure(option, RATE_PROVIDER, VOL_PROVIDER);
      FxMatrix fxMatrix = FxMatrix.builder().addRate(EUR, USD, SPOT + FD_EPS).build();
      ImmutableRatesProvider provBumped = RATE_PROVIDER.toBuilder().fxRateProvider(fxMatrix).build();
      CurrencyAmount pvBumped = PRICER.presentValue(option, provBumped, VOL_PROVIDER);
      double ceCounterFD = pvBumped.getAmount() - pv.getAmount();
      double ceBaseFD = pvBumped.getAmount() * (SPOT + FD_EPS) - pv.getAmount() * SPOT;
      assertEquals(computed.getAmount(EUR).getAmount() * FD_EPS, ceCounterFD, NOTIONAL * FD_EPS);
      assertEquals(-(computed.getAmount(USD).getAmount() - pv.getAmount()) * FD_EPS, ceBaseFD, NOTIONAL * FD_EPS);
    }
  }

  public void test_currencyExposure_atExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      CurrencyAmount pv = PRICER.presentValue(option, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
      MultiCurrencyAmount computed = PRICER.currencyExposure(option, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
      FxMatrix fxMatrix = FxMatrix.builder().addRate(EUR, USD, SPOT + FD_EPS).build();
      ImmutableRatesProvider provBumped = RATE_PROVIDER_EXPIRY.toBuilder().fxRateProvider(fxMatrix).build();
      CurrencyAmount pvBumped = PRICER.presentValue(option, provBumped, VOL_PROVIDER_EXPIRY);
      double ceCounterFD = pvBumped.getAmount() - pv.getAmount();
      double ceBaseFD = pvBumped.getAmount() * (SPOT + FD_EPS) - pv.getAmount() * SPOT;
      assertEquals(computed.getAmount(EUR).getAmount() * FD_EPS, ceCounterFD, NOTIONAL * FD_EPS);
      assertEquals(-(computed.getAmount(USD).getAmount() - pv.getAmount()) * FD_EPS, ceBaseFD, NOTIONAL * FD_EPS);
    }
  }

  public void test_currencyExposure_afterExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      MultiCurrencyAmount computed = PRICER.currencyExposure(option, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
      assertEquals(computed, MultiCurrencyAmount.empty());
    }
  }

  //-------------------------------------------------------------------------
  public void test_delta_presentValueDelta() {
    double computedDeltaCall = PRICER.delta(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    double computedDeltaPut = PRICER.delta(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvDeltaCall = PRICER.presentValueDelta(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvDeltaPut = PRICER.presentValueDelta(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(0);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(0);
    double expectedDeltaCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(0) + rebateRate * expectedCash;
    double expectedDeltaPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(0) + rebateRate * expectedAsset;
    assertEquals(computedDeltaCall, expectedDeltaCall, TOL);
    assertEquals(computedDeltaPut, expectedDeltaPut, TOL);
    assertEquals(computedPvDeltaCall.getCurrency(), USD);
    assertEquals(computedPvDeltaPut.getCurrency(), USD);
    assertEquals(computedPvDeltaCall.getAmount(), expectedDeltaCall * NOTIONAL, TOL);
    assertEquals(computedPvDeltaPut.getAmount(), -expectedDeltaPut * NOTIONAL, TOL);
  }

  public void test_delta_presentValueDelta_atExpiry() {
    double computedDeltaCall = PRICER.delta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double computedDeltaPut = PRICER.delta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvDeltaCall = PRICER.presentValueDelta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvDeltaPut = PRICER.presentValueDelta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double expectedDeltaPut = -1d;
    assertEquals(computedDeltaCall, 0d, TOL);
    assertEquals(computedDeltaPut, expectedDeltaPut, TOL);
    assertEquals(computedPvDeltaCall.getAmount(), 0d, TOL);
    assertEquals(computedPvDeltaPut.getAmount(), -expectedDeltaPut * NOTIONAL, TOL);
  }

  public void test_delta_presentValueDelta_afterExpiry() {
    double computedDeltaCall = PRICER.delta(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    double computedDeltaPut = PRICER.delta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvDeltaCall = PRICER.presentValueDelta(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvDeltaPut = PRICER.presentValueDelta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    assertEquals(computedDeltaCall, 0d, TOL);
    assertEquals(computedDeltaPut, 0d, TOL);
    assertEquals(computedPvDeltaCall.getAmount(), 0d, TOL);
    assertEquals(computedPvDeltaPut.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_gamma_presentValueGamma() {
    double computedGammaCall = PRICER.gamma(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    double computedGammaPut = PRICER.gamma(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvGammaCall = PRICER.presentValueGamma(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvGammaPut = PRICER.presentValueGamma(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(5);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(5);
    double expectedGammaCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(6) + rebateRate * expectedCash;
    double expectedGammaPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(6) + rebateRate * expectedAsset;
    assertEquals(computedGammaCall, expectedGammaCall, TOL);
    assertEquals(computedGammaPut, expectedGammaPut, TOL);
    assertEquals(computedPvGammaCall.getCurrency(), USD);
    assertEquals(computedPvGammaPut.getCurrency(), USD);
    assertEquals(computedPvGammaCall.getAmount(), expectedGammaCall * NOTIONAL, TOL);
    assertEquals(computedPvGammaPut.getAmount(), -expectedGammaPut * NOTIONAL, TOL);
  }

  public void test_gamma_presentValueGamma_atExpiry() {
    double computedGammaCall = PRICER.gamma(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double computedGammaPut = PRICER.gamma(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvGammaCall = PRICER.presentValueGamma(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvGammaPut = PRICER.presentValueGamma(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    assertEquals(computedGammaCall, 0d, TOL);
    assertEquals(computedGammaPut, 0d, TOL);
    assertEquals(computedPvGammaCall.getAmount(), 0d, TOL);
    assertEquals(computedPvGammaPut.getAmount(), 0d, TOL);
  }

  public void test_gamma_presentValueGamma_afterExpiry() {
    double computedGammaCall = PRICER.gamma(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    double computedGammaPut = PRICER.gamma(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvGammaCall = PRICER.presentValueGamma(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvGammaPut = PRICER.presentValueGamma(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    assertEquals(computedGammaCall, 0d, TOL);
    assertEquals(computedGammaPut, 0d, TOL);
    assertEquals(computedPvGammaCall.getAmount(), 0d, TOL);
    assertEquals(computedPvGammaPut.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_theta_presentValueTheta() {
    double computedThetaCall = PRICER.theta(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    double computedThetaPut = PRICER.theta(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvThetaCall = PRICER.presentValueTheta(CALL_UKI, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPvThetaPut = PRICER.presentValueTheta(PUT_UKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOL_PROVIDER.getVolatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOL_PROVIDER.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(4);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(4);
    double expectedThetaCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(5) + rebateRate * expectedCash;
    double expectedThetaPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(5) + rebateRate * expectedAsset;
    expectedThetaCall *= -1d;
    expectedThetaPut *= -1d;
    assertEquals(computedThetaCall, expectedThetaCall, TOL);
    assertEquals(computedThetaPut, expectedThetaPut, TOL);
    assertEquals(computedPvThetaCall.getCurrency(), USD);
    assertEquals(computedPvThetaPut.getCurrency(), USD);
    assertEquals(computedPvThetaCall.getAmount(), expectedThetaCall * NOTIONAL, TOL);
    assertEquals(computedPvThetaPut.getAmount(), -expectedThetaPut * NOTIONAL, TOL);
  }

  public void test_theta_presentValueTheta_atExpiry() {
    double computedThetaCall = PRICER.theta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double computedThetaPut = PRICER.theta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvThetaCall = PRICER.presentValueTheta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    CurrencyAmount computedPvThetaPut = PRICER.presentValueTheta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOL_PROVIDER_EXPIRY);
    double rateBase = RATE_PROVIDER_EXPIRY.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER_EXPIRY.discountFactors(USD).zeroRate(PAY_DATE);
    double expectedThetaCall = -(REBATE_AMOUNT / NOTIONAL) * rateCounter;
    double expectedThetaPut = -rateCounter * STRIKE_RATE + rateBase * SPOT;
    expectedThetaCall *= -1d;
    expectedThetaPut *= -1d;
    assertEquals(computedThetaCall, expectedThetaCall, TOL);
    assertEquals(computedThetaPut, expectedThetaPut, TOL);
    assertEquals(computedPvThetaCall.getAmount(), expectedThetaCall * NOTIONAL, TOL * NOTIONAL);
    assertEquals(computedPvThetaPut.getAmount(), -expectedThetaPut * NOTIONAL, TOL);
  }

  public void test_theta_presentValueTheta_afterExpiry() {
    double computedThetaCall = PRICER.theta(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    double computedThetaPut = PRICER.theta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvThetaCall = PRICER.presentValueTheta(CALL_UKI, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    CurrencyAmount computedPvThetaPut = PRICER.presentValueTheta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOL_PROVIDER_AFTER);
    assertEquals(computedThetaCall, 0d, TOL);
    assertEquals(computedThetaPut, 0d, TOL);
    assertEquals(computedPvThetaCall.getAmount(), 0d, TOL);
    assertEquals(computedPvThetaPut.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void regression_pv() {
    CurrencyAmount pv = PRICER.presentValue(CALL_DKI, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pv.getAmount(), 9035006.129433425, NOTIONAL * TOL);
    CurrencyAmount pvBase = PRICER.presentValue(CALL_DKI_BASE, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvBase.getAmount(), 9038656.396419544, NOTIONAL * TOL); // UI put on USD/EUR rate with FX conversion in 2.x
    CurrencyAmount pvPut = PRICER.presentValue(PUT_DKO, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvPut.getAmount(), -55369.48871310125, NOTIONAL * TOL);
    CurrencyAmount pvPutBase = PRICER.presentValue(PUT_DKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvPutBase.getAmount(), -71369.96172030675, NOTIONAL * TOL); // UI call on USD/EUR rate with FX conversion in 2.x
  }

  public void regression_curveSensitivity() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivity(CALL_DKI, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvSensi = RATE_PROVIDER.curveParameterSensitivity(point.build());
    double[] eurSensi = new double[] {0.0, 0.0, 0.0, -8.23599758653779E7, -5.943903918586236E7 };
    double[] usdSensi = new double[] {0.0, 0.0, 0.0, 6.526531701730868E7, 4.710185614928411E7 };
    assertTrue(DoubleArrayMath.fuzzyEquals(
        eurSensi,
        pvSensi.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), USD).getSensitivity().toArray(),
        NOTIONAL * TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(
        usdSensi,
        pvSensi.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), USD).getSensitivity().toArray(),
        NOTIONAL * TOL));
    PointSensitivityBuilder pointBase = PRICER.presentValueSensitivity(CALL_DKI_BASE, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities pvSensiBase =
        RATE_PROVIDER.curveParameterSensitivity(pointBase.build()).convertedTo(EUR, RATE_PROVIDER);
    double[] eurSensiBase = new double[] {0.0, 0.0, 0.0, -5.885393657463378E7, -4.247477498074986E7 };
    double[] usdSensiBase = new double[] {0.0, 0.0, 0.0, 4.663853277047497E7, 3.365894110322015E7 };
    assertTrue(DoubleArrayMath.fuzzyEquals(
        eurSensiBase,
        pvSensiBase.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(
        usdSensiBase,
        pvSensiBase.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL));
    PointSensitivityBuilder pointPut =
        PRICER.presentValueSensitivity(PUT_DKO, RATE_PROVIDER, VOL_PROVIDER).multipliedBy(-1d);
    CurveCurrencyParameterSensitivities pvSensiPut = RATE_PROVIDER.curveParameterSensitivity(pointPut.build());
    double[] eurSensiPut = new double[] {0.0, 0.0, 0.0, 22176.623866383557, 16004.827601682477 };
    double[] usdSensiPut = new double[] {0.0, 0.0, 0.0, -48509.60688347871, -35009.29176024644 };
    assertTrue(DoubleArrayMath.fuzzyEquals(
        eurSensiPut,
        pvSensiPut.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), USD).getSensitivity().toArray(),
        NOTIONAL * TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(
        usdSensiPut,
        pvSensiPut.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), USD).getSensitivity().toArray(),
        NOTIONAL * TOL));
    PointSensitivityBuilder pointPutBase =
        PRICER.presentValueSensitivity(PUT_DKO_BASE, RATE_PROVIDER, VOL_PROVIDER).multipliedBy(-1d);
    CurveCurrencyParameterSensitivities pvSensiPutBase =
        RATE_PROVIDER.curveParameterSensitivity(pointPutBase.build()).convertedTo(EUR, RATE_PROVIDER);
    double[] eurSensiPutBase = new double[] {0.0, 0.0, 0.0, 24062.637495868825, 17365.96007956571 };
    double[] usdSensiPutBase = new double[] {0.0, 0.0, 0.0, -44888.77092190999, -32396.141278548253 };
    assertTrue(DoubleArrayMath.fuzzyEquals(
        eurSensiPutBase,
        pvSensiPutBase.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(
        usdSensiPutBase,
        pvSensiPutBase.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL));
  }

  public void regression_volSensitivity() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityVolatility(CALL_DKI, RATE_PROVIDER, VOL_PROVIDER);
    SurfaceCurrencyParameterSensitivity pvSensi = VOL_PROVIDER.surfaceParameterSensitivity((FxOptionSensitivity) point);
    PointSensitivityBuilder pointBase =
        PRICER.presentValueSensitivityVolatility(CALL_DKI_BASE, RATE_PROVIDER, VOL_PROVIDER);
    SurfaceCurrencyParameterSensitivity pvSensiBase = VOL_PROVIDER
        .surfaceParameterSensitivity((FxOptionSensitivity) pointBase).convertedTo(EUR, RATE_PROVIDER);
    PointSensitivityBuilder pointPut =
        PRICER.presentValueSensitivityVolatility(PUT_DKO, RATE_PROVIDER, VOL_PROVIDER).multipliedBy(-1d);
    SurfaceCurrencyParameterSensitivity pvSensiPut =
        VOL_PROVIDER.surfaceParameterSensitivity((FxOptionSensitivity) pointPut);
    PointSensitivityBuilder pointPutBase =
        PRICER.presentValueSensitivityVolatility(PUT_DKO_BASE, RATE_PROVIDER, VOL_PROVIDER).multipliedBy(-1d);
    SurfaceCurrencyParameterSensitivity pvSensiPutBase = VOL_PROVIDER
        .surfaceParameterSensitivity((FxOptionSensitivity) pointPutBase).convertedTo(EUR, RATE_PROVIDER);
    double[] computed = pvSensi.getSensitivity().toArray();
    double[] computedBase = pvSensiBase.getSensitivity().toArray();
    double[] computedPut = pvSensiPut.getSensitivity().toArray();
    double[] computedPutBase = pvSensiPutBase.getSensitivity().toArray();
    double[][] expected = new double[][] {
      {0.0, 0.0, 0.0, 0.0, 0.0 }, {0.0, 0.0, 0.0, 0.0, 0.0 }, {0.0, 0.0, 0.0, 0.0, 0.0 },
      {0.0, 0.0, 3.154862889936005E7, 186467.57005640838, 0.0 },
      {0.0, 0.0, 5.688931113627187E7, 336243.18963600876, 0.0 } };
    double[][] expectedBase = new double[][] {
      {0.0, 0.0, 0.0, 0.0, 0.0 }, {0.0, 0.0, 0.0, 0.0, 0.0 }, {0.0, 0.0, 0.0, 0.0, 0.0 },
      {0.0, 0.0, 2.2532363577178854E7, 133177.10564432456, 0.0 },
      {0.0, 0.0, 4.063094615828866E7, 240148.4331822043, 0.0 } };
    double[][] expectedPut = new double[][] {
      {-0.0, -0.0, -0.0, -0.0, -0.0 }, {-0.0, -0.0, -0.0, -0.0, -0.0 },
      {-0.0, -0.0, -0.0, -0.0, -0.0 }, {-0.0, -0.0, -53011.143048566446, -313.32135103910525, -0.0 },
      {-0.0, -0.0, -95591.07688006328, -564.989238732409, -0.0 } };
    double[][] expectedPutBase = new double[][] {
      {-0.0, -0.0, -0.0, -0.0, -0.0 }, {-0.0, -0.0, -0.0, -0.0, -0.0 }, {-0.0, -0.0, -0.0, -0.0, -0.0 },
      {-0.0, -0.0, -35148.33541137355, -207.743566815316, -0.0 },
      {-0.0, -0.0, -63380.39588085656, -374.6086223530026, -0.0 } };
    for (int i = 0; i < computed.length; ++i) {
      int row = i / 5;
      int col = i % 5;
      assertTrue(DoubleMath.fuzzyEquals(computed[i], expected[row][col], NOTIONAL * TOL));
      assertTrue(DoubleMath.fuzzyEquals(computedBase[i], expectedBase[row][col], NOTIONAL * TOL));
      assertTrue(DoubleMath.fuzzyEquals(computedPut[i], expectedPut[row][col], NOTIONAL * TOL));
      assertTrue(DoubleMath.fuzzyEquals(computedPutBase[i], expectedPutBase[row][col], NOTIONAL * TOL));
    }
  }

  public void regression_currencyExposure() {
    MultiCurrencyAmount pv = PRICER.currencyExposure(CALL_DKI, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pv.getAmount(EUR).getAmount(), -2.8939530642669797E7, NOTIONAL * TOL);
    assertEquals(pv.getAmount(USD).getAmount(), 4.955034902917114E7, NOTIONAL * TOL);
    MultiCurrencyAmount pvBase = PRICER.currencyExposure(CALL_DKI_BASE, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvBase.getAmount(EUR).getAmount(), -2.8866459583853487E7, NOTIONAL * TOL);
    assertEquals(pvBase.getAmount(USD).getAmount(), 4.9451699813814424E7, NOTIONAL * TOL);
    MultiCurrencyAmount pvPut = PRICER.currencyExposure(PUT_DKO, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvPut.getAmount(EUR).getAmount(), -105918.46956467835, NOTIONAL * TOL);
    assertEquals(pvPut.getAmount(USD).getAmount(), 92916.36867744842, NOTIONAL * TOL);
    MultiCurrencyAmount pvPutBase = PRICER.currencyExposure(PUT_DKO_BASE, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvPutBase.getAmount(EUR).getAmount(), -76234.66256109312, NOTIONAL * TOL);
    assertEquals(pvPutBase.getAmount(USD).getAmount(), 35358.56586522361, NOTIONAL * TOL);
  }

}
