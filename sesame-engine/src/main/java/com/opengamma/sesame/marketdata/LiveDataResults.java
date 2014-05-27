/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.opengamma.id.ExternalIdBundle;

/**
 * The results that have come in from a live market data source.
 * <p>
 * Each result is an instance of {@link LiveDataResult} and may represent
 * either an actual value or an indications of a problem.
 */
public interface LiveDataResults {

  /**
   * Indicates if a result is held for the specified ticker.
   * A result being held does not imply that market data is
   * available as the result may indicate a failure situation.
   *
   * @param ticker  the ticker to check, not null
   * @return true if a result is held for the ticker
   */
  boolean containsTicker(ExternalIdBundle ticker);

  /**
   * Gets the set of tickers that are held.
   * <p>
   * The presence of a result for a ticker does not imply that market data is
   * available as the result may indicate a failure situation.
   *
   * @return the set of tickers
   */
  ImmutableSet<ExternalIdBundle> tickerSet();

  /**
   * Returns the number of results held.
   * <p>
   * Each result is for a different ticker.
   *
   * @return the number of results held
   */
  int size();

  /**
   * Retrieves the result held for the specified ticker.
   *
   * @param ticker  the ticker to get the result for
   * @return result for the ticker, null if no result held
   */
  // TODO - should this either throw an exception or return a PENDING result if ticker is not present?
  @Nullable
  LiveDataResult get(ExternalIdBundle ticker);

  /**
   * Returns whether the result held for the specified ticker
   * indicates that the market data is still pending.
   *
   * @param ticker  the ticker to check
   * @return true if the data is still pending
   * @throws IllegalArgumentException if the ticker is not present
   */
  boolean isPending(ExternalIdBundle ticker);

}
