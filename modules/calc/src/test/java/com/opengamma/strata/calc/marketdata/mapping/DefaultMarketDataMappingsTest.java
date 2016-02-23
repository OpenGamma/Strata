/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.TestId;
import com.opengamma.strata.calc.marketdata.TestKey;
import com.opengamma.strata.calc.marketdata.TestMapping;
import com.opengamma.strata.calc.marketdata.TestObservableId;
import com.opengamma.strata.calc.marketdata.TestObservableKey;
import com.opengamma.strata.calc.marketdata.TestSimpleId;
import com.opengamma.strata.calc.marketdata.TestSimpleKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

@Test
public class DefaultMarketDataMappingsTest {

  private MarketDataFeed marketDataFeed;
  private CalculationEnvironment calculationEnvironment;
  private TestKey testKey;
  private TestObservableKey testObservableKey;
  private TestSimpleKey testSimpleKey;
  private DefaultMarketDataMappings mappings;
  private LocalDateDoubleTimeSeries timeSeries;

  @BeforeMethod
  public void setUp() throws Exception {
    marketDataFeed = MarketDataFeed.of("testFeed");
    timeSeries = LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2011, 3, 8), 1)
        .put(LocalDate.of(2011, 3, 9), 2)
        .put(LocalDate.of(2011, 3, 10), 3)
        .build();
    calculationEnvironment = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(new TestId("1", marketDataFeed), "one")
        .addValue(new TestSimpleId("2", marketDataFeed), "two")
        .addValue(new TestObservableId("3", marketDataFeed), 3d)
        .addTimeSeries(new TestObservableId("3", marketDataFeed), timeSeries)
        .build();
    testKey = new TestKey("1");
    testSimpleKey = new TestSimpleKey("2");
    testObservableKey = new TestObservableKey("3");
    mappings = DefaultMarketDataMappings.of(marketDataFeed, new TestMapping("foo", marketDataFeed));
  }

  /**
   * Test that keys that implement SimpleMarketDataKey are converted to IDs without needing a mapping.
   */
  public void simpleMarketDataKey() {
    MarketDataMappings mappings = MarketDataMappings.of(marketDataFeed);
    MarketDataId<String> id = mappings.getIdForKey(new TestSimpleKey("1"));
    assertThat(id).isInstanceOf(TestSimpleId.class);
    assertThat(((TestSimpleId) id).getMarketDataFeed()).isEqualTo(marketDataFeed);
  }

  public void containsMapping() {
    assertThat(mappings.containsValue(testKey, calculationEnvironment)).isTrue();
    assertThat(mappings.containsValue(testSimpleKey, calculationEnvironment)).isTrue();
    assertThat(mappings.containsValue(testObservableKey, calculationEnvironment)).isTrue();
    assertThat(mappings.findValue(testKey, calculationEnvironment)).isPresent();
    assertThat(mappings.findValue(testSimpleKey, calculationEnvironment)).isPresent();
    assertThat(mappings.findValue(testObservableKey, calculationEnvironment)).isPresent();
  }

  public void containsMappingNoData() {
    assertThat(mappings.containsValue(testKey, CalculationEnvironment.empty())).isFalse();
    assertThat(mappings.containsValue(testSimpleKey, CalculationEnvironment.empty())).isFalse();
    assertThat(mappings.containsValue(testObservableKey, CalculationEnvironment.empty())).isFalse();
    assertThat(mappings.findValue(testKey, CalculationEnvironment.empty())).isEmpty();
    assertThat(mappings.findValue(testSimpleKey, CalculationEnvironment.empty())).isEmpty();
    assertThat(mappings.findValue(testObservableKey, CalculationEnvironment.empty())).isEmpty();
  }

  public void containsMappingNoMarketDataMapping() {
    MarketDataMappings testMappings = DefaultMarketDataMappings.of(marketDataFeed);
    assertThat(testMappings.containsValue(testKey, calculationEnvironment)).isFalse();
    assertThat(testMappings.findValue(testKey, calculationEnvironment)).isEmpty();
  }

  public void getValue() {
    assertThat(mappings.getValue(testKey, calculationEnvironment)).isEqualTo(MarketDataBox.ofSingleValue("one"));
    assertThat(mappings.getValue(testSimpleKey, calculationEnvironment)).isEqualTo(MarketDataBox.ofSingleValue("two"));
    assertThat(mappings.getValue(testObservableKey, calculationEnvironment)).isEqualTo(MarketDataBox.ofSingleValue(3d));
  }

  public void getValueNoData() {
    assertThrowsIllegalArg(() -> mappings.getValue(testKey, CalculationEnvironment.empty()));
    assertThrowsIllegalArg(() -> mappings.getValue(testSimpleKey, CalculationEnvironment.empty()));
    assertThrowsIllegalArg(() -> mappings.getValue(testObservableKey, CalculationEnvironment.empty()));
  }

  public void getTimeSeries() {
    assertThat(mappings.getTimeSeries(testObservableKey, calculationEnvironment)).isEqualTo(timeSeries);
  }

  public void getTimeSeriesNoData() {
    assertThat(mappings.getTimeSeries(testObservableKey, CalculationEnvironment.empty()).isEmpty()).isTrue();
  }

  public void coverage() {
    coverImmutableBean(mappings);
    coverBeanEquals(mappings, DefaultMarketDataMappings.of(MarketDataFeed.NONE));
  }
}
