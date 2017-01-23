/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.credit.PaymentOnDefault.ACCRUED_PREMIUM;
import static com.opengamma.strata.product.credit.ProtectionStartOfDay.BEGINNING;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Test {@link Cds}.
 */
@Test
public class CdsTest {
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final DaysAdjustment SETTLE_DAY_ADJ = DaysAdjustment.ofBusinessDays(3, CALENDAR);
  private static final DaysAdjustment STEPIN_DAY_ADJ = DaysAdjustment.ofCalendarDays(1);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2024, 9, 20);
  private static final Cds PRODUCT_STD = Cds.of(
      BUY, LEGAL_ENTITY, USD, NOTIONAL, START_DATE, END_DATE, P3M, SAT_SUN, COUPON);

  public void test_builder() {
    LocalDate startDate = LocalDate.of(2014, 12, 20);
    LocalDate endDate = LocalDate.of(2020, 10, 20);
    PeriodicSchedule sch =
        PeriodicSchedule.of(startDate, endDate, P3M, BusinessDayAdjustment.NONE, SHORT_INITIAL, RollConventions.NONE);
    Cds test = Cds.builder()
        .paymentSchedule(sch)
        .buySell(SELL)
        .currency(JPY)
        .dayCount(ACT_365F)
        .fixedRate(COUPON)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertEquals(test.getPaymentSchedule(), sch);
    assertEquals(test.getBuySell(), SELL);
    assertEquals(test.getCurrency(), JPY);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getFixedRate(), COUPON);
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getPaymentOnDefault(), PaymentOnDefault.NONE);
    assertEquals(test.getProtectionStart(), ProtectionStartOfDay.NONE);
    assertEquals(test.getSettlementDateOffset(), SETTLE_DAY_ADJ);
    assertEquals(test.getStepinDateOffset(), STEPIN_DAY_ADJ);
  }

  public void test_of() {
    BusinessDayAdjustment bussAdj = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
    PeriodicSchedule expected = PeriodicSchedule.builder()
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(bussAdj)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .frequency(P3M)
        .rollConvention(RollConventions.NONE)
        .stubConvention(SHORT_INITIAL)
        .build();
    assertEquals(PRODUCT_STD.getPaymentSchedule(), expected);
    assertEquals(PRODUCT_STD.getBuySell(), BUY);
    assertEquals(PRODUCT_STD.getCurrency(), USD);
    assertEquals(PRODUCT_STD.getDayCount(), ACT_360);
    assertEquals(PRODUCT_STD.getFixedRate(), COUPON);
    assertEquals(PRODUCT_STD.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(PRODUCT_STD.getNotional(), NOTIONAL);
    assertEquals(PRODUCT_STD.getPaymentOnDefault(), ACCRUED_PREMIUM);
    assertEquals(PRODUCT_STD.getProtectionStart(), BEGINNING);
    assertEquals(PRODUCT_STD.getSettlementDateOffset(), SETTLE_DAY_ADJ);
    assertEquals(PRODUCT_STD.getStepinDateOffset(), STEPIN_DAY_ADJ);
    Cds test = Cds.of(BUY, LEGAL_ENTITY, USD, NOTIONAL, START_DATE, END_DATE, P3M, SAT_SUN, COUPON);
    assertEquals(test, PRODUCT_STD);
  }

  public void test_resolve() {
    BusinessDayAdjustment bussAdj = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
    ResolvedCds test = PRODUCT_STD.resolve(REF_DATA);
    int nDates = 44;
    LocalDate[] dates = new LocalDate[nDates];
    for (int i = 0; i < nDates; ++i) {
      dates[i] = START_DATE.plusMonths(3 * i);
    }
    List<CreditCouponPaymentPeriod> payments = new ArrayList<>(nDates - 1);
    for (int i = 0; i < nDates - 2; ++i) {
      LocalDate start = i == 0 ? dates[i] : bussAdj.adjust(dates[i], REF_DATA);
      LocalDate end = bussAdj.adjust(dates[i + 1], REF_DATA);
      payments.add(CreditCouponPaymentPeriod.builder()
          .startDate(start)
          .endDate(end)
          .unadjustedStartDate(dates[i])
          .unadjustedEndDate(dates[i + 1])
          .effectiveStartDate(start.minusDays(1))
          .effectiveEndDate(end.minusDays(1))
          .paymentDate(end).currency(USD)
          .notional(NOTIONAL)
          .fixedRate(COUPON)
          .yearFraction(ACT_360.relativeYearFraction(start, end))
          .build());
    }
    LocalDate start = bussAdj.adjust(dates[nDates - 2], REF_DATA);
    LocalDate end = dates[nDates - 1];
    payments.add(CreditCouponPaymentPeriod.builder()
        .startDate(start)
        .endDate(end.plusDays(1))
        .unadjustedStartDate(dates[nDates - 2])
        .unadjustedEndDate(end)
        .effectiveStartDate(start.minusDays(1))
        .effectiveEndDate(end)
        .paymentDate(bussAdj.adjust(end, REF_DATA))
        .currency(USD)
        .notional(NOTIONAL)
        .fixedRate(COUPON)
        .yearFraction(ACT_360.relativeYearFraction(start, end.plusDays(1)))
        .build());
    ResolvedCds expected = ResolvedCds.builder()
        .buySell(BUY)
        .legalEntityId(LEGAL_ENTITY)
        .dayCount(ACT_360)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .paymentPeriods(payments)
        .protectionStart(BEGINNING)
        .protectionEndDate(END_DATE)
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PRODUCT_STD);
    Cds other = Cds.builder()
        .buySell(SELL)
        .legalEntityId(StandardId.of("OG", "EFG"))
        .currency(JPY)
        .notional(1d)
        .fixedRate(0.01)
        .dayCount(ACT_365F)
        .paymentSchedule(
            PeriodicSchedule.builder()
                .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, JPTO))
                .startDate(LocalDate.of(2014, 1, 4))
                .endDate(LocalDate.of(2020, 11, 20))
                .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                .frequency(P6M)
                .rollConvention(RollConventions.NONE)
                .stubConvention(StubConvention.SHORT_FINAL)
                .build())
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .stepinDateOffset(DaysAdjustment.NONE)
        .settlementDateOffset(DaysAdjustment.NONE)
        .build();
    coverBeanEquals(PRODUCT_STD, other);
  }

  public void test_serialization() {
    assertSerialization(PRODUCT_STD);
  }

}
