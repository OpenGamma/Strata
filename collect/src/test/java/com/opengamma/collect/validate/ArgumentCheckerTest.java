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
 * Test ArgumentChecker.
 */
@Test
public class ArgumentCheckerTest {

  public void test_isTrue_ok() {
     ArgumentChecker.isTrue(true, "Message");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message")
  public void test_isTrue_false() {
    ArgumentChecker.isTrue(false, "Message");
  }

  public void test_isTrue_ok_args() {
    ArgumentChecker.isTrue(true, "Message {} {} {}", "A", 2, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message A, 2, 3.0")
  public void test_isTrue_false_args() {
    ArgumentChecker.isTrue(false, "Message {}, {}, {}", "A", 2, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Message A, 2 blah - \\[3.0\\]")
  public void test_isTrue_false_one_too_many_args() {
    ArgumentChecker.isTrue(false, "Message {}, {} blah", "A", 2, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Message A, 2 - \\[3.0, true\\]")
  public void test_isTrue_false_too_many_args() {
    ArgumentChecker.isTrue(false, "Message {}, {}", "A", 2, 3., true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Message A, 2, 3.0, \\{\\} blah")
  public void test_isTrue_false_too_many_placeholders() {
    ArgumentChecker.isTrue(false, "Message {}, {}, {}, {} blah", "A", 2, 3.);
  }
  
  public void test_isFalse_ok() {
    ArgumentChecker.isFalse(false, "Message");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message")
  public void test_isFalse_true() {
    ArgumentChecker.isFalse(true, "Message");
  }

  public void test_isFalse_ok_args() {
    ArgumentChecker.isFalse(false, "Message {} {} {}", "A", 2., 3, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message A, 2.0, 3, true")
  public void test_isFalse_true_args() {
    ArgumentChecker.isFalse(true, "Message {}, {}, {}, {}", "A", 2., 3, true);
  }
  
  //-------------------------------------------------------------------------
  public void test_notNull_ok() {
    assertEquals("Kirk", ArgumentChecker.notNull("Kirk", "name"));
    assertEquals(Integer.valueOf(1), ArgumentChecker.notNull(1, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notNull_null() {
    ArgumentChecker.notNull(null, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNullInjected_ok() {
    assertEquals("Kirk", ArgumentChecker.notNullInjected("Kirk", "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Injected.*'name'.*")
  public void test_notNullInjected_null() {
    ArgumentChecker.notNullInjected(null, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notBlank_String_ok() {
    assertEquals("Kirk", ArgumentChecker.notBlank("Kirk", "name"));
  }

  public void test_notBlank_String_ok_trimmed() {
    assertEquals("Kirk", ArgumentChecker.notBlank(" Kirk ", "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notBlank_String_null() {
    ArgumentChecker.notBlank(null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notBlank_String_empty() {
    ArgumentChecker.notBlank("", "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notBlank_String_spaces() {
    ArgumentChecker.notBlank("  ", "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_String_ok() {
    assertEquals("Kirk", ArgumentChecker.notEmpty("Kirk", "name"));
    assertEquals(" ", ArgumentChecker.notEmpty(" ", "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_String_null() {
      ArgumentChecker.notEmpty((String) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*empty.*")
  public void test_notEmpty_String_empty() {
    ArgumentChecker.notEmpty("", "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Array_ok() {
    Object[] array = new Object[] {"Element"};
    Object[] result = ArgumentChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Array_null() {
    ArgumentChecker.notEmpty((Object[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_Array_empty() {
    ArgumentChecker.notEmpty(new Object[] {}, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_2DArray_null() {
    ArgumentChecker.notEmpty((Object[][]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_2DArray_empty() {
      ArgumentChecker.notEmpty(new Object[0][0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_intArray_ok() {
    int[] array = new int[] {6};
    int[] result = ArgumentChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_intArray_null() {
    ArgumentChecker.notEmpty((int[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_intArray_empty() {
    ArgumentChecker.notEmpty(new int[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_longArray_ok() {
    long[] array = new long[] {6L};
    long[] result = ArgumentChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_longArray_null() {
    ArgumentChecker.notEmpty((long[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_longArray_empty() {
    ArgumentChecker.notEmpty(new long[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_doubleArray_ok() {
    double[] array = new double[] {6.0d};
    double[] result = ArgumentChecker.notEmpty(array, "name");
    assertEquals(array, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_doubleArray_null() {
    ArgumentChecker.notEmpty((double[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_doubleArray_empty() {
    ArgumentChecker.notEmpty(new double[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Iterable_ok() {
    Iterable<String> coll = Arrays.asList("Element");
    Iterable<String> result = ArgumentChecker.notEmpty(coll, "name");
    assertEquals(coll, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Iterable_null() {
    ArgumentChecker.notEmpty((Iterable<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*collection.*'name'.*empty.*")
  public void test_notEmpty_Iterable_empty() {
    ArgumentChecker.notEmpty(Collections.emptyList(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Collection_ok() {
    List<String> coll = Arrays.asList("Element");
    List<String> result = ArgumentChecker.notEmpty(coll, "name");
    assertEquals(coll, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Collection_null() {
      ArgumentChecker.notEmpty((Collection<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*collection.*'name'.*empty.*")
  public void test_notEmpty_Collection_empty() {
    ArgumentChecker.notEmpty(Collections.emptyList(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Map_ok() {
    SortedMap<String, String> map = ImmutableSortedMap.of("Element", "Element");
    SortedMap<String, String> result = ArgumentChecker.notEmpty(map, "name");
    assertEquals(map, result);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Map_null() {
    ArgumentChecker.notEmpty((Map<?, ?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*map.*'name'.*empty.*")
  public void test_notEmpty_Map_empty() {
    ArgumentChecker.notEmpty(Collections.emptyMap(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Array_ok() {
    String[] array = new String[] {"Element"};
    String[] result = ArgumentChecker.noNulls(array, "name");
    assertEquals(array, result);
  }

  public void test_noNulls_Array_ok_empty() {
    Object[] array = new Object[] {};
    ArgumentChecker.noNulls(array, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_noNulls_Array_null() {
    ArgumentChecker.noNulls((Object[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*null.*")
  public void test_noNulls_Array_nullElement() {
    ArgumentChecker.noNulls(new Object[] {null}, "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Iterable_ok() {
    List<String> coll = Arrays.asList("Element");
    List<String> result = ArgumentChecker.noNulls(coll, "name");
    assertEquals(coll, result);
  }

  public void test_noNulls_Iterable_ok_empty() {
    Iterable<?> coll = Arrays.asList();
    ArgumentChecker.noNulls(coll, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_noNulls_Iterable_null() {
    ArgumentChecker.noNulls((Iterable<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*iterable.*'name'.*null.*")
  public void test_noNulls_Iterable_nullElement() {
    ArgumentChecker.noNulls(Arrays.asList((Object) null), "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Map_ok() {
    ImmutableSortedMap<String, String> map = ImmutableSortedMap.of("A", "B");
    ImmutableSortedMap<String, String> result = ArgumentChecker.noNulls(map, "name");
    assertEquals(map, result);
  }

  public void test_noNulls_Map_ok_empty() {
    Map<Object, Object> map = new HashMap<>();
    ArgumentChecker.noNulls(map, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_noNulls_Map_null() {
    ArgumentChecker.noNulls((Map<Object, Object>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*map.*'name'.*null.*")
  public void test_noNulls_Map_nullKey() {
    Map<Object, Object> map = new HashMap<>();
    map.put("A", "B");
    map.put(null, "Z");
    ArgumentChecker.noNulls(map, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*map.*'name'.*null.*")
  public void test_noNulls_Map_nullValue() {
    Map<Object, Object> map = new HashMap<>();
    map.put("A", "B");
    map.put("Z", null);
    ArgumentChecker.noNulls(map, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNegative_int_ok() {
    assertEquals(0, ArgumentChecker.notNegative(0, "name"));
    assertEquals(1, ArgumentChecker.notNegative(1, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_int_negative() {
    ArgumentChecker.notNegative(-1, "name");
  }

  public void test_notNegative_long_ok() {
    assertEquals(0L, ArgumentChecker.notNegative(0L, "name"));
    assertEquals(1L, ArgumentChecker.notNegative(1L, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_long_negative() {
    ArgumentChecker.notNegative(-1L, "name");
  }

  public void test_notNegative_double_ok() {
    assertEquals(0d, ArgumentChecker.notNegative(0d, "name"), 0.0001d);
    assertEquals(1d, ArgumentChecker.notNegative(1d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_double_negative() {
    ArgumentChecker.notNegative(-1.0d, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNegativeOrZero_int_ok() {
    assertEquals(1, ArgumentChecker.notNegativeOrZero(1, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_int_zero() {
    ArgumentChecker.notNegativeOrZero(0, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_int_negative() {
    ArgumentChecker.notNegativeOrZero(-1, "name");
  }

  public void test_notNegativeOrZero_long_ok() {
    assertEquals(1, ArgumentChecker.notNegativeOrZero(1L, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_long_zero() {
    ArgumentChecker.notNegativeOrZero(0L, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_long_negative() {
    ArgumentChecker.notNegativeOrZero(-1L, "name");
  }

  public void test_notNegativeOrZero_double_ok() {
    assertEquals(1d, ArgumentChecker.notNegativeOrZero(1d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_double_zero() {
    ArgumentChecker.notNegativeOrZero(0.0d, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*zero.*")
  public void test_notNegativeOrZero_double_negative() {
    ArgumentChecker.notNegativeOrZero(-1.0d, "name");
  }

  public void test_notNegativeOrZero_double_eps_ok() {
    assertEquals(1d, ArgumentChecker.notNegativeOrZero(1d, 0.0001d, "name"), 0.0001d);
    assertEquals(0.1d, ArgumentChecker.notNegativeOrZero(0.1d, 0.0001d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*zero.*")
  public void test_notNegativeOrZero_double_eps_zero() {
    ArgumentChecker.notNegativeOrZero(0.0000001d, 0.0001d, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*greater.*zero.*")
  public void test_notNegativeOrZero_double_eps_negative() {
    ArgumentChecker.notNegativeOrZero(-1.0d, 0.0001d, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notZero_double_ok() {
    assertEquals(1d, ArgumentChecker.notZero(1d, 0.1d, "name"), 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*zero.*")
  public void test_notZero_double_zero() {
    ArgumentChecker.notZero(0d, 0.1d, "name");
  }

  public void test_notZero_double_negative() {
    ArgumentChecker.notZero(-1d, 0.1d, "name");
  }

  //-------------------------------------------------------------------------
  public void testHasNullElement() {
    Collection<?> c = Sets.newHashSet(null, new Object(), new Object());
    assertTrue(ArgumentChecker.hasNullElement(c));
    c = Sets.newHashSet(new Object(), new Object());
    assertFalse(ArgumentChecker.hasNullElement(c));
  }
  
  public void testHasNegativeElement() {
    Collection<Double> c = Sets.newHashSet(4., -5., -6.);
    assertTrue(ArgumentChecker.hasNegativeElement(c));
    c = Sets.newHashSet(1., 2., 3.);
    assertFalse(ArgumentChecker.hasNegativeElement(c));
  }
  
  public void testIsInRange() {
    double low = 0;
    double high = 1;    
    assertTrue(ArgumentChecker.isInRangeExclusive(low, high, 0.5));
    assertFalse(ArgumentChecker.isInRangeExclusive(low, high, -high));
    assertFalse(ArgumentChecker.isInRangeExclusive(low, high, 2 * high));
    assertFalse(ArgumentChecker.isInRangeExclusive(low, high, low));
    assertFalse(ArgumentChecker.isInRangeExclusive(low, high, high));
    assertTrue(ArgumentChecker.isInRangeInclusive(low, high, 0.5));
    assertFalse(ArgumentChecker.isInRangeInclusive(low, high, -high));
    assertFalse(ArgumentChecker.isInRangeInclusive(low, high, 2 * high));
    assertTrue(ArgumentChecker.isInRangeInclusive(low, high, low));
    assertTrue(ArgumentChecker.isInRangeInclusive(low, high, high));
    assertTrue(ArgumentChecker.isInRangeExcludingLow(low, high, 0.5));
    assertFalse(ArgumentChecker.isInRangeExcludingLow(low, high, -high));
    assertFalse(ArgumentChecker.isInRangeExcludingLow(low, high, 2 * high));
    assertFalse(ArgumentChecker.isInRangeExcludingLow(low, high, low));
    assertTrue(ArgumentChecker.isInRangeExcludingLow(low, high, high));
    assertTrue(ArgumentChecker.isInRangeExcludingHigh(low, high, 0.5));
    assertFalse(ArgumentChecker.isInRangeExcludingHigh(low, high, -high));
    assertFalse(ArgumentChecker.isInRangeExcludingHigh(low, high, 2 * high));
    assertTrue(ArgumentChecker.isInRangeExcludingHigh(low, high, low));
    assertFalse(ArgumentChecker.isInRangeExcludingHigh(low, high, high));
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void testNotEmptyDoubleArray() {
    ArgumentChecker.notEmpty(new double[0], "name");
  } 
  
  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void testNotEmptyLongArray() {
    ArgumentChecker.notEmpty(new double[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_inOrderOrEqual_true() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgumentChecker.inOrderOrEqual(a, b, "a", "b");
    ArgumentChecker.inOrderOrEqual(a, a, "a", "b");
    ArgumentChecker.inOrderOrEqual(b, b, "a", "b");
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
//    ArgumentChecker.inOrderOrEqual(ca, b, "a", "b");
//    ArgumentChecker.inOrderOrEqual(ca, a, "a", "b");
//    ArgumentChecker.inOrderOrEqual(cb, b, "a", "b");
//  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*a.*before.*b.*")
  public void test_inOrderOrEqual_false() {
    LocalDate a = LocalDate.of(2011, 7, 3);
    LocalDate b = LocalDate.of(2011, 7, 2);
    ArgumentChecker.inOrderOrEqual(a, b, "a", "b");
  }

}
