/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import org.fudgemsg.FudgeMsg;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Responsible for managing live market data subscriptions
 * for a set of client processes. Each client can have their
 * own set of subscriptions and they will be notified as
 * the market data gets updated.
 * <p>
 * The general approach would be for a client to ask for
 * a set of subscriptions, wait for the subscriptions to
 * be fulfilled, then request the data corresponding to the
 * subscriptions. The client can then make additional
 * subscriptions as required. When completed, the client
 * unregisters itself.
 */
public interface LiveDataManager {

  /**
   * Make a subscription for market data for the specified
   * client. The data will be requested from a market data
   * source and latest values can be obtained by calling the
   * {@link #snapshot(LDListener)} method.
   *
   * @param client  the client the data is for, not null
   * @param request  the market data subscriptions required, not null
   */
  void makeSubscriptionRequest(LDListener client,
                               SubscriptionRequest<ExternalIdBundle> request);

  /**
   * Block until all subscriptions requested for this client
   * have been completed. This means that each of the subscriptions
   * will either have data or a failure state (e.g missing, permission
   * denied). No pending data will be left.
   *
   * @param client  the client the data is for, not null
   */
  void waitForAllData(LDListener client);

  /**
   * Returns the current data for all subscriptions the client has
   * requested. If no subscriptions have been made then an
   * {@link IllegalStateException} will be thrown.
   * <p>
   * The data will be permission checked to ensure the
   * user is entitled to see it. If not, then a FailureResult with status
   * {@link FailureStatus#PERMISSION_DENIED} will be returned for it in
   * the snapshot. The authentication used in this method relies on
   * threadlocal context provided by Apache Shiro. If data has been
   * requested for a particular ticker but has not yet arrived from
   * the market data source then a FailureResult with status
   * {@link FailureStatus#PENDING_DATA} will be returned for it in
   * the snapshot.
   *
   * @param client  the client to use, not null
   * @return the snapshot, not null
   * @throws IllegalStateException if {@link #makeSubscriptionRequest(LDListener, SubscriptionRequest)}
   * has not previously been called, or the client has been unregistered
   */
  Map<ExternalIdBundle, Result<FudgeMsg>> snapshot(LDListener client);

  /**
   * Unregister the client. This will unsubscribe tickers for which
   * there are no other clients.
   *
   * @param client  the client to unsubscribe, not null
   */
  void unregister(LDListener client);
}
