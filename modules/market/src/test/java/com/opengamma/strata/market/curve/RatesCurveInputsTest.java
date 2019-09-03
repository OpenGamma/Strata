/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@link RatesCurveInputs}.
 */
public class RatesCurveInputsTest {

  private static final Map<MarketDataId<?>, Object> DATA_MAP =
      ImmutableMap.of(QuoteId.of(StandardId.of("OG", "Ticker")), 6d);
  private static final Map<MarketDataId<?>, Object> DATA_MAP2 =
      ImmutableMap.of(QuoteId.of(StandardId.of("OG", "Ticker")), 7d);
  private static final CurveMetadata METADATA = DefaultCurveMetadata.of("Test");
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of("Test2");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    RatesCurveInputs test = RatesCurveInputs.of(DATA_MAP, METADATA);
    assertThat(test.getMarketData()).isEqualTo(DATA_MAP);
    assertThat(test.getCurveMetadata()).isEqualTo(METADATA);
  }

  @Test
  public void test_builder() {
    RatesCurveInputs test = RatesCurveInputs.builder().marketData(DATA_MAP).curveMetadata(METADATA).build();
    assertThat(test.getMarketData()).isEqualTo(DATA_MAP);
    assertThat(test.getCurveMetadata()).isEqualTo(METADATA);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RatesCurveInputs test = RatesCurveInputs.of(DATA_MAP, METADATA);
    coverImmutableBean(test);
    RatesCurveInputs test2 = RatesCurveInputs.of(DATA_MAP2, METADATA2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RatesCurveInputs test = RatesCurveInputs.of(DATA_MAP, METADATA);
    assertSerialization(test);
  }

}
