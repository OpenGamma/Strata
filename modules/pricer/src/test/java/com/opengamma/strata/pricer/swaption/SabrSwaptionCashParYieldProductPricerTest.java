/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link SabrSwaptionCashParYieldProductPricer}.
 */
@Test
public class SabrSwaptionCashParYieldProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ZonedDateTime VAL_DATE_TIME = dateUtc(2008, 8, 18);

  private static final ZonedDateTime MATURITY = dateUtc(2014, 3, 18);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final LocalDate SETTLE =
      BDA_MF.adjust(CALENDAR.resolve(REF_DATA).shift(MATURITY.toLocalDate(), 2), REF_DATA);
  private static final double NOTIONAL = 100000000; //100m
  private static final int TENOR_YEAR = 5;
  private static final LocalDate END = SETTLE.plusYears(TENOR_YEAR);
  private static final double RATE = 0.0175;
  private static final PeriodicSchedule PERIOD_FIXED = PeriodicSchedule.builder()
      .startDate(SETTLE)
      .endDate(END)
      .frequency(P6M)
      .businessDayAdjustment(BDA_MF)
      .stubConvention(StubConvention.SHORT_FINAL)
      .rollConvention(RollConventions.EOM)
      .build();
  private static final PaymentSchedule PAYMENT_FIXED = PaymentSchedule.builder()
      .paymentFrequency(P6M)
      .paymentDateOffset(DaysAdjustment.NONE)
      .build();
  private static final FixedRateCalculation RATE_FIXED = FixedRateCalculation.builder()
      .dayCount(THIRTY_U_360)
      .rate(ValueSchedule.of(RATE))
      .build();
  private static final PeriodicSchedule PERIOD_IBOR = PeriodicSchedule.builder()
      .startDate(SETTLE)
      .endDate(END)
      .frequency(P6M)
      .businessDayAdjustment(BDA_MF)
      .stubConvention(StubConvention.SHORT_FINAL)
      .rollConvention(RollConventions.EOM)
      .build();
  private static final PaymentSchedule PAYMENT_IBOR = PaymentSchedule.builder()
      .paymentFrequency(P6M)
      .paymentDateOffset(DaysAdjustment.NONE)
      .build();
  private static final IborRateCalculation RATE_IBOR = IborRateCalculation.builder()
      .index(EUR_EURIBOR_6M)
      .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CALENDAR, BDA_MF))
      .build();
  private static final SwapLeg FIXED_LEG_REC = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
      .accrualSchedule(PERIOD_FIXED)
      .paymentSchedule(PAYMENT_FIXED)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_FIXED)
      .build();
  private static final SwapLeg FIXED_LEG_PAY = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(PERIOD_FIXED)
      .paymentSchedule(PAYMENT_FIXED)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_FIXED)
      .build();
  private static final SwapLeg IBOR_LEG_REC = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
      .accrualSchedule(PERIOD_IBOR)
      .paymentSchedule(PAYMENT_IBOR)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_IBOR)
      .build();
  private static final SwapLeg IBOR_LEG_PAY = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(PERIOD_IBOR)
      .paymentSchedule(PAYMENT_IBOR)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_IBOR)
      .build();
  private static final Swap SWAP_REC = Swap.of(FIXED_LEG_REC, IBOR_LEG_PAY);
  private static final ResolvedSwap RSWAP_REC = SWAP_REC.resolve(REF_DATA);
  private static final Swap SWAP_PAY = Swap.of(FIXED_LEG_PAY, IBOR_LEG_REC);
  private static final ResolvedSwapLeg RFIXED_LEG_REC = FIXED_LEG_REC.resolve(REF_DATA);
  private static final CashSwaptionSettlement PAR_YIELD =
      CashSwaptionSettlement.of(SETTLE, CashSwaptionSettlementMethod.PAR_YIELD);
  private static final ResolvedSwaption SWAPTION_REC_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PAR_YIELD)
      .longShort(LONG)
      .underlying(SWAP_REC)
      .build().
      resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PAR_YIELD)
      .longShort(SHORT)
      .underlying(SWAP_REC)
      .build().
      resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PAR_YIELD)
      .longShort(LONG)
      .underlying(SWAP_PAY)
      .build().
      resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PAR_YIELD)
      .longShort(SHORT)
      .underlying(SWAP_PAY)
      .build().
      resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PHYS = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate()))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .underlying(SWAP_REC)
      .build().
      resolve(REF_DATA);

  private static final SabrParametersSwaptionVolatilities VOLS_REG =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VAL_DATE_TIME.toLocalDate(), false);
  private static final SabrParametersSwaptionVolatilities VOLS =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VAL_DATE_TIME.toLocalDate(), true);
  private static final SabrParametersSwaptionVolatilities VOLS_AT_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(MATURITY.toLocalDate(), true);
  private static final SabrParametersSwaptionVolatilities VOLS_AFTER_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(MATURITY.toLocalDate().plusDays(1), true);
  private static final ImmutableRatesProvider RATE_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VAL_DATE_TIME.toLocalDate());
  private static final ImmutableRatesProvider RATE_PROVIDER_AT_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(MATURITY.toLocalDate());
  private static final ImmutableRatesProvider RATE_PROVIDER_AFTER_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(MATURITY.toLocalDate().plusDays(1));

  private static final double TOL = 1.0e-13;
  private static final double TOLERANCE_DELTA = 1.0E-2;
  private static final double FD_EPS = 1.0e-6;
  private static final SabrSwaptionCashParYieldProductPricer PRICER = SabrSwaptionCashParYieldProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  public void validate_cash_settlement() {
    assertThrowsIllegalArg(() -> PRICER.presentValue(SWAPTION_PHYS, RATE_PROVIDER, VOLS));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computedRec = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPay = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(MATURITY);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), TENOR_YEAR, RATE, forward);
    double df = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    double expectedRec = df * annuityCash * BlackFormulaRepository.price(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, expiry, volatility, false);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.price(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, expiry, volatility, true);
    assertEquals(computedRec.getCurrency(), EUR);
    assertEquals(computedRec.getAmount(), expectedRec, NOTIONAL * TOL);
    assertEquals(computedPay.getCurrency(), EUR);
    assertEquals(computedPay.getAmount(), expectedPay, NOTIONAL * TOL);
  }

  public void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double df = RATE_PROVIDER_AT_MATURITY.discountFactor(EUR, SETTLE);
    assertEquals(computedRec.getAmount(), df * annuityCash * (RATE - forward), NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_afterExpiry() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertEquals(computedRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvRecShort = PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayLong = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayShort = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double df = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    double expected = df * annuityCash * (forward - RATE);
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), expected, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -expected, NOTIONAL * TOL);
  }

  public void test_presentValue_parity_atMaturity() {
    CurrencyAmount pvRecLong =
        PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount pvRecShort =
        PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount pvPayLong =
        PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount pvPayShort =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double df = RATE_PROVIDER_AT_MATURITY.discountFactor(EUR, SETTLE);
    double expected = df * annuityCash * (forward - RATE);
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), expected, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -expected, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), expectedRec.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), expectedPay.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), expectedRec.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), expectedPay.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double computedRec = PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    double computedPay = PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double expected = VOLS.volatility(MATURITY, TENOR_YEAR, RATE, forward);
    assertEquals(computedRec, expected, TOL);
    assertEquals(computedPay, expected, TOL);
  }

  public void test_impliedVolatility_atMaturity() {
    double computedRec =
        PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double computedPay =
        PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double expected = VOLS_AT_MATURITY.volatility(MATURITY, TENOR_YEAR, RATE, forward);
    assertEquals(computedRec, expected, TOL);
    assertEquals(computedPay, expected, TOL);
  }

  public void test_impliedVolatility_afterMaturity() {
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY));
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueDelta_parity() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    ResolvedSwapLeg fixedLeg = SWAPTION_REC_LONG.getUnderlying().getLegs(SwapLegType.FIXED).get(0);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(fixedLeg, forward);
    CashSwaptionSettlement cashSettlement = (CashSwaptionSettlement) SWAPTION_REC_LONG.getSwaptionSettlement();
    double discountSettle = RATE_PROVIDER.discountFactor(fixedLeg.getCurrency(), cashSettlement.getSettlementDate());
    double pvbpCash = Math.abs(annuityCash * discountSettle);
    CurrencyAmount deltaRec = PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount deltaPay = PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertEquals(deltaRec.getAmount() + deltaPay.getAmount(), -pvbpCash, TOLERANCE_DELTA);
  }

  public void test_presentValueDelta_afterMaturity() {
    CurrencyAmount deltaRec =
        PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertEquals(deltaRec.getAmount(), 0, TOLERANCE_DELTA);
    CurrencyAmount deltaPay =
        PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertEquals(deltaPay.getAmount(), 0, TOLERANCE_DELTA);
  }

  public void test_presentValueDelta_atMaturity() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER_AT_MATURITY);
    ResolvedSwapLeg fixedLeg = SWAPTION_REC_LONG.getUnderlying().getLegs(SwapLegType.FIXED).get(0);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(fixedLeg, forward);
    CashSwaptionSettlement cashSettlement = (CashSwaptionSettlement) SWAPTION_REC_LONG.getSwaptionSettlement();
    double discountSettle = RATE_PROVIDER_AT_MATURITY.discountFactor(fixedLeg.getCurrency(), cashSettlement.getSettlementDate());
    double pvbpCash = Math.abs(annuityCash * discountSettle);
    CurrencyAmount deltaRec =
        PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertEquals(deltaRec.getAmount(), RATE > forward ? -pvbpCash : 0, TOLERANCE_DELTA);
    CurrencyAmount deltaPay =
        PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertEquals(deltaPay.getAmount(), RATE > forward ? 0 : pvbpCash, TOLERANCE_DELTA);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), VOLS));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 200d));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay.build());
    CurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_PAY_SHORT, (p), VOLS));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 200d));
  }

  public void test_presentValueSensitivityRatesStickyStrike() {
    SwaptionVolatilities volSabr = SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VAL_DATE_TIME.toLocalDate(), false);
    double impliedVol = PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, volSabr);
    SurfaceMetadata blackMeta =
        Surfaces.blackVolatilityByExpiryTenor("CST", VOLS.getDayCount());
    SwaptionVolatilities volCst = BlackSwaptionExpiryTenorVolatilities.of(
        VOLS.getConvention(), VOLS.getValuationDateTime(), ConstantSurface.of(blackMeta, impliedVol));
    // To obtain a constant volatility surface which create a sticky strike sensitivity
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, volSabr);
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), volCst));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 300d));

    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, volSabr);
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay.build());
    CurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_PAY_SHORT, (p), volCst));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 300d));
  }

  public void test_presentValueSensitivityRatesStickyModel_atMaturity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyParameterSensitivities computedRec =
        RATE_PROVIDER_AT_MATURITY.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATE_PROVIDER_AT_MATURITY, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), VOLS_AT_MATURITY));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d));
    PointSensitivities pointPay = PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT,
        RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivityRatesStickyModel_afterMaturity() {
    PointSensitivities pointRec = PRICER.presentValueSensitivityRatesStickyModel(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
    PointSensitivities pointPay = PRICER.presentValueSensitivityRatesStickyModel(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivityRatesStickyModel_parity() {
    CurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build());
    assertTrue(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL));

    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    PointSensitivityBuilder forwardSensi = PRICER_SWAP.parRateSensitivity(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double annuityCashDeriv = PRICER_SWAP.getLegPricer()
        .annuityCashDerivative(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward).getDerivative(0);
    double discount = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    PointSensitivityBuilder discountSensi = RATE_PROVIDER.discountFactors(EUR).zeroRatePointSensitivity(SETTLE);
    PointSensitivities expecedPoint = discountSensi.multipliedBy(annuityCash * (forward - RATE)).combinedWith(
        forwardSensi.multipliedBy(discount * annuityCash + discount * annuityCashDeriv * (forward - RATE))).build();
    CurrencyParameterSensitivities expected = RATE_PROVIDER.parameterSensitivity(expecedPoint);
    assertTrue(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL));
    assertTrue(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueVega_parity() {
    SwaptionSensitivity vegaRec = PRICER
        .presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity vegaPay = PRICER
        .presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertEquals(vegaRec.getSensitivity(), -vegaPay.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_presentValueVega_atMaturity() {
    SwaptionSensitivity vegaRec = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertEquals(vegaRec.getSensitivity(), 0, TOLERANCE_DELTA);
    SwaptionSensitivity vegaPay = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertEquals(vegaPay.getSensitivity(), 0, TOLERANCE_DELTA);
  }

  public void test_presentValueVega_afterMaturity() {
    SwaptionSensitivity vegaRec = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertEquals(vegaRec.getSensitivity(), 0, TOLERANCE_DELTA);
    SwaptionSensitivity vegaPay = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertEquals(vegaPay.getSensitivity(), 0, TOLERANCE_DELTA);
  }

  public void test_presentValueVega_SwaptionSensitivity() {
    SwaptionSensitivity vegaRec = PRICER
        .presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    assertEquals(VOLS.parameterSensitivity(vegaRec), CurrencyParameterSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivities sensiRec =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build();
    PointSensitivities sensiPay =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build();
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(MATURITY);
    double volatility = VOLS.volatility(SWAPTION_REC_LONG.getExpiry(), TENOR_YEAR, RATE, forward);
    double df = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    double[] volSensi =
        VOLS.getParameters().volatilityAdjoint(expiry, TENOR_YEAR, RATE, forward).getDerivatives().toArray();
    double vegaRec = df * annuityCash * BlackFormulaRepository.vega(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, expiry, volatility);
    double vegaPay = -df * annuityCash * BlackFormulaRepository.vega(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, expiry, volatility);
    assertSensitivity(sensiRec, SabrParameterType.ALPHA, vegaRec * volSensi[2]);
    assertSensitivity(sensiRec, SabrParameterType.BETA, vegaRec * volSensi[3]);
    assertSensitivity(sensiRec, SabrParameterType.RHO, vegaRec * volSensi[4]);
    assertSensitivity(sensiRec, SabrParameterType.NU, vegaRec * volSensi[5]);
    assertSensitivity(sensiPay, SabrParameterType.ALPHA, vegaPay * volSensi[2]);
    assertSensitivity(sensiPay, SabrParameterType.BETA, vegaPay * volSensi[3]);
    assertSensitivity(sensiPay, SabrParameterType.RHO, vegaPay * volSensi[4]);
    assertSensitivity(sensiPay, SabrParameterType.NU, vegaPay * volSensi[5]);
  }

  private void assertSensitivity(PointSensitivities points, SabrParameterType type, double expected) {
    for (PointSensitivity point : points.getSensitivities()) {
      SwaptionSabrSensitivity sens = (SwaptionSabrSensitivity) point;
      assertEquals(sens.getCurrency(), EUR);
      assertEquals(sens.getVolatilitiesName(), VOLS.getName());
      if (sens.getSensitivityType() == type) {
        assertEquals(sens.getSensitivity(), expected, NOTIONAL * TOL);
        return;
      }
    }
    fail("Did not find sensitivity: " + type + " in " + points);
  }

  public void test_presentValueSensitivityModelParamsSabr_atMaturity() {
    PointSensitivities sensiRec = PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    assertSensitivity(sensiRec, SabrParameterType.ALPHA, 0);
    assertSensitivity(sensiRec, SabrParameterType.BETA, 0);
    assertSensitivity(sensiRec, SabrParameterType.RHO, 0);
    assertSensitivity(sensiRec, SabrParameterType.NU, 0);
    PointSensitivities sensiPay = PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    assertSensitivity(sensiPay, SabrParameterType.ALPHA, 0);
    assertSensitivity(sensiPay, SabrParameterType.BETA, 0);
    assertSensitivity(sensiPay, SabrParameterType.RHO, 0);
    assertSensitivity(sensiPay, SabrParameterType.NU, 0);
  }

  public void test_presentValueSensitivityModelParamsSabr_afterMaturity() {
    PointSensitivities sensiRec = PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    assertEquals(sensiRec.getSensitivities().size(), 0);
    PointSensitivities sensiPay = PRICER.presentValueSensitivityModelParamsSabr(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    assertEquals(sensiPay.getSensitivities().size(), 0);
  }

  public void test_presentValueSensitivityModelParamsSabr_parity() {
    PointSensitivities pvSensiRecLong =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build();
    PointSensitivities pvSensiRecShort =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS).build();
    PointSensitivities pvSensiPayLong =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS).build();
    PointSensitivities pvSensiPayShort =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build();

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.ALPHA, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.ALPHA, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.ALPHA, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.ALPHA, 1);

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.BETA, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.BETA, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.BETA, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.BETA, 1);

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.RHO, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.RHO, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.RHO, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.RHO, 1);

    assertSensitivity(pvSensiRecLong, pvSensiRecShort, SabrParameterType.NU, -1);
    assertSensitivity(pvSensiPayLong, pvSensiPayShort, SabrParameterType.NU, -1);
    assertSensitivity(pvSensiRecLong, pvSensiPayLong, SabrParameterType.NU, 1);
    assertSensitivity(pvSensiPayShort, pvSensiPayShort, SabrParameterType.NU, 1);
  }

  private void assertSensitivity(
      PointSensitivities points1,
      PointSensitivities points2,
      SabrParameterType type,
      int factor) {

    // use ordinal() as a hack to find correct type
    assertEquals(
        points1.getSensitivities().get(type.ordinal()).getSensitivity(),
        points2.getSensitivities().get(type.ordinal()).getSensitivity() * factor,
        NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void regressionPresentValue() {
    CurrencyAmount pvLongPay = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS_REG);
    CurrencyAmount pvShortPay = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS_REG);
    CurrencyAmount pvLongRec = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS_REG);
    CurrencyAmount pvShortRec = PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS_REG);
    assertEquals(pvLongPay.getAmount(), 2419978.690066857, NOTIONAL * TOL);
    assertEquals(pvShortPay.getAmount(), -2419978.690066857, NOTIONAL * TOL);
    assertEquals(pvLongRec.getAmount(), 3498144.2628540806, NOTIONAL * TOL);
    assertEquals(pvShortRec.getAmount(), -3498144.2628540806, NOTIONAL * TOL);
  }

  public void regressionCurveSensitivity() {
    double[] sensiDscExp = new double[] {0.0, 0.0, 0.0, 0.0, -1.1942174487944763E7, -1565567.6976298545};
    double[] sensiFwdExp = new double[] {0.0, 0.0, 0.0, 0.0, -2.3978768078237808E8, 4.8392987803482056E8};
    PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyModel(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS_REG);
    CurrencyParameterSensitivities sensi = RATE_PROVIDER.parameterSensitivity(point.build());
    double[] sensiDscCmp = sensi.getSensitivity(SwaptionSabrRateVolatilityDataSet.META_DSC_EUR.getCurveName(), EUR)
        .getSensitivity().toArray();
    double[] sensiFwdCmp = sensi.getSensitivity(SwaptionSabrRateVolatilityDataSet.META_FWD_EUR.getCurveName(), EUR)
        .getSensitivity().toArray();
    assertTrue(DoubleArrayMath.fuzzyEquals(sensiDscCmp, sensiDscExp, TOL * NOTIONAL));
    assertTrue(DoubleArrayMath.fuzzyEquals(sensiFwdCmp, sensiFwdExp, TOL * NOTIONAL));
  }

  public void regressionSurfaceSensitivity() {
    PointSensitivities pointComputed =
        PRICER.presentValueSensitivityModelParamsSabr(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS_REG).build();
    assertSensitivity(pointComputed, SabrParameterType.ALPHA, 4.862767907309804E7);
    assertSensitivity(pointComputed, SabrParameterType.BETA, -1.1095143998998241E7);
    assertSensitivity(pointComputed, SabrParameterType.RHO, 575158.6667143379);
    assertSensitivity(pointComputed, SabrParameterType.NU, 790627.3506603877);

    CurrencyParameterSensitivities sensiComputed =
        VOLS_REG.parameterSensitivity(pointComputed);
    double[][] alphaExp = new double[][] {
        {0.0, 0.0, 0.0}, {0.5, 0.0, 0.0}, {1.0, 0.0, 0.0}, {2.0, 0.0, 0.0}, {5.0, 0.0, 0.0}, {10.0, 0.0, 0.0},
        {0.0, 1.0, 0.0}, {0.5, 1.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0}, {5.0, 1.0, 2.3882653164816026E7},
        {10.0, 1.0, 3132724.0980162215}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {1.0, 10.0, 0.0}, {2.0, 10.0, 0.0},
        {5.0, 10.0, 1.910612253185282E7}, {10.0, 10.0, 2506179.2784129772}, {0.0, 100.0, 0.0}, {0.5, 100.0, 0.0},
        {1.0, 100.0, 0.0}, {2.0, 100.0, 0.0}, {5.0, 100.0, 0.0}, {10.0, 100.0, 0.0}};
    double[][] betaExp = new double[][] {
        {0.0, 0.0, -0.0}, {0.5, 0.0, -0.0}, {1.0, 0.0, -0.0}, {2.0, 0.0, -0.0}, {5.0, 0.0, -0.0},
        {10.0, 0.0, -0.0}, {100.0, 0.0, -0.0}, {0.0, 1.0, -0.0}, {0.5, 1.0, -0.0}, {1.0, 1.0, -0.0},
        {2.0, 1.0, -0.0}, {5.0, 1.0, -5449190.275839399}, {10.0, 1.0, -714778.6124929579}, {100.0, 1.0, -0.0},
        {0.0, 10.0, -0.0}, {0.5, 10.0, -0.0}, {1.0, 10.0, -0.0}, {2.0, 10.0, -0.0}, {5.0, 10.0, -4359352.220671519},
        {10.0, 10.0, -571822.8899943662}, {100.0, 10.0, -0.0}, {0.0, 100.0, -0.0}, {0.5, 100.0, -0.0},
        {1.0, 100.0, -0.0}, {2.0, 100.0, -0.0}, {5.0, 100.0, -0.0}, {10.0, 100.0, -0.0}, {100.0, 100.0, -0.0}};
    double[][] rhoExp = new double[][] {
        {0.0, 0.0, 0.0}, {0.5, 0.0, 0.0}, {1.0, 0.0, 0.0}, {2.0, 0.0, 0.0}, {5.0, 0.0, 0.0}, {10.0, 0.0, 0.0},
        {100.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.5, 1.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0},
        {5.0, 1.0, 282479.3453791586}, {10.0, 1.0, 37053.24723991797}, {100.0, 1.0, 0.0}, {0.0, 10.0, 0.0},
        {1.0, 10.0, 0.0}, {2.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {5.0, 10.0, 225983.4763033269},
        {10.0, 10.0, 29642.597791934375}, {100.0, 10.0, 0.0}, {0.0, 100.0, 0.0}, {0.5, 100.0, 0.0},
        {1.0, 100.0, 0.0}, {2.0, 100.0, 0.0}, {5.0, 100.0, 0.0}, {10.0, 100.0, 0.0}, {100.0, 100.0, 0.0}};
    double[][] nuExp = new double[][] {
        {0.0, 0.0, 0.0}, {0.5, 0.0, 0.0}, {1.0, 0.0, 0.0}, {2.0, 0.0, 0.0}, {5.0, 0.0, 0.0}, {10.0, 0.0, 0.0},
        {100.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.5, 1.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0},
        {5.0, 1.0, 388303.1055225815}, {10.0, 1.0, 50934.31151096723}, {100.0, 1.0, 0.0}, {0.0, 10.0, 0.0},
        {0.5, 10.0, 0.0}, {1.0, 10.0, 0.0}, {2.0, 10.0, 0.0}, {5.0, 10.0, 310642.48441806517},
        {10.0, 10.0, 40747.44920877378}, {100.0, 10.0, 0.0}, {0.0, 100.0, 0.0}, {0.5, 100.0, 0.0},
        {1.0, 100.0, 0.0}, {2.0, 100.0, 0.0}, {5.0, 100.0, 0.0}, {10.0, 100.0, 0.0}, {100.0, 100.0, 0.0}};
    double[][][] exps = new double[][][] {alphaExp, betaExp, rhoExp, nuExp};
    SurfaceMetadata[] metadata = new SurfaceMetadata[] {SwaptionSabrRateVolatilityDataSet.META_ALPHA,
        SwaptionSabrRateVolatilityDataSet.META_BETA_EUR, SwaptionSabrRateVolatilityDataSet.META_RHO,
        SwaptionSabrRateVolatilityDataSet.META_NU};
    // x-y-value order does not match sorted order in surface, thus sort it
    CurrencyParameterSensitivities sensiExpected = CurrencyParameterSensitivities.empty();
    for (int i = 0; i < exps.length; ++i) {
      int size = exps[i].length;
      Map<DoublesPair, Double> sensiMap = new TreeMap<>();
      for (int j = 0; j < size; ++j) {
        sensiMap.put(DoublesPair.of(exps[i][j][0], exps[i][j][1]), exps[i][j][2]);
      }
      List<ParameterMetadata> paramMetadata = new ArrayList<>(size);
      List<Double> sensi = new ArrayList<>();
      for (Entry<DoublesPair, Double> entry : sensiMap.entrySet()) {
        paramMetadata.add(SwaptionSurfaceExpiryTenorParameterMetadata.of(
            entry.getKey().getFirst(), entry.getKey().getSecond()));
        sensi.add(entry.getValue());
      }
      SurfaceMetadata surfaceMetadata = metadata[i].withParameterMetadata(paramMetadata);
      sensiExpected = sensiExpected.combinedWith(
          CurrencyParameterSensitivity.of(
              surfaceMetadata.getSurfaceName(),
              surfaceMetadata.getParameterMetadata().get(),
              EUR,
              DoubleArray.copyOf(sensi)));
    }
    testSurfaceParameterSensitivities(sensiComputed, sensiExpected, TOL * NOTIONAL);
  }

  //-------------------------------------------------------------------------
  private void testSurfaceParameterSensitivities(
      CurrencyParameterSensitivities computed,
      CurrencyParameterSensitivities expected,
      double tol) {
    List<CurrencyParameterSensitivity> listComputed = new ArrayList<>(computed.getSensitivities());
    List<CurrencyParameterSensitivity> listExpected = new ArrayList<>(expected.getSensitivities());
    for (CurrencyParameterSensitivity sensExpected : listExpected) {
      int index = Math.abs(Collections.binarySearch(listComputed, sensExpected,
          CurrencyParameterSensitivity::compareKey));
      CurrencyParameterSensitivity sensComputed = listComputed.get(index);
      int nSens = sensExpected.getParameterCount();
      assertEquals(sensComputed.getParameterCount(), nSens);
      for (int i = 0; i < nSens; ++i) {
        assertEquals(sensComputed.getSensitivity().get(i), sensExpected.getSensitivity().get(i), tol);
      }
      listComputed.remove(index);
    }
  }

}
