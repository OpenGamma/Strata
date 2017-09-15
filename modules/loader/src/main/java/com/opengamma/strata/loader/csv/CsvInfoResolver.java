/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.PositionInfoBuilder;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.common.ExchangeId;
import com.opengamma.strata.product.etd.EtdContractCode;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdIdUtils;
import com.opengamma.strata.product.etd.EtdType;

/**
 * Resolves security information from CSV files, enriching the parser.
 * <p>
 * Data loaded from a CSV may contain additional information that needs to be captured.
 * This plugin point allows the additional CSV columns to be parsed and captured.
 */
public interface CsvInfoResolver {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static CsvInfoResolver standard() {
    return StandardCsvInfoResolver.of(ReferenceData.standard());
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static CsvInfoResolver of(ReferenceData refData) {
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
   * Parses attributes into {@code TradeInfo}.
   * <p>
   * If they are available, the trade ID, date, time and zone will have been set
   * before this method is called. They may be altered if necessary, although
   * this is not recommended.
   * 
   * @param row  the CSV row to parse
   * @param builder  the builder to update
   */
  public default void parseTradeInfo(CsvRow row, TradeInfoBuilder builder) {
    // do nothing
  }

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
