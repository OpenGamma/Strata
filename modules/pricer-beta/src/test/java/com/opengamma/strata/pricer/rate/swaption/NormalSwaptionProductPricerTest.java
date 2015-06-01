/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swaption.Swaption;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.provider.NormalVolatilityExpiryTenorSwaptionProvider;
import com.opengamma.strata.pricer.provider.NormalVolatilitySwaptionProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.sensitivity.SwaptionSensitivity;

/**
 * Tests {@link NormalSwaptionProductPricerBeta}.
 */
public class NormalSwaptionProductPricerTest {
  
  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 4, 30);
  
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, USNY);  

  private static final LocalDate SWAPTION_EXERCISE_DATE = LocalDate.of(2020, 4, 30);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SWAP_EFFECTIVE_DATE = USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final Period SWAP_TENOR = Period.ofYears(SWAP_TENOR_YEAR);
  private static final LocalDate SWAP_MATURITY_DATE = SWAP_EFFECTIVE_DATE.plus(SWAP_TENOR);
  private static final double STRIKE = 0.01;
  static final NotionalSchedule NOTIONAL = NotionalSchedule.of(USD, 100_000_000);
  private static final Swap SWAP_REC = swapUsd(SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, RECEIVE, NOTIONAL, STRIKE);
  private static final Swap SWAP_PAY = swapUsd(SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, PAY, NOTIONAL, STRIKE);
  
  private static final Swaption SWAPTION_LONG_REC = Swaption.builder().cashSettled(false)
      .expiryDate(SWAPTION_EXERCISE_DATE).expiryTime(SWAPTION_EXPIRY_TIME).expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG).underlying(SWAP_REC).build();
  private static final Swaption SWAPTION_SHORT_REC = Swaption.builder().cashSettled(false)
      .expiryDate(SWAPTION_EXERCISE_DATE).expiryTime(SWAPTION_EXPIRY_TIME).expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT).underlying(SWAP_REC).build();
  private static final Swaption SWAPTION_LONG_PAY = Swaption.builder().cashSettled(false)
      .expiryDate(SWAPTION_EXERCISE_DATE).expiryTime(SWAPTION_EXPIRY_TIME).expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG).underlying(SWAP_PAY).build();

  public static final NormalPriceFunction NORMAL = new NormalPriceFunction();

  private static final NormalSwaptionProductPricerBeta PRICER_SWAPTION_NORMAL = NormalSwaptionProductPricerBeta.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final double FD_SHIFT = 0.5E-8;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR = 
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);
  
  private static final ImmutableRatesProvider MULTI_USD = 
      RatesProviderDataSets.MULTI_USD.toBuilder().valuationDate(VALUATION_DATE).build();
  private static final NormalVolatilityExpiryTenorSwaptionProvider NORMAL_VOL_SWAPTION_PROVIDER_USD =
      NormalSwaptionVolatilityDataSets.NORMAL_VOL_SWAPTION_PROVIDER_USD_STD;  
  private static final NormalVolatilitySwaptionProvider NORMAL_VOL_SWAPTION_PROVIDER_USD_FLAT =
      NormalSwaptionVolatilityDataSets.NORMAL_VOL_SWAPTION_PROVIDER_USD_FLAT;  
  
  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_PV_DELTA = 1.0E+2;
  private static final double TOLERANCE_PV_VEGA = 1.0E+4;
  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_implied_volatility() {
    double forward = PRICER_SWAP.parRate(SWAP_REC, MULTI_USD);
    double volExpected = NORMAL_VOL_SWAPTION_PROVIDER_USD.getVolatility(SWAPTION_LONG_REC.getExpiryDateTime(), 
        SWAP_TENOR_YEAR, STRIKE, forward);
    double volComputed = PRICER_SWAPTION_NORMAL
        .impliedVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    assertEquals(volComputed, volExpected, TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void present_value_formula() {
    double forward = PRICER_SWAP.parRate(SWAP_REC, MULTI_USD);
    double pvbp = PRICER_SWAP.getLegPricer().pvbp(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), MULTI_USD);
    double volatility = NORMAL_VOL_SWAPTION_PROVIDER_USD.getVolatility(SWAPTION_LONG_REC.getExpiryDateTime(), 
        SWAP_TENOR_YEAR, STRIKE, forward);
    NormalFunctionData normalData = new NormalFunctionData(forward, Math.abs(pvbp), volatility);
    double expiry = NORMAL_VOL_SWAPTION_PROVIDER_USD.relativeTime(SWAPTION_LONG_REC.getExpiryDateTime());
    EuropeanVanillaOption option = new EuropeanVanillaOption(STRIKE, expiry, false);
    double pvExpected = NORMAL.getPriceFunction(option).evaluate(normalData);
    CurrencyAmount pvComputed = 
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    assertEquals(pvComputed.getCurrency(), USD);
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }
  
  @Test
  public void present_value_long_short_parity() {
    CurrencyAmount pvLong = 
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    CurrencyAmount pvShort = 
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    assertEquals(pvLong.getAmount(), -pvShort.getAmount(), TOLERANCE_PV);
  }
  
  @Test
  public void present_value_payer_receiver_parity() {
    CurrencyAmount pvLongPay = 
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    CurrencyAmount pvShortRec = 
        PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    MultiCurrencyAmount pvSwapPay =
        PRICER_SWAP.presentValue(SWAP_PAY, MULTI_USD);
    assertEquals(pvLongPay.getAmount() + pvShortRec.getAmount(), pvSwapPay.getAmount(USD).getAmount(), TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  @Test
  public void present_value_sensitivity_FD() {
    PointSensitivities pvpt = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD_FLAT).build();
    CurveParameterSensitivities pvpsAd = MULTI_USD.parameterSensitivity(pvpt);
    CurveParameterSensitivities pvpsFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(MULTI_USD,
        (p) -> PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_SHORT_REC, p, NORMAL_VOL_SWAPTION_PROVIDER_USD_FLAT));
    assertTrue(pvpsAd.equalWithTolerance(pvpsFd, TOLERANCE_PV_DELTA));
  }
  
  @Test
  public void present_value_sensitivity_long_short_parity() {
    PointSensitivities pvptLong = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityStickyStrike(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD).build();
    PointSensitivities pvptShort = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD).build();
    CurveParameterSensitivities pvpsLong = MULTI_USD.parameterSensitivity(pvptLong);
    CurveParameterSensitivities pvpsShort = MULTI_USD.parameterSensitivity(pvptShort);
    assertTrue(pvpsLong.equalWithTolerance(pvpsShort.multipliedBy(-1.0), TOLERANCE_PV_DELTA));
  }
  
  @Test
  public void present_value_sensitivity_payer_receiver_parity() {
    PointSensitivities pvptLongPay = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityStickyStrike(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD).build();
    PointSensitivities pvptShortRec = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityStickyStrike(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD).build();
    PointSensitivities pvptSwapRec = PRICER_SWAP.presentValueSensitivity(SWAP_PAY, MULTI_USD).build();
    CurveParameterSensitivities pvpsLongPay = MULTI_USD.parameterSensitivity(pvptLongPay);
    CurveParameterSensitivities pvpsShortRec = MULTI_USD.parameterSensitivity(pvptShortRec);
    CurveParameterSensitivities pvpsSwapRec = MULTI_USD.parameterSensitivity(pvptSwapRec);
    assertTrue(pvpsLongPay.combinedWith(pvpsShortRec).equalWithTolerance(pvpsSwapRec, TOLERANCE_PV_DELTA));
  }
  
  //-------------------------------------------------------------------------
  @Test
  public void present_value_sensitivityNormalVolatility_FD() {
    double shiftVol = 1.0E-4;
    CurrencyAmount pvP = PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD,
        NormalSwaptionVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(shiftVol));
    CurrencyAmount pvM = PRICER_SWAPTION_NORMAL.presentValue(SWAPTION_LONG_PAY, MULTI_USD,
        NormalSwaptionVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(-shiftVol));
    double pvnvsFd = (pvP.getAmount() - pvM.getAmount()) / (2 * shiftVol);
    SwaptionSensitivity pvnvsAd = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityNormalVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    assertEquals(pvnvsAd.getCurrency(), USD);
    assertEquals(pvnvsAd.getSensitivity(), pvnvsFd, TOLERANCE_PV_VEGA);
    assertEquals(pvnvsAd.getIndex(), USD_LIBOR_3M);
    assertEquals(pvnvsAd.getExpiry(), SWAPTION_LONG_PAY.getExpiryDateTime());
    assertEquals(pvnvsAd.getTenor(), SWAP_TENOR_YEAR, TOLERANCE_RATE);
    assertEquals(pvnvsAd.getStrike(), STRIKE, TOLERANCE_RATE);
    double forward = PRICER_SWAP.parRate(SWAP_REC, MULTI_USD);
    assertEquals(pvnvsAd.getForward(), forward, TOLERANCE_RATE);
  }
  
  @Test
  public void present_value_sensitivityNormalVolatility_long_short_parity() {
    SwaptionSensitivity pvptLongPay = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityNormalVolatility(SWAPTION_LONG_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    SwaptionSensitivity pvptShortRec = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityNormalVolatility(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    assertEquals(pvptLongPay.getSensitivity(), - pvptShortRec.getSensitivity(), TOLERANCE_PV_DELTA);    
  }
  
  @Test
  public void present_value_sensitivityNormalVolatility_payer_receiver_parity() {
    SwaptionSensitivity pvptLongPay = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityNormalVolatility(SWAPTION_LONG_PAY, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    SwaptionSensitivity pvptShortRec = PRICER_SWAPTION_NORMAL
        .presentValueSensitivityNormalVolatility(SWAPTION_SHORT_REC, MULTI_USD, NORMAL_VOL_SWAPTION_PROVIDER_USD);
    assertEquals(pvptLongPay.getSensitivity() + pvptShortRec.getSensitivity(), 0, TOLERANCE_PV_DELTA);    
  }
  
  //-------------------------------------------------------------------------
  // swap USD standard conventions- TODO: replace by a template when available
  private static Swap swapUsd(LocalDate start, LocalDate end, PayReceive payReceive, 
      NotionalSchedule notional, double fixedRate) {
    RateCalculationSwapLeg fixedLeg = 
        fixedLeg(start, end, Frequency.P6M, payReceive, notional, fixedRate, StubConvention.SHORT_INITIAL);
    RateCalculationSwapLeg iborLeg = 
        iborLeg(start, end, USD_LIBOR_3M, (payReceive==PAY)?RECEIVE:PAY, notional, StubConvention.SHORT_INITIAL);
    return Swap.of(fixedLeg, iborLeg);
  }
  
  
  // fixed rate leg
  private static RateCalculationSwapLeg fixedLeg(
      LocalDate start, LocalDate end, Frequency frequency,
      PayReceive payReceive, NotionalSchedule notional, double fixedRate, StubConvention stubConvention) {

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(frequency)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(frequency)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(fixedRate))
            .build())
        .build();
  }
  
  // fixed rate leg
  private static RateCalculationSwapLeg iborLeg(
      LocalDate start, LocalDate end, IborIndex index,
      PayReceive payReceive, NotionalSchedule notional, StubConvention stubConvention) {
    Frequency freq = Frequency.of(index.getTenor().getPeriod());
    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(freq)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(freq)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .dayCount(index.getDayCount())
            .index(index)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, index.getFixingCalendar(), BDA_P))
            .build())
        .build();
  }
  
}
