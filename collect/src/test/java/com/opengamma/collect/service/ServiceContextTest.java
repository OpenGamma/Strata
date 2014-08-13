/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.testng.annotations.Test;

/**
 * Test {@link ServiceContext}.
 */
@Test
public class ServiceContextTest {

  public void test_of_Map() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    map.put(Integer.class, Integer.valueOf(4));
    map.put(CharSequence.class, "HelloWorld");
    ServiceContext test = ServiceContext.of(map);
    assertEquals(test.getServices(), map);
    assertEquals(test.getServiceTypes(), map.keySet());
    assertEquals(test.toString(), "ServiceContext[size=3]");
  }

  public void test_of_Map_null() {
    assertThrows(() -> ServiceContext.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_single() {
    Map<Class<?>, Object> expected = new HashMap<>();
    expected.put(CharSequence.class, "HelloWorld");
    ServiceContext test = ServiceContext.of(CharSequence.class, "HelloWorld");
    assertEquals(test.getServices(), expected);
    assertEquals(test.getServiceTypes(), expected.keySet());
    assertEquals(test.toString(), "ServiceContext[size=1]");
  }

  public void test_of_single_null() {
    assertThrows(() -> ServiceContext.of(CharSequence.class, null), IllegalArgumentException.class);
    assertThrows(() -> ServiceContext.of(null, "HelloWorld"), IllegalArgumentException.class);
    assertThrows(() -> ServiceContext.of(null, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    map.put(Integer.class, Integer.valueOf(4));
    map.put(CharSequence.class, "HelloWorld");
    ServiceContext test = ServiceContext.of(map);
    
    assertEquals(test.contains(Number.class), true);
    assertEquals(test.contains(Integer.class), true);
    assertEquals(test.contains(CharSequence.class), true);
    assertEquals(test.contains(String.class), false);
    assertThrows(() -> test.contains(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    map.put(Integer.class, Integer.valueOf(4));
    map.put(CharSequence.class, "HelloWorld");
    ServiceContext test = ServiceContext.of(map);
    
    assertEquals(test.get(Number.class), Integer.valueOf(2));
    assertEquals(test.get(Integer.class), Integer.valueOf(4));
    assertEquals(test.get(CharSequence.class), "HelloWorld");
    assertThrows(() -> test.get(String.class), IllegalArgumentException.class);
    assertThrows(() -> test.get(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_find() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    map.put(Integer.class, Integer.valueOf(4));
    map.put(CharSequence.class, "HelloWorld");
    ServiceContext test = ServiceContext.of(map);
    
    assertEquals(test.find(Number.class), Integer.valueOf(2));
    assertEquals(test.find(Integer.class), Integer.valueOf(4));
    assertEquals(test.find(CharSequence.class), "HelloWorld");
    assertEquals(test.find(String.class), null);
    assertThrows(() -> test.find(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_with_Map() {
    Map<Class<?>, Object> map1 = new HashMap<>();
    map1.put(Number.class, Integer.valueOf(2));
    Map<Class<?>, Object> map2 = new HashMap<>();
    map2.put(Number.class, Long.valueOf(0));
    map2.put(CharSequence.class, "HelloWorld");
    ServiceContext base = ServiceContext.of(map1);
    ServiceContext test = base.with(map2);
    
    assertEquals(base.get(Number.class), Integer.valueOf(2));
    assertEquals(base.contains(CharSequence.class), false);
    assertEquals(test.get(Number.class), Long.valueOf(0));
    assertEquals(test.get(CharSequence.class), "HelloWorld");
  }

  //-------------------------------------------------------------------------
  public void test_with_single_add() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    ServiceContext base = ServiceContext.of(map);
    ServiceContext test = base.with(CharSequence.class, "HelloWorld");
    
    assertEquals(base.get(Number.class), Integer.valueOf(2));
    assertEquals(base.contains(CharSequence.class), false);
    assertEquals(test.get(Number.class), Integer.valueOf(2));
    assertEquals(test.get(CharSequence.class), "HelloWorld");
  }

  public void test_with_single_replace() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    ServiceContext base = ServiceContext.of(map);
    ServiceContext test = base.with(Number.class, Long.valueOf(0));
    
    assertEquals(base.get(Number.class), Integer.valueOf(2));
    assertEquals(test.get(Number.class), Long.valueOf(0));
  }

  //-------------------------------------------------------------------------
  public void test_withAdded_add() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    ServiceContext base = ServiceContext.of(map);
    ServiceContext test = base.withAdded(CharSequence.class, "HelloWorld");
    
    assertEquals(base.get(Number.class), Integer.valueOf(2));
    assertEquals(base.contains(CharSequence.class), false);
    assertEquals(test.get(Number.class), Integer.valueOf(2));
    assertEquals(test.get(CharSequence.class), "HelloWorld");
  }

  public void test_withAdded_existsDuplicate() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    ServiceContext base = ServiceContext.of(map);
    ServiceContext test = base.withAdded(Number.class, Integer.valueOf(2));
    
    assertEquals(base.get(Number.class), Integer.valueOf(2));
    assertEquals(test.get(Number.class), Integer.valueOf(2));
  }

  public void test_withAdded_existsNotDuplicate() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    ServiceContext test = ServiceContext.of(map);
    assertThrows(() -> test.withAdded(Number.class, Long.valueOf(0)), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_run() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    ServiceContext test = ServiceContext.of(map);
    assertEquals(ServiceManager.getContext().contains(Number.class), false);
    test.run(() -> {
      assertEquals(ServiceManager.getContext().get(Number.class), Integer.valueOf(2));
    });
    assertEquals(ServiceManager.getContext().contains(Number.class), false);
  }

  //-------------------------------------------------------------------------
  public void test_associateWith_Runnable() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    Runnable r = () -> {
      assertEquals(ServiceManager.getContext().get(Number.class), Integer.valueOf(2));
    };
    Runnable test = ServiceContext.of(map).associateWith(r);
    assertEquals(ServiceManager.getContext().contains(Number.class), false);
    test.run();
    assertEquals(ServiceManager.getContext().contains(Number.class), false);
  }

  public void test_associateWith_Runnable_throws() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    Runnable r = () -> {
      assertEquals(ServiceManager.getContext().get(Number.class), Integer.valueOf(2));
      throw new IllegalStateException("Test exception throwing for coverage");
    };
    Runnable test = ServiceContext.of(map).associateWith(r);
    assertThrows(() -> test.run(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void test_associateWith_Callable() throws Exception {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    Callable<Number> c = () -> {
      return ServiceManager.getContext().get(Number.class);
    };
    Callable<Number> test = ServiceContext.of(map).associateWith(c);
    assertEquals(ServiceManager.getContext().contains(Number.class), false);
    assertEquals(test.call(), Integer.valueOf(2));
    assertEquals(ServiceManager.getContext().contains(Number.class), false);
  }

  public void test_associateWith_Callable_throws() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Number.class, Integer.valueOf(2));
    Callable<Number> r = () -> {
      assertEquals(ServiceManager.getContext().get(Number.class), Integer.valueOf(2));
      throw new IllegalStateException("Test exception throwing for coverage");
    };
    Callable<Number> test = ServiceContext.of(map).associateWith(r);
    assertThrows(() -> test.call(), IllegalStateException.class);
  }

}
