/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;

import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Sets of market data used in FX tests.
 */
public class RatesProviderFxDataSets {

  /** Wednesday. */
  public static final LocalDate VAL_DATE_2014_01_22 = RatesProviderDataSets.VAL_DATE_2014_01_22;

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

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray USD_DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0);
  private static final DoubleArray USD_DSC_RATE = DoubleArray.of(0.0100, 0.0120, 0.0120, 0.0140, 0.0140);
  private static final CurveMetadata USD_DSC_METADATA = Curves.zeroRates("USD Dsc", ACT_360);
  private static final InterpolatedNodalCurve USD_DSC =
      InterpolatedNodalCurve.of(USD_DSC_METADATA, USD_DSC_TIME, USD_DSC_RATE, INTERPOLATOR);

  private static final DoubleArray EUR_DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0);
  private static final DoubleArray EUR_DSC_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150);
  private static final CurveMetadata EUR_DSC_METADATA = Curves.zeroRates("EUR Dsc", ACT_360);
  private static final InterpolatedNodalCurve EUR_DSC =
      InterpolatedNodalCurve.of(EUR_DSC_METADATA, EUR_DSC_TIME, EUR_DSC_RATE, INTERPOLATOR);

  private static final DoubleArray GBP_DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0);
  private static final DoubleArray GBP_DSC_RATE = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0160);
  private static final CurveMetadata GBP_DSC_METADATA = Curves.zeroRates("GBP Dsc", ACT_360);
  private static final InterpolatedNodalCurve GBP_DSC =
      InterpolatedNodalCurve.of(GBP_DSC_METADATA, GBP_DSC_TIME, GBP_DSC_RATE, INTERPOLATOR);

  private static final DoubleArray KRW_DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0);
  private static final DoubleArray KRW_DSC_RATE = DoubleArray.of(0.0350, 0.0325, 0.0350, 0.0375, 0.0350);
  private static final CurveMetadata KRW_DSC_METADATA = Curves.zeroRates("KRW Dsc", ACT_360);
  private static final InterpolatedNodalCurve KRW_DSC =
      InterpolatedNodalCurve.of(KRW_DSC_METADATA, KRW_DSC_TIME, KRW_DSC_RATE, INTERPOLATOR);

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
        .valuationDate(VAL_DATE_2014_01_22)
        .discountCurves(ImmutableMap.<Currency, Curve>builder()
            .put(EUR, EUR_DSC)
            .put(USD, USD_DSC)
            .put(GBP, GBP_DSC)
            .put(KRW, KRW_DSC)
            .build())
        .fxRateProvider(FX_MATRIX)
        .build();
  }

  public static RatesProvider createProviderEURUSD() {
    FxMatrix fxMatrix = FxMatrix.builder().addRate(USD, EUR, 1.0d / EUR_USD).build();
    return ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE_2014_01_22)
        .discountCurves(ImmutableMap.<Currency, Curve>builder()
            .put(EUR, EUR_DSC)
            .put(USD, USD_DSC)
            .build())
        .fxRateProvider(fxMatrix)
        .build();
  }

  public static String[] curveNames() {
    return new String[] {DISCOUNTING_EUR, DISCOUNTING_USD, DISCOUNTING_GBP, DISCOUNTING_KRW};
  }

  public static FxMatrix fxMatrix() {
    return FX_MATRIX;
  }

}
