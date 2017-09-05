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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;

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
  // time formats
  private static final DateTimeFormatter HH_MM_SS_COLON = new DateTimeFormatterBuilder()
      .appendValue(HOUR_OF_DAY, 2)
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
    if (IborIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return IborIndex.of(reference);

    } else if (OvernightIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return OvernightIndex.of(reference);

    } else if (PriceIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return PriceIndex.of(reference);

    } else if (FxIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return FxIndex.of(reference);

    } else {
      throw new IllegalArgumentException(Messages.format("No index found for reference: {}", reference));
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
   * Parses a double from the input string.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws NumberFormatException if the string cannot be parsed
   */
  public static double parseDouble(String str) {
    return new BigDecimal(str).doubleValue();
  }

  /**
   * Parses a double from the input string, converting it from a percentage to a decimal values.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws NumberFormatException if the string cannot be parsed
   */
  public static double parseDoublePercent(String str) {
    return new BigDecimal(str).movePointLeft(2).doubleValue();
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

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private LoaderUtils() {
  }

}
