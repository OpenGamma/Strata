/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

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
    assertEquals(0, ExternalIdBundle.EMPTY.size());
  }

  //-------------------------------------------------------------------------
  public void test_factory_ExternalScheme_String() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11.getScheme(), _id11.getValue());
    assertEquals(1, test.size());
    assertEquals(Sets.newHashSet(_id11), test.getExternalIds());
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
    assertEquals(1, test.size());
    assertEquals(Sets.newHashSet(_id11), test.getExternalIds());
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
    assertEquals(1, test.size());
    assertEquals(Sets.newHashSet(_id11), test.getExternalIds());
  }

  //-------------------------------------------------------------------------
  public void factory_of_varargs_noExternalIds() {
    ExternalIdBundle test = ExternalIdBundle.of();
    assertEquals(0, test.size());
  }

  public void factory_of_varargs_oneExternalId() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11);
    assertEquals(1, test.size());
    assertEquals(Sets.newHashSet(_id11), test.getExternalIds());
  }

  public void factory_of_varargs_twoExternalIds() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals(2, test.size());
    assertEquals(Sets.newHashSet(_id11, _id12), test.getExternalIds());
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
    assertEquals(0, test.size());
  }

  public void factory_of_Iterable_two() {
    ExternalIdBundle test = ExternalIdBundle.of(Arrays.asList(_id11, _id12));
    assertEquals(2, test.size());
    assertEquals(Sets.newHashSet(_id11, _id12), test.getExternalIds());
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
    assertEquals(0, test.size());
  }

  public void factory_parse_Iterable_two() {
    ExternalIdBundle test = ExternalIdBundle.parse(Arrays.asList(_id11.toString(), _id12.toString()));
    assertEquals(2, test.size());
    assertEquals(Sets.newHashSet(_id11, _id12), test.getExternalIds());
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
    assertEquals(input, input.getExternalIdBundle());
  }

  //-------------------------------------------------------------------------
  public void getExternalId() {
    ExternalIdBundle input = ExternalIdBundle.of(_id11, _id22);
    
    assertEquals(ExternalId.of("D1", "V1"), input.getExternalId(ExternalScheme.of("D1")));
    assertEquals(ExternalId.of("D2", "V2"), input.getExternalId(ExternalScheme.of("D2")));
    assertNull(input.getExternalId(ExternalScheme.of("KirkWylie")));
    assertNull(input.getExternalId(null));
  }

  public void getValue() {
    ExternalIdBundle input = ExternalIdBundle.of(_id11, _id22);

    assertEquals("V1", input.getValue(ExternalScheme.of("D1")));
    assertEquals("V2", input.getValue(ExternalScheme.of("D2")));
    assertNull(input.getValue(ExternalScheme.of("KirkWylie")));
    assertNull(input.getValue(null));
  }

  //-------------------------------------------------------------------------
  public void withExternalId() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalId(ExternalId.of("A", "C"));
    assertEquals(1, base.size());
    assertEquals(2, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
  }

  public void withExternalId_same() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalId(ExternalId.of("A", "B"));
    assertSame(base, test);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void withExternalId_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    base.withExternalId((ExternalId) null);
  }

  //-------------------------------------------------------------------------
  public void withExternalIds() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalIds(ImmutableList.of(ExternalId.of("A", "C"), ExternalId.of("A", "D")));
    assertEquals(1, base.size());
    assertEquals(3, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "D")));
  }

  public void withExternalIds_same() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withExternalIds(ImmutableList.of(ExternalId.of("A", "B"), ExternalId.of("A", "B")));
    assertSame(base, test);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void withExternalIds_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    base.withExternalIds((Iterable<ExternalId>) null);
  }

  //-------------------------------------------------------------------------
  public void withoutExternalId_match() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutExternalId(ExternalId.of("A", "B"));
    assertEquals(1, base.size());
    assertEquals(0, test.size());
  }

  public void withoutExternalId_noMatch() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutExternalId(ExternalId.of("A", "C"));
    assertEquals(1, base.size());
    assertEquals(1, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void withoutExternalId_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    base.withoutExternalId(null);
  }

  //-------------------------------------------------------------------------
  public void withoutScheme_ExternalScheme_match() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutScheme(ExternalScheme.of("A"));
    assertEquals(1, base.size());
    assertEquals(0, test.size());
  }

  public void withoutScheme_ExternalScheme_noMatch() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutScheme(ExternalScheme.of("BLOOMBERG_BUID"));
    assertEquals(1, base.size());
    assertEquals(1, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  public void withoutScheme_ExternalScheme_null() {
    ExternalIdBundle base = ExternalIdBundle.of(ExternalId.of("A", "B"));
    ExternalIdBundle test = base.withoutScheme(null);
    assertEquals(1, base.size());
    assertEquals(1, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  //-------------------------------------------------------------------------
  public void test_size() {
    assertEquals(0, ExternalIdBundle.EMPTY.size());
    assertEquals(1, ExternalIdBundle.of(_id11).size());
    assertEquals(2, ExternalIdBundle.of(_id11, _id12).size());
  }
  
  public void test_isEmpty() {
    assertEquals(true, ExternalIdBundle.EMPTY.isEmpty());
    assertEquals(false, ExternalIdBundle.of(_id11).isEmpty());
  }

  //-------------------------------------------------------------------------
  public void test_iterator() {
    Set<ExternalId> expected = new HashSet<>();
    expected.add(_id11);
    expected.add(_id12);
    Iterable<ExternalId> base = ExternalIdBundle.of(_id11, _id12);
    Iterator<ExternalId> test = base.iterator();
    assertEquals(true, test.hasNext());
    assertEquals(true, expected.remove(test.next()));
    assertEquals(true, test.hasNext());
    assertEquals(true, expected.remove(test.next()));
    assertEquals(false, test.hasNext());
    assertEquals(0, expected.size());
  }

  //-------------------------------------------------------------------------
  public void test_containsAll1() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11);
    assertEquals(false, test.containsAll(ExternalIdBundle.of(_id11, _id12)));
    assertEquals(true, test.containsAll(ExternalIdBundle.of(_id11)));
    assertEquals(false, test.containsAll(ExternalIdBundle.of(_id12)));
    assertEquals(false, test.containsAll(ExternalIdBundle.of(_id21)));
    assertEquals(true, test.containsAll(ExternalIdBundle.EMPTY));
  }

  public void test_containsAll2() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals(true, test.containsAll(ExternalIdBundle.of(_id11, _id12)));
    assertEquals(true, test.containsAll(ExternalIdBundle.of(_id11)));
    assertEquals(true, test.containsAll(ExternalIdBundle.of(_id12)));
    assertEquals(false, test.containsAll(ExternalIdBundle.of(_id21)));
    assertEquals(true, test.containsAll(ExternalIdBundle.EMPTY));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_containsAll_null() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    test.containsAll(null);
  }

  //-------------------------------------------------------------------------
  public void test_containsAny() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals(true, test.containsAny(ExternalIdBundle.of(_id11, _id12)));
    assertEquals(true, test.containsAny(ExternalIdBundle.of(_id11)));
    assertEquals(true, test.containsAny(ExternalIdBundle.of(_id12)));
    assertEquals(false, test.containsAny(ExternalIdBundle.of(_id21)));
    assertEquals(false, test.containsAny(ExternalIdBundle.EMPTY));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_containsAny_null() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    test.containsAny(null);
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals(true, test.contains(_id11));
    assertEquals(true, test.contains(_id11));
    assertEquals(false, test.contains(_id21));
  }

  public void test_contains_null() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals(false, test.contains(null));
  }

  //-------------------------------------------------------------------------
  public void test_equals_same_empty() {
    ExternalIdBundle a1 = ExternalIdBundle.EMPTY;
    ExternalIdBundle a2 = ExternalIdBundle.of(_id11).withoutScheme(_id11.getScheme());
    
    assertEquals(true, a1.equals(a1));
    assertEquals(true, a1.equals(a2));
    assertEquals(true, a2.equals(a1));
    assertEquals(true, a2.equals(a2));
  }

  public void test_equals_same_nonEmpty() {
    ExternalIdBundle a1 = ExternalIdBundle.of(_id11, _id12);
    ExternalIdBundle a2 = ExternalIdBundle.of(_id11, _id12);
    
    assertEquals(true, a1.equals(a1));
    assertEquals(true, a1.equals(a2));
    assertEquals(true, a2.equals(a1));
    assertEquals(true, a2.equals(a2));
  }

  public void test_equals_different() {
    ExternalIdBundle a = ExternalIdBundle.EMPTY;
    ExternalIdBundle b = ExternalIdBundle.of(_id11, _id12);
    
    assertEquals(true, a.equals(a));
    assertEquals(false, a.equals(b));
    assertEquals(false, b.equals(a));
    assertEquals(true, b.equals(b));

    assertEquals(false, b.equals("Rubbish"));
    assertEquals(false, b.equals(null));
  }

  public void test_hashCode() {
    ExternalIdBundle a = ExternalIdBundle.of(_id11, _id12);
    ExternalIdBundle b = ExternalIdBundle.of(_id11, _id12);
    
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a.hashCode(), a.hashCode());
  }

  public void test_toString_empty() {
    ExternalIdBundle test = ExternalIdBundle.EMPTY;
    assertEquals("Bundle[]", test.toString());
  }

  public void test_toString_nonEmpty() {
    ExternalIdBundle test = ExternalIdBundle.of(_id11, _id12);
    assertEquals("Bundle[" + _id11.toString() + ", " + _id12.toString() + "]", test.toString());
  }

  public void test_getExeternalIds() {
    ExternalIdBundle bundle = ExternalIdBundle.of(_id11, _id12, _id21, _id22);
    Set<ExternalId> expected = Sets.newHashSet(_id11, _id12);
    assertEquals(expected, bundle.getExternalIds(ExternalScheme.of("D1")));
  }

  public void test_getValues() {
    ExternalIdBundle bundle = ExternalIdBundle.of(_id11, _id12, _id21, _id22);
    Set<String> expected = Sets.newHashSet(_id11.getValue(), _id12.getValue());
    assertEquals(expected, bundle.getValues(ExternalScheme.of("D1")));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ExternalIdBundle.of(_id11, _id12, _id21, _id22));
  }

}
