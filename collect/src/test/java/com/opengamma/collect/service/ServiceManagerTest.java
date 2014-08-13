/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import static com.opengamma.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

/**
 * Test {@link ServiceManager}.
 */
@Test
public class ServiceManagerTest {

  public void test_getContext_default() {
    ServiceContext test = ServiceManager.getContext();
    assertNotNull(test);
  }

  public void test_init_clear() {
    try {
      ServiceContext base = ServiceContext.of(CharSequence.class, "HelloWorld");
      ServiceContext test1 = ServiceManager.getContext();
      assertNotNull(test1);
      
      ServiceManager.init(base);
      ServiceContext test2 = ServiceManager.getContext();
      assertSame(test2, base);
      
      ServiceManager.clear();
      ServiceContext test3 = ServiceManager.getContext();
      assertNotNull(test3);
      assertSame(test3, test1);
      
    } finally {
      // try to cleanup in case of problems
      ServiceManager.clear();
    }
  }

  //-------------------------------------------------------------------------
  public void test_addServiceToDefault() {
    assertEquals(ServiceManager.getContext().contains(ServiceManagerTest.class), false);
    ServiceManager.addServiceToDefault(ServiceManagerTest.class, this);
    assertEquals(ServiceManager.getContext().contains(ServiceManagerTest.class), true);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(ServiceManager.class);
  }

}
