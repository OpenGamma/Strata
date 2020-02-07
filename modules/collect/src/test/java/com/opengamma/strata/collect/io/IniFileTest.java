/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link IniFile}.
 */
public class IniFileTest {

  private static final String INI1 = "" +
      "# comment\n" +
      "[section]\n" +
      "c = x\n" +
      "b = y\n" +
      "a = z\n" +
      "\n" +
      "; comment\n" +
      "[name]\n" +
      "a = m\n" +
      "b = n\n";
  private static final String INI2 = "" +
      "[section]\n" +
      "a = x\n" +
      "b = y\n";
  private static final String INI3 = "" +
      "[section]\n" +
      "a = x\n" +
      "a = y\n";
  private static final String INI4 = "" +
      "[section]\n" +
      "a=d= = x\n";
  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of_noLists() {
    IniFile test = IniFile.of(CharSource.wrap(INI1));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("c", "x", "b", "y", "a", "z");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("a", "m", "b", "n");
    assertThat(test.asMap())
        .hasSize(2)
        .containsEntry("section", PropertySet.of(keyValues1))
        .containsEntry("name", PropertySet.of(keyValues2));

    assertThat(test.contains("section")).isEqualTo(true);
    assertThat(test.section("section")).isEqualTo(PropertySet.of(keyValues1));
    assertThat(test.findSection("section")).hasValue(PropertySet.of(keyValues1));
    assertThat(test.section("section").contains("c")).isEqualTo(true);
    assertThat(test.section("section").value("c")).isEqualTo("x");
    assertThat(test.section("section").valueList("c")).isEqualTo(ImmutableList.of("x"));
    assertThat(test.section("section").contains("b")).isEqualTo(true);
    assertThat(test.section("section").value("b")).isEqualTo("y");
    assertThat(test.section("section").valueList("b")).isEqualTo(ImmutableList.of("y"));
    assertThat(test.section("section").contains("a")).isEqualTo(true);
    assertThat(test.section("section").value("a")).isEqualTo("z");
    assertThat(test.section("section").valueList("a")).isEqualTo(ImmutableList.of("z"));
    assertThat(test.section("section").contains("d")).isEqualTo(false);
    // order must be retained
    assertThat(ImmutableList.copyOf(test.section("section").keys())).isEqualTo(ImmutableList.of("c", "b", "a"));
    assertThat(test.section("section").asMultimap()).isEqualTo(ImmutableListMultimap.of("c", "x", "b", "y", "a", "z"));

    assertThat(test.contains("name")).isEqualTo(true);
    assertThat(test.section("name")).isEqualTo(PropertySet.of(keyValues2));
    assertThat(test.section("name").contains("a")).isEqualTo(true);
    assertThat(test.section("name").value("a")).isEqualTo("m");
    assertThat(test.section("name").valueList("a")).isEqualTo(ImmutableList.of("m"));
    assertThat(test.section("name").contains("b")).isEqualTo(true);
    assertThat(test.section("name").value("b")).isEqualTo("n");
    assertThat(test.section("name").valueList("b")).isEqualTo(ImmutableList.of("n"));
    assertThat(test.section("name").contains("c")).isEqualTo(false);
    assertThat(ImmutableList.copyOf(test.section("name").keys())).isEqualTo(ImmutableList.of("a", "b"));
    assertThat(test.section("name").asMultimap()).isEqualTo(ImmutableListMultimap.of("a", "m", "b", "n"));

    assertThat(test.contains("unknown")).isEqualTo(false);
    assertThat(test.findSection("unknown")).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> test.section("unknown"));
    assertThat(test.section("section").valueList("unknown")).isEqualTo(ImmutableList.of());
    assertThatIllegalArgumentException().isThrownBy(() -> test.section("section").value("unknown"));
    assertThat(test.toString()).isEqualTo("{section={c=[x], b=[y], a=[z]}, name={a=[m], b=[n]}}");
  }

  @Test
  public void test_of_list() {
    IniFile test = IniFile.of(CharSource.wrap(INI3));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "x", "a", "y");
    assertThat(test.asMap()).isEqualTo(ImmutableMap.of("section", PropertySet.of(keyValues1)));

    assertThat(test.section("section")).isEqualTo(PropertySet.of(keyValues1));
    assertThat(test.section("section").contains("a")).isEqualTo(true);
    assertThat(test.section("section").value("a")).isEqualTo("x,y");
    assertThat(test.section("section").valueList("a")).isEqualTo(ImmutableList.of("x", "y"));
    assertThat(test.section("section").contains("b")).isEqualTo(false);
    assertThat(test.section("section").keys()).isEqualTo(ImmutableSet.of("a"));
    assertThat(test.section("section").asMultimap()).isEqualTo(ImmutableListMultimap.of("a", "x", "a", "y"));
    assertThat(test.toString()).isEqualTo("{section={a=[x, y]}}");
  }

  @Test
  public void test_of_escaping() {
    IniFile test = IniFile.of(CharSource.wrap(INI4));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a=d=", "x");
    assertThat(test.asMap()).isEqualTo(ImmutableMap.of("section", PropertySet.of(keyValues1)));
  }

  @Test
  public void test_of_propertyNoEquals() {
    IniFile test = IniFile.of(CharSource.wrap("[section]\na\n"));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "");
    assertThat(test.asMap()).isEqualTo(ImmutableMap.of("section", PropertySet.of(keyValues1)));

    assertThat(test.section("section")).isEqualTo(PropertySet.of(keyValues1));
    assertThat(test.section("section").contains("a")).isEqualTo(true);
    assertThat(test.section("section").valueList("a")).isEqualTo(ImmutableList.of(""));
    assertThat(test.section("section").contains("b")).isEqualTo(false);
    assertThat(test.section("section").keys()).isEqualTo(ImmutableSet.of("a"));
    assertThat(test.section("section").asMultimap()).isEqualTo(ImmutableListMultimap.of("a", ""));
    assertThat(test.toString()).isEqualTo("{section={a=[]}}");
  }

  @Test
  public void test_of_invalid_propertyAtStart() {
    String invalid =
        "a = x\n";
    assertThatIllegalArgumentException().isThrownBy(() -> IniFile.of(CharSource.wrap(invalid)));
  }

  @Test
  public void test_of_invalid_badSection() {
    String invalid = "" +
        "[section\n" +
        "b\n";
    assertThatIllegalArgumentException().isThrownBy(() -> IniFile.of(CharSource.wrap(invalid)));
  }

  @Test
  public void test_of_invalid_duplicateSection() {
    String invalid = "" +
        "[section]\n" +
        "a = y\n" +
        "[section]\n" +
        "b = y\n";
    assertThatIllegalArgumentException().isThrownBy(() -> IniFile.of(CharSource.wrap(invalid)));
  }

  @Test
  public void test_of_invalid_emptyKey() {
    String invalid = "" +
        "[section]\n" +
        "= y\n";
    assertThatIllegalArgumentException().isThrownBy(() -> IniFile.of(CharSource.wrap(invalid)));
  }

  @Test
  public void test_of_ioException() {
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
        () -> IniFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8)));
  }

  @Test
  public void test_combinedWith() {
    Map<String, PropertySet> aSections = ImmutableMap.of(
        "InA", PropertySet.of(ImmutableMap.of("ATest", "AValue")),
        "InBoth", PropertySet.of(ImmutableMultimap.of("InBoth", "Override", "InBoth", "AlsoOverrides")));
    IniFile a = IniFile.of(aSections);

    Map<String, PropertySet> bSections = ImmutableMap.of(
        "InB", PropertySet.of(ImmutableMap.of("BTest", "BValue")),
        "InBoth", PropertySet.of(ImmutableMultimap.of("InBoth", "Ignored", "InBoth", "AlsoIgnored")));
    IniFile b = IniFile.of(bSections);

    IniFile combined = a.combinedWith(b);
    assertThat(combined.sections()).containsExactlyInAnyOrder("InA", "InB", "InBoth");
    assertThat(combined.section("InA").valueList("ATest")).isEqualTo(ImmutableList.of("AValue"));
    assertThat(combined.section("InB").valueList("BTest")).isEqualTo(ImmutableList.of("BValue"));
    assertThat(combined.section("InBoth").valueList("InBoth")).isEqualTo(ImmutableList.of("Override", "AlsoOverrides"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    IniFile a1 = IniFile.of(CharSource.wrap(INI1));
    IniFile a2 = IniFile.of(CharSource.wrap(INI1));
    IniFile b = IniFile.of(CharSource.wrap(INI2));

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_equalsHashCode_section() {
    IniFile a1 = IniFile.of(CharSource.wrap(INI1));
    IniFile a2 = IniFile.of(CharSource.wrap(INI1));
    IniFile b = IniFile.of(CharSource.wrap(INI2));

    assertThat(a1.section("name").equals(a1.section("name"))).isEqualTo(true);
    assertThat(a1.section("name").equals(a2.section("name"))).isEqualTo(true);
    assertThat(a1.section("name").equals(b.section("section"))).isEqualTo(false);
    assertThat(a1.section("name").equals(null)).isEqualTo(false);
    assertThat(a1.section("name").equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.section("name").hashCode()).isEqualTo(a2.section("name").hashCode());
  }

}
