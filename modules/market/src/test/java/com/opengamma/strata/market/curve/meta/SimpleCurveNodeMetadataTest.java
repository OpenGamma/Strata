/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.meta;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link SimpleCurveNodeMetadata}.
 */
@Test
public class SimpleCurveNodeMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);

  //-------------------------------------------------------------------------
  public void test_of() {
    SimpleCurveNodeMetadata test = SimpleCurveNodeMetadata.of(DATE, "Label");
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getLabel()).isEqualTo("Label");
    assertThat(test.getIdentifier()).isEqualTo("Label");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleCurveNodeMetadata test = SimpleCurveNodeMetadata.of(DATE, "Label");
    coverImmutableBean(test);
    SimpleCurveNodeMetadata test2 = SimpleCurveNodeMetadata.of(date(2014, 1, 1), "Label2");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleCurveNodeMetadata test = SimpleCurveNodeMetadata.of(DATE, "Label");
    assertSerialization(test);
  }

}
