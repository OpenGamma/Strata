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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;

/**
 * Test {@link TermDepositTemplate}.
 */
public class TermDepositTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final TermDepositConvention CONVENTION = TermDepositConventions.EUR_DEPOSIT_T2;
  private static final Period DEPOSIT_PERIOD = Period.ofMonths(3);

  @Test
  public void test_builder() {
    TermDepositTemplate test = TermDepositTemplate.builder()
        .convention(CONVENTION)
        .depositPeriod(DEPOSIT_PERIOD)
        .build();
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDepositPeriod()).isEqualTo(DEPOSIT_PERIOD);
  }

  @Test
  public void test_builder_negativePeriod() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TermDepositTemplate.builder()
            .convention(CONVENTION)
            .depositPeriod(Period.ofMonths(-2))
            .build());
  }

  @Test
  public void test_of() {
    TermDepositTemplate test = TermDepositTemplate.of(DEPOSIT_PERIOD, CONVENTION);
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDepositPeriod()).isEqualTo(DEPOSIT_PERIOD);
  }

  @Test
  public void test_createTrade() {
    TermDepositTemplate template = TermDepositTemplate.of(DEPOSIT_PERIOD, CONVENTION);
    LocalDate tradeDate = LocalDate.of(2015, 1, 23);
    BuySell buy = BuySell.BUY;
    double notional = 2_000_000d;
    double rate = 0.0125;
    TermDepositTrade trade = template.createTrade(tradeDate, buy, notional, rate, REF_DATA);
    TradeInfo tradeInfoExpected = TradeInfo.of(tradeDate);
    LocalDate startDateExpected = PLUS_TWO_DAYS.adjust(tradeDate, REF_DATA);
    LocalDate endDateExpected = startDateExpected.plus(DEPOSIT_PERIOD);
    TermDeposit productExpected = TermDeposit.builder()
        .buySell(buy)
        .currency(EUR)
        .notional(notional)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .rate(rate)
        .dayCount(ACT_360)
        .build();
    assertThat(trade.getInfo()).isEqualTo(tradeInfoExpected);
    assertThat(trade.getProduct()).isEqualTo(productExpected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TermDepositTemplate test1 = TermDepositTemplate.of(DEPOSIT_PERIOD, CONVENTION);
    coverImmutableBean(test1);
    TermDepositTemplate test2 = TermDepositTemplate.of(Period.ofMonths(6),
        ImmutableTermDepositConvention.of(
            "GBP-Dep", GBP, BDA_MOD_FOLLOW, ACT_365F, DaysAdjustment.ofBusinessDays(2, GBLO)));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    TermDepositTemplate test = TermDepositTemplate.of(DEPOSIT_PERIOD, CONVENTION);
    assertSerialization(test);
  }

}
