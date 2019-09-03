/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Test {@link LabelDateParameterMetadata}.
 */
public class LabelDateParameterMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_1arg() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE);
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getLabel()).isEqualTo(DATE.toString());
    assertThat(test.getIdentifier()).isEqualTo(DATE.toString());
  }

  @Test
  public void test_of_2args() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE, "Label");
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getLabel()).isEqualTo("Label");
    assertThat(test.getIdentifier()).isEqualTo("Label");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE, "Label");
    coverImmutableBean(test);
    LabelDateParameterMetadata test2 = LabelDateParameterMetadata.of(date(2014, 1, 1), "Label2");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    LabelDateParameterMetadata test = LabelDateParameterMetadata.of(DATE, "Label");
    assertSerialization(test);
  }

}
