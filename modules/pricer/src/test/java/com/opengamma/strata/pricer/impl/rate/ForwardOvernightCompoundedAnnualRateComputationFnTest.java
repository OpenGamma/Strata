/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.BRL_CDI;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.OvernightRateSensitivity;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.rate.OvernightCompoundedAnnualRateComputation;

/**
 * Test {@link ForwardOvernightCompoundedAnnualRateComputationFn}.
 */
public class ForwardOvernightCompoundedAnnualRateComputationFnTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2015, 1, 31); // Accrual dates irrelevant for the rate
  private static final LocalDate FIXING_START_DATE = date(2015, 1, 8);
  private static final LocalDate FIXING_END_DATE = date(2015, 1, 15); // 1w only to decrease data
  private static final LocalDate[] FIXING_DATES = new LocalDate[] {
      date(2015, 1, 7),
      date(2015, 1, 8),
      date(2015, 1, 9),
      date(2015, 1, 12),
      date(2015, 1, 13),
      date(2015, 1, 14),
      date(2015, 1, 15)};
  private static final OvernightIndexObservation[] BRL_OBS = new OvernightIndexObservation[] {
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 7), REF_DATA),
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 8), REF_DATA),
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 9), REF_DATA),
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 12), REF_DATA),
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 13), REF_DATA),
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 14), REF_DATA),
      OvernightIndexObservation.of(BRL_CDI, date(2015, 1, 15), REF_DATA)};

  private static final double[] FIXING_RATES = {0.0012, 0.0023, 0.0034, 0.0045, 0.0056, 0.0067, 0.0078};
  private static final double[] FORWARD_RATES = {0.0112, 0.0123, 0.0134, 0.0145, 0.0156, 0.0167, 0.0178};

  private static final double TOLERANCE_RATE = 1.0E-10;
  private static final double EPS_FD = 1.0E-7;

  private static final ForwardOvernightCompoundedAnnualRateComputationFn OBS_BRL_FWD_ONCMP =
      ForwardOvernightCompoundedAnnualRateComputationFn.DEFAULT;

  @Test
  public void test_rateForward() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    double rateCmp = 0.0123;
    when(mockRates.periodRate(BRL_OBS[1], FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = rateCmp;
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertThat(rateExpected).isCloseTo(rateComputed, offset(TOLERANCE_RATE));
    }

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    double explainedRate = OBS_BRL_FWD_ONCMP.explainRate(
        ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv, builder);
    assertThat(explainedRate).isCloseTo(rateExpected, offset(TOLERANCE_RATE));

    ExplainMap built = builder.build();
    assertThat(built.get(ExplainKey.OBSERVATIONS)).isNotPresent();
    assertThat(built.get(ExplainKey.COMBINED_RATE).get().doubleValue()).isCloseTo(rateExpected, offset(TOLERANCE_RATE));
  }

  @Test
  public void test_rateForwardSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    double rateCmp = 0.0123;
    when(mockRates.periodRate(BRL_OBS[1], FIXING_END_DATE)).thenReturn(rateCmp);
    PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.ofPeriod(BRL_OBS[1], FIXING_END_DATE, 1.0);
    when(mockRates.periodRatePointSensitivity(BRL_OBS[1], FIXING_END_DATE)).thenReturn(rateSensitivity);
    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    when(mockRatesUp.periodRate(BRL_OBS[1], FIXING_END_DATE)).thenReturn(rateCmp + EPS_FD);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesDw.periodRate(BRL_OBS[1], FIXING_END_DATE)).thenReturn(rateCmp - EPS_FD);

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected =
          OvernightRateSensitivity.ofPeriod(BRL_OBS[1], FIXING_END_DATE, sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_BRL_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertThat(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD)).isTrue();
    }
  }

  @Test
  public void test_rateForwardOneFixing() {
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getFixings()).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockRates.rate(BRL_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = 2; i < BRL_OBS.length; i++) {
      when(mockRates.rate(BRL_OBS[i])).thenReturn(FORWARD_RATES[i]);
    }
    LocalDate fixingknown = FIXING_DATES[1];
    LocalDate endDateKnown = BRL_CDI.calculateMaturityFromEffective(fixingknown, REF_DATA);
    double afKnown = BRL_CDI.getDayCount().yearFraction(fixingknown, endDateKnown);
    double investmentFactor = 1.0;
    double af = 0.0;
    for (int i = 2; i < 6; i++) {
      LocalDate endDate = BRL_CDI.calculateMaturityFromEffective(FIXING_DATES[i], REF_DATA);
      double localAf = BRL_CDI.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      af += localAf;
      investmentFactor *= Math.pow((1.0d + FORWARD_RATES[i]), localAf);
    }
    double rateCmp = (investmentFactor - 1.0d) / af;
    when(mockRates.periodRate(BRL_OBS[2], FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = (Math.pow(1.0d + FIXING_RATES[1], afKnown) * (1.0 + rateCmp * af) - 1.0d) / (afKnown + af);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertThat(rateExpected).isCloseTo(rateComputed, offset(TOLERANCE_RATE));
    }
  }

  @Test
  public void test_rateForwardOneFixingSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getFixings()).thenReturn(tsb.build());

    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesUp.getFixings()).thenReturn(tsb.build());
    when(mockRatesDw.getFixings()).thenReturn(tsb.build());
    double investmentFactor = 1.0;
    double af = 0.0;
    for (int i = 2; i < 6; i++) {
      LocalDate endDate = BRL_CDI.calculateMaturityFromEffective(FIXING_DATES[i], REF_DATA);
      double localAf = BRL_CDI.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      af += localAf;
      investmentFactor *= Math.pow(1.0d + FORWARD_RATES[i], localAf);
    }
    double rateCmp = (investmentFactor - 1.0d) / af;
    when(mockRates.periodRate(BRL_OBS[2], FIXING_END_DATE)).thenReturn(rateCmp);
    when(mockRatesUp.periodRate(BRL_OBS[2], FIXING_END_DATE)).thenReturn(rateCmp + EPS_FD);
    when(mockRatesDw.periodRate(BRL_OBS[2], FIXING_END_DATE)).thenReturn(rateCmp - EPS_FD);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.ofPeriod(BRL_OBS[2], FIXING_END_DATE, 1.0d);
    when(mockRates.periodRatePointSensitivity(BRL_OBS[2], FIXING_END_DATE)).thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected =
          OvernightRateSensitivity.ofPeriod(BRL_OBS[2], FIXING_END_DATE, sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_BRL_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertThat(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD)).isTrue();
    }
  }

  @Test
  public void test_rateAllFixed() {
    LocalDate[] valuationDate = {date(2015, 1, 15), date(2015, 1, 16), date(2015, 1, 17)};
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getFixings()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(BRL_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < BRL_OBS.length; i++) {
      when(mockRates.rate(BRL_OBS[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 0; i < 5; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = BRL_CDI.calculateMaturityFromEffective(fixingknown, REF_DATA);
      double af = BRL_CDI.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= Math.pow(1.0d + FIXING_RATES[i + 1], af);
    }
    double rateExpected = (investmentFactorKnown - 1.0d) / afKnown;
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertThat(rateExpected).isCloseTo(rateComputed, offset(TOLERANCE_RATE));
    }
  }

  @Test
  public void test_rateAllFixedSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 16), date(2015, 1, 17)};
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 7;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getFixings()).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_BRL_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertThat(sensitivityComputed).isEqualTo(PointSensitivityBuilder.none());
    }
  }

  @Test
  public void test_rateAndSensitivityMissingFixing() {
    LocalDate valuationDate = date(2015, 1, 13);
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(BRL_CDI);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(valuationDate, mockRates);
    when(mockRates.getValuationDate()).thenReturn(valuationDate);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 2;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getFixings()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(BRL_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < BRL_OBS.length; i++) {
      when(mockRates.rate(BRL_OBS[i])).thenReturn(FORWARD_RATES[i]);
    }
    assertThatExceptionOfType(PricingException.class)
        .isThrownBy(() -> OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv));
    assertThatExceptionOfType(PricingException.class)
        .isThrownBy(() -> OBS_BRL_FWD_ONCMP.rateSensitivity(
            ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv));
  }

  //-------------------------------------------------------------------------
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final LocalDateDoubleTimeSeries TIME_SERIES;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < FIXING_DATES.length; i++) {
      builder.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    TIME_SERIES = builder.build();
  }

  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  @Test
  public void test_rateParameterSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    DoubleArray timeUsd = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
    DoubleArray rateUsd = DoubleArray.of(0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135);
    OvernightCompoundedAnnualRateComputation ro =
        OvernightCompoundedAnnualRateComputation.of(BRL_CDI, FIXING_START_DATE, FIXING_END_DATE, REF_DATA);

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      Curve brlCDICurve = InterpolatedNodalCurve.of(
          Curves.zeroRates("BRL-CDI", DayCount.of("BUS/252 BRBD")), timeUsd, rateUsd, INTERPOLATOR);
      ImmutableRatesProvider prov = ImmutableRatesProvider.builder(valuationDate[loopvaldate])
          .overnightIndexCurve(BRL_CDI, brlCDICurve, TIME_SERIES)
          .build();
      PointSensitivityBuilder sensitivityBuilderComputed =
          OBS_BRL_FWD_ONCMP.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
      CurrencyParameterSensitivities parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());
      CurrencyParameterSensitivities parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(BRL_CDI.getCurrency(),
              OBS_BRL_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertThat(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0)).isTrue();
    }
  }

}
