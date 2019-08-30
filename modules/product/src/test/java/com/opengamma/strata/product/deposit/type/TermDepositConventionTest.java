/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
public class TermDepositConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_full() {
    ImmutableTermDepositConvention test = ImmutableTermDepositConvention.builder()
        .name("Test")
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    assertThat(test.getName()).isEqualTo("Test");
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
  }

  @Test
  public void test_of() {
    ImmutableTermDepositConvention test = ImmutableTermDepositConvention.of(
        "EUR-Deposit", EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    assertThat(test.getName()).isEqualTo("EUR-Deposit");
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(trade.getProduct()).isEqualTo(termDepositExpected);
    assertThat(trade.getInfo()).isEqualTo(tradeInfoExpected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {TermDepositConventions.USD_DEPOSIT_T2, "USD-Deposit-T2"},
        {TermDepositConventions.EUR_DEPOSIT_T2, "EUR-Deposit-T2"},
        {TermDepositConventions.GBP_DEPOSIT_T0, "GBP-Deposit-T0"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(TermDepositConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(TermDepositConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(TermDepositConvention convention, String name) {
    assertThat(TermDepositConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(TermDepositConvention convention, String name) {
    TermDepositConvention.of(name);  // ensures map is populated
    ImmutableMap<String, TermDepositConvention> map = TermDepositConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TermDepositConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TermDepositConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spotAndConv() {
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

  @ParameterizedTest
  @MethodSource("data_spotAndConv")
  public void test_spotAndConv(ImmutableTermDepositConvention convention, int spotT, BusinessDayConvention conv) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(spotT);
    assertThat(convention.getBusinessDayAdjustment().getConvention()).isEqualTo(conv);
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    ImmutableTermDepositConvention test = ImmutableTermDepositConvention.of(
        "EUR-Deposit", EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
