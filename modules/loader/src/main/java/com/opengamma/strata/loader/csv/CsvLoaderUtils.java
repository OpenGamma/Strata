/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * CSV information resolver helper.
 * <p>
 * This simplifies implementations of {@link TradeCsvInfoResolver} and {@link PositionCsvInfoResolver}.
 */
public final class CsvLoaderUtils {

  /**
   * The column name for the security ID scheme/symbology.
   */
  public static final String SECURITY_ID_SCHEME_FIELD = "Security Id Scheme";
  /**
   * The column name for the security ID.
   */
  public static final String SECURITY_ID_FIELD = "Security Id";
  /**
   * The column name for the exchange.
   */
  public static final String EXCHANGE_FIELD = "Exchange";
  /**
   * The column name for the contract code.
   */
  public static final String CONTRACT_CODE_FIELD = "Contract Code";
  /**
   * The column name for the long quantity.
   */
  public static final String LONG_QUANTITY_FIELD = "Long Quantity";
  /**
   * The column name for the short quantity.
   */
  public static final String SHORT_QUANTITY_FIELD = "Short Quantity";
  /**
   * The column name for the quantity.
   */
  public static final String QUANTITY_FIELD = "Quantity";
  /**
   * The column name for the price.
   */
  public static final String PRICE_FIELD = "Price";
  /**
   * The column name for the expiry month/year.
   */
  public static final String EXPIRY_FIELD = "Expiry";
  /**
   * The column name for the expiry week.
   */
  public static final String EXPIRY_WEEK_FIELD = "Expiry Week";
  /**
   * The column name for the expiry day.
   */
  public static final String EXPIRY_DAY_FIELD = "Expiry Day";
  /**
   * The column name for the settlement type.
   */
  public static final String SETTLEMENT_TYPE_FIELD = "Settlement Type";
  /**
   * The column name for the exercise style.
   */
  public static final String EXERCISE_STYLE_FIELD = "Exercise Style";
  /**
   * The column name for the option version.
   */
  public static final String VERSION_FIELD = "Version";
  /**
   * The column name for the put/call flag.
   */
  public static final String PUT_CALL_FIELD = "Put Call";
  /**
   * The column name for the option strike price.
   */
  public static final String EXERCISE_PRICE_FIELD = "Exercise Price";
  /**
   * The column name for the underlying expiry month/year.
   */
  public static final String UNDERLYING_EXPIRY_FIELD = "Underlying Expiry";
  /**
   * The column name for the currency.
   */
  public static final String CURRENCY = "Currency";
  /**
   * The column name for the tick size.
   */
  public static final String TICK_SIZE = "Tick Size";
  /**
   * The column name for the tick value.
   */
  public static final String TICK_VALUE = "Tick Value";
  /**
   * The column name for the contract size.
   */
  public static final String CONTRACT_SIZE = "Contract Size";

  /**
   * Default version used as an option might not specify a version number.
   */
  public static final int DEFAULT_OPTION_VERSION_NUMBER = 0;
  /**
   * Lookup settlement by code.
   */
  public static final ImmutableMap<String, EtdSettlementType> SETTLEMENT_BY_CODE =
      Stream.of(EtdSettlementType.values()).collect(toImmutableMap(EtdSettlementType::getCode));

  // Restricted constructor.
  private CsvLoaderUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the year-month and variant.
   * 
   * @param row  the CSV row to parse
   * @param type  the ETD type
   * @return the expiry year-month and variant
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  public static Pair<YearMonth, EtdVariant> parseEtdVariant(CsvRow row, EtdType type) {
    YearMonth yearMonth = row.getValue(EXPIRY_FIELD, LoaderUtils::parseYearMonth);
    int week = row.findValue(EXPIRY_WEEK_FIELD).map(s -> LoaderUtils.parseInteger(s)).orElse(0);
    int day = row.findValue(EXPIRY_DAY_FIELD).map(s -> LoaderUtils.parseInteger(s)).orElse(0);
    Optional<EtdSettlementType> settleType = row.findValue(SETTLEMENT_TYPE_FIELD).map(s -> parseEtdSettlementType(s));
    Optional<EtdOptionType> optionType = row.findValue(EXERCISE_STYLE_FIELD).map(s -> parseEtdOptionType(s));
    // check valid combinations
    if (!settleType.isPresent()) {
      if (day == 0) {
        if (week == 0) {
          return Pair.of(yearMonth, EtdVariant.ofMonthly());
        } else {
          return Pair.of(yearMonth, EtdVariant.ofWeekly(week));
        }
      } else {
        if (week == 0) {
          return Pair.of(yearMonth, EtdVariant.ofDaily(day));
        } else {
          throw new IllegalArgumentException("ETD date columns conflict, cannot set both expiry day and expiry week");
        }
      }
    } else {
      if (day == 0) {
        throw new IllegalArgumentException("ETD date columns conflict, must set expiry day for Flex " + type);
      }
      if (week != 0) {
        throw new IllegalArgumentException("ETD date columns conflict, cannot set expiry week for Flex " + type);
      }
      if (type == EtdType.FUTURE) {
        return Pair.of(yearMonth, EtdVariant.ofFlexFuture(day, settleType.get()));
      } else {
        if (!optionType.isPresent()) {
          throw new IllegalArgumentException("ETD option type not found for Flex Option");
        }
        return Pair.of(yearMonth, EtdVariant.ofFlexOption(day, settleType.get(), optionType.get()));
      }
    }
  }

