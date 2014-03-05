/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

// TODO make this RecordingMarketDataSourceTest
@Test(groups = TestGroup.UNIT)
public class DefaultResettableMarketDataFnTest {

  /*private ResettableMarketDataFn _resettableMarketDataProviderFunction;
  private MarketDataRequirement _mdReqmt1 = mock(MarketDataRequirement.class);
  private MarketDataRequirement _mdReqmt2 = mock(MarketDataRequirement.class);

  @BeforeMethod
  public void setUp() {
    _resettableMarketDataProviderFunction = new DefaultResettableMarketDataFn();
  }

  @Test
  public void emptyProviderReturnsPendingResultAndRequestIsRecorded() {

    MarketDataValues result = _resettableMarketDataProviderFunction.requestData(_mdReqmt1).getValue();
    assertThat(result.getStatus(_mdReqmt1), is(PENDING));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), contains(_mdReqmt1));
  }

  @Test
  public void alreadyPendingDataReturnsPendingResultButNoRequest() {

    Map<MarketDataRequirement, MarketDataItem> data = ImmutableMap.of(_mdReqmt1, MarketDataItem.pending());
    _resettableMarketDataProviderFunction.resetMarketData(data);

    MarketDataValues result = _resettableMarketDataProviderFunction.requestData(_mdReqmt1).getValue();
    assertThat(result.getStatus(_mdReqmt1), is(PENDING));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), is(empty()));
  }

  @Test
  public void availableDataReturnsResultButNoRequest() {

    MarketDataItem item = MarketDataItem.available(123.45);
    Map<MarketDataRequirement, MarketDataItem> data =
        ImmutableMap.of(_mdReqmt1, item);
    _resettableMarketDataProviderFunction.resetMarketData(data);

    MarketDataValues result = _resettableMarketDataProviderFunction.requestData(_mdReqmt1).getValue();
    assertThat(result.getStatus(_mdReqmt1), is(AVAILABLE));
    assertThat(result.getValue(_mdReqmt1), is((Object) 123.45));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), is(empty()));
  }

  @Test
  public void resettingRemovesRequestsAndAvailableData() {

    MarketDataItem item = MarketDataItem.available(123.45);
    ImmutableMap<MarketDataRequirement, MarketDataItem> data = ImmutableMap.of(_mdReqmt1, item);
    _resettableMarketDataProviderFunction.resetMarketData(data);

    MarketDataValues result1 = _resettableMarketDataProviderFunction.requestData(ImmutableSet.of(_mdReqmt1, _mdReqmt2)).getValue();
    assertThat(result1.getStatus(_mdReqmt1), is(AVAILABLE));
    assertThat(result1.getStatus(_mdReqmt2), is(PENDING));
    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), contains(_mdReqmt2));

    _resettableMarketDataProviderFunction.resetMarketData(Collections.<MarketDataRequirement, MarketDataItem>emptyMap());

    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), is(empty()));
    MarketDataValues result2 = _resettableMarketDataProviderFunction.requestData(ImmutableSet.of(_mdReqmt1, _mdReqmt2)).getValue();

    assertThat(result2.getStatus(_mdReqmt1), is(PENDING));
    assertThat(result2.getStatus(_mdReqmt2), is(PENDING));

    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), containsInAnyOrder(_mdReqmt1, _mdReqmt2));
  }

  @Test
  public void requestsAreAggregatedBetweenCalls() {

    _resettableMarketDataProviderFunction.requestData(_mdReqmt1);
    _resettableMarketDataProviderFunction.requestData(_mdReqmt2);
    _resettableMarketDataProviderFunction.requestData(_mdReqmt1);

    assertThat(_resettableMarketDataProviderFunction.getCollectedRequests(), containsInAnyOrder(_mdReqmt1, _mdReqmt2));
  }*/
}
