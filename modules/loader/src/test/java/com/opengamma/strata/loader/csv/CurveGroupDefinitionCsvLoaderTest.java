/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurveGroupDefinitionCsvLoader}.
 */
@Test
public class CurveGroupDefinitionCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/groups.csv";
  private static final String SETTINGS_1 = "classpath:com/opengamma/strata/loader/csv/settings.csv";
  private static final String CURVES_1 = "classpath:com/opengamma/strata/loader/csv/curves-1.csv";
  private static final String CURVES_2 = "classpath:com/opengamma/strata/loader/csv/curves-2.csv";
  private static final LocalDate CURVE_DATE = LocalDate.of(2009, 7, 31);

  //-------------------------------------------------------------------------
  public void test_loadCurveGroupDefinition() {
    List<CurveGroupDefinition> defns = CurveGroupDefinitionCsvLoader.loadCurveGroupDefinitions(ResourceLocator.of(GROUPS_1));
    assertEquals(defns.size(), 1);
    CurveGroupDefinition defn = defns.get(0);
    assertEquals(defn.getEntries().get(0), CurveGroupEntry.builder()
        .curveName(CurveName.of("USD-Disc"))
        .discountCurrencies(USD)
        .build());
    assertEquals(defn.getEntries().get(1), CurveGroupEntry.builder()
        .curveName(CurveName.of("USD-3ML"))
        .indices(USD_LIBOR_3M)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_writeCurveGroupDefinition() {
    CurveGroupDefinition defn = CurveGroupDefinitionCsvLoader.loadCurveGroupDefinitions(ResourceLocator.of(GROUPS_1)).get(0);
    Appendable underlying = new StringBuilder();
    CurveGroupDefinitionCsvLoader.writeCurveGroupDefinition(underlying, defn);
    String created = underlying.toString();
    String expected =
        "Group Name,Curve Type,Reference,Curve Name" + System.lineSeparator() +
            "Default,discount,USD,USD-Disc" + System.lineSeparator() +
            "Default,forward,USD-LIBOR-3M,USD-3ML" + System.lineSeparator();
    assertEquals(created, expected);
  }

  public void test_writeCurveGroup() {
    List<CurveGroup> curveGroups = RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
    Appendable underlying = new StringBuilder();
    CurveGroupDefinitionCsvLoader.writeCurveGroup(underlying, curveGroups.get(0));
    String created = underlying.toString();
    String expected =
        "Group Name,Curve Type,Reference,Curve Name" + System.lineSeparator() +
            "Default,discount,USD,USD-Disc" + System.lineSeparator() +
            "Default,forward,USD-LIBOR-3M,USD-3ML" + System.lineSeparator();
    assertEquals(created, expected);
  }

  public void test_test_writeCurveGroupDefinition_roundtrip() throws Exception {
    List<CurveGroupDefinition> defn = CurveGroupDefinitionCsvLoader.loadCurveGroupDefinitions(ResourceLocator.of(GROUPS_1));
    File tempFile = File.createTempFile("TestCurveGroupLoading", "csv");
    tempFile.deleteOnExit();
    CurveGroupDefinitionCsvLoader.writeCurveGroupDefinition(tempFile, defn.get(0));
    assertEquals(CurveGroupDefinitionCsvLoader.loadCurveGroupDefinitions(ResourceLocator.ofFile(tempFile)), defn);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CurveGroupDefinitionCsvLoader.class);
  }

}
