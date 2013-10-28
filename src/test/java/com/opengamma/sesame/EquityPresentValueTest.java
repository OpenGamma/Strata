/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.ResultStatus.AWAITING_MARKET_DATA;
import static com.opengamma.sesame.ResultStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class EquityPresentValueTest {

  EquityPresentValueFunction _equityPresentValueFunction;

  @BeforeTest
  public void setUp() {
    _equityPresentValueFunction = new EquityPresentValue();
  }

  @Test
  public void testMarketDataUnavailable() {

    MarketData marketData = new EmptyMarketData(new StandardResultGenerator());
    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    FunctionResult<Double> result = _equityPresentValueFunction.calculateEquityPresentValue(marketData, security);
    assertThat(result.getStatus(), is(AWAITING_MARKET_DATA));
  }

  @Test
  public void testMarketDataAvailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());

    Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> marketData = new HashMap<>();
    marketData.put(
        StandardMarketDataRequirement.of(security, MarketDataRequirementNames.MARKET_VALUE),
        Pairs.of(MarketDataStatus.AVAILABLE, new SingleMarketDataValue(123.45)));

    MarketData context = new PopulatedMarketData(new StandardResultGenerator(), marketData);
    FunctionResult<Double> result = _equityPresentValueFunction.calculateEquityPresentValue(context, security);
    assertThat(result.getStatus(), is(SUCCESS));
    assertThat(result.getResult(), is(123.45));
  }

  private static class PopulatedMarketData implements MarketData {

    private final MarketDataResultGenerator _resultGenerator;
    private final Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> _requirementStatus;

    private PopulatedMarketData(MarketDataResultGenerator resultGenerator,
                                Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> requirementStatus) {
      _resultGenerator = resultGenerator;
      _requirementStatus = requirementStatus;
    }

    @Override
    public MarketDataFunctionResult retrieveItems(Set<MarketDataRequirement> requiredMarketData) {

      Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> result = new HashMap<>();
      Set<MarketDataRequirement> missing = new HashSet<>();

      for (MarketDataRequirement requirement : requiredMarketData) {

        if (_requirementStatus.containsKey(requirement)) {
          result.put(requirement, _requirementStatus.get(requirement));
        }
        else {
          result.put(requirement, Pairs.of(MarketDataStatus.PENDING, (MarketDataValue) null));
          missing.add(requirement);
        }

      }

      return _resultGenerator.marketDataResultBuilder().foundData(result).missingData(missing).build();
    }

    @Override
    public MarketDataFunctionResult retrieveItem(MarketDataRequirement requiredMarketData) {
      return retrieveItems(ImmutableSet.of(requiredMarketData));
    }
  }
}
