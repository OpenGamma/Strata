/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class TradeInfoTest {

  public void test_builder() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other"))
        .build();
    assertEquals(test.getCounterparty(), Optional.of(StandardId.of("OG-Party", "Other")));
    assertEquals(test.getTradeDate(), Optional.empty());
    assertEquals(test.getTradeTime(), Optional.empty());
    assertEquals(test.getZone(), Optional.empty());
    assertEquals(test.getSettlementDate(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TradeInfo test = TradeInfo.builder()
        .attributes(ImmutableMap.of("A", "B"))
        .counterparty(StandardId.of("OG-Party", "Other"))
        .tradeDate(date(2014, 6, 20))
        .tradeTime(LocalTime.MIDNIGHT)
        .zone(ZoneId.systemDefault())
        .settlementDate(date(2014, 6, 20))
        .build();
    coverImmutableBean(test);
    TradeInfo test2 = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other2"))
        .tradeDate(date(2014, 6, 21))
        .tradeTime(LocalTime.NOON)
        .zone(ZoneOffset.UTC)
        .settlementDate(date(2014, 6, 21))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(StandardId.of("OG-Party", "Other"))
        .tradeDate(date(2014, 6, 20))
        .tradeTime(LocalTime.MIDNIGHT)
        .zone(ZoneOffset.UTC)
        .settlementDate(date(2014, 6, 20))
        .build();
    assertSerialization(test);
  }

}
