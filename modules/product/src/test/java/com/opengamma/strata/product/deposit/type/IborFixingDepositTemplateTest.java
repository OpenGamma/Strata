/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;

/**
 * Test {@link IborFixingDepositTemplate}.
 */
@Test
public class IborFixingDepositTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFixingDepositConvention CONVENTION = IborFixingDepositConvention.of(EUR_LIBOR_3M);

  public void test_builder() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.builder()
        .convention(CONVENTION)
        .depositPeriod(Period.ofMonths(1))
        .build();
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getDepositPeriod(), Period.ofMonths(1));
  }

  public void test_builder_noPeriod() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.builder()
        .convention(CONVENTION)
        .build();
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getDepositPeriod(), EUR_LIBOR_3M.getTenor().getPeriod());
  }

  public void test_build_negativePeriod() {
    assertThrowsIllegalArg(() -> IborFixingDepositTemplate.builder()
        .convention(CONVENTION)
        .depositPeriod(Period.ofMonths(-3))
        .build());
  }

  public void test_of_index() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getDepositPeriod(), EUR_LIBOR_3M.getTenor().getPeriod());
  }

  public void test_of_periodAndIndex() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.of(Period.ofMonths(1), EUR_LIBOR_3M);
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getDepositPeriod(), Period.ofMonths(1));
  }

  public void test_createTrade() {
    IborFixingDepositTemplate template = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    double notional = 1d;
    double fixedRate = 0.045;
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    IborFixingDepositTrade trade = template.createTrade(tradeDate, BUY, notional, fixedRate, REF_DATA);
    ImmutableIborFixingDepositConvention conv = (ImmutableIborFixingDepositConvention) template.getConvention();
    LocalDate startExpected = conv.getSpotDateOffset().adjust(tradeDate, REF_DATA);
    LocalDate endExpected = startExpected.plus(template.getDepositPeriod());
    IborFixingDeposit productExpected = IborFixingDeposit.builder()
        .businessDayAdjustment(conv.getBusinessDayAdjustment())
        .buySell(BUY)
        .startDate(startExpected)
        .endDate(endExpected)
        .fixedRate(fixedRate)
        .index(EUR_LIBOR_3M)
        .notional(notional)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(tradeDate)
        .build();
    assertEquals(trade.getInfo(), tradeInfoExpected);
    assertEquals(trade.getProduct(), productExpected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFixingDepositTemplate test1 = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    coverImmutableBean(test1);
    IborFixingDepositTemplate test2 = IborFixingDepositTemplate.of(GBP_LIBOR_6M);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborFixingDepositTemplate test = IborFixingDepositTemplate.of(EUR_LIBOR_3M);
    assertSerialization(test);
  }

}
