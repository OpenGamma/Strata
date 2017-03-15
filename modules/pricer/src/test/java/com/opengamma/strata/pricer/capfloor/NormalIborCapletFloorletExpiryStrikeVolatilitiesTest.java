/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link NormalIborCapletFloorletExpiryStrikeVolatilities}.
 */
@Test
public class NormalIborCapletFloorletExpiryStrikeVolatilitiesTest {

  private static final String NAME = "CAP_VOL";
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0);
  private static final DoubleArray STRIKE =
      DoubleArray.of(-0.015, 0.0, 0.01, 0.025, -0.015, 0.0, 0.01, 0.025, -0.015, 0.0, 0.01, 0.025);
  private static final DoubleArray VOL =
      DoubleArray.of(0.14, 0.14, 0.13, 0.12, 0.12, 0.13, 0.12, 0.11, 0.1, 0.12, 0.11, 0.1);
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionParameterMetadata> list =
        new ArrayList<GenericVolatilitySurfaceYearFractionParameterMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionParameterMetadata parameterMetadata =
          GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME.get(i), SimpleStrike.of(STRIKE.get(i)));
      list.add(parameterMetadata);
    }
    METADATA = Surfaces.normalVolatilityByExpiryStrike(NAME, ACT_365F).withParameterMetadata(list);
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, VOL, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS =
      NormalIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, SURFACE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY =
      new ZonedDateTime[] {dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {-0.02, 0.0, 0.019, 0.032};
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, 12.0, -41.0, -2.0};
  private static final double TEST_FORWARD = 0.015; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  public void test_getter() {
    assertEquals(VOLS.getValuationDate(), VAL_DATE);
    assertEquals(VOLS.getIndex(), GBP_LIBOR_3M);
    assertEquals(VOLS.getSurface(), SURFACE);
  }

  public void test_price_formula() {
    double sampleVol = 0.2;
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST; j++) {
        for (PutCall putCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
          double price = VOLS.price(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double delta = VOLS.priceDelta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double gamma = VOLS.priceGamma(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double theta = VOLS.priceTheta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double vega = VOLS.priceVega(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          assertEquals(price,
              NormalFormulaRepository.price(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall));
          assertEquals(delta,
              NormalFormulaRepository.delta(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall));
          assertEquals(gamma,
              NormalFormulaRepository.gamma(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall));
          assertEquals(theta,
              NormalFormulaRepository.theta(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall));
          assertEquals(vega,
              NormalFormulaRepository.vega(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall));
        }
      }
    }
  }

  public void test_relativeTime() {
    double test1 = VOLS.relativeTime(VAL_DATE_TIME);
    assertEquals(test1, 0d);
    double test2 = VOLS.relativeTime(date(2018, 2, 17).atStartOfDay(LONDON_ZONE));
    double test3 = VOLS.relativeTime(date(2012, 2, 17).atStartOfDay(LONDON_ZONE));
    assertEquals(test2, -test3); // consistency checked
  }

  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST; ++j) {
        double volExpected = SURFACE.zValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = VOLS.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[j], TEST_FORWARD);
        assertEquals(volComputed, volExpected, TOLERANCE_VOL);
      }
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      for (int k = 0; k < NB_TEST; k++) {
        double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
        IborCapletFloorletSensitivity point = IborCapletFloorletSensitivity.of(
            IborCapletFloorletVolatilitiesName.of(NAME), expiryTime, TEST_STRIKE[k], TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
        CurrencyParameterSensitivity sensActual = VOLS.parameterSensitivity(point).getSensitivities().get(0);
        DoubleArray computed = sensActual.getSensitivity();
        for (int j = 0; j < nData; ++j) {
          DoubleArray volDataUp = VOL.subArray(0, nData).with(j, VOL.get(j) + eps);
          DoubleArray volDataDw = VOL.subArray(0, nData).with(j, VOL.get(j) - eps);
          InterpolatedNodalSurface paramUp =
              InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, volDataUp, INTERPOLATOR_2D);
          InterpolatedNodalSurface paramDw =
              InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, volDataDw, INTERPOLATOR_2D);
          NormalIborCapletFloorletExpiryStrikeVolatilities provUp =
              NormalIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, paramUp);
          NormalIborCapletFloorletExpiryStrikeVolatilities provDw =
              NormalIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, paramDw);
          double volUp = provUp.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double volDw = provDw.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double fd = 0.5 * (volUp - volDw) / eps;
          assertEquals(computed.get(j), fd * TEST_SENSITIVITY[i], eps);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(VOLS);
    coverBeanEquals(VOLS, VOLS);
  }

}
