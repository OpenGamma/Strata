/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_LEVEL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BARRIER_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_TIME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.EXPIRY_ZONE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.KNOCK_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_1_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LEG_2_PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.LONG_SHORT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_AMOUNT_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PREMIUM_DIRECTION_FIELD;

import java.time.LocalDate;
import java.time.ZoneId;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.option.Barrier;

/**
 * Groups several utilities method for CsvPlugins
 */
public final class CsvWriterUtils {

  /**
   * Restricted constructor
   */
  private CsvWriterUtils() {
  }

  /**
   * Writes an AdjustablePayment object to CSV
   * <p>
   * @param csv the csv row output
   * @param premium the premium
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
   * Writes a Barrier object to csv
   * <p>
   * @param csv the csv row output
   * @param barrier the barrier
   */
  public static void writeBarrierFields(CsvOutput.CsvRowOutputWithHeaders csv, Barrier barrier) {
    csv.writeCell(KNOCK_TYPE_FIELD, barrier.getKnockType());
    csv.writeCell(BARRIER_TYPE_FIELD, barrier.getBarrierType());
    csv.writeCell(BARRIER_LEVEL_FIELD, barrier.getBarrierLevel(LocalDate.now(ZoneId.systemDefault())));
  }

  /**
   * Write the FxSingle to CSV
   * <p>
   * @param csv the csv row output
   * @param prefix a prefix to use on leg headers (often far / near)
   * @param product the fx single
   */
  public static void writeFxSingle(CsvOutput.CsvRowOutputWithHeaders csv, String prefix, FxSingle product) {
    Payment basePayment = product.getBaseCurrencyPayment();
    csv.writeCell(prefix + LEG_1_DIRECTION_FIELD, PayReceive.ofSignedAmount(basePayment.getAmount()));
    csv.writeCell(prefix + LEG_1_CURRENCY_FIELD, basePayment.getCurrency());
    csv.writeCell(prefix + LEG_1_NOTIONAL_FIELD, basePayment.getAmount());
    csv.writeCell(prefix + LEG_1_PAYMENT_DATE_FIELD, basePayment.getDate());
    Payment counterPayment = product.getCounterCurrencyPayment();
    csv.writeCell(prefix + LEG_2_DIRECTION_FIELD, PayReceive.ofSignedAmount(counterPayment.getAmount()));
    csv.writeCell(prefix + LEG_2_CURRENCY_FIELD, counterPayment.getCurrency());
    csv.writeCell(prefix + LEG_2_NOTIONAL_FIELD, counterPayment.getAmount());
    csv.writeCell(prefix + LEG_2_PAYMENT_DATE_FIELD, counterPayment.getDate());
    product.getPaymentDateAdjustment().ifPresent(bda -> {
      csv.writeCell(PAYMENT_DATE_CAL_FIELD, bda.getCalendar());
      csv.writeCell(PAYMENT_DATE_CNV_FIELD, bda.getConvention());
    });
  }

  /**
   * Write a FxVanillaOption to CSV
   *
   * @param csv the csv row output
   * @param product the product
   */
  public static void writeFxVanillaOption(CsvOutput.CsvRowOutputWithHeaders csv, FxVanillaOption product) {
    csv.writeCell(LONG_SHORT_FIELD, product.getLongShort());
    csv.writeCell(EXPIRY_DATE_FIELD, product.getExpiryDate());
    csv.writeCell(EXPIRY_TIME_FIELD, product.getExpiryTime());
    csv.writeCell(EXPIRY_ZONE_FIELD, product.getExpiryZone());
    writeFxSingle(csv, "", product.getUnderlying());
  }

}
