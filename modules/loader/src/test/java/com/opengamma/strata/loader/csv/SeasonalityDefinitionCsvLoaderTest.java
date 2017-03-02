/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.SeasonalityDefinition;

/**
 * Test {@link SeasonalityDefinitionCsvLoader}.
 */
@Test
public class SeasonalityDefinitionCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/seasonality.csv";

  //-------------------------------------------------------------------------
  public void test_loadSeasonalityDefinition() {
    Map<CurveName, SeasonalityDefinition> defns =
        SeasonalityDefinitionCsvLoader.loadSeasonalityDefinitions(ResourceLocator.of(GROUPS_1));
    assertEquals(defns.size(), 1);
    SeasonalityDefinition defn = defns.get(CurveName.of("USD-CPI"));
    assertEquals(defn.getAdjustmentType(), ShiftType.SCALED);
    assertEquals(defn.getSeasonalityMonthOnMonth().size(), 12);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SeasonalityDefinitionCsvLoader.class);
  }

}
