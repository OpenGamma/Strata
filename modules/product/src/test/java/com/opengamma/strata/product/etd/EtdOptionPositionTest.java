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
 * Test {@link EtdOptionPosition}.
 */
@Test
public class EtdOptionPositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final EtdOptionSecurity SECURITY = EtdOptionSecurityTest.sut();

  public void test_ofNet() {
    EtdOptionPosition test = EtdOptionPosition.ofNet(SECURITY, 1000);
    assertEquals(test.getLongQuantity(), 1000d, 0d);
    assertEquals(test.getShortQuantity(), 0d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_ofNet_short() {
    EtdOptionPosition test = EtdOptionPosition.ofNet(SECURITY, -1000);
    assertEquals(test.getLongQuantity(), 0d, 0d);
    assertEquals(test.getShortQuantity(), 1000d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), -1000d, 0d);
  }

  public void test_ofNet_withInfo() {
    EtdOptionPosition test = EtdOptionPosition.ofNet(POSITION_INFO, SECURITY, 1000);
    assertEquals(test.getLongQuantity(), 1000d, 0d);
    assertEquals(test.getShortQuantity(), 0d, 0d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_ofLongShort() {
    EtdOptionPosition test = EtdOptionPosition.ofLongShort(SECURITY, 2000, 1000);
    assertEquals(test.getLongQuantity(), 2000d, 0d);
    assertEquals(test.getShortQuantity(), 1000d, 0d);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_ofLongShort_withInfo() {
    EtdOptionPosition test = EtdOptionPosition.ofLongShort(POSITION_INFO, SECURITY, 2000, 1000);
    assertEquals(test.getLongQuantity(), 2000d, 0d);
    assertEquals(test.getShortQuantity(), 1000d, 0d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
  }

  public void test_methods() {
    EtdOptionPosition test = sut();
    assertEquals(test.getType(), EtdType.OPTION);
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
  static EtdOptionPosition sut() {
    return EtdOptionPosition.builder()
        .info(POSITION_INFO)
        .security(SECURITY)
        .longQuantity(3000)
        .shortQuantity(2000)
        .build();
  }

  static EtdOptionPosition sut2() {
    return EtdOptionPosition.builder()
        .security(EtdOptionSecurityTest.sut2())
        .longQuantity(4000)
        .shortQuantity(1000)
        .build();
  }

}
