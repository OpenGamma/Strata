/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Set;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

@Test(groups = TestGroup.UNIT)
public class ResettableLiveMarketDataSourceTest {

  private final ExternalIdBundle _id1 = ExternalId.of("foo", "1").toBundle();
  private final ExternalIdBundle _id2 = ExternalId.of("foo", "2").toBundle();
  private final FieldName _fieldName = FieldName.of("fieldName");

  @Test
  public void emptyProviderReturnsPendingResultAndRequestIsRecorded() {
    ResettableLiveMarketDataSource dataSource = new ResettableLiveMarketDataSource(MarketData.live(), mock(LDClient.class));
    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(result.getStatus(), FailureStatus.PENDING_DATA);
    assertTrue(dataSource.getRequestedData().contains(Pairs.of(_id1, _fieldName)));
  }

  @Test
  public void alreadyPendingDataReturnsPendingResultButNoRequest() {
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    ResettableLiveMarketDataSource dataSource = builder.pending(_id1, _fieldName).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertThat(result.getStatus(), Matchers.<ResultStatus>is(FailureStatus.PENDING_DATA));
    assertTrue(dataSource.getRequestedData().isEmpty());
  }

  @Test
  public void missingDataReturnsMissingResultButNoRequest() {
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    ResettableLiveMarketDataSource dataSource = builder.missing(_id1, _fieldName).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(result.getStatus(), FailureStatus.MISSING_DATA);
    assertTrue(dataSource.getRequestedData().isEmpty());
  }

  @Test
  public void availableDataReturnsResultButNoRequest() {
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    ResettableLiveMarketDataSource dataSource = builder.data(_id1, _fieldName, 123.45).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertTrue(result.isSuccess());
    assertEquals(result.getValue(), 123.45);
    assertTrue(dataSource.getRequestedData().isEmpty());
  }

  @Test
  public void requestsAreAggregatedBetweenCalls() {
    ResettableLiveMarketDataSource dataSource = new ResettableLiveMarketDataSource(MarketData.live(), mock(LDClient.class));
    dataSource.get(_id1, _fieldName);
    dataSource.get(_id2, _fieldName);
    dataSource.get(_id1, _fieldName);

    Set<Pair<ExternalIdBundle, FieldName>> requests = dataSource.getRequestedData();
    assertTrue(requests.contains(Pairs.of(_id1, _fieldName)));
    assertTrue(requests.contains(Pairs.of(_id2, _fieldName)));
  }
}
