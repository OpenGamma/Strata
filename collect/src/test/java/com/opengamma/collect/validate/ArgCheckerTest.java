/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.validate;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test ArgChecker.
 */
@Test
public class ArgCheckerTest {

  public void test_isTrue_ok() {
     ArgChecker.isTrue(true, "Message");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message")
  public void test_isTrue_false() {
    ArgChecker.isTrue(false, "Message");
  }

  public void test_isTrue_ok_args() {
    ArgChecker.isTrue(true, "Message {} {} {}", "A", 2, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "A")
  public void test_isTrue_false_only_arg() {
    ArgChecker.isTrue(false, "{}", "A");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message A, 2, 3.0")
  public void test_isTrue_false_args() {
    ArgChecker.isTrue(false, "Message {}, {}, {}", "A", 2, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Message A, 2 blah - \\[3.0\\]")
  public void test_isTrue_false_one_too_many_args() {
    ArgChecker.isTrue(false, "Message {}, {} blah", "A", 2, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Message A, 2 - \\[3.0, true\\]")
  public void test_isTrue_false_too_many_args() {
    ArgChecker.isTrue(false, "Message {}, {}", "A", 2, 3., true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Message A, 2, 3.0, \\{\\} blah")
  public void test_isTrue_false_too_many_placeholders() {
    ArgChecker.isTrue(false, "Message {}, {}, {}, {} blah", "A", 2, 3.);
  }
  
  public void test_isFalse_ok() {
    ArgChecker.isFalse(false, "Message");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message")
  public void test_isFalse_true() {
    ArgChecker.isFalse(true, "Message");
  }

  public void test_isFalse_ok_args() {
    ArgChecker.isFalse(false, "Message {} {} {}", "A", 2., 3, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message A, 2.0, 3, true")
  public void test_isFalse_true_args() {
    ArgChecker.isFalse(true, "Message {}, {}, {}, {}", "A", 2., 3, true);
  }
  
  //-------------------------------------------------------------------------
  public void test_notNull_ok() {
    assertEquals("Kirk", ArgChecker.notNull("Kirk", "name"));
    assertEquals(Integer.valueOf(1), ArgChecker.notNull(1, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notNull_null() {
    ArgChecker.notNull(null, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNullInjected_ok() {
    assertEquals("Kirk", ArgChecker.notNullInjected("Kirk", "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Injected.*'name'.*")
  public void test_notNullInjected_null() {
    ArgChecker.notNullInjected(null, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notBlank_String_ok() {
    assertEquals("Kirk", ArgChecker.notBlank("Kirk", "name"));
  }

  public void test_notBlank_String_ok_trimmed() {
    assertEquals("Kirk", ArgChecker.notBlank(" Kirk ", "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notBlank_String_null() {
    ArgChecker.notBlank(null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notBlank_String_empty() {
    ArgChecker.notBlank("", "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notBlank_String_spaces() {
    ArgChecker.notBlank("  ", "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_String_ok() {
    assertEquals("Kirk", ArgChecker.notEmpty("Kirk", "name"));
    assertEquals(" ", ArgChecker.notEmpty(" ", "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_String_null() {
      ArgChecker.notEmpty((String) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*empty.*")
  public void test_notEmpty_String_empty() {
    ArgChecker.notEmpty("", "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Array_ok() {
    Object[] array = new Object[] {"Element"};
    Object[] result = ArgChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Array_null() {
    ArgChecker.notEmpty((Object[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_Array_empty() {
    ArgChecker.notEmpty(new Object[]{}, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_2DArray_null() {
    ArgChecker.notEmpty((Object[][]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_2DArray_empty() {
      ArgChecker.notEmpty(new Object[0][0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_intArray_ok() {
    int[] array = new int[] {6};
    int[] result = ArgChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_intArray_null() {
    ArgChecker.notEmpty((int[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_intArray_empty() {
    ArgChecker.notEmpty(new int[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_longArray_ok() {
    long[] array = new long[] {6L};
    long[] result = ArgChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_longArray_null() {
    ArgChecker.notEmpty((long[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_longArray_empty() {
    ArgChecker.notEmpty(new long[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_doubleArray_ok() {
    double[] array = new double[] {6.0d};
    double[] result = ArgChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_doubleArray_null() {
    ArgChecker.notEmpty((double[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_doubleArray_empty() {
    ArgChecker.notEmpty(new double[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Iterable_ok() {
    Iterable<String> coll = Arrays.asList("Element");
    Iterable<String> result = ArgChecker.notEmpty(coll, "name");
    assertEquals(coll, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Iterable_null() {
    ArgChecker.notEmpty((Iterable<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*collection.*'name'.*empty.*")
  public void test_notEmpty_Iterable_empty() {
    ArgChecker.notEmpty(Collections.emptyList(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Collection_ok() {
    List<String> coll = Arrays.asList("Element");
    List<String> result = ArgChecker.notEmpty(coll, "name");
    assertEquals(coll, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Collection_null() {
      ArgChecker.notEmpty((Collection<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*collection.*'name'.*empty.*")
  public void test_notEmpty_Collection_empty() {
    ArgChecker.notEmpty(Collections.emptyList(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Map_ok() {
    SortedMap<String, String> map = ImmutableSortedMap.of("Element", "Element");
    SortedMap<String, String> result = ArgChecker.notEmpty(map, "name");
    assertEquals(map, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Map_null() {
    ArgChecker.notEmpty((Map<?, ?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*map.*'name'.*empty.*")
  public void test_notEmpty_Map_empty() {
    ArgChecker.notEmpty(Collections.emptyMap(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Array_ok() {
    String[] array = new String[] {"Element"};
    String[] result = ArgChecker.noNulls(array, "name");
    assertEquals(array, result);
  }

  public void test_noNulls_Array_ok_empty() {
    Object[] array = new Object[] {};
    ArgChecker.noNulls(array, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_noNulls_Array_null() {
    ArgChecker.noNulls((Object[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*null.*")
  public void test_noNulls_Array_nullElement() {
    ArgChecker.noNulls(new Object[]{null}, "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Iterable_ok() {
    List<String> coll = Arrays.asList("Element");
    List<String> result = ArgChecker.noNulls(coll, "name");
    assertEquals(coll, result);
  }

  public void test_noNulls_Iterable_ok_empty() {
    Iterable<?> coll = Arrays.asList();
    ArgChecker.noNulls(coll, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_noNulls_Iterable_null() {
    ArgChecker.noNulls((Iterable<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*iterable.*'name'.*null.*")
  public void test_noNulls_Iterable_nullElement() {
    ArgChecker.noNulls(Arrays.asList((Object) null), "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Map_ok() {
    ImmutableSortedMap<String, String> map = ImmutableSortedMap.of("A", "B");
    ImmutableSortedMap<String, String> result = ArgChecker.noNulls(map, "name");
    assertEquals(map, result);
  }

  public void test_noNulls_Map_ok_empty() {
    Map<Object, Object> map = new HashMap<>();
    ArgChecker.noNulls(map, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_noNulls_Map_null() {
    ArgChecker.noNulls((Map<Object, Object>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*map.*'name'.*null.*")
  public void test_noNulls_Map_nullKey() {
    Map<Object, Object> map = new HashMap<>();
    map.put("A", "B");
    map.put(null, "Z");
    ArgChecker.noNulls(map, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*map.*'name'.*null.*")
  public void test_noNulls_Map_nullValue() {
    Map<Object, Object> map = new HashMap<>();
    map.put("A", "B");
    map.put("Z", null);
    ArgChecker.noNulls(map, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNegative_int_ok() {
    assertEquals(0, ArgChecker.notNegative(0, "name"));
    assertEquals(1, ArgChecker.notNegative(1, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_int_negative() {
    ArgChecker.notNegative(-1, "name");
  }

  public void test_notNegative_long_ok() {
    assertEquals(0L, ArgChecker.notNegative(0L, "name"));
    assertEquals(1L, ArgChecker.notNegative(1L, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_long_negative() {
    ArgChecker.notNegative(-1L, "name");
  }

  public void test_notNegative_double_ok() {
    assertEquals(0d, ArgChecker.notNegative(0d, "name"), 0.0001d);
    assertEquals(1d, ArgChecker.notNegative(1d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_double_negative() {
    ArgChecker.notNegative(-1.0d, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNegativeOrZero_int_ok() {
    assertEquals(1, ArgChecker.notNegativeOrZero(1, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_int_zero() {
    ArgChecker.notNegativeOrZero(0, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_int_negative() {
    ArgChecker.notNegativeOrZero(-1, "name");
  }

  public void test_notNegativeOrZero_long_ok() {
    assertEquals(1, ArgChecker.notNegativeOrZero(1L, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_long_zero() {
    ArgChecker.notNegativeOrZero(0L, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_long_negative() {
    ArgChecker.notNegativeOrZero(-1L, "name");
  }

  public void test_notNegativeOrZero_double_ok() {
    assertEquals(1d, ArgChecker.notNegativeOrZero(1d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_double_zero() {
    ArgChecker.notNegativeOrZero(0.0d, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_double_negative() {
    ArgChecker.notNegativeOrZero(-1.0d, "name");
  }

  public void test_notNegativeOrZero_double_eps_ok() {
    assertEquals(1d, ArgChecker.notNegativeOrZero(1d, 0.0001d, "name"), 0.0001d);
    assertEquals(0.1d, ArgChecker.notNegativeOrZero(0.1d, 0.0001d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*zero.*")
  public void test_notNegativeOrZero_double_eps_zero() {
    ArgChecker.notNegativeOrZero(0.0000001d, 0.0001d, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*greater.*zero.*")
  public void test_notNegativeOrZero_double_eps_negative() {
    ArgChecker.notNegativeOrZero(-1.0d, 0.0001d, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notZero_double_ok() {
    assertEquals(1d, ArgChecker.notZero(1d, 0.1d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*zero.*")
  public void test_notZero_double_zero() {
    ArgChecker.notZero(0d, 0.1d, "name");
  }

  public void test_notZero_double_negative() {
    ArgChecker.notZero(-1d, 0.1d, "name");
  }

  //-------------------------------------------------------------------------
  public void testHasNullElement() {
    Collection<?> c = Sets.newHashSet(null, new Object(), new Object());
    assertTrue(ArgChecker.hasNullElement(c));
    c = Sets.newHashSet(new Object(), new Object());
    assertFalse(ArgChecker.hasNullElement(c));
  }
  
  public void testHasNegativeElement() {
    Collection<Double> c = Sets.newHashSet(4., -5., -6.);
    assertTrue(ArgChecker.hasNegativeElement(c));
    c = Sets.newHashSet(1., 2., 3.);
    assertFalse(ArgChecker.hasNegativeElement(c));
  }
  
  public void testIsInRange() {
    double low = 0;
    double high = 1;    
    assertTrue(ArgChecker.isInRangeExclusive(low, high, 0.5));
    assertFalse(ArgChecker.isInRangeExclusive(low, high, -high));
    assertFalse(ArgChecker.isInRangeExclusive(low, high, 2 * high));
    assertFalse(ArgChecker.isInRangeExclusive(low, high, low));
    assertFalse(ArgChecker.isInRangeExclusive(low, high, high));
    assertTrue(ArgChecker.isInRangeInclusive(low, high, 0.5));
    assertFalse(ArgChecker.isInRangeInclusive(low, high, -high));
    assertFalse(ArgChecker.isInRangeInclusive(low, high, 2 * high));
    assertTrue(ArgChecker.isInRangeInclusive(low, high, low));
    assertTrue(ArgChecker.isInRangeInclusive(low, high, high));
    assertTrue(ArgChecker.isInRangeExcludingLow(low, high, 0.5));
    assertFalse(ArgChecker.isInRangeExcludingLow(low, high, -high));
    assertFalse(ArgChecker.isInRangeExcludingLow(low, high, 2 * high));
    assertFalse(ArgChecker.isInRangeExcludingLow(low, high, low));
    assertTrue(ArgChecker.isInRangeExcludingLow(low, high, high));
    assertTrue(ArgChecker.isInRangeExcludingHigh(low, high, 0.5));
    assertFalse(ArgChecker.isInRangeExcludingHigh(low, high, -high));
    assertFalse(ArgChecker.isInRangeExcludingHigh(low, high, 2 * high));
    assertTrue(ArgChecker.isInRangeExcludingHigh(low, high, low));
    assertFalse(ArgChecker.isInRangeExcludingHigh(low, high, high));
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void testNotEmptyDoubleArray() {
    ArgChecker.notEmpty(new double[0], "name");
  } 
  
  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void testNotEmptyLongArray() {
    ArgChecker.notEmpty(new double[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_inOrderOrEqual_true() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderOrEqual(a, b, "a", "b");
    ArgChecker.inOrderOrEqual(a, a, "a", "b");
    ArgChecker.inOrderOrEqual(b, b, "a", "b");
  }

  // TODO - re-enable this test once Pair has been created
//  public void test_inOrderOrEqual_generics() {
//    final Pair<String, String> a = ObjectsPair.of("c", "d");
//    final Pair<String, String> b = ObjectsPair.of("e", "f");
//    final FirstThenSecondPairComparator<String, String> comparator = new FirstThenSecondPairComparator<String, String>();
//    Comparable<? super Pair<String, String>> ca = new Comparable<Pair<String, String>>() {
//      @Override
//      public int compareTo(Pair<String, String> other) {
//        return comparator.compare(a, other);
//      }
//    };
//    Comparable<? super Pair<String, String>> cb = new Comparable<Pair<String, String>>() {
//      @Override
//      public int compareTo(Pair<String, String> other) {
//        return comparator.compare(b, other);
//      }
//    };
//    ArgChecker.inOrderOrEqual(ca, b, "a", "b");
//    ArgChecker.inOrderOrEqual(ca, a, "a", "b");
//    ArgChecker.inOrderOrEqual(cb, b, "a", "b");
//  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*a.*before.*b.*")
  public void test_inOrderOrEqual_false() {
    LocalDate a = LocalDate.of(2011, 7, 3);
    LocalDate b = LocalDate.of(2011, 7, 2);
    ArgChecker.inOrderOrEqual(a, b, "a", "b");
  }

}
