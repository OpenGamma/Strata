/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link EtdContractSpecId}.
 */
@Test
public class EtdContractSpecIdTest {

  public void test_of() {
    EtdContractSpecId test = EtdContractSpecId.of(StandardId.of("A", "B"));
    assertEquals(test.getStandardId(), StandardId.of("A", "B"));
    assertEquals(test.getReferenceDataType(), EtdContractSpec.class);
    assertEquals(test.toString(), "A~B");
  }

  public void test_parse() {
    EtdContractSpecId test = EtdContractSpecId.parse("A~B");
    assertEquals(test.getStandardId(), StandardId.of("A", "B"));
    assertEquals(test.toString(), "A~B");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    EtdContractSpecId a = EtdContractSpecId.of(StandardId.of("A", "B"));
    EtdContractSpecId a2 = EtdContractSpecId.of(StandardId.of("A", "B"));
    EtdContractSpecId b = EtdContractSpecId.of(StandardId.of("C", "D"));
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    EtdContractSpecId test = EtdContractSpecId.of("A", "B");
    assertSerialization(test);
  }

}
