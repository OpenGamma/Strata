/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link EtdContractCode}.
 */
@Test
public class EtdContractCodeTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    EtdContractCode test = EtdContractCode.of("test");
    assertEquals(test.toString(), "test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    EtdContractCode a = EtdContractCode.of("test");
    EtdContractCode a2 = EtdContractCode.of("test");
    EtdContractCode b = EtdContractCode.of("test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
