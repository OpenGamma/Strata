/**
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
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link IndexQuoteId}.
 */
@Test
public class IndexQuoteIdTest {

  private static final FieldName FIELD = FieldName.of("Field");
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_1arg() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getStandardId(), StandardId.of("OG-Index", GBP_SONIA.getName()));
    assertEquals(test.getMarketDataType(), Double.class);
  }

  public void test_of_2args() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA, FIELD);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getFieldName(), FIELD);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getStandardId(), StandardId.of("OG-Index", GBP_SONIA.getName()));
    assertEquals(test.getMarketDataType(), Double.class);
  }

  public void test_of_3args() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA, FIELD, OBS_SOURCE);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getFieldName(), FIELD);
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getStandardId(), StandardId.of("OG-Index", GBP_SONIA.getName()));
    assertEquals(test.getMarketDataType(), Double.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA);
    coverImmutableBean(test);
    IndexQuoteId test2 = IndexQuoteId.of(USD_FED_FUND, FIELD, OBS_SOURCE);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IndexQuoteId test = IndexQuoteId.of(GBP_SONIA);
    assertSerialization(test);
  }

}
