/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.Matrix;

/**
 * Test ArgChecker.
 */
public class ArgCheckerTest {

  @Test
  public void test_isTrue_simple_ok() {
    ArgChecker.isTrue(true);
  }

  @Test
  public void test_isTrue_simple_false() {
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.isTrue(false));
  }

  @Test
  public void test_isTrue_ok() {
    ArgChecker.isTrue(true, "Message");
  }

  @Test
  public void test_isTrue_false() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.isTrue(false, "Message"))
        .withMessage("Message");
  }

  @Test
  public void test_isTrue_ok_args() {
    ArgChecker.isTrue(true, "Message {} {} {}", "A", 2, 3d);
  }

  @Test
  public void test_isTrue_false_args() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.isTrue(false, "Message {} {} {}", "A", 2, 3d))
        .withMessage("Message A 2 3.0");

    
  }

  @Test
  public void test_isTrue_ok_longArg() {
    ArgChecker.isTrue(true, "Message {}", 3L);
  }

  @Test
  public void test_isTrue_false_longArg() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.isTrue(false, "Message {}", 3L))
        .withMessage("Message 3");
  }

  @Test
  public void test_isTrue_ok_doubleArg() {
    ArgChecker.isTrue(true, "Message {}", 3d);
  }

  @Test
  public void test_isTrue_false_doubleArg() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.isTrue(false, "Message {}", 3d))
        .withMessage("Message 3.0");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_isFalse_ok() {
    ArgChecker.isFalse(false, "Message");
  }

  @Test
  public void test_isFalse_true() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.isFalse(true, "Message"))
        .withMessage("Message");
  }

  @Test
  public void test_isFalse_ok_args() {
    ArgChecker.isFalse(false, "Message {} {} {}", "A", 2., 3, true);
  }

  @Test
  public void test_isFalse_ok_args_true() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.isFalse(true, "Message {} {} {}", "A", 2, 3.))
        .withMessage("Message A 2 3.0");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notNull_ok() {
    assertThat(ArgChecker.notNull("OG", "name")).isEqualTo("OG");
    assertThat(ArgChecker.notNull(1, "name")).isEqualTo(Integer.valueOf(1));
  }

  @Test
  public void test_notNull_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNull(null, "name"))
        .withMessageMatching(".*'name'.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notNullItem_noText_ok() {
    assertThat(ArgChecker.notNullItem("OG")).isEqualTo("OG");
    assertThat(ArgChecker.notNullItem(1)).isEqualTo(Integer.valueOf(1));
  }

  @Test
  public void test_notNullItem_noText_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNullItem(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_matches_String_ok() {
    assertThat(ArgChecker.matches(Pattern.compile("[A-Z]+"), "OG", "name")).isEqualTo("OG");
  }

  @Test
  public void test_matches_String_nullPattern() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(null, "", "name"))
        .withMessageMatching(".*'pattern'.*");
  }

  @Test
  public void test_matches_String_nullString() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(Pattern.compile("[A-Z]+"), null, "name"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_matches_String_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(Pattern.compile("[A-Z]+"), "", "name"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_matches_String_noMatch() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(Pattern.compile("[A-Z]+"), "123", "name"))
        .withMessageMatching(".*'name'.*'123'.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_matches_CharMatcher_String_ok() {
    assertThat(ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, Integer.MAX_VALUE, "OG", "name", "[A-Z]+"))
        .isEqualTo("OG");
  }

  @Test
  public void test_matches_CharMatcher_String_tooShort() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, 2, "", "name", "[A-Z]+"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_matches_CharMatcher_String_tooLong() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, 2, "abc", "name", "[A-Z]+"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_matches_CharMatcher_String_nullMatcher() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(null, 1, Integer.MAX_VALUE, "", "name", "[A-Z]+"))
        .withMessageMatching(".*'pattern'.*");
  }

  @Test
  public void test_matches_CharMatcher_String_nullString() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, 2, null, "name", "[A-Z]+"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_matches_CharMatcher_String_noMatch() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.matches(CharMatcher.inRange('A', 'Z'), 1, Integer.MAX_VALUE, "123", "name", "[A-Z]+"))
        .withMessageMatching(".*'name'.*'123'.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notBlank_String_ok() {
    assertThat(ArgChecker.notBlank("OG", "name")).isEqualTo("OG");
  }

  @Test
  public void test_notBlank_String_ok_notTrimmed() {
    assertThat(ArgChecker.notBlank(" OG ", "name")).isEqualTo(" OG ");
  }

  @Test
  public void test_notBlank_String_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notBlank(null, "name"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_notBlank_String_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notBlank("", "name"))
        .withMessageMatching(".*'name'.*");
  }

  @Test
  public void test_notBlank_String_spaces() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notBlank("  ", "name"))
        .withMessageMatching(".*'name'.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_String_ok() {
    assertThat(ArgChecker.notEmpty("OG", "name")).isEqualTo("OG");
    assertThat(ArgChecker.notEmpty(" ", "name")).isEqualTo(" ");
  }

  @Test
  public void test_notEmpty_String_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((String) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_String_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(DoubleArray.of(), "name"))
        .withMessageMatching(".*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_Matrix_ok() {
    DoubleArray expected = DoubleArray.of(1);
    DoubleArray result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_Matrix_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Matrix) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_Matrix_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(DoubleArray.of(), "name"))
        .withMessageMatching(".*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_Array_ok() {
    Object[] expected = new Object[] {"Element"};
    Object[] result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_Array_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Object[]) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_Array_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(new Object[] {}, "name"))
        .withMessageMatching(".*array.*'name'.*empty.*");
  }

  @Test
  public void test_notEmpty_2DArray_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Object[][]) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_2DArray_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(new Object[0][0], "name"))
        .withMessageMatching(".*array.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_intArray_ok() {
    int[] expected = new int[] {6};
    int[] result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_intArray_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((int[]) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_intArray_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(new int[0], "name"))
        .withMessageMatching(".*array.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_longArray_ok() {
    long[] expected = new long[] {6L};
    long[] result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_longArray_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((long[]) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_longArray_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(new long[0], "name"))
        .withMessageMatching(".*array.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_doubleArray_ok() {
    double[] expected = new double[] {6.0d};
    double[] result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_doubleArray_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((double[]) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_doubleArray_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(new double[0], "name"))
        .withMessageMatching(".*array.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_Iterable_ok() {
    Iterable<String> expected = Arrays.asList("Element");
    Iterable<String> result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_Iterable_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Iterable<?>) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_Iterable_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Iterable<?>) Collections.emptyList(), "name"))
        .withMessageMatching(".*iterable.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_Collection_ok() {
    List<String> expected = Arrays.asList("Element");
    List<String> result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_Collection_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Collection<?>) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_Collection_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(Collections.emptyList(), "name"))
        .withMessageMatching(".*collection.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notEmpty_Map_ok() {
    SortedMap<String, String> expected = ImmutableSortedMap.of("Element", "Element");
    SortedMap<String, String> result = ArgChecker.notEmpty(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_notEmpty_Map_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty((Map<?, ?>) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_notEmpty_Map_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(Collections.emptyMap(), "name"))
        .withMessageMatching(".*map.*'name'.*empty.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_noNulls_Array_ok() {
    String[] expected = new String[] {"Element"};
    String[] result = ArgChecker.noNulls(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_noNulls_Array_ok_empty() {
    Object[] array = new Object[] {};
    ArgChecker.noNulls(array, "name");
  }

  @Test
  public void test_noNulls_Array_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls((Object[]) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_noNulls_Array_nullElement() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls(new Object[] {null}, "name"))
        .withMessageMatching(".*array.*'name'.*null.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_noNulls_Iterable_ok() {
    List<String> expected = Arrays.asList("Element");
    List<String> result = ArgChecker.noNulls(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_noNulls_Iterable_ok_empty() {
    Iterable<?> coll = Arrays.asList();
    ArgChecker.noNulls(coll, "name");
  }

  @Test
  public void test_noNulls_Iterable_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls((Iterable<?>) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_noNulls_Iterable_nullElement() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls(Arrays.asList((Object) null), "name"))
        .withMessageMatching(".*iterable.*'name'.*null.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_noNulls_Map_ok() {
    ImmutableSortedMap<String, String> expected = ImmutableSortedMap.of("A", "B");
    ImmutableSortedMap<String, String> result = ArgChecker.noNulls(expected, "name");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void test_noNulls_Map_ok_empty() {
    Map<Object, Object> map = new HashMap<>();
    ArgChecker.noNulls(map, "name");
  }

  @Test
  public void test_noNulls_Map_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls((Map<Object, Object>) null, "name"))
        .withMessageMatching(".*'name'.*null.*");
  }

  @Test
  public void test_noNulls_Map_nullKey() {
    Map<Object, Object> map = new HashMap<>();
    map.put("A", "B");
    map.put(null, "Z");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls(map, "name"))
        .withMessageMatching(".*map.*'name'.*null.*");
  }

  @Test
  public void test_noNulls_Map_nullValue() {
    Map<Object, Object> map = new HashMap<>();
    map.put("A", "B");
    map.put("Z", null);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.noNulls(map, "name"))
        .withMessageMatching(".*map.*'name'.*null.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notPositive_int_ok() {
    assertThat(ArgChecker.notPositive(0, "name")).isEqualTo(0);
    assertThat(ArgChecker.notPositive(-1, "name")).isEqualTo(-1);
  }

  @Test
  public void test_notPositve_int_positive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notPositive(1, "name"))
        .withMessageMatching(".*'name'.*positive.*");

  }

  @Test
  public void test_notPositive_long_ok() {
    assertThat(ArgChecker.notPositive(0L, "name")).isEqualTo(0L);
    assertThat(ArgChecker.notPositive(-1L, "name")).isEqualTo(-1L);
  }

  @Test
  public void test_notPositive_long_positive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notPositive(1L, "name"))
        .withMessageMatching(".*'name'.*positive.*");
  }

  @Test
  public void test_notPositive_double_ok() {
    assertThat(ArgChecker.notPositive(0d, "name")).isEqualTo(0d);
    assertThat(ArgChecker.notPositive(-1d, "name")).isEqualTo(-1d);
  }

  @Test
  public void test_notPositive_double_positive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notPositive(1d, "name"))
        .withMessageMatching(".*'name'.*positive.*");
  }

  @Test
  public void test_notPositive_Decimal_ok() {
    assertThat(ArgChecker.notPositive(Decimal.of(0), "name")).isEqualTo(Decimal.of(0));
    assertThat(ArgChecker.notPositive(Decimal.of(-1), "name")).isEqualTo(Decimal.of(-1));
  }

  @Test
  public void test_notPositive_Decimal_positive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notPositive(Decimal.of(1), "name"))
        .withMessageMatching(".*'name'.*positive.*");
  }

  @Test
  public void test_notPositiveIfPresent_Decimal_ok() {
    assertThat(ArgChecker.notPositiveIfPresent(null, "name")).isNull();
    assertThat(ArgChecker.notPositiveIfPresent(Decimal.of(0), "name")).isEqualTo(Decimal.of(0));
    assertThat(ArgChecker.notPositiveIfPresent(Decimal.of(-1), "name")).isEqualTo(Decimal.of(-1));
  }

  @Test
  public void test_notPositiveIfPresent_Decimal_positive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notPositiveIfPresent(Decimal.of(1), "name"))
        .withMessageMatching(".*'name'.*positive.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notNegative_int_ok() {
    assertThat(ArgChecker.notNegative(0, "name")).isEqualTo(0);
    assertThat(ArgChecker.notNegative(1, "name")).isEqualTo(1);
  }

  @Test
  public void test_notNegative_int_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegative(-1, "name"))
        .withMessageMatching(".*'name'.*negative.*");
    
  }

  @Test
  public void test_notNegative_long_ok() {
    assertThat(ArgChecker.notNegative(0L, "name")).isEqualTo(0L);
    assertThat(ArgChecker.notNegative(1L, "name")).isEqualTo(1L);
  }

  @Test
  public void test_notNegative_long_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegative(-1L, "name"))
        .withMessageMatching(".*'name'.*negative.*");
  }

  @Test
  public void test_notNegative_double_ok() {
    assertThat(ArgChecker.notNegative(0d, "name")).isEqualTo(0d);
    assertThat(ArgChecker.notNegative(1d, "name")).isEqualTo(1d);
  }

  @Test
  public void test_notNegative_double_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegative(-1d, "name"))
        .withMessageMatching(".*'name'.*negative.*");
  }

  @Test
  public void test_notNegative_Decimal() {
    assertThat(ArgChecker.notNegative(Decimal.of(0d), "name")).isEqualTo(Decimal.of(0d));
    assertThat(ArgChecker.notNegative(Decimal.of(1.2d), "name")).isEqualTo(Decimal.of(1.2d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegative(Decimal.of(-1.2d), "name"))
        .withMessageMatching(".*'name'.*negative.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notNaN_double_ok() {
    assertThat(ArgChecker.notNaN(0d, "name")).isEqualTo(0d);
    assertThat(ArgChecker.notNaN(1d, "name")).isEqualTo(1d);
  }

  @Test
  public void test_notNaN_double_NaN() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNaN(Double.NaN, "name"))
        .withMessageMatching(".*'name'.*NaN.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notNegativeOrZero_int_ok() {
    assertThat(ArgChecker.notNegativeOrZero(1, "name")).isEqualTo(1);
  }

  @Test
  public void test_notNegativeOrZero_int_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(0, "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_int_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(-1, "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_long_ok() {
    assertThat(ArgChecker.notNegativeOrZero(1L, "name")).isEqualTo(1);
  }

  @Test
  public void test_notNegativeOrZero_long_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(0L, "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_long_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(-1L, "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_double_ok() {
    assertThat(ArgChecker.notNegativeOrZero(1d, "name")).isEqualTo(1d);
  }

  @Test
  public void test_notNegativeOrZero_double_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(0d, "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_double_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(-1d, "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_double_eps_ok() {
    assertThat(ArgChecker.notNegativeOrZero(1d, 0.0001d, "name")).isEqualTo(1d);
    assertThat(ArgChecker.notNegativeOrZero(0.1d, 0.0001d, "name")).isEqualTo(0.1d);
  }

  @Test
  public void test_notNegativeOrZero_double_eps_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(0.0000001d, 0.0001d, "name"))
        .withMessageMatching(".*'name'.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_double_eps_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(-1.0d, 0.0001d, "name"))
        .withMessageMatching(".*'name'.*greater.*zero.*");
  }

  @Test
  public void test_notNegativeOrZero_Decimal() {
    assertThat(ArgChecker.notNegativeOrZero(Decimal.of(1.2d), "name")).isEqualTo(Decimal.of(1.2d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(Decimal.of(0d), "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notNegativeOrZero(Decimal.of(-1.2d), "name"))
        .withMessageMatching(".*'name'.*negative.*zero.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notZero_double_ok() {
    assertThat(ArgChecker.notZero(1d, "name")).isEqualTo(1d);
  }

  @Test
  public void test_notZero_double_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notZero(0d, "name"))
        .withMessageMatching(".*'name'.*zero.*");
  }

  @Test
  public void test_notZero_double_negative() {
    ArgChecker.notZero(-1d, "name");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_notZero_double_tolerance_ok() {
    assertThat(ArgChecker.notZero(1d, 0.1d, "name")).isEqualTo(1d);
  }

  @Test
  public void test_notZero_double_tolerance_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notZero(0d, 0.1d, "name"))
        .withMessageMatching(".*'name'.*zero.*");
  }

  @Test
  public void test_notZero_double_tolerance_negative() {
    ArgChecker.notZero(-1d, 0.1d, "name");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_double_inRange() {
    double low = 0d;
    double mid = 0.5d;
    double high = 1d;
    double small = 0.00000000001d;
    assertThat(ArgChecker.inRange(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRange(low, low, high, "name")).isEqualTo(low);
    assertThat(ArgChecker.inRange(high - small, low, high, "name")).isEqualTo(high - small);

    assertThat(ArgChecker.inRangeInclusive(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRangeInclusive(low, low, high, "name")).isEqualTo(low);
    assertThat(ArgChecker.inRangeInclusive(high, low, high, "name")).isEqualTo(high);

    assertThat(ArgChecker.inRangeExclusive(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRangeExclusive(small, low, high, "name")).isEqualTo(small);
    assertThat(ArgChecker.inRangeExclusive(high - small, low, high, "name")).isEqualTo(high - small);
  }

  @Test
  public void test_double_inRange_outOfRange() {
    double low = 0d;
    double high = 1d;
    double small = 0.00000000001d;
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRange(low - small, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRange(high, low, high, "name"));

    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeInclusive(low - small, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeInclusive(high + small, low, high, "name"));

    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeExclusive(low, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeExclusive(high, low, high, "name"));
  }

  @Test
  public void test_int_inRange() {
    int low = 0;
    int mid = 1;
    int high = 2;
    assertThat(ArgChecker.inRange(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRange(low, low, high, "name")).isEqualTo(low);

    assertThat(ArgChecker.inRangeInclusive(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRangeInclusive(low, low, high, "name")).isEqualTo(low);
    assertThat(ArgChecker.inRangeInclusive(high, low, high, "name")).isEqualTo(high);

    assertThat(ArgChecker.inRangeExclusive(mid, low, high, "name")).isEqualTo(mid);
  }

  @Test
  public void test_int_inRange_outOfRange() {
    int low = 0;
    int high = 1;
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRange(low - 1, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRange(high, low, high, "name"));

    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeInclusive(low - 1, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeInclusive(high + 1, low, high, "name"));

    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeExclusive(low, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeExclusive(high, low, high, "name"));
  }

  @Test
  public void test_generic_inRange() {
    Duration low = Duration.ZERO;
    Duration mid = Duration.ofSeconds(1);
    Duration high = Duration.ofSeconds(2);
    assertThat(ArgChecker.inRangeComparable(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRangeComparable(low, low, high, "name")).isEqualTo(low);

    assertThat(ArgChecker.inRangeComparableInclusive(mid, low, high, "name")).isEqualTo(mid);
    assertThat(ArgChecker.inRangeComparableInclusive(low, low, high, "name")).isEqualTo(low);
    assertThat(ArgChecker.inRangeComparableInclusive(high, low, high, "name")).isEqualTo(high);

    assertThat(ArgChecker.inRangeComparableExclusive(mid, low, high, "name")).isEqualTo(mid);
  }

  @Test
  public void test_generic_inRange_outOfRange() {
    Duration low = Duration.ZERO;
    Duration high = Duration.ofSeconds(1);
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeComparable(low.minusSeconds(1), low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeComparable(high, low, high, "name"));

    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeComparableInclusive(low.minusSeconds(1), low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeComparableInclusive(high.plusSeconds(1), low, high, "name"));

    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeComparableExclusive(low, low, high, "name"));
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.inRangeComparableExclusive(high, low, high, "name"));
  }

  @Test
  public void testNotEmptyLongArray() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.notEmpty(new double[0], "name"))
        .withMessageMatching(".*array.*'name'.*empty.*");
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_double_noDuplicates() {
    double[] values = {0d, 1d, 10d, 5d};
    assertThat(ArgChecker.noDuplicates(values, "name")).containsExactly(values);
  }

  @Test
  public void test_double_noDuplicates_NaN() {
    double[] values = {0d, 1d, 10d, Double.NaN};
    assertThat(ArgChecker.noDuplicates(values, "name")).containsExactly(values);
  }

  @Test
  public void test_double_noDuplicates_hasDuplicates() {
    double[] values = {0d, 1d, 10d, 5d, 1d};
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.noDuplicates(values, "name"));
  }

  @Test
  public void test_double_noDuplicatesSorted() {
    double[] values = {0d, 1d, 5d, 10d};
    assertThat(ArgChecker.noDuplicatesSorted(values, "name")).containsExactly(values);
  }

  @Test
  public void test_double_noDuplicatesSorted_Nan() {
    double[] values = {0d, 1d, 5d, Double.NaN, 10d};
    assertThat(ArgChecker.noDuplicatesSorted(values, "name")).containsExactly(values);
  }

  @Test
  public void test_double_noDuplicatesSorted_hasDuplicates() {
    double[] values = {0d, 1d, 5d, 5d, 10d};
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.noDuplicatesSorted(values, "name"));
  }

  @Test
  public void test_double_noDuplicatesSorted_notSorted() {
    double[] values = {0d, 1d, 5d, 10d, 4d};
    assertThatIllegalArgumentException().isThrownBy(() -> ArgChecker.noDuplicatesSorted(values, "name"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_inOrderNotEqual_true() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderNotEqual(a, b, "a", "b");
  }

  @Test
  public void test_inOrderNotEqual_false_invalidOrder() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.inOrderNotEqual(b, a, "a", "b"))
        .withMessageMatching(".*a.* [<] .*b.*");
  }

  @Test
  public void test_inOrderNotEqual_false_equal() {
    LocalDate a = LocalDate.of(2011, 7, 3);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.inOrderNotEqual(a, a, "a", "b"))
        .withMessageMatching(".*a.* [<] .*b.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_inOrderOrEqual_true() {
    LocalDate a = LocalDate.of(2011, 7, 2);
    LocalDate b = LocalDate.of(2011, 7, 3);
    ArgChecker.inOrderOrEqual(a, b, "a", "b");
    ArgChecker.inOrderOrEqual(a, a, "a", "b");
    ArgChecker.inOrderOrEqual(b, b, "a", "b");
  }

  @Test
  public void test_inOrderOrEqual_false() {
    LocalDate a = LocalDate.of(2011, 7, 3);
    LocalDate b = LocalDate.of(2011, 7, 2);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArgChecker.inOrderOrEqual(a, b, "a", "b"))
        .withMessageMatching(".*a.* [<][=] .*b.*");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_validUtilityClass() {
    assertUtilityClass(ArgChecker.class);
  }

}
