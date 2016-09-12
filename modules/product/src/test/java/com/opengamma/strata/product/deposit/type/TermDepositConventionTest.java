/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;

/**
 * Test {@link TermDepositConvention}.
 */
@Test
public class TermDepositConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA);

  //-------------------------------------------------------------------------
  public void test_builder_full() {
    ImmutableTermDepositConvention test = ImmutableTermDepositConvention.builder()
        .name("Test")
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    assertEquals(test.getName(), "Test");
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  public void test_of() {
    ImmutableTermDepositConvention test = ImmutableTermDepositConvention.of(
        "EUR-Deposit", EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    assertEquals(test.getName(), "EUR-Deposit");
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade() {
    TermDepositConvention convention = ImmutableTermDepositConvention.builder()
        .name("EUR-Dep")
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    Period period3M = Period.ofMonths(3);
    BuySell buy = BuySell.BUY;
    double notional = 2_000_000d;
    double rate = 0.0125;
    TermDepositTrade trade = convention.createTrade(tradeDate, period3M, buy, notional, rate, REF_DATA);
    LocalDate startDateExpected = PLUS_TWO_DAYS.adjust(tradeDate, REF_DATA);
    LocalDate endDateExpected = startDateExpected.plus(period3M);
    TermDeposit termDepositExpected = TermDeposit.builder()
        .buySell(buy)
        .currency(EUR)
        .notional(notional)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .rate(rate)
        .dayCount(ACT_360)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.of(tradeDate);
    assertEquals(trade.getProduct(), termDepositExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {TermDepositConventions.USD_DEPOSIT_T2, "USD-Deposit-T2"},
        {TermDepositConventions.EUR_DEPOSIT_T2, "EUR-Deposit-T2"},
        {TermDepositConventions.GBP_DEPOSIT_T0, "GBP-Deposit-T0"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(TermDepositConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(TermDepositConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(TermDepositConvention convention, String name) {
    assertEquals(TermDepositConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(TermDepositConvention convention, String name) {
    TermDepositConvention.of(name);  // ensures map is populated
    ImmutableMap<String, TermDepositConvention> map = TermDepositConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> TermDepositConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> TermDepositConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "spotAndConv")
  static Object[][] data_spotAndConv() {
    return new Object[][] {
        {TermDepositConventions.GBP_DEPOSIT_T0, 0, BusinessDayConventions.MODIFIED_FOLLOWING},
        {TermDepositConventions.GBP_SHORT_DEPOSIT_T0, 0, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.GBP_SHORT_DEPOSIT_T1, 1, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.EUR_DEPOSIT_T2, 2, BusinessDayConventions.MODIFIED_FOLLOWING},
        {TermDepositConventions.EUR_SHORT_DEPOSIT_T0, 0, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.EUR_SHORT_DEPOSIT_T1, 1, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.EUR_SHORT_DEPOSIT_T2, 2, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.USD_DEPOSIT_T2, 2, BusinessDayConventions.MODIFIED_FOLLOWING},
        {TermDepositConventions.USD_SHORT_DEPOSIT_T0, 0, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.USD_SHORT_DEPOSIT_T1, 1, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.USD_SHORT_DEPOSIT_T2, 2, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.CHF_DEPOSIT_T2, 2, BusinessDayConventions.MODIFIED_FOLLOWING},
        {TermDepositConventions.CHF_SHORT_DEPOSIT_T0, 0, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.CHF_SHORT_DEPOSIT_T1, 1, BusinessDayConventions.FOLLOWING},
        {TermDepositConventions.CHF_SHORT_DEPOSIT_T2, 2, BusinessDayConventions.FOLLOWING},
    };
  }

  @Test(dataProvider = "spotAndConv")
  public void test_spotAndConv(ImmutableTermDepositConvention convention, int spotT, BusinessDayConvention conv) {
    assertEquals(convention.getSpotDateOffset().getDays(), spotT);
    assertEquals(convention.getBusinessDayAdjustment().getConvention(), conv);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableTermDepositConvention test1 = ImmutableTermDepositConvention.of(
        "EUR-Deposit", EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    coverImmutableBean(test1);
    ImmutableTermDepositConvention test2 = ImmutableTermDepositConvention.of(
        "GBP-Deposit", GBP, BDA_MOD_FOLLOW, ACT_365F, DaysAdjustment.ofBusinessDays(0, GBLO));
    coverBeanEquals(test1, test2);

    coverPrivateConstructor(TermDepositConventions.class);
    coverPrivateConstructor(StandardTermDepositConventions.class);
  }

  public void test_serialization() {
    ImmutableTermDepositConvention test = ImmutableTermDepositConvention.of(
        "EUR-Deposit", EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
