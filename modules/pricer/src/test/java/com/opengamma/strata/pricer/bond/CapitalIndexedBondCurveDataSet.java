/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.JP_CPI_EXF;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;

import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * The data set for testing capital indexed bonds.
 */
public class CapitalIndexedBondCurveDataSet {

  private static final StandardId ISSUER_ID = StandardId.of("OG-Ticker", "GOVT");
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT");
  private static final BondGroup GROUP_REPO = BondGroup.of("GOVT BONDS");
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName ISSUER_CURVE_NAME = CurveName.of("issuerCurve");
  private static final CurveName REPO_CURVE_NAME = CurveName.of("repoCurve");

  private static final InterpolatedNodalCurve ISSUER_CURVE;
  private static final InterpolatedNodalCurve REPO_CURVE;
  private static final InterpolatedNodalCurve CPI_CURVE;
  private static final InterpolatedNodalCurve RPI_CURVE;
  private static final InterpolatedNodalCurve CPIJ_CURVE;
  static {
    DoubleArray timeIssuer = DoubleArray.of(0.2493150684931507, 0.4986301369863014, 0.9397260273972603,
        1.9760386256456322, 4.975342465753425, 9.850355565536344);
    DoubleArray rateIssuer = DoubleArray.of(6.796425420368682E-5, 3.114315257821455E-4, 7.126179601599612E-4,
        0.04946562985220742, 0.01404542200399637, 0.022260846895257275);
    CurveMetadata metaIssuer = Curves.zeroRates(ISSUER_CURVE_NAME, ACT_ACT_ISDA);
    ISSUER_CURVE = InterpolatedNodalCurve.of(
        metaIssuer, timeIssuer, rateIssuer, INTERPOLATOR);
    DoubleArray timeRepo = DoubleArray.of(0.0027397260273972603, 0.0136986301369863, 0.1095890410958904,
        0.18904109589041096, 0.27123287671232876, 0.5178082191780822, 0.7671232876712328, 1.0191780821917809,
        2.025218953514485, 3.0246575342465754, 4.021917808219178, 5.019178082191781, 6.019754472640168,
        7.024657534246575, 8.024657534246575, 9.024657534246575, 10.019754472640168);
    DoubleArray rateRepo = DoubleArray.of(0.0016222186172986138, 0.001622209965572477, 7.547616096755544E-4,
        9.003947315389025E-4, 9.833562990057003E-4, 9.300905368344651E-4, 0.0010774349342544426, 0.001209299356175582,
        0.003243498783874946, 0.007148138535707508, 0.011417234937364525, 0.015484713638367467, 0.01894872475170524,
        0.02177798040124286, 0.024146976832379798, 0.02610320121432829, 0.027814843351943817);
    CurveMetadata metaRepo = Curves.zeroRates(REPO_CURVE_NAME, ACT_ACT_ISDA);
    REPO_CURVE = InterpolatedNodalCurve.of(metaRepo, timeRepo, rateRepo, INTERPOLATOR);
    DoubleArray timeCpi = DoubleArray.of(10, 22, 34, 46, 58, 70, 82, 94, 106, 118, 142, 178, 238, 298, 358);
    DoubleArray valueCpi = DoubleArray.of(242.88404516129032, 248.03712245417105, 252.98128118335094,
        258.0416354687366, 263.20242369585515, 268.4653023378886, 273.83617795725064, 279.3124974961296,
        284.8987721100803, 290.5954768446179, 302.3336095056465, 320.8351638061777, 354.2203489141063,
        391.08797576744865, 431.7913437911175);
    CurveMetadata metaCpi = Curves.prices("cpiCurve");
    CPI_CURVE = InterpolatedNodalCurve.of(metaCpi, timeCpi, valueCpi, INTERPOLATOR);
    DoubleArray timeRpi = DoubleArray.of(10, 22, 34, 46, 58, 70, 82, 94, 106, 118, 142);
    DoubleArray valueRpi = DoubleArray.of(263.49967737807305, 270.2383424030053, 277.34957060924364, 284.992794643866,
        293.2359607153748, 302.0252215004671, 311.3482439082226, 321.10465920118116, 331.44556112285863,
        342.4913522908549, 366.076015086898);
    CurveMetadata metaRpi = Curves.prices("rpiCurve");
    RPI_CURVE = InterpolatedNodalCurve.of(metaRpi, timeRpi, valueRpi, INTERPOLATOR);
    DoubleArray timeCpij = DoubleArray.of(10, 22, 34, 46, 58, 70, 82, 94, 106, 118, 142, 178, 238, 298, 358);
    DoubleArray valueCpij = DoubleArray.of(103.3374833371608, 104.2306743501241, 104.3107880426369, 104.27037709028433,
        104.19961127790909, 104.062704760821, 103.89860712110973, 103.73391283682416, 103.78374038315715,
        103.83356515845553, 104.18698970060639, 104.72128789312038, 106.46204440686186, 108.231124353441,
        110.03241679315009);
    CurveMetadata metaCpij = Curves.prices("cpijCurve");
    CPIJ_CURVE = InterpolatedNodalCurve.of(metaCpij, timeCpij, valueCpij, INTERPOLATOR);
  }

