/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedPercentage;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DAY_COUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIRST_REGULAR_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FREQUENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.LAST_REGULAR_END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.OVERRIDE_START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.OVERRIDE_START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.OVERRIDE_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PREMIUM_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.ROLL_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.STUB_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.type.CdsConvention;

/**
 * Handles the CSV file format for CDS trades.
 */
final class CdsTradeCsvPlugin implements TradeTypeCsvWriter<CdsTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final CdsTradeCsvPlugin INSTANCE = new CdsTradeCsvPlugin();

  private static final String DEFAULT_LEGAL_ENTITY_SCHEME = "OG-Entity";
  private static final String LEGAL_ENTITY_ID_SCHEME_FIELD = "Legal Entity Id Scheme";
  private static final String LEGAL_ENTITY_ID_FIELD = "Legal Entity Id";
  private static final String PAYMENT_ON_DEFAULT_FIELD = "Payment On Default";
  private static final String PROTECTION_START_FIELD = "Protection Start";
  private static final String STEP_IN_DATE_OFFSET_DAYS_FIELD = "Step In Date Offset Days";
  private static final String STEP_IN_DATE_OFFSET_CAL_FIELD = "Step In Date Offset Calendar";
  private static final String STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD = "Step In Date Offset Adjustment Convention";
  private static final String STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD = "Step In Date Offset Adjustment Calendar";
  private static final String SETTLEMENT_DATE_OFFSET_DAYS_FIELD = "Settlement Date Offset Days";
  private static final String SETTLEMENT_DATE_OFFSET_CAL_FIELD = "Settlement Date Offset Calendar";
  private static final String SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD = "Settlement Date Offset Adjustment Convention";
  private static final String SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD = "Settlement Date Offset Adjustment Calendar";

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
  static CdsTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    CdsTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  // parse the row to a trade
  private static CdsTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    BuySell buySell = LoaderUtils.parseBuySell(row.getValue(BUY_SELL_FIELD));
    double notional = LoaderUtils.parseDouble(row.getValue(NOTIONAL_FIELD));
    double fixedRate = LoaderUtils.parseDoublePercent(row.getValue(FIXED_RATE_FIELD));
    String entityScheme = row.findValue(LEGAL_ENTITY_ID_SCHEME_FIELD).orElse(DEFAULT_LEGAL_ENTITY_SCHEME);
    StandardId entityId = StandardId.of(entityScheme, row.getValue(LEGAL_ENTITY_ID_FIELD));
    Optional<Tenor> tenorOpt = row.findValue(TENOR_FIELD).map(s -> LoaderUtils.parseTenor(s));
    Optional<LocalDate> startDateOpt = row.findValue(START_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
    Optional<AdjustablePayment> premiumOpt = row.findValue(PREMIUM_DIRECTION_FIELD)
        .map(ignored -> {
          CurrencyAmount amount = CsvLoaderUtils.parseCurrencyAmountWithDirection(
              row, PREMIUM_CURRENCY_FIELD, PREMIUM_AMOUNT_FIELD, PREMIUM_DIRECTION_FIELD);
          AdjustableDate date = CsvLoaderUtils.parseAdjustableDate(
              row, PREMIUM_DATE_FIELD, PREMIUM_DATE_CNV_FIELD, PREMIUM_DATE_CAL_FIELD);
          return AdjustablePayment.of(amount, date);
        });

    // parse by convention
    Optional<CdsConvention> conventionOpt = row.findValue(CONVENTION_FIELD).map(s -> CdsConvention.of(s));
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
    Currency currency = LoaderUtils.parseCurrency(row.getValue(CURRENCY_FIELD));
    Cds.Builder cdsBuilder = Cds.builder()
        .buySell(buySell)
        .legalEntityId(entityId)
        .currency(currency)
        .notional(notional)
        .fixedRate(fixedRate)
        .paymentSchedule(parseSchedule(row, currency));

    row.findValue(DAY_COUNT_FIELD)
        .map(s -> LoaderUtils.parseDayCount(s))
        .ifPresent(dayCount -> cdsBuilder.dayCount(dayCount));
    row.findValue(PAYMENT_ON_DEFAULT_FIELD)
        .map(s -> PaymentOnDefault.of(s))
        .ifPresent(pay -> cdsBuilder.paymentOnDefault(pay));
    row.findValue(PROTECTION_START_FIELD)
        .map(s -> ProtectionStartOfDay.of(s))
        .ifPresent(protect -> cdsBuilder.protectionStart(protect));
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
  // accrual schedule
  private static PeriodicSchedule parseSchedule(CsvRow row, Currency currency) {
    PeriodicSchedule.Builder builder = PeriodicSchedule.builder();
    // basics
    builder.startDate(LoaderUtils.parseDate(row.getValue(START_DATE_FIELD)));
    builder.endDate(LoaderUtils.parseDate(row.getValue(END_DATE_FIELD)));
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
    builder.stubConvention(row.findValue(STUB_CONVENTION_FIELD)
        .map(s -> StubConvention.of(s))
        .orElse(StubConvention.SMART_INITIAL));
    row.findValue(ROLL_CONVENTION_FIELD)
        .map(s -> LoaderUtils.parseRollConvention(s))
        .ifPresent(v -> builder.rollConvention(v));
    row.findValue(FIRST_REGULAR_START_DATE_FIELD)
        .map(s -> LoaderUtils.parseDate(s))
        .ifPresent(v -> builder.firstRegularStartDate(v));
    row.findValue(LAST_REGULAR_END_DATE_FIELD)
        .map(s -> LoaderUtils.parseDate(s))
        .ifPresent(v -> builder.lastRegularEndDate(v));
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
  public List<String> headers(List<CdsTrade> trades) {
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
    // select the headers
    List<String> headers = new ArrayList<>();
    headers.add(BUY_SELL_FIELD);
    headers.add(CURRENCY_FIELD);
    headers.add(NOTIONAL_FIELD);
    headers.add(FIXED_RATE_FIELD);
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

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, CdsTrade trade) {
    Cds product = trade.getProduct();
    csv.writeCell(TradeCsvLoader.TYPE_FIELD, "CDS");
    csv.writeCell(BUY_SELL_FIELD, product.getBuySell());
    csv.writeCell(CURRENCY_FIELD, product.getCurrency());
    csv.writeCell(NOTIONAL_FIELD, product.getNotional());
    csv.writeCell(FIXED_RATE_FIELD, formattedPercentage(product.getFixedRate()));
    csv.writeCell(LEGAL_ENTITY_ID_SCHEME_FIELD, product.getLegalEntityId().getScheme());
    csv.writeCell(LEGAL_ENTITY_ID_FIELD, product.getLegalEntityId().getValue());
    trade.getUpfrontFee().ifPresent(premium -> {
      csv.writeCell(PREMIUM_DIRECTION_FIELD, PayReceive.ofSignedAmount(premium.getAmount()));
      csv.writeCell(PREMIUM_CURRENCY_FIELD, premium.getCurrency());
      csv.writeCell(PREMIUM_AMOUNT_FIELD, premium.getAmount());
      csv.writeCell(PREMIUM_DATE_FIELD, premium.getDate().getUnadjusted());
      csv.writeCell(PREMIUM_DATE_CNV_FIELD, premium.getDate().getAdjustment().getConvention());
      csv.writeCell(PREMIUM_DATE_CAL_FIELD, premium.getDate().getAdjustment().getCalendar());
    });
    csv.writeCell(DAY_COUNT_FIELD, product.getDayCount());
    csv.writeCell(PAYMENT_ON_DEFAULT_FIELD, product.getPaymentOnDefault());
    csv.writeCell(PROTECTION_START_FIELD, product.getProtectionStart());
    if (csv.headers().contains(STEP_IN_DATE_OFFSET_DAYS_FIELD)) {
      DaysAdjustment offset = product.getStepinDateOffset();
      csv.writeCell(STEP_IN_DATE_OFFSET_DAYS_FIELD, offset.getDays());
      csv.writeCell(STEP_IN_DATE_OFFSET_CAL_FIELD, offset.getCalendar());
      csv.writeCell(STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD, offset.getAdjustment().getConvention());
      csv.writeCell(STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD, offset.getAdjustment().getCalendar());
    }
    if (csv.headers().contains(SETTLEMENT_DATE_OFFSET_DAYS_FIELD)) {
      DaysAdjustment offset = product.getSettlementDateOffset();
      csv.writeCell(SETTLEMENT_DATE_OFFSET_DAYS_FIELD, offset.getDays());
      csv.writeCell(SETTLEMENT_DATE_OFFSET_CAL_FIELD, offset.getCalendar());
      csv.writeCell(SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD, offset.getAdjustment().getConvention());
      csv.writeCell(SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD, offset.getAdjustment().getCalendar());
    }
    // schedule
    PeriodicSchedule accrual = product.getPaymentSchedule();
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

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private CdsTradeCsvPlugin() {
  }

}
