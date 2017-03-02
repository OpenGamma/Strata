/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.bond.IssuerCurveDiscountFactors;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.bond.RepoCurveDiscountFactors;
import com.opengamma.strata.pricer.bond.RepoGroup;
import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link LegalEntityDiscountingMarketDataLookup}.
 */
@Test
public class LegalEntityDiscountingMarketDataLookupTest {

  private static final SecurityId SEC_A1 = SecurityId.of("OG-Bond", "A1");
  private static final SecurityId SEC_A2 = SecurityId.of("OG-Bond", "A2");
  private static final SecurityId SEC_B1 = SecurityId.of("OG-Bond", "B1");
  private static final SecurityId SEC_C1 = SecurityId.of("OG-Bond", "C1");
  private static final SecurityId SEC_D1 = SecurityId.of("OG-Bond", "D1");
  private static final StandardId ISSUER_A = StandardId.of("OG-LegEnt", "A");
  private static final StandardId ISSUER_B = StandardId.of("OG-LegEnt", "B");
  private static final StandardId ISSUER_C = StandardId.of("OG-LegEnt", "C");
  private static final StandardId ISSUER_D = StandardId.of("OG-LegEnt", "D");
  private static final RepoGroup GROUP_REPO_X = RepoGroup.of("X");
  private static final RepoGroup GROUP_REPO_Y = RepoGroup.of("Y");
  private static final LegalEntityGroup GROUP_ISSUER_M = LegalEntityGroup.of("M");
  private static final LegalEntityGroup GROUP_ISSUER_N = LegalEntityGroup.of("N");

  private static final CurveId CURVE_ID_USD1 = CurveId.of("Group", "USD1");
  private static final CurveId CURVE_ID_USD2 = CurveId.of("Group", "USD2");
  private static final CurveId CURVE_ID_USD3 = CurveId.of("Group", "USD3");
  private static final CurveId CURVE_ID_USD4 = CurveId.of("Group", "USD4");
  private static final CurveId CURVE_ID_GBP1 = CurveId.of("Group", "GBP1");
  private static final CurveId CURVE_ID_GBP2 = CurveId.of("Group", "GBP2");
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  //-------------------------------------------------------------------------
  public void test_of_map() {
    ImmutableMap<StandardId, RepoGroup> repoGroups = ImmutableMap.of(
        SEC_A1.getStandardId(), GROUP_REPO_X,
        ISSUER_A, GROUP_REPO_Y,
        ISSUER_B, GROUP_REPO_Y,
        ISSUER_C, GROUP_REPO_Y,
        ISSUER_D, GROUP_REPO_Y);
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.of(
        Pair.of(GROUP_REPO_X, USD), CURVE_ID_USD1,
        Pair.of(GROUP_REPO_Y, USD), CURVE_ID_USD2,
        Pair.of(GROUP_REPO_Y, GBP), CURVE_ID_GBP1);

    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
        ISSUER_A, GROUP_ISSUER_M,
        ISSUER_B, GROUP_ISSUER_N,
        ISSUER_C, GROUP_ISSUER_M);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.of(
        Pair.of(GROUP_ISSUER_M, USD), CURVE_ID_USD3,
        Pair.of(GROUP_ISSUER_N, USD), CURVE_ID_USD4,
        Pair.of(GROUP_ISSUER_N, GBP), CURVE_ID_GBP2);

    LegalEntityDiscountingMarketDataLookup test =
        LegalEntityDiscountingMarketDataLookup.of(repoGroups, repoCurves, issuerGroups, issuerCurves);
    assertEquals(test.queryType(), LegalEntityDiscountingMarketDataLookup.class);

    assertEquals(
        test.requirements(SEC_A1, ISSUER_A, USD),
        FunctionRequirements.builder().valueRequirements(CURVE_ID_USD1, CURVE_ID_USD3).outputCurrencies(USD).build());
    assertEquals(
        test.requirements(SEC_A2, ISSUER_A, USD),
        FunctionRequirements.builder().valueRequirements(CURVE_ID_USD2, CURVE_ID_USD3).outputCurrencies(USD).build());
    assertEquals(
        test.requirements(SEC_B1, ISSUER_B, USD),
        FunctionRequirements.builder().valueRequirements(CURVE_ID_USD2, CURVE_ID_USD4).outputCurrencies(USD).build());
    assertEquals(
        test.requirements(SEC_B1, ISSUER_B, GBP),
        FunctionRequirements.builder().valueRequirements(CURVE_ID_GBP1, CURVE_ID_GBP2).outputCurrencies(GBP).build());
    assertThrowsIllegalArg(() -> test.requirements(SEC_B1, StandardId.of("XXX", "XXX"), GBP));
    assertThrowsIllegalArg(() -> test.requirements(SecurityId.of("XXX", "XXX"), StandardId.of("XXX", "XXX"), GBP));
    assertThrowsIllegalArg(() -> test.requirements(SEC_A1, ISSUER_A, GBP));
    assertThrowsIllegalArg(() -> test.requirements(SEC_C1, ISSUER_C, GBP));
    assertThrowsIllegalArg(() -> test.requirements(SEC_D1, ISSUER_D, GBP));

