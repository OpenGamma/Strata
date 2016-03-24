/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.meta.SwaptionSurfaceExpiryTenorNodeMetadata;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link BlackSwaptionExpiryTenorVolatilities}.
 */
@Test
public class BlackSwaptionExpiryTenorVolatilitiesTest {

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.5, 1.0, 0.25, 0.5, 1.0, 0.25, 0.5, 1.0, 0.25, 0.5, 1.0);
  private static final DoubleArray TENOR =
      DoubleArray.of(3.0, 3.0, 3.0, 5.0, 5.0, 5.0, 7.0, 7.0, 7.0, 10.0, 10.0, 10.0);
  private static final DoubleArray VOL =
      DoubleArray.of(0.14, 0.12, 0.1, 0.14, 0.13, 0.12, 0.13, 0.12, 0.11, 0.12, 0.11, 0.1);
  private static final SurfaceMetadata METADATA;
  static {
    List<SwaptionSurfaceExpiryTenorNodeMetadata> list =
        new ArrayList<SwaptionSurfaceExpiryTenorNodeMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      SwaptionSurfaceExpiryTenorNodeMetadata parameterMetadata =
          SwaptionSurfaceExpiryTenorNodeMetadata.of(TIME.get(i), TENOR.get(i));
      list.add(parameterMetadata);
    }
    METADATA = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .parameterMetadata(list)
        .surfaceName(SurfaceName.of("GOVT1-SWAPTION-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, TENOR, VOL, INTERPOLATOR_2D);
  private static final FixedIborSwapConvention CONVENTION = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final BlackSwaptionExpiryTenorVolatilities PROVIDER =
      BlackSwaptionExpiryTenorVolatilities.of(
          SURFACE, CONVENTION, VAL_DATE, VAL_TIME, LONDON_ZONE, ACT_365F);

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
    assertEquals(PROVIDER.getValuationDateTime(), VAL_DATE_TIME);
  }

  public void test_swapConvention() {
    assertEquals(PROVIDER.getConvention(), CONVENTION);
  }

  public void test_tenor() {
    double test1 = PROVIDER.tenor(VAL_DATE, VAL_DATE);
    assertEquals(test1, 0d);
    double test2 = PROVIDER.tenor(VAL_DATE, date(2018, 2, 28));
    assertEquals(test2, 3d);
    double test3 = PROVIDER.tenor(VAL_DATE, date(2018, 2, 10));
    assertEquals(test3, 3d);
  }

  public void test_relativeTime() {
    double test1 = PROVIDER.relativeTime(VAL_DATE_TIME);
    assertEquals(test1, 0d);
    double test2 = PROVIDER.relativeTime(date(2018, 2, 17).atStartOfDay(LONDON_ZONE));
    double test3 = PROVIDER.relativeTime(date(2012, 2, 17).atStartOfDay(LONDON_ZONE));
    assertEquals(test2, -test3); // consistency checked
  }

  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_OPTION_EXPIRY[i]);
      double volExpected = SURFACE.zValue(expiryTime, TEST_TENOR[i]);
      double volComputed = PROVIDER.volatility(
          TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      SwaptionSensitivity point = SwaptionSensitivity.of(
          CONVENTION, TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
      SurfaceCurrencyParameterSensitivity sensActual = PROVIDER.surfaceCurrencyParameterSensitivity(point);
      double[] computed = sensActual.getSensitivity().toArray();
      for (int j = 0; j < nData; j++) {
        DoubleArray volDataUp = VOL.with(j, VOL.get(j) + eps);
        DoubleArray volDataDw = VOL.with(j, VOL.get(j) - eps);
        InterpolatedNodalSurface paramUp =
            InterpolatedNodalSurface.of(METADATA, TIME, TENOR, volDataUp, INTERPOLATOR_2D);
        InterpolatedNodalSurface paramDw =
            InterpolatedNodalSurface.of(METADATA, TIME, TENOR, volDataDw, INTERPOLATOR_2D);
        BlackSwaptionExpiryTenorVolatilities provUp = BlackSwaptionExpiryTenorVolatilities.of(
            paramUp, CONVENTION, VAL_DATE_TIME, ACT_365F);
        BlackSwaptionExpiryTenorVolatilities provDw = BlackSwaptionExpiryTenorVolatilities.of(
            paramDw, CONVENTION, VAL_DATE_TIME, ACT_365F);
        double volUp = provUp.volatility(
            TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
        double volDw = provDw.volatility(
            TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
        double fd = 0.5 * (volUp - volDw) / eps;
        assertEquals(computed[j], fd * TEST_SENSITIVITY[i], eps);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackSwaptionExpiryTenorVolatilities test1 = BlackSwaptionExpiryTenorVolatilities.of(
        SURFACE, CONVENTION, VAL_DATE_TIME, ACT_365F);
    coverImmutableBean(test1);
    BlackSwaptionExpiryTenorVolatilities test2 = BlackSwaptionExpiryTenorVolatilities.of(
        SURFACE, CONVENTION, VAL_DATE.atStartOfDay(ZoneOffset.UTC), ACT_360);
    coverBeanEquals(test1, test2);
  }

}
