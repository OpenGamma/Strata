/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate.swap;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.platform.finance.rate.swap.MockSwapLeg.MOCK_EXPANDED_GBP1;
import static com.opengamma.platform.finance.rate.swap.MockSwapLeg.MOCK_EXPANDED_USD1;
import static com.opengamma.platform.finance.rate.swap.MockSwapLeg.MOCK_GBP1;
import static com.opengamma.platform.finance.rate.swap.MockSwapLeg.MOCK_GBP2;
import static com.opengamma.platform.finance.rate.swap.MockSwapLeg.MOCK_USD1;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Test.
 */
@Test
public class SwapTest {

  public void test_builder_list() {
    Swap test = Swap.builder()
        .legs(ImmutableSet.of(MOCK_GBP1, MOCK_USD1))
        .build();
    assertEquals(test.getLegs(), ImmutableSet.of(MOCK_GBP1, MOCK_USD1));
  }

  public void test_builder_varargs() {
    Swap test = Swap.builder()
        .legs(MOCK_GBP1, MOCK_USD1)
        .build();
    assertEquals(test.getLegs(), ImmutableSet.of(MOCK_GBP1, MOCK_USD1));
  }

  public void test_of_varargs() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertEquals(test.getLegs(), ImmutableSet.of(MOCK_GBP1, MOCK_USD1));
    // ensure order retained indirectly via ImmutableSet
    assertEquals(ImmutableList.copyOf(test.getLegs()), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> Swap.of((SwapLeg[]) null));
  }

  //-------------------------------------------------------------------------
  public void test_isCrossCurrency() {
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).isCrossCurrency(), true);
    assertEquals(Swap.of(MOCK_GBP1, MOCK_GBP2, MOCK_USD1).isCrossCurrency(), true);
    assertEquals(Swap.of(MOCK_GBP1, MOCK_GBP2).isCrossCurrency(), false);
    assertEquals(Swap.of(MOCK_GBP1).isCrossCurrency(), false);
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    Swap test = Swap.builder()
        .legs(ImmutableSet.of(MOCK_GBP1, MOCK_USD1))
        .build();
    assertEquals(test.expand(), ExpandedSwap.of(MOCK_EXPANDED_GBP1, MOCK_EXPANDED_USD1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    coverImmutableBean(test);
    Swap test2 = Swap.of(MOCK_GBP1);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertSerialization(test);
  }

}
