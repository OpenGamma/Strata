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
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.DefaultImplementationProvider;
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
    FunctionModelConfig decoratedConfig = DecoratorConfig.decorate(config, Decorator1.class);

    assertEquals(Decorator1.class, decoratedConfig.getFunctionImplementation(Fn.class));
    assertEquals(Impl.class, decoratedConfig.getFunctionImplementation(DECORATOR1_PARAM));
  }

  @Test
  public void decorators() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionModelConfig decoratedConfig = DecoratorConfig.decorate(config, Decorator1.class, Decorator2.class);

    assertEquals(Decorator1.class, decoratedConfig.getFunctionImplementation(Fn.class));
    assertEquals(Decorator2.class, decoratedConfig.getFunctionImplementation(DECORATOR1_PARAM));
    assertEquals(Impl.class, decoratedConfig.getFunctionImplementation(DECORATOR2_PARAM));
  }

  @Test
  public void ordering() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionModelConfig decoratedConfig = DecoratorConfig.decorate(config, Decorator2.class, Decorator1.class);

    assertEquals(Decorator2.class, decoratedConfig.getFunctionImplementation(Fn.class));
    assertEquals(Decorator1.class, decoratedConfig.getFunctionImplementation(DECORATOR2_PARAM));
    assertEquals(Impl.class, decoratedConfig.getFunctionImplementation(DECORATOR1_PARAM));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notADecorator() {
    DecoratorConfig.decorate(config(), Impl.class);
  }

  @Test
  public void chainedDecoratorConfig() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionModelConfig decoratedConfig = DecoratorConfig.decorate(config, Decorator1.class, Decorator2.class);

    assertEquals(Decorator1.class, decoratedConfig.getFunctionImplementation(Fn.class));
    assertEquals(Decorator2.class, decoratedConfig.getFunctionImplementation(DECORATOR1_PARAM));
    assertEquals(Impl.class, decoratedConfig.getFunctionImplementation(DECORATOR2_PARAM));
  }

  @Test
  public void undecoratedConfig() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class, Fn2.class, Impl2.class));
    FunctionModelConfig decoratedConfig = DecoratorConfig.decorate(config, Decorator1.class);

    assertEquals(Impl2.class, decoratedConfig.getFunctionImplementation(Fn2.class));
  }

  @Test
  public void defaultImplementationProvider() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(Impl.class);
    FunctionModelConfig defaultImpls = new DefaultImplementationProvider(availableImplementations);
    FunctionModelConfig decoratedConfig = DecoratorConfig.decorate(defaultImpls, Decorator1.class);

    assertEquals(Decorator1.class, decoratedConfig.getFunctionImplementation(Fn.class));
    assertEquals(Impl.class, decoratedConfig.getFunctionImplementation(DECORATOR1_PARAM));
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

    public Decorator1(Fn delegate) { }

    @Override
    public String foo(Double d) {
      return null;
    }
  }

  public static class Decorator2 implements Fn {

    public Decorator2(Fn fn) { }

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
