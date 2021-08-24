/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.collect.Guavate.entry;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link MarketData} and {@link ImmutableMarketData}.
 */
public class MarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestingNamedId ID1 = new TestingNamedId("1");
  private static final TestingNamedId ID2 = new TestingNamedId("2");
  private static final TestingNamedId ID3 = new TestingNamedId("3");
  private static final TestingObservableId ID4 = new TestingObservableId("4");
  private static final TestingObservableId ID5 = new TestingObservableId("5");
  private static final String VAL1 = "1";
  private static final String VAL2 = "2";
  private static final String VAL3 = "3";
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_of_2arg() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    MarketData test = MarketData.of(VAL_DATE, dataMap);

    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));

    assertThat(test.containsValue(ID2)).isEqualTo(true);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.findValue(ID2)).isEqualTo(Optional.of(VAL2));

    assertThat(test.containsValue(ID3)).isEqualTo(false);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID3));
    assertThat(test.findValue(ID3)).isEqualTo(Optional.empty());

    assertThat(test.getIds()).containsExactly(ID1, ID2);

    assertThat(test.findIds(ID1.getMarketDataName())).isEqualTo(ImmutableSet.of(ID1));
    assertThat(test.findIds(new TestingName("Foo"))).isEqualTo(ImmutableSet.of());

    assertThat(test.getTimeSeries(ID4)).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(test.getTimeSeries(ID5)).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  @Test
  public void test_of_3arg() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID5, TIME_SERIES);
    MarketData test = MarketData.of(VAL_DATE, dataMap, tsMap);

    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));

    assertThat(test.containsValue(ID2)).isEqualTo(false);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID2));
    assertThat(test.findValue(ID2)).isEqualTo(Optional.empty());

    assertThat(test.getIds()).isEqualTo(ImmutableSet.of(ID1));

    assertThat(test.getTimeSeries(ID4)).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(test.getTimeSeries(ID5)).isEqualTo(TIME_SERIES);
  }

  @Test
  public void test_of_badType() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, 123d);
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> MarketData.of(VAL_DATE, dataMap));
  }

  @Test
  public void test_of_null() {
    Map<MarketDataId<?>, Object> dataMap = new HashMap<>();
    dataMap.put(ID1, null);
    assertThatIllegalArgumentException().isThrownBy(() -> MarketData.of(VAL_DATE, dataMap));
  }

  @Test
  public void empty() {
    MarketData test = MarketData.empty(VAL_DATE);

    assertThat(test.containsValue(ID1)).isEqualTo(false);
    assertThat(test.getIds()).isEqualTo(ImmutableSet.of());
    assertThat(test.getTimeSeries(ID4)).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ImmutableMarketData test = ImmutableMarketData.builder(VAL_DATE.plusDays(1))
        .valuationDate(VAL_DATE)
        .addValue(ID1, "123")
        .addValueUnsafe(ID2, "124")
        .addValueMap(ImmutableMap.of(ID3, "201"))
        .addTimeSeries(ID4, TIME_SERIES)
        .build();
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getValues().get(ID1)).isEqualTo("123");
    assertThat(test.getValues().get(ID2)).isEqualTo("124");
    assertThat(test.getIds()).containsExactlyInAnyOrder(ID1, ID2, ID3);
    assertThat(test.getTimeSeries().get(ID4)).isEqualTo(TIME_SERIES);
  }

  @Test
  public void test_builder_combine() {
    ImmutableMarketData base = ImmutableMarketData.builder(VAL_DATE).addValue(ID1, "123").build();
    ImmutableMarketData extra = ImmutableMarketData.builder(VAL_DATE).addValue(ID2, "456").build();
    ImmutableMarketData test = base.toBuilder().add(extra).build();
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getValues()).containsOnly(entry(ID1, "123"), entry(ID2, "456"));
    assertThat(test.getTimeSeries()).isEmpty();
  }

  @Test
  public void test_builder_removeIf() {
    ImmutableMarketData base = ImmutableMarketData.builder(VAL_DATE).addValue(ID1, "123").addValue(ID2, "456").build();
    ImmutableMarketData test = base.toBuilder()
        .removeValueIf(id -> id.equals(ID1))
        .build();
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getValues()).containsOnly(entry(ID2, "456"));
    assertThat(test.getTimeSeries()).isEmpty();
  }

  @Test
  public void test_builder_badType() {
    assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> ImmutableMarketData.builder(VAL_DATE).addValueUnsafe(ID1, 123d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_defaultMethods() {
    MarketData test = new MarketData() {

      @Override
      public LocalDate getValuationDate() {
        return VAL_DATE;
      }

      @Override
      public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
        return TIME_SERIES;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> Optional<T> findValue(MarketDataId<T> id) {
        return id.equals(ID1) ? Optional.of((T) VAL1) : Optional.empty();
      }

      @Override
      public Set<MarketDataId<?>> getIds() {
        return ImmutableSet.of();
      }

      @Override
      public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
        return ImmutableSet.of();
      }

      @Override
      public Set<ObservableId> getTimeSeriesIds() {
        return ImmutableSet.of();
      }
    };
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.containsValue(ID2)).isEqualTo(false);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID2));
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));
    assertThat(test.findValue(ID2)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_noClash() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);

    MarketData test = test1.combinedWith(test2);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.getIds()).containsExactlyInAnyOrder(ID1, ID2);
  }

  @Test
  public void test_combinedWith_noClashSame() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);

    MarketData test = test1.combinedWith(test2);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.getIds()).containsExactlyInAnyOrder(ID1, ID2);
  }

  @Test
  public void test_combinedWith_clash() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);
    MarketData combined = test1.combinedWith(test2);
    assertThat(combined.getValue(ID1)).isEqualTo(VAL1);
    assertThat(combined.getIds()).isEqualTo(ImmutableSet.of(ID1));
  }

  @Test
  public void test_combinedWith_dateMismatch() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    MarketData test2 = MarketData.of(VAL_DATE.plusDays(1), dataMap2);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combinedWith(test2));
  }

  /**
   * Tests the combinedWith method when the MarketData instances are not both ImmutableMarketData.
   */
  @Test
  public void test_combinedWith_differentTypes() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL1);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);
    ExtendedMarketData<String> test3 = ExtendedMarketData.of(ID1, VAL3, test2);

    MarketData test = test3.combinedWith(test1);
    assertThat(test.getValue(ID1)).isEqualTo(VAL3);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.getIds()).containsExactly(ID1, ID2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withValue() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    MarketData test = MarketData.of(VAL_DATE, dataMap).withValue(ID1, VAL3);
    assertThat(test.getValue(ID1)).isEqualTo(VAL3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ImmutableMarketData test = ImmutableMarketData.of(VAL_DATE, dataMap);
    coverImmutableBean(test);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableMarketData test2 = ImmutableMarketData.of(VAL_DATE.minusDays(1), dataMap2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    MarketData test = MarketData.of(VAL_DATE, dataMap);
    assertSerialization(test);
  }

}
