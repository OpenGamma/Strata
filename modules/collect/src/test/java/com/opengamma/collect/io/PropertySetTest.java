/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.io;

import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Test {@link PropertySet}.
 */
@Test
public class PropertySetTest {

  public void test_of_map() {
    Map<String, String> keyValues = ImmutableMap.of("a", "x", "b", "y");
    PropertySet test = PropertySet.of(keyValues);

    assertEquals(test.contains("a"), true);
    assertEquals(test.getValue("a"), "x");
    assertEquals(test.getValueList("a"), ImmutableList.of("x"));
    assertEquals(test.contains("b"), true);
    assertEquals(test.getValue("b"), "y");
    assertEquals(test.getValueList("b"), ImmutableList.of("y"));
    assertEquals(test.contains("c"), false);
    assertEquals(test.getKeys(), ImmutableSet.of("a", "b"));
    assertEquals(test.getKeyValues(), ImmutableListMultimap.of("a", "x", "b", "y"));

    assertThrows(() -> test.getValue("rubbish"), IllegalArgumentException.class);
    assertThrows(() -> test.getValueList("rubbish"), IllegalArgumentException.class);
    assertEquals(test.toString(), "{a=[x], b=[y]}");
  }

  public void test_of_multimap() {
    Multimap<String, String> keyValues = ImmutableMultimap.of("a", "x", "a", "y", "b", "z");
    PropertySet test = PropertySet.of(keyValues);

    assertEquals(test.contains("a"), true);
    assertThrows(() -> test.getValue("a"), IllegalArgumentException.class);
    assertEquals(test.getValueList("a"), ImmutableList.of("x", "y"));
    assertEquals(test.contains("b"), true);
    assertEquals(test.getValue("b"), "z");
    assertEquals(test.getValueList("b"), ImmutableList.of("z"));
    assertEquals(test.contains("c"), false);
    assertEquals(test.getKeys(), ImmutableSet.of("a", "b"));
    assertEquals(test.getKeyValues(), ImmutableListMultimap.of("a", "x", "a", "y", "b", "z"));

    assertThrows(() -> test.getValue("rubbish"), IllegalArgumentException.class);
    assertThrows(() -> test.getValueList("rubbish"), IllegalArgumentException.class);
    assertEquals(test.toString(), "{a=[x, y], b=[z]}");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    Map<String, String> keyValues = ImmutableMap.of("a", "x", "b", "y");
    PropertySet a1 = PropertySet.of(keyValues);
    PropertySet a2 = PropertySet.of(keyValues);
    PropertySet b = PropertySet.of(ImmutableMap.of("a", "x", "b", "z"));

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

}
