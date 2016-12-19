/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;

/**
 * Test {@link CdsIndexCalibrationTrade}.
 */
@Test
public class CdsIndexCalibrationTradeTest {
  private static final StandardId INDEX_ID = StandardId.of("OG", "ABCXX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES =
      ImmutableList.of(StandardId.of("OG", "ABC1"), StandardId.of("OG", "ABC2"), StandardId.of("OG", "ABC3"));
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2024, 9, 20);

  private static final CdsIndex PRODUCT =
      CdsIndex.of(BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START_DATE, END_DATE, P3M, SAT_SUN, COUPON);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(LocalDate.of(2014, 1, 9));
  private static final AdjustablePayment UPFRONT = AdjustablePayment.of(USD, NOTIONAL, LocalDate.of(2014, 1, 12));
  private static final CdsIndexTrade TRADE = CdsIndexTrade.builder()
      .product(PRODUCT)
      .upfrontFee(UPFRONT)
      .info(TRADE_INFO)
      .build();
  private static final CdsQuote QUOTE1 = CdsQuote.of(CdsQuoteConvention.POINTS_UPFRONT, 0.95);
  private static final CdsQuote QUOTE2 = CdsQuote.of(CdsQuoteConvention.QUOTED_SPREAD, 0.0155);
  private static final CdsQuote QUOTE3 = CdsQuote.of(CdsQuoteConvention.PAR_SPREAD, 0.012);

  //-------------------------------------------------------------------------
  public void test_of_trade() {
    CdsIndexCalibrationTrade test = CdsIndexCalibrationTrade.of(TRADE, QUOTE1);
    assertEquals(test.getUnderlyingTrade(), TRADE);
    assertEquals(test.getQuote(), QUOTE1);
    assertEquals(test.getInfo(), TRADE.getInfo());
  }

  //-------------------------------------------------------------------------
  public void coverage_trade() {
    CdsIndexCalibrationTrade test1 = CdsIndexCalibrationTrade.of(TRADE, QUOTE1);
    coverImmutableBean(test1);
    CdsIndexCalibrationTrade test2 = CdsIndexCalibrationTrade.of(
        CdsIndexTrade.builder()
            .product(PRODUCT)
            .info(TRADE_INFO)
            .build(),
        QUOTE2);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization_trade() {
    CdsIndexCalibrationTrade test = CdsIndexCalibrationTrade.of(TRADE, QUOTE3);
    assertSerialization(test);
  }

}
