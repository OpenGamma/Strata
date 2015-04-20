/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.finance.rate.swap.MockSwapLeg.MOCK_EXPANDED_GBP1;
import static com.opengamma.strata.finance.rate.swap.MockSwapLeg.MOCK_EXPANDED_USD1;
import static com.opengamma.strata.finance.rate.swap.MockSwapLeg.MOCK_GBP1;
import static com.opengamma.strata.finance.rate.swap.MockSwapLeg.MOCK_GBP2;
import static com.opengamma.strata.finance.rate.swap.MockSwapLeg.MOCK_USD1;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.FIXED;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.IBOR;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.OTHER;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.OVERNIGHT;
import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Optional;

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
        .legs(ImmutableList.of(MOCK_GBP1, MOCK_USD1))
        .build();
    assertEquals(test.getLegs(), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
  }

  public void test_builder_varargs() {
    Swap test = Swap.builder()
        .legs(MOCK_GBP1, MOCK_USD1)
        .build();
    assertEquals(test.getLegs(), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
  }

  public void test_of_varargs() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertEquals(test.getLegs(), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
    assertEquals(ImmutableList.copyOf(test.getLegs()), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
    assertThrowsIllegalArg(() -> Swap.of((SwapLeg[]) null));
  }

  public void test_of_list() {
    Swap test = Swap.of(ImmutableList.of(MOCK_GBP1, MOCK_USD1));
    assertEquals(test.getLegs(), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
    assertEquals(ImmutableList.copyOf(test.getLegs()), ImmutableList.of(MOCK_GBP1, MOCK_USD1));
    assertThrowsIllegalArg(() -> Swap.of((List<SwapLeg>) null));
  }

  //-------------------------------------------------------------------------
  public void test_getLegs_SwapLegType() {
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(FIXED), ImmutableList.of(MOCK_GBP1));
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(IBOR), ImmutableList.of(MOCK_USD1));
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(OVERNIGHT), ImmutableList.of());
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getLegs(OTHER), ImmutableList.of());
  }

  public void test_getLeg_PayReceive() {
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getLeg(PAY), Optional.of(MOCK_GBP1));
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getLeg(RECEIVE), Optional.of(MOCK_USD1));
    assertEquals(Swap.of(MOCK_GBP1).getLeg(PAY), Optional.of(MOCK_GBP1));
    assertEquals(Swap.of(MOCK_USD1).getLeg(PAY), Optional.empty());
    assertEquals(Swap.of(MOCK_GBP1).getLeg(RECEIVE), Optional.empty());
    assertEquals(Swap.of(MOCK_USD1).getLeg(RECEIVE), Optional.of(MOCK_USD1));
  }

  public void test_getPayLeg() {
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getPayLeg(), Optional.of(MOCK_GBP1));
    assertEquals(Swap.of(MOCK_GBP1).getPayLeg(), Optional.of(MOCK_GBP1));
    assertEquals(Swap.of(MOCK_USD1).getPayLeg(), Optional.empty());
  }

  public void test_getReceiveLeg() {
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).getReceiveLeg(), Optional.of(MOCK_USD1));
    assertEquals(Swap.of(MOCK_GBP1).getReceiveLeg(), Optional.empty());
    assertEquals(Swap.of(MOCK_USD1).getReceiveLeg(), Optional.of(MOCK_USD1));
  }

  //-------------------------------------------------------------------------
  public void test_isCrossCurrency() {
    assertEquals(Swap.of(MOCK_GBP1, MOCK_USD1).isCrossCurrency(), true);
    assertEquals(Swap.of(MOCK_GBP1, MOCK_GBP2, MOCK_USD1).isCrossCurrency(), true);
    assertEquals(Swap.of(MOCK_GBP1, MOCK_GBP2).isCrossCurrency(), false);
    assertEquals(Swap.of(MOCK_GBP1).isCrossCurrency(), false);
  }

  //-------------------------------------------------------------------------
  public void test_allIndices() {
    Swap test = Swap.of(MOCK_GBP1, MOCK_USD1);
    assertEquals(test.allIndices(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    Swap test = Swap.builder()
        .legs(ImmutableList.of(MOCK_GBP1, MOCK_USD1))
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
