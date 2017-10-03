/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.DEFAULT_SECURITY_SCHEME;

import java.time.YearMonth;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdFutureSecurity;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdOptionSecurity;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * Loads security trades from CSV files.
 */
final class SecurityCsvLoader {

  // lookup settlement by code
  private static final ImmutableMap<String, EtdSettlementType> SETTLEMENT_BY_CODE =
      Stream.of(EtdSettlementType.values()).collect(toImmutableMap(EtdSettlementType::getCode));

  // CSV column headers
  private static final String SECURITY_ID_SCHEME_FIELD = "Security Id Scheme";
  private static final String SECURITY_ID_FIELD = "Security Id";
  private static final String LONG_QUANTITY_FIELD = "Long Quantity";
  private static final String SHORT_QUANTITY_FIELD = "Short Quantity";
  private static final String QUANTITY_FIELD = "Quantity";
  private static final String PRICE_FIELD = "Price";

  // ETD columns
  private static final String EXPIRY_FIELD = "Expiry";
  private static final String EXPIRY_WEEK_FIELD = "Expiry Week";
  private static final String EXPIRY_DAY_FIELD = "Expiry Day";
  private static final String SETTLEMENT_TYPE_FIELD = "Settlement Type";
  private static final String EXERCISE_STYLE_FIELD = "Exercise Style";
  private static final String VERSION_FIELD = "Version";
  private static final String PUT_CALL_FIELD = "Put Call";
  private static final String EXERCISE_PRICE_FIELD = "Exercise Price";

