/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_LEVEL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.KNOCK_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.AdjustablePayment;
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
import com.opengamma.strata.collect.Decimal;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.result.ParseFailureException;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;
import com.opengamma.strata.product.option.Barrier;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * CSV information resolver helper.
 * <p>
 * This simplifies implementations of {@link TradeCsvInfoResolver} and {@link PositionCsvInfoResolver}.
 */
public final class CsvLoaderUtils {

  /**
   * The column name for the security ID scheme/symbology.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String SECURITY_ID_SCHEME_FIELD = CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
  /**
   * The column name for the security ID.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String SECURITY_ID_FIELD = CsvLoaderColumns.SECURITY_ID_FIELD;
  /**
   * The column name for the exchange.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String EXCHANGE_FIELD = CsvLoaderColumns.EXCHANGE_FIELD;
  /**
   * The column name for the contract code.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String CONTRACT_CODE_FIELD = CsvLoaderColumns.CONTRACT_CODE_FIELD;
  /**
   * The column name for the long quantity.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String LONG_QUANTITY_FIELD = CsvLoaderColumns.LONG_QUANTITY_FIELD;
  /**
   * The column name for the short quantity.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String SHORT_QUANTITY_FIELD = CsvLoaderColumns.SHORT_QUANTITY_FIELD;
  /**
   * The column name for the quantity.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String QUANTITY_FIELD = CsvLoaderColumns.QUANTITY_FIELD;
  /**
   * The column name for the price.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String PRICE_FIELD = CsvLoaderColumns.PRICE_FIELD;
  /**
   * The column name for the expiry month/year.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String EXPIRY_FIELD = CsvLoaderColumns.EXPIRY_FIELD;
  /**
   * The column name for the expiry week.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String EXPIRY_WEEK_FIELD = CsvLoaderColumns.EXPIRY_WEEK_FIELD;
  /**
   * The column name for the expiry day.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String EXPIRY_DAY_FIELD = CsvLoaderColumns.EXPIRY_DAY_FIELD;
  /**
   * The column name for the settlement type.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String SETTLEMENT_TYPE_FIELD = CsvLoaderColumns.SETTLEMENT_TYPE_FIELD;
  /**
   * The column name for the exercise style.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String EXERCISE_STYLE_FIELD = CsvLoaderColumns.EXERCISE_STYLE_FIELD;
  /**
   * The column name for the option version.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String VERSION_FIELD = CsvLoaderColumns.VERSION_FIELD;
  /**
   * The column name for the put/call flag.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String PUT_CALL_FIELD = CsvLoaderColumns.PUT_CALL_FIELD;
  /**
   * The column name for the option strike price.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String EXERCISE_PRICE_FIELD = CsvLoaderColumns.EXERCISE_PRICE_FIELD;
  /**
   * The column name for the underlying expiry month/year.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String UNDERLYING_EXPIRY_FIELD = CsvLoaderColumns.UNDERLYING_EXPIRY_FIELD;
  /**
   * The column name for the currency.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String CURRENCY = CsvLoaderColumns.CURRENCY_FIELD;
  /**
   * The column name for the tick size.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String TICK_SIZE = CsvLoaderColumns.TICK_SIZE_FIELD;
  /**
   * The column name for the tick value.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String TICK_VALUE = CsvLoaderColumns.TICK_VALUE_FIELD;
  /**
   * The column name for the contract size.
   * @deprecated Use {@link CsvLoaderColumns}.
   */
  @Deprecated
  public static final String CONTRACT_SIZE = CsvLoaderColumns.CONTRACT_SIZE_FIELD;

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
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static Pair<YearMonth, EtdVariant> parseEtdVariant(CsvRow row, EtdType type) {
    YearMonth yearMonth = row.getValue(EXPIRY_FIELD, LoaderUtils::parseYearMonth);
    int week = row.findValue(EXPIRY_WEEK_FIELD, LoaderUtils::parseInteger).orElse(0);
    int day = row.findValue(EXPIRY_DAY_FIELD, LoaderUtils::parseInteger).orElse(0);
    Optional<EtdSettlementType> settleTypeOpt = row.findValue(SETTLEMENT_TYPE_FIELD, CsvLoaderUtils::parseEtdSettlementType);
    Optional<EtdOptionType> optionTypeOpt = row.findValue(EXERCISE_STYLE_FIELD, CsvLoaderUtils::parseEtdOptionType);
    // check valid combinations
    if (!settleTypeOpt.isPresent()) {
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
          throw new ParseFailureException("Unable to parse ETD variant, date columns conflict, must not set both " +
              "expiry day and expiry week");
        }
      }
    } else {
      if (day == 0) {
        throw new ParseFailureException(
            "'{field}' is empty, must be set when '{}' is provided as this denotes a Flex Option",
            EXPIRY_DAY_FIELD,
            SETTLEMENT_TYPE_FIELD);
      }
      if (week != 0) {
        throw new ParseFailureException(
            "'{field}' is empty, must be set when '{}' is provided as this denotes a Flex Option",
            EXPIRY_WEEK_FIELD,
            SETTLEMENT_TYPE_FIELD);
      }
      if (type == EtdType.FUTURE) {
        return Pair.of(yearMonth, EtdVariant.ofFlexFuture(day, settleTypeOpt.get()));
      } else {
        if (!optionTypeOpt.isPresent()) {
          throw new ParseFailureException(
              "'{field}' is empty, must be set when '{}' is provided as this denotes a Flex Option",
              EXERCISE_STYLE_FIELD,
              SETTLEMENT_TYPE_FIELD);
        }
        return Pair.of(yearMonth, EtdVariant.ofFlexOption(day, settleTypeOpt.get(), optionTypeOpt.get()));
      }
    }
  }

  /**
   * Parses the ETD settlement type from the short code or full name.
   *
   * @param str  the string to parse
   * @return the settlement type
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static EtdSettlementType parseEtdSettlementType(String str) {
    try {
      String upper = str.toUpperCase(Locale.ENGLISH);
      EtdSettlementType fromCode = SETTLEMENT_BY_CODE.get(upper);
      return fromCode != null ? fromCode : EtdSettlementType.of(str);
    } catch (IllegalArgumentException ex) {
      throw new ParseFailureException("Unable to parse ETD settlement type from '{value}'", str);
    }
  }

  /**
   * Parses the ETD option type from the short code or full name.
   *
   * @param str  the string to parse
   * @return the option type
   * @throws ParseFailureException if the string cannot be parsed
   */
  public static EtdOptionType parseEtdOptionType(String str) {
    switch (str.toUpperCase(Locale.ENGLISH)) {
      case "AMERICAN":
      case "A":
        return EtdOptionType.AMERICAN;
      case "EUROPEAN":
      case "E":
        return EtdOptionType.EUROPEAN;
      case "ASIAN":
      case "T":
        return EtdOptionType.ASIAN;
      default:
        throw new ParseFailureException(
            "Unable to parse ETD option type from '{value}', must be 'American', 'European', 'Asian', 'A', 'E' or " +
                "'T' (case insensitive)", str);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the quantity.
   *
   * @param row  the CSV row to parse
   * @return the quantity, long first, short second
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static DoublesPair parseQuantity(CsvRow row) {
    Optional<Double> quantityOpt = row.findValue(QUANTITY_FIELD, LoaderUtils::parseDouble);
    if (quantityOpt.isPresent()) {
      double quantity = quantityOpt.get();
      return DoublesPair.of(quantity >= 0 ? quantity : 0, quantity >= 0 ? 0 : -quantity);
    }
    Optional<Double> longQuantityOpt = row.findValue(LONG_QUANTITY_FIELD, LoaderUtils::parseDouble);
    Optional<Double> shortQuantityOpt = row.findValue(SHORT_QUANTITY_FIELD, LoaderUtils::parseDouble);
    if (!longQuantityOpt.isPresent() && !shortQuantityOpt.isPresent()) {
      throw new ParseFailureException(
          "Security must contain a quantity column, either '{}' or '{}' and '{}'",
          QUANTITY_FIELD, LONG_QUANTITY_FIELD, SHORT_QUANTITY_FIELD);
    }
    double longQuantity = ArgChecker.notNegative(longQuantityOpt.orElse(0d), LONG_QUANTITY_FIELD);
    double shortQuantity = ArgChecker.notNegative(shortQuantityOpt.orElse(0d), SHORT_QUANTITY_FIELD);
    return DoublesPair.of(longQuantity, shortQuantity);
  }

  /**
   * Parse a ZonedDateTime from the provided fields.
   *
   * @param row  the CSV row
   * @param dateField  the date field
   * @param timeField  the time field
   * @param zoneField  the zone field
   * @return the zoned date time
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static ZonedDateTime parseZonedDateTime(CsvRow row, String dateField, String timeField, String zoneField) {
    LocalDate date = row.getValue(dateField, LoaderUtils::parseDate);
    LocalTime time = row.getValue(timeField, LoaderUtils::parseTime);
    ZoneId zone = row.getValue(zoneField, LoaderUtils::parseZoneId);
    return ZonedDateTime.of(date, time, zone);
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
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static AdjustableDate parseAdjustableDate(
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
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static AdjustableDate parseAdjustableDate(
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

  /**
   * Parses an adjustable payment.
   *
   * @param row  the CSV row to parse
   * @param currencyField  the currency field
   * @param amountField  the amount field
   * @param directionField  the direction field
   * @param dateField  the date field
   * @param conventionField  the convention field
   * @param calendarField  the calendar field
   * @return the adjustable payment
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static AdjustablePayment parseAdjustablePayment(
      CsvRow row,
      String currencyField,
      String amountField,
      String directionField,
      String dateField,
      String conventionField,
      String calendarField) {

    CurrencyAmount ccyAmount = parseCurrencyAmountWithDirection(row, currencyField, amountField, directionField);
    AdjustableDate adjustableDate = parseAdjustableDate(row, dateField, calendarField, conventionField);
    return AdjustablePayment.of(ccyAmount, adjustableDate);
  }

  /**
   * Parses a barrier from the csv row.
   *
   * @param row the CSV row to parse
   * @param barrierTypeField the barrier type field
   * @param knockTypeField the knock type field
   * @param barrierLevelField the barrier level field
   * @return the barrier
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static Barrier parseBarrier(
      CsvRow row,
      String barrierTypeField,
      String knockTypeField,
      String barrierLevelField) {

    BarrierType barrierType = row.getValue(barrierTypeField, LoaderUtils::parseBarrierType);
    KnockType knockType = row.getValue(knockTypeField, LoaderUtils::parseKnockType);
    double barrierLevel = row.getValue(barrierLevelField, LoaderUtils::parseDouble);

    return SimpleConstantContinuousBarrier.of(barrierType, knockType, barrierLevel);
  }

  /**
   * Parses a barrier using the default barrier fields.
   *
   * @param row the CSV row to parse
   * @return the barrier
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static Barrier parseBarrierFromDefaultFields(CsvRow row) {
    return parseBarrier(row, BARRIER_TYPE_FIELD, KNOCK_TYPE_FIELD, BARRIER_LEVEL_FIELD);
  }

  /**
   * Parses the premium using the default premium fields.
   *
   * @param row  the CSV row to parse
   * @return the adjustable payment
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static AdjustablePayment parsePremiumFromDefaultFields(CsvRow row) {
    return parseAdjustablePayment(
        row,
        PREMIUM_CURRENCY_FIELD,
        PREMIUM_AMOUNT_FIELD,
        PREMIUM_DIRECTION_FIELD,
        PREMIUM_DATE_FIELD,
        PREMIUM_DATE_CAL_FIELD,
        PREMIUM_DATE_CNV_FIELD);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a business day adjustment.
   *
   * @param row  the CSV row to parse
   * @param conventionField  the convention field
   * @param calendarField  the calendar field
   * @return the adjustment
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static Optional<BusinessDayAdjustment> parseBusinessDayAdjustment(
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
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static DaysAdjustment parseDaysAdjustment(
      CsvRow row,
      String daysField,
      String daysCalField,
      String cnvField,
      String calField) {

    int days = row.getValue(daysField, LoaderUtils::parseInteger);
    HolidayCalendarId daysCal = row.findValue(daysCalField, HolidayCalendarId::of)
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
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static CurrencyAmount parseCurrencyAmount(CsvRow row, String currencyField, String amountField) {
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
   * @throws ParseFailureException if the row cannot be parsed
   */
  public static CurrencyAmount parseCurrencyAmountWithDirection(
      CsvRow row,
      String currencyField,
      String amountField,
      String directionField) {

    Currency currency = row.getValue(currencyField, LoaderUtils::parseCurrency);
    double amount = row.getValue(amountField, LoaderUtils::parseDouble);
    PayReceive direction = row.getValue(directionField, LoaderUtils::parsePayReceive);
    return CurrencyAmount.of(currency, direction.normalize(amount));
  }

  /**
   * Tries parsing an adjustable date from the mentioned fields in the csv row.
   *
   * @param row  the CSV row to parse
   * @param dateField  the date field
   * @param conventionField  the convention field
   * @param calendarField  the calendar field
   * @return the adjustable date option
   */
  public static Optional<AdjustableDate> tryParseAdjustableDate(
      CsvRow row,
      String dateField,
      String conventionField,
      String calendarField) {

    Optional<LocalDate> date = row.findValue(dateField, LoaderUtils::parseDate);
    Optional<BusinessDayConvention> convention = row.findValue(
        conventionField,
        LoaderUtils::parseBusinessDayConvention);
    Optional<HolidayCalendarId> direction = row.findValue(calendarField, HolidayCalendarId::of);

    if (date.isPresent() && convention.isPresent() && direction.isPresent()) {
      return Optional.of(AdjustableDate.of(date.get(), BusinessDayAdjustment.of(convention.get(), direction.get())));
    }
    return Optional.empty();
  }

  /**
   * Tries parsing a currency amount from the mentioned fields in the csv row.
   *
   * @param row  the CSV row to parse
   * @param currencyField  the currency field
   * @param amountField  the amount field
   * @param directionField  the direction field
   * @return the currency amount option
   */
  public static Optional<CurrencyAmount> tryParseCurrencyAmountWithDirection(
      CsvRow row,
      String currencyField,
      String amountField,
      String directionField) {

    Optional<Currency> currency = row.findValue(currencyField, LoaderUtils::parseCurrency);
    Optional<Double> amount = row.findValue(amountField, LoaderUtils::parseDouble);
    Optional<PayReceive> direction = row.findValue(directionField, LoaderUtils::parsePayReceive);

    if (currency.isPresent() && amount.isPresent() && direction.isPresent()) {
      return Optional.of(CurrencyAmount.of(currency.get(), direction.get().normalize(amount.get())));
    }
    return Optional.empty();
  }

  /**
   * Tries parsing the premium using the default premium fields.
   *
   * @param row  the CSV row to parse
   * @return the premium option
   */
  public static Optional<AdjustablePayment> tryParsePremiumFromDefaultFields(CsvRow row) {
    return tryParseAdjustablePayment(
        row,
        PREMIUM_CURRENCY_FIELD,
        PREMIUM_AMOUNT_FIELD,
        PREMIUM_DIRECTION_FIELD,
        PREMIUM_DATE_FIELD,
        PREMIUM_DATE_CNV_FIELD,
        PREMIUM_DATE_CAL_FIELD);
  }

  /**
   * Tries parsing an adjustable payment, defaulting the AdjustableDate to no BusinessDayAdjustment.
   *
   * @param row  the CSV row to parse
   * @param currencyField  the currency field
   * @param amountField  the amount field
   * @param directionField  the direction field
   * @param dateField  the date field
   * @return the adjustable payment option
   */
  public static Optional<AdjustablePayment> tryParseAdjustablePayment(
      CsvRow row,
      String currencyField,
      String amountField,
      String directionField,
      String dateField) {

    Optional<CurrencyAmount> currencyAmount = tryParseCurrencyAmountWithDirection(
        row,
        currencyField,
        amountField,
        directionField);
    Optional<LocalDate> localDate = row.findValue(dateField, LoaderUtils::parseDate);

    if (currencyAmount.isPresent() && localDate.isPresent()) {
      return Optional.of(AdjustablePayment.of(currencyAmount.get(), localDate.get()));
    }
    return Optional.empty();
  }

  /**
   * Tries parsing an adjustable payment using the mentioned fields.
   * <p>
   * Defaults to {@link #tryParseAdjustablePayment(CsvRow,String,String,String,String)} if the adjustable date parsing
   * fails as it is often due to a missing calendar or convention.
   *
   * @param row  the CSV row to parse
   * @param currencyField  the currency field
   * @param amountField  the amount field
   * @param directionField  the direction field
   * @param dateField  the date field
   * @param conventionField  the date convention field
   * @param calendarField  the date calendar field
   * @return the adjustable payment option
   */
  public static Optional<AdjustablePayment> tryParseAdjustablePayment(
      CsvRow row,
      String currencyField,
      String amountField,
      String directionField,
      String dateField,
      String conventionField,
      String calendarField) {

    Optional<CurrencyAmount> currencyAmount = tryParseCurrencyAmountWithDirection(
        row,
        currencyField,
        amountField,
        directionField);
    Optional<AdjustableDate> adjustableDate = tryParseAdjustableDate(
        row,
        dateField,
        conventionField,
        calendarField);
    if (currencyAmount.isPresent() && adjustableDate.isPresent()) {
      return Optional.of(AdjustablePayment.of(currencyAmount.get(), adjustableDate.get()));
    } else if (!adjustableDate.isPresent()) {
      return tryParseAdjustablePayment(row, currencyField, amountField, directionField, dateField);
    }
    return Optional.empty();
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
    return Decimal.of(value).movePoint(2).toString();
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
    return Decimal.of(value).toString();
  }
}
