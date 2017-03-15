/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link SecurityPosition}.
 */
@Test
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
  public void test_ofNet_noInfo() {
    SecurityPosition test = SecurityPosition.ofNet(SECURITY_ID, QUANTITY);
    assertEquals(test.getInfo(), PositionInfo.empty());
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), QUANTITY);
    assertEquals(test.getShortQuantity(), 0d);
    assertEquals(test.getQuantity(), QUANTITY);
  }

  public void test_ofNet_withInfo_positive() {
    SecurityPosition test = SecurityPosition.ofNet(POSITION_INFO, SECURITY_ID, 100d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), 100d);
    assertEquals(test.getShortQuantity(), 0d);
    assertEquals(test.getQuantity(), 100d);
  }

  public void test_ofNet_withInfo_zero() {
    SecurityPosition test = SecurityPosition.ofNet(POSITION_INFO, SECURITY_ID, 0d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), 0d);
    assertEquals(test.getShortQuantity(), 0d);
    assertEquals(test.getQuantity(), 0d);
  }

  public void test_ofNet_withInfo_negative() {
    SecurityPosition test = SecurityPosition.ofNet(POSITION_INFO, SECURITY_ID, -100d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), 0d);
    assertEquals(test.getShortQuantity(), 100d);
    assertEquals(test.getQuantity(), -100d);
  }

  public void test_ofLongShort_noInfo() {
    SecurityPosition test = SecurityPosition.ofLongShort(SECURITY_ID, LONG_QUANTITY, SHORT_QUANTITY);
    assertEquals(test.getInfo(), PositionInfo.empty());
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), LONG_QUANTITY);
    assertEquals(test.getShortQuantity(), SHORT_QUANTITY);
    assertEquals(test.getQuantity(), QUANTITY);
  }

  public void test_ofLongShort_withInfo() {
    SecurityPosition test = SecurityPosition.ofLongShort(POSITION_INFO, SECURITY_ID, LONG_QUANTITY, SHORT_QUANTITY);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), LONG_QUANTITY);
    assertEquals(test.getShortQuantity(), SHORT_QUANTITY);
    assertEquals(test.getQuantity(), QUANTITY);
  }

  public void test_builder() {
    SecurityPosition test = sut();
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getLongQuantity(), LONG_QUANTITY);
    assertEquals(test.getShortQuantity(), SHORT_QUANTITY);
    assertEquals(test.getQuantity(), QUANTITY);
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
