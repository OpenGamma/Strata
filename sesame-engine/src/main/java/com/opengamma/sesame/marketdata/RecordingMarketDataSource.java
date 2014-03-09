/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Data source which contains a fixed snapshot of market data and records requests for data it doesn't contain.
 * TODO this is a strawman implementation. review the API when it's clearer what we need
 */
public class RecordingMarketDataSource implements MarketDataSource {

  private final Map<Pair<ExternalIdBundle, FieldName>, Object> _data = new ConcurrentHashMap<>();
  private final Set<Pair<ExternalIdBundle, FieldName>> _requests =
      Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>());
  // TODO a set of data that is known to have failed

  /**
   * Creates an empty data source that returns no data but records all data requests.
   */
  public RecordingMarketDataSource() {
    this(Collections.<Pair<ExternalIdBundle, FieldName>, Object>emptyMap());
  }

  /**
   * Creates a source with a fixed set of data.
   *
   * @param data the data this source will return, keyed by the ID and name of the data field
   */
  public RecordingMarketDataSource(Map<Pair<ExternalIdBundle, FieldName>, ?> data) {
    _data.putAll(ArgumentChecker.notNull(data, "data"));
  }

  /**
   * Creates a source with a fixed set of data.
   *
   * @param fieldName the data field of all the data
   * @param data the data this source will return, keyed by ID. Assumes all requests will be for the same data field
   */
  public RecordingMarketDataSource(FieldName fieldName, Map<ExternalIdBundle, ?> data) {
    for (Map.Entry<ExternalIdBundle, ?> entry : data.entrySet()) {
      _data.put(Pairs.of(entry.getKey(), fieldName), entry.getValue());
    }
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    Pair<ExternalIdBundle, FieldName> key = Pairs.of(id, fieldName);
    Object value = _data.get(key);

    // TODO check if the request is in the failed set and return UNAVAILABLE

    if (value != null) {
      return ResultGenerator.success(value);
    } else {
      _requests.add(key);
      return ResultGenerator.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName);
    }
  }

  /**
   * @return the data that was requested but wasn't available
   * TODO should this clear the set of requests? and if so should it have a different name?
   */
  public Set<Pair<ExternalIdBundle, FieldName>> getRequests() {
    return Collections.unmodifiableSet(_requests);
  }
}
