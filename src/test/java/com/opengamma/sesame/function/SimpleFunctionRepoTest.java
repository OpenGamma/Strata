/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class SimpleFunctionRepoTest {

  private static final String O1 = "O1";
  private static final String O2 = "O2";
  private static final ImmutableSet<Class<?>> s_inputTypes =
      ImmutableSet.<Class<?>>of(Target1.class, Target2.class, Target3.class);

  @Test
  public void getAvailableOutputs() {
    SimpleFunctionRepo repo = new SimpleFunctionRepo(s_inputTypes);
    repo.register(F1.class, F2.class);
    assertEquals(ImmutableSortedSet.of(O1, O2), repo.getAvailableOutputs(Target3.class));
    assertEquals(ImmutableSortedSet.of(O1, O2), repo.getAvailableOutputs(Target2.class));
    assertEquals(ImmutableSortedSet.of(O1), repo.getAvailableOutputs(Target1.class));
    assertEquals(Collections.<String>emptySet(), repo.getAvailableOutputs(Object.class));
  }

  class Target1 { }
  class Target2 extends Target1 { }
  class Target3 extends Target2 { }

  interface F1 {

    @Output(SimpleFunctionRepoTest.O1)
    Object execute(Target1 target);
  }

  interface F2 {

    @Output(SimpleFunctionRepoTest.O2)
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
    SimpleFunctionRepo repo = new SimpleFunctionRepo(s_inputTypes);
    repo.register(F1.class, F2.class);
    assertEquals(F1.class, repo.getOutputFunction(O1, Target1.class).getDeclaringType());
    assertEquals(F1.class, repo.getOutputFunction(O1, Target2.class).getDeclaringType());
    assertEquals(F1.class, repo.getOutputFunction(O1, Target3.class).getDeclaringType());
    assertEquals(F2.class, repo.getOutputFunction(O2, Target2.class).getDeclaringType());
    assertEquals(F2.class, repo.getOutputFunction(O2, Target3.class).getDeclaringType());
  }

  @Test
  public void getImplementations() {
    SimpleFunctionRepo repo = new SimpleFunctionRepo(s_inputTypes);
    repo.register(F1Impl1.class, F1Impl2.class, F2Impl1.class);
    assertEquals(Sets.<Class<?>>newHashSet(F1Impl1.class, F1Impl2.class), repo.getImplementationTypes(F1.class));
    assertEquals(Sets.<Class<?>>newHashSet(F2Impl1.class), repo.getImplementationTypes(F2.class));
    assertEquals(Collections.<Class<?>>emptySet(), repo.getImplementationTypes(Runnable.class));
  }

  /**
   * Check the repo returns an implementation for an interface where there's only one implementation registered
   */
  @Test
  public void inferSingleImplementation() {
    SimpleFunctionRepo repo = new SimpleFunctionRepo(s_inputTypes);
    repo.register(F1Impl1.class, F2Impl1.class);
    assertEquals(F1Impl1.class, repo.getDefaultImplementation(F1.class));
    assertEquals(F2Impl1.class, repo.getDefaultImplementation(F2.class));
  }

  /**
   * Can't infer the implementation type if there are multiple known implementations.
   */
  @Test
  public void cantInferMultipleImplementations() {
    SimpleFunctionRepo repo = new SimpleFunctionRepo(s_inputTypes);
    repo.register(F1Impl1.class, F1Impl2.class);
    assertNull(repo.getDefaultImplementation(F1.class));
  }
}
