/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import static com.opengamma.strata.collect.Guavate.tryCatchToOptional;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.math.BigDecimal;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.MarketTenor;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.BasisPoints;
import com.opengamma.strata.collect.Decimal;
import com.opengamma.strata.collect.Percentage;
import com.opengamma.strata.collect.result.ParseFailureException;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.CapFloor;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;

/**
 * Contains utilities for loading market data from input files.
 */
public final class LoaderUtils {
  // in most cases, the underlying cause is not included in the ParseFailureException (because it isn't interesting)

  /**
   * Default scheme for trades.
   */
  public static final String DEFAULT_TRADE_SCHEME = StandardSchemes.OG_TRADE_SCHEME;
  /**
   * Default scheme for positions.
   */
  public static final String DEFAULT_POSITION_SCHEME = StandardSchemes.OG_POSITION_SCHEME;
  /**
   * Default scheme for securities.
   */
  public static final String DEFAULT_SECURITY_SCHEME = StandardSchemes.OG_SECURITY_SCHEME;

  // date formats
  private static final DateTimeFormatter D_M_YEAR_SLASH = new DateTimeFormatterBuilder()
      .appendPattern("d/M/")
      .parseLenient()
      .appendValueReduced(ChronoField.YEAR_OF_ERA, 2, 2, 2000)
      .toFormatter(Locale.ENGLISH);
  private static final DateTimeFormatter YYYY_M_D_SLASH = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.ENGLISH);
  private static final DateTimeFormatter YYYY_MM_DD_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
  private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);
  private static final DateTimeFormatter D_MMM_YEAR_DASH = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("d-MMM-")
      .parseLenient()
      .appendValueReduced(ChronoField.YEAR_OF_ERA, 2, 2, 2000)
      .toFormatter(Locale.ENGLISH);
  private static final DateTimeFormatter D_MMM_YEAR_NODASH = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("dMMM")
      .parseLenient()
      .appendValueReduced(ChronoField.YEAR_OF_ERA, 2, 2, 2000)
      .toFormatter(Locale.ENGLISH);
  // year-month formats
  private static final DateTimeFormatter YYYY_MM_DASH = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH);
  private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM", Locale.ENGLISH);
  private static final DateTimeFormatter MMM_YEAR = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("MMM")
      .optionalStart()
      .appendLiteral('-')
      .optionalEnd()
      .parseLenient()
      .appendValueReduced(ChronoField.YEAR_OF_ERA, 2, 2, 2000)
      .toFormatter(Locale.ENGLISH);
  // time formats
  private static final DateTimeFormatter HH_MM_SS_COLON = new DateTimeFormatterBuilder()
      .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NEVER)
      .optionalStart()
      .appendLiteral(':')
      .appendValue(MINUTE_OF_HOUR, 2)
      .optionalStart()
      .appendLiteral(':')
      .appendValue(SECOND_OF_MINUTE, 2)
      .optionalStart()
      .appendFraction(NANO_OF_SECOND, 0, 9, true)
      .toFormatter(Locale.ENGLISH)
      .withResolverStyle(ResolverStyle.STRICT);
  // match a currency
  private static final CharMatcher CURRENCY_MATCHER = CharMatcher.inRange('A', 'Z');

  private static final Splitter DOT_SPLITTER = Splitter.on('.');

  //-------------------------------------------------------------------------
  /**
   * Attempts to locate a rate index by reference name.
   * <p>
   * This utility searches {@link IborIndex}, {@link OvernightIndex}, {@link FxIndex}
   * and {@link PriceIndex}.
   *
   * @param reference  the reference name
   * @return the resolved rate index
   */
  public static Index findIndex(String reference) {
    try {
      return Index.of(reference);
    } catch (RuntimeException ex) {
      throw new ParseFailureException("Unable to parse index from '{value}'", reference);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a boolean from the input string.
   * <p>
   * Parsing is case insensitive.
   * True is parsed as 'TRUE', 'T', 'YES', 'Y'.
   * False is parsed as 'FALSE', 'F', 'NO', 'N'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static boolean parseBoolean(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "TRUE":
      case "T":
      case "YES":
      case "Y":
        return true;
      case "FALSE":
      case "F":
      case "NO":
      case "N":
        return false;
      default:
        throw new ParseFailureException(
            "Unable to parse boolean from '{value}', must be one of {options}",
            str,
            "'True', 'False', 'T', 'F', 'Yes', 'No', 'Y' or 'N' (case insensitive)");
    }
  }

  /**
   * Parses an integer from the input string.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * Comma separated values will be parsed assuming American decimal format. Values in European decimal formats
   * (thus "12456789" formatted as "12.456.789") will not be parsed.
   * <p>
   * For example "12,300" and "12300" will be parsed as integer "12300" and similarly "(12,300)",
   * "-12,300" and "-12300" will be parsed as integer "-12300".
   * <p>
   * Note: Comma separated values such as "1,234,5,6" will be parsed as integer "123456".
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static int parseInteger(String str) {
    try {
      return Integer.parseInt(normalize(str));
    } catch (NumberFormatException ex) {
      throw new ParseFailureException("Unable to parse integer from '{value}'", str);
    }
  }

  /**
   * Parses a double from the input string.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * Comma separated values will be parsed assuming American decimal format. Values in European decimal formats
   * (thus "12456789.444" formatted as "12.456.789,444") will not be parsed.
   * <p>
   * For example "12,300.12" and "12300.12" will be parsed as "12300.12d" and similarly "(12,300.12)",
   * "-12,300.12" and "-12300.12" will be parsed as "-12300.12d".
   * <p>
   * Note: Comma separated values such as "1,234,5,6.12" will be parsed as "123456.12d".
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static double parseDouble(String str) {
    try {
      return parseDecimal(str).doubleValue();
    } catch (ParseFailureException ex) {
      throw new ParseFailureException("Unable to parse double from '{value}'", str);
    }
  }

  /**
   * Parses a double from the input string, converting it from a percentage to a decimal values.
   * <p>
   * If input value is bracketed, it will be parsed as a negative decimal percentage.
   * For example '(12.34)' will be parsed as -0.1234d.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static double parseDoublePercent(String str) {
    try {
      return parseDecimalPercent(str).doubleValue();
    } catch (ParseFailureException ex) {
      throw new ParseFailureException("Unable to parse percentage from '{value}'", str);
    }
  }

  /**
   * Parses a decimal from the input string.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * Comma separated values will be parsed assuming American decimal format. Values in European decimal formats
   * (thus "12456789.444" formatted as "12.456.789,444") will not be parsed.
   * <p>
   * For example "12,300.12" and "12300.12" will be parsed as big decimal "12300.12" and similarly "(12,300.12)",
   * "-12,300.12" and "-12300.12" will be parsed as big decimal "-12300.12".
   * <p>
   * Note: Comma separated values such as "1,234,5,6.12" will be parsed as big decimal "123456.12".
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BigDecimal parseBigDecimal(String str) {
    try {
      return new BigDecimal(normalize(str));
    } catch (NumberFormatException ex) {
      throw new ParseFailureException("Unable to parse decimal from '{value}'", str);
    }
  }

  /**
   * Parses a decimal from the input string, converting it from a percentage to decimal form.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * For example '(12.3456789)' will be parsed as a big decimal -0.123456789.
   *
   * @param str  the string to parse
   * @return the parsed value in decimal form
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BigDecimal parseBigDecimalPercent(String str) {
    try {
      return parseBigDecimal(str).movePointLeft(2);
    } catch (ParseFailureException ex) {
      throw new ParseFailureException("Unable to parse decimal percentage from '{value}'", str);
    }
  }

  /**
   * Parses a decimal from the input string, converting it from a basis points to decimal form.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * For example '(12.3456789)' will be parsed as a big decimal -0.00123456789.
   *
   * @param str  the string to parse
   * @return the parsed value in decimal form
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BigDecimal parseBigDecimalBasisPoint(String str) {
    try {
      return parseBigDecimal(str).movePointLeft(4);
    } catch (ParseFailureException ex) {
      throw new ParseFailureException("Unable to parse decimal basis points from '{value}'", str);
    }
  }

  /**
   * Parses a decimal from the input string.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * Comma separated values will be parsed assuming American decimal format. Values in European decimal formats
   * (thus "12456789.444" formatted as "12.456.789,444") will not be parsed.
   * <p>
   * For example "12,300.12" and "12300.12" will be parsed as big decimal "12300.12" and similarly "(12,300.12)",
   * "-12,300.12" and "-12300.12" will be parsed as big decimal "-12300.12".
   * <p>
   * Note: Comma separated values such as "1,234,5,6.12" will be parsed as big decimal "123456.12".
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Decimal parseDecimal(String str) {
    try {
      return Decimal.of(normalize(str));
    } catch (IllegalArgumentException ex) {
      throw new ParseFailureException("Unable to parse decimal from '{value}'", str);
    }
  }

  /**
   * Parses a decimal from the input string, converting it from a percentage to decimal form.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * For example '(12.3456789)' will be parsed as a big decimal -0.123456789.
   *
   * @param str  the string to parse
   * @return the parsed value in decimal form
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Decimal parseDecimalPercent(String str) {
    try {
      return parseDecimal(str).movePoint(-2);
    } catch (ParseFailureException ex) {
      throw new ParseFailureException("Unable to parse decimal percentage from '{value}'", str);
    }
  }

  /**
   * Parses a decimal from the input string, converting it from a basis points to decimal form.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * For example '(12.3456789)' will be parsed as a big decimal -0.00123456789.
   *
   * @param str  the string to parse
   * @return the parsed value in decimal form
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Decimal parseDecimalBasisPoint(String str) {
    try {
      return parseDecimal(str).movePoint(-4);
    } catch (ParseFailureException ex) {
      throw new ParseFailureException("Unable to parse decimal basis points from '{value}'", str);
    }
  }

  /**
   * Parses a percentage from the input string.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * For example. '(12.3456789)' will be parsed as '-12.3456789%'.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Percentage parsePercentage(String str) {
    try {
      return Percentage.parse(normalize(str));
    } catch (IllegalArgumentException ex) {
      throw new ParseFailureException("Unable to parse percentage from '{value}'", str);
    }
  }

  /**
   * Parses basis points from the input string.
   * <p>
   * If input value is bracketed, it will be parsed as a negative value.
   * For example. '(12.3456789)' will be parsed as '12.3456789bps'.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BasisPoints parseBasisPoints(String str) {
    try {
      return BasisPoints.parse(normalize(str));
    } catch (IllegalArgumentException ex) {
      throw new ParseFailureException("Unable to parse basis points from '{value}'", str);
    }
  }

  private static String normalize(String value) {
    String normalizedValue = value.trim();
    normalizedValue = normalizeIfBracketed(normalizedValue);
    normalizedValue = normalizeIfCommaSeparated(normalizedValue);
    return normalizedValue;
  }

  private static String normalizeIfBracketed(String value) {
    if (value.length() > 2 && value.startsWith("(") && value.endsWith(")")) {
      String valueWithoutBrackets = value.substring(1, value.length() - 1);
      return "-" + valueWithoutBrackets; // prepends the negative sign
    } else {
      return value;
    }
  }

  private static String normalizeIfCommaSeparated(String value) {
    if (!value.startsWith(",") && !value.startsWith("-,") && !value.endsWith(",") && !value.contains(",,")) {
      List<String> parts = ImmutableList.copyOf(DOT_SPLITTER.split(value));
      // ensure we only deal with american decimal format
      if (parts.size() == 1 || (parts.size() == 2 && !parts.get(1).contains(","))) {
        return value.replace(",", "");
      }
    }
    return value; // incorrectly formatted values or values in european decimal formats will not be normalized
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a date from the input string using the specified formatters.
   * <p>
   * Each formatter is tried in order.
   *
   * @param str  the string to parse
   * @param formatters  the date formats
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static LocalDate parseDate(String str, DateTimeFormatter... formatters) {
    ArgChecker.notEmpty(formatters, "formatters");
    for (DateTimeFormatter formatter : formatters) {
      try {
        ParsePosition pp = new ParsePosition(0);
        formatter.parseUnresolved(str, pp);
        int len = str.length();
        if (pp.getErrorIndex() == -1 && pp.getIndex() == len) {
          return formatter.parse(str, LocalDate::from);
        }
      } catch (RuntimeException ex) {
        // should not happen, but ignore if it does
      }
    }
    throw new ParseFailureException("Unable to parse date from '{value}'", str);
  }

  /**
   * Parses a date from the input string.
   * <p>
   * Parsing is case insensitive.
   * It accepts formats 'yyyy-MM-dd', 'yyyyMMdd', 'yyyy/M/d', 'd/M/yyyy', 'd-MMM-yyyy' or 'dMMMyyyy'.
   * Some formats also accept two-digits years (use is not recommended): 'd/M/yy', 'd-MMM-yy' or 'dMMMyy'.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static LocalDate parseDate(String str) {
    try {
      // yyyy-MM-dd
      if (str.length() == 10 && str.charAt(4) == '-' && str.charAt(7) == '-') {
        return LocalDate.parse(str, YYYY_MM_DD_DASH);
      }
      // yyyy/M/d
      if (str.length() >= 8 && str.charAt(4) == '/') {
        return LocalDate.parse(str, YYYY_M_D_SLASH);
      }
      // d/M/yy
      // d/M/yyyy
      if (str.length() >= 6 && (str.charAt(1) == '/' || str.charAt(2) == '/')) {
        return LocalDate.parse(str, D_M_YEAR_SLASH);
      }
      // d-MMM-yy
      // d-MMM-yyyy
      if (str.length() >= 8 && (str.charAt(1) == '-' || str.charAt(2) == '-')) {
        return LocalDate.parse(str, D_MMM_YEAR_DASH);
      }
      // yyyyMMdd
      if (str.length() == 8 && Character.isDigit(str.charAt(2))) {
        return LocalDate.parse(str, YYYYMMDD);
      }
      // dMMMyy
      // dMMMyyyy
      return LocalDate.parse(str, D_MMM_YEAR_NODASH);

    } catch (DateTimeParseException ex) {
      throw new ParseFailureException(
          ex,
          "Unable to parse date from '{value}', must be formatted as " +
              "'yyyy-MM-dd', 'yyyyMMdd', 'yyyy/M/d', 'd/M/yyyy', 'd-MMM-yyyy', 'dMMMyyyy', 'd/M/yy', 'd-MMM-yy' or 'dMMMyy'",
          str);
    }
  }

  /**
   * Parses a year-month from the input string.
   * <p>
   * Parsing is case insensitive.
   * It accepts formats 'yyyy-MM', 'yyyyMM', 'MMM-yyyy' or 'MMMyyyy'.
   * Some formats also accept two-digits years (use is not recommended): 'MMM-yy' or 'MMMyy'.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static YearMonth parseYearMonth(String str) {
    try {
      // yyyy-MM
      if (str.length() == 7 && str.charAt(4) == '-') {
        return YearMonth.parse(str, YYYY_MM_DASH);
      }
      // MMM-yy
      // MMM-yyyy
      // MMMyy
      // MMMyyyy
      if (str.length() >= 5 && !Character.isDigit(str.charAt(0))) {
        return YearMonth.parse(str, MMM_YEAR);
      }
      // d/M/yyyy - handle Excel converting YearMonth to date
      if (str.length() >= 8 && (str.charAt(1) == '/' || str.charAt(2) == '/')) {
        LocalDate date = LocalDate.parse(str, D_M_YEAR_SLASH);
        if (date.getDayOfMonth() == 1) {
          return YearMonth.of(date.getYear(), date.getMonth());
        }
        throw new ParseFailureException(
            "Unable to parse year-month from '{value}', found Excel-style date but day-of-month was not set to 1", str);
      }
      // yyyyMM
      return YearMonth.parse(str, YYYYMM);

    } catch (DateTimeParseException ex) {
      throw new ParseFailureException(
          "Unable to parse year-month from '{value}', must be formatted as " +
              "'yyyy-MM', 'yyyyMM', 'MMM-yyyy', 'MMMyyyy', 'MMM-yy' or 'MMMyy'",
          str);
    }
  }

  /**
   * Parses time from the input string.
   * <p>
   * It accepts formats 'HH[:mm[:ss.SSS]]'.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static LocalTime parseTime(String str) {
    try {
      return LocalTime.parse(str, HH_MM_SS_COLON);

    } catch (DateTimeParseException ex) {
      throw new ParseFailureException(
          "Unable to parse time from '{value}', must be formatted as 'HH', 'HH:mm', 'HH:mm:ss' or 'HH:mm:ss.SSS'", str);
    }
  }

  /**
   * Parses time-zone from the input string.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static ZoneId parseZoneId(String str) {
    try {
      return ZoneId.of(str);

    } catch (DateTimeParseException ex) {
      throw new ParseFailureException("Unable to parse time-zone from '{value}'", str);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a period from the input string.
   * <p>
   * It accepts the same formats as {@link Period}, but the "P" at the start is optional.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Period parsePeriod(String str) {
    try {
      String prefixed = str.startsWith("P") ? str : "P" + str;
      return Period.parse(prefixed);

    } catch (DateTimeParseException ex) {
      throw new ParseFailureException("Unable to parse period from '{value}'", str);
    }
  }

  /**
   * Tries to parse a period from the input string.
   *
   * @param str  the string to parse, may be null
   * @return the parsed period, empty if unable to parse
   */
  public static Optional<Period> tryParsePeriod(String str) {
    if (str != null && str.length() >= 2) {
      return tryCatchToOptional(() -> parsePeriod(str));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a market tenor from the input string.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static MarketTenor parseMarketTenor(String str) {
    try {
      return MarketTenor.parse(str);

    } catch (RuntimeException ex) {
      throw new ParseFailureException("Unable to parse market tenor from '{value}'", str);
    }
  }

  /**
   * Tries to parse a market tenor from the input string.
   *
   * @param str  the string to parse, may be null
   * @return the parsed market tenor, empty if unable to parse
   */
  public static Optional<MarketTenor> tryParseMarketTenor(String str) {
    if (str != null && str.length() >= 2) {
      return tryCatchToOptional(() -> MarketTenor.parse(str));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a tenor from the input string.
   * <p>
   * A tenor cannot be zero or negative.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Tenor parseTenor(String str) {
    try {
      return Tenor.parse(str);

    } catch (RuntimeException ex) {
      throw new ParseFailureException("Unable to parse tenor from '{value}'", str);
    }
  }

  /**
   * Tries to parse a tenor from the input string.
   * <p>
   * Parsing is case insensitive.
   *
   * @param str  the string to parse, may be null
   * @return the parsed tenor, empty if unable to parse
   */
  public static Optional<Tenor> tryParseTenor(String str) {
    if (str != null && str.length() >= 2) {
      return tryCatchToOptional(() -> Tenor.parse(str));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a frequency from the input string.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Frequency parseFrequency(String str) {
    try {
      return Frequency.parse(str);

    } catch (RuntimeException ex) {
      throw new ParseFailureException("Unable to parse frequency from '{value}'", str);
    }
  }

  /**
   * Tries to parse a frequency from the input string.
   *
   * @param str  the string to parse, may be null
   * @return the parsed frequency, empty if unable to parse
   */
  public static Optional<Frequency> tryParseFrequency(String str) {
    if (str != null && !str.isEmpty()) {
      return tryCatchToOptional(() -> Frequency.parse(str));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Parses currency from the input string.
   * <p>
   * Parsing is case insensitive.
   *
   * @param str  the string to parse
   * @return the parsed currency
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static Currency parseCurrency(String str) {
    try {
      return Currency.parse(str);
    } catch (RuntimeException ex) {
      throw new ParseFailureException("Unable to parse currency from '{value}', must be 3 letter ISO-4217 format", str);
    }
  }

  /**
   * Tries to parse a currency from the input string.
   * <p>
   * Parsing is case insensitive.
   *
   * @param str  the string to parse, may be null
   * @return the parsed currency, empty if unable to parse
   */
  public static Optional<Currency> tryParseCurrency(String str) {
    if (str != null && str.length() == 3 && CURRENCY_MATCHER.matchesAllOf(str)) {
      return tryCatchToOptional(() -> Currency.parse(str));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Parses day count from the input string.
   * <p>
   * Parsing is case insensitive.
   * It leniently handles a variety of known variants of day counts.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static DayCount parseDayCount(String str) {
    return DayCount.extendedEnum().findLenient(str)
        .orElseThrow(() -> new ParseFailureException(
            "Unable to parse day count from '{value}', must be one of {options}",
            str,
            DayCount.extendedEnum().lookupAllNormalized().keySet()));
  }

  /**
   * Parses business day convention from the input string.
   * <p>
   * Parsing is case insensitive.
   * It leniently handles a variety of known variants of business day conventions.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BusinessDayConvention parseBusinessDayConvention(String str) {
    return BusinessDayConvention.extendedEnum().findLenient(str)
        .orElseThrow(() -> new ParseFailureException(
            "Unable to parse business day convention from '{value}', must be one of {options}",
            str,
            BusinessDayConvention.extendedEnum().lookupAllNormalized().keySet()));
  }

  /**
   * Parses roll convention from the input string.
   * <p>
   * Parsing is case insensitive.
   * It leniently handles a variety of known variants of roll conventions.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static RollConvention parseRollConvention(String str) {
    return RollConvention.extendedEnum().findLenient(str)
        .orElseThrow(() -> new ParseFailureException(
            "Unable to parse roll convention from '{value}', must be one of {options}",
            str,
            RollConvention.extendedEnum().lookupAllNormalized().keySet()));
  }

  /**
   * Parses buy/sell from the input string.
   * <p>
   * Parsing is case insensitive.
   * Buy is parsed as 'BUY' or 'B'.
   * Sell is parsed as 'SELL' or 'S'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BuySell parseBuySell(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "BUY":
      case "B":
        return BuySell.BUY;
      case "SELL":
      case "S":
        return BuySell.SELL;
      default:
        throw new ParseFailureException(
            "Unable to parse buy/sell from '{value}', must be one of {options}",
            str,
            "'Buy', 'Sell', 'B', or 'S' (case insensitive)");
    }
  }

  /**
   * Parses cap/floor from the input string.
   * <p>
   * Parsing is case insensitive.
   * Cap is parsed as 'CAP' or 'C'.
   * Floor is parsed as 'FLOOR' or 'F'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException  if the string cannot be parsed
   */
  public static CapFloor parseCapFloor(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "CAP":
      case "C":
        return CapFloor.CAP;
      case "FLOOR":
      case "F":
        return CapFloor.FLOOR;
      default:
        throw new ParseFailureException(
            "Unable to parse cap/floor from '{value}', must be one of {options}",
            str,
            "'Cap', 'Floor', 'C', or 'F' (case insensitive)");
    }
  }

  /**
   * Parses pay/receive from the input string.
   * <p>
   * Parsing is case insensitive.
   * Pay is parsed as 'PAY' or 'P'.
   * Receive is parsed as 'RECEIVE', 'REC' or 'R'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static PayReceive parsePayReceive(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "PAY":
      case "P":
        return PayReceive.PAY;
      case "RECEIVE":
      case "REC":
      case "R":
        return PayReceive.RECEIVE;
      default:
        throw new ParseFailureException(
            "Unable to parse pay/receive from '{value}', must be one of {options}",
            str,
            "'Pay', 'Receive', 'Rec', 'P', or 'R' (case insensitive)");
    }
  }

  /**
   * Parses put/call from the input string.
   * <p>
   * Parsing is case insensitive.
   * Put is parsed as 'PUT' or 'P'.
   * Call is parsed as 'CALL' or 'C'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static PutCall parsePutCall(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "PUT":
      case "P":
        return PutCall.PUT;
      case "CALL":
      case "C":
        return PutCall.CALL;
      default:
        throw new ParseFailureException(
            "Unable to parse put/call from '{value}', must be one of {options}",
            str,
            "'Put', 'Call', 'P', or 'C' (case insensitive)");
    }
  }

  /**
   * Parses long/short from the input string.
   * <p>
   * Parsing is case insensitive.
   * Long is parsed as 'LONG' or 'L'.
   * Short is parsed as 'SHORT' or 'S'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static LongShort parseLongShort(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "LONG":
      case "L":
        return LongShort.LONG;
      case "SHORT":
      case "S":
        return LongShort.SHORT;
      default:
        throw new ParseFailureException(
            "Unable to parse long/short from '{value}', must be one of {options}",
            str,
            "'Long', 'Short', 'L', or 'S' (case insensitive)");
    }
  }

  /**
   * Parses barrier type from the input string.
   * <p>
   * Parsing is case insensitive.
   * Up is parsed as 'UP' or 'U'.
   * Down is parsed as 'DOWN' or 'D'.
   * Other strings are rejected.
   *
   * @param str the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static BarrierType parseBarrierType(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "UP":
      case "U":
        return BarrierType.UP;
      case "DOWN":
      case "D":
        return BarrierType.DOWN;
      default:
        throw new ParseFailureException(
            "Unable to parse up/down from '{value}', must be one of {options}",
            str,
            "'Up', 'Down', 'U', or 'D' (case insensitive)");
    }
  }

  /**
   * Parses knock type from the input string.
   * <p>
   * Parsing is case insensitive.
   * KnockIn is parsed as 'KNOCKIN', 'IN'.
   * KnockOut is parsed as 'KNOCKOUT', 'OUT'.
   * Other strings are rejected.
   *
   * @param str  the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static KnockType parseKnockType(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "KNOCKIN":
      case "IN":
      case "I":
        return KnockType.KNOCK_IN;
      case "KNOCKOUT":
      case "OUT":
      case "O":
        return KnockType.KNOCK_OUT;
      default:
        throw new ParseFailureException(
            "Unable to parse in/out from '{value}', must be one of {options}",
            str,
            "'In', 'Out', 'KnockIn', 'KnockOut', 'I' or 'O' (case insensitive)");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a RED code from the input string.
   * <p>
   * The input string but be 6 or 9 characters long to be valid.
   *
   * @param str the string to parse
   * @return the parsed value
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static StandardId parseRedCode(String str) {
    if (str.length() == 9) {
      return StandardId.of(StandardSchemes.RED9_SCHEME, str);
    } else if (str.length() == 6) {
      return StandardId.of(StandardSchemes.RED6_SCHEME, str);
    } else {
      throw new ParseFailureException("Unable to parse RED code from '{value}', must be 6 or 9 characters long", str);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private LoaderUtils() {
  }

}
