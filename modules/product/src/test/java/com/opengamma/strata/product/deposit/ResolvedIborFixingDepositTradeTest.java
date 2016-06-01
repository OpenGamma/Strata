/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedIborFixingDepositTrade}. 
 */
@Test
public class ResolvedIborFixingDepositTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 20);
  private static final double YEAR_FRACTION = ACT_365F.yearFraction(START_DATE, END_DATE);
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(GBP_LIBOR_6M, FIXING_DATE, REF_DATA);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0250;

  private static final ResolvedIborFixingDeposit DEPOSIT = ResolvedIborFixingDeposit.builder()
      .currency(GBP)
      .notional(NOTIONAL)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .yearFraction(YEAR_FRACTION)
      .floatingRate(RATE_COMP)
      .fixedRate(RATE)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedIborFixingDepositTrade test = ResolvedIborFixingDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    ResolvedIborFixingDepositTrade test = ResolvedIborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborFixingDepositTrade test1 = ResolvedIborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    ResolvedIborFixingDepositTrade test2 = ResolvedIborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedIborFixingDepositTrade test = ResolvedIborFixingDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
