/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_LEVEL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.KNOCK_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.SecurityQuantityTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.option.Barrier;
import com.opengamma.strata.product.swap.Swap;

/**
 * Groups several utilities methods for CsvPlugins
 */
public final class CsvWriterUtils {

  /**
   * Restricted constructor
   */
  private CsvWriterUtils() {
  }

  /**
   * Writes an AdjustablePayment object to CSV
   *
   * @param csv  the csv row output
   * @param premium  the premium
   */
  public static void writePremiumFields(CsvOutput.CsvRowOutputWithHeaders csv, AdjustablePayment premium) {
    writeAdjustablePayment(
        csv,
        premium,
        PREMIUM_AMOUNT_FIELD,
        PREMIUM_CURRENCY_FIELD,
        PREMIUM_DIRECTION_FIELD,
        PREMIUM_DATE_FIELD,
        PREMIUM_DATE_CNV_FIELD,
        PREMIUM_DATE_CAL_FIELD);
  }

  /**
   * Writes an AdjustablePayment object to CSV
   *
   * @param csv  the csv row
   * @param adjustablePayment  the adjustable payment
   * @param amountField  the amount field
   * @param currencyField  the currency field
   * @param directionField  the direction field
   * @param dateField  the date field
   * @param dateConventionField  the date convention field
   * @param dateCalendarField  the date calendar field
   */
  public static void writeAdjustablePayment(
      CsvOutput.CsvRowOutputWithHeaders csv,
      AdjustablePayment adjustablePayment,
      String amountField,
      String currencyField,
      String directionField,
      String dateField,
      String dateConventionField,
      String dateCalendarField) {

    csv.writeCell(amountField, adjustablePayment.getAmount());
    csv.writeCell(currencyField, adjustablePayment.getCurrency());
    csv.writeCell(directionField, PayReceive.ofSignedAmount(adjustablePayment.getAmount()));
    csv.writeCell(dateField, adjustablePayment.getDate().getUnadjusted());
    csv.writeCell(dateConventionField, adjustablePayment.getDate().getAdjustment().getConvention());
    csv.writeCell(dateCalendarField, adjustablePayment.getDate().getAdjustment().getCalendar());
  }

  /**
   * Writes a currency amount using the provided fields
   *
   * @param csv  the csv row output
   * @param ccyAmount  the currency amount to write
   * @param amountField  the amount header
   * @param currencyField  the currency header
   * @param directionField  the direction header
   */
  public static void writeCurrencyAmount(
      CsvOutput.CsvRowOutputWithHeaders csv,
      CurrencyAmount ccyAmount,
      String amountField,
      String currencyField,
      String directionField) {

    csv.writeCell(amountField, ccyAmount.getAmount());
    csv.writeCell(currencyField, ccyAmount.getCurrency());
    csv.writeCell(directionField, PayReceive.ofSignedAmount(ccyAmount.getAmount()));
  }

  /**
   * Writes a zoned date time using the provided field
   *
   * @param csv  the csv row output
   * @param zonedDateTime  the zoned date time object
   * @param dateField  the date field
   * @param timeField  the time field
   * @param zoneField  the zone field
   */
  public static void writeZonedDateTime(
      CsvOutput.CsvRowOutputWithHeaders csv,
      ZonedDateTime zonedDateTime,
      String dateField,
      String timeField,
      String zoneField) {

    csv.writeCell(dateField, zonedDateTime.toLocalDate());
    csv.writeCell(timeField, zonedDateTime.toLocalTime());
    csv.writeCell(zoneField, zonedDateTime.getZone());
  }

  /**
   * Writes a Barrier object to CSV
   *
   * @param csv  the csv row output
   * @param barrier  the barrier
   * @param obsDate  the barrier observation date
   */
  public static void writeBarrier(CsvOutput.CsvRowOutputWithHeaders csv, Barrier barrier, LocalDate obsDate) {
    csv.writeCell(BARRIER_TYPE_FIELD, barrier.getBarrierType());
    csv.writeCell(KNOCK_TYPE_FIELD, barrier.getKnockType());
    csv.writeCell(BARRIER_LEVEL_FIELD, barrier.getBarrierLevel(obsDate));
  }

  /**
   * Write the FxSingle to CSV
   *
   * @param csv  the csv row output
   * @param prefix  a prefix to use on leg headers (often far / near)
   * @param product  the fx single
   */
  public static void writeFxSingle(CsvOutput.CsvRowOutputWithHeaders csv, String prefix, FxSingle product) {
    FxSingleTradeCsvPlugin.INSTANCE.writeFxSingle(csv, prefix, product);
  }

  /**
   * Write a FxVanillaOption to CSV
   *
   * @param csv  the csv row output
   * @param product  the product
   */
  public static void writeFxVanillaOption(CsvOutput.CsvRowOutputWithHeaders csv, FxVanillaOption product) {
    FxVanillaOptionTradeCsvPlugin.INSTANCE.writeFxVanillaOption(csv, product);
  }

  /**
   * Write a Swap to CSV
   *
   * @param csv  the csv row output
   * @param product  the swap to write
   */
  public static void writeSwap(CsvOutput.CsvRowOutputWithHeaders csv, Swap product) {
    FullSwapTradeCsvPlugin.INSTANCE.writeProduct(csv, product);
  }

  /**
   * Write a SecurityQuantityTrade to CSV
   *
   * @param csv  the csv row output
   * @param trade  the security quantity trade
   */
  public static void writeSecurityQuantityTrade(CsvOutput.CsvRowOutputWithHeaders csv, SecurityQuantityTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "Security");
    csv.writeCell(SECURITY_ID_SCHEME_FIELD, trade.getSecurityId().getStandardId().getScheme());
    csv.writeCell(SECURITY_ID_FIELD, trade.getSecurityId().getStandardId().getValue());
    csv.writeCell(BUY_SELL_FIELD, trade.getQuantity() < 0 ? BuySell.SELL : BuySell.BUY);
    csv.writeCell(QUANTITY_FIELD, Math.abs(trade.getQuantity()));
    csv.writeCell(PRICE_FIELD, trade.getPrice());
  }

}
