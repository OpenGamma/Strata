/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.ExchangeId;
import com.opengamma.strata.product.common.PutCall;

/**
 * A utility for generating ETD identifiers.
 * <p>
 * An exchange traded derivative (ETD) is uniquely identified by a set of fields.
 * In most cases, these fields should be kept separate, as on {@link EtdContractSpec}.
 * However, it can be useful to create a single identifier from the separate fields.
 * We do not recommend parsing the combined identifier to retrieve individual fields.
 */
public final class EtdIdUtils {

  /**
   * The length of the year-month digit block, "202304", that starts the expiry and option details group.
   * <p>
   * The separator between the contract details and expiry + option details is the last '-' in the
   * identifier that is immediately followed by 6 digits.
   * Example separator "-202304" in "F-IFEN-ABC-202304" or "O-IFEN-ABC-202304-PM12.34-U202309".
   */
  private static final int YEAR_MONTH_DIGITS = 6;

  /**
   * Scheme used for ETDs.
   */
  public static final String ETD_SCHEME = StandardSchemes.OG_ETD_SCHEME;
  /**
   * The separator to use.
   */
  private static final String SEPARATOR = "-";
  /**
   * Prefix for futures.
   */
  private static final String FUT_PREFIX = "F" + SEPARATOR;
  /**
   * Prefix for option.
   */
  private static final String OPT_PREFIX = "O" + SEPARATOR;
  /**
   * The year-month format.
   */
  private static final DateTimeFormatter YM_FORMAT = new DateTimeFormatterBuilder()
      .appendValue(YEAR, 4)
      .appendValue(MONTH_OF_YEAR, 2)
      .toFormatter(Locale.ROOT);

  //-------------------------------------------------------------------------
  /**
   * Creates an identifier for a contract specification.
   * <p>
   * This will have the format:
   * {@code 'OG-ETD~F-ECAG-FGBS'} or {@code 'OG-ETD~O-ECAG-OGBS'}.
   *
   * @param type  type of the contract - future or option
   * @param exchangeId  the MIC code of the exchange where the instruments are traded
   * @param contractCode  the code supplied by the exchange for use in clearing and margining, such as in SPAN
   * @return the identifier
   */
  public static EtdContractSpecId contractSpecId(EtdType type, ExchangeId exchangeId, EtdContractCode contractCode) {
    ArgChecker.notNull(type, "type");
    ArgChecker.notNull(exchangeId, "exchangeId");
    ArgChecker.notNull(contractCode, "contractCode");
    switch (type) {
      case FUTURE:
        return EtdContractSpecId.of(ETD_SCHEME, FUT_PREFIX + exchangeId + SEPARATOR + contractCode);
      case OPTION:
        return EtdContractSpecId.of(ETD_SCHEME, OPT_PREFIX + exchangeId + SEPARATOR + contractCode);
      default:
        throw new IllegalArgumentException("Unknown ETD type: " + type);
    }
  }

  /**
   * Creates an identifier for a contract specification.
   * <p>
   * This will have the format:
   * {@code 'OG-ETD~F-ECAG-FGBS'} or {@code 'OG-ETD~O-ECAG-OGBS'}.
   *
   * @param securityId  the security id
   * @return the identifier
   */
  public static EtdContractSpecId contractSpecId(SecurityId securityId) {
    SplitEtdId splitEtdId = splitId(securityId);
    return contractSpecId(splitEtdId.getType(), splitEtdId.getExchangeId(), splitEtdId.getContractCode());
  }

  /**
   * Creates an identifier for an ETD future instrument.
   * <p>
   * A typical monthly ETD will have the format:
   * {@code 'OG-ETD~F-ECAG-OGBS-201706'}.
   * <p>
   * A more complex flex ETD (12th of the month, Physical settlement) will have the format:
   * {@code 'OG-ETD~F-ECAG-OGBS-20170612E'}.
   *
   * @param exchangeId  the MIC code of the exchange where the instruments are traded
   * @param contractCode  the code supplied by the exchange for use in clearing and margining, such as in SPAN
   * @param expiryMonth  the month of expiry
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex'
   * @return the identifier
   */
  public static SecurityId futureId(
      ExchangeId exchangeId,
      EtdContractCode contractCode,
      YearMonth expiryMonth,
      EtdVariant variant) {

    ArgChecker.notNull(exchangeId, "exchangeId");
    ArgChecker.notNull(contractCode, "contractCode");
    ArgChecker.notNull(expiryMonth, "expiryMonth");
    ArgChecker.isTrue(expiryMonth.getYear() >= 1000 && expiryMonth.getYear() <= 9999, "Invalid expiry year: ", expiryMonth);
    ArgChecker.notNull(variant, "variant");

    String id = new StringBuilder(40)
        .append(FUT_PREFIX)
        .append(exchangeId)
        .append(SEPARATOR)
        .append(contractCode)
        .append(SEPARATOR)
        .append(expiryMonth.format(YM_FORMAT))
        .append(variant.getCode())
        .toString();
    return SecurityId.of(ETD_SCHEME, id);
  }

