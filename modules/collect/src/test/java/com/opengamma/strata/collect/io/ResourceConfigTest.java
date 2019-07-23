/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.caputureSystemErr;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Test {@link ResourceConfig}.
 */
public class ResourceConfigTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_orderedResources() throws Exception {
    List<ResourceLocator> list = ResourceConfig.orderedResources("TestFile.txt");
    assertThat(list.size()).isEqualTo(1);
    ResourceLocator test = list.get(0);
    assertThat(test.getLocator())
        .startsWith("classpath:")
        .endsWith("com/opengamma/strata/config/base/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  @Test
  public void test_orderedResources_notFound() throws Exception {
    String captured = caputureSystemErr(
        () -> assertThatIllegalStateException().isThrownBy(() -> ResourceConfig.orderedResources("NotFound.txt")));
    assertThat(captured).contains("No resource files found on the classpath");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofChained_chainNextFileTrue() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain1.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "z", "b", "y");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n");
    assertThat(test.asMap())
        .hasSize(2)
        .containsEntry("one", PropertySet.of(keyValues1))
        .containsEntry("two", PropertySet.of(keyValues2));
  }

  @Test
  public void test_ofChained_chainNextFileFalse() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain2.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "z");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n");
    assertThat(test.asMap())
        .hasSize(2)
        .containsEntry("one", PropertySet.of(keyValues1))
        .containsEntry("two", PropertySet.of(keyValues2));
  }

  @Test
  public void test_ofChained_chainToNowhere() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain3.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "x", "b", "y");
    assertThat(test.asMap()).isEqualTo(ImmutableMap.of("one", PropertySet.of(keyValues1)));
  }

  @Test
  public void test_ofChained_autoChain() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain4.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "z", "b", "y");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n");
    assertThat(test.asMap())
        .hasSize(2)
        .containsEntry("one", PropertySet.of(keyValues1))
        .containsEntry("two", PropertySet.of(keyValues2));
  }

  @Test
  public void test_ofChained_chainRemoveSections() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain5.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "a");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n", "o", "z");
    assertThat(test.asMap())
        .hasSize(2)
        .containsEntry("one", PropertySet.of(keyValues1))
        .containsEntry("two", PropertySet.of(keyValues2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(ResourceConfig.class);
  }

}
