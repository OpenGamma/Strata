/**
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Measures;
import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.TestId;
import com.opengamma.strata.calc.marketdata.TestObservableId;
import com.opengamma.strata.calc.result.CurrencyValuesArray;
import com.opengamma.strata.calc.result.ScenarioResult;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
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
      ImmutableSet.of(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY);

  public void requirements() {
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, NATURAL);
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
    CurrencyValuesArray list = CurrencyValuesArray.of(GBP, values);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8))
        .addScenarioValue(FxRateId.of(GBP, USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyValuesArray expectedArray = CurrencyValuesArray.of(USD, expectedValues);

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
    CurrencyValuesArray list = CurrencyValuesArray.of(GBP, values);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8))
        .addScenarioValue(FxRateId.of(GBP, USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE_MULTI_CCY, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    CurrencyValuesArray expectedArray = CurrencyValuesArray.of(GBP, values);

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
    CurrencyValuesArray list = CurrencyValuesArray.of(GBP, values);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8))
        .addScenarioValue(FxRateId.of(GBP, USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, USD);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyValuesArray expectedArray = CurrencyValuesArray.of(USD, expectedValues);

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
    } , GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasFailureMessageMatching("Function '.*' threw an exception: This is a failure");
  }

  /**
   * Test the result is returned unchanged if it is not ScenarioFxConvertible.
   */
  public void convertResultCurrencyNotConvertible() {
    TestFunction fn = new TestFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioResult.of("bar"));
  }

  /**
   * Test a non-convertible result is returned even if there is no reporting currency.
   */
  public void nonConvertibleResultReturnedWhenNoReportingCurrency() {
    TestFunction fn = new TestFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioResult.of("bar"));
  }

  /**
   * Test that a failure is returned if currency conversion fails.
   */
  public void convertResultCurrencyConversionFails() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyValuesArray list = CurrencyValuesArray.of(GBP, values);
    // Market data doesn't include FX rates, conversion to USD will fail
    ScenarioMarketData marketData = ScenarioMarketData.empty();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list, GBP);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasFailureMessageMatching("Failed to convert value '.*' to currency 'USD'");
  }

  /**
   * Tests that executing a function wraps its return value in a success result.
   */
  public void execute() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> "foo");
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioResult.of("foo"));
  }

  /**
   * Tests that executing a function that throws an exception wraps the exception in a failure result.
   */
  public void executeException() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> {
      throw new IllegalArgumentException("foo");
    });
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ScenarioMarketData.empty();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).isFailure(FailureReason.ERROR)
        .hasFailureMessageMatching("Function 'SupplierFunction' threw an exception: foo");
  }

  /**
   * Tests that executing a function that returns a success result returns the underlying result without wrapping it.
   */
  public void executeSuccessResultValue() {
    SupplierFunction<Result<ScenarioResult<String>>> fn =
        SupplierFunction.of(() -> Result.success(ScenarioResult.of("foo")));
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(date(2011, 3, 8)).build();

    CalculationResults calculationResults = task.execute(marketData, REF_DATA);
    Result<?> result = calculationResults.getCells().get(0).getResult();
    assertThat(result).hasValue(ScenarioResult.of("foo"));
  }

  /**
   * Tests that executing a function that returns a failure result returns the underlying result without wrapping it.
   */
  public void executeFailureResultValue() {
    SupplierFunction<Result<String>> fn =
        SupplierFunction.of(() -> Result.failure(FailureReason.NOT_APPLICABLE, "bar"));
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
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
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    MarketDataRequirements requirements = task.requirements(REF_DATA);

    assertThat(requirements.getNonObservables()).containsOnly(
        FxRateId.of(GBP, USD, OBS_SOURCE),
        FxRateId.of(EUR, USD, OBS_SOURCE));
  }

  public void testToString() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(1, 2, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    assertThat(task.toString())
        .isEqualTo("CalculationTask[CalculationTaskCell[(1, 2), measure=PresentValue, currency=Specific:USD]]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(1, 2, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
    CalculationTask test = CalculationTask.of(TARGET, fn, cell);
    coverImmutableBean(test);

    OutputCurrenciesFunction fn2 = new OutputCurrenciesFunction();
    CalculationTaskCell cell2 = CalculationTaskCell.of(1, 3, Measures.PRESENT_VALUE, REPORTING_CURRENCY_USD);
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
          .singleValueRequirements(
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

      ScenarioResult<String> array = ScenarioResult.of("bar");
      return ImmutableMap.of(Measures.PRESENT_VALUE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value that is currency convertible.
   */
  private static final class ConvertibleFunction
      implements CalculationFunction<TestTarget> {

    private final Supplier<CurrencyValuesArray> supplier;
    private final Currency naturalCurrency;

    static ConvertibleFunction of(Supplier<CurrencyValuesArray> supplier, Currency naturalCurrency) {
      return new ConvertibleFunction(supplier, naturalCurrency);
    }

    private ConvertibleFunction(Supplier<CurrencyValuesArray> supplier, Currency naturalCurrency) {
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

      Result<CurrencyValuesArray> result = Result.success(supplier.get());
      return ImmutableMap.of(Measures.PRESENT_VALUE, result, Measures.PRESENT_VALUE_MULTI_CCY, result);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function that returns a value from a Supplier.
   */
  private static final class SupplierFunction<T> implements CalculationFunction<TestTarget> {

    private final Supplier<T> supplier;

    public static <T> SupplierFunction<T> of(Supplier<T> supplier) {
      return new SupplierFunction<>(supplier);
    }

    private SupplierFunction(Supplier<T> supplier) {
      this.supplier = supplier;
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
        return ImmutableMap.of(Measures.PRESENT_VALUE, (Result<?>) obj);
      }
      ScenarioResult<Object> array = ScenarioResult.of(obj);
      return ImmutableMap.of(Measures.PRESENT_VALUE, Result.success(array));
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
