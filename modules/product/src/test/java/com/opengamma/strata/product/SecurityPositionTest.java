/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link SecurityPosition}.
 */
public class SecurityPositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Id");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Id2");
  private static final double LONG_QUANTITY = 300;
  private static final double LONG_QUANTITY2 = 350;
  private static final double SHORT_QUANTITY = 200;
  private static final double SHORT_QUANTITY2 = 150;
  private static final double QUANTITY = 100;

  //-------------------------------------------------------------------------
  @Test
  public void test_ofNet_noInfo() {
    SecurityPosition test = SecurityPosition.ofNet(SECURITY_ID, QUANTITY);
    assertThat(test.getInfo()).isEqualTo(PositionInfo.empty());
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(0d);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.withInfo(POSITION_INFO).getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withQuantity(-129).getQuantity()).isCloseTo(-129d, offset(0d));
  }

  @Test
  public void test_ofNet_withInfo_positive() {
    SecurityPosition test = SecurityPosition.ofNet(POSITION_INFO, SECURITY_ID, 100d);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(100d);
    assertThat(test.getShortQuantity()).isEqualTo(0d);
    assertThat(test.getQuantity()).isEqualTo(100d);
  }

  @Test
  public void test_ofNet_withInfo_zero() {
    SecurityPosition test = SecurityPosition.ofNet(POSITION_INFO, SECURITY_ID, 0d);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(0d);
    assertThat(test.getShortQuantity()).isEqualTo(0d);
    assertThat(test.getQuantity()).isEqualTo(0d);
  }

  @Test
  public void test_ofNet_withInfo_negative() {
    SecurityPosition test = SecurityPosition.ofNet(POSITION_INFO, SECURITY_ID, -100d);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(0d);
    assertThat(test.getShortQuantity()).isEqualTo(100d);
    assertThat(test.getQuantity()).isEqualTo(-100d);
  }

  @Test
  public void test_ofLongShort_noInfo() {
    SecurityPosition test = SecurityPosition.ofLongShort(SECURITY_ID, LONG_QUANTITY, SHORT_QUANTITY);
    assertThat(test.getInfo()).isEqualTo(PositionInfo.empty());
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(LONG_QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(SHORT_QUANTITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
  }

  @Test
  public void test_ofLongShort_withInfo() {
    SecurityPosition test = SecurityPosition.ofLongShort(POSITION_INFO, SECURITY_ID, LONG_QUANTITY, SHORT_QUANTITY);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(LONG_QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(SHORT_QUANTITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
  }

  @Test
  public void test_builder() {
    SecurityPosition test = sut();
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getLongQuantity()).isEqualTo(LONG_QUANTITY);
    assertThat(test.getShortQuantity()).isEqualTo(SHORT_QUANTITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    SecurityPosition trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.SECURITY)
        .description("Id x 100")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolveTarget() {
    SecurityPosition position = sut();
    GenericSecurity resolvedSecurity = GenericSecurity.of(SecurityInfo.of(SECURITY_ID, 1, CurrencyAmount.of(USD, 0.01)));
    ImmutableReferenceData refData = ImmutableReferenceData.of(SECURITY_ID, resolvedSecurity);
    GenericSecurityPosition expected =
        GenericSecurityPosition.ofLongShort(POSITION_INFO, resolvedSecurity, LONG_QUANTITY, SHORT_QUANTITY);
    assertThat(position.resolveTarget(refData)).isEqualTo(expected);
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
  static SecurityPosition sut() {
    return SecurityPosition.builder()
        .info(POSITION_INFO)
        .securityId(SECURITY_ID)
        .longQuantity(LONG_QUANTITY)
        .shortQuantity(SHORT_QUANTITY)
        .build();
  }

  static SecurityPosition sut2() {
    return SecurityPosition.builder()
        .info(PositionInfo.empty())
        .securityId(SECURITY_ID2)
        .longQuantity(LONG_QUANTITY2)
        .shortQuantity(SHORT_QUANTITY2)
        .build();
  }

}
