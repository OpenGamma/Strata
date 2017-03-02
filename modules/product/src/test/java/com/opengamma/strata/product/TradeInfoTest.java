/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
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
import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link TradeInfo}.
 */
@Test
public class TradeInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId COUNTERPARTY = StandardId.of("OG-Party", "Other");

  public void test_builder() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .build();
    assertEquals(test.getId(), Optional.empty());
    assertEquals(test.getCounterparty(), Optional.of(COUNTERPARTY));
    assertEquals(test.getTradeDate(), Optional.empty());
    assertEquals(test.getTradeTime(), Optional.empty());
    assertEquals(test.getZone(), Optional.empty());
    assertEquals(test.getSettlementDate(), Optional.empty());
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getAttribute(TradeAttributeType.DESCRIPTION));
    assertEquals(test.findAttribute(TradeAttributeType.DESCRIPTION), Optional.empty());
  }

  public void test_builder_withAttribute() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .build()
        .withAttribute(TradeAttributeType.DESCRIPTION, "A");
    assertEquals(test.getId(), Optional.empty());
    assertEquals(test.getCounterparty(), Optional.of(COUNTERPARTY));
    assertEquals(test.getTradeDate(), Optional.empty());
    assertEquals(test.getTradeTime(), Optional.empty());
    assertEquals(test.getZone(), Optional.empty());
    assertEquals(test.getSettlementDate(), Optional.empty());
    assertEquals(test.getAttributes(), ImmutableMap.of(TradeAttributeType.DESCRIPTION, "A"));
    assertEquals(test.getAttribute(TradeAttributeType.DESCRIPTION), "A");
    assertEquals(test.findAttribute(TradeAttributeType.DESCRIPTION), Optional.of("A"));
  }

  public void test_toBuilder() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .build()
        .toBuilder()
        .id(ID)
        .build();
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getCounterparty(), Optional.of(COUNTERPARTY));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TradeInfo test = TradeInfo.builder()
        .addAttribute(TradeAttributeType.DESCRIPTION, "A")
        .counterparty(COUNTERPARTY)
        .tradeDate(date(2014, 6, 20))
        .tradeTime(LocalTime.MIDNIGHT)
        .zone(ZoneId.systemDefault())
        .settlementDate(date(2014, 6, 20))
        .build();
    coverImmutableBean(test);
    TradeInfo test2 = TradeInfo.builder()
        .id(StandardId.of("OG-Id", "1"))
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
        .counterparty(COUNTERPARTY)
        .tradeDate(date(2014, 6, 20))
        .tradeTime(LocalTime.MIDNIGHT)
        .zone(ZoneOffset.UTC)
        .settlementDate(date(2014, 6, 20))
        .build();
    assertSerialization(test);
  }

}
