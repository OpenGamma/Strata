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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.GB_IL_FLOAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;

/**
 * Test {@link CapitalIndexedBondSecurity}.
 */
public class CapitalIndexedBondSecurityTest {

  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBondTest.sut();
  private static final CapitalIndexedBond PRODUCT2 = CapitalIndexedBondTest.sut2();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(PRODUCT.getSecurityId(), PRICE_INFO);
  private static final CapitalIndexedBondYieldConvention YIELD_CONVENTION = GB_IL_FLOAT;
  private static final LegalEntityId LEGAL_ENTITY = LegalEntityId.of("OG-Ticker", "BUN EUR");
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
  @Test
  public void test_builder() {
    CapitalIndexedBondSecurity test = sut();
    assertThat(test.getInfo()).isEqualTo(INFO);
    assertThat(test.getSecurityId()).isEqualTo(PRODUCT.getSecurityId());
    assertThat(test.getCurrency()).isEqualTo(PRODUCT.getCurrency());
    assertThat(test.getUnderlyingIds()).isEmpty();
    assertThat(test.getFirstIndexValue()).isEqualTo(PRODUCT.getFirstIndexValue());
  }

  @Test
  public void test_builder_fail() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondSecurity.builder()
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondSecurity.builder()
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
  @Test
  public void test_createProduct() {
    CapitalIndexedBondSecurity test = sut();
    assertThat(test.createProduct(ReferenceData.empty())).isEqualTo(PRODUCT);
    TradeInfo tradeInfo = TradeInfo.builder().tradeDate(date(2016, 6, 30)).settlementDate(date(2016, 7, 1)).build();
    CapitalIndexedBondTrade expectedTrade = CapitalIndexedBondTrade.builder()
        .info(tradeInfo)
        .product(PRODUCT)
        .quantity(100)
        .price(123.50)
        .build();
    assertThat(test.createTrade(tradeInfo, 100, 123.50, ReferenceData.empty())).isEqualTo(expectedTrade);
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
