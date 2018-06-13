/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityPosition;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link EtdFuturePosition}.
 */
@Test
public class EtdFuturePositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final EtdFutureSecurity SECURITY = EtdFutureSecurityTest.sut();
  private static final int LONG_QUANTITY = 3000;
  private static final int SHORT_QUANTITY = 2000;

  public void test_ofNet() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(SECURITY, 1000);
    assertEquals(test.getLongQuantity(), 1000d, 0d);
    assertEquals(test.getShortQuantity(), 0d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
    assertEquals(test.withInfo(POSITION_INFO).getInfo(), POSITION_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
    assertEquals(test.withQuantity(-129).getQuantity(), -129d, 0d);
  }

  public void test_ofNet_short() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(SECURITY, -1000);
    assertEquals(test.getLongQuantity(), 0d, 0d);
    assertEquals(test.getShortQuantity(), 1000d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), -1000d, 0d);
  }

  public void test_ofNet_withInfo() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(POSITION_INFO, SECURITY, 1000);
    assertEquals(test.getLongQuantity(), 1000d, 0d);
    assertEquals(test.getShortQuantity(), 0d, 0d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_ofLongShort() {
    EtdFuturePosition test = EtdFuturePosition.ofLongShort(SECURITY, 2000, 1000);
    assertEquals(test.getLongQuantity(), 2000d, 0d);
    assertEquals(test.getShortQuantity(), 1000d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_ofLongShort_withInfo() {
    EtdFuturePosition test = EtdFuturePosition.ofLongShort(POSITION_INFO, SECURITY, 2000, 1000);
    assertEquals(test.getLongQuantity(), 2000d, 0d);
    assertEquals(test.getShortQuantity(), 1000d, 0d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_methods() {
    EtdFuturePosition test = sut();
    assertEquals(test.getType(), EtdType.FUTURE);
    assertEquals(test.getCurrency(), Currency.GBP);
    assertEquals(test.getSecurityId(), test.getSecurity().getSecurityId());
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    EtdFuturePosition trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.ETD_FUTURE)
        .currencies(SECURITY.getCurrency())
        .description(SECURITY.getSecurityId().getStandardId().getValue() + " x 1000, Jun17")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolveTarget() {
    EtdFuturePosition position = sut();
    GenericSecurity resolvedSecurity = GenericSecurity.of(SECURITY.getInfo());
    ImmutableReferenceData refData = ImmutableReferenceData.of(SECURITY.getSecurityId(), resolvedSecurity);
    GenericSecurityPosition expected =
        GenericSecurityPosition.ofLongShort(POSITION_INFO, resolvedSecurity, LONG_QUANTITY, SHORT_QUANTITY);
    assertEquals(position.resolveTarget(refData), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

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
