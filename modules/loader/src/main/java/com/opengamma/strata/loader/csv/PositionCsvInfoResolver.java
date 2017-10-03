/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.PositionInfoBuilder;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.common.ExchangeId;
import com.opengamma.strata.product.etd.EtdContractCode;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdIdUtils;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdType;

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

  /**
   * Parses the contract specification from the row.
   * 
   * @param row  the CSV row to parse
   * @param type  the ETD type
   * @return the loaded positions, position-level errors are captured in the result
   */
  public default EtdContractSpec parseEtdContractSpec(CsvRow row, EtdType type) {
    ExchangeId exchangeId = ExchangeId.of(row.getValue(StandardCsvInfoResolver.EXCHANGE_FIELD));
    EtdContractCode contractCode = EtdContractCode.of(row.getValue(StandardCsvInfoResolver.CONTRACT_CODE_FIELD));
    EtdContractSpecId specId = EtdIdUtils.contractSpecId(type, exchangeId, contractCode);
    return getReferenceData().findValue(specId).orElseThrow(
        () -> new IllegalArgumentException("ETD contract specification not found in reference data: " + specId));
  }

}
