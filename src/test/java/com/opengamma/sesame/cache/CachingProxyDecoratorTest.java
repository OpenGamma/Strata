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

import org.testng.annotations.Test;

import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.test.TestGroup;

import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

@Test(groups = TestGroup.UNIT)
public class CachingProxyDecoratorTest {

  private static final SelfPopulatingCache CACHE = CachingProxyDecorator.createCache();

  /** check the cache contains the item returns from the function */
  @Test
  public void oneLookup() {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class),
                                   arguments(function(Impl.class, argument("s", "s"))));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(CACHE));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(ComponentMap.EMPTY).getReceiver();
    Method foo = ConfigUtils.getMethod(TestFn.class, "foo");
    MethodInvocationKey key = new MethodInvocationKey(Impl.class, foo, new Object[]{"bar"}, new Impl("s"));

    Object results = fn.foo("bar");
    Element element = CACHE.get(key);
    assertNotNull(element);
    Object cacheValue = element.getObjectValue();
    assertSame(cacheValue, results);
  }

  /** check that multiple instances of the same function return the cached value when invoked with the same args */
  @Test
  public void multipleFunctions() {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class),
                                   arguments(function(Impl.class, argument("s", "s"))));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(CACHE));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");

    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn1 = (TestFn) functionModel1.build(ComponentMap.EMPTY).getReceiver();

    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn2 = (TestFn) functionModel2.build(ComponentMap.EMPTY).getReceiver();
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
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(CACHE));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(ComponentMap.EMPTY).getReceiver();
    assertSame(fn.foo("bar"), fn.foo("bar"));
  }

  // TODO this is disabled because it's a test for a bug that hasn't been fixed yet
  @Test(enabled = false)
  public void sameFunctionDifferentConstructorArgs() {
    FunctionConfig config1 = config(implementations(TestFn.class, Impl.class),
                                    arguments(function(Impl.class, argument("s", "s"))));
    FunctionConfig config2 = config(implementations(TestFn.class, Impl.class),
                                    arguments(function(Impl.class, argument("s", "t"))));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    GraphConfig graphConfig1 = new GraphConfig(config1, ComponentMap.EMPTY, new CachingProxyDecorator(CACHE));
    GraphConfig graphConfig2 = new GraphConfig(config2, ComponentMap.EMPTY, new CachingProxyDecorator(CACHE));

    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, graphConfig1);
    TestFn fn1 = (TestFn) functionModel1.build(ComponentMap.EMPTY).getReceiver();

    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, graphConfig2);
    TestFn fn2 = (TestFn) functionModel2.build(ComponentMap.EMPTY).getReceiver();
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

  /** check caching works when the class method is annotated and the interface isn't */
  @Test
  public void annotationOnClass() {
    FunctionConfig config = config(implementations(TestFn2.class, Impl2.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator(CACHE));
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn2.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn2 fn = (TestFn2) functionModel.build(ComponentMap.EMPTY).getReceiver();
    Method foo = ConfigUtils.getMethod(TestFn2.class, "foo");
    MethodInvocationKey key = new MethodInvocationKey(Impl2.class, foo, new Object[]{"bar"}, new Impl2());

    Object results = fn.foo("bar");
    Element element = CACHE.get(key);
    assertNotNull(element);
    Object cacheValue = element.getObjectValue();
    assertSame(cacheValue, results);
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

