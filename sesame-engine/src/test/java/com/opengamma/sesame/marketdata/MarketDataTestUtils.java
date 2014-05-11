/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.livedata.msg.LiveDataSubscriptionResult.NOT_PRESENT;
import static org.mockito.Mockito.mock;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.msg.LiveDataSubscriptionResponse;
import com.opengamma.livedata.msg.LiveDataSubscriptionResult;

/**
 * Static helper methods for testing market data retrieval.
 */
class MarketDataTestUtils {

  static ExternalIdBundle createBundle(String ticker) {
    return createTicker(ticker).toBundle();
  }

  static ExternalId createTicker(String ticker) {
    return ExternalId.of("TICKER", ticker);
  }

  static LiveDataSpecification createLiveDataSpec(String ticker) {
    return new LiveDataSpecification("OpenGamma", createBundle(ticker));
  }

  static DefaultLiveDataManager createLiveDataManager() {
    return createLiveDataManager(createMockLiveDataClient());
  }

  static DefaultLiveDataManager createLiveDataManager(LiveDataClient liveDataClient) {
    // Don't delay unsubscribe events as it hampers testing
    return new DefaultLiveDataManager(liveDataClient, 0);
  }

  static LiveDataClient createMockLiveDataClient() {
    return mock(LiveDataClient.class);
  }

  static LiveDataSubscriptionResponse buildSuccessResponse(String ticker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    return new LiveDataSubscriptionResponse(specification, LiveDataSubscriptionResult.SUCCESS, null, null, null, null);
  }

  static LiveDataSubscriptionResponse buildSuccessResponse(String ticker, String mappedTicker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    LiveDataSpecification fqSpecification = createLiveDataSpec(mappedTicker);
    return new LiveDataSubscriptionResponse(specification, LiveDataSubscriptionResult.SUCCESS, null, fqSpecification, null, null);
  }

  static LiveDataSubscriptionResponse buildFailureResponse(String ticker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    return new LiveDataSubscriptionResponse(specification, NOT_PRESENT, "Data is not available", null, null, null);
  }

  static LiveDataSubscriptionResponse buildFailureResponse(String ticker, String mappedTicker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    LiveDataSpecification fqSpecification = createLiveDataSpec(mappedTicker);
    return new LiveDataSubscriptionResponse(specification, NOT_PRESENT, "Data is not available", fqSpecification, null, null);
  }
}
