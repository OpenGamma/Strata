/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletBinaryPeriod;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

/**
 * Test {@link VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer}.
 */
class VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex EUR_ESTRTERM_3M = ImmutableIborIndex.builder()
      .name("EUR-ESTRTERM-3M")
      .currency(EUR)
      .dayCount(DayCounts.ACT_360)
      .fixingCalendar(EUTA)
      .fixingTime(LocalTime.of(11, 0))
      .fixingZone(ZoneId.of("Europe/Brussels"))
      .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
      .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
      .maturityDateOffset(TenorAdjustment.of(
          Tenor.TENOR_3M,
          PeriodAdditionConventions.LAST_BUSINESS_DAY,
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA)))
      .build();
  private static final ZonedDateTime VALUATION = dateUtc(2021, 12, 20);
  private static final LocalDate START_DATE = LocalDate.of(2022, 6, 22);
  private static final LocalDate END_DATE = LocalDate.of(2022, 9, 22);
  private static final LocalDate PAYMENT_DATE = END_DATE.plusMonths(1);
  private static final double AMOUNT = 1_000_000.0d;
  private static final double STRIKE = 0.0155;
  private static final double ACCRUAL_FACTOR = 0.30;
  private static final OvernightCompoundedRateComputation RATE_COMP =
      OvernightCompoundedRateComputation.of(EUR_ESTR, START_DATE, END_DATE, REF_DATA);

  private static final OvernightInArrearsCapletFloorletBinaryPeriod CAPLET_LONG =
      OvernightInArrearsCapletFloorletBinaryPeriod.builder()
          .caplet(STRIKE)
          .startDate(START_DATE)
          .endDate(END_DATE)
          .paymentDate(PAYMENT_DATE)
          .yearFraction(ACCRUAL_FACTOR)
          .amount(AMOUNT)
          .overnightRate(RATE_COMP)
          .build();
  private static final OvernightInArrearsCapletFloorletBinaryPeriod CAPLET_SHORT =
      CAPLET_LONG.toBuilder().amount(-AMOUNT).build();
  private static final OvernightInArrearsCapletFloorletBinaryPeriod FLOORLET_LONG =
      CAPLET_LONG.toBuilder().caplet(null).floorlet(STRIKE).build();
  private static final OvernightInArrearsCapletFloorletBinaryPeriod FLOORLET_SHORT =
      CAPLET_SHORT.toBuilder().caplet(null).floorlet(STRIKE).build();

  // valuation date before start date
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_ESTRTERM_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrParametersIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_ESTRTERM_3M);

  // Pricers
  private static final VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer PRICER_SABR_BINARYIA =
      VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer.DEFAULT;
  private static final SabrOvernightInArrearsCapletFloorletPeriodPricer PRICER_VANILLA =
      SabrOvernightInArrearsCapletFloorletPeriodPricer.DEFAULT;

  private static final Offset<Double> TOLERANCE_PV = Offset.offset(1.0E-2);
  private static final double TOLERANCE_PV01 = 1.0E+0;
  private static final double TOLERANCE_VEGA = 1.0E+0;

  /* Check that correct call spread are generated. */
  @Test
  void call_spread() {
    double spread = PRICER_SABR_BINARYIA.getSpread();
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairCapLong =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    assertThat(pairCapLong.getFirst().getStrike()).isEqualTo(STRIKE - spread);
    assertThat(pairCapLong.getSecond().getStrike()).isEqualTo(STRIKE + spread);
    assertThat(pairCapLong.getFirst().getNotional()).isEqualTo(-pairCapLong.getSecond().getNotional());
    assertThat(pairCapLong.getFirst().getNotional() * 2 * spread).isEqualTo(AMOUNT);
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairCapShort =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(CAPLET_SHORT);
    assertThat(pairCapLong).isEqualTo(pairCapShort); // Long/short dealt with at pricer level
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairFloorLong =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(FLOORLET_LONG);
    assertThat(pairFloorLong.getFirst().getStrike()).isEqualTo(STRIKE - spread);
    assertThat(pairFloorLong.getSecond().getStrike()).isEqualTo(STRIKE + spread);
    assertThat(pairFloorLong.getFirst().getNotional()).isEqualTo(-pairFloorLong.getSecond().getNotional());
    assertThat(pairFloorLong.getSecond().getNotional() * 2 * spread).isEqualTo(AMOUNT);
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairFloorShort =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(FLOORLET_SHORT);
    assertThat(pairFloorLong).isEqualTo(pairFloorShort); // Long/short dealt with at pricer level
  }

  /* Deep ITM, short expiry, option value = discounted amount */
  @Test
  void present_value_deep_itm() {
    OvernightCompoundedRateComputation rateComputation =
        OvernightCompoundedRateComputation.of(EUR_ESTR, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 2, 21), REF_DATA);
    OvernightInArrearsCapletFloorletBinaryPeriod capletItm =
        OvernightInArrearsCapletFloorletBinaryPeriod.builder()
            .caplet(-0.0100)
            .startDate(START_DATE)
            .endDate(END_DATE)
            .paymentDate(PAYMENT_DATE)
            .yearFraction(1.0)
            .amount(AMOUNT)
            .overnightRate(rateComputation)
            .build();
    double df = RATES.discountFactor(EUR, capletItm.getPaymentDate());
    double pvExpected = df * AMOUNT;
    CurrencyAmount pvComputed = PRICER_SABR_BINARYIA.presentValue(capletItm, RATES, VOLS);
    assertThat(pvComputed.getCurrency()).isEqualTo(EUR);
    assertThat(pvComputed.getAmount()).isEqualTo(pvExpected, TOLERANCE_PV);
  }

  /* Deep OTM, short expiry, option value = 0.0 */
  @Test
  void present_value_deep_otm() {
    OvernightCompoundedRateComputation rateComputation =
        OvernightCompoundedRateComputation.of(EUR_ESTR, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 2, 21), REF_DATA);
    OvernightInArrearsCapletFloorletBinaryPeriod capletOtm =
        OvernightInArrearsCapletFloorletBinaryPeriod.builder()
            .caplet(0.0500)
            .startDate(START_DATE)
            .endDate(END_DATE)
            .paymentDate(PAYMENT_DATE)
            .yearFraction(1.0)
            .amount(AMOUNT)
            .overnightRate(rateComputation)
            .build();
    double pvExpected = 0.0;
    CurrencyAmount pvComputed = PRICER_SABR_BINARYIA.presentValue(capletOtm, RATES, VOLS);
    assertThat(pvComputed.getCurrency()).isEqualTo(EUR);
    assertThat(pvComputed.getAmount()).isEqualTo(pvExpected, TOLERANCE_PV);
  }

  @Test
  void present_value_long_short() {
    CurrencyAmount pvCapletLongComputed = PRICER_SABR_BINARYIA.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount pvCapletSortComputed = PRICER_SABR_BINARYIA.presentValue(CAPLET_SHORT, RATES, VOLS);
    assertThat(pvCapletLongComputed.getAmount()).isEqualTo(-pvCapletSortComputed.getAmount(), TOLERANCE_PV);
    CurrencyAmount pvFloorletLongComputed = PRICER_SABR_BINARYIA.presentValue(FLOORLET_LONG, RATES, VOLS);
    CurrencyAmount pvFloorletSortComputed = PRICER_SABR_BINARYIA.presentValue(FLOORLET_SHORT, RATES, VOLS);
    assertThat(pvFloorletLongComputed.getAmount()).isEqualTo(-pvFloorletSortComputed.getAmount(), TOLERANCE_PV);
  }

  @Test
  void put_call_parity() {
    CurrencyAmount pvCapletLongComputed = PRICER_SABR_BINARYIA.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount pvFloorletLongComputed = PRICER_SABR_BINARYIA.presentValue(FLOORLET_LONG, RATES, VOLS);
    assertThat(pvCapletLongComputed.getAmount() + pvFloorletLongComputed.getAmount())
        .isEqualTo(RATES.discountFactor(EUR, PAYMENT_DATE) * AMOUNT * ACCRUAL_FACTOR, TOLERANCE_PV);
  }

  @Test
  void present_value() {
    CurrencyAmount pvCapLongComputed = PRICER_SABR_BINARYIA.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount pvFloorLongComputed = PRICER_SABR_BINARYIA.presentValue(FLOORLET_LONG, RATES, VOLS);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    assertThat(pvCapLongComputed.getAmount() + pvFloorLongComputed.getAmount())
        .isEqualTo(df * AMOUNT * CAPLET_LONG.getYearFraction(), TOLERANCE_PV); // Call + floor = discounted amount
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairCapLong =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    CurrencyAmount pvLow = PRICER_VANILLA.presentValue(pairCapLong.getFirst(), RATES, VOLS);
    CurrencyAmount pvHigh = PRICER_VANILLA.presentValue(pairCapLong.getSecond(), RATES, VOLS);
    assertThat(pvCapLongComputed.getAmount())
        .isEqualTo(pvLow.getAmount() + pvHigh.getAmount(), TOLERANCE_PV);
  }

  @Test
  void present_value_rates_sensitivity() {
    PointSensitivityBuilder ptsCapLongComputed =
        PRICER_SABR_BINARYIA.presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES, VOLS);
    CurrencyParameterSensitivities psCapLongComputed =
        RATES.parameterSensitivity(ptsCapLongComputed.build());
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairCapLong =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    PointSensitivityBuilder ptsLow =
        PRICER_VANILLA.presentValueSensitivityRatesStickyModel(pairCapLong.getFirst(), RATES, VOLS);
    PointSensitivityBuilder ptsHigh =
        PRICER_VANILLA.presentValueSensitivityRatesStickyModel(pairCapLong.getSecond(), RATES, VOLS);
    CurrencyParameterSensitivities psCapLongExpected =
        RATES.parameterSensitivity(ptsLow.combinedWith(ptsHigh).build());
    assertThat(psCapLongComputed.equalWithTolerance(psCapLongExpected, TOLERANCE_PV01)).isTrue();
  }

  @Test
  void present_value_parameters_sensitivity() {
    PointSensitivityBuilder ptsCapLongComputed =
        PRICER_SABR_BINARYIA.presentValueSensitivityModelParamsSabr(CAPLET_LONG, RATES, VOLS);
    CurrencyParameterSensitivities psCapLongComputed =
        VOLS.parameterSensitivity(ptsCapLongComputed.build());
    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> pairCapLong =
        PRICER_SABR_BINARYIA.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    PointSensitivityBuilder ptsLow =
        PRICER_VANILLA.presentValueSensitivityModelParamsSabr(pairCapLong.getFirst(), RATES, VOLS);
    PointSensitivityBuilder ptsHigh =
        PRICER_VANILLA.presentValueSensitivityModelParamsSabr(pairCapLong.getSecond(), RATES, VOLS);
    CurrencyParameterSensitivities psCapLongExpected =
        VOLS.parameterSensitivity(ptsLow.combinedWith(ptsHigh).build());
    assertThat(psCapLongComputed.equalWithTolerance(psCapLongExpected, TOLERANCE_VEGA)).isTrue();
  }

}
