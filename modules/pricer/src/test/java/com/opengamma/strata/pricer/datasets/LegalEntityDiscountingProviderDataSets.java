/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;

import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.bond.RepoGroup;

/**
 * LegalEntityDiscountingProvider data sets for testing.
 */
public class LegalEntityDiscountingProviderDataSets {

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;

  //  =====     issuer curve + repo curve in USD      =====     
  private static final LocalDate VAL_DATE_USD = LocalDate.of(2011, 6, 20);
  private static final StandardId ISSUER_ID_USD = StandardId.of("OG-Ticker", "GOVT1");
  private static final CurveName NAME_REPO_USD = CurveName.of("TestRepoCurve");
  private static final CurveName NAME_ISSUER_USD = CurveName.of("TestIssuerCurve");
  /** time data for repo rate curve */
  public static final DoubleArray REPO_TIME_USD = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  /** zero rate data for repo rate curve */
  public static final DoubleArray REPO_RATE_USD = DoubleArray.of(0.0120, 0.0120, 0.0120, 0.0140, 0.0140, 0.0140);
  /** discount factor data for repo rate curve */
  public static final DoubleArray REPO_FACTOR_USD = DoubleArray.of(1.0, 0.9940, 0.9881, 0.9724, 0.9324, 0.8694);
  /** meta data of repo zero rate curve*/
  public static final CurveMetadata META_ZERO_REPO_USD = Curves.zeroRates(NAME_REPO_USD, ACT_ACT_ISDA);
  /** meta data of repo discount factor curve */
  public static final CurveMetadata META_SIMPLE_REPO_USD = Curves.discountFactors(NAME_REPO_USD, ACT_ACT_ISDA);
  /** time data for issuer curve */
  public static final DoubleArray ISSUER_TIME_USD = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  /** zero rate data for issuer curve */
  public static final DoubleArray ISSUER_RATE_USD = DoubleArray.of(0.0100, 0.0100, 0.0100, 0.0120, 0.0120, 0.0120);
  /** discount factor data for issuer curve */
  public static final DoubleArray ISSUER_FACTOR_USD = DoubleArray.of(1.0, 0.9950, 0.9900, 0.9763, 0.9418, 0.8869);
  /** meta data of issuer zero rate curve*/
  public static final CurveMetadata META_ZERO_ISSUER_USD = Curves.zeroRates(NAME_ISSUER_USD, ACT_ACT_ISDA);
  /** meta data of issuer discount factor curve */
  public static final CurveMetadata META_SIMPLE_ISSUER_USD = Curves.discountFactors(NAME_ISSUER_USD, ACT_ACT_ISDA);
  private static final RepoGroup GROUP_REPO_USD = RepoGroup.of("GOVT1 BONDS");
  private static final LegalEntityGroup GROUP_ISSUER_USD = LegalEntityGroup.of("GOVT1");
  // zero rate curves
  private static final InterpolatedNodalCurve CURVE_ZERO_REPO_USD =
      InterpolatedNodalCurve.of(META_ZERO_REPO_USD, REPO_TIME_USD, REPO_RATE_USD, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ZERO_REPO_USD =
      ZeroRateDiscountFactors.of(USD, VAL_DATE_USD, CURVE_ZERO_REPO_USD);
  private static final InterpolatedNodalCurve CURVE_ZERO_ISSUER_USD =
      InterpolatedNodalCurve.of(META_ZERO_ISSUER_USD, ISSUER_TIME_USD, ISSUER_RATE_USD, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ZERO_ISSUER_USD =
      ZeroRateDiscountFactors.of(USD, VAL_DATE_USD, CURVE_ZERO_ISSUER_USD);
  // discount factor curves
  private static final InterpolatedNodalCurve CURVE_SIMPLE_REPO =
      InterpolatedNodalCurve.of(META_SIMPLE_REPO_USD, REPO_TIME_USD, REPO_FACTOR_USD, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_SIMPLE_REPO =
      SimpleDiscountFactors.of(USD, VAL_DATE_USD, CURVE_SIMPLE_REPO);
  private static final InterpolatedNodalCurve CURVE_SIMPLE_ISSUER_USD =
      InterpolatedNodalCurve.of(META_SIMPLE_ISSUER_USD, ISSUER_TIME_USD, ISSUER_FACTOR_USD, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_SIMPLE_ISSUER_USD =
      SimpleDiscountFactors.of(USD, VAL_DATE_USD, CURVE_SIMPLE_ISSUER_USD);

  //  =====     issuer curve + repo curve in EUR      =====     
  private static final LocalDate VAL_DATE_EUR = LocalDate.of(2014, 3, 31);
  private static final StandardId ISSUER_ID_EUR = StandardId.of("OG-Ticker", "GOVT2");
  private static final CurveName NAME_REPO_EUR = CurveName.of("TestRepoCurve2");
  private static final CurveName NAME_ISSUER_EUR = CurveName.of("TestIssuerCurve2");
  /** time data for repo rate curve */
  public static final DoubleArray REPO_TIME_EUR = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  /** zero rate data for repo rate curve */
  public static final DoubleArray REPO_RATE_EUR = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  /** meta data of repo zero rate curve*/
  public static final CurveMetadata META_ZERO_REPO_EUR = Curves.zeroRates(NAME_REPO_EUR, ACT_ACT_ISDA);
  /** meta data of repo discount factor curve */
  public static final CurveMetadata META_SIMPLE_REPO_EUR = Curves.discountFactors(NAME_REPO_EUR, ACT_ACT_ISDA);
  /** time data for issuer curve */
  public static final DoubleArray ISSUER_TIME_EUR = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  /** zero rate data for issuer curve */
  public static final DoubleArray ISSUER_RATE_EUR = DoubleArray.of(0.0250, 0.0225, 0.0250, 0.0275, 0.0250, 0.0250);
  /** meta data of issuer zero rate curve*/
  public static final CurveMetadata META_ZERO_ISSUER_EUR = Curves.zeroRates(NAME_ISSUER_EUR, ACT_ACT_ISDA);
  /** meta data of issuer discount factor curve */
  public static final CurveMetadata META_SIMPLE_ISSUER_EUR = Curves.discountFactors(NAME_ISSUER_EUR, ACT_ACT_ISDA);
  private static final RepoGroup GROUP_REPO_EUR = RepoGroup.of("GOVT2 BONDS");
  private static final LegalEntityGroup GROUP_ISSUER_EUR = LegalEntityGroup.of("GOVT2");
  // zero rate curves
  private static final InterpolatedNodalCurve CURVE_ZERO_REPO_EUR =
      InterpolatedNodalCurve.of(META_ZERO_REPO_EUR, REPO_TIME_EUR, REPO_RATE_EUR, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ZERO_REPO_EUR =
      ZeroRateDiscountFactors.of(EUR, VAL_DATE_EUR, CURVE_ZERO_REPO_EUR);
  private static final InterpolatedNodalCurve CURVE_ZERO_ISSUER_EUR =
      InterpolatedNodalCurve.of(META_ZERO_ISSUER_EUR, ISSUER_TIME_EUR, ISSUER_RATE_EUR, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ZERO_ISSUER_EUR =
      ZeroRateDiscountFactors.of(EUR, VAL_DATE_EUR, CURVE_ZERO_ISSUER_EUR);

  /** provider with zero rate curves, USD */
  public static final LegalEntityDiscountingProvider ISSUER_REPO_ZERO = ImmutableLegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER_USD, USD), DSC_FACTORS_ZERO_ISSUER_USD))
      .issuerCurveGroups(ImmutableMap.of(ISSUER_ID_USD, GROUP_ISSUER_USD))
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_USD, USD), DSC_FACTORS_ZERO_REPO_USD))
      .repoCurveGroups(ImmutableMap.of(ISSUER_ID_USD, GROUP_REPO_USD))
      .build();
  /** provider with zero rate curves, EUR */
  public static final LegalEntityDiscountingProvider ISSUER_REPO_ZERO_EUR = ImmutableLegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER_EUR, EUR), DSC_FACTORS_ZERO_ISSUER_EUR))
      .issuerCurveGroups(ImmutableMap.of(ISSUER_ID_EUR, GROUP_ISSUER_EUR))
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_EUR, EUR), DSC_FACTORS_ZERO_REPO_EUR))
      .repoCurveGroups(ImmutableMap.of(ISSUER_ID_EUR, GROUP_REPO_EUR))
      .build();
  /** provider with discount factor curve, USD */
  public static final LegalEntityDiscountingProvider ISSUER_REPO_SIMPLE = ImmutableLegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER_USD, USD), DSC_FACTORS_SIMPLE_ISSUER_USD))
      .issuerCurveGroups(ImmutableMap.of(ISSUER_ID_USD, GROUP_ISSUER_USD))
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_USD, USD), DSC_FACTORS_SIMPLE_REPO))
      .repoCurveGroups(ImmutableMap.of(ISSUER_ID_USD, GROUP_REPO_USD))
      .build();
}
