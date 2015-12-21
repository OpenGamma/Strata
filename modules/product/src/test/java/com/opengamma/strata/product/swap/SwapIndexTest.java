/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Test {@link SwapIndex}.
 */
@Test
public class SwapIndexTest {

  public void test_notFound() {
    assertThrowsIllegalArg(() -> SwapIndex.of("foo"));
  }

  public void test_swapIndicies() {
    ImmutableMap<String, SwapIndex> mapAll = SwapIndices.ENUM_LOOKUP.lookupAll();
    ImmutableList<SwapIndex> indexAll = mapAll.values().asList();
    ImmutableList<String> nameAll = mapAll.keySet().asList();
    int size = indexAll.size();
    for (int i = 0; i < size; ++i) {
      // check no duplication
      for (int j = i + 1; j < size; ++j) {
        assertFalse(nameAll.get(i).equals(nameAll.get(j)));
        assertFalse(indexAll.get(i).equals(indexAll.get(j)));
      }
    }
    for (String name : nameAll) {
      SwapIndex index = mapAll.get(name);
      assertEquals(SwapIndex.of(name), index);
      FixedIborSwapTemplate temp = index.getTemplate();
      FixedIborSwapConvention conv = temp.getConvention();
      Tenor tenor = temp.getTenor();
      // test consistency between name and template
      assertTrue(name.contains(tenor.toString()));
      if (name.startsWith("USD")) {
        assertTrue(name.contains("1100") || name.contains("1500"));
        assertTrue(conv.equals(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M));
      }
      if (name.startsWith("JPY")) {
        assertTrue(name.contains("1000") || name.contains("1500"));
        assertTrue(conv.equals(FixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M));
      }
      if (name.startsWith("GBP")) {
        assertTrue(name.contains("1100"));
        if (tenor.equals(Tenor.TENOR_1Y)) {
          assertTrue(conv.equals(FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M));
        } else {
          assertTrue(conv.equals(FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M));
        }
      }
      if (name.startsWith("EUR")) {
        assertTrue(name.contains("1100") || name.contains("1200"));
        if (tenor.equals(Tenor.TENOR_1Y)) {
          assertTrue(conv.equals(FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M));
        } else {
          assertTrue(conv.equals(FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M));
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableSwapIndex index = ImmutableSwapIndex.builder()
        .name("FooIndex")
        .template(FixedIborSwapTemplate.of(Tenor.TENOR_9M, FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M))
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(SwapIndices.class);
  }

  public void test_serialization() {
    SwapIndex index = ImmutableSwapIndex.builder()
        .name("FooIndex")
        .template(FixedIborSwapTemplate.of(Tenor.TENOR_9M, FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M))
        .build();
    assertSerialization(index);
  }

}
