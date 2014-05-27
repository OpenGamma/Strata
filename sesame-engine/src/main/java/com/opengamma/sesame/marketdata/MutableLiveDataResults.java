/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.id.ExternalIdBundle;

/**
 * A mutable representation of the results of live market data.
 * <p>
 * This extends {@link LiveDataResults} adding in methods that allow the
 * entries to be mutated. If this class is to be used in a multi-threaded
 * environment then usage must be synchronized externally.
 * <p>
 * Implementations of this interface should be mutable.
 */
public interface MutableLiveDataResults extends LiveDataResults {

  /**
   * Update the value held for the specified ticker with new data.
   * <p>
   * If data is already held then it will be merged with the supplied data.
   * If no data is held, a new entry containing the supplied data will be created.
   *
   * @param ticker  the ticker to update
   * @param update  the data to use to update the ticker
   */
  void update(ExternalIdBundle ticker, LiveDataUpdate update);

  /**
   * Remove the entry held for the specified ticker.
   *
   * @param ticker  the ticker to remove
   */
  void remove(ExternalIdBundle ticker);

  /**
   * Create an immutable snapshot of the current state of the results.
   * <p>
   * The snapshot will contain only those tickers specified.
   *
   * @param tickers  the set of tickers to be included in the snapshot
   * @return an ImmutableLiveDataResults containing the results for
   *  the requested set of tickers
   */
  ImmutableLiveDataResults createSnapshot(Set<ExternalIdBundle> tickers);

  /**
   * Create an immutable snapshot of the current state of the results.
   * <p>
   * The snapshot will contain all tickers that have been recorded.
   *
   * @return an ImmutableLiveDataResults containing all recorded results
   */
  ImmutableLiveDataResults createSnapshot();

  //-------------------------------------------------------------------------
  /**
   * Marks the entry for the specified ticker indicating that it is missing.
   * <p>
   * Calling this method replaces any data previously stored for the ticker.
   * If no entry was previously present, an entry is added.
   * <p>
   * The message should indicate why data is unavailable.
   * <p>
   * Formatting of the message uses placeholders as per SLF4J.
   * Each {} in the message is replaced by the next message argument.
   * 
   * @param ticker  the ticker to mark as unavailable
   * @param userMessage  the message detailing why the data was unavailable with {} placeholders
   * @param args  the message arguments to be used when constructing the message
   */
  void markAsMissing(ExternalIdBundle ticker, String userMessage, Object... args);

  /**
   * Marks the entry for the specified ticker indicating that it is pending.
   * <p>
   * Calling this method replaces any data previously stored for the ticker.
   * If no entry was previously present, an entry is added.
   *
   * @param ticker  the ticker to mark as pending
   */
  void markAsPending(ExternalIdBundle ticker);

  /**
   * Marks the entry for the specified ticker indicating that permission is denied for the current user.
   * <p>
   * Calling this method replaces any data previously stored for the ticker.
   * If no entry was previously present, an entry is added.
   * <p>
   * The message should indicate why permission is denied.
   * Formatting of the message uses placeholders as per SLF4J.
   * Each {} in the message is replaced by the next message argument.
   *
   * @param ticker  the ticker to mark as permission denied
   * @param userMessage  the message detailing why permission was denied with {} placeholders
   * @param args  the message arguments to be used when constructing the message
   */
  void markAsPermissionDenied(ExternalIdBundle ticker, String userMessage, Object... args);

}
