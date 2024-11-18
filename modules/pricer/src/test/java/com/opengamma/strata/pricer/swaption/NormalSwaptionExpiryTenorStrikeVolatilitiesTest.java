/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Triple;
import com.opengamma.strata.market.cube.CubeMetadata;
import com.opengamma.strata.market.cube.CubeName;
import com.opengamma.strata.market.cube.Cubes;
import com.opengamma.strata.market.cube.InterpolatedNodalCube;
import com.opengamma.strata.market.cube.interpolator.CubeInterpolator;
import com.opengamma.strata.market.cube.interpolator.GridCubeInterpolator;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorTenorStrikeParameterMetadata;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link NormalSwaptionExpiryTenorStrikeVolatilities}.
 */
public class NormalSwaptionExpiryTenorStrikeVolatilitiesTest {

  private static final CubeInterpolator INTERPOLATOR_3D = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);
  private static final DoubleArray TIME = DoubleArray.of(
      0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25,
      0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
  private static final DoubleArray TENOR = DoubleArray.of(
      3, 3, 3, 3, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10, 10, 10,
      3, 3, 3, 3, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10, 10, 10,
      3, 3, 3, 3, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10, 10, 10);
  private static final DoubleArray STRIKE = DoubleArray.of(
      0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04,
      0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04,
      0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04, 0.025, 0.03, 0.035, 0.04);
  private static final DoubleArray VOL = DoubleArray.of(
      0.018, 0.015, 0.013, 0.012, 0.017, 0.014, 0.013, 0.012, 0.016, 0.013, 0.011, 0.01, 0.015, 0.012, 0.01, 0.009,
      0.015, 0.012, 0.011, 0.01, 0.016, 0.013, 0.012, 0.011, 0.015, 0.012, 0.011, 0.01, 0.014, 0.011, 0.01, 0.009,
      0.013, 0.01, 0.009, 0.008, 0.014, 0.012, 0.011, 0.01, 0.013, 0.011, 0.01, 0.009, 0.012, 0.01, 0.009, 0.007);

  private static final FixedIborSwapConvention CONVENTION = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
  private static final CubeMetadata METADATA;
  private static final ImmutableList<Tenor> EXPIRY_TENOR;

  static {
    ImmutableList.Builder<TenorTenorStrikeParameterMetadata> paramList = ImmutableList.builder();
    ImmutableList.Builder<Tenor> expiryTenorList = ImmutableList.builder();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      Tenor expiryTenor = TIME.get(i) == 0.25 ? Tenor.TENOR_3M : TIME.get(i) == 0.5 ? Tenor.TENOR_6M : Tenor.TENOR_1Y;
      expiryTenorList.add(expiryTenor);
      TenorTenorStrikeParameterMetadata parameterMetadata =
          TenorTenorStrikeParameterMetadata.of(expiryTenor, Tenor.ofYears((int) TENOR.get(i)), STRIKE.get(i));
      paramList.add(parameterMetadata);
    }
    METADATA = Cubes.normalVolatilityByExpiryTenorStrike(
        "GOVT1-SWAPTION-VOL",
        ACT_365F).withParameterMetadata(paramList.build());
    EXPIRY_TENOR = expiryTenorList.build();
  }

  private static final InterpolatedNodalCube CUBE =
      InterpolatedNodalCube.of(METADATA, TIME, TENOR, STRIKE, VOL, INTERPOLATOR_3D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final NormalSwaptionExpiryTenorStrikeVolatilities VOLS =
      NormalSwaptionExpiryTenorStrikeVolatilities.of(CONVENTION, VAL_DATE_TIME, CUBE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[]{
      dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_TENOR = new double[]{2.0, 6.0, 7.0, 15.0};
  private static final double[] TEST_SENSITIVITY = new double[]{1.0, 1.0, 1.0, 1.0};
  private static final double[] TEST_STRIKE = new double[]{0.01, 0.027, 0.037, 0.05};
  private static final double TEST_FORWARD = 0.025; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  @Test
  public void test_valuationDate() {
    assertThat(VOLS.getValuationDateTime()).isEqualTo(VAL_DATE_TIME);
  }

  @Test
  public void test_swapConvention() {
    assertThat(VOLS.getConvention()).isEqualTo(CONVENTION);
  }

  @Test
  public void test_findData() {
    assertThat(VOLS.findData(CUBE.getName())).isEqualTo(Optional.of(CUBE));
    assertThat(VOLS.findData(CubeName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_tenor() {
    double test1 = VOLS.tenor(VAL_DATE, VAL_DATE);
    assertThat(test1).isEqualTo(0d);
    double test2 = VOLS.tenor(VAL_DATE, date(2018, 2, 28));
    assertThat(test2).isEqualTo(3d);
    double test3 = VOLS.tenor(VAL_DATE, date(2018, 2, 10));
    assertThat(test3).isEqualTo(3d);
  }

  @Test
  public void test_relativeTime() {
    double test1 = VOLS.relativeTime(VAL_DATE_TIME);
    assertThat(test1).isEqualTo(0d);
    double test2 = VOLS.relativeTime(date(2018, 2, 17).atStartOfDay(LONDON_ZONE));
    double test3 = VOLS.relativeTime(date(2012, 2, 17).atStartOfDay(LONDON_ZONE));
    assertThat(test2).isEqualTo(-test3); // consistency checked
  }

  @Test
  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      double volExpected = CUBE.wValue(expiryTime, TEST_TENOR[i], TEST_STRIKE[i]);
      double volComputed = VOLS.volatility(TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE[i], TEST_FORWARD);
      assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE_VOL));
    }
  }

  @Test
  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      SwaptionSensitivity point = SwaptionSensitivity.of(
          VOLS.getName(), expiryTime, TEST_TENOR[i], TEST_STRIKE[i], TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
      CurrencyParameterSensitivities sensActual = VOLS.parameterSensitivity(point);
      CurrencyParameterSensitivity sensi = sensActual.getSensitivity(CUBE.getName(), GBP);
      DoubleArray computed = sensi.getSensitivity();

      Map<Triple<Tenor, Tenor, Double>, Double> map = new HashMap<>();
      for (int j = 0; j < nData; ++j) {
        DoubleArray volDataUp = VOL.subArray(0, nData).with(j, VOL.get(j) + eps);
        DoubleArray volDataDw = VOL.subArray(0, nData).with(j, VOL.get(j) - eps);
        InterpolatedNodalCube paramUp =
            InterpolatedNodalCube.of(METADATA, TIME, TENOR, STRIKE, volDataUp, INTERPOLATOR_3D);
        InterpolatedNodalCube paramDw =
            InterpolatedNodalCube.of(METADATA, TIME, TENOR, STRIKE, volDataDw, INTERPOLATOR_3D);
        NormalSwaptionExpiryTenorStrikeVolatilities provUp =
            NormalSwaptionExpiryTenorStrikeVolatilities.of(CONVENTION, VAL_DATE_TIME, paramUp);
        NormalSwaptionExpiryTenorStrikeVolatilities provDw =
            NormalSwaptionExpiryTenorStrikeVolatilities.of(CONVENTION, VAL_DATE_TIME, paramDw);
        double volUp = provUp.volatility(TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE[i], TEST_FORWARD);
        double volDw = provDw.volatility(TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE[i], TEST_FORWARD);
        double fd = 0.5 * (volUp - volDw) / eps;
        map.put(Triple.of(EXPIRY_TENOR.get(j), Tenor.ofYears((int) TENOR.get(j)), STRIKE.get(j)), fd);
      }
      List<ParameterMetadata> list = sensi.getParameterMetadata();
      assertThat(computed.size()).isEqualTo(nData);
      for (int j = 0; j < list.size(); ++j) {
        TenorTenorStrikeParameterMetadata metadata =
            (TenorTenorStrikeParameterMetadata) list.get(i);
        double expected = map.get(Triple.of(
            metadata.getExpiryTenor(),
            metadata.getUnderlyingTenor(),
            metadata.getStrike()));
        assertThat(computed.get(i)).isCloseTo(expected, offset(eps));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    NormalSwaptionExpiryTenorStrikeVolatilities test1 =
        NormalSwaptionExpiryTenorStrikeVolatilities.of(CONVENTION, VAL_DATE_TIME, CUBE);
    coverImmutableBean(test1);
    NormalSwaptionExpiryTenorStrikeVolatilities test2 = NormalSwaptionExpiryTenorStrikeVolatilities.of(
        FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M,
        VAL_DATE.atStartOfDay(ZoneOffset.UTC),
        CUBE.withPerturbation((i, v, m) -> 2d * v));
    coverBeanEquals(test1, test2);
  }

}