  /**
   * Obtains an immutable rates providers with valuation date and time series.
   * <p>
   * The time series must contain historical data for the price index.
   * 
   * @param valuationDate  the valuation date
   * @param timeSeries  the time series
   * @return the rates provider
   */
  public static ImmutableRatesProvider getRatesProvider(LocalDate valuationDate, LocalDateDoubleTimeSeries timeSeries) {
    return ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(FxMatrix.empty())
        .priceIndexCurve(US_CPI_U, CPI_CURVE)
        .timeSeries(US_CPI_U, timeSeries)
        .build();
  }

  /**
   * Obtains an immutable rates providers with valuation date and time series.
   * <p>
   * The time series must contain historical data for the price index.
   * 
   * @param valuationDate  the valuation date
   * @param timeSeries  the time series
   * @return the rates provider
   */
  public static ImmutableRatesProvider getRatesProviderGb(LocalDate valuationDate, LocalDateDoubleTimeSeries timeSeries) {
    return ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(FxMatrix.empty())
        .priceIndexCurve(GB_RPI, RPI_CURVE)
        .timeSeries(GB_RPI, timeSeries)
        .build();
  }

  /**
   * Obtains an immutable rates providers with valuation date and time series.
   * <p>
   * The time series must contain historical data for the price index.
   * 
   * @param valuationDate  the valuation date
   * @param timeSeries  the time series
   * @return the rates provider
   */
  public static ImmutableRatesProvider getRatesProviderJp(LocalDate valuationDate, LocalDateDoubleTimeSeries timeSeries) {
    return ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(FxMatrix.empty())
        .priceIndexCurve(JP_CPI_EXF, CPIJ_CURVE)
        .timeSeries(JP_CPI_EXF, timeSeries)
        .build();
  }

