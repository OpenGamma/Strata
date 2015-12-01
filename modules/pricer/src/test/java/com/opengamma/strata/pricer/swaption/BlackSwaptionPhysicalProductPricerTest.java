/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.BuySell.SELL;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.ConstantNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFunctionData;
import com.opengamma.strata.pricer.impl.option.BlackPriceFunction;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.type.IborIborSwapConvention;
import com.opengamma.strata.product.swap.type.ImmutableIborIborSwapConvention;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;

/**
 * Tests {@link BlackSwaptionPhysicalProductPricer}.
 */
@Test
public class BlackSwaptionPhysicalProductPricerTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 8, 7);
  private static final LocalDate SWAPTION_EXERCISE_DATE = VALUATION_DATE.plusYears(5);
  private static final LocalDate SWAPTION_PAST_EXERCISE_DATE = VALUATION_DATE.minusYears(1);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SWAP_EFFECTIVE_DATE = USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final Period SWAP_TENOR = Period.ofYears(SWAP_TENOR_YEAR);
  private static final LocalDate SWAP_MATURITY_DATE = SWAP_EFFECTIVE_DATE.plus(SWAP_TENOR);
  private static final double STRIKE = 0.01;
  private static final double NOTIONAL = 100_000_000;
  private static final Swap SWAP_REC = USD_FIXED_6M_LIBOR_3M
      .toTrade(VALUATION_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, SELL, NOTIONAL, STRIKE).getProduct();
  private static final Swap SWAP_PAY = USD_FIXED_6M_LIBOR_3M
      .toTrade(VALUATION_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, BUY, NOTIONAL, STRIKE).getProduct();
  //Only for ArgChecker, not real convention
  private static final IborIborSwapConvention USD_LIBOR_3M_LIBOR_3M = ImmutableIborIborSwapConvention.of(
      "USD-Swap", USD_FIXED_6M_LIBOR_3M.getFloatingLeg(), USD_FIXED_6M_LIBOR_3M.getFloatingLeg());
  private static final Swap SWAP_BASIS = USD_LIBOR_3M_LIBOR_3M
      .toTrade(VALUATION_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, BUY, NOTIONAL, STRIKE).getProduct();
  private static final Swap SWAP_PAST = USD_FIXED_6M_LIBOR_3M // Only for checks; no actual computation on that swap
      .toTrade(SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE.plusYears(10),
          BUY, NOTIONAL, STRIKE).getProduct();
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SWAP_REC.getStartDate())
      .build();

  private static final Swaption SWAPTION_LONG_REC = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_SHORT_REC = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_LONG_PAY = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_LONG_REC_CASH = Swaption.builder()
      .swaptionSettlement(CASH_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_BASIS = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_BASIS)
      .build();
  private static final Swaption SWAPTION_REC_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(VALUATION_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_PAY_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(VALUATION_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_PAST = Swaption.builder()
      .swaptionSettlement(PHYSICAL_SETTLE)
      .expiryDate(AdjustableDate.of(SWAPTION_PAST_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAST)
      .build();

  private static final BlackPriceFunction BLACK = new BlackPriceFunction();
  private static final BlackSwaptionPhysicalProductPricer PRICER_SWAPTION_BLACK =
      BlackSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final double FD_SHIFT = 0.5E-8;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);

  private static final ImmutableRatesProvider MULTI_USD = RatesProviderDataSets.MULTI_USD.toBuilder()
      .valuationDate(VALUATION_DATE)
      .build();
  private static final BlackVolatilityExpiryTenorSwaptionProvider BLACK_VOL_CST_SWAPTION_PROVIDER_USD =
      SwaptionBlackVolatilityDataSets.BLACK_VOL_CST_SWAPTION_PROVIDER_USD;
  private static final BlackVolatilityExpiryTenorSwaptionProvider BLACK_VOL_SWAPTION_PROVIDER_USD_STD =
      SwaptionBlackVolatilityDataSets.BLACK_VOL_SWAPTION_PROVIDER_USD_STD;

  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_PV_DELTA = 1.0E+2;
  private static final double TOLERANCE_PV_VEGA = 1.0E+4;
  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  public void validate_physical_settlement() {
    assertThrowsIllegalArg(() -> PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_REC_CASH, MULTI_USD,
        BLACK_VOL_SWAPTION_PROVIDER_USD_STD));
  }

  public void validate_swap_fixed_leg() {
    assertThrowsIllegalArg(() -> PRICER_SWAPTION_BLACK.presentValue(SWAPTION_BASIS, MULTI_USD,
        BLACK_VOL_SWAPTION_PROVIDER_USD_STD));
  }

  //-------------------------------------------------------------------------
  public void test_implied_volatility() {
    double forward = PRICER_SWAP.parRate(SWAP_REC, MULTI_USD);
    double volExpected = BLACK_VOL_SWAPTION_PROVIDER_USD_STD.getVolatility(SWAPTION_LONG_REC.getExpiryDateTime(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    double volComputed = PRICER_SWAPTION_BLACK
        .impliedVolatility(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(volComputed, volExpected, TOLERANCE_RATE);
  }

  public void test_implied_volatility_after_expiry() {
    assertThrowsIllegalArg(() -> PRICER_SWAPTION_BLACK.impliedVolatility(SWAPTION_PAST, MULTI_USD,
        BLACK_VOL_SWAPTION_PROVIDER_USD_STD));
  }

  //-------------------------------------------------------------------------
  public void present_value_formula() {
    double forward = PRICER_SWAP.parRate(SWAP_REC, MULTI_USD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    double volatility = BLACK_VOL_SWAPTION_PROVIDER_USD_STD.getVolatility(SWAPTION_LONG_REC.getExpiryDateTime(),
        SWAP_TENOR_YEAR, STRIKE, forward);
    BlackFunctionData blackData = BlackFunctionData.of(forward, Math.abs(pvbp), volatility);
    double expiry = BLACK_VOL_SWAPTION_PROVIDER_USD_STD.relativeTime(SWAPTION_LONG_REC.getExpiryDateTime());
    EuropeanVanillaOption option = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    double pvExpected = BLACK.getPriceFunction(option).apply(blackData);
    CurrencyAmount pvComputed =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvComputed.getCurrency(), USD);
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  public void present_value_long_short_parity() {
    CurrencyAmount pvLong =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    CurrencyAmount pvShort =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvLong.getAmount(), -pvShort.getAmount(), TOLERANCE_PV);
  }

  public void present_value_payer_receiver_parity() {
    CurrencyAmount pvLongPay =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    CurrencyAmount pvShortRec =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    MultiCurrencyAmount pvSwapPay =
        PRICER_SWAP.presentValue(SWAP_PAY, MULTI_USD);
    assertEquals(pvLongPay.getAmount() + pvShortRec.getAmount(), pvSwapPay.getAmount(USD).getAmount(), TOLERANCE_PV);
  }

  public void present_value_at_expiry() {
    CurrencyAmount pvRec =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_REC_AT_EXPIRY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvRec.getAmount(), 0.0d, TOLERANCE_PV);
    CurrencyAmount pvPay =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_PAY_AT_EXPIRY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvPay.getAmount(), PRICER_SWAP.presentValue(SWAP_PAY, MULTI_USD).getAmount(USD).getAmount(), TOLERANCE_PV);
  }

  public void present_value_after_expiry() {
    CurrencyAmount pv = PRICER_SWAPTION_BLACK.presentValue(SWAPTION_PAST, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pv.getAmount(), 0.0d, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------  
  public void currency_exposure() {
    CurrencyAmount pv =
        PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    MultiCurrencyAmount ce =
        PRICER_SWAPTION_BLACK.currencyExposure(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pv.getAmount(), ce.getAmount(USD).getAmount(), TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void present_value_sensitivity_FD() {
    PointSensitivities pvpt = PRICER_SWAPTION_BLACK
        .presentValueSensitivityStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_CST_SWAPTION_PROVIDER_USD)
        .build();
    CurveCurrencyParameterSensitivities pvpsAd = MULTI_USD.curveParameterSensitivity(pvpt);
    CurveCurrencyParameterSensitivities pvpsFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(MULTI_USD,
        (p) -> PRICER_SWAPTION_BLACK.presentValue(SWAPTION_SHORT_REC, p, BLACK_VOL_CST_SWAPTION_PROVIDER_USD));
    assertTrue(pvpsAd.equalWithTolerance(pvpsFd, TOLERANCE_PV_DELTA));
  }

  public void present_value_sensitivity_long_short_parity() {
    PointSensitivities pvptLong = PRICER_SWAPTION_BLACK
        .presentValueSensitivityStickyStrike(SWAPTION_LONG_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD).build();
    PointSensitivities pvptShort = PRICER_SWAPTION_BLACK
        .presentValueSensitivityStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD)
        .build();
    CurveCurrencyParameterSensitivities pvpsLong = MULTI_USD.curveParameterSensitivity(pvptLong);
    CurveCurrencyParameterSensitivities pvpsShort = MULTI_USD.curveParameterSensitivity(pvptShort);
    assertTrue(pvpsLong.equalWithTolerance(pvpsShort.multipliedBy(-1.0), TOLERANCE_PV_DELTA));
  }

  public void present_value_sensitivity_payer_receiver_parity() {
    PointSensitivities pvptLongPay = PRICER_SWAPTION_BLACK
        .presentValueSensitivityStickyStrike(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD).build();
    PointSensitivities pvptShortRec = PRICER_SWAPTION_BLACK
        .presentValueSensitivityStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD)
        .build();
    PointSensitivities pvptSwapRec = PRICER_SWAP.presentValueSensitivity(SWAP_PAY, MULTI_USD).build();
    CurveCurrencyParameterSensitivities pvpsLongPay = MULTI_USD.curveParameterSensitivity(pvptLongPay);
    CurveCurrencyParameterSensitivities pvpsShortRec = MULTI_USD.curveParameterSensitivity(pvptShortRec);
    CurveCurrencyParameterSensitivities pvpsSwapRec = MULTI_USD.curveParameterSensitivity(pvptSwapRec);
    assertTrue(pvpsLongPay.combinedWith(pvpsShortRec).equalWithTolerance(pvpsSwapRec, TOLERANCE_PV_DELTA));
  }

  public void present_value_sensitivity_at_expiry() {
    PointSensitivities sensiRec = PRICER_SWAPTION_BLACK.presentValueSensitivityStickyStrike(
        SWAPTION_REC_AT_EXPIRY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD).build();
    for (PointSensitivity sensi : sensiRec.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
    PointSensitivities sensiPay = PRICER_SWAPTION_BLACK.presentValueSensitivityStickyStrike(
        SWAPTION_PAY_AT_EXPIRY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD).build();
    PointSensitivities sensiPaySwap = PRICER_SWAP.presentValueSensitivity(SWAP_PAY, MULTI_USD).build();
    assertTrue(MULTI_USD.curveParameterSensitivity(sensiPay).equalWithTolerance(
        MULTI_USD.curveParameterSensitivity(sensiPaySwap), TOLERANCE_PV));
  }

  public void present_value_sensitivity_after_expiry() {
    PointSensitivityBuilder pvpts = PRICER_SWAPTION_BLACK
        .presentValueSensitivityStickyStrike(SWAPTION_PAST, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvpts, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void present_value_sensitivityBlackVolatility_FD() {
    double shiftVol = 1.0E-4;
    NodalSurface surfaceUp = ConstantNodalSurface.of(
        SwaptionBlackVolatilityDataSets.META_DATA, SwaptionBlackVolatilityDataSets.VOLATILITY + shiftVol);
    NodalSurface surfaceDw = ConstantNodalSurface.of(
        SwaptionBlackVolatilityDataSets.META_DATA, SwaptionBlackVolatilityDataSets.VOLATILITY - shiftVol);
    CurrencyAmount pvP = PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_PAY, MULTI_USD,
        BlackVolatilityExpiryTenorSwaptionProvider.of(surfaceUp, USD_FIXED_6M_LIBOR_3M, ACT_365F, VALUATION_DATE));
    CurrencyAmount pvM = PRICER_SWAPTION_BLACK.presentValue(SWAPTION_LONG_PAY, MULTI_USD,
        BlackVolatilityExpiryTenorSwaptionProvider.of(surfaceDw, USD_FIXED_6M_LIBOR_3M, ACT_365F, VALUATION_DATE));
    double pvnvsFd = (pvP.getAmount() - pvM.getAmount()) / (2 * shiftVol);
    SwaptionSensitivity pvnvsAd = PRICER_SWAPTION_BLACK
        .presentValueSensitivityBlackVolatility(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_CST_SWAPTION_PROVIDER_USD);
    assertEquals(pvnvsAd.getCurrency(), USD);
    assertEquals(pvnvsAd.getSensitivity(), pvnvsFd, TOLERANCE_PV_VEGA);
    assertEquals(pvnvsAd.getConvention(), USD_FIXED_6M_LIBOR_3M);
    assertEquals(pvnvsAd.getExpiry(), SWAPTION_LONG_PAY.getExpiryDateTime());
    assertEquals(pvnvsAd.getTenor(), SWAP_TENOR_YEAR, TOLERANCE_RATE);
    assertEquals(pvnvsAd.getStrike(), STRIKE, TOLERANCE_RATE);
    double forward = PRICER_SWAP.parRate(SWAP_REC, MULTI_USD);
    assertEquals(pvnvsAd.getForward(), forward, TOLERANCE_RATE);
  }

  public void present_value_sensitivityBlackVolatility_long_short_parity() {
    SwaptionSensitivity pvptLongPay = PRICER_SWAPTION_BLACK
        .presentValueSensitivityBlackVolatility(SWAPTION_LONG_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    SwaptionSensitivity pvptShortRec = PRICER_SWAPTION_BLACK
        .presentValueSensitivityBlackVolatility(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvptLongPay.getSensitivity(), -pvptShortRec.getSensitivity(), TOLERANCE_PV_VEGA);
  }

  public void present_value_sensitivityBlackVolatility_payer_receiver_parity() {
    SwaptionSensitivity pvptLongPay = PRICER_SWAPTION_BLACK
        .presentValueSensitivityBlackVolatility(SWAPTION_LONG_PAY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    SwaptionSensitivity pvptShortRec = PRICER_SWAPTION_BLACK
        .presentValueSensitivityBlackVolatility(SWAPTION_SHORT_REC, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(pvptLongPay.getSensitivity() + pvptShortRec.getSensitivity(), 0, TOLERANCE_PV_VEGA);
  }

  public void present_value_sensitivityBlackVolatility_at_expiry() {
    SwaptionSensitivity sensiRec = PRICER_SWAPTION_BLACK.presentValueSensitivityBlackVolatility(
        SWAPTION_REC_AT_EXPIRY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(sensiRec.getSensitivity(), 0d, TOLERANCE_PV);
    SwaptionSensitivity sensiPay = PRICER_SWAPTION_BLACK.presentValueSensitivityBlackVolatility(
        SWAPTION_PAY_AT_EXPIRY, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(sensiPay.getSensitivity(), 0d, TOLERANCE_PV);
  }

  public void present_value_sensitivityBlackVolatility_after_expiry() {
    SwaptionSensitivity v = PRICER_SWAPTION_BLACK
        .presentValueSensitivityBlackVolatility(SWAPTION_PAST, MULTI_USD, BLACK_VOL_SWAPTION_PROVIDER_USD_STD);
    assertEquals(v.getSensitivity(), 0.0d, TOLERANCE_PV_VEGA);
  }

}
