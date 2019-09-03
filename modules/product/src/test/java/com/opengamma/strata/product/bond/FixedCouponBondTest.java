/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link FixedCouponBond}.
 */
public class FixedCouponBondTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION = FixedCouponBondYieldConvention.DE_BONDS;
  private static final LegalEntityId LEGAL_ENTITY = LegalEntityId.of("OG-Ticker", "BUN EUR");
  private static final double NOTIONAL = 1.0e7;
  private static final double FIXED_RATE = 0.015;
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(3, EUTA);
  private static final DayCount DAY_COUNT = DayCounts.ACT_365F;
  private static final LocalDate START_DATE = LocalDate.of(2015, 4, 12);
  private static final LocalDate END_DATE = LocalDate.of(2025, 4, 12);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Bond");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Bond2");
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA);
  private static final PeriodicSchedule PERIOD_SCHEDULE = PeriodicSchedule.of(
      START_DATE, END_DATE, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
  private static final int EX_COUPON_DAYS = 5;
  private static final DaysAdjustment EX_COUPON =
      DaysAdjustment.ofBusinessDays(-EX_COUPON_DAYS, EUTA, BUSINESS_ADJUST);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    FixedCouponBond test = sut();
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getDayCount()).isEqualTo(DAY_COUNT);
    assertThat(test.getFixedRate()).isEqualTo(FIXED_RATE);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getAccrualSchedule()).isEqualTo(PERIOD_SCHEDULE);
    assertThat(test.getSettlementDateOffset()).isEqualTo(DATE_OFFSET);
    assertThat(test.getYieldConvention()).isEqualTo(YIELD_CONVENTION);
    assertThat(test.getExCouponPeriod()).isEqualTo(EX_COUPON);
  }

  @Test
  public void test_builder_fail() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBond.builder()
            .securityId(SECURITY_ID)
            .dayCount(DAY_COUNT)
            .fixedRate(FIXED_RATE)
            .legalEntityId(LEGAL_ENTITY)
            .currency(EUR)
            .notional(NOTIONAL)
            .accrualSchedule(PERIOD_SCHEDULE)
            .settlementDateOffset(DATE_OFFSET)
            .yieldConvention(YIELD_CONVENTION)
            .exCouponPeriod(DaysAdjustment.ofBusinessDays(EX_COUPON_DAYS, EUTA, BUSINESS_ADJUST))
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBond.builder()
            .securityId(SECURITY_ID)
            .dayCount(DAY_COUNT)
            .fixedRate(FIXED_RATE)
            .legalEntityId(LEGAL_ENTITY)
            .currency(EUR)
            .notional(NOTIONAL)
            .accrualSchedule(PERIOD_SCHEDULE)
            .settlementDateOffset(DaysAdjustment.ofBusinessDays(-3, EUTA))
            .yieldConvention(YIELD_CONVENTION)
            .build());
  }

  @Test
  public void test_resolve() {
    FixedCouponBond base = sut();
    ResolvedFixedCouponBond resolved = base.resolve(REF_DATA);
    assertThat(resolved.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(resolved.getSettlementDateOffset()).isEqualTo(DATE_OFFSET);
    assertThat(resolved.getYieldConvention()).isEqualTo(YIELD_CONVENTION);
    ImmutableList<FixedCouponBondPaymentPeriod> periodicPayments = resolved.getPeriodicPayments();
    int expNum = 20;
    assertThat(periodicPayments).hasSize(expNum);
    LocalDate unadjustedEnd = END_DATE;
    Schedule unadjusted = PERIOD_SCHEDULE.createSchedule(REF_DATA).toUnadjusted();
    for (int i = 0; i < expNum; ++i) {
      FixedCouponBondPaymentPeriod payment = periodicPayments.get(expNum - 1 - i);
      assertThat(payment.getCurrency()).isEqualTo(EUR);
      assertThat(payment.getNotional()).isEqualTo(NOTIONAL);
      assertThat(payment.getFixedRate()).isEqualTo(FIXED_RATE);
      assertThat(payment.getUnadjustedEndDate()).isEqualTo(unadjustedEnd);
      assertThat(payment.getEndDate()).isEqualTo(BUSINESS_ADJUST.adjust(unadjustedEnd, REF_DATA));
      assertThat(payment.getPaymentDate()).isEqualTo(payment.getEndDate());
      LocalDate unadjustedStart = unadjustedEnd.minusMonths(6);
      assertThat(payment.getUnadjustedStartDate()).isEqualTo(unadjustedStart);
      assertThat(payment.getStartDate()).isEqualTo(BUSINESS_ADJUST.adjust(unadjustedStart, REF_DATA));
      assertThat(payment.getYearFraction()).isEqualTo(unadjusted.getPeriod(expNum - 1 - i).yearFraction(DAY_COUNT, unadjusted));
      assertThat(payment.getDetachmentDate()).isEqualTo(EX_COUPON.adjust(payment.getPaymentDate(), REF_DATA));
      unadjustedEnd = unadjustedStart;
    }
    Payment expectedPayment = Payment.of(CurrencyAmount.of(EUR, NOTIONAL), BUSINESS_ADJUST.adjust(END_DATE, REF_DATA));
    assertThat(resolved.getNominalPayment()).isEqualTo(expectedPayment);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FixedCouponBond sut() {
    return FixedCouponBond.builder()
        .securityId(SECURITY_ID)
        .dayCount(DAY_COUNT)
        .fixedRate(FIXED_RATE)
        .legalEntityId(LEGAL_ENTITY)
        .currency(EUR)
        .notional(NOTIONAL)
        .accrualSchedule(PERIOD_SCHEDULE)
        .settlementDateOffset(DATE_OFFSET)
        .yieldConvention(YIELD_CONVENTION)
        .exCouponPeriod(EX_COUPON)
        .build();
  }

  static FixedCouponBond sut2() {
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
    PeriodicSchedule sche = PeriodicSchedule.of(
        START_DATE, END_DATE, Frequency.P12M, adj, StubConvention.SHORT_INITIAL, true);
    return FixedCouponBond.builder()
        .securityId(SECURITY_ID2)
        .dayCount(DayCounts.ACT_360)
        .fixedRate(0.005)
        .legalEntityId(LegalEntityId.of("OG-Ticker", "BUN EUR 2"))
        .currency(GBP)
        .notional(1.0e6)
        .accrualSchedule(sche)
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, SAT_SUN))
        .yieldConvention(FixedCouponBondYieldConvention.GB_BUMP_DMO)
        .build();
  }

}
