/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.rate.swap.CompoundingMethod.STRAIGHT;
import static com.opengamma.strata.product.rate.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.rate.swap.SwapLegType.IBOR;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.product.rate.InflationMonthlyRateObservation;

/**
 * Test.
 */
@Test
public class RateCalculationSwapLegTest {

  private static final LocalDate DATE_01_02 = date(2014, 1, 2);
  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_02_03 = date(2014, 2, 3);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_02_07 = date(2014, 2, 7);
  private static final LocalDate DATE_03_03 = date(2014, 3, 3);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_03_07 = date(2014, 3, 7);
  private static final LocalDate DATE_04_03 = date(2014, 4, 3);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);
  private static final LocalDate DATE_04_09 = date(2014, 4, 9);
  private static final LocalDate DATE_05_01 = date(2014, 5, 1);
  private static final LocalDate DATE_05_05 = date(2014, 5, 5);
  private static final LocalDate DATE_05_06 = date(2014, 5, 6);
  private static final LocalDate DATE_05_08 = date(2014, 5, 8);
  private static final LocalDate DATE_06_05 = date(2014, 6, 5);
  private static final LocalDate DATE_06_09 = date(2014, 6, 9);
  private static final LocalDate DATE_14_06_09 = date(2014, 6, 9);
  private static final LocalDate DATE_19_06_09 = date(2019, 6, 9);
  private static final DaysAdjustment PLUS_THREE_DAYS = DaysAdjustment.ofBusinessDays(3, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_01_05)
        .endDate(DATE_04_05)
        .frequency(P1M)
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(P1M)
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    FixedRateCalculation rateCalc = FixedRateCalculation.builder()
        .dayCount(DayCounts.ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    assertEquals(test.getStartDate(), DATE_01_06);
    assertEquals(test.getEndDate(), DATE_04_07);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPayReceive(), PAY);
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getNotionalSchedule(), notionalSchedule);
    assertEquals(test.getCalculation(), rateCalc);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices_simple() {
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(IborRateCalculation.builder()
            .dayCount(DayCounts.ACT_365F)
            .index(GBP_LIBOR_3M)
            .fixingDateOffset(MINUS_TWO_DAYS)
            .build())
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  public void test_collectIndices_fxReset() {
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(GBP)
            .amount(ValueSchedule.of(1000d))
            .fxReset(FxResetCalculation.builder()
                .referenceCurrency(EUR)
                .index(EUR_GBP_ECB)
                .fixingDateOffset(MINUS_TWO_DAYS)
                .build())
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(DayCounts.ACT_365F)
            .index(GBP_LIBOR_3M)
            .fixingDateOffset(MINUS_TWO_DAYS)
            .build())
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M, EUR_GBP_ECB));
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M, EUR_GBP_ECB));
  }

  //-------------------------------------------------------------------------
  public void test_expand_oneAccrualPerPayment_fixedRate() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_02_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_01_06)
            .endDate(DATE_02_05)
            .unadjustedStartDate(DATE_01_05)
            .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
            .rateObservation(FixedRateObservation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
            .rateObservation(FixedRateObservation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_05)
            .endDate(DATE_04_07)
            .unadjustedEndDate(DATE_04_05)
            .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
            .rateObservation(FixedRateObservation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .build());
  }

  public void test_expand_twoAccrualsPerPayment_iborRate_varyingNotional_notionalExchange() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_06_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P2M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .compoundingMethod(STRAIGHT)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(GBP)
            .amount(ValueSchedule.of(1000d, ValueStep.of(1, ValueAdjustment.ofReplace(1500d))))
            .initialExchange(true)
            .intermediateExchange(true)
            .finalExchange(true)
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_365F)
            .index(GBP_LIBOR_1M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_02_05)
                .unadjustedStartDate(DATE_01_05)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
                .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_02))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_05)
                .endDate(DATE_03_05)
                .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
                .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .compoundingMethod(STRAIGHT)
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_05_08)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_03_05)
                .endDate(DATE_04_07)
                .unadjustedEndDate(DATE_04_05)
                .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
                .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_04_07)
                .endDate(DATE_05_06)
                .unadjustedStartDate(DATE_04_05)
                .unadjustedEndDate(DATE_05_05)
                .yearFraction(ACT_365F.yearFraction(DATE_04_07, DATE_05_06))
                .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_04_03))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1500d)
        .compoundingMethod(STRAIGHT)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_06_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_05_06)
            .endDate(DATE_06_05)
            .unadjustedStartDate(DATE_05_05)
            .yearFraction(ACT_365F.yearFraction(DATE_05_06, DATE_06_05))
            .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_05_01))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1500d)
        .compoundingMethod(STRAIGHT)
        .build();
    // events (only one intermediate exchange)
    NotionalExchange nexInitial = NotionalExchange.of(DATE_01_06, CurrencyAmount.of(GBP, 1000d));
    NotionalExchange nexIntermediate = NotionalExchange.of(DATE_03_07, CurrencyAmount.of(GBP, 500d));
    NotionalExchange nexFinal = NotionalExchange.of(DATE_06_09, CurrencyAmount.of(GBP, -1500d));
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .paymentEvents(nexInitial, nexIntermediate, nexFinal)
        .build());
  }

  public void test_expand_threeAccrualsPerPayment() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .compoundingMethod(STRAIGHT)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_02_05)
                .unadjustedStartDate(DATE_01_05)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
                .rateObservation(FixedRateObservation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_05)
                .endDate(DATE_03_05)
                .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
                .rateObservation(FixedRateObservation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_03_05)
                .endDate(DATE_04_07)
                .unadjustedEndDate(DATE_04_05)
                .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
                .rateObservation(FixedRateObservation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .compoundingMethod(STRAIGHT)
        .build();
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand_oneAccrualPerPayment_fxReset() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(GBP)
            .amount(ValueSchedule.of(1000d))
            .fxReset(FxResetCalculation.builder()
                .referenceCurrency(EUR)
                .index(EUR_GBP_ECB)
                .fixingDateOffset(MINUS_TWO_DAYS)
                .build())
            .build())
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_02_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_01_06)
            .endDate(DATE_02_05)
            .unadjustedStartDate(DATE_01_05)
            .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
            .rateObservation(FixedRateObservation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(EUR_GBP_ECB, EUR, DATE_01_02))
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
            .rateObservation(FixedRateObservation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(EUR_GBP_ECB, EUR, DATE_02_03))
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_05)
            .endDate(DATE_04_07)
            .unadjustedEndDate(DATE_04_05)
            .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
            .rateObservation(FixedRateObservation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(EUR_GBP_ECB, EUR, DATE_03_03))
        .build();
    FxResetNotionalExchange ne1a = FxResetNotionalExchange.builder()
        .paymentDate(DATE_01_06)
        .referenceCurrency(EUR)
        .index(EUR_GBP_ECB)
        .notional(1000d)
        .fixingDate(DATE_01_02)
        .build();
    FxResetNotionalExchange ne1b = FxResetNotionalExchange.builder()
        .paymentDate(DATE_02_07)
        .referenceCurrency(EUR)
        .index(EUR_GBP_ECB)
        .notional(-1000d)
        .fixingDate(DATE_01_02)
        .build();
    FxResetNotionalExchange ne2a = FxResetNotionalExchange.builder()
        .paymentDate(DATE_02_07)
        .referenceCurrency(EUR)
        .index(EUR_GBP_ECB)
        .notional(1000d)
        .fixingDate(DATE_02_03)
        .build();
    FxResetNotionalExchange ne2b = FxResetNotionalExchange.builder()
        .paymentDate(DATE_03_07)
        .referenceCurrency(EUR)
        .index(EUR_GBP_ECB)
        .notional(-1000d)
        .fixingDate(DATE_02_03)
        .build();
    FxResetNotionalExchange ne3a = FxResetNotionalExchange.builder()
        .paymentDate(DATE_03_07)
        .referenceCurrency(EUR)
        .index(EUR_GBP_ECB)
        .notional(1000d)
        .fixingDate(DATE_03_03)
        .build();
    FxResetNotionalExchange ne3b = FxResetNotionalExchange.builder()
        .paymentDate(DATE_04_09)
        .referenceCurrency(EUR)
        .index(EUR_GBP_ECB)
        .notional(-1000d)
        .fixingDate(DATE_03_03)
        .build();
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .paymentEvents(ne1a, ne1b, ne2a, ne2b, ne3a, ne3b)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_inflation_monthly() {
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(Frequency.ofYears(5))
        .businessDayAdjustment(adj)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    InflationRateCalculation rateCalc = InflationRateCalculation.builder()
        .index(GB_RPI)
        .interpolated(false)
        .lag(Period.ofMonths(3))
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    assertEquals(test.getStartDate(), adj.adjust(DATE_14_06_09));
    assertEquals(test.getEndDate(), adj.adjust(DATE_19_06_09));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPayReceive(), PAY);
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getNotionalSchedule(), notionalSchedule);
    assertEquals(test.getCalculation(), rateCalc);

    RatePaymentPeriod rpp = RatePaymentPeriod.builder()
        .paymentDate(DaysAdjustment.ofBusinessDays(2, GBLO).adjust(adj.adjust(DATE_19_06_09)))
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(BusinessDayAdjustment.of(FOLLOWING, GBLO).adjust(DATE_14_06_09))
            .endDate(BusinessDayAdjustment.of(FOLLOWING, GBLO).adjust(DATE_19_06_09))
            .unadjustedStartDate(DATE_14_06_09)
            .unadjustedEndDate(DATE_19_06_09)
            .yearFraction(1.0)
            .rateObservation(
                InflationMonthlyRateObservation.of(
                    GB_RPI,
                    YearMonth.from(adj.adjust(DATE_14_06_09)).minusMonths(3),
                    YearMonth.from(adj.adjust(DATE_19_06_09)).minusMonths(3)))
            .build())
        .dayCount(ONE_ONE)
        .currency(GBP)
        .notional(-1000d)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(rpp)
        .payReceive(PAY)
        .type(SwapLegType.INFLATION)
        .build();
    ExpandedSwapLeg testExpand = test.expand();
    assertEquals(testExpand, expected);

  }

  public void test_inflation_interpolated() {
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(Frequency.ofYears(5))
        .businessDayAdjustment(adj)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    InflationRateCalculation rateCalc = InflationRateCalculation.builder()
        .index(GB_RPI)
        .interpolated(true)
        .lag(Period.ofMonths(3))
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    assertEquals(test.getStartDate(), adj.adjust(DATE_14_06_09));
    assertEquals(test.getEndDate(), adj.adjust(DATE_19_06_09));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPayReceive(), RECEIVE);
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getNotionalSchedule(), notionalSchedule);
    assertEquals(test.getCalculation(), rateCalc);

    double weight = 1. - 9.0 / 30.0;
    RatePaymentPeriod rpp0 = RatePaymentPeriod.builder()
        .paymentDate(DaysAdjustment.ofBusinessDays(2, GBLO).adjust(adj.adjust(DATE_19_06_09)))
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(adj.adjust(DATE_14_06_09))
            .endDate(adj.adjust(DATE_19_06_09))
            .unadjustedStartDate(DATE_14_06_09)
            .unadjustedEndDate(DATE_19_06_09)
            .yearFraction(1.0)
            .rateObservation(
                InflationInterpolatedRateObservation.of(
                    GB_RPI,
                    YearMonth.from(adj.adjust(DATE_14_06_09)).minusMonths(3),
                    YearMonth.from(adj.adjust(DATE_19_06_09)).minusMonths(3),
                    weight))
            .build())
        .dayCount(ONE_ONE)
        .currency(GBP)
        .notional(1000d)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(rpp0)
        .payReceive(RECEIVE)
        .type(SwapLegType.INFLATION)
        .build();
    ExpandedSwapLeg testExpand = test.expand();
    assertEquals(testExpand, expected);
  }

  public void test_inflation_fixed() {
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(P12M)
        .businessDayAdjustment(adj)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .compoundingMethod(STRAIGHT)
        .build();
    FixedRateCalculation rateCalc = FixedRateCalculation.builder()
        .rate(ValueSchedule.of(0.05))
        .dayCount(ONE_ONE) // year fraction is always 1.
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    assertEquals(test.getStartDate(), adj.adjust(DATE_14_06_09));
    assertEquals(test.getEndDate(), adj.adjust(DATE_19_06_09));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPayReceive(), RECEIVE);
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getNotionalSchedule(), notionalSchedule);
    assertEquals(test.getCalculation(), rateCalc);
    RateAccrualPeriod rap0 = RateAccrualPeriod.builder()
        .startDate(adj.adjust(DATE_14_06_09))
        .endDate(adj.adjust(DATE_14_06_09.plusYears(1)))
        .unadjustedStartDate(DATE_14_06_09)
        .unadjustedEndDate(DATE_14_06_09.plusYears(1))
        .yearFraction(1.0)
        .rateObservation(FixedRateObservation.of(0.05))
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder()
        .startDate(adj.adjust(DATE_14_06_09.plusYears(1)))
        .endDate(adj.adjust(DATE_14_06_09.plusYears(2)))
        .unadjustedStartDate(DATE_14_06_09.plusYears(1))
        .unadjustedEndDate(DATE_14_06_09.plusYears(2))
        .yearFraction(1.0)
        .rateObservation(FixedRateObservation.of(0.05))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder()
        .startDate(adj.adjust(DATE_14_06_09.plusYears(2)))
        .endDate(adj.adjust(DATE_14_06_09.plusYears(3)))
        .unadjustedStartDate(DATE_14_06_09.plusYears(2))
        .unadjustedEndDate(DATE_14_06_09.plusYears(3))
        .yearFraction(1.0)
        .rateObservation(FixedRateObservation.of(0.05))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder()
        .startDate(adj.adjust(DATE_14_06_09.plusYears(3)))
        .endDate(adj.adjust(DATE_14_06_09.plusYears(4)))
        .unadjustedStartDate(DATE_14_06_09.plusYears(3))
        .unadjustedEndDate(DATE_14_06_09.plusYears(4))
        .yearFraction(1.0)
        .rateObservation(FixedRateObservation.of(0.05))
        .build();
    RateAccrualPeriod rap4 = RateAccrualPeriod.builder()
        .startDate(adj.adjust(DATE_14_06_09.plusYears(4)))
        .endDate(adj.adjust(DATE_19_06_09))
        .unadjustedStartDate(DATE_14_06_09.plusYears(4))
        .unadjustedEndDate(DATE_19_06_09)
        .yearFraction(1.0)
        .rateObservation(FixedRateObservation.of(0.05))
        .build();
    RatePaymentPeriod rpp = RatePaymentPeriod.builder()
        .paymentDate(DaysAdjustment.ofBusinessDays(2, GBLO).adjust(adj.adjust(DATE_19_06_09)))
        .accrualPeriods(rap0, rap1, rap2, rap3, rap4)
        .compoundingMethod(STRAIGHT)
        .dayCount(ONE_ONE)
        .currency(GBP)
        .notional(1000d)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(rpp)
        .payReceive(RECEIVE)
        .type(SwapLegType.FIXED)
        .build();
    ExpandedSwapLeg testExpand = test.expand();
    assertEquals(testExpand, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    coverImmutableBean(test);
    RateCalculationSwapLeg test2 = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_THREE_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 2000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(DayCounts.ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    assertSerialization(test);
  }

}
