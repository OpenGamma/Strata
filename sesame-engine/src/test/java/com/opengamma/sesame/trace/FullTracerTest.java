/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class FullTracerTest {

  private static final String METHOD1 = "method1";
  private static final String METHOD2 = "method2";

  private I1 buildFunction() {
    FunctionModelConfig functionModelConfig = config(implementations(I1.class, C1.class, I2.class, C2.class));
    return FunctionModel.build(I1.class, functionModelConfig, ComponentMap.EMPTY, TracingProxy.INSTANCE);
  }

  @Test
  public void trace1Level() throws NoSuchMethodException {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(0);
    CallGraph trace = TracingProxy.end();

    CallGraph expected = CallGraph.builder()
        .receiverClass(I1.class)
        .methodName(METHOD1)
        .parameterTypes(ImmutableList.<Class<?>>of(Integer.TYPE))
        .arguments(ImmutableList.<Object>of(0))
        .returnValue("foo")
        .build();

    assertEquals(expected, trace);
    System.out.println(trace.prettyPrint());
  }

  @Test
  public void trace2Levels() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(1);
    CallGraph trace = TracingProxy.end();

    CallGraph expected = CallGraph.builder()
        .receiverClass(I1.class)
        .methodName(METHOD1)
        .parameterTypes(ImmutableList.<Class<?>>of(Integer.TYPE))
        .arguments(ImmutableList.<Object>of(1))
        .returnValue("foo 42")
        .calls(ImmutableList.of(
            CallGraph.builder()
                .receiverClass(I2.class)
                .methodName(METHOD2)
                .parameterTypes(ImmutableList.<Class<?>>of(Boolean.TYPE))
                .arguments(ImmutableList.<Object>of(true))
                .returnValue(42)
                .build()))
        .build();

    assertEquals(expected, trace);
    System.out.println(trace.prettyPrint());
  }

  @Test
  public void traceMultiple() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(2);
    CallGraph trace = TracingProxy.end();

    CallGraph expected = CallGraph.builder()
        .receiverClass(I1.class)
        .methodName(METHOD1)
        .parameterTypes(ImmutableList.<Class<?>>of(Integer.TYPE))
        .arguments(ImmutableList.<Object>of(2))
        .returnValue("bar 42 84")
        .calls(ImmutableList.of(
            CallGraph.builder()
                .receiverClass(I2.class)
                .methodName(METHOD2)
                .parameterTypes(ImmutableList.<Class<?>>of(Boolean.TYPE))
                .arguments(ImmutableList.<Object>of(true))
                .returnValue(42)
                .build(),
            CallGraph.builder()
                .receiverClass(I2.class)
                .methodName(METHOD2)
                .parameterTypes(ImmutableList.<Class<?>>of(Boolean.TYPE))
                .arguments(ImmutableList.<Object>of(true))
                .returnValue(42)
                .build()))
        .build();

    assertEquals(expected, trace);
    System.out.println(trace.prettyPrint());
  }

  @Test
  public void traceException() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    try {
      i1.method1(3);
      fail();
    } catch (OpenGammaRuntimeException e) {
      // expected
    }
    CallGraph trace = TracingProxy.end();

    CallGraph expected = CallGraph.builder()
        .receiverClass(I1.class)
        .methodName(METHOD1)
        .parameterTypes(ImmutableList.<Class<?>>of(Integer.TYPE))
        .arguments(ImmutableList.<Object>of(3))
        .throwableClass(OpenGammaRuntimeException.class)
        .errorMessage("an exception")
        .calls(ImmutableList.of(
            CallGraph.builder()
                .receiverClass(I2.class)
                .methodName(METHOD2)
                .parameterTypes(ImmutableList.<Class<?>>of(Boolean.TYPE))
                .arguments(ImmutableList.<Object>of(false))
                .throwableClass(OpenGammaRuntimeException.class)
                .errorMessage("an exception")
                .build()))
        .build();

    assertEquals(expected, trace);
    System.out.println(trace.prettyPrint());
  }

  @Test
  public void tracingDisabled() {
    I1 i1 = buildFunction();
    i1.method1(2);
    assertNull(TracingProxy.end());
  }

  interface I1 {

    @Output("not used")
    String method1(int i);
  }

  public static class C1 implements I1 {

    private final I2 _i2;

    public C1(I2 i2) {
      _i2 = i2;
    }

    @Override
    public String method1(int i) {
      switch (i) {
        case 0:
          return "foo";
        case 1:
          return "foo " + _i2.method2(true);
        case 2:
          return "bar " + _i2.method2(true) + " " + (_i2.method2(true) * 2);
        default:
          return "baz " + _i2.method2(false);
      }
    }
  }

  interface I2 {

    Integer method2(boolean b);
  }

  public static class C2 implements I2 {

    @Override
    public Integer method2(boolean b) {
      if (b) {
        return 42;
      } else {
        throw new OpenGammaRuntimeException("an exception");
      }
    }
  }
}
