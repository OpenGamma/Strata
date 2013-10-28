/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.ResultStatus.AWAITING_MARKET_DATA;
import static com.opengamma.util.money.Currency.USD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableSet;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.util.money.Currency;

public class MarketDataContextTest {

  private MarketDataContext _marketDataContext;

  @BeforeTest
  public void setUp() {
    _marketDataContext = createEmptyMarketDataContext();
  }

  @Test
  public void testAskingForSingleMarketDataItemMeansRequestIsStored() {

    MarketDataRequirement requirement = createEquityRequirement();
    MarketDataFunctionResult result = _marketDataContext.retrieveMarketData(ImmutableSet.of(requirement));

    assertThat(result.getStatus(), is(AWAITING_MARKET_DATA));
    assertThat(result.getRequiredMarketData().size(), is(1));
    assertThat(result.getRequiredMarketData(), is((Set) ImmutableSet.of(requirement)));
    assertThat(result.getMarketDataState(requirement), is(MarketDataStatus.PENDING));
  }

  @Test
  public void testAskingForMultipleMarketDataItemsMeansRequestsAreStored() {

    Set<MarketDataRequirement> requirements = new HashSet<>();
    requirements.add(createEquityRequirement());
    requirements.add(createFraRequirement());

    MarketDataFunctionResult result = _marketDataContext.retrieveMarketData(requirements);

    assertThat(result.getStatus(), is(AWAITING_MARKET_DATA));

    Set<MarketDataRequirement> requiredMarketData = result.getRequiredMarketData();
    assertThat(requiredMarketData.size(), is(2));
    assertThat(requiredMarketData, is(requirements));
  }

  @Test
  public void testResultsCanBeCombined() {

    MarketDataRequirement eqtReqmt = createEquityRequirement();
    MarketDataFunctionResult eqtResult = _marketDataContext.retrieveMarketData(ImmutableSet.of(eqtReqmt));

    MarketDataRequirement fraReqmt = createFraRequirement();
    MarketDataFunctionResult fraResult = _marketDataContext.retrieveMarketData(ImmutableSet.of(fraReqmt));

    FunctionResult combined = eqtResult.combine(fraResult);

    Set<MarketDataRequirement> requiredMarketData = combined.getRequiredMarketData();
    assertThat(requiredMarketData.size(), is(2));
    assertThat(requiredMarketData, is((Set) ImmutableSet.of(eqtReqmt, fraReqmt)));
  }

  private MarketDataRequirement createFraRequirement() {
    return StandardMarketDataRequirement.of(createFRASecurity(), MarketDataRequirementNames.MARKET_VALUE);
  }

  private MarketDataRequirement createEquityRequirement() {
    return StandardMarketDataRequirement.of(
        createEquitySecurity(), MarketDataRequirementNames.MARKET_VALUE);
  }

  private FinancialSecurity createEquitySecurity() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    return security;
  }

  private FinancialSecurity createFRASecurity() {

    ZonedDateTime start = ZonedDateTime.now();
    ZonedDateTime end = start.plusYears(1);
    ZonedDateTime fixing = start.plusDays(3);
    FinancialSecurity security = new FRASecurity(USD, ExternalSchemes.currencyRegionId(USD), start, end,
                                                 0.1d, 1000000, ExternalSchemes.bloombergTickerSecurityId("USD"), fixing);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("FRA").toBundle());
    return security;
  }

  private MarketDataContext createEmptyMarketDataContext() {
    return new EmptyMarketDataContext(new StandardResultGenerator());
  }
}
