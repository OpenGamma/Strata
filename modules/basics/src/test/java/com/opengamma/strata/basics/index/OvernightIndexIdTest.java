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
 * Test {@link OvernightIndexId}.
 */
@Test
public class OvernightIndexIdTest {

  public void test_of_single() {
    OvernightIndexId test = OvernightIndexId.of("GB");
    assertEquals(test.getName(), "GB");
    assertEquals(test.getReferenceDataType(), OvernightIndex.class);
    assertEquals(test.toString(), "GB");
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    OvernightIndexId gbp3m = OvernightIndexIds.GBP_SONIA;
    OvernightIndexId gbp6m = OvernightIndexIds.CHF_TOIS;
    OvernightIndex index = OvernightIndices.GBP_SONIA;
    ReferenceData refData = ImmutableReferenceData.of(gbp3m, index);
    assertEquals(gbp3m.resolve(refData), index);
    assertThrows(() -> gbp6m.resolve(refData), ReferenceDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    OvernightIndexId a = OvernightIndexId.of("GB");
    OvernightIndexId a2 = OvernightIndexId.of("GB");
    OvernightIndexId b = OvernightIndexId.of("EU");
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(OvernightIndexIds.class);
  }

  public void test_serialization() {
    OvernightIndexId test = OvernightIndexId.of("US");
    assertSerialization(test);
  }

}
