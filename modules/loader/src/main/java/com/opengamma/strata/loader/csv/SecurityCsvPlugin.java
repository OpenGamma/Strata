/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderUtils.CONTRACT_SIZE;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.CURRENCY;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXERCISE_PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.EXPIRY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.PUT_CALL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.TICK_SIZE;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.TICK_VALUE;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.DEFAULT_SECURITY_SCHEME;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.SecurityQuantityTrade;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * Handles the CSV file format for security trades.
 */
final class SecurityCsvPlugin {

  // parses a trade from the CSV row
  static SecurityQuantityTrade parseTrade(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    SecurityTrade trade = parseSecurityTrade(row, info, resolver);
    SecurityTrade base = resolver.completeTrade(row, trade);

    Optional<Double> tickSizeOpt = row.findValue(TICK_SIZE).map(str -> LoaderUtils.parseDouble(str));
    Optional<Currency> currencyOpt = row.findValue(CURRENCY).map(str -> Currency.of(str));
    Optional<Double> tickValueOpt = row.findValue(TICK_VALUE).map(str -> LoaderUtils.parseDouble(str));
    double contractSize = row.findValue(CONTRACT_SIZE).map(str -> LoaderUtils.parseDouble(str)).orElse(1d);
    if (tickSizeOpt.isPresent() && currencyOpt.isPresent() && tickValueOpt.isPresent()) {
      SecurityPriceInfo priceInfo =
          SecurityPriceInfo.of(tickSizeOpt.get(), CurrencyAmount.of(currencyOpt.get(), tickValueOpt.get()), contractSize);
      GenericSecurity sec = GenericSecurity.of(SecurityInfo.of(base.getSecurityId(), priceInfo));
      return GenericSecurityTrade.of(base.getInfo(), sec, base.getQuantity(), base.getPrice());
    }
    return base;
  }

  // parses a SecurityTrade from the CSV row
  private static SecurityTrade parseSecurityTrade(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    String securityIdScheme = row.findValue(SECURITY_ID_SCHEME_FIELD).orElse(DEFAULT_SECURITY_SCHEME);
    String securityIdValue = row.getValue(SECURITY_ID_FIELD);
    SecurityId securityId = SecurityId.of(securityIdScheme, securityIdValue);
    double price = row.getValue(PRICE_FIELD, LoaderUtils::parseDouble);
    double quantity = parseTradeQuantity(row);
    return SecurityTrade.of(info, securityId, quantity, price);
  }

  // parses the trade quantity, considering the optional buy/sell field
  private static double parseTradeQuantity(CsvRow row) {
    double quantity = row.getValue(QUANTITY_FIELD, LoaderUtils::parseDouble);
    Optional<BuySell> buySellOpt = row.findValue(BUY_SELL_FIELD).map(str -> LoaderUtils.parseBuySell(str));
    if (buySellOpt.isPresent()) {
      quantity = buySellOpt.get().normalize(quantity);
    }
    return quantity;
  }

  //-------------------------------------------------------------------------
  // parses a position from the CSV row
  static Position parsePosition(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    if (row.findValue(EXPIRY_FIELD).isPresent()) {
      // etd
      if (row.findValue(PUT_CALL_FIELD).isPresent() || row.findValue(EXERCISE_PRICE_FIELD).isPresent()) {
        return resolver.parseEtdOptionPosition(row, info);
      } else {
        return resolver.parseEtdFuturePosition(row, info);
      }
    } else {
      return resolver.parseNonEtdPosition(row, info);
    }
  }

