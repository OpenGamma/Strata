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

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PositionInfo;

/**
 * Test {@link EtdFuturePosition}.
 */
@Test
public class EtdFuturePositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final EtdFutureSecurity SECURITY = EtdFutureSecurityTest.sut();

  public void test_ofNet() {
    EtdFuturePosition test = EtdFuturePosition.ofNet(SECURITY, 1000);
    assertEquals(test.getLongQuantity(), 1000d, 0d);
    assertEquals(test.getShortQuantity(), 0d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
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
        .longQuantity(3000)
        .shortQuantity(2000)
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
