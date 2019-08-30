/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link GenericSecurityPosition}.
 */
public class GenericSecurityPositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final GenericSecurity SECURITY = GenericSecurityTest.sut();
  private static final GenericSecurity SECURITY2 = GenericSecurityTest.sut2();
  private static final double LONG_QUANTITY = 300;
  private static final double LONG_QUANTITY2 = 350;
  private static final double SHORT_QUANTITY = 200;
  private static final double SHORT_QUANTITY2 = 150;
  private static final double QUANTITY = 100;

  //-------------------------------------------------------------------------
  @Test
  public void test_ofNet_noInfo() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(SECURITY, QUANTITY);
    assertThat(test.getInfo()).isEqualTo(PositionInfo.empty());
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(0d);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getProduct()).isEqualTo(SECURITY);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY.getSecurityId());
    assertThat(test.getCurrency()).isEqualTo(SECURITY.getCurrency());
    assertThat(test.withInfo(POSITION_INFO).getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withQuantity(-129).getQuantity()).isCloseTo(-129d, offset(0d));
  }

  @Test
  public void test_ofNet_withInfo_positive() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(POSITION_INFO, SECURITY, 100d);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(100d);
    assertThat(test.getShortQuantity()).isEqualTo(0d);
    assertThat(test.getQuantity()).isEqualTo(100d);
  }

  @Test
  public void test_ofNet_withInfo_zero() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(POSITION_INFO, SECURITY, 0d);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(0d);
    assertThat(test.getShortQuantity()).isEqualTo(0d);
    assertThat(test.getQuantity()).isEqualTo(0d);
  }

  @Test
  public void test_ofNet_withInfo_negative() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(POSITION_INFO, SECURITY, -100d);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(0d);
    assertThat(test.getShortQuantity()).isEqualTo(100d);
    assertThat(test.getQuantity()).isEqualTo(-100d);
  }

  @Test
  public void test_ofLongShort_noInfo() {
    GenericSecurityPosition test = GenericSecurityPosition.ofLongShort(SECURITY, LONG_QUANTITY, SHORT_QUANTITY);
    assertThat(test.getInfo()).isEqualTo(PositionInfo.empty());
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(LONG_QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(SHORT_QUANTITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
  }

  @Test
  public void test_ofLongShort_withInfo() {
    GenericSecurityPosition test = GenericSecurityPosition.ofLongShort(POSITION_INFO, SECURITY, LONG_QUANTITY, SHORT_QUANTITY);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(LONG_QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(SHORT_QUANTITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
  }

  @Test
  public void test_builder() {
    GenericSecurityPosition test = sut();
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getLongQuantity()).isEqualTo(LONG_QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(SHORT_QUANTITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    GenericSecurityPosition trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.SECURITY)
        .currencies(SECURITY.getCurrency())
        .description("1 x 100")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static GenericSecurityPosition sut() {
    return GenericSecurityPosition.builder()
        .info(POSITION_INFO)
        .security(SECURITY)
        .longQuantity(LONG_QUANTITY)
        .shortQuantity(SHORT_QUANTITY)
        .build();
  }

  static GenericSecurityPosition sut2() {
    return GenericSecurityPosition.builder()
        .info(PositionInfo.empty())
        .security(SECURITY2)
        .longQuantity(LONG_QUANTITY2)
        .shortQuantity(SHORT_QUANTITY2)
        .build();
  }

}
