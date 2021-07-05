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
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.common.BuySell;

/**
 * here
 */
public class GenericSecurityTradeCsvPlugin implements TradeCsvWriterPlugin<GenericSecurityTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final GenericSecurityTradeCsvPlugin INSTANCE = new GenericSecurityTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.<String>builder()
      .add(SECURITY_ID_SCHEME_FIELD)
      .add(SECURITY_ID_FIELD)
      .add(BUY_SELL_FIELD)
      .add(QUANTITY_FIELD)
      .add(PRICE_FIELD)
      .add(TICK_SIZE_FIELD)
      .add(CURRENCY_FIELD)
      .add(TICK_VALUE_FIELD)
      .add(CONTRACT_SIZE_FIELD)
      .build();

  @Override
  public Set<String> headers(List<GenericSecurityTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvOutput.CsvRowOutputWithHeaders csv, GenericSecurityTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "Security");
    csv.writeCell(SECURITY_ID_SCHEME_FIELD, trade.getSecurityId().getStandardId().getScheme());
    csv.writeCell(SECURITY_ID_FIELD, trade.getSecurityId().getStandardId().getValue());
    csv.writeCell(BUY_SELL_FIELD, trade.getQuantity() < 0 ? BuySell.SELL : BuySell.BUY);
    csv.writeCell(QUANTITY_FIELD, Math.abs(trade.getQuantity()));
    csv.writeCell(PRICE_FIELD, trade.getPrice());
    csv.writeCell(TICK_SIZE_FIELD, trade.getProduct().getInfo().getPriceInfo().getTickSize());
    csv.writeCell(CURRENCY_FIELD, trade.getProduct().getInfo().getPriceInfo().getTickValue().getCurrency());
    csv.writeCell(TICK_VALUE_FIELD, trade.getProduct().getInfo().getPriceInfo().getTickValue().getAmount());
    csv.writeCell(CONTRACT_SIZE_FIELD, trade.getProduct().getInfo().getPriceInfo().getContractSize());
    csv.writeNewLine();
  }

  @Override
  public String getName() {
    return GenericSecurityTrade.class.getSimpleName();
  }

  @Override
  public Set<String> supportedTradeTypes() {
    return ImmutableSet.of(GenericSecurityTrade.class.getSimpleName());
  }
}
