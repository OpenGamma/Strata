/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CombinedCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivities;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivity;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link CurveGammaCalculator} cross-gamma.
 */
public class CurveGammaCalculatorCrossGammaTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double EPS = 1.0e-6;
  private static final double TOL = 1.0e-14;
  private static final CurveGammaCalculator FORWARD =
      CurveGammaCalculator.ofForwardDifference(EPS * 0.1);
  private static final CurveGammaCalculator CENTRAL =
      CurveGammaCalculator.ofCentralDifference(EPS);
  private static final CurveGammaCalculator BACKWARD =
      CurveGammaCalculator.ofBackwardDifference(EPS * 0.1);

  @Test
  public void sensitivity_single_curve() {
    CrossGammaParameterSensitivities forward =
        FORWARD.calculateCrossGammaIntraCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    CrossGammaParameterSensitivities central =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    CrossGammaParameterSensitivities backward =
        BACKWARD.calculateCrossGammaIntraCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    DoubleArray times = RatesProviderDataSets.TIMES_1;
    for (CrossGammaParameterSensitivities sensi : new CrossGammaParameterSensitivities[] {forward, central, backward}) {
      CurrencyParameterSensitivities diagonalComputed = sensi.diagonal();
      assertThat(sensi.size()).isEqualTo(1);
      assertThat(diagonalComputed.size()).isEqualTo(1);
      DoubleMatrix s = sensi.getSensitivities().get(0).getSensitivity();
      assertThat(s.columnCount()).isEqualTo(times.size());
      for (int i = 0; i < times.size(); i++) {
        for (int j = 0; j < times.size(); j++) {
          double expected = 32d * times.get(i) * times.get(j);
          assertThat(s.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
        }
      }
    }
    // no difference for single curve
    CrossGammaParameterSensitivities forwardCross =
        FORWARD.calculateCrossGammaCrossCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    assertThat(forward.equalWithTolerance(forwardCross, TOL)).isTrue();
    CrossGammaParameterSensitivities centralCross =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    assertThat(central.equalWithTolerance(centralCross, TOL)).isTrue();
    CrossGammaParameterSensitivities backwardCross =
        BACKWARD.calculateCrossGammaCrossCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    assertThat(backward.equalWithTolerance(backwardCross, TOL)).isTrue();
  }

  @Test
  public void sensitivity_intra_multi_curve() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiFn);
    DoubleArray times1 = RatesProviderDataSets.TIMES_1;
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    DoubleArray times4 = RatesProviderDataSets.TIMES_4;
    assertThat(sensiComputed.size()).isEqualTo(4);
    DoubleMatrix s1 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    assertThat(s1.columnCount()).isEqualTo(times1.size());
    for (int i = 0; i < times1.size(); i++) {
      for (int j = 0; j < times1.size(); j++) {
        double expected = 8d * times1.get(i) * times1.get(j);
        assertThat(s1.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(times2.size());
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < times2.size(); j++) {
        double expected = 2d * times2.get(i) * times2.get(j);
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(times3.size());
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < times3.size(); j++) {
        double expected = 2d * times3.get(i) * times3.get(j);
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    DoubleMatrix s4 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    assertThat(s4.columnCount()).isEqualTo(times4.size());
    for (int i = 0; i < times4.size(); i++) {
      for (int j = 0; j < times4.size(); j++) {
        double expected = 2d * times4.get(i) * times4.get(j);
        assertThat(s4.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
  }

  @Test
  public void sensitivity_multi_curve_empty() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiModFn);
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    assertThat(sensiComputed.size()).isEqualTo(2);
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(times2.size());
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < times2.size(); j++) {
        double expected = 2d * times2.get(i) * times2.get(j);
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(times3.size());
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < times3.size(); j++) {
        double expected = 2d * times3.get(i) * times3.get(j);
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    Optional<CrossGammaParameterSensitivity> oisSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD);
    assertThat(oisSensi.isPresent()).isFalse();
    Optional<CrossGammaParameterSensitivity> priceIndexSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD);
    assertThat(priceIndexSensi.isPresent()).isFalse();
  }

  @Test
  public void sensitivity_cross_multi_curve() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiFn);
    DoubleArray times1 = RatesProviderDataSets.TIMES_1;
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    DoubleArray times4 = RatesProviderDataSets.TIMES_4;
    int paramsTotal = times1.size() + times2.size() + times3.size() + times4.size();
    double[] timesTotal = new double[paramsTotal];
    DoubleArray times1Twice = times1.multipliedBy(2d);
    System.arraycopy(times4.toArray(), 0, timesTotal, 0, times4.size());
    System.arraycopy(times1Twice.toArray(), 0, timesTotal, times4.size(), times1.size());
    System.arraycopy(times2.toArray(), 0, timesTotal, times1.size() + times4.size(), times2.size());
    System.arraycopy(times3.toArray(), 0, timesTotal, times1.size() + times2.size() + times4.size(), times3.size());

    assertThat(sensiComputed.size()).isEqualTo(4);
    DoubleMatrix s1 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    assertThat(s1.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times1.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 4d * times1.get(i) * timesTotal[j];
        assertThat(s1.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times2.get(i) * timesTotal[j];
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times3.get(i) * timesTotal[j];
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s4 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    assertThat(s4.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times4.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times4.get(i) * timesTotal[j];
        assertThat(s4.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 20d));
      }
    }
  }

  @Test
  public void sensitivity_cross_multi_curve_empty() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiModFn);
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    int paramsTotal = times2.size() + times3.size();
    double[] timesTotal = new double[paramsTotal];
    System.arraycopy(times2.toArray(), 0, timesTotal, 0, times2.size());
    System.arraycopy(times3.toArray(), 0, timesTotal, times2.size(), times3.size());
    assertThat(sensiComputed.size()).isEqualTo(2);
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times2.get(i) * timesTotal[j];
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times3.get(i) * timesTotal[j];
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    Optional<CrossGammaParameterSensitivity> oisSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD);
    assertThat(oisSensi.isPresent()).isFalse();
    Optional<CrossGammaParameterSensitivity> priceIndexSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD);
    assertThat(priceIndexSensi.isPresent()).isFalse();
  }

  // test diagonal part against finite difference approximation computed from pv
  @Test
  public void swap_exampleTest() {
    LocalDate start = LocalDate.of(2014, 3, 10);
    LocalDate end = LocalDate.of(2021, 3, 10);
    double notional = 1.0e6;
    ResolvedSwap swap = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
        .toTrade(RatesProviderDataSets.VAL_DATE_2014_01_22, start, end, BuySell.BUY, notional, 0.005)
        .getProduct()
        .resolve(REF_DATA);
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    Function<ImmutableRatesProvider, CurrencyAmount> pvFunction = p -> pricer.presentValue(swap, USD, p);
    Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensiFunction = p -> {
      PointSensitivities sensi = pricer.presentValueSensitivity(swap, p).build();
      return p.parameterSensitivity(sensi);
    };
    CurrencyParameterSensitivities expected = sensitivityDiagonal(RatesProviderDataSets.MULTI_CPI_USD, pvFunction);
    CurrencyParameterSensitivities computed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_CPI_USD, sensiFunction).diagonal();
    assertThat(computed.equalWithTolerance(expected, Math.sqrt(EPS) * notional)).isTrue();
    CurrencyParameterSensitivities computedFromCross =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD, sensiFunction).diagonal();
    assertThat(computed.equalWithTolerance(computedFromCross, TOL)).isTrue();
  }

  @Test
  public void sensitivity_multi_combined_curve() {
    CrossGammaParameterSensitivities sensiCrossComputed =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD_COMBINED, this::sensiCombinedFn);
    DoubleArray times1 = RatesProviderDataSets.TIMES_1; // ois
    DoubleArray times2 = RatesProviderDataSets.TIMES_2; // l3
    DoubleArray times3 = RatesProviderDataSets.TIMES_3; // l6
    DoubleArray times4 = RatesProviderDataSets.TIMES_4; // cpi
    int paramsTotal = times1.size() + times2.size() + times3.size() + times4.size();
    double[] timesTotal = new double[paramsTotal];
    DoubleArray times1Twice = times1.multipliedBy(2d);
    System.arraycopy(times4.toArray(), 0, timesTotal, 0, times4.size());
    System.arraycopy(times1Twice.toArray(), 0, timesTotal, times4.size(), times1.size());
    System.arraycopy(times2.toArray(), 0, timesTotal, times1.size() + times4.size(), times2.size());
    System.arraycopy(times3.toArray(), 0, timesTotal, times1.size() + times2.size() + times4.size(), times3.size());

    assertThat(sensiCrossComputed.size()).isEqualTo(4);
    DoubleMatrix s1 = sensiCrossComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    assertThat(s1.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times1.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 4d * times1.get(i) * timesTotal[j];
        assertThat(s1.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s2 = sensiCrossComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 8d * times2.get(i) * timesTotal[j];
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s3 = sensiCrossComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times3.get(i) * timesTotal[j];
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s4 = sensiCrossComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    assertThat(s4.columnCount()).isEqualTo(paramsTotal);
    for (int i = 0; i < times4.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times4.get(i) * timesTotal[j];
        assertThat(s4.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 20d));
      }
    }

    CrossGammaParameterSensitivities sensiIntraComputed = CENTRAL.calculateCrossGammaIntraCurve(
        RatesProviderDataSets.MULTI_CPI_USD_COMBINED, this::sensiCombinedFn);
    DoubleMatrix s1Intra = sensiIntraComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    DoubleMatrix s2Intra = sensiIntraComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    DoubleMatrix s3Intra = sensiIntraComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    DoubleMatrix s4Intra = sensiIntraComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    int offsetOis = times4.size();
    for (int i = 0; i < times1.size(); i++) {
      for (int j = 0; j < times1.size(); j++) {
        assertThat(s1Intra.get(i, j)).isCloseTo(s1.get(i, offsetOis + j), offset(TOL));
      }
    }
    int offset3m = times4.size() + times1.size();
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < times2.size(); j++) {
        assertThat(s2Intra.get(i, j)).isCloseTo(s2.get(i, offset3m + j), offset(TOL));
      }
    }
    int offset6m = times4.size() + times1.size() + times2.size();
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < times3.size(); j++) {
        assertThat(s3Intra.get(i, j)).isCloseTo(s3.get(i, offset6m + j), offset(TOL));
      }
    }
    for (int i = 0; i < times4.size(); i++) {
      for (int j = 0; j < times4.size(); j++) {
        assertThat(s4Intra.get(i, j)).isCloseTo(s4.get(i, j), offset(TOL));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void sensitivity_intra_multi_bond_curve() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_BOND, this::sensiFnBond);
    DoubleArray timesUsRepo = RatesProviderDataSets.TIMES_1;
    DoubleArray timesUsIssuer1 = RatesProviderDataSets.TIMES_3;
    DoubleArray timesUsIssuer2 = RatesProviderDataSets.TIMES_2;
    assertThat(sensiComputed.size()).isEqualTo(3);
    DoubleMatrix s1 = sensiComputed.getSensitivity(RatesProviderDataSets.US_REPO_CURVE_NAME, USD).getSensitivity();
    assertThat(s1.columnCount()).isEqualTo(timesUsRepo.size());
    for (int i = 0; i < timesUsRepo.size(); i++) {
      for (int j = 0; j < timesUsRepo.size(); j++) {
        double expected = 2d * timesUsRepo.get(i) * timesUsRepo.get(j);
        assertThat(s1.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.US_ISSUER_CURVE_1_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(timesUsIssuer1.size());
    for (int i = 0; i < timesUsIssuer1.size(); i++) {
      for (int j = 0; j < timesUsIssuer1.size(); j++) {
        double expected = 2d * timesUsIssuer1.get(i) * timesUsIssuer1.get(j);
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.US_ISSUER_CURVE_2_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(timesUsIssuer2.size());
    for (int i = 0; i < timesUsIssuer2.size(); i++) {
      for (int j = 0; j < timesUsIssuer2.size(); j++) {
        double expected = 2d * timesUsIssuer2.get(i) * timesUsIssuer2.get(j);
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS));
      }
    }
  }

  @Test
  public void sensitivity_multi_combined_bond_curve() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_BOND_COMBINED, this::sensiCombinedFnBond);
    DoubleArray timesUsL3 = RatesProviderDataSets.TIMES_2;
    DoubleArray timesUsRepo = RatesProviderDataSets.TIMES_1;
    DoubleArray timesUsIssuer1 = RatesProviderDataSets.TIMES_3;
    DoubleArray timesUsIssuer2 = RatesProviderDataSets.TIMES_2;
    assertThat(sensiComputed.size()).isEqualTo(4);
    DoubleMatrix s1 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertThat(s1.columnCount()).isEqualTo(timesUsL3.size());
    for (int i = 0; i < timesUsL3.size(); i++) {
      for (int j = 0; j < timesUsL3.size(); j++) {
        double expected = 2d * timesUsL3.get(i) * timesUsL3.get(j) * 3d * 3d;
        assertThat(s1.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.US_REPO_CURVE_NAME, USD).getSensitivity();
    assertThat(s2.columnCount()).isEqualTo(timesUsRepo.size());
    for (int i = 0; i < timesUsRepo.size(); i++) {
      for (int j = 0; j < timesUsRepo.size(); j++) {
        double expected = 2d * timesUsRepo.get(i) * timesUsRepo.get(j);
        assertThat(s2.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.US_ISSUER_CURVE_1_NAME, USD).getSensitivity();
    assertThat(s3.columnCount()).isEqualTo(timesUsIssuer1.size());
    for (int i = 0; i < timesUsIssuer1.size(); i++) {
      for (int j = 0; j < timesUsIssuer1.size(); j++) {
        double expected = 2d * timesUsIssuer1.get(i) * timesUsIssuer1.get(j);
        assertThat(s3.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 10d));
      }
    }
    DoubleMatrix s4 = sensiComputed.getSensitivity(RatesProviderDataSets.US_ISSUER_CURVE_2_NAME, USD).getSensitivity();
    assertThat(s4.columnCount()).isEqualTo(timesUsIssuer2.size());
    for (int i = 0; i < timesUsIssuer2.size(); i++) {
      for (int j = 0; j < timesUsIssuer2.size(); j++) {
        double expected = 2d * timesUsIssuer2.get(i) * timesUsIssuer2.get(j);
        assertThat(s4.get(i, j)).isCloseTo(expected, offset(Math.max(Math.abs(expected), 1d) * EPS * 20d));
      }
    }
  }

  //-------------------------------------------------------------------------
  private CurrencyParameterSensitivities sensiFn(ImmutableRatesProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      double sumSqrt = sum(provider);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
          DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      double sumSqrt = sum(provider);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
          DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
    }
    return sensi;
  }

  // modified sensitivity function - CombinedCurve involved
  private CurrencyParameterSensitivities sensiCombinedFn(ImmutableRatesProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    double sum = sumCombine(provider);
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      CombinedCurve curveComb = (CombinedCurve) entry.getValue();
      InterpolatedNodalCurve baseCurveInt = checkInterpolated(curveComb.getBaseCurve());
      InterpolatedNodalCurve spreadCurveInt = checkInterpolated(curveComb.getSpreadCurve());
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(baseCurveInt.getName(), USD,
          DoubleArray.of(baseCurveInt.getParameterCount(), i -> 2d * sum * baseCurveInt.getXValues().get(i))));
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(spreadCurveInt.getName(), USD,
          DoubleArray.of(spreadCurveInt.getParameterCount(), i -> 2d * sum * spreadCurveInt.getXValues().get(i))));
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      if (entry.getValue() instanceof CombinedCurve) {
        CombinedCurve curveComb = (CombinedCurve) entry.getValue();
        InterpolatedNodalCurve baseCurveInt = checkInterpolated(curveComb.getBaseCurve());
        InterpolatedNodalCurve spreadCurveInt = checkInterpolated(curveComb.getSpreadCurve());
        sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(baseCurveInt.getName(), USD,
            DoubleArray.of(baseCurveInt.getParameterCount(), i -> 2d * sum * baseCurveInt.getXValues().get(i))));
        sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(spreadCurveInt.getName(), USD,
            DoubleArray.of(spreadCurveInt.getParameterCount(), i -> 2d * sum * spreadCurveInt.getXValues().get(i))));
      } else {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
            DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sum * curveInt.getXValues().get(i))));
      }
    }
    return sensi;
  }

  // modified sensitivity function - sensitivities are computed only for ibor index curves
  private CurrencyParameterSensitivities sensiModFn(ImmutableRatesProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      if (entry.getKey() instanceof IborIndex) {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        double sumSqrt = sumMod(provider);
        sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
            DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
      }
    }
    return sensi;
  }

  private double sum(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumSingle(curveInt);
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumSingle(curveInt);
    }
    return result;
  }

  private double sumCombine(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      if (entry.getValue() instanceof CombinedCurve) {
        InterpolatedNodalCurve baseCurveInt = checkInterpolated(entry.getValue().split().get(0));
        InterpolatedNodalCurve spreadCurveInt = checkInterpolated(entry.getValue().split().get(1));
        result += 0.25d * sumSingle(baseCurveInt);
        result += sumSingle(spreadCurveInt);
      } else {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        result += sumSingle(curveInt);
      }
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      if (entry.getValue() instanceof CombinedCurve) {
        InterpolatedNodalCurve baseCurveInt = checkInterpolated(entry.getValue().split().get(0));
        InterpolatedNodalCurve spreadCurveInt = checkInterpolated(entry.getValue().split().get(1));
        result += 0.25d * sumSingle(baseCurveInt);
        result += sumSingle(spreadCurveInt);
      } else {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        result += entry.getKey().equals(IborIndices.USD_LIBOR_3M) ? 0.25d * sumSingle(curveInt) : sumSingle(curveInt);
      }
    }
    return result;
  }

  private double sumMod(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      if (entry.getKey() instanceof IborIndex) {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        result += sumSingle(curveInt);
      }
    }
    return result;
  }

  // check that the curve is InterpolatedNodalCurve
  private InterpolatedNodalCurve checkInterpolated(Curve curve) {
    ArgChecker.isTrue(curve instanceof InterpolatedNodalCurve, "Curve should be a InterpolatedNodalCurve");
    return (InterpolatedNodalCurve) curve;
  }

  //-------------------------------------------------------------------------
  // computes diagonal part
  private CurrencyParameterSensitivities sensitivityDiagonal(
      RatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    ImmutableRatesProvider immProv = provider.toImmutableRatesProvider();
    CurrencyAmount valueInit = valueFn.apply(immProv);
    CurrencyParameterSensitivities discounting = sensitivity(
        immProv,
        immProv.getDiscountCurves(),
        (base, bumped) -> base.toBuilder().discountCurves(bumped).build(),
        valueFn,
        valueInit);
    CurrencyParameterSensitivities forward = sensitivity(
        immProv,
        immProv.getIndexCurves(),
        (base, bumped) -> base.toBuilder().indexCurves(bumped).build(),
        valueFn,
        valueInit);
    return discounting.combinedWith(forward);
  }

  // computes the sensitivity with respect to the curves
  private <T> CurrencyParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Map<T, Curve> baseCurves,
      BiFunction<ImmutableRatesProvider, Map<T, Curve>, ImmutableRatesProvider> storeBumpedFn,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (Entry<T, Curve> entry : baseCurves.entrySet()) {
      Curve curve = entry.getValue();
      DoubleArray sensitivity = DoubleArray.of(curve.getParameterCount(), i -> {
        Curve dscUp = curve.withParameter(i, curve.getParameter(i) + EPS);
        Curve dscDw = curve.withParameter(i, curve.getParameter(i) - EPS);
        HashMap<T, Curve> mapUp = new HashMap<>(baseCurves);
        HashMap<T, Curve> mapDw = new HashMap<>(baseCurves);
        mapUp.put(entry.getKey(), dscUp);
        mapDw.put(entry.getKey(), dscDw);
        ImmutableRatesProvider providerUp = storeBumpedFn.apply(provider, mapUp);
        ImmutableRatesProvider providerDw = storeBumpedFn.apply(provider, mapDw);
        return (valueFn.apply(providerUp).getAmount() + valueFn.apply(providerDw).getAmount() - 2d * valueInit.getAmount()) /
            EPS / EPS;
      });
      result = result.combinedWith(curve.createParameterSensitivity(valueInit.getCurrency(), sensitivity));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  private CurrencyParameterSensitivities sensiFnBond(ImmutableLegalEntityDiscountingProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    double sum = sum(provider);
    // repo curves
    ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> mapRepoCurves = provider.getRepoCurves();
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : mapRepoCurves.entrySet()) {
      DiscountFactors discountFactors = entry.getValue();
      InterpolatedNodalCurve curve = (InterpolatedNodalCurve) getCurve(discountFactors);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curve.getName(), discountFactors.getCurrency(),
          DoubleArray.of(discountFactors.getParameterCount(), i -> 2d * curve.getXValues().get(i) * sum)));
    }
    // issuer curves
    ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> mapIssuerCurves = provider.getIssuerCurves();
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : mapIssuerCurves.entrySet()) {
      DiscountFactors discountFactors = entry.getValue();
      InterpolatedNodalCurve curve = (InterpolatedNodalCurve) getCurve(discountFactors);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curve.getName(), discountFactors.getCurrency(),
          DoubleArray.of(discountFactors.getParameterCount(), i -> 2d * curve.getXValues().get(i) * sum)));
    }
    return sensi;
  }

  // modified sensitivity function - CombinedCurve involved
  private CurrencyParameterSensitivities sensiCombinedFnBond(ImmutableLegalEntityDiscountingProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    double sum = sumCombine(provider);
    // repo curves
    ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> mapCurrency = provider.getRepoCurves();
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : mapCurrency.entrySet()) {
      CombinedCurve curveComb = (CombinedCurve) getCurve(entry.getValue());
      InterpolatedNodalCurve baseCurveInt = checkInterpolated(curveComb.getBaseCurve());
      InterpolatedNodalCurve spreadCurveInt = checkInterpolated(curveComb.getSpreadCurve());
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(baseCurveInt.getName(), USD,
          DoubleArray.of(baseCurveInt.getParameterCount(), i -> 2d * sum * baseCurveInt.getXValues().get(i))));
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(spreadCurveInt.getName(), USD,
          DoubleArray.of(spreadCurveInt.getParameterCount(), i -> 2d * sum * spreadCurveInt.getXValues().get(i))));
    }
    // issuer curves
    ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> mapIndex = provider.getIssuerCurves();
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : mapIndex.entrySet()) {
      CombinedCurve curveComb = (CombinedCurve) getCurve(entry.getValue());
      InterpolatedNodalCurve baseCurveInt = checkInterpolated(curveComb.getBaseCurve());
      InterpolatedNodalCurve spreadCurveInt = checkInterpolated(curveComb.getSpreadCurve());
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(baseCurveInt.getName(), USD,
          DoubleArray.of(baseCurveInt.getParameterCount(), i -> 2d * sum * baseCurveInt.getXValues().get(i))));
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(spreadCurveInt.getName(), USD,
          DoubleArray.of(spreadCurveInt.getParameterCount(), i -> 2d * sum * spreadCurveInt.getXValues().get(i))));
    }
    return sensi;
  }

  private double sumCombine(ImmutableLegalEntityDiscountingProvider provider) {
    double result = 0d;
    // repo curves
    ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> mapCurrency = provider.getRepoCurves();
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : mapCurrency.entrySet()) {
      CombinedCurve curve = (CombinedCurve) getCurve(entry.getValue());
      result += sumSingle((InterpolatedNodalCurve) curve.split().get(0));
      result += sumSingle((InterpolatedNodalCurve) curve.split().get(1));
    }
    // issuer curves
    ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> mapIndex = provider.getIssuerCurves();
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : mapIndex.entrySet()) {
      CombinedCurve curve = (CombinedCurve) getCurve(entry.getValue());
      result += sumSingle((InterpolatedNodalCurve) curve.split().get(0));
      result += sumSingle((InterpolatedNodalCurve) curve.split().get(1));
    }
    return result;
  }

  private double sum(ImmutableLegalEntityDiscountingProvider provider) {
    double result = 0d;
    // repo curves
    ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> mapIndex = provider.getRepoCurves();
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curve = (InterpolatedNodalCurve) getCurve(entry.getValue());
      result += sumSingle(curve);
    }
    // issuer curves
    ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> mapCurrency = provider.getIssuerCurves();
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curve = (InterpolatedNodalCurve) getCurve(entry.getValue());
      result += sumSingle(curve);
    }
    return result;
  }

  private double sumSingle(InterpolatedNodalCurve interpolatedNodalCurve) {
    double result = 0.0;
    int nbNodePoints = interpolatedNodalCurve.getParameterCount();
    for (int i = 0; i < nbNodePoints; i++) {
      result += interpolatedNodalCurve.getXValues().get(i) * interpolatedNodalCurve.getYValues().get(i);
    }
    return result;
  }

  private Curve getCurve(DiscountFactors discountFactors) {
    return ((ZeroRateDiscountFactors) discountFactors).getCurve();
  }

}
