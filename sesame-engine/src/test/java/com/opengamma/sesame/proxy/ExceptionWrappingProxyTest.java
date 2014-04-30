/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Proxy;

import org.testng.annotations.Test;

import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.result.Result;

/**
 * Tests to check that the ExceptionWrappingProxy behaves as expected.
 */
public class ExceptionWrappingProxyTest {

  @Test
  public void testNoProxyCreatedWhenNoMethodsReturnResult() {
    FunctionModelConfig config = config(implementations(MockSingleFn.class, HappyMockSingleFn.class));
    FunctionMetadata metadata = EngineUtils.createMetadata(MockSingleFn.class, "doSomethingWithoutResult");
    FunctionModel functionModel =
        FunctionModel.forFunction(metadata, config, ComponentMap.EMPTY.getComponentTypes(),
                                  ExceptionWrappingProxy.INSTANCE);
    Object fn = functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertThat(fn instanceof MockSingleFn, is(true));
    assertThat(fn instanceof Proxy, is(false));
  }

  @Test
  public void nonExceptionMethodReturnsNormally() {
    MockFn fn = createHappyNonResultReturner();
    assertThat(fn.doSomethingElse(), is(true));
  }

  @Test
  public void nonExceptionResultMethodReturnsNormally() {
    MockFn fn = createHappyResultReturner();
    Result<Boolean> result = fn.doSomething();
    assertThat(result.isSuccess(), is(true));
    assertThat(result.getValue(), is(true));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void exceptionMethodThrowsException() {
    MockFn fn = createUnappyNonResultReturner();
    assertThat(fn.doSomethingElse(), is(true));
  }

  @Test
  public void exceptionResultMethodReturnsNormally() {
    MockFn fn = createUnhappyResultReturner();
    Result<Boolean> result = fn.doSomething();
    assertThat(result.isSuccess(), is(false));
    assertThat(result.getFailureMessage(), containsString("so unhappy"));
  }

  private MockFn createHappyResultReturner() {
    return createMockFn("doSomething", HappyMockFn.class);
  }

  private MockFn createHappyNonResultReturner() {
    return createMockFn("doSomethingElse", HappyMockFn.class);
  }

  private MockFn createUnhappyResultReturner() {
    return createMockFn("doSomething", UnhappyMockFn.class);
  }

  private MockFn createUnappyNonResultReturner() {
    return createMockFn("doSomethingElse", UnhappyMockFn.class);
  }

  private MockFn createMockFn(String methodName, Class<? extends MockFn> implementationClass) {
    FunctionModelConfig config = config(implementations(MockFn.class, implementationClass));
    FunctionMetadata metadata = EngineUtils.createMetadata(MockFn.class, methodName);
    FunctionModel functionModel =
        FunctionModel.forFunction(metadata, config, ComponentMap.EMPTY.getComponentTypes(),
                                  ExceptionWrappingProxy.INSTANCE);
    return (MockFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
  }

  private interface MockFn {
    @Output(value = "this")
    Result<Boolean> doSomething();
    @Output(value = "that")
    boolean doSomethingElse();
  }

  public  static class HappyMockFn implements MockFn {

    public Result<Boolean> doSomething() {
      return Result.success(true);
    }

    @Override
    public boolean doSomethingElse() {
      return true;
    }
  }

  public static class UnhappyMockFn implements MockFn {

    public Result<Boolean> doSomething() {
      throw new RuntimeException("so unhappy");
    }

    @Override
    public boolean doSomethingElse() {
      throw new RuntimeException("so sad");
    }
  }


  private interface MockSingleFn {
    @Output(value = "that")
    boolean doSomethingWithoutResult();
  }

  public static class HappyMockSingleFn implements MockSingleFn {
    @Override
    public boolean doSomethingWithoutResult() {
      return true;
    }
  }
}