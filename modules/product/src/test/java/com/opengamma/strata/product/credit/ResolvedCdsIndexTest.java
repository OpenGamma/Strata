/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.credit.PaymentOnDefault.ACCRUED_PREMIUM;
import static com.opengamma.strata.product.credit.ProtectionStartOfDay.BEGINNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.product.common.BuySell;

/**
 * Test {@link ResolvedCdsIndex}.
 */
public class ResolvedCdsIndexTest {
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
  private static final BusinessDayAdjustment BUSS_ADJ = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);

  private static final List<CreditCouponPaymentPeriod> PAYMENTS = new ArrayList<CreditCouponPaymentPeriod>();
  static {
    int nDates = 44;
    LocalDate[] dates = new LocalDate[nDates];
    for (int i = 0; i < nDates; ++i) {
      dates[i] = START_DATE.plusMonths(3 * i);
    }
    for (int i = 0; i < nDates - 2; ++i) {
      LocalDate start = i == 0 ? dates[i] : BUSS_ADJ.adjust(dates[i], REF_DATA);
      LocalDate end = BUSS_ADJ.adjust(dates[i + 1], REF_DATA);
      PAYMENTS.add(CreditCouponPaymentPeriod.builder()
          .startDate(start)
          .endDate(end)
          .effectiveStartDate(start.minusDays(1))
          .effectiveEndDate(end.minusDays(1))
          .paymentDate(end).currency(USD)
          .notional(NOTIONAL)
          .fixedRate(COUPON)
          .yearFraction(ACT_360.relativeYearFraction(start, end))
          .build());
    }
    LocalDate start = BUSS_ADJ.adjust(dates[nDates - 2], REF_DATA);
    LocalDate end = dates[nDates - 1];
    PAYMENTS.add(CreditCouponPaymentPeriod.builder()
        .startDate(start)
        .endDate(end.plusDays(1))
        .effectiveStartDate(start.minusDays(1))
        .effectiveEndDate(end)
        .paymentDate(BUSS_ADJ.adjust(end, REF_DATA))
        .currency(USD)
        .notional(NOTIONAL)
        .fixedRate(COUPON)
        .yearFraction(ACT_360.relativeYearFraction(start, end.plusDays(1)))
        .build());
  }

  @Test
  public void test_builder() {
    ResolvedCdsIndex test = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertThat(test.getBuySell()).isEqualTo(BUY);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getAccrualStartDate()).isEqualTo(PAYMENTS.get(0).getStartDate());
    assertThat(test.getAccrualEndDate()).isEqualTo(PAYMENTS.get(42).getEndDate());
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getFixedRate()).isEqualTo(COUPON);
    assertThat(test.getCdsIndexId()).isEqualTo(INDEX_ID);
    assertThat(test.getLegalEntityIds()).isEqualTo(LEGAL_ENTITIES);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getPaymentOnDefault()).isEqualTo(ACCRUED_PREMIUM);
    assertThat(test.getPaymentPeriods()).isEqualTo(PAYMENTS);
    assertThat(test.getProtectionEndDate()).isEqualTo(PAYMENTS.get(42).getEffectiveEndDate());
    assertThat(test.getSettlementDateOffset()).isEqualTo(SETTLE_DAY_ADJ);
    assertThat(test.getProtectionStart()).isEqualTo(BEGINNING);
    assertThat(test.getStepinDateOffset()).isEqualTo(STEPIN_DAY_ADJ);
  }

  @Test
  public void test_accruedYearFraction() {
    double eps = 1.0e-15;
    ResolvedCdsIndex test = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    double accStart = test.accruedYearFraction(START_DATE.minusDays(1));
    double accNextMinusOne = test.accruedYearFraction(START_DATE.plusMonths(3).minusDays(1));
    double accNext = test.accruedYearFraction(START_DATE.plusMonths(3));
    double accNextOne = test.accruedYearFraction(START_DATE.plusMonths(3).plusDays(1));
    double accMod = test.accruedYearFraction(START_DATE.plusYears(1));
    double accEnd = test.accruedYearFraction(END_DATE);
    double accEndOne = test.accruedYearFraction(END_DATE.plusDays(1));
    assertThat(accStart).isEqualTo(0d);
    assertThat(accNext).isEqualTo(0d);
    assertThat(accNextMinusOne)
        .isCloseTo(ACT_360.relativeYearFraction(START_DATE, START_DATE.plusMonths(3).minusDays(1)), offset(eps));
    assertThat(accNextOne).isCloseTo(1d / 360d, offset(eps));
    // 2.x
    assertThat(accMod).isCloseTo(0.24722222222222223, offset(eps));
    assertThat(accEnd).isCloseTo(0.25555555555555554, offset(eps));
    assertThat(accEndOne).isCloseTo(0.25833333333333336, offset(eps));
  }

  @Test
  public void test_effectiveStartDate() {
    ResolvedCdsIndex test1 = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    LocalDate date1 = LocalDate.of(2016, 3, 22);
    assertThat(test1.calculateEffectiveStartDate(date1)).isEqualTo(date1.minusDays(1));
    LocalDate date2 = LocalDate.of(2013, 9, 22);
    assertThat(test1.calculateEffectiveStartDate(date2)).isEqualTo(START_DATE.minusDays(1));
    ResolvedCdsIndex test2 = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(ProtectionStartOfDay.NONE)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    LocalDate date3 = LocalDate.of(2016, 3, 22);
    assertThat(test2.calculateEffectiveStartDate(date3)).isEqualTo(date3);
    LocalDate date4 = LocalDate.of(2013, 9, 22);
    assertThat(test2.calculateEffectiveStartDate(date4)).isEqualTo(START_DATE);
  }

  @Test
  public void test_totoSingleNameCds() {
    ResolvedCdsIndex base = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    ResolvedCds test = base.toSingleNameCds();
    ResolvedCds expected = ResolvedCds.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .legalEntityId(INDEX_ID)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedCdsIndex test1 = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    coverImmutableBean(test1);
    ResolvedCdsIndex test2 = ResolvedCdsIndex.builder()
        .buySell(BuySell.SELL)
        .dayCount(DayCounts.ACT_365F)
        .cdsIndexId(StandardId.of("OG", "AA-INDEX"))
        .legalEntityIds(ImmutableList.of(StandardId.of("OG", "ABC1"), StandardId.of("OG", "ABC2")))
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .paymentPeriods(PAYMENTS.get(0))
        .protectionEndDate(PAYMENTS.get(0).getEffectiveEndDate())
        .settlementDateOffset(DaysAdjustment.NONE)
        .stepinDateOffset(DaysAdjustment.NONE)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedCdsIndex test = ResolvedCdsIndex.builder()
        .buySell(BUY)
        .dayCount(ACT_360)
        .cdsIndexId(INDEX_ID)
        .legalEntityIds(LEGAL_ENTITIES)
        .paymentOnDefault(ACCRUED_PREMIUM)
        .protectionStart(BEGINNING)
        .paymentPeriods(PAYMENTS)
        .protectionEndDate(PAYMENTS.get(PAYMENTS.size() - 1).getEffectiveEndDate())
        .settlementDateOffset(SETTLE_DAY_ADJ)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .build();
    assertSerialization(test);
  }

}
