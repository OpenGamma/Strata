/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.calc.TestingMeasures.BUCKETED_PV01;
import static com.opengamma.strata.calc.TestingMeasures.CASH_FLOWS;
import static com.opengamma.strata.calc.TestingMeasures.PAR_RATE;
import static com.opengamma.strata.calc.TestingMeasures.PRESENT_VALUE_MULTI_CCY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

@Test
public class DerivedCalculationFunctionsTest {

  public void oneDerivedFunction() {
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5));
    DelegateFn delegateFn = new DelegateFn(delegateResults);
    DerivedFn derivedFn = new DerivedFn();
    CalculationFunctions calculationFunctions = CalculationFunctions.of(delegateFn);
    DerivedCalculationFunctions derivedFunctions =
        new DerivedCalculationFunctions(calculationFunctions, ImmutableList.of(derivedFn));
    TestTarget target = new TestTarget(42);
    CalculationFunction<? super TestTarget> function = derivedFunctions.getFunction(target);

    ImmutableSet<Measure> expectedMeasures = ImmutableSet.of(BUCKETED_PV01, CASH_FLOWS, PAR_RATE);
    assertThat(function.supportedMeasures()).isEqualTo(expectedMeasures);
  }

  /**
   * Test that multiple derived functions for the same target type are correctly combined.
   */
  public void multipleDerivedFunctionsForSameTargetType() {
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5));
    DelegateFn delegateFn = new DelegateFn(delegateResults);
    DerivedFn derivedFn1 = new DerivedFn();
    DerivedFn derivedFn2 = new DerivedFn(PRESENT_VALUE_MULTI_CCY);

    CalculationFunctions calculationFunctions = CalculationFunctions.of(delegateFn);
    DerivedCalculationFunctions derivedFunctions =
        new DerivedCalculationFunctions(calculationFunctions, ImmutableList.of(derivedFn1, derivedFn2));
    TestTarget target = new TestTarget(42);
    CalculationFunction<? super TestTarget> function = derivedFunctions.getFunction(target);

    ImmutableSet<Measure> expectedMeasures = ImmutableSet.of(BUCKETED_PV01, CASH_FLOWS, PAR_RATE, PRESENT_VALUE_MULTI_CCY);
    assertThat(function.supportedMeasures()).isEqualTo(expectedMeasures);
  }

  /**
   * Test that multiple derived functions for the same target type are correctly combined when one derived function
   * depends on another.
   */
  public void multipleDerivedFunctionsForSameTargetTypeWithDependencyBetweenDerivedFunctions() {
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5));
    DelegateFn delegateFn = new DelegateFn(delegateResults);
    DerivedFn derivedFn1 = new DerivedFn();
    // This depends on the measure calculated by derivedFn1
    DerivedFn derivedFn2 = new DerivedFn(PRESENT_VALUE_MULTI_CCY, ImmutableSet.of(BUCKETED_PV01));

    CalculationFunctions calculationFunctions = CalculationFunctions.of(delegateFn);
    // The derived functions must be specified in the correct order.
    // The function higher up the dependency chain must come second
    DerivedCalculationFunctions derivedFunctions =
        new DerivedCalculationFunctions(calculationFunctions, ImmutableList.of(derivedFn1, derivedFn2));
    TestTarget target = new TestTarget(42);
    CalculationFunction<? super TestTarget> function = derivedFunctions.getFunction(target);

    ImmutableSet<Measure> expectedMeasures = ImmutableSet.of(BUCKETED_PV01, CASH_FLOWS, PAR_RATE, PRESENT_VALUE_MULTI_CCY);
    assertThat(function.supportedMeasures()).isEqualTo(expectedMeasures);
  }

  public void oneDerivedFunctionWithAnotherTargetType() {
    Map<Measure, Result<?>> delegate1Results = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5));
    DelegateFn delegate1Fn = new DelegateFn(delegate1Results);
    Delegate2Fn delegate2Fn = new Delegate2Fn();
    CalculationFunctions calculationFunctions = CalculationFunctions.of(delegate1Fn, delegate2Fn);
    DerivedFn derivedFn = new DerivedFn();
    DerivedCalculationFunctions derivedFunctions =
        new DerivedCalculationFunctions(calculationFunctions, ImmutableList.of(derivedFn));
    TestTarget target1 = new TestTarget(42);
    CalculationFunction<? super TestTarget> function1 = derivedFunctions.getFunction(target1);
    TestTarget2 target2 = new TestTarget2(84);
    CalculationFunction<? super TestTarget> function2 = derivedFunctions.getFunction(target2);

    assertThat(function1.supportedMeasures()).containsOnly(BUCKETED_PV01, CASH_FLOWS, PAR_RATE);
    assertThat(function2.supportedMeasures()).containsOnly(PAR_RATE);
  }

  //-------------------------------------------------------------------------
  final class TestTarget2 implements CalculationTarget {

    private final int value;

    TestTarget2(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  //-------------------------------------------------------------------------
  class Delegate2Fn implements CalculationFunction<TestTarget2> {

    @Override
    public Class<TestTarget2> targetType() {
      return TestTarget2.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return ImmutableSet.of(PAR_RATE);
    }

    @Override
    public Currency naturalCurrency(TestTarget2 target, ReferenceData refData) {
      return AUD;
    }

    @Override
    public FunctionRequirements requirements(
        TestTarget2 target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget2 target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      return ImmutableMap.of(PAR_RATE, Result.success(target.value));
    }
  }

}
