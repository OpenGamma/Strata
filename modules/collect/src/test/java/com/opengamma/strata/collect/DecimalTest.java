/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Strings;

/**
 * Test {@link Decimal}.
 */
public class DecimalTest {

  public static Object[][] dataValues() {
    return new Object[][] {
        {"1", 1, 0},
        {"1.2", 12, 1},
        {"1.23", 123, 2},
        {"1.1234567890123456", 11234567890123456L, 16},
        {"1.12345678901234567", 112345678901234567L, 17},
        {"1.123456789012345678", 112345678901234567L, 17},
        {"1.1234567890123456789", 112345678901234567L, 17},
        {"0.1234567890123456", 1234567890123456L, 16},
        {"0.12345678901234567", 12345678901234567L, 17},
        {"0.123456789012345678", 123456789012345678L, 18},
        {"0.1234567890123456789", 123456789012345678L, 18},
        {"0.01234567890123456", 1234567890123456L, 17},
        {"0.012345678901234567", 12345678901234567L, 18},
        {"0.0123456789012345678", 12345678901234567L, 18},
        {"0.01234567890123456789", 12345678901234567L, 18},
        {"123456789012345678.123456789012345678", 123456789012345678L, 0},
        {"120.00", 120, 0},
        {"1.20", 12, 1},
        {".123", 123, 3},
        {"-.123", -123, 3},
        {"0.123", 123, 3},
        {"-0.123", -123, 3},
        {"0.0123", 123, 4},
        {"-0.0123", -123, 4},
        {"2000000", 2000000, 0},
        {"2000000.000", 2000000, 0},
        {"2000000.000000000", 2000000, 0},
        {"2000000.00000000000000000", 2000000, 0},
        {"2000000.000000000000000000", 2000000, 0},
        {"2000000.0000000000000000000", 2000000, 0},
        {"2000000.00000000000000000000", 2000000, 0},
        {"123E-2", 123, 2},
        {"123E-0", 123, 0},
        {"123E0", 123, 0},
        {"123E+0", 123, 0},
        {"123E+2", 12300, 0},
        {"123.456E2", 123456, 1},
        {"123.456e3", 123456, 0},
        {"\u066123", 123, 0},
        {"123456789012345678.9", 123456789012345678L, 0},
    };
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValuesOfScaled(String str, long unscaled, int scale) {
    Decimal test = Decimal.ofScaled(unscaled, scale);
    assertThat(test.unscaledValue()).isEqualTo(unscaled);
    assertThat(test.scale()).isEqualTo(scale);
    assertThat(test.toBigDecimal()).isEqualTo(BigDecimal.valueOf(unscaled, scale));
    assertThat(test.toString()).isEqualTo(BigDecimal.valueOf(unscaled, scale).toPlainString());
    assertThat(test.doubleValue()).isEqualTo(BigDecimal.valueOf(unscaled, scale).doubleValue());
    if (Double.toString(test.doubleValue()).equals(test.toString())) {
      assertThat(Decimal.of(test.doubleValue())).isEqualTo(test);
    }
    assertThat(test.equals(test)).isTrue();
    assertThat(test)
        .isNotEqualTo("")
        .isNotEqualTo(Decimal.ofScaled(unscaled != 0 ? Math.abs(unscaled) - 1 : 1, scale))
        .isNotEqualTo(Decimal.ofScaled(unscaled, scale > 0 ? scale - 1 : 1))
        .hasSameHashCodeAs(test);
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValuesOfBigDecimal(String str, long unscaled, int scale) {
    BigDecimal base = BigDecimal.valueOf(unscaled, scale);
    Decimal test = Decimal.of(base);
    assertThat(test.unscaledValue()).isEqualTo(unscaled);
    assertThat(test.scale()).isEqualTo(scale);
    assertThat(test.toBigDecimal()).isEqualTo(base);
    assertThat(test.toString()).isEqualTo(base.toPlainString());
    assertThat(test.doubleValue()).isEqualTo(base.doubleValue());
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValuesToFixedScale(String str, long unscaled, int scale) {
    Decimal base = Decimal.ofScaled(unscaled, scale).roundToScale(2, RoundingMode.HALF_UP);
    FixedScaleDecimal test = base.toFixedScale(2);
    assertThat(test.decimal()).isEqualTo(base);
    assertThat(test.fixedScale()).isEqualTo(2);
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValuesOfString(String str, long unscaled, int scale) {
    Decimal test = Decimal.of(str);
    assertThat(test.unscaledValue()).isEqualTo(unscaled);
    assertThat(test.scale()).isEqualTo(scale);
    assertThat(test.toBigDecimal()).isEqualTo(BigDecimal.valueOf(unscaled, scale));
    assertThat(test.toString()).isEqualTo(BigDecimal.valueOf(unscaled, scale).toPlainString());
    assertThat(test.doubleValue()).isEqualTo(BigDecimal.valueOf(unscaled, scale).doubleValue());
    assertThat(Decimal.parse(str)).isEqualTo(test);
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValuesOfStringPlusPrefix(String str, long unscaled, int scale) {
    if (str.startsWith("-") || str.startsWith("+")) {
      return;
    }
    testValuesOfString("+" + str, unscaled, scale);
  }

  @ParameterizedTest
  @MethodSource("dataValues")
  public void testValuesOfStringZeroPrefix(String str, long unscaled, int scale) {
    String adjStr = (str.startsWith("-") || str.startsWith("+")) ? str.substring(0, 1) + "0" + str.substring(1) : "0" + str;
    testValuesOfString(adjStr, unscaled, scale);
  }

  //-------------------------------------------------------------------------
  public static Object[][] dataLongValues() {
    return new Object[][] {
        {0L},
        {123L},
        {-456L},
        {123456789012345678L},
    };
  }

  @ParameterizedTest
  @MethodSource("dataLongValues")
  public void testValuesOfLong(long value) {
    Decimal test = Decimal.of(value);
    assertThat(test.unscaledValue()).isEqualTo(value);
    assertThat(test.scale()).isEqualTo(0);
    assertThat(test.longValue()).isEqualTo(value);
  }

  @Test
  public void testValuesOfScaledTruncated() {
    Decimal test = Decimal.ofScaled(1234567890123456789L, 1);
    assertThat(test.unscaledValue()).isEqualTo(123456789012345678L);
    assertThat(test.scale()).isEqualTo(0);
  }

  @Test
  public void testValuesOfScaledZeroAnyScale() {
    assertThat(Decimal.ofScaled(0, 0)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(0, 1)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(0, 17)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(0, -1)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(0, -1000)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(0, Integer.MAX_VALUE)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(0, Integer.MIN_VALUE)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(11, 20)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(1, 19)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(2, 36)).isEqualTo(Decimal.ZERO);
  }

  @Test
  public void testValuesOfScaledTrailingZeroes() {
    assertThat(Decimal.ofScaled(20, 2)).isEqualTo(Decimal.ofScaled(2, 1));
    assertThat(Decimal.ofScaled(2000, 4)).isEqualTo(Decimal.ofScaled(2, 1));
    assertThat(Decimal.ofScaled(123000, 5)).isEqualTo(Decimal.ofScaled(123, 2));
    assertThat(Decimal.ofScaled(1234567890123456000L, 19)).isEqualTo(Decimal.ofScaled(1234567890123456L, 16));
    assertThat(Decimal.ofScaled(1000, 20)).isEqualTo(Decimal.ofScaled(1L, 17));
  }

  @Test
  public void testValuesOfScaledNegativeScale() {
    assertThat(Decimal.ofScaled(123, 2)).isEqualTo(Decimal.ofScaled(123L, 2));
    assertThat(Decimal.ofScaled(123, 0)).isEqualTo(Decimal.ofScaled(123L, 0));
    assertThat(Decimal.ofScaled(123, -2)).isEqualTo(Decimal.ofScaled(12300L, 0));
  }

  @Test
  public void testValuesOfBigDecimal() {
    assertThat(Decimal.of(new BigDecimal("0.0000000000"))).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.of(new BigDecimal("0.123000000"))).isEqualTo(Decimal.ofScaled(123L, 3));
    assertThat(Decimal.of(new BigDecimal("-0.123000000"))).isEqualTo(Decimal.ofScaled(-123L, 3));
    assertThat(Decimal.of(new BigDecimal("0.12345678901234567890"))).isEqualTo(Decimal.ofScaled(123456789012345678L, 18));
    assertThat(Decimal.of(BigDecimal.valueOf(123, 2))).isEqualTo(Decimal.ofScaled(123L, 2));
    assertThat(Decimal.of(BigDecimal.valueOf(123, 0))).isEqualTo(Decimal.ofScaled(123L, 0));
    assertThat(Decimal.of(BigDecimal.valueOf(123, -2))).isEqualTo(Decimal.ofScaled(12300L, 0));
  }

  @Test
  public void testBad() {
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of(""));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("-"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("+"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("\n"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("A"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("--123"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("-+123"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("++123"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("+-123"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("1+23"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("123.4.56"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("1234567890123456789"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("1000000000000000000"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("-1000000000000000000"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("12345678901234567890"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("1234567890123456789A"));
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Decimal.of("1234567890123456789-"));
    assertThatExceptionOfType(NumberFormatException.class)
        .isThrownBy(() -> Decimal.of("." + Strings.repeat("1", 256)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(Double.NaN));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(Double.POSITIVE_INFINITY));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(Double.POSITIVE_INFINITY));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(BigDecimal.valueOf(1, -18)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(BigDecimal.valueOf(-1, -18)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(BigDecimal.valueOf(999, -16)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(BigDecimal.valueOf(-999, -16)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(BigDecimal.valueOf(1, -19)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(BigDecimal.valueOf(-1, -19)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(new BigDecimal("12345678901234567890")));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(new BigDecimal("912345678901234567890")));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(1000000000000000000d));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(-1000000000000000000d));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(1000000000000000000L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(-1000000000000000000L));
  }

  @Test
  public void testZero() {
    assertThat(Decimal.ZERO).isEqualTo(Decimal.ofScaled(0, 0));
  }

  //-------------------------------------------------------------------------
  public static Object[][] dataPlus() {
    return new Object[][] {
        {0L, 0, 2L, 0, 2L, 0},
        {2L, 0, 0L, 0, 2L, 0},
        {2L, 0, 3L, 0, 5L, 0},
        {999L, 0, 999L, 0, 1998L, 0},
        {999_999_999_999_999_998L, 0, 1L, 0, 999_999_999_999_999_999L, 0},
        {999_999_999_999_999_999L, 0, -999_999_999_999_999_999L, 0, 0L, 0},
        {23L, 1, 1L, 0, 33L, 1},
        {23L, 1, 11L, 0, 133L, 1},
        {23L, 1, 11L, 1, 34L, 1},
        {1L, 18, 1L, 0, 1L, 0},
        {1L, 17, 1L, 0, 100_000_000_000_000_001L, 17},
        {1L, 16, 1L, 0, 10_000_000_000_000_001L, 16},
    };
  }

  @ParameterizedTest
  @MethodSource("dataPlus")
  public void testPlus(long value1, int scale1, long value2, int scale2, long expValue, int expScale) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    Decimal test2 = Decimal.ofScaled(value2, scale2);
    Decimal expected = Decimal.ofScaled(expValue, expScale);
    assertThat(test1.plus(test2)).isEqualTo(expected);
    assertThat(test2.plus(test1)).isEqualTo(expected);
    if (scale2 == 0) {
      assertThat(test1.plus(value2)).isEqualTo(expected);
    }
  }

  @ParameterizedTest
  @MethodSource("dataPlus")
  public void testPlusNegated(long value1, int scale1, long value2, int scale2, long expValue, int expScale) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    Decimal test2 = Decimal.ofScaled(value2, scale2);
    Decimal expected = Decimal.ofScaled(expValue, expScale);
    assertThat(test1.negated().plus(test2.negated())).isEqualTo(expected.negated());
    assertThat(test2.negated().plus(test1.negated())).isEqualTo(expected.negated());
    if (scale2 == 0) {
      assertThat(test1.negated().plus(-value2)).isEqualTo(expected.negated());
    }
  }

  @ParameterizedTest
  @MethodSource("dataPlus")
  public void testMinus(long value1, int scale1, long value2, int scale2, long expValue, int expScale) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    Decimal test2 = Decimal.ofScaled(value2, scale2);
    Decimal expected = Decimal.ofScaled(expValue, expScale);
    assertThat(test1.minus(test2.negated())).isEqualTo(expected);
    assertThat(test2.minus(test1.negated())).isEqualTo(expected);
    if (scale2 == 0) {
      assertThat(test1.minus(-value2)).isEqualTo(expected);
    }
  }

  @ParameterizedTest
  @MethodSource("dataPlus")
  public void testPlusToZero(long value1, int scale1, long value2, int scale2, long expValue, int expScale) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    assertThat(test1.minus(test1)).isEqualTo(Decimal.ZERO);
    assertThat(test1.plus(test1.negated())).isEqualTo(Decimal.ZERO);
  }

  @Test
  public void testPlusOverflow() {
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.plus(Decimal.of(1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.minus(Decimal.of(-1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MIN_VALUE.minus(Decimal.of(1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MIN_VALUE.plus(Decimal.of(-1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.plus(Decimal.MAX_VALUE));
  }

  @Test
  public void testPlusLong() {
    assertThat(Decimal.of(12d).plus(13L)).isEqualTo(Decimal.of(25L));
    assertThat(Decimal.of(0d).plus(0L)).isEqualTo(Decimal.of(0L));
    assertThat(Decimal.of(0d).plus(999_999_999_999_999_999L)).isEqualTo(Decimal.of(999_999_999_999_999_999L));
    assertThat(Decimal.of(-1d).plus(1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(999_999_999_999_999_999L));
    assertThat(Decimal.of(-1.5d).plus(1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(999_999_999_999_999_999L));
    assertThat(Decimal.of(0d).plus(-999_999_999_999_999_999L)).isEqualTo(Decimal.of(-999_999_999_999_999_999L));
    assertThat(Decimal.of(1d).plus(-1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(-999_999_999_999_999_999L));
    assertThat(Decimal.of(1.5d).plus(-1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(-999_999_999_999_999_999L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).plus(1_000_000_000_000_000_000L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).plus(Long.MAX_VALUE));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(1d).plus(Long.MAX_VALUE));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).plus(-1_000_000_000_000_000_000L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).plus(Long.MIN_VALUE));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(-1d).plus(Long.MIN_VALUE));
  }

  @Test
  public void testPlusDouble() {
    assertThat(Decimal.of(0d).plus(0d)).isEqualTo(Decimal.of(0d));
    assertThat(Decimal.of(0d).plus(123.45d)).isEqualTo(Decimal.of(123.45d));
    assertThat(Decimal.of(123.45d).plus(123.45d)).isEqualTo(Decimal.of(246.9d));
    assertThat(Decimal.of(-123.45d).plus(123.45d)).isEqualTo(Decimal.of(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testMinusLong() {
    assertThat(Decimal.of(12d).minus(13L)).isEqualTo(Decimal.of(-1L));
    assertThat(Decimal.of(0d).minus(0L)).isEqualTo(Decimal.of(0L));
    assertThat(Decimal.of(0d).minus(999_999_999_999_999_999L)).isEqualTo(Decimal.of(-999_999_999_999_999_999L));
    assertThat(Decimal.of(1d).minus(1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(-999_999_999_999_999_999L));
    assertThat(Decimal.of(1.5d).minus(1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(-999_999_999_999_999_999L));
    assertThat(Decimal.of(0d).minus(-999_999_999_999_999_999L)).isEqualTo(Decimal.of(999_999_999_999_999_999L));
    assertThat(Decimal.of(-1d).minus(-1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(999_999_999_999_999_999L));
    assertThat(Decimal.of(-1.5d).minus(-1_000_000_000_000_000_000L)).isEqualTo(Decimal.of(999_999_999_999_999_999L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).minus(1_000_000_000_000_000_000L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).minus(Long.MAX_VALUE));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(-1d).minus(Long.MAX_VALUE));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).minus(-1_000_000_000_000_000_000L));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(0d).minus(Long.MIN_VALUE));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.of(1d).minus(Long.MIN_VALUE));
  }

  @Test
  public void testMinusDouble() {
    assertThat(Decimal.of(0d).minus(0d)).isEqualTo(Decimal.of(0d));
    assertThat(Decimal.of(0d).minus(123.45d)).isEqualTo(Decimal.of(-123.45d));
    assertThat(Decimal.of(123.45d).minus(123.45d)).isEqualTo(Decimal.of(0d));
    assertThat(Decimal.of(-123.45d).minus(123.45d)).isEqualTo(Decimal.of(-246.9d));
  }

  //-------------------------------------------------------------------------
  public static Object[][] dataMultipliedBy() {
    return new Object[][] {
        {0L, 0, 2L, 0, 0L, 0},
        {2L, 0, 0L, 0, 0L, 0},
        {2L, 0, 1L, 0, 2L, 0},
        {2L, 0, 3L, 0, 6L, 0},
        {999_999_999_999_999_999L, 0, 1L, 0, 999_999_999_999_999_999L, 0},
        {2L, 2, 3L, 5, 6L, 7},
        {2L, 10, 3L, 9, 0L, 0},
        {11L, 18, 2L, 1, 2L, 18},
        {111_111_111_111_111_111L, 18, 30L, 0, 333_333_333_333_333_333L, 17},
        {111_111_111_111_111_111L, 18, 33L, 0, 366_666_666_666_666_666L, 17},
        {999_999_999_999_999_999L, 3, 10L, 0, 999_999_999_999_999_999L, 2},
        {2L, 18, 10L, 0, 2L, 17, 2L},
        {2L, 18, 100L, 0, 2L, 16, 2L},
        {2L, 18, 100_000_000_000_000_000L, 0, 2L, 1, 2L},
    };
  }

  @ParameterizedTest
  @MethodSource("dataMultipliedBy")
  public void testMultipliedBy(long value1, int scale1, long value2, int scale2, long expValue, int expScale) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    Decimal test2 = Decimal.ofScaled(value2, scale2);
    Decimal expected = Decimal.ofScaled(expValue, expScale);
    assertThat(test1.multipliedBy(test2)).isEqualTo(expected);
    assertThat(test2.multipliedBy(test1)).isEqualTo(expected);
    assertThat(test1.negated().multipliedBy(test2)).isEqualTo(expected.negated());
    assertThat(test2.negated().multipliedBy(test1)).isEqualTo(expected.negated());
    if (scale2 == 0) {
      assertThat(test1.multipliedBy(value2)).isEqualTo(expected);
    }
  }

  @Test
  public void testMultipliedByOverflow() {
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.multipliedBy(Decimal.of(3)));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.multipliedBy(10));
  }

  @Test
  public void testMultipliedByDouble() {
    assertThat(Decimal.of(0d).multipliedBy(0d)).isEqualTo(Decimal.of(0d));
    assertThat(Decimal.of(0d).multipliedBy(123.45d)).isEqualTo(Decimal.of(0d));
    assertThat(Decimal.of(123.45d).multipliedBy(2d)).isEqualTo(Decimal.of(246.9d));
    assertThat(Decimal.of(-123.45d).multipliedBy(2d)).isEqualTo(Decimal.of(-246.9d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testMovePoint() {
    assertThat(Decimal.ofScaled(1235, 3).movePoint(0)).isEqualTo(Decimal.ofScaled(1235, 3));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(1)).isEqualTo(Decimal.ofScaled(1235, 2));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(2)).isEqualTo(Decimal.ofScaled(1235, 1));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(3)).isEqualTo(Decimal.ofScaled(1235, 0));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(4)).isEqualTo(Decimal.ofScaled(12350, 0));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(-1)).isEqualTo(Decimal.ofScaled(1235, 4));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(-2)).isEqualTo(Decimal.ofScaled(1235, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ofScaled(1235, 3).movePoint(20));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ofScaled(1235, 3).movePoint(Integer.MAX_VALUE));
    assertThat(Decimal.ofScaled(1235, 3).movePoint(-20)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(1235, 3).movePoint(Integer.MIN_VALUE + 100)).isEqualTo(Decimal.ZERO);
    assertThat(Decimal.ofScaled(1235, 3).movePoint(Integer.MIN_VALUE)).isEqualTo(Decimal.ZERO);
  }

  //-------------------------------------------------------------------------
  public static Object[][] dataDividedBy() {
    return new Object[][] {
        {0L, 0, 2L, 0, 0L, 0, 0L},
        {2L, 0, 1L, 0, 2L, 0, 2L},
        {2L, 0, 2L, 0, 1L, 0, 1L},
        {2L, 0, 4L, 0, 5L, 1, 5L},
        {2L, 0, 3L, 0, 666_666_666_666_666_666L, 18, 666_666_666_666_666_667L,},
        {999_999_999_999_999_999L, 0, 1L, 0, 999_999_999_999_999_999L, 0, 999_999_999_999_999_999L},
        {999_999_999_999_999_999L, 0, 2L, 0, 499_999_999_999_999_999L, 0, 500_000_000_000_000_000L},
        {99_999_999_999_999_999L, 0, 2L, 0, 499_999_999_999_999_995L, 1, 499_999_999_999_999_995L},
        {2L, 0, 10L, 0, 2L, 1, 2L},
        {2L, 0, 100L, 0, 2L, 2, 2L},
        {2L, 0, 100_000_000_000_000_000L, 0, 2L, 17, 2L},
    };
  }

  @ParameterizedTest
  @MethodSource("dataDividedBy")
  public void testDividedBy(long value1, int scale1, long value2, int scale2, long expValue, int expScale, long rounded) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    Decimal test2 = Decimal.ofScaled(value2, scale2);
    Decimal expected = Decimal.ofScaled(expValue, expScale);
    assertThat(test1.dividedBy(test2)).isEqualTo(expected);
    assertThat(test1.negated().dividedBy(test2)).isEqualTo(expected.negated());
    if (scale2 == 0) {
      assertThat(test1.dividedBy(value2)).isEqualTo(expected);
    }
  }

  @ParameterizedTest
  @MethodSource("dataDividedBy")
  public void testDividedByRounded(long value1, int scale1, long value2, int scale2, long expValue, int expScale, long rounded) {
    Decimal test1 = Decimal.ofScaled(value1, scale1);
    Decimal test2 = Decimal.ofScaled(value2, scale2);
    Decimal expected = Decimal.ofScaled(rounded, expScale);
    assertThat(test1.dividedBy(test2, RoundingMode.HALF_UP)).isEqualTo(expected);
  }

  @Test
  public void testDividedByOverflow() {
    assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> Decimal.ZERO.dividedBy(Decimal.ZERO));
    assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> Decimal.of(2).dividedBy(Decimal.ZERO));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.dividedBy(Decimal.ofScaled(1, 1)));
  }

  @Test
  public void testDividedByDouble() {
    assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> Decimal.ZERO.dividedBy(0d));
    assertThat(Decimal.of(0d).dividedBy(123.45d)).isEqualTo(Decimal.of(0d));
    assertThat(Decimal.of(123.45d).dividedBy(2d)).isEqualTo(Decimal.of(61.725d));
    assertThat(Decimal.of(-123.45d).dividedBy(2d)).isEqualTo(Decimal.of(-61.725d));
  }

  //-------------------------------------------------------------------------
  public static Object[][] dataRoundToScale() {
    return new Object[][] {
        {"0", 2},
        {"0", 0},
        {"0", -2},
        {"1.234", 2},
        {"1.235", 2},
        {"1.236", 2},
        {"123.45", 100},
        {"123.45", 18},
        {"123.45", 17},
        {"123.45", 3},
        {"123.45", 2},
        {"123.45", 1},
        {"123.45", 0},
        {"123.45", -1},
        {"123.45", -2},
        {"123.45", -3},
        {"123.45", -17},
        {"12345", -2},
        {"1.2005", 2},
        {"125.6", 0},
        {"125.6", -1},
        {"0.000005", -15},
    };
  }

  @ParameterizedTest
  @MethodSource("dataRoundToScale")
  public void testRoundToScale(String value, int desiredScale) {
    for (RoundingMode mode : RoundingMode.values()) {
      if (mode != RoundingMode.UNNECESSARY) {
        assertThat(Decimal.of(value).roundToScale(desiredScale, mode).toString())
            .as("Failed in %s", mode)
            .isEqualTo(new BigDecimal(value).setScale(desiredScale, mode).stripTrailingZeros().toPlainString());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("dataRoundToScale")
  public void testRoundToScaleNegative(String value, int desiredScale) {
    for (RoundingMode mode : RoundingMode.values()) {
      if (mode != RoundingMode.UNNECESSARY) {
        assertThat(Decimal.of(value).negated().roundToScale(desiredScale, mode).toString())
            .as("Failed in %s", mode)
            .isEqualTo(new BigDecimal(value).negate().setScale(desiredScale, mode).stripTrailingZeros().toPlainString());
      }
    }
  }

  @Test
  public void testRoundToScaleBad() {
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.roundToScale(-18, RoundingMode.DOWN));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.roundToScale(-18, RoundingMode.HALF_UP));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.roundToScale(-19, RoundingMode.HALF_UP));
  }

  @Test
  public void testRoundToPrecision() {
    assertThat(Decimal.ofScaled(12345, 2).roundToPrecision(3, RoundingMode.UP)).isEqualTo(Decimal.ofScaled(124, 0));
    assertThat(Decimal.ofScaled(12345, 2).roundToPrecision(2, RoundingMode.UP)).isEqualTo(Decimal.ofScaled(130, 0));
    assertThat(Decimal.ofScaled(12345, 2).roundToPrecision(1, RoundingMode.UP)).isEqualTo(Decimal.ofScaled(200, 0));
    assertThat(Decimal.ofScaled(12345, 2).roundToPrecision(1, RoundingMode.HALF_UP)).isEqualTo(Decimal.ofScaled(100, 0));
    assertThat(Decimal.ofScaled(12345, 2).roundToPrecision(18, RoundingMode.HALF_UP)).isEqualTo(Decimal.ofScaled(12345, 2));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ZERO.roundToPrecision(-1, RoundingMode.CEILING));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.roundToPrecision(-18, RoundingMode.DOWN));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.roundToPrecision(-18, RoundingMode.HALF_UP));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.MAX_VALUE.roundToPrecision(-19, RoundingMode.HALF_UP));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testIsZero() {
    assertThat(Decimal.ofScaled(123, 2).isZero()).isFalse();
    assertThat(Decimal.ZERO.isZero()).isTrue();
    assertThat(Decimal.ofScaled(-123, 2).isZero()).isFalse();
  }

  @Test
  public void testAbs() {
    assertThat(Decimal.ofScaled(123, 2).abs()).isEqualTo(Decimal.ofScaled(123, 2));
    assertThat(Decimal.ofScaled(-123, 2).abs()).isEqualTo(Decimal.ofScaled(123, 2));
  }

  @Test
  public void testNegated() {
    assertThat(Decimal.ofScaled(123, 2).negated()).isEqualTo(Decimal.ofScaled(-123, 2));
    assertThat(Decimal.ofScaled(-123, 2).negated()).isEqualTo(Decimal.ofScaled(123, 2));
  }

  @Test
  public void testSignum() {
    assertThat(Decimal.ofScaled(123, 2).signum()).isEqualTo(1);
    assertThat(Decimal.ZERO.signum()).isZero();
    assertThat(Decimal.ofScaled(-123, 2).signum()).isEqualTo(-1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void testMapAsDouble() {
    assertThat(Decimal.ofScaled(123, 2).mapAsDouble(v -> v / 2 + 5)).isEqualTo(Decimal.ofScaled(5615, 3));
  }

  @Test
  public void testMapAsBigDecimal() {
    assertThat(Decimal.ofScaled(123, 2).mapAsBigDecimal(v -> v.divide(BigDecimal.valueOf(2)))).isEqualTo(Decimal.ofScaled(615, 3));
  }

  //-------------------------------------------------------------------------
  public static Object[][] dataFormat() {
    return new Object[][] {
        {"0", 0, "0", "0"},
        {"0", 1, "0.0", "0.0"},
        {"0", 2, "0.00", "0.00"},
        {"0", 18, "0.000000000000000000", "0.000000000000000000"},
        {"12.345", 0, "12", "12.345"},
        {"12.345", 1, "12.3", "12.345"},
        {"12.345", 2, "12.35", "12.345"},
        {"12.345", 3, "12.345", "12.345"},
        {"12.345", 4, "12.3450", "12.3450"},
        {"12.345", 18, "12.345000000000000000", "12.345000000000000000"},
        {"-12.345", 0, "-12", "-12.345"},
        {"-12.345", 1, "-12.3", "-12.345"},
        {"-12.345", 2, "-12.35", "-12.345"},
        {"-12.345", 3, "-12.345", "-12.345"},
        {"-12.345", 4, "-12.3450", "-12.3450"},
        {"-12.345", 18, "-12.345000000000000000", "-12.345000000000000000"},
        {"0.45", 0, "0", "0.45"},
        {"0.45", 1, "0.5", "0.45"},
        {"0.45", 2, "0.45", "0.45"},
        {"0.45", 3, "0.450", "0.450"},
        {"-0.45", 0, "0", "-0.45"},
        {"-0.45", 1, "-0.5", "-0.45"},
        {"-0.45", 2, "-0.45", "-0.45"},
        {"-0.45", 3, "-0.450", "-0.450"},
    };
  }
  
  @ParameterizedTest
  @MethodSource("dataFormat")
  public void testFormatExact(String input, int dp, String expectedExact, String expectedAtleast) {
    assertThat(Decimal.of(input).format(dp, RoundingMode.HALF_UP)).isEqualTo(expectedExact);
  }

  @ParameterizedTest
  @MethodSource("dataFormat")
  public void testFormatAtleast(String input, int dp, String expectedExact, String expectedAtleast) {
    assertThat(Decimal.of(input).formatAtLeast(dp)).isEqualTo(expectedAtleast);
  }

  @Test
  public void testFormatBad() {
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ZERO.format(-1, RoundingMode.HALF_UP));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ZERO.format(19, RoundingMode.HALF_UP));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ZERO.formatAtLeast(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Decimal.ZERO.formatAtLeast(19));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testCompareTo() {
    assertThat(Decimal.ZERO)
        .isEqualByComparingTo(Decimal.ZERO)
        .isLessThan(Decimal.ofScaled(1, 18))
        .isLessThan(Decimal.ofScaled(1, 0))
        .isLessThan(Decimal.MAX_VALUE)
        .isGreaterThan(Decimal.ofScaled(-1, 18))
        .isGreaterThan(Decimal.ofScaled(-1, 0))
        .isGreaterThan(Decimal.MIN_VALUE);
    assertThat(Decimal.of("1.23"))
        .isEqualByComparingTo(Decimal.ofScaled(123, 2))
        .isLessThan(Decimal.of("2"))
        .isLessThan(Decimal.of("1.230000000001"))
        .isLessThan(Decimal.of("1.231"))
        .isLessThan(Decimal.of("1.24"))
        .isGreaterThan(Decimal.of("0"))
        .isGreaterThan(Decimal.of("1.229999999999"))
        .isGreaterThan(Decimal.of("1.229"))
        .isGreaterThan(Decimal.of("1.22"))
        .isGreaterThan(Decimal.of("1.2"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testMega() {
    // test using a wide range of random values
    Random r = new Random(1001);
    for (int i = 0; i < 10000; i++) {
      Decimal base = Decimal.ofScaled(r.nextInt(), r.nextInt(19));
      Decimal other = Decimal.ofScaled(r.nextInt(), r.nextInt(19));
      assertThat(base.plus(other))
          .as(base + " + " + other)
          .isEqualTo(Decimal.of(base.toBigDecimal().add(other.toBigDecimal())));
      assertThat(base.minus(other))
          .as(base + " - " + other)
          .isEqualTo(Decimal.of(base.toBigDecimal().subtract(other.toBigDecimal())));
      if (base.abs().unscaledValue() < 999_999_999 && other.abs().unscaledValue() < 999_999_999) {
        assertThat(base.multipliedBy(other))
            .as(base + " * " + other)
            .isEqualTo(Decimal.of(base.toBigDecimal().multiply(other.toBigDecimal())));
        if (other.scale() < 9) {
          assertThat(base.dividedBy(other, RoundingMode.HALF_UP))
              .as(base + " / " + other)
              .isEqualTo(Decimal.of(base.toBigDecimal().divide(other.toBigDecimal(), new MathContext(18, RoundingMode.HALF_UP))));
          assertThat(base.dividedBy(other, RoundingMode.DOWN))
              .as(base + " / " + other)
              .isEqualTo(Decimal.of(base.toBigDecimal().divide(other.toBigDecimal(), new MathContext(18, RoundingMode.DOWN))));
          assertThat(base.remainder(other))
              .as(base + " % " + other)
              .isEqualTo(Decimal.of(base.toBigDecimal().remainder(other.toBigDecimal())));
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  @Disabled
  public void testPerformanceUnscaled() {
    long total = 0;
    for (int i = 0; i < 100000; i++) {
      total += BigDecimal.valueOf(i, 2).longValue();
    }
    for (int i = 0; i < 100000; i++) {
      total += Decimal.ofScaled(i, 2).longValue();
    }
    long nano1 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += BigDecimal.valueOf(i, 2).longValue();
    }
    long nano2 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += Decimal.ofScaled(i, 2).longValue();
    }
    long nano3 = System.nanoTime();
    System.out.println(total);
    System.out.println(((nano2 - nano1) / 1000000) + "ns (BigDecimal)");
    System.out.println(((nano3 - nano2) / 1000000) + "ns (Decimal)");
  }

  @Test
  @Disabled
  public void testPerformanceString() {
    long total = 0;
    String[] values = new String[] {"1", "1.2", "1.23", "1.234", "1.2345", "-1", "-2.3", "-3.4", "-4.5", "0"};
    for (int i = 0; i < 100000; i++) {
      total += new BigDecimal(values[i % 10]).longValue();
    }
    for (int i = 0; i < 100000; i++) {
      total += Decimal.of(values[i % 10]).longValue();
    }
    long nano1 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += new BigDecimal(values[i % 10]).longValue();
    }
    long nano2 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += Decimal.of(values[i % 10]).longValue();
    }
    long nano3 = System.nanoTime();
    System.out.println(total);
    System.out.println(((nano2 - nano1) / 1000000) + "ns (BigDecimal)");
    System.out.println(((nano3 - nano2) / 1000000) + "ns (Decimal)");
  }

  @Test
  @Disabled
  public void testPerformanceMultiply() {
    long total = 0;
    String[] values = new String[] {"1", "1.2", "1.23", "1.234", "1.2345", "-1", "-2.3", "-3.4", "-4.5", "0"};
    Decimal[] decimals = Stream.of(values).map(str -> Decimal.of(str)).toArray(Decimal[]::new);
    BigDecimal[] bigDecimals = Stream.of(values).map(str -> new BigDecimal(str)).toArray(BigDecimal[]::new);
    long amount = 2;
    BigDecimal amountBD = BigDecimal.valueOf(2);
    for (int i = 0; i < 10000000; i++) {
      total += bigDecimals[i % 10].multiply(amountBD).longValue();
    }
    for (int i = 0; i < 10000000; i++) {
      total += decimals[i % 10].multipliedBy(amount).longValue();
    }
    long nano1 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += bigDecimals[i % 10].multiply(amountBD).longValue();
    }
    long nano2 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += decimals[i % 10].multipliedBy(amount).longValue();
    }
    long nano3 = System.nanoTime();
    System.out.println(total);
    System.out.println(((nano2 - nano1) / 1000000) + "ns (BigDecimal)");
    System.out.println(((nano3 - nano2) / 1000000) + "ns (Decimal)");
  }

  @Test
  @Disabled
  public void testPerformanceRound() {
    long total = 0;
    String[] values = new String[] {"1", "1.2", "1.23", "1.234", "1.2345", "-1", "-2.3", "-3.4", "-4.5", "0"};
    Decimal[] decimals = Stream.of(values).map(str -> Decimal.of(str)).toArray(Decimal[]::new);
    BigDecimal[] bigDecimals = Stream.of(values).map(str -> new BigDecimal(str)).toArray(BigDecimal[]::new);
    for (int i = 0; i < 1000000; i++) {
      total += bigDecimals[i % 10].setScale(1, RoundingMode.HALF_UP).longValue();
    }
    for (int i = 0; i < 1000000; i++) {
      total += decimals[i % 10].roundToScale(1, RoundingMode.HALF_UP).longValue();
    }
    long nano1 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += bigDecimals[i % 10].setScale(1, RoundingMode.HALF_UP).longValue();
    }
    long nano2 = System.nanoTime();
    for (int i = 0; i < 50000000; i++) {
      total += decimals[i % 10].roundToScale(1, RoundingMode.HALF_UP).longValue();
    }
    long nano3 = System.nanoTime();
    System.out.println(total);
    System.out.println(((nano2 - nano1) / 1000000) + "ns (BigDecimal)");
    System.out.println(((nano3 - nano2) / 1000000) + "ns (Decimal)");
  }

}
