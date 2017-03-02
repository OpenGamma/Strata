/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.calc.TestingMeasures;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.TestId;
import com.opengamma.strata.calc.marketdata.TestObservableId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Test {@link CalculationTask}.
 */
@Test
public class CalculationTaskTest {

  static final ObservableSource OBS_SOURCE = ObservableSource.of("MarketDataVendor");

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ReportingCurrency NATURAL = ReportingCurrency.NATURAL;
  private static final ReportingCurrency REPORTING_CURRENCY_USD = ReportingCurrency.of(Currency.USD);
  private static final TestTarget TARGET = new TestTarget();
  private static final Set<Measure> MEASURES =
      ImmutableSet.of(TestingMeasures.PRESENT_VALUE, TestingMeasures.PRESENT_VALUE_MULTI_CCY);

  public void requirements() {
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, new TestFunction(), cell);
    MarketDataRequirements requirements = task.requirements(REF_DATA);
    Set<? extends MarketDataId<?>> nonObservables = requirements.getNonObservables();
    ImmutableSet<? extends ObservableId> observables = requirements.getObservables();
    ImmutableSet<ObservableId> timeSeries = requirements.getTimeSeries();

    MarketDataId<?> timeSeriesId = TestObservableId.of("3", OBS_SOURCE);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(timeSeriesId);

    MarketDataId<?> nonObservableId = new TestId("1");
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(nonObservableId);

