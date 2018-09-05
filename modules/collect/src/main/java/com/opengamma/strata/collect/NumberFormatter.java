/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Provides the ability to parse and format numbers.
 * <p>
 * This exists as an alternative to {@link NumberFormat} and {@link DecimalFormat}
 * which are not thread-safe.
 * <p>
 * Instances of this class are immutable and thread-safe.
 */
public final class NumberFormatter {

  /**
   * The underlying format.
   */
  private final ThreadLocal<NumberFormat> underlying;

  /**
   * Restricted constructor.
   */
  private NumberFormatter(NumberFormat format) {
    ArgChecker.notNull(format, "format");
    format.setParseIntegerOnly(false);
    this.underlying = ThreadLocal.withInitial(() -> (NumberFormat) format.clone());
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a standard formatter configured by grouping and decimal places.
   * <p>
   * The formatter will have the specified number of decimal places.
   * The integer part will be grouped if the flag is set.
   * The decimal part will never be grouped or truncated.
   * The implementation uses English locale data, which uses commas as a separator and a decimal point (dot).
   * Numbers will be rounded using {@link RoundingMode#HALF_EVEN}
   *
   * @param grouped  true to group, false to not group
   * @param decimalPlaces  the minimum number of decimal places, from 0 to 9
   * @return the formatter
   * @throws IllegalArgumentException if the decimal places is invalid
   */
  public static NumberFormatter of(boolean grouped, int decimalPlaces) {
    return of(grouped, decimalPlaces, decimalPlaces);
  }

  /**
   * Obtains a standard formatter configured by grouping and decimal places.
   * <p>
   * The formatter will have the specified number of decimal places.
   * The integer part will be grouped if the flag is set.
   * The decimal part will never be grouped or truncated.
   * The implementation uses English locale data, which uses commas as a separator and a decimal point (dot).
   * Numbers will be rounded using {@link RoundingMode#HALF_EVEN}
   *
   * @param grouped  true to group, false to not group
   * @param minDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @param maxDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @return the formatter
   * @throws IllegalArgumentException if the decimal places is invalid
   */
  public static NumberFormatter of(boolean grouped, int minDecimalPlaces, int maxDecimalPlaces) {
    ArgChecker.inRangeInclusive(minDecimalPlaces, 0, 9, "minDecimalPlaces");
    ArgChecker.inRangeInclusive(maxDecimalPlaces, 0, 9, "maxDecimalPlaces");
    ArgChecker.isTrue(minDecimalPlaces <= maxDecimalPlaces, "Expected minDecimalPlaces <= maxDecimalPlaces");
    return create(grouped, minDecimalPlaces, maxDecimalPlaces);
  }

  // creates an instance ignoring the cache
  private static NumberFormatter create(boolean grouped, int minDecimalPlaces, int maxDecimalPlaces) {
    NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
    format.setGroupingUsed(grouped);
    format.setMinimumIntegerDigits(1);
    format.setMinimumFractionDigits(minDecimalPlaces);
    format.setMaximumFractionDigits(maxDecimalPlaces);
    return new NumberFormatter(format);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a formatter for decimal percentages configured by grouping and decimal places.
   * <p>
   * The formatter will have the specified number of decimal places.
   * The integer part will be grouped if the flag is set.
   * The decimal part will never be grouped or truncated.
   * The implementation uses English locale data, which uses commas as a separator and a decimal point (dot).
   * The formatter will suffix the output with '%'.
   * Numbers will be rounded using {@link RoundingMode#HALF_EVEN}
   * <p>
   * The number passed in must be the decimal representation of the percentage.
   * It will be multiplied by 100 before formatting.
   *
   * @param grouped  true to group, false to not group
   * @param minDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @param maxDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @return the formatter
   * @throws IllegalArgumentException if the decimal places is invalid
   */
  public static NumberFormatter ofPercentage(boolean grouped, int minDecimalPlaces, int maxDecimalPlaces) {
    ArgChecker.inRangeInclusive(minDecimalPlaces, 0, 9, "minDecimalPlaces");
    ArgChecker.inRangeInclusive(maxDecimalPlaces, 0, 9, "maxDecimalPlaces");
    ArgChecker.isTrue(minDecimalPlaces <= maxDecimalPlaces, "Expected minDecimalPlaces <= maxDecimalPlaces");
    NumberFormat format = NumberFormat.getPercentInstance(Locale.ENGLISH);
    format.setGroupingUsed(grouped);
    format.setMinimumIntegerDigits(1);
    format.setMinimumFractionDigits(minDecimalPlaces);
    format.setMaximumFractionDigits(maxDecimalPlaces);
    return new NumberFormatter(format);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a formatter based on a pattern in the specified locale.
   *
   * @param pattern  the pattern string to use
   * @param locale  the locale to use
   * @return the formatter
   * @throws IllegalArgumentException if the pattern is invalid
   * @see DecimalFormat
   */
  public static NumberFormatter ofPattern(String pattern, Locale locale) {
    ArgChecker.notNull(pattern, "pattern");
    ArgChecker.notNull(locale, "locale");
    return new NumberFormatter(new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)));
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a number formatter for general-purpose use in the specified locale.
   *
   * @param locale  the locale to use
   * @return the formatter
   * @see NumberFormat#getNumberInstance(Locale)
   */
  public static NumberFormatter ofLocalizedNumber(Locale locale) {
    ArgChecker.notNull(locale, "locale");
    return new NumberFormatter(NumberFormat.getInstance(locale));
  }

  //-------------------------------------------------------------------------
  /**
   * Formats a {@code double} using this formatter.
   *
   * @param number  the number to format
   * @return the formatted string
   */
  public String format(double number) {
    return underlying.get().format(number);
  }

  /**
   * Formats a {@code long} using this formatter.
   *
   * @param number  the number to format
   * @return the formatted string
   */
  public String format(long number) {
    return underlying.get().format(number);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the specific string, returning a double.
   *
   * @param text  the string to parse
   * @return the parsed number
   * @throws IllegalArgumentException if the text cannot be parsed
   */
  public double parse(String text) {
    try {
      return underlying.get().parse(text).doubleValue();
    } catch (ParseException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string representation of this formatter.
   * 
   * @return the string
   */
  @Override
  public String toString() {
    return underlying.toString();
  }

}
