/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderUtils.CONTRACT_CODE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.DEFAULT_OPTION_VERSION_NUMBER;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXCHANGE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXERCISE_PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.PUT_CALL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.UNDERLYING_EXPIRY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.VERSION_FIELD;

import java.time.YearMonth;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.PositionInfoBuilder;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.common.ExchangeId;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.etd.EtdContractCode;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdFutureSecurity;
import com.opengamma.strata.product.etd.EtdIdUtils;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdOptionSecurity;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * Resolves additional information when parsing position CSV files.
 * <p>
 * Data loaded from a CSV may contain additional information that needs to be captured.
 * This plugin point allows the additional CSV columns to be parsed and captured.
 * It also allows the ETD contract specification to be loaded.
 */
public interface PositionCsvInfoResolver {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static PositionCsvInfoResolver standard() {
    return StandardCsvInfoResolver.of(ReferenceData.standard());
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static PositionCsvInfoResolver of(ReferenceData refData) {
    return StandardCsvInfoResolver.of(refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the reference data being used.
   * 
   * @return the reference data
   */
  public abstract ReferenceData getReferenceData();

  /**
   * Parses attributes into {@code PositionInfo}.
   * <p>
   * If it is available, the position ID will have been set before this method is called.
   * It may be altered if necessary, although this is not recommended.
   * 
   * @param row  the CSV row to parse
   * @param builder  the builder to update
   */
  public default void parsePositionInfo(CsvRow row, PositionInfoBuilder builder) {
    // do nothing
  }

  /**
   * Completes the position, potentially parsing additional columns.
   * <p>
   * This is called after the position has been parsed and after
   * {@link #parsePositionInfo(CsvRow, PositionInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param position  the parsed position
   * @param spec  the contract specification
   * @return the updated position
   */
  public default EtdFuturePosition completePosition(CsvRow row, EtdFuturePosition position, EtdContractSpec spec) {
    // do nothing
    return position;
  }

  /**
   * Completes the position, potentially parsing additional columns.
   * <p>
   * This is called after the position has been parsed and after
   * {@link #parsePositionInfo(CsvRow, PositionInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param position  the parsed position
   * @param spec  the contract specification
   * @return the updated position
   */
  public default EtdOptionPosition completePosition(CsvRow row, EtdOptionPosition position, EtdContractSpec spec) {
    // do nothing
    return position;
  }

  /**
   * Completes the position, potentially parsing additional columns.
   * <p>
   * This is called after the position has been parsed and after
   * {@link #parsePositionInfo(CsvRow, PositionInfoBuilder)}.
   * 
   * @param row  the CSV row to parse
   * @param position  the parsed position
   * @return the updated position
   */
  public default SecurityPosition completePosition(CsvRow row, SecurityPosition position) {
    // do nothing
    return position;
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the contract specification from the row.
   * 
   * @param row  the CSV row to parse
   * @param type  the ETD type
   * @return the contract specification
   * @throws IllegalArgumentException if the specification is not found
   */
  public default EtdContractSpec parseEtdContractSpec(CsvRow row, EtdType type) {
    ExchangeId exchangeId = ExchangeId.of(row.getValue(EXCHANGE_FIELD));
    EtdContractCode contractCode = EtdContractCode.of(row.getValue(CONTRACT_CODE_FIELD));
    EtdContractSpecId specId = EtdIdUtils.contractSpecId(type, exchangeId, contractCode);
    return getReferenceData().findValue(specId).orElseThrow(
        () -> new IllegalArgumentException("ETD contract specification not found in reference data: " + specId));
  }

  /**
   * Parses an ETD future position from the CSV row.
   * <p>
   * This is intended to use reference data to find the ETD future security,
   * returning it as an instance of {@link EtdFuturePosition}.
   * The reference data lookup uses {@link #parseEtdContractSpec(CsvRow, EtdType)} by default,
   * however it could be overridden to lookup the security directly in reference data.
   * 
   * @param row  the CSV row to parse
   * @param info  the position information
   * @return the parsed position
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  public default Position parseEtdFuturePosition(CsvRow row, PositionInfo info) {
    EtdContractSpec contract = parseEtdContractSpec(row, EtdType.FUTURE);
    Pair<YearMonth, EtdVariant> variant = CsvLoaderUtils.parseEtdVariant(row, EtdType.FUTURE);
    EtdFutureSecurity security = contract.createFuture(variant.getFirst(), variant.getSecond());
    DoublesPair quantity = CsvLoaderUtils.parseQuantity(row);
    EtdFuturePosition position = EtdFuturePosition.ofLongShort(info, security, quantity.getFirst(), quantity.getSecond());
    return completePosition(row, position, contract);
  }

  /**
   * Parses an ETD future position from the CSV row.
   * <p>
   * This is intended to use reference data to find the ETD future security,
   * returning it as an instance of {@link EtdOptionPosition}.
   * The reference data lookup uses {@link #parseEtdContractSpec(CsvRow, EtdType)} by default,
   * however it could be overridden to lookup the security directly in reference data.
   * 
   * @param row  the CSV row
   * @param info  the position info
   * @return the parsed position
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  public default Position parseEtdOptionPosition(CsvRow row, PositionInfo info) {
    EtdContractSpec contract = parseEtdContractSpec(row, EtdType.OPTION);
    Pair<YearMonth, EtdVariant> variant = CsvLoaderUtils.parseEtdVariant(row, EtdType.OPTION);
    int version = row.findValue(VERSION_FIELD).map(Integer::parseInt).orElse(DEFAULT_OPTION_VERSION_NUMBER);
    PutCall putCall = LoaderUtils.parsePutCall(row.getValue(PUT_CALL_FIELD));
    double strikePrice = Double.parseDouble(row.getValue(EXERCISE_PRICE_FIELD));
    YearMonth underlyingExpiry = row.findValue(UNDERLYING_EXPIRY_FIELD)
        .map(str -> LoaderUtils.parseYearMonth(str))
        .orElse(null);
    EtdOptionSecurity security = contract.createOption(
        variant.getFirst(), variant.getSecond(), version, putCall, strikePrice, underlyingExpiry);
    DoublesPair quantity = CsvLoaderUtils.parseQuantity(row);
    EtdOptionPosition position = EtdOptionPosition.ofLongShort(info, security, quantity.getFirst(), quantity.getSecond());
    return completePosition(row, position, contract);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses an ETD future position from the CSV row without using reference data.
   * <p>
   * This returns a {@link SecurityPosition} based on a standard ETD identifier from {@link EtdIdUtils}.
   * 
   * @param row  the CSV row to parse
   * @param info  the position information
   * @return the loaded positions, position-level errors are captured in the result
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  public default SecurityPosition parseEtdFutureSecurityPosition(CsvRow row, PositionInfo info) {
    ExchangeId exchangeId = ExchangeId.of(row.getValue(EXCHANGE_FIELD));
    EtdContractCode contractCode = EtdContractCode.of(row.getValue(CONTRACT_CODE_FIELD));
    Pair<YearMonth, EtdVariant> variant = CsvLoaderUtils.parseEtdVariant(row, EtdType.FUTURE);
    SecurityId securityId = EtdIdUtils.futureId(exchangeId, contractCode, variant.getFirst(), variant.getSecond());
    DoublesPair quantity = CsvLoaderUtils.parseQuantity(row);
    SecurityPosition position = SecurityPosition.ofLongShort(info, securityId, quantity.getFirst(), quantity.getSecond());
    return completePosition(row, position);
  }

  /**
   * Parses an ETD option position from the CSV row without using reference data.
   * <p>
   * This returns a {@link SecurityPosition} based on a standard ETD identifier from {@link EtdIdUtils}.
   * 
   * @param row  the CSV row to parse
   * @param info  the position information
   * @return the loaded positions, position-level errors are captured in the result
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  public default SecurityPosition parseEtdOptionSecurityPosition(CsvRow row, PositionInfo info) {
    ExchangeId exchangeId = ExchangeId.of(row.getValue(EXCHANGE_FIELD));
    EtdContractCode contractCode = EtdContractCode.of(row.getValue(CONTRACT_CODE_FIELD));
    Pair<YearMonth, EtdVariant> variant = CsvLoaderUtils.parseEtdVariant(row, EtdType.OPTION);
    int version = row.findValue(VERSION_FIELD).map(Integer::parseInt).orElse(DEFAULT_OPTION_VERSION_NUMBER);
    PutCall putCall = LoaderUtils.parsePutCall(row.getValue(PUT_CALL_FIELD));
    double strikePrice = Double.parseDouble(row.getValue(EXERCISE_PRICE_FIELD));
    YearMonth underlyingExpiry = row.findValue(UNDERLYING_EXPIRY_FIELD)
        .map(str -> LoaderUtils.parseYearMonth(str))
        .orElse(null);
    SecurityId securityId = EtdIdUtils.optionId(
        exchangeId, contractCode, variant.getFirst(), variant.getSecond(), version, putCall, strikePrice, underlyingExpiry);
    DoublesPair quantity = CsvLoaderUtils.parseQuantity(row);
    SecurityPosition position = SecurityPosition.ofLongShort(info, securityId, quantity.getFirst(), quantity.getSecond());
    return completePosition(row, position);
  }

}
