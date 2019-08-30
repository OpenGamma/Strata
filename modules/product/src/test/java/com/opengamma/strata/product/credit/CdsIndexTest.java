/*
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
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.credit.PaymentOnDefault.ACCRUED_PREMIUM;
import static com.opengamma.strata.product.credit.ProtectionStartOfDay.BEGINNING;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
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
 * Test {@link CdsIndex}.
 */
public class CdsIndexTest {
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final DaysAdjustment SETTLE_DAY_ADJ = DaysAdjustment.ofBusinessDays(3, CALENDAR);
  private static final DaysAdjustment STEPIN_DAY_ADJ = DaysAdjustment.ofCalendarDays(1);
  private static final StandardId INDEX_ID = StandardId.of("OG", "AA-INDEX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES = ImmutableList.of(
      StandardId.of("OG", "ABC1"), StandardId.of("OG", "ABC2"), StandardId.of("OG", "ABC3"), StandardId.of("OG", "ABC4"));
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2024, 9, 20);
  private static final CdsIndex PRODUCT = CdsIndex.of(
      BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START_DATE, END_DATE, P3M, SAT_SUN, COUPON);

  @Test
  public void test_builder() {
    LocalDate startDate = LocalDate.of(2014, 12, 20);
    LocalDate endDate = LocalDate.of(2020, 10, 20);
    PeriodicSchedule sch =
        PeriodicSchedule.of(startDate, endDate, P3M, BusinessDayAdjustment.NONE, SHORT_INITIAL, RollConventions.NONE);
    CdsIndex test = CdsIndex.builder()
        .paymentSchedule(sch)
        .buySell(SELL)
        .currency(JPY)
        .dayCount(ACT_365F)
        .fixedRate(COUPON)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .notional(NOTIONAL)
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertThat(test.getPaymentSchedule()).isEqualTo(sch);
    assertThat(test.getBuySell()).isEqualTo(SELL);
    assertThat(test.getCurrency()).isEqualTo(JPY);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getFixedRate()).isEqualTo(COUPON);
    assertThat(test.getCdsIndexId()).isEqualTo(INDEX_ID);
    assertThat(test.getLegalEntityIds()).isEqualTo(LEGAL_ENTITIES);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getPaymentOnDefault()).isEqualTo(PaymentOnDefault.NONE);
    assertThat(test.getProtectionStart()).isEqualTo(ProtectionStartOfDay.NONE);
    assertThat(test.getSettlementDateOffset()).isEqualTo(SETTLE_DAY_ADJ);
    assertThat(test.getStepinDateOffset()).isEqualTo(STEPIN_DAY_ADJ);
    assertThat(test.isCrossCurrency()).isFalse();
    assertThat(test.allPaymentCurrencies()).containsOnly(JPY);
    assertThat(test.allCurrencies()).containsOnly(JPY);
  }

  @Test
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
        .stubConvention(SMART_INITIAL)
        .build();
    assertThat(PRODUCT.getPaymentSchedule()).isEqualTo(expected);
    assertThat(PRODUCT.getBuySell()).isEqualTo(BUY);
    assertThat(PRODUCT.getCurrency()).isEqualTo(USD);
    assertThat(PRODUCT.getDayCount()).isEqualTo(ACT_360);
    assertThat(PRODUCT.getFixedRate()).isEqualTo(COUPON);
    assertThat(PRODUCT.getCdsIndexId()).isEqualTo(INDEX_ID);
    assertThat(PRODUCT.getLegalEntityIds()).isEqualTo(LEGAL_ENTITIES);
    assertThat(PRODUCT.getNotional()).isEqualTo(NOTIONAL);
    assertThat(PRODUCT.getPaymentOnDefault()).isEqualTo(ACCRUED_PREMIUM);
    assertThat(PRODUCT.getProtectionStart()).isEqualTo(BEGINNING);
    assertThat(PRODUCT.getSettlementDateOffset()).isEqualTo(SETTLE_DAY_ADJ);
    assertThat(PRODUCT.getStepinDateOffset()).isEqualTo(STEPIN_DAY_ADJ);
    CdsIndex test = CdsIndex.of(BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START_DATE, END_DATE, P3M, SAT_SUN, COUPON);
    assertThat(test).isEqualTo(PRODUCT);
  }

  @Test
  public void test_resolve() {
    BusinessDayAdjustment bussAdj = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
    ResolvedCdsIndex test = PRODUCT.resolve(REF_DATA);
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
    ResolvedCdsIndex expected = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .dayCount(ACT_360)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .paymentPeriods(payments)
        .protectionStart(BEGINNING)
        .protectionEndDate(END_DATE)
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(PRODUCT);
    CdsIndex other = CdsIndex.builder()
        .buySell(SELL)
        .cdsIndexId(StandardId.of("OG", "AA-INDEX"))
        .legalEntityIds(ImmutableList.of(StandardId.of("OG", "ABC1"), StandardId.of("OG", "ABC2")))
        .currency(JPY)
        .notional(1d)
        .paymentSchedule(
            PeriodicSchedule.of(
                LocalDate.of(2014, 1, 4),
                LocalDate.of(2020, 11, 20),
                P6M,
                BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, JPTO),
                StubConvention.SHORT_FINAL,
                RollConventions.NONE))
        .fixedRate(0.01)
        .dayCount(ACT_365F)
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .settlementDateOffset(DaysAdjustment.NONE)
        .stepinDateOffset(DaysAdjustment.NONE)
        .build();
    coverBeanEquals(PRODUCT, other);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PRODUCT);
  }

}
