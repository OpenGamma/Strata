/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.helpers.MessageFormatter;

import com.opengamma.id.ExternalIdBundle;

/**
 * A LiveDataResultMapper which maintains the set of results in place.
 */
public class DefaultMutableLiveDataResultMapper implements MutableLiveDataResultMapper {

  /**
   * The set of results being maintained.
   */
  private final Map<ExternalIdBundle, LiveDataResult> _currentResults = new HashMap<>();

  @Override
  public boolean containsKey(ExternalIdBundle ticker) {
    return _currentResults.containsKey(ticker);
  }

  @Override
  public LiveDataResult get(ExternalIdBundle ticker) {
    return _currentResults.get(ticker);
  }

  @Override
  public void update(ExternalIdBundle ticker, LiveDataUpdate update) {
    _currentResults.put(ticker, generateMergedResult(ticker, update));
  }

  private LiveDataResult generateMergedResult(ExternalIdBundle ticker,
                                              LiveDataUpdate update) {
    if (_currentResults.containsKey(ticker)) {
      LiveDataResult result = _currentResults.get(ticker);
      return result.update(update);
    } else {
      return new DefaultLiveDataResult(ticker, update);
    }
  }

  @Override
  public void remove(ExternalIdBundle ticker) {
    _currentResults.remove(ticker);
  }

  @Override
  public DefaultImmutableLiveDataResultMapper createSnapshot(Set<ExternalIdBundle> tickers) {

    Map<ExternalIdBundle, LiveDataResult> results = new HashMap<>();

    for (ExternalIdBundle ticker : tickers) {
      LiveDataResult result = _currentResults.get(ticker);
      results.put(ticker, result.permissionCheck());
    }

    return new DefaultImmutableLiveDataResultMapper(results);
  }

  @Override
  public ImmutableLiveDataResultMapper createSnapshot() {
    return new DefaultImmutableLiveDataResultMapper(_currentResults);
  }

  @Override
  public void addPending(ExternalIdBundle ticker) {
    _currentResults.put(ticker, new PendingLiveDataResult(ticker));
  }

  @Override
  public void addMissing(ExternalIdBundle ticker, String userMessage, Object... args) {
    String message = formatMessage(userMessage, args);
    _currentResults.put(ticker, new MissingLiveDataResult(ticker, message));
  }

  @Override
  public void addPermissionDenied(ExternalIdBundle ticker, String userMessage, Object... args) {
    String message = formatMessage(userMessage, args);
    _currentResults.put(ticker, new PermissionDeniedLiveDataResult(message));
  }

  private String formatMessage(String userMessage, Object[] args) {
    return MessageFormatter.format(userMessage, args).getMessage();
  }

  @Override
  public boolean isPending(ExternalIdBundle ticker) {
    if (containsKey(ticker)) {
      return _currentResults.get(ticker).isPending();
    } else {
      throw new IllegalArgumentException("No result found for ticker: " + ticker);
    }
  }

  @Override
  public int size() {
    return _currentResults.size();
  }

}
