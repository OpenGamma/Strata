/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.bond.BondFutureVolatilities;
import com.opengamma.strata.pricer.bond.BondFutureVolatilitiesId;
import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link BondFutureOptionMarketDataLookup}.
 */
@Test
public class BondFutureOptionMarketDataLookupTest {

  private static final BondFutureVolatilitiesId VOL_ID1 = BondFutureVolatilitiesId.of("ID1");
  private static final BondFutureVolatilities MOCK_VOLS = mock(BondFutureVolatilities.class);
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);
  private static final SecurityId SEC_OG1 = SecurityId.of("OG", "1");
  private static final SecurityId SEC_OG2 = SecurityId.of("OG", "2");
  private static final SecurityId SEC_OG3 = SecurityId.of("OG", "3");

  static {
    when(MOCK_MARKET_DATA.getValue(VOL_ID1)).thenReturn(MOCK_VOLS);
  }

  //-------------------------------------------------------------------------
  public void test_of_single() {
    BondFutureOptionMarketDataLookup test = BondFutureOptionMarketDataLookup.of(SEC_OG1, VOL_ID1);
    assertEquals(test.queryType(), BondFutureOptionMarketDataLookup.class);
    assertEquals(test.getVolatilitySecurityIds(), ImmutableSet.of(SEC_OG1));
    assertEquals(test.getVolatilityIds(SEC_OG1), ImmutableSet.of(VOL_ID1));
    assertThrowsIllegalArg(() -> test.getVolatilityIds(SEC_OG2));

    assertEquals(
        test.requirements(SEC_OG1),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertEquals(
        test.requirements(ImmutableSet.of(SEC_OG1)),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(SEC_OG3)));
  }

  public void test_of_map() {
    ImmutableMap<SecurityId, BondFutureVolatilitiesId> ids = ImmutableMap.of(SEC_OG1, VOL_ID1, SEC_OG2, VOL_ID1);
    BondFutureOptionMarketDataLookup test = BondFutureOptionMarketDataLookup.of(ids);
    assertEquals(test.queryType(), BondFutureOptionMarketDataLookup.class);
    assertEquals(test.getVolatilitySecurityIds(), ImmutableSet.of(SEC_OG1, SEC_OG2));
    assertEquals(test.getVolatilityIds(SEC_OG1), ImmutableSet.of(VOL_ID1));
    assertThrowsIllegalArg(() -> test.getVolatilityIds(SEC_OG3));

    assertEquals(
        test.requirements(SEC_OG1),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertEquals(
        test.requirements(ImmutableSet.of(SEC_OG1)),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(SEC_OG3)));

    assertEquals(test.volatilities(SEC_OG1, MOCK_MARKET_DATA), MOCK_VOLS);
    assertThrowsIllegalArg(() -> test.volatilities(SEC_OG3, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  public void test_marketDataView() {
    BondFutureOptionMarketDataLookup test = BondFutureOptionMarketDataLookup.of(SEC_OG1, VOL_ID1);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    BondFutureOptionScenarioMarketData multiScenario = test.marketDataView(md);
    assertEquals(multiScenario.getLookup(), test);
    assertEquals(multiScenario.getMarketData(), md);
    assertEquals(multiScenario.getScenarioCount(), 1);
    BondFutureOptionMarketData scenario = multiScenario.scenario(0);
    assertEquals(scenario.getLookup(), test);
    assertEquals(scenario.getMarketData(), md.scenario(0));
    assertEquals(scenario.getValuationDate(), valDate);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultBondFutureOptionMarketDataLookup test =
        DefaultBondFutureOptionMarketDataLookup.of(ImmutableMap.of(SEC_OG1, VOL_ID1, SEC_OG2, VOL_ID1));
    coverImmutableBean(test);
    DefaultBondFutureOptionMarketDataLookup test2 = DefaultBondFutureOptionMarketDataLookup.of(SEC_OG1, VOL_ID1);
    coverBeanEquals(test, test2);

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
  }

  public void test_serialization() {
    DefaultBondFutureOptionMarketDataLookup test =
        DefaultBondFutureOptionMarketDataLookup.of(ImmutableMap.of(SEC_OG1, VOL_ID1, SEC_OG2, VOL_ID1));
    assertSerialization(test);
  }

}
