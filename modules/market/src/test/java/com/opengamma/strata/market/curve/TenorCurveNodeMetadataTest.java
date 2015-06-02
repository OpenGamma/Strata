/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link TenorCurveNodeMetadata}.
 */
@Test
public class TenorCurveNodeMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);

  //-------------------------------------------------------------------------
  public void test_of() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(DATE, TENOR_10Y);
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getDescription()).isEqualTo("10Y");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(DATE, TENOR_10Y);
    coverImmutableBean(test);
    TenorCurveNodeMetadata test2 = TenorCurveNodeMetadata.of(date(2014, 1, 1), TENOR_12M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(DATE, TENOR_10Y);
    assertSerialization(test);
  }

  public void test_identifier() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(DATE, TENOR_10Y);
    assertThat(test.getIdentifier()).isEqualTo(TENOR_10Y);
  }
}
