/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Test.
 */
@Test(groups = TestGroup.UNIT)
public class MapMarketDataSourceTest {

  private static final ExternalIdBundle BUNDLE1 = ExternalIdBundle.of("A", "B");
  private static final ExternalIdBundle BUNDLE2 = ExternalIdBundle.of("A", "C");
  private static final FieldName FIELD = FieldName.of("other");

  public void testDefaultField() {
    assertEquals(MarketDataRequirementNames.MARKET_VALUE, MapMarketDataSource.DEFAULT_FIELD.getName());
  }

  public void testEmpty() {
    MarketDataSource test = MapMarketDataSource.of();
    Result<?> result = test.get(BUNDLE1, MapMarketDataSource.DEFAULT_FIELD);
    assertEquals(false, result.isSuccess());
    assertEquals(FailureStatus.MISSING_DATA, result.getStatus());
  }

  public void testSingleValue_found() {
    MarketDataSource test = MapMarketDataSource.of(BUNDLE1, Double.valueOf(12.34d));
    Result<?> result = test.get(BUNDLE1, MapMarketDataSource.DEFAULT_FIELD);
    assertEquals(true, result.isSuccess());
    assertEquals(12.34d, result.getValue());
  }

  public void testSingleValue_notFoundBundle() {
    MarketDataSource test = MapMarketDataSource.of(BUNDLE1, Double.valueOf(12.34d));
    Result<?> result = test.get(BUNDLE2, MapMarketDataSource.DEFAULT_FIELD);
    assertEquals(false, result.isSuccess());
    assertEquals(FailureStatus.MISSING_DATA, result.getStatus());
  }

  public void testSingleValue_notFoundField() {
    MarketDataSource test = MapMarketDataSource.of(BUNDLE1, Double.valueOf(12.34d));
    Result<?> result = test.get(BUNDLE1, FIELD);
    assertEquals(false, result.isSuccess());
    assertEquals(FailureStatus.MISSING_DATA, result.getStatus());
  }

}
