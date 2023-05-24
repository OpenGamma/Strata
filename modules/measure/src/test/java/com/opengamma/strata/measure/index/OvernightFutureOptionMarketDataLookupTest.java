/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_SOFR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilitiesId;

/**
 * Test {@link OvernightFutureOptionMarketDataLookup}.
 */
public class OvernightFutureOptionMarketDataLookupTest {

  private static final OvernightFutureOptionVolatilitiesId VOL_ID1 = OvernightFutureOptionVolatilitiesId.of("USD1");
  private static final OvernightFutureOptionVolatilities MOCK_VOLS = mock(OvernightFutureOptionVolatilities.class);
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  static {
    when(MOCK_MARKET_DATA.getValue(VOL_ID1)).thenReturn(MOCK_VOLS);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_single() {
    OvernightFutureOptionMarketDataLookup test = OvernightFutureOptionMarketDataLookup.of(USD_SOFR, VOL_ID1);
    assertThat(test.queryType()).isEqualTo(OvernightFutureOptionMarketDataLookup.class);
    assertThat(test.getVolatilityIndices()).containsOnly(USD_SOFR);
    assertThat(test.getVolatilityIds(USD_SOFR)).containsOnly(VOL_ID1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getVolatilityIds(GBP_SONIA));

    assertThat(test.requirements(USD_SOFR)).isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThat(test.requirements(ImmutableSet.of(USD_SOFR)))
        .isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(GBP_SONIA)));
  }

  @Test
  public void test_of_map() {
    ImmutableMap<OvernightIndex, OvernightFutureOptionVolatilitiesId> ids =
        ImmutableMap.of(USD_SOFR, VOL_ID1, USD_FED_FUND, VOL_ID1);
    OvernightFutureOptionMarketDataLookup test = OvernightFutureOptionMarketDataLookup.of(ids);
    assertThat(test.queryType()).isEqualTo(OvernightFutureOptionMarketDataLookup.class);
    assertThat(test.getVolatilityIndices()).containsOnly(USD_SOFR, USD_FED_FUND);
    assertThat(test.getVolatilityIds(USD_SOFR)).containsOnly(VOL_ID1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getVolatilityIds(GBP_SONIA));

    assertThat(test.requirements(USD_SOFR)).isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThat(test.requirements(ImmutableSet.of(USD_SOFR)))
        .isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(GBP_SONIA)));

    assertThat(test.volatilities(USD_SOFR, MOCK_MARKET_DATA)).isEqualTo(MOCK_VOLS);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.volatilities(GBP_SONIA, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_marketDataView() {
    OvernightFutureOptionMarketDataLookup test = OvernightFutureOptionMarketDataLookup.of(USD_SOFR, VOL_ID1);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    OvernightFutureOptionScenarioMarketData multiScenario = test.marketDataView(md);
    assertThat(multiScenario.getLookup()).isEqualTo(test);
    assertThat(multiScenario.getMarketData()).isEqualTo(md);
    assertThat(multiScenario.getScenarioCount()).isEqualTo(1);
    OvernightFutureOptionMarketData scenario = multiScenario.scenario(0);
    assertThat(scenario.getLookup()).isEqualTo(test);
    assertThat(scenario.getMarketData()).isEqualTo(md.scenario(0));
    assertThat(scenario.getValuationDate()).isEqualTo(valDate);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DefaultOvernightFutureOptionMarketDataLookup test =
        DefaultOvernightFutureOptionMarketDataLookup.of(ImmutableMap.of(USD_SOFR, VOL_ID1, USD_FED_FUND, VOL_ID1));
    coverImmutableBean(test);
    DefaultOvernightFutureOptionMarketDataLookup test2 = DefaultOvernightFutureOptionMarketDataLookup.of(USD_SOFR, VOL_ID1);
    coverBeanEquals(test, test2);

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
  }

  @Test
  public void test_serialization() {
    DefaultOvernightFutureOptionMarketDataLookup test =
        DefaultOvernightFutureOptionMarketDataLookup.of(ImmutableMap.of(USD_SOFR, VOL_ID1, USD_FED_FUND, VOL_ID1));
    assertSerialization(test);
  }

}
