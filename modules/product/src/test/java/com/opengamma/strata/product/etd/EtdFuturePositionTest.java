/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link EtdFuturePosition}.
 */
public class EtdFuturePositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final EtdFutureSecurity SECURITY = EtdFutureSecurityTest.sut();
  private static final int LONG_QUANTITY = 3000;
  private static final int SHORT_QUANTITY = 2000;

  @Test
  public void test_ofNet() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(SECURITY, 1000);
    assertThat(test.getLongQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.getShortQuantity()).isCloseTo(0d, offset(0d));
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.withInfo(POSITION_INFO).getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withQuantity(-129).getQuantity()).isCloseTo(-129d, offset(0d));
  }

  @Test
  public void test_ofNet_short() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(SECURITY, -1000);
    assertThat(test.getLongQuantity()).isCloseTo(0d, offset(0d));
    assertThat(test.getShortQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(-1000d, offset(0d));
  }

  @Test
  public void test_ofNet_withInfo() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(POSITION_INFO, SECURITY, 1000);
    assertThat(test.getLongQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.getShortQuantity()).isCloseTo(0d, offset(0d));
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(1000d, offset(0d));
  }

  @Test
  public void test_ofLongShort() {
    EtdFuturePosition test = EtdFuturePosition.ofLongShort(SECURITY, 2000, 1000);
    assertThat(test.getLongQuantity()).isCloseTo(2000d, offset(0d));
    assertThat(test.getShortQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.withQuantities(100d, 300d).getQuantity()).isCloseTo(-200d, offset(0d));
    assertThat(test.withQuantities(100d, 300d).getShortQuantity()).isCloseTo(300d, offset(0d));
    assertThat(test.withQuantities(300d, 100d).getQuantity()).isCloseTo(200d, offset(0d));
    assertThat(test.withQuantities(300d, 100d).getLongQuantity()).isCloseTo(300d, offset(0d));
  }

  @Test
  public void test_ofLongShort_withInfo() {
    EtdFuturePosition test = EtdFuturePosition.ofLongShort(POSITION_INFO, SECURITY, 2000, 1000);
    assertThat(test.getLongQuantity()).isCloseTo(2000d, offset(0d));
    assertThat(test.getShortQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(1000d, offset(0d));
  }

  @Test
  public void test_methods() {
    EtdFuturePosition test = sut();
    assertThat(test.getType()).isEqualTo(EtdType.FUTURE);
    assertThat(test.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(test.getSecurityId()).isEqualTo(test.getSecurity().getSecurityId());
    assertThat(test.getProduct()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(1000d, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    EtdFuturePosition trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.ETD_FUTURE)
        .currencies(SECURITY.getCurrency())
        .description(SECURITY.getSecurityId().getStandardId().getValue() + " x 1000, Jun17")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolveTarget() {
    EtdFuturePosition position = sut();
    GenericSecurity resolvedSecurity = GenericSecurity.of(SECURITY.getInfo());
    ImmutableReferenceData refData = ImmutableReferenceData.of(SECURITY.getSecurityId(), resolvedSecurity);
    assertThat(position.resolveTarget(refData)).isEqualTo(position);
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
  static EtdFuturePosition sut() {
    return EtdFuturePosition.builder()
        .info(POSITION_INFO)
        .security(SECURITY)
        .longQuantity(LONG_QUANTITY)
        .shortQuantity(SHORT_QUANTITY)
        .build();
  }

  static EtdFuturePosition sut2() {
    return EtdFuturePosition.builder()
        .security(EtdFutureSecurityTest.sut2())
        .longQuantity(4000)
        .shortQuantity(1000)
        .build();
  }

}
