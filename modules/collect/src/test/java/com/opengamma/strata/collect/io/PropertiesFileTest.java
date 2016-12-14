/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link PropertiesFile}.
 */
@Test
public class PropertiesFileTest {

  private final String FILE1 = "" +
      "# comment\n" +
      "a = x\n" +
      " \n" +
      "; comment\n" +
      "c = z\n" +
      "b = y\n";
  private final String FILE2 = "" +
      "a = x\n" +
      "a = y\n";

  public void test_of_noLists() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap(FILE1));
    Multimap<String, String> keyValues = ImmutableListMultimap.of("a", "x", "c", "z", "b", "y");
    assertEquals(test.getProperties(), PropertySet.of(keyValues));
    assertEquals(test.toString(), "{a=[x], c=[z], b=[y]}");
  }

  public void test_of_list() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap(FILE2));
    Multimap<String, String> keyValues = ImmutableListMultimap.of("a", "x", "a", "y");
    assertEquals(test.getProperties(), PropertySet.of(keyValues));
    assertEquals(test.toString(), "{a=[x, y]}");
  }

  public void test_of_propertyNoEquals() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap("b\n"));
    Multimap<String, String> keyValues = ImmutableListMultimap.of("b", "");
    assertEquals(test.getProperties(), PropertySet.of(keyValues));
    assertEquals(test.toString(), "{b=[]}");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_invalid_emptyKey() {
    String invalid =
        "= y\n";
    PropertiesFile.of(CharSource.wrap(invalid));
  }

  public void test_of_ioException() {
    assertThrows(
        () -> PropertiesFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8)),
        UncheckedIOException.class);
  }

  public void test_of_set() {
    Multimap<String, String> keyValues = ImmutableListMultimap.of("a", "x", "b", "y");
    PropertiesFile test = PropertiesFile.of(PropertySet.of(keyValues));
    assertEquals(test.getProperties(), PropertySet.of(keyValues));
    assertEquals(test.toString(), "{a=[x], b=[y]}");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    PropertiesFile a1 = PropertiesFile.of(CharSource.wrap(FILE1));
    PropertiesFile a2 = PropertiesFile.of(CharSource.wrap(FILE1));
    PropertiesFile b = PropertiesFile.of(CharSource.wrap(FILE2));

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

}
