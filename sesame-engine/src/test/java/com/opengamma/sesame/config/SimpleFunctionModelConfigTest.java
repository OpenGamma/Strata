/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class SimpleFunctionModelConfigTest {

  private static final Map<Class<?>, Annotation> ANNOTATIONS = Collections.emptyMap();
  private static final Parameter DECORATOR1_PARAM = new Parameter(Decorator1.class, "delegate", Fn.class, 0, ANNOTATIONS);
  private static final Parameter DECORATOR2_PARAM = new Parameter(Decorator2.class, "fn", Fn.class, 0, ANNOTATIONS);
  private static final Map<Class<?>, Class<?>> IMPLS = ImmutableMap.<Class<?>, Class<?>>of(Fn.class, Impl.class);
  private static final Map<Class<?>, FunctionArguments> ARGS = Collections.emptyMap();

  @Test
  public void decorator() {
    Set<Class<?>> decorators = ImmutableSet.<Class<?>>of(Decorator1.class);
    SimpleFunctionModelConfig config = new SimpleFunctionModelConfig(IMPLS, ARGS, decorators);
    Parameter dummyParam = new Parameter(Object.class, "notUsed", Object.class, 0, ANNOTATIONS);

    assertEquals(Decorator1.class, config.getFunctionImplementation(Fn.class, dummyParam));
    assertEquals(Impl.class, config.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
  }

  @Test
  public void decorators() {
    Set<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator1.class);
    decorators.add(Decorator2.class);
    SimpleFunctionModelConfig config = new SimpleFunctionModelConfig(IMPLS, ARGS, decorators);
    Parameter dummyParam = new Parameter(Object.class, "notUsed", Object.class, 0, ANNOTATIONS);

    assertEquals(Decorator1.class, config.getFunctionImplementation(Fn.class, dummyParam));
    assertEquals(Decorator2.class, config.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
    assertEquals(Impl.class, config.getFunctionImplementation(Fn.class, DECORATOR2_PARAM));
  }

  @Test
  public void ordering() {
    Set<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator2.class);
    decorators.add(Decorator1.class);
    SimpleFunctionModelConfig config = new SimpleFunctionModelConfig(IMPLS, ARGS, decorators);
    Parameter dummyParam = new Parameter(Object.class, "notUsed", Object.class, 0, ANNOTATIONS);

    assertEquals(Decorator2.class, config.getFunctionImplementation(Fn.class, dummyParam));
    assertEquals(Decorator1.class, config.getFunctionImplementation(Fn.class, DECORATOR2_PARAM));
    assertEquals(Impl.class, config.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notADecorator() {
    Set<Class<?>> decorators = ImmutableSet.<Class<?>>of(Impl.class);
    new SimpleFunctionModelConfig(IMPLS, ARGS, decorators);
  }

  public interface Fn {

    @Output("Foo")
    String foo(Double d);
  }

  public static class Impl implements Fn {

    @Override
    public String foo(Double d) {
      return null;
    }
  }

  public static class Decorator1 implements Fn {

    private final Fn _delegate;

    public Decorator1(Fn delegate) {
      _delegate = delegate;
    }

    @Override
    public String foo(Double d) {
      return null;
    }
  }

  public static class Decorator2 implements Fn {

    private final Fn _delegate;

    public Decorator2(Fn fn) {
      _delegate = fn;
    }

    @Override
    public String foo(Double d) {
      return null;
    }
  }
}
