/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class MapFunctionRepoTest {

  private static final String O1 = "O1";
  private static final String O2 = "O2";
  private static final ImmutableSet<Class<?>> s_inputTypes =
      ImmutableSet.<Class<?>>of(Target1.class, Target2.class, Target3.class);

  @Test
  public void getAvailableOutputs() {
    MapFunctionRepo repo = new MapFunctionRepo(s_inputTypes, Collections.<Class<?>, Class<?>>emptyMap());
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
    Object execute(Target1 target);
  }

  interface F2 {

    @Output(MapFunctionRepoTest.O2)
    Object execute(Target2 target);
  }

  class F1Impl1 implements F1 {

    @Override
    public Object execute(Target1 target) {
      return null;
    }
  }

  class F1Impl2 implements F1 {

    @Override
    public Object execute(Target1 target) {
      return null;
    }
  }

  class F2Impl1 implements F2 {

    @Override
    public Object execute(Target2 target) {
      return null;
    }
  }

  @Test
  public void getFunctionType() {
    MapFunctionRepo repo = new MapFunctionRepo(s_inputTypes, Collections.<Class<?>, Class<?>>emptyMap());
    repo.register(F1.class);
    repo.register(F2.class);
    assertEquals(F1.class, repo.getOutputFunction(O1, Target1.class).getDeclaringType());
    assertEquals(F1.class, repo.getOutputFunction(O1, Target2.class).getDeclaringType());
    assertEquals(F1.class, repo.getOutputFunction(O1, Target3.class).getDeclaringType());
    assertEquals(F2.class, repo.getOutputFunction(O2, Target2.class).getDeclaringType());
    assertEquals(F2.class, repo.getOutputFunction(O2, Target3.class).getDeclaringType());
  }

  @Test
  public void specifyDefaultImplementations() {
    Map<Class<?>, Class<?>> defaultImpls = ImmutableMap.<Class<?>, Class<?>>of(F1.class, F1Impl2.class);
    MapFunctionRepo repo = new MapFunctionRepo(s_inputTypes, defaultImpls);
    assertEquals(F1Impl2.class, repo.getDefaultImplementationType(F1.class));
  }

  /**
   * Check the repo returns an implementation for an interface where there's only one implementation registered
   */
  @Test
  public void inferDefaultImplementation() {
    // TODO this doesn't work yet
  }
}
