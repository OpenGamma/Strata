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
import java.util.List;
import java.util.Locale;

import com.google.common.base.Splitter;
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
    List<String> split = Splitter.on('-').splitToList(specId.getStandardId().getValue());
    if (split.size() != 3) {
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
    if (!securityId.getStandardId().getScheme().equals(ETD_SCHEME)) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
    List<String> split = Splitter.on('-').splitToList(securityId.getStandardId().getValue());
    if (split.size() < 4) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
    // common fields
    ExchangeId exchangeId = ExchangeId.of(split.get(1));
    EtdContractCode contractCode = EtdContractCode.of(split.get(2));
    String dateStr = split.get(3);
    if (dateStr.length() < 6) {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
    YearMonth month = YearMonth.parse(dateStr.substring(0, 6), YM_FORMAT);
    EtdVariant variant = EtdVariant.parse(dateStr.substring(6));
    SplitEtdId.Builder parsed = SplitEtdId.builder()
        .securityId(securityId)
        .exchangeId(exchangeId)
        .contractCode(contractCode)
        .expiry(month)
        .variant(variant);
    // future vs option
    if (securityId.getStandardId().getValue().startsWith(FUT_PREFIX) && split.size() == 4) {
      return parsed.build();
    } else if (securityId.getStandardId().getValue().startsWith(OPT_PREFIX) && split.size() > 4) {
      SplitEtdOption parsedOption = parseEtdOptionId(split, securityId);
      return parsed.option(parsedOption).build();
    } else {
      throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
    }
  }

  // parses an option
  private static SplitEtdOption parseEtdOptionId(List<String> split, SecurityId securityId) {
    String versionStr = split.get(4);
    String putCallStrikeStr = split.size() > 5 ? split.get(5) : "";
    String underlyingMonthStr = split.size() > 6 ? split.get(6) : "";
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
    String strikeStr = putCallStrikeStr.substring(1).replace('M', '-');
    double strike = Double.parseDouble(strikeStr);
    YearMonth underlyingMonth = null;
    if (!underlyingMonthStr.isEmpty()) {
      if (!underlyingMonthStr.startsWith("U") || underlyingMonthStr.length() != 7) {
        throw new IllegalArgumentException("ETD ID cannot be parsed: " + securityId);
      }
      underlyingMonth = YearMonth.parse(underlyingMonthStr.substring(1), YM_FORMAT);
    }
    return SplitEtdOption.of(version, putCall, strike, underlyingMonth);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private EtdIdUtils() {
  }

}
