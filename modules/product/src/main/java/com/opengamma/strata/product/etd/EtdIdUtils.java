/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.Locale;

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
  public static final String ETD_SCHEME = "OG-ETD";
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
   * Creates an identifier for an ETD future instrument.
   * <p>
   * A typical monthly ETD will have the format:
   * {@code 'OG-ETD~O-ECAG-OGBS-201706'}.
   * <p>
   * A more complex flex ETD (12th of the month, Physical settlement) will have the format:
   * {@code 'OG-ETD~O-ECAG-OGBS-20170612E'}.
   *
   * @param exchangeId  the MIC code of the exchange where the instruments are traded
   * @param contractCode  the code supplied by the exchange for use in clearing and margining, such as in SPAN
   * @param expiryMonth  the month of expiry
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex.
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

    String id = FUT_PREFIX +
        exchangeId + SEPARATOR +
        contractCode + SEPARATOR +
        expiryMonth.getYear() +
        ((char) ((expiryMonth.getMonthValue() / 10) + '0')) +
        ((char) ((expiryMonth.getMonthValue() % 10) + '0')) +
        variant.getCode();
    return SecurityId.of(ETD_SCHEME, id);
  }

  /**
   * Creates an identifier for an ETD future instrument.
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
   * @param variant  the variant of the ETD, such as 'Monthly', 'Weekly, 'Daily' or 'Flex.
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

    ArgChecker.notNull(exchangeId, "exchangeId");
    ArgChecker.notNull(contractCode, "contractCode");
    ArgChecker.notNull(expiryMonth, "expiryMonth");
    ArgChecker.isTrue(expiryMonth.getYear() >= 1000 && expiryMonth.getYear() <= 9999, "Invalid expiry year: ", expiryMonth);
    ArgChecker.notNull(variant, "variant");
    ArgChecker.notNull(putCall, "putCall");

    String putCallStr = putCall == PutCall.PUT ? "P" : "C";
    String versionCode = version > 0 ? "V" + version + SEPARATOR : "";

    NumberFormat f = NumberFormat.getIntegerInstance(Locale.ENGLISH);
    f.setGroupingUsed(false);
    f.setMaximumFractionDigits(8);
    String strikeStr = f.format(strikePrice).replace('-', 'M');

    String id = OPT_PREFIX +
        exchangeId + SEPARATOR +
        contractCode + SEPARATOR +
        expiryMonth.getYear() +
        ((char) ((expiryMonth.getMonthValue() / 10) + '0')) +
        ((char) ((expiryMonth.getMonthValue() % 10) + '0')) +
        variant.getCode() + SEPARATOR +
        versionCode +
        putCallStr +
        strikeStr;
    return SecurityId.of(ETD_SCHEME, id);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private EtdIdUtils() {
  }

}
