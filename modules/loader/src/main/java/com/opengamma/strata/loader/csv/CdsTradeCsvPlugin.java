/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CDS_INDEX_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CDS_INDEX_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIRST_REGULAR_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FREQUENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LAST_REGULAR_END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEGAL_ENTITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEGAL_ENTITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_ON_DEFAULT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PROTECTION_START_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.RED_CODE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ROLL_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SENIORITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SETTLEMENT_DATE_OFFSET_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SETTLEMENT_DATE_OFFSET_DAYS_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STEP_IN_DATE_OFFSET_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STEP_IN_DATE_OFFSET_DAYS_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STUB_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedPercentage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.type.CdsConvention;

/**
 * Handles the CSV file format for CDS trades.
 */
final class CdsTradeCsvPlugin implements TradeCsvParserPlugin, TradeCsvWriterPlugin<CdsTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final CdsTradeCsvPlugin INSTANCE = new CdsTradeCsvPlugin();

  private static final String DEFAULT_LEGAL_ENTITY_SCHEME = "OG-Entity";
  private static final String DEFAULT_TICKER_SCHEME = "OG-Ticker";

  private static final Set<String> ACCEPTED_SENIORITY_VALUES = ImmutableSet.of(
      "JRSUBUT2", "PREFT1", "SECDOM", "SNRLAC", "SNRFOR", "SUBLT2");
  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("CDS");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(CdsTrade.class)) {
      return Optional.of(resolver.parseCdsTrade(baseRow, info));
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
  static CdsTrade parseCds(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    StandardId entityId = parseLegalEntityId(row);
    CdsTrade trade = parseCdsRow(row, info, entityId, resolver);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  static CdsTrade parseCdsRow(CsvRow row, TradeInfo info, StandardId entityId, TradeCsvInfoResolver resolver) {
    BuySell buySell = row.getValue(BUY_SELL_FIELD, LoaderUtils::parseBuySell);
    double notional = row.getValue(NOTIONAL_FIELD, LoaderUtils::parseDouble);
    double fixedRate = row.getValue(FIXED_RATE_FIELD, LoaderUtils::parseDoublePercent);
    Optional<Tenor> tenorOpt = row.findValue(TENOR_FIELD, LoaderUtils::parseTenor);
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD, LoaderUtils::parseDate);
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD, LoaderUtils::parseDate);
    Optional<AdjustablePayment> premiumOpt = row.findValue(PREMIUM_DIRECTION_FIELD)
        .map(ignored -> {
          CurrencyAmount amount = CsvLoaderUtils.parseCurrencyAmountWithDirection(
              row, PREMIUM_CURRENCY_FIELD, PREMIUM_AMOUNT_FIELD, PREMIUM_DIRECTION_FIELD);
          AdjustableDate date = CsvLoaderUtils.parseAdjustableDate(
              row, PREMIUM_DATE_FIELD, PREMIUM_DATE_CNV_FIELD, PREMIUM_DATE_CAL_FIELD);
          return AdjustablePayment.of(amount, date);
        });

    // parse by convention
    Optional<CdsConvention> conventionOpt = row.findValue(CONVENTION_FIELD, CdsConvention::of);
    if (conventionOpt.isPresent()) {
      CdsConvention convention = conventionOpt.get();
      // explicit dates take precedence over relative ones
      if (startDateOpt.isPresent() && endDateOpt.isPresent()) {
        if (tenorOpt.isPresent()) {
          throw new IllegalArgumentException(
              "CDS trade had invalid combination of fields. When these fields are found " +
                  ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
                  " then these fields must not be present " +
                  ImmutableList.of(TENOR_FIELD));
        }
        LocalDate startDate = startDateOpt.get();
        LocalDate endDate = endDateOpt.get();
        if (premiumOpt.isPresent()) {
          return convention.toTrade(entityId, info, startDate, endDate, buySell, notional, fixedRate, premiumOpt.get());
        } else {
          return convention.toTrade(entityId, info, startDate, endDate, buySell, notional, fixedRate);
        }

      }
      // relative dates
      if (tenorOpt.isPresent() && info.getTradeDate().isPresent()) {
        if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
          throw new IllegalArgumentException(
              "CDS trade had invalid combination of fields. When these fields are found " +
                  ImmutableList.of(CONVENTION_FIELD, TENOR_FIELD) +
                  " then these fields must not be present " +
                  ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD));
        }
        Tenor tenor = tenorOpt.get();
        LocalDate tradeDate = info.getTradeDate().get();
        ReferenceData refData = resolver.getReferenceData();
        if (premiumOpt.isPresent()) {
          return convention
              .createTrade(entityId, tradeDate, tenor, buySell, notional, fixedRate, premiumOpt.get(), refData)
              .withInfo(info);
        } else {
          return convention.createTrade(entityId, tradeDate, tenor, buySell, notional, fixedRate, refData)
              .withInfo(info);
        }
      }
      // no match
      throw new IllegalArgumentException(
          "CDS trade had invalid combination of fields. These fields are mandatory:" +
              ImmutableList.of(BUY_SELL_FIELD, NOTIONAL_FIELD, FIXED_RATE_FIELD, LEGAL_ENTITY_ID_SCHEME_FIELD) +
              " and one of these combinations is mandatory: " +
              ImmutableList.of(CONVENTION_FIELD, TRADE_DATE_FIELD, TENOR_FIELD) +
              " or " +
              ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD));
    }

    // parse by full details
    Currency currency = row.getValue(CURRENCY_FIELD, LoaderUtils::parseCurrency);
    Cds.Builder cdsBuilder = Cds.builder()
        .buySell(buySell)
        .legalEntityId(entityId)
        .currency(currency)
        .notional(notional)
        .fixedRate(fixedRate)
        .paymentSchedule(parseSchedule(row, currency));

    row.findValue(DAY_COUNT_FIELD, LoaderUtils::parseDayCount).ifPresent(cdsBuilder::dayCount);
    row.findValue(PAYMENT_ON_DEFAULT_FIELD, PaymentOnDefault::of).ifPresent(cdsBuilder::paymentOnDefault);
    row.findValue(PROTECTION_START_FIELD, ProtectionStartOfDay::of).ifPresent(cdsBuilder::protectionStart);
    row.findValue(STEP_IN_DATE_OFFSET_DAYS_FIELD)
        .map(ignored -> CsvLoaderUtils.parseDaysAdjustment(
            row,
            STEP_IN_DATE_OFFSET_DAYS_FIELD,
            STEP_IN_DATE_OFFSET_CAL_FIELD,
            STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD,
            STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD))
        .ifPresent(stepIn -> cdsBuilder.stepinDateOffset(stepIn));
    row.findValue(SETTLEMENT_DATE_OFFSET_DAYS_FIELD)
        .map(ignored -> CsvLoaderUtils.parseDaysAdjustment(
            row,
            SETTLEMENT_DATE_OFFSET_DAYS_FIELD,
            SETTLEMENT_DATE_OFFSET_CAL_FIELD,
            SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD,
            SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD))
        .ifPresent(settleOffset -> cdsBuilder.settlementDateOffset(settleOffset));

    return CdsTrade.builder()
        .info(info)
        .product(cdsBuilder.build())
        .upfrontFee(premiumOpt.orElse(null))
        .build();
  }

  //-------------------------------------------------------------------------
  // parse the legal entity ID
  private static StandardId parseLegalEntityId(CsvRow row) {
    Optional<String> redCodeOpt = row.findValue(RED_CODE_FIELD);
    Optional<String> seniorityOpt = row.findValue(SENIORITY_FIELD);
    if (redCodeOpt.isPresent() && seniorityOpt.isPresent()) {
      StandardId redCode = LoaderUtils.parseRedCode(redCodeOpt.get());
      String stem = redCode.getValue().substring(0, 6) + "-CDS";
      return StandardId.of(DEFAULT_TICKER_SCHEME, stem + '-' + parseSeniority(seniorityOpt.get()));
    }
    String entityScheme = row.findValue(LEGAL_ENTITY_ID_SCHEME_FIELD).orElse(DEFAULT_LEGAL_ENTITY_SCHEME);
    return StandardId.of(entityScheme, row.getValue(LEGAL_ENTITY_ID_FIELD));
  }

  // checks seniority is valid
  private static String parseSeniority(String seniority) {
    String seniorityUpper = seniority.toUpperCase(Locale.ENGLISH);
    if (ACCEPTED_SENIORITY_VALUES.contains(seniorityUpper)) {
      return seniorityUpper;
    }
    throw new IllegalArgumentException("Unknown Seniority value, must be : " + ACCEPTED_SENIORITY_VALUES + " but was '"
        + seniority + "'. The parser is case insensitive.");
  }

  // accrual schedule
  private static PeriodicSchedule parseSchedule(CsvRow row, Currency currency) {
    PeriodicSchedule.Builder builder = PeriodicSchedule.builder();
    // basics
    builder.startDate(row.getValue(START_DATE_FIELD, LoaderUtils::parseDate));
    builder.endDate(row.getValue(END_DATE_FIELD, LoaderUtils::parseDate));
    builder.frequency(Frequency.parse(row.getValue(FREQUENCY_FIELD)));
    // adjustments
    BusinessDayAdjustment dateAdj =
        CsvLoaderUtils.parseBusinessDayAdjustment(row, DATE_ADJ_CNV_FIELD, DATE_ADJ_CAL_FIELD)
            .orElse(BusinessDayAdjustment.NONE);
    builder.businessDayAdjustment(dateAdj);
    CsvLoaderUtils.parseBusinessDayAdjustment(row, START_DATE_CNV_FIELD, START_DATE_CAL_FIELD)
        .ifPresent(bda -> builder.startDateBusinessDayAdjustment(bda));
    CsvLoaderUtils.parseBusinessDayAdjustment(row, END_DATE_CNV_FIELD, END_DATE_CAL_FIELD)
        .ifPresent(bda -> builder.endDateBusinessDayAdjustment(bda));
    // optionals
    builder.stubConvention(row.findValue(STUB_CONVENTION_FIELD, StubConvention::of)
        .orElse(StubConvention.SMART_INITIAL));
    row.findValue(ROLL_CONVENTION_FIELD, LoaderUtils::parseRollConvention).ifPresent(builder::rollConvention);
    row.findValue(FIRST_REGULAR_START_DATE_FIELD, LoaderUtils::parseDate).ifPresent(builder::firstRegularStartDate);
    row.findValue(LAST_REGULAR_END_DATE_FIELD, LoaderUtils::parseDate).ifPresent(builder::lastRegularEndDate);
    Optional<AdjustableDate> overrideDateOpt = row.findValue(OVERRIDE_START_DATE_FIELD)
        .map(ignored -> CsvLoaderUtils.parseAdjustableDate(
            row,
            OVERRIDE_START_DATE_FIELD,
            OVERRIDE_START_DATE_CNV_FIELD,
            OVERRIDE_START_DATE_CAL_FIELD,
            BusinessDayConventions.MODIFIED_FOLLOWING,
            currency));
    overrideDateOpt.ifPresent(d -> builder.overrideStartDate(d));
    return builder.build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<String> headers(List<CdsTrade> trades) {
    // determine what elements of trades are present
    boolean premium = false;
    boolean stepInOffset = false;
    boolean settleOffset = false;
    boolean startConv = false;
    boolean endConv = false;
    boolean overrideStart = false;
    for (CdsTrade trade : trades) {
      Cds cds = trade.getProduct();
      PeriodicSchedule schedule = cds.getPaymentSchedule();
      premium |= trade.getUpfrontFee().isPresent();
      stepInOffset |= !cds.getStepinDateOffset().equals(DaysAdjustment.ofCalendarDays(1));
      settleOffset |= !cds.getSettlementDateOffset().equals(
          DaysAdjustment.ofBusinessDays(3, schedule.getBusinessDayAdjustment().getCalendar()));
      startConv |= schedule.getStartDateBusinessDayAdjustment().isPresent();
      endConv |= schedule.getEndDateBusinessDayAdjustment().isPresent();
      overrideStart |= schedule.getOverrideStartDate().isPresent();
    }
    return createHeaders(
        false,
        premium,
        stepInOffset,
        settleOffset,
        startConv,
        endConv,
        overrideStart);
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, CdsTrade trade) {
    Cds product = trade.getProduct();
    csv.writeCell(TRADE_TYPE_FIELD, "Cds");
    csv.writeCell(LEGAL_ENTITY_ID_SCHEME_FIELD, product.getLegalEntityId().getScheme());
    csv.writeCell(LEGAL_ENTITY_ID_FIELD, product.getLegalEntityId().getValue());
    trade.getUpfrontFee().ifPresent(premium -> CsvWriterUtils.writePremiumFields(csv, premium));
    writeCdsDetails(
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
    return CdsTrade.class.getSimpleName();
  }

  @Override
  public Set<Class<?>> supportedTradeTypes() {
    return ImmutableSet.of(CdsTrade.class);
  }

  // Restricted constructor.
  private CdsTradeCsvPlugin() {
  }

  // creates the correct set of headers
  static LinkedHashSet<String> createHeaders(
      boolean cdsIndex,
      boolean premium,
      boolean stepInOffset,
      boolean settleOffset,
      boolean startConv,
      boolean endConv,
      boolean overrideStart) {

    // select the headers
    LinkedHashSet<String> headers = new LinkedHashSet<>();
    headers.add(BUY_SELL_FIELD);
    headers.add(CURRENCY_FIELD);
    headers.add(NOTIONAL_FIELD);
    headers.add(FIXED_RATE_FIELD);
    if (cdsIndex) {
      headers.add(CDS_INDEX_ID_SCHEME_FIELD);
      headers.add(CDS_INDEX_ID_FIELD);
    }
    headers.add(LEGAL_ENTITY_ID_SCHEME_FIELD);
    headers.add(LEGAL_ENTITY_ID_FIELD);
    if (premium) {
      headers.add(PREMIUM_DIRECTION_FIELD);
      headers.add(PREMIUM_CURRENCY_FIELD);
      headers.add(PREMIUM_AMOUNT_FIELD);
      headers.add(PREMIUM_DATE_FIELD);
      headers.add(PREMIUM_DATE_CNV_FIELD);
      headers.add(PREMIUM_DATE_CAL_FIELD);
    }
    headers.add(DAY_COUNT_FIELD);
    headers.add(PAYMENT_ON_DEFAULT_FIELD);
    headers.add(PROTECTION_START_FIELD);
    if (stepInOffset) {
      headers.add(STEP_IN_DATE_OFFSET_DAYS_FIELD);
      headers.add(STEP_IN_DATE_OFFSET_CAL_FIELD);
      headers.add(STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD);
      headers.add(STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD);
    }
    if (settleOffset) {
      headers.add(SETTLEMENT_DATE_OFFSET_DAYS_FIELD);
      headers.add(SETTLEMENT_DATE_OFFSET_CAL_FIELD);
      headers.add(SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD);
      headers.add(SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD);
    }
    // schedule
    headers.add(START_DATE_FIELD);
    if (startConv) {
      headers.add(START_DATE_CNV_FIELD);
      headers.add(START_DATE_CAL_FIELD);
    }
    headers.add(END_DATE_FIELD);
    if (endConv) {
      headers.add(END_DATE_CNV_FIELD);
      headers.add(END_DATE_CAL_FIELD);
    }
    headers.add(FREQUENCY_FIELD);
    headers.add(ROLL_CONVENTION_FIELD);
    headers.add(STUB_CONVENTION_FIELD);
    if (overrideStart) {
      headers.add(OVERRIDE_START_DATE_FIELD);
      headers.add(OVERRIDE_START_DATE_CNV_FIELD);
      headers.add(OVERRIDE_START_DATE_CAL_FIELD);
    }
    headers.add(FIRST_REGULAR_START_DATE_FIELD);
    headers.add(LAST_REGULAR_END_DATE_FIELD);
    headers.add(DATE_ADJ_CNV_FIELD);
    headers.add(DATE_ADJ_CAL_FIELD);
    return headers;
  }

  // writes the details to CSV
  static void writeCdsDetails(
      CsvRowOutputWithHeaders csv,
      BuySell buySell,
      Currency currency,
      double notional,
      double fixedRate,
      DayCount dayCount,
      PaymentOnDefault paymentOnDefault,
      ProtectionStartOfDay protectionStartOfDay,
      DaysAdjustment stepIn,
      DaysAdjustment settlement,
      PeriodicSchedule accrual) {

    csv.writeCell(BUY_SELL_FIELD, buySell);
    csv.writeCell(CURRENCY_FIELD, currency);
    csv.writeCell(NOTIONAL_FIELD, notional);
    csv.writeCell(FIXED_RATE_FIELD, formattedPercentage(fixedRate));
    csv.writeCell(DAY_COUNT_FIELD, dayCount);
    csv.writeCell(PAYMENT_ON_DEFAULT_FIELD, paymentOnDefault);
    csv.writeCell(PROTECTION_START_FIELD, protectionStartOfDay);
    // step in offset
    if (csv.headers().contains(STEP_IN_DATE_OFFSET_DAYS_FIELD)) {
      csv.writeCell(STEP_IN_DATE_OFFSET_DAYS_FIELD, stepIn.getDays());
      csv.writeCell(STEP_IN_DATE_OFFSET_CAL_FIELD, stepIn.getCalendar());
      csv.writeCell(STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD, stepIn.getAdjustment().getConvention());
      csv.writeCell(STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD, stepIn.getAdjustment().getCalendar());
    }
    // settlement offset
    if (csv.headers().contains(SETTLEMENT_DATE_OFFSET_DAYS_FIELD)) {
      csv.writeCell(SETTLEMENT_DATE_OFFSET_DAYS_FIELD, settlement.getDays());
      csv.writeCell(SETTLEMENT_DATE_OFFSET_CAL_FIELD, settlement.getCalendar());
      csv.writeCell(SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD, settlement.getAdjustment().getConvention());
      csv.writeCell(SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD, settlement.getAdjustment().getCalendar());
    }
    // accrual
    csv.writeCell(START_DATE_FIELD, accrual.getStartDate());
    accrual.getStartDateBusinessDayAdjustment().ifPresent(bda -> {
      csv.writeCell(START_DATE_CNV_FIELD, bda.getConvention());
      csv.writeCell(START_DATE_CAL_FIELD, bda.getCalendar());
    });
    csv.writeCell(END_DATE_FIELD, accrual.getEndDate());
    accrual.getEndDateBusinessDayAdjustment().ifPresent(bda -> {
      csv.writeCell(END_DATE_CNV_FIELD, bda.getConvention());
      csv.writeCell(END_DATE_CAL_FIELD, bda.getCalendar());
    });
    csv.writeCell(FREQUENCY_FIELD, accrual.getFrequency());
    accrual.getRollConvention().ifPresent(val -> csv.writeCell(ROLL_CONVENTION_FIELD, val));
    accrual.getStubConvention().ifPresent(val -> csv.writeCell(STUB_CONVENTION_FIELD, val));
    accrual.getOverrideStartDate().ifPresent(date -> {
      csv.writeCell(OVERRIDE_START_DATE_FIELD, date.getUnadjusted());
      csv.writeCell(OVERRIDE_START_DATE_CNV_FIELD, date.getAdjustment().getConvention());
      csv.writeCell(OVERRIDE_START_DATE_CAL_FIELD, date.getAdjustment().getCalendar());
    });
    accrual.getFirstRegularStartDate().ifPresent(val -> csv.writeCell(FIRST_REGULAR_START_DATE_FIELD, val));
    accrual.getLastRegularEndDate().ifPresent(val -> csv.writeCell(LAST_REGULAR_END_DATE_FIELD, val));
    csv.writeCell(DATE_ADJ_CNV_FIELD, accrual.getBusinessDayAdjustment().getConvention());
    csv.writeCell(DATE_ADJ_CAL_FIELD, accrual.getBusinessDayAdjustment().getCalendar());
    csv.writeNewLine();
  }
}
