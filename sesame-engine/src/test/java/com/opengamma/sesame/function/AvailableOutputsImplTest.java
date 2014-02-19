/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.sesame.OutputName;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class AvailableOutputsImplTest {

  private static final String O1 = "O1";
  private static final OutputName O1_NAME = OutputName.of(O1);
  private static final String O2 = "O2";
  private static final OutputName O2_NAME = OutputName.of(O2);
  private static final String O3 = "O3";
  private static final OutputName O3_NAME = OutputName.of(O3);
  private static final String O4 = "O4";
  private static final OutputName O4_NAME = OutputName.of(O4);

  private static final ImmutableSet<Class<?>> s_inputTypes =
      ImmutableSet.<Class<?>>of(Target1.class, Target2.class, Target3.class);

  @Test
  public void getAvailableOutputs() {
    AvailableOutputs outputs = new AvailableOutputsImpl(s_inputTypes);
    outputs.register(F1.class, F2.class);
    assertEquals(ImmutableSortedSet.of(O1_NAME, O2_NAME), outputs.getAvailableOutputs(Target3.class));
    assertEquals(ImmutableSortedSet.of(O1_NAME, O2_NAME), outputs.getAvailableOutputs(Target2.class));
    assertEquals(ImmutableSortedSet.of(O1_NAME), outputs.getAvailableOutputs(Target1.class));
    assertEquals(Collections.<String>emptySet(), outputs.getAvailableOutputs(Object.class));
  }

  @Test
  public void getFunctionType() {
    AvailableOutputs outputs = new AvailableOutputsImpl(s_inputTypes);
    outputs.register(F1.class, F2.class, F3.class, F4.class);
    assertEquals(F1.class, outputs.getOutputFunction(O1_NAME, Target1.class).getDeclaringType());
    assertEquals(F1.class, outputs.getOutputFunction(O1_NAME, Target2.class).getDeclaringType());
    assertEquals(F1.class, outputs.getOutputFunction(O1_NAME, Target3.class).getDeclaringType());
    assertEquals(F2.class, outputs.getOutputFunction(O2_NAME, Target2.class).getDeclaringType());
    assertEquals(F2.class, outputs.getOutputFunction(O2_NAME, Target3.class).getDeclaringType());
    assertEquals(F3.class, outputs.getOutputFunction(O3_NAME).getDeclaringType());
    assertEquals(F4.class, outputs.getOutputFunction(O4_NAME).getDeclaringType());
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

  interface F3 {

    @Output(O3)
    Object execute();
  }

  interface F4 {

    @Output(O4)
    Object execute(String notTheTarget);
  }
}
