/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIRST_REGULAR_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.INDEX_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LAST_REGULAR_END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.OVERRIDE_START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_FREQUENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ROLL_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.STUB_CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderUtils.formattedPercentage;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.capfloor.IborCapFloor;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.IborRateCalculation;

/**
 * Handles the CSV file format for CapFloor trades.
 */
public class IborCapFloorTradeCsvPlugin implements TradeCsvParserPlugin, TradeTypeCsvWriter<IborCapFloorTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final IborCapFloorTradeCsvPlugin INSTANCE = new IborCapFloorTradeCsvPlugin();

  private static final String CAP_FLOOR_FIELD = "CapFloor";

  private static final String STRIKE_FIELD = "Strike";

  private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
      .add(CAP_FLOOR_FIELD)
      .add(START_DATE_FIELD)
      .add(END_DATE_FIELD)
      .add(PAYMENT_FREQUENCY_FIELD)
      .add(DIRECTION_FIELD)
      .add(CURRENCY_FIELD)
      .add(STRIKE_FIELD)
      .add(NOTIONAL_FIELD)
      .add(INDEX_FIELD)
      .add(PREMIUM_AMOUNT_FIELD)
      .add(PREMIUM_CURRENCY_FIELD)
      .add(PREMIUM_DIRECTION_FIELD)
      .add(PREMIUM_DATE_FIELD)
      .add(DATE_ADJ_CNV_FIELD)
      .add(DATE_ADJ_CAL_FIELD)
      .add(START_DATE_CNV_FIELD)
      .add(START_DATE_CAL_FIELD)
      .add(END_DATE_CNV_FIELD)
      .add(END_DATE_CAL_FIELD)
      .add(STUB_CONVENTION_FIELD)
      .add(ROLL_CONVENTION_FIELD)
      .add(FIRST_REGULAR_START_DATE_FIELD)
      .add(LAST_REGULAR_END_DATE_FIELD)
      .add(OVERRIDE_START_DATE_FIELD)
      .add(OVERRIDE_START_DATE_CNV_FIELD)
      .add(OVERRIDE_START_DATE_CAL_FIELD)
      .build();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("CAPFLOOR");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(IborCapFloorTrade.class)) {
      return Optional.of(resolver.parseCapFloorTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "CapFloor";
  }

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   *
   * @param row the CSV row
   * @param info the trade info
   * @param resolver the resolver used to parse additional information
   * @return the parsed trade
   */
  static IborCapFloorTrade parseCapFloor(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    IborCapFloorTrade iborCapFloorTrade = parseRow(row, info);
    return resolver.completeTrade(row, iborCapFloorTrade);
  }

  //-------------------------------------------------------------------------
  // parses the row to a trade
  private static IborCapFloorTrade parseRow(CsvRow row, TradeInfo info) {
    IborCapFloor iborCapFloor = parseIborCapFloor(row);
    Optional<AdjustablePayment> paymentOpt = parsePremium(row);
    return paymentOpt.map(adjustablePayment ->
        IborCapFloorTrade.builder()
            .info(info)
            .product(iborCapFloor)
            .premium(adjustablePayment)
            .build())
        .orElseGet(() -> IborCapFloorTrade.builder()
            .info(info)
            .product(iborCapFloor)
            .build());
  }

  // parse an IborCapFloor
  private static IborCapFloor parseIborCapFloor(CsvRow row) {
    PayReceive payReceive = row.getValue(DIRECTION_FIELD, LoaderUtils::parsePayReceive);
    Currency currency = row.getValue(CURRENCY_FIELD, LoaderUtils::parseCurrency);
    ValueSchedule strike = ValueSchedule.of(row.getValue(STRIKE_FIELD, LoaderUtils::parseDoublePercent));
    double notional = row.getValue(NOTIONAL_FIELD, LoaderUtils::parseDouble);
    IborIndex iborIndex = parseIborIndex(row);
    PeriodicSchedule paymentSchedule = parseSchedule(row, currency);
    IborCapFloorLeg.Builder capFloorLegBuilder = IborCapFloorLeg.builder()
        .payReceive(payReceive)
        .paymentSchedule(paymentSchedule)
        .currency(currency)
        .notional(ValueSchedule.of(notional))
        .calculation(IborRateCalculation.of(iborIndex));
    String capFloorType = row.getValue(CAP_FLOOR_FIELD);
    if (capFloorType.toUpperCase(Locale.ENGLISH).equals("CAP")) {
      capFloorLegBuilder.capSchedule(strike);
    } else if (capFloorType.toUpperCase(Locale.ENGLISH).equals("FLOOR")) {
      capFloorLegBuilder.floorSchedule(strike);
    } else {
      throw new IllegalArgumentException(
          "Unknown CapFloor value, must be 'Cap' or 'Floor' but was '" + capFloorType + "'; " +
              "parser is case insensitive.");
    }
    IborCapFloorLeg iborCapFloorLeg = capFloorLegBuilder.build();
    return IborCapFloor.of(iborCapFloorLeg);
  }

  // parse the IBOR index
  private static IborIndex parseIborIndex(CsvRow row) {
    Index index = row.getValue(INDEX_FIELD, LoaderUtils::findIndex);
    if (index instanceof IborIndex) {
      return (IborIndex) index;
    } else {
      throw new IllegalArgumentException("Index " + index.getName() + "is not an IBOR index");
    }
  }

  // parse the periodic schedule for CapFloor
  private static PeriodicSchedule parseSchedule(CsvRow row, Currency currency) {
    PeriodicSchedule.Builder builder = PeriodicSchedule.builder();
    // basics
    builder.startDate(row.getValue(START_DATE_FIELD, LoaderUtils::parseDate));
    builder.endDate(row.getValue(END_DATE_FIELD, LoaderUtils::parseDate));
    builder.frequency(Frequency.parse(row.getValue(PAYMENT_FREQUENCY_FIELD)));
    // stub period is not allowed for CapFloor trade
    builder.stubConvention(StubConvention.NONE);
    // adjustments
    BusinessDayAdjustment dateAdj =
        CsvLoaderUtils.parseBusinessDayAdjustment(row, DATE_ADJ_CNV_FIELD, DATE_ADJ_CAL_FIELD)
            .orElse(BusinessDayAdjustment.NONE);
    builder.businessDayAdjustment(dateAdj);
    CsvLoaderUtils.parseBusinessDayAdjustment(row, START_DATE_CNV_FIELD, START_DATE_CAL_FIELD)
        .ifPresent(builder::startDateBusinessDayAdjustment);
    CsvLoaderUtils.parseBusinessDayAdjustment(row, END_DATE_CNV_FIELD, END_DATE_CAL_FIELD)
        .ifPresent(builder::endDateBusinessDayAdjustment);
    // optionals
    row.findValue(ROLL_CONVENTION_FIELD)
        .map(LoaderUtils::parseRollConvention)
        .ifPresent(builder::rollConvention);
    row.findValue(FIRST_REGULAR_START_DATE_FIELD)
        .map(LoaderUtils::parseDate)
        .ifPresent(builder::firstRegularStartDate);
    row.findValue(LAST_REGULAR_END_DATE_FIELD)
        .map(LoaderUtils::parseDate)
        .ifPresent(builder::lastRegularEndDate);
    Optional<AdjustableDate> overrideDateOpt = row.findValue(OVERRIDE_START_DATE_FIELD)
        .map(ignored -> CsvLoaderUtils.parseAdjustableDate(
            row,
            OVERRIDE_START_DATE_FIELD,
            OVERRIDE_START_DATE_CNV_FIELD,
            OVERRIDE_START_DATE_CAL_FIELD,
            BusinessDayConventions.MODIFIED_FOLLOWING,
            currency));
    overrideDateOpt.ifPresent(builder::overrideStartDate);
    return builder.build();
  }

  // parse the upfront premium amount
  private static Optional<AdjustablePayment> parsePremium(CsvRow row) {
    Optional<Double> premiumAmountOpt = row.findValue(PREMIUM_AMOUNT_FIELD)
        .map(LoaderUtils::parseDouble);
    if (premiumAmountOpt.isPresent()) {
      Currency premiumCurrency = row.getValue(PREMIUM_CURRENCY_FIELD, LoaderUtils::parseCurrency);
      PayReceive premiumDirection = row.getValue(PREMIUM_DIRECTION_FIELD, LoaderUtils::parsePayReceive);
      LocalDate premiumDate = row.getValue(PREMIUM_DATE_FIELD, LoaderUtils::parseDate);
      CurrencyAmount premiumAmount = CurrencyAmount.of(premiumCurrency, premiumAmountOpt.get());
      AdjustablePayment payment = premiumDirection.isPay() ? AdjustablePayment.ofPay(premiumAmount, premiumDate)
          : AdjustablePayment.ofReceive(premiumAmount, premiumDate);
      return Optional.of(payment);
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public List<String> headers(List<IborCapFloorTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, IborCapFloorTrade trade) {
    IborCapFloorLeg capFloorLeg = trade.getProduct().getCapFloorLeg();
    csv.writeCell(TRADE_TYPE_FIELD, "CapFloor");
    String capFloorType;
    Double strike;
    if (capFloorLeg.getCapSchedule().isPresent()) {
      capFloorType = "Cap";
      strike = capFloorLeg.getCapSchedule().get().getInitialValue();
    } else if (capFloorLeg.getFloorSchedule().isPresent()) {
      capFloorType = "Floor";
      strike = capFloorLeg.getFloorSchedule().get().getInitialValue();
    } else {
      throw new UnsupportedOperationException("Unknown CapFloor type, trade is missing cap or floor schedule");
    }
    csv.writeCell(CAP_FLOOR_FIELD, capFloorType);
    writePeriodicSchedule(csv, capFloorLeg.getPaymentSchedule());
    csv.writeCell(DIRECTION_FIELD, capFloorLeg.getPayReceive());
    csv.writeCell(CURRENCY_FIELD, capFloorLeg.getCurrency());
    csv.writeCell(STRIKE_FIELD, formattedPercentage(strike));
    csv.writeCell(NOTIONAL_FIELD, capFloorLeg.getNotional().getInitialValue());
    csv.writeCell(INDEX_FIELD, capFloorLeg.getCalculation().getIndex());
    trade.getPremium().ifPresent(premium -> {
      csv.writeCell(PREMIUM_AMOUNT_FIELD, premium.getAmount());
      csv.writeCell(PREMIUM_CURRENCY_FIELD, premium.getCurrency());
      csv.writeCell(PREMIUM_DIRECTION_FIELD, premium.getValue().isNegative() ? PayReceive.PAY : PayReceive.RECEIVE);
      csv.writeCell(PREMIUM_DATE_FIELD, premium.getDate().getUnadjusted());
    });
    csv.writeNewLine();
  }

  private void writePeriodicSchedule(CsvRowOutputWithHeaders csv, PeriodicSchedule paymentSchedule) {
    csv.writeCell(START_DATE_FIELD, paymentSchedule.getStartDate());
    csv.writeCell(END_DATE_FIELD, paymentSchedule.getEndDate());
    csv.writeCell(PAYMENT_FREQUENCY_FIELD, paymentSchedule.getFrequency());
    csv.writeCell(DATE_ADJ_CNV_FIELD, paymentSchedule.getBusinessDayAdjustment().getConvention());
    csv.writeCell(DATE_ADJ_CAL_FIELD, paymentSchedule.getBusinessDayAdjustment().getCalendar());
    paymentSchedule.getStartDateBusinessDayAdjustment().ifPresent(startDateAdjustment -> {
      csv.writeCell(START_DATE_CNV_FIELD, startDateAdjustment.getConvention());
      csv.writeCell(START_DATE_CAL_FIELD, startDateAdjustment.getCalendar());
    });
    paymentSchedule.getEndDateBusinessDayAdjustment().ifPresent(endDateAdjustment -> {
      csv.writeCell(END_DATE_CNV_FIELD, endDateAdjustment.getConvention());
      csv.writeCell(END_DATE_CAL_FIELD, endDateAdjustment.getCalendar());
    });
    paymentSchedule.getStubConvention().ifPresent(
        stubConvention -> csv.writeCell(STUB_CONVENTION_FIELD, stubConvention));
    paymentSchedule.getRollConvention().ifPresent(
        rollConvention -> csv.writeCell(ROLL_CONVENTION_FIELD, rollConvention));
    paymentSchedule.getFirstRegularStartDate().ifPresent(
        firstRegDate -> csv.writeCell(FIRST_REGULAR_START_DATE_FIELD, firstRegDate));
    paymentSchedule.getLastRegularEndDate().ifPresent(
        lastRegDate -> csv.writeCell(LAST_REGULAR_END_DATE_FIELD, lastRegDate));
    paymentSchedule.getOverrideStartDate().ifPresent(overrideStartDate -> {
      csv.writeCell(OVERRIDE_START_DATE_FIELD, overrideStartDate.getUnadjusted());
      csv.writeCell(OVERRIDE_START_DATE_CNV_FIELD, overrideStartDate.getAdjustment().getConvention());
      csv.writeCell(OVERRIDE_START_DATE_CAL_FIELD, overrideStartDate.getAdjustment().getCalendar());
    });
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private IborCapFloorTradeCsvPlugin() {
  }

}
