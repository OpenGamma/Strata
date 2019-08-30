/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link IborRateResetMethod}.
 */
public class IborRateResetMethodTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {IborRateResetMethod.WEIGHTED, "Weighted"},
        {IborRateResetMethod.UNWEIGHTED, "Unweighted"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(IborRateResetMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(IborRateResetMethod convention, String name) {
    assertThat(IborRateResetMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateResetMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateResetMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(IborRateResetMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(IborRateResetMethod.WEIGHTED);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(IborRateResetMethod.class, IborRateResetMethod.WEIGHTED);
  }

}
