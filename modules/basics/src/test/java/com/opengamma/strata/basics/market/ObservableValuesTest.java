/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link ObservableValues}.
 */
@Test
public class ObservableValuesTest {

  private static ObservableKey KEY1 = TestObservableKey.of("1");
  private static ObservableKey KEY2 = TestObservableKey.of("2");
  private static ObservableKey KEY3 = TestObservableKey.of("3");
  private static ObservableId ID1 = KEY1.toObservableId(MarketDataFeed.NONE);
  private static ObservableId ID2 = KEY2.toObservableId(MarketDataFeed.NONE);

  public void test_of_map() {
    ObservableValues test = ObservableValues.of(ImmutableMap.of(KEY1, 1d, KEY2, 2d));
    assertEquals(test.containsValue(KEY1), true);
    assertEquals(test.containsValue(KEY2), true);
    assertEquals(test.containsValue(KEY3), false);
    assertEquals(test.getValue(KEY1), 1d, 0d);
    assertEquals(test.getValue(KEY2), 2d, 0d);
    assertThrowsIllegalArg(() -> test.getValue(KEY3));
  }

  public void test_ofIdMap_map() {
    ObservableValues test = ObservableValues.ofIdMap(ImmutableMap.of(ID1, 1d, ID2, 2d));
    assertEquals(test.containsValue(KEY1), true);
    assertEquals(test.containsValue(KEY2), true);
    assertEquals(test.containsValue(KEY3), false);
    assertEquals(test.getValue(KEY1), 1d, 0d);
    assertEquals(test.getValue(KEY2), 2d, 0d);
    assertThrowsIllegalArg(() -> test.getValue(KEY3));
  }

  public void test_of_single() {
    ObservableValues test = ObservableValues.of(KEY1, 1d);
    assertEquals(test.containsValue(KEY1), true);
    assertEquals(test.containsValue(KEY2), false);
    assertEquals(test.containsValue(KEY3), false);
    assertEquals(test.getValue(KEY1), 1d, 0d);
    assertThrowsIllegalArg(() -> test.getValue(KEY2));
    assertThrowsIllegalArg(() -> test.getValue(KEY3));
  }

}
