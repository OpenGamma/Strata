/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.util.test.TestGroup;

/**
 *
 */
@Test(groups = TestGroup.UNIT)
public class MapFunctionRepoTest {

  @Test
  public void availableOutputs() {
    MapFunctionRepo repo = new MapFunctionRepo();
    repo.register(I1.class);
    repo.register(I2.class);
    assertEquals(ImmutableSortedSet.of("I1", "I2"), repo.getAvailableOutputs(C3.class));
    assertEquals(ImmutableSortedSet.of("I1", "I2"), repo.getAvailableOutputs(C2.class));
    assertEquals(ImmutableSortedSet.of("I1"), repo.getAvailableOutputs(C1.class));
  }
}

class C1 {}
class C2 extends C1 {}
class C3 extends C2 {}
@DefaultImplementation(Object.class) interface I1 { @EngineFunction("I1") void fn(@Target C1 target); }
@DefaultImplementation(Object.class) interface I2 { @EngineFunction("I2") void fn(@Target C2 target); }
