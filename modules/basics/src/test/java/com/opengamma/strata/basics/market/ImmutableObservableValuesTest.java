/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link ImmutableObservableValues}.
 */
@Test
public class ImmutableObservableValuesTest {

  private static final String ID_1 = "ID 1";
  private static final String ID_2 = "ID 2";
  private static final double VALUE_1 = 1.0;
  private static final double VALUE_2 = 2.0;
  private static final Map<TestObservableKey, Double> VALUES = ImmutableMap.of(
      TestObservableKey.of(ID_1), VALUE_1,
      TestObservableKey.of(ID_2), VALUE_2);

  public void test_null_map() {
    assertThrowsIllegalArg(() -> ImmutableObservableValues.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    ImmutableObservableValues test = ImmutableObservableValues.of(VALUES);
    assertEquals(test.getValues(), VALUES);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableObservableValues test = ImmutableObservableValues.of(VALUES);
    coverImmutableBean(test);
    Map<ObservableKey, Double> valuesOther = new HashMap<>();
    valuesOther.put(TestObservableKey.of(ID_1), VALUE_1);
    valuesOther.put(TestObservableKey.of(ID_2), VALUE_2);
    ImmutableObservableValues test2 = ImmutableObservableValues.of(valuesOther);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ImmutableObservableValues test = ImmutableObservableValues.of(VALUES);
    assertSerialization(test);
  }

}
