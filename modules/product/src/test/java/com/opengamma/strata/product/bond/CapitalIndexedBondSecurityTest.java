/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.GB_IL_FLOAT;
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
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;

/**
 * Test {@link CapitalIndexedBondSecurity}.
 */
@Test
public class CapitalIndexedBondSecurityTest {

  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBondTest.sut();
  private static final CapitalIndexedBond PRODUCT2 = CapitalIndexedBondTest.sut2();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(PRODUCT.getSecurityId(), PRICE_INFO);
  private static final CapitalIndexedBondYieldConvention YIELD_CONVENTION = GB_IL_FLOAT;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "BUN EUR");
  private static final double NOTIONAL = 1.0e7;
  private static final InflationRateCalculation RATE =
      InflationRateCalculation.of(GB_HICP, 3, PriceIndexCalculationMethod.MONTHLY, 120d);
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
    CapitalIndexedBondSecurity test = sut();
    assertEquals(test.getInfo(), INFO);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of());
    assertEquals(test.getFirstIndexValue(), PRODUCT.getFirstIndexValue());
  }

  public void test_builder_fail() {
    assertThrowsIllegalArg(() -> CapitalIndexedBondSecurity.builder()
        .info(INFO)
        .dayCount(DAY_COUNT)
        .rateCalculation(RATE)
        .legalEntityId(LEGAL_ENTITY)
        .currency(EUR)
        .notional(NOTIONAL)
        .accrualSchedule(PERIOD_SCHEDULE)
        .settlementDateOffset(DATE_OFFSET)
        .yieldConvention(YIELD_CONVENTION)
        .exCouponPeriod(DaysAdjustment.ofBusinessDays(EX_COUPON_DAYS, EUTA, BUSINESS_ADJUST))
        .build());
    assertThrowsIllegalArg(() -> CapitalIndexedBondSecurity.builder()
        .info(INFO)
        .dayCount(DAY_COUNT)
        .rateCalculation(RATE)
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
    CapitalIndexedBondSecurity test = sut();
    assertEquals(test.createProduct(ReferenceData.empty()), PRODUCT);
    TradeInfo tradeInfo = TradeInfo.builder().tradeDate(date(2016, 6, 30)).settlementDate(date(2016, 7, 1)).build();
    CapitalIndexedBondTrade expectedTrade = CapitalIndexedBondTrade.builder()
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
  static CapitalIndexedBondSecurity sut() {
    return createSecurity(PRODUCT);
  }

  static CapitalIndexedBondSecurity sut2() {
    return createSecurity(PRODUCT2);
  }

  static CapitalIndexedBondSecurity createSecurity(CapitalIndexedBond product) {
    return CapitalIndexedBondSecurity.builder()
        .info(SecurityInfo.of(product.getSecurityId(), INFO.getPriceInfo()))
        .currency(product.getCurrency())
        .notional(product.getNotional())
        .accrualSchedule(product.getAccrualSchedule())
        .rateCalculation(product.getRateCalculation())
        .dayCount(product.getDayCount())
        .yieldConvention(product.getYieldConvention())
        .legalEntityId(product.getLegalEntityId())
        .settlementDateOffset(product.getSettlementDateOffset())
        .exCouponPeriod(product.getExCouponPeriod())
        .build();
  }

}
