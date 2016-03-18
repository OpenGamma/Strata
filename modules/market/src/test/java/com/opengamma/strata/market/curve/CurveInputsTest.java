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
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Test {@link CurveInputs}.
 */
@Test
public class CurveInputsTest {

  private static final Map<MarketDataKey<?>, Object> DATA_MAP =
      ImmutableMap.of(QuoteKey.of(StandardId.of("OG", "Ticker")), 6d);
  private static final Map<MarketDataKey<?>, Object> DATA_MAP2 =
      ImmutableMap.of(QuoteKey.of(StandardId.of("OG", "Ticker")), 7d);
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