  /**
   * Creates an identifier for an ETD option instrument.
   * <p>
   * A typical monthly ETD with version zero will have the format:
   * {@code 'OG-ETD~O-ECAG-OGBS-201706-P1.50'}.
   * <p>
   * A more complex flex ETD (12th of the month, Cash settlement, European) with version two will have the format:
   * {@code 'OG-ETD~O-ECAG-OGBS-20170612CE-V2-P1.50'}.
   *
   * @param exchangeId  the MIC code of the exchange where the instruments are traded
   * @param contractCode  the code supplied by the exchange for use in clearing and margining, such as in SPAN
   * @param expiryMonth  the month of expiry
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex'
   * @param version  the non-negative version, zero by default
   * @param putCall  the Put/Call flag
   * @param strikePrice  the strike price
   * @return the identifier
   */
  public static SecurityId optionId(
      ExchangeId exchangeId,
      EtdContractCode contractCode,
      YearMonth expiryMonth,
      EtdVariant variant,
      int version,
      PutCall putCall,
      double strikePrice) {

    return optionId(exchangeId, contractCode, expiryMonth, variant, version, putCall, strikePrice, null);
  }

  /**
   * Creates an identifier for an ETD option instrument.
   * <p>
   * This takes into account the expiry of the underlying instrument. If the underlying expiry
   * is the same as the expiry of the option, the identifier is the same as the normal one.
   * Otherwise, the underlying expiry is added after the option expiry. For example:
   * {@code 'OG-ETD~O-ECAG-OGBS-201706-P1.50-U201709'}.
   *
   * @param exchangeId  the MIC code of the exchange where the instruments are traded
   * @param contractCode  the code supplied by the exchange for use in clearing and margining, such as in SPAN
   * @param expiryMonth  the month of expiry
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex'
   * @param version  the non-negative version, zero by default
   * @param putCall  the Put/Call flag
   * @param strikePrice  the strike price
   * @param underlyingExpiryMonth  the expiry of the underlying instrument, such as a future, may be null
   * @return the identifier
   */
  public static SecurityId optionId(
      ExchangeId exchangeId,
      EtdContractCode contractCode,
      YearMonth expiryMonth,
      EtdVariant variant,
      int version,
      PutCall putCall,
      double strikePrice,
      YearMonth underlyingExpiryMonth) {

    ArgChecker.notNull(exchangeId, "exchangeId");
    ArgChecker.notNull(contractCode, "contractCode");
    ArgChecker.notNull(expiryMonth, "expiryMonth");
    ArgChecker.notNull(variant, "variant");
    ArgChecker.notNull(putCall, "putCall");

    String putCallStr = putCall == PutCall.PUT ? "P" : "C";
    String versionCode = version > 0 ? "V" + version + SEPARATOR : "";

    NumberFormat f = NumberFormat.getIntegerInstance(Locale.ENGLISH);
    f.setGroupingUsed(false);
    f.setMaximumFractionDigits(8);
    String strikeStr = f.format(strikePrice).replace('-', 'M');

    String underlying = "";
    if (underlyingExpiryMonth != null && !underlyingExpiryMonth.equals(expiryMonth)) {
      underlying = SEPARATOR + "U" + underlyingExpiryMonth.format(YM_FORMAT);
    }

    String id = new StringBuilder(40)
        .append(OPT_PREFIX)
        .append(exchangeId)
        .append(SEPARATOR)
        .append(contractCode)
        .append(SEPARATOR)
        .append(expiryMonth.format(YM_FORMAT))
        .append(variant.getCode())
        .append(SEPARATOR)
        .append(versionCode)
        .append(putCallStr)
        .append(strikeStr)
        .append(underlying)
        .toString();
    return SecurityId.of(ETD_SCHEME, id);
  }

