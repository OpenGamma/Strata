/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;

/**
 * Test.
 */
@Test
public class KnownAmountSwapLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_02_07 = date(2014, 2, 7);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_03_07 = date(2014, 3, 7);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);
  private static final LocalDate DATE_04_09 = date(2014, 4, 9);
  private static final DaysAdjustment PLUS_THREE_DAYS = DaysAdjustment.ofBusinessDays(3, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
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
    ValueSchedule amountSchedule = ValueSchedule.of(123d);
    KnownAmountSwapLeg test = KnownAmountSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .amount(amountSchedule)
        .currency(GBP)
        .build();
    assertEquals(test.getPayReceive(), PAY);
    assertEquals(test.getStartDate(), AdjustableDate.of(DATE_01_05, bda));
    assertEquals(test.getEndDate(), AdjustableDate.of(DATE_04_05, bda));
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getAmount(), amountSchedule);
    assertEquals(test.getCurrency(), GBP);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    KnownAmountSwapLeg test = KnownAmountSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .build())
        .amount(ValueSchedule.of(123d))
        .currency(GBP)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of());
    assertEquals(test.allIndices(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    // test case
    KnownAmountSwapLeg test = KnownAmountSwapLeg.builder()
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
        .amount(ValueSchedule.builder()
            .initialValue(123d)
            .steps(ValueStep.of(1, ValueAdjustment.ofReplace(234d)))
            .build())
        .currency(GBP)
        .build();
    // expected
    KnownAmountSwapPaymentPeriod rpp1 = KnownAmountSwapPaymentPeriod.builder()
        .payment(Payment.ofPay(CurrencyAmount.of(GBP, 123d), DATE_02_07))
        .startDate(DATE_01_06)
        .endDate(DATE_02_05)
        .unadjustedStartDate(DATE_01_05)
        .build();
    KnownAmountSwapPaymentPeriod rpp2 = KnownAmountSwapPaymentPeriod.builder()
        .payment(Payment.ofPay(CurrencyAmount.of(GBP, 234d), DATE_03_07))
        .startDate(DATE_02_05)
        .endDate(DATE_03_05)
        .build();
    KnownAmountSwapPaymentPeriod rpp3 = KnownAmountSwapPaymentPeriod.builder()
        .payment(Payment.ofPay(CurrencyAmount.of(GBP, 234d), DATE_04_09))
        .startDate(DATE_03_05)
        .endDate(DATE_04_07)
        .unadjustedEndDate(DATE_04_05)
        .build();
    // assertion
    assertEquals(test.resolve(REF_DATA), ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(rpp1, rpp2, rpp3)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    KnownAmountSwapLeg test = KnownAmountSwapLeg.builder()
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
        .amount(ValueSchedule.of(123d))
        .currency(GBP)
        .build();
    coverImmutableBean(test);
    KnownAmountSwapLeg test2 = KnownAmountSwapLeg.builder()
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
        .amount(ValueSchedule.of(2000d))
        .currency(EUR)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    KnownAmountSwapLeg test = KnownAmountSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .build())
        .amount(ValueSchedule.of(123d))
        .currency(GBP)
        .build();
    assertSerialization(test);
  }

}
