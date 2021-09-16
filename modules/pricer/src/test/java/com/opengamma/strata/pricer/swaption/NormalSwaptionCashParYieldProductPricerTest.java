/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link NormalSwaptionCashParYieldProductPricer}.
 */
public class NormalSwaptionCashParYieldProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate SWAPTION_EXERCISE_DATE = VAL_DATE.plusYears(5);
  private static final LocalDate SWAPTION_PAST_EXERCISE_DATE = VAL_DATE.minusYears(1);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SWAP_EFFECTIVE_DATE =
      USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE, REF_DATA);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final Period SWAP_TENOR = Period.ofYears(SWAP_TENOR_YEAR);
  private static final LocalDate SWAP_MATURITY_DATE = SWAP_EFFECTIVE_DATE.plus(SWAP_TENOR);
  private static final double STRIKE = 0.01;
  private static final double NOTIONAL = 100_000_000;
  private static final Swap SWAP_REC = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, SELL, NOTIONAL, STRIKE).getProduct();
  private static final ResolvedSwap RSWAP_REC = SWAP_REC.resolve(REF_DATA);
  private static final Swap SWAP_PAY = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, BUY, NOTIONAL, STRIKE).getProduct();
  private static final ResolvedSwap RSWAP_PAY = SWAP_PAY.resolve(REF_DATA);
  private static final Swap SWAP_REC_PAST = USD_FIXED_6M_LIBOR_3M // Only for checks; no actual computation on that swap
      .toTrade(SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE.plusYears(10),
          SELL, NOTIONAL, STRIKE).getProduct();
  private static final Swap SWAP_PAY_PAST = USD_FIXED_6M_LIBOR_3M // Only for checks; no actual computation on that swap
      .toTrade(SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE.plusYears(10),
          BUY, NOTIONAL, STRIKE).getProduct();
  private static final LocalDate SETTLE_DATE = USD_LIBOR_3M.getEffectiveDateOffset().adjust(SWAPTION_EXERCISE_DATE, REF_DATA);
  private static final CashSwaptionSettlement PAR_YIELD =
      CashSwaptionSettlement.of(SETTLE_DATE, CashSwaptionSettlementMethod.PAR_YIELD);
  private static final ResolvedSwaption SWAPTION_REC_LONG = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_SHORT = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_LONG = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_LONG_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(VAL_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(VAL_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_LONG_PAST = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_PAST_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC_PAST)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT_PAST = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_PAST_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY_PAST)
      .build()
      .resolve(REF_DATA);
  // volatility and rate providers
  private static final ImmutableRatesProvider RATE_PROVIDER = RatesProviderDataSets.multiUsd(VAL_DATE);
  private static final NormalSwaptionExpiryTenorVolatilities VOLS =
      SwaptionNormalVolatilityDataSets.NORMAL_SWAPTION_VOLS_USD_STD;
  private static final NormalSwaptionVolatilities VOLS_FLAT =
      SwaptionNormalVolatilityDataSets.NORMAL_SWAPTION_VOLS_USD_FLAT;
  // test parameters
  private static final double FD_EPS = 1.0E-7;
  private static final double TOL = 1.0e-12;
  // pricers
  private static final NormalPriceFunction NORMAL = new NormalPriceFunction();
  private static final NormalSwaptionCashParYieldProductPricer PRICER_SWAPTION =
      NormalSwaptionCashParYieldProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  @Test
  void test_presentValue() {
    CurrencyAmount pvRecComputed = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayComputed = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvRecExpected = NORMAL.getPriceFunction(optionRec).apply(normalData);
    double pvPayExpected = -NORMAL.getPriceFunction(optionPay).apply(normalData);
    assertThat(pvRecComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvRecComputed.getAmount()).isCloseTo(pvRecExpected, offset(NOTIONAL * TOL));
    assertThat(pvPayComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvPayComputed.getAmount()).isCloseTo(pvPayExpected, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_at_expiry() {
    CurrencyAmount pvRec = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    assertThat(pvRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvPay.getAmount()).isCloseTo(discount * annuityCash * (STRIKE - forward), offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_after_expiry() {
    CurrencyAmount pvRec = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(pvRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvRecShort = PRICER_SWAPTION.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayLong = PRICER_SWAPTION.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayShort = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvRecLong.getAmount()).isCloseTo(-pvRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvPayLong.getAmount()).isCloseTo(-pvPayShort.getAmount(), offset(NOTIONAL * TOL));
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    double expected = discount * annuityCash * (forward - STRIKE);
    assertThat(pvPayLong.getAmount() - pvRecLong.getAmount()).isCloseTo(expected, offset(NOTIONAL * TOL));
    assertThat(pvPayShort.getAmount() - pvRecShort.getAmount()).isCloseTo(-expected, offset(NOTIONAL * TOL));
  }

  @Test
  void test_physicalSettlement() {
    Swaption swaption = Swaption.builder()
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
        .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
        .expiryTime(SWAPTION_EXPIRY_TIME)
        .expiryZone(SWAPTION_EXPIRY_ZONE)
        .longShort(LongShort.LONG)
        .underlying(SWAP_REC)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION.presentValue(swaption.resolve(REF_DATA), RATE_PROVIDER, VOLS));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueDelta() {
    CurrencyAmount pvDeltaRecComputed =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPayComputed =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvDeltaRecExpected = NORMAL.getDelta(optionRec, normalData);
    double pvDeltaPayExpected = -NORMAL.getDelta(optionPay, normalData);
    assertThat(pvDeltaRecComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvDeltaRecComputed.getAmount()).isCloseTo(pvDeltaRecExpected, offset(NOTIONAL * TOL));
    assertThat(pvDeltaPayComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvDeltaPayComputed.getAmount()).isCloseTo(pvDeltaPayExpected, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueDelta_at_expiry() {
    CurrencyAmount pvDeltaRec =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPay =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    assertThat(pvDeltaRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvDeltaPay.getAmount()).isCloseTo(-discount * annuityCash, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueDelta_after_expiry() {
    CurrencyAmount pvDeltaRec = PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPay = PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(pvDeltaRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvDeltaPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueDelta_parity() {
    CurrencyAmount pvDeltaRecLong = PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaRecShort = PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPayLong = PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPayShort = PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvDeltaRecLong.getAmount()).isCloseTo(-pvDeltaRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvDeltaPayLong.getAmount()).isCloseTo(-pvDeltaPayShort.getAmount(), offset(NOTIONAL * TOL));
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    double expected = discount * annuityCash;
    assertThat(pvDeltaPayLong.getAmount() - pvDeltaRecLong.getAmount()).isCloseTo(expected, offset(NOTIONAL * TOL));
    assertThat(pvDeltaPayShort.getAmount() - pvDeltaRecShort.getAmount()).isCloseTo(-expected, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueGamma() {
    CurrencyAmount pvGammaRecComputed =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPayComputed =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvGammaRecExpected = NORMAL.getGamma(optionRec, normalData);
    double pvGammaPayExpected = -NORMAL.getGamma(optionPay, normalData);
    assertThat(pvGammaRecComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvGammaRecComputed.getAmount()).isCloseTo(pvGammaRecExpected, offset(NOTIONAL * TOL));
    assertThat(pvGammaPayComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvGammaPayComputed.getAmount()).isCloseTo(pvGammaPayExpected, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueGamma_at_expiry() {
    CurrencyAmount pvGammaRec =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPay =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    assertThat(pvGammaRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvGammaPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueGamma_after_expiry() {
    CurrencyAmount pvGammaRec = PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPay = PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(pvGammaRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvGammaPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueGamma_parity() {
    CurrencyAmount pvGammaRecLong = PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaRecShort = PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPayLong = PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPayShort = PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvGammaRecLong.getAmount()).isCloseTo(-pvGammaRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvGammaPayLong.getAmount()).isCloseTo(-pvGammaPayShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvGammaPayLong.getAmount()).isCloseTo(pvGammaRecLong.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvGammaPayShort.getAmount()).isCloseTo(pvGammaRecShort.getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueTheta() {
    CurrencyAmount pvThetaRecComputed =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPayComputed =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE,
        forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvThetaRecExpected = NORMAL.getTheta(optionRec, normalData);
    double pvThetaPayExpected = -NORMAL.getTheta(optionPay, normalData);
    assertThat(pvThetaRecComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvThetaRecComputed.getAmount()).isCloseTo(pvThetaRecExpected, offset(NOTIONAL * TOL));
    assertThat(pvThetaPayComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvThetaPayComputed.getAmount()).isCloseTo(pvThetaPayExpected, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueTheta_at_expiry() {
    CurrencyAmount pvThetaRec =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPay =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    assertThat(pvThetaRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvThetaPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueTheta_after_expiry() {
    CurrencyAmount pvThetaRec = PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPay = PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(pvThetaRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(pvThetaPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueTheta_parity() {
    CurrencyAmount pvThetaRecLong = PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaRecShort = PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPayLong = PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPayShort = PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvThetaRecLong.getAmount()).isCloseTo(-pvThetaRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvThetaPayLong.getAmount()).isCloseTo(-pvThetaPayShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvThetaPayLong.getAmount()).isCloseTo(pvThetaRecLong.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvThetaPayShort.getAmount()).isCloseTo(pvThetaRecShort.getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------  
  @Test
  void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER_SWAPTION.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount computedPay = PRICER_SWAPTION.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pointRec =
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(USD).getAmount()).isCloseTo(expectedRec.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(USD).getAmount()).isCloseTo(expectedPay.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  void test_currencyExposure_at_expiry() {
    MultiCurrencyAmount computedRec =
        PRICER_SWAPTION.currencyExposure(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount computedPay =
        PRICER_SWAPTION.currencyExposure(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pointRec =
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(USD).getAmount()).isCloseTo(expectedRec.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(USD).getAmount()).isCloseTo(expectedPay.getAmount(USD).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  void test_currencyExposure_after_expiry() {
    MultiCurrencyAmount computedRec =
        PRICER_SWAPTION.currencyExposure(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount computedPay =
        PRICER_SWAPTION.currencyExposure(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(USD).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(USD).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_impliedVolatility() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double expected = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double computedRec = PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    double computedPay = PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(computedRec).isCloseTo(expected, offset(TOL));
    assertThat(computedPay).isCloseTo(expected, offset(TOL));
  }

  @Test
  void test_impliedVolatility_at_expiry() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double expected = VOLS.volatility(
        VAL_DATE.atTime(SWAPTION_EXPIRY_TIME).atZone(SWAPTION_EXPIRY_ZONE), SWAP_TENOR_YEAR, STRIKE, forward);
    double computedRec = PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    double computedPay = PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    assertThat(computedRec).isCloseTo(expected, offset(TOL));
    assertThat(computedPay).isCloseTo(expected, offset(TOL));
  }

  @Test
  void test_impliedVolatility_after_expiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS));
  }

  //-------------------------------------------------------------------------
  @Test
  void implied_volatility_round_trip() { // Compute pv and then implied vol from PV and compare with direct implied vol
    CurrencyAmount pvLongRec =
        PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    double impliedLongRecComputed = PRICER_SWAPTION.impliedVolatilityFromPresentValue(
        SWAPTION_REC_LONG, RATE_PROVIDER, ACT_365F, pvLongRec.getAmount());
    double impliedLongRecInterpolated =
        PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    assertThat(impliedLongRecComputed).isCloseTo(impliedLongRecInterpolated, offset(TOL));

    CurrencyAmount pvLongPay =
        PRICER_SWAPTION.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    double impliedLongPayComputed = PRICER_SWAPTION.impliedVolatilityFromPresentValue(
        SWAPTION_PAY_LONG, RATE_PROVIDER, ACT_365F, pvLongPay.getAmount());
    double impliedLongPayInterpolated =
        PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    assertThat(impliedLongPayComputed).isCloseTo(impliedLongPayInterpolated, offset(TOL));

    CurrencyAmount pvShortRec =
        PRICER_SWAPTION.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    double impliedShortRecComputed = PRICER_SWAPTION.impliedVolatilityFromPresentValue(
        SWAPTION_REC_SHORT, RATE_PROVIDER, ACT_365F, pvShortRec.getAmount());
    double impliedShortRecInterpolated =
        PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    assertThat(impliedShortRecComputed).isCloseTo(impliedShortRecInterpolated, offset(TOL));
  }

  @Test
  void implied_volatility_wrong_sign() {
    CurrencyAmount pvLongRec =
        PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION.impliedVolatilityFromPresentValue(
        SWAPTION_REC_LONG, RATE_PROVIDER, ACT_365F, -pvLongRec.getAmount()));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueSensitivityRatesStickyStrike() {
    PointSensitivities pointRec = PRICER_SWAPTION
        .presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS_FLAT).build();
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec);
    CurrencyParameterSensitivities expectedRec = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATE_PROVIDER, (p) -> PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, p, VOLS_FLAT));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 200d)).isTrue();
    PointSensitivities pointPay = PRICER_SWAPTION
        .presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS_FLAT).build();
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay);
    CurrencyParameterSensitivities expectedPay = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATE_PROVIDER, (p) -> PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, p, VOLS_FLAT));
    assertThat(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 200d)).isTrue();
  }

  @Test
  void test_presentValueSensitivityRatesStickyStrike_at_expiry() {
    PointSensitivities pointRec = PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(
        SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
    PointSensitivities pointPay = PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(
        SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS).build();
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay);
    CurrencyParameterSensitivities expectedPay = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATE_PROVIDER, (p) -> PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_AT_EXPIRY, p, VOLS_FLAT));
    assertThat(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d)).isTrue();
  }

  @Test
  void test_presentValueSensitivityRatesStickyStrike_after_expiry() {
    PointSensitivityBuilder pointRec = PRICER_SWAPTION
        .presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pointPay = PRICER_SWAPTION
        .presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(pointRec).isEqualTo(PointSensitivityBuilder.none());
    assertThat(pointPay).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  void test_presentValueSensitivityRatesStickyStrike_parity() {
    CurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build());
    assertThat(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();

    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    PointSensitivityBuilder forwardSensi = PRICER_SWAP.parRateSensitivity(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double annuityCashDeriv = PRICER_SWAP.getLegPricer()
        .annuityCashDerivative(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward).getDerivative(0);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    PointSensitivityBuilder discountSensi = RATE_PROVIDER.discountFactors(USD).zeroRatePointSensitivity(SETTLE_DATE);
    PointSensitivities expecedPoint = discountSensi.multipliedBy(annuityCash * (forward - STRIKE)).combinedWith(
        forwardSensi.multipliedBy(discount * annuityCash + discount * annuityCashDeriv * (forward - STRIKE))).build();
    CurrencyParameterSensitivities expected = RATE_PROVIDER.parameterSensitivity(expecedPoint);
    assertThat(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL)).isTrue();
    assertThat(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  void test_presentValueSensitivityNormalVolatility() {
    SwaptionSensitivity computedRec = PRICER_SWAPTION
        .presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvRecUp = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(FD_EPS));
    CurrencyAmount pvRecDw = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(-FD_EPS));
    double expectedRec = 0.5 * (pvRecUp.getAmount() - pvRecDw.getAmount()) / FD_EPS;
    assertThat(computedRec.getCurrency()).isEqualTo(USD);
    assertThat(computedRec.getSensitivity()).isCloseTo(expectedRec, offset(FD_EPS * NOTIONAL));
    assertThat(computedRec.getVolatilitiesName()).isEqualTo(VOLS.getName());
    assertThat(computedRec.getExpiry()).isEqualTo(VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry()));
    assertThat(computedRec.getTenor()).isCloseTo(SWAP_TENOR_YEAR, offset(TOL));
    assertThat(computedRec.getStrike()).isCloseTo(STRIKE, offset(TOL));
    assertThat(computedRec.getForward()).isCloseTo(PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER), offset(TOL));
    SwaptionSensitivity computedPay = PRICER_SWAPTION
        .presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvUpPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(FD_EPS));
    CurrencyAmount pvDwPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(-FD_EPS));
    double expectedPay = 0.5 * (pvUpPay.getAmount() - pvDwPay.getAmount()) / FD_EPS;
    assertThat(computedPay.getCurrency()).isEqualTo(USD);
    assertThat(computedPay.getSensitivity()).isCloseTo(expectedPay, offset(FD_EPS * NOTIONAL));
    assertThat(computedPay.getVolatilitiesName()).isEqualTo(VOLS.getName());
    assertThat(computedPay.getExpiry()).isEqualTo(VOLS.relativeTime(SWAPTION_PAY_SHORT.getExpiry()));
    assertThat(computedPay.getTenor()).isCloseTo(SWAP_TENOR_YEAR, offset(TOL));
    assertThat(computedPay.getStrike()).isCloseTo(STRIKE, offset(TOL));
    assertThat(computedPay.getForward()).isCloseTo(PRICER_SWAP.parRate(RSWAP_PAY, RATE_PROVIDER), offset(TOL));
  }

  @Test
  void test_presentValueSensitivityNormalVolatility_at_expiry() {
    SwaptionSensitivity sensiRec =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOLS);
    assertThat(sensiRec.getSensitivity()).isCloseTo(0d, offset(NOTIONAL * TOL));
    SwaptionSensitivity sensiPay =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOLS);
    assertThat(sensiPay.getSensitivity()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueSensitivityNormalVolatility_after_expiry() {
    SwaptionSensitivity sensiRec =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOLS);
    SwaptionSensitivity sensiPay =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOLS);
    assertThat(sensiRec.getSensitivity()).isCloseTo(0.0d, offset(NOTIONAL * TOL));
    assertThat(sensiPay.getSensitivity()).isCloseTo(0.0d, offset(NOTIONAL * TOL));
  }

  @Test
  void test_presentValueSensitivityNormalVolatility_parity() {
    SwaptionSensitivity pvSensiRecLong =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity pvSensiRecShort =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    SwaptionSensitivity pvSensiPayLong =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity pvSensiPayShort =
        PRICER_SWAPTION.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvSensiRecLong.getSensitivity()).isCloseTo(-pvSensiRecShort.getSensitivity(), offset(NOTIONAL * TOL));
    assertThat(pvSensiPayLong.getSensitivity()).isCloseTo(-pvSensiPayShort.getSensitivity(), offset(NOTIONAL * TOL));
    assertThat(pvSensiRecLong.getSensitivity()).isCloseTo(pvSensiPayLong.getSensitivity(), offset(NOTIONAL * TOL));
    assertThat(pvSensiPayShort.getSensitivity()).isCloseTo(pvSensiPayShort.getSensitivity(), offset(NOTIONAL * TOL));
  }

}