  //-------------------------------------------------------------------------
  /**
   * Splits an OG-ETD identifier.
   *
   * @param specId  the contract spec ID
   * @return a split representation of the ID
   * @throws IllegalArgumentException if the ID is not of the right scheme or format
   */
  public static SplitEtdContractSpecId splitId(EtdContractSpecId specId) {
    ArgChecker.notNull(specId, "specId");
    if (!specId.getStandardId().getScheme().equals(ETD_SCHEME)) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + specId);
    }
    String value = specId.getStandardId().getValue();
    // sometimes a contract code can have "-" in the name, like F-IFEN-BAJAJ-AUTO, so we need to
    // limit the split to 3: type, exchangeId, and contract code
    List<String> split = splitOnDash(value, 0, value.length(), 3);
    if (split.size() < 3) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + specId);
    }
    EtdType type = null;
    if (split.get(0).equals("F")) {
      type = EtdType.FUTURE;
    } else if (split.get(0).equals("O")) {
      type = EtdType.OPTION;
    } else {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + specId);
    }
    // common fields
    ExchangeId exchangeId = ExchangeId.of(split.get(1));
    EtdContractCode contractCode = EtdContractCode.of(split.get(2));
    return SplitEtdContractSpecId.builder()
        .specId(specId)
        .type(type)
        .exchangeId(exchangeId)
        .contractCode(contractCode)
        .build();
  }

  /**
   * Splits an OG-ETD identifier.
   *
   * @param securityId  the security ID
   * @return a split representation of the ID
   * @throws IllegalArgumentException if the ID is not of the right scheme or format
   */
  public static SplitEtdId splitId(SecurityId securityId) {
    ArgChecker.notNull(securityId, "securityId");
    StandardId standardId = securityId.getStandardId();
    if (!standardId.getScheme().equals(ETD_SCHEME)) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }

    String value = standardId.getValue();
    int separatorIndex = findExpiryGroupSeparatorIndex(value, securityId);

    // Example: F-IFEN-ABC or F-IFEN-ABC-XYZ
    List<String> contractDetailsSplit = splitOnDash(value, 0, separatorIndex, 3);
    if (contractDetailsSplit.size() != 3) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }

    // common fields
    ExchangeId exchangeId = ExchangeId.of(contractDetailsSplit.get(1));
    EtdContractCode contractCode = EtdContractCode.of(contractDetailsSplit.get(2));

    // Example: 20230412 for futures or 202304-V3-PM12.43-U202304 for options
    List<String> expiryAndOptionDetailsSplit =
        splitOnDash(value, separatorIndex + 1, value.length(), Integer.MAX_VALUE);
    String dateStr = expiryAndOptionDetailsSplit.get(0);
    if (dateStr.length() < 6) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
    YearMonth month = parseYearMonth(dateStr, 0, securityId);
    EtdVariant variant = EtdVariant.parse(dateStr.substring(6));
    SplitEtdId.Builder parsed = SplitEtdId.builder()
        .securityId(securityId)
        .exchangeId(exchangeId)
        .contractCode(contractCode)
        .expiry(month)
        .variant(variant);

    // future vs option
    if (standardId.getValue().startsWith(FUT_PREFIX) && expiryAndOptionDetailsSplit.size() == 1) {
      return parsed.build();
    } else if (standardId.getValue().startsWith(OPT_PREFIX) && expiryAndOptionDetailsSplit.size() > 1) {
      SplitEtdOption parsedOption = parseEtdOptionId(expiryAndOptionDetailsSplit, securityId);
      return parsed.option(parsedOption).build();
    } else {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
  }

  /**
   * Splits an OG-ETD identifier to obtain the exchange ID.
   *
   * @param securityId  the security ID
   * @return the exchange ID
   * @throws IllegalArgumentException if the ID is not of the right scheme or format
   */
  public static ExchangeId splitIdToExchangeId(SecurityId securityId) {
    ArgChecker.notNull(securityId, "securityId");
    StandardId standardId = securityId.getStandardId();
    if (!standardId.getScheme().equals(ETD_SCHEME)) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }

    String value = standardId.getValue();
    int separatorIndex = findExpiryGroupSeparatorIndex(value, securityId);

    // Example: F-IFEN-ABC or F-IFEN-ABC-XYZ
    List<String> contractDetailsSplit = splitOnDash(value, 0, separatorIndex, 3);
    if (contractDetailsSplit.size() != 3) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }

    // common fields
    return ExchangeId.of(contractDetailsSplit.get(1));
  }

  // splits value.substring(fromIndex, toIndex) on '-', stopping once maxTokens tokens have been
  // produced (the last token keeps any remaining, unsplit content) - splitting directly against
  // the bounds of the original string avoids having to first carve out a wrapper substring, which
  // is a regex/Guava-free equivalent of Splitter.on('-').limit(n)
  private static List<String> splitOnDash(String value, int fromIndex, int toIndex, int maxTokens) {
    List<String> tokens = new ArrayList<>(4);
    int start = fromIndex;
    int tokenCount = 1;
    int dash = value.indexOf('-', start);
    while (dash >= 0 && dash < toIndex && tokenCount < maxTokens) {
      tokens.add(value.substring(start, dash));
      start = dash + 1;
      tokenCount++;
      dash = value.indexOf('-', start);
    }
    tokens.add(value.substring(start, toIndex));
    return tokens;
  }

  // parses the 4-digit year and 2-digit month starting at offset, such as "202304" at offset 0,
  // without going through DateTimeFormatter's general-purpose (and much costlier) parse/resolve
  private static YearMonth parseYearMonth(String value, int offset, SecurityId securityId) {
    int year = parseDigits(value, offset, 4, securityId);
    int month = parseDigits(value, offset + 4, 2, securityId);
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
    return YearMonth.of(year, month);
  }

  // parses 'length' consecutive digit characters starting at offset into an int
  private static int parseDigits(String value, int offset, int length, SecurityId securityId) {
    int result = 0;
    for (int i = offset; i < offset + length; i++) {
      char ch = value.charAt(i);
      if (ch < '0' || ch > '9') {
        throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
      }
      result = result * 10 + (ch - '0');
    }
    return result;
  }

  // finds the index of the '-' that separates the contract details from the expiry and option
  // details, which is the last '-' in the value immediately followed by 6 digits, such as the
  // '-' before "202304" in "F-IFEN-ABC-202304" or "O-IFEN-ABC-202304-PM12.34-U202309"
  private static int findExpiryGroupSeparatorIndex(String value, SecurityId securityId) {
    for (int i = value.length() - YEAR_MONTH_DIGITS - 1; i >= 0; i--) {
      if (value.charAt(i) == '-' && isSixDigitsFrom(value, i + 1)) {
        return i;
      }
    }
    throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
  }

  // checks if the 6 characters starting at fromIndex are all digits
  private static boolean isSixDigitsFrom(String value, int fromIndex) {
    for (int i = fromIndex; i < fromIndex + YEAR_MONTH_DIGITS; i++) {
      char ch = value.charAt(i);
      if (ch < '0' || ch > '9') {
        return false;
      }
    }
    return true;
  }

  // parses an option
  private static SplitEtdOption parseEtdOptionId(
      List<String> expiryAndOptionDetailsSplit,
      SecurityId securityId) {

    int optionTokenCount = expiryAndOptionDetailsSplit.size() - 1;
    String versionStr = expiryAndOptionDetailsSplit.get(1);
    String putCallStrikeStr = optionTokenCount > 1 ? expiryAndOptionDetailsSplit.get(1 + 1) : "";
    String underlyingMonthStr = optionTokenCount > 2 ? expiryAndOptionDetailsSplit.get(1 + 2) : "";
    int version = 0;
    if (versionStr.startsWith("V")) {
      version = Integer.parseInt(versionStr.substring(1));
    } else {
      underlyingMonthStr = putCallStrikeStr;
      putCallStrikeStr = versionStr;
    }
    PutCall putCall;
    if (putCallStrikeStr.startsWith("P")) {
      putCall = PutCall.PUT;
    } else if (putCallStrikeStr.startsWith("C")) {
      putCall = PutCall.CALL;
    } else {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
    boolean strikeNegative = putCallStrikeStr.length() > 1 && putCallStrikeStr.charAt(1) == 'M';
    String strikeStr = putCallStrikeStr.substring(strikeNegative ? 2 : 1);
    double strike = strikeNegative ? -Double.parseDouble(strikeStr) : Double.parseDouble(strikeStr);
    YearMonth underlyingMonth = null;
    if (!underlyingMonthStr.isEmpty()) {
      if (!underlyingMonthStr.startsWith("U") || underlyingMonthStr.length() != 7) {
        throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
      }
      underlyingMonth = parseYearMonth(underlyingMonthStr, 1, securityId);
    }
    return SplitEtdOption.of(version, putCall, strike, underlyingMonth);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private EtdIdUtils() {
  }

}
