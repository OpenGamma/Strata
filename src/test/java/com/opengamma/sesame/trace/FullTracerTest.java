/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.fail;

import org.testng.annotations.Test;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.test.TestGroup;

/**
 * TODO some assertions
 */
@Test(groups = TestGroup.UNIT)
public class FullTracerTest {

  private I1 buildFunction() {
    FunctionConfig functionConfig = config(implementations(I1.class, C1.class, I2.class, C2.class));
    GraphConfig graphConfig = new GraphConfig(functionConfig, ComponentMap.EMPTY, TracingProxy.INSTANCE);
    return FunctionModel.build(I1.class, "method1", graphConfig);
  }

  @Test
  public void trace1Level() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(0);
    Call call = TracingProxy.end();
    System.out.println(call.prettyPrint());
  }

  @Test
  public void trace2Levels() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(1);
    Call call2 = TracingProxy.end();
    System.out.println(call2.prettyPrint());
  }

  @Test
  public void traceMultiple() {
    I1 i1 = buildFunction();
    TracingProxy.start(new FullTracer());
    i1.method1(2);
    Call call2 = TracingProxy.end();
    System.out.println(call2.prettyPrint());
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
    Call call = TracingProxy.end();
    System.out.println(call.prettyPrint());
  }

  @Test
  public void tracingDisabled() {


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
