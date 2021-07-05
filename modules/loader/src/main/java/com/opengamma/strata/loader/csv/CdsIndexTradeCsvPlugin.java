/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CDS_INDEX_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CDS_INDEX_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEGAL_ENTITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEGAL_ENTITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;

/**
 * Handles the CSV file format for CDS index trades.
 */
final class CdsIndexTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<CdsIndexTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final CdsIndexTradeCsvPlugin INSTANCE = new CdsIndexTradeCsvPlugin();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("CDSINDEX", "CDS INDEX");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(CdsIndexTrade.class)) {
      return Optional.of(resolver.parseCdsIndexTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public Set<String> headers(List<CdsIndexTrade> trades) {
    // determine what elements of trades are present
    boolean premium = false;
    boolean stepInOffset = false;
    boolean settleOffset = false;
    boolean startConv = false;
    boolean endConv = false;
    boolean overrideStart = false;
    for (CdsIndexTrade trade : trades) {
      CdsIndex cds = trade.getProduct();
      PeriodicSchedule schedule = cds.getPaymentSchedule();
      premium |= trade.getUpfrontFee().isPresent();
      stepInOffset |= !cds.getStepinDateOffset().equals(DaysAdjustment.ofCalendarDays(1));
      settleOffset |= !cds.getSettlementDateOffset().equals(
          DaysAdjustment.ofBusinessDays(3, schedule.getBusinessDayAdjustment().getCalendar()));
      startConv |= schedule.getStartDateBusinessDayAdjustment().isPresent();
      endConv |= schedule.getEndDateBusinessDayAdjustment().isPresent();
      overrideStart |= schedule.getOverrideStartDate().isPresent();
    }
    return CdsTradeCsvPlugin.createHeaders(true, premium, stepInOffset, settleOffset, startConv, endConv, overrideStart);
  }

  @Override
  public void writeCsv(CsvOutput.CsvRowOutputWithHeaders csv, CdsIndexTrade trade) {
    CdsIndex product = trade.getProduct();
    csv.writeCell(TRADE_TYPE_FIELD, "CdsIndex");
    csv.writeCell(CDS_INDEX_ID_SCHEME_FIELD, product.getCdsIndexId().getScheme());
    csv.writeCell(CDS_INDEX_ID_FIELD, product.getCdsIndexId().getValue());
    String scheme;
    if (product.getLegalEntityIds().stream().map(StandardId::getScheme).distinct().count() == 1) {
      scheme = product.getLegalEntityIds().get(0).getScheme();
    } else {
      scheme = product.getLegalEntityIds().stream()
          .map(StandardId::getScheme)
          .collect(joining(";"));
    }
    csv.writeCell(LEGAL_ENTITY_ID_SCHEME_FIELD, scheme);
    csv.writeCell(LEGAL_ENTITY_ID_FIELD, product.getLegalEntityIds().stream()
        .map(StandardId::getValue)
        .collect(joining(";")));
    trade.getUpfrontFee().ifPresent(premium -> CsvWriterUtils.writePremiumFields(csv, premium));
    CdsTradeCsvPlugin.writeCdsDetails(
        csv,
        product.getBuySell(),
        product.getCurrency(),
        product.getNotional(),
        product.getFixedRate(),
        product.getDayCount(),
        product.getPaymentOnDefault(),
        product.getProtectionStart(),
        product.getStepinDateOffset(),
        product.getSettlementDateOffset(),
        product.getPaymentSchedule());
  }

  @Override
  public String getName() {
    return CdsIndexTrade.class.getSimpleName();
  }

  @Override
  public Set<String> supportedTradeTypes() {
    return ImmutableSet.of(CdsIndexTrade.class.getSimpleName());
  }

}
