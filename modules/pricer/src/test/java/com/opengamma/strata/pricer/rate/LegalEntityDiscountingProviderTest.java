/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.IssuerCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.RepoCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.BondGroup;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.value.LegalEntityGroup;
import com.opengamma.strata.market.value.RepoCurveDiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Test {@link LegalEntityDiscountingProvider}.
 */
@Test
public class LegalEntityDiscountingProviderTest {

  private static final LocalDate DATE = date(2015, 6, 4);
  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;

  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO =
      InterpolatedNodalCurve.of(METADATA_REPO, new double[] {0, 10 }, new double[] {1, 2 }, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_REPO = ZeroRateDiscountFactors.of(GBP, DATE, CURVE_REPO);
  private static final BondGroup GROUP_REPO = BondGroup.of("ISSUER1 BND 5Y");
  private static final ImmutableList<StandardId> LIST_REPO = ImmutableList.<StandardId>of(
      StandardId.of("OG-Ticker", "Issuer-1"), StandardId.of("OG-Ticker", "Bond-5Y"));

  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER =
      InterpolatedNodalCurve.of(METADATA_ISSUER, new double[] {0, 15 }, new double[] {1, 2.5 }, INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ISSUER = ZeroRateDiscountFactors.of(GBP, DATE, CURVE_ISSUER);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("ISSUER1");
  private static final StandardId ID_ISSUER = StandardId.of("OG-Ticker", "Issuer-1");

  public void test_builder() {
    LegalEntityDiscountingProvider test = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .valuationDate(DATE)
        .build();
    assertEquals(test.issuerCurveDiscountFactors(ID_ISSUER, GBP),
        IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    assertEquals(test.repoCurveDiscountFactors(LIST_REPO, GBP),
        RepoCurveDiscountFactors.of(DSC_FACTORS_REPO, GROUP_REPO));
    assertEquals(test.getValuationDate(), DATE);
  }

  public void test_builder_noValuationDate() {
    LegalEntityDiscountingProvider test = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .build();
    assertEquals(test.issuerCurveDiscountFactors(ID_ISSUER, GBP),
        IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    assertEquals(test.repoCurveDiscountFactors(LIST_REPO, GBP),
        RepoCurveDiscountFactors.of(DSC_FACTORS_REPO, GROUP_REPO));
    assertEquals(test.getValuationDate(), DATE);

  }

  public void test_builder_noRepoRate() {
    LegalEntityDiscountingProvider test = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .build();
    assertEquals(test.issuerCurveDiscountFactors(ID_ISSUER, GBP),
        IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    assertEquals(test.getValuationDate(), DATE);
  }

  public void test_builder_fail() {
    // no relevant map for repo curve
    assertThrowsIllegalArg(() -> LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, BondGroup.of("ISSUER2 BND 5Y")))
        .build());
    // no relevant map for issuer curve
    assertThrowsIllegalArg(() -> LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, LegalEntityGroup.of("ISSUER2")))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .build());
    // issuer curve is missing
    assertThrowsIllegalArg(() -> LegalEntityDiscountingProvider.builder()
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .valuationDate(DATE)
        .build());
    // issuer curve and valuation date are missing
    assertThrowsIllegalArg(() -> LegalEntityDiscountingProvider.builder()
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .build());
    // issuer curve date is different from valuation date
    DiscountFactors dscFactorIssuer = ZeroRateDiscountFactors.of(GBP, date(2015, 6, 14), CURVE_ISSUER);
    assertThrowsIllegalArg(() -> LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), dscFactorIssuer))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .valuationDate(DATE)
        .build());
    // repo curve rate is different from valuation date
    DiscountFactors dscFactorRepo = ZeroRateDiscountFactors.of(GBP, date(2015, 6, 14), CURVE_REPO);
    assertThrowsIllegalArg(() -> LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), dscFactorRepo))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .valuationDate(DATE)
        .build());
  }

  public void test_discountFactor_notFound() {
    StandardId issuerId = StandardId.of("OG-Ticker", "Issuer-2");
    LegalEntityGroup issuerGroup = LegalEntityGroup.of("ISSUER2");
    BondGroup bondGroup = BondGroup.of("ISSUER2 BND 5Y");
    ImmutableList<StandardId> idList = ImmutableList.<StandardId>of(
        StandardId.of("OG-Ticker", "Issuer-2"), StandardId.of("OG-Ticker", "Bond-5Y"));
    LegalEntityDiscountingProvider test = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER, issuerId, issuerGroup))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO, idList, bondGroup))
        .valuationDate(DATE)
        .build();
    assertThrowsIllegalArg(() -> test.issuerCurveDiscountFactors(ID_ISSUER, USD));
    assertThrowsIllegalArg(() -> test.issuerCurveDiscountFactors(StandardId.of("OG-Ticker", "foo"), GBP));
    assertThrowsIllegalArg(() -> test.issuerCurveDiscountFactors(issuerId, GBP));
    assertThrowsIllegalArg(() -> test.repoCurveDiscountFactors(LIST_REPO, USD));
    assertThrowsIllegalArg(() -> test.repoCurveDiscountFactors(
        ImmutableList.<StandardId>of(StandardId.of("OG-Ticker", "foo")), GBP));
    assertThrowsIllegalArg(() -> test.repoCurveDiscountFactors(idList, GBP));
  }

  public void test_curveParameterSensitivity() {
    LegalEntityDiscountingProvider test = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .valuationDate(DATE)
        .build();
    LocalDate refDate = date(2018, 11, 24);
    IssuerCurveZeroRateSensitivity sensi1 = test.issuerCurveDiscountFactors(ID_ISSUER, GBP)
        .zeroRatePointSensitivity(refDate, GBP);
    RepoCurveZeroRateSensitivity sensi2 = test.repoCurveDiscountFactors(LIST_REPO, GBP)
        .zeroRatePointSensitivity(refDate, GBP);
    PointSensitivities sensi = PointSensitivities.of(sensi1, sensi2);
    CurveCurrencyParameterSensitivities computed = test.curveParameterSensitivity(sensi);
    CurveCurrencyParameterSensitivities expected =
        DSC_FACTORS_ISSUER.curveParameterSensitivity(sensi1.createZeroRateSensitivity()).combinedWith(
            DSC_FACTORS_REPO.curveParameterSensitivity(sensi2.createZeroRateSensitivity()));
    assertTrue(computed.equalWithTolerance(expected, 1.0e-12));
  }

  public void test_curveParameterSensitivity_noSensi() {
    LegalEntityDiscountingProvider test = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .valuationDate(DATE)
        .build();
    ZeroRateSensitivity sensi = ZeroRateSensitivity.of(USD, date(2018, 11, 24), 25d);
    CurveCurrencyParameterSensitivities computed = test.curveParameterSensitivity(sensi.build());
    assertEquals(computed, CurveCurrencyParameterSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LegalEntityDiscountingProvider test1 = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), DSC_FACTORS_ISSUER))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ID_ISSUER, GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(GROUP_REPO, GBP), DSC_FACTORS_REPO))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, GROUP_REPO))
        .build();
    coverImmutableBean(test1);
    LocalDate val = date(2015, 6, 14);
    DiscountFactors dscFactorIssuer = ZeroRateDiscountFactors.of(GBP, val, CURVE_ISSUER);
    DiscountFactors dscFactorRepo = ZeroRateDiscountFactors.of(GBP, val, CURVE_REPO);
    LegalEntityDiscountingProvider test2 = LegalEntityDiscountingProvider.builder()
        .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
            Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, GBP), dscFactorIssuer))
        .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(StandardId.of("OG-Ticker", "foo"), GROUP_ISSUER))
        .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
            Pair.<BondGroup, Currency>of(BondGroup.of("ISSUER2 BND 5Y"), GBP), dscFactorRepo))
        .BondMap(ImmutableMap.<List<StandardId>, BondGroup>of(LIST_REPO, BondGroup.of("ISSUER2 BND 5Y")))
        .build();
    coverBeanEquals(test1, test2);
  }
}
