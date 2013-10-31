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

  interface F1 {

    @Output(MapFunctionRepoTest.O1)
    Object execute(@Target Target1 target);
  }

  class F1Impl implements F1 {

    @Override
    public Object execute(Target1 target) {
      return null;
    }
  }

  interface F2 {

    @Output(MapFunctionRepoTest.O2)
    Object execute(@Target Target2 target);
  }

  class F2Impl implements F2 {

    @Override
    public Object execute(Target2 target) {
      return null;
    }
  }

  @Test
  public void getFunctionType() {
    MapFunctionRepo repo = new MapFunctionRepo();
    repo.register(F1.class);
    repo.register(F2.class);
    assertEquals(F1.class, repo.getOutputFunction(O1, Target1.class));
    assertEquals(F1.class, repo.getOutputFunction(O1, Target2.class));
    assertEquals(F1.class, repo.getOutputFunction(O1, Target3.class));
    assertEquals(F2.class, repo.getOutputFunction(O2, Target2.class));
    assertEquals(F2.class, repo.getOutputFunction(O2, Target3.class));
  }
}
