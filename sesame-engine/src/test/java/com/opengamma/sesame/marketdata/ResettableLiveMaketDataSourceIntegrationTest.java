/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.sesame.marketdata.MarketDataTestUtils.buildSuccessResponse;
import static com.opengamma.sesame.marketdata.MarketDataTestUtils.createBundle;
import static com.opengamma.sesame.marketdata.MarketDataTestUtils.createLiveDataManager;
import static com.opengamma.sesame.marketdata.MarketDataTestUtils.createLiveDataSpec;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.FailureStatus.PENDING_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.shiro.util.ThreadContext;
import org.fudgemsg.FudgeContext;
import org.fudgemsg.MutableFudgeMsg;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.livedata.LiveDataValueUpdateBean;
import com.opengamma.util.auth.PermissiveSecurityManager;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

/**
 * Tests for ResettableLiveMarketDataSource. Checks that it
 * works correctly with the DefaultLiveDataManager. Separate
 * from {@link ResettableLiveMarketDataSourceTest} as testing
 * different aspects. ResettableLiveMarketDataSource should be
 * split in two as per SSM-195.
 */
@Test(groups = TestGroup.UNIT)
public class ResettableLiveMaketDataSourceIntegrationTest {

  private DefaultLiveDataManager _manager;
  private FudgeContext _fudgeContext;
  private StrategyAwareMarketDataSource _source;
  private LDClient _client;

  @BeforeMethod
  public void setUp() {
    // Set authorization to allow everything for most tests
    ThreadContext.bind(new PermissiveSecurityManager());
    _fudgeContext = new FudgeContext();
    _manager = createLiveDataManager();
    _client = new LDClient(_manager);
    _source = new ResettableLiveMarketDataSource(LiveMarketDataSpecification.LIVE_SPEC, _client);
  }

  @Test
  public void testClientWorksCorrectly() throws InterruptedException {

    // Initially all requests should be pending
    assertThat(_source.get(createBundle("T1"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T1"), FieldName.of("Dividend_Date")).getStatus(), is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T2"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T3"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));

    // New thread to wait for the data to come in
    final AtomicReference<StrategyAwareMarketDataSource> updatedSource = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    new Thread(new Runnable() {
      @Override
      public void run() {
        updatedSource.set(_source.createPrimedSource());
        latch.countDown();
      }
    }).start();

    Thread.sleep(10);
    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T1"), buildSuccessResponse("T2"), buildSuccessResponse("T3")));

    MutableFudgeMsg msgT1 = _fudgeContext.newMessage();
    msgT1.add("Market_Value", 1.23);
    msgT1.add("Dividend_Date", "2014-01-01");

    MutableFudgeMsg msgT2 = _fudgeContext.newMessage();
    msgT2.add("Market_Value", 2.345);

    MutableFudgeMsg msgT3 = _fudgeContext.newMessage();
    msgT3.add("Market_Value", 3.4567);

    // Now send the value updates
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T2"), msgT2));

    Thread.sleep(10);

    // No update for T3 so should not have the new source yet
    assertThat(updatedSource.get(), is(nullValue()));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T3"), msgT3));

    latch.await();

    // Now everything has come in
    StrategyAwareMarketDataSource newSource = updatedSource.get();
    assertThat(newSource, is(not(nullValue())));

    Result<?> t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 1.23));

    Result<?> t1DdResult = newSource.get(createBundle("T1"), FieldName.of("Dividend_Date"));
    assertThat(t1DdResult.isSuccess(), is(true));
    assertThat(t1DdResult.getValue(), is((Object) "2014-01-01"));

    Result<?> t2Result = newSource.get(createBundle("T2"), FieldName.of("Market_Value"));
    assertThat(t2Result.isSuccess(), is(true));
    assertThat(t2Result.getValue(), is((Object) 2.345));

    Result<?> t3Result = newSource.get(createBundle("T3"), FieldName.of("Market_Value"));
    assertThat(t3Result.isSuccess(), is(true));
    assertThat(t3Result.getValue(), is((Object) 3.4567));
  }

  @Test
  public void testMissingDataForTickerIsCorrectlyFlagged()  {
    _manager.subscribe(_client, ImmutableSet.of(createBundle("T1")));
    _manager.subscriptionResultsReceived(ImmutableSet.of(buildSuccessResponse("T1")));

    // Make requests for the data, pending as client hasn't asked before
    assertThat(_source.get(createBundle("T1"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T1"), FieldName.of("Dividend_Date")).getStatus(),
               is((ResultStatus) PENDING_DATA));

    MutableFudgeMsg msgT1 = _fudgeContext.newMessage();
    msgT1.add("Market_Value", 1.23);
    // No dividend date field

    // Now send the value updates
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));

    StrategyAwareMarketDataSource newSource = _source.createPrimedSource();

    Result<?> t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 1.23));

