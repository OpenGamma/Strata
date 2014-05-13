/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification.LIVE_SPEC;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ResettableLiveMarketDataSourceTest {

  private final ExternalIdBundle _id1 = ExternalId.of("foo", "1").toBundle();
  private final ExternalIdBundle _id2 = ExternalId.of("foo", "2").toBundle();
  private final FieldName _fieldName = FieldName.of("fieldName");

  @Test
  public void emptyProviderReturnsPendingResultAndRequestIsRecorded() {
    ResettableLiveMarketDataSource dataSource = createBuilder().build();
    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(result.getStatus(), FailureStatus.PENDING_DATA);
    assertTrue(dataSource.getRequestedData().contains(_id1));
  }

  @Test
  public void alreadyPendingDataReturnsPendingResultButNoRequest() {
    ResettableLiveMarketDataSource dataSource = createBuilder().pending(_id1).build();
        new ResettableLiveMarketDataSource(MarketData.live(), mock(LDClient.class));
    Result<?> result = dataSource.get(_id1, _fieldName);
    assertThat(result.getStatus(), Matchers.<ResultStatus>is(FailureStatus.PENDING_DATA));
    assertTrue(dataSource.getRequestedData().isEmpty());
  }

  @Test
  public void missingDataReturnsMissingResultButNoRequest() {
    ResettableLiveMarketDataSource dataSource = createBuilder().missing(_id1).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(MISSING_DATA, result.getStatus());
    assertTrue(dataSource.getRequestedData().isEmpty());
  }

  @Test
  public void availableDataReturnsResultButNoRequest() {
    ResettableLiveMarketDataSource dataSource = createBuilder().data(_id1, _fieldName, 123.45).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertTrue(result.isSuccess());
    assertEquals(result.getValue(), 123.45);
    assertTrue(dataSource.getRequestedData().isEmpty());
  }

  private ResettableLiveMarketDataSourceBuilder createBuilder() {
    return new ResettableLiveMarketDataSourceBuilder();
  }

  @Test
  public void requestsAreAggregatedBetweenCalls() {
    ResettableLiveMarketDataSource dataSource = createBuilder().build();
    dataSource.get(_id1, _fieldName);
    dataSource.get(_id2, _fieldName);
    dataSource.get(_id1, _fieldName);

    Set<ExternalIdBundle> requests = dataSource.getRequestedData();
    assertTrue(requests.contains(_id1));
    assertTrue(requests.contains(_id2));
  }

  class ResettableLiveMarketDataSourceBuilder {

    private final MutableLiveDataResultMapper _data = new DefaultMutableLiveDataResultMapper();

    ResettableLiveMarketDataSourceBuilder data(ExternalIdBundle id, FieldName fieldName, Object value) {
      _data.update(id, new LiveDataUpdate(ImmutableMap.of(fieldName, value), ImmutableSet.<Permission>of()));
      return this;
    }

    ResettableLiveMarketDataSourceBuilder missing(ExternalIdBundle id) {
      _data.addMissing(id, "No data for this");
      return this;
    }

    ResettableLiveMarketDataSourceBuilder pending(ExternalIdBundle id) {
      _data.addPending(id);
      return this;
    }

    ResettableLiveMarketDataSource build() {
      LDClient client = mock(LDClient.class);
      return new ResettableLiveMarketDataSource(LIVE_SPEC, client, _data.createSnapshot());
    }
  }
}
