/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_LEVEL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.KNOCK_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.REBATE_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.REBATE_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.REBATE_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOptionTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Handles the CSV files format for FX Single Barrier Option trades.
 */
public class FxSingleBarrierOptionTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<FxSingleBarrierOptionTrade> {

  /** The singleton instance of the plugin. */
  public static final FxSingleBarrierOptionTradeCsvPlugin INSTANCE = new FxSingleBarrierOptionTradeCsvPlugin();

  /** The CSV headers. */
  public static final Set<String> HEADERS = ImmutableSet.<String>builder()
      .addAll(FxVanillaOptionTradeCsvPlugin.INSTANCE.headers(ImmutableList.of()))
      .add(BARRIER_TYPE_FIELD)
      .add(KNOCK_TYPE_FIELD)
      .add(BARRIER_LEVEL_FIELD)
      .add(REBATE_AMOUNT_FIELD)
      .add(REBATE_CURRENCY_FIELD)
      .add(REBATE_DIRECTION_FIELD)
      .build();

  /** Restricted constructor */
  private FxSingleBarrierOptionTradeCsvPlugin(){
  }

  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("FXSINGLEBARRIEROPTION", "FX SINGLE BARRIER OPTION");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(FxSingleBarrierOptionTrade.class)) {
      return Optional.of(parse(baseRow, info, resolver));
    }
    return Optional.empty();
  }

  private FxSingleBarrierOptionTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxVanillaOptionTrade vanillaOptionTrade = resolver.parseFxVanillaOptionTrade(row, info);

    BarrierType barrierType = row.getValue(BARRIER_TYPE_FIELD, LoaderUtils::parseBarrierType);
    KnockType knockType = row.getValue(KNOCK_TYPE_FIELD, LoaderUtils::parseKnockType);
    double barrierLevel = row.getValue(BARRIER_LEVEL_FIELD, LoaderUtils::parseDouble);
    AdjustablePayment premium = CsvLoaderUtils.parsePremiumFromDefaultFields(row);

    FxSingleBarrierOption.Builder productBuilder = FxSingleBarrierOption.builder()
        .underlyingOption(vanillaOptionTrade.getProduct())
        .barrier(SimpleConstantContinuousBarrier.of(barrierType, knockType, barrierLevel));

    if (CsvLoaderUtils.hasValidCurrencyAmount(
        row,
        REBATE_CURRENCY_FIELD,
        REBATE_AMOUNT_FIELD,
        REBATE_DIRECTION_FIELD)) {
      productBuilder.rebate(
          CsvLoaderUtils.parseCurrencyAmountWithDirection(row,
          REBATE_CURRENCY_FIELD,
          REBATE_AMOUNT_FIELD,
          REBATE_DIRECTION_FIELD));
    }

    return FxSingleBarrierOptionTrade.builder()
        .product(productBuilder.build())
        .premium(premium)
        .info(info)
        .build();
  }

  @Override
  public String getName() {
    return FxSingleBarrierOptionTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(FxSingleBarrierOptionTrade.class);
  }

  @Override
  public Set<String> headers(List<FxSingleBarrierOptionTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvOutput.CsvRowOutputWithHeaders csv, FxSingleBarrierOptionTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "FxSingleBarrierOption");
    writeSingleBarrierOption(csv, trade.getProduct());
    CsvWriterUtils.writePremiumFields(csv, trade.getPremium());
    csv.writeNewLine();
  }

  // Add a public static method in CsvWriterUtils when this is moved to Granite.
  protected void writeSingleBarrierOption(CsvOutput.CsvRowOutputWithHeaders csv, FxSingleBarrierOption product) {
    CsvWriterUtils.writeFxVanillaOption(csv, product.getUnderlyingOption());
    CsvWriterUtils.writeBarrierFields(csv, product.getBarrier(), LocalDate.now(ZoneId.systemDefault()));
    product.getRebate().ifPresent(ccyAmount -> CsvWriterUtils.writeCurrencyAmount(
        csv,
        ccyAmount,
        REBATE_AMOUNT_FIELD,
        REBATE_CURRENCY_FIELD,
        REBATE_DIRECTION_FIELD));
  }
}