    assertEquals(
        test.discountingProvider(MOCK_MARKET_DATA),
        DefaultLookupLegalEntityDiscountingProvider.of((DefaultLegalEntityDiscountingMarketDataLookup) test, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  public void test_of_map_invalid() {
    ImmutableMap<StandardId, RepoGroup> repoGroups = ImmutableMap.of(
        SEC_A1.getStandardId(), GROUP_REPO_X);
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.of(
        Pair.of(GROUP_REPO_X, USD), CURVE_ID_USD1);

    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
        ISSUER_A, GROUP_ISSUER_M);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.of(
        Pair.of(GROUP_ISSUER_M, USD), CURVE_ID_USD3);

    assertThrowsIllegalArg(() -> LegalEntityDiscountingMarketDataLookup.of(
        repoGroups, ImmutableMap.of(), issuerGroups, issuerCurves));
    assertThrowsIllegalArg(() -> LegalEntityDiscountingMarketDataLookup.of(
        repoGroups, repoCurves, issuerGroups, ImmutableMap.of()));
  }

  //-------------------------------------------------------------------------
  public void test_marketDataView() {
    ImmutableMap<StandardId, RepoGroup> repoGroups = ImmutableMap.of(
        SEC_A1.getStandardId(), GROUP_REPO_X);
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.of(
        Pair.of(GROUP_REPO_X, USD), CURVE_ID_USD1);

    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
        ISSUER_A, GROUP_ISSUER_M);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.of(
        Pair.of(GROUP_ISSUER_M, USD), CURVE_ID_USD3);

    LegalEntityDiscountingMarketDataLookup test =
        LegalEntityDiscountingMarketDataLookup.of(repoGroups, repoCurves, issuerGroups, issuerCurves);

    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    LegalEntityDiscountingScenarioMarketData multiScenario = test.marketDataView(md);
    assertEquals(multiScenario.getLookup(), test);
    assertEquals(multiScenario.getMarketData(), md);
    assertEquals(multiScenario.getScenarioCount(), 1);
    LegalEntityDiscountingMarketData scenario = multiScenario.scenario(0);
    assertEquals(scenario.getLookup(), test);
    assertEquals(scenario.getMarketData(), md.scenario(0));
    assertEquals(scenario.getValuationDate(), valDate);
  }

  public void test_bondDiscountingProvider() {
    ImmutableMap<StandardId, RepoGroup> repoGroups = ImmutableMap.of(
        SEC_A1.getStandardId(), GROUP_REPO_X, ISSUER_B, GROUP_REPO_X);
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.of(
        Pair.of(GROUP_REPO_X, USD), CURVE_ID_USD1);

    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
        ISSUER_A, GROUP_ISSUER_M);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.of(
        Pair.of(GROUP_ISSUER_M, USD), CURVE_ID_USD3);

    LegalEntityDiscountingMarketDataLookup test =
        LegalEntityDiscountingMarketDataLookup.of(repoGroups, repoCurves, issuerGroups, issuerCurves);
    LocalDate valDate = date(2015, 6, 30);
    Curve repoCurve = ConstantCurve.of(Curves.discountFactors(CURVE_ID_USD1.getCurveName(), ACT_360), 1d);
    Curve issuerCurve = ConstantCurve.of(Curves.discountFactors(CURVE_ID_USD3.getCurveName(), ACT_360), 2d);
    MarketData md = ImmutableMarketData.of(valDate, ImmutableMap.of(CURVE_ID_USD1, repoCurve, CURVE_ID_USD3, issuerCurve));
    LegalEntityDiscountingProvider provider = test.discountingProvider(md);

