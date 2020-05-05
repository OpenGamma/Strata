/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderUtils.CONTRACT_CODE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXCHANGE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXPIRY_DAY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXPIRY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXPIRY_WEEK_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.LONG_QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.SETTLEMENT_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.SHORT_QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.TYPE_FIELD;
import static com.opengamma.strata.product.etd.EtdIdUtils.ETD_SCHEME;
import static com.opengamma.strata.product.etd.EtdIdUtils.SEPARATOR;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.product.etd.EtdExpiryType;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * Handles the CSV file format for ETD Future positions.
 */
final class EtdFuturePositionCsvPlugin implements PositionTypeCsvWriter<EtdFuturePosition> {

  /**
   * The singleton instance of the plugin.
   */
  public static final EtdFuturePositionCsvPlugin INSTANCE = new EtdFuturePositionCsvPlugin();
  private static final int CONTRACT_CODE_INDEX = 2;
  private static final int EXCHANGE_INDEX = 1;

  @Override
  public List<String> headers(List<EtdFuturePosition> positions) {
    List<String> headers = new ArrayList<>();
    headers.add(CONTRACT_CODE_FIELD);
    headers.add(EXCHANGE_FIELD);
    headers.add(EXPIRY_FIELD);
    headers.add(LONG_QUANTITY_FIELD);
    headers.add(SHORT_QUANTITY_FIELD);
    if (positions.stream().anyMatch(position -> position.getProduct().getVariant().isFlex())) {
      headers.add(SETTLEMENT_TYPE_FIELD);
    }
    if (positions.stream().anyMatch(position -> position.getProduct().getVariant().getType() == EtdExpiryType.WEEKLY)) {
      headers.add(EXPIRY_WEEK_FIELD);
    }
    if (positions.stream().anyMatch(position -> position.getProduct().getVariant().getType() == EtdExpiryType.DAILY)) {
      headers.add(EXPIRY_DAY_FIELD);
    }
    return ImmutableList.copyOf(headers);
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, EtdFuturePosition position) {
    csv.writeCell(TYPE_FIELD, "FUT");
    csv.writeCell(EXPIRY_FIELD, position.getProduct().getExpiry());
    csv.writeCell(LONG_QUANTITY_FIELD, position.getLongQuantity());
    csv.writeCell(SHORT_QUANTITY_FIELD, position.getShortQuantity());

    String idScheme = position.getSecurity().getContractSpecId().getStandardId().getScheme();
    if (ETD_SCHEME.equals(idScheme)) {
      String[] standardIdValueComponents =
          position.getSecurity().getContractSpecId().getStandardId().getValue().split(SEPARATOR);
      csv.writeCell(CONTRACT_CODE_FIELD, standardIdValueComponents[CONTRACT_CODE_INDEX]);
      csv.writeCell(EXCHANGE_FIELD, standardIdValueComponents[EXCHANGE_INDEX]);
    } else {
      throw new IllegalArgumentException("Unable to write position to CSV with id scheme: " + idScheme);
    }
    EtdVariant productVariant = position.getProduct().getVariant();
    if (productVariant.getSettlementType().isPresent()) {
      csv.writeCell(SETTLEMENT_TYPE_FIELD, productVariant.getSettlementType().get());
    }
    if (EtdExpiryType.WEEKLY == productVariant.getType() && productVariant.getDateCode().isPresent()) {
      csv.writeCell(EXPIRY_WEEK_FIELD, productVariant.getDateCode().getAsInt());
    } else if (EtdExpiryType.DAILY == productVariant.getType() && productVariant.getDateCode().isPresent()) {
      csv.writeCell(EXPIRY_DAY_FIELD, productVariant.getDateCode().getAsInt());
    }

    csv.writeNewLine();
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private EtdFuturePositionCsvPlugin() {
  }

}
