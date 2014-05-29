/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.shiro.subject.Subject;
import org.fudgemsg.FudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.LiveDataListener;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.LiveDataValueUpdate;
import com.opengamma.livedata.LiveDataValueUpdateBean;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.livedata.msg.LiveDataSubscriptionResponse;
import com.opengamma.livedata.msg.LiveDataSubscriptionResult;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.auth.AuthUtils;

/**
 * Responsible for the management of live market data subscriptions
 * for a single live data supplier. Keeps track of the clients
 * interested in a particular LiveDataSpecification and automatically unsubscribes
 * some time after all clients have disconnected (this is not done immediately to
 * allow for cases where multiple clients connect once and quickly disconnect).
 * <p>
 * Each client will be associated with a particular user and before the data is
 * returned to them, permissions will be checked to ensure they are entitled
 * to access the data.
 * <p>
 * Class maintains a record for each subscription request storing a Result
 * object holding either the data for the ticker, or a Failure indicating why
 * the ticker is not available. As soon as a request is made, a Failure with status
 * PENDING_DATA will be stored. If the subscription is successful then this will
 * get replaced with the actual values as they become available.
 * <p>
 * Methods can safely be called from multiple threads as all access to the
 * data structured is via a single threaded queue. For this reason, none of
 * the data structures are concurrency aware.
 *
 */
public class DefaultLiveDataManager implements LiveDataListener, LiveDataManager {

  /**
   * Logger for the class.
   */
  private static final Logger s_logger = LoggerFactory.getLogger(DefaultLiveDataManager.class);

  /**
   * Default number of seconds to wait before unsubscribing
   * from a ticker.
   */
  public static final int DEFAULT_UNSUBSCRIPTION_DELAY_SECONDS = 300;

  /**
   * The connection (ultimately) to the market data server.
   */
  private final LiveDataClient _marketDataConnection;

  /**
   * In order to avoid both race conditions and synchronization, all
   * methods are routed through this central queue. As data is read
   * off the queue, the Callable or Runnable is run which in turn will
   * call the appropriate internal method.
   */
  private final ScheduledExecutorService _commandQueue = Executors.newSingleThreadScheduledExecutor();

  /**
   * Maintains the mapping between clients and their subscriptions. Note
   * that we keep track of tickers as they are known to the engine, not the
   * fully qualified specs that may be returned from market data servers.
   */
  private final ClientSubscriptionManager _clientSubscriptions = new ClientSubscriptionManager();

  /**
   * LiveDataResults storing the latest values for all subscriptions
   * that have been requested across the entire set of listeners. The value
   * held will either be a Success and contain the underlying market data or will
   * be a Failure and hold the failure reason.
   */
  private final MutableLiveDataResults _currentValues;

  /**
   * Mapping from the ticker received from the market data server to the
   * ticker originally requested in the engine. E.g. we request a Bloomberg
   * ticker but get results as Bloomberg BUIDs, so this map would hold
   * a mapping of BUID -> Ticker.
   */
  private final Map<ExternalIdBundle, ExternalIdBundle> _specificationMapping = new HashMap<>();

  /**
   * Latches that are used when a client subscribes to data and
   * then calls {@link #waitForAllData(LDListener)}.
   */
  private final Map<LDListener, CountDownLatch> _latches = new HashMap<>();

  /**
   * Time to wait after a client has unregistered before unsubscribing
   * from tickers for which they were the only client. This is to
   * avoid market data server sub/unsub cycles when the same view is
   * stopped and started in quick succession.
   */
  private final int _unsubscriptionDelaySeconds;

  /**
   * Create a live data manger, obtaining market data from the
   * supplied market data connection. This uses a default value
   * for an unsubscription delay.
   *
   * @param marketDataConnection  the connection to use to subscribe, not null
   */
  public DefaultLiveDataManager(LiveDataClient marketDataConnection) {
   this(marketDataConnection, DEFAULT_UNSUBSCRIPTION_DELAY_SECONDS);
  }

