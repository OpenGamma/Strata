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
import com.opengamma.strata.product.swaption.SwaptionSettlement;

/**
 * Tests {@link NormalSwaptionPhysicalProductPricer}.
 */
public class NormalSwaptionPhysicalProductPricerTest {

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
  private static final Swap SWAP_PAST = USD_FIXED_6M_LIBOR_3M // Only for checks; no actual computation on that swap
      .toTrade(SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE.plusYears(10),
          BUY, NOTIONAL, STRIKE).getProduct();
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE =
      CashSwaptionSettlement.of(SWAP_REC.getStartDate().getUnadjusted(), CashSwaptionSettlementMethod.PAR_YIELD);

  private static final ResolvedSwaption SWAPTION_LONG_REC = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_SHORT_REC = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_LONG_PAY = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_LONG_REC_CASH = Swaption.builder()
      .swaptionSettlement(CASH_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(VAL_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(VAL_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAST = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_PAST_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAST)
      .build()
      .resolve(REF_DATA);

  private static final NormalPriceFunction NORMAL = new NormalPriceFunction();
  private static final NormalSwaptionPhysicalProductPricer PRICER_SWAPTION_NORMAL =
      NormalSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final double FD_SHIFT = 0.5E-8;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);

  private static final ImmutableRatesProvider MULTI_USD = RatesProviderDataSets.multiUsd(VAL_DATE);
  private static final NormalSwaptionExpiryTenorVolatilities NORMAL_VOLS_USD_STD =
      SwaptionNormalVolatilityDataSets.NORMAL_SWAPTION_VOLS_USD_STD;
  private static final NormalSwaptionVolatilities NORMAL_VOLS_USD_FLAT =
      SwaptionNormalVolatilityDataSets.NORMAL_SWAPTION_VOLS_USD_FLAT;

  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_PV_DELTA = 1.0E+2;
  private static final double TOLERANCE_PV_VEGA = 1.0E+4;
  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  void validate_physical_settlement() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC_CASH, MULTI_USD,
        NORMAL_VOLS_USD_STD));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_implied_volatility() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, MULTI_USD);
    double volExpected = NORMAL_VOLS_USD_STD.volatility(SWAPTION_LONG_REC.getExpiry(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    double volComputed = PRICER_SWAPTION_NORMAL
        .impliedVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE_RATE));
  }

  @Test
  void test_implied_volatility_after_expiry() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION_NORMAL.impliedVolatility(SWAPTION_PAST, MULTI_USD,
        NORMAL_VOLS_USD_STD));
  }

  //-------------------------------------------------------------------------
  @Test
  void implied_volatility_round_trip() { // Compute pv and then implied vol from PV and compare with direct implied vol
    CurrencyAmount pvLongRec =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    double impliedLongRecComputed = PRICER_SWAPTION_NORMAL.impliedVolatilityFromPresentValue(
        SWAPTION_LONG_REC, MULTI_USD, ACT_365F, pvLongRec.getAmount());
    double impliedLongRecInterpolated =
        PRICER_SWAPTION_NORMAL.impliedVolatility(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(impliedLongRecComputed).isCloseTo(impliedLongRecInterpolated, offset(TOLERANCE_RATE));

    CurrencyAmount pvShortRec =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    double impliedShortRecComputed = PRICER_SWAPTION_NORMAL.impliedVolatilityFromPresentValue(
        SWAPTION_SHORT_REC, MULTI_USD, ACT_365F, pvShortRec.getAmount());
    double impliedShortRecInterpolated =
        PRICER_SWAPTION_NORMAL.impliedVolatility(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(impliedShortRecComputed).isCloseTo(impliedShortRecInterpolated, offset(TOLERANCE_RATE));

    CurrencyAmount pvLongPay =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    double impliedLongPayComputed = PRICER_SWAPTION_NORMAL.impliedVolatilityFromPresentValue(
        SWAPTION_LONG_PAY, MULTI_USD, ACT_365F, pvLongPay.getAmount());
    double impliedLongPayInterpolated =
        PRICER_SWAPTION_NORMAL.impliedVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(impliedLongPayComputed).isCloseTo(impliedLongPayInterpolated, offset(TOLERANCE_RATE));
  }

  @Test
  void implied_volatility_wrong_sign() {
    CurrencyAmount pvLongRec =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_SWAPTION_NORMAL.impliedVolatilityFromPresentValue(
        SWAPTION_LONG_REC, MULTI_USD, ACT_365F, -pvLongRec.getAmount()));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_formula() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, MULTI_USD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    double volatility = NORMAL_VOLS_USD_STD.volatility(SWAPTION_LONG_REC.getExpiry(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    double expiry = NORMAL_VOLS_USD_STD.relativeTime(SWAPTION_LONG_REC.getExpiry());
    EuropeanVanillaOption option = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    double pvExpected = NORMAL.getPriceFunction(option).apply(normalData);
    CurrencyAmount pvComputed =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvComputed.getAmount()).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_long_short_parity() {
    CurrencyAmount pvLong =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvShort =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvLong.getAmount()).isCloseTo(-pvShort.getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_payer_receiver_parity() {
    CurrencyAmount pvLongPay =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvShortRec =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    MultiCurrencyAmount pvSwapPay =
        PRICER_SWAP.presentValue(RSWAP_PAY, MULTI_USD);
    assertThat(pvLongPay.getAmount() + pvShortRec.getAmount()).isCloseTo(pvSwapPay.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_at_expiry() {
    CurrencyAmount pvRec =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_REC_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvRec.getAmount()).isCloseTo(0.0d, offset(TOLERANCE_PV));
    CurrencyAmount pvPay =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_PAY_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvPay.getAmount()).isCloseTo(PRICER_SWAP.presentValue(RSWAP_PAY, MULTI_USD).getAmount(USD).getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_after_expiry() {
    CurrencyAmount pv = PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_PAST, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pv.getAmount()).isCloseTo(0.0d, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_delta_formula() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, MULTI_USD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    double volatility = NORMAL_VOLS_USD_STD.volatility(SWAPTION_LONG_REC.getExpiry(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    double expiry = NORMAL_VOLS_USD_STD.relativeTime(SWAPTION_LONG_REC.getExpiry());
    EuropeanVanillaOption option = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    double pvDeltaExpected = NORMAL.getDelta(option, normalData);
    CurrencyAmount pvDeltaComputed =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvDeltaComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvDeltaComputed.getAmount()).isCloseTo(pvDeltaExpected, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_delta_long_short_parity() {
    CurrencyAmount pvDeltaLong =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvDeltaShort =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvDeltaLong.getAmount()).isCloseTo(-pvDeltaShort.getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_delta_payer_receiver_parity() {
    CurrencyAmount pvDeltaLongPay =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvDeltaShortRec =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    assertThat(pvDeltaLongPay.getAmount() + pvDeltaShortRec.getAmount()).isCloseTo(Math.abs(pvbp), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_delta_at_expiry() {
    CurrencyAmount pvDeltaRec =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_REC_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvDeltaRec.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
    CurrencyAmount pvDeltaPay =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_PAY_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    assertThat(pvDeltaPay.getAmount()).isCloseTo(Math.abs(pvbp), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_delta_after_expiry() {
    CurrencyAmount pvDelta =
        PRICER_SWAPTION_NORMAL.presentValueDelta(SWAPTION_PAST, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvDelta.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_gamma_formula() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, MULTI_USD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    double volatility = NORMAL_VOLS_USD_STD.volatility(SWAPTION_LONG_REC.getExpiry(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    double expiry = NORMAL_VOLS_USD_STD.relativeTime(SWAPTION_LONG_REC.getExpiry());
    EuropeanVanillaOption option = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    double pvGammaExpected = NORMAL.getGamma(option, normalData);
    CurrencyAmount pvGammaComputed =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvGammaComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvGammaComputed.getAmount()).isCloseTo(pvGammaExpected, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_gamma_long_short_parity() {
    CurrencyAmount pvGammaLong =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvGammaShort =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvGammaLong.getAmount()).isCloseTo(-pvGammaShort.getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_gamma_payer_receiver_parity() {
    CurrencyAmount pvGammaLongPay =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvGammaShortRec =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvGammaLongPay.getAmount() + pvGammaShortRec.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_gamma_at_expiry() {
    CurrencyAmount pvGammaRec =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_REC_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvGammaRec.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
    CurrencyAmount pvGammaPay =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_PAY_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvGammaPay.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_gamma_after_expiry() {
    CurrencyAmount pvGamma =
        PRICER_SWAPTION_NORMAL.presentValueGamma(SWAPTION_PAST, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvGamma.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_theta_formula() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, MULTI_USD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    double volatility = NORMAL_VOLS_USD_STD.volatility(SWAPTION_LONG_REC.getExpiry(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    double expiry = NORMAL_VOLS_USD_STD.relativeTime(SWAPTION_LONG_REC.getExpiry());
    EuropeanVanillaOption option = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    double pvThetaExpected = NORMAL.getTheta(option, normalData);
    CurrencyAmount pvThetaComputed =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvThetaComputed.getCurrency()).isEqualTo(USD);
    assertThat(pvThetaComputed.getAmount()).isCloseTo(pvThetaExpected, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_theta_long_short_parity() {
    CurrencyAmount pvThetaLong =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvThetaShort =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvThetaLong.getAmount()).isCloseTo(-pvThetaShort.getAmount(), offset(TOLERANCE_PV));
  }

  @Test
  void present_value_theta_payer_receiver_parity() {
    CurrencyAmount pvThetaLongPay =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    CurrencyAmount pvThetaShortRec =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvThetaLongPay.getAmount() + pvThetaShortRec.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_theta_at_expiry() {
    CurrencyAmount pvThetaRec =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_REC_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvThetaRec.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
    CurrencyAmount pvThetaPay =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_PAY_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvThetaPay.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_theta_after_expiry() {
    CurrencyAmount pvTheta =
        PRICER_SWAPTION_NORMAL.presentValueTheta(SWAPTION_PAST, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvTheta.getAmount()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------  
  @Test
  void currency_exposure() {
    CurrencyAmount pv =
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    MultiCurrencyAmount ce =
        PRICER_SWAPTION_NORMAL.currencyExposure(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pv.getAmount()).isCloseTo(ce.getAmount(USD).getAmount(), offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_sensitivity_FD() {
    PointSensitivities pvpt = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityRatesStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_FLAT).build();
    CurrencyParameterSensitivities pvpsAd = MULTI_USD.parameterSensitivity(pvpt);
    CurrencyParameterSensitivities pvpsFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(MULTI_USD,
        (p) -> PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, p, NORMAL_VOLS_USD_FLAT));
    assertThat(pvpsAd.equalWithTolerance(pvpsFd, TOLERANCE_PV_DELTA)).isTrue();
  }

  @Test
  void present_value_sensitivity_long_short_parity() {
    PointSensitivities pvptLong = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityRatesStickyStrike(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD).build();
    PointSensitivities pvptShort = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityRatesStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD).build();
    CurrencyParameterSensitivities pvpsLong = MULTI_USD.parameterSensitivity(pvptLong);
    CurrencyParameterSensitivities pvpsShort = MULTI_USD.parameterSensitivity(pvptShort);
    assertThat(pvpsLong.equalWithTolerance(pvpsShort.multipliedBy(-1.0), TOLERANCE_PV_DELTA)).isTrue();
  }

  @Test
  void present_value_sensitivity_payer_receiver_parity() {
    PointSensitivities pvptLongPay = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityRatesStickyStrike(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD).build();
    PointSensitivities pvptShortRec = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityRatesStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD).build();
    PointSensitivities pvptSwapRec = PRICER_SWAP.presentValueSensitivity(RSWAP_PAY, MULTI_USD).build();
    CurrencyParameterSensitivities pvpsLongPay = MULTI_USD.parameterSensitivity(pvptLongPay);
    CurrencyParameterSensitivities pvpsShortRec = MULTI_USD.parameterSensitivity(pvptShortRec);
    CurrencyParameterSensitivities pvpsSwapRec = MULTI_USD.parameterSensitivity(pvptSwapRec);
    assertThat(pvpsLongPay.combinedWith(pvpsShortRec).equalWithTolerance(pvpsSwapRec, TOLERANCE_PV_DELTA)).isTrue();
  }

  @Test
  void present_value_sensitivity_at_expiry() {
    PointSensitivities sensiRec = PRICER_SWAPTION_NORMAL.presentValueSensitivityRatesStickyStrike(
        SWAPTION_REC_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD).build();
    for (PointSensitivity sensi : sensiRec.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
    PointSensitivities sensiPay = PRICER_SWAPTION_NORMAL.presentValueSensitivityRatesStickyStrike(
        SWAPTION_PAY_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD).build();
    PointSensitivities sensiPaySwap = PRICER_SWAP.presentValueSensitivity(RSWAP_PAY, MULTI_USD).build();
    assertThat(MULTI_USD.parameterSensitivity(sensiPay).equalWithTolerance(
        MULTI_USD.parameterSensitivity(sensiPaySwap), TOLERANCE_PV)).isTrue();
  }

  @Test
  void present_value_sensitivity_after_expiry() {
    PointSensitivityBuilder pvpts = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityRatesStickyStrike(SWAPTION_PAST, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvpts).isEqualTo(PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_sensitivityNormalVolatility_FD() {
    double shiftVol = 1.0E-4;
    CurrencyAmount pvP = PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(shiftVol));
    CurrencyAmount pvM = PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(-shiftVol));
    double pvnvsFd = (pvP.getAmount() - pvM.getAmount()) / (2 * shiftVol);
    SwaptionSensitivity pvnvsAd = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityModelParamsVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvnvsAd.getCurrency()).isEqualTo(USD);
    assertThat(pvnvsAd.getSensitivity()).isCloseTo(pvnvsFd, offset(TOLERANCE_PV_VEGA));
    assertThat(pvnvsAd.getVolatilitiesName()).isEqualTo(NORMAL_VOLS_USD_STD.getName());
    assertThat(pvnvsAd.getExpiry()).isEqualTo(NORMAL_VOLS_USD_STD.relativeTime(SWAPTION_LONG_PAY.getExpiry()));
    assertThat(pvnvsAd.getTenor()).isCloseTo(SWAP_TENOR_YEAR, offset(TOLERANCE_RATE));
    assertThat(pvnvsAd.getStrike()).isCloseTo(STRIKE, offset(TOLERANCE_RATE));
    double forward = PRICER_SWAP.parRate(RSWAP_REC, MULTI_USD);
    assertThat(pvnvsAd.getForward()).isCloseTo(forward, offset(TOLERANCE_RATE));
  }

  @Test
  void present_value_sensitivityNormalVolatility_long_short_parity() {
    SwaptionSensitivity pvptLongPay = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityModelParamsVolatility(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    SwaptionSensitivity pvptShortRec = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityModelParamsVolatility(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvptLongPay.getSensitivity()).isCloseTo(-pvptShortRec.getSensitivity(), offset(TOLERANCE_PV_VEGA));
  }

  @Test
  void present_value_sensitivityNormalVolatility_payer_receiver_parity() {
    SwaptionSensitivity pvptLongPay = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityModelParamsVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOLS_USD_STD);
    SwaptionSensitivity pvptShortRec = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityModelParamsVolatility(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(pvptLongPay.getSensitivity() + pvptShortRec.getSensitivity()).isCloseTo(0, offset(TOLERANCE_PV_VEGA));
  }

  @Test
  void present_value_sensitivityBlackVolatility_at_expiry() {
    SwaptionSensitivity sensiRec = PRICER_SWAPTION_NORMAL.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(sensiRec.getSensitivity()).isCloseTo(0d, offset(TOLERANCE_PV));
    SwaptionSensitivity sensiPay = PRICER_SWAPTION_NORMAL.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_AT_EXPIRY, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(sensiPay.getSensitivity()).isCloseTo(0d, offset(TOLERANCE_PV));
  }

  @Test
  void present_value_sensitivityNormalVolatility_after_expiry() {
    SwaptionSensitivity v = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityModelParamsVolatility(SWAPTION_PAST, MULTI_USD, NORMAL_VOLS_USD_STD);
    assertThat(v.getSensitivity()).isCloseTo(0.0d, offset(TOLERANCE_PV_VEGA));
  }

}
