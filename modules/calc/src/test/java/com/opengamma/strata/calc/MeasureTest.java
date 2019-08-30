/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.convert.StringConvert;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link Measure}.
 */
public class MeasureTest {

  @Test
  public void test_extendedEnum() {
    ImmutableMap<String, Measure> map = Measure.extendedEnum().lookupAll();
    assertThat(map).hasSize(0);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> Measure.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> Measure.of(null));
  }

  //-------------------------------------------------------------------------

  @Test
  public void test_isConvertible() {
    assertThat(StringConvert.INSTANCE.isConvertible(Measure.class)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableMeasure test = ImmutableMeasure.of("A");
    coverImmutableBean(test);
    ImmutableMeasure test2 = ImmutableMeasure.of("B", false);
    coverBeanEquals(test, test2);

    coverPrivateConstructor(MeasureHelper.class);
  }

}
