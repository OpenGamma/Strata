/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.math.BigDecimal;
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
import java.util.Locale;
import java.util.Optional;

import com.google.common.base.CharMatcher;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.PutCall;

/**
 * Contains utilities for loading market data from input files.
 */
public final class LoaderUtils {

  /**
   * Default scheme for trades.
   */
  public static final String DEFAULT_TRADE_SCHEME = "OG-Trade";
  /**
   * Default scheme for positions.
   */
  public static final String DEFAULT_POSITION_SCHEME = "OG-Position";
  /**
   * Default scheme for securities.
   */
  public static final String DEFAULT_SECURITY_SCHEME = "OG-Security";

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
    return Index.of(reference);
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
   * @throws IllegalArgumentException if the string cannot be parsed
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
        throw new IllegalArgumentException(
            "Unknown BuySell value, must be 'True' or 'False' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'T', 'Yes', 'Y', 'F', 'No' and 'N'");
    }
  }

  /**
   * Parses an integer from the input string.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws NumberFormatException if the string cannot be parsed
   */
  public static int parseInteger(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      NumberFormatException nfex = new NumberFormatException("Unable to parse integer from '" + str + "'");
      nfex.initCause(ex);
      throw nfex;
    }
  }

  /**
   * Parses a double from the input string.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws NumberFormatException if the string cannot be parsed
   */
  public static double parseDouble(String str) {
    try {
      return new BigDecimal(str).doubleValue();
    } catch (NumberFormatException ex) {
      NumberFormatException nfex = new NumberFormatException("Unable to parse double from '" + str + "'");
      nfex.initCause(ex);
      throw nfex;
    }
  }

  /**
   * Parses a double from the input string, converting it from a percentage to a decimal values.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws NumberFormatException if the string cannot be parsed
   */
  public static double parseDoublePercent(String str) {
    try {
      return new BigDecimal(str).movePointLeft(2).doubleValue();
    } catch (NumberFormatException ex) {
      NumberFormatException nfex = new NumberFormatException("Unable to parse percentage from '" + str + "'");
      nfex.initCause(ex);
      throw nfex;
    }
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
   * @throws IllegalArgumentException if the string cannot be parsed
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
      throw new IllegalArgumentException(
          "Unknown date format, must be formatted as 'yyyy-MM-dd', 'yyyyMMdd', 'yyyy/M/d', 'd/M/yyyy', " +
              "'d-MMM-yyyy', 'dMMMyyyy', 'd/M/yy', 'd-MMM-yy' or 'dMMMyy' but was: " + str);
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
   * @throws IllegalArgumentException if the string cannot be parsed
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
        throw new IllegalArgumentException("Found Excel-style date but day-of-month was not set to 1:" + str);
      }
      // yyyyMM
      return YearMonth.parse(str, YYYYMM);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          "Unknown date format, must be formatted as 'yyyy-MM', 'yyyyMM', " +
              "'MMM-yyyy', 'MMMyyyy', 'MMM-yy' or 'MMMyy' but was: " + str);
    }
  }

  /**
   * Parses time from the input string.
   * <p>
   * It accepts formats 'HH[:mm[:ss.SSS]]'.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static LocalTime parseTime(String str) {
    try {
      return LocalTime.parse(str, HH_MM_SS_COLON);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          "Unknown time format, must be formatted as 'HH', 'HH:mm', 'HH:mm:ss' or 'HH:mm:ss.SSS' but was: " + str);
    }
  }

  /**
   * Parses time-zone from the input string.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static ZoneId parseZoneId(String str) {
    try {
      return ZoneId.of(str);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Unknown time-zone, was: " + str);
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
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static Period parsePeriod(String str) {
    try {
      String prefixed = str.startsWith("P") ? str : "P" + str;
      return Period.parse(prefixed);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Unknown period format: " + str);
    }
  }

  /**
   * Parses a tenor from the input string.
   * <p>
   * A tenor cannot be zero or negative.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static Tenor parseTenor(String str) {
    try {
      return Tenor.parse(str);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Unknown tenor format: " + str);
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
    if (str != null && str.length() > 1) {
      try {
        return Optional.of(Tenor.parse(str));
      } catch (RuntimeException ex) {
        // ignore
      }
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
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static Currency parseCurrency(String str) {
    try {
      return Currency.parse(str);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          "Unknown Currency, must be 3 letter ISO-4217 format but was '" + str + "'");
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
      try {
        return Optional.of(Currency.parse(str));
      } catch (RuntimeException ex) {
        // ignore
      }
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
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static DayCount parseDayCount(String str) {
    return DayCount.extendedEnum().findLenient(str)
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown DayCount value, must be one of " +
                DayCount.extendedEnum().lookupAllNormalized().keySet() +
                " but was '" + str + "'"));
  }

  /**
   * Parses business day convention from the input string.
   * <p>
   * Parsing is case insensitive.
   * It leniently handles a variety of known variants of business day conventions.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static BusinessDayConvention parseBusinessDayConvention(String str) {
    return BusinessDayConvention.extendedEnum().findLenient(str)
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown BusinessDayConvention value, must be one of " +
                BusinessDayConvention.extendedEnum().lookupAllNormalized().keySet() +
                " but was '" + str + "'"));
  }

  /**
   * Parses roll convention from the input string.
   * <p>
   * Parsing is case insensitive.
   * It leniently handles a variety of known variants of roll conventions.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static RollConvention parseRollConvention(String str) {
    return RollConvention.extendedEnum().findLenient(str)
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown RollConvention value, must be one of " +
                RollConvention.extendedEnum().lookupAllNormalized().keySet() +
                " but was '" + str + "'"));
  }

  /**
   * Parses buy/sell from the input string.
   * <p>
   * Parsing is case insensitive.
   * Buy is parsed as 'BUY', 'B'.
   * Sell is parsed as 'SELL', 'S'.
   * Other strings are rejected.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
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
        throw new IllegalArgumentException(
            "Unknown BuySell value, must be 'Buy' or 'Sell' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'B' and 'S'");
    }
  }

  /**
   * Parses pay/receive from the input string.
   * <p>
   * Parsing is case insensitive.
   * Pay is parsed as 'PAY', 'P'.
   * Receive is parsed as 'RECEIVE', 'REC', 'R'.
   * Other strings are rejected.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
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
        throw new IllegalArgumentException(
            "Unknown PayReceive value, must be 'Pay' or 'Receive' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'P', 'Rec' and 'R'");
    }
  }

  /**
   * Parses put/call from the input string.
   * <p>
   * Parsing is case insensitive.
   * Put is parsed as 'PUT', 'P'.
   * Call is parsed as 'CALL', 'C'.
   * Other strings are rejected.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
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
        throw new IllegalArgumentException(
            "Unknown PutCall value, must be 'Put' or 'Call' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'P' and 'C'");
    }
  }

  /**
   * Parses long/short from the input string.
   * <p>
   * Parsing is case insensitive.
   * Long is parsed as 'LONG', 'L'.
   * Short is parsed as 'SHORT', 'S'.
   * Other strings are rejected.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
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
        throw new IllegalArgumentException(
            "Unknown LongShort value, must be 'Long' or 'Short' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'L' and 'S'");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private LoaderUtils() {
  }

}
