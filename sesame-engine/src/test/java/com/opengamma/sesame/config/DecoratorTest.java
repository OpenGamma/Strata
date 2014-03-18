/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class DecoratorTest {

  private static final Map<Class<?>, Annotation> ANNOTATIONS = Collections.emptyMap();
  private static final Parameter DECORATOR1_PARAM = new Parameter(Decorator1.class, "delegate", Fn.class, 0, ANNOTATIONS);
  private static final Parameter DECORATOR2_PARAM = new Parameter(Decorator2.class, "fn", Fn.class, 0, ANNOTATIONS);

  @Test
  public void decorator() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator1.class);
    DecoratorConfig decoratorConfig = new DecoratorConfig(config, decorators);

    assertEquals(Decorator1.class, decoratorConfig.getFunctionImplementation(Fn.class));
    assertEquals(Impl.class, decoratorConfig.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
  }

  @Test
  public void decorators() {
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator1.class);
    decorators.add(Decorator2.class);
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    DecoratorConfig decoratorConfig = new DecoratorConfig(config, decorators);

    assertEquals(Decorator1.class, decoratorConfig.getFunctionImplementation(Fn.class));
    assertEquals(Decorator2.class, decoratorConfig.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
    assertEquals(Impl.class, decoratorConfig.getFunctionImplementation(Fn.class, DECORATOR2_PARAM));
  }

  @Test
  public void ordering() {
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator2.class);
    decorators.add(Decorator1.class);
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    DecoratorConfig decoratorConfig = new DecoratorConfig(config, decorators);

    assertEquals(Decorator2.class, decoratorConfig.getFunctionImplementation(Fn.class));
    assertEquals(Decorator1.class, decoratorConfig.getFunctionImplementation(Fn.class, DECORATOR2_PARAM));
    assertEquals(Impl.class, decoratorConfig.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notADecorator() {
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Impl.class);
    new DecoratorConfig(config(implementations(Fn.class, Impl.class)), decorators);
  }

  @Test
  public void chainedDecoratorConfig() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    DecoratorConfig decoratorConfig2 = new DecoratorConfig(config, Decorator2.class);
    DecoratorConfig decoratorConfig1 = new DecoratorConfig(decoratorConfig2, Decorator1.class);

    assertEquals(Decorator1.class, decoratorConfig1.getFunctionImplementation(Fn.class));
    assertEquals(Decorator2.class, decoratorConfig1.getFunctionImplementation(Fn.class, DECORATOR1_PARAM));
    assertEquals(Impl.class, decoratorConfig1.getFunctionImplementation(Fn.class, DECORATOR2_PARAM));
  }

  @Test
  public void undecoratedConfig() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class, Fn2.class, Impl2.class));
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator1.class);
    DecoratorConfig decoratorConfig = new DecoratorConfig(config, decorators);

    assertEquals(Impl2.class, decoratorConfig.getFunctionImplementation(Fn2.class));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void noUnderlyingFunction() {
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(Decorator1.class);
    new DecoratorConfig(FunctionModelConfig.EMPTY, decorators);
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

  public interface Fn2 {

    @Output("Bar")
    String bar(Double d);
  }

  public static class Impl2 implements Fn2 {

    @Override
    public String bar(Double d) {
      return null;
    }
  }
}
