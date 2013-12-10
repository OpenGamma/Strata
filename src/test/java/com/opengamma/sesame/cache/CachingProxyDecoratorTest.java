/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.test.TestGroup;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

@SuppressWarnings("unchecked")
@Test(groups = TestGroup.UNIT)
public class CachingProxyDecoratorTest {

  private CacheManager _cacheManager;

  @BeforeClass
  public void setUpClass() {
    _cacheManager = EHCacheUtils.createTestCacheManager(getClass());
  }
  
  /** check the cache contains the item returns from the function */
  @Test
  public void oneLookup() throws Exception {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class),
                                   arguments(function(Impl.class, argument("s", "s"))));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, cachingDecorator);
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    Method foo = ConfigUtils.getMethod(TestFn.class, "foo");
    CachingProxyDecorator.Handler invocationHandler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(fn);
    Impl delegate = (Impl) invocationHandler.getDelegate();
    MethodInvocationKey key = new MethodInvocationKey(delegate, foo, new Object[]{"bar"});

    Object results = fn.foo("bar");
    Ehcache cache = cachingDecorator.getCache();
    Element element = cache.get(key);
    assertNotNull(element);
    FutureTask<Object> task = (FutureTask<Object>) element.getObjectValue();
    assertSame(task.get(), results);
  }

  /** check that multiple instances of the same function return the cached value when invoked with the same args */
  @Test
  public void multipleFunctions() {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class),
                                   arguments(function(Impl.class, argument("s", "s"))));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, cachingDecorator);
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionBuilder functionBuilder = new FunctionBuilder();

    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn1 = (TestFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn2 = (TestFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    assertSame(fn1.foo("bar"), fn2.foo("bar"));
  }

  /**
   * check that multiple identical calls produce the same value even if the underlying function doesn't.
   * this isn't how functions are supposed to work but it demonstrates a point for testing
   */
  @Test
  public void multipleCalls() {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class),
                                   arguments(function(Impl.class, argument("s", "s"))));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, cachingDecorator);
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertSame(fn.foo("bar"), fn.foo("bar"));
  }

  @Test
  public void sameFunctionDifferentConstructorArgs() {
    FunctionConfig config1 = config(implementations(TestFn.class, Impl.class),
                                    arguments(function(Impl.class, argument("s", "a string"))));
    FunctionConfig config2 = config(implementations(TestFn.class, Impl.class),
                                    arguments(function(Impl.class, argument("s", "a different string"))));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, cachingDecorator);
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, cachingDecorator);

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig1);
    TestFn fn1 = (TestFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig2);
    TestFn fn2 = (TestFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.foo("bar");
    Object val2 = fn2.foo("bar");
    assertTrue(val1 != val2);
  }


  interface TestFn {

    @Cache
    @Output("Foo")
    Object foo(String arg);
  }

  public static class Impl implements TestFn {

    private final String _s;

    public Impl(String s) {
      _s = s;
    }

    @Override
    public Object foo(String arg) {
      return _s + new Object();
    }
  }

  /* package */ interface TopLevelFn {

    @Output("topLevel")
    Object fn();
  }

  public static class TopLevel implements TopLevelFn {

    private final DelegateFn _delegateFn;

    public TopLevel(DelegateFn delegateFn) {
      _delegateFn = delegateFn;
    }

    @Override
    @Cache
    public Object fn() {
      return _delegateFn.fn();
    }
  }

  /* package */ interface DelegateFn {

    Object fn();
  }

  public static class Delegate1 implements DelegateFn {

    private final String _s;

    public Delegate1(String s) {
      _s = s;
    }

    @Override
    public Object fn() {
      return _s + new Object();
    }
  }

  public static class Delegate2 implements DelegateFn {

    private final String _s;

    public Delegate2(String s) {
      _s = s;
    }

    @Override
    public Object fn() {
      return _s + new Object();
    }
  }

  /**
   * 2 functions where the top level function is the same and the dependency functions are the same implementation
   * type but have different constructor args.
   */
  @Test
  public void sameFunctionDifferentDependencyInstances() {
    FunctionConfig config1 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                    DelegateFn.class, Delegate1.class),
                                    arguments(function(Delegate1.class, argument("s", "a string"))));
    FunctionConfig config2 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                    DelegateFn.class, Delegate1.class),
                                    arguments(function(Delegate1.class, argument("s", "a different string"))));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TopLevelFn.class, "fn");
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, cachingDecorator);
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, cachingDecorator);

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig1);
    TopLevelFn fn1 = (TopLevelFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig2);
    TopLevelFn fn2 = (TopLevelFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.fn();
    Object val2 = fn2.fn();
    assertTrue(val1 != val2);
  }

  /**
   * 2 functions where the top level function is the same and the dependency functions implement the same interface
   * but are instances of different classes.
   */
  @Test
  public void sameFunctionDifferentDependencyTypes() {
    FunctionConfig config1 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                    DelegateFn.class, Delegate1.class),
                                    arguments(function(Delegate1.class, argument("s", "a string"))));
    FunctionConfig config2 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                    DelegateFn.class, Delegate2.class),
                                    arguments(function(Delegate2.class, argument("s", "a string"))));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TopLevelFn.class, "fn");
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, cachingDecorator);
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, cachingDecorator);

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig1);
    TopLevelFn fn1 = (TopLevelFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig2);
    TopLevelFn fn2 = (TopLevelFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.fn();
    Object val2 = fn2.fn();
    assertTrue(val1 != val2);
  }

  /** check caching works when the class method is annotated and the interface isn't */
  @Test
  public void annotationOnClass() throws Exception {
    FunctionConfig config = config(implementations(TestFn2.class, Impl2.class));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, cachingDecorator);
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn2.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn2 fn = (TestFn2) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    Method foo = ConfigUtils.getMethod(TestFn2.class, "foo");
    CachingProxyDecorator.Handler invocationHandler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(fn);
    Impl2 delegate = (Impl2) invocationHandler.getDelegate();
    MethodInvocationKey key = new MethodInvocationKey(delegate, foo, new Object[]{"bar"});

    Object results = fn.foo("bar");
    Ehcache cache = cachingDecorator.getCache();
    Element element = cache.get(key);
    assertNotNull(element);
    FutureTask<Object> task = (FutureTask<Object>) element.getObjectValue();
    assertSame(task.get(), results);
  }

  interface TestFn2 {

    @Output("Foo")
    Object foo(String arg);
  }

  public static class Impl2 implements TestFn2 {

    @Cache
    @Override
    public Object foo(String arg) {
      return new Object();
    }
  }

  /** Check the expected cache keys are pushed onto a thread local stack while a cacheable method executes. */
  @Test
  public void executingMethods() {
    FunctionConfig config = config(implementations(ExecutingMethodsI1.class, ExecutingMethodsC1.class,
                                                   ExecutingMethodsI2.class, ExecutingMethodsC2.class));
    ExecutingMethodsThreadLocal executingMethods = new ExecutingMethodsThreadLocal();
    ComponentMap components = ComponentMap.of(ImmutableMap.<Class<?>, Object>of(ExecutingMethodsThreadLocal.class,
                                                                                executingMethods));
    GraphConfig graphConfig = new GraphConfig(config, components, new CachingProxyDecorator(_cacheManager, executingMethods));
    ExecutingMethodsI1 i1 = FunctionModel.build(ExecutingMethodsI1.class, graphConfig);
    i1.fn("s", 1);
  }

  interface ExecutingMethodsI1 {

    @Output("abc")
    @Cache
    Object fn(String s, int i);
  }

  public static class ExecutingMethodsC1 implements ExecutingMethodsI1 {

    private final ExecutingMethodsI2 _i2;
    private final ExecutingMethodsThreadLocal _executingMethods;

    public ExecutingMethodsC1(ExecutingMethodsI2 i2, ExecutingMethodsThreadLocal executingMethods) {
      _i2 = i2;
      // this is a bit grubby but necessary so the method keys can be checked
      CachingProxyDecorator.Handler handler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(i2);
      ExecutingMethodsC2 c2 = (ExecutingMethodsC2) handler.getDelegate();
      c2._c1 = this;
      _executingMethods = executingMethods;
    }

    @Override
    public Object fn(String s, int i) {
      Method fn = ConfigUtils.getMethod(ExecutingMethodsI1.class, "fn");
      MethodInvocationKey key = new MethodInvocationKey(this, fn, new Object[]{s, i});
      LinkedList<MethodInvocationKey> expected = Lists.newLinkedList();
      expected.add(key);
      assertEquals(expected, _executingMethods.get());
      Object retVal = _i2.fn(s, i, s + i);
      assertEquals(expected, _executingMethods.get());
      return retVal;
    }
  }

  interface ExecutingMethodsI2 {

    @Cache
    Object fn(String s, int i, String s2);
  }

  public static class ExecutingMethodsC2 implements ExecutingMethodsI2 {

    private final ExecutingMethodsThreadLocal _executingMethods;

    private ExecutingMethodsC1 _c1;

    public ExecutingMethodsC2(ExecutingMethodsThreadLocal executingMethods) {
      _executingMethods = executingMethods;
    }

    @Override
    public Object fn(String s, int i, String s2) {
      Method fn1 = ConfigUtils.getMethod(ExecutingMethodsI1.class, "fn");
      MethodInvocationKey key1 = new MethodInvocationKey(_c1, fn1, new Object[]{s, i});
      Method fn2 = ConfigUtils.getMethod(ExecutingMethodsI2.class, "fn");
      MethodInvocationKey key2 = new MethodInvocationKey(this, fn2, new Object[]{s, i, s2});
      LinkedList<MethodInvocationKey> expected = Lists.newLinkedList();
      expected.add(key2);
      expected.add(key1);
      assertEquals(expected, _executingMethods.get());
      return null;
    }
  }
}

