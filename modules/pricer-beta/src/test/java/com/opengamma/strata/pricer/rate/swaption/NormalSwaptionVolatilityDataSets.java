/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.provider.NormalVolatilityExpiryTenorSwaptionProvider;

/**
 * Normal swaption volatility data sets for testing.
 */
public class NormalSwaptionVolatilityDataSets {
  
  private static final double BP1 = 1.0E-4;

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  
  //     =====     Standard figures for testing     =====
  private static final double[] TIMES =
      new double[] {0.50, 1.00, 5.00, 10.0, 0.50, 1.00, 5.00, 10.0,
        0.50, 1.00, 5.00, 10.0, 0.50, 1.00, 5.00, 10.0, 0.50, 1.00, 5.00, 10.0 };
  private static final double[] TENOR =
      new double[] {1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 2.0,
        5.0, 5.0, 5.0, 5.0, 10.0, 10.0, 10.0, 10.0, 30.0, 30.0, 30.0, 30.0 };
  private static final double[] NORMAL_VOL =
      new double[] {0.010, 0.011, 0.012, 0.013, 0.011, 0.012, 0.013, 0.014,
        0.012, 0.013, 0.014, 0.015, 0.013, 0.014, 0.015, 0.016, 0.014, 0.015, 0.016, 0.017 };
  private static final InterpolatedDoublesSurface SURFACE_STD =
      new InterpolatedDoublesSurface(TIMES, TENOR, NORMAL_VOL, INTERPOLATOR_2D);

  private static final LocalDate VALUATION_DATE_STD = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalTime VALUATION_TIME_STD = LocalTime.of(13, 45);
  private static final ZoneId VALUATION_ZONE_STD = ZoneId.of("Europe/London");
  
  public static final NormalVolatilityExpiryTenorSwaptionProvider NORMAL_VOL_SWAPTION_PROVIDER_USD_STD = 
      NormalVolatilityExpiryTenorSwaptionProvider.of(SURFACE_STD, USD_LIBOR_3M, DayCounts.ACT_365F, 
          VALUATION_DATE_STD, VALUATION_TIME_STD, VALUATION_ZONE_STD);
  
  /**
   * Returns the swaption normal volatility surface shifted by a given amount. The shift is parallel.
   * @param shift  the shift
   * @return the swaption normal volatility surface
   */
  public static NormalVolatilityExpiryTenorSwaptionProvider normalVolSwaptionProviderUsdStsShifted(double shift) {
    double[] volShifted = NORMAL_VOL.clone();
    for(int i=0; i<volShifted.length; i++) {
      volShifted[i] += shift;
    }
    return NormalVolatilityExpiryTenorSwaptionProvider.of(
        new InterpolatedDoublesSurface(TIMES, TENOR, volShifted, INTERPOLATOR_2D), 
        USD_LIBOR_3M, DayCounts.ACT_365F, VALUATION_DATE_STD, VALUATION_TIME_STD, VALUATION_ZONE_STD);
  }
  
  //     =====     Flat volatilities for testing     =====
  
  private static final double[] TIMES_FLAT = new double[] {0.0, 100.0, 0.0, 100.0 };
  private static final double[] TENOR_FLAT = new double[] {0.0, 0.0, 30.0, 30.0 };
  private static final double[] NORMAL_VOL_FLAT = new double[] { 0.01, 0.01, 0.01, 0.01};  
  private static final InterpolatedDoublesSurface SURFACE_FLAT = 
      new InterpolatedDoublesSurface(TIMES_FLAT, TENOR_FLAT, NORMAL_VOL_FLAT, INTERPOLATOR_2D);
  
  public static final NormalVolatilityExpiryTenorSwaptionProvider NORMAL_VOL_SWAPTION_PROVIDER_USD_FLAT = 
      NormalVolatilityExpiryTenorSwaptionProvider.of(SURFACE_FLAT, USD_LIBOR_3M, DayCounts.ACT_365F, 
          VALUATION_DATE_STD, VALUATION_TIME_STD, VALUATION_ZONE_STD);
      
  
  //     =====     Market data as of 2014-03-20     =====
  
  private static final double[] TIMES_20150320 = new double[] {
    0.25, 0.25, 0.25, 0.25, 0.25, 
    0.50, 0.50, 0.50, 0.50, 0.50, 
    1.0, 1.0, 1.0, 1.0, 1.0, 
    2.0, 2.0, 2.0, 2.0, 2.0, 
    5.0, 5.0, 5.0, 5.0, 5.0, 
    10.0, 10.0, 10.0, 10.0, 10.0
  };
  private static final double[] TENORS_20150320 = new double[] {
    1.0, 2.0, 5.0, 10.0, 30.0,
    1.0, 2.0, 5.0, 10.0, 30.0,
    1.0, 2.0, 5.0, 10.0, 30.0,
    1.0, 2.0, 5.0, 10.0, 30.0,
    1.0, 2.0, 5.0, 10.0, 30.0,
    1.0, 2.0, 5.0, 10.0, 30.0 };
  private static final double[] NORMAL_VOL_20150320_BP = new double[] {
    43.6, 65.3, 88, 87.5, 88, // 3M
    55.5, 72.2, 90.3, 89.3, 88.6, // 6M
    72.6, 82.7, 91.6, 89.8, 87.3, // 1Y
    90.4, 91.9, 93.4, 84.7, 93.5, // 2Y
    99.3, 96.8, 94.3, 88.6, 77.3,  // 5Y
    88.4, 85.9, 82.2, 76.7, 65.1 // 10Y
  };
  private static final double[] NORMAL_VOL_20150320 = new double[NORMAL_VOL_20150320_BP.length];
  static {
    for(int i=0; i<NORMAL_VOL_20150320_BP.length; i++) {
      NORMAL_VOL_20150320[i] = NORMAL_VOL_20150320_BP[i] * BP1;
    }
  }
  private static final InterpolatedDoublesSurface SURFACE_20150320 =
      new InterpolatedDoublesSurface(TIMES_20150320, TENORS_20150320, NORMAL_VOL_20150320, INTERPOLATOR_2D);

  private static final LocalDate VALUATION_DATE_20150320 = LocalDate.of(2015, 3, 20);
  private static final LocalTime VALUATION_TIME_20150320 = LocalTime.of(18, 00);
  private static final ZoneId VALUATION_ZONE_20150320 = ZoneId.of("Europe/London");
  
  public static final NormalVolatilityExpiryTenorSwaptionProvider NORMAL_VOL_SWAPTION_PROVIDER_USD_20150320 = 
      NormalVolatilityExpiryTenorSwaptionProvider.of(SURFACE_20150320, USD_LIBOR_3M, DayCounts.ACT_365F, 
          VALUATION_DATE_20150320, VALUATION_TIME_20150320, VALUATION_ZONE_20150320);  

}
