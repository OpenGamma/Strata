/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.SeasonalityDefinition;

/**
 * Test {@link SeasonalityDefinitionCsvLoader}.
 */
public class SeasonalityDefinitionCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/seasonality.csv";

  //-------------------------------------------------------------------------
  @Test
  public void test_loadSeasonalityDefinition() {
    Map<CurveName, SeasonalityDefinition> defns =
        SeasonalityDefinitionCsvLoader.loadSeasonalityDefinitions(ResourceLocator.of(GROUPS_1));
    assertThat(defns).hasSize(1);
    SeasonalityDefinition defn = defns.get(CurveName.of("USD-CPI"));
    assertThat(defn.getAdjustmentType()).isEqualTo(ShiftType.SCALED);
    assertThat(defn.getSeasonalityMonthOnMonth().size()).isEqualTo(12);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(SeasonalityDefinitionCsvLoader.class);
  }

}
