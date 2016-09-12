/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.ignoreThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;
import com.opengamma.strata.product.rate.IborInterpolatedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Test.
 */
@SuppressWarnings("unchecked")
@Test
public class DispatchingRateComputationFnTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);

  private static final YearMonth ACCRUAL_START_MONTH = YearMonth.of(2014, 7);
  private static final YearMonth ACCRUAL_END_MONTH = YearMonth.of(2015, 7);

  private static final RatesProvider MOCK_PROV = new MockRatesProvider();
  private static final RateComputationFn<IborRateComputation> MOCK_IBOR_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<IborInterpolatedRateComputation> MOCK_IBOR_INT_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<IborAveragedRateComputation> MOCK_IBOR_AVE_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<OvernightCompoundedRateComputation> MOCK_ON_CPD_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<OvernightAveragedRateComputation> MOCK_ON_AVE_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<InflationMonthlyRateComputation> MOCK_INF_MON_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<InflationInterpolatedRateComputation> MOCK_INF_INT_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<InflationEndMonthRateComputation> MOCK_INF_BOND_MON_EMPTY =
      mock(RateComputationFn.class);
  private static final RateComputationFn<InflationEndInterpolatedRateComputation> MOCK_INF_BOND_INT_EMPTY =
      mock(RateComputationFn.class);

  private static final double TOLERANCE_RATE = 1.0E-10;

  public void test_rate_FixedRateComputation() {
    FixedRateComputation ro = FixedRateComputation.of(0.0123d);
    DispatchingRateComputationFn test = DispatchingRateComputationFn.DEFAULT;
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), 0.0123d, 0d);
  }

  public void test_rate_IborRateComputation() {
    RateComputationFn<IborRateComputation> mockIbor = mock(RateComputationFn.class);
    IborRateComputation ro = IborRateComputation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    when(mockIbor.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(0.0123d);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        mockIbor,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), 0.0123d, 0d);
  }

  public void test_rate_IborInterpolatedRateComputation() {
    double mockRate = 0.0123d;
    RateComputationFn<IborInterpolatedRateComputation> mockIborInt = mock(RateComputationFn.class);
    IborInterpolatedRateComputation ro =
        IborInterpolatedRateComputation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE, REF_DATA);
    when(mockIborInt.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        mockIborInt,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, 0d);
  }

  public void test_rate_IborAverageRateComputation() {
    double mockRate = 0.0123d;
    RateComputationFn<IborAveragedRateComputation> mockIborAve = mock(RateComputationFn.class);
    LocalDate[] fixingDates = new LocalDate[] {
        date(2014, 6, 30), date(2014, 7, 7), date(2014, 7, 14), date(2014, 7, 21)};
    double[] weights = {0.10d, 0.20d, 0.30d, 0.40d};
    List<IborAveragedFixing> fixings = new ArrayList<>();
    for (int i = 0; i < fixingDates.length; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .observation(IborIndexObservation.of(GBP_LIBOR_3M, fixingDates[i], REF_DATA))
          .weight(weights[i])
          .build();
      fixings.add(fixing);
    }
    IborAveragedRateComputation ro = IborAveragedRateComputation.of(fixings);
    when(mockIborAve.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        mockIborAve,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, 0d);
  }

  public void test_rate_OvernightCompoundedRateComputation() {
    double mockRate = 0.0123d;
    RateComputationFn<OvernightCompoundedRateComputation> mockOnCpd = mock(RateComputationFn.class);
    OvernightCompoundedRateComputation ro =
        OvernightCompoundedRateComputation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0, REF_DATA);
    when(mockOnCpd.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        mockOnCpd,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_OvernightAveragedRateComputation() {
    double mockRate = 0.0123d;
    RateComputationFn<OvernightAveragedRateComputation> mockOnAve = mock(RateComputationFn.class);
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0, REF_DATA);
    when(mockOnAve.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        mockOnAve,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_InflationMonthlyRateComputation() {
    double mockRate = 223.0d;
    RateComputationFn<InflationMonthlyRateComputation> mockInfMon = mock(RateComputationFn.class);
    InflationMonthlyRateComputation ro =
        InflationMonthlyRateComputation.of(US_CPI_U, ACCRUAL_START_MONTH, ACCRUAL_END_MONTH);
    when(mockInfMon.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        mockInfMon,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_InflationInterpolatedRateComputation() {
    double mockRate = 223.0d;
    RateComputationFn<InflationInterpolatedRateComputation> mockInfInt = mock(RateComputationFn.class);
    InflationInterpolatedRateComputation ro =
        InflationInterpolatedRateComputation.of(US_CPI_U, ACCRUAL_START_MONTH, ACCRUAL_END_MONTH, 0.3);
    when(mockInfInt.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        mockInfInt,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_InflationEndMonthRateComputation() {
    double mockRate = 223.0d;
    RateComputationFn<InflationEndMonthRateComputation> mockInfMon = mock(RateComputationFn.class);
    InflationEndMonthRateComputation ro =
        InflationEndMonthRateComputation.of(US_CPI_U, 123d, ACCRUAL_END_MONTH);
    when(mockInfMon.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        mockInfMon,
        MOCK_INF_BOND_INT_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_InflationEndInterpolatedRateComputation() {
    double mockRate = 223.0d;
    RateComputationFn<InflationEndInterpolatedRateComputation> mockInfInt = mock(RateComputationFn.class);
    InflationEndInterpolatedRateComputation ro =
        InflationEndInterpolatedRateComputation.of(US_CPI_U, 234d, ACCRUAL_END_MONTH, 0.3);
    when(mockInfInt.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        mockInfInt);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_unknownType() {
    RateComputation mockComputation = mock(RateComputation.class);
    DispatchingRateComputationFn test = DispatchingRateComputationFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.rate(mockComputation, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_explainRate_FixedRateComputation() {
    FixedRateComputation ro = FixedRateComputation.of(0.0123d);
    DispatchingRateComputationFn test = DispatchingRateComputationFn.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    assertEquals(test.explainRate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, builder), 0.0123d, 0d);
    ExplainMap built = builder.build();
    assertEquals(built.get(ExplainKey.FIXED_RATE), Optional.of(0.0123d));
    assertEquals(built.get(ExplainKey.COMBINED_RATE), Optional.of(0.0123d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DispatchingRateComputationFn test = new DispatchingRateComputationFn(
        MOCK_IBOR_EMPTY,
        MOCK_IBOR_INT_EMPTY,
        MOCK_IBOR_AVE_EMPTY,
        MOCK_ON_CPD_EMPTY,
        MOCK_ON_AVE_EMPTY,
        MOCK_INF_MON_EMPTY,
        MOCK_INF_INT_EMPTY,
        MOCK_INF_BOND_MON_EMPTY,
        MOCK_INF_BOND_INT_EMPTY);
    FixedRateComputation fixed = FixedRateComputation.of(0.0123d);
    IborRateComputation ibor = IborRateComputation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    IborInterpolatedRateComputation iborInt =
        IborInterpolatedRateComputation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE, REF_DATA);
    IborAveragedRateComputation iborAvg =
        IborAveragedRateComputation.of(ImmutableList.of(IborAveragedFixing.of(ibor.getObservation())));
    OvernightCompoundedRateComputation onCpd =
        OvernightCompoundedRateComputation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0, REF_DATA);
    OvernightAveragedRateComputation onAvg =
        OvernightAveragedRateComputation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0, REF_DATA);
    InflationMonthlyRateComputation inflationMonthly =
        InflationMonthlyRateComputation.of(US_CPI_U, ACCRUAL_START_MONTH, ACCRUAL_END_MONTH);
    InflationInterpolatedRateComputation inflationInterp =
        InflationInterpolatedRateComputation.of(US_CPI_U, ACCRUAL_START_MONTH, ACCRUAL_END_MONTH, 0.3);
    InflationEndMonthRateComputation inflationEndMonth =
        InflationEndMonthRateComputation.of(US_CPI_U, 234d, ACCRUAL_END_MONTH);
    InflationEndInterpolatedRateComputation inflationEndInterp =
        InflationEndInterpolatedRateComputation.of(US_CPI_U, 1234d, ACCRUAL_END_MONTH, 0.3);

    RateComputation mock = mock(RateComputation.class);
    ignoreThrows(() -> test.rateSensitivity(fixed, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(ibor, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(iborInt, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(iborAvg, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(onCpd, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(onAvg, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(inflationMonthly, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(inflationInterp, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(inflationEndMonth, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(inflationEndInterp, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(mock, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));

    ExplainMapBuilder explain = ExplainMap.builder();
    ignoreThrows(() -> test.explainRate(fixed, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(ibor, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(iborInt, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(iborAvg, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(onCpd, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(onAvg, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(inflationMonthly, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(inflationInterp, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(inflationEndMonth, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(inflationEndInterp, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainRate(mock, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV, explain));
  }

}
