/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link IniFile}.
 */
@Test
public class IniFileTest {

  private final String INI1 = "" +
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
  private final String INI2 = "" +
      "[section]\n" +
      "a = x\n" +
      "b = y\n";
  private final String INI3 = "" +
      "[section]\n" +
      "a = x\n" +
      "a = y\n";

  public void test_of_noLists() {
    IniFile test = IniFile.of(CharSource.wrap(INI1));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("c", "x", "b", "y", "a", "z");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("a", "m", "b", "n");
    assertEquals(
        test.asMap(),
        ImmutableMap.of("section", PropertySet.of(keyValues1), "name", PropertySet.of(keyValues2)));

    assertEquals(test.contains("section"), true);
    assertEquals(test.section("section"), PropertySet.of(keyValues1));
    assertEquals(test.section("section").contains("c"), true);
    assertEquals(test.section("section").value("c"), "x");
    assertEquals(test.section("section").valueList("c"), ImmutableList.of("x"));
    assertEquals(test.section("section").contains("b"), true);
    assertEquals(test.section("section").value("b"), "y");
    assertEquals(test.section("section").valueList("b"), ImmutableList.of("y"));
    assertEquals(test.section("section").contains("a"), true);
    assertEquals(test.section("section").value("a"), "z");
    assertEquals(test.section("section").valueList("a"), ImmutableList.of("z"));
    assertEquals(test.section("section").contains("d"), false);
    // order must be retained
    assertEquals(ImmutableList.copyOf(test.section("section").keys()), ImmutableList.of("c", "b", "a"));
    assertEquals(test.section("section").asMultimap(), ImmutableListMultimap.of("c", "x", "b", "y", "a", "z"));

    assertEquals(test.contains("name"), true);
    assertEquals(test.section("name"), PropertySet.of(keyValues2));
    assertEquals(test.section("name").contains("a"), true);
    assertEquals(test.section("name").value("a"), "m");
    assertEquals(test.section("name").valueList("a"), ImmutableList.of("m"));
    assertEquals(test.section("name").contains("b"), true);
    assertEquals(test.section("name").value("b"), "n");
    assertEquals(test.section("name").valueList("b"), ImmutableList.of("n"));
    assertEquals(test.section("name").contains("c"), false);
    assertEquals(ImmutableList.copyOf(test.section("name").keys()), ImmutableList.of("a", "b"));
    assertEquals(test.section("name").asMultimap(), ImmutableListMultimap.of("a", "m", "b", "n"));

    assertEquals(test.contains("unknown"), false);
    assertThrowsIllegalArg(() -> test.section("unknown"));
    assertEquals(test.section("section").valueList("unknown"), ImmutableList.of());
    assertThrowsIllegalArg(() -> test.section("section").value("unknown"));
    assertEquals(test.toString(), "{section={c=[x], b=[y], a=[z]}, name={a=[m], b=[n]}}");
  }

  public void test_of_list() {
    IniFile test = IniFile.of(CharSource.wrap(INI3));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "x", "a", "y");
    assertEquals(test.asMap(), ImmutableMap.of("section", PropertySet.of(keyValues1)));

    assertEquals(test.section("section"), PropertySet.of(keyValues1));
    assertEquals(test.section("section").contains("a"), true);
    assertThrowsIllegalArg(() -> test.section("section").value("a"));
    assertEquals(test.section("section").valueList("a"), ImmutableList.of("x", "y"));
    assertEquals(test.section("section").contains("b"), false);
    assertEquals(test.section("section").keys(), ImmutableSet.of("a"));
    assertEquals(test.section("section").asMultimap(), ImmutableListMultimap.of("a", "x", "a", "y"));
    assertEquals(test.toString(), "{section={a=[x, y]}}");
  }

  public void test_of_propertyNoEquals() {
    IniFile test = IniFile.of(CharSource.wrap("[section]\na\n"));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "");
    assertEquals(test.asMap(), ImmutableMap.of("section", PropertySet.of(keyValues1)));

    assertEquals(test.section("section"), PropertySet.of(keyValues1));
    assertEquals(test.section("section").contains("a"), true);
    assertEquals(test.section("section").valueList("a"), ImmutableList.of(""));
    assertEquals(test.section("section").contains("b"), false);
    assertEquals(test.section("section").keys(), ImmutableSet.of("a"));
    assertEquals(test.section("section").asMultimap(), ImmutableListMultimap.of("a", ""));
    assertEquals(test.toString(), "{section={a=[]}}");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_invalid_propertyAtStart() {
    String invalid =
        "a = x\n";
    IniFile.of(CharSource.wrap(invalid));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_invalid_badSection() {
    String invalid = "" +
        "[section\n" +
        "b\n";
    IniFile.of(CharSource.wrap(invalid));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_invalid_duplicateSection() {
    String invalid = "" +
        "[section]\n" +
        "a = y\n" +
        "[section]\n" +
        "b = y\n";
    IniFile.of(CharSource.wrap(invalid));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_invalid_emptyKey() {
    String invalid = "" +
        "[section]\n" +
        "= y\n";
    IniFile.of(CharSource.wrap(invalid));
  }

  public void test_of_ioException() {
    assertThrows(
        () -> IniFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8)),
        UncheckedIOException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    IniFile a1 = IniFile.of(CharSource.wrap(INI1));
    IniFile a2 = IniFile.of(CharSource.wrap(INI1));
    IniFile b = IniFile.of(CharSource.wrap(INI2));

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_equalsHashCode_section() {
    IniFile a1 = IniFile.of(CharSource.wrap(INI1));
    IniFile a2 = IniFile.of(CharSource.wrap(INI1));
    IniFile b = IniFile.of(CharSource.wrap(INI2));

    assertEquals(a1.section("name").equals(a1.section("name")), true);
    assertEquals(a1.section("name").equals(a2.section("name")), true);
    assertEquals(a1.section("name").equals(b.section("section")), false);
    assertEquals(a1.section("name").equals(null), false);
    assertEquals(a1.section("name").equals(""), false);
    assertEquals(a1.section("name").hashCode(), a2.section("name").hashCode());
  }

}
