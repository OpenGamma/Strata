/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link ValueDerivatives}.
 */
@Test
public class ValueDerivativesTest {

  private static final double VALUE = 123.4;
  private static final DoubleArray DERIVATIVES = DoubleArray.of(1.0, 2.0, 3.0);

  public void test_of() {
    ValueDerivatives test = ValueDerivatives.of(VALUE, DERIVATIVES);
    assertEquals(test.getValue(), VALUE, 0);
    assertEquals(test.getDerivatives(), DERIVATIVES);
    assertEquals(test.getDerivative(0), DERIVATIVES.get(0));
    assertEquals(test.getDerivative(1), DERIVATIVES.get(1));
    assertEquals(test.getDerivative(2), DERIVATIVES.get(2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ValueDerivatives test = ValueDerivatives.of(VALUE, DERIVATIVES);
    coverImmutableBean(test);
    assertNotNull(ValueDerivatives.meta());
    ValueDerivatives test2 = ValueDerivatives.of(123.4, DoubleArray.of(1.0, 2.0, 3.0));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ValueDerivatives test = ValueDerivatives.of(VALUE, DERIVATIVES);
    assertSerialization(test);
  }

}
