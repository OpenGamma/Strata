/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
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
 * Test {@link IborFixingDepositTrade}.
 */
@Test
public class IborFixingDepositTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final IborFixingDeposit DEPOSIT = IborFixingDeposit.builder()
      .buySell(BuySell.BUY)
      .notional(100000000d)
      .startDate(LocalDate.of(2015, 1, 19))
      .endDate(LocalDate.of(2015, 7, 19))
      .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
      .index(GBP_LIBOR_6M)
      .fixedRate(0.0250)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  public void test_of() {
    IborFixingDepositTrade test = IborFixingDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    IborFixingDepositTrade test = IborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    IborFixingDepositTrade test = IborFixingDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertEquals(test.resolve(REF_DATA).getInfo(), TRADE_INFO);
    assertEquals(test.resolve(REF_DATA).getProduct(), DEPOSIT.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFixingDepositTrade test1 = IborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    IborFixingDepositTrade test2 = IborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborFixingDepositTrade test = IborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
