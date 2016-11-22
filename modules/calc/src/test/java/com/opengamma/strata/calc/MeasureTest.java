/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsRuntime;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.joda.convert.StringConvert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link Measure}.
 */
@Test
public class MeasureTest {

  public void test_extendedEnum() {
    ImmutableMap<String, Measure> map = Measure.extendedEnum().lookupAll();
    assertEquals(map.size(), 0);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> Measure.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsRuntime(() -> Measure.of(null));
  }

  //-------------------------------------------------------------------------

  public void test_isConvertible() {
    assertTrue(StringConvert.INSTANCE.isConvertible(Measure.class));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableMeasure test = ImmutableMeasure.of("A");
    coverImmutableBean(test);
    ImmutableMeasure test2 = ImmutableMeasure.of("B", false);
    coverBeanEquals(test, test2);

    coverPrivateConstructor(MeasureHelper.class);
  }

}
