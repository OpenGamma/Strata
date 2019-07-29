/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Test {@link PropertySet}.
 */
public class PropertySetTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_empty() {
    PropertySet test = PropertySet.empty();

    assertThat(test.isEmpty()).isEqualTo(true);
    assertThat(test.contains("unknown")).isEqualTo(false);
    assertThat(test.valueList("unknown")).isEqualTo(ImmutableList.of());
    assertThatIllegalArgumentException().isThrownBy(() -> test.value("unknown"));
    assertThat(test.toString()).isEqualTo("{}");
  }

  @Test
  public void test_of_map() {
    Map<String, String> keyValues = ImmutableMap.of("a", "x", "b", "y");
    PropertySet test = PropertySet.of(keyValues);

    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.contains("a")).isEqualTo(true);
    assertThat(test.value("a")).isEqualTo("x");
    assertThat(test.valueList("a")).isEqualTo(ImmutableList.of("x"));
    assertThat(test.contains("b")).isEqualTo(true);
    assertThat(test.value("b")).isEqualTo("y");
    assertThat(test.valueList("b")).isEqualTo(ImmutableList.of("y"));
    assertThat(test.contains("c")).isEqualTo(false);
    assertThat(test.keys()).isEqualTo(ImmutableSet.of("a", "b"));
    assertThat(test.asMap()).isEqualTo(ImmutableMap.of("a", "x", "b", "y"));
    assertThat(test.asMultimap()).isEqualTo(ImmutableListMultimap.of("a", "x", "b", "y"));
    assertThat(test.valueList("unknown")).isEqualTo(ImmutableList.of());

    assertThatIllegalArgumentException().isThrownBy(() -> test.value("unknown"));
    assertThat(test.toString()).isEqualTo("{a=[x], b=[y]}");
  }

  @Test
  public void test_of_multimap() {
    Multimap<String, String> keyValues = ImmutableMultimap.of("a", "x", "a", "y", "b", "z");
    PropertySet test = PropertySet.of(keyValues);

    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.contains("a")).isEqualTo(true);
    assertThatIllegalArgumentException().isThrownBy(() -> test.value("a"));
    assertThat(test.valueList("a")).isEqualTo(ImmutableList.of("x", "y"));
    assertThat(test.contains("b")).isEqualTo(true);
    assertThat(test.value("b")).isEqualTo("z");
    assertThat(test.valueList("b")).isEqualTo(ImmutableList.of("z"));
    assertThat(test.contains("c")).isEqualTo(false);
    assertThat(test.keys()).isEqualTo(ImmutableSet.of("a", "b"));
    assertThat(test.asMultimap()).isEqualTo(ImmutableListMultimap.of("a", "x", "a", "y", "b", "z"));
    assertThat(test.valueList("unknown")).isEqualTo(ImmutableList.of());

    assertThatIllegalArgumentException().isThrownBy(() -> test.asMap());
    assertThatIllegalArgumentException().isThrownBy(() -> test.value("unknown"));
    assertThat(test.toString()).isEqualTo("{a=[x, y], b=[z]}");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "c", "z"));
    PropertySet other = PropertySet.of(ImmutableListMultimap.of("a", "aa", "b", "bb", "d", "dd"));
    PropertySet expected = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "c", "z", "b", "bb", "d", "dd"));
    assertThat(base.combinedWith(other)).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_emptyBase() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertThat(base.combinedWith(PropertySet.empty())).isEqualTo(base);
  }

  @Test
  public void test_combinedWith_emptyOther() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertThat(PropertySet.empty().combinedWith(base)).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_overrideWith() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    PropertySet other = PropertySet.of(ImmutableListMultimap.of("a", "aa", "c", "cc", "d", "dd", "e", "ee"));
    PropertySet expected =
        PropertySet.of(ImmutableListMultimap.of("a", "aa", "b", "y", "c", "cc", "d", "dd", "e", "ee"));
    assertThat(base.overrideWith(other)).isEqualTo(expected);
  }

  @Test
  public void test_overrideWith_emptyBase() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertThat(base.overrideWith(PropertySet.empty())).isEqualTo(base);
  }

  @Test
  public void test_overrideWith_emptyOther() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertThat(PropertySet.empty().overrideWith(base)).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    Map<String, String> keyValues = ImmutableMap.of("a", "x", "b", "y");
    PropertySet a1 = PropertySet.of(keyValues);
    PropertySet a2 = PropertySet.of(keyValues);
    PropertySet b = PropertySet.of(ImmutableMap.of("a", "x", "b", "z"));

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

}
