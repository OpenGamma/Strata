/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link LegalEntityCurveGroup}.
 */
@Test
public class LegalEntityCurveGroupTest {

  private static final CurveGroupName NAME1 = CurveGroupName.of("TestGroup1");
  private static final CurveGroupName NAME2 = CurveGroupName.of("TestGroup2");
  private static final CurveName REPO_NAME = CurveName.of("Repo");
  private static final CurveName ISSUER_NAME1 = CurveName.of("Issuer1Gbp");
  private static final CurveName ISSUER_NAME2 = CurveName.of("Issuer1Usd");
  private static final CurveName ISSUER_NAME3 = CurveName.of("Issuer2");
  private static final RepoGroup REPO_GROUP = RepoGroup.of("RepoGroup");
  private static final LegalEntityGroup LEGAL_ENTITY_GROUP1 = LegalEntityGroup.of("LegalEntityGroup1");
  private static final LegalEntityGroup LEGAL_ENTITY_GROUP2 = LegalEntityGroup.of("LegalEntityGroup2");
  private static final Curve REPO_CURVE = ConstantCurve.of(REPO_NAME, 0.99);
  private static final Map<Pair<RepoGroup, Currency>, Curve> REPO_CURVES = ImmutableMap.of(
      Pair.of(REPO_GROUP, GBP), REPO_CURVE,
      Pair.of(REPO_GROUP, USD), REPO_CURVE);
  private static final Curve ISSUER_CURVE1 = ConstantCurve.of(ISSUER_NAME1, 0.5);
  private static final Curve ISSUER_CURVE2 = ConstantCurve.of(ISSUER_NAME2, 0.6);
  private static final Curve ISSUER_CURVE3 = ConstantCurve.of(ISSUER_NAME3, 0.7);
  private static final Map<Pair<LegalEntityGroup, Currency>, Curve> ISSUER_CURVES = ImmutableMap.of(
      Pair.of(LEGAL_ENTITY_GROUP1, GBP), ISSUER_CURVE1,
      Pair.of(LEGAL_ENTITY_GROUP1, USD), ISSUER_CURVE2,
      Pair.of(LEGAL_ENTITY_GROUP2, GBP), ISSUER_CURVE3);

  public void test_of() {
    LegalEntityCurveGroup test = LegalEntityCurveGroup.of(NAME1, REPO_CURVES, ISSUER_CURVES);
    assertEquals(test.getName(), NAME1);
    assertEquals(test.getRepoCurves(), REPO_CURVES);
    assertEquals(test.getIssuerCurves(), ISSUER_CURVES);
    assertEquals(test.findCurve(REPO_NAME).get(), REPO_CURVE);
    assertEquals(test.findCurve(ISSUER_NAME1).get(), ISSUER_CURVE1);
    assertEquals(test.findCurve(ISSUER_NAME2).get(), ISSUER_CURVE2);
    assertEquals(test.findCurve(ISSUER_NAME3).get(), ISSUER_CURVE3);
    assertFalse(test.findCurve(CurveName.of("foo")).isPresent());
    assertEquals(test.findRepoCurve(REPO_GROUP, GBP).get(), REPO_CURVE);
    assertEquals(test.findRepoCurve(REPO_GROUP, USD).get(), REPO_CURVE);
    assertFalse(test.findRepoCurve(REPO_GROUP, JPY).isPresent());
    assertEquals(test.findIssuerCurve(LEGAL_ENTITY_GROUP1, GBP).get(), ISSUER_CURVE1);
    assertEquals(test.findIssuerCurve(LEGAL_ENTITY_GROUP1, USD).get(), ISSUER_CURVE2);
    assertEquals(test.findIssuerCurve(LEGAL_ENTITY_GROUP2, GBP).get(), ISSUER_CURVE3);
    assertFalse(test.findIssuerCurve(LEGAL_ENTITY_GROUP2, USD).isPresent());
  }

  public void test_builder() {
    LegalEntityCurveGroup test = LegalEntityCurveGroup.builder()
        .name(NAME2)
        .repoCurves(REPO_CURVES)
        .issuerCurves(ISSUER_CURVES)
        .build();
    assertEquals(test.getName(), NAME2);
    assertEquals(test.getRepoCurves(), REPO_CURVES);
    assertEquals(test.getIssuerCurves(), ISSUER_CURVES);
  }

  public void stream() {
    LegalEntityCurveGroup test = LegalEntityCurveGroup.of(NAME1, REPO_CURVES, ISSUER_CURVES);
    List<Curve> expectedAll = ImmutableList.<Curve>builder()
        .add(REPO_CURVE)
        .add(ISSUER_CURVE1)
        .add(ISSUER_CURVE2)
        .add(ISSUER_CURVE3)
        .build();
    test.stream().collect(Collectors.toList()).containsAll(expectedAll);
    List<Curve> expectedRepo = ImmutableList.<Curve>builder()
        .add(REPO_CURVE)
        .build();
    test.repoCurveStream().collect(Collectors.toList()).containsAll(expectedRepo);
    List<Curve> expectedIssuer = ImmutableList.<Curve>builder()
        .add(ISSUER_CURVE1)
        .add(ISSUER_CURVE2)
        .add(ISSUER_CURVE3)
        .build();
    test.issuerCurveStream().collect(Collectors.toList()).containsAll(expectedIssuer);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LegalEntityCurveGroup test1 = LegalEntityCurveGroup.of(NAME1, REPO_CURVES, ISSUER_CURVES);
    coverImmutableBean(test1);
    LegalEntityCurveGroup test2 = LegalEntityCurveGroup.of(NAME1, ImmutableMap.of(), ImmutableMap.of());
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    LegalEntityCurveGroup test = LegalEntityCurveGroup.of(NAME1, REPO_CURVES, ISSUER_CURVES);
    assertSerialization(test);
  }

}
