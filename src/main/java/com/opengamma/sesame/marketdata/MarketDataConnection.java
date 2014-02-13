/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.livedata.LiveDataListener;
import com.opengamma.livedata.LiveDataSpecification;

public interface MarketDataConnection {

  /**
   * Creates a number of non-persistent subscriptions to market data.
   *
   * @param specifications the market data to subscribe to including which
   * standardized format you want the server to give it to you.
   * @param listener the listener that receives the results of the subscription request
   */
  void subscribe(Set<LiveDataSpecification> specifications, LiveDataListener listener);


  /**
   * Deletes a market data subscription.
   *
   * @param specifications what market data you no longer want to subscribe to.
   * @param listener the listener that receives the results of the unsubscription request
   */
  void unsubscribe(Set<LiveDataSpecification> specifications, LiveDataListener listener);

}
