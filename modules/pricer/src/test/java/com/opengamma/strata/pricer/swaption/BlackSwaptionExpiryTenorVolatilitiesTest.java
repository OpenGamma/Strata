/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link BlackSwaptionExpiryTenorVolatilities}.
 */
@Test
public class BlackSwaptionExpiryTenorVolatilitiesTest {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0);
  private static final DoubleArray TENOR =
      DoubleArray.of(3.0, 5.0, 7.0, 10.0, 3.0, 5.0, 7.0, 10.0, 3.0, 5.0, 7.0, 10.0);
  private static final DoubleArray VOL =
      DoubleArray.of(0.14, 0.14, 0.13, 0.12, 0.12, 0.13, 0.12, 0.11, 0.1, 0.12, 0.11, 0.1);
  private static final FixedIborSwapConvention CONVENTION = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
  private static final SurfaceMetadata METADATA;
  static {
    List<SwaptionSurfaceExpiryTenorParameterMetadata> list =
        new ArrayList<SwaptionSurfaceExpiryTenorParameterMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      SwaptionSurfaceExpiryTenorParameterMetadata parameterMetadata =
          SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME.get(i), TENOR.get(i));
      list.add(parameterMetadata);
    }
    METADATA = Surfaces.blackVolatilityByExpiryTenor("GOVT1-SWAPTION-VOL", ACT_365F).withParameterMetadata(list);
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, TENOR, VOL, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final BlackSwaptionExpiryTenorVolatilities VOLS =
      BlackSwaptionExpiryTenorVolatilities.of(CONVENTION, VAL_DATE_TIME, SURFACE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_TENOR = new double[] {2.0, 6.0, 7.0, 15.0};
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, 2.0, 1.2, -1.0};
  private static final double TEST_FORWARD = 0.025; // not used internally
  private static final double TEST_STRIKE = 0.03; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  public void test_valuationDate() {
    assertEquals(VOLS.getValuationDateTime(), VAL_DATE_TIME);
  }

  public void test_swapConvention() {
    assertEquals(VOLS.getConvention(), CONVENTION);
  }

  public void test_findData() {
    assertEquals(VOLS.findData(SURFACE.getName()), Optional.of(SURFACE));
    assertEquals(VOLS.findData(SurfaceName.of("Rubbish")), Optional.empty());
  }

  public void test_tenor() {
    double test1 = VOLS.tenor(VAL_DATE, VAL_DATE);
    assertEquals(test1, 0d);
    double test2 = VOLS.tenor(VAL_DATE, date(2018, 2, 28));
    assertEquals(test2, 3d);
    double test3 = VOLS.tenor(VAL_DATE, date(2018, 2, 10));
    assertEquals(test3, 3d);
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
      double volExpected = SURFACE.zValue(expiryTime, TEST_TENOR[i]);
      double volComputed = VOLS.volatility(
          TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      SwaptionSensitivity point = SwaptionSensitivity.of(
          VOLS.getName(), expiryTime, TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
      CurrencyParameterSensitivities sensActual = VOLS.parameterSensitivity(point);
      DoubleArray computed = sensActual.getSensitivity(SURFACE.getName(), GBP).getSensitivity();
      for (int j = 0; j < nData; j++) {
        DoubleArray volDataUp = VOL.with(j, VOL.get(j) + eps);
        DoubleArray volDataDw = VOL.with(j, VOL.get(j) - eps);
        InterpolatedNodalSurface paramUp =
            InterpolatedNodalSurface.of(METADATA, TIME, TENOR, volDataUp, INTERPOLATOR_2D);
        InterpolatedNodalSurface paramDw =
            InterpolatedNodalSurface.of(METADATA, TIME, TENOR, volDataDw, INTERPOLATOR_2D);
        BlackSwaptionExpiryTenorVolatilities provUp =
            BlackSwaptionExpiryTenorVolatilities.of(CONVENTION, VAL_DATE_TIME, paramUp);
        BlackSwaptionExpiryTenorVolatilities provDw =
            BlackSwaptionExpiryTenorVolatilities.of(CONVENTION, VAL_DATE_TIME, paramDw);
        double volUp = provUp.volatility(
            TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
        double volDw = provDw.volatility(
            TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
        double fd = 0.5 * (volUp - volDw) / eps;
        assertEquals(computed.get(j), fd * TEST_SENSITIVITY[i], eps);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackSwaptionExpiryTenorVolatilities test1 =
        BlackSwaptionExpiryTenorVolatilities.of(CONVENTION, VAL_DATE_TIME, SURFACE);
    coverImmutableBean(test1);
    BlackSwaptionExpiryTenorVolatilities test2 =
        BlackSwaptionExpiryTenorVolatilities.of(CONVENTION, VAL_DATE.atStartOfDay(ZoneOffset.UTC), SURFACE);
    coverBeanEquals(test1, test2);
  }

}
