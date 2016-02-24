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
 * Test {@link FxIndexId}.
 */
@Test
public class FxIndexIdTest {

  public void test_of_single() {
    FxIndexId test = FxIndexId.of("GB");
    assertEquals(test.getName(), "GB");
    assertEquals(test.getReferenceDataType(), FxIndex.class);
    assertEquals(test.toString(), "GB");
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxIndexId gbp3m = FxIndexIds.GBP_USD_WM;
    FxIndexId gbp6m = FxIndexIds.USD_CHF_WM;
    FxIndex index = FxIndices.GBP_USD_WM;
    ReferenceData refData = ImmutableReferenceData.of(gbp3m, index);
    assertEquals(gbp3m.resolve(refData), index);
    assertThrows(() -> gbp6m.resolve(refData), ReferenceDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    FxIndexId a = FxIndexId.of("GB");
    FxIndexId a2 = FxIndexId.of("GB");
    FxIndexId b = FxIndexId.of("EU");
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FxIndexIds.class);
  }

  public void test_serialization() {
    FxIndexId test = FxIndexId.of("US");
    assertSerialization(test);
  }

}
