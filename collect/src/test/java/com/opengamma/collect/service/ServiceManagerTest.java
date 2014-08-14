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
 * Test {@link ServiceContext}.
 */
@Test
public class ServiceManagerTest {

  public void test_getContext_default() {
    ServiceContextMap test = ServiceContext.getMap();
    assertNotNull(test);
  }

  public void test_init_clear() {
    try {
      ServiceContextMap base = ServiceContextMap.of(CharSequence.class, "HelloWorld");
      ServiceContextMap test1 = ServiceContext.getMap();
      assertNotNull(test1);
      
      ServiceContext.init(base);
      ServiceContextMap test2 = ServiceContext.getMap();
      assertSame(test2, base);
      
      ServiceContext.clear();
      ServiceContextMap test3 = ServiceContext.getMap();
      assertNotNull(test3);
      assertSame(test3, test1);
      
    } finally {
      // try to cleanup in case of problems
      ServiceContext.clear();
    }
  }

  //-------------------------------------------------------------------------
  public void test_addServiceToDefault() {
    assertEquals(ServiceContext.getMap().contains(ServiceManagerTest.class), false);
    ServiceContext.addServiceToDefault(ServiceManagerTest.class, this);
    assertEquals(ServiceContext.getMap().contains(ServiceManagerTest.class), true);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(ServiceContext.class);
  }

}
