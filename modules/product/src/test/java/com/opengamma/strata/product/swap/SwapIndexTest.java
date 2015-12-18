package com.opengamma.strata.product.swap;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test swap index.
 */
@Test
public class SwapIndexTest {

  public void test() {
    ImmutableMap<String, SwapIndex> mapAll = SwapIndices.ENUM_LOOKUP.lookupAll();
    ImmutableList<SwapIndex> indexAll = mapAll.values().asList();
  }
}
