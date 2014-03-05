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
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 *
 */
public class RecordingMarketDataSource implements MarketDataSource {

  private final Map<Pair<ExternalIdBundle, FieldName>, Object> _data = new ConcurrentHashMap<>();
  private final Set<Pair<ExternalIdBundle, FieldName>> _requests =
      Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>());
  // TODO a set of data that is known to have failed

  public RecordingMarketDataSource() {
    this(Collections.<Pair<ExternalIdBundle, FieldName>, Object>emptyMap());
  }

  public RecordingMarketDataSource(Map<Pair<ExternalIdBundle, FieldName>, ?> data) {
    _data.putAll(ArgumentChecker.notNull(data, "data"));
  }

  public RecordingMarketDataSource(FieldName fieldName, Map<ExternalIdBundle, ?> data) {
    for (Map.Entry<ExternalIdBundle, ?> entry : data.entrySet()) {
      _data.put(Pairs.of(entry.getKey(), fieldName), entry.getValue());
    }
  }

  @Override
  public MarketDataItem<?> get(ExternalIdBundle id, FieldName fieldName) {
    Pair<ExternalIdBundle, FieldName> key = Pairs.of(id, fieldName);
    Object value = _data.get(key);

    if (value != null) {
      return MarketDataItem.available(value);
    } else {
      _requests.add(key);
      return null;
    }
  }

  public Set<Pair<ExternalIdBundle, FieldName>> getRequests() {
    return Collections.unmodifiableSet(_requests);
  }
}
