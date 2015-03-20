/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.io;

import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
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
      "a = x\n" +
      "b = y\n" +
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
    Multimap<String, String> keyValues1 = ArrayListMultimap.create();
    keyValues1.put("a", "x");
    keyValues1.put("b", "y");
    Multimap<String, String> keyValues2 = ArrayListMultimap.create();
    keyValues2.put("a", "m");
    keyValues2.put("b", "n");
    assertEquals(test.getSections(), ImmutableMap.of("section", PropertySet.of(keyValues1), "name", PropertySet.of(keyValues2)));

    assertEquals(test.contains("section"), true);
    assertEquals(test.getSection("section"), PropertySet.of(keyValues1));
    assertEquals(test.getSection("section").contains("a"), true);
    assertEquals(test.getSection("section").getValue("a"), "x");
    assertEquals(test.getSection("section").getValueList("a"), ImmutableList.of("x"));
    assertEquals(test.getSection("section").contains("b"), true);
    assertEquals(test.getSection("section").getValue("b"), "y");
    assertEquals(test.getSection("section").getValueList("b"), ImmutableList.of("y"));
    assertEquals(test.getSection("section").contains("c"), false);
    assertEquals(test.getSection("section").getKeyValues(), ImmutableListMultimap.of("a", "x", "b", "y"));

    assertEquals(test.contains("name"), true);
    assertEquals(test.getSection("name"), PropertySet.of(keyValues2));
    assertEquals(test.getSection("name").contains("a"), true);
    assertEquals(test.getSection("name").getValue("a"), "m");
    assertEquals(test.getSection("name").getValueList("a"), ImmutableList.of("m"));
    assertEquals(test.getSection("name").contains("b"), true);
    assertEquals(test.getSection("name").getValue("b"), "n");
    assertEquals(test.getSection("name").getValueList("b"), ImmutableList.of("n"));
    assertEquals(test.getSection("name").contains("c"), false);
    assertEquals(test.getSection("name").getKeyValues(), ImmutableListMultimap.of("a", "m", "b", "n"));

    assertEquals(test.contains("rubbish"), false);
    assertThrows(() -> test.getSection("rubbish"), IllegalArgumentException.class);
    assertThrows(() -> test.getSection("section").getValue("rubbish"), IllegalArgumentException.class);
    assertThrows(() -> test.getSection("section").getValueList("rubbish"), IllegalArgumentException.class);
    assertEquals(test.toString(), "{section={a=[x], b=[y]}, name={a=[m], b=[n]}}");
  }

  public void test_of_list() {
    IniFile test = IniFile.of(CharSource.wrap(INI3));
    Multimap<String, String> keyValues1 = ArrayListMultimap.create();
    keyValues1.put("a", "x");
    keyValues1.put("a", "y");
    assertEquals(test.getSections(), ImmutableMap.of("section", PropertySet.of(keyValues1)));

    assertEquals(test.getSection("section"), PropertySet.of(keyValues1));
    assertEquals(test.getSection("section").contains("a"), true);
    assertThrows(() -> test.getSection("section").getValue("a"), IllegalArgumentException.class);
    assertEquals(test.getSection("section").getValueList("a"), ImmutableList.of("x", "y"));
    assertEquals(test.getSection("section").contains("b"), false);
    assertEquals(test.getSection("section").getKeyValues(), ImmutableListMultimap.of("a", "x", "a", "y"));
    assertEquals(test.toString(), "{section={a=[x, y]}}");
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
  public void test_of_invalid_propertyNoEquals() {
    String invalid = "" +
        "[section]\n" +
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

    assertEquals(a1.getSection("name").equals(a1.getSection("name")), true);
    assertEquals(a1.getSection("name").equals(a2.getSection("name")), true);
    assertEquals(a1.getSection("name").equals(b.getSection("section")), false);
    assertEquals(a1.getSection("name").equals(null), false);
    assertEquals(a1.getSection("name").equals(""), false);
    assertEquals(a1.getSection("name").hashCode(), a2.getSection("name").hashCode());
  }

}
