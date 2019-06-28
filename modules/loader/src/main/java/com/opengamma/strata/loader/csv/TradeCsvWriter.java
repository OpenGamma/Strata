/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.CONTRACT_SIZE;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.TICK_SIZE;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.TICK_VALUE;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CCP_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CPTY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CPTY_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DESCRIPTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FRA_DISCOUNTING_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.ID_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.INTERPOLATED_INDEX_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NAME_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.SETTLEMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_TIME_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_ZONE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TYPE_FIELD;
import static java.util.stream.Collectors.groupingBy;

import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.loader.csv.SecurityCsvPlugin.GenericSecurityTradeCsvPlugin;
import com.opengamma.strata.loader.csv.SecurityCsvPlugin.SecurityTradeCsvPlugin;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.BillTrade;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.etd.EtdFutureTrade;
import com.opengamma.strata.product.etd.EtdOptionTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Writes trades to a CSV file.
 * <p>
 * This takes a Strata {@link Trade} instance and creates a matching CSV file.
 */
public final class TradeCsvWriter {

  /**
   * The writers.
   * The order of this map affects the order of columns in the CSV, though it is partially sorted later.
   */
  private static final ImmutableMap<Class<?>, TradeTypeCsvWriter<?>> WRITERS =
      ImmutableMap.<Class<?>, TradeTypeCsvWriter<?>>builder()
          .put(BulletPaymentTrade.class, BulletPaymentTradeCsvPlugin.INSTANCE)  // start with rates
          .put(FraTrade.class, FraTradeCsvPlugin.INSTANCE)
          .put(TermDepositTrade.class, TermDepositTradeCsvPlugin.INSTANCE)
          .put(SwapTrade.class, FullSwapTradeCsvPlugin.INSTANCE)  // then swap
          .put(FxSingleTrade.class, FxSingleTradeCsvPlugin.INSTANCE)  // then FX
          .put(FxSwapTrade.class, FxSwapTradeCsvPlugin.INSTANCE)
          .put(SwaptionTrade.class, SwaptionTradeCsvPlugin.INSTANCE)  // then options
          .put(FxVanillaOptionTrade.class, FxVanillaOptionTradeCsvPlugin.INSTANCE)
          .put(CdsTrade.class, CdsTradeCsvPlugin.CDS_INSTANCE)  // then credit
          .put(CdsIndexTrade.class, CdsTradeCsvPlugin.CDS_INDEX_INSTANCE)  // then credit
          .put(SecurityTrade.class, SecurityTradeCsvPlugin.INSTANCE)  // then securities
          .put(EtdFutureTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(EtdOptionTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(BillTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(BondFutureOptionTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(CapitalIndexedBondTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(DsfTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(FixedCouponBondTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(IborFutureOptionTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(IborFutureTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(OvernightFutureTrade.class, SecurityTradeCsvPlugin.INSTANCE)
          .put(GenericSecurityTrade.class, GenericSecurityTradeCsvPlugin.INSTANCE)
          .build();
  /**
   * The header order.
   * This sorts some of the columns (not all of them).
   */
  private static final ImmutableList<String> HEADER_ORDER = ImmutableList.of(
      TYPE_FIELD,
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
      TICK_SIZE,
      TICK_VALUE,
      CONTRACT_SIZE,
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
      TradeTypeCsvWriter detailsWriter = WRITERS.get(trade.getClass());
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
    headers.add(TYPE_FIELD);
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
    Map<Class<?>, List<Trade>> splitByType = trades.stream().collect(groupingBy(t -> t.getClass()));
    for (Entry<Class<?>, List<Trade>> entry : splitByType.entrySet()) {
      TradeTypeCsvWriter detailsWriter = WRITERS.get(entry.getKey());
      if (detailsWriter == null) {
        throw new IllegalArgumentException(
            "Unable to write trade to CSV: " + entry.getKey().getClass().getSimpleName());
      }
      headers.addAll(detailsWriter.headers((List) entry.getValue()));
    }
    return headers.stream().sorted(HEADER_COMPARATOR).collect(toImmutableList());
  }

}
