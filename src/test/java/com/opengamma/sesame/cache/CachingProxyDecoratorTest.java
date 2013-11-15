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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.test.TestGroup;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

@SuppressWarnings("unchecked")
@Test(groups = TestGroup.UNIT)
public class CachingProxyDecoratorTest {

  /** Ehcache has mutable static state (ugh) so this is necessary */
  private static final AtomicInteger s_nextCacheName = new AtomicInteger();

  /** check the cache contains the item returns from the function */
  @Test
  public void oneLookup() throws Exception {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class),
                                   arguments(function(Impl.class, argument("s", "s"))));
    Ehcache cache = createCache();
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    Method foo = ConfigUtils.getMethod(TestFn.class, "foo");
    CachingProxyDecorator.Handler invocationHandler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(fn);
    Impl delegate = (Impl) invocationHandler.getDelegate();
    MethodInvocationKey key = new MethodInvocationKey(Impl.class, foo, new Object[]{"bar"}, delegate);

    Object results = fn.foo("bar");
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
    Ehcache cache = createCache();
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
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
    Ehcache cache = createCache();
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
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
    Ehcache cache = createCache();
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, new CachingProxyDecorator(cache));

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

  /* package */ interface TopLevelFunction {

    @Output("topLevel")
    Object fn();
  }

  public static class TopLevel implements TopLevelFunction {

    private final DelegateFunction _delegateFunction;

    public TopLevel(DelegateFunction delegateFunction) {
      _delegateFunction = delegateFunction;
    }

    @Override
    @Cache
    public Object fn() {
      return _delegateFunction.fn();
    }
  }

  /* package */ interface DelegateFunction {

    Object fn();
  }

  public static class Delegate1 implements DelegateFunction {

    private final String _s;

    public Delegate1(String s) {
      _s = s;
    }

    @Override
    public Object fn() {
      return _s + new Object();
    }
  }

  public static class Delegate2 implements DelegateFunction {

    @Override
    public Object fn() {
      return new Object();
    }
  }

  /**
   * 2 functions where the top level function is the same and the dependency functions are the same implementation
   * type but have different constructor args.
   */
  @Test
  public void sameFunctionDifferentDependencyInstances() {
    FunctionConfig config1 = config(implementations(TopLevelFunction.class, TopLevel.class,
                                                    DelegateFunction.class, Delegate1.class),
                                    arguments(function(Delegate1.class, argument("s", "a string"))));
    FunctionConfig config2 = config(implementations(TopLevelFunction.class, TopLevel.class,
                                                    DelegateFunction.class, Delegate1.class),
                                    arguments(function(Delegate1.class, argument("s", "a different string"))));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TopLevelFunction.class, "fn");
    Ehcache cache = createCache();
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, new CachingProxyDecorator(cache));

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig1);
    TopLevelFunction fn1 = (TopLevelFunction) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig2);
    TopLevelFunction fn2 = (TopLevelFunction) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

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
    FunctionConfig config1 = config(implementations(TopLevelFunction.class, TopLevel.class,
                                                    DelegateFunction.class, Delegate1.class),
                                    arguments(function(Delegate1.class, argument("s", "a string"))));
    FunctionConfig config2 = config(implementations(TopLevelFunction.class, TopLevel.class,
                                                    DelegateFunction.class, Delegate2.class),
                                    arguments(function(Delegate2.class, argument("s", "a string"))));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TopLevelFunction.class, "fn");
    Ehcache cache = createCache();
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, new CachingProxyDecorator(cache));

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig1);
    TopLevelFunction fn1 = (TopLevelFunction) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig2);
    TopLevelFunction fn2 = (TopLevelFunction) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.fn();
    Object val2 = fn2.fn();
    assertTrue(val1 != val2);
  }

  /** check caching works when the class method is annotated and the interface isn't */
  @Test
  public void annotationOnClass() throws Exception {
    FunctionConfig config = config(implementations(TestFn2.class, Impl2.class));
    Ehcache cache = createCache();
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(cache));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn2.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn2 fn = (TestFn2) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    Method foo = ConfigUtils.getMethod(TestFn2.class, "foo");
    CachingProxyDecorator.Handler invocationHandler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(fn);
    Impl2 delegate = (Impl2) invocationHandler.getDelegate();
    MethodInvocationKey key = new MethodInvocationKey(Impl2.class, foo, new Object[]{"bar"}, delegate);

    Object results = fn.foo("bar");
    Element element = cache.get(key);
    assertNotNull(element);
    FutureTask<Object> task = (FutureTask<Object>) element.getObjectValue();
    assertSame(task.get(), results);
  }

  private static Ehcache createCache() {
    CacheConfiguration config = new CacheConfiguration(Integer.toString(s_nextCacheName.getAndIncrement()), 1000)
        .eternal(true)
        .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    Ehcache cache = new net.sf.ehcache.Cache(config);
    CacheManager.getInstance().addCache(cache);
    return cache;
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
}

