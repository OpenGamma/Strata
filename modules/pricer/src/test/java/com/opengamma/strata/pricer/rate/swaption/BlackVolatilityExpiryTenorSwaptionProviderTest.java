/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.market.sensitivity.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.SwaptionVolatilitySurfaceExpiryTenorNodeMetadata;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;

/**
 * Test {@link BlackVolatilityExpiryTenorSwaptionProvider}.
 */
@Test
public class BlackVolatilityExpiryTenorSwaptionProviderTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final double[] TIME = new double[] {0.25, 0.5, 1.0, 0.25, 0.5, 1.0, 0.25, 0.5, 1.0, 0.25, 0.5, 1.0};
  private static final double[] TENOR = new double[] {3.0, 3.0, 3.0, 5.0, 5.0, 5.0, 7.0, 7.0, 7.0, 10.0, 10.0, 10.0};
  private static final double[] VOL = new double[] {0.14, 0.12, 0.1, 0.14, 0.13, 0.12, 0.13, 0.12, 0.11, 0.12, 0.11, 0.1};
  private static final SurfaceMetadata METADATA_WITH_PARAM;
  private static final SurfaceMetadata METADATA;
  static {
    List<SwaptionVolatilitySurfaceExpiryTenorNodeMetadata> list =
        new ArrayList<SwaptionVolatilitySurfaceExpiryTenorNodeMetadata>();
    int nData = TIME.length;
    for (int i = 0; i < nData; ++i) {
      SwaptionVolatilitySurfaceExpiryTenorNodeMetadata parameterMetadata =
          SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(TIME[i], TENOR[i]);
      list.add(parameterMetadata);
    }
    METADATA_WITH_PARAM = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .parameterMetadata(list)
        .surfaceName(SurfaceName.of("GOVT1-SWAPTION-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .build();
    METADATA = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .surfaceName(SurfaceName.of("GOVT1-SWAPTION-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE_WITH_PARAM =
      InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, TENOR, VOL, INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, TENOR, VOL, INTERPOLATOR_2D);
  private static final FixedIborSwapConvention CONVENTION = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);
  private static final BlackVolatilityExpiryTenorSwaptionProvider PROVIDER_WITH_PARAM =
      BlackVolatilityExpiryTenorSwaptionProvider.of(
          SURFACE_WITH_PARAM, CONVENTION, ACT_365F, VALUATION_DATE, VALUATION_TIME, LONDON_ZONE);
  private static final BlackVolatilityExpiryTenorSwaptionProvider PROVIDER =
      BlackVolatilityExpiryTenorSwaptionProvider.of(
          SURFACE, CONVENTION, ACT_365F, VALUATION_DATE, VALUATION_TIME, LONDON_ZONE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_TENOR = new double[] {2.0, 6.0, 7.0, 15.0};
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, 1.0, 1.0, 1.0};
  private static final double TEST_FORWARD = 0.025; // not used internally
  private static final double TEST_STRIKE = 0.03; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  public void test_valuationDate() {
    assertEquals(PROVIDER_WITH_PARAM.getValuationDateTime(), VALUATION_DATE_TIME);
  }

  public void test_swapConvention() {
    assertEquals(PROVIDER_WITH_PARAM.getConvention(), CONVENTION);
  }

  public void test_tenor() {
    double test1 = PROVIDER_WITH_PARAM.tenor(VALUATION_DATE, VALUATION_DATE);
    assertEquals(test1, 0d);
    double test2 = PROVIDER_WITH_PARAM.tenor(VALUATION_DATE, date(2018, 2, 28));
    assertEquals(test2, 3d);
    double test3 = PROVIDER_WITH_PARAM.tenor(VALUATION_DATE, date(2018, 2, 10));
    assertEquals(test3, 3d);
  }

  public void test_relativeTime() {
    double test1 = PROVIDER_WITH_PARAM.relativeTime(VALUATION_DATE_TIME);
    assertEquals(test1, 0d);
    double test2 = PROVIDER_WITH_PARAM.relativeTime(date(2018, 2, 17).atStartOfDay(LONDON_ZONE));
    double test3 = PROVIDER_WITH_PARAM.relativeTime(date(2012, 2, 17).atStartOfDay(LONDON_ZONE));
    assertEquals(test2, -test3); // consistency checked
  }

  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expirationTime = PROVIDER_WITH_PARAM.relativeTime(TEST_OPTION_EXPIRY[i]);
      double volExpected = SURFACE_WITH_PARAM.zValue(expirationTime, TEST_TENOR[i]);
      double volComputed = PROVIDER_WITH_PARAM.getVolatility(
          TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.length;
    for (int i = 0; i < NB_TEST; i++) {
      SwaptionSensitivity point = SwaptionSensitivity.of(
          CONVENTION, TEST_OPTION_EXPIRY[i], TENOR[i], TEST_STRIKE, TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
      SurfaceCurrencyParameterSensitivity sensi = PROVIDER_WITH_PARAM.surfaceCurrencyParameterSensitivity(point);
      Map<DoublesPair, Double> map = new HashMap<DoublesPair, Double>();
      for (int j = 0; j < nData; ++j) {
        double[] volDataUp = Arrays.copyOf(VOL, nData);
        double[] volDataDw = Arrays.copyOf(VOL, nData);
        volDataUp[j] += eps;
        volDataDw[j] -= eps;
        InterpolatedNodalSurface paramUp =
            InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, TENOR, volDataUp, INTERPOLATOR_2D);
        InterpolatedNodalSurface paramDw =
            InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, TENOR, volDataDw, INTERPOLATOR_2D);
        BlackVolatilityExpiryTenorSwaptionProvider provUp = BlackVolatilityExpiryTenorSwaptionProvider.of(
            paramUp, CONVENTION, ACT_365F, VALUATION_DATE_TIME);
        BlackVolatilityExpiryTenorSwaptionProvider provDw = BlackVolatilityExpiryTenorSwaptionProvider.of(
            paramDw, CONVENTION, ACT_365F, VALUATION_DATE_TIME);
        double volUp = provUp.getVolatility(
            TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
        double volDw = provDw.getVolatility(
            TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE, TEST_FORWARD);
        double fd = 0.5 * (volUp - volDw) / eps;
        map.put(DoublesPair.of(TIME[j], TENOR[j]), fd);
      }
      SurfaceCurrencyParameterSensitivity sensiFromNoMetadata = PROVIDER.surfaceCurrencyParameterSensitivity(point);
      List<SurfaceParameterMetadata> list = sensi.getMetadata().getParameterMetadata().get();
      double[] computed = sensi.getSensitivity();
      assertEquals(computed.length, nData);
      for (int j = 0; j < list.size(); ++j) {
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata metadata =
            (SwaptionVolatilitySurfaceExpiryTenorNodeMetadata) list.get(i);
        double expected = map.get(DoublesPair.of(metadata.getYearFraction(), metadata.getTenor()));
        assertEquals(computed[i], expected, eps);
        assertTrue(sensiFromNoMetadata.getMetadata().getParameterMetadata().get().contains(metadata));
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackVolatilityExpiryTenorSwaptionProvider test1 = BlackVolatilityExpiryTenorSwaptionProvider.of(
        SURFACE_WITH_PARAM, CONVENTION, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test1);
    BlackVolatilityExpiryTenorSwaptionProvider test2 = BlackVolatilityExpiryTenorSwaptionProvider.of(
        SURFACE, CONVENTION, ACT_360, VALUATION_DATE);
    coverBeanEquals(test1, test2);
  }

}
