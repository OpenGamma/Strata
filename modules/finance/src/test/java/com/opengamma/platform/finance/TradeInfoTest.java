/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalTime;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class TradeInfoTest {

  public void test_of() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other"))
        .build();
    assertEquals(test.getCounterparty(), Optional.of(StandardId.of("OG-Party", "Other")));
    assertEquals(test.getTradeDate(), Optional.empty());
    assertEquals(test.getTradeTime(), Optional.empty());
    assertEquals(test.getSettlementDate(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other"))
        .tradeDate(date(2014, 6, 20))
        .tradeTime(LocalTime.MIDNIGHT)
        .settlementDate(date(2014, 6, 20))
        .build();
    coverImmutableBean(test);
    TradeInfo test2 = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other2"))
        .tradeDate(date(2014, 6, 21))
        .tradeTime(LocalTime.NOON)
        .settlementDate(date(2014, 6, 21))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other"))
        .tradeDate(date(2014, 6, 20))
        .tradeTime(LocalTime.MIDNIGHT)
        .settlementDate(date(2014, 6, 20))
        .build();
    assertSerialization(test);
  }

}
