package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.builders.DiscountingCurveMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.IndexCurveMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.TimeSeriesProvider;
import com.opengamma.strata.marketdata.curve.CurveGroup;
import com.opengamma.strata.marketdata.id.CurveGroupId;
import com.opengamma.strata.marketdata.id.CurveId;
import com.opengamma.strata.marketdata.id.DiscountingCurveId;
import com.opengamma.strata.marketdata.id.IndexCurveId;
import com.opengamma.strata.marketdata.id.IndexRateId;
import com.opengamma.strata.marketdata.id.ObservableId;

@Test
public class DefaultMarketDataFactoryTest {

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
    DefaultMarketDataFactory marketDataFactory = new DefaultMarketDataFactory(new TestTimeSeriesProvider(timeSeries));
    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .timeSeriesRequirements(chfId, eurId)
            .build();
    BaseMarketData marketData =
        marketDataFactory.buildBaseMarketData(requirements, BaseMarketData.empty(date(2015, 3, 25))).getMarketData();

    assertThat(marketData.getTimeSeries(chfId)).isEqualTo(chfTimeSeries);
    assertThat(marketData.getTimeSeries(eurId)).isEqualTo(eurTimeSeries);
  }

  /**
   * Tests building single values using market data builders.
   */
  public void buildSingleValues() {
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
            new DiscountingCurveMarketDataBuilder(),
            new IndexCurveMarketDataBuilder());

    ImmutableSet<? extends CurveId> curveReqs = ImmutableSet.of(discountingCurveId, iborCurveId);
    MarketDataRequirements requirements = MarketDataRequirements.builder().singleValueRequirements(curveReqs).build();
    MarketDataResult result = marketDataFactory.buildBaseMarketData(requirements, suppliedData);
    BaseMarketData marketData = result.getMarketData();
    assertThat(marketData.getValue(discountingCurveId)).isEqualTo(discountingCurve);
    assertThat(marketData.getValue(iborCurveId)).isEqualTo(iborCurve);
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
}
