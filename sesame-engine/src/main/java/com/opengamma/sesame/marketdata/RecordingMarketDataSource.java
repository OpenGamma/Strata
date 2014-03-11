/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * TODO package private? will this only be used by the engine?
 * TODO should the data be mutable? revisit as part of the live data caching [SSM-146]
 */
public class RecordingMarketDataSource implements MarketDataSource {

  /** The market data. */
  private final Map<Pair<ExternalIdBundle, FieldName>, Object> _data = new ConcurrentHashMap<>();

  /** Market data that has been requested. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _requests =
      Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>());

  /** Market data that was previously requested and is still pending. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _pending;

  /** Market data that has been requested and is known to be unavailable. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _missing;

  public RecordingMarketDataSource(Map<Pair<ExternalIdBundle, FieldName>, ?> data,
                                   Set<Pair<ExternalIdBundle, FieldName>> pending,
                                   Set<Pair<ExternalIdBundle, FieldName>> missing) {
    _data.putAll(ArgumentChecker.notNull(data, "data"));
    _pending = ArgumentChecker.notNull(pending, "pending");
    _missing = ArgumentChecker.notNull(missing, "missing");
  }

  /**
   * Creates an empty data source that returns no data but records all data requests.
   */
  public RecordingMarketDataSource() {
    this(Collections.<Pair<ExternalIdBundle, FieldName>, Object>emptyMap(),
         Collections.<Pair<ExternalIdBundle, FieldName>>emptySet(),
         Collections.<Pair<ExternalIdBundle, FieldName>>emptySet());
  }

  /**
   * Creates a source with a fixed set of data.
   *
   * @param data the data this source will return, keyed by the ID and name of the data field
   */
  public RecordingMarketDataSource(Map<Pair<ExternalIdBundle, FieldName>, ?> data) {
    this(data,
         Collections.<Pair<ExternalIdBundle, FieldName>>emptySet(),
         Collections.<Pair<ExternalIdBundle, FieldName>>emptySet());
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    if (_missing.contains(Pairs.of(id, fieldName))) {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data available for {}/{}", id, fieldName);
    }
    // data is already pending, no need to add it to requests and ask for it again
    if (_pending.contains(Pairs.of(id, fieldName))) {
      return ResultGenerator.failure(FailureStatus.PENDING_DATA, "Already requested data for {}/{}", id, fieldName);
    }
    Pair<ExternalIdBundle, FieldName> key = Pairs.of(id, fieldName);
    Object value = _data.get(key);

    if (value != null) {
      return ResultGenerator.success(value);
    } else {
      _requests.add(key);
      return ResultGenerator.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName);
    }
  }

  /**
   * @return the available market data used when initializing the source.
   */
  public Map<Pair<ExternalIdBundle, FieldName>, Object> getData() {
    return _data;
  }

  /**
   * @return the pending market data used when initializing the source.
   */
  public Set<Pair<ExternalIdBundle, FieldName>> getPending() {
    return _pending;
  }

  /**
   * @return the missing market data used when initializing the source.
   */
  public Set<Pair<ExternalIdBundle, FieldName>> getMissing() {
    return _missing;
  }

  /**
   * @return the data that was requested but wasn't available
   * TODO should this clear the set of requests? and if so should it have a different name?
   */
  public Set<Pair<ExternalIdBundle, FieldName>> getRequests() {
    return Collections.unmodifiableSet(_requests);
  }

  public static final class Builder {

    private Map<Pair<ExternalIdBundle, FieldName>, Object> _data = new HashMap<>();
    private Set<Pair<ExternalIdBundle, FieldName>> _pending = new HashSet<>();
    private Set<Pair<ExternalIdBundle, FieldName>> _missing = new HashSet<>();

    public Builder data(Map<Pair<ExternalIdBundle, FieldName>, Object> data) {
      _data = ArgumentChecker.notNull(data, "data");
      return this;
    }

    public Builder data(FieldName fieldName, Map<ExternalIdBundle, ?> data) {
      ArgumentChecker.notNull(fieldName, "fieldName");

      for (Map.Entry<ExternalIdBundle, ?> entry : data.entrySet()) {
        _data.put(Pairs.of(entry.getKey(), fieldName), entry.getValue());
      }
      return this;
    }

    public Builder data(ExternalIdBundle id, FieldName fieldName, Object data) {
      _data.put(key(id, fieldName), ArgumentChecker.notNull(data, "data"));
      return this;
    }

    public Builder pending(Set<Pair<ExternalIdBundle, FieldName>> pending) {
      _pending.addAll(ArgumentChecker.notNull(pending, "pending"));
      return this;
    }

    public Builder pending(ExternalIdBundle id, FieldName fieldName) {
      _pending.add(key(id, fieldName));
      return this;
    }

    public Builder missing(Set<Pair<ExternalIdBundle, FieldName>> missing) {
      _missing.addAll(ArgumentChecker.notNull(missing, "missing"));
      return this;
    }

    public Builder missing(ExternalIdBundle id, FieldName fieldName) {
      _missing.add(key(id, fieldName));
      return this;
    }

    public RecordingMarketDataSource build() {
      return new RecordingMarketDataSource(_data, _pending, _missing);
    }

    private static Pair<ExternalIdBundle, FieldName> key(ExternalIdBundle id, FieldName fieldName) {
      return Pairs.of(ArgumentChecker.notNull(id, "id"), ArgumentChecker.notNull(fieldName, "fieldName"));
    }
  }
}