    MarketDataId<?> observableId = TestObservableId.of("2", OBS_SOURCE);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(observableId);
  }

  /**
   * Test that the result is converted to the reporting currency if it implements ScenarioFxConvertible and
   * the FX rates are available in the market data.
   */
  public void convertResultCurrencyUsingReportingCurrency() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(GBP, USD, rate))
        .collect(toImmutableList());
    CurrencyScenarioArray list = CurrencyScenarioArray.of(GBP, values);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8))
        .addScenarioValue(FxRateId.of(GBP, USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyScenarioArray expectedArray = CurrencyScenarioArray.of(USD, expectedValues);

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(expectedArray);
  }

  /**
   * Test that the result is not converted if the isCurrencyConvertible flag on the measure is false.
   */
  public void currencyConversionHonoursConvertibleFlagOnMeasure() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(GBP, USD, rate))
        .collect(toImmutableList());
    CurrencyScenarioArray list = CurrencyScenarioArray.of(GBP, values);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8))
        .addScenarioValue(FxRateId.of(GBP, USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE_MULTI_CCY, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    CurrencyScenarioArray expectedArray = CurrencyScenarioArray.of(GBP, values);

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(expectedArray);
  }

  /**
   * Test that the result is converted to the reporting currency if it implements ScenarioFxConvertible and
   * the FX rates are available in the market data. The "natural" currency is taken from the function.
   */
  public void convertResultCurrencyUsingDefaultReportingCurrency() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(GBP, USD, rate))
        .collect(toImmutableList());
    CurrencyScenarioArray list = CurrencyScenarioArray.of(GBP, values);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8))
        .addScenarioValue(FxRateId.of(GBP, USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, USD);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyScenarioArray expectedArray = CurrencyScenarioArray.of(USD, expectedValues);

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(expectedArray);
  }

  /**
   * Test that the result is returned unchanged if it is a failure.
   */
  public void convertResultCurrencyFailure() {
    ConvertibleFunction fn = ConvertibleFunction.of(() -> {
      throw new RuntimeException("This is a failure");
    }, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.CALCULATION_FAILED)
        .hasFailureMessageMatching("Error when invoking function 'ConvertibleFunction' for ID '123': This is a failure");
  }

  /**
   * Test the result is returned unchanged if using ReportingCurrency.NONE.
   */
  public void convertResultCurrencyNoConversionRequested() {
    SupplierFunction<CurrencyAmount> fn = SupplierFunction.of(() -> CurrencyAmount.of(EUR, 1d));
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, ReportingCurrency.NONE);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioArray.of(CurrencyAmount.of(EUR, 1d)));
  }

  /**
   * Test the result is returned unchanged if it is not ScenarioFxConvertible.
   */
  public void convertResultCurrencyNotConvertible() {
    TestFunction fn = new TestFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioArray.of("bar"));
  }

  /**
   * Test a non-convertible result is returned even if there is no reporting currency.
   */
  public void nonConvertibleResultReturnedWhenNoReportingCurrency() {
    TestFunction fn = new TestFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioArray.of("bar"));
  }

  /**
   * Test that a failure is returned if currency conversion fails.
   */
  public void convertResultCurrencyConversionFails() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyScenarioArray list = CurrencyScenarioArray.of(GBP, values);
    // Market data doesn't include FX rates, conversion to USD will fail
    ScenarioMarketData marketData = ScenarioMarketData.empty();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.CURRENCY_CONVERSION)
        .hasFailureMessageMatching("Failed to convert value '.*' to currency 'USD'");
  }

  /**
   * Tests that executing a function wraps its return value in a success result.
   */
  public void execute() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> "foo");
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioArray.of("foo"));
  }

  /**
   * Test executing a bad function that fails to return expected measure.
   */
  public void executeMissingMeasure() {
    // function claims it supports 'PresentValueMultiCurrency' but fails to return it when asked
    MeasureCheckFunction fn = new MeasureCheckFunction(ImmutableSet.of(TestingMeasures.PRESENT_VALUE), Optional.of("123"));
    CalculationTaskCell cell0 = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTaskCell cell1 = CalculationTaskCell.of(0, 1, TestingMeasures.PRESENT_VALUE_MULTI_CCY, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell0, cell1);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result0 = calculationResults.getCells().get(0).getResult();
    assertThat(result0)
        .isSuccess()
        .hasValue(ImmutableSet.of(TestingMeasures.PRESENT_VALUE, TestingMeasures.PRESENT_VALUE_MULTI_CCY));
    Result<?> result1 = calculationResults.getCells().get(1).getResult();
    assertThat(result1)
        .isFailure(FailureReason.CALCULATION_FAILED)
        .hasFailureMessageMatching(
            "Function 'MeasureCheckFunction' did not return requested measure 'PresentValueMultiCurrency' for ID '123'");
  }

  /**
   * Tests that executing a function filters the set of measures sent to function.
   */
  public void executeFilterMeasures() {
    // function does not support 'ParRate', so it should not be asked for it
    MeasureCheckFunction fn = new MeasureCheckFunction(ImmutableSet.of(TestingMeasures.PRESENT_VALUE), Optional.of("123"));
    CalculationTaskCell cell0 = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTaskCell cell1 = CalculationTaskCell.of(0, 1, TestingMeasures.PAR_RATE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell0, cell1);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result0 = calculationResults.getCells().get(0).getResult();
    assertThat(result0)
        .isSuccess()
        .hasValue(ImmutableSet.of(TestingMeasures.PRESENT_VALUE));  // ParRate not requested
    Result<?> result1 = calculationResults.getCells().get(1).getResult();
    assertThat(result1)
        .isFailure(FailureReason.UNSUPPORTED)
        .hasFailureMessageMatching("Measure 'ParRate' is not supported by function 'MeasureCheckFunction'");
  }

  /**
   * Tests that executing a function that throws an exception wraps the exception in a failure result.
   */
  public void executeException() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new IllegalArgumentException("foo");
    });
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.CALCULATION_FAILED)
        .hasFailureMessageMatching("Error when invoking function 'SupplierFunction' for ID '123': foo");
  }

  /**
   * Tests that executing a function that throws a market data exception wraps the exception in a failure result.
   */
  public void executeException_marketData() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new MarketDataNotFoundException("foo");
    });
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.MISSING_DATA)
        .hasFailureMessageMatching("Missing market data when invoking function 'SupplierFunction' for ID '123': foo");
  }

  /**
   * Tests that executing a function that throws a market data exception wraps the exception in a failure result.
   * Target has no identifier.
   */
  public void executeException_marketData_noIdentifier() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new MarketDataNotFoundException("foo");
    }, Optional.empty());
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.MISSING_DATA)
        .hasFailureMessageMatching("Missing market data when invoking function 'SupplierFunction': foo: for target '.*'");
  }

  /**
   * Tests that executing a function that throws a reference data exception wraps the exception in a failure result.
   */
  public void executeException_referenceData() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new ReferenceDataNotFoundException("foo");
    });
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.MISSING_DATA)
        .hasFailureMessageMatching("Missing reference data when invoking function 'SupplierFunction' for ID '123': foo");
  }

  /**
   * Tests that executing a function that throws an unsupported exception wraps the exception in a failure result.
   */
  public void executeException_unsupported() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new UnsupportedOperationException("foo");
    });
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result)
        .isFailure(FailureReason.UNSUPPORTED)
        .hasFailureMessageMatching("Unsupported operation when invoking function 'SupplierFunction' for ID '123': foo");
  }

  /**
   * Tests that executing a function that returns a success result returns the underlying result without wrapping it.
   */
  public void executeSuccessResultValue() {
    SupplierFunction<Result<ScenarioArray<String>>> fn =
        SupplierFunction.of(() -> Result.success(ScenarioArray.of("foo")));
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioArray.of("foo"));
  }

  /**
   * Tests that executing a function that returns a failure result returns the underlying result without wrapping it.
   */
  public void executeFailureResultValue() {
    SupplierFunction<Result<String>> fn =
        SupplierFunction.of(() -> Result.failure(FailureReason.NOT_APPLICABLE, "bar"));
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).isFailure(FailureReason.NOT_APPLICABLE).hasFailureMessageMatching("bar");
  }

  /**
   * Tests that requirements are added for the FX rates needed to convert the results into the reporting currency.
   */
  public void fxConversionRequirements() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    MarketDataRequirements requirements = task.requirements(REF_DATA);

    assertThat(requirements.getNonObservables()).containsOnly(
        FxRateId.of(GBP, USD, OBS_SOURCE),
        FxRateId.of(EUR, USD, OBS_SOURCE));
  }

  /**
   * Tests that no requirements are added when not performing currency conversion.
   */
  public void fxConversionRequirements_noConversion() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, ReportingCurrency.NONE);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    MarketDataRequirements requirements = task.requirements(REF_DATA);

    assertThat(requirements.getNonObservables()).isEmpty();
  }

  public void testToString() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(1, 2, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    assertThat(task.toString())
        .isEqualTo("CalculationTask[CalculationTaskCell[(1, 2), measure=PresentValue, currency=Specific:USD]]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(1, 2, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask test = CalculationTask.of(TARGET, fn, cell);
    coverImmutableBean(test);

    OutputCurrenciesFunction fn2 = new OutputCurrenciesFunction();
    CalculationTaskCell cell2 = CalculationTaskCell.of(1, 3, TestingMeasures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask test2 = CalculationTask.of(new TestTarget(), fn2, cell2);
    coverBeanEquals(test, test2);
    assertNotNull(CalculationTask.meta());
  }

  //-------------------------------------------------------------------------
  static class TestTarget implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value that is not currency convertible.
   */
  public static final class TestFunction implements CalculationFunction<TestTarget> {

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
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.builder()
          .valueRequirements(
              ImmutableSet.of(
                  TestId.of("1"),
                  TestObservableId.of("2")))
          .timeSeriesRequirements(TestObservableId.of("3"))
          .observableSource(OBS_SOURCE)
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      ScenarioArray<String> array = ScenarioArray.of("bar");
      return ImmutableMap.of(TestingMeasures.PRESENT_VALUE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value that is currency convertible.
   */
  private static final class ConvertibleFunction
      implements CalculationFunction<TestTarget> {

    private final Supplier<CurrencyScenarioArray> supplier;
    private final Currency naturalCurrency;

    static ConvertibleFunction of(Supplier<CurrencyScenarioArray> supplier, Currency naturalCurrency) {
      return new ConvertibleFunction(supplier, naturalCurrency);
    }

    private ConvertibleFunction(Supplier<CurrencyScenarioArray> supplier, Currency naturalCurrency) {
      this.supplier = supplier;
      this.naturalCurrency = naturalCurrency;
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
    public Optional<String> identifier(TestTarget target) {
      return Optional.of("123");
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return naturalCurrency;
    }

    @Override
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      Result<CurrencyScenarioArray> result = Result.success(supplier.get());
      return ImmutableMap.of(TestingMeasures.PRESENT_VALUE, result, TestingMeasures.PRESENT_VALUE_MULTI_CCY, result);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value from a Supplier.
   */
  private static final class SupplierFunction<T> implements CalculationFunction<TestTarget> {

    private final Supplier<T> supplier;
    private final Optional<String> id;

    public static <T> SupplierFunction<T> of(Supplier<T> supplier) {
      return of(supplier, Optional.of("123"));
    }

    public static <T> SupplierFunction<T> of(Supplier<T> supplier, Optional<String> id) {
      return new SupplierFunction<>(supplier, id);
    }

    private SupplierFunction(Supplier<T> supplier, Optional<String> id) {
      this.supplier = supplier;
      this.id = id;
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
    public Optional<String> identifier(TestTarget target) {
      return id;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      T obj = supplier.get();
      if (obj instanceof Result<?>) {
        return ImmutableMap.of(TestingMeasures.PRESENT_VALUE, (Result<?>) obj);
      }
      ScenarioArray<Object> array = ScenarioArray.of(obj);
      return ImmutableMap.of(TestingMeasures.PRESENT_VALUE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value from a Supplier.
   */
  private static final class MeasureCheckFunction implements CalculationFunction<TestTarget> {

    private final Set<Measure> resultMeasures;
    private final Optional<String> id;

    private MeasureCheckFunction(Set<Measure> resultMeasures, Optional<String> id) {
      this.resultMeasures = resultMeasures;
      this.id = id;
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
    public Optional<String> identifier(TestTarget target) {
      return id;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      Map<Measure, Result<?>> map = new HashMap<>();
      for (Measure measure : resultMeasures) {
        map.put(measure, Result.success(measures));
      }
      return map;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns requirements containing output currencies.
   */
  private static final class OutputCurrenciesFunction implements CalculationFunction<TestTarget> {

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
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.builder()
          .outputCurrencies(GBP, EUR, USD)
          .observableSource(OBS_SOURCE)
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      throw new UnsupportedOperationException("calculate not implemented");
    }
  }

}
