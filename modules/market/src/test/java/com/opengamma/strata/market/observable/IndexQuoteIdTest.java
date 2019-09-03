/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.observable;

import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link IndexQuoteId}.
 */
public class IndexQuoteIdTest {

  private static final FieldName FIELD = FieldName.of("Field");
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_1arg() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getFieldName()).isEqualTo(FieldName.MARKET_VALUE);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-Index", GBP_SONIA.getName()));
    assertThat(test.getMarketDataType()).isEqualTo(Double.class);
    assertThat(test.toString()).isEqualTo("IndexQuoteId:GBP-SONIA/MarketValue");
  }

  @Test
  public void test_of_2args() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA, FIELD);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getFieldName()).isEqualTo(FIELD);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-Index", GBP_SONIA.getName()));
    assertThat(test.getMarketDataType()).isEqualTo(Double.class);
    assertThat(test.toString()).isEqualTo("IndexQuoteId:GBP-SONIA/Field");
  }

  @Test
  public void test_of_3args() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA, FIELD, OBS_SOURCE);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getFieldName()).isEqualTo(FIELD);
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-Index", GBP_SONIA.getName()));
    assertThat(test.getMarketDataType()).isEqualTo(Double.class);
    assertThat(test.toString()).isEqualTo("IndexQuoteId:GBP-SONIA/Field/Vendor");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA);
    coverImmutableBean(test);
    IndexQuoteId test2 = IndexQuoteId.of(USD_FED_FUND, FIELD, OBS_SOURCE);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA);
    assertSerialization(test);
  }

}
