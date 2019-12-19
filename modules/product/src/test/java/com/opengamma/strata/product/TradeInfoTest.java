/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link TradeInfo}.
 */
public class TradeInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId COUNTERPARTY = StandardId.of("OG-Party", "Other");
  private static final StandardId COUNTERPARTY2 = StandardId.of("OG-Party", "Other2");

  @Test
  public void test_builder() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .build();
    assertThat(test.getId()).isEmpty();
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY);
    assertThat(test.getTradeDate()).isEmpty();
    assertThat(test.getTradeTime()).isEmpty();
    assertThat(test.getZone()).isEmpty();
    assertThat(test.getSettlementDate()).isEmpty();
    assertThat(test.getAttributeTypes()).isEmpty();
    assertThat(test.getAttributes()).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getAttribute(AttributeType.DESCRIPTION));
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).isEmpty();
  }

  @Test
  public void test_builder_withers() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .build()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY);
    assertThat(test.getTradeDate()).isEmpty();
    assertThat(test.getTradeTime()).isEmpty();
    assertThat(test.getZone()).isEmpty();
    assertThat(test.getSettlementDate()).isEmpty();
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(test.getAttributes()).containsEntry(AttributeType.DESCRIPTION, "A");
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("A");
  }

  @Test
  public void test_builder_with_bulk() {
    Attributes override = Attributes.of(AttributeType.DESCRIPTION, "B").withAttribute(AttributeType.NAME, "C");
    TradeInfo test = TradeInfo.builder()
        .build()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A")
        .withAttributes(override);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("B");
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("C");
  }

  @Test
  public void test_combinedWith() {
    TradeInfo base = TradeInfo.builder()
        .id(ID)
        .counterparty(COUNTERPARTY)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    TradeInfo other = TradeInfo.builder()
        .counterparty(COUNTERPARTY2)
        .tradeDate(date(2014, 6, 21))
        .tradeTime(LocalTime.NOON)
        .zone(ZoneOffset.UTC)
        .settlementDate(date(2014, 6, 21))
        .addAttribute(AttributeType.DESCRIPTION, "B")
        .addAttribute(AttributeType.NAME, "B")
        .build();
    TradeInfo test = base.combinedWith(other);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY);
    assertThat(test.getTradeDate()).hasValue(date(2014, 6, 21));
    assertThat(test.getTradeTime()).hasValue(LocalTime.NOON);
    assertThat(test.getZone()).hasValue(ZoneOffset.UTC);
    assertThat(test.getSettlementDate()).hasValue(date(2014, 6, 21));
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "A")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_combinedWith_otherType() {
    TradeInfo base = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    PositionInfo other = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "B")
        .addAttribute(AttributeType.NAME, "B")
        .build();
    TradeInfo test = base.combinedWith(other);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY);
    assertThat(test.getTradeDate()).isEmpty();
    assertThat(test.getTradeTime()).isEmpty();
    assertThat(test.getZone()).isEmpty();
    assertThat(test.getSettlementDate()).isEmpty();
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "A")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_overrideWith() {
    TradeInfo base = TradeInfo.builder()
        .id(ID)
        .counterparty(COUNTERPARTY)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    TradeInfo other = TradeInfo.builder()
        .counterparty(COUNTERPARTY2)
        .tradeDate(date(2014, 6, 21))
        .tradeTime(LocalTime.NOON)
        .zone(ZoneOffset.UTC)
        .settlementDate(date(2014, 6, 21))
        .addAttribute(AttributeType.DESCRIPTION, "B")
        .addAttribute(AttributeType.NAME, "B")
        .build();
    TradeInfo test = base.overrideWith(other);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY2);
    assertThat(test.getTradeDate()).hasValue(date(2014, 6, 21));
    assertThat(test.getTradeTime()).hasValue(LocalTime.NOON);
    assertThat(test.getZone()).hasValue(ZoneOffset.UTC);
    assertThat(test.getSettlementDate()).hasValue(date(2014, 6, 21));
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "B")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_overrideWith_otherType() {
    TradeInfo base = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    PositionInfo other = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "B")
        .addAttribute(AttributeType.NAME, "B")
        .build();
    TradeInfo test = base.overrideWith(other);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY);
    assertThat(test.getTradeDate()).isEmpty();
    assertThat(test.getTradeTime()).isEmpty();
    assertThat(test.getZone()).isEmpty();
    assertThat(test.getSettlementDate()).isEmpty();
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "B")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_toBuilder() {
    TradeInfo test = TradeInfo.builder()
        .counterparty(COUNTERPARTY)
        .build()
        .toBuilder()
        .id(ID)
        .build();
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getCounterparty()).hasValue(COUNTERPARTY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TradeInfo test = TradeInfo.builder()
        .addAttribute(AttributeType.DESCRIPTION, "A")
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

  @Test
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
