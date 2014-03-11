/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

@Test(groups = TestGroup.UNIT)
public class RecordingMarketDataSourceTest {

  private final ExternalIdBundle _id1 = ExternalId.of("foo", "1").toBundle();
  private final ExternalIdBundle _id2 = ExternalId.of("foo", "2").toBundle();
  private final FieldName _fieldName = FieldName.of("fieldName");

  @Test
  public void emptyProviderReturnsPendingResultAndRequestIsRecorded() {
    RecordingMarketDataSource dataSource = new RecordingMarketDataSource();
    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(result.getStatus(), FailureStatus.PENDING_DATA);
    assertTrue(dataSource.getRequests().contains(Pairs.of(_id1, _fieldName)));
  }

  @Test
  public void alreadyPendingDataReturnsPendingResultButNoRequest() {
    RecordingMarketDataSource.Builder builder = new RecordingMarketDataSource.Builder();
    RecordingMarketDataSource dataSource = builder.pending(_id1, _fieldName).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(result.getStatus(), FailureStatus.PENDING_DATA);
    assertTrue(dataSource.getRequests().isEmpty());
  }

  @Test
  public void missingDataReturnsMissingResultButNoRequest() {
    RecordingMarketDataSource.Builder builder = new RecordingMarketDataSource.Builder();
    RecordingMarketDataSource dataSource = builder.missing(_id1, _fieldName).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertEquals(result.getStatus(), FailureStatus.MISSING_DATA);
    assertTrue(dataSource.getRequests().isEmpty());
  }

  @Test
  public void availableDataReturnsResultButNoRequest() {
    RecordingMarketDataSource.Builder builder = new RecordingMarketDataSource.Builder();
    RecordingMarketDataSource dataSource = builder.data(_id1, _fieldName, 123.45).build();

    Result<?> result = dataSource.get(_id1, _fieldName);
    assertTrue(result.isValueAvailable());
    assertEquals(result.getValue(), 123.45);
    assertTrue(dataSource.getRequests().isEmpty());
  }

  @Test
  public void requestsAreAggregatedBetweenCalls() {
    RecordingMarketDataSource dataSource = new RecordingMarketDataSource();
    dataSource.get(_id1, _fieldName);
    dataSource.get(_id2, _fieldName);
    dataSource.get(_id1, _fieldName);

    Set<Pair<ExternalIdBundle, FieldName>> requests = dataSource.getRequests();
    assertTrue(requests.contains(Pairs.of(_id1, _fieldName)));
    assertTrue(requests.contains(Pairs.of(_id2, _fieldName)));
  }
}
