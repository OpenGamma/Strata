/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P13W;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P1W;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.CompoundingMethod.STRAIGHT;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.INTERPOLATED;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.MONTHLY;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;

/**
 * Test.
 */
public class RateCalculationSwapLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_01_02 = date(2014, 1, 2);
  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_01_07 = date(2014, 1, 7);
  private static final LocalDate DATE_01_09 = date(2014, 1, 9);
  private static final LocalDate DATE_01_12 = date(2014, 1, 12);
  private static final LocalDate DATE_01_13 = date(2014, 1, 13);
  private static final LocalDate DATE_01_14 = date(2014, 1, 14);
  private static final LocalDate DATE_01_19 = date(2014, 1, 19);
  private static final LocalDate DATE_01_20 = date(2014, 1, 20);
  private static final LocalDate DATE_01_21 = date(2014, 1, 21);
  private static final LocalDate DATE_01_26 = date(2014, 1, 26);
  private static final LocalDate DATE_01_27 = date(2014, 1, 27);
  private static final LocalDate DATE_01_28 = date(2014, 1, 28);
  private static final LocalDate DATE_02_03 = date(2014, 2, 3);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_02_06 = date(2014, 2, 6);
  private static final LocalDate DATE_02_07 = date(2014, 2, 7);
  private static final LocalDate DATE_02_10 = date(2014, 2, 10);
  private static final LocalDate DATE_02_11 = date(2014, 2, 11);
  private static final LocalDate DATE_03_03 = date(2014, 3, 3);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_03_06 = date(2014, 3, 6);
  private static final LocalDate DATE_03_07 = date(2014, 3, 7);
  private static final LocalDate DATE_03_10 = date(2014, 3, 10);
  private static final LocalDate DATE_04_03 = date(2014, 4, 3);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_06 = date(2014, 4, 6);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);
  private static final LocalDate DATE_04_08 = date(2014, 4, 8);
  private static final LocalDate DATE_04_09 = date(2014, 4, 9);
  private static final LocalDate DATE_04_10 = date(2014, 4, 10);
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
  @Test
  public void test_builder() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_01_05)
        .endDate(DATE_04_05)
        .frequency(P1M)
        .businessDayAdjustment(bda)
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
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(DATE_01_05, bda));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(DATE_04_05, bda));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getPayReceive()).isEqualTo(PAY);
    assertThat(test.getAccrualSchedule()).isEqualTo(accrualSchedule);
    assertThat(test.getPaymentSchedule()).isEqualTo(paymentSchedule);
    assertThat(test.getNotionalSchedule()).isEqualTo(notionalSchedule);
    assertThat(test.getCalculation()).isEqualTo(rateCalc);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(builder.build()).containsOnly(GBP_LIBOR_3M);
    assertThat(test.allIndices()).containsOnly(GBP_LIBOR_3M);
    assertThat(test.allCurrencies()).containsOnly(GBP);
  }

  @Test
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
            .finalExchange(true)
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
    assertThat(builder.build()).containsOnly(GBP_LIBOR_3M, EUR_GBP_ECB);
    assertThat(test.allIndices()).containsOnly(GBP_LIBOR_3M, EUR_GBP_ECB);
    assertThat(test.allCurrencies()).containsOnly(GBP, EUR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_replaceStartDate() {
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
    RateCalculationSwapLeg expected = test.toBuilder()
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_02)
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .stubConvention(SMART_INITIAL)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .build();
    // assertion
    assertThat(test.replaceStartDate(DATE_01_02)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve_oneAccrualPerPayment_fixedRate() {
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
            .rateComputation(FixedRateComputation.of(0.025d))
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
            .rateComputation(FixedRateComputation.of(0.025d))
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
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .build());
  }

  @Test
  public void test_resolve_knownAmountStub() {
    // test case
    CurrencyAmount knownAmount = CurrencyAmount.of(GBP, 150d);
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_02_03)
            .endDate(DATE_04_03)
            .firstRegularStartDate(DATE_02_05)
            .lastRegularEndDate(DATE_03_05)
            .frequency(P1M)
            .stubConvention(StubConvention.BOTH)
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
            .initialStub(FixedRateStubCalculation.ofKnownAmount(knownAmount))
            .finalStub(FixedRateStubCalculation.ofFixedRate(0.1d))
            .build())
        .build();
    // expected
    KnownAmountNotionalSwapPaymentPeriod pp1 = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(Payment.of(knownAmount, DATE_02_07))
        .startDate(DATE_02_03)
        .endDate(DATE_02_05)
        .unadjustedStartDate(DATE_02_03)
        .notionalAmount(CurrencyAmount.of(GBP, -1000d))
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_05)
            .endDate(DATE_04_03)
            .unadjustedEndDate(DATE_04_03)
            .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_03))
            .rateComputation(FixedRateComputation.of(0.1d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(pp1, rpp2, rpp3)
        .build());
  }

  @Test
  public void test_resolve_twoAccrualsPerPayment_iborRate_varyingNotional_notionalExchange() {
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
                .rateComputation(IborRateComputation.of(GBP_LIBOR_1M, DATE_01_02, REF_DATA))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_05)
                .endDate(DATE_03_05)
                .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
                .rateComputation(IborRateComputation.of(GBP_LIBOR_1M, DATE_02_03, REF_DATA))
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
                .rateComputation(IborRateComputation.of(GBP_LIBOR_1M, DATE_03_03, REF_DATA))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_04_07)
                .endDate(DATE_05_06)
                .unadjustedStartDate(DATE_04_05)
                .unadjustedEndDate(DATE_05_05)
                .yearFraction(ACT_365F.yearFraction(DATE_04_07, DATE_05_06))
                .rateComputation(IborRateComputation.of(GBP_LIBOR_1M, DATE_04_03, REF_DATA))
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
            .rateComputation(IborRateComputation.of(GBP_LIBOR_1M, DATE_05_01, REF_DATA))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1500d)
        .compoundingMethod(STRAIGHT)
        .build();
    // events (only one intermediate exchange)
    NotionalExchange nexInitial = NotionalExchange.of(CurrencyAmount.of(GBP, 1000d), DATE_01_06);
    NotionalExchange nexIntermediate = NotionalExchange.of(CurrencyAmount.of(GBP, 500d), DATE_03_07);
    NotionalExchange nexFinal = NotionalExchange.of(CurrencyAmount.of(GBP, -1500d), DATE_06_09);
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .paymentEvents(nexInitial, nexIntermediate, nexFinal)
        .build());
  }

  @Test
  public void test_resolve_threeAccrualsPerPayment() {
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
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_05)
                .endDate(DATE_03_05)
                .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_03_05)
                .endDate(DATE_04_07)
                .unadjustedEndDate(DATE_04_05)
                .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .compoundingMethod(STRAIGHT)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1)
        .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve_oneAccrualPerPayment_fxReset() {
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
            .initialExchange(true)
            .intermediateExchange(true)
            .finalExchange(true)
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
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, DATE_01_02, REF_DATA), EUR))
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, DATE_02_03, REF_DATA), EUR))
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_05)
            .endDate(DATE_04_07)
            .unadjustedEndDate(DATE_04_05)
            .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, DATE_03_03, REF_DATA), EUR))
        .build();
    FxResetNotionalExchange ne1a = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, 1000d), DATE_01_06, FxIndexObservation.of(EUR_GBP_ECB, DATE_01_02, REF_DATA));
    FxResetNotionalExchange ne1b = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, -1000d), DATE_02_07, FxIndexObservation.of(EUR_GBP_ECB, DATE_01_02, REF_DATA));
    FxResetNotionalExchange ne2a = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, 1000d), DATE_02_07, FxIndexObservation.of(EUR_GBP_ECB, DATE_02_03, REF_DATA));
    FxResetNotionalExchange ne2b = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, -1000d), DATE_03_07, FxIndexObservation.of(EUR_GBP_ECB, DATE_02_03, REF_DATA));
    FxResetNotionalExchange ne3a = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, 1000d), DATE_03_07, FxIndexObservation.of(EUR_GBP_ECB, DATE_03_03, REF_DATA));
    FxResetNotionalExchange ne3b = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, -1000d), DATE_04_09, FxIndexObservation.of(EUR_GBP_ECB, DATE_03_03, REF_DATA));
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .paymentEvents(ne1a, ne1b, ne2a, ne2b, ne3a, ne3b)
        .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_inflation_monthly() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(Frequency.ofYears(5))
        .businessDayAdjustment(bda)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    InflationRateCalculation rateCalc = InflationRateCalculation.builder()
        .index(GB_RPI)
        .indexCalculationMethod(MONTHLY)
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
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(DATE_14_06_09, bda));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(DATE_19_06_09, bda));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getPayReceive()).isEqualTo(PAY);
    assertThat(test.getAccrualSchedule()).isEqualTo(accrualSchedule);
    assertThat(test.getPaymentSchedule()).isEqualTo(paymentSchedule);
    assertThat(test.getNotionalSchedule()).isEqualTo(notionalSchedule);
    assertThat(test.getCalculation()).isEqualTo(rateCalc);

    RatePaymentPeriod rpp = RatePaymentPeriod.builder()
        .paymentDate(DaysAdjustment.ofBusinessDays(2, GBLO).adjust(bda.adjust(DATE_19_06_09, REF_DATA), REF_DATA))
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(BusinessDayAdjustment.of(FOLLOWING, GBLO).adjust(DATE_14_06_09, REF_DATA))
            .endDate(BusinessDayAdjustment.of(FOLLOWING, GBLO).adjust(DATE_19_06_09, REF_DATA))
            .unadjustedStartDate(DATE_14_06_09)
            .unadjustedEndDate(DATE_19_06_09)
            .yearFraction(1.0)
            .rateComputation(
                InflationMonthlyRateComputation.of(
                    GB_RPI,
                    YearMonth.from(bda.adjust(DATE_14_06_09, REF_DATA)).minusMonths(3),
                    YearMonth.from(bda.adjust(DATE_19_06_09, REF_DATA)).minusMonths(3)))
            .build())
        .dayCount(ONE_ONE)
        .currency(GBP)
        .notional(-1000d)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .paymentPeriods(rpp)
        .payReceive(PAY)
        .type(SwapLegType.INFLATION)
        .build();
    ResolvedSwapLeg testResolved = test.resolve(REF_DATA);
    assertThat(testResolved).isEqualTo(expected);

  }

  @Test
  public void test_inflation_interpolated() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(Frequency.ofYears(5))
        .businessDayAdjustment(bda)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    InflationRateCalculation rateCalc = InflationRateCalculation.builder()
        .index(GB_RPI)
        .indexCalculationMethod(INTERPOLATED)
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
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(DATE_14_06_09, bda));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(DATE_19_06_09, bda));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getAccrualSchedule()).isEqualTo(accrualSchedule);
    assertThat(test.getPaymentSchedule()).isEqualTo(paymentSchedule);
    assertThat(test.getNotionalSchedule()).isEqualTo(notionalSchedule);
    assertThat(test.getCalculation()).isEqualTo(rateCalc);

    double weight = 1. - 9.0 / 30.0;
    RatePaymentPeriod rpp0 = RatePaymentPeriod.builder()
        .paymentDate(DaysAdjustment.ofBusinessDays(2, GBLO).adjust(bda.adjust(DATE_19_06_09, REF_DATA), REF_DATA))
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(bda.adjust(DATE_14_06_09, REF_DATA))
            .endDate(bda.adjust(DATE_19_06_09, REF_DATA))
            .unadjustedStartDate(DATE_14_06_09)
            .unadjustedEndDate(DATE_19_06_09)
            .yearFraction(1.0)
            .rateComputation(
                InflationInterpolatedRateComputation.of(
                    GB_RPI,
                    YearMonth.from(bda.adjust(DATE_14_06_09, REF_DATA)).minusMonths(3),
                    YearMonth.from(bda.adjust(DATE_19_06_09, REF_DATA)).minusMonths(3),
                    weight))
            .build())
        .dayCount(ONE_ONE)
        .currency(GBP)
        .notional(1000d)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .paymentPeriods(rpp0)
        .payReceive(RECEIVE)
        .type(SwapLegType.INFLATION)
        .build();
    ResolvedSwapLeg testExpand = test.resolve(REF_DATA);
    assertThat(testExpand).isEqualTo(expected);
  }

  @Test
  public void test_inflation_fixed() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(P12M)
        .businessDayAdjustment(bda)
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
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(DATE_14_06_09, bda));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(DATE_19_06_09, bda));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getAccrualSchedule()).isEqualTo(accrualSchedule);
    assertThat(test.getPaymentSchedule()).isEqualTo(paymentSchedule);
    assertThat(test.getNotionalSchedule()).isEqualTo(notionalSchedule);
    assertThat(test.getCalculation()).isEqualTo(rateCalc);
    RateAccrualPeriod rap0 = RateAccrualPeriod.builder()
        .startDate(bda.adjust(DATE_14_06_09, REF_DATA))
        .endDate(bda.adjust(DATE_14_06_09.plusYears(1), REF_DATA))
        .unadjustedStartDate(DATE_14_06_09)
        .unadjustedEndDate(DATE_14_06_09.plusYears(1))
        .yearFraction(1.0)
        .rateComputation(FixedRateComputation.of(0.05))
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder()
        .startDate(bda.adjust(DATE_14_06_09.plusYears(1), REF_DATA))
        .endDate(bda.adjust(DATE_14_06_09.plusYears(2), REF_DATA))
        .unadjustedStartDate(DATE_14_06_09.plusYears(1))
        .unadjustedEndDate(DATE_14_06_09.plusYears(2))
        .yearFraction(1.0)
        .rateComputation(FixedRateComputation.of(0.05))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder()
        .startDate(bda.adjust(DATE_14_06_09.plusYears(2), REF_DATA))
        .endDate(bda.adjust(DATE_14_06_09.plusYears(3), REF_DATA))
        .unadjustedStartDate(DATE_14_06_09.plusYears(2))
        .unadjustedEndDate(DATE_14_06_09.plusYears(3))
        .yearFraction(1.0)
        .rateComputation(FixedRateComputation.of(0.05))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder()
        .startDate(bda.adjust(DATE_14_06_09.plusYears(3), REF_DATA))
        .endDate(bda.adjust(DATE_14_06_09.plusYears(4), REF_DATA))
        .unadjustedStartDate(DATE_14_06_09.plusYears(3))
        .unadjustedEndDate(DATE_14_06_09.plusYears(4))
        .yearFraction(1.0)
        .rateComputation(FixedRateComputation.of(0.05))
        .build();
    RateAccrualPeriod rap4 = RateAccrualPeriod.builder()
        .startDate(bda.adjust(DATE_14_06_09.plusYears(4), REF_DATA))
        .endDate(bda.adjust(DATE_19_06_09, REF_DATA))
        .unadjustedStartDate(DATE_14_06_09.plusYears(4))
        .unadjustedEndDate(DATE_19_06_09)
        .yearFraction(1.0)
        .rateComputation(FixedRateComputation.of(0.05))
        .build();
    RatePaymentPeriod rpp = RatePaymentPeriod.builder()
        .paymentDate(DaysAdjustment.ofBusinessDays(2, GBLO).adjust(bda.adjust(DATE_19_06_09, REF_DATA), REF_DATA))
        .accrualPeriods(rap0, rap1, rap2, rap3, rap4)
        .compoundingMethod(STRAIGHT)
        .dayCount(ONE_ONE)
        .currency(GBP)
        .notional(1000d)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .paymentPeriods(rpp)
        .payReceive(RECEIVE)
        .type(SwapLegType.FIXED)
        .build();
    ResolvedSwapLeg testExpand = test.resolve(REF_DATA);
    assertThat(testExpand).isEqualTo(expected);
  }

  @Test
  public void test_resolve_weeklyAccruals_termPayments_fixedRate() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_02_07)
            .frequency(P1W)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .stubConvention(SMART_FINAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
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
        .paymentDate(DATE_02_11)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_01_13)
                .unadjustedStartDate(DATE_01_06)
                .unadjustedEndDate(DATE_01_13)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_01_13))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
          RateAccrualPeriod.builder()
              .startDate(DATE_01_13)
              .endDate(DATE_01_20)
              .unadjustedStartDate(DATE_01_13)
              .unadjustedEndDate(DATE_01_20)
              .yearFraction(ACT_365F.yearFraction(DATE_01_13, DATE_01_20))
              .rateComputation(FixedRateComputation.of(0.025d))
              .build(),
          RateAccrualPeriod.builder()
              .startDate(DATE_01_20)
              .endDate(DATE_01_27)
              .unadjustedStartDate(DATE_01_20)
              .unadjustedEndDate(DATE_01_27)
              .yearFraction(ACT_365F.yearFraction(DATE_01_20, DATE_01_27))
              .rateComputation(FixedRateComputation.of(0.025d))
              .build(),
          RateAccrualPeriod.builder()
              .startDate(DATE_01_27)
              .endDate(DATE_02_03)
              .unadjustedStartDate(DATE_01_27)
              .unadjustedEndDate(DATE_02_03)
              .yearFraction(ACT_365F.yearFraction(DATE_01_27, DATE_02_03))
              .rateComputation(FixedRateComputation.of(0.025d))
              .build(),
          RateAccrualPeriod.builder()
              .startDate(DATE_02_03)
              .endDate(DATE_02_07)
              .unadjustedStartDate(DATE_02_03)
              .unadjustedEndDate(DATE_02_07)
              .yearFraction(ACT_365F.yearFraction(DATE_02_03, DATE_02_07))
              .rateComputation(FixedRateComputation.of(0.025d))
              .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1)
        .build());
  }

  @Test
  public void test_resolve_weeklyAccruals_monthlyPayments_fixedRate() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_02_07)
            .frequency(P1W)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .stubConvention(SMART_FINAL)
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
        .paymentDate(DATE_02_11)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_01_13)
                .unadjustedStartDate(DATE_01_06)
                .unadjustedEndDate(DATE_01_13)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_01_13))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_13)
                .endDate(DATE_01_20)
                .unadjustedStartDate(DATE_01_13)
                .unadjustedEndDate(DATE_01_20)
                .yearFraction(ACT_365F.yearFraction(DATE_01_13, DATE_01_20))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_20)
                .endDate(DATE_01_27)
                .unadjustedStartDate(DATE_01_20)
                .unadjustedEndDate(DATE_01_27)
                .yearFraction(ACT_365F.yearFraction(DATE_01_20, DATE_01_27))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_27)
                .endDate(DATE_02_03)
                .unadjustedStartDate(DATE_01_27)
                .unadjustedEndDate(DATE_02_03)
                .yearFraction(ACT_365F.yearFraction(DATE_01_27, DATE_02_03))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_03)
                .endDate(DATE_02_07)
                .unadjustedStartDate(DATE_02_03)
                .unadjustedEndDate(DATE_02_07)
                .yearFraction(ACT_365F.yearFraction(DATE_02_03, DATE_02_07))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1)
        .build());
  }

  @Test
  public void test_resolve_weeklyAccruals_monthlyPayments_initialStub_fixedRate() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_02_07)
            .frequency(P1W)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .stubConvention(SMART_INITIAL)
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
    RatePaymentPeriod rpp = RatePaymentPeriod.builder()
        .paymentDate(DATE_02_11)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_01_13)
                .unadjustedStartDate(DATE_01_06)
                .unadjustedEndDate(DATE_01_13)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_01_13))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_13)
                .endDate(DATE_01_20)
                .unadjustedStartDate(DATE_01_13)
                .unadjustedEndDate(DATE_01_20)
                .yearFraction(ACT_365F.yearFraction(DATE_01_13, DATE_01_20))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_20)
                .endDate(DATE_01_27)
                .unadjustedStartDate(DATE_01_20)
                .unadjustedEndDate(DATE_01_27)
                .yearFraction(ACT_365F.yearFraction(DATE_01_20, DATE_01_27))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_27)
                .endDate(DATE_02_03)
                .unadjustedStartDate(DATE_01_27)
                .unadjustedEndDate(DATE_02_03)
                .yearFraction(ACT_365F.yearFraction(DATE_01_27, DATE_02_03))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_03)
                .endDate(DATE_02_07)
                .unadjustedStartDate(DATE_02_03)
                .unadjustedEndDate(DATE_02_07)
                .yearFraction(ACT_365F.yearFraction(DATE_02_03, DATE_02_07))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp)
        .build());
  }

  @Test
  public void test_resolve_weeklyAccruals_monthlyPayments_initialStub_fixedRate_combineDuplicateAccrualPeriods() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_01_27)
            .frequency(P1W)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarId.of("Mock")))
            .stubConvention(SMART_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp = RatePaymentPeriod.builder()
        .paymentDate(DATE_01_27)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_05)
                .endDate(DATE_01_12)
                .unadjustedStartDate(DATE_01_05)
                .unadjustedEndDate(DATE_01_12)
                .yearFraction(ACT_365F.yearFraction(DATE_01_05, DATE_01_12))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_12)
                .endDate(DATE_01_19)
                .unadjustedStartDate(DATE_01_12)
                .unadjustedEndDate(DATE_01_19)
                .yearFraction(ACT_365F.yearFraction(DATE_01_12, DATE_01_19))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_19)
                .endDate(DATE_01_26)
                .unadjustedStartDate(DATE_01_19)
                .unadjustedEndDate(DATE_01_26)
                .yearFraction(ACT_365F.yearFraction(DATE_01_19, DATE_01_26))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_01_26)
                .endDate(DATE_01_27)
                .unadjustedStartDate(DATE_01_26)
                .unadjustedEndDate(DATE_01_27)
                .yearFraction(ACT_365F.yearFraction(DATE_01_26, DATE_01_27))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    ReferenceData mockRd = ReferenceData.of(ImmutableMap.of(HolidayCalendarId.of("Mock"), new MockHolCal()));
    assertThat(test.resolve(mockRd)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp)
        .build());
  }

  @Test
  public void test_resolve_weeklyAccruals_monthlyPayments_finalSmartStub_endOfMonth_fixedRate() {
    // test case
    LocalDate tradeStartDate = LocalDate.of(2022, 11, 30);
    LocalDate tradeEndDate = LocalDate.of(2023, 12, 5);
    LocalDate firstPeriodEnd = LocalDate.of(2023, 2, 28);
    LocalDate secondPeriodEnd = LocalDate.of(2023, 5, 30);
    LocalDate thirdPeriodFirstAccEnd = LocalDate.of(2023, 8, 29);
    LocalDate thirdPeriodEnd = LocalDate.of(2023, 8, 30);
    LocalDate fourthPeriodFirstAccEnd = LocalDate.of(2023, 11, 29);

    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(tradeStartDate)
            .endDate(tradeEndDate)
            .frequency(P13W)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .stubConvention(SMART_FINAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();

    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(firstPeriodEnd)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(tradeStartDate)
                .endDate(firstPeriodEnd)
                .unadjustedStartDate(tradeStartDate)
                .unadjustedEndDate(firstPeriodEnd)
                .yearFraction(ACT_365F.yearFraction(tradeStartDate, firstPeriodEnd))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(secondPeriodEnd)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(firstPeriodEnd)
            .endDate(secondPeriodEnd)
            .unadjustedStartDate(firstPeriodEnd)
            .unadjustedEndDate(secondPeriodEnd)
            .yearFraction(ACT_365F.yearFraction(firstPeriodEnd, secondPeriodEnd))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(thirdPeriodEnd)
        .accrualPeriods(RateAccrualPeriod.builder()
                .startDate(secondPeriodEnd)
                .endDate(thirdPeriodFirstAccEnd)
                .unadjustedStartDate(secondPeriodEnd)
                .unadjustedEndDate(thirdPeriodFirstAccEnd)
                .yearFraction(ACT_365F.yearFraction(secondPeriodEnd, thirdPeriodFirstAccEnd))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(thirdPeriodFirstAccEnd)
                .endDate(thirdPeriodEnd)
                .unadjustedStartDate(thirdPeriodFirstAccEnd)
                .unadjustedEndDate(thirdPeriodEnd)
                .yearFraction(ACT_365F.yearFraction(thirdPeriodFirstAccEnd, thirdPeriodEnd))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp4 = RatePaymentPeriod.builder()
        .paymentDate(tradeEndDate)
        .accrualPeriods(RateAccrualPeriod.builder()
                .startDate(thirdPeriodEnd)
                .endDate(fourthPeriodFirstAccEnd)
                .unadjustedStartDate(thirdPeriodEnd)
                .unadjustedEndDate(fourthPeriodFirstAccEnd)
                .yearFraction(ACT_365F.yearFraction(thirdPeriodEnd, fourthPeriodFirstAccEnd))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(fourthPeriodFirstAccEnd)
                .endDate(tradeEndDate)
                .unadjustedStartDate(fourthPeriodFirstAccEnd)
                .unadjustedEndDate(tradeEndDate)
                .yearFraction(ACT_365F.yearFraction(fourthPeriodFirstAccEnd, tradeEndDate))
                .rateComputation(FixedRateComputation.of(0.025d))
                .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3, rpp4)
        .build());
  }

  @Test
  public void test_accrual_offset() {
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
        .accrualScheduleOffset(DaysAdjustment.ofBusinessDays(1, GBLO))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_02_10)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_01_07)
            .endDate(DATE_02_06)
            .unadjustedStartDate(DATE_01_05)
            .unadjustedEndDate(DATE_02_05)
            .yearFraction(ACT_365F.yearFraction(DATE_01_07, DATE_02_06))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_10)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_06)
            .unadjustedStartDate(DATE_02_05)
            .endDate(DATE_03_06)
            .unadjustedEndDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_06, DATE_03_06))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_10)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_06)
            .unadjustedStartDate(DATE_03_05)
            .endDate(DATE_04_08)
            .unadjustedEndDate(DATE_04_05)
            .yearFraction(ACT_365F.yearFraction(DATE_03_06, DATE_04_08))
            .rateComputation(FixedRateComputation.of(0.025d))
            .build())
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .build());
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
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

  static class MockHolCal implements HolidayCalendar {
    @Override
    public boolean isHoliday(LocalDate date) {
      return date.getDayOfMonth() == 12 ||
          date.getDayOfMonth() == 13 ||
          date.getDayOfMonth() == 14 ||
          date.getDayOfMonth() == 15 ||
          date.getDayOfMonth() == 16 ||
          date.getDayOfMonth() == 17 ||
          date.getDayOfMonth() == 18 ||
          date.getDayOfMonth() == 19 ||
          date.getDayOfMonth() == 20 ||
          date.getDayOfMonth() == 21 ||
          date.getDayOfMonth() == 22 ||
          date.getDayOfMonth() == 23 ||
          date.getDayOfMonth() == 24 ||
          date.getDayOfMonth() == 25;
    }

    @Override
    public HolidayCalendarId getId() {
      return HolidayCalendarId.of("Mock");
    }
  }

}
