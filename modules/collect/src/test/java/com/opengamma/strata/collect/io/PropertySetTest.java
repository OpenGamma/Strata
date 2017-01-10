/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
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

  public void test_empty() {
    PropertySet test = PropertySet.empty();

    assertEquals(test.isEmpty(), true);
    assertEquals(test.contains("unknown"), false);
    assertEquals(test.valueList("unknown"), ImmutableList.of());
    assertThrowsIllegalArg(() -> test.value("unknown"));
    assertEquals(test.toString(), "{}");
  }

  public void test_of_map() {
    Map<String, String> keyValues = ImmutableMap.of("a", "x", "b", "y");
    PropertySet test = PropertySet.of(keyValues);

    assertEquals(test.isEmpty(), false);
    assertEquals(test.contains("a"), true);
    assertEquals(test.value("a"), "x");
    assertEquals(test.valueList("a"), ImmutableList.of("x"));
    assertEquals(test.contains("b"), true);
    assertEquals(test.value("b"), "y");
    assertEquals(test.valueList("b"), ImmutableList.of("y"));
    assertEquals(test.contains("c"), false);
    assertEquals(test.keys(), ImmutableSet.of("a", "b"));
    assertEquals(test.asMap(), ImmutableMap.of("a", "x", "b", "y"));
    assertEquals(test.asMultimap(), ImmutableListMultimap.of("a", "x", "b", "y"));
    assertEquals(test.valueList("unknown"), ImmutableSet.of());

    assertThrowsIllegalArg(() -> test.value("unknown"));
    assertEquals(test.toString(), "{a=[x], b=[y]}");
  }

  public void test_of_multimap() {
    Multimap<String, String> keyValues = ImmutableMultimap.of("a", "x", "a", "y", "b", "z");
    PropertySet test = PropertySet.of(keyValues);

    assertEquals(test.isEmpty(), false);
    assertEquals(test.contains("a"), true);
    assertThrowsIllegalArg(() -> test.value("a"));
    assertEquals(test.valueList("a"), ImmutableList.of("x", "y"));
    assertEquals(test.contains("b"), true);
    assertEquals(test.value("b"), "z");
    assertEquals(test.valueList("b"), ImmutableList.of("z"));
    assertEquals(test.contains("c"), false);
    assertEquals(test.keys(), ImmutableSet.of("a", "b"));
    assertEquals(test.asMultimap(), ImmutableListMultimap.of("a", "x", "a", "y", "b", "z"));
    assertEquals(test.valueList("unknown"), ImmutableSet.of());

    assertThrowsIllegalArg(() -> test.asMap());
    assertThrowsIllegalArg(() -> test.value("unknown"));
    assertEquals(test.toString(), "{a=[x, y], b=[z]}");
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "c", "z"));
    PropertySet other = PropertySet.of(ImmutableListMultimap.of("a", "aa", "b", "bb", "d", "dd"));
    PropertySet expected = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "c", "z", "b", "bb", "d", "dd"));
    assertEquals(base.combinedWith(other), expected);
  }

  public void test_combinedWith_emptyBase() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertEquals(base.combinedWith(PropertySet.empty()), base);
  }

  public void test_combinedWith_emptyOther() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertEquals(PropertySet.empty().combinedWith(base), base);
  }

  //-------------------------------------------------------------------------
  public void test_overrideWith() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    PropertySet other = PropertySet.of(ImmutableListMultimap.of("a", "aa", "c", "cc", "d", "dd", "e", "ee"));
    PropertySet expected = PropertySet.of(ImmutableListMultimap.of("a", "aa", "b", "y", "c", "cc", "d", "dd", "e", "ee"));
    assertEquals(base.overrideWith(other), expected);
  }

  public void test_overrideWith_emptyBase() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertEquals(base.overrideWith(PropertySet.empty()), base);
  }

  public void test_overrideWith_emptyOther() {
    PropertySet base = PropertySet.of(ImmutableListMultimap.of("a", "x", "a", "y", "b", "y", "c", "z"));
    assertEquals(PropertySet.empty().overrideWith(base), base);
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
