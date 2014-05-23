/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.EmptyFunctionArguments;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.SimpleFunctionArguments;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class MethodInvokableFunctionTest {

  @Test
  public void oneMissingArg() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionMetadata metadata = EngineUtils.createMetadata(Fn.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, config);
    InvokableFunction invokableFunction = model.build(new FunctionBuilder(), ComponentMap.EMPTY);
    FunctionArguments args = new SimpleFunctionArguments(ImmutableMap.<String, Object>of("bar", "barVal"));

    Result<?> result = (Result<?>) invokableFunction.invoke(mock(Environment.class), null, args);
    assertFalse(result.isSuccess());
    assertEquals(FailureStatus.MISSING_ARGUMENT, result.getStatus());
    String expectedMessage = "No argument provided for non-nullable parameter for method Fn.foo(), " +
        "parameter 'baz', type java.lang.Object";
    assertEquals(expectedMessage, result.getFailureMessage());
  }

  @Test
  public void multipleMissingArgs() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionMetadata metadata = EngineUtils.createMetadata(Fn.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, config);
    InvokableFunction invokableFunction = model.build(new FunctionBuilder(), ComponentMap.EMPTY);

    Result<?> result = (Result<?>) invokableFunction.invoke(mock(Environment.class), null, EmptyFunctionArguments.INSTANCE);
    assertFalse(result.isSuccess());
    assertEquals(FailureStatus.MISSING_ARGUMENT, result.getStatus());
    String expectedMessage = "No arguments provided for non-nullable parameters of method Fn.foo(), " +
        "parameters [Object bar, Object baz]";
    assertEquals(expectedMessage, result.getFailureMessage());
  }

  public interface Fn {

    @Output("Foo")
    Object foo(Object bar, Object baz);
  }

  public static class Impl implements Fn {

    @Override
    public Object foo(Object bar, Object baz) {
      return null;
    }
  }
}
