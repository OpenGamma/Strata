/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.Sets;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ConfigUtilsTest {

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
    Set<Class<?>> supertypes = Sets.newLinkedHashSet();
    supertypes.add(C1.class);
    supertypes.add(C2.class);
    supertypes.add(C3.class);
    supertypes.add(Object.class);
    supertypes.add(I1.class);
    supertypes.add(I2.class);
    supertypes.add(I3.class);
    supertypes.add(I4.class);
    supertypes.add(I5.class);
    assertEquals(supertypes, ConfigUtils.getSupertypes(C1.class));
  }

  @Test
  public void getInterfaces() {
    Set<Class<?>> interfaces = Sets.newLinkedHashSet();
    interfaces.add(I1.class);
    interfaces.add(I2.class);
    interfaces.add(I3.class);
    interfaces.add(I4.class);
    interfaces.add(I5.class);
    assertEquals(interfaces, ConfigUtils.getInterfaces(C1.class));
  }
}
