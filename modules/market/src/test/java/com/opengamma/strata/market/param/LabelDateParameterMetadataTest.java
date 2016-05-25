/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link LabelDateParameterMetadata}.
 */
@Test
public class LabelDateParameterMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);

  //-------------------------------------------------------------------------
  public void test_of() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE, "Label");
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getLabel(), "Label");
    assertEquals(test.getIdentifier(), "Label");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE, "Label");
    coverImmutableBean(test);
    LabelDateParameterMetadata test2 = LabelDateParameterMetadata.of(date(2014, 1, 1), "Label2");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE, "Label");
    assertSerialization(test);
  }

}
