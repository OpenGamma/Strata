/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;

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

@Test(groups = TestGroup.UNIT)
public class CachingProxyDecoratorTest {

  /** check the cache contains the item returns from the function */
  @Test
  public void oneLookup() {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator());
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(ComponentMap.EMPTY).getReceiver();
    CacheKey key = new CacheKey(Impl.class, ConfigUtils.getMethod(TestFn.class, "foo"), new Object[]{"bar"});

    Object results = fn.foo("bar");
    Element element = CachingProxyDecorator.getCache().get(key);
    assertNotNull(element);
    Object cacheValue = element.getObjectValue();
    assertSame(cacheValue, results);
  }

  /** check that multiple instances of the same function return the cached value when invoked with the same args */
  @Test
  public void multipleFunctions() {
    FunctionConfig config = config(implementations(TestFn.class, Impl.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator());
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
    FunctionConfig config = config(implementations(TestFn.class, Impl.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, new CachingProxyDecorator());
    FunctionMetadata metadata = ConfigUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, graphConfig);
    TestFn fn = (TestFn) functionModel.build(ComponentMap.EMPTY).getReceiver();
    assertSame(fn.foo("bar"), fn.foo("bar"));
  }

  interface TestFn {

    @Cache
    @Output("Foo")
    Object foo(String arg);
  }

  public static class Impl implements TestFn {

    @Override
    public Object foo(String arg) {
      return new Object();
    }
  }
}

