/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ScenarioPerturbation;

@Test
public class DefaultMarketDataFactoryTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final MarketDataConfig MARKET_DATA_CONFIG = MarketDataConfig.empty();

  /**
   * Tests building time series from requirements.
   */
  public void buildTimeSeries() {
    TestObservableId id1 = TestObservableId.of("1");
    TestObservableId id2 = TestObservableId.of("2");
    LocalDateDoubleTimeSeries timeSeries1 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 1)
        .put(date(2011, 3, 9), 2)
        .put(date(2011, 3, 10), 3)
        .build();
    LocalDateDoubleTimeSeries timeSeries2 = LocalDateDoubleTimeSeries.builder()
        .put(date(2012, 4, 8), 10)
        .put(date(2012, 4, 9), 20)
        .put(date(2012, 4, 10), 30)
        .build();
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of(id1, timeSeries1, id2, timeSeries2);
    MarketDataFactory factory =
        MarketDataFactory.of(ObservableDataProvider.none(), new TestTimeSeriesProvider(timeSeries));

    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addTimeSeries(id1, id2)
        .build();
    MarketData suppliedData = MarketData.empty(date(2011, 3, 8));
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);
    assertThat(marketData.getTimeSeries(id1)).isEqualTo(timeSeries1);
    assertThat(marketData.getTimeSeries(id2)).isEqualTo(timeSeries2);
    assertThat(marketData.getTimeSeriesIds()).isEqualTo(ImmutableSet.of(id1, id2));
  }

  /**
   * Tests non-observable market data values supplied by the user are included in the results.
   */
  public void buildSuppliedNonObservableValues() {
    TestId id1 = new TestId("1");
    TestId id2 = new TestId("2");
    MarketData suppliedData = ImmutableMarketData.builder(date(2011, 3, 8))
        .addValue(id1, "foo")
        .addValue(id2, "bar")
        .build();
    MarketDataFactory factory =
        MarketDataFactory.of(ObservableDataProvider.none(), new TestTimeSeriesProvider(ImmutableMap.of()));
    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(id1, id2)
        .build();
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);
    assertThat(marketData.getValue(id1)).isEqualTo("foo");
    assertThat(marketData.getValue(id2)).isEqualTo("bar");
  }

  /**
   * Tests building single values using market data functions.
   */
  public void buildNonObservableValues() {
    ObservableId idA = new TestIdA("1");
    MarketDataId<?> idC = new TestIdC("1");
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.builder()
        .put(date(2012, 4, 8), 10)
        .put(date(2012, 4, 9), 20)
        .put(date(2012, 4, 10), 30)
        .build();

    MarketData suppliedData = ImmutableMarketData.builder(date(2011, 3, 8))
        .addTimeSeries(idA, timeSeries)
        .build();
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()),
        new TestMarketDataFunctionC());

    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(idC)
        .build();
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);
    assertThat(marketData.getValue(idC)).isEqualTo(new TestMarketDataC(timeSeries));
  }

  /**
   * Tests building observable market data values.
   */
  public void buildObservableValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()));

    MarketData suppliedData = MarketData.empty(date(2011, 3, 8));
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);
    assertThat(marketData.getValue(id1)).isEqualTo(1d);
    assertThat(marketData.getValue(id2)).isEqualTo(2d);
  }

  /**
   * Tests observable market data values supplied by the user are included in the results.
   */
  public void buildSuppliedObservableValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()));

    TestObservableId id1 = TestObservableId.of("a");
    TestObservableId id2 = TestObservableId.of("b");

    MarketData suppliedData = ImmutableMarketData.builder(date(2011, 3, 8))
        .addValue(id1, 1d)
        .addValue(id2, 2d)
        .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);
    assertThat(marketData.getValue(id1)).isEqualTo(1d);
    assertThat(marketData.getValue(id2)).isEqualTo(2d);
  }

  /**
   * Tests building market data that depends on other market data.
   */
  public void buildDataFromOtherData() {
    TestMarketDataFunctionB builderB = new TestMarketDataFunctionB();
    TestMarketDataFunctionC builderC = new TestMarketDataFunctionC();

    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(new TestIdB("1"), new TestIdB("2"))
        .build();

    LocalDateDoubleTimeSeries timeSeries1 =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 1)
            .put(date(2011, 3, 9), 2)
            .put(date(2011, 3, 10), 3)
            .build();

    LocalDateDoubleTimeSeries timeSeries2 =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 10)
            .put(date(2011, 3, 9), 20)
            .put(date(2011, 3, 10), 30)
            .build();

    Map<TestIdA, LocalDateDoubleTimeSeries> timeSeriesMap =
        ImmutableMap.of(
            new TestIdA("1"), timeSeries1,
            new TestIdA("2"), timeSeries2);

    TimeSeriesProvider timeSeriesProvider = new TestTimeSeriesProvider(timeSeriesMap);

    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        timeSeriesProvider,
        builderB,
        builderC);

    MarketData suppliedData = MarketData.empty(date(2011, 3, 8));
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);

    assertThat(marketData.getValueFailures()).isEmpty();
    assertThat(marketData.getTimeSeriesFailures()).isEmpty();

    TestMarketDataB marketDataB1 = marketData.getValue(new TestIdB("1"));
    TestMarketDataB marketDataB2 = marketData.getValue(new TestIdB("2"));

    TestMarketDataB expectedB1 = new TestMarketDataB(1, new TestMarketDataC(timeSeries1));
    TestMarketDataB expectedB2 = new TestMarketDataB(2, new TestMarketDataC(timeSeries2));

    assertThat(marketDataB1).isEqualTo(expectedB1);
    assertThat(marketDataB2).isEqualTo(expectedB2);
  }

  /**
   * Tests building market data that depends on other market data that is supplied by the user.
   *
   * This tests that supplied data is included in scenario data if it is not in the requirements but it is
   * needed to build data that is in the requirements.
   *
   * For example, par rates are required to build curves but are not used directly by functions so the
   * requirements will not contain par rates IDs. The requirements contain curve IDs and the curve
   * building function will declare that it requires par rates.
   */
  public void buildDataFromSuppliedData() {
    TestMarketDataFunctionB builderB = new TestMarketDataFunctionB();
    TestMarketDataFunctionC builderC = new TestMarketDataFunctionC();

    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(new TestIdB("1"), new TestIdB("2"))
        .build();

    LocalDateDoubleTimeSeries timeSeries1 =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 1)
            .put(date(2011, 3, 9), 2)
            .put(date(2011, 3, 10), 3)
            .build();

    LocalDateDoubleTimeSeries timeSeries2 =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 10)
            .put(date(2011, 3, 9), 20)
            .put(date(2011, 3, 10), 30)
            .build();

    TestIdA idA1 = new TestIdA("1");
    TestIdA idA2 = new TestIdA("2");

    MarketData suppliedData = ImmutableMarketData.builder(date(2011, 3, 8))
        .addTimeSeries(idA1, timeSeries1)
        .addTimeSeries(idA2, timeSeries2)
        .addValue(idA1, 1d)
        .addValue(idA2, 2d)
        .build();

    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        TimeSeriesProvider.none(),
        builderB,
        builderC);

    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);

    assertThat(marketData.getValueFailures()).isEmpty();
    assertThat(marketData.getTimeSeriesFailures()).isEmpty();

    TestMarketDataB marketDataB1 = marketData.getValue(new TestIdB("1"));
    TestMarketDataB marketDataB2 = marketData.getValue(new TestIdB("2"));

    TestMarketDataB expectedB1 = new TestMarketDataB(1, new TestMarketDataC(timeSeries1));
    TestMarketDataB expectedB2 = new TestMarketDataB(2, new TestMarketDataC(timeSeries2));

    assertThat(marketDataB1).isEqualTo(expectedB1);
    assertThat(marketDataB2).isEqualTo(expectedB2);
  }

  /**
   * Tests an exception is thrown when there is no builder for an ID type.
   */
  public void noMarketDataBuilderAvailable() {
    TestIdB idB1 = new TestIdB("1");
    TestIdB idB2 = new TestIdB("2");
    TestMarketDataFunctionB builder = new TestMarketDataFunctionB();

    // Market data B depends on market data C so these requirements should cause instances of C to be built.
    // There is no market data function for building instances of C so this should cause failures.
    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(idB1, idB2)
        .build();

    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()),
        builder);

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();
    assertThrows(
        () -> factory.createMultiScenario(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA, ScenarioDefinition.empty()),
        IllegalStateException.class,
        "No market data function available for market data ID of type.*");
  }

  /**
   * Tests building a result and keeping the intermediate values.
   */
  public void buildWithIntermediateValues() {
    TestMarketDataFunctionB builderB = new TestMarketDataFunctionB();
    TestMarketDataFunctionC builderC = new TestMarketDataFunctionC();

    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(new TestIdB("1"), new TestIdB("2"))
        .build();

    LocalDateDoubleTimeSeries timeSeries1 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 1)
        .put(date(2011, 3, 9), 2)
        .put(date(2011, 3, 10), 3)
        .build();

    LocalDateDoubleTimeSeries timeSeries2 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 10)
        .put(date(2011, 3, 9), 20)
        .put(date(2011, 3, 10), 30)
        .build();

    Map<TestIdA, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(
        new TestIdA("1"), timeSeries1,
        new TestIdA("2"), timeSeries2);

    TimeSeriesProvider timeSeriesProvider = new TestTimeSeriesProvider(timeSeriesMap);

    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        timeSeriesProvider,
        builderB,
        builderC);

    MarketData suppliedData = MarketData.empty(date(2011, 3, 8));
    BuiltMarketData marketData = factory.create(requirements, MARKET_DATA_CONFIG, suppliedData, REF_DATA);

    assertThat(marketData.getValueFailures()).isEmpty();
    assertThat(marketData.getTimeSeriesFailures()).isEmpty();

    TestMarketDataC expectedC1 = new TestMarketDataC(timeSeries1);
    TestMarketDataC expectedC2 = new TestMarketDataC(timeSeries2);
    TestMarketDataB expectedB1 = new TestMarketDataB(1, expectedC1);
    TestMarketDataB expectedB2 = new TestMarketDataB(2, expectedC2);

    // Check the values in the requirements are present
    assertThat(marketData.getValue(new TestIdB("1"))).isEqualTo(expectedB1);
    assertThat(marketData.getValue(new TestIdB("2"))).isEqualTo(expectedB2);

    // Check the intermediate values are present
    assertThat(marketData.getValue(new TestIdA("1"))).isEqualTo(1d);
    assertThat(marketData.getValue(new TestIdA("2"))).isEqualTo(2d);
    assertThat(marketData.getValue(new TestIdC("1"))).isEqualTo(expectedC1);
    assertThat(marketData.getValue(new TestIdC("2"))).isEqualTo(expectedC2);
  }

  /**
   * Tests building multiple observable values for scenarios where the values aren't perturbed.
   */
  public void buildObservableScenarioValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()));

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new FalseFilter<>(TestObservableId.class),
            new AbsoluteDoubleShift(1, 2, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);
    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofSingleValue(1d));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofSingleValue(2d));
  }

  /**
   * Tests observable values supplied by the user are included in the results when they aren't perturbed
   */
  public void buildSuppliedObservableScenarioValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()));
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    BuiltScenarioMarketData suppliedData =
        BuiltScenarioMarketData.builder(date(2011, 3, 8))
            .addValue(id1, 1d)
            .addValue(id2, 2d)
            .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new FalseFilter<>(TestObservableId.class),
            new AbsoluteDoubleShift(1, 2, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofSingleValue(1d));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofSingleValue(2d));
  }

  /**
   * Test that time series from the supplied data are copied to the scenario data.
   */
  public void buildSuppliedTimeSeries() {
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()));

    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));

    LocalDateDoubleTimeSeries timeSeries1 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 1)
        .put(date(2011, 3, 9), 2)
        .put(date(2011, 3, 10), 3)
        .build();

    LocalDateDoubleTimeSeries timeSeries2 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 10)
        .put(date(2011, 3, 9), 20)
        .put(date(2011, 3, 10), 30)
        .build();

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8))
        .addTimeSeries(id1, timeSeries1)
        .addTimeSeries(id2, timeSeries2)
        .build();

    MarketDataRequirements requirements = MarketDataRequirements.builder().addTimeSeries(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<Double> mapping = PerturbationMapping.of(
        Double.class,
        new FalseFilter<>(TestObservableId.class),
        new AbsoluteDoubleShift(1, 2, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getTimeSeries(id1)).isEqualTo(timeSeries1);
    assertThat(marketData.getTimeSeries(id2)).isEqualTo(timeSeries2);
  }

  public void perturbObservableValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()));

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    PerturbationMapping<Double> mapping = PerturbationMapping.of(
        Double.class,
        new ExactIdFilter<>(id1),
        new AbsoluteDoubleShift(1, 2, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofScenarioValues(2d, 3d, 4d));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofSingleValue(2d));
  }

  /**
   * Tests that observable data is only perturbed once, even if there are two applicable perturbation mappings.
   */
  public void observableDataOnlyPerturbedOnce() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()));

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    PerturbationMapping<Double> mapping1 = PerturbationMapping.of(
        Double.class,
        new ExactIdFilter<>(id2),
        new RelativeDoubleShift(0.1, 0.2, 0.3));
    PerturbationMapping<Double> mapping2 = PerturbationMapping.of(
        Double.class,
        new ExactIdFilter<>(id2),
        new AbsoluteDoubleShift(1, 2, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping1, mapping2));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofSingleValue(1d));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofScenarioValues(2.2d, 2.4d, 2.6d));
  }

  /**
   * Tests building multiple values of non-observable market data for multiple scenarios. The data isn't perturbed.
   */
  public void buildNonObservableScenarioValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()),
        new NonObservableMarketDataFunction());

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();
    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<String> mapping = PerturbationMapping.of(
        String.class,
        new FalseFilter<>(NonObservableId.class),
        new StringAppender("", "", ""));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    MarketDataBox<String> box1 = marketData.getValue(id1);
    assertThat(box1.getValue(0)).isEqualTo("1.0");
    assertThat(box1.getValue(1)).isEqualTo("1.0");
    assertThat(box1.getValue(2)).isEqualTo("1.0");

    MarketDataBox<String> box2 = marketData.getValue(id2);
    assertThat(box2.getValue(0)).isEqualTo("2.0");
    assertThat(box2.getValue(1)).isEqualTo("2.0");
    assertThat(box2.getValue(2)).isEqualTo("2.0");
  }

  /**
   * Tests non-observable values supplied by the user are included in the results when they aren't perturbed
   */
  public void buildSuppliedNonObservableScenarioValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()));
    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8))
        .addValue(id1, "value1")
        .addValue(id2, "value2")
        .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<String> mapping = PerturbationMapping.of(
        String.class,
        new FalseFilter<>(NonObservableId.class),
        new StringAppender("", "", ""));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofSingleValue("value1"));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofSingleValue("value2"));
  }

  /**
   * Tests building scenario data from values that are supplied by the user but aren't directly required
   * by the functions.
   *
   * For example, par rates are required to build curves but are not used directly by functions so the
   * requirements will not contain par rates IDs. The requirements contain curve IDs and the curve
   * building function will declare that it requires par rates.
   */
  public void buildScenarioValuesFromSuppliedData() {
    TestMarketDataFunctionB builderB = new TestMarketDataFunctionB();
    TestMarketDataFunctionC builderC = new TestMarketDataFunctionC();
    TestIdB idB1 = new TestIdB("1");
    TestIdB idB2 = new TestIdB("2");

    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(idB1, idB2)
        .build();

    LocalDateDoubleTimeSeries timeSeries1 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 1)
        .put(date(2011, 3, 9), 2)
        .put(date(2011, 3, 10), 3)
        .build();

    LocalDateDoubleTimeSeries timeSeries2 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 10)
        .put(date(2011, 3, 9), 20)
        .put(date(2011, 3, 10), 30)
        .build();

    TestIdA idA1 = new TestIdA("1");
    TestIdA idA2 = new TestIdA("2");

    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8))
        .addTimeSeries(idA1, timeSeries1)
        .addTimeSeries(idA2, timeSeries2)
        .addValue(idA1, 1d)
        .addValue(idA2, 2d)
        .build();

    MarketDataFactory marketDataFactory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        TimeSeriesProvider.none(),
        builderB,
        builderC);

    PerturbationMapping<Double> aMapping = PerturbationMapping.of(
        Double.class,
        new ExactIdFilter<>(new TestIdA("2")),
        new RelativeDoubleShift(0.2, 0.3, 0.4));

    PerturbationMapping<TestMarketDataC> cMapping = PerturbationMapping.of(
        TestMarketDataC.class,
        new ExactIdFilter<>(new TestIdC("1")),
        new TestCPerturbation(1.1, 1.2, 1.3));

    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(aMapping, cMapping);
    BuiltScenarioMarketData marketData = marketDataFactory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValueFailures()).isEmpty();
    assertThat(marketData.getTimeSeriesFailures()).isEmpty();

    MarketDataBox<TestMarketDataB> marketDataB1 = marketData.getValue(idB1);
    MarketDataBox<TestMarketDataB> marketDataB2 = marketData.getValue(idB2);

    MarketDataBox<TestMarketDataB> expectedB1 = MarketDataBox.ofScenarioValues(
        new TestMarketDataB(1, new TestMarketDataC(timeSeries1.mapValues(v -> v * 1.1))),
        new TestMarketDataB(1, new TestMarketDataC(timeSeries1.mapValues(v -> v * 1.2))),
        new TestMarketDataB(1, new TestMarketDataC(timeSeries1.mapValues(v -> v * 1.3))));

    MarketDataBox<TestMarketDataB> expectedB2 = MarketDataBox.ofScenarioValues(
        new TestMarketDataB(2.4, new TestMarketDataC(timeSeries2)),
        new TestMarketDataB(2.6, new TestMarketDataC(timeSeries2)),
        new TestMarketDataB(2.8, new TestMarketDataC(timeSeries2)));

    assertThat(marketDataB1).isEqualTo(expectedB1);
    assertThat(marketDataB2).isEqualTo(expectedB2);
  }

  /**
   * Tests that perturbations are applied to non-observable market data.
   */
  public void perturbNonObservableValues() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()),
        new NonObservableMarketDataFunction());
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    PerturbationMapping<String> mapping =
        PerturbationMapping.of(
            String.class,
            new ExactIdFilter<>(id1),
            new StringAppender("foo", "bar", "baz"));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofScenarioValues("1.0foo", "1.0bar", "1.0baz"));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofSingleValue("2.0"));
  }

  /**
   * Tests that non-observable data is only perturbed once, even if there are two applicable perturbation mappings.
   */
  public void nonObservableDataOnlyPerturbedOnce() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()),
        new NonObservableMarketDataFunction());
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    PerturbationMapping<String> mapping1 = PerturbationMapping.of(
        String.class,
        new ExactIdFilter<>(id1),
        new StringAppender("FOO", "BAR", "BAZ"));
    PerturbationMapping<String> mapping2 = PerturbationMapping.of(
        String.class,
        new ExactIdFilter<>(id1),
        new StringAppender("foo", "bar", "baz"));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping1, mapping2));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofScenarioValues("1.0FOO", "1.0BAR", "1.0BAZ"));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofSingleValue("2.0"));
  }

  /**
   * Tests that observable data built from observable values see the effects of the perturbations.
   */
  public void nonObservableDataBuiltFromPerturbedObservableData() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()),
        new NonObservableMarketDataFunction());
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();

    MarketDataId<?> id1 = new NonObservableId("a");
    MarketDataId<?> id2 = new NonObservableId("b");
    TestObservableId quoteId = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    PerturbationMapping<Double> mapping = PerturbationMapping.of(
        Double.class,
        new ExactIdFilter<>(quoteId),
        new RelativeDoubleShift(0.1, 0.2, 0.3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);

    assertThat(marketData.getValue(id1)).isEqualTo(MarketDataBox.ofSingleValue("1.0"));
    assertThat(marketData.getValue(id2)).isEqualTo(MarketDataBox.ofScenarioValues("2.2", "2.4", "2.6"));
  }

  /**
   * Tests that an exception is thrown when building observable market data for scenarios where there is no
   * market data function.
   */
  public void nonObservableScenarioDataWithMissingBuilder() {
    MarketDataFactory factory = MarketDataFactory.of(
        new TestObservableDataProvider(),
        new TestTimeSeriesProvider(ImmutableMap.of()));
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8)).build();

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<String> mapping = PerturbationMapping.of(
        String.class,
        new FalseFilter<>(NonObservableId.class),
        new StringAppender("", "", ""));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));

    assertThrows(
        () -> factory.createMultiScenario(
            requirements,
            MARKET_DATA_CONFIG,
            suppliedData,
            REF_DATA, scenarioDefinition),
        IllegalStateException.class,
        "No market data function available for market data ID of type.*");

  }

  /**
   * Tests that perturbations are applied to observable data supplied by the user.
   */
  public void perturbSuppliedNonObservableData() {
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()));
    NonObservableId id = new NonObservableId("a");
    PerturbationMapping<String> mapping = PerturbationMapping.of(
        String.class,
        new ExactIdFilter<>(id),
        new StringAppender("Foo", "Bar", "Baz"));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8))
        .addValue(id, "value")
        .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id).build();
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);
    MarketDataBox<String> values = marketData.getValue(id);
    MarketDataBox<String> expectedValues = MarketDataBox.ofScenarioValues("valueFoo", "valueBar", "valueBaz");
    assertThat(values).isEqualTo(expectedValues);
  }

  /**
   * Tests that perturbations are applied to non-observable data supplied by the user.
   */
  public void perturbSuppliedObservableData() {
    MarketDataFactory factory = MarketDataFactory.of(
        ObservableDataProvider.none(),
        new TestTimeSeriesProvider(ImmutableMap.of()));
    TestObservableId id = TestObservableId.of(StandardId.of("reqs", "a"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id).build();
    PerturbationMapping<Double> mapping = PerturbationMapping.of(
        Double.class,
        new ExactIdFilter<>(id),
        new RelativeDoubleShift(0.1, 0.2, 0.3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BuiltScenarioMarketData suppliedData = BuiltScenarioMarketData.builder(date(2011, 3, 8))
        .addValue(id, 2d)
        .build();
    BuiltScenarioMarketData marketData = factory.createMultiScenario(
        requirements,
        MARKET_DATA_CONFIG,
        suppliedData,
        REF_DATA, scenarioDefinition);
    MarketDataBox<Double> values = marketData.getValue(id);
    MarketDataBox<Double> expectedValues = MarketDataBox.ofScenarioValues(2.2, 2.4, 2.6);
    assertThat(values).isEqualTo(expectedValues);
  }

  /**
   * Tests ObservableDataProvider.none(), which is never normally be invoked.
   */
  public void coverage_ObservableDataProvider_none() {
    TestObservableId id = TestObservableId.of(StandardId.of("reqs", "a"));
    ObservableDataProvider test = ObservableDataProvider.none();
    Map<ObservableId, Result<Double>> result = test.provideObservableData(ImmutableSet.of(id));
    assertThat(result).containsOnlyKeys(id);
    assertThat(result.get(id).isFailure()).isTrue();
  }

  //-------------------------------------------------------------------------
  /**
   * Simple time series provider backed by a map.
   */
  private static final class TestTimeSeriesProvider implements TimeSeriesProvider {

    private final Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries;

    private TestTimeSeriesProvider(Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
      this.timeSeries = timeSeries;
    }

    @Override
    public Result<LocalDateDoubleTimeSeries> provideTimeSeries(ObservableId id) {
      LocalDateDoubleTimeSeries series = timeSeries.get(id);
      return Result.ofNullable(series, FailureReason.MISSING_DATA, "No time series found for ID {}", id);
    }
  }

  /**
   * Builds observable data by parsing the value of the standard ID.
   */
  private static final class TestObservableDataProvider implements ObservableDataProvider {

    // demonstrates provider that maps identifiers
    private final Map<ObservableId, ObservableId> idMap =
        ImmutableMap.of(
            TestObservableId.of(StandardId.of("reqs", "a")), TestObservableId.of(StandardId.of("vendor", "1")),
            TestObservableId.of(StandardId.of("reqs", "b")), TestObservableId.of(StandardId.of("vendor", "2")));

    @Override
    public Map<ObservableId, Result<Double>> provideObservableData(Set<? extends ObservableId> requirements) {
      return requirements.stream().collect(toImmutableMap(id -> id, id -> buildResult(idMap.getOrDefault(id, id))));
    }

    private Result<Double> buildResult(ObservableId id) {
      return Result.success(Double.parseDouble(id.getStandardId().getValue()));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Test ID A.
   */
  private static final class TestIdA implements ObservableId {

    private final StandardId id;

    TestIdA(String id) {
      this.id = StandardId.of("test", id);
    }

    @Override
    public StandardId getStandardId() {
      return id;
    }

    @Override
    public FieldName getFieldName() {
      return FieldName.MARKET_VALUE;
    }

    @Override
    public ObservableSource getObservableSource() {
      return ObservableSource.NONE;
    }

    @Override
    public ObservableId withObservableSource(ObservableSource obsSource) {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestIdA id = (TestIdA) o;
      return Objects.equals(this.id, id.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "TestIdA [id=" + id + "]";
    }
  }

  /**
   * Test ID B.
   */
  private static final class TestIdB implements MarketDataId<TestMarketDataB> {

    private final String str;

    TestIdB(String str) {
      this.str = str;
    }

    @Override
    public Class<TestMarketDataB> getMarketDataType() {
      return TestMarketDataB.class;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestIdB id = (TestIdB) o;
      return Objects.equals(str, id.str);
    }

    @Override
    public int hashCode() {
      return Objects.hash(str);
    }

    @Override
    public String toString() {
      return "TestIdB [str='" + str + "']";
    }
  }

  private static final class TestIdC implements MarketDataId<TestMarketDataC> {

    private final String str;

    TestIdC(String str) {
      this.str = str;
    }

    @Override
    public Class<TestMarketDataC> getMarketDataType() {
      return TestMarketDataC.class;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestIdC id = (TestIdC) o;
      return Objects.equals(str, id.str);
    }

    @Override
    public int hashCode() {
      return Objects.hash(str);
    }

    @Override
    public String toString() {
      return "TestIdC [str='" + str + "']";
    }
  }

  /**
   * Test market data B.
   */
  private static final class TestMarketDataB {

    private final double value;

    private final TestMarketDataC marketData;

    TestMarketDataB(double value, TestMarketDataC marketData) {
      this.value = value;
      this.marketData = marketData;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestMarketDataB that = (TestMarketDataB) o;
      return Objects.equals(value, that.value) &&
          Objects.equals(marketData, that.marketData);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, marketData);
    }
  }

  /**
   * Function for building TestMarketDataB.
   * Requires a value with ID TestIdA(id.str) and TestMarketDataC with ID TestIdC(id.str)
   */
  private final class TestMarketDataFunctionB implements MarketDataFunction<TestMarketDataB, TestIdB> {

    @Override
    public MarketDataRequirements requirements(TestIdB id, MarketDataConfig marketDataConfig) {
      return MarketDataRequirements.builder()
          .addValues(new TestIdA(id.str), new TestIdC(id.str))
          .build();
    }

    @Override
    public MarketDataBox<TestMarketDataB> build(
        TestIdB id,
        MarketDataConfig marketDataConfig,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      TestIdA idA = new TestIdA(id.str);
      TestIdC idC = new TestIdC(id.str);
      MarketDataBox<Double> valueA = marketData.getValue(idA);
      MarketDataBox<TestMarketDataC> marketDataC = marketData.getValue(idC);
      return valueA.combineWith(marketDataC, TestMarketDataB::new);
    }

    @Override
    public Class<TestIdB> getMarketDataIdType() {
      return TestIdB.class;
    }
  }

  /**
   * Test market data C.
   */
  private static final class TestMarketDataC {

    private final LocalDateDoubleTimeSeries timeSeries;

    TestMarketDataC(LocalDateDoubleTimeSeries timeSeries) {
      this.timeSeries = timeSeries;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestMarketDataC that = (TestMarketDataC) o;
      return Objects.equals(timeSeries, that.timeSeries);
    }

    @Override
    public int hashCode() {
      return Objects.hash(timeSeries);
    }
  }

  /**
   * Function for building TestMarketDataC.
   * Requires a time series with ID TestIdA(id.str)
   */
  private static final class TestMarketDataFunctionC implements MarketDataFunction<TestMarketDataC, TestIdC> {

    @Override
    public MarketDataRequirements requirements(TestIdC id, MarketDataConfig marketDataConfig) {
      return MarketDataRequirements.builder()
          .addTimeSeries(new TestIdA(id.str))
          .build();
    }

    @Override
    public MarketDataBox<TestMarketDataC> build(
        TestIdC id,
        MarketDataConfig marketDataConfig,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      LocalDateDoubleTimeSeries timeSeries = marketData.getTimeSeries(new TestIdA(id.str));
      return MarketDataBox.ofSingleValue(new TestMarketDataC(timeSeries));
    }

    @Override
    public Class<TestIdC> getMarketDataIdType() {
      return TestIdC.class;
    }
  }

  /**
   * Market data filter that doesn't match any market data.
   */
  private static final class FalseFilter<T, I extends MarketDataId<T>> implements MarketDataFilter<T, I> {

    private final Class<?> idType;

    private FalseFilter(Class<?> idType) {
      this.idType = idType;
    }

    @Override
    public boolean matches(I marketDataId, MarketDataBox<T> marketData, ReferenceData refData) {
      return false;
    }

    @Override
    public Class<?> getMarketDataIdType() {
      return idType;
    }
  }

  /**
   * Perturbation that applies a shift to a double value.
   */
  private static final class AbsoluteDoubleShift implements ScenarioPerturbation<Double> {

    private final double[] shiftAmount;

    private AbsoluteDoubleShift(double... shiftAmount) {
      this.shiftAmount = shiftAmount;
    }

    @Override
    public MarketDataBox<Double> applyTo(MarketDataBox<Double> marketData, ReferenceData refData) {
      return marketData.mapWithIndex(getScenarioCount(), (value, scenarioIndex) -> value + shiftAmount[scenarioIndex]);
    }

    @Override
    public int getScenarioCount() {
      return shiftAmount.length;
    }
  }

  /**
   * Perturbation that applies a shift to a double value.
   */
  private static final class RelativeDoubleShift implements ScenarioPerturbation<Double> {

    private final double[] shiftAmounts;

    private RelativeDoubleShift(double... shiftAmounts) {
      this.shiftAmounts = shiftAmounts;
    }

    @Override
    public MarketDataBox<Double> applyTo(MarketDataBox<Double> marketData, ReferenceData refData) {
      return marketData.mapWithIndex(getScenarioCount(), (value, scenarioIndex) -> value * (1 + shiftAmounts[scenarioIndex]));
    }

    @Override
    public int getScenarioCount() {
      return shiftAmounts.length;
    }
  }

  /**
   * Market data filter that matches an ID exactly.
   */
  private static final class ExactIdFilter<T, I extends MarketDataId<T>> implements MarketDataFilter<T, I> {

    private final I id;

    private ExactIdFilter(I id) {
      this.id = id;
    }

    @Override
    public boolean matches(I marketDataId, MarketDataBox<T> marketData, ReferenceData refData) {
      return id.equals(marketDataId);
    }

    @Override
    public Class<?> getMarketDataIdType() {
      return id.getClass();
    }
  }

  /**
   * Market data ID for a piece of non-observable market data that is a string.
   */
  private static final class NonObservableId implements MarketDataId<String> {

    private final String str;

    private NonObservableId(String str) {
      this.str = str;
    }

    @Override
    public Class<String> getMarketDataType() {
      return String.class;
    }

    @Override
    public String toString() {
      return "NonObservableId [str='" + str + "']";
    }
  }

  /**
   * Market data function that builds a piece of non-observable market data (a string).
   */
  private static final class NonObservableMarketDataFunction implements MarketDataFunction<String, NonObservableId> {

    @Override
    public MarketDataRequirements requirements(NonObservableId id, MarketDataConfig marketDataConfig) {
      return MarketDataRequirements.builder()
          .addValues(TestObservableId.of(StandardId.of("reqs", id.str)))
          .build();
    }

    @Override
    public MarketDataBox<String> build(
        NonObservableId id,
        MarketDataConfig marketDataConfig,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      MarketDataBox<Double> value = marketData.getValue(TestObservableId.of(StandardId.of("reqs", id.str)));
      return value.map(v -> Double.toString(v));
    }

    @Override
    public Class<NonObservableId> getMarketDataIdType() {
      return NonObservableId.class;
    }
  }

  /**
   * A perturbation which perturbs a string by appending another string to it.
   */
  private static final class StringAppender implements ScenarioPerturbation<String> {

    private final String[] str;

    public StringAppender(String... str) {
      this.str = str;
    }

    @Override
    public MarketDataBox<String> applyTo(MarketDataBox<String> marketData, ReferenceData refData) {
      return marketData.mapWithIndex(getScenarioCount(), (value, scenarioIndex) -> value + str[scenarioIndex]);
    }

    @Override
    public int getScenarioCount() {
      return str.length;
    }
  }

  /**
   * Perturbation that perturbs TestMarketDataC by scaling its time series.
   */
  private static final class TestCPerturbation implements ScenarioPerturbation<TestMarketDataC> {

    private final double[] scaleFactors;

    private TestCPerturbation(double... scaleFactors) {
      this.scaleFactors = scaleFactors;
    }

    @Override
    public MarketDataBox<TestMarketDataC> applyTo(MarketDataBox<TestMarketDataC> marketData, ReferenceData refData) {
      return marketData.mapWithIndex(getScenarioCount(), this::perturb);
    }

    private TestMarketDataC perturb(TestMarketDataC data, int scenarioIndex) {
      LocalDateDoubleTimeSeries perturbedTimeSeries = data.timeSeries.mapValues(v -> v * scaleFactors[scenarioIndex]);
      return new TestMarketDataC(perturbedTimeSeries);
    }

    @Override
    public int getScenarioCount() {
      return scaleFactors.length;
    }
  }
}
