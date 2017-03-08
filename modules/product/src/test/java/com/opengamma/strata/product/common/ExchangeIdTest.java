/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.common.ExchangeId;
import com.opengamma.strata.product.common.ExchangeIds;

/**
 * Test {@link ExchangeId}.
 */
@Test
public class ExchangeIdTest {

  public void test_of() {
    ExchangeId test = ExchangeId.of("GB");
    assertEquals(test.getName(), "GB");
    assertEquals(test.toString(), "GB");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    ExchangeId a = ExchangeId.of("ECAG");
    ExchangeId a2 = ExchangeId.of("ECAG");
    ExchangeId b = ExchangeId.of("XLON");
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(ExchangeIds.class);
  }

  public void test_serialization() {
    ExchangeId test = ExchangeId.of("ECAG");
    assertSerialization(test);
  }

}
