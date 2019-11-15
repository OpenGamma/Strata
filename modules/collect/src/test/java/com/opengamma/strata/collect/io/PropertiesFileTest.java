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

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link PropertiesFile}.
 */
public class PropertiesFileTest {

  private static final String FILE1 = "" +
      "# comment\n" +
      "a = x\n" +
      " \n" +
      "; comment\n" +
      "c = z\n" +
      "b = y\n";
  private static final String FILE2 = "" +
      "a = x\n" +
      "a = y\n";
  private static final String FILE3 = "" +
      "a=d= = x\n";
  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of_noLists() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap(FILE1));
    Multimap<String, String> keyValues = ImmutableListMultimap.of("a", "x", "c", "z", "b", "y");
    assertThat(test.getProperties()).isEqualTo(PropertySet.of(keyValues));
    assertThat(test.toString()).isEqualTo("{a=[x], c=[z], b=[y]}");
  }

  @Test
  public void test_of_list() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap(FILE2));
    Multimap<String, String> keyValues = ImmutableListMultimap.of("a", "x", "a", "y");
    assertThat(test.getProperties()).isEqualTo(PropertySet.of(keyValues));
    assertThat(test.toString()).isEqualTo("{a=[x, y]}");
  }

  @Test
  public void test_of_escaping() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap(FILE3));
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a=d=", "x");
    assertThat(test.getProperties()).isEqualTo(PropertySet.of(keyValues1));
  }

  @Test
  public void test_of_propertyNoEquals() {
    PropertiesFile test = PropertiesFile.of(CharSource.wrap("b\n"));
    Multimap<String, String> keyValues = ImmutableListMultimap.of("b", "");
    assertThat(test.getProperties()).isEqualTo(PropertySet.of(keyValues));
    assertThat(test.toString()).isEqualTo("{b=[]}");
  }

  @Test
  public void test_of_invalid_emptyKey() {
    String invalid =
        "= y\n";
    assertThatIllegalArgumentException().isThrownBy(() -> PropertiesFile.of(CharSource.wrap(invalid)));
  }

  @Test
  public void test_of_ioException() {
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
        () -> PropertiesFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8)));
  }

  @Test
  public void test_of_set() {
    Multimap<String, String> keyValues = ImmutableListMultimap.of("a", "x", "b", "y");
    PropertiesFile test = PropertiesFile.of(PropertySet.of(keyValues));
    assertThat(test.getProperties()).isEqualTo(PropertySet.of(keyValues));
    assertThat(test.toString()).isEqualTo("{a=[x], b=[y]}");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    PropertiesFile a1 = PropertiesFile.of(CharSource.wrap(FILE1));
    PropertiesFile a2 = PropertiesFile.of(CharSource.wrap(FILE1));
    PropertiesFile b = PropertiesFile.of(CharSource.wrap(FILE2));

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

}