  // Option might not specify a version number, which is when we use this default
  private static final int DEFAULT_OPTION_VERSION_NUMBER = 0;

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static SecurityTrade parseTrade(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    SecurityTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  private static SecurityTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    String securityIdScheme = row.findValue(SECURITY_ID_SCHEME_FIELD).orElse(DEFAULT_SECURITY_SCHEME);
    String securityIdValue = row.getValue(SECURITY_ID_FIELD);
    SecurityId securityId = SecurityId.of(securityIdScheme, securityIdValue);
    double price = LoaderUtils.parseDouble(row.getValue(PRICE_FIELD));
    DoublesPair quantity = parseQuantity(row);
    return SecurityTrade.of(info, securityId, quantity.getFirst() - quantity.getSecond(), price);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row, inferring the position type.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed position
   */
  static Position parsePosition(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    if (row.findValue(EXPIRY_FIELD).isPresent()) {
      // etd
      if (row.findValue(PUT_CALL_FIELD).isPresent() || row.findValue(EXERCISE_PRICE_FIELD).isPresent()) {
        return parseOption(row, info, resolver);
      } else {
        return parseFuture(row, info, resolver);
      }
    } else {
      // simple
      return parseSimple(row, info, resolver);
    }
  }

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed position
   */
  static SecurityPosition parseSimple(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    String securityIdScheme = row.findValue(SECURITY_ID_SCHEME_FIELD).orElse(DEFAULT_SECURITY_SCHEME);
    String securityIdValue = row.getValue(SECURITY_ID_FIELD);
    SecurityId securityId = SecurityId.of(securityIdScheme, securityIdValue);
    DoublesPair quantity = parseQuantity(row);
    SecurityPosition position = SecurityPosition.ofLongShort(info, securityId, quantity.getFirst(), quantity.getSecond());
    return resolver.completePosition(row, position);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the position info
   * @param resolver  the resolver of additional security information
   * @return the parsed position
   */
  static EtdFuturePosition parseFuture(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    EtdContractSpec contract = resolver.parseEtdContractSpec(row, EtdType.FUTURE);
    Pair<YearMonth, EtdVariant> variant = parseVariant(row, EtdType.FUTURE);
    EtdFutureSecurity security = contract.createFuture(variant.getFirst(), variant.getSecond());
    DoublesPair quantity = parseQuantity(row);
    EtdFuturePosition position = EtdFuturePosition.ofLongShort(info, security, quantity.getFirst(), quantity.getSecond());
    return resolver.completePosition(row, position, contract);
  }

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the position info
   * @param resolver  the resolver of additional security information
   * @return the parsed position
   */
  static EtdOptionPosition parseOption(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    EtdContractSpec contract = resolver.parseEtdContractSpec(row, EtdType.OPTION);
    Pair<YearMonth, EtdVariant> variant = parseVariant(row, EtdType.OPTION);
    int version = row.findValue(VERSION_FIELD).map(Integer::parseInt).orElse(DEFAULT_OPTION_VERSION_NUMBER);
    PutCall putCall = LoaderUtils.parsePutCall(row.getValue(PUT_CALL_FIELD));
    double strikePrice = Double.parseDouble(row.getValue(EXERCISE_PRICE_FIELD));
    EtdOptionSecurity security = contract.createOption(variant.getFirst(), variant.getSecond(), version, putCall, strikePrice);
    DoublesPair quantity = parseQuantity(row);
    EtdOptionPosition position = EtdOptionPosition.ofLongShort(info, security, quantity.getFirst(), quantity.getSecond());
    return resolver.completePosition(row, position, contract);
  }

  //-------------------------------------------------------------------------
  // parses a quantity field
  private static DoublesPair parseQuantity(CsvRow row) {
    Optional<Double> quantityOpt = row.findValue(QUANTITY_FIELD).map(s -> LoaderUtils.parseDouble(s));
    if (quantityOpt.isPresent()) {
      double quantity = quantityOpt.get();
      return DoublesPair.of(quantity >= 0 ? quantity : 0, quantity >= 0 ? 0 : -quantity);
    }
    Optional<Double> longQuantityOpt = row.findValue(LONG_QUANTITY_FIELD).map(s -> LoaderUtils.parseDouble(s));
    Optional<Double> shortQuantityOpt = row.findValue(SHORT_QUANTITY_FIELD).map(s -> LoaderUtils.parseDouble(s));
    if (!longQuantityOpt.isPresent() && !shortQuantityOpt.isPresent()) {
      throw new IllegalArgumentException(
          Messages.format("Security must contain a quantity column, either '{}' or '{}' and '{}'",
              QUANTITY_FIELD, LONG_QUANTITY_FIELD, SHORT_QUANTITY_FIELD));
    }
    double longQuantity = ArgChecker.notNegative(longQuantityOpt.orElse(0d), LONG_QUANTITY_FIELD);
    double shortQuantity = ArgChecker.notNegative(shortQuantityOpt.orElse(0d), SHORT_QUANTITY_FIELD);
    return DoublesPair.of(longQuantity, shortQuantity);
  }

  // parses the year-month and variant
  private static Pair<YearMonth, EtdVariant> parseVariant(CsvRow row, EtdType type) {
    YearMonth yearMonth = LoaderUtils.parseYearMonth(row.getValue(EXPIRY_FIELD));
    int week = row.findValue(EXPIRY_WEEK_FIELD).map(s -> LoaderUtils.parseInteger(s)).orElse(0);
    int day = row.findValue(EXPIRY_DAY_FIELD).map(s -> LoaderUtils.parseInteger(s)).orElse(0);
    Optional<EtdSettlementType> settleType = row.findValue(SETTLEMENT_TYPE_FIELD).map(s -> parseEtdSettlementType(s));
    Optional<EtdOptionType> optionType = row.findValue(EXERCISE_STYLE_FIELD).map(s -> parseEtdOptionType(s));
    // check valid combinations
    if (!settleType.isPresent()) {
      if (day == 0) {
        if (week == 0) {
          return Pair.of(yearMonth, EtdVariant.ofMonthly());
        } else {
          return Pair.of(yearMonth, EtdVariant.ofWeekly(week));
        }
      } else {
        if (week == 0) {
          return Pair.of(yearMonth, EtdVariant.ofDaily(day));
        } else {
          throw new IllegalArgumentException("ETD date columns conflict, cannot set both expiry day and expiry week");
        }
      }
    } else {
      if (day == 0) {
        throw new IllegalArgumentException("ETD date columns conflict, must set expiry day for Flex " + type);
      }
      if (week != 0) {
        throw new IllegalArgumentException("ETD date columns conflict, cannot set expiry week for Flex " + type);
      }
      if (type == EtdType.FUTURE) {
        return Pair.of(yearMonth, EtdVariant.ofFlexFuture(day, settleType.get()));
      } else {
        if (!optionType.isPresent()) {
          throw new IllegalArgumentException("ETD option type not found for Flex Option");
        }
        return Pair.of(yearMonth, EtdVariant.ofFlexOption(day, settleType.get(), optionType.get()));
      }
    }
  }

  // parses the ETD settlement type form short code or full name
  private static EtdSettlementType parseEtdSettlementType(String str) {
    String upper = str.toUpperCase(Locale.ENGLISH);
    EtdSettlementType fromCode = SETTLEMENT_BY_CODE.get(upper);
    return fromCode != null ? fromCode : EtdSettlementType.of(str);
  }

  // parses the ETD option type form short code or full name
  private static EtdOptionType parseEtdOptionType(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "AMERICAN":
      case "A":
        return EtdOptionType.AMERICAN;
      case "EUROPEAN":
      case "E":
        return EtdOptionType.EUROPEAN;
      default:
        throw new IllegalArgumentException(
            "Unknown EtdOptionType value, must be 'American' or 'European' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'A' and 'E'");
    }
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SecurityCsvLoader() {
  }

}
