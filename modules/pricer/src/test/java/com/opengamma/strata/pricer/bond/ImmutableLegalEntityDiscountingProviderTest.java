/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link ImmutableLegalEntityDiscountingProvider}.
 */
@Test
public class ImmutableLegalEntityDiscountingProviderTest {

  private static final LocalDate DATE = date(2015, 6, 4);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;

  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO =
      InterpolatedNodalCurve.of(METADATA_REPO, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DSC_FACTORS_REPO = ZeroRateDiscountFactors.of(GBP, DATE, CURVE_REPO);
  private static final RepoGroup GROUP_REPO_SECURITY = RepoGroup.of("ISSUER1 BND 5Y");
  private static final RepoGroup GROUP_REPO_ISSUER = RepoGroup.of("ISSUER1");
  private static final SecurityId ID_SECURITY = SecurityId.of("OG-Ticker", "Bond-5Y");

  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER =
      InterpolatedNodalCurve.of(METADATA_ISSUER, DoubleArray.of(0, 15), DoubleArray.of(1, 2.5), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DSC_FACTORS_ISSUER = ZeroRateDiscountFactors.of(GBP, DATE, CURVE_ISSUER);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("ISSUER1");
  private static final LegalEntityId ID_ISSUER = LegalEntityId.of("OG-Ticker", "Issuer-1");

  //-------------------------------------------------------------------------
  public void test_builder() {
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_SECURITY, GBP), DSC_FACTORS_REPO))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, GROUP_REPO_SECURITY))
        .valuationDate(DATE)
        .build();
    assertEquals(
        test.issuerCurveDiscountFactors(ID_ISSUER, GBP),
        IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    assertEquals(test.repoCurveDiscountFactors(ID_SECURITY, ID_ISSUER, GBP),
        RepoCurveDiscountFactors.of(DSC_FACTORS_REPO, GROUP_REPO_SECURITY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.repoCurveDiscountFactors(ID_ISSUER, GBP));
    assertEquals(test.getValuationDate(), DATE);
  }

  public void test_builder_noValuationDate() {
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_REPO_ISSUER))
        .build();
    assertEquals(
        test.issuerCurveDiscountFactors(ID_ISSUER, GBP),
        IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    assertEquals(test.repoCurveDiscountFactors(ID_SECURITY, ID_ISSUER, GBP),
        RepoCurveDiscountFactors.of(DSC_FACTORS_REPO, GROUP_REPO_ISSUER));
    assertEquals(test.repoCurveDiscountFactors(ID_ISSUER, GBP),
        RepoCurveDiscountFactors.of(DSC_FACTORS_REPO, GROUP_REPO_ISSUER));
    assertEquals(test.getValuationDate(), DATE);

  }

  public void test_builder_noRepoRate() {
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .build();
    assertEquals(
        test.issuerCurveDiscountFactors(ID_ISSUER, GBP),
        IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    assertEquals(test.getValuationDate(), DATE);
  }

  public void test_builder_fail() {
    // no relevant map for repo curve
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, RepoGroup.of("ISSUER2 BND 5Y")))
        .build());
    // no relevant map for issuer curve
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, LegalEntityGroup.of("ISSUER2")))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_REPO_ISSUER))
        .build());
    // issuer curve and valuation date are missing
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_SECURITY, GBP), DSC_FACTORS_REPO))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, GROUP_REPO_SECURITY))
        .build());
    // issuer curve date is different from valuation date
    DiscountFactors dscFactorIssuer = ZeroRateDiscountFactors.of(GBP, date(2015, 6, 14), CURVE_ISSUER);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), dscFactorIssuer))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_SECURITY, GBP), DSC_FACTORS_REPO))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, GROUP_REPO_SECURITY))
        .valuationDate(DATE)
        .build());
    // repo curve rate is different from valuation date
    DiscountFactors dscFactorRepo = ZeroRateDiscountFactors.of(GBP, date(2015, 6, 14), CURVE_REPO);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_SECURITY, GBP), dscFactorRepo))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, GROUP_REPO_SECURITY))
        .valuationDate(DATE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_discountFactor_notFound() {
    LegalEntityId issuerId = LegalEntityId.of("OG-Ticker", "Issuer-2");
    LegalEntityGroup issuerGroup = LegalEntityGroup.of("ISSUER2");
    RepoGroup repoGroup = RepoGroup.of("ISSUER2 BND 5Y");
    SecurityId securityId = SecurityId.of("OG-Ticker", "Issuer-2-bond-5Y");
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER, issuerId, issuerGroup))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_SECURITY, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(issuerId, repoGroup))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, GROUP_REPO_SECURITY))
        .valuationDate(DATE)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.issuerCurveDiscountFactors(ID_ISSUER, USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.issuerCurveDiscountFactors(LegalEntityId.of("OG-Ticker", "foo"), GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.issuerCurveDiscountFactors(issuerId, GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.repoCurveDiscountFactors(ID_SECURITY, ID_ISSUER, USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.repoCurveDiscountFactors(
        SecurityId.of("OG-Ticker", "foo-bond"), LegalEntityId.of("OG-Ticker", "foo"), GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.repoCurveDiscountFactors(securityId, issuerId, GBP));
  }

  public void test_curveParameterSensitivity() {
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_REPO_ISSUER))
        .valuationDate(DATE)
        .build();
    LocalDate refDate = date(2018, 11, 24);
    IssuerCurveZeroRateSensitivity sensi1 = test.issuerCurveDiscountFactors(ID_ISSUER, GBP)
        .zeroRatePointSensitivity(refDate, GBP);
    RepoCurveZeroRateSensitivity sensi2 = test.repoCurveDiscountFactors(ID_SECURITY, ID_ISSUER, GBP)
        .zeroRatePointSensitivity(refDate, GBP);
    PointSensitivities sensi = PointSensitivities.of(sensi1, sensi2);
    CurrencyParameterSensitivities computed = test.parameterSensitivity(sensi);
    CurrencyParameterSensitivities expected =
        DSC_FACTORS_ISSUER.parameterSensitivity(sensi1.createZeroRateSensitivity()).combinedWith(
            DSC_FACTORS_REPO.parameterSensitivity(sensi2.createZeroRateSensitivity()));
    assertTrue(computed.equalWithTolerance(expected, 1.0e-12));
  }

  public void test_curveParameterSensitivity_noSensi() {
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_REPO_ISSUER))
        .valuationDate(DATE)
        .build();
    ZeroRateSensitivity sensi =
        ZeroRateSensitivity.of(USD, DSC_FACTORS_ISSUER.relativeYearFraction(date(2018, 11, 24)), 25d);
    CurrencyParameterSensitivities computed = test.parameterSensitivity(sensi.build());
    assertEquals(computed, CurrencyParameterSensitivities.empty());
  }

  public void test_findData() {
    ImmutableLegalEntityDiscountingProvider test = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_REPO_ISSUER))
        .valuationDate(DATE)
        .build();
    assertEquals(test.findData(DSC_FACTORS_ISSUER.getCurve().getName()), Optional.of(DSC_FACTORS_ISSUER.getCurve()));
    assertEquals(test.findData(DSC_FACTORS_REPO.getCurve().getName()), Optional.of(DSC_FACTORS_REPO.getCurve()));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableLegalEntityDiscountingProvider test1 = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .issuerCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO_ISSUER, GBP), DSC_FACTORS_REPO))
        .repoCurveGroups(ImmutableMap.of(ID_ISSUER, GROUP_REPO_ISSUER))
        .build();
    coverImmutableBean(test1);
    LocalDate val = date(2015, 6, 14);
    DiscountFactors dscFactorIssuer = ZeroRateDiscountFactors.of(GBP, val, CURVE_ISSUER);
    DiscountFactors dscFactorRepo = ZeroRateDiscountFactors.of(GBP, val, CURVE_REPO);
    ImmutableLegalEntityDiscountingProvider test2 = ImmutableLegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, GBP), dscFactorIssuer))
        .issuerCurveGroups(ImmutableMap.of(LegalEntityId.of("OG-Ticker", "foo"), GROUP_ISSUER))
        .repoCurves(ImmutableMap.of(Pair.of(RepoGroup.of("ISSUER2 BND 5Y"), GBP), dscFactorRepo))
        .repoCurveSecurityGroups(ImmutableMap.of(ID_SECURITY, RepoGroup.of("ISSUER2 BND 5Y")))
        .build();
    coverBeanEquals(test1, test2);
  }

}