  /**
   * Obtains legal entity discounting rates provider from valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the discounting rates provider
   */
  public static LegalEntityDiscountingProvider getLegalEntityDiscountingProvider(LocalDate valuationDate) {
    DiscountFactors dscIssuer = ZeroRateDiscountFactors.of(USD, valuationDate, ISSUER_CURVE);
    DiscountFactors dscRepo = ZeroRateDiscountFactors.of(USD, valuationDate, REPO_CURVE);
    return LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, USD), dscIssuer))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ISSUER_ID, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, USD), dscRepo))
        .bondMap(ImmutableMap.<StandardId, BondGroup>of(ISSUER_ID, GROUP_REPO))
        .build();
  }

  /**
   * Obtains legal entity discounting rates provider from valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the discounting rates provider
   */
  public static LegalEntityDiscountingProvider getLegalEntityDiscountingProviderGb(LocalDate valuationDate) {
    DiscountFactors dscIssuer = ZeroRateDiscountFactors.of(GBP, valuationDate, ISSUER_CURVE);
    DiscountFactors dscRepo = ZeroRateDiscountFactors.of(GBP, valuationDate, REPO_CURVE);
    return LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), dscIssuer))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ISSUER_ID, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), dscRepo))
        .bondMap(ImmutableMap.<StandardId, BondGroup>of(ISSUER_ID, GROUP_REPO))
        .build();
  }

  /**
   * Obtains legal entity discounting rates provider from valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the discounting rates provider
   */
  public static LegalEntityDiscountingProvider getLegalEntityDiscountingProviderJp(LocalDate valuationDate) {
    DiscountFactors dscIssuer = ZeroRateDiscountFactors.of(JPY, valuationDate, ISSUER_CURVE);
    DiscountFactors dscRepo = ZeroRateDiscountFactors.of(JPY, valuationDate, REPO_CURVE);
    return LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, JPY), dscIssuer))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ISSUER_ID, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, JPY), dscRepo))
        .bondMap(ImmutableMap.<StandardId, BondGroup>of(ISSUER_ID, GROUP_REPO))
        .build();
  }

  /**
   * Obtains issuer curve discount factors form valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the discount factors
   */
  public static IssuerCurveDiscountFactors getIssuerCurveDiscountFactors(LocalDate valuationDate) {
    DiscountFactors dscIssuer = ZeroRateDiscountFactors.of(USD, valuationDate, ISSUER_CURVE);
    return IssuerCurveDiscountFactors.of(dscIssuer, GROUP_ISSUER);
  }

  /**
   * Obtains the issuer ID.
   * 
   * @return the issuer ID
   */
  public static StandardId getIssuerId() {
    return ISSUER_ID;
  }

  /**
   * Obtains time series of price index up to valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the time series
   */
  public static LocalDateDoubleTimeSeries getTimeSeries(LocalDate valuationDate) {
    LocalDate[] dates = new LocalDate[] {LocalDate.of(2005, 1, 31), LocalDate.of(2005, 2, 28), LocalDate.of(2005, 3, 31),
      LocalDate.of(2005, 4, 30), LocalDate.of(2005, 5, 31), LocalDate.of(2005, 6, 30), LocalDate.of(2005, 7, 31),
      LocalDate.of(2005, 8, 31), LocalDate.of(2005, 9, 30), LocalDate.of(2005, 10, 31), LocalDate.of(2005, 11, 30),
      LocalDate.of(2005, 12, 31), LocalDate.of(2006, 1, 31), LocalDate.of(2006, 2, 28), LocalDate.of(2006, 3, 31),
      LocalDate.of(2006, 4, 30), LocalDate.of(2006, 5, 31), LocalDate.of(2006, 6, 30), LocalDate.of(2006, 7, 31),
      LocalDate.of(2006, 8, 31), LocalDate.of(2006, 9, 30), LocalDate.of(2006, 10, 31), LocalDate.of(2006, 11, 30),
      LocalDate.of(2006, 12, 31), LocalDate.of(2007, 1, 31), LocalDate.of(2007, 2, 28), LocalDate.of(2007, 3, 31),
      LocalDate.of(2007, 4, 30), LocalDate.of(2007, 5, 31), LocalDate.of(2007, 6, 30), LocalDate.of(2007, 7, 31),
      LocalDate.of(2007, 8, 31), LocalDate.of(2007, 9, 30), LocalDate.of(2007, 10, 31), LocalDate.of(2007, 11, 30),
      LocalDate.of(2007, 12, 31), LocalDate.of(2008, 1, 31), LocalDate.of(2008, 2, 29), LocalDate.of(2008, 3, 31),
      LocalDate.of(2008, 4, 30), LocalDate.of(2008, 5, 31), LocalDate.of(2008, 6, 30), LocalDate.of(2008, 7, 31),
      LocalDate.of(2008, 8, 31), LocalDate.of(2008, 9, 30), LocalDate.of(2008, 10, 31), LocalDate.of(2008, 11, 30),
      LocalDate.of(2008, 12, 31), LocalDate.of(2009, 1, 31), LocalDate.of(2009, 2, 28), LocalDate.of(2009, 3, 31),
      LocalDate.of(2009, 4, 30), LocalDate.of(2009, 5, 31), LocalDate.of(2009, 6, 30), LocalDate.of(2009, 7, 31),
      LocalDate.of(2009, 8, 31), LocalDate.of(2009, 9, 30), LocalDate.of(2009, 10, 31), LocalDate.of(2009, 11, 30),
      LocalDate.of(2009, 12, 31), LocalDate.of(2010, 1, 31), LocalDate.of(2010, 2, 28), LocalDate.of(2010, 3, 31),
      LocalDate.of(2010, 4, 30), LocalDate.of(2010, 5, 31), LocalDate.of(2010, 6, 30), LocalDate.of(2010, 7, 31),
      LocalDate.of(2010, 8, 31), LocalDate.of(2010, 9, 30), LocalDate.of(2010, 10, 31), LocalDate.of(2010, 11, 30),
      LocalDate.of(2010, 12, 31), LocalDate.of(2011, 1, 31), LocalDate.of(2011, 2, 28), LocalDate.of(2011, 3, 31),
      LocalDate.of(2011, 4, 30), LocalDate.of(2011, 5, 31), LocalDate.of(2011, 6, 30), LocalDate.of(2011, 7, 31),
      LocalDate.of(2011, 8, 31), LocalDate.of(2011, 9, 30), LocalDate.of(2011, 10, 31), LocalDate.of(2011, 11, 30),
      LocalDate.of(2011, 12, 31), LocalDate.of(2012, 1, 31), LocalDate.of(2012, 2, 29), LocalDate.of(2012, 3, 31),
      LocalDate.of(2012, 4, 30), LocalDate.of(2012, 5, 31), LocalDate.of(2012, 6, 30), LocalDate.of(2012, 7, 31),
      LocalDate.of(2012, 8, 31), LocalDate.of(2012, 9, 30), LocalDate.of(2012, 10, 31), LocalDate.of(2012, 11, 30),
      LocalDate.of(2012, 12, 31), LocalDate.of(2013, 1, 31), LocalDate.of(2013, 2, 28), LocalDate.of(2013, 3, 31),
      LocalDate.of(2013, 4, 30), LocalDate.of(2013, 5, 31), LocalDate.of(2013, 6, 30), LocalDate.of(2013, 7, 31),
      LocalDate.of(2013, 8, 31), LocalDate.of(2013, 9, 30), LocalDate.of(2013, 10, 31), LocalDate.of(2013, 11, 30),
      LocalDate.of(2013, 12, 31), LocalDate.of(2014, 1, 31), LocalDate.of(2014, 2, 28), LocalDate.of(2014, 3, 31),
      LocalDate.of(2014, 4, 30), LocalDate.of(2014, 5, 31), LocalDate.of(2014, 6, 30), LocalDate.of(2014, 7, 31),
      LocalDate.of(2014, 8, 31), LocalDate.of(2014, 9, 30), LocalDate.of(2014, 10, 31), LocalDate.of(2014, 11, 30),
      LocalDate.of(2014, 12, 31), LocalDate.of(2015, 1, 31), LocalDate.of(2015, 2, 28), LocalDate.of(2015, 3, 31),
      LocalDate.of(2015, 4, 30), LocalDate.of(2015, 5, 31), LocalDate.of(2015, 6, 30), LocalDate.of(2015, 7, 31),
      LocalDate.of(2015, 8, 31), LocalDate.of(2015, 9, 30), LocalDate.of(2015, 10, 31), LocalDate.of(2015, 11, 30),
      LocalDate.of(2015, 12, 31), LocalDate.of(2016, 1, 31) };
    double[] values = new double[] {211.143, 212.193, 212.709, 213.24, 213.856, 215.693, 215.351, 215.834, 215.969,
      216.177, 216.33, 215.949, 211.143, 212.193, 212.709, 213.24, 213.856, 215.693, 215.351, 215.834, 215.969,
      216.177, 216.33, 215.949, 211.143, 212.193, 212.709, 213.24, 213.856, 215.693, 215.351, 215.834, 215.969,
      216.177, 216.33, 215.949, 211.143, 212.193, 212.709, 213.24, 213.856, 215.693, 215.351, 215.834, 215.969,
      216.177, 216.33, 215.949, 211.143, 212.193, 212.709, 213.24, 213.856, 215.693, 215.351, 215.834, 215.969,
      216.177, 216.33, 215.949, 216.687, 216.741, 217.631, 218.009, 218.178, 217.965, 218.011, 218.312, 218.439,
      218.711, 218.803, 219.179, 220.223, 221.309, 223.467, 224.906, 225.964, 225.722, 225.922, 226.545, 226.889,
      226.421, 226.23, 225.672, 226.655, 227.663, 229.392, 230.085, 229.815, 229.478, 229.104, 230.379, 231.407,
      231.317, 230.221, 229.601, 230.28, 232.166, 232.773, 232.531, 232.945, 233.504, 233.596, 233.877, 234.149,
      233.546, 233.069, 233.049, 233.916, 234.781, 236.293, 237.072, 237.9, 238.343, 238.25, 237.852, 238.031, 237.433,
      236.151, 234.812, 233.707, 234.722, 236.119, 236.599, 237.805, 238.638, 238.654, 238.316, 237.945, 237.838,
      237.336, 236.525, 236.916 };
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < values.length; ++i) {
      if (dates[i].isBefore(valuationDate)) {
        builder.put(dates[i], values[i]);
      }
    }
    return builder.build();
  }

  /**
   * Obtains time series of price index up to valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the time series
   */
  public static LocalDateDoubleTimeSeries getTimeSeriesGb(LocalDate valuationDate) {
    LocalDate[] dates = new LocalDate[] {
      LocalDate.of(2015, 1, 31), LocalDate.of(2015, 2, 28), LocalDate.of(2015, 3, 31),
      LocalDate.of(2015, 4, 30), LocalDate.of(2015, 5, 31), LocalDate.of(2015, 6, 30), LocalDate.of(2015, 7, 31),
      LocalDate.of(2015, 8, 31), LocalDate.of(2015, 9, 30), LocalDate.of(2015, 10, 31), LocalDate.of(2015, 11, 30),
      LocalDate.of(2015, 12, 31), LocalDate.of(2016, 1, 31) };
    double[] values = new double[] {
      255.4, 256.7, 257.1, 258.0, 258.5, 258.9, 258.6, 259.8, 259.6, 259.5, 259.8, 260.6, 258.8 };

    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < values.length; ++i) {
      if (dates[i].isBefore(valuationDate)) {
        builder.put(dates[i], values[i]);
      }
    }
    return builder.build();
  }

  /**
   * Obtains time series of price index up to valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the time series
   */
  public static LocalDateDoubleTimeSeries getTimeSeriesJp(LocalDate valuationDate) {
    LocalDate[] dates = new LocalDate[] {LocalDate.of(2013, 1, 31), LocalDate.of(2013, 2, 28),
      LocalDate.of(2013, 3, 31), LocalDate.of(2013, 4, 30), LocalDate.of(2013, 5, 31), LocalDate.of(2013, 6, 30),
      LocalDate.of(2013, 7, 31), LocalDate.of(2013, 8, 31), LocalDate.of(2013, 9, 30), LocalDate.of(2013, 10, 31),
      LocalDate.of(2013, 11, 30), LocalDate.of(2013, 12, 31), LocalDate.of(2014, 1, 31), LocalDate.of(2014, 2, 28),
      LocalDate.of(2014, 3, 31), LocalDate.of(2014, 4, 30), LocalDate.of(2014, 5, 31), LocalDate.of(2014, 6, 30),
      LocalDate.of(2014, 7, 31), LocalDate.of(2014, 8, 31), LocalDate.of(2014, 9, 30), LocalDate.of(2014, 10, 31),
      LocalDate.of(2014, 11, 30), LocalDate.of(2014, 12, 31), LocalDate.of(2015, 1, 31), LocalDate.of(2015, 2, 28),
      LocalDate.of(2015, 3, 31), LocalDate.of(2015, 4, 30), LocalDate.of(2015, 5, 31), LocalDate.of(2015, 6, 30),
      LocalDate.of(2015, 7, 31), LocalDate.of(2015, 8, 31), LocalDate.of(2015, 9, 30), LocalDate.of(2015, 10, 31),
      LocalDate.of(2015, 11, 30), LocalDate.of(2015, 12, 31), LocalDate.of(2016, 1, 31) };
    double[] values = new double[] {
      99.1, 99.2, 99.5, 99.8, 100, 100, 100.1, 100.4, 100.5, 100.7, 100.7, 100.6, 100.4, 100.5, 100.8, 103, 103.4,
      103.4, 103.5, 103.5, 103.5, 103.6, 103.4, 103.2, 102.6, 102.5, 103, 103.3, 103.4, 103.4, 103.4, 103.4, 103.4,
      103.5, 103.4, 103.3 };

    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < values.length; ++i) {
      if (dates[i].isBefore(valuationDate)) {
        builder.put(dates[i], values[i]);
      }
    }
    return builder.build();
  }
}
