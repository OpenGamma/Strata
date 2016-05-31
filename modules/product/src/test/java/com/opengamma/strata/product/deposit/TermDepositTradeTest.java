/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * Test {@link TermDepositTrade}. 
 */
@Test
public class TermDepositTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final TermDeposit DEPOSIT = TermDeposit.builder()
      .buySell(BuySell.BUY)
      .currency(GBP)
      .notional(100000000d)
      .startDate(LocalDate.of(2015, 1, 19))
      .endDate(LocalDate.of(2015, 7, 19))
      .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
      .dayCount(ACT_365F)
      .rate(0.0250)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  public void test_of() {
    TermDepositTrade test = TermDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    TermDepositTrade test = TermDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertEquals(test.resolve(REF_DATA).getInfo(), TRADE_INFO);
    assertEquals(test.resolve(REF_DATA).getProduct(), DEPOSIT.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TermDepositTrade test1 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    TermDepositTrade test2 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
