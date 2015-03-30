/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.opengamma.strata.collect.TestHelper;

/**
 * Test.
 */
@Test
public class ExternalIdBundleTest {

  private static final ExternalScheme SCHEME = ExternalScheme.of("Scheme");
  private final ExternalId _id11 = ExternalId.of("D1", "V1");
  private final ExternalId _id21 = ExternalId.of("D2", "V1");
  private final ExternalId _id12 = ExternalId.of("D1", "V2");
  private final ExternalId _id22 = ExternalId.of("D2", "V2");

  public void singleton_empty() {
    assertEquals((Object) ExternalIdBundle.EMPTY.size(), 0);
  }

  //-------------------------------------------------------------------------
  public void test_factory_ExternalScheme_String() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11.getScheme(), _id11.getValue());
    assertEquals((Object) test.size(), 1);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_nullScheme() {
    ExternalIdBundle.of((ExternalScheme) null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_nullValue() {
    ExternalIdBundle.of(SCHEME, (String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_emptyValue() {
    ExternalIdBundle.of(SCHEME, "");
  }

  //-------------------------------------------------------------------------
  public void test_factory_String_String() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11.getScheme().getName(), _id11.getValue());
    assertEquals((Object) test.size(), 1);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullScheme() {
    ExternalIdBundle.of((String) null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullValue() {
    ExternalIdBundle.of("Scheme", (String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_emptyValue() {
    ExternalIdBundle.of("Scheme", "");
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_of_ExternalId_null() {
    ExternalIdBundle.of((ExternalId) null);
  }

  public void factory_of_ExternalId() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11);
    assertEquals((Object) test.size(), 1);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11));
  }

  //-------------------------------------------------------------------------
  public void factory_of_varargs_noExternalIds() {
    ExternalIdBundle test = ExternalIdBundle.of();
    assertEquals((Object) test.size(), 0);
  }

  public void factory_of_varargs_oneExternalId() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11);
    assertEquals((Object) test.size(), 1);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11));
  }

  public void factory_of_varargs_twoExternalIds() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals((Object) test.size(), 2);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11, _id12));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_of_varargs_null() {
    ExternalIdBundle.of((ExternalId[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_of_varargs_noNulls() {
    ExternalIdBundle.of(_id11, null, _id12);
  }

  //-------------------------------------------------------------------------
  public void factory_of_Iterable_empty() {
    ExternalIdBundle test = ExternalIdBundle.of(new ArrayList<ExternalId>());
    assertEquals((Object) test.size(), 0);
  }

  public void factory_of_Iterable_two() {
    ExternalIdBundle test = ExternalIdBundle.of(Arrays.asList(_id11, _id12));
    assertEquals((Object) test.size(), 2);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11, _id12));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_of_Iterable_null() {
    ExternalIdBundle.of((Iterable<ExternalId>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_of_Iterable_noNulls() {
    ExternalIdBundle.of(Arrays.asList(_id11, null, _id12));
  }

  //-------------------------------------------------------------------------
  public void factory_parse_Iterable_empty() {
    ExternalIdBundle test = ExternalIdBundle.parse(new ArrayList<String>());
    assertEquals((Object) test.size(), 0);
  }

  public void factory_parse_Iterable_two() {
    ExternalIdBundle test = ExternalIdBundle.parse(Arrays.asList(_id11.toString(), _id12.toString()));
    assertEquals((Object) test.size(), 2);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11, _id12));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_parse_Iterable_null() {
    ExternalIdBundle.parse((Iterable<String>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void factory_parse_Iterable_noNulls() {
    ExternalIdBundle.parse(Arrays.asList(_id11.toString(), null, _id12.toString()));
  }

  //-------------------------------------------------------------------------
  public void singleIdDifferentConstructors() {
    assertTrue(ExternalIdBundle.of(_id11).equals(ExternalIdBundle.of(Collections.singleton(_id11))));
  }

  public void singleVersusMultipleId() {
    assertFalse(ExternalIdBundle.of(_id11).equals(ExternalIdBundle.of(_id11, _id12)));
    assertFalse(ExternalIdBundle.of(_id11, _id12).equals(ExternalIdBundle.of(_id11)));
  }

  //-------------------------------------------------------------------------
  public void getExternalIdBundle() {
    ExternalIdBundle input = ExternalIdBundle.of(_id11, _id22);
    assertEquals(input.getExternalIdBundle(), input);
  }

  //-------------------------------------------------------------------------
  public void getExternalId() {
    ExternalIdBundle input = ExternalIdBundle.of(_id11, _id22);

    assertEquals(input.getExternalId(ExternalScheme.of("D1")), ExternalId.of("D1", "V1"));
    assertEquals(input.getExternalId(ExternalScheme.of("D2")), ExternalId.of("D2", "V2"));
    assertNull(input.getExternalId(ExternalScheme.of("KirkWylie")));
    TestHelper.assertThrows(() -> input.getExternalId(null), IllegalArgumentException.class);
  }

  public void getValue() {
    ExternalIdBundle input = ExternalIdBundle.of(_id11, _id22);

    assertEquals(input.getValue(ExternalScheme.of("D1")), "V1");
    assertEquals(input.getValue(ExternalScheme.of("D2")), "V2");
    assertNull(input.getValue(ExternalScheme.of("KirkWylie")));
    TestHelper.assertThrows(() -> input.getValue(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void withExternalId() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalId(ExternalId.of("A", "C"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 2);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
  }

  public void withExternalId_same() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalId(ExternalId.of("A", "B"));
    assertSame(test, base);
  }

  public void withExternalId_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> base.withExternalId((ExternalId) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void withExternalIds() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalIds(ImmutableList.of(ExternalId.of("A", "C"), ExternalId.of("A", "D")));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 3);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "D")));
  }

  public void withExternalIds_same() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalIds(ImmutableList.of(ExternalId.of("A", "B"), ExternalId.of("A", "B")));
    assertSame(test, base);
  }

  public void withExternalIds_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> base.withExternalIds((Iterable<ExternalId>) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void withoutExternalId_match() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutExternalId(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 0);
  }

  public void withoutExternalId_noMatch() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutExternalId(ExternalId.of("A", "C"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 1);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  public void withoutExternalId_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> base.withoutExternalId(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void withoutScheme_ExternalScheme_match() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutScheme(ExternalScheme.of("A"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 0);
  }

  public void withoutScheme_ExternalScheme_noMatch() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutScheme(ExternalScheme.of("BLOOMBERG_BUID"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 1);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  public void withoutScheme_ExternalScheme_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> base.withoutScheme(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_size() {
    assertEquals((Object) ExternalIdBundle.EMPTY.size(), 0);
    assertEquals((Object) ExternalIdBundle.of(_id11).size(), 1);
    assertEquals((Object) ExternalIdBundle.of(_id11, _id12).size(), 2);
  }

  public void test_isEmpty() {
    assertEquals((Object) ExternalIdBundle.EMPTY.isEmpty(), true);
    assertEquals((Object) ExternalIdBundle.of(_id11).isEmpty(), false);
  }

  //-------------------------------------------------------------------------
  public void test_iterator() {
    Set<ExternalId> expected = new HashSet<>();
    expected.add(_id11);
    expected.add(_id12);
    Iterable<ExternalId> base = ExternalIdBundle.of(_id11, _id12);
    Iterator<ExternalId> test = base.iterator();
    assertEquals((Object) test.hasNext(), true);
    assertEquals((Object) expected.remove(test.next()), true);
    assertEquals((Object) test.hasNext(), true);
    assertEquals((Object) expected.remove(test.next()), true);
    assertEquals((Object) test.hasNext(), false);
    assertEquals((Object) expected.size(), 0);
  }

  //-------------------------------------------------------------------------
  public void test_containsAll1() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id11, _id12)), false);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id11)), true);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id12)), false);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id21)), false);
    assertEquals((Object) test.containsAll(ExternalIdBundle.EMPTY), true);
  }

  public void test_containsAll2() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id11, _id12)), true);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id11)), true);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id12)), true);
    assertEquals((Object) test.containsAll(ExternalIdBundle.of(_id21)), false);
    assertEquals((Object) test.containsAll(ExternalIdBundle.EMPTY), true);
  }

  public void test_containsAll_null() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    TestHelper.assertThrows(() -> test.containsAll(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_containsAny() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals((Object) test.containsAny(ExternalIdBundle.of(_id11, _id12)), true);
    assertEquals((Object) test.containsAny(ExternalIdBundle.of(_id11)), true);
    assertEquals((Object) test.containsAny(ExternalIdBundle.of(_id12)), true);
    assertEquals((Object) test.containsAny(ExternalIdBundle.of(_id21)), false);
    assertEquals((Object) test.containsAny(ExternalIdBundle.EMPTY), false);
  }

  public void test_containsAny_null() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    TestHelper.assertThrows(() -> test.containsAny(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals((Object) test.contains(_id11), true);
    assertEquals((Object) test.contains(_id11), true);
    assertEquals((Object) test.contains(_id21), false);
  }

  public void test_contains_null() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals((Object) test.contains(null), false);
  }

  //-------------------------------------------------------------------------
  public void test_equals_same_empty() {
    ExternalIdBundle a1 = ExternalIdBundle.EMPTY;
    ExternalIdBundle a2 = ExternalIdBundle.of(_id11).withoutScheme(_id11.getScheme());

    assertEquals((Object) a1.equals(a1), true);
    assertEquals((Object) a1.equals(a2), true);
    assertEquals((Object) a2.equals(a1), true);
    assertEquals((Object) a2.equals(a2), true);
  }

  public void test_equals_same_nonEmpty() {
    ExternalIdBundle a1 = ExternalIdBundle.of(_id11, _id12);
    ExternalIdBundle a2 = ExternalIdBundle.of(_id11, _id12);

    assertEquals((Object) a1.equals(a1), true);
    assertEquals((Object) a1.equals(a2), true);
    assertEquals((Object) a2.equals(a1), true);
    assertEquals((Object) a2.equals(a2), true);
  }

  public void test_equals_different() {
    ExternalIdBundle a = ExternalIdBundle.EMPTY;
    ExternalIdBundle b = ExternalIdBundle.of(_id11, _id12);

    assertEquals((Object) a.equals(a), true);
    assertEquals((Object) a.equals(b), false);
    assertEquals((Object) b.equals(a), false);
    assertEquals((Object) b.equals(b), true);

    assertEquals((Object) b.equals("Rubbish"), false);
    assertEquals((Object) b.equals(null), false);
  }

  public void test_hashCode() {
    ExternalIdBundle a = ExternalIdBundle.of(_id11, _id12);
    ExternalIdBundle b = ExternalIdBundle.of(_id11, _id12);

    assertEquals((Object) b.hashCode(), a.hashCode());
    assertEquals((Object) a.hashCode(), a.hashCode());
  }

  public void test_toString_empty() {
    ExternalIdBundle test = ExternalIdBundle.EMPTY;
    assertEquals(test.toString(), "Bundle[]");
  }

  public void test_toString_nonEmpty() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals(test.toString(), "Bundle[" + _id11.toString() + ", " + _id12.toString() + "]");
  }

  public void test_getExeternalIds() {
    ExternalIdBundle bundle = ExternalIdBundle.of(_id11, _id12, _id21, _id22);
    Set<ExternalId> expected = Sets.newHashSet(_id11, _id12);
    assertEquals(bundle.getExternalIds(ExternalScheme.of("D1")), expected);
  }

  public void test_getValues() {
    ExternalIdBundle bundle = ExternalIdBundle.of(_id11, _id12, _id21, _id22);
    Set<String> expected = Sets.newHashSet(_id11.getValue(), _id12.getValue());
    assertEquals(bundle.getValues(ExternalScheme.of("D1")), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TestHelper.coverImmutableBean(ExternalIdBundle.of(_id11, _id12, _id21, _id22));
  }

}
