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
import static com.opengamma.strata.calc.TestingMeasures.PRESENT_VALUE;
import static com.opengamma.strata.calc.TestingMeasures.PRESENT_VALUE_MULTI_CCY;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.marketdata.TestObservableId;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

public class DerivedCalculationFunctionTest {

  /**
   * Tests all measures are calculated by the derived function and the underlying function.
   */
  @Test
  public void calculateMeasure() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5),
        PRESENT_VALUE, Result.success(7));
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        new DelegateFn(delegateResults));

    Set<Measure> measures = ImmutableSet.of(BUCKETED_PV01, CASH_FLOWS, PAR_RATE, PRESENT_VALUE);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    assertThat(wrapper.supportedMeasures()).isEqualTo(measures);
    assertThat(wrapper.targetType()).isEqualTo(TestTarget.class);
    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(BUCKETED_PV01)).hasValue(35);
    assertThat(results.get(CASH_FLOWS)).hasValue(3);
    assertThat(results.get(PAR_RATE)).hasValue(5);
    assertThat(results.get(PRESENT_VALUE)).hasValue(7);
  }

  /**
   * Test two derived function composed together
   */
  @Test
  public void calculateMeasuresNestedDerivedClasses() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5),
        PRESENT_VALUE, Result.success(7));
    DerivedFn derivedFn1 = new DerivedFn(BUCKETED_PV01);
    DerivedFn derivedFn2 = new DerivedFn(PRESENT_VALUE_MULTI_CCY);
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        derivedFn1,
        new DelegateFn(delegateResults));
    wrapper = new DerivedCalculationFunctionWrapper<>(derivedFn2, wrapper);

    Set<Measure> measures = ImmutableSet.of(BUCKETED_PV01, PRESENT_VALUE_MULTI_CCY, CASH_FLOWS, PAR_RATE, PRESENT_VALUE);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    assertThat(wrapper.supportedMeasures()).isEqualTo(measures);
    assertThat(wrapper.targetType()).isEqualTo(TestTarget.class);
    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(BUCKETED_PV01)).hasValue(35);
    assertThat(results.get(PRESENT_VALUE_MULTI_CCY)).hasValue(35);
    assertThat(results.get(CASH_FLOWS)).hasValue(3);
    assertThat(results.get(PAR_RATE)).hasValue(5);
    assertThat(results.get(PRESENT_VALUE)).hasValue(7);
  }

  /**
   * Test that the derived measure isn't calculated unless it is requested.
   */
  @Test
  public void derivedMeasureNotRequested() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PRESENT_VALUE, Result.success(7));
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        new DelegateFn(delegateResults));

    Set<Measure> measures = ImmutableSet.of(CASH_FLOWS, PRESENT_VALUE);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(CASH_FLOWS)).hasValue(3);
    assertThat(results.get(PRESENT_VALUE)).hasValue(7);
  }

  /**
   * Test that measures aren't returned if they are needed to calculate the derived measure but aren't
   * requested by the user.
   */
  @Test
  public void requiredMeasureNotReturned() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5),
        PRESENT_VALUE, Result.success(7));
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        new DelegateFn(delegateResults));

    Set<Measure> measures = ImmutableSet.of(BUCKETED_PV01);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(BUCKETED_PV01)).hasValue(35);
  }

  /**
   * Test the behaviour when the underlying function doesn't support the measures required by the derived function.
   */
  @Test
  public void requiredMeasuresNotSupported() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        PAR_RATE, Result.success(5),
        PRESENT_VALUE, Result.success(7));
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        new DelegateFn(delegateResults));

    Set<Measure> measures = ImmutableSet.of(BUCKETED_PV01, PAR_RATE);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    // The derived measure isn't supported because its required measure isn't available
    assertThat(wrapper.supportedMeasures()).isEqualTo(ImmutableSet.of(PAR_RATE, PRESENT_VALUE));
    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(BUCKETED_PV01)).hasFailureMessageMatching(".*cannot calculate the required measures.*");
    assertThat(results.get(PAR_RATE)).hasValue(5);
  }

  /**
   * Test the derived measure result is a failure if any of the required measures are failures
   */
  @Test
  public void requiredMeasureFails() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.failure(FailureReason.ERROR, "Failed to calculate bar"),
        PAR_RATE, Result.success(5),
        PRESENT_VALUE, Result.success(7));
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        new DelegateFn(delegateResults));

    Set<Measure> measures = ImmutableSet.of(BUCKETED_PV01);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(BUCKETED_PV01)).hasFailureMessageMatching("Failed to calculate bar");
  }

  /**
   * Test the behaviour when the delegate function returns no value for a measure it claims to support.
   * This is a bug in the function, it should always return a result for all measures that are supported and
   * were requested.
   */
  @Test
  public void supportedMeasureNotReturned() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of(
        CASH_FLOWS, Result.success(3),
        PAR_RATE, Result.success(5),
        PRESENT_VALUE, Result.success(7));

    DelegateFn delegateFn = new DelegateFn(delegateResults) {
      @Override
      public Map<Measure, Result<?>> calculate(
          TestTarget target,
          Set<Measure> measures,
          CalculationParameters parameters,
          ScenarioMarketData marketData,
          ReferenceData refData) {

        // Don't return TestingMeasures.CASH_FLOWS even though it should be supported
        return ImmutableMap.of(PAR_RATE, Result.success(5));
      }
    };
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        delegateFn);

    Set<Measure> measures = ImmutableSet.of(BUCKETED_PV01);
    Map<Measure, Result<?>> results = wrapper.calculate(
        target,
        measures,
        CalculationParameters.empty(),
        ScenarioMarketData.empty(),
        ReferenceData.standard());

    assertThat(results.keySet()).isEqualTo(measures);
    assertThat(results.get(BUCKETED_PV01)).hasFailureMessageMatching(".*did not return the expected measures.*");
  }

  @Test
  public void requirements() {
    TestTarget target = new TestTarget(10);
    Map<Measure, Result<?>> delegateResults = ImmutableMap.of();
    DerivedCalculationFunctionWrapper<TestTarget, Integer> wrapper = new DerivedCalculationFunctionWrapper<>(
        new DerivedFn(),
        new DelegateFn(delegateResults));

    FunctionRequirements requirements = wrapper.requirements(
        target,
        ImmutableSet.of(),
        CalculationParameters.empty(),
        ReferenceData.empty());

    FunctionRequirements expected = FunctionRequirements.builder()
        .valueRequirements(TestObservableId.of("a"), TestObservableId.of("b"), TestObservableId.of("d"))
        .timeSeriesRequirements(TestObservableId.of("c"), TestObservableId.of("e"))
        .outputCurrencies(Currency.GBP, Currency.EUR, Currency.USD)
        .build();

    assertThat(requirements).isEqualTo(expected);
    assertThat(wrapper.naturalCurrency(target, ReferenceData.empty())).isEqualTo(Currency.AUD);
  }
}

