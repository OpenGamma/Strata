/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Test ArgChecker.
 */
@Test
public class ArgCheckerTest {

  public void test_isTrue_simple_ok() {
    ArgChecker.isTrue(true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_isTrue_simple_false() {
    ArgChecker.isTrue(false);
  }

  public void test_isTrue_ok() {
    ArgChecker.isTrue(true, "Message");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message")
  public void test_isTrue_false() {
    ArgChecker.isTrue(false, "Message");
  }

  public void test_isTrue_ok_args() {
    ArgChecker.isTrue(true, "Message {} {} {}", "A", 2, 3d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message A 2 3.0")
  public void test_isTrue_false_args() {
    ArgChecker.isTrue(false, "Message {} {} {}", "A", 2, 3d);
  }

  public void test_isTrue_ok_longArg() {
    ArgChecker.isTrue(true, "Message {}", 3L);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message 3")
  public void test_isTrue_false_longArg() {
    ArgChecker.isTrue(false, "Message {}", 3L);
  }

  public void test_isTrue_ok_doubleArg() {
    ArgChecker.isTrue(true, "Message {}", 3d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message 3.0")
  public void test_isTrue_false_doubleArg() {
    ArgChecker.isTrue(false, "Message {}", 3d);
  }

  //-------------------------------------------------------------------------
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

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Message A 2 3.0")
  public void test_isFalse_ok_args_true() {
    ArgChecker.isFalse(true, "Message {} {} {}", "A", 2, 3.);
  }

  //-------------------------------------------------------------------------
  public void test_notNull_ok() {
    assertEquals(ArgChecker.notNull("OG", "name"), "OG");
    assertEquals(ArgChecker.notNull(1, "name"), Integer.valueOf(1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_notNull_null() {
    ArgChecker.notNull(null, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNullItem_noText_ok() {
    assertEquals(ArgChecker.notNullItem("OG"), "OG");
    assertEquals(ArgChecker.notNullItem(1), Integer.valueOf(1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_notNullItem_noText_null() {
    ArgChecker.notNullItem(null);
  }

  //-------------------------------------------------------------------------
  public void test_matches_String_ok() {
    assertEquals(ArgChecker.matches(Pattern.compile("[A-Z]+"), "OG", "name"), "OG");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'pattern'.*")
  public void test_matches_String_nullPattern() {
    ArgChecker.matches(null, "", "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_String_nullString() {
    ArgChecker.matches(Pattern.compile("[A-Z]+"), null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_String_empty() {
    ArgChecker.matches(Pattern.compile("[A-Z]+"), "", "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*'123'.*")
  public void test_matches_String_noMatch() {
    ArgChecker.matches(Pattern.compile("[A-Z]+"), "123", "name");
  }

  //-------------------------------------------------------------------------
  public void test_matches_CharMatcher_String_ok() {
    assertEquals(ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, Integer.MAX_VALUE, "OG", "name", "[A-Z]+"), "OG");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_CharMatcher_String_tooShort() {
    ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, 2, "", "name", "[A-Z]+");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_CharMatcher_String_tooLong() {
    ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, 2, "abc", "name", "[A-Z]+");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'pattern'.*")
  public void test_matches_CharMatcher_String_nullMatcher() {
    ArgChecker.matches(null, 1, Integer.MAX_VALUE, "", "name", "[A-Z]+");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*")
  public void test_matches_CharMatcher_String_nullString() {
    ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, 2, null, "name", "[A-Z]+");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*'123'.*")
  public void test_matches_CharMatcher_String_noMatch() {
    ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, Integer.MAX_VALUE, "123", "name", "[A-Z]+");
  }

  //-------------------------------------------------------------------------
  public void test_notBlank_String_ok() {
    assertEquals(ArgChecker.notBlank("OG", "name"), "OG");
  }

  public void test_notBlank_String_ok_notTrimmed() {
    assertEquals(ArgChecker.notBlank(" OG ", "name"), " OG ");
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
    assertEquals(ArgChecker.notEmpty("OG", "name"), "OG");
    assertEquals(ArgChecker.notEmpty(" ", "name"), " ");
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
    Object[] expected = new Object[] {"Element"};
    Object[] result = ArgChecker.notEmpty(expected, "name");
    assertEquals(result, expected);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Array_null() {
    ArgChecker.notEmpty((Object[]) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void test_notEmpty_Array_empty() {
    ArgChecker.notEmpty(new Object[] {}, "name");
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
    int[] expected = new int[] {6};
    int[] result = ArgChecker.notEmpty(expected, "name");
    assertEquals(result, expected);
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
    long[] expected = new long[] {6L};
    long[] result = ArgChecker.notEmpty(expected, "name");
    assertEquals(result, expected);
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
    double[] expected = new double[] {6.0d};
    double[] result = ArgChecker.notEmpty(expected, "name");
    assertEquals(result, expected);
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
    Iterable<String> expected = Arrays.asList("Element");
    Iterable<String> result = ArgChecker.notEmpty((Iterable<String>) expected, "name");
    assertEquals(result, expected);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*'name'.*null.*")
  public void test_notEmpty_Iterable_null() {
    ArgChecker.notEmpty((Iterable<?>) null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*iterable.*'name'.*empty.*")
  public void test_notEmpty_Iterable_empty() {
    ArgChecker.notEmpty((Iterable<?>) Collections.emptyList(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_notEmpty_Collection_ok() {
    List<String> expected = Arrays.asList("Element");
    List<String> result = ArgChecker.notEmpty(expected, "name");
    assertEquals(result, expected);
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
    SortedMap<String, String> expected = ImmutableSortedMap.of("Element", "Element");
    SortedMap<String, String> result = ArgChecker.notEmpty(expected, "name");
    assertEquals(result, expected);
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
    String[] expected = new String[] {"Element"};
    String[] result = ArgChecker.noNulls(expected, "name");
    assertEquals(result, expected);
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
    ArgChecker.noNulls(new Object[] {null}, "name");
  }

  //-------------------------------------------------------------------------
  public void test_noNulls_Iterable_ok() {
    List<String> expected = Arrays.asList("Element");
    List<String> result = ArgChecker.noNulls(expected, "name");
    assertEquals(result, expected);
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
    ImmutableSortedMap<String, String> expected = ImmutableSortedMap.of("A", "B");
    ImmutableSortedMap<String, String> result = ArgChecker.noNulls(expected, "name");
    assertEquals(result, expected);
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
    assertEquals(ArgChecker.notNegative(0, "name"), 0);
    assertEquals(ArgChecker.notNegative(1, "name"), 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_int_negative() {
    ArgChecker.notNegative(-1, "name");
  }

  public void test_notNegative_long_ok() {
    assertEquals(ArgChecker.notNegative(0L, "name"), 0L);
    assertEquals(ArgChecker.notNegative(1L, "name"), 1L);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_long_negative() {
    ArgChecker.notNegative(-1L, "name");
  }

  public void test_notNegative_double_ok() {
    assertEquals(ArgChecker.notNegative(0d, "name"), 0d, 0.0001d);
    assertEquals(ArgChecker.notNegative(1d, "name"), 1d, 0.0001d);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*'name'.*negative.*")
  public void test_notNegative_double_negative() {
    ArgChecker.notNegative(-1.0d, "name");
  }

  //-------------------------------------------------------------------------
  public void test_notNegativeOrZero_int_ok() {
    assertEquals(ArgChecker.notNegativeOrZero(1, "name"), 1);
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
    assertEquals(ArgChecker.notNegativeOrZero(1L, "name"), 1);
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
    assertEquals(ArgChecker.notNegativeOrZero(1d, "name"), 1d, 0.0001d);
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
    assertEquals(ArgChecker.notNegativeOrZero(1d, 0.0001d, "name"), 1d, 0.0001d);
    assertEquals(ArgChecker.notNegativeOrZero(0.1d, 0.0001d, "name"), 0.1d, 0.0001d);
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
    assertEquals(ArgChecker.notZero(1d, 0.1d, "name"), 1d, 0.0001d);
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
  public void test_double_inRange() {
    double low = 0d;
    double mid = 0.5d;
    double high = 1d;
    double small = 0.00000000001d;
    assertEquals(ArgChecker.inRange(mid, low, high, "name"), mid);
    assertEquals(ArgChecker.inRange(low, low, high, "name"), low);
    assertEquals(ArgChecker.inRange(high - small, low, high, "name"), high - small);

    assertEquals(ArgChecker.inRangeInclusive(mid, low, high, "name"), mid);
    assertEquals(ArgChecker.inRangeInclusive(low, low, high, "name"), low);
    assertEquals(ArgChecker.inRangeInclusive(high, low, high, "name"), high);

    assertEquals(ArgChecker.inRangeExclusive(mid, low, high, "name"), mid);
    assertEquals(ArgChecker.inRangeExclusive(small, low, high, "name"), small);
    assertEquals(ArgChecker.inRangeExclusive(high - small, low, high, "name"), high - small);
  }

  public void test_double_inRange_outOfRange() {
    double low = 0d;
    double high = 1d;
    double small = 0.00000000001d;
    assertThrowsIllegalArg(() -> ArgChecker.inRange(low - small, low, high, "name"));
    assertThrowsIllegalArg(() -> ArgChecker.inRange(high, low, high, "name"));

    assertThrowsIllegalArg(() -> ArgChecker.inRangeInclusive(low - small, low, high, "name"));
    assertThrowsIllegalArg(() -> ArgChecker.inRangeInclusive(high + small, low, high, "name"));

    assertThrowsIllegalArg(() -> ArgChecker.inRangeExclusive(low, low, high, "name"));
    assertThrowsIllegalArg(() -> ArgChecker.inRangeExclusive(high, low, high, "name"));
  }

  public void test_int_inRange() {
    int low = 0;
    int mid = 1;
    int high = 2;
    assertEquals(ArgChecker.inRange(mid, low, high, "name"), mid);
    assertEquals(ArgChecker.inRange(low, low, high, "name"), low);

    assertEquals(ArgChecker.inRangeInclusive(mid, low, high, "name"), mid);
    assertEquals(ArgChecker.inRangeInclusive(low, low, high, "name"), low);
    assertEquals(ArgChecker.inRangeInclusive(high, low, high, "name"), high);

    assertEquals(ArgChecker.inRangeExclusive(mid, low, high, "name"), mid);
  }

  public void test_int_inRange_outOfRange() {
    int low = 0;
    int high = 1;
    assertThrowsIllegalArg(() -> ArgChecker.inRange(low - 1, low, high, "name"));
    assertThrowsIllegalArg(() -> ArgChecker.inRange(high, low, high, "name"));

    assertThrowsIllegalArg(() -> ArgChecker.inRangeInclusive(low - 1, low, high, "name"));
    assertThrowsIllegalArg(() -> ArgChecker.inRangeInclusive(high + 1, low, high, "name"));

    assertThrowsIllegalArg(() -> ArgChecker.inRangeExclusive(low, low, high, "name"));
    assertThrowsIllegalArg(() -> ArgChecker.inRangeExclusive(high, low, high, "name"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*array.*'name'.*empty.*")
  public void testNotEmptyLongArray() {
    ArgChecker.notEmpty(new double[0], "name");
  }

  //-------------------------------------------------------------------------
  public void test_inOrderNotEqual_true() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderNotEqual(a, b, "a", "b");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*a.* [<] .*b.*")
  public void test_inOrderNotEqual_false_invalidOrder() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderNotEqual(b, a, "a", "b");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*a.* [<] .*b.*")
  public void test_inOrderNotEqual_false_equal() {
    LocalDate a = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderNotEqual(a, a, "a", "b");
  }

  //-------------------------------------------------------------------------
  public void test_inOrderOrEqual_true() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderOrEqual(a, b, "a", "b");
    ArgChecker.inOrderOrEqual(a, a, "a", "b");
    ArgChecker.inOrderOrEqual(b, b, "a", "b");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*a.* [<][=] .*b.*")
  public void test_inOrderOrEqual_false() {
    LocalDate a = LocalDate.of(2011, 7, 3);
    LocalDate b = LocalDate.of(2011, 7, 2);
    ArgChecker.inOrderOrEqual(a, b, "a", "b");
  }

  //-------------------------------------------------------------------------
  public void test_validUtilityClass() {
    assertUtilityClass(ArgChecker.class);
  }

}
