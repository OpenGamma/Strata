/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

@Test
public class FunctionConfigTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Measure MEASURE = Measures.PRESENT_VALUE;
  private static final Set<Measure> MEASURES = ImmutableSet.of(MEASURE);
  private static final CalculationMarketData MARKET_DATA = mock(CalculationMarketData.class);

  public void createFunctionWithNoArgsConstructor() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionNoParams.class);
    @SuppressWarnings("unchecked")
    CalculationFunction<TestTarget> function = config.createFunction();
    Map<Measure, Result<?>> result = function.calculate(new TestTarget("foo"), MEASURES, MARKET_DATA, REF_DATA);
    assertThat(result.get(MEASURE).isSuccess()).isTrue();
    assertThat(result.get(MEASURE).getValue()).isEqualTo(ScenarioResult.of("FOO"));
  }

  public void createFunctionWithConstructorArgsFromConfig() {
    FunctionConfig<TestTarget> config =
        FunctionConfig.builder(TestFunctionWithParams.class)
            .addArgument("count", 2)
            .addArgument("str", "Foo")
            .build();
    CalculationFunction<TestTarget> function = config.createFunction();
    Map<Measure, Result<?>> result = function.calculate(new TestTarget("Bar"), MEASURES, MARKET_DATA, REF_DATA);
    assertThat(result.get(MEASURE).isSuccess()).isTrue();
    assertThat(result.get(MEASURE).getValue()).isEqualTo(ScenarioResult.of("FooBarFooBar"));
  }

  public void createFunctionWithConstructorArgsPassedIn() {
    FunctionConfig<TestTarget> config = FunctionConfig.of(TestFunctionWithParams.class);
    CalculationFunction<TestTarget> function = config.createFunction(ImmutableMap.of("count", 2, "str", "Foo"));
    Map<Measure, Result<?>> result = function.calculate(new TestTarget("Bar"), MEASURES, MARKET_DATA, REF_DATA);
    assertThat(result.get(MEASURE).isSuccess()).isTrue();
    assertThat(result.get(MEASURE).getValue()).isEqualTo(ScenarioResult.of("FooBarFooBar"));
  }

  public void createFunctionWithMixedConstructorArgs() {
    FunctionConfig<TestTarget> config =
        FunctionConfig.builder(TestFunctionWithParams.class)
            .addArgument("count", 2)
            .build();
    CalculationFunction<TestTarget> function = config.createFunction(ImmutableMap.of("str", "Foo"));
    Map<Measure, Result<?>> result = function.calculate(new TestTarget("Bar"), MEASURES, MARKET_DATA, REF_DATA);
    assertThat(result.get(MEASURE).isSuccess()).isTrue();
    assertThat(result.get(MEASURE).getValue()).isEqualTo(ScenarioResult.of("FooBarFooBar"));
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

  //-------------------------------------------------------------------------
  private static final class TestTarget implements CalculationTarget {
    private final String str;

    private TestTarget(String str) {
      this.str = str;
    }
  }

  //-------------------------------------------------------------------------
  /** An engine function with no constructor parameters. */
  public static final class TestFunctionNoParams implements CalculationFunction<TestTarget> {

    @Override
    public Class<TestTarget> targetType() {
      return TestTarget.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of(target.str.toUpperCase(Locale.ENGLISH));
      return ImmutableMap.of(MEASURE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /** An engine function with constructor parameters. */
  public static final class TestFunctionWithParams implements CalculationFunction<TestTarget> {

    private final int count;
    private final String str;

    public TestFunctionWithParams(int count, String str) {
      this.count = count;
      this.str = str;
    }

    @Override
    public Class<TestTarget> targetType() {
      return TestTarget.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of(Strings.repeat(str + target.str, count));
      return ImmutableMap.of(MEASURE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /** An engine function that can't be instantiated because it has no public constructor. */
  public static final class TestFunctionNoPublicConstructor implements CalculationFunction<TestTarget> {

    TestFunctionNoPublicConstructor() {
    }

    @Override
    public Class<TestTarget> targetType() {
      return TestTarget.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of("");
      return ImmutableMap.of(MEASURE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /** An engine function that can't be instantiated because it has multiple public constructors. */
  public static final class TestFunctionMultiplePublicConstructors implements CalculationFunction<TestTarget> {

    public TestFunctionMultiplePublicConstructors() {
    }

    public TestFunctionMultiplePublicConstructors(String s) {
    }

    @Override
    public Class<TestTarget> targetType() {
      return TestTarget.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of("");
      return ImmutableMap.of(MEASURE, Result.success(array));
    }
  }

}
