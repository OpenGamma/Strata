/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchAssetPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchCashPriceFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link BlackFxVanillaOptionProductPricer}.
 */
public class BlackFxSingleBarrierOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final LocalDate SPOT_DATE = LocalDate.of(2011, 6, 15);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2014, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  // providers
  private static final BlackFxOptionSmileVolatilities VOLS =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(VAL_DATETIME);
  private static final ImmutableRatesProvider RATE_PROVIDER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(VAL_DATE);
  private static final BlackFxOptionSmileVolatilities VOLS_FLAT =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5Flat(VAL_DATETIME);
  // providers - valuation at expiry
  private static final BlackFxOptionSmileVolatilities VOLS_EXPIRY =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(EXPIRY_DATETIME);
  private static final ImmutableRatesProvider RATE_PROVIDER_EXPIRY =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(EXPIRY_DATE);
  // provider - valuation after expiry
  private static final BlackFxOptionSmileVolatilities VOLS_AFTER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(EXPIRY_DATETIME.plusDays(1));
  private static final ImmutableRatesProvider RATE_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEurUsdActActIsda(EXPIRY_DATE.plusDays(1));
  // smile term
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      FxVolatilitySmileDataSet.getSmileDeltaTermStructure6();

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
      PUT_DKI, PUT_DKI_BASE, PUT_DKO, PUT_DKO_BASE, PUT_UKI, PUT_UKI_BASE, PUT_UKO, PUT_UKO_BASE};

  private static final BlackFxSingleBarrierOptionProductPricer PRICER = BlackFxSingleBarrierOptionProductPricer.DEFAULT;
  private static final BlackFxVanillaOptionProductPricer VANILLA_PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;
  private static final BlackBarrierPriceFormulaRepository BARRIER_PRICER = new BlackBarrierPriceFormulaRepository();
  private static final BlackOneTouchAssetPriceFormulaRepository ASSET_REBATE_PRICER = new BlackOneTouchAssetPriceFormulaRepository();
  private static final BlackOneTouchCashPriceFormulaRepository CASH_REBATE_PRICER = new BlackOneTouchCashPriceFormulaRepository();
  private static final double TOL = 1.0e-12;
  private static final double FD_EPS = 1.0e-6;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL = new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  @Test
  public void test_price_presentValue() {
    double computedPriceCall = PRICER.price(CALL_UKI, RATE_PROVIDER, VOLS);
    double computedPricePut = PRICER.price(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvCall = PRICER.presentValue(CALL_UKI, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvPut = PRICER.presentValue(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOLS.volatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOLS.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash =
        CASH_REBATE_PRICER.price(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO);
    double expectedAsset =
        ASSET_REBATE_PRICER.price(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI);
    double expectedPriceCall = BARRIER_PRICER.price(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI) + rebateRate * expectedCash;
    double expectedPricePut = BARRIER_PRICER.price(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO) + rebateRate * expectedAsset;

//    SXSD-6095
//    assertThat(computedPriceCall).isCloseTo(expectedPriceCall, offset(TOL));
//    assertThat(computedPricePut).isCloseTo(expectedPricePut, offset(TOL));
//    assertThat(computedPvCall.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvPut.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvCall.getAmount()).isCloseTo(expectedPriceCall * NOTIONAL, offset(TOL));
//    assertThat(computedPvPut.getAmount()).isCloseTo(-expectedPricePut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_price_presentValue_atExpiry() {
    double computedPriceCall = PRICER.price(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double computedPriceCallZero = PRICER.price(CALL_UKO, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double computedPricePut = PRICER.price(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvCall = PRICER.presentValue(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvCallZero = PRICER.presentValue(CALL_UKO, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvPut = PRICER.presentValue(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double expectedPriceCall = REBATE_AMOUNT / NOTIONAL;
    double expectedPricePut = STRIKE_RATE - SPOT;
    assertThat(computedPriceCall).isCloseTo(expectedPriceCall, offset(TOL));
    assertThat(computedPriceCallZero).isCloseTo(0d, offset(TOL));
    assertThat(computedPricePut).isCloseTo(expectedPricePut, offset(TOL));
    assertThat(computedPvCall.getAmount()).isCloseTo(expectedPriceCall * NOTIONAL, offset(TOL));
    assertThat(computedPvCallZero.getAmount()).isCloseTo(0d * NOTIONAL, offset(TOL));
    assertThat(computedPvPut.getAmount()).isCloseTo(-expectedPricePut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_price_presentValue_afterExpiry() {
    double computedPriceCall = PRICER.price(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    double computedPricePut = PRICER.price(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvCall = PRICER.presentValue(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvPut = PRICER.presentValue(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(computedPriceCall).isCloseTo(0d, offset(TOL));
    assertThat(computedPricePut).isCloseTo(0d, offset(TOL));
    assertThat(computedPvCall.getAmount()).isCloseTo(0d, offset(TOL));
    assertThat(computedPvPut.getAmount()).isCloseTo(0d, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
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
    CurrencyAmount pvCall = VANILLA_PRICER.presentValue(CALL, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvCallUp = PRICER.presentValue(callUKO, RATE_PROVIDER, VOLS)
        .plus(PRICER.presentValue(callUKI, RATE_PROVIDER, VOLS));
    CurrencyAmount computedPvCallDw = PRICER.presentValue(callDKO, RATE_PROVIDER, VOLS)
        .plus(PRICER.presentValue(callDKI, RATE_PROVIDER, VOLS));

//    SXSD-6095
//    assertThat(computedPvCallUp.getAmount()).isCloseTo(pvCall.getAmount(), offset(NOTIONAL * TOL));
//    assertThat(computedPvCallDw.getAmount()).isCloseTo(pvCall.getAmount(), offset(NOTIONAL * TOL));
    CurrencyAmount pvPut = VANILLA_PRICER.presentValue(PUT, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvPutUp = PRICER.presentValue(putUKO, RATE_PROVIDER, VOLS)
        .plus(PRICER.presentValue(putUKI, RATE_PROVIDER, VOLS));
    CurrencyAmount computedPvPutDw = PRICER.presentValue(putDKO, RATE_PROVIDER, VOLS)
        .plus(PRICER.presentValue(putDKI, RATE_PROVIDER, VOLS));

//    SXSD-6095
//    assertThat(computedPvPutUp.getAmount()).isCloseTo(pvPut.getAmount(), offset(NOTIONAL * TOL));
//    assertThat(computedPvPutDw.getAmount()).isCloseTo(pvPut.getAmount(), offset(NOTIONAL * TOL));
    // curve sensitivity
    PointSensitivities pvSensiCall = VANILLA_PRICER.presentValueSensitivityRatesStickyStrike(CALL, RATE_PROVIDER, VOLS);
    PointSensitivities computedPvSensiCallUp = PRICER.presentValueSensitivityRatesStickyStrike(callUKO, RATE_PROVIDER, VOLS)
        .combinedWith(PRICER.presentValueSensitivityRatesStickyStrike(callUKI, RATE_PROVIDER, VOLS)).build();
    PointSensitivities computedPvSensiCallDw = PRICER.presentValueSensitivityRatesStickyStrike(callDKO, RATE_PROVIDER, VOLS)
        .combinedWith(PRICER.presentValueSensitivityRatesStickyStrike(callDKI, RATE_PROVIDER, VOLS)).build();

//    SXSD-6095
//    assertThat(RATE_PROVIDER.parameterSensitivity(pvSensiCall).equalWithTolerance(
//        RATE_PROVIDER.parameterSensitivity(computedPvSensiCallUp), TOL * NOTIONAL)).isTrue();
//    assertThat(RATE_PROVIDER.parameterSensitivity(pvSensiCall).equalWithTolerance(
//        RATE_PROVIDER.parameterSensitivity(computedPvSensiCallDw), TOL * NOTIONAL)).isTrue();
    PointSensitivities pvSensiPut = VANILLA_PRICER.presentValueSensitivityRatesStickyStrike(PUT, RATE_PROVIDER, VOLS);
    PointSensitivities computedPvSensiPutUp = PRICER.presentValueSensitivityRatesStickyStrike(putUKO, RATE_PROVIDER, VOLS)
        .combinedWith(PRICER.presentValueSensitivityRatesStickyStrike(putUKI, RATE_PROVIDER, VOLS)).build();
    PointSensitivities computedPvSensiPutDw = PRICER.presentValueSensitivityRatesStickyStrike(putDKO, RATE_PROVIDER, VOLS)
        .combinedWith(PRICER.presentValueSensitivityRatesStickyStrike(putDKI, RATE_PROVIDER, VOLS)).build();

//    SXSD-6095
//    assertThat(RATE_PROVIDER.parameterSensitivity(pvSensiPut).equalWithTolerance(
//        RATE_PROVIDER.parameterSensitivity(computedPvSensiPutUp), TOL * NOTIONAL)).isTrue();
//    assertThat(RATE_PROVIDER.parameterSensitivity(pvSensiPut).equalWithTolerance(
//        RATE_PROVIDER.parameterSensitivity(computedPvSensiPutDw), TOL * NOTIONAL)).isTrue();
  }

  @Test
  public void farBarrierOutTest() {
    double smallBarrier = 1.0e-6;
    double largeBarrier = 1.0e6;

    SimpleConstantContinuousBarrier dkoSmall =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, smallBarrier);
    SimpleConstantContinuousBarrier uKoLarge =
        SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, largeBarrier);
    ResolvedFxSingleBarrierOption callDko = ResolvedFxSingleBarrierOption.of(CALL, dkoSmall, REBATE);
    ResolvedFxSingleBarrierOption callUko = ResolvedFxSingleBarrierOption.of(CALL, uKoLarge, REBATE_BASE);
    ResolvedFxSingleBarrierOption putDko = ResolvedFxSingleBarrierOption.of(PUT, dkoSmall, REBATE);
    ResolvedFxSingleBarrierOption putUko = ResolvedFxSingleBarrierOption.of(PUT, uKoLarge, REBATE_BASE);
    // pv
    CurrencyAmount pvCallDko = PRICER.presentValue(callDko, RATE_PROVIDER, VOLS);
    CurrencyAmount pvCallUko = PRICER.presentValue(callUko, RATE_PROVIDER, VOLS);
    CurrencyAmount pvCall = VANILLA_PRICER.presentValue(CALL, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPutDko = PRICER.presentValue(putDko, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPutUko = PRICER.presentValue(putUko, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPut = VANILLA_PRICER.presentValue(PUT, RATE_PROVIDER, VOLS);
//    SXSD-6095
//    assertThat(pvCallDko.getAmount()).isCloseTo(pvCall.getAmount(), offset(NOTIONAL * TOL));
//    assertThat(pvCallUko.getAmount()).isCloseTo(pvCall.getAmount(), offset(NOTIONAL * TOL));
//    assertThat(pvPutDko.getAmount()).isCloseTo(pvPut.getAmount(), offset(NOTIONAL * TOL));
//    assertThat(pvPutUko.getAmount()).isCloseTo(pvPut.getAmount(), offset(NOTIONAL * TOL));
    // currency exposure
    MultiCurrencyAmount ceCallDko = PRICER.currencyExposure(callDko, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount ceCallUko = PRICER.currencyExposure(callUko, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount ceCall = VANILLA_PRICER.currencyExposure(CALL, RATE_PROVIDER, VOLS);
//    SXSD-6095
//    assertThat(ceCallDko.getAmount(EUR).getAmount()).isCloseTo(ceCall.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
//    assertThat(ceCallDko.getAmount(USD).getAmount()).isCloseTo(ceCall.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
//    assertThat(ceCallUko.getAmount(EUR).getAmount()).isCloseTo(ceCall.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
//    assertThat(ceCallUko.getAmount(USD).getAmount()).isCloseTo(ceCall.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
    MultiCurrencyAmount cePutDko = PRICER.currencyExposure(putDko, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount cePutUko = PRICER.currencyExposure(putUko, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount cePut = VANILLA_PRICER.currencyExposure(PUT, RATE_PROVIDER, VOLS);
//    SXSD-6095
//    assertThat(cePutDko.getAmount(EUR).getAmount()).isCloseTo(cePut.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
//    assertThat(cePutDko.getAmount(USD).getAmount()).isCloseTo(cePut.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
//    assertThat(cePutUko.getAmount(EUR).getAmount()).isCloseTo(cePut.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
//    assertThat(cePutUko.getAmount(USD).getAmount()).isCloseTo(cePut.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void farBarrierInTest() {
    double smallBarrier = 1.0e-6;
    double largeBarrier = 1.0e6;
    SimpleConstantContinuousBarrier dkiSmall =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, smallBarrier);
    SimpleConstantContinuousBarrier uKiLarge =
        SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, largeBarrier);
    ResolvedFxSingleBarrierOption callDki = ResolvedFxSingleBarrierOption.of(CALL, dkiSmall, REBATE);
    ResolvedFxSingleBarrierOption callUki = ResolvedFxSingleBarrierOption.of(CALL, uKiLarge, REBATE_BASE);
    ResolvedFxSingleBarrierOption putDki = ResolvedFxSingleBarrierOption.of(PUT, dkiSmall, REBATE);
    ResolvedFxSingleBarrierOption putUki = ResolvedFxSingleBarrierOption.of(PUT, uKiLarge, REBATE_BASE);
    // pv
    CurrencyAmount pvCallDki = PRICER.presentValue(callDki, RATE_PROVIDER, VOLS);
    CurrencyAmount pvCallUki = PRICER.presentValue(callUki, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPutDki = PRICER.presentValue(putDki, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPutUki = PRICER.presentValue(putUki, RATE_PROVIDER, VOLS);
    double dfUsd = RATE_PROVIDER.discountFactor(USD, PAY_DATE);
    double dfEur = RATE_PROVIDER.discountFactor(EUR, PAY_DATE);
    assertThat(pvCallDki.getAmount()).isCloseTo(REBATE_AMOUNT * dfUsd, offset(NOTIONAL * TOL));
    assertThat(pvCallUki.getAmount()).isCloseTo(REBATE_AMOUNT * SPOT * dfEur, offset(NOTIONAL * TOL));
    assertThat(pvPutDki.getAmount()).isCloseTo(-REBATE_AMOUNT * dfUsd, offset(NOTIONAL * TOL));
    assertThat(pvPutUki.getAmount()).isCloseTo(-REBATE_AMOUNT * SPOT * dfEur, offset(NOTIONAL * TOL));
    // currency exposure
    MultiCurrencyAmount ceCallDki = PRICER.currencyExposure(callDki, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount ceCallUki = PRICER.currencyExposure(callUki, RATE_PROVIDER, VOLS);
    assertThat(ceCallDki.getAmount(EUR).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(ceCallDki.getAmount(USD).getAmount()).isCloseTo(REBATE_AMOUNT * dfUsd, offset(NOTIONAL * TOL));
    assertThat(ceCallUki.getAmount(EUR).getAmount()).isCloseTo(REBATE_AMOUNT * dfEur, offset(NOTIONAL * TOL));
    assertThat(ceCallUki.getAmount(USD).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    MultiCurrencyAmount cePutDki = PRICER.currencyExposure(putDki, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount cePutUki = PRICER.currencyExposure(putUki, RATE_PROVIDER, VOLS);
    assertThat(cePutDki.getAmount(EUR).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(cePutDki.getAmount(USD).getAmount()).isCloseTo(-REBATE_AMOUNT * dfUsd, offset(NOTIONAL * TOL));
    assertThat(cePutUki.getAmount(EUR).getAmount()).isCloseTo(-REBATE_AMOUNT * dfEur, offset(NOTIONAL * TOL));
    assertThat(cePutUki.getAmount(USD).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(option, RATE_PROVIDER, VOLS);
      CurrencyParameterSensitivities computed = RATE_PROVIDER.parameterSensitivity(point.build());
      CurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER,
          p -> PRICER.presentValue(option, p, VOLS));
      double pvVega = ((FxOptionSensitivity)
          PRICER.presentValueSensitivityModelParamsVolatility(option, RATE_PROVIDER, VOLS)).getSensitivity();
      CurrencyParameterSensitivities sensiViaFwd = FD_CAL.sensitivity(RATE_PROVIDER,
          p -> CurrencyAmount.of(USD, VANILLA_PRICER.impliedVolatility(CALL, p, VOLS))).multipliedBy(-pvVega);
      expected = expected.combinedWith(sensiViaFwd);
//      SXSD-6095
//      assertThat(computed.equalWithTolerance(expected, FD_EPS * NOTIONAL * 10d)).isTrue();
    }
  }

  @Test
  public void test_presentValueSensitivity_atExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(option, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
      CurrencyParameterSensitivities computed = RATE_PROVIDER_EXPIRY.parameterSensitivity(point.build());
      CurrencyParameterSensitivities expected = FD_CAL.sensitivity(RATE_PROVIDER_EXPIRY,
          p -> PRICER.presentValue(option, p, VOLS_EXPIRY));
      assertThat(computed.equalWithTolerance(expected, FD_EPS * NOTIONAL * 10d)).isTrue();
    }
  }

  @Test
  public void test_presentValueSensitivity_afterExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(option, RATE_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(point).isEqualTo(PointSensitivityBuilder.none());
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_vega_presentValueSensitivityVolatility() {
    double computedVegaCall = PRICER.vega(CALL_UKI, RATE_PROVIDER, VOLS);
    FxOptionSensitivity computedCall =
        (FxOptionSensitivity) PRICER.presentValueSensitivityModelParamsVolatility(CALL_UKI, RATE_PROVIDER, VOLS);
    double computedVegaPut = PRICER.vega(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    FxOptionSensitivity computedPut =
        (FxOptionSensitivity) PRICER.presentValueSensitivityModelParamsVolatility(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOLS.volatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOLS.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(3);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(3);
    double expectedCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(4) + rebateRate * expectedCash;
    double expectedPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(4) + rebateRate * expectedAsset;
//    SXSD-6095
//    assertThat(computedVegaCall).isCloseTo(expectedCall, offset(TOL));
//    assertThat(computedCall.getSensitivity()).isCloseTo(expectedCall * NOTIONAL, offset(TOL * NOTIONAL));
//    assertThat(computedCall.getCurrency()).isEqualTo(USD);
//    assertThat(computedCall.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
//    assertThat(computedCall.getStrike()).isEqualTo(STRIKE_RATE);
//    assertThat(computedCall.getForward()).isCloseTo(forward, offset(TOL));
//    assertThat(computedCall.getExpiry()).isEqualTo(timeToExpiry);
//    assertThat(computedVegaPut).isCloseTo(expectedPut, offset(TOL));
//    assertThat(computedPut.getSensitivity()).isCloseTo(-expectedPut * NOTIONAL, offset(TOL * NOTIONAL));
//    assertThat(computedPut.getCurrency()).isEqualTo(USD);
//    assertThat(computedPut.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
//    assertThat(computedPut.getStrike()).isEqualTo(STRIKE_RATE);
//    assertThat(computedPut.getForward()).isCloseTo(forward, offset(TOL));
//    assertThat(computedPut.getExpiry()).isEqualTo(timeToExpiry);
  }

  @Test
  public void test_vega_presentValueSensitivityVolatility_atExpiry() {
    double computedVegaCall = PRICER.vega(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    PointSensitivityBuilder computedCall =
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double computedVegaPut = PRICER.vega(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    PointSensitivityBuilder computedPut =
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(computedVegaCall).isEqualTo(0d);
    assertThat(computedCall).isEqualTo(PointSensitivityBuilder.none());
    assertThat(computedVegaPut).isEqualTo(0d);
    assertThat(computedPut).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_vega_presentValueSensitivityVolatility_afterExpiry() {
    double computedVegaCall = PRICER.vega(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    PointSensitivityBuilder computedCall =
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    double computedVegaPut = PRICER.vega(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    PointSensitivityBuilder computedPut =
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(computedVegaCall).isEqualTo(0d);
    assertThat(computedCall).isEqualTo(PointSensitivityBuilder.none());
    assertThat(computedVegaPut).isEqualTo(0d);
    assertThat(computedPut).isEqualTo(PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      CurrencyAmount pv = PRICER.presentValue(option, RATE_PROVIDER, VOLS_FLAT);
      MultiCurrencyAmount computed = PRICER.currencyExposure(option, RATE_PROVIDER, VOLS_FLAT);
      FxMatrix fxMatrix = FxMatrix.builder().addRate(EUR, USD, SPOT + FD_EPS).build();
      ImmutableRatesProvider provBumped = RATE_PROVIDER.toBuilder().fxRateProvider(fxMatrix).build();
      CurrencyAmount pvBumped = PRICER.presentValue(option, provBumped, VOLS_FLAT);
      double ceCounterFD = pvBumped.getAmount() - pv.getAmount();
      double ceBaseFD = pvBumped.getAmount() / (SPOT + FD_EPS) - pv.getAmount() / SPOT;
      assertThat(computed.getAmount(EUR).getAmount() * FD_EPS).isCloseTo(ceCounterFD, offset(NOTIONAL * TOL));
      assertThat(computed.getAmount(USD).getAmount() * (1.0d / (SPOT + FD_EPS) - 1.0d / SPOT)).isCloseTo(ceBaseFD, offset(NOTIONAL * TOL));
    }
  }

  @Test
  public void test_currencyExposure_atExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      CurrencyAmount pv = PRICER.presentValue(option, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
      MultiCurrencyAmount computed = PRICER.currencyExposure(option, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
      FxMatrix fxMatrix = FxMatrix.builder().addRate(EUR, USD, SPOT + FD_EPS).build();
      ImmutableRatesProvider provBumped = RATE_PROVIDER_EXPIRY.toBuilder().fxRateProvider(fxMatrix).build();
      CurrencyAmount pvBumped = PRICER.presentValue(option, provBumped, VOLS_EXPIRY);
      double ceCounterFD = pvBumped.getAmount() - pv.getAmount();
      double ceBaseFD = pvBumped.getAmount() / (SPOT + FD_EPS) - pv.getAmount() / SPOT;
      assertThat(computed.getAmount(EUR).getAmount() * FD_EPS).isCloseTo(ceCounterFD, offset(NOTIONAL * TOL));
      assertThat(computed.getAmount(USD).getAmount() * (1.0d / (SPOT + FD_EPS) - 1.0d / SPOT)).isCloseTo(ceBaseFD, offset(NOTIONAL * TOL));
    }
  }

  @Test
  public void test_currencyExposure_afterExpiry() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      MultiCurrencyAmount computed = PRICER.currencyExposure(option, RATE_PROVIDER_AFTER, VOLS_AFTER);
      assertThat(computed).isEqualTo(MultiCurrencyAmount.empty());
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_delta_presentValueDelta() {
    double computedDeltaCall = PRICER.delta(CALL_UKI, RATE_PROVIDER, VOLS);
    double computedDeltaPut = PRICER.delta(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvDeltaCall = PRICER.presentValueDelta(CALL_UKI, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvDeltaPut = PRICER.presentValueDelta(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOLS.volatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOLS.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(0);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(0);
    double expectedDeltaCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(0) + rebateRate * expectedCash;
    double expectedDeltaPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(0) + rebateRate * expectedAsset;

//    SXSD-6095
//    assertThat(computedDeltaCall).isCloseTo(expectedDeltaCall, offset(TOL));
//    assertThat(computedDeltaPut).isCloseTo(expectedDeltaPut, offset(TOL));
//    assertThat(computedPvDeltaCall.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvDeltaPut.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvDeltaCall.getAmount()).isCloseTo(expectedDeltaCall * NOTIONAL, offset(TOL));
//    assertThat(computedPvDeltaPut.getAmount()).isCloseTo(-expectedDeltaPut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_delta_presentValueDelta_atExpiry() {
    double computedDeltaCall = PRICER.delta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double computedDeltaPut = PRICER.delta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvDeltaCall = PRICER.presentValueDelta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvDeltaPut = PRICER.presentValueDelta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double expectedDeltaPut = -1d;
    assertThat(computedDeltaCall).isCloseTo(0d, offset(TOL));
    assertThat(computedDeltaPut).isCloseTo(expectedDeltaPut, offset(TOL));
    assertThat(computedPvDeltaCall.getAmount()).isCloseTo(0d, offset(TOL));
    assertThat(computedPvDeltaPut.getAmount()).isCloseTo(-expectedDeltaPut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_delta_presentValueDelta_afterExpiry() {
    double computedDeltaCall = PRICER.delta(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    double computedDeltaPut = PRICER.delta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvDeltaCall = PRICER.presentValueDelta(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvDeltaPut = PRICER.presentValueDelta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(computedDeltaCall).isCloseTo(0d, offset(TOL));
    assertThat(computedDeltaPut).isCloseTo(0d, offset(TOL));
    assertThat(computedPvDeltaCall.getAmount()).isCloseTo(0d, offset(TOL));
    assertThat(computedPvDeltaPut.getAmount()).isCloseTo(0d, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_gamma_presentValueGamma() {
    double computedGammaCall = PRICER.gamma(CALL_UKI, RATE_PROVIDER, VOLS);
    double computedGammaPut = PRICER.gamma(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvGammaCall = PRICER.presentValueGamma(CALL_UKI, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvGammaPut = PRICER.presentValueGamma(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOLS.volatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOLS.relativeTime(EXPIRY_DATETIME);
    double rebateRate = REBATE_AMOUNT / NOTIONAL;
    double expectedCash = CASH_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKO).getDerivative(5);
    double expectedAsset = ASSET_REBATE_PRICER
        .priceAdjoint(SPOT, timeToExpiry, costOfCarry, rateCounter, volatility, BARRIER_UKI).getDerivative(5);
    double expectedGammaCall = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, true, BARRIER_UKI).getDerivative(6) + rebateRate * expectedCash;
    double expectedGammaPut = BARRIER_PRICER.priceAdjoint(SPOT, STRIKE_RATE, timeToExpiry, costOfCarry, rateCounter,
        volatility, false, BARRIER_UKO).getDerivative(6) + rebateRate * expectedAsset;
//    SXSD-6095
//    assertThat(computedGammaCall).isCloseTo(expectedGammaCall, offset(TOL));
//    assertThat(computedGammaPut).isCloseTo(expectedGammaPut, offset(TOL));
//    assertThat(computedPvGammaCall.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvGammaPut.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvGammaCall.getAmount()).isCloseTo(expectedGammaCall * NOTIONAL, offset(TOL));
//    assertThat(computedPvGammaPut.getAmount()).isCloseTo(-expectedGammaPut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_gamma_presentValueGamma_atExpiry() {
    double computedGammaCall = PRICER.gamma(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double computedGammaPut = PRICER.gamma(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvGammaCall = PRICER.presentValueGamma(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvGammaPut = PRICER.presentValueGamma(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    assertThat(computedGammaCall).isCloseTo(0d, offset(TOL));
    assertThat(computedGammaPut).isCloseTo(0d, offset(TOL));
    assertThat(computedPvGammaCall.getAmount()).isCloseTo(0d, offset(TOL));
    assertThat(computedPvGammaPut.getAmount()).isCloseTo(0d, offset(TOL));
  }

  @Test
  public void test_gamma_presentValueGamma_afterExpiry() {
    double computedGammaCall = PRICER.gamma(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    double computedGammaPut = PRICER.gamma(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvGammaCall = PRICER.presentValueGamma(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvGammaPut = PRICER.presentValueGamma(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(computedGammaCall).isCloseTo(0d, offset(TOL));
    assertThat(computedGammaPut).isCloseTo(0d, offset(TOL));
    assertThat(computedPvGammaCall.getAmount()).isCloseTo(0d, offset(TOL));
    assertThat(computedPvGammaPut.getAmount()).isCloseTo(0d, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_theta_presentValueTheta() {
    double computedThetaCall = PRICER.theta(CALL_UKI, RATE_PROVIDER, VOLS);
    double computedThetaPut = PRICER.theta(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvThetaCall = PRICER.presentValueTheta(CALL_UKI, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPvThetaPut = PRICER.presentValueTheta(PUT_UKO_BASE, RATE_PROVIDER, VOLS);
    double rateBase = RATE_PROVIDER.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER.discountFactors(USD).zeroRate(PAY_DATE);
    double costOfCarry = rateCounter - rateBase;
    double forward = RATE_PROVIDER.fxForwardRates(CURRENCY_PAIR).rate(EUR, PAY_DATE);
    double volatility = VOLS.volatility(CURRENCY_PAIR, EXPIRY_DATETIME, STRIKE_RATE, forward);
    double timeToExpiry = VOLS.relativeTime(EXPIRY_DATETIME);
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

//    SXSD-6095
//    assertThat(computedThetaCall).isCloseTo(expectedThetaCall, offset(TOL));
//    assertThat(computedThetaPut).isCloseTo(expectedThetaPut, offset(TOL));
//    assertThat(computedPvThetaCall.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvThetaPut.getCurrency()).isEqualTo(USD);
//    assertThat(computedPvThetaCall.getAmount()).isCloseTo(expectedThetaCall * NOTIONAL, offset(TOL));
//    assertThat(computedPvThetaPut.getAmount()).isCloseTo(-expectedThetaPut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_theta_presentValueTheta_atExpiry() {
    double computedThetaCall = PRICER.theta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double computedThetaPut = PRICER.theta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvThetaCall = PRICER.presentValueTheta(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    CurrencyAmount computedPvThetaPut = PRICER.presentValueTheta(PUT_UKO_BASE, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY);
    double rateBase = RATE_PROVIDER_EXPIRY.discountFactors(EUR).zeroRate(PAY_DATE);
    double rateCounter = RATE_PROVIDER_EXPIRY.discountFactors(USD).zeroRate(PAY_DATE);
    double expectedThetaCall = -(REBATE_AMOUNT / NOTIONAL) * rateCounter;
    double expectedThetaPut = -rateCounter * STRIKE_RATE + rateBase * SPOT;
    expectedThetaCall *= -1d;
    expectedThetaPut *= -1d;
    assertThat(computedThetaCall).isCloseTo(expectedThetaCall, offset(TOL));
    assertThat(computedThetaPut).isCloseTo(expectedThetaPut, offset(TOL));
    assertThat(computedPvThetaCall.getAmount()).isCloseTo(expectedThetaCall * NOTIONAL, offset(TOL * NOTIONAL));
    assertThat(computedPvThetaPut.getAmount()).isCloseTo(-expectedThetaPut * NOTIONAL, offset(TOL));
  }

  @Test
  public void test_theta_presentValueTheta_afterExpiry() {
    double computedThetaCall = PRICER.theta(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    double computedThetaPut = PRICER.theta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvThetaCall = PRICER.presentValueTheta(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER);
    CurrencyAmount computedPvThetaPut = PRICER.presentValueTheta(PUT_UKO_BASE, RATE_PROVIDER_AFTER, VOLS_AFTER);
    assertThat(computedThetaCall).isCloseTo(0d, offset(TOL));
    assertThat(computedThetaPut).isCloseTo(0d, offset(TOL));
    assertThat(computedPvThetaCall.getAmount()).isCloseTo(0d, offset(TOL));
    assertThat(computedPvThetaPut.getAmount()).isCloseTo(0d, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forwardFxRate() {
    // forward rate is computed by discounting for any RatesProvider input.
    FxRate computed = PRICER.forwardFxRate(CALL_UKI, RATE_PROVIDER);
    double df1 = RATE_PROVIDER.discountFactor(EUR, PAY_DATE) / RATE_PROVIDER.discountFactor(EUR, SPOT_DATE);
    double df2 = RATE_PROVIDER.discountFactor(USD, PAY_DATE) / RATE_PROVIDER.discountFactor(USD, SPOT_DATE);
    double spot = RATE_PROVIDER.fxRate(EUR, USD);
    FxRate expected = FxRate.of(CURRENCY_PAIR, spot * df1 / df2);
    assertThat(computed.getPair()).isEqualTo(expected.getPair());
    assertThat(computed.fxRate(CURRENCY_PAIR)).isCloseTo(expected.fxRate(CURRENCY_PAIR), offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_impliedVolatility() {
    double computedCall = PRICER.impliedVolatility(CALL_UKI, RATE_PROVIDER, VOLS);
    double computedPut = PRICER.impliedVolatility(PUT_UKI, RATE_PROVIDER, VOLS);
    double timeToExpiry = VOLS.relativeTime(EXPIRY_DATETIME);
    double forward = PRICER.forwardFxRate(CALL_UKI, RATE_PROVIDER).fxRate(CURRENCY_PAIR);
    double expected = SMILE_TERM.volatility(timeToExpiry, STRIKE_RATE, forward);
    assertThat(computedCall).isEqualTo(expected);
    assertThat(computedPut).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_atExpiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(CALL_UKI, RATE_PROVIDER_EXPIRY, VOLS_EXPIRY));
  }

  @Test
  public void test_impliedVolatility_afterExpiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(CALL_UKI, RATE_PROVIDER_AFTER, VOLS_AFTER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void regression_pv() {
    CurrencyAmount pv = PRICER.presentValue(CALL_DKI, RATE_PROVIDER, VOLS);
    assertThat(pv.getAmount()).isCloseTo(9035006.129433425, offset(NOTIONAL * TOL));
    CurrencyAmount pvBase = PRICER.presentValue(CALL_DKI_BASE, RATE_PROVIDER, VOLS);
    assertThat(pvBase.getAmount()).isCloseTo(9038656.396419544, offset(NOTIONAL * TOL)); // UI put on USD/EUR rate with FX conversion in 2.x
    CurrencyAmount pvPut = PRICER.presentValue(PUT_DKO, RATE_PROVIDER, VOLS);
    assertThat(pvPut.getAmount()).isCloseTo(-55369.48871310125, offset(NOTIONAL * TOL));
    CurrencyAmount pvPutBase = PRICER.presentValue(PUT_DKO_BASE, RATE_PROVIDER, VOLS);
    assertThat(pvPutBase.getAmount()).isCloseTo(-71369.96172030675, offset(NOTIONAL * TOL)); // UI call on USD/EUR rate with FX conversion in 2.x
  }

  @Test
  public void regression_curveSensitivity() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(CALL_DKI, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities pvSensi = RATE_PROVIDER.parameterSensitivity(point.build());
    double[] eurSensi = new double[] {0.0, 0.0, 0.0, -8.23599758653779E7, -5.943903918586236E7 };
    double[] usdSensi = new double[] {0.0, 0.0, 0.0, 6.526531701730868E7, 4.710185614928411E7 };
    assertThat(DoubleArrayMath.fuzzyEquals(
        eurSensi,
        pvSensi.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), USD).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(
        usdSensi,
        pvSensi.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), USD).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    PointSensitivityBuilder pointBase = PRICER.presentValueSensitivityRatesStickyStrike(CALL_DKI_BASE, RATE_PROVIDER,
        VOLS);
    CurrencyParameterSensitivities pvSensiBase =
        RATE_PROVIDER.parameterSensitivity(pointBase.build()).convertedTo(EUR, RATE_PROVIDER);
    double[] eurSensiBase = new double[] {0.0, 0.0, 0.0, -5.885393657463378E7, -4.247477498074986E7 };
    double[] usdSensiBase = new double[] {0.0, 0.0, 0.0, 4.663853277047497E7, 3.365894110322015E7 };
    assertThat(DoubleArrayMath.fuzzyEquals(
        eurSensiBase,
        pvSensiBase.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(
        usdSensiBase,
        pvSensiBase.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    PointSensitivityBuilder pointPut =
        PRICER.presentValueSensitivityRatesStickyStrike(PUT_DKO, RATE_PROVIDER, VOLS).multipliedBy(-1d);
    CurrencyParameterSensitivities pvSensiPut = RATE_PROVIDER.parameterSensitivity(pointPut.build());
    double[] eurSensiPut = new double[] {0.0, 0.0, 0.0, 22176.623866383557, 16004.827601682477 };
    double[] usdSensiPut = new double[] {0.0, 0.0, 0.0, -48509.60688347871, -35009.29176024644 };
    assertThat(DoubleArrayMath.fuzzyEquals(
        eurSensiPut,
        pvSensiPut.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), USD).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(
        usdSensiPut,
        pvSensiPut.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), USD).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    PointSensitivityBuilder pointPutBase =
        PRICER.presentValueSensitivityRatesStickyStrike(PUT_DKO_BASE, RATE_PROVIDER, VOLS).multipliedBy(-1d);
    CurrencyParameterSensitivities pvSensiPutBase =
        RATE_PROVIDER.parameterSensitivity(pointPutBase.build()).convertedTo(EUR, RATE_PROVIDER);
    double[] eurSensiPutBase = new double[] {0.0, 0.0, 0.0, 24062.637495868825, 17365.96007956571 };
    double[] usdSensiPutBase = new double[] {0.0, 0.0, 0.0, -44888.77092190999, -32396.141278548253 };
    assertThat(DoubleArrayMath.fuzzyEquals(
        eurSensiPutBase,
        pvSensiPutBase.getSensitivity(RatesProviderFxDataSets.getCurveName(EUR), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(
        usdSensiPutBase,
        pvSensiPutBase.getSensitivity(RatesProviderFxDataSets.getCurveName(USD), EUR).getSensitivity().toArray(),
        NOTIONAL * TOL)).isTrue();
  }

  @Test
  public void regression_volSensitivity() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityModelParamsVolatility(CALL_DKI, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivity pvSensi =
        VOLS.parameterSensitivity((FxOptionSensitivity) point).getSensitivities().get(0);
    PointSensitivityBuilder pointBase =
        PRICER.presentValueSensitivityModelParamsVolatility(CALL_DKI_BASE, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivity pvSensiBase = VOLS
        .parameterSensitivity((FxOptionSensitivity) pointBase).convertedTo(EUR, RATE_PROVIDER).getSensitivities().get(0);
    PointSensitivityBuilder pointPut =
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_DKO, RATE_PROVIDER, VOLS).multipliedBy(-1d);
    CurrencyParameterSensitivity pvSensiPut =
        VOLS.parameterSensitivity((FxOptionSensitivity) pointPut).getSensitivities().get(0);
    PointSensitivityBuilder pointPutBase =
        PRICER.presentValueSensitivityModelParamsVolatility(PUT_DKO_BASE, RATE_PROVIDER, VOLS).multipliedBy(-1d);
    CurrencyParameterSensitivity pvSensiPutBase = VOLS
        .parameterSensitivity((FxOptionSensitivity) pointPutBase).convertedTo(EUR, RATE_PROVIDER).getSensitivities().get(0);
    double[] computed = pvSensi.getSensitivity().toArray();
    double[] computedBase = pvSensiBase.getSensitivity().toArray();
    double[] computedPut = pvSensiPut.getSensitivity().toArray();
    double[] computedPutBase = pvSensiPutBase.getSensitivity().toArray();
    double[][] expected = new double[][] {
        {0.0, 0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 0.0, 3.154862889936005E7, 186467.57005640838, 0.0},
        {0.0, 0.0, 5.688931113627187E7, 336243.18963600876, 0.0}};
    double[][] expectedBase = new double[][] {
        {0.0, 0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 0.0, 2.2532363577178854E7, 133177.10564432456, 0.0},
        {0.0, 0.0, 4.063094615828866E7, 240148.4331822043, 0.0}};
    double[][] expectedPut = new double[][] {
        {-0.0, -0.0, -0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0, -0.0, -0.0},
        {-0.0, -0.0, -0.0, -0.0, -0.0}, {-0.0, -0.0, -53011.143048566446, -313.32135103910525, -0.0},
        {-0.0, -0.0, -95591.07688006328, -564.989238732409, -0.0}};
    double[][] expectedPutBase = new double[][] {
        {-0.0, -0.0, -0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0, -0.0, -0.0},
        {-0.0, -0.0, -35148.33541137355, -207.743566815316, -0.0},
        {-0.0, -0.0, -63380.39588085656, -374.6086223530026, -0.0}};
    for (int i = 0; i < computed.length; ++i) {
      int row = i / 5;
      int col = i % 5;
      assertThat(DoubleMath.fuzzyEquals(computed[i], expected[row][col], NOTIONAL * TOL)).isTrue();
      assertThat(DoubleMath.fuzzyEquals(computedBase[i], expectedBase[row][col], NOTIONAL * TOL)).isTrue();
      assertThat(DoubleMath.fuzzyEquals(computedPut[i], expectedPut[row][col], NOTIONAL * TOL)).isTrue();
      assertThat(DoubleMath.fuzzyEquals(computedPutBase[i], expectedPutBase[row][col], NOTIONAL * TOL)).isTrue();
    }
  }

  @Test
  public void regression_currencyExposure() {
    MultiCurrencyAmount pv = PRICER.currencyExposure(CALL_DKI, RATE_PROVIDER, VOLS);
    assertThat(pv.getAmount(EUR).getAmount()).isCloseTo(-2.8939530642669797E7, offset(NOTIONAL * TOL));
    assertThat(pv.getAmount(USD).getAmount()).isCloseTo(4.955034902917114E7, offset(NOTIONAL * TOL));
    MultiCurrencyAmount pvBase = PRICER.currencyExposure(CALL_DKI_BASE, RATE_PROVIDER, VOLS);
    assertThat(pvBase.getAmount(EUR).getAmount()).isCloseTo(-2.8866459583853487E7, offset(NOTIONAL * TOL));
    assertThat(pvBase.getAmount(USD).getAmount()).isCloseTo(4.9451699813814424E7, offset(NOTIONAL * TOL));
    MultiCurrencyAmount pvPut = PRICER.currencyExposure(PUT_DKO, RATE_PROVIDER, VOLS);
    assertThat(pvPut.getAmount(EUR).getAmount()).isCloseTo(-105918.46956467835, offset(NOTIONAL * TOL));
    assertThat(pvPut.getAmount(USD).getAmount()).isCloseTo(92916.36867744842, offset(NOTIONAL * TOL));
    MultiCurrencyAmount pvPutBase = PRICER.currencyExposure(PUT_DKO_BASE, RATE_PROVIDER, VOLS);
    assertThat(pvPutBase.getAmount(EUR).getAmount()).isCloseTo(-76234.66256109312, offset(NOTIONAL * TOL));
    assertThat(pvPutBase.getAmount(USD).getAmount()).isCloseTo(35358.56586522361, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_continuity_at_barrier() {
    double eps = 1e-5;
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      double barrier = option.getBarrier().getBarrierLevel(VAL_DATE);
      double rateUp = barrier + eps;
      double rateDw = barrier - eps;
      ImmutableRatesProvider providerUp = ratesProviderWithFxRate(option, RATE_PROVIDER, rateUp);
      ImmutableRatesProvider providerDw = ratesProviderWithFxRate(option, RATE_PROVIDER, rateDw);
      double priceUp = PRICER.price(option, providerUp, VOLS);
      double priceDw = PRICER.price(option, providerDw, VOLS);
      double pvUp = PRICER.presentValue(option, providerUp, VOLS).getAmount();
      double pvDw = PRICER.presentValue(option, providerDw, VOLS).getAmount();
      assertThat(priceUp).isCloseTo(priceDw, Offset.strictOffset(eps * 10d));
      double referenceAmount = option.getUnderlyingOption().getUnderlying().getReceiveCurrencyAmount().getAmount();
      assertThat(pvUp).isCloseTo(pvDw, Offset.strictOffset(eps * referenceAmount * 10d));
    }
  }

  @Test
  public void test_delta_gamma_touched() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      double barrier = option.getBarrier().getBarrierLevel(VAL_DATE);
      double fxRateTouched = option.getBarrier().getBarrierType().isDown() ? barrier * 0.9 : barrier * 1.1;
      ImmutableRatesProvider ratesProviderTouched = ratesProviderWithFxRate(option, RATE_PROVIDER, fxRateTouched);
      double rateUp = fxRateTouched + FD_EPS;
      double rateDw = fxRateTouched - FD_EPS;
      ImmutableRatesProvider providerUp = ratesProviderWithFxRate(option, RATE_PROVIDER, rateUp);
      ImmutableRatesProvider providerDw = ratesProviderWithFxRate(option, RATE_PROVIDER, rateDw);
      double deltaComputed = PRICER.delta(option, ratesProviderTouched, VOLS_FLAT);
      double gammaComputed = PRICER.gamma(option, ratesProviderTouched, VOLS_FLAT);
      double priceUp = PRICER.price(option, providerUp, VOLS_FLAT);
      double priceDw = PRICER.price(option, providerDw, VOLS_FLAT);
      double deltaExpected = 0.5 * (priceUp - priceDw) / FD_EPS;
      assertThat(deltaComputed).isCloseTo(deltaExpected, Offset.strictOffset(FD_EPS * 10d));
      double deltaUp = PRICER.delta(option, providerUp, VOLS_FLAT);
      double deltaDw = PRICER.delta(option, providerDw, VOLS_FLAT);
      double gammaExpected = 0.5 * (deltaUp - deltaDw) / FD_EPS;
      assertThat(gammaComputed).isCloseTo(gammaExpected, Offset.strictOffset(FD_EPS * 10d));
    }
  }

  @Test
  public void test_theta_touched() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      double barrier = option.getBarrier().getBarrierLevel(VAL_DATE);
      double fxRateTouched = option.getBarrier().getBarrierType().isDown() ? barrier * 0.9 : barrier * 1.1;
      ImmutableRatesProvider ratesProviderTouched = ratesProviderWithFxRate(
          option,
          RatesProviderFxDataSets.createProviderEurUsdFlat(VAL_DATE),
          fxRateTouched);
      ImmutableRatesProvider providerUp = ratesProviderWithFxRate(
          option,
          RatesProviderFxDataSets.createProviderEurUsdFlat(VAL_DATE.plusDays(1)),
          fxRateTouched);
      ImmutableRatesProvider providerDw = ratesProviderWithFxRate(
          option,
          RatesProviderFxDataSets.createProviderEurUsdFlat(VAL_DATE.minusDays(1)),
          fxRateTouched);
      BlackFxOptionSmileVolatilities volsUp = FxVolatilitySmileDataSet.createVolatilitySmileProvider5Flat(
          VAL_DATETIME.plusDays(1));
      BlackFxOptionSmileVolatilities volsDw = FxVolatilitySmileDataSet.createVolatilitySmileProvider5Flat(
          VAL_DATETIME.minusDays(1));
      double thetaComputed = PRICER.theta(option, ratesProviderTouched, VOLS_FLAT);
      double priceUp = PRICER.price(option, providerUp, volsUp);
      double priceDw = PRICER.price(option, providerDw, volsDw);
      double thetaExpected = 0.5 * (priceUp - priceDw) * 365d;
      assertThat(thetaComputed).isCloseTo(thetaExpected, Offset.strictOffset(0.1 / 365d));
    }
  }

  @Test
  public void test_presentValueSensitivityRatesStickyStrike_touched() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      double barrier = option.getBarrier().getBarrierLevel(VAL_DATE);
      double fxRateTouched = option.getBarrier().getBarrierType().isDown() ? barrier * 0.9 : barrier * 1.1;
      ImmutableRatesProvider ratesProviderTouched = ratesProviderWithFxRate(option, RATE_PROVIDER, fxRateTouched);
      PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(
          option, ratesProviderTouched, VOLS_FLAT);
      CurrencyParameterSensitivities computed = ratesProviderTouched.parameterSensitivity(point.build());
      CurrencyParameterSensitivities expected = FD_CAL.sensitivity(
          ratesProviderTouched, p -> PRICER.presentValue(option, p, VOLS_FLAT));
      double referenceAmount = option.getUnderlyingOption().getUnderlying().getReceiveCurrencyAmount().getAmount();
      assertThat(computed.equalWithTolerance(expected, referenceAmount * FD_EPS * 10d)).isTrue();
    }
  }

  @Test
  public void test_presentValueSensitivityModelParamsVolatility_touched() {
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      double barrier = option.getBarrier().getBarrierLevel(VAL_DATE);
      double fxRateTouched = option.getBarrier().getBarrierType().isDown() ? barrier * 0.9 : barrier * 1.1;
      ImmutableRatesProvider ratesProviderTouched = ratesProviderWithFxRate(option, RATE_PROVIDER, fxRateTouched);
      PointSensitivityBuilder points = PRICER.presentValueSensitivityModelParamsVolatility(
          option, ratesProviderTouched, VOLS_FLAT);
      CurrencyParameterSensitivities computed = VOLS_FLAT.parameterSensitivity(points.build());
      double referenceAmount = option.getUnderlyingOption().getUnderlying().getReceiveCurrencyAmount().getAmount();
      Optional<CurrencyParameterSensitivity> optSensi = computed.findSensitivity(
          VOLS_FLAT.getName(), option.getUnderlyingOption().getCurrencyPair().getCounter());
      double totalComputed = optSensi.isPresent() ? optSensi.get().getSensitivity().sum() : 0d;
      BlackFxOptionSmileVolatilities volsUp = VOLS_FLAT.withPerturbation((index, vol, metadata) -> vol + FD_EPS);
      BlackFxOptionSmileVolatilities volsDw = VOLS_FLAT.withPerturbation((index, vol, metadata) -> vol - FD_EPS);
      CurrencyAmount pvUp = PRICER.presentValue(option, ratesProviderTouched, volsUp);
      CurrencyAmount pvDw = PRICER.presentValue(option, ratesProviderTouched, volsDw);
      double totalExpected = 0.5 * (pvUp.getAmount() - pvDw.getAmount()) / FD_EPS;
      assertThat(totalComputed).isCloseTo(
          totalExpected, Offset.strictOffset(referenceAmount * FD_EPS));
      double signedNotional = (option.getUnderlyingOption().getLongShort().isLong() ? 1d : -1d) *
          Math.abs(option.getUnderlyingOption().getUnderlying().getBaseCurrencyPayment().getAmount());
      double vegaComputed = PRICER.vega(option, ratesProviderTouched, VOLS_FLAT);
      assertThat(vegaComputed).isCloseTo(totalComputed / signedNotional, Offset.strictOffset(TOL));
    }
  }

  @Test
  public void test_currencyExposure_touched() {
    Currency foreignCurrency = Currency.GBP;
    double eurGbpStart = 0.80;
    for (ResolvedFxSingleBarrierOption option : OPTION_ALL) {
      double barrier = option.getBarrier().getBarrierLevel(VAL_DATE);
      CurrencyPair currencyPair = option.getCurrencyPair();
      double spotTouched = option.getBarrier().getBarrierType().isDown() ? barrier * 0.9 : barrier * 1.1;
      double spot = currencyPair.getBase().equals(EUR) ? spotTouched : 1d / spotTouched;
      ImmutableRatesProvider ratesProvider = ratesProviderWithFxRate(option, RATE_PROVIDER, spot);
      FxMatrix fxMatrixStart = FxMatrix.builder().addRate(EUR, USD, spot)
          .addRate(EUR, foreignCurrency, eurGbpStart).build();
      FxMatrix fxMatrixEurGbpUp = fxMatrixStart.toBuilder()
          .addRate(USD, EUR, 1.0d / spot + FD_EPS).build(); // EUR updated
      ImmutableRatesProvider rateProvideEurGbpUp = ratesProvider.toImmutableRatesProvider().toBuilder()
          .fxRateProvider(fxMatrixEurGbpUp).build();
      FxMatrix fxMatrixGbpUsdUp = fxMatrixStart.toBuilder()
          .addRate(EUR, USD, spot + FD_EPS).build(); // USD updated
      ImmutableRatesProvider rateProvideGbpUsdUp = ratesProvider.toImmutableRatesProvider().toBuilder()
          .fxRateProvider(fxMatrixGbpUsdUp).build();
      double referenceAmount = option.getUnderlyingOption().getUnderlying().getReceiveCurrencyAmount().getAmount();
      Offset<Double> offset = Offset.strictOffset(referenceAmount * FD_EPS);
      CurrencyAmount pv = PRICER.presentValue(option, ratesProvider, VOLS_FLAT);
      MultiCurrencyAmount ce = PRICER.currencyExposure(option, ratesProvider, VOLS_FLAT);
      assertThat(pv.convertedTo(foreignCurrency, fxMatrixStart).getAmount())
          .isCloseTo(ce.convertedTo(foreignCurrency, fxMatrixStart).getAmount(), offset);
      CurrencyAmount pvEurGbpUp = PRICER.presentValue(option, rateProvideEurGbpUp, VOLS_FLAT);
      CurrencyAmount plPvEurGbpUp = pvEurGbpUp.convertedTo(foreignCurrency, fxMatrixEurGbpUp)
          .minus(pv.convertedTo(foreignCurrency, fxMatrixStart));
      CurrencyAmount plCeEurGbpUp = ce.convertedTo(foreignCurrency, fxMatrixEurGbpUp)
          .minus(ce.convertedTo(foreignCurrency, fxMatrixStart));
      assertThat(plPvEurGbpUp.getAmount()).isCloseTo(plCeEurGbpUp.getAmount(), offset);
      CurrencyAmount pvGbpUsdUp = PRICER.presentValue(option, rateProvideGbpUsdUp, VOLS_FLAT);
      CurrencyAmount plPvGbpUsdUp = pvGbpUsdUp.convertedTo(foreignCurrency, fxMatrixGbpUsdUp)
          .minus(pv.convertedTo(foreignCurrency, fxMatrixStart));
      CurrencyAmount plCeGbpUsdUp = ce.convertedTo(foreignCurrency, fxMatrixGbpUsdUp)
          .minus(ce.convertedTo(foreignCurrency, fxMatrixStart));
      assertThat(plPvGbpUsdUp.getAmount()).isCloseTo(plCeGbpUsdUp.getAmount(), offset);
    }
  }

  private ImmutableRatesProvider ratesProviderWithFxRate(
      ResolvedFxSingleBarrierOption option,
      ImmutableRatesProvider baseRatesProvider,
      double fxRate) {

    FxMatrix fxMatrix = FxMatrix.builder().addRate(option.getCurrencyPair(), fxRate).build();
    return baseRatesProvider.toBuilder().fxRateProvider(fxMatrix).build();
  }

}