    assertEquals(provider.getValuationDate(), valDate);
    assertEquals(provider.findData(CURVE_ID_USD1.getCurveName()), Optional.of(repoCurve));
    assertEquals(provider.findData(CURVE_ID_USD3.getCurveName()), Optional.of(issuerCurve));
    assertEquals(provider.findData(CurveName.of("Rubbish")), Optional.empty());
    // check repo
    RepoCurveDiscountFactors rcdf = provider.repoCurveDiscountFactors(SEC_A1, ISSUER_A, USD);
    SimpleDiscountFactors rdf = (SimpleDiscountFactors) rcdf.getDiscountFactors();
    assertEquals(rdf.getCurve().getName(), repoCurve.getName());
    assertEquals(rcdf, provider.repoCurveDiscountFactors(SEC_B1, ISSUER_B, USD));
    assertThrowsIllegalArg(() -> provider.repoCurveDiscountFactors(SEC_A1, ISSUER_A, GBP));
    assertThrowsIllegalArg(() -> provider.repoCurveDiscountFactors(SEC_C1, ISSUER_C, USD));
    // check issuer
    IssuerCurveDiscountFactors icdf = provider.issuerCurveDiscountFactors(ISSUER_A, USD);
    SimpleDiscountFactors idf = (SimpleDiscountFactors) icdf.getDiscountFactors();
    assertEquals(idf.getCurve().getName(), issuerCurve.getName());
    assertThrowsIllegalArg(() -> provider.issuerCurveDiscountFactors(ISSUER_A, GBP));
    assertThrowsIllegalArg(() -> provider.issuerCurveDiscountFactors(ISSUER_C, USD));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableMap<StandardId, RepoGroup> repoGroups = ImmutableMap.of(
        SEC_A1.getStandardId(), GROUP_REPO_X, ISSUER_A, GROUP_REPO_Y, ISSUER_B, GROUP_REPO_Y);
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.of(
        Pair.of(GROUP_REPO_X, USD), CURVE_ID_USD1,
        Pair.of(GROUP_REPO_Y, USD), CURVE_ID_USD2,
        Pair.of(GROUP_REPO_Y, GBP), CURVE_ID_GBP1);

    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
        ISSUER_A, GROUP_ISSUER_M, ISSUER_B, GROUP_ISSUER_N);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.of(
        Pair.of(GROUP_ISSUER_M, USD), CURVE_ID_USD3,
        Pair.of(GROUP_ISSUER_N, USD), CURVE_ID_USD4,
        Pair.of(GROUP_ISSUER_N, GBP), CURVE_ID_GBP2);

    LegalEntityDiscountingMarketDataLookup test =
        LegalEntityDiscountingMarketDataLookup.of(repoGroups, repoCurves, issuerGroups, issuerCurves);
    coverImmutableBean((ImmutableBean) test);

    ImmutableMap<StandardId, RepoGroup> repoGroups2 = ImmutableMap.of();
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves2 = ImmutableMap.of();
    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups2 = ImmutableMap.of();
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves2 = ImmutableMap.of();

    LegalEntityDiscountingMarketDataLookup test2 =
        LegalEntityDiscountingMarketDataLookup.of(repoGroups2, repoCurves2, issuerGroups2, issuerCurves2, OBS_SOURCE);
    coverBeanEquals((ImmutableBean) test, (ImmutableBean) test2);

    // related coverage
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    DefaultLegalEntityDiscountingScenarioMarketData.meta();

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
    DefaultLegalEntityDiscountingMarketData.meta();

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA).discountingProvider());
    DefaultLookupLegalEntityDiscountingProvider.meta();
  }

  public void test_serialization() {
    ImmutableMap<StandardId, RepoGroup> repoGroups = ImmutableMap.of(
        SEC_A1.getStandardId(), GROUP_REPO_X, ISSUER_A, GROUP_REPO_Y, ISSUER_B, GROUP_REPO_Y);
    ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.of(
        Pair.of(GROUP_REPO_X, USD), CURVE_ID_USD1,
        Pair.of(GROUP_REPO_Y, USD), CURVE_ID_USD2,
        Pair.of(GROUP_REPO_Y, GBP), CURVE_ID_GBP1);

    ImmutableMap<StandardId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
        ISSUER_A, GROUP_ISSUER_M, ISSUER_B, GROUP_ISSUER_N);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.of(
        Pair.of(GROUP_ISSUER_M, USD), CURVE_ID_USD3,
        Pair.of(GROUP_ISSUER_N, USD), CURVE_ID_USD4,
        Pair.of(GROUP_ISSUER_N, GBP), CURVE_ID_GBP2);

    LegalEntityDiscountingMarketDataLookup test =
        LegalEntityDiscountingMarketDataLookup.of(repoGroups, repoCurves, issuerGroups, issuerCurves);
    assertSerialization(test);
  }

}
