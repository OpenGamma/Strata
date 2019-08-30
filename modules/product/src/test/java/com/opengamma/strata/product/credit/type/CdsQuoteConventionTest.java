/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link CdsQuoteConvention}.
 */
public class CdsQuoteConventionTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {CdsQuoteConvention.PAR_SPREAD, "ParSpread"},
        {CdsQuoteConvention.POINTS_UPFRONT, "PointsUpfront"},
        {CdsQuoteConvention.QUOTED_SPREAD, "QuotedSpread"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CdsQuoteConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CdsQuoteConvention convention, String name) {
    assertThat(CdsQuoteConvention.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CdsQuoteConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CdsQuoteConvention.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(CdsQuoteConvention.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(CdsQuoteConvention.POINTS_UPFRONT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CdsQuoteConvention.class, CdsQuoteConvention.POINTS_UPFRONT);
  }

}
