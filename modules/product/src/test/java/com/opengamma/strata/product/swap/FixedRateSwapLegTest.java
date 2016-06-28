/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;

/**
 * Test.
 */
@Test
public class FixedRateSwapLegTest {

  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
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
    FixedRateCalculation rateCalc = FixedRateCalculation.builder()
        .dayCount(DayCounts.ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    FixedRateSwapLeg test = FixedRateSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    assertEquals(test.getStartDate(), AdjustableDate.of(DATE_01_05, bda));
    assertEquals(test.getEndDate(), AdjustableDate.of(DATE_04_05, bda));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPayReceive(), PAY);
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getNotionalSchedule(), notionalSchedule);
    assertEquals(test.getCalculation(), rateCalc);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedRateSwapLeg test = FixedRateSwapLeg.builder()
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
    FixedRateSwapLeg test2 = FixedRateSwapLeg.builder()
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
            .rate(ValueSchedule.of(0.026d))
            .build())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FixedRateSwapLeg test = FixedRateSwapLeg.builder()
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
