/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.sesame.marketdata.LiveDataManager.RequestType.SUBSCRIBE;
import static com.opengamma.sesame.marketdata.LiveDataManager.RequestType.UNSUBSCRIBE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.LiveDataListener;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.LiveDataValueUpdate;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.livedata.msg.LiveDataSubscriptionResponse;
import com.opengamma.livedata.msg.LiveDataSubscriptionResult;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Responsible for the management of live market data subscriptions
 * for a single live data supplier. Reference counts the number of clients
 * interested in a particular LiveDataSpecification and automatically unsubscribes
 * some time after all clients have disconnected (this is not done immediately to
 * allow for cases where multiple clients connect once and quickly disconnect).
 */
public class LiveDataManager implements LiveDataListener {

  private static final Logger s_logger = LoggerFactory.getLogger(LiveDataManager.class);

  /**
   * The subscription requests that have been made. This is the synchronization point
   * for all market data requests
   */
  private final BlockingQueue<SubscriptionRequest<ExternalIdBundle>> _requests = new LinkedBlockingQueue<>();

  private final Map<LDListener, Set<ExternalIdBundle>> _subscriptionsPerClient = new HashMap<>();

  private final Map<ExternalIdBundle, Set<LDListener>> _clientsPerSubscription = new HashMap<>();

  private final Map<ExternalIdBundle, FudgeMsg> _currentValues = new HashMap<>();

  private final LiveDataClient _marketDataConnection;

  /**
   * Mapping between received spec and requested spec e.g. we request a Bloomberg ticker but get
   * results as Bloomberg BUIDs.
   */
  private final Map<ExternalIdBundle, ExternalIdBundle> _specificationMapping = new HashMap<>();

  // todo there are still race conditions with these latches - see the removal of them
  private final ConcurrentMap<LDListener, CountDownLatch> _latches = new ConcurrentHashMap<>();

  public LiveDataManager(LiveDataClient marketDataConnection) {
    _marketDataConnection = marketDataConnection;
    new Thread(createConsumer()).start();
  }

