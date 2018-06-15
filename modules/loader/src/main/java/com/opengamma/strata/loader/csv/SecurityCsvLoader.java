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

import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityPosition;
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
 * Loads security trades from CSV files.
 */
final class SecurityCsvLoader {

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
    double price = LoaderUtils.parseDouble(row.getValue(PRICE_FIELD));
    double quantity = parseTradeQuantity(row);
    return SecurityTrade.of(info, securityId, quantity, price);
  }

  // parses the trade quantity, considering the optional buy/sell field
  private static double parseTradeQuantity(CsvRow row) {
    double quantity = LoaderUtils.parseDouble(row.getValue(QUANTITY_FIELD));
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
      return parseNonEtdPosition(row, info, resolver);
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
      return parseSecurityPosition(row, info, resolver);
    }
  }

  // parses the base SecurityPosition
  static SecurityPosition parseSecurityPosition(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    String securityIdScheme = row.findValue(SECURITY_ID_SCHEME_FIELD).orElse(DEFAULT_SECURITY_SCHEME);
    String securityIdValue = row.getValue(SECURITY_ID_FIELD);
    SecurityId securityId = SecurityId.of(securityIdScheme, securityIdValue);
    DoublesPair quantity = CsvLoaderUtils.parseQuantity(row);
    SecurityPosition position = SecurityPosition.ofLongShort(info, securityId, quantity.getFirst(), quantity.getSecond());
    return resolver.completePosition(row, position);
  }

  // parses the additional GenericSecurityPosition information
  static Position parseNonEtdPosition(CsvRow row, PositionInfo info, PositionCsvInfoResolver resolver) {
    SecurityPosition base = parseSecurityPosition(row, info, resolver);
    Optional<Double> tickSizeOpt = row.findValue(TICK_SIZE).map(str -> LoaderUtils.parseDouble(str));
    Optional<Currency> currencyOpt = row.findValue(CURRENCY).map(str -> Currency.of(str));
    Optional<Double> tickValueOpt = row.findValue(TICK_VALUE).map(str -> LoaderUtils.parseDouble(str));
    double contractSize = row.findValue(CONTRACT_SIZE).map(str -> LoaderUtils.parseDouble(str)).orElse(1d);
    if (tickSizeOpt.isPresent() && currencyOpt.isPresent() && tickValueOpt.isPresent()) {
      SecurityPriceInfo priceInfo =
          SecurityPriceInfo.of(tickSizeOpt.get(), CurrencyAmount.of(currencyOpt.get(), tickValueOpt.get()), contractSize);
      GenericSecurity sec = GenericSecurity.of(SecurityInfo.of(base.getSecurityId(), priceInfo));
      return GenericSecurityPosition.ofLongShort(base.getInfo(), sec, base.getLongQuantity(), base.getShortQuantity());
    }
    return base;
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SecurityCsvLoader() {
  }

}
