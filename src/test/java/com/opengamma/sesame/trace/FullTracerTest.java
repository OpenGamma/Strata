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

import java.lang.reflect.Method;

import org.testng.annotations.Test;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class FullTracerTest {

  private static final Method METHOD1;
  private static final Method METHOD2;

  static {
    try {
      METHOD1 = I1.class.getMethod("method1", Integer.TYPE);
      METHOD2 = I2.class.getMethod("method2", Boolean.TYPE);
    } catch (NoSuchMethodException e) {
      throw new OpenGammaRuntimeException("", e);
    }
  }

  private I1 buildFunction() {
    FunctionConfig functionConfig = config(implementations(I1.class, C1.class, I2.class, C2.class));
    GraphConfig graphConfig = new GraphConfig(functionConfig, ComponentMap.EMPTY, TracingProxy.INSTANCE);
    return FunctionModel.build(I1.class, graphConfig);
  }

  @Test
  public void trace1Level() throws NoSuchMethodException {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(0);
    CallGraph callGraph = TracingProxy.end();
    CallGraph expected = new CallGraph(METHOD1, 0);
    expected.returned("foo");
    assertEquals(expected, callGraph);
    System.out.println(callGraph.prettyPrint());
  }

  @Test
  public void trace2Levels() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(1);
    CallGraph callGraph1 = TracingProxy.end();
    CallGraph expected = new CallGraph(METHOD1, 1);
    expected.returned("foo 42");
    CallGraph callGraph2 = new CallGraph(METHOD2, true);
    callGraph2.returned(42);
    expected.called(callGraph2);
    assertEquals(expected, callGraph1);
    System.out.println(callGraph1.prettyPrint());
  }

  @Test
  public void traceMultiple() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(2);
    CallGraph expected = new CallGraph(METHOD1, 2);
    expected.returned("bar 42 84");
    CallGraph callGraph2 = new CallGraph(METHOD2, true);
    callGraph2.returned(42);
    CallGraph callGraph3 = new CallGraph(METHOD2, true);
    callGraph3.returned(42);
    expected.called(callGraph2);
    expected.called(callGraph3);
    CallGraph callGraph = TracingProxy.end();
    assertEquals(expected, callGraph);
    System.out.println(callGraph.prettyPrint());
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
    CallGraph expected = new CallGraph(METHOD1, 3);
    expected.threw(new OpenGammaRuntimeException("an exception"));
    CallGraph callGraph2 = new CallGraph(METHOD2, true);
    callGraph2.threw(new OpenGammaRuntimeException("an exception"));
    expected.called(callGraph2);
    CallGraph callGraph = TracingProxy.end();
    System.out.println(callGraph.prettyPrint());
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
