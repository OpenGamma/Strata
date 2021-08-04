/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.zip;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CDS_INDEX_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CDS_INDEX_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INDEX_SERIES_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INDEX_VERSION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEGAL_ENTITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEGAL_ENTITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RED_CODE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.JodaBeanUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;

/**
 * Handles the CSV file format for CDS index trades.
 */
final class CdsIndexTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<CdsIndexTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final CdsIndexTradeCsvPlugin INSTANCE = new CdsIndexTradeCsvPlugin();

  private static final String DEFAULT_CDS_INDEX_SCHEME = "OG-CDS";
  private static final String DEFAULT_LEGAL_ENTITY_SCHEME = "OG-Entity";
  private static final String DEFAULT_TICKER_SCHEME = "OG-Ticker";

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

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   *
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static CdsIndexTrade parseCdsIndex(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    StandardId indexId = parseCdsIndexId(row);
    // handle either one scheme for all IDs, or one scheme for each ID
    List<String> entitySchemeStrs = Splitter.on(';')
        .splitToList(row.findValue(LEGAL_ENTITY_ID_SCHEME_FIELD).orElse(DEFAULT_LEGAL_ENTITY_SCHEME));
    List<String> entityIdStrs = row.findValue(LEGAL_ENTITY_ID_FIELD)
        .map(Splitter.on(';')::splitToList)
        .orElse(ImmutableList.of());
    List<StandardId> entityIds;
    if (entityIdStrs.size() == 0) {
      entityIds = ImmutableList.of();
    } else if (entitySchemeStrs.size() >= entityIdStrs.size()) {
      entityIds = zip(entitySchemeStrs.stream(), entityIdStrs.stream())
          .map(pair -> StandardId.of(pair.getFirst(), pair.getSecond()))
          .collect(toImmutableList());
    } else {
      String entityScheme = entitySchemeStrs.get(0);
      entityIds = entityIdStrs.stream()
          .map(entityIdStr -> StandardId.of(entityScheme, entityIdStr))
          .collect(toImmutableList());
    }
    CdsTrade cdsTrade = CdsTradeCsvPlugin.parseCdsRow(row, info, indexId, resolver);
    Cds cds = cdsTrade.getProduct();
    CdsIndex.Builder indexBuilder = CdsIndex.builder()
        .cdsIndexId(indexId)
        .legalEntityIds(entityIds);
    JodaBeanUtils.copyInto(cds, CdsIndex.meta(), indexBuilder);
    CdsIndexTrade trade = CdsIndexTrade.builder()
        .info(info)
        .product(indexBuilder.build())
        .upfrontFee(cdsTrade.getUpfrontFee().orElse(null))
        .build();
    return resolver.completeTrade(row, trade);
  }

  private static StandardId parseCdsIndexId(CsvRow row) {
    Optional<String> redCodeOpt = row.findValue(RED_CODE_FIELD);
    Optional<String> seriesOpt = row.findValue(INDEX_SERIES_FIELD);
    Optional<String> versionOpt = row.findValue(INDEX_VERSION_FIELD);
    if (redCodeOpt.isPresent() && seriesOpt.isPresent() && versionOpt.isPresent()) {
      StandardId redCode = LoaderUtils.parseRedCode(redCodeOpt.get());
      String stem = redCode.getValue() + "-CDX";
      String variant = "S" + seriesOpt.get() + "V" + versionOpt.get();
      return StandardId.of(DEFAULT_TICKER_SCHEME, stem + '-' + variant);
    }
    String indexScheme = row.findValue(CDS_INDEX_ID_SCHEME_FIELD).orElse(DEFAULT_CDS_INDEX_SCHEME);
    return StandardId.of(indexScheme, row.getValue(CDS_INDEX_ID_FIELD));
  }

  //-------------------------------------------------------------------------
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
    return CdsTradeCsvPlugin.createHeaders(
        true,
        premium,
        stepInOffset,
        settleOffset,
        startConv,
        endConv,
        overrideStart);
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
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(CdsIndexTrade.class);
  }

}
