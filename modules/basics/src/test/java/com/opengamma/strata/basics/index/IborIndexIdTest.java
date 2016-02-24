/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ImmutableReferenceData;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.ReferenceDataNotFoundException;

/**
 * Test {@link IborIndexId}.
 */
@Test
public class IborIndexIdTest {

  public void test_of_single() {
    IborIndexId test = IborIndexId.of("GB");
    assertEquals(test.getName(), "GB");
    assertEquals(test.getReferenceDataType(), IborIndex.class);
    assertEquals(test.toString(), "GB");
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    IborIndexId gbp3m = IborIndexIds.GBP_LIBOR_3M;
    IborIndexId gbp6m = IborIndexIds.GBP_LIBOR_6M;
    IborIndex index = IborIndices.GBP_LIBOR_3M;
    ReferenceData refData = ImmutableReferenceData.of(gbp3m, index);
    assertEquals(gbp3m.resolve(refData), index);
    assertThrows(() -> gbp6m.resolve(refData), ReferenceDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    IborIndexId a = IborIndexId.of("GB");
    IborIndexId a2 = IborIndexId.of("GB");
    IborIndexId b = IborIndexId.of("EU");
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(IborIndexIds.class);
  }

  public void test_serialization() {
    IborIndexId test = IborIndexId.of("US");
    assertSerialization(test);
  }

}
