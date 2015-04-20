package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;
import com.opengamma.strata.engine.marketdata.builders.DiscountingCurveMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.IndexCurveMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.ObservableMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.marketdata.curve.CurveGroup;
import com.opengamma.strata.marketdata.id.CurveGroupId;
import com.opengamma.strata.marketdata.id.DiscountingCurveId;
import com.opengamma.strata.marketdata.id.IndexCurveId;
import com.opengamma.strata.marketdata.id.IndexRateId;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.id.QuoteId;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;

@Test
public class DefaultMarketDataFactoryTest {

  private static final MarketDataFeed VENDOR = MarketDataFeed.of("RealFeed");
  private static final IndexRateId LIBOR_1M_ID = IndexRateId.of(IborIndices.USD_LIBOR_1M, VENDOR);
  private static final IndexRateId LIBOR_3M_ID = IndexRateId.of(IborIndices.USD_LIBOR_3M, VENDOR);

  /**
   * Tests building time series from requirements
   */
  public void buildTimeSeries() {
    IborIndex chfIndex = IborIndices.CHF_LIBOR_1M;
    IborIndex eurIndex = IborIndices.EUR_LIBOR_1M;
    IndexRateId chfId = IndexRateId.of(chfIndex);
    IndexRateId eurId = IndexRateId.of(eurIndex);
    LocalDateDoubleTimeSeries chfTimeSeries =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2011, 3, 8), 1)
            .put(date(2011, 3, 9), 2)
            .put(date(2011, 3, 10), 3)
            .build();
    LocalDateDoubleTimeSeries eurTimeSeries =
        LocalDateDoubleTimeSeries.builder()
            .put(date(2012, 4, 8), 10)
            .put(date(2012, 4, 9), 20)
            .put(date(2012, 4, 10), 30)
            .build();
    Map<IndexRateId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of(chfId, chfTimeSeries, eurId, eurTimeSeries);
    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(timeSeries),
            ObservableMarketDataBuilder.none(),
            FeedIdMapping.identity());

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .timeSeries(chfId, eurId)
            .build();
    BaseMarketData marketData =
        marketDataFactory.buildBaseMarketData(requirements, BaseMarketData.empty(date(2015, 3, 25))).getMarketData();

    assertThat(marketData.getTimeSeries(chfId)).isEqualTo(chfTimeSeries);
    assertThat(marketData.getTimeSeries(eurId)).isEqualTo(eurTimeSeries);
  }

  /**
   * Tests building single values using market data builders.
   */
  public void buildNonObservableValues() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve discountingCurve = MarketDataTestUtils.discountingCurve(1, Currency.AUD, curveGroup);
    YieldCurve iborCurve = MarketDataTestUtils.iborIndexCurve(1, IborIndices.EUR_EURIBOR_12M, curveGroup);
    DiscountingCurveId discountingCurveId = DiscountingCurveId.of(Currency.AUD, MarketDataTestUtils.CURVE_GROUP_NAME);
    IndexCurveId iborCurveId = IndexCurveId.of(IborIndices.EUR_EURIBOR_12M, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData suppliedData = BaseMarketData.builder(date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            ObservableMarketDataBuilder.none(),
            FeedIdMapping.identity(),
            new DiscountingCurveMarketDataBuilder(),
            new IndexCurveMarketDataBuilder());

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .values(discountingCurveId, iborCurveId)
            .build();
    MarketDataResult result = marketDataFactory.buildBaseMarketData(requirements, suppliedData);
    BaseMarketData marketData = result.getMarketData();
    assertThat(marketData.getValue(discountingCurveId)).isEqualTo(discountingCurve);
    assertThat(marketData.getValue(iborCurveId)).isEqualTo(iborCurve);
  }

  /**
   * Tests building observable market data values.
   */
  public void buildObservableValues() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataBuilder(),
            new TestFeedIdMapping());

    BaseMarketData suppliedData = BaseMarketData.empty(date(2011, 3, 8));
    QuoteId id1 = QuoteId.of(StandardId.of("reqs", "a"));
    QuoteId id2 = QuoteId.of(StandardId.of("reqs", "b"));
    MarketDataRequirements requirements = MarketDataRequirements.builder().values(id1, id2).build();
    BaseMarketData marketData = factory.buildBaseMarketData(requirements, suppliedData).getMarketData();

    assertThat(marketData.getValue(id1)).isEqualTo(1d);
    assertThat(marketData.getValue(id2)).isEqualTo(2d);
  }

  /**
   * Tests that failures are included in the results for keys with no mapping
   */
  public void missingMapping() {
    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataBuilder(),
            new TestFeedIdMapping());

    DiscountingCurveKey key = DiscountingCurveKey.of(Currency.GBP);
    MissingMappingId missingId = MissingMappingId.of(key);
    MarketDataRequirements requirements = MarketDataRequirements.builder().values(missingId).build();
    MarketDataResult result = factory.buildBaseMarketData(requirements, BaseMarketData.empty(date(2011, 3, 8)));
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
    IndexRateId libor6mId = IndexRateId.of(IborIndices.USD_LIBOR_6M, MarketDataFeed.NO_RULE);
    IndexRateId libor12mId = IndexRateId.of(IborIndices.USD_LIBOR_12M, MarketDataFeed.NO_RULE);

    Set<IndexRateId> requirements = ImmutableSet.of(libor6mId, libor12mId, LIBOR_1M_ID, LIBOR_3M_ID);

    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new DelegateBuilder(),
            Optional::of);

    MarketDataRequirements marketDataRequirements = MarketDataRequirements.builder().values(requirements).build();
    MarketDataResult result =
        factory.buildBaseMarketData(marketDataRequirements, BaseMarketData.empty(date(2011, 3, 8)));
    Map<MarketDataId<?>, Result<?>> failures = result.getSingleValueFailures();
    BaseMarketData marketData = result.getMarketData();

    assertThat(marketData.getValue(LIBOR_1M_ID)).isEqualTo(1d);
    assertThat(marketData.getValue(LIBOR_3M_ID)).isEqualTo(3d);
    assertThat(failures.get(libor6mId)).hasFailureMessageMatching("No market data rule.*");
    assertThat(failures.get(libor12mId)).hasFailureMessageMatching("No market data rule.*");
  }

  /**
   * Tests that failures are included in the results for non-observable market data when there is no matching
   * market data rule for a calculation.
   */
  public void noMatchingMarketDataRuleNonObservables() {
    DiscountingCurveKey gbpCurveKey = DiscountingCurveKey.of(Currency.GBP);
    NoMatchingRuleId gbpCurveId = NoMatchingRuleId.of(gbpCurveKey);
    DiscountingCurveId eurCurveId = DiscountingCurveId.of(Currency.EUR, "curve group");

    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(ImmutableMap.of()),
            new TestObservableMarketDataBuilder(),
            Optional::of,
            new CurveBuilder());

    MarketDataRequirements requirements = MarketDataRequirements.builder().values(eurCurveId, gbpCurveId).build();
    MarketDataResult result = factory.buildBaseMarketData(requirements, BaseMarketData.empty(date(2011, 3, 8)));
    Map<MarketDataId<?>, Result<?>> singleValueFailures = result.getSingleValueFailures();
    BaseMarketData marketData = result.getMarketData();

    assertThat(singleValueFailures.get(gbpCurveId)).hasFailureMessageMatching("No market data rule.*");
    assertThat(marketData.getValue(eurCurveId)).isNotNull();
  }

  /**
   * Tests that failures are included in the results for time series when there is no matching
   * market data rule for a calculation.
   */
  public void noMatchingMarketDataRuleTimeSeries() {
    IndexRateId libor6mId = IndexRateId.of(IborIndices.USD_LIBOR_6M, MarketDataFeed.NO_RULE);
    IndexRateId libor12mId = IndexRateId.of(IborIndices.USD_LIBOR_12M, MarketDataFeed.NO_RULE);
    Set<IndexRateId> requirements = ImmutableSet.of(libor6mId, libor12mId, LIBOR_1M_ID, LIBOR_3M_ID);

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

    Map<IndexRateId, LocalDateDoubleTimeSeries> timeSeriesMap =
        ImmutableMap.of(
            LIBOR_1M_ID, libor1mTimeSeries,
            LIBOR_3M_ID, libor3mTimeSeries);

    DefaultMarketDataFactory factory =
        new DefaultMarketDataFactory(
            new TestTimeSeriesProvider(timeSeriesMap),
            new DelegateBuilder(),
            Optional::of);

    MarketDataRequirements marketDataRequirements = MarketDataRequirements.builder().timeSeries(requirements).build();
    MarketDataResult result =
        factory.buildBaseMarketData(marketDataRequirements, BaseMarketData.empty(date(2011, 3, 8)));
    Map<MarketDataId<?>, Result<?>> failures = result.getTimeSeriesFailures();
    BaseMarketData marketData = result.getMarketData();

    assertThat(marketData.getTimeSeries(LIBOR_1M_ID)).isEqualTo(libor1mTimeSeries);
    assertThat(marketData.getTimeSeries(LIBOR_3M_ID)).isEqualTo(libor3mTimeSeries);
    assertThat(failures.get(libor6mId)).hasFailureMessageMatching("No market data rule.*");
    assertThat(failures.get(libor12mId)).hasFailureMessageMatching("No market data rule.*");
  }


  private static final class DelegateBuilder implements ObservableMarketDataBuilder {

    private final Map<ObservableId, Result<Double>> marketData =
        ImmutableMap.of(
            LIBOR_1M_ID, Result.success(1d),
            LIBOR_3M_ID, Result.success(3d));

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

    private TestTimeSeriesProvider(Map<IndexRateId, LocalDateDoubleTimeSeries> timeSeries) {
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
  private static final class TestObservableMarketDataBuilder implements ObservableMarketDataBuilder {

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
            QuoteId.of(StandardId.of("reqs", "a")), QuoteId.of(StandardId.of("vendor", "1")),
            QuoteId.of(StandardId.of("reqs", "b")), QuoteId.of(StandardId.of("vendor", "2")));

    @Override
    public Optional<ObservableId> idForFeed(ObservableId id) {
      return Optional.ofNullable(idMap.get(id));
    }
  }

  private static final class CurveBuilder implements MarketDataBuilder<YieldCurve, DiscountingCurveId> {

    @Override
    public MarketDataRequirements requirements(DiscountingCurveId id) {
      return MarketDataRequirements.EMPTY;
    }

    @Override
    public Map<DiscountingCurveId, Result<YieldCurve>> build(
        Set<DiscountingCurveId> requirements,
        BaseMarketData builtData) {

      DiscountingCurveId curveId = DiscountingCurveId.of(Currency.EUR, "curve group");
      if (requirements.size() == 1 && requirements.iterator().next().equals(curveId)) {
        YieldCurve yieldCurve = mock(YieldCurve.class);
        return ImmutableMap.of(curveId, Result.success(yieldCurve));
      } else {
        throw new IllegalArgumentException("Unexpected requirements " + requirements);
      }
    }

    @Override
    public Class<DiscountingCurveId> getMarketDataIdType() {
      return DiscountingCurveId.class;
    }
  }
}
