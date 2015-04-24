/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

@Test
public class FunctionConfigTest {

  private static final CalculationMarketData MARKET_DATA = mock(CalculationMarketData.class);

  public void createFunctionWithNoArgsConstructor() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionNoParams.class);
    EngineSingleFunction<TestTarget, ?> function = config.createFunction();
    Object result = function.execute(new TestTarget("foo"), MARKET_DATA);
    assertThat(result).isEqualTo("FOO");
  }

  public void createFunctionWithConstructorArgsFromConfig() {
    FunctionConfig<TestTarget> config =
        FunctionConfig.builder(TestFunctionWithParams.class)
            .addArgument("count", 2)
            .addArgument("str", "Foo")
            .build();
    EngineSingleFunction<TestTarget, ?> function = config.createFunction();
    Object result = function.execute(new TestTarget("Bar"), MARKET_DATA);
    assertThat(result).isEqualTo("FooBarFooBar");
  }

  public void createFunctionWithConstructorArgsPassedIn() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionWithParams.class);
    EngineSingleFunction<TestTarget, ?> function = config.createFunction(ImmutableMap.of("count", 2, "str", "Foo"));
    Object result = function.execute(new TestTarget("Bar"), MARKET_DATA);
    assertThat(result).isEqualTo("FooBarFooBar");
  }

  public void createFunctionWithMixedConstructorArgs() {
    FunctionConfig<TestTarget> config =
        FunctionConfig.builder(TestFunctionWithParams.class)
            .addArgument("count", 2)
            .build();
    EngineSingleFunction<TestTarget, ?> function = config.createFunction(ImmutableMap.of("str", "Foo"));
    Object result = function.execute(new TestTarget("Bar"), MARKET_DATA);
    assertThat(result).isEqualTo("FooBarFooBar");
  }

  public void createFunctionMissingArguments() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionWithParams.class);
    String msgRegex = "No argument found with name 'str'";
    assertThrows(() -> config.createFunction(ImmutableMap.of("count", 2)), IllegalArgumentException.class, msgRegex);
  }

  public void createFunctionNoPublicConstructor() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionNoPublicConstructor.class);
    assertThrows(config::createFunction, IllegalArgumentException.class, "Functions must have one public.*");
  }

  public void createFunctionMultiplePublicConstructors() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionMultiplePublicConstructors.class);
    assertThrows(config::createFunction, IllegalArgumentException.class, "Functions must have one public.*");
  }

  public void overwriteFixedArguments() {
    FunctionConfig<TestTarget> config =
        FunctionConfig.builder(TestFunctionWithParams.class)
            .addArgument("count", 2)
            .addArgument("str", "Foo")
            .build();
    String msgRegex = "Built-in function arguments.*";
    assertThrows(() -> config.createFunction(ImmutableMap.of("count", 2)), IllegalArgumentException.class, msgRegex);
  }

  private static final class TestTarget implements CalculationTarget {

    private final String str;

    private TestTarget(String str) {
      this.str = str;
    }
  }

  /** An engine function with no constructor parameters. */
  public static final class TestFunctionNoParams implements EngineSingleFunction<TestTarget, String> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public String execute(TestTarget target, CalculationMarketData marketData) {
      return target.str.toUpperCase();
    }
  }

  /** An engine function with constructor parameters. */
  public static final class TestFunctionWithParams implements EngineSingleFunction<TestTarget, String> {

    private final int count;
    private final String str;

    public TestFunctionWithParams(int count, String str) {
      this.count = count;
      this.str = str;
    }

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public String execute(TestTarget target, CalculationMarketData marketData) {
      return Strings.repeat(str + target.str, count);
    }
  }

  /** An engine function that can't be instantiated because it has no public constructor. */
  public static final class TestFunctionNoPublicConstructor implements EngineSingleFunction<TestTarget, String> {

    TestFunctionNoPublicConstructor() {
    }

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public String execute(TestTarget target, CalculationMarketData marketData) {
      return "";
    }
  }

  /** An engine function that can't be instantiated because it has multiple public constructors. */
  public static final class TestFunctionMultiplePublicConstructors implements EngineSingleFunction<TestTarget, String> {

    public TestFunctionMultiplePublicConstructors() {
    }

    public TestFunctionMultiplePublicConstructors(String s) {
    }

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public String execute(TestTarget target, CalculationMarketData marketData) {
      return "";
    }
  }
}
