/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedCdsIndexTrade}.
 */
@Test
public class ResolvedCdsIndexTradeTest {
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final DaysAdjustment SETTLE_DAY_ADJ = DaysAdjustment.ofBusinessDays(3, CALENDAR);
  private static final StandardId INDEX_ID = StandardId.of("OG", "AA-INDEX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES = ImmutableList.of(
      StandardId.of("OG", "ABC1"), StandardId.of("OG", "ABC2"), StandardId.of("OG", "ABC3"), StandardId.of("OG", "ABC4"));
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2024, 9, 20);
  private static final ResolvedCdsIndex PRODUCT = CdsIndex.of(
      BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, START_DATE, END_DATE, P3M, SAT_SUN, COUPON).resolve(REF_DATA);
  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 1, 9);
  private static final LocalDate SETTLE_DATE = SETTLE_DAY_ADJ.adjust(TRADE_DATE, REF_DATA);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLE_DATE)
      .build();
  private static final Payment UPFRONT = Payment.of(USD, NOTIONAL, SETTLE_DATE);

  public void test_builder_full() {
    ResolvedCdsIndexTrade test = ResolvedCdsIndexTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .upfrontFee(UPFRONT)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getUpfrontFee().get(), UPFRONT);

    ResolvedCdsTrade singleName = test.toSingleNameCds();
    assertEquals(singleName.getProduct(), PRODUCT.toSingleNameCds());
    assertEquals(singleName.getInfo(), TRADE_INFO);
    assertEquals(singleName.getUpfrontFee().get(), UPFRONT);
  }

  public void test_builder_min() {
    ResolvedCdsIndexTrade test = ResolvedCdsIndexTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertFalse(test.getUpfrontFee().isPresent());

    ResolvedCdsTrade singleName = test.toSingleNameCds();
    assertEquals(singleName.getProduct(), PRODUCT.toSingleNameCds());
    assertEquals(singleName.getInfo(), TRADE_INFO);
    assertFalse(singleName.getUpfrontFee().isPresent());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedCdsIndexTrade test1 = ResolvedCdsIndexTrade.builder()
        .product(PRODUCT)
        .upfrontFee(UPFRONT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    ResolvedCdsIndex product =
        CdsIndex.of(BUY, INDEX_ID, LEGAL_ENTITIES, USD, 1.e9, START_DATE, END_DATE, P6M, SAT_SUN, 0.067).resolve(REF_DATA);
    ResolvedCdsIndexTrade test2 = ResolvedCdsIndexTrade.builder()
        .product(product)
        .info(TradeInfo.empty())
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedCdsIndexTrade test = ResolvedCdsIndexTrade.builder()
        .product(PRODUCT)
        .upfrontFee(UPFRONT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
