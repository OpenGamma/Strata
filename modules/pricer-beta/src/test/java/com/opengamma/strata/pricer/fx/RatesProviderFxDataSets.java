/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Sets of market data used in FX tests.
 */
public class RatesProviderFxDataSets {

  private static final Currency KRW = Currency.of("KRW");
  private static final String DISCOUNTING_EUR = "Discounting EUR";
  private static final String DISCOUNTING_USD = "Discounting USD";
  private static final String DISCOUNTING_GBP = "Discounting GBP";
  private static final String DISCOUNTING_KRW = "Discounting KRW";
  private static final double EUR_USD = 1.40;
  private static final double USD_KRW = 1111.11;
  private static final double GBP_USD = 1.50;
  private static final FxMatrix FX_MATRIX =
      FxMatrix.builder()
          .addRate(EUR, USD, EUR_USD)
          .addRate(KRW, USD, 1.0 / USD_KRW)
          .addRate(GBP, USD, GBP_USD)
          .build();

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
      Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final double[] USD_DSC_TIME = new double[] {0.0, 0.5, 1.0, 2.0, 5.0};
  private static final double[] USD_DSC_RATE = new double[] {0.0100, 0.0120, 0.0120, 0.0140, 0.0140};
  private static final String USD_DSC_NAME = "USD Dsc";
  private static final YieldAndDiscountCurve USD_DSC = new YieldCurve(USD_DSC_NAME, new InterpolatedDoublesCurve(USD_DSC_TIME,
      USD_DSC_RATE, LINEAR_FLAT, true, USD_DSC_NAME));

  private static final double[] EUR_DSC_TIME = new double[] {0.0, 0.5, 1.0, 2.0, 5.0};
  private static final double[] EUR_DSC_RATE = new double[] {0.0150, 0.0125, 0.0150, 0.0175, 0.0150};
  private static final String EUR_DSC_NAME = "EUR Dsc";
  private static final YieldAndDiscountCurve EUR_DSC = new YieldCurve(EUR_DSC_NAME, new InterpolatedDoublesCurve(EUR_DSC_TIME,
      EUR_DSC_RATE, LINEAR_FLAT, true, EUR_DSC_NAME));

  private static final double[] GBP_DSC_TIME = new double[] {0.0, 0.5, 1.0, 2.0, 5.0};
  private static final double[] GBP_DSC_RATE = new double[] {0.0160, 0.0135, 0.0160, 0.0185, 0.0160};
  private static final String GBP_DSC_NAME = "GBP Dsc";
  private static final YieldAndDiscountCurve GBP_DSC = new YieldCurve(GBP_DSC_NAME, new InterpolatedDoublesCurve(GBP_DSC_TIME,
      GBP_DSC_RATE, LINEAR_FLAT, true, GBP_DSC_NAME));

  private static final double[] KRW_DSC_TIME = new double[] {0.0, 0.5, 1.0, 2.0, 5.0};
  private static final double[] KRW_DSC_RATE = new double[] {0.0350, 0.0325, 0.0350, 0.0375, 0.0350};
  private static final String KRW_DSC_NAME = "KRW Dsc";
  private static final YieldAndDiscountCurve KRW_DSC = new YieldCurve(KRW_DSC_NAME, new InterpolatedDoublesCurve(KRW_DSC_TIME,
      KRW_DSC_RATE, LINEAR_FLAT, true, KRW_DSC_NAME));

  /**
   * Create a yield curve bundle with three curves.
   * One called "Discounting EUR" with a constant rate of 2.50%, one called "Discounting USD"
   * with a constant rate of 1.00% and one called "Discounting GBP" with a constant rate of 2.00%;
   * "Discounting KRW" with a constant rate of 3.21%;
   * 
   * @return the provider
   */
  public static RatesProvider createProvider() {
    return ImmutableRatesProvider.builder()
        .dayCount(DayCounts.ACT_360)
        .valuationDate(LocalDate.of(2011, 11, 10))
        .discountCurves(ImmutableMap.<Currency, YieldAndDiscountCurve>builder()
            .put(EUR, EUR_DSC)
            .put(USD, USD_DSC)
            .put(GBP, GBP_DSC)
            .put(KRW, KRW_DSC)
            .build())
        .fxMatrix(FX_MATRIX)
        .build();
  }

  public static RatesProvider createProviderEURUSD() {
    FxMatrix fxMatrix = FxMatrix.builder().addRate(USD, EUR, 1.0d / EUR_USD).build();
    return ImmutableRatesProvider.builder()
        .dayCount(DayCounts.ACT_360)
        .valuationDate(LocalDate.of(2011, 11, 10))
        .discountCurves(ImmutableMap.<Currency, YieldAndDiscountCurve>builder()
            .put(EUR, EUR_DSC)
            .put(USD, USD_DSC)
            .build())
        .fxMatrix(fxMatrix)
        .build();
  }

  public static String[] curveNames() {
    return new String[] {DISCOUNTING_EUR, DISCOUNTING_USD, DISCOUNTING_GBP, DISCOUNTING_KRW};
  }

  public static FxMatrix fxMatrix() {
    return FX_MATRIX;
  }

}
