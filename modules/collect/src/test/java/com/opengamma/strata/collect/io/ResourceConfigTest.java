/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.caputureSystemErr;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Test {@link ResourceConfig}.
 */
@Test
public class ResourceConfigTest {

  //-------------------------------------------------------------------------
  public void test_orderedResources() throws Exception {
    List<ResourceLocator> list = ResourceConfig.orderedResources("TestFile.txt");
    assertEquals(list.size(), 1);
    ResourceLocator test = list.get(0);
    assertEquals(test.getLocator().startsWith("classpath"), true);
    assertEquals(test.getLocator().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString().startsWith("classpath"), true);
    assertEquals(test.toString().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
  }

  public void test_orderedResources_notFound() throws Exception {
    String captured =
        caputureSystemErr(() -> assertThrows(IllegalStateException.class, () -> ResourceConfig.orderedResources("NotFound.txt")));
    assertTrue(captured.contains("No resource files found on the classpath"));
  }

  //-------------------------------------------------------------------------
  public void test_ofChained_chainNextFileTrue() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain1.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "z", "b", "y");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n");
    assertEquals(
        test.asMap(),
        ImmutableMap.of("one", PropertySet.of(keyValues1), "two", PropertySet.of(keyValues2)));
  }

  public void test_ofChained_chainNextFileFalse() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain2.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "z");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n");
    assertEquals(
        test.asMap(),
        ImmutableMap.of("one", PropertySet.of(keyValues1), "two", PropertySet.of(keyValues2)));
  }

  public void test_ofChained_chainToNowhere() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain3.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "x", "b", "y");
    assertEquals(test.asMap(), ImmutableMap.of("one", PropertySet.of(keyValues1)));
  }

  public void test_ofChained_autoChain() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain4.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "z", "b", "y");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n");
    assertEquals(
        test.asMap(),
        ImmutableMap.of("one", PropertySet.of(keyValues1), "two", PropertySet.of(keyValues2)));
  }

  public void test_ofChained_chainRemoveSections() {
    IniFile test = ResourceConfig.combinedIniFile("TestChain5.ini");
    Multimap<String, String> keyValues1 = ImmutableListMultimap.of("a", "a");
    Multimap<String, String> keyValues2 = ImmutableListMultimap.of("m", "n", "o", "z");
    assertEquals(
        test.asMap(),
        ImmutableMap.of("one", PropertySet.of(keyValues1), "two", PropertySet.of(keyValues2)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(ResourceConfig.class);
  }

}
