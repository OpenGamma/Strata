/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FixedCouponBondSecurity}.
 */
@Test
public class FixedCouponBondSecurityTest {

  private static final FixedCouponBond PRODUCT = FixedCouponBondTest.sut();
  private static final FixedCouponBond PRODUCT2 = FixedCouponBondTest.sut2();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(PRODUCT.getSecurityId(), PRICE_INFO);
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION = FixedCouponBondYieldConvention.DE_BONDS;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "BUN EUR");
  private static final double NOTIONAL = 1.0e7;
  private static final double FIXED_RATE = 0.015;
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(3, EUTA);
  private static final DayCount DAY_COUNT = DayCounts.ACT_365F;
  private static final LocalDate START_DATE = LocalDate.of(2015, 4, 12);
  private static final LocalDate END_DATE = LocalDate.of(2025, 4, 12);
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA);
  private static final PeriodicSchedule PERIOD_SCHEDULE = PeriodicSchedule.of(
      START_DATE, END_DATE, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
  private static final int EX_COUPON_DAYS = 5;

  //-------------------------------------------------------------------------
  public void test_builder() {
    FixedCouponBondSecurity test = sut();
    assertEquals(test.getInfo(), INFO);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of());
  }

  public void test_builder_fail() {
    assertThrowsIllegalArg(() -> FixedCouponBondSecurity.builder()
        .info(INFO)
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
    assertThrowsIllegalArg(() -> FixedCouponBondSecurity.builder()
        .info(INFO)
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

  //-------------------------------------------------------------------------
  public void test_createProduct() {
    FixedCouponBondSecurity test = sut();
    assertEquals(test.createProduct(ReferenceData.empty()), PRODUCT);
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    FixedCouponBondTrade expectedTrade = FixedCouponBondTrade.builder()
        .info(tradeInfo)
        .product(PRODUCT)
        .quantity(100)
        .price(123.50)
        .build();
    assertEquals(test.createTrade(tradeInfo, 100, 123.50, ReferenceData.empty()), expectedTrade);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FixedCouponBondSecurity sut() {
    return createSecurity(PRODUCT);
  }

  static FixedCouponBondSecurity sut2() {
    return createSecurity(PRODUCT2);
  }

  static FixedCouponBondSecurity createSecurity(FixedCouponBond product) {
    return FixedCouponBondSecurity.builder()
        .info(SecurityInfo.of(product.getSecurityId(), INFO.getPriceInfo()))
        .currency(product.getCurrency())
        .notional(product.getNotional())
        .accrualSchedule(product.getAccrualSchedule())
        .fixedRate(product.getFixedRate())
        .dayCount(product.getDayCount())
        .yieldConvention(product.getYieldConvention())
        .legalEntityId(product.getLegalEntityId())
        .settlementDateOffset(product.getSettlementDateOffset())
        .exCouponPeriod(product.getExCouponPeriod())
        .build();
  }

}
