/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.sesame.MarketData;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class MapFunctionRepoTest {

  protected static final String O1 = "O1";
  protected static final String O2 = "O2";

  @Test
  public void getAvailableOutputs() {
    MapFunctionRepo repo = new MapFunctionRepo();
    repo.register(F1.class);
    repo.register(F2.class);
    assertEquals(ImmutableSortedSet.of(O1, O2), repo.getAvailableOutputs(Target3.class));
    assertEquals(ImmutableSortedSet.of(O1, O2), repo.getAvailableOutputs(Target2.class));
    assertEquals(ImmutableSortedSet.of(O1), repo.getAvailableOutputs(Target1.class));
    assertEquals(Collections.<String>emptySet(), repo.getAvailableOutputs(Object.class));
  }

  class Target1 { }
  class Target2 extends Target1 { }
  class Target3 extends Target2 { }

  @OutputName(MapFunctionRepoTest.O1)
  @DefaultImplementation(F1Impl.class)
  interface F1 extends OutputFunction<Target1, Object> { }

  class F1Impl implements F1 {

    @Override
    public Object execute(MarketData marketData, Target1 target) {
      return null;
    }
  }

  @OutputName(MapFunctionRepoTest.O2)
  @DefaultImplementation(F2Impl.class)
  interface F2 extends OutputFunction<Target2, Object> { }

  class F2Impl implements F2 {

    @Override
    public Object execute(MarketData marketData, Target2 target) {
      return null;
    }
  }

  @Test
  public void getFunctionType() {
    MapFunctionRepo repo = new MapFunctionRepo();
    repo.register(F1.class);
    repo.register(F2.class);
    assertEquals(F1.class, repo.getFunctionType(O1, Target1.class));
    assertEquals(F1.class, repo.getFunctionType(O1, Target2.class));
    assertEquals(F1.class, repo.getFunctionType(O1, Target3.class));
    assertEquals(F2.class, repo.getFunctionType(O2, Target2.class));
    assertEquals(F2.class, repo.getFunctionType(O2, Target3.class));
  }
}
