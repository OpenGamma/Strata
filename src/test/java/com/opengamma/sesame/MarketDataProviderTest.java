/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.marketdata.MarketDataStatus.AVAILABLE;
import static com.opengamma.sesame.marketdata.MarketDataStatus.PENDING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValue;
import com.opengamma.sesame.marketdata.SingleMarketDataValue;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;


@Test(groups = TestGroup.UNIT)
public class MarketDataProviderTest {

  private ResettableMarketDataProviderFunction _resettableMarketDataProviderFunction;
  private MarketDataRequirement _mdReqmt1 = new MarketDataRequirement() {
  };
  private MarketDataRequirement _mdReqmt2 = new MarketDataRequirement() {
  };

  @BeforeMethod
  public void setUp() {
    _resettableMarketDataProviderFunction = new MarketDataProvider();
  }

  @Test
  public void emptyProviderReturnsPendingResultAndRequestIsRecorded() {

    MarketDataFunctionResult result = _resettableMarketDataProviderFunction.requestData(_mdReqmt1);
    assertThat(result.getMarketDataState(_mdReqmt1), is(PENDING));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), contains(_mdReqmt1));
  }

  @Test
  public void alreadyPendingDataReturnsPendingResultButNoRequest() {

    _resettableMarketDataProviderFunction.resetMarketData(ImmutableMap.of(_mdReqmt1, Pairs.of(PENDING, (MarketDataValue) null)));

    MarketDataFunctionResult result = _resettableMarketDataProviderFunction.requestData(_mdReqmt1);
    assertThat(result.getMarketDataState(_mdReqmt1), is(PENDING));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), is(empty()));
  }

  @Test
  public void availableDataReturnsResultButNoRequest() {

    _resettableMarketDataProviderFunction.resetMarketData(ImmutableMap.of(_mdReqmt1, Pairs.<MarketDataStatus, MarketDataValue>of(AVAILABLE, new SingleMarketDataValue(123.45))));

    MarketDataFunctionResult result = _resettableMarketDataProviderFunction.requestData(_mdReqmt1);
    assertThat(result.getMarketDataState(_mdReqmt1), is(AVAILABLE));
    assertThat(result.getMarketDataValue(_mdReqmt1).getValue(), is((Object) 123.45));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), is(empty()));
  }

  @Test
  public void resettingRemovesRequestsAndAvailableData() {

    _resettableMarketDataProviderFunction.resetMarketData(ImmutableMap.of(_mdReqmt1, Pairs.<MarketDataStatus, MarketDataValue>of(AVAILABLE, new SingleMarketDataValue(123.45))));

    MarketDataFunctionResult result1 = _resettableMarketDataProviderFunction.requestData(ImmutableSet.of(_mdReqmt1, _mdReqmt2));
    assertThat(result1.getMarketDataState(_mdReqmt1), is(AVAILABLE));
    assertThat(result1.getMarketDataState(_mdReqmt2), is(PENDING));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), contains(_mdReqmt2));

    _resettableMarketDataProviderFunction.resetMarketData(ImmutableMap.<MarketDataRequirement, Pair<MarketDataStatus, MarketDataValue>>of());

    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), is(empty()));
    MarketDataFunctionResult result2 = _resettableMarketDataProviderFunction.requestData(ImmutableSet.of(_mdReqmt1, _mdReqmt2));

    assertThat(result2.getMarketDataState(_mdReqmt1), is(PENDING));
    assertThat(result2.getMarketDataState(_mdReqmt2), is(PENDING));

    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), containsInAnyOrder(_mdReqmt1, _mdReqmt2));
  }

  @Test
  public void requestsAreAggregatedBetweenCalls() {

    _resettableMarketDataProviderFunction.requestData(_mdReqmt1);
    _resettableMarketDataProviderFunction.requestData(_mdReqmt2);
    _resettableMarketDataProviderFunction.requestData(_mdReqmt1);

    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), containsInAnyOrder(_mdReqmt1, _mdReqmt2));
  }
}
