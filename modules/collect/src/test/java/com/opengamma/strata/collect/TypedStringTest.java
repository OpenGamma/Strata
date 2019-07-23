/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test {@link TypedString}.
 */
public class TypedStringTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of() {
    SampleType test = SampleType.of("A");
    assertThat(test.toString()).isEqualTo("A");
  }

  @Test
  public void test_of_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> SampleType.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> SampleType.of(""));
  }

  @Test
  public void test_of_validated() {
    SampleValidatedType test = SampleValidatedType.of("ABC");
    assertThat(test.toString()).isEqualTo("ABC");
  }

  @Test
  public void test_of_validated_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> SampleValidatedType.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> SampleValidatedType.of("ABc"));
  }

  @Test
  public void test_equalsHashCode() {
    SampleType a1 = SampleType.of("A");
    SampleType a2 = SampleType.of("A");
    SampleType b = SampleType.of("B");

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_compareTo() {
    SampleType a = SampleType.of("A");
    SampleType b = SampleType.of("B");
    SampleType c = SampleType.of("C");
    List<SampleType> list = new ArrayList<>(Arrays.asList(a, b, c));
    list.sort(Comparator.naturalOrder());
    assertThat(list).containsExactly(a, b, c);
    list.sort(Comparator.reverseOrder());
    assertThat(list).containsExactly(c, b, a);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(SampleType.of("A"));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(SampleType.class, SampleType.of("A"));
  }

}
