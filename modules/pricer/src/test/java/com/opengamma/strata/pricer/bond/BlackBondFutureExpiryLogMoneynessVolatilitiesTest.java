/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;

/**
 * Test {@link BlackBondFutureExpiryLogMoneynessVolatilities}.
 */
@Test
public class BlackBondFutureExpiryLogMoneynessVolatilitiesTest {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.50, 0.50, 0.50, 0.50, 1.00, 1.00, 1.00, 1.00);
  private static final DoubleArray MONEYNESS =
      DoubleArray.of(-0.02, -0.01, 0.00, 0.01, -0.02, -0.01, 0.00, 0.01, -0.02, -0.01, 0.00, 0.01);
  private static final DoubleArray VOL =
      DoubleArray.of(0.01, 0.011, 0.012, 0.010, 0.011, 0.012, 0.013, 0.012, 0.012, 0.013, 0.014, 0.014);
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionParameterMetadata> list = new ArrayList<GenericVolatilitySurfaceYearFractionParameterMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionParameterMetadata parameterMetadata = GenericVolatilitySurfaceYearFractionParameterMetadata.of(
          TIME.get(i), LogMoneynessStrike.of(MONEYNESS.get(i)));
      list.add(parameterMetadata);
    }
    METADATA = DefaultSurfaceMetadata.builder()
        .surfaceName(SurfaceName.of("GOVT1-BOND-FUT-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.LOG_MONEYNESS)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_365F)
        .parameterMetadata(list)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, VOL, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final BlackBondFutureExpiryLogMoneynessVolatilities VOLS =
      BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, SURFACE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final LocalDate[] TEST_FUTURE_EXPIRY =
      new LocalDate[] {date(2015, 2, 17), date(2015, 5, 17), date(2015, 5, 17), date(2015, 5, 17)};
  private static final double[] TEST_STRIKE_PRICE = new double[] {0.985, 0.985, 0.985, 0.985};
  private static final double[] TEST_FUTURE_PRICE = new double[] {0.98, 0.985, 1.00, 1.01};
  //  private static final double[] TEST_SENSITIVITY = new double[] {9.2, 16.0, 1.8, 5.7 };
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, 1.0, 1.0, 1.0};

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  public void test_valuationDate() {
    assertEquals(VOLS.getValuationDateTime(), VAL_DATE_TIME);
  }

  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      double volExpected = SURFACE.zValue(expiryTime, Math.log(TEST_STRIKE_PRICE[i]
          / TEST_FUTURE_PRICE[i]));
      double volComputed = VOLS.volatility(
          TEST_OPTION_EXPIRY[i], TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      double expiry = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      BondFutureOptionSensitivity point = BondFutureOptionSensitivity.of(
          VOLS.getName(),
          expiry,
          TEST_FUTURE_EXPIRY[i],
          TEST_STRIKE_PRICE[i],
          TEST_FUTURE_PRICE[i],
          USD, TEST_SENSITIVITY[i]);
      CurrencyParameterSensitivity sensActual = VOLS.parameterSensitivity(point).getSensitivities().get(0);
      double[] computed = sensActual.getSensitivity().toArray();
      for (int j = 0; j < nData; j++) {
        DoubleArray volDataUp = VOL.with(j, VOL.get(j) + eps);
        DoubleArray volDataDw = VOL.with(j, VOL.get(j) - eps);
        InterpolatedNodalSurface paramUp =
            InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, volDataUp, INTERPOLATOR_2D);
        InterpolatedNodalSurface paramDw =
            InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, volDataDw, INTERPOLATOR_2D);
        BlackBondFutureExpiryLogMoneynessVolatilities provUp = BlackBondFutureExpiryLogMoneynessVolatilities.of(
            VAL_DATE_TIME, paramUp);
        BlackBondFutureExpiryLogMoneynessVolatilities provDw = BlackBondFutureExpiryLogMoneynessVolatilities.of(
            VAL_DATE_TIME, paramDw);
        double volUp = provUp.volatility(
            expiry, TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
        double volDw = provDw.volatility(
            expiry, TEST_FUTURE_EXPIRY[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
        double fd = 0.5 * (volUp - volDw) / eps;
        assertEquals(computed[j], fd, eps);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackBondFutureExpiryLogMoneynessVolatilities test1 = BlackBondFutureExpiryLogMoneynessVolatilities.of(
        VAL_DATE_TIME, SURFACE);
    coverImmutableBean(test1);
    BlackBondFutureExpiryLogMoneynessVolatilities test2 = BlackBondFutureExpiryLogMoneynessVolatilities.of(
        VAL_DATE_TIME.plusDays(1), SURFACE.withParameter(0, 1d));
    coverBeanEquals(test1, test2);
  }

}