  // Create the consumer which monitors the request queue
  private Runnable createConsumer() {
    return new Runnable() {
        @Override
        public void run() {
          try {
            while (isRunning()) {
              consume(_requests.take());
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      };
  }


  private boolean isRunning() {
    return true;
  }

  public void makeSubscriptionRequest(SubscriptionRequest<ExternalIdBundle> request) {
    _requests.add(request);
  }

  /**
   * Returns the current data for all subscriptions the client has requested.
   * 
   * @param listener  the listener to use, not null
   * @return the snapshot, not null
   */
  public Map<ExternalIdBundle, FudgeMsg> snapshot(LDListener listener) {
    Map<ExternalIdBundle, FudgeMsg> result = new HashMap<>();
    if (_subscriptionsPerClient.containsKey(listener)) {
      for (ExternalIdBundle idBundle : _subscriptionsPerClient.get(listener)) {
        if (_currentValues.containsKey(idBundle)) {
          result.put(idBundle, _currentValues.get(idBundle));
        }
      }
      return result;
    } else {
      throw new IllegalStateException("Listener must make subscription requests before asking for snapshot");
    }
  }

  // Access to this method is controlled via the requests queue so we
  // know we are effectively single threaded
  private void consume(SubscriptionRequest<ExternalIdBundle> request) {

    LDListener client = request.getListener();

    Set<ExternalIdBundle> currentSubscriptions = _subscriptionsPerClient.get(client);
    if (currentSubscriptions == null) {
      currentSubscriptions = new HashSet<>();
      _subscriptionsPerClient.put(client, currentSubscriptions);
    }

    final Set<ExternalIdBundle> subscriptionKeys = request.getSubscriptionKeys();

    if (request.getRequestType() == SUBSCRIBE) {

      Set<ExternalIdBundle> alreadySubscribed = new HashSet<>();
      Set<ExternalIdBundle> toSubscribe = new HashSet<>();

      for (ExternalIdBundle id : subscriptionKeys) {

        // Is there already a subscription, if so just add us the interested parties
        if (_clientsPerSubscription.containsKey(id)) {
          _clientsPerSubscription.get(id).add(client);
          alreadySubscribed.add(id);
        } else {
          toSubscribe.add(id);
          Set<LDListener> clients = new HashSet<>();
          clients.add(client);
          _clientsPerSubscription.put(id, clients);
        }
        currentSubscriptions.add(id);
      }

      if (!alreadySubscribed.isEmpty()) {
        client.receiveSubscriptionResponse(new SubscriptionResponse<>(SUBSCRIBE, alreadySubscribed));
      }
      Set<LiveDataSpecification> specifications = createSpecifications(toSubscribe);

      // Add a latch if we're subscribing to something and don't already have
      // a latch waiting
      _latches.putIfAbsent(request.getListener(), new CountDownLatch(1));

      // todo the use of UserPrincipal is definitely wrong!
      _marketDataConnection.subscribe(UserPrincipal.getTestUser(), specifications, this);

    } else {

      Set<ExternalIdBundle> toUnsubscribe = new HashSet<>();

      for (ExternalIdBundle id : subscriptionKeys) {

        // We are expecting to have a subscription
        if (_clientsPerSubscription.containsKey(id)) {
          Set<LDListener> listeners = _clientsPerSubscription.get(id);
          listeners.remove(client);
          currentSubscriptions.remove(id);
          if (listeners.isEmpty()) {
            toUnsubscribe.add(id);
            // todo - remove from the spec mapping?
            //_specificationMapping.remove(id);
          }

        } else {
          s_logger.warn("Removed unsubscribe request for id: {} from client who was not subscribed", id);
        }
      }

      // todo the use of UserPrincipal is definitely wrong!
      _marketDataConnection.unsubscribe(UserPrincipal.getTestUser(), createSpecifications(toUnsubscribe), this);
      client.receiveSubscriptionResponse(new SubscriptionResponse<>(UNSUBSCRIBE, subscriptionKeys));
    }
  }

  private Set<LiveDataSpecification> createSpecifications(Set<ExternalIdBundle> toSubscribe) {

    Set<LiveDataSpecification> result = new HashSet<>(toSubscribe.size());
    for (ExternalIdBundle id : toSubscribe) {
      result.add(createSpecification(id));
    }
    return result;
  }

  private LiveDataSpecification createSpecification(ExternalIdBundle id) {
    return new LiveDataSpecification("OpenGamma", id);
  }

  @Override
  public void subscriptionResultReceived(LiveDataSubscriptionResponse subscriptionResult) {
    subscriptionResultsReceived(ImmutableSet.of(subscriptionResult));
  }

  @Override
  public void subscriptionResultsReceived(Collection<LiveDataSubscriptionResponse> subscriptionResults) {
    // consider events in the following order
    // subscription request 1 -> MD subscribe
    // request 2 (subscription already but before listener is added)
    // response from MD server updates client 1 but not client 2
    // client 2 loses an update

    SubscriptionResultRecorder recorder = new SubscriptionResultRecorder();

    for (LiveDataSubscriptionResponse result : subscriptionResults) {

      ExternalIdBundle requestedBundle = result.getRequestedSpecification().getIdentifiers();
      ExternalIdBundle returnedBundle = result.getFullyQualifiedSpecification().getIdentifiers();
      _specificationMapping.put(returnedBundle,  requestedBundle);

      // Bloomberg will reply to a ticker request with a BUID so we need to
      // keep track of the mapping

      if (result.getSubscriptionResult() == LiveDataSubscriptionResult.SUCCESS) {

        recorder.success(requestedBundle, _clientsPerSubscription.get(requestedBundle));

        // todo - we may get a snapshot value in here - use it if we do

      } else {

        if (result.getSubscriptionResult() == LiveDataSubscriptionResult.NOT_AUTHORIZED) {
          s_logger.warn("Subscription to {} failed because user is not authorised: {}", result.getRequestedSpecification(), result);
        } else {
          s_logger.warn("Subscription to {} failed: {}", result.getRequestedSpecification(), result);
        }

        recorder.failure(requestedBundle, _clientsPerSubscription.get(requestedBundle), result.getUserMessage());
        final Set<LDListener> removed = _clientsPerSubscription.remove(requestedBundle);
        for (LDListener listener : removed) {
          _subscriptionsPerClient.get(listener).remove(requestedBundle);
        }

      }
    }

    for (Map.Entry<LDListener, Pair<Map<ExternalIdBundle, String>, Set<ExternalIdBundle>>> entry : recorder.getResults().entrySet()) {
      LDListener listener = entry.getKey();
      Pair<Map<ExternalIdBundle, String>, Set<ExternalIdBundle>> results = entry.getValue();
      Map<ExternalIdBundle, String> failures = results.getFirst();
      Set<ExternalIdBundle> successes = results.getSecond();
      listener.receiveSubscriptionResponse(new SubscriptionResponse<>(SUBSCRIBE, successes, failures));
    }

  }

  public void waitForAllData(LDListener listener) {

    CountDownLatch latch = _latches.get(listener);
    if (latch != null && latch.getCount() > 0) {
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new OpenGammaRuntimeException("Got error waiting for latch on market data", e);
      }
    }
  }

  private static class SubscriptionResultRecorder {

    // Pair<Failures, Successes>
    private final Map<LDListener, Pair<Map<ExternalIdBundle, String>, Set<ExternalIdBundle>>> _results = new HashMap<>();

    public void success(ExternalIdBundle idBundle, Set<LDListener> listeners) {
      if (listeners != null && !listeners.isEmpty()) {
        for (LDListener listener : listeners) {
          getOrCreateBucket(listener).getSecond().add(idBundle);
        }
      }
    }
    public void failure(ExternalIdBundle idBundle, Set<LDListener> listeners, String reason) {
      if (listeners != null && !listeners.isEmpty()) {
        for (LDListener listener : listeners) {
          getOrCreateBucket(listener).getFirst().put(idBundle, reason);
        }
      }
    }

    public Map<LDListener, Pair<Map<ExternalIdBundle, String>, Set<ExternalIdBundle>>> getResults() {
      return _results;
    }

    private Pair<Map<ExternalIdBundle, String>, Set<ExternalIdBundle>> getOrCreateBucket(LDListener listener) {
      if (!_results.containsKey(listener)) {
        _results.put(listener,
                     Pairs.<Map<ExternalIdBundle, String>, Set<ExternalIdBundle>>of(new HashMap<ExternalIdBundle, String>(), new HashSet<ExternalIdBundle>()));
      }
      return _results.get(listener);
    }
  }

  @Override
  public void subscriptionStopped(LiveDataSpecification fullyQualifiedSpecification) {
    // nothing to do
  }

  @Override
  public void valueUpdate(LiveDataValueUpdate valueUpdate) {

    s_logger.debug("Update received {}", valueUpdate);
    ExternalIdBundle idBundle = valueUpdate.getSpecification().getIdentifiers();
    ExternalIdBundle mappedBundle = _specificationMapping.get(idBundle);

    if (!_clientsPerSubscription.containsKey(mappedBundle)) {
      s_logger.warn("Received value update for which no subscriptions were found: {}", mappedBundle);
      return;
    }

    // todo - we may want to filter so we only retain fields that have been requested
    FudgeMsg originalValues = _currentValues.get(mappedBundle);
    FudgeMsg newValues = valueUpdate.getFields();

    FudgeMsg mergedValues;
    if (originalValues == null) {
      mergedValues = newValues;
    } else {
      MutableFudgeMsg unionMsg = OpenGammaFudgeContext.getInstance().newMessage(newValues);
      Set<String> missingFields = originalValues.getAllFieldNames();
      missingFields.removeAll(newValues.getAllFieldNames());
      for (String missingField : missingFields) {
        unionMsg.add(originalValues.getByName(missingField));
      }
      mergedValues = unionMsg;
    }

    _currentValues.put(mappedBundle, mergedValues);

    for (LDListener listener : _clientsPerSubscription.get(mappedBundle)) {
      listener.valueUpdated(mappedBundle);

      // Check if this client is waiting on data completion
      final CountDownLatch latch = _latches.get(listener);
      if (latch != null && clientsRequirementsAreSatisfied(listener)) {
        latch.countDown();
        _latches.remove(listener);
      }
    }
  }

  private boolean clientsRequirementsAreSatisfied(LDListener listener) {
    return _currentValues.keySet().containsAll(_subscriptionsPerClient.get(listener));
  }


  public enum RequestType {
    SUBSCRIBE, UNSUBSCRIBE
  }

  public static class SubscriptionRequest<K> {

    private final LDListener _listener;

    private final RequestType _requestType;

    private final Set<K> _subscriptionKeys;

    public SubscriptionRequest(LDListener listener,
                               RequestType requestType,
                               Set<K> subscriptionKeys) {
      _listener = ArgumentChecker.notNull(listener, "listener");
      _requestType = ArgumentChecker.notNull(requestType, "requestType");
      _subscriptionKeys = ArgumentChecker.notEmpty(subscriptionKeys, "subscriptionKeys");
    }

    public SubscriptionRequest(LDListener listener,
                               RequestType subscribe,
                               K idBundle) {
      this(listener, subscribe, ImmutableSet.of(idBundle));
    }

    public LDListener getListener() {
      return _listener;
    }

    public RequestType getRequestType() {
      return _requestType;
    }

    public Set<K> getSubscriptionKeys() {
      return _subscriptionKeys;
    }
  }

  public static class SubscriptionResponse<K> {

    private final RequestType _requestType;

    private final Set<K> _successes;

    private final Map<K, String> _failures;

    public SubscriptionResponse(RequestType requestType, Set<K> successes, Map<K, String> failures) {
      _requestType = ArgumentChecker.notNull(requestType, "requestType");
      _successes = ArgumentChecker.notNull(successes, "successes");
      _failures = ArgumentChecker.notNull(failures, "failures");
    }

    public SubscriptionResponse(RequestType requestType, Set<K> successes) {
      this(requestType, successes, ImmutableMap.<K, String>of());
    }

    public RequestType getRequestType() {
      return _requestType;
    }

    public Set<K> getSuccesses() {
      return _successes;
    }

    public Map<K, String> getFailures() {
      return _failures;
    }
  }

  public interface LDListener {

    void receiveSubscriptionResponse(SubscriptionResponse<ExternalIdBundle> externalIdBundleSubscriptionResponse);

    void valueUpdated(ExternalIdBundle idBundle);
  }

}
