/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.Guavate.genericClass;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.tuple.IntDoublePair;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link SerializedValue}.
 */
public class SerializedValueTest {

  @Test
  public void test_null() {
    SerializedValue test = SerializedValue.serialize(String.class, null);
    assertThat(test.property("convert").get()).isNull();
    assertThat(test.property("bean").get()).isNull();
    assertThat(test.property("java").get()).isNull();
    assertThat(test.deserialize(String.class)).isNull();
  }

  @Test
  public void test_convert() {
    IntDoublePair pair = IntDoublePair.of(2, 3.4);
    SerializedValue test = SerializedValue.serialize(IntDoublePair.class, pair);
    assertThat(test.property("convert").get()).isEqualTo(pair.toString());
    assertThat(test.property("bean").get()).isNull();
    assertThat(test.property("java").get()).isNull();
    assertThat(test.deserialize(IntDoublePair.class)).isEqualTo(pair);
  }

  @Test
  public void test_bean() {
    Pair<String, Double> pair = Pair.of("2", 3.4);
    SerializedValue test = SerializedValue.serialize(Pair.class, pair);
    assertThat(test.property("convert").get()).isNull();
    assertThat(test.property("bean").get()).isNotNull();
    assertThat(test.property("java").get()).isNull();
    Class<Pair<String, Double>> cls = genericClass(Pair.class);
    assertThat(test.deserialize(cls)).isEqualTo(pair);
  }

  @Test
  public void test_java() {
    ImmutableList<String> pair = ImmutableList.of("2", "3.4");
    SerializedValue test = SerializedValue.serialize(ImmutableList.class, pair);
    assertThat(test.property("convert").get()).isNull();
    assertThat(test.property("bean").get()).isNull();
    assertThat(test.property("java").get()).isNotNull();
    Class<ImmutableList<String>> cls = genericClass(ImmutableList.class);
    assertThat(test.deserialize(cls)).isEqualTo(pair);
  }

  @Test
  public void test_java_bad() {
    SerializedValue test = SerializedValue.meta().builder().set("java", new byte[0]).build();
    assertThat(test.property("java").get()).isNotNull();
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> test.deserialize(String.class));
  }

  @Test
  public void test_unknown() {
    assertThatIllegalArgumentException().isThrownBy(() -> SerializedValue.serialize(SerializedValueTest.class, this));
  }

  @Test
  public void coverage() {
    SerializedValue test = SerializedValue.serialize(Pair.class, Pair.of("2", 3.4));
    SerializedValue test2 = SerializedValue.serialize(IntDoublePair.class, IntDoublePair.of(2, 3.4));
    coverImmutableBean(test);
    coverBeanEquals(test, test2);
  }

}
