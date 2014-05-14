/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.id.ExternalIdBundle;

/**
 * An extension to LiveDataResults adding in methods
 * that allow the entries to be mutated. If this class
 * is to be used in a multi-threaded environment then
 * usage must be synchronized externally.
 */
public interface MutableLiveDataResults extends LiveDataResults {

  /**
   * Update the value held for the specified ticker with new data.
   * If data is already held then it will be merged with the
   * supplied data. If no data is held, a new entry containing the
   * supplied data will be created.
   *
   * @param ticker  the ticker to update, not null
   * @param update  the data to use to update the ticker, not null
   */
  void update(ExternalIdBundle ticker, LiveDataUpdate update);

  /**
   * Remove the entry held for the specified ticker.
   *
   * @param ticker  the ticker to remove, not null
   */
  void remove(ExternalIdBundle ticker);

  /**
   * Create an immutable version of this LiveDataResults, containing
   * only the results for the supplied set of tickers.
   *
   * @param tickers  the set of tickers to be included in the snapshot, not null
   * @return an ImmutableLiveDataResults containing the results for
   * the requested set of tickers, not null
   */
  ImmutableLiveDataResults createSnapshot(Set<ExternalIdBundle> tickers);

  /**
   * Create an immutable version of this LiveDataResults, containing
   * all the results.
   *
   * @return an ImmutableLiveDataResults containing all the results
   * from this LiveDataResults, not null
   */
  ImmutableLiveDataResults createSnapshot();

  /**
   * Add an entry indicating that the specified ticker is not
   * available for some reason. It is expected that the user
   * message supplies details of why the data is unavailable.
   * SLF4J message formatting can be used in the construction
   * of the user message
   *
   * @param ticker  the ticker to mark as unavailable, not null
   * @param userMessage  the message detailing why the data was
   * unavailable, not null.
   * @param args  additional parameters to be used when constructing
   * the message, not null
   */
  void markAsMissing(ExternalIdBundle ticker, String userMessage, Object... args);

  /**
   * Add an entry indicating that the specified ticker is pending.
   * Note that this method actually inserts an entry so subsequent
   * calls to {@link #containsTicker(ExternalIdBundle)} with the same
   * ticker will return true.
   *
   * @param ticker  the ticker to mark as pending, not null
   */
  void markAsPending(ExternalIdBundle ticker);

   /**
    * Add an entry indicating that the specified ticker is not
    * allowed to be viewed by the current user due to permissions.
    *
    * @param ticker  the ticker to mark as permission denied, not null
    * @param userMessage  the message detailing why the data was
    * unavailable, not null.
    * @param args  additional parameters to be used when constructing
    * the message, not null
    */
  void markAsPermissionDenied(ExternalIdBundle ticker, String userMessage, Object... args);
}