  /**
   * Create a live data manger, obtaining market data from the
   * supplied market data connection. When an unsubscription occurs,
   * the unsubscribe will not be sent to the market data source
   * until at least the specified number of seconds has elapsed.
   *
   * @param marketDataConnection  the connection to use to subscribe, not null
   * @param unsubscriptionDelay  number of seconds to wait before unsubscribing
   */
  public DefaultLiveDataManager(LiveDataClient marketDataConnection,
                               int unsubscriptionDelay) {
    _marketDataConnection = ArgumentChecker.notNull(marketDataConnection, "marketDataConnection");
    _unsubscriptionDelaySeconds = unsubscriptionDelay;
    _currentValues = new DefaultMutableLiveDataResults();
  }

  @Override
  public ImmutableLiveDataResults snapshot(final LDListener listener) {

    Callable<ImmutableLiveDataResults> callable = new Callable<ImmutableLiveDataResults>() {
      @Override
      public ImmutableLiveDataResults call() {
        return doSnapshot(listener);
      }
    };

    // We need to ensure that we run the callable with the same user
    // details as the client was using
    Subject subject = AuthUtils.getSubject();
    Future<ImmutableLiveDataResults> future = _commandQueue.submit(subject.associateWith(callable));

    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Error waiting on result from future", e);
    } catch (ExecutionException e) {
      // Throw the cause of the exception, possibly wrapping in RuntimeException
      throw Throwables.propagate(e.getCause());
    }
  }

  private ImmutableLiveDataResults doSnapshot(LDListener client) {
    if (_clientSubscriptions.containsClient(client)) {
      Set<ExternalIdBundle> subscriptions = _clientSubscriptions.getSubscriptionsForClient(client);
      return _currentValues.createSnapshot(subscriptions);
    } else {
      throw new IllegalStateException("Listener must make subscription requests before asking for snapshot");
    }
  }

  @Override
  public void subscribe(final LDListener client,
                        final Set<ExternalIdBundle> tickers) {
    _commandQueue.submit(new Runnable() {
      @Override
      public void run() {
        doSubscribe(client, tickers);
      }
    });
  }

  @Override
  public void unsubscribe(final LDListener client,
                        final Set<ExternalIdBundle> tickers) {
    _commandQueue.submit(new Runnable() {
      @Override
      public void run() {
        doUnsubscribe(client, tickers);
      }
    });
  }

  @Override
  public void subscriptionResultReceived(LiveDataSubscriptionResponse subscriptionResult) {
    subscriptionResultsReceived(ImmutableSet.of(subscriptionResult));
  }

  @Override
  public void subscriptionResultsReceived(final Collection<LiveDataSubscriptionResponse> subscriptionResponses) {
    _commandQueue.submit(new Runnable() {
      @Override
      public void run() {
        doSubscriptionResultsReceived(subscriptionResponses);
      }
    });
  }

  private void doSubscriptionResultsReceived(Collection<LiveDataSubscriptionResponse> subscriptionResponses) {

    // Keep track of which tickers we have received responses
    // for (including failures)
    Set<ExternalIdBundle> responsesReceived = new HashSet<>();

    for (LiveDataSubscriptionResponse response : subscriptionResponses) {

      ExternalIdBundle requestedBundle = response.getRequestedSpecification().getIdentifiers();
      responsesReceived.add(requestedBundle);

      // Bloomberg will reply to a ticker request with a BUID so we need to
      // keep track of the mapping
      LiveDataSpecification specification = response.getFullyQualifiedSpecification();

      // If we're no given a fully qualified spec, then use the original
      ExternalIdBundle returnedBundle = specification == null ? requestedBundle : specification.getIdentifiers();
      _specificationMapping.put(returnedBundle, requestedBundle);

      if (response.getSubscriptionResult() == LiveDataSubscriptionResult.SUCCESS) {

        final LiveDataValueUpdateBean snapshot = response.getSnapshot();
        if (snapshot != null) {
          updateCurrentValue(requestedBundle, snapshot.getFields());
        }
      } else {
        if (response.getSubscriptionResult() == LiveDataSubscriptionResult.NOT_AUTHORIZED) {
          s_logger.warn("Subscription to {} failed because user is not authorised: {}",
                        response.getRequestedSpecification(), response);
          // We are not expecting this as generally the market data server has
          // access to all data which we check permissions for in this class
          _currentValues.markAsPermissionDenied(requestedBundle, response.getUserMessage());
        } else {
          s_logger.warn("Subscription to ticker {} failed with response: [{}]", response.getRequestedSpecification(), response);
          _currentValues.markAsMissing(requestedBundle, response.getUserMessage());
        }
      }
    }
    notifyClients(responsesReceived);
  }

  private void doSubscribe(LDListener client, Set<ExternalIdBundle> subscriptionKeys) {

    // Subscriptions we are actually going to have to ask the
    // market data source for
    Set<ExternalIdBundle> requiredSubscriptions = new HashSet<>();

    for (ExternalIdBundle id : subscriptionKeys) {
      _clientSubscriptions.addClientSubscription(client, id);

      // Add in placeholder if one isn't there already
      if (!_currentValues.containsTicker(id)) {
        _currentValues.markAsPending(id);
        requiredSubscriptions.add(id);
      }
    }

    if (!requiredSubscriptions.isEmpty()) {

      // Add a latch if we're subscribing to something new
      // and don't already have a latch waiting
      if (!_latches.containsKey(client)) {
        _latches.put(client, new CountDownLatch(1));
      }
      _marketDataConnection.subscribe(getMarketDataUser(), createSpecifications(requiredSubscriptions), this);
    }
  }

  private void doUnsubscribe(LDListener client, Set<ExternalIdBundle> subscriptionKeys) {
    Set<ExternalIdBundle> redundant = _clientSubscriptions.removeClientSubscriptions(client, subscriptionKeys);
    scheduleUnsubscribe(redundant);
  }

  private UserPrincipal getMarketDataUser() {
    // TODO the use of UserPrincipal is definitely wrong but MDS currently requires one
    return UserPrincipal.getTestUser();
  }

  private Set<LiveDataSpecification> createSpecifications(Set<ExternalIdBundle> requiredSubscriptions) {
    Set<LiveDataSpecification> result = new HashSet<>(requiredSubscriptions.size());
    for (ExternalIdBundle id : requiredSubscriptions) {
      result.add(createSpecification(id));
    }
    return result;
  }

  private LiveDataSpecification createSpecification(ExternalIdBundle id) {
    return new LiveDataSpecification("OpenGamma", id);
  }

  @Override
  public void waitForAllData(final LDListener listener) {

    // We want to wait on the latch which was setup when the
    // original subscription was. However, to get it we need
    // to go via the command queue and return it in a future.
    // We can then wait on the latch. Note that we must not
    // wait in the single threaded part as everything would
    // seize up. This accounts for the slightly clunky mechanism
    CountDownLatch latch = retrieveLatch(listener);

    if (latch != null) {
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new OpenGammaRuntimeException("Got error waiting for latch on market data", e);
      }
    }
  }

  private CountDownLatch retrieveLatch(final LDListener listener) {

    Future<CountDownLatch> future = _commandQueue.submit(new Callable<CountDownLatch>() {
      @Override
      public CountDownLatch call() throws Exception {
        return _latches.get(listener);
      }
    });
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Error whilst waiting to retrieve latch");
    } catch (ExecutionException e) {
      // Throw the cause of the exception, possibly wrapping in RuntimeException
      throw Throwables.propagate(e.getCause());
    }
  }

  @Override
  public void subscriptionStopped(LiveDataSpecification fullyQualifiedSpecification) {
    // nothing to do
  }

  @Override
  public void valueUpdate(final LiveDataValueUpdate valueUpdate) {
    _commandQueue.submit(new Runnable() {
      @Override
      public void run() {
        doValueUpdate(valueUpdate);
      }
    });
  }

  private void doValueUpdate(LiveDataValueUpdate valueUpdate) {

    // The market data server id bundle
    ExternalIdBundle serverIdBundle = valueUpdate.getSpecification().getIdentifiers();

    // The engine id bundle
    ExternalIdBundle idBundle = _specificationMapping.get(serverIdBundle);
    if (idBundle == null) {
      s_logger.warn("Received value update for which no subscription mapping was found: {}", serverIdBundle);
      return;
    }

    if (!_clientSubscriptions.containsSubscription(idBundle)) {
      s_logger.warn("Received value update for which no subscriptions were found: {}", idBundle);
      return;
    }

    updateCurrentValue(idBundle, valueUpdate.getFields());
    notifyClients(idBundle);
  }

  private void updateCurrentValue(ExternalIdBundle idBundle, FudgeMsg updatedValues) {
    LiveDataUpdate update = LiveDataUpdate.fromFudge(updatedValues);
    _currentValues.update(idBundle, update);
  }

  private void notifyClients(ExternalIdBundle idBundle) {
    for (LDListener listener : _clientSubscriptions.getClientsForSubscription(idBundle)) {
      notifyClient(listener);
    }
  }

  private void notifyClients(Set<ExternalIdBundle> idBundles) {

    // We only want to notify each client once for the whole
    // set of tickers, not once per ticker. For this reason we
    // start with the set of all clients. If we notify a client,
    // we remove them from this set so they don't get notified
    // again. There may be nothing for a client in this set of
    // updates (but we have to go through them all before we
    // know this). However, if the set becomes empty we know we
    // can stop processing as we must have notified each client
    // already.
    Set<LDListener> unnotified = new HashSet<>(_clientSubscriptions.getClients());

    for (Iterator<ExternalIdBundle> it = idBundles.iterator(); it.hasNext() && !unnotified.isEmpty();) {
      ExternalIdBundle idBundle = it.next();
      for (LDListener client : _clientSubscriptions.getClientsForSubscription(idBundle)) {
        if (unnotified.contains(client)) {
          notifyClient(client);
          unnotified.remove(client);
        }
      }
    }
  }

  private void notifyClient(LDListener listener) {
    listener.valueUpdated();

    // Check if this client is potentially waiting on data completion
    if (_latches.containsKey(listener) && clientsRequirementsAreSatisfied(listener)) {
      // Latch is no longer required so we can remove
      // it and complete it
      _latches.remove(listener).countDown();
    }
  }

  @Override
  public void unregister(final LDListener listener) {
    _commandQueue.submit(new Runnable() {
      @Override
      public void run() {
        doUnregister(listener);
      }
    });
  }

  private void doUnregister(LDListener client) {
    Set<ExternalIdBundle> redundantSubscriptions = _clientSubscriptions.removeClient(client);
    scheduleUnsubscribe(redundantSubscriptions);
  }

  private void scheduleUnsubscribe(final Set<ExternalIdBundle> redundantSubscriptions) {

    if (!redundantSubscriptions.isEmpty()) {

      Runnable unsubscribeCommand = new Runnable() {
        @Override
        public void run() {
          doUnsubscribe(redundantSubscriptions);
        }
      };

      // Schedule removal of redundant subs for some time in the future
      _commandQueue.schedule(unsubscribeCommand, _unsubscriptionDelaySeconds, SECONDS);
    }
  }

  private void doUnsubscribe(Set<ExternalIdBundle> redundantSubscriptions) {

    Set<LiveDataSpecification> toUnsubscribe = new HashSet<>();

    // We need to unsubscribe using mapped key but the mapping we hold
    // is the reverse of what we need. So iterate that map and check for
    // matches
    for (Iterator<Map.Entry<ExternalIdBundle, ExternalIdBundle>> it = _specificationMapping.entrySet().iterator();
         it.hasNext(); ) {

      Map.Entry<ExternalIdBundle, ExternalIdBundle> entry = it.next();

      // The id for the spec as we know it
      ExternalIdBundle spec = entry.getValue();
      if (redundantSubscriptions.contains(spec)) {

        // Check that we do still want to unsubscribe i.e. that no
        // one else has subscribed in the interim
        if (!_clientSubscriptions.containsSubscription(spec)) {
          toUnsubscribe.add(createSpecification(entry.getKey()));
          _currentValues.remove(spec);
          it.remove();
        }
      }
    }
    if (!toUnsubscribe.isEmpty()) {
      _marketDataConnection.unsubscribe(getMarketDataUser(), toUnsubscribe, this);
    }
  }

  private boolean clientsRequirementsAreSatisfied(LDListener client) {
    for (ExternalIdBundle requirement : _clientSubscriptions.getSubscriptionsForClient(client)) {
      if (_currentValues.isPending(requirement)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Keeps track of the client/subscription mapping. Internally uses
   * a {@link BidirectionalMultiMap} to maintain the mapping. Internally
   * we keep track of tickers as they are known to the system, not the
   * fully qualified specs that may be returned from market data servers.
   */
  private static class ClientSubscriptionManager {

    private final BidirectionalMultiMap<LDListener, ExternalIdBundle> _clientSubscriptions =
        new BidirectionalMultiMap<>();

    /**
     * Indicates if this client already has subscription mappings.
     *
     * @param client  the client to check for mappings
     * @return true if the client has mappings
     */
    private boolean containsClient(LDListener client) {
      return _clientSubscriptions.containsKey(client);
    }

    /**
     * Add a subscription for the specified client.
     *
     * @param client  the client
     * @param subscription  the subscription id
     */
    private void addClientSubscription(LDListener client, ExternalIdBundle subscription) {
      _clientSubscriptions.put(client, subscription);
    }

    /**
     * Indicates if this subscription already has subscription mappings.
     *
     * @param subscription  the client to check for mappings
     * @return true if the client has mappings
     */
    private boolean containsSubscription(ExternalIdBundle subscription) {
      return _clientSubscriptions.inverse().containsKey(subscription);
    }

    /**
     * Get the subscription mappings for the specifed client.
     *
     * @param client  the client to get mappings for
     * @return the set of subscriptions
     */
    private Set<ExternalIdBundle> getSubscriptionsForClient(LDListener client) {
      return _clientSubscriptions.get(client);
    }

    /**
     * Get the client mappings for the specified subscription.
     *
     * @param subscription  the subscription to get mappings for
     * @return the set of clients
     */
    private Collection<LDListener> getClientsForSubscription(ExternalIdBundle subscription) {
      return _clientSubscriptions.inverse().get(subscription);
    }

    /**
     * Get the set of clients who have subscription mappings.
     *
     * @return the set of clients
     */
    private Set<LDListener> getClients() {
      return _clientSubscriptions.keySet();
    }

    /**
     * Remove the specified set of subscriptions from the client.
     *
     * @param client the client to remove subscriptions from
     * @param subscriptions the subscriptions to remove
     * @return the subset of the subscriptions which are no
     * longer mapped to any client
     */
    private Set<ExternalIdBundle> removeClientSubscriptions(LDListener client, Set<ExternalIdBundle> subscriptions) {
      Set<ExternalIdBundle> redundantSpecs = new HashSet<>();
      for (ExternalIdBundle key : subscriptions) {
        _clientSubscriptions.remove(client, key);
        if (!_clientSubscriptions.inverse().containsKey(key)) {
          redundantSpecs.add(key);
        }
      }
      return redundantSpecs;
    }

    /**
     * Remove the client and consequently remove all subscriptions from
     * the client.
     *
     * @param client the client to remove
     * @return the subset of the client's subscriptions which are no
     * longer mapped to any client
     */
    private Set<ExternalIdBundle> removeClient(LDListener client) {
      Set<ExternalIdBundle> redundantSpecs = new HashSet<>();
      Set<ExternalIdBundle> removed = _clientSubscriptions.removeAll(client);
      for (ExternalIdBundle key : removed) {
        if (!_clientSubscriptions.inverse().containsKey(key)) {
          redundantSpecs.add(key);
        }
      }
      return redundantSpecs;
    }
  }
}
