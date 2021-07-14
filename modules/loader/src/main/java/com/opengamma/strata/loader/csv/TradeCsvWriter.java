/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CCP_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONTRACT_SIZE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CPTY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CPTY_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DESCRIPTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FRA_DISCOUNTING_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INTERPOLATED_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NAME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SETTLEMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TICK_SIZE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TICK_VALUE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TIME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_ZONE_FIELD;
import static java.util.stream.Collectors.groupingBy;

import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

/**
 * Writes trades to a CSV file.
 * <p>
 * This takes a Strata {@link Trade} instance and creates a matching CSV file.
 */
public final class TradeCsvWriter {

  /**
   * The lookup of trade parsers.
   */
  static final ExtendedEnum<TradeCsvWriterPlugin> ENUM_LOOKUP = ExtendedEnum.of(TradeCsvWriterPlugin.class);

  /**
   * The lookup of trade parsers.
   */
  private static final ImmutableMap<Class<?>, TradeCsvWriterPlugin> PLUGINS =
      MapStream.of(TradeCsvWriterPlugin.extendedEnum().lookupAllNormalized().values())
          .flatMapKeys(plugin -> plugin.supportedTradeTypes().stream())
          .toMap((a, b) -> {
            System.err.println("Two plugins declare the same product type: " + ((TradeCsvWriterPlugin) a).supportedTradeTypes());
            return a;
          });

  /**
   * The header order.
   * This sorts some of the columns (not all of them).
   */
  private static final ImmutableList<String> HEADER_ORDER = ImmutableList.of(
      TRADE_TYPE_FIELD,
      ID_SCHEME_FIELD,
      ID_FIELD,
      DESCRIPTION_FIELD,
      NAME_FIELD,
      CCP_FIELD,
      CPTY_SCHEME_FIELD,
      CPTY_FIELD,
      TRADE_DATE_FIELD,
      TRADE_TIME_FIELD,
      TRADE_ZONE_FIELD,
      SETTLEMENT_DATE_FIELD,
      BUY_SELL_FIELD,
      DIRECTION_FIELD,
      QUANTITY_FIELD,
      SECURITY_ID_SCHEME_FIELD,
      SECURITY_ID_FIELD,
      PRICE_FIELD,
      TICK_SIZE_FIELD,
      TICK_VALUE_FIELD,
      CONTRACT_SIZE_FIELD,
      CURRENCY_FIELD,
      NOTIONAL_FIELD,
      START_DATE_FIELD,
      END_DATE_FIELD,
      DAY_COUNT_FIELD,
      FIXED_RATE_FIELD,
      INDEX_FIELD,
      INTERPOLATED_INDEX_FIELD,
      FRA_DISCOUNTING_FIELD,
      DATE_ADJ_CNV_FIELD,
      DATE_ADJ_CAL_FIELD,
      PAYMENT_DATE_FIELD,
      PAYMENT_DATE_CNV_FIELD,
      PAYMENT_DATE_CAL_FIELD);
  /**
   * The header comparator.
   */
  private static final Comparator<String> HEADER_COMPARATOR = (str1, str2) -> {
    int index1 = HEADER_ORDER.indexOf(str1);
    int index2 = HEADER_ORDER.indexOf(str2);
    int i1 = index1 >= 0 ? index1 : HEADER_ORDER.size();
    int i2 = index2 >= 0 ? index2 : HEADER_ORDER.size();
    return i1 - i2;
  };

  /**
   * The supplier, providing additional information.
   */
  private final TradeCsvInfoSupplier supplier;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that uses the standard set of reference data.
   *
   * @return the loader
   */
  public static TradeCsvWriter standard() {
    return new TradeCsvWriter(TradeCsvInfoSupplier.standard());
  }

  /**
   * Obtains an instance that uses the specified supplier for additional information.
   *
   * @param supplier  the supplier used to extract additional information to output
   * @return the loader
   */
  public static TradeCsvWriter of(TradeCsvInfoSupplier supplier) {
    return new TradeCsvWriter(supplier);
  }

  // restricted constructor
  private TradeCsvWriter(TradeCsvInfoSupplier supplier) {
    this.supplier = ArgChecker.notNull(supplier, "supplier");
  }

