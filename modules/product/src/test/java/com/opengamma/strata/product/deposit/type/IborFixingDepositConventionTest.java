/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;

/**
 * Test {@link IborFixingDepositConvention}.
 */
@Test
public class IborFixingDepositConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment SPOT_ADJ = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final DaysAdjustment FIXING_ADJ =
      DaysAdjustment.ofBusinessDays(-2, EUTA, BusinessDayAdjustment.of(PRECEDING, GBLO));

  //-------------------------------------------------------------------------
  public void test_builder_full() {
    ImmutableIborFixingDepositConvention test = ImmutableIborFixingDepositConvention.builder()
        .name("Name")
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_365F)
        .fixingDateOffset(FIXING_ADJ)
        .index(EUR_LIBOR_3M)
        .spotDateOffset(SPOT_ADJ)
        .build();
    assertEquals(test.getName(), "Name");
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getFixingDateOffset(), FIXING_ADJ);
    assertEquals(test.getIndex(), EUR_LIBOR_3M);
    assertEquals(test.getSpotDateOffset(), SPOT_ADJ);
  }

  public void test_builder_indexOnly() {
    ImmutableIborFixingDepositConvention test = ImmutableIborFixingDepositConvention.builder()
        .index(GBP_LIBOR_6M)
        .build();
    assertEquals(test.getBusinessDayAdjustment(),
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBP_LIBOR_6M.getFixingCalendar()));
    assertEquals(test.getCurrency(), GBP_LIBOR_6M.getCurrency());
    assertEquals(test.getDayCount(), GBP_LIBOR_6M.getDayCount());
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_6M.getFixingDateOffset());
    assertEquals(test.getIndex(), GBP_LIBOR_6M);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_6M.getEffectiveDateOffset());
  }

  public void test_of_indexOnly() {
    ImmutableIborFixingDepositConvention test = ImmutableIborFixingDepositConvention.of(GBP_LIBOR_6M);
    assertEquals(test.getBusinessDayAdjustment(),
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBP_LIBOR_6M.getFixingCalendar()));
    assertEquals(test.getCurrency(), GBP_LIBOR_6M.getCurrency());
    assertEquals(test.getDayCount(), GBP_LIBOR_6M.getDayCount());
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_6M.getFixingDateOffset());
    assertEquals(test.getIndex(), GBP_LIBOR_6M);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_6M.getEffectiveDateOffset());
  }

  //-------------------------------------------------------------------------
  public void test_toTrade() {
    IborFixingDepositConvention convention = ImmutableIborFixingDepositConvention.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_365F)
        .fixingDateOffset(FIXING_ADJ)
        .index(EUR_LIBOR_3M)
        .spotDateOffset(SPOT_ADJ)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    Period depositPeriod = Period.ofMonths(3);
    double notional = 1d;
    double fixedRate = 0.045;
    IborFixingDepositTrade trade = convention.createTrade(tradeDate, depositPeriod, BUY, notional, fixedRate, REF_DATA);
    LocalDate startExpected = SPOT_ADJ.adjust(tradeDate, REF_DATA);
    LocalDate endExpected = startExpected.plus(depositPeriod);
    IborFixingDeposit productExpected = IborFixingDeposit.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .buySell(BUY)
        .currency(EUR)
        .dayCount(ACT_365F)
        .startDate(startExpected)
        .endDate(endExpected)
        .fixedRate(fixedRate)
        .fixingDateOffset(FIXING_ADJ)
        .index(EUR_LIBOR_3M)
        .notional(notional)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(tradeDate)
        .build();
    assertEquals(trade.getProduct(), productExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {ImmutableIborFixingDepositConvention.of(GBP_LIBOR_3M), "GBP-LIBOR-3M"},
        {ImmutableIborFixingDepositConvention.of(USD_LIBOR_3M), "USD-LIBOR-3M"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(IborFixingDepositConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(IborFixingDepositConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(IborFixingDepositConvention convention, String name) {
    assertEquals(IborFixingDepositConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(IborFixingDepositConvention convention, String name) {
    IborFixingDepositConvention.of(name);  // ensures map is populated
    ImmutableMap<String, IborFixingDepositConvention> map = IborFixingDepositConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> IborFixingDepositConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> IborFixingDepositConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableIborFixingDepositConvention test1 = ImmutableIborFixingDepositConvention.of(GBP_LIBOR_6M);
    coverImmutableBean(test1);
    ImmutableIborFixingDepositConvention test2 = ImmutableIborFixingDepositConvention.of(EUR_LIBOR_3M)
        .toBuilder()
        .name("Foo")
        .build();
    coverBeanEquals(test1, test2);

    coverPrivateConstructor(IborFixingDepositConventions.class);
    coverPrivateConstructor(IborFixingDepositConventionLookup.class);
  }

  public void test_serialization() {
    ImmutableIborFixingDepositConvention test = ImmutableIborFixingDepositConvention.of(GBP_LIBOR_6M);
    assertSerialization(test);
  }

}