  /**
   * Parses the ETD settlement type from the short code or full name.
   * 
   * @param str  the string to parse
   * @return the settlement type
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static EtdSettlementType parseEtdSettlementType(String str) {
    String upper = str.toUpperCase(Locale.ENGLISH);
    EtdSettlementType fromCode = SETTLEMENT_BY_CODE.get(upper);
    return fromCode != null ? fromCode : EtdSettlementType.of(str);
  }

  /**
   * Parses the ETD option type from the short code or full name.
   * 
   * @param str  the string to parse
   * @return the option type
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static EtdOptionType parseEtdOptionType(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "AMERICAN":
      case "A":
        return EtdOptionType.AMERICAN;
      case "EUROPEAN":
      case "E":
        return EtdOptionType.EUROPEAN;
      default:
        throw new IllegalArgumentException(
            "Unknown EtdOptionType value, must be 'American' or 'European' but was '" + str + "'; " +
                "parser is case insensitive and also accepts 'A' and 'E'");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the quantity.
   * 
   * @param row  the CSV row to parse
   * @return the quantity, long first, short second
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  public static DoublesPair parseQuantity(CsvRow row) {
    Optional<Double> quantityOpt = row.findValue(QUANTITY_FIELD).map(s -> LoaderUtils.parseDouble(s));
    if (quantityOpt.isPresent()) {
      double quantity = quantityOpt.get();
      return DoublesPair.of(quantity >= 0 ? quantity : 0, quantity >= 0 ? 0 : -quantity);
    }
    Optional<Double> longQuantityOpt = row.findValue(LONG_QUANTITY_FIELD).map(s -> LoaderUtils.parseDouble(s));
    Optional<Double> shortQuantityOpt = row.findValue(SHORT_QUANTITY_FIELD).map(s -> LoaderUtils.parseDouble(s));
    if (!longQuantityOpt.isPresent() && !shortQuantityOpt.isPresent()) {
      throw new IllegalArgumentException(
          Messages.format("Security must contain a quantity column, either '{}' or '{}' and '{}'",
              QUANTITY_FIELD, LONG_QUANTITY_FIELD, SHORT_QUANTITY_FIELD));
    }
    double longQuantity = ArgChecker.notNegative(longQuantityOpt.orElse(0d), LONG_QUANTITY_FIELD);
    double shortQuantity = ArgChecker.notNegative(shortQuantityOpt.orElse(0d), SHORT_QUANTITY_FIELD);
    return DoublesPair.of(longQuantity, shortQuantity);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a business day adjustment, without defaulting the adjustment.
   * 
   * @param row  the CSV row to parse
   * @param dateField  the date field
   * @param conventionField  the convention field
   * @param calendarField  the calendar field
   * @return the adjustment
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  static AdjustableDate parseAdjustableDate(
      CsvRow row,
      String dateField,
      String conventionField,
      String calendarField) {

    LocalDate date = row.getValue(dateField, LoaderUtils::parseDate);
    return parseBusinessDayAdjustment(row, conventionField, calendarField)
        .map(adj -> AdjustableDate.of(date, adj))
        .orElse(AdjustableDate.of(date));
  }

  /**
   * Parses a business day adjustment, defaulting the adjustment using the currency.
   * 
   * @param row  the CSV row to parse
   * @param dateField  the date field
   * @param conventionField  the convention field
   * @param calendarField  the calendar field
   * @param defaultConvention  the default convention
   * @param currency  the applicable currency, used for defaulting
   * @return the adjustment
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  static AdjustableDate parseAdjustableDate(
      CsvRow row,
      String dateField,
      String conventionField,
      String calendarField,
      BusinessDayConvention defaultConvention,
      Currency currency) {

    LocalDate date = row.getValue(dateField, LoaderUtils::parseDate);
    BusinessDayAdjustment adj = parseBusinessDayAdjustment(row, conventionField, calendarField)
        .orElseGet(() -> BusinessDayAdjustment.of(defaultConvention, HolidayCalendarId.defaultByCurrency(currency)));
    return AdjustableDate.of(date, adj);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a business day adjustment.
   * 
   * @param row  the CSV row to parse
   * @param conventionField  the convention field
   * @param calendarField  the calendar field
   * @return the adjustment
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  static Optional<BusinessDayAdjustment> parseBusinessDayAdjustment(
      CsvRow row,
      String conventionField,
      String calendarField) {

    Optional<String> cnvOpt = row.findValue(conventionField);
    if (cnvOpt.isPresent()) {
      BusinessDayConvention cnv = LoaderUtils.parseBusinessDayConvention(cnvOpt.get());
      if (cnv.equals(BusinessDayConventions.NO_ADJUST)) {
        return Optional.of(BusinessDayAdjustment.NONE);
      }
      Optional<String> calOpt = row.findValue(calendarField);
      if (calOpt.isPresent()) {
        HolidayCalendarId cal = HolidayCalendarId.of(calOpt.get());
        return Optional.of(BusinessDayAdjustment.of(cnv, cal));
      }
    }
    return Optional.empty();
  }

  /**
   * Parses days adjustment from CSV.
   * 
   * @param row  the CSV row to parse
   * @param daysField  the days field
   * @param daysCalField  the days calendar field
   * @param cnvField  the convention field
   * @param calField  the calendar field
   * @return the adjustment
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  static DaysAdjustment parseDaysAdjustment(
      CsvRow row,
      String daysField,
      String daysCalField,
      String cnvField,
      String calField) {

    int days = row.getValue(daysField, LoaderUtils::parseInteger);
    HolidayCalendarId daysCal = row.findValue(daysCalField)
        .map(s -> HolidayCalendarId.of(s))
        .orElse(HolidayCalendarIds.NO_HOLIDAYS);
    BusinessDayAdjustment bda = parseBusinessDayAdjustment(row, cnvField, calField)
        .orElse(BusinessDayAdjustment.NONE);
    return DaysAdjustment.builder()
        .days(days)
        .calendar(daysCal)
        .adjustment(bda)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a currency amount.
   * 
   * @param row  the CSV row to parse
   * @param currencyField  the currency field
   * @param amountField  the amount field
   * @return the currency amount
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  static CurrencyAmount parseCurrencyAmount(CsvRow row, String currencyField, String amountField) {
    Currency currency = row.getValue(currencyField, LoaderUtils::parseCurrency);
    double amount = row.getValue(amountField, LoaderUtils::parseDouble);
    return CurrencyAmount.of(currency, amount);
  }

  /**
   * Parses a currency amount with direction.
   * 
   * @param row  the CSV row to parse
   * @param currencyField  the currency field
   * @param amountField  the amount field
   * @param directionField  the direction field
   * @return the currency amount
   * @throws IllegalArgumentException if the row cannot be parsed
   */
  static CurrencyAmount parseCurrencyAmountWithDirection(
      CsvRow row,
      String currencyField,
      String amountField,
      String directionField) {

    Currency currency = row.getValue(currencyField, LoaderUtils::parseCurrency);
    double amount = row.getValue(amountField, LoaderUtils::parseDouble);
    PayReceive direction = row.getValue(directionField, LoaderUtils::parsePayReceive);
    return CurrencyAmount.of(currency, direction.normalize(amount));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a value formatted as a percentage.
   * <p>
   * Using this method avoids nasty effects from floating point arithmetic.
   * 
   * @param value  the value in decimal format (to be multiplied by 100)
   * @return the formatted percentage value
   */
  public static String formattedPercentage(double value) {
    String str = BigDecimal.valueOf(value).movePointRight(2).toPlainString();
    return str.endsWith(".0") ? str.substring(0, str.length() - 2) : str;
  }

  /**
   * Returns a value formatted as a double.
   * <p>
   * Using this method avoids nasty effects from floating point arithmetic.
   * 
   * @param value  the value
   * @return the formatted value
   */
  public static String formattedDouble(double value) {
    String str = BigDecimal.valueOf(value).toPlainString();
    return str.endsWith(".0") ? str.substring(0, str.length() - 2) : str;
  }

}
