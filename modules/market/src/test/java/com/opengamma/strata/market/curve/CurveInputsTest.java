/**
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

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@link CurveInputs}.
 */
@Test
public class CurveInputsTest {

  private static final Map<MarketDataId<?>, Object> DATA_MAP =
      ImmutableMap.of(QuoteId.of(StandardId.of("OG", "Ticker")), 6d);
  private static final Map<MarketDataId<?>, Object> DATA_MAP2 =
      ImmutableMap.of(QuoteId.of(StandardId.of("OG", "Ticker")), 7d);
  private static final CurveMetadata METADATA = DefaultCurveMetadata.of("Test");
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of("Test2");

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveInputs test = CurveInputs.of(DATA_MAP, METADATA);
    assertThat(test.getMarketData()).isEqualTo(DATA_MAP);
    assertThat(test.getCurveMetadata()).isEqualTo(METADATA);
  }

  public void test_builder() {
    CurveInputs test = CurveInputs.builder().marketData(DATA_MAP).curveMetadata(METADATA).build();
    assertThat(test.getMarketData()).isEqualTo(DATA_MAP);
    assertThat(test.getCurveMetadata()).isEqualTo(METADATA);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveInputs test = CurveInputs.of(DATA_MAP, METADATA);
    coverImmutableBean(test);
    CurveInputs test2 = CurveInputs.of(DATA_MAP2, METADATA2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveInputs test = CurveInputs.of(DATA_MAP, METADATA);
    assertSerialization(test);
  }

}
