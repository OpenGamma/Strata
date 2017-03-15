/*
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
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedCds}.
 */
@Test
public class ResolvedCdsTradeTest {
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2024, 9, 20);

  private static final ResolvedCds PRODUCT =
      Cds.of(BUY, LEGAL_ENTITY, USD, NOTIONAL, START_DATE, END_DATE, P3M, CALENDAR, COUPON).resolve(REF_DATA);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(LocalDate.of(2014, 1, 9));
  private static final Payment UPFRONT = Payment.of(USD, NOTIONAL, LocalDate.of(2014, 1, 12));

  public void test_builder_full() {
    ResolvedCdsTrade test = ResolvedCdsTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .upfrontFee(UPFRONT)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getUpfrontFee().get(), UPFRONT);
  }

  public void test_builder_min() {
    ResolvedCdsTrade test = ResolvedCdsTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertFalse(test.getUpfrontFee().isPresent());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedCdsTrade test1 = ResolvedCdsTrade.builder()
        .product(PRODUCT)
        .upfrontFee(UPFRONT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    ResolvedCds product =
        Cds.of(BUY, LEGAL_ENTITY, USD, 1.e9, START_DATE, END_DATE, Frequency.P6M, SAT_SUN, 0.067).resolve(REF_DATA);
    ResolvedCdsTrade test2 = ResolvedCdsTrade.builder()
        .product(product)
        .info(TradeInfo.empty())
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedCdsTrade test = ResolvedCdsTrade.builder()
        .product(PRODUCT)
        .upfrontFee(UPFRONT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