//--------------------------------------------------------------------------------------------------

final class TestTarget implements CalculationTarget {  // CSIGNORE

  private final int value;

  TestTarget(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}

final class DerivedFn extends AbstractDerivedCalculationFunction<TestTarget, Integer> {  // CSIGNORE

  DerivedFn(Measure measure, Set<Measure> requiredMeasures) {
    super(TestTarget.class, measure, requiredMeasures);
  }

  DerivedFn(Measure measure) {
    this(measure, ImmutableSet.of(CASH_FLOWS, PAR_RATE));
  }

  DerivedFn() {
    this(BUCKETED_PV01);
  }

  @Override
  public Integer calculate(
      TestTarget target,
      Map<Measure, Object> requiredMeasures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    Integer bar = (Integer) requiredMeasures.get(CASH_FLOWS);
    Integer baz = (Integer) requiredMeasures.get(PAR_RATE);

    return target.getValue() * bar + baz;
  }

  @Override
  public FunctionRequirements requirements(TestTarget target, CalculationParameters parameters, ReferenceData refData) {
    return FunctionRequirements.builder()
        .valueRequirements(TestObservableId.of("a"), TestObservableId.of("b"))
        .timeSeriesRequirements(TestObservableId.of("c"))
        .outputCurrencies(Currency.GBP)
        .build();
  }
}

//--------------------------------------------------------------------------------------------------
class DelegateFn implements CalculationFunction<TestTarget> {  // CSIGNORE

  private final Map<Measure, Result<?>> results;

  DelegateFn(Map<Measure, Result<?>> results) {
    this.results = results;
  }

  @Test
  @Override
  public Class<TestTarget> targetType() {
    return TestTarget.class;
  }

  @Test
  @Override
  public Set<Measure> supportedMeasures() {
    return results.keySet();
  }

  @Override
  public Currency naturalCurrency(TestTarget target, ReferenceData refData) {
    return AUD;
  }

  @Override
  public FunctionRequirements requirements(
      TestTarget target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    return FunctionRequirements.builder()
        .valueRequirements(TestObservableId.of("d"))
        .timeSeriesRequirements(TestObservableId.of("e"))
        .outputCurrencies(Currency.EUR, Currency.USD)
        .build();
  }

  @Override
  public Map<Measure, Result<?>> calculate(
      TestTarget target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    Set<Measure> missingMeasures = Sets.difference(measures, results.keySet());
    Map<Measure, Result<?>> results = MapStream.of(this.results).filterKeys(measures::contains).toMap();
    Map<Measure, Result<?>> missingResults = missingMeasures.stream().collect(toMap(m -> m, this::missingResult));
    Map<Measure, Result<?>> allResults = new HashMap<>(results);
    allResults.putAll(missingResults);
    return allResults;
  }

  private Result<?> missingResult(Measure measure) {
    return Result.failure(FailureReason.CALCULATION_FAILED, "{} not supported", measure);
  }
}
