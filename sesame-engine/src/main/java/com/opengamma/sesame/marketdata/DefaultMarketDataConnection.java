/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.opengamma.livedata.LiveDataListener;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.livedata.client.AbstractLiveDataClient;
import com.opengamma.livedata.client.SubscriptionHandle;

/**
 * A connection to a live market data service
 */
public class DefaultMarketDataConnection extends AbstractLiveDataClient implements MarketDataConnection {

  @Override
  protected void handleSubscriptionRequest(Collection<SubscriptionHandle> subHandle) {

  }

  @Override
  protected void cancelPublication(LiveDataSpecification fullyQualifiedSpecification) {

  }

  @Override
  public boolean isEntitled(UserPrincipal user, LiveDataSpecification requestedSpecification) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Map<LiveDataSpecification, Boolean> isEntitled(UserPrincipal user,
                                                        Collection<LiveDataSpecification> requestedSpecifications) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void subscribe(Set<LiveDataSpecification> specifications, LiveDataListener listener) {
    subscribe(UserPrincipal.getLocalUser(), specifications, listener);
  }

  @Override
  public void unsubscribe(Set<LiveDataSpecification> specifications, LiveDataListener listener) {
    unsubscribe(UserPrincipal.getLocalUser(), specifications, listener);
  }
}
