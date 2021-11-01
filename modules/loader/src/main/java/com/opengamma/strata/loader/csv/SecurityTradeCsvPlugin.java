/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

//-------------------------------------------------------------------------

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONTRACT_SIZE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXERCISE_PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PUT_CALL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TICK_SIZE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TICK_VALUE_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.DEFAULT_SECURITY_SCHEME;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.CsvOutput;
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
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.BillTrade;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.etd.EtdFutureTrade;
import com.opengamma.strata.product.etd.EtdOptionTrade;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.OvernightFutureTrade;

/**
 * Handles the CSV file format for Security trades.
 */
public class SecurityTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<SecurityQuantityTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final SecurityTradeCsvPlugin INSTANCE = new SecurityTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.of(
          SECURITY_ID_SCHEME_FIELD,
          SECURITY_ID_FIELD,
          BUY_SELL_FIELD,
          QUANTITY_FIELD,
          PRICE_FIELD);

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("SECURITY");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    boolean isGenericSecurityTradeRequired = requiredJavaType.isAssignableFrom(GenericSecurityTrade.class);
    if (isGenericSecurityTradeRequired || requiredJavaType.isAssignableFrom(SecurityTrade.class)) {
      // for legacy reasons, this code parses either a SecurityTrade or a GenericSecurityTrade
      // once parsed, if we parsed GenericSecurityTrade we downgrade it to SecurityTrade if necessary
      SecurityQuantityTrade parsed = resolver.parseSecurityTrade(baseRow, info);
      if (parsed instanceof GenericSecurityTrade && !isGenericSecurityTradeRequired) {
        parsed = SecurityTrade.of(parsed.getInfo(), parsed.getSecurityId(), parsed.getQuantity(), parsed.getPrice());
      }
      // the calling code does further checks to ensure the returned object is of the correct type
      return Optional.of(parsed);
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  // parses a trade from the CSV row
  static SecurityQuantityTrade parseTradeWithPriceInfo(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    SecurityTrade trade = parseSecurityTrade(row, info);
    SecurityTrade base = resolver.completeTrade(row, trade);

    Optional<Double> tickSizeOpt = row.findValue(TICK_SIZE_FIELD, LoaderUtils::parseDouble);
    Optional<Currency> currencyOpt = row.findValue(CURRENCY_FIELD, Currency::of);
    Optional<Double> tickValueOpt = row.findValue(TICK_VALUE_FIELD, LoaderUtils::parseDouble);
    double contractSize = row.findValue(CONTRACT_SIZE_FIELD, LoaderUtils::parseDouble).orElse(1d);
    if (tickSizeOpt.isPresent() && currencyOpt.isPresent() && tickValueOpt.isPresent()) {
      SecurityPriceInfo priceInfo =
          SecurityPriceInfo.of(tickSizeOpt.get(), CurrencyAmount.of(currencyOpt.get(), tickValueOpt.get()), contractSize);
      GenericSecurity sec = GenericSecurity.of(SecurityInfo.of(base.getSecurityId(), priceInfo));
      return GenericSecurityTrade.of(base.getInfo(), sec, base.getQuantity(), base.getPrice());
    }
    return base;
  }

  // parses a SecurityTrade from the CSV row
  private static SecurityTrade parseSecurityTrade(CsvRow row, TradeInfo info) {
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
    Optional<BuySell> buySellOpt = row.findValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
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

  @Override
  public Set<String> headers(List<SecurityQuantityTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvOutput.CsvRowOutputWithHeaders csv, SecurityQuantityTrade trade) {
    CsvWriterUtils.writeSecurityQuantityTrade(csv, trade);
    csv.writeNewLine();
  }

  @Override
  public String getName() {
    return SecurityTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(
        SecurityTrade.class,
        EtdFutureTrade.class,
        EtdOptionTrade.class,
        BillTrade.class,
        BondFutureOptionTrade.class,
        CapitalIndexedBondTrade.class,
        DsfTrade.class,
        FixedCouponBondTrade.class,
        IborFutureOptionTrade.class,
        IborFutureTrade.class,
        OvernightFutureTrade.class);
  }

  //Restricted constructor
  private SecurityTradeCsvPlugin(){
  }
}