  // parses a SecurityPosition from the CSV row, converting ETD information
  static SecurityPosition parsePositionLightweight(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    if (row.findValue(EXPIRY_FIELD).isPresent()) {
      // etd
      if (row.findValue(PUT_CALL_FIELD).isPresent() || row.findValue(EXERCISE_PRICE_FIELD).isPresent()) {
        return resolver.parseEtdOptionSecurityPosition(row, info);
      } else {
        return resolver.parseEtdFutureSecurityPosition(row, info);
      }
    } else {
      // simple
      return resolver.parseNonEtdSecurityPosition(row, info);
    }
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SecurityCsvPlugin() {
  }

  //-------------------------------------------------------------------------
  /** The writer for security trade. */
  static class SecurityTradeCsvPlugin implements TradeTypeCsvWriter<SecurityQuantityTrade> {

    /**
     * The singleton instance of the plugin.
     */
    public static final SecurityTradeCsvPlugin INSTANCE = new SecurityTradeCsvPlugin();

    /** The headers. */
    private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
        .add(SECURITY_ID_SCHEME_FIELD)
        .add(SECURITY_ID_FIELD)
        .add(BUY_SELL_FIELD)
        .add(QUANTITY_FIELD)
        .add(PRICE_FIELD)
        .build();

    @Override
    public List<String> headers(List<SecurityQuantityTrade> trades) {
      return HEADERS;
    }

    @Override
    public void writeCsv(CsvRowOutputWithHeaders csv, SecurityQuantityTrade trade) {
      csv.writeCell(TradeCsvLoader.TYPE_FIELD, "Security");
      csv.writeCell(SECURITY_ID_SCHEME_FIELD, trade.getSecurityId().getStandardId().getScheme());
      csv.writeCell(SECURITY_ID_FIELD, trade.getSecurityId().getStandardId().getValue());
      csv.writeCell(BUY_SELL_FIELD, trade.getQuantity() < 0 ? BuySell.SELL : BuySell.BUY);
      csv.writeCell(QUANTITY_FIELD, Math.abs(trade.getQuantity()));
      csv.writeCell(PRICE_FIELD, trade.getPrice());
      csv.writeNewLine();
    }
  }

  //-------------------------------------------------------------------------
  /** The writer for security trade. */
  static class GenericSecurityTradeCsvPlugin implements TradeTypeCsvWriter<GenericSecurityTrade> {

    /**
     * The singleton instance of the plugin.
     */
    public static final GenericSecurityTradeCsvPlugin INSTANCE = new GenericSecurityTradeCsvPlugin();

    /** The headers. */
    private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
        .add(SECURITY_ID_SCHEME_FIELD)
        .add(SECURITY_ID_FIELD)
        .add(BUY_SELL_FIELD)
        .add(QUANTITY_FIELD)
        .add(PRICE_FIELD)
        .add(TICK_SIZE)
        .add(CURRENCY)
        .add(TICK_VALUE)
        .add(CONTRACT_SIZE)
        .build();

    @Override
    public List<String> headers(List<GenericSecurityTrade> trades) {
      return HEADERS;
    }

    @Override
    public void writeCsv(CsvRowOutputWithHeaders csv, GenericSecurityTrade trade) {
      csv.writeCell(TradeCsvLoader.TYPE_FIELD, "Security");
      csv.writeCell(SECURITY_ID_SCHEME_FIELD, trade.getSecurityId().getStandardId().getScheme());
      csv.writeCell(SECURITY_ID_FIELD, trade.getSecurityId().getStandardId().getValue());
      csv.writeCell(BUY_SELL_FIELD, trade.getQuantity() < 0 ? BuySell.SELL : BuySell.BUY);
      csv.writeCell(QUANTITY_FIELD, Math.abs(trade.getQuantity()));
      csv.writeCell(PRICE_FIELD, trade.getPrice());
      csv.writeCell(TICK_SIZE, trade.getProduct().getInfo().getPriceInfo().getTickSize());
      csv.writeCell(CURRENCY, trade.getProduct().getInfo().getPriceInfo().getTickValue().getCurrency());
      csv.writeCell(TICK_VALUE, trade.getProduct().getInfo().getPriceInfo().getTickValue().getAmount());
      csv.writeCell(CONTRACT_SIZE, trade.getProduct().getInfo().getPriceInfo().getContractSize());
      csv.writeNewLine();
    }
  }

}
