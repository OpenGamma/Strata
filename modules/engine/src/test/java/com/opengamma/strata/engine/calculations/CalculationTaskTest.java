/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.CurrencyValuesArray;
import com.opengamma.strata.engine.calculations.function.result.FxRateMapping;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.TestId;
import com.opengamma.strata.engine.marketdata.TestKey;
import com.opengamma.strata.engine.marketdata.TestMapping;
import com.opengamma.strata.engine.marketdata.TestObservableKey;
import com.opengamma.strata.engine.marketdata.mapping.DefaultMarketDataMappings;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class CalculationTaskTest {

  private static final MarketDataMappings MAPPINGS = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
  private static final ReportingRules REPORTING_RULES = ReportingRules.fixedCurrency(Currency.USD);
  private static final TestTarget TARGET = new TestTarget();

  public void requirements() {
    MarketDataFeed marketDataFeed = MarketDataFeed.of("MarketDataVendor");
    MarketDataMappings marketDataMappings =
        DefaultMarketDataMappings.builder()
            .mappings(ImmutableMap.of(TestKey.class, new TestMapping("foo")))
            .marketDataFeed(marketDataFeed)
            .build();
    CalculationTask task =
        new CalculationTask(new TestTarget(), 0, 0, new TestFunction(), marketDataMappings, ReportingRules.empty());
    MarketDataRequirements requirements = task.requirements();
    Set<? extends MarketDataId<?>> nonObservables = requirements.getNonObservables();
    ImmutableSet<? extends ObservableId> observables = requirements.getObservables();
    ImmutableSet<ObservableId> timeSeries = requirements.getTimeSeries();

    MarketDataId<?> timeSeriesId = TestObservableKey.of("3").toObservableId(marketDataFeed);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(timeSeriesId);

    MarketDataId<?> nonObservableId = TestId.of("1");
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(nonObservableId);

    MarketDataId<?> observableId = TestObservableKey.of("2").toObservableId(marketDataFeed);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(observableId);
  }

  /**
   * Test that the result is converted to the reporting currency if it implements CurrencyConvertible and
   * the FX rates are available in the market data
   */
  public void convertResultCurrency() {
    double[] values = {1, 2, 3};
    List<Double> rates = ImmutableList.of(1.61, 1.62, 1.63);
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8))
        .addValues(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list);
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);

    double[] expectedValues = {1 * 1.61, 2 * 1.62, 3 * 1.63};
    CurrencyValuesArray expectedArray = CurrencyValuesArray.of(Currency.USD, expectedValues);
    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(expectedArray);
  }

  /**
   * Test that the result is returned unchanged if it is a failure.
   */
  public void convertResultCurrencyFailure() {
    ConvertibleFunction fn = ConvertibleFunction.of(() -> { throw new RuntimeException("This is a failure"); });
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    CalculationResult calculationResult = task.execute(ScenarioMarketData.builder(1, date(2011, 3, 8)).build());
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasFailureMessageMatching("This is a failure");
  }

  /**
   * Test the result is returned unchanged if it is not CurrencyConvertible.
   */
  public void convertResultCurrencyNotConvertible() {
    TestFunction fn = new TestFunction();
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    CalculationResult calculationResult = task.execute(ScenarioMarketData.builder(1, date(2011, 3, 8)).build());
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue("bar");
  }

  /**
   * Test the result is returned unchanged if it is convertible but no reporting currency is available.
   */
  public void convertResultCurrencyNoReportingCurrency() {
    double[] values = {1, 2, 3};
    List<Double> rates = ImmutableList.of(1.61, 1.62, 1.63);
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8))
        .addValues(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list);
    ReportingRules reportingRules = ReportingRules.empty();
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, reportingRules);

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue(list);
  }

  /**
   * Test that a failure is returned if currency conversion fails.
   */
  public void convertResultCurrencyConversionFails() {
    double[] values = {1, 2, 3};
    CurrencyValuesArray list = CurrencyValuesArray.of(Currency.GBP, values);
    // Market data doesn't include FX rates, conversion to USD will fail
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();
    ConvertibleFunction fn = ConvertibleFunction.of(() -> list);
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasFailureMessageMatching("Failed to convert value .* to currency USD");
  }

  /**
   * Tests that executing a function wraps its return value in a success result.
   */
  public void execute() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> "foo");
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue("foo");
  }

  /**
   * Tests that executing a function that throws an exception wraps the exception in a failure result.
   */
  public void executeException() {
    SupplierFunction<String> fn = SupplierFunction.of(() -> { throw new IllegalArgumentException("foo"); });
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).isFailure(FailureReason.ERROR).hasFailureMessageMatching("foo");
  }

  /**
   * Tests that executing a function that returns a success result returns the underlying result without wrapping it.
   */
  public void executeSuccessResultValue() {
    SupplierFunction<Result<String>> fn = SupplierFunction.of(() -> Result.success("foo"));
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).hasValue("foo");
  }

  /**
   * Tests that executing a function that returns a failure result returns the underlying result without wrapping it.
   */
  public void executeFailureResultValue() {
    SupplierFunction<Result<String>> fn = SupplierFunction.of(() -> Result.failure(FailureReason.NOT_APPLICABLE, "bar"));
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();

    CalculationResult calculationResult = task.execute(marketData);
    Result<?> result = calculationResult.getResult();
    assertThat(result).isFailure(FailureReason.NOT_APPLICABLE).hasFailureMessageMatching("bar");
  }

  /**
   * Tests that requirements are added for the FX rates needed to convert the results into the reporting currency.
   */
  public void fxConversionRequirements() {
    OutputCurrenciesFunction fn = new OutputCurrenciesFunction();
    CalculationTask task = new CalculationTask(TARGET, 0, 0, fn, MAPPINGS, REPORTING_RULES);
    MarketDataRequirements requirements = task.requirements();

    assertThat(requirements.getNonObservables()).contains(
        FxRateId.of(Currency.GBP, Currency.USD),
        FxRateId.of(Currency.EUR, Currency.USD));
  }

  //--------------------------------------------------------------------------------------------------------------------

  private static class TestTarget implements CalculationTarget { }

  /**
   * Function that returns a value that is not currency convertible.
   */
  public static final class TestFunction implements CalculationSingleFunction<TestTarget, Object> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  TestKey.of("1"),
                  TestObservableKey.of("2")))
          .timeSeriesRequirements(TestObservableKey.of("3"))
          .build();
    }

    @Override
    public Object execute(TestTarget target, CalculationMarketData marketData) {
      return "bar";
    }
  }

  /**
   * Function that returns a value that is currency convertible.
   */
  private static final class ConvertibleFunction
      implements CalculationSingleFunction<TestTarget, CurrencyValuesArray> {

    private final Supplier<CurrencyValuesArray> supplier;

    public static ConvertibleFunction of(Supplier<CurrencyValuesArray> supplier) {
      return new ConvertibleFunction(supplier);
    }

    private ConvertibleFunction(Supplier<CurrencyValuesArray> supplier) {
      this.supplier = supplier;
    }

    @Override
    public CurrencyValuesArray execute(TestTarget target, CalculationMarketData marketData) {
      return supplier.get();
    }

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.empty();
    }
  }

  /**
   * Function that returns a value from a Supplier.
   */
  private static final class SupplierFunction<T> implements CalculationSingleFunction<TestTarget, T> {

    private final Supplier<T> supplier;

    public static <T> SupplierFunction<T> of(Supplier<T> supplier) {
      return new SupplierFunction<>(supplier);
    }

    private SupplierFunction(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    @Override
    public T execute(TestTarget target, CalculationMarketData marketData) {
      return supplier.get();
    }

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.empty();
    }
  }

  /**
   * Function that returns requirements containing output currencies.
   */
  private static final class OutputCurrenciesFunction implements CalculationSingleFunction<TestTarget, Object> {

    @Override
    public Object execute(TestTarget target, CalculationMarketData marketData) {
      throw new UnsupportedOperationException("execute not implemented");
    }

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.builder()
          .outputCurrencies(Currency.GBP, Currency.EUR)
          .build();
    }
  }
}
