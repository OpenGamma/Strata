/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;

/**
 * Test {@link CdsConvention}.
 */
@Test
public class CdsConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DaysAdjustment SETTLE_DAY_ADJ = DaysAdjustment.ofBusinessDays(3, GBLO);
  private static final DaysAdjustment SETTLE_DAY_ADJ_STD = DaysAdjustment.ofBusinessDays(3, SAT_SUN);
  private static final DaysAdjustment STEPIN_DAY_ADJ = DaysAdjustment.ofCalendarDays(1);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;

  private static final BusinessDayAdjustment BUSI_ADJ = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BUSI_ADJ_STD = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
  private static final String NAME = "GB_CDS";

  public void test_of() {
    ImmutableCdsConvention test = ImmutableCdsConvention.of(NAME, GBP, ACT_365F, P3M, BUSI_ADJ, SETTLE_DAY_ADJ);
    assertEquals(test.getBusinessDayAdjustment(), BUSI_ADJ);
    assertEquals(test.getStartDateBusinessDayAdjustment(), BUSI_ADJ);
    assertEquals(test.getEndDateBusinessDayAdjustment(), BusinessDayAdjustment.NONE);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getPaymentFrequency(), P3M);
    assertEquals(test.getPaymentOnDefault(), PaymentOnDefault.ACCRUED_PREMIUM);
    assertEquals(test.getProtectionStart(), ProtectionStartOfDay.BEGINNING);
    assertEquals(test.getRollConvention(), RollConventions.DAY_20);
    assertEquals(test.getSettlementDateOffset(), SETTLE_DAY_ADJ);
    assertEquals(test.getStepinDateOffset(), DaysAdjustment.ofCalendarDays(1));
    assertEquals(test.getStubConvention(), StubConvention.SHORT_INITIAL);
  }

  public void test_builder() {
    ImmutableCdsConvention test = ImmutableCdsConvention.builder()
        .businessDayAdjustment(BUSI_ADJ)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .endDateBusinessDayAdjustment(BUSI_ADJ)
        .currency(GBP)
        .dayCount(ACT_365F)
        .name(NAME)
        .paymentFrequency(P6M)
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .rollConvention(RollConventions.NONE)
        .settlementDateOffset(DaysAdjustment.ofCalendarDays(7))
        .stepinDateOffset(DaysAdjustment.NONE)
        .stubConvention(StubConvention.LONG_INITIAL)
        .build();
    assertEquals(test.getBusinessDayAdjustment(), BUSI_ADJ);
    assertEquals(test.getStartDateBusinessDayAdjustment(), BusinessDayAdjustment.NONE);
    assertEquals(test.getEndDateBusinessDayAdjustment(), BUSI_ADJ);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getPaymentFrequency(), P6M);
    assertEquals(test.getPaymentOnDefault(), PaymentOnDefault.NONE);
    assertEquals(test.getProtectionStart(), ProtectionStartOfDay.NONE);
    assertEquals(test.getRollConvention(), RollConventions.NONE);
    assertEquals(test.getSettlementDateOffset(), DaysAdjustment.ofCalendarDays(7));
    assertEquals(test.getStepinDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getStubConvention(), StubConvention.LONG_INITIAL);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade() {
    LocalDate tradeDate = LocalDate.of(2015, 12, 21); // 19, 20 weekend
    LocalDate startDate = LocalDate.of(2015, 12, 20);
    LocalDate endDate = LocalDate.of(2020, 12, 20);
    LocalDate settlementDate = LocalDate.of(2015, 12, 24);
    TradeInfo info = TradeInfo.builder().tradeDate(tradeDate).settlementDate(settlementDate).build();
    Tenor tenor = Tenor.TENOR_5Y;
    ImmutableCdsConvention base = ImmutableCdsConvention.of(NAME, GBP, ACT_360, P3M, BUSI_ADJ_STD, SETTLE_DAY_ADJ_STD);
    Cds product = Cds.builder()
        .legalEntityId(LEGAL_ENTITY)
        .paymentSchedule(
            PeriodicSchedule.builder()
                .startDate(startDate)
                .endDate(endDate)
                .frequency(P3M)
                .businessDayAdjustment(BUSI_ADJ_STD)
                .startDateBusinessDayAdjustment(BUSI_ADJ_STD)
                .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                .stubConvention(StubConvention.SHORT_INITIAL)
                .rollConvention(RollConventions.DAY_20)
                .build())
        .buySell(BUY)
        .currency(GBP)
        .dayCount(ACT_360)
        .notional(NOTIONAL)
        .fixedRate(COUPON)
        .paymentOnDefault(PaymentOnDefault.ACCRUED_PREMIUM)
        .protectionStart(ProtectionStartOfDay.BEGINNING)
        .stepinDateOffset(STEPIN_DAY_ADJ)
        .settlementDateOffset(SETTLE_DAY_ADJ_STD)
        .build();
    CdsTrade expected = CdsTrade.builder()
        .info(info)
        .product(product)
        .build();
    CdsTrade test1 = base.createTrade(LEGAL_ENTITY, tradeDate, tenor, BUY, NOTIONAL, COUPON, REF_DATA);
    assertEquals(test1, expected);
    CdsTrade test2 = base.createTrade(LEGAL_ENTITY, tradeDate, startDate, tenor, BUY, NOTIONAL, COUPON, REF_DATA);
    assertEquals(test2, expected);
    CdsTrade test3 = base.createTrade(LEGAL_ENTITY, tradeDate, startDate, endDate, BUY, NOTIONAL, COUPON, REF_DATA);
    assertEquals(test3, expected);
    CdsTrade test4 = base.toTrade(LEGAL_ENTITY, info, startDate, endDate, BUY, NOTIONAL, COUPON);
    assertEquals(test4, expected);

    AdjustablePayment upfront = AdjustablePayment.of(CurrencyAmount.of(GBP, 0.1 * NOTIONAL), settlementDate);
    CdsTrade expectedWithUf = CdsTrade.builder()
        .info(info)
        .product(product)
        .upfrontFee(upfront)
        .build();
    CdsTrade test5 = base.createTrade(LEGAL_ENTITY, tradeDate, tenor, BUY, NOTIONAL, COUPON, upfront, REF_DATA);
    assertEquals(test5, expectedWithUf);
    CdsTrade test6 = base.createTrade(LEGAL_ENTITY, tradeDate, startDate, tenor, BUY, NOTIONAL, COUPON, upfront, REF_DATA);
    assertEquals(test6, expectedWithUf);
    CdsTrade test7 = base.createTrade(LEGAL_ENTITY, tradeDate, startDate, endDate, BUY, NOTIONAL, COUPON, upfront, REF_DATA);
    assertEquals(test7, expectedWithUf);
    CdsTrade test8 = base.toTrade(LEGAL_ENTITY, info, startDate, endDate, BUY, NOTIONAL, COUPON, upfront);
    assertEquals(test8, expectedWithUf);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {CdsConventions.USD_STANDARD, "USD-STANDARD"},
        {CdsConventions.JPY_US_GB_STANDARD, "JPY-US-GB-STANDARD"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(CdsConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(CdsConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CdsConvention convention, String name) {
    assertEquals(CdsConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(CdsConvention convention, String name) {
    CdsConvention.of(name);  // ensures map is populated
    ImmutableMap<String, CdsConvention> map = CdsConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> CdsConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> CdsConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableCdsConvention test1 = ImmutableCdsConvention.of(NAME, GBP, ACT_360, P3M, BUSI_ADJ_STD, SETTLE_DAY_ADJ_STD);
    coverImmutableBean(test1);
    ImmutableCdsConvention test2 = ImmutableCdsConvention.builder()
        .businessDayAdjustment(BUSI_ADJ)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .endDateBusinessDayAdjustment(BUSI_ADJ)
        .currency(USD)
        .dayCount(ACT_365F)
        .name("another")
        .paymentFrequency(P6M)
        .paymentOnDefault(PaymentOnDefault.NONE)
        .protectionStart(ProtectionStartOfDay.NONE)
        .rollConvention(RollConventions.NONE)
        .settlementDateOffset(DaysAdjustment.ofCalendarDays(7))
        .stepinDateOffset(DaysAdjustment.NONE)
        .stubConvention(StubConvention.LONG_INITIAL)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ImmutableCdsConvention test = ImmutableCdsConvention.of(NAME, GBP, ACT_360, P3M, BUSI_ADJ_STD, SETTLE_DAY_ADJ_STD);
    assertSerialization(test);
  }

}
