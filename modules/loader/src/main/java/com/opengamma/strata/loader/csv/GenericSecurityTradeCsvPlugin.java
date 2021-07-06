/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONTRACT_SIZE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TICK_SIZE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TICK_VALUE_FIELD;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityPriceInfo;

/**
 * Handles the CSV file format for Generic Security trades.
 */
public class GenericSecurityTradeCsvPlugin implements TradeCsvWriterPlugin<GenericSecurityTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final GenericSecurityTradeCsvPlugin INSTANCE = new GenericSecurityTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.of(
      SECURITY_ID_SCHEME_FIELD,
      SECURITY_ID_FIELD,
      BUY_SELL_FIELD,
      QUANTITY_FIELD,
      PRICE_FIELD,
      TICK_SIZE_FIELD,
      CURRENCY_FIELD,
      TICK_VALUE_FIELD,
      CONTRACT_SIZE_FIELD);

  @Override
  public Set<String> headers(List<GenericSecurityTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvOutput.CsvRowOutputWithHeaders csv, GenericSecurityTrade trade) {
    CsvWriterUtils.writeSecurityQuantityTrade(csv, trade);
    SecurityPriceInfo securityPriceInfo = trade.getProduct().getInfo().getPriceInfo();
    csv.writeCell(TICK_SIZE_FIELD, securityPriceInfo.getTickSize());
    csv.writeCell(CURRENCY_FIELD, securityPriceInfo.getTickValue().getCurrency());
    csv.writeCell(TICK_VALUE_FIELD, securityPriceInfo.getTickValue().getAmount());
    csv.writeCell(CONTRACT_SIZE_FIELD, securityPriceInfo.getContractSize());
    csv.writeNewLine();
  }

  @Override
  public String getName() {
    return GenericSecurityTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(GenericSecurityTrade.class);
  }
}
