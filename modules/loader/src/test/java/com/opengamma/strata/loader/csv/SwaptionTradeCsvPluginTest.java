/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.loader.csv.CsvTestUtils.checkRoundtrip;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.AdjustableDates;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionExercise;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Tests for the {@link SwaptionTradeCsvPlugin}
 */
final class SwaptionTradeCsvPluginTest {

  private static final ResourceLocator CSV_FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/swaption_trades.csv");

  private static final SwaptionTradeCsvPlugin PLUGIN = SwaptionTradeCsvPlugin.INSTANCE;

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 8, 7);
  private static final LocalDate SWAPTION_EXERCISE_DATE = VAL_DATE.plusYears(5);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(17, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SWAP_EFFECTIVE_DATE =
      USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE, REF_DATA);
  private static final LocalDate SWAP_MATURITY_DATE = SWAP_EFFECTIVE_DATE.plus(Period.ofYears(10));
  private static final Swap SWAP_REC = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, SELL, 1E6, 0.01).getProduct();

  // With no exercise info, the exercise date is the expiry date.
  private static final Swaption DEFAULT_EUROPEAN_SWAPTION = Swaption.builder()
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();

  private static final Swaption EUROPEAN_SWAPTION_WITH_SPECIFIC_EXERCISE = Swaption.builder()
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .exerciseInfo(SwaptionExercise.ofEuropean(AdjustableDate.of(date(2018, 6, 14)), DaysAdjustment.NONE))
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();

  private static final AdjustableDates BERMUDAN_EXERCISE_DATES =
      AdjustableDates.of(date(2016, 6, 14), date(2017, 6, 14), date(2018, 6, 14), date(2019, 6, 14));

  private static final Swaption BERMUDAN_SWAPTION = Swaption.builder()
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .exerciseInfo(SwaptionExercise.ofBermudan(BERMUDAN_EXERCISE_DATES, DaysAdjustment.NONE))
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build();

  private static SwaptionTrade toSwaptionTrade(Swaption swaption) {
    return SwaptionTrade.of(TradeInfo.empty(), swaption, Payment.of(CurrencyAmount.of(USD, 0), VAL_DATE));
  }

  private static final SwaptionTrade DEFAULT_EUROPEAN_SWAPTION_TRADE = toSwaptionTrade(DEFAULT_EUROPEAN_SWAPTION);
  private static final SwaptionTrade EUROPEAN_SWAPTION_WITH_SPECIFIC_EXERCISE_TRADE =
      toSwaptionTrade(EUROPEAN_SWAPTION_WITH_SPECIFIC_EXERCISE);
  private static final SwaptionTrade BERMUDAN_SWAPTION_TRADE = toSwaptionTrade(BERMUDAN_SWAPTION);

  @Test
  void testSwaptionCsvPlugin() {
    ValueWithFailures<List<SwaptionTrade>> trades = TradeCsvLoader.standard()
        .parse(ImmutableList.of(CSV_FILE.getCharSource()), SwaptionTrade.class);
    checkRoundtrip(
        SwaptionTrade.class,
        trades.getValue(),
        EUROPEAN_SWAPTION_WITH_SPECIFIC_EXERCISE_TRADE,
        DEFAULT_EUROPEAN_SWAPTION_TRADE,
        BERMUDAN_SWAPTION_TRADE);
  }

}
