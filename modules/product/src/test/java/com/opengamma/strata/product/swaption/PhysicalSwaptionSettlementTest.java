/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.common.SettlementType;

/**
 * Test {@link PhysicalSwaptionSettlement}.
 */
@Test
public class PhysicalSwaptionSettlementTest {

  //-------------------------------------------------------------------------
  public void test_DEFAULT() {
    PhysicalSwaptionSettlement test = PhysicalSwaptionSettlement.DEFAULT;
    assertEquals(test.getSettlementType(), SettlementType.PHYSICAL);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PhysicalSwaptionSettlement test = PhysicalSwaptionSettlement.DEFAULT;
    coverImmutableBean(test);
  }

  public void test_serialization() {
    PhysicalSwaptionSettlement test = PhysicalSwaptionSettlement.DEFAULT;
    assertSerialization(test);
  }

}
