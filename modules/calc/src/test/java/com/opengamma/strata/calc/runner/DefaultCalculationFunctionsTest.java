/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.calc.TestingMeasures.BUCKETED_PV01;
import static com.opengamma.strata.calc.TestingMeasures.CASH_FLOWS;
import static com.opengamma.strata.calc.TestingMeasures.PAR_RATE;
import static com.opengamma.strata.calc.TestingMeasures.PRESENT_VALUE_MULTI_CCY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.result.Result;

@Test
public class DefaultCalculationFunctionsTest {

  public void oneDerivedFunction() {
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5));
    DelegateFn delegateFn = new DelegateFn(delegateResults);
    DerivedFn derivedFn = new DerivedFn();
    CalculationFunctions calculationFunctions = CalculationFunctions.of(delegateFn);
    CalculationFunctions derivedFunctions = calculationFunctions.composedWith(derivedFn);
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
    CalculationFunctions derivedFunctions = calculationFunctions.composedWith(derivedFn1, derivedFn2);
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
    CalculationFunctions derivedFunctions = calculationFunctions.composedWith(derivedFn1, derivedFn2);
    TestTarget target = new TestTarget(42);
    CalculationFunction<? super TestTarget> function = derivedFunctions.getFunction(target);

    ImmutableSet<Measure> expectedMeasures = ImmutableSet.of(BUCKETED_PV01, CASH_FLOWS, PAR_RATE, PRESENT_VALUE_MULTI_CCY);
    assertThat(function.supportedMeasures()).isEqualTo(expectedMeasures);
  }
}
