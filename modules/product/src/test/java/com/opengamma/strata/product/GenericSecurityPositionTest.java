/**
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
 * Test {@link GenericSecurityPosition}.
 */
@Test
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
  public void test_ofNet_noInfo() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(SECURITY, QUANTITY);
    assertEquals(test.getInfo(), PositionInfo.empty());
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getLongQuantity(), QUANTITY);
    assertEquals(test.getShortQuantity(), 0d);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityId(), SECURITY.getSecurityId());
    assertEquals(test.getCurrency(), SECURITY.getCurrency());
  }

  public void test_ofNet_withInfo_positive() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(POSITION_INFO, SECURITY, 100d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getLongQuantity(), 100d);
    assertEquals(test.getShortQuantity(), 0d);
    assertEquals(test.getQuantity(), 100d);
  }

  public void test_ofNet_withInfo_zero() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(POSITION_INFO, SECURITY, 0d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getLongQuantity(), 0d);
    assertEquals(test.getShortQuantity(), 0d);
    assertEquals(test.getQuantity(), 0d);
  }

  public void test_ofNet_withInfo_negative() {
    GenericSecurityPosition test = GenericSecurityPosition.ofNet(POSITION_INFO, SECURITY, -100d);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getLongQuantity(), 0d);
    assertEquals(test.getShortQuantity(), 100d);
    assertEquals(test.getQuantity(), -100d);
  }

  public void test_ofLongShort_noInfo() {
    GenericSecurityPosition test = GenericSecurityPosition.ofLongShort(SECURITY, LONG_QUANTITY, SHORT_QUANTITY);
    assertEquals(test.getInfo(), PositionInfo.empty());
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getLongQuantity(), LONG_QUANTITY);
    assertEquals(test.getShortQuantity(), SHORT_QUANTITY);
    assertEquals(test.getQuantity(), QUANTITY);
  }

  public void test_ofLongShort_withInfo() {
    GenericSecurityPosition test = GenericSecurityPosition.ofLongShort(POSITION_INFO, SECURITY, LONG_QUANTITY, SHORT_QUANTITY);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getLongQuantity(), LONG_QUANTITY);
    assertEquals(test.getShortQuantity(), SHORT_QUANTITY);
    assertEquals(test.getQuantity(), QUANTITY);
  }

  public void test_builder() {
    GenericSecurityPosition test = sut();
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getSecurity(), SECURITY);
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
