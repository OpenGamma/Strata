/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;

/**
 * Client listener for the market data manager. The manager notifies
 * the listener as soon as data it is interested in has been updated.
 *
 * Note that although the manager notifies the listener, the manager
 * does not automatically pass the data back to the listener. This
 * allows the listener implementation to decide an appropriate point
 * to pick up the data e.g. only every 30s, only in response to
 * direct user request, immediately on a change etc. This prevents
 * unnecessary copying of data when the listener decides to ignore
 * some updates.
 */
public interface LDListener {

  /**
   * Indicates that a value that the listener registered an interest in
   * has been updated. It is for the listener to decide what action it
   * wants to take in response to this.
   *
   * @param idBundle the id of the data that was updated, not null
   */
  void valueUpdated(ExternalIdBundle idBundle);
}
