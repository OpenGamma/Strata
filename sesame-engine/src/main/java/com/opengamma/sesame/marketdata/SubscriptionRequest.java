/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.util.ArgumentChecker;

/**
 * Simple value class representing a subscription or unsubscription
 * request. The request will typically be be handled by a
 * {@link LiveDataManager} implementation.
 *
 * @param <K>  the type used as the identifier for the subscription
 */
public class SubscriptionRequest<K> {

  /**
   * The possible subscription request types.
   */
  public enum RequestType {
    /**
     * Subscription request.
     */
    SUBSCRIBE,
    /**
     * Unsubscription request.
     */
    UNSUBSCRIBE
  }

  /**
   * The type of this request.
   */
  private final RequestType _requestType;

  /**
   * The keys to be subscribed to or unsubscribed from.
   */
  private final Set<K> _subscriptionKeys;

  /**
   * Create a new request.
   *
   * @param requestType  the type of this request, not null
   * @param subscriptionKeys  the keys to be subscribed to or
   * unsubscribed from, not null
   */
  public SubscriptionRequest(RequestType requestType,
                             Set<K> subscriptionKeys) {
    _requestType = ArgumentChecker.notNull(requestType, "requestType");
    _subscriptionKeys = ArgumentChecker.notEmpty(subscriptionKeys, "subscriptionKeys");
  }

  /**
   * Get the request type.
   *
   * @return the request type, not null
   */
  public RequestType getRequestType() {
    return _requestType;
  }

  /**
   * Get the keys be subscribed to or unsubscribed from.
   *
   * @return the subscription keys, not null
   */
  public Set<K> getSubscriptionKeys() {
    return _subscriptionKeys;
  }
}
