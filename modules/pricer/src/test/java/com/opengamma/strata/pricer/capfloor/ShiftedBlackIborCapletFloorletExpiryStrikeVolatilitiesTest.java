/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.TIME_SQUARE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities}.
 */
public class ShiftedBlackIborCapletFloorletExpiryStrikeVolatilitiesTest {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0);
  private static final DoubleArray STRIKE =
      DoubleArray.of(-0.005, 0.005, 0.01, 0.025, -0.005, 0.005, 0.01, 0.025, -0.005, 0.005, 0.01, 0.025);
  private static final DoubleArray VOL =
      DoubleArray.of(0.14, 0.14, 0.13, 0.12, 0.12, 0.13, 0.12, 0.11, 0.1, 0.12, 0.11, 0.1);
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionParameterMetadata> list = new ArrayList<>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionParameterMetadata parameterMetadata =
          GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME.get(i), SimpleStrike.of(STRIKE.get(i)));
      list.add(parameterMetadata);
    }
    METADATA = Surfaces.blackVolatilityByExpiryStrike("CAP_VOL", ACT_365F).withParameterMetadata(list);
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, VOL, INTERPOLATOR_2D);
  private static final double SHIFT = 0.02;
  private static final ConstantCurve CURVE = ConstantCurve.of("shift parameter", SHIFT);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities VOLS =
      ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, SURFACE, CURVE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY =
      new ZonedDateTime[] {dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {-0.01, 0.003, 0.016, 0.032};
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, -34.0, 12.0, 0.1};
  private static final double TEST_FORWARD = 0.015; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  @Test
  public void test_getter() {
    assertThat(VOLS.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(VOLS.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(VOLS.getSurface()).isEqualTo(SURFACE);
    assertThat(VOLS.getParameterCount()).isEqualTo(TIME.size());
    assertThat(VOLS.findData(CURVE.getName()).get()).isEqualTo(CURVE);
    assertThat(VOLS.findData(SURFACE.getName()).get()).isEqualTo(SURFACE);
    assertThat(VOLS.findData(CurveName.of("foo")).isPresent()).isFalse();
    int nParams = VOLS.getParameterCount();
    double newValue = 152d;
    for (int i = 0; i < nParams; ++i) {
      assertThat(VOLS.getParameter(i)).isEqualTo(SURFACE.getParameter(i));
      assertThat(VOLS.getParameterMetadata(i)).isEqualTo(SURFACE.getParameterMetadata(i));
      assertThat(VOLS.withParameter(i, newValue)).isEqualTo(ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
          GBP_LIBOR_3M, VAL_DATE_TIME, SURFACE.withParameter(i, newValue), CURVE));
      assertThat(VOLS.withPerturbation((n, v, m) -> 2d * v)).isEqualTo(ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
          GBP_LIBOR_3M, VAL_DATE_TIME, SURFACE.withPerturbation((n, v, m) -> 2d * v), CURVE));
    }
  }

  @Test
  public void test_price_formula() {
    double sampleVol = 0.2;
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST; j++) {
        for (PutCall putCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
          assertThat(VOLS.price(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol))
              .isEqualTo(BlackFormulaRepository.price(
                  TEST_FORWARD + SHIFT, TEST_STRIKE[j] + SHIFT, expiryTime, sampleVol, putCall.isCall()));
          assertThat(VOLS.priceDelta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol))
              .isEqualTo(BlackFormulaRepository.delta(
                  TEST_FORWARD + SHIFT, TEST_STRIKE[j] + SHIFT, expiryTime, sampleVol, putCall.isCall()));
          assertThat(VOLS.priceGamma(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol))
              .isEqualTo(BlackFormulaRepository.gamma(
                  TEST_FORWARD + SHIFT, TEST_STRIKE[j] + SHIFT, expiryTime, sampleVol));
          assertThat(VOLS.priceTheta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol))
              .isEqualTo(BlackFormulaRepository.driftlessTheta(
                  TEST_FORWARD + SHIFT, TEST_STRIKE[j] + SHIFT, expiryTime, sampleVol));
          assertThat(VOLS.priceVega(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol))
              .isEqualTo(BlackFormulaRepository.vega(TEST_FORWARD + SHIFT, TEST_STRIKE[j] + SHIFT, expiryTime, sampleVol));
        }
      }
    }
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
      for (int j = 0; j < NB_TEST; ++j) {
        double volExpected = SURFACE.zValue(expiryTime, TEST_STRIKE[j] + SHIFT);
        double volComputed = VOLS.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[j], TEST_FORWARD);
        assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE_VOL));
      }
    }
  }

  @Test
  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      for (int k = 0; k < NB_TEST; k++) {
        double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
        IborCapletFloorletSensitivity point = IborCapletFloorletSensitivity.of(
            VOLS.getName(), expiryTime, TEST_STRIKE[k], TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
        double[] sensFd = new double[nData];
        for (int j = 0; j < nData; j++) {
          DoubleArray volDataUp = VOL.subArray(0, nData).with(j, VOL.get(j) + eps);
          DoubleArray volDataDw = VOL.subArray(0, nData).with(j, VOL.get(j) - eps);
          InterpolatedNodalSurface paramUp =
              InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, volDataUp, INTERPOLATOR_2D);
          InterpolatedNodalSurface paramDw =
              InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, volDataDw, INTERPOLATOR_2D);
          ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities provUp =
              ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, paramUp, CURVE);
          ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities provDw =
              ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, paramDw, CURVE);
          double volUp = provUp.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double volDw = provDw.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double fd = 0.5 * (volUp - volDw) / eps;
          sensFd[j] = fd * TEST_SENSITIVITY[i];
        }
        CurrencyParameterSensitivity sensActual = VOLS.parameterSensitivity(point).getSensitivities().get(0);
        double[] computed = sensActual.getSensitivity().toArray();
        assertThat(DoubleArrayMath.fuzzyEquals(computed, sensFd, eps)).isTrue();
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(VOLS);
    ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities vols =
        ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M,
            VAL_DATE_TIME.plusMonths(1),
            InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, VOL, GridSurfaceInterpolator.of(TIME_SQUARE, LINEAR)),
            ConstantCurve.of("shift", 0.05));
    coverBeanEquals(VOLS, vols);
  }

}
