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
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.id.QuoteId;

/**
 * Test {@link ParRates}.
 */
@Test
public class ParRatesTest {

  private static final Map<ObservableId, Double> RATE_MAP =
      ImmutableMap.of(QuoteId.of(StandardId.of("OG", "Ticker")), 6d);
  private static final Map<ObservableId, Double> RATE_MAP2 =
      ImmutableMap.of(QuoteId.of(StandardId.of("OG", "Ticker")), 7d);
  private static final CurveMetadata METADATA = DefaultCurveMetadata.of("Test");
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of("Test2");

  //-------------------------------------------------------------------------
  public void test_of() {
    ParRates test = ParRates.of(RATE_MAP, METADATA);
    assertThat(test.getRates()).isEqualTo(RATE_MAP);
    assertThat(test.getCurveMetadata()).isEqualTo(METADATA);
  }

  public void test_builder() {
    ParRates test = ParRates.builder().rates(RATE_MAP).curveMetadata(METADATA).build();
    assertThat(test.getRates()).isEqualTo(RATE_MAP);
    assertThat(test.getCurveMetadata()).isEqualTo(METADATA);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParRates test = ParRates.of(RATE_MAP, METADATA);
    coverImmutableBean(test);
    ParRates test2 = ParRates.of(RATE_MAP2, METADATA2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ParRates test = ParRates.of(RATE_MAP, METADATA);
    assertSerialization(test);
  }

}
