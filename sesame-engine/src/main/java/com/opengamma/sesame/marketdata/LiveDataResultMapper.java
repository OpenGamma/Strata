/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;

/**
 * Holds the results that have come in from a live market data
 * source. The results may be actual values or maybe indications
 * that something is in error.
 */
public interface LiveDataResultMapper {

  /**
   * Indicates if a result is held for the specified ticker.
   * A result being held does not imply that market data is
   * available as the result may indicate a failure situation.
   *
   * @param ticker  the ticker to check, not null
   * @return true if a result is held for the ticker
   */
  boolean containsKey(ExternalIdBundle ticker);

  /**
   * Retrieves the result held for the specified ticker.
   *
   * @param ticker  the ticker to get the result for, not null
   * @return result for the ticker, null if no result held
   */
  // TODO - should this either throw an exception or return a PENDING result if ticker is not present?
  LiveDataResult get(ExternalIdBundle ticker);

  /**
   * Returns the number of results held.
   *
   * @return the number of results held
   */
  int size();

  /**
   * Returns whether the result held for the specified ticker
   * indicates that the market data is still pending.
   *
   * @param ticker  the ticker to check, not null
   * @return true if the data is still pending
   * @throws IllegalArgumentException if the ticker is not present
   */
  boolean isPending(ExternalIdBundle ticker);
}
