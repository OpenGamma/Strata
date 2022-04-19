/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletBinaryPeriod;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link VerticalSpreadVolatilityIborCapletFloorletBinaryPeriodPricer}.
 */
class VerticalSpreadVolatilityIborCapletFloorletBinaryPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ZonedDateTime VALUATION = dateUtc(2008, 8, 18);
  private static final LocalDate FIXING = LocalDate.of(2011, 1, 3);
  private static final double AMOUNT = 1000000; //1m
  private static final double STRIKE = 0.01;

  private static final IborRateComputation RATE_COMP = IborRateComputation.of(EUR_EURIBOR_3M, FIXING, REF_DATA);
  private static final IborCapletFloorletBinaryPeriod CAPLET_LONG = IborCapletFloorletBinaryPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .amount(AMOUNT)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletBinaryPeriod CAPLET_SHORT = IborCapletFloorletBinaryPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .amount(-AMOUNT)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletBinaryPeriod FLOORLET_LONG = IborCapletFloorletBinaryPeriod.builder()
      .floorlet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .amount(AMOUNT)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletBinaryPeriod FLOORLET_SHORT = IborCapletFloorletBinaryPeriod.builder()
      .floorlet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .amount(-AMOUNT)
      .iborRate(RATE_COMP)
      .build();

  // valuation date before fixing date
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_EURIBOR_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrParametersIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_EURIBOR_3M);
  private static final double OBS_INDEX = 0.013;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final ImmutableRatesProvider RATES_AFTER_FIX =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(FIXING.plusWeeks(1), EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER_FIX = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(FIXING.plusWeeks(1).atStartOfDay(ZoneOffset.UTC), EUR_EURIBOR_3M);

  private static final VerticalSpreadVolatilityIborCapletFloorletBinaryPeriodPricer PRICER_BINARY_DEFAULT =
      VerticalSpreadVolatilityIborCapletFloorletBinaryPeriodPricer.DEFAULT;
  private static final double SPREAD_2 = 2.0E-4;
  private static final VerticalSpreadVolatilityIborCapletFloorletBinaryPeriodPricer PRICER_BINARY_2 =
      new VerticalSpreadVolatilityIborCapletFloorletBinaryPeriodPricer(
          VolatilityIborCapletFloorletPeriodPricer.DEFAULT, SPREAD_2);
  private static final VolatilityIborCapletFloorletPeriodPricer PRICER_VANILLA =
      VolatilityIborCapletFloorletPeriodPricer.DEFAULT;

  private static final Offset<Double> TOLERANCE_PV = Offset.offset(1.0E-2);
  private static final double TOLERANCE_PV01 = 1.0E+0;
  private static final double TOLERANCE_VEGA = 1.0E+0;

  //-------------------------------------------------------------------------
  @Test
  void call_spread() {
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairCapLong =
        PRICER_BINARY_2.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    assertThat(pairCapLong.getFirst().getStrike()).isEqualTo(STRIKE - SPREAD_2);
    assertThat(pairCapLong.getSecond().getStrike()).isEqualTo(STRIKE + SPREAD_2);
    assertThat(pairCapLong.getFirst().getNotional()).isEqualTo(-pairCapLong.getSecond().getNotional());
    assertThat(pairCapLong.getFirst().getNotional() * 2 * SPREAD_2).isEqualTo(AMOUNT);
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairCapShort =
        PRICER_BINARY_2.vanillaOptionVerticalSpreadPair(CAPLET_SHORT);
    assertThat(pairCapLong).isEqualTo(pairCapShort); // Long/short dealt with at pricer level
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairFloorLong =
        PRICER_BINARY_2.vanillaOptionVerticalSpreadPair(FLOORLET_LONG);
    assertThat(pairFloorLong.getFirst().getStrike()).isEqualTo(STRIKE - SPREAD_2);
    assertThat(pairFloorLong.getSecond().getStrike()).isEqualTo(STRIKE + SPREAD_2);
    assertThat(pairFloorLong.getFirst().getNotional()).isEqualTo(-pairFloorLong.getSecond().getNotional());
    assertThat(pairFloorLong.getSecond().getNotional() * 2 * SPREAD_2).isEqualTo(AMOUNT);
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairFloorShort =
        PRICER_BINARY_2.vanillaOptionVerticalSpreadPair(FLOORLET_SHORT);
    assertThat(pairFloorLong).isEqualTo(pairFloorShort); // Long/short dealt with at pricer level
  }

  /* Deep ITM, short expiry, option value = discounted amount */
  @Test
  void present_value_deep_itm() {
    IborRateComputation iborComputation = IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2008, 10, 17), REF_DATA);
    IborCapletFloorletBinaryPeriod capletItm = IborCapletFloorletBinaryPeriod.builder()
        .caplet(-0.0100)
        .startDate(iborComputation.getEffectiveDate())
        .endDate(iborComputation.getMaturityDate())
        .yearFraction(1.0)
        .amount(AMOUNT)
        .iborRate(iborComputation)
        .build();
    double df = RATES.discountFactor(EUR, capletItm.getPaymentDate());
    double pvExpected = df * AMOUNT;
    CurrencyAmount pvComputed = PRICER_BINARY_DEFAULT.presentValue(capletItm, RATES, VOLS);
    assertThat(pvComputed.getCurrency()).isEqualTo(EUR);
    assertThat(pvComputed.getAmount()).isEqualTo(pvExpected, TOLERANCE_PV);
  }

  /* Deep OTM, short expiry, option value = 0.0 */
  @Test
  void present_value_deep_otm() {
    IborRateComputation iborComputation = IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2008, 10, 17), REF_DATA);
    IborCapletFloorletBinaryPeriod capletOtm = IborCapletFloorletBinaryPeriod.builder()
        .caplet(0.0500)
        .startDate(iborComputation.getEffectiveDate())
        .endDate(iborComputation.getMaturityDate())
        .yearFraction(1.0)
        .amount(AMOUNT)
        .iborRate(iborComputation)
        .build();
    double pvExpected = 0.0;
    CurrencyAmount pvComputed = PRICER_BINARY_DEFAULT.presentValue(capletOtm, RATES, VOLS);
    assertThat(pvComputed.getCurrency()).isEqualTo(EUR);
    assertThat(pvComputed.getAmount()).isEqualTo(pvExpected, TOLERANCE_PV);
  }

  @Test
  void present_value() {
    CurrencyAmount pvCapLongComputed = PRICER_BINARY_DEFAULT.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount pvCapShortComputed = PRICER_BINARY_DEFAULT.presentValue(CAPLET_SHORT, RATES, VOLS);
    CurrencyAmount pvFloorLongComputed = PRICER_BINARY_DEFAULT.presentValue(FLOORLET_LONG, RATES, VOLS);
    CurrencyAmount pvFloorShortComputed = PRICER_BINARY_DEFAULT.presentValue(FLOORLET_SHORT, RATES, VOLS);
    assertThat(pvCapLongComputed.getAmount()).isEqualTo(-pvCapShortComputed.getAmount(), TOLERANCE_PV); // Long / Short
    assertThat(pvFloorLongComputed.getAmount()).isEqualTo(-pvFloorShortComputed.getAmount(), TOLERANCE_PV); // Long / Short
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    assertThat(pvCapLongComputed.getAmount() + pvFloorLongComputed.getAmount())
        .isEqualTo(df * AMOUNT * CAPLET_LONG.getYearFraction(), TOLERANCE_PV); // Call + floor = discounted amount
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairCapLong =
        PRICER_BINARY_DEFAULT.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    CurrencyAmount pvLow = PRICER_VANILLA.presentValue(pairCapLong.getFirst(), RATES, VOLS);
    CurrencyAmount pvHigh = PRICER_VANILLA.presentValue(pairCapLong.getSecond(), RATES, VOLS);
    assertThat(pvCapLongComputed.getAmount())
        .isEqualTo(pvLow.getAmount() + pvHigh.getAmount(), TOLERANCE_PV);
  }

  @Test
  void present_value_rates_sensitivity() {
    PointSensitivityBuilder ptsCapLongComputed =
        PRICER_BINARY_DEFAULT.presentValueSensitivityRatesStickyStrike(CAPLET_LONG, RATES, VOLS);
    CurrencyParameterSensitivities psCapLongComputed =
        RATES.parameterSensitivity(ptsCapLongComputed.build());
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairCapLong =
        PRICER_BINARY_DEFAULT.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    PointSensitivityBuilder ptsLow =
        PRICER_VANILLA.presentValueSensitivityRates(pairCapLong.getFirst(), RATES, VOLS);
    PointSensitivityBuilder ptsHigh =
        PRICER_VANILLA.presentValueSensitivityRates(pairCapLong.getSecond(), RATES, VOLS);
    CurrencyParameterSensitivities psCapLongExpected =
        RATES.parameterSensitivity(ptsLow.combinedWith(ptsHigh).build());
    assertThat(psCapLongComputed.equalWithTolerance(psCapLongExpected, TOLERANCE_PV01)).isTrue();
  }

  @Test
  void present_value_parameters_sensitivity() {
    PointSensitivityBuilder ptsCapLongComputed =
        PRICER_BINARY_DEFAULT.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES, VOLS);
    CurrencyParameterSensitivities psCapLongComputed =
        VOLS.parameterSensitivity(ptsCapLongComputed.build());
    Pair<IborCapletFloorletPeriod, IborCapletFloorletPeriod> pairCapLong =
        PRICER_BINARY_DEFAULT.vanillaOptionVerticalSpreadPair(CAPLET_LONG);
    PointSensitivityBuilder ptsLow =
        PRICER_VANILLA.presentValueSensitivityModelParamsVolatility(pairCapLong.getFirst(), RATES, VOLS);
    PointSensitivityBuilder ptsHigh =
        PRICER_VANILLA.presentValueSensitivityModelParamsVolatility(pairCapLong.getSecond(), RATES, VOLS);
    CurrencyParameterSensitivities psCapLongExpected =
        VOLS.parameterSensitivity(ptsLow.combinedWith(ptsHigh).build());
    assertThat(psCapLongComputed.equalWithTolerance(psCapLongExpected, TOLERANCE_VEGA)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  void present_value_afterfix() {
    CurrencyAmount pvCapLongComputed =
        PRICER_BINARY_DEFAULT.presentValue(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyAmount pvCapShortComputed =
        PRICER_BINARY_DEFAULT.presentValue(CAPLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertThat(pvCapLongComputed.getAmount()).isEqualTo(0.0d, TOLERANCE_PV);
    assertThat(pvCapShortComputed.getAmount()).isEqualTo(0.0d, TOLERANCE_PV);
    PointSensitivityBuilder ptsRatesCapLongComputed =
        PRICER_BINARY_DEFAULT.presentValueSensitivityRatesStickyStrike(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertThat(ptsRatesCapLongComputed).isEqualTo(PointSensitivityBuilder.none());
    PointSensitivityBuilder ptsModelCapLongComputed =
        PRICER_BINARY_DEFAULT.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertThat(ptsModelCapLongComputed).isEqualTo(PointSensitivityBuilder.none());
  }

}
