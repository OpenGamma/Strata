/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link PhysicalSettlement}.
 */
@Test
public class PhysicalSettlementTest {

  //-------------------------------------------------------------------------
  public void test_DEFAULT() {
    PhysicalSettlement test = PhysicalSettlement.DEFAULT;
    assertEquals(test.getSettlementType(), SettlementType.PHYSICAL);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PhysicalSettlement test = PhysicalSettlement.DEFAULT;
    coverImmutableBean(test);
  }

  public void test_serialization() {
    PhysicalSettlement test = PhysicalSettlement.DEFAULT;
    assertSerialization(test);
  }

}
