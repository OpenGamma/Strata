package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.ShiftType;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.engine.marketdata.scenarios.MarketDataFilter;
import com.opengamma.strata.engine.marketdata.scenarios.Perturbation;
import com.opengamma.strata.engine.marketdata.scenarios.PerturbationMapping;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;

@Test
public class DefaultMarketDataFactoryTest {

  private static final MarketDataFeed VENDOR = MarketDataFeed.of("RealFeed");
  private static final TestObservableId ID1 = TestObservableId.of("1", VENDOR);
  private static final TestObservableId ID2 = TestObservableId.of("2", VENDOR);
  private static final MarketDataConfig MARKET_DATA_CONFIG = mock(MarketDataConfig.class);

  /**
   * Tests building time series from requirements.
   */
  public void buildTimeSeries() {
    TestObservableId id1 = TestObservableId.of("1");
    TestObservableId id2 = TestObservableId.of("2");
    LocalDateDoubleTimeSeries timeSeries1 =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 1)
            .put(date(2011, 3, 9), 2)
            .put(date(2011, 3, 10), 3)
            .build();
    LocalDateDoubleTimeSeries timeSeries2 =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2012, 4, 8), 10)
            .put(date(2012, 4, 9), 20)
            .put(date(2012, 4, 10), 30)
            .build();
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of(id1, timeSeries1, id2, timeSeries2);
    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(timeSeries),
            ObservableMarketDataFunction.none(),
            FeedIdMapping.identity());

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addTimeSeries(id1, id2)
            .build();
    BaseMarketDataResult result =
        marketDataFactory.buildBaseMarketData(
            requirements,
            BaseMarketData.empty(date(2015, 3, 25)),
            MARKET_DATA_CONFIG);
    BaseMarketData marketData = result.getMarketData();

    assertThat(marketData.getTimeSeries(id1)).isEqualTo(timeSeries1);
    assertThat(marketData.getTimeSeries(id2)).isEqualTo(timeSeries2);
  }

  /**
   * Tests non-observable market data values supplied by the user are included in the results.
   */
  public void buildSuppliedNonObservableValues() {
    TestId id1 = TestId.of("1");
    TestId id2 = TestId.of("2");
    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(id1, "foo")
            .addValue(id2, "bar")
            .build();
    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            FeedIdMapping.identity());
    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addValues(id1, id2)
            .build();
    BaseMarketDataResult result = marketDataFactory.buildBaseMarketData(requirements, suppliedData, MARKET_DATA_CONFIG);
    BaseMarketData marketData = result.getMarketData();
    assertThat(marketData.getValue(id1)).isEqualTo("foo");
    assertThat(marketData.getValue(id2)).isEqualTo("bar");
  }

  /**
   * Tests building single values using market data functions.
   */
  public void buildNonObservableValues() {
    ObservableId idA = new TestIdA("1");
    MarketDataId<?> idC = new TestIdC("1");
    LocalDateDoubleTimeSeries timeSeries =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2012, 4, 8), 10)
            .put(date(2012, 4, 9), 20)
            .put(date(2012, 4, 10), 30)
            .build();

    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addTimeSeries(idA, timeSeries)
            .build();
    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            FeedIdMapping.identity(),
            new TestMarketDataFunctionC());

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addValues(idC)
            .build();
    BaseMarketDataResult result = marketDataFactory.buildBaseMarketData(requirements, suppliedData, MARKET_DATA_CONFIG);
    BaseMarketData marketData = result.getMarketData();
    assertThat(marketData.getValue(idC)).isEqualTo(new TestMarketDataC(timeSeries));
  }

  /**
   * Tests building observable market data values.
   */
  public void buildObservableValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping());

    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    BaseMarketDataResult result = factory.buildBaseMarketData(requirements, suppliedData, MARKET_DATA_CONFIG);
    BaseMarketData marketData = result.getMarketData();

    assertThat(marketData.getValue(id1)).isEqualTo(1d);
    assertThat(marketData.getValue(id2)).isEqualTo(2d);
  }

  /**
   * Tests observable market data values supplied by the user are included in the results.
   */
  public void buildSuppliedObservableValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            new TestFeedIdMapping());

    TestObservableId id1 = TestObservableId.of("a");
    TestObservableId id2 = TestObservableId.of("b");

    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(id1, 1d)
            .addValue(id2, 2d)
            .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    BaseMarketDataResult result = factory.buildBaseMarketData(requirements, suppliedData, MARKET_DATA_CONFIG);
    BaseMarketData marketData = result.getMarketData();

    assertThat(marketData.getValue(id1)).isEqualTo(1d);
    assertThat(marketData.getValue(id2)).isEqualTo(2d);
  }

  /**
   * Tests that failures are included in the results for keys with no mapping.
   */
  public void missingMapping() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping());

    TestObservableKey key = TestObservableKey.of("1");
    MissingMappingId missingId = MissingMappingId.of(key);
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(missingId).build();
    BaseMarketDataResult result =
        factory.buildBaseMarketData(requirements, BaseMarketData.empty(date(2011, 3, 8)), MARKET_DATA_CONFIG);
    Map<MarketDataId<?>, Result<?>> failures = result.getSingleValueFailures();
    Result<?> missingResult = failures.get(missingId);

    assertThat(missingResult).isFailure();
    String message = Messages.format("No market data mapping found for market data key {}", key);
    assertThat(missingResult.getFailure().getMessage()).isEqualTo(message);
  }

  /**
   * Tests that failures are included in the results for observable market data when there is no
   * matching market data rule for a calculation.
   */
  public void noMatchingMarketDataRuleObservables() {
    TestObservableId id3 = TestObservableId.of("3", MarketDataFeed.NO_RULE);
    TestObservableId id4 = TestObservableId.of("4", MarketDataFeed.NO_RULE);

    Set<ObservableId> requirements = ImmutableSet.of(id3, id4, ID1, ID2);

    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableFunction(),
            Optional::of);

    MarketDataRequirements marketDataRequirements = MarketDataRequirements.builder().addValues(requirements).build();
    BaseMarketDataResult result =
        factory.buildBaseMarketData(marketDataRequirements, BaseMarketData.empty(date(2011, 3, 8)), MARKET_DATA_CONFIG);
    Map<MarketDataId<?>, Result<?>> failures = result.getSingleValueFailures();
    BaseMarketData marketData = result.getMarketData();

    assertThat(failures.get(id3)).hasFailureMessageMatching("No market data rule.*");
    assertThat(failures.get(id4)).hasFailureMessageMatching("No market data rule.*");
    assertThat(marketData.getValue(ID1)).isEqualTo(1d);
    assertThat(marketData.getValue(ID2)).isEqualTo(3d);
  }

  /**
   * Tests that failures are included in the results for non-observable market data when there is no matching
   * market data rule for a calculation.
   */
  public void noMatchingMarketDataRuleNonObservables() {
    TestKey key1 = TestKey.of("1");
    NoMatchingRuleId id1 = NoMatchingRuleId.of(key1);

    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            Optional::of);

    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1).build();
    BaseMarketDataResult result =
        factory.buildBaseMarketData(requirements, BaseMarketData.empty(date(2011, 3, 8)), MARKET_DATA_CONFIG);
    Map<MarketDataId<?>, Result<?>> singleValueFailures = result.getSingleValueFailures();
    BaseMarketData marketData = result.getMarketData();

    assertThat(singleValueFailures.get(id1)).hasFailureMessageMatching("No market data rule.*");
  }

  /**
   * Tests that failures are included in the results for time series when there is no matching
   * market data rule for a calculation.
   */
  public void noMatchingMarketDataRuleTimeSeries() {
    TestObservableId id3 = TestObservableId.of("3", MarketDataFeed.NO_RULE);
    TestObservableId id4 = TestObservableId.of("4", MarketDataFeed.NO_RULE);
    Set<ObservableId> requirements = ImmutableSet.of(id3, id4, ID1, ID2);

    LocalDateDoubleTimeSeries libor1mTimeSeries =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 1d)
            .put(date(2011, 3, 9), 2d)
            .put(date(2011, 3, 10), 3d)
            .build();

    LocalDateDoubleTimeSeries libor3mTimeSeries =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2012, 3, 8), 10d)
            .put(date(2012, 3, 9), 20d)
            .put(date(2012, 3, 10), 30d)
            .build();

    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap =
        ImmutableMap.of(
            ID1, libor1mTimeSeries,
            ID2, libor3mTimeSeries);

    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(timeSeriesMap),
            new TestObservableFunction(),
            Optional::of);

    MarketDataRequirements marketDataRequirements = MarketDataRequirements.builder().addTimeSeries(requirements).build();
    BaseMarketDataResult result =
        factory.buildBaseMarketData(marketDataRequirements, BaseMarketData.empty(date(2011, 3, 8)), MARKET_DATA_CONFIG);
    Map<MarketDataId<?>, Result<?>> failures = result.getTimeSeriesFailures();
    BaseMarketData marketData = result.getMarketData();

    assertThat(marketData.getTimeSeries(ID1)).isEqualTo(libor1mTimeSeries);
    assertThat(marketData.getTimeSeries(ID2)).isEqualTo(libor3mTimeSeries);
    assertThat(failures.get(id3)).hasFailureMessageMatching("No market data rule.*");
    assertThat(failures.get(id4)).hasFailureMessageMatching("No market data rule.*");
  }

  /**
   * Tests building market data that depends on other market data.
   */
  public void buildDataFromOtherData() {
    TestMarketDataFunctionB builderB = new TestMarketDataFunctionB();
    TestMarketDataFunctionC builderC = new TestMarketDataFunctionC();

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
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

    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            timeSeriesProvider,
            new TestObservableMarketDataFunction(),
            FeedIdMapping.identity(),
            builderB,
            builderC);

    BaseMarketDataResult result =
        marketDataFactory.buildBaseMarketData(
            requirements,
            BaseMarketData.empty(date(2011, 3, 8)),
            MARKET_DATA_CONFIG);

    assertThat(result.getSingleValueFailures()).isEmpty();
    assertThat(result.getTimeSeriesFailures()).isEmpty();

    BaseMarketData marketData = result.getMarketData();
    TestMarketDataB marketDataB1 = marketData.getValue(new TestIdB("1"));
    TestMarketDataB marketDataB2 = marketData.getValue(new TestIdB("2"));

    TestMarketDataB expectedB1 = new TestMarketDataB(1, new TestMarketDataC(timeSeries1));
    TestMarketDataB expectedB2 = new TestMarketDataB(2, new TestMarketDataC(timeSeries2));

    assertThat(marketDataB1).isEqualTo(expectedB1);
    assertThat(marketDataB2).isEqualTo(expectedB2);
  }

  /**
   * Tests failures when there is no builder for an ID type.
   */
  public void noMarketDataBuilderAvailable() {
    TestIdB idB1 = new TestIdB("1");
    TestIdB idB2 = new TestIdB("2");
    TestIdC idC1 = new TestIdC("1");
    TestIdC idC2 = new TestIdC("2");
    TestMarketDataFunctionB builder = new TestMarketDataFunctionB();

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addValues(idB1, idB2)
            .build();

    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            FeedIdMapping.identity(),
            builder);

    BaseMarketDataResult result =
        marketDataFactory.buildBaseMarketData(
            requirements,
            BaseMarketData.empty(date(2011, 3, 8)),
            MARKET_DATA_CONFIG);
    Map<MarketDataId<?>, Result<?>> failures = result.getSingleValueFailures();

    Result<?> failureB1 = failures.get(idB1);
    Result<?> failureB2 = failures.get(idB2);
    Result<?> failureC1 = failures.get(idC1);
    Result<?> failureC2 = failures.get(idC2);

    assertThat(failureB1).hasFailureMessageMatching("No value for.*");
    assertThat(failureB2).hasFailureMessageMatching("No value for.*");
    assertThat(failureC1).hasFailureMessageMatching("No market data function available to handle.*");
    assertThat(failureC2).hasFailureMessageMatching("No market data function available to handle.*");
  }

  /**
   * Tests building multiple observable values for scenarios where the values aren't perturbed.
   */
  public void buildObservableScenarioValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping());

    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new FalseFilter<>(TestObservableId.class),
            new DoubleShift(ShiftType.ABSOLUTE, 1),
            new DoubleShift(ShiftType.ABSOLUTE, 2),
            new DoubleShift(ShiftType.ABSOLUTE, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of(1d, 1d, 1d));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of(2d, 2d, 2d));
  }

  /**
   * Tests observable values supplied by the user are included in the results when they aren't perturbed
   */
  public void buildSuppliedObservableScenarioValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            new TestFeedIdMapping());
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(id1, 1d)
            .addValue(id2, 2d)
            .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new FalseFilter<>(TestObservableId.class),
            new DoubleShift(ShiftType.ABSOLUTE, 1),
            new DoubleShift(ShiftType.ABSOLUTE, 2),
            new DoubleShift(ShiftType.ABSOLUTE, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of(1d, 1d, 1d));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of(2d, 2d, 2d));
  }

  public void perturbObservableValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping());

    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new ExactIdFilter<>(id1),
            new DoubleShift(ShiftType.ABSOLUTE, 1),
            new DoubleShift(ShiftType.ABSOLUTE, 2),
            new DoubleShift(ShiftType.ABSOLUTE, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of(2d, 3d, 4d));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of(2d, 2d, 2d));
  }

  /**
   * Tests that observable data is only perturbed once, even if there are two applicable perturbation mappings.
   */
  public void observableDataOnlyPerturbedOnce() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping());

    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));
    TestObservableId id1 = TestObservableId.of(StandardId.of("reqs", "a"));
    TestObservableId id2 = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    PerturbationMapping<Double> mapping1 =
        PerturbationMapping.of(
            Double.class,
            new ExactIdFilter<>(id2),
            new DoubleShift(ShiftType.RELATIVE, 0.1),
            new DoubleShift(ShiftType.RELATIVE, 0.2),
            new DoubleShift(ShiftType.RELATIVE, 0.3));
    PerturbationMapping<Double> mapping2 =
        PerturbationMapping.of(
            Double.class,
            new ExactIdFilter<>(id2),
            new DoubleShift(ShiftType.ABSOLUTE, 1),
            new DoubleShift(ShiftType.ABSOLUTE, 2),
            new DoubleShift(ShiftType.ABSOLUTE, 3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping1, mapping2));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of(1d, 1d, 1d));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of(2.2d, 2.4d, 2.6d));
  }

  /**
   * Tests building multiple values of non-observable market data for multiple scenarios. The data isn't perturbed.
   */
  public void buildNonObservableScenarioValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping(),
            new NonObservableMarketDataFunction());
    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<String> mapping =
        PerturbationMapping.of(
            String.class,
            new FalseFilter<>(NonObservableId.class),
            new StringAppender(""),
            new StringAppender(""),
            new StringAppender(""));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of("1.0", "1.0", "1.0"));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of("2.0", "2.0", "2.0"));
  }

  /**
   * Tests non-observable values supplied by the user are included in the results when they aren't perturbed
   */
  public void buildSuppliedNonObservableScenarioValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            new TestFeedIdMapping());
    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(id1, "value1")
            .addValue(id2, "value2")
            .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();
    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<String> mapping =
        PerturbationMapping.of(
            String.class,
            new FalseFilter<>(NonObservableId.class),
            new StringAppender(""),
            new StringAppender(""),
            new StringAppender(""));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of("value1", "value1", "value1"));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of("value2", "value2", "value2"));
  }

  /**
   * Tests that perturbations are applied to non-observable market data.
   */
  public void perturbNonObservableValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping(),
            new NonObservableMarketDataFunction());
    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    PerturbationMapping<String> mapping =
        PerturbationMapping.of(
            String.class,
            new ExactIdFilter<>(id1),
            new StringAppender("foo"),
            new StringAppender("bar"),
            new StringAppender("baz"));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of("1.0foo", "1.0bar", "1.0baz"));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of("2.0", "2.0", "2.0"));
  }

  /**
   * Tests that non-observable data is only perturbed once, even if there are two applicable perturbation mappings.
   */
  public void nonObservableDataOnlyPerturbedOnce() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping(),
            new NonObservableMarketDataFunction());
    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    PerturbationMapping<String> mapping1 =
        PerturbationMapping.of(
            String.class,
            new ExactIdFilter<>(id1),
            new StringAppender("FOO"),
            new StringAppender("BAR"),
            new StringAppender("BAZ"));
    PerturbationMapping<String> mapping2 =
        PerturbationMapping.of(
            String.class,
            new ExactIdFilter<>(id1),
            new StringAppender("foo"),
            new StringAppender("bar"),
            new StringAppender("baz"));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping1, mapping2));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of("1.0FOO", "1.0BAR", "1.0BAZ"));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of("2.0", "2.0", "2.0"));
  }

  /**
   * Tests that observable data built from observable values see the effects of the perturbations.
   */
  public void nonObservableDataBuiltFromPerturbedObservableData() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping(),
            new NonObservableMarketDataFunction());
    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));

    MarketDataId<?> id1 = new NonObservableId("a");
    MarketDataId<?> id2 = new NonObservableId("b");
    TestObservableId quoteId = TestObservableId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new ExactIdFilter<>(quoteId),
            new DoubleShift(ShiftType.RELATIVE, 0.1),
            new DoubleShift(ShiftType.RELATIVE, 0.2),
            new DoubleShift(ShiftType.RELATIVE, 0.3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData marketData = result.getMarketData();

    assertThat(marketData.getValues(id1)).isEqualTo(ImmutableList.of("1.0", "1.0", "1.0"));
    assertThat(marketData.getValues(id2)).isEqualTo(ImmutableList.of("2.2", "2.4", "2.6"));
  }

  /**
   * Tests that a failure is returned when building observable market data for scenarios where there is no
   * market data function.
   */
  public void nonObservableScenarioDataWithMissingBuilder() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataFunction(),
            new TestFeedIdMapping());
    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));

    NonObservableId id1 = new NonObservableId("a");
    NonObservableId id2 = new NonObservableId("b");
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id1, id2).build();

    // This mapping doesn't perturb any data but it causes three scenarios to be built
    PerturbationMapping<String> mapping =
        PerturbationMapping.of(
            String.class,
            new FalseFilter<>(NonObservableId.class),
            new StringAppender(""),
            new StringAppender(""),
            new StringAppender(""));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    Map<MarketDataId<?>, Result<?>> failures = result.getSingleValueFailures();

    assertThat(failures.size()).isEqualTo(2);
    assertThat(failures.get(id1)).hasFailureMessageMatching("No market data function available.*");
    assertThat(failures.get(id2)).hasFailureMessageMatching("No market data function available.*");

    ScenarioMarketData marketData = result.getMarketData();
    assertThrows(() -> marketData.getValues(id1), IllegalArgumentException.class, "No values available for.*");
    assertThrows(() -> marketData.getValues(id2), IllegalArgumentException.class, "No values available for.*");
  }

  /**
   * Tests that perturbations are applied to observable data supplied by the user.
   */
  public void perturbSuppliedNonObservableData() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            FeedIdMapping.identity());
    NonObservableId id = new NonObservableId("a");
    PerturbationMapping<String> mapping =
        PerturbationMapping.of(
            String.class,
            new ExactIdFilter<>(id),
            new StringAppender("Foo"),
            new StringAppender("Bar"),
            new StringAppender("Baz"));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(id, "value")
            .build();
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id).build();
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData scenarioData = result.getMarketData();
    List<String> values = scenarioData.getValues(id);
    List<String> expectedValues = ImmutableList.of("valueFoo", "valueBar", "valueBaz");
    assertThat(values).isEqualTo(expectedValues);
  }

  /**
   * Tests that perturbations are applied to non-observable data supplied by the user.
   */
  public void perturbSuppliedObservableData() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataFunction.none(),
            FeedIdMapping.identity());
    TestObservableId id = TestObservableId.of(StandardId.of("reqs", "a"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(id).build();
    PerturbationMapping<Double> mapping =
        PerturbationMapping.of(
            Double.class,
            new ExactIdFilter<>(id),
            new DoubleShift(ShiftType.RELATIVE, 0.1),
            new DoubleShift(ShiftType.RELATIVE, 0.2),
            new DoubleShift(ShiftType.RELATIVE, 0.3));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(ImmutableList.of(mapping));
    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(id, 2d)
            .build();
    ScenarioMarketDataResult result =
        factory.buildScenarioMarketData(
            requirements,
            suppliedData,
            scenarioDefinition,
            MARKET_DATA_CONFIG);
    ScenarioMarketData scenarioData = result.getMarketData();
    List<Double> values = scenarioData.getValues(id);
    List<Double> expectedValues = ImmutableList.of(2.2, 2.4, 2.6);
    assertThat(values).isEqualTo(expectedValues);
  }

  //-----------------------------------------------------------------------------------------------------------

  private static final class TestObservableFunction implements ObservableMarketDataFunction {

    private final Map<ObservableId, Result<Double>> marketData =
        ImmutableMap.of(
            ID1, Result.success(1d),
            ID2, Result.success(3d));

    @Override
    public Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements) {
      return requirements.stream()
          .filter(marketData::containsKey)
          .collect(toImmutableMap(id -> id, marketData::get));
    }
  }

  /**
   * Simple time series provider backed by a map.
   */
  private static final class TestTimeSeriesProvider implements TimeSeriesProvider {

    private final Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries;

    private TestTimeSeriesProvider(Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
      this.timeSeries = timeSeries;
    }

    @Override
    public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
      LocalDateDoubleTimeSeries series = timeSeries.get(id);
      return Result.ofNullable(series, FailureReason.MISSING_DATA, "No time series found for ID {}", id);
    }
  }

  /**
   * Builds observable data by parsing the value of the standard ID.
   */
  private static final class TestObservableMarketDataFunction implements ObservableMarketDataFunction {

    @Override
    public Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements) {
      return requirements.stream().collect(toImmutableMap(id -> id, this::buildResult));
    }

    private Result<Double> buildResult(ObservableId id) {
      return Result.success(Double.parseDouble(id.getStandardId().getValue()));
    }
  }

  /**
   * Simple ID mapping backed by a map.
   */
  private static final class TestFeedIdMapping implements FeedIdMapping {

    private final Map<ObservableId, ObservableId> idMap =
        ImmutableMap.of(
            TestObservableId.of(StandardId.of("reqs", "a")), TestObservableId.of(StandardId.of("vendor", "1")),
            TestObservableId.of(StandardId.of("reqs", "b")), TestObservableId.of(StandardId.of("vendor", "2")));

    @Override
    public Optional<ObservableId> idForFeed(ObservableId id) {
      return Optional.ofNullable(idMap.get(id));
    }
  }

  //-----------------------------------------------------------------------------------------------------------

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
    public MarketDataFeed getMarketDataFeed() {
      return MarketDataFeed.NONE;
    }

    @Override
    public Class<Double> getMarketDataType() {
      return Double.class;
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
   */
  private final class TestMarketDataFunctionB implements MarketDataFunction<TestMarketDataB, TestIdB> {

    @Override
    public MarketDataRequirements requirements(TestIdB id) {
      return MarketDataRequirements.builder()
          .addValues(new TestIdA(id.str), new TestIdC(id.str))
          .build();
    }

    @Override
    public Result<TestMarketDataB> build(TestIdB id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
      TestIdA idA = new TestIdA(id.str);
      TestIdC idC = new TestIdC(id.str);

      if (!builtData.containsValue(idA)) {
        return Result.failure(FailureReason.MISSING_DATA, "No value for {}", idA);
      }
      if (!builtData.containsValue(idC)) {
        return Result.failure(FailureReason.MISSING_DATA, "No value for {}", idC);
      }
      Double value = builtData.getValue(idA);
      TestMarketDataC marketDataC = builtData.getValue(idC);
      return Result.success(new TestMarketDataB(value, marketDataC));
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
   */
  private static final class TestMarketDataFunctionC implements MarketDataFunction<TestMarketDataC, TestIdC> {

    @Override
    public MarketDataRequirements requirements(TestIdC id) {
      return MarketDataRequirements.builder()
          .addTimeSeries(new TestIdA(id.str))
          .build();
    }

    @Override
    public Result<TestMarketDataC> build(TestIdC id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
      LocalDateDoubleTimeSeries timeSeries = builtData.getTimeSeries(new TestIdA(id.str));
      return Result.success(new TestMarketDataC(timeSeries));
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
    public boolean apply(I marketDataId, T marketData) {
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
  private static final class DoubleShift implements Perturbation<Double> {

    private final ShiftType shiftType;

    private final double shiftAmount;

    private DoubleShift(ShiftType shiftType, double shiftAmount) {
      this.shiftType = shiftType;
      this.shiftAmount = shiftAmount;
    }

    @Override
    public Double apply(Double marketData) {
      return shiftType.applyShift(marketData, shiftAmount);
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
    public boolean apply(I marketDataId, T marketData) {
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
  }

  private static final class NonObservableMarketDataFunction implements MarketDataFunction<String, NonObservableId> {

    @Override
    public MarketDataRequirements requirements(NonObservableId id) {
      return MarketDataRequirements.builder()
          .addValues(TestObservableId.of(StandardId.of("reqs", id.str)))
          .build();
    }

    @Override
    public Result<String> build(NonObservableId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
      Double value = builtData.getValue(TestObservableId.of(StandardId.of("reqs", id.str)));
      return Result.success(Double.toString(value));
    }

    @Override
    public Class<NonObservableId> getMarketDataIdType() {
      return NonObservableId.class;
    }
  }

  private static final class StringAppender implements Perturbation<String> {

    private final String str;

    public StringAppender(String str) {
      this.str = str;
    }

    @Override
    public String apply(String marketData) {
      return marketData + str;
    }
  }
}
