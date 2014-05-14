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
import com.opengamma.util.ArgumentChecker;

/**
 * A LiveDataResults implementation which maintains the set of results in place.
 */
public class DefaultMutableLiveDataResults implements MutableLiveDataResults {

  /**
   * The set of results being maintained.
   */
  private final Map<ExternalIdBundle, LiveDataResult> _currentResults = new HashMap<>();

  @Override
  public boolean containsTicker(ExternalIdBundle ticker) {
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
  public DefaultImmutableLiveDataResults createSnapshot(Set<ExternalIdBundle> tickers) {

    Map<ExternalIdBundle, LiveDataResult> results = new HashMap<>();

    for (ExternalIdBundle ticker : ArgumentChecker.notNull(tickers, "tickers")) {
      LiveDataResult result = _currentResults.get(ticker);
      results.put(ticker, result.permissionCheck());
    }

    return new DefaultImmutableLiveDataResults(results);
  }

  @Override
  public ImmutableLiveDataResults createSnapshot() {
    return new DefaultImmutableLiveDataResults(_currentResults);
  }

  @Override
  public void markAsPending(ExternalIdBundle ticker) {
    _currentResults.put(ArgumentChecker.notNull(ticker, "ticker"), new PendingLiveDataResult(ticker));
  }

  @Override
  public void markAsMissing(ExternalIdBundle ticker, String userMessage, Object... args) {
    String message = formatMessage(userMessage, args);
    _currentResults.put(ArgumentChecker.notNull(ticker, "ticker"), new MissingLiveDataResult(ticker, message));
  }

  @Override
  public void markAsPermissionDenied(ExternalIdBundle ticker, String userMessage, Object... args) {
    String message = formatMessage(userMessage, args);
    _currentResults.put(ArgumentChecker.notNull(ticker, "ticker"), new PermissionDeniedLiveDataResult(message));
  }

  private String formatMessage(String userMessage, Object[] args) {
    return MessageFormatter.format(ArgumentChecker.notNull(userMessage, "userMessage"), args).getMessage();
  }

  @Override
  public boolean isPending(ExternalIdBundle ticker) {
    if (containsTicker(ticker)) {
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
