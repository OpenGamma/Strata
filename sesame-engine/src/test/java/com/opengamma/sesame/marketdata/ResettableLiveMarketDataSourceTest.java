/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification.LIVE_SPEC;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.FailureStatus.PENDING_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

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
    assertEquals(result.getStatus(), MISSING_DATA);
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

    private final Map<ExternalIdBundle, Result<FudgeMsg>> _data = new HashMap<>();
    private final FudgeContext _fudgeContext = new FudgeContext();

    ResettableLiveMarketDataSourceBuilder data(ExternalIdBundle id, FieldName fieldName, Object value) {

      MutableFudgeMsg msg;
      if (_data.containsKey(id)) {
        Result<FudgeMsg> result = _data.get(id);
        if (!result.isSuccess()) {
          throw new IllegalStateException("Result for id: " + id + " is a Failure with status: " + result.getStatus());
        }
        msg = _fudgeContext.newMessage(result.getValue());
      } else {
        msg = _fudgeContext.newMessage();
      }
      msg.add(fieldName.getName(), value);
      _data.put(id, Result.<FudgeMsg>success(msg));

      return this;
    }

    ResettableLiveMarketDataSource build() {
      LDClient client = mock(LDClient.class);
      when(client.retrieveLatestData()).thenReturn(_data);

      ResettableLiveMarketDataSource source = new ResettableLiveMarketDataSource(LIVE_SPEC, client);
      for (ExternalIdBundle id : _data.keySet()) {
        source.get(id, FieldName.of("a field"));
      }
      return (ResettableLiveMarketDataSource) source.createPrimedSource();
    }

    ResettableLiveMarketDataSourceBuilder missing(ExternalIdBundle id) {
      if (_data.containsKey(id)) {
        Result<FudgeMsg> result = _data.get(id);
        if (result.getStatus() != MISSING_DATA) {
          throw new IllegalStateException("Result for id: " + id + " already has status: " + result.getStatus() +
                                              " - cannot add MISSING status");
        }
      } else {
        _data.put(id, Result.<FudgeMsg>failure(MISSING_DATA, "No data for id: {}", id));
      }
      return this;
    }

    ResettableLiveMarketDataSourceBuilder pending(ExternalIdBundle id) {
      if (_data.containsKey(id)) {
        Result<FudgeMsg> result = _data.get(id);
        if (result.getStatus() != PENDING_DATA) {
          throw new IllegalStateException("Result for id: " + id + " already has status: " + result.getStatus() +
                                              " - cannot add PENDING status");
        }
      } else {
        _data.put(id, Result.<FudgeMsg>failure(PENDING_DATA, "Not yet got data for id: {}", id));
      }
      return this;
    }
  }
}
