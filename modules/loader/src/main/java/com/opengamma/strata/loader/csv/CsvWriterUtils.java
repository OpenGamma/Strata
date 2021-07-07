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

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.SecurityQuantityTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.option.Barrier;

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
    csv.writeCell(PREMIUM_DATE_FIELD, premium.getDate().getUnadjusted());
    csv.writeCell(PREMIUM_DATE_CNV_FIELD, premium.getDate().getAdjustment().getConvention());
    csv.writeCell(PREMIUM_DATE_CAL_FIELD, premium.getDate().getAdjustment().getCalendar());
    csv.writeCell(PREMIUM_DIRECTION_FIELD, PayReceive.ofSignedAmount(premium.getAmount()));
    csv.writeCell(PREMIUM_CURRENCY_FIELD, premium.getCurrency());
    csv.writeCell(PREMIUM_AMOUNT_FIELD, premium.getAmount());
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
   * Writes a Barrier object to CSV
   *
   * @param csv  the csv row output
   * @param barrier  the barrier
   * @param obsDate  the barrier observation date
   */
  public static void writeBarrierFields(CsvOutput.CsvRowOutputWithHeaders csv, Barrier barrier, LocalDate obsDate) {
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