  //-------------------------------------------------------------------------
  /**
   * Write trades to an appendable in the applicable full details trade format.
   * <p>
   * The output is written in full details trade format.
   *
   * @param trades  the trades to write
   * @param output  the appendable to write to
   * @throws IllegalArgumentException if the metadata does not contain tenors
   * @throws UncheckedIOException if an IO error occurs
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void write(List<? extends Trade> trades, Appendable output) {
    List<String> headers = headers(trades);
    CsvRowOutputWithHeaders csv = CsvOutput.standard(output, "\n").withHeaders(headers, false);
    for (Trade trade : trades) {
      TradeInfo info = trade.getInfo();
      info.getId().ifPresent(id -> csv.writeCell(ID_SCHEME_FIELD, id.getScheme()));
      info.getId().ifPresent(id -> csv.writeCell(ID_FIELD, id.getValue()));
      info.findAttribute(AttributeType.DESCRIPTION).ifPresent(str -> csv.writeCell(DESCRIPTION_FIELD, str));
      info.findAttribute(AttributeType.NAME).ifPresent(str -> csv.writeCell(NAME_FIELD, str));
      info.findAttribute(AttributeType.CCP).ifPresent(str -> csv.writeCell(CCP_FIELD, str));
      info.getCounterparty().ifPresent(cpty -> csv.writeCell(CPTY_SCHEME_FIELD, cpty.getScheme()));
      info.getCounterparty().ifPresent(cpty -> csv.writeCell(CPTY_FIELD, cpty.getValue()));
      info.getTradeDate().ifPresent(date -> csv.writeCell(TRADE_DATE_FIELD, date.toString()));
      info.getTradeTime().ifPresent(time -> csv.writeCell(TRADE_TIME_FIELD, time.toString()));
      info.getZone().ifPresent(zone -> csv.writeCell(TRADE_ZONE_FIELD, zone.toString()));
      info.getSettlementDate().ifPresent(date -> csv.writeCell(SETTLEMENT_DATE_FIELD, date.toString()));
      csv.writeCells(supplier.values(headers, trade));
      TradeCsvWriterPlugin detailsWriter = PLUGINS.get(trade.getClass());
      if (detailsWriter == null) {
        throw new IllegalArgumentException("Unable to write trade to CSV: " + trade.getClass().getSimpleName());
      }
      detailsWriter.writeCsv(csv, trade);
    }
  }

  // collect the set of headers that are needed
  @SuppressWarnings({"rawtypes", "unchecked"})
  private List<String> headers(List<? extends Trade> trades) {
    Set<String> headers = new LinkedHashSet<>();

    // common headers
    headers.add(TRADE_TYPE_FIELD);
    if (trades.stream().anyMatch(trade -> trade.getInfo().getId().isPresent())) {
      headers.add(ID_SCHEME_FIELD);
      headers.add(ID_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().findAttribute(AttributeType.DESCRIPTION).isPresent())) {
      headers.add(DESCRIPTION_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().findAttribute(AttributeType.NAME).isPresent())) {
      headers.add(NAME_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().findAttribute(AttributeType.CCP).isPresent())) {
      headers.add(CCP_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().getCounterparty().isPresent())) {
      headers.add(CPTY_SCHEME_FIELD);
      headers.add(CPTY_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().getTradeDate().isPresent())) {
      headers.add(TRADE_DATE_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().getTradeTime().isPresent())) {
      headers.add(TRADE_TIME_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().getZone().isPresent())) {
      headers.add(TRADE_ZONE_FIELD);
    }
    if (trades.stream().anyMatch(trade -> trade.getInfo().getSettlementDate().isPresent())) {
      headers.add(SETTLEMENT_DATE_FIELD);
    }

    // additional headers
    headers.addAll(trades.stream()
        .flatMap(trade -> supplier.headers(trade).stream())
        .collect(toImmutableList()));

    // types
    Map<Class<?>, List<Trade>> splitByType = trades.stream().collect(groupingBy(
        Object::getClass,
        LinkedHashMap<Class<?>, List<Trade>>::new,
        Collectors.<Trade>toList()));
    for (Entry<Class<?>, List<Trade>> entry : splitByType.entrySet()) {
      TradeCsvWriterPlugin detailsWriter = PLUGINS.get(entry.getKey());
      if (detailsWriter == null) {
        throw new IllegalArgumentException(
            "Unable to write trade type to CSV: " + entry.getKey().getSimpleName());
      }
      headers.addAll(detailsWriter.headers(entry.getValue()));
    }
    return headers.stream().sorted(HEADER_COMPARATOR).collect(toImmutableList());
  }

}
