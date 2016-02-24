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
 * Test {@link PriceIndexId}.
 */
@Test
public class PriceIndexIdTest {

  public void test_of_single() {
    PriceIndexId test = PriceIndexId.of("GB");
    assertEquals(test.getName(), "GB");
    assertEquals(test.getReferenceDataType(), PriceIndex.class);
    assertEquals(test.toString(), "GB");
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    PriceIndexId gbp3m = PriceIndexIds.GB_HICP;
    PriceIndexId gbp6m = PriceIndexIds.CH_CPI;
    PriceIndex index = PriceIndices.GB_HICP;
    ReferenceData refData = ImmutableReferenceData.of(gbp3m, index);
    assertEquals(gbp3m.resolve(refData), index);
    assertThrows(() -> gbp6m.resolve(refData), ReferenceDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    PriceIndexId a = PriceIndexId.of("GB");
    PriceIndexId a2 = PriceIndexId.of("GB");
    PriceIndexId b = PriceIndexId.of("EU");
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(PriceIndexIds.class);
  }

  public void test_serialization() {
    PriceIndexId test = PriceIndexId.of("US");
    assertSerialization(test);
  }

}
