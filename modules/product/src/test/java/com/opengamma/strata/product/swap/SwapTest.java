/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.basics.value.ValueAdjustment.ofDeltaAmount;
import static com.opengamma.strata.collect.TestHelper.assertEqualsBean;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.MockSwapLeg.MOCK_EXPANDED_GBP1;
import static com.opengamma.strata.product.swap.MockSwapLeg.MOCK_EXPANDED_USD1;
import static com.opengamma.strata.product.swap.MockSwapLeg.MOCK_GBP1;
import static com.opengamma.strata.product.swap.MockSwapLeg.MOCK_GBP2;
import static com.opengamma.strata.product.swap.MockSwapLeg.MOCK_USD1;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static com.opengamma.strata.product.swap.SwapLegType.OTHER;
import static com.opengamma.strata.product.swap.SwapLegType.OVERNIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStepSequence;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConventions;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions;
import com.opengamma.strata.product.swap.type.IborIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;

/**
 * Test.
 */
public class SwapTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double RATE = 0.01d;
  private static final double NOTIONAL = 100_000d;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_list() {
    Swap test = Swap.builder()
        .legs(ImmutableList.of(MOCK_GBP1, MOCK_USD1))
        .build();
    assertThat(test.getLegs()).containsExactly(MOCK_GBP1, MOCK_USD1);
    assertThat(test.isCrossCurrency()).isTrue();
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP, USD);
    assertThat(test.allCurrencies()).containsOnly(GBP, EUR, USD);
  }

  @Test
  public void test_builder_varargs() {
    Swap test = Swap.builder()
        .legs(MOCK_GBP1, MOCK_USD1)
        .build();
    assertThat(test.getLegs()).containsExactly(MOCK_GBP1, MOCK_USD1);
  }

  @Test
  public void test_of_varargs() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertThat(test.getLegs()).containsExactly(MOCK_GBP1, MOCK_USD1);
    assertThat(ImmutableList.copyOf(test.getLegs())).containsExactly(MOCK_GBP1, MOCK_USD1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Swap.of((SwapLeg[]) null));
  }

  @Test
  public void test_of_list() {
    Swap test = Swap.of(ImmutableList.of(MOCK_GBP1, MOCK_USD1));
    assertThat(test.getLegs()).containsExactly(MOCK_GBP1, MOCK_USD1);
    assertThat(ImmutableList.copyOf(test.getLegs())).containsExactly(MOCK_GBP1, MOCK_USD1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Swap.of((List<SwapLeg>) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getLegs_SwapLegType() {
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(FIXED)).containsExactly(MOCK_GBP1);
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(IBOR)).containsExactly(MOCK_USD1);
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(OVERNIGHT)).isEmpty();
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(OTHER)).isEmpty();
  }

  @Test
  public void test_getLeg_PayReceive() {
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getLeg(PAY)).isEqualTo(Optional.of(MOCK_GBP1));
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getLeg(RECEIVE)).isEqualTo(Optional.of(MOCK_USD1));
    assertThat(Swap.of(MOCK_GBP1).getLeg(PAY)).isEqualTo(Optional.of(MOCK_GBP1));
    assertThat(Swap.of(MOCK_USD1).getLeg(PAY)).isEqualTo(Optional.empty());
    assertThat(Swap.of(MOCK_GBP1).getLeg(RECEIVE)).isEqualTo(Optional.empty());
    assertThat(Swap.of(MOCK_USD1).getLeg(RECEIVE)).isEqualTo(Optional.of(MOCK_USD1));
  }

  @Test
  public void test_getPayLeg() {
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getPayLeg()).isEqualTo(Optional.of(MOCK_GBP1));
    assertThat(Swap.of(MOCK_GBP1).getPayLeg()).isEqualTo(Optional.of(MOCK_GBP1));
    assertThat(Swap.of(MOCK_USD1).getPayLeg()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_getReceiveLeg() {
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).getReceiveLeg()).isEqualTo(Optional.of(MOCK_USD1));
    assertThat(Swap.of(MOCK_GBP1).getReceiveLeg()).isEqualTo(Optional.empty());
    assertThat(Swap.of(MOCK_USD1).getReceiveLeg()).isEqualTo(Optional.of(MOCK_USD1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getStartDate() {
    SwapLeg leg1 = MockSwapLeg.of(FIXED, PAY, date(2015, 6, 29), date(2017, 6, 30), Currency.USD);
    SwapLeg leg2 = MockSwapLeg.of(FIXED, RECEIVE, date(2015, 6, 30), date(2017, 6, 29), Currency.USD);
    assertThat(Swap.of(leg1).getStartDate()).isEqualTo(AdjustableDate.of(date(2015, 6, 29)));
    assertThat(Swap.of(leg2).getStartDate()).isEqualTo(AdjustableDate.of(date(2015, 6, 30)));
    assertThat(Swap.of(leg1, leg2).getStartDate()).isEqualTo(AdjustableDate.of(date(2015, 6, 29)));
    assertThat(Swap.of(leg2, leg1).getStartDate()).isEqualTo(AdjustableDate.of(date(2015, 6, 29)));
  }

  @Test
  public void test_getEndDate() {
    SwapLeg leg1 = MockSwapLeg.of(FIXED, PAY, date(2015, 6, 29), date(2017, 6, 30), Currency.USD);
    SwapLeg leg2 = MockSwapLeg.of(FIXED, RECEIVE, date(2015, 6, 30), date(2017, 6, 29), Currency.USD);
    assertThat(Swap.of(leg1).getEndDate()).isEqualTo(AdjustableDate.of(date(2017, 6, 30)));
    assertThat(Swap.of(leg2).getEndDate()).isEqualTo(AdjustableDate.of(date(2017, 6, 29)));
    assertThat(Swap.of(leg1, leg2).getEndDate()).isEqualTo(AdjustableDate.of(date(2017, 6, 30)));
    assertThat(Swap.of(leg2, leg1).getEndDate()).isEqualTo(AdjustableDate.of(date(2017, 6, 30)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_isCrossCurrency() {
    assertThat(Swap.of(MOCK_GBP1, MOCK_USD1).isCrossCurrency()).isTrue();
    assertThat(Swap.of(MOCK_GBP1, MOCK_GBP2, MOCK_USD1).isCrossCurrency()).isTrue();
    assertThat(Swap.of(MOCK_GBP1, MOCK_GBP2).isCrossCurrency()).isFalse();
    assertThat(Swap.of(MOCK_GBP1).isCrossCurrency()).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_allPaymentCurrencies() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP, USD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_allCurrencies() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertThat(test.allCurrencies()).containsOnly(GBP, USD, EUR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_allIndices() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertThat(test.allIndices()).containsOnly(IborIndices.GBP_LIBOR_3M, FxIndices.EUR_GBP_ECB, OvernightIndices.EUR_EONIA);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    Swap test = Swap.builder()
        .legs(ImmutableList.of(MOCK_GBP1, MOCK_USD1))
        .build();
    assertThat(test.summaryDescription()).isEqualTo(
        "7M Pay [GBP-LIBOR-3M, EUR/GBP-ECB, EUR-EONIA] / Rec [GBP-LIBOR-3M, EUR/GBP-ECB, EUR-EONIA] : 15Jan12-15Aug12");
  }

  @Test
  public void test_summarize_irs() {
    Swap test = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M
        .createTrade(date(2018, 2, 12), Tenor.TENOR_5Y, BuySell.BUY, 1_500_000d, 0.015d, REF_DATA).getProduct();
    assertThat(test.summaryDescription()).isEqualTo("5Y GBP 1.5mm Rec GBP-LIBOR-3M / Pay 1.5% : 12Feb18-12Feb23");
  }

  @Test
  public void test_summarize_irs_weird() {
    PeriodicSchedule accrual = PeriodicSchedule.of(
        date(2018, 2, 12), date(2020, 2, 12), Frequency.P3M, BusinessDayAdjustment.NONE, SHORT_INITIAL, false);
    PaymentSchedule payment = PaymentSchedule.builder()
        .paymentFrequency(Frequency.P3M)
        .paymentDateOffset(DaysAdjustment.NONE)
        .build();
    NotionalSchedule notional = NotionalSchedule.of(GBP, ValueSchedule.builder()
        .initialValue(1_000_000)
        .stepSequence(
            ValueStepSequence.of(date(2018, 8, 12), date(2019, 8, 12), Frequency.P6M, ofDeltaAmount(-50_000)))
        .build());
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.builder()
                .initialValue(0.0012)
                .stepSequence(ValueStepSequence.of(date(2018, 8, 12), date(2019, 8, 12), Frequency.P6M, ofDeltaAmount(0.0001)))
                .build())
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(IborIndices.GBP_LIBOR_3M)
            .gearing(ValueSchedule.of(1.1))
            .spread(ValueSchedule.of(0.002))
            .build())
        .build();
    Swap test = Swap.of(payLeg, recLeg);
    assertThat(test.summaryDescription())
        .isEqualTo("2Y GBP 1mm variable Rec GBP-LIBOR-3M * 1.1 + 0.2% / Pay 0.12% variable : 12Feb18-12Feb20");
  }

  @Test
  public void test_summarize_ois() {
    Swap test = FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS
        .createTrade(date(2018, 2, 12), Tenor.TENOR_2Y, BuySell.SELL, 1_500_000d, 0.015d, REF_DATA).getProduct();
    assertThat(test.summaryDescription()).isEqualTo("2Y GBP 1.5mm Rec 1.5% / Pay GBP-SONIA : 12Feb18-12Feb20");
  }

  @Test
  public void test_summarize_inf() {
    Swap test = FixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPI
        .createTrade(date(2018, 2, 12), Tenor.TENOR_2Y, BuySell.BUY, 1_500_000d, 0.015d, REF_DATA).getProduct();
    assertThat(test.summaryDescription()).isEqualTo("2Y GBP 1.5mm Rec GB-RPI / Pay 1.5% : 14Feb18-14Feb20");
  }

  @Test
  public void test_summarize_bas() {
    Swap test = IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M
        .createTrade(date(2018, 2, 12), Tenor.TENOR_2Y, BuySell.BUY, 2_500_000d, 0.007d, REF_DATA).getProduct();
    assertThat(test.summaryDescription()).isEqualTo("2Y USD 2.5mm Rec USD-LIBOR-6M / Pay USD-LIBOR-3M + 0.7% : 14Feb18-14Feb20");
  }

  @Test
  public void test_summarize_xccy() {
    Swap test = XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M
        .createTrade(date(2018, 2, 12), Tenor.TENOR_2Y, BuySell.BUY, 2_500_000d, 3_000_000d, 0.007d, REF_DATA).getProduct();
    assertThat(test.summaryDescription())
        .isEqualTo("2Y Rec USD-LIBOR-3M USD 3mm / Pay GBP-LIBOR-3M + 0.7% GBP 2.5mm : 14Feb18-14Feb20");
  }

  @Test
  public void test_summarize_knownAmount() {
    Swap test = Swap.of(KnownAmountSwapLeg.builder()
        .accrualSchedule(PeriodicSchedule.of(
            date(2018, 2, 12), date(2020, 2, 12), Frequency.P3M, BusinessDayAdjustment.NONE, SHORT_INITIAL, false))
        .amount(ValueSchedule.of(145_000))
        .currency(GBP)
        .payReceive(PAY)
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .build());
    assertThat(test.summaryDescription()).isEqualTo("2Y Pay GBP 145k : 12Feb18-12Feb20");
  }

  @Test
  public void test_summarize_knownAmountVarying() {
    Swap test = Swap.of(KnownAmountSwapLeg.builder()
        .accrualSchedule(PeriodicSchedule.of(
            date(2018, 2, 12), date(2020, 2, 12), Frequency.P3M, BusinessDayAdjustment.NONE, SHORT_INITIAL, false))
        .amount(ValueSchedule.builder()
            .initialValue(145_000)
            .stepSequence(
                ValueStepSequence.of(date(2018, 8, 12), date(2019, 8, 12), Frequency.P6M, ofDeltaAmount(-20_000)))
            .build())
        .currency(GBP)
        .payReceive(PAY)
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .build());
    assertThat(test.summaryDescription()).isEqualTo("2Y Pay GBP 145k variable : 12Feb18-12Feb20");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_replaceStartDate() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> Swap.builder().legs(MOCK_GBP1, MOCK_USD1).build().replaceStartDate(date(2000, 1, 1)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    Swap test = Swap.builder()
        .legs(ImmutableList.of(MOCK_GBP1, MOCK_USD1))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(ResolvedSwap.of(MOCK_EXPANDED_GBP1, MOCK_EXPANDED_USD1));
  }

  @Test
  public void test_resolve_unadjustedAccrualAdjustedPayment() {
    Swap test = Swap.builder()
        .legs(RateCalculationSwapLeg.builder()
            .payReceive(RECEIVE)
            .accrualSchedule(PeriodicSchedule.builder()
                .startDate(date(2016, 1, 3))
                .endDate(date(2016, 5, 3))
                .frequency(Frequency.P1M)  // Jan + Apr are Sunday
                .businessDayAdjustment(BusinessDayAdjustment.NONE)
                .build())
            .paymentSchedule(PaymentSchedule.builder()
                .paymentFrequency(Frequency.P1M)
                .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, SAT_SUN))
                .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, SAT_SUN))
                .build())
            .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL))
            .calculation(FixedRateCalculation.of(RATE, ACT_360))
            .build())
        .build();
    RatePaymentPeriod pp1 = RatePaymentPeriod.builder()
        .paymentDate(date(2016, 2, 5))  // 3rd plus two days
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(date(2016, 1, 3))
            .unadjustedStartDate(date(2016, 1, 3))
            .endDate(date(2016, 2, 3))
            .unadjustedEndDate(date(2016, 2, 3))
            .yearFraction(ACT_360.yearFraction(date(2016, 1, 3), date(2016, 2, 3)))
            .rateComputation(FixedRateComputation.of(RATE))
            .build())
        .dayCount(ACT_360)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    RatePaymentPeriod pp2 = RatePaymentPeriod.builder()
        .paymentDate(date(2016, 3, 7))  // 3rd plus two days is Saturday, Monday is 7th
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(date(2016, 2, 3))
            .unadjustedStartDate(date(2016, 2, 3))
            .endDate(date(2016, 3, 3))
            .unadjustedEndDate(date(2016, 3, 3))
            .yearFraction(ACT_360.yearFraction(date(2016, 2, 3), date(2016, 3, 3)))
            .rateComputation(FixedRateComputation.of(RATE))
            .build())
        .dayCount(ACT_360)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    RatePaymentPeriod pp3 = RatePaymentPeriod.builder()
        .paymentDate(date(2016, 4, 6))  // 3rd is Sunday, bumped to Monday by schedule, then plus two days
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(date(2016, 3, 3))
            .unadjustedStartDate(date(2016, 3, 3))
            .endDate(date(2016, 4, 3))
            .unadjustedEndDate(date(2016, 4, 3))
            .yearFraction(ACT_360.yearFraction(date(2016, 3, 3), date(2016, 4, 3)))
            .rateComputation(FixedRateComputation.of(RATE))
            .build())
        .dayCount(ACT_360)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    RatePaymentPeriod pp4 = RatePaymentPeriod.builder()
        .paymentDate(date(2016, 5, 5))  // 3rd plus two days
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(date(2016, 4, 3))
            .unadjustedStartDate(date(2016, 4, 3))
            .endDate(date(2016, 5, 3))
            .unadjustedEndDate(date(2016, 5, 3))
            .yearFraction(ACT_360.yearFraction(date(2016, 4, 3), date(2016, 5, 3)))
            .rateComputation(FixedRateComputation.of(RATE))
            .build())
        .dayCount(ACT_360)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    ResolvedSwap expected = ResolvedSwap.builder()
        .legs(ResolvedSwapLeg.builder()
            .paymentPeriods(pp1, pp2, pp3, pp4)
            .payReceive(RECEIVE)
            .type(FIXED)
            .build())
        .build();
    assertEqualsBean(test.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    coverImmutableBean(test);
    Swap test2 = Swap.of(MOCK_GBP1);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertSerialization(test);
  }

}
