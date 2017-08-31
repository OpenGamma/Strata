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
  private static final DateTimeFormatter DD_MM_YY_SLASH = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH);
  private static final DateTimeFormatter DD_MM_YYYY_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter YYYY_MM_DD_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH);
  private static final DateTimeFormatter YYYY_MM_DD_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
  private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);
  private static final DateTimeFormatter D_MMM_YYYY_DASH =
      new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH);
  private static final DateTimeFormatter D_MMM_YYYY_NODASH =
      new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dMMMyyyy").toFormatter(Locale.ENGLISH);
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
        throw new IllegalArgumentException("Unknown boolean value, must 'True' or 'False' but was '" + str + "'");
    }
  }

  /**
   * Parses a double from the input string.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static double parseDouble(String str) {
    return new BigDecimal(str).doubleValue();
  }

  /**
   * Parses a double from the input string, converting it from a percentage to a decimal values.
   * 
   * @param str  the string to parse
   * @return the parsed value
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static double parseDoublePercent(String str) {
    return new BigDecimal(str).movePointLeft(2).doubleValue();
  }

  /**
   * Parses a date from the input string.
   * <p>
   * Parsing is case insensitive.
   * It accepts formats 'yyyy-MM-dd', 'yyyy/MM/dd', 'dd/MM/yy', 'dd/MM/yyyy', 'd-MMM-yyyy', 'yyyyMMdd', 'dMMMyyyy'.
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
      // yyyy/MM/dd
      if (str.length() == 10 && str.charAt(4) == '/' && str.charAt(7) == '/') {
        return LocalDate.parse(str, YYYY_MM_DD_SLASH);
      }
      // dd/MM/yy
      // dd/MM/yyyy
      if (str.length() >= 8 && str.charAt(2) == '/' && str.charAt(5) == '/') {
        if (str.length() == 8) {
          return LocalDate.parse(str, DD_MM_YY_SLASH);
        } else {
          return LocalDate.parse(str, DD_MM_YYYY_SLASH);
        }
      }
      // d-MMM-yyyy
      if (str.length() >= 10 && str.charAt(str.length() - 5) == '-') {
        return LocalDate.parse(str, D_MMM_YYYY_DASH);
      }
      if (str.length() == 8 && Character.isDigit(str.charAt(2))) {
        // yyyyMMdd (and all others)
        return LocalDate.parse(str, YYYYMMDD);
      }
      // dMMMyyyy (and all others)
      return LocalDate.parse(str, D_MMM_YYYY_NODASH);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          "Unknown date format, must be formatted as " +
              "yyyy-MM-dd, yyyyMMdd, dd/MM/yyyy, yyyy/MM/dd, 'd-MMM-yyyy' or 'dMMMyyyy' but was: " + str,
          ex);
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
          "Unknown date format, must be formatted as " +
              "yyyy-MM-dd, yyyyMMdd, dd/MM/yyyy, yyyy/MM/dd, 'd-MMM-yyyy' or 'dMMMyyyy' but was: " + str,
          ex);
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
        throw new IllegalArgumentException("Unknown BuySell value, must 'Buy' or 'Sell' but was '" + str + "'");
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
        throw new IllegalArgumentException("Unknown PayReceive value, must 'Pay' or 'Receive' but was '" + str + "'");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private LoaderUtils() {
  }

}
