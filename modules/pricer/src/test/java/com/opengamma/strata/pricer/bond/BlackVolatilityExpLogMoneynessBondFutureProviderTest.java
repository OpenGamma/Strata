/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
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

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.sensitivity.BondFutureOptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.GenericVolatilitySurfaceYearFractionMetadata;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * Test {@link BlackVolatilityExpLogMoneynessBondFutureProvider}.
 */
@Test
public class BlackVolatilityExpLogMoneynessBondFutureProviderTest {

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00);
  private static final DoubleArray MONEYNESS =
      DoubleArray.of(-0.02, -0.02, -0.02, -0.01, -0.01, -0.01, 0.00, 0.00, 0.00, 0.01, 0.01, 0.01);
  private static final DoubleArray VOL =
      DoubleArray.of(0.01, 0.011, 0.012, 0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014);
  private static final SurfaceMetadata METADATA_WITH_PARAM;
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionMetadata> list = new ArrayList<GenericVolatilitySurfaceYearFractionMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionMetadata parameterMetadata = GenericVolatilitySurfaceYearFractionMetadata.of(
          TIME.get(i), LogMoneynessStrike.of(MONEYNESS.get(i)));
      list.add(parameterMetadata);
    }
    METADATA_WITH_PARAM = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .parameterMetadata(list)
        .surfaceName(SurfaceName.of("GOVT1-BOND-FUT-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .build();
    METADATA = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .surfaceName(SurfaceName.of("GOVT1-BOND-FUT-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE_WITH_PARAM =
      InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, MONEYNESS, VOL, INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, VOL, INTERPOLATOR_2D);
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT");
  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);
  private static final BlackVolatilityExpLogMoneynessBondFutureProvider PROVIDER_WITH_PARAM =
      BlackVolatilityExpLogMoneynessBondFutureProvider.of(
          SURFACE_WITH_PARAM, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
  private static final BlackVolatilityExpLogMoneynessBondFutureProvider PROVIDER =
      BlackVolatilityExpLogMoneynessBondFutureProvider.of(SURFACE, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
    dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17) };
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final LocalDate[] TEST_FUTURE_EXPIRY =
      new LocalDate[] {date(2015, 2, 17), date(2015, 5, 17), date(2015, 5, 17), date(2015, 5, 17) };
  private static final double[] TEST_STRIKE_PRICE = new double[] {0.985, 0.985, 0.985, 0.985 };
  private static final double[] TEST_FUTURE_PRICE = new double[] {0.98, 0.985, 1.00, 1.01 };
  //  private static final double[] TEST_SENSITIVITY = new double[] {9.2, 16.0, 1.8, 5.7 };
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, 1.0, 1.0, 1.0 };

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  public void test_valuationDate() {
    assertEquals(PROVIDER_WITH_PARAM.getValuationDateTime(), VALUATION_DATE_TIME);
  }

  public void test_futureId() {
    assertEquals(PROVIDER_WITH_PARAM.getFutureSecurityId(), FUTURE_SECURITY_ID);
  }

  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = PROVIDER_WITH_PARAM.relativeTime(TEST_OPTION_EXPIRY[i]);
      double volExpected = SURFACE_WITH_PARAM.zValue(expiryTime, Math.log(TEST_STRIKE_PRICE[i]
          / TEST_FUTURE_PRICE[i]));
      double volComputed = PROVIDER_WITH_PARAM.getVolatility(
          TEST_OPTION_EXPIRY[i], TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      BondFutureOptionSensitivity point = BondFutureOptionSensitivity.of(FUTURE_SECURITY_ID, TEST_OPTION_EXPIRY[i],
          TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i], USD, TEST_SENSITIVITY[i]);
      SurfaceCurrencyParameterSensitivity sensi = PROVIDER_WITH_PARAM.surfaceCurrencyParameterSensitivity(point);
      Map<DoublesPair, Double> map = new HashMap<DoublesPair, Double>();
      for (int j = 0; j < nData; ++j) {
        double[] volDataUp = Arrays.copyOf(VOL.toArray(), nData);
        double[] volDataDw = Arrays.copyOf(VOL.toArray(), nData);
        volDataUp[j] += eps;
        volDataDw[j] -= eps;
        InterpolatedNodalSurface paramUp = InterpolatedNodalSurface.of(
            METADATA_WITH_PARAM, TIME, MONEYNESS, DoubleArray.copyOf(volDataUp), INTERPOLATOR_2D);
        InterpolatedNodalSurface paramDw = InterpolatedNodalSurface.of(
            METADATA_WITH_PARAM, TIME, MONEYNESS, DoubleArray.copyOf(volDataDw), INTERPOLATOR_2D);
      BlackVolatilityExpLogMoneynessBondFutureProvider provUp = BlackVolatilityExpLogMoneynessBondFutureProvider.of(
          paramUp, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
      BlackVolatilityExpLogMoneynessBondFutureProvider provDw = BlackVolatilityExpLogMoneynessBondFutureProvider.of(
          paramDw, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
      double volUp = provUp.getVolatility(
          TEST_OPTION_EXPIRY[i], TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      double volDw = provDw.getVolatility(
          TEST_OPTION_EXPIRY[i], TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
        double fd = 0.5 * (volUp - volDw) / eps;
        map.put(DoublesPair.of(TIME.get(j), MONEYNESS.get(j)), fd);
      }
      SurfaceCurrencyParameterSensitivity sensiFromNoMetadata = PROVIDER.surfaceCurrencyParameterSensitivity(point);
      List<SurfaceParameterMetadata> list = sensi.getMetadata().getParameterMetadata().get();
      double[] computed = sensi.getSensitivity().toArray();
      assertEquals(computed.length, nData);
      for (int j = 0; j < list.size(); ++j) {
        GenericVolatilitySurfaceYearFractionMetadata metadata =
            (GenericVolatilitySurfaceYearFractionMetadata) list.get(i);
        double expected = map.get(DoublesPair.of(metadata.getYearFraction(), metadata.getStrike().getValue()));
        assertEquals(computed[i], expected, eps);
        assertTrue(sensiFromNoMetadata.getMetadata().getParameterMetadata().get().contains(metadata));
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackVolatilityExpLogMoneynessBondFutureProvider test1 = BlackVolatilityExpLogMoneynessBondFutureProvider.of(
        SURFACE_WITH_PARAM, FUTURE_SECURITY_ID, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test1);
    BlackVolatilityExpLogMoneynessBondFutureProvider test2 = BlackVolatilityExpLogMoneynessBondFutureProvider.of(
        SURFACE_WITH_PARAM, FUTURE_SECURITY_ID, ACT_360, VALUATION_DATE_TIME.plusDays(1));
    coverBeanEquals(test1, test2);
  }

}
