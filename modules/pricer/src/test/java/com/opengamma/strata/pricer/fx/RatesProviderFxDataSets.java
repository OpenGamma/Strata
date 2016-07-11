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
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Sets of market data used in FX tests.
 */
public class RatesProviderFxDataSets {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /** Wednesday. */
  public static final LocalDate VAL_DATE_2014_01_22 = RatesProviderDataSets.VAL_DATE_2014_01_22;

  private static final Currency KRW = Currency.of("KRW");
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
  private static final DoubleArray USD_DSC_RATE_FLAT = DoubleArray.of(0.0110, 0.0110, 0.0110, 0.0110, 0.0110);
  private static final CurveMetadata USD_DSC_METADATA = Curves.zeroRates("USD Dsc", ACT_360);
  private static final InterpolatedNodalCurve USD_DSC =
      InterpolatedNodalCurve.of(USD_DSC_METADATA, USD_DSC_TIME, USD_DSC_RATE, INTERPOLATOR);
  private static final InterpolatedNodalCurve USD_DSC_FLAT =
      InterpolatedNodalCurve.of(USD_DSC_METADATA, USD_DSC_TIME, USD_DSC_RATE_FLAT, INTERPOLATOR);
  private static final CurveMetadata USD_DSC_METADATA_ISDA = Curves.zeroRates("USD Dsc", ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve USD_DSC_ISDA =
      InterpolatedNodalCurve.of(USD_DSC_METADATA_ISDA, USD_DSC_TIME, USD_DSC_RATE, INTERPOLATOR);

  private static final DoubleArray EUR_DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0);
  private static final DoubleArray EUR_DSC_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150);
  private static final DoubleArray EUR_DSC_RATE_FLAT = DoubleArray.of(0.0150, 0.0150, 0.0150, 0.0150, 0.0150);
  private static final CurveMetadata EUR_DSC_METADATA = Curves.zeroRates("EUR Dsc", ACT_360);
  private static final InterpolatedNodalCurve EUR_DSC =
      InterpolatedNodalCurve.of(EUR_DSC_METADATA, EUR_DSC_TIME, EUR_DSC_RATE, INTERPOLATOR);
  private static final InterpolatedNodalCurve EUR_DSC_FLAT =
      InterpolatedNodalCurve.of(EUR_DSC_METADATA, EUR_DSC_TIME, EUR_DSC_RATE_FLAT, INTERPOLATOR);
  private static final CurveMetadata EUR_DSC_METADATA_ISDA = Curves.zeroRates("EUR Dsc", ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve EUR_DSC_ISDA =
      InterpolatedNodalCurve.of(EUR_DSC_METADATA_ISDA, EUR_DSC_TIME, EUR_DSC_RATE, INTERPOLATOR);

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
    return ImmutableRatesProvider.builder(VAL_DATE_2014_01_22)
        .discountCurve(EUR, EUR_DSC)
        .discountCurve(USD, USD_DSC)
        .discountCurve(GBP, GBP_DSC)
        .discountCurve(KRW, KRW_DSC)
        .fxRateProvider(FX_MATRIX)
        .build();
  }

  /**
   * Create a yield curve bundle with three curves.
   * One called "Discounting EUR" with a constant rate of 2.50%, one called "Discounting USD"
   * with a constant rate of 1.00% and one called "Discounting GBP" with a constant rate of 2.00%;
   * "Discounting KRW" with a constant rate of 3.21%;
   * 
   * @param valuationDate  the valuation date
   * @param fxIndex  the FX index
   * @param spotRate  the spot rate for the index
   * @return the provider
   */
  public static RatesProvider createProvider(LocalDate valuationDate, FxIndex fxIndex, double spotRate) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(EUR, EUR_DSC)
        .discountCurve(USD, USD_DSC)
        .discountCurve(GBP, GBP_DSC)
        .discountCurve(KRW, KRW_DSC)
        .fxRateProvider(FX_MATRIX)
        .timeSeries(
            fxIndex,
            LocalDateDoubleTimeSeries.of(fxIndex.calculateFixingFromMaturity(valuationDate, REF_DATA), spotRate))
        .build();
  }

  /**
   * Creates rates provider for EUR, USD with FX matrix.
   * 
   * @param valuationDate  the valuation date
   * @return the rates provider
   */
  public static ImmutableRatesProvider createProviderEURUSD(LocalDate valuationDate) {
    FxMatrix fxMatrix = FxMatrix.builder().addRate(USD, EUR, 1.0d / EUR_USD).build();
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(EUR, EUR_DSC)
        .discountCurve(USD, USD_DSC)
        .fxRateProvider(fxMatrix)
        .build();
  }

  /**
   * Creates rates provider for EUR, USD with FX matrix.
   * <p>
   * The discount curves are based on the day count convention, ACT/ACT ISDA.
   * 
   * @param valuationDate  the valuation date
   * @return the rates provider
   */
  public static ImmutableRatesProvider createProviderEurUsdActActIsda(LocalDate valuationDate) {
    FxMatrix fxMatrix = FxMatrix.builder().addRate(USD, EUR, 1.0d / EUR_USD).build();
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(EUR, EUR_DSC_ISDA)
        .discountCurve(USD, USD_DSC_ISDA)
        .fxRateProvider(fxMatrix)
        .build();
  }

  /**
   * Creates rates provider for EUR, USD with FX matrix.
   * <p>
   * The discount curves are flat.
   * 
   * @param valuationDate  the valuation date
   * @return the rates provider
   */
  public static ImmutableRatesProvider createProviderEurUsdFlat(LocalDate valuationDate) {
    FxMatrix fxMatrix = FxMatrix.builder().addRate(USD, EUR, 1.0d / EUR_USD).build();
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurve(EUR, EUR_DSC_FLAT)
        .discountCurve(USD, USD_DSC_FLAT)
        .fxRateProvider(fxMatrix)
        .build();
  }

  /**
   * Get the curve name of the curve for a given currency.
   * 
   * @param currency the currency
   * @return the curve name
   */
  public static CurveName getCurveName(Currency currency) {
    if (currency.equals(EUR)) {
      return EUR_DSC.getName();
    }
    if (currency.equals(USD)) {
      return USD_DSC.getName();
    }
    if (currency.equals(GBP)) {
      return GBP_DSC.getName();
    }
    throw new IllegalArgumentException();
  }

  /**
   * Gets the FX matrix.
   * 
   * @return the FX matrix
   */
  public static FxMatrix fxMatrix() {
    return FX_MATRIX;
  }

}