    Result<?> t1DdResult = newSource.get(createBundle("T1"), FieldName.of("Dividend_Date"));
    assertThat(t1DdResult.isSuccess(), is(false));
    assertThat(t1DdResult.getStatus(), is((ResultStatus) MISSING_DATA));
    assertThat(t1DdResult.getFailureMessage(), containsString("Data is available"));
  }

  @Test
  public void testMergingOfData() throws InterruptedException {

    // Setup data first so we don't to multithread test
    _manager.subscribe(_client, ImmutableSet.of(createBundle("T1"), createBundle("T2")));
    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T1"), buildSuccessResponse("T2")));

    // Make requests for the data, pending as client hasn't asked before
    assertThat(_source.get(createBundle("T1"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T1"), FieldName.of("Dividend_Date")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T2"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    assertThat(_source.get(createBundle("T2"), FieldName.of("Dividend_Date")).getStatus(),
               is((ResultStatus) PENDING_DATA));

    MutableFudgeMsg msgT1 = _fudgeContext.newMessage();
    msgT1.add("Market_Value", 1.23);
    msgT1.add("Dividend_Date", "2014-01-01");

    MutableFudgeMsg msgT2 = _fudgeContext.newMessage();
    msgT2.add("Market_Value", 2.345);
    msgT2.add("Dividend_Date", "2014-02-02");

    // Now send the value updates
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T2"), msgT2));

    StrategyAwareMarketDataSource newSource = _source.createPrimedSource();

    Result<?> t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 1.23));

    Result<?> t1DdResult = newSource.get(createBundle("T1"), FieldName.of("Dividend_Date"));
    assertThat(t1DdResult.isSuccess(), is(true));
    assertThat(t1DdResult.getValue(), is((Object) "2014-01-01"));

    Result<?> t2MvResult = newSource.get(createBundle("T2"), FieldName.of("Market_Value"));
    assertThat(t2MvResult.isSuccess(), is(true));
    assertThat(t2MvResult.getValue(), is((Object) 2.345));

    Result<?> t2DdResult = newSource.get(createBundle("T2"), FieldName.of("Dividend_Date"));
    assertThat(t2DdResult.isSuccess(), is(true));
    assertThat(t2DdResult.getValue(), is((Object) "2014-02-02"));

    // Update both values for T1
    msgT1 = _fudgeContext.newMessage();
    msgT1.add("Market_Value", 9.87);
    msgT1.add("Dividend_Date", "2014-12-12");

    // Update just one value for T2
    msgT2 = _fudgeContext.newMessage();
    msgT2.add("Market_Value", 8.765);

    // Now send the value updates
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T2"), msgT2));

    newSource = newSource.createPrimedSource();

    t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 9.87));

    t1DdResult = newSource.get(createBundle("T1"), FieldName.of("Dividend_Date"));
    assertThat(t1DdResult.isSuccess(), is(true));
    assertThat(t1DdResult.getValue(), is((Object) "2014-12-12"));

    t2MvResult = newSource.get(createBundle("T2"), FieldName.of("Market_Value"));
    assertThat(t2MvResult.isSuccess(), is(true));
    assertThat(t2MvResult.getValue(), is((Object) 8.765));

    t2DdResult = newSource.get(createBundle("T2"), FieldName.of("Dividend_Date"));
    assertThat(t2DdResult.isSuccess(), is(true));
    // Value is unchanged from previously
    assertThat(t2DdResult.getValue(), is((Object) "2014-02-02"));
  }

  /**
   * Test that data is available for a ticker even when the field
   * has not been requested before (though some other field on the
   * ticker has been).
   */
  @Test
  public void testDataIsAvailableForAllFieldsForTicker() {
    _manager.subscribe(_client, ImmutableSet.of(createBundle("T1")));
    _manager.subscriptionResultsReceived(ImmutableSet.of(buildSuccessResponse("T1")));

    // Make requests for the data, pending as client hasn't asked before
    assertThat(_source.get(createBundle("T1"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));
    // No request made for dividend date

    MutableFudgeMsg msgT1 = _fudgeContext.newMessage();
    msgT1.add("Market_Value", 1.23);
    msgT1.add("Dividend_Date", "2014-01-01");

    // Now send the value updates
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));

    StrategyAwareMarketDataSource newSource = _source.createPrimedSource();

    Result<?> t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 1.23));

    Result<?> t1DdResult = newSource.get(createBundle("T1"), FieldName.of("Dividend_Date"));
    assertThat(t1DdResult.isSuccess(), is(true));
    assertThat(t1DdResult.getValue(), is((Object) "2014-01-01"));
  }

  @Test
  public void testReusingTheSameSourceIsNotRecommendedButWorks() {
    // Test introduced after previous testing accidentally
    // uncovered issue with reuse of source
    _manager.subscribe(_client, ImmutableSet.of(createBundle("T1")));
    _manager.subscriptionResultsReceived(ImmutableSet.of(buildSuccessResponse("T1")));

    // Make requests for the data, pending as client hasn't asked before
    assertThat(_source.get(createBundle("T1"), FieldName.of("Market_Value")).getStatus(),
               is((ResultStatus) PENDING_DATA));

    MutableFudgeMsg msgT1 = _fudgeContext.newMessage();
    msgT1.add("Market_Value", 1.23);

    // Now send the value updates
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));

    StrategyAwareMarketDataSource newSource = _source.createPrimedSource();

    Result<?> t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 1.23));

    // Now send the value updates again
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msgT1));

    // Reuse the original source
    newSource = newSource.createPrimedSource();

    t1MvResult = newSource.get(createBundle("T1"), FieldName.of("Market_Value"));
    assertThat(t1MvResult.isSuccess(), is(true));
    assertThat(t1MvResult.getValue(), is((Object) 1.23));
  }

}
