/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.Sets;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ViewFactoryFunctionUtilsTest {

  interface I1 {}
  interface I2 {}
  interface I3 extends I4 {}
  interface I4 extends I5 {}
  interface I5 {}
  class C1 extends C2 implements I1, I2 {}
  class C2 extends C3 {}
  class C3 implements I3 {}

  @Test
  public void getSupertypes() {
    Set<Class<?>> expectedSupertypes = Sets.newLinkedHashSet();
    expectedSupertypes.add(C1.class);
    expectedSupertypes.add(C2.class);
    expectedSupertypes.add(C3.class);
    expectedSupertypes.add(Object.class);
    expectedSupertypes.add(I1.class);
    expectedSupertypes.add(I2.class);
    expectedSupertypes.add(I3.class);
    expectedSupertypes.add(I4.class);
    expectedSupertypes.add(I5.class);
    Set<Class<?>> supertypes = EngineUtils.getSupertypes(C1.class);
    assertEquals(expectedSupertypes, supertypes);
    // check that results are cached
    assertSame(supertypes, EngineUtils.getSupertypes(C1.class));
  }

  @Test
  public void getInterfaces() {
    Set<Class<?>> expectedInterfaces = Sets.newLinkedHashSet();
    expectedInterfaces.add(I1.class);
    expectedInterfaces.add(I2.class);
    expectedInterfaces.add(I3.class);
    expectedInterfaces.add(I4.class);
    expectedInterfaces.add(I5.class);
    Set<Class<?>> interfaces = EngineUtils.getInterfaces(C1.class);
    assertEquals(expectedInterfaces, interfaces);
    // check that results are cached
    assertSame(interfaces, EngineUtils.getInterfaces(C1.class));
  }
}
