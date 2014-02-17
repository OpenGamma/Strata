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

import com.google.common.collect.Sets;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class AvailableImplementationsImplTest {

  private static final String O1 = "O1";
  private static final String O2 = "O2";

  @Test
  public void getImplementations() {
    AvailableImplementations impls = new AvailableImplementationsImpl();
    impls.register(F1Impl1.class, F1Impl2.class, F2Impl1.class);
    assertEquals(Sets.<Class<?>>newHashSet(F1Impl1.class, F1Impl2.class), impls.getImplementationTypes(F1.class));
    assertEquals(Sets.<Class<?>>newHashSet(F2Impl1.class), impls.getImplementationTypes(F2.class));
    assertEquals(Collections.<Class<?>>emptySet(), impls.getImplementationTypes(Runnable.class));
  }

  /**
   * Check the repo returns an implementation for an interface where there's only one implementation registered
   */
  @Test
  public void inferSingleImplementation() {
    AvailableImplementations impls = new AvailableImplementationsImpl();
    impls.register(F1Impl1.class, F2Impl1.class);
    assertEquals(F1Impl1.class, impls.getDefaultImplementation(F1.class));
    assertEquals(F2Impl1.class, impls.getDefaultImplementation(F2.class));
  }

  /**
   * Can't infer the implementation type if there are multiple known implementations.
   */
  @Test
  public void cantInferMultipleImplementations() {
    AvailableImplementations impls = new AvailableImplementationsImpl();
    impls.register(F1Impl1.class, F1Impl2.class);
    assertNull(impls.getDefaultImplementation(F1.class));
  }

  class Target1 { }
  class Target2 extends Target1 { }
  class Target3 extends Target2 { }

  interface F1 {

    @Output(O1)
    Object execute(Target1 target);
  }

  interface F2 {

    @Output(O2)
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
}
