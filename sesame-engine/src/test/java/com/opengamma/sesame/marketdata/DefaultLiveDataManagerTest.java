package com.opengamma.sesame.marketdata;

import static com.opengamma.livedata.msg.LiveDataSubscriptionResult.NOT_PRESENT;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.FailureStatus.PENDING_DATA;
import static com.opengamma.util.result.FailureStatus.PERMISSION_DENIED;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.LiveDataListener;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.LiveDataValueUpdateBean;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.livedata.msg.LiveDataSubscriptionResponse;
import com.opengamma.livedata.msg.LiveDataSubscriptionResult;
import com.opengamma.livedata.permission.PermissionUtils;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.auth.PermissiveSecurityManager;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

/**
 * Tests for the DefaultLiveDataManager.
 */
@Test(groups = TestGroup.UNIT)
public class DefaultLiveDataManagerTest {

  private DefaultLiveDataManager _manager;
  private FudgeContext _fudgeContext;
  private LiveDataClient _mockLiveDataClient;

  enum WaitState {INITIAL, WAITING, DONE}

  @BeforeMethod
  public void setUp() {
    // Set authorization to allow everything for most tests
    ThreadContext.bind(new PermissiveSecurityManager());

    _fudgeContext = new FudgeContext();
    _mockLiveDataClient = mock(LiveDataClient.class);

    // Don't delay unsubscribe events as it hampers testing
    _manager = new DefaultLiveDataManager(_mockLiveDataClient, 0);
  }

  @AfterMethod
  public void tearDown() {
    // This is necessary as once the subject has been set, the security manager is
    // ignored. But we want to be able to change the security manager for tests.
    ThreadContext.unbindSubject();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testUnknownClientGivesError() {
    LiveDataManager manager = _manager;
    manager.snapshot(mock(LDListener.class));
  }

  @Test
  public void testUnfulfilledSubsReturnPending() {

    LDListener client = mock(LDListener.class);

    String[] tickers = {"T1", "T2", "T3"};
    _manager.subscribe(client, createIdBundles(tickers));
    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(3));

    for (String ticker : tickers) {
      checkSnapshotEntry(snapshot, ticker, PENDING_DATA);
    }
  }

  private void checkSnapshotEntry(Map<ExternalIdBundle, Result<FudgeMsg>> snapshot,
                                  String ticker, ResultStatus status) {
    ExternalIdBundle bundle = createBundle(ticker);
    assertThat(snapshot.get(bundle), is(notNullValue()));
    assertThat(snapshot.get(bundle).getStatus(), is(status));
  }

  @Test
  public void testFailedSubsReturnMissing() {

    LDListener client = mock(LDListener.class);

    String[] tickers = {"T1", "T2", "T3"};
    _manager.subscribe(client, createIdBundles(tickers));

    // Now simulate market data server returning its response
    // indicating data is not available
    Set<LiveDataSubscriptionResponse> responses = new HashSet<>();
    for (String ticker : tickers) {
      LiveDataSubscriptionResponse response = buildFailureResponse(ticker);
      responses.add(response);
    }

    _manager.subscriptionResultsReceived(responses);

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(3));

    for (String ticker : tickers) {
      checkSnapshotEntry(snapshot, ticker, MISSING_DATA);
    }
  }

  private LiveDataSubscriptionResponse buildFailureResponse(String ticker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    return new LiveDataSubscriptionResponse(specification, NOT_PRESENT, "Data is not available", null, null, null);
  }

  private LiveDataSubscriptionResponse buildFailureResponse(String ticker, String mappedTicker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    LiveDataSpecification fqSpecification = createLiveDataSpec(mappedTicker);
    return new LiveDataSubscriptionResponse(specification, NOT_PRESENT, "Data is not available", fqSpecification, null, null);
  }

  private LiveDataSubscriptionResponse buildSuccessResponse(String ticker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    return new LiveDataSubscriptionResponse(specification, LiveDataSubscriptionResult.SUCCESS, null, null, null, null);
  }

  private LiveDataSubscriptionResponse buildSuccessResponse(String ticker, String mappedTicker) {
    LiveDataSpecification specification = createLiveDataSpec(ticker);
    LiveDataSpecification fqSpecification = createLiveDataSpec(mappedTicker);
    return new LiveDataSubscriptionResponse(specification, LiveDataSubscriptionResult.SUCCESS, null, fqSpecification, null, null);
  }

  private LiveDataSpecification createLiveDataSpec(String ticker) {
    return new LiveDataSpecification("OpenGamma", createBundle(ticker));
  }

  @Test
  public void testMixedResults() {

    LDListener client = mock(LDListener.class);

    String[] tickers = {
        "T1", // Available but not yet received
        "T2", // Missing
        "T3", // Pending
        "T4"  // Available and received
    };
    _manager.subscribe(client, createIdBundles(tickers));

    Set<LiveDataSubscriptionResponse> responses = new HashSet<>();
    responses.add(buildSuccessResponse("T1"));
    responses.add(buildFailureResponse("T2"));
    responses.add(buildSuccessResponse("T4"));

    _manager.subscriptionResultsReceived(responses);

    // Now send the value update for T4
    MutableFudgeMsg msg = _fudgeContext.newMessage();
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T4"), msg));

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(4));

    // Data is available but not sent so still pending
    checkSnapshotEntry(snapshot, "T1", PENDING_DATA);
    checkSnapshotEntry(snapshot, "T2", MISSING_DATA);
    checkSnapshotEntry(snapshot, "T3", PENDING_DATA);
    checkSnapshotEntry(snapshot, "T4", SUCCESS);
  }

  @Test
  public void testMultipleSubscribes() {

    LDListener client = mock(LDListener.class);

    _manager.subscribe(client, createIdBundles("T1", "T2", "T3"));

    _manager.subscriptionResultsReceived(ImmutableSet.of(buildSuccessResponse("T1"), buildFailureResponse("T2")));

    // Now send the value update for T1
    MutableFudgeMsg msg = _fudgeContext.newMessage();
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msg));

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(3));

    // Data is available but not sent so still pending
    checkSnapshotEntry(snapshot, "T1", SUCCESS);
    checkSnapshotEntry(snapshot, "T2", MISSING_DATA);
    checkSnapshotEntry(snapshot, "T3", PENDING_DATA);

    // Now subscribe to more
    _manager.subscribe(client, createIdBundles("T4", "T5"));

    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T3"), buildSuccessResponse("T4"), buildFailureResponse("T5")));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T3"), msg));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T4"), msg));

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot2 = _manager.snapshot(client);

    assertThat(snapshot2.size(), is(5));

    // Data is available but not sent so still pending
    checkSnapshotEntry(snapshot2, "T1", SUCCESS);
    checkSnapshotEntry(snapshot2, "T2", MISSING_DATA);
    checkSnapshotEntry(snapshot2, "T3", SUCCESS);
    checkSnapshotEntry(snapshot2, "T4", SUCCESS);
    checkSnapshotEntry(snapshot2, "T5", MISSING_DATA);
  }

  /**
   * Test that if client opts to wait for results they are held until
   * all results are available for them. For failure this means
   * don't need to wait for data to be received.
   */
  @Test
  public void testAwaitingResultsAllFailures() throws InterruptedException {

    WaitingClient client = new WaitingClient(_manager);

    String[] tickers = {"T1", "T2", "T3"};
    _manager.subscribe(client, createIdBundles(tickers));

    // Start in another thread and wait on data
    new Thread(client).start();

    waitForStateChange(client, WaitState.INITIAL, WaitState.WAITING);

    _manager.subscriptionResultsReceived
        (ImmutableSet.of(buildFailureResponse("T1"), buildFailureResponse("T2")));

    // No response for T3 so still waiting
    assertThat(client.getWaitState(), is(WaitState.WAITING));

    _manager.subscriptionResultsReceived(ImmutableSet.of(buildFailureResponse("T3")));
    waitForStateChange(client, WaitState.WAITING, WaitState.DONE);
  }

  /**
   * Test that if client opts to wait for results they are held until
   * all results are available for them. For successes we have to wait
   * for data to arrive.
   */
  @Test
  public void testAwaitingResults() throws InterruptedException {

    WaitingClient client = new WaitingClient(_manager);

    String[] tickers = {"T1", "T2", "T3"};
    _manager.subscribe(client, createIdBundles(tickers));

    // Start in another thread and wait on data
    new Thread(client).start();

    waitForStateChange(client, WaitState.INITIAL, WaitState.WAITING);

    _manager.subscriptionResultsReceived
        (ImmutableSet.of(buildFailureResponse("T1"), buildSuccessResponse("T2"), buildSuccessResponse("T3")));

    // No data for T2 or T3 so still waiting
    assertThat(client.getWaitState(), is(WaitState.WAITING));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T2"), _fudgeContext.newMessage()));

    // No data for T3 so still waiting
    assertThat(client.getWaitState(), is(WaitState.WAITING));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T3"), _fudgeContext.newMessage()));
    waitForStateChange(client, WaitState.WAITING, WaitState.DONE);
  }

  private void waitForStateChange(WaitingClient client, WaitState state1, WaitState state2) throws InterruptedException {

    ArgumentChecker.isTrue(state1.compareTo(state2) < 0, "state: {} comes after state: {}", state1, state2);
    while (client.getWaitState().compareTo(state1) <= 0) {
      Thread.sleep(1);
    }
    assertThat(client.getWaitState(), is(state2));
  }

  /**
   * Set security manager such that every data request is turned down.
   */
  @Test
  public void testUnauthorizedUserGetsPermissionDenied() {
    // By default this will deny any authorization request
    ThreadContext.bind(new DefaultSecurityManager(new SimpleAccountRealm()));

    LDListener client = mock(LDListener.class);

    String[] tickers = {"T1", "T2", "T3"};
    _manager.subscribe(client, createIdBundles(tickers));

    Set<LiveDataSubscriptionResponse> responses = new HashSet<>();
    responses.add(buildSuccessResponse("T1"));
    responses.add(buildSuccessResponse("T2"));
    responses.add(buildSuccessResponse("T3"));

    _manager.subscriptionResultsReceived(responses);

    // Now send the value updates for all
    MutableFudgeMsg msg = buildPermissionedMsg("somePerm");
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), msg));
    _manager.valueUpdate(new LiveDataValueUpdateBean(2, createLiveDataSpec("T2"), msg));
    _manager.valueUpdate(new LiveDataValueUpdateBean(3, createLiveDataSpec("T3"), msg));

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(3));

    // Data is available but not sent so still pending
    checkSnapshotEntry(snapshot, "T1", PERMISSION_DENIED);
    checkSnapshotEntry(snapshot, "T2", PERMISSION_DENIED);
    checkSnapshotEntry(snapshot, "T3", PERMISSION_DENIED);
  }

  // test permissions stops only some tickers and allows others through
  @Test
  public void testUserMayBePartiallyDeniedTickers() {

    ThreadContext.bind(createSecurityManagerAllowing("T3-Perm"));

    LDListener client = mock(LDListener.class);

    String[] tickers = {"T1", "T2", "T3"};
    _manager.subscribe(client, createIdBundles(tickers));

    Set<LiveDataSubscriptionResponse> responses = new HashSet<>();
    responses.add(buildSuccessResponse("T1"));
    responses.add(buildSuccessResponse("T2"));
    responses.add(buildSuccessResponse("T3"));

    _manager.subscriptionResultsReceived(responses);

    // Now send the value updates for all, with individual permissioning
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), buildPermissionedMsg("T1-Perm")));
    _manager.valueUpdate(new LiveDataValueUpdateBean(2, createLiveDataSpec("T2"), buildPermissionedMsg("T2-Perm")));
    _manager.valueUpdate(new LiveDataValueUpdateBean(3, createLiveDataSpec("T3"), buildPermissionedMsg("T3-Perm")));

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(3));

    checkSnapshotEntry(snapshot, "T1", PERMISSION_DENIED);
    checkSnapshotEntry(snapshot, "T2", PERMISSION_DENIED);
    checkSnapshotEntry(snapshot, "T3", SUCCESS);
  }

  // test permissions only affects some clients - 1 client allowed all, 1 allowed some, 1 allowed none
  @Test
  public void testUsersCanHaveDifferentResultsForSameTickers() throws InterruptedException {

    PermissionedClient client1 = new PermissionedClient(_manager, "T1-Perm", "T2-Perm", "T3-Perm");
    PermissionedClient client2 = new PermissionedClient(_manager, "T1-Perm", "T2-Perm");
    PermissionedClient client3 = new PermissionedClient(_manager);

    Set<ExternalIdBundle> subscriptionRequest = createIdBundles("T1", "T2", "T3");
    _manager.subscribe(client1, subscriptionRequest);
    _manager.subscribe(client2, subscriptionRequest);
    _manager.subscribe(client3, subscriptionRequest);

    Set<LiveDataSubscriptionResponse> responses = ImmutableSet.of(
        buildSuccessResponse("T1"),
        buildSuccessResponse("T2"),
        buildSuccessResponse("T3"));

    _manager.subscriptionResultsReceived(responses);

    // Now send the value updates for all, with individual permissioning
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T1"), buildPermissionedMsg("T1-Perm")));
    _manager.valueUpdate(new LiveDataValueUpdateBean(2, createLiveDataSpec("T2"), buildPermissionedMsg("T2-Perm")));
    _manager.valueUpdate(new LiveDataValueUpdateBean(3, createLiveDataSpec("T3"), buildPermissionedMsg("T3-Perm")));

    for (PermissionedClient client : new PermissionedClient[]{client1, client2, client3}) {
      new Thread(client).start();
    }

    // Delay to allow the clients to pick up their data
    Thread.sleep(50);

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot1 = client1.getSnapshot();
    assertThat(snapshot1.size(), is(3));
    checkSnapshotEntry(snapshot1, "T1", SUCCESS);
    checkSnapshotEntry(snapshot1, "T2", SUCCESS);
    checkSnapshotEntry(snapshot1, "T3", SUCCESS);

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot2 = client2.getSnapshot();
    assertThat(snapshot2.size(), is(3));
    checkSnapshotEntry(snapshot2, "T1", SUCCESS);
    checkSnapshotEntry(snapshot2, "T2", SUCCESS);
    checkSnapshotEntry(snapshot2, "T3", PERMISSION_DENIED);

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot3 = client3.getSnapshot();
    assertThat(snapshot3.size(), is(3));
    checkSnapshotEntry(snapshot3, "T1", PERMISSION_DENIED);
    checkSnapshotEntry(snapshot3, "T2", PERMISSION_DENIED);
    checkSnapshotEntry(snapshot3, "T3", PERMISSION_DENIED);
  }


  // test multiple clients get their results
  @Test
  public void testClientsGetOnlyTheirResults() throws InterruptedException {
    WaitingClient client1 = new WaitingClient(_manager);
    WaitingClient client2 = new WaitingClient(_manager);
    WaitingClient client3 = new WaitingClient(_manager);

    _manager.subscribe(client1, createIdBundles("T1", "T2", "T3"));
    _manager.subscribe(client2, createIdBundles("T3", "T4", "T5"));
    _manager.subscribe(client3, createIdBundles("T1", "T4", "T6"));

    _manager.subscriptionResultsReceived
        (ImmutableSet.of(buildFailureResponse("T1"), buildSuccessResponse("T2"), buildSuccessResponse("T3"),
                         buildFailureResponse("T4"), buildSuccessResponse("T5"), buildSuccessResponse("T6")));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T2"), _fudgeContext.newMessage()));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T3"), _fudgeContext.newMessage()));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T5"), _fudgeContext.newMessage()));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T6"), _fudgeContext.newMessage()));

    // Start in another thread and wait on data
    new Thread(client1).start();
    new Thread(client2).start();
    new Thread(client3).start();

    waitForStateChange(client1, WaitState.WAITING, WaitState.DONE);
    waitForStateChange(client2, WaitState.WAITING, WaitState.DONE);
    waitForStateChange(client3, WaitState.WAITING, WaitState.DONE);

    assertThat(_manager.snapshot(client1).keySet(),
               containsInAnyOrder(createBundle("T1"), createBundle("T2"), createBundle("T3")));
    assertThat(_manager.snapshot(client2).keySet(),
               containsInAnyOrder(createBundle("T3"), createBundle("T4"), createBundle("T5")));
    assertThat(_manager.snapshot(client3).keySet(),
               containsInAnyOrder(createBundle("T1"), createBundle("T4"), createBundle("T6")));
  }

  // test multiple clients stop waiting as their results arrive

  @Test
  public void testClientsStopWaitingAsTheyGetTheirResults() throws InterruptedException {

    WaitingClient client1 = new WaitingClient(_manager);
    WaitingClient client2 = new WaitingClient(_manager);
    WaitingClient client3 = new WaitingClient(_manager);

    _manager.subscribe(client1, createIdBundles("T1", "T2", "T3"));
    _manager.subscribe(client2, createIdBundles("T3", "T4", "T5"));
    _manager.subscribe(client3, createIdBundles("T1", "T4", "T6"));

    _manager.subscriptionResultsReceived
        (ImmutableSet.of(buildFailureResponse("T1"), buildSuccessResponse("T2"), buildSuccessResponse("T3"),
                         buildFailureResponse("T4"), buildSuccessResponse("T5"), buildSuccessResponse("T6")));

    // Start in another thread and wait on data
    new Thread(client1).start();
    new Thread(client2).start();
    new Thread(client3).start();

    // Client 1 first
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T2"), _fudgeContext.newMessage()));
    assertThat(client1.getWaitState(), is(WaitState.WAITING));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T3"), _fudgeContext.newMessage()));
    waitForStateChange(client1, WaitState.WAITING, WaitState.DONE);

    // Now client 2
    assertThat(client2.getWaitState(), is(WaitState.WAITING));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T5"), _fudgeContext.newMessage()));
    waitForStateChange(client2, WaitState.WAITING, WaitState.DONE);

    // Now client 3
    assertThat(client3.getWaitState(), is(WaitState.WAITING));

    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T6"), _fudgeContext.newMessage()));
    waitForStateChange(client2, WaitState.WAITING, WaitState.DONE);

    assertThat(_manager.snapshot(client1).keySet(),
               containsInAnyOrder(createBundle("T1"), createBundle("T2"), createBundle("T3")));
    assertThat(_manager.snapshot(client2).keySet(),
               containsInAnyOrder(createBundle("T3"), createBundle("T4"), createBundle("T5")));
    assertThat(_manager.snapshot(client3).keySet(),
               containsInAnyOrder(createBundle("T1"), createBundle("T4"), createBundle("T6")));
  }

  @Test
  public void testMappingOfTickers() {

    LDListener client = mock(LDListener.class);

    _manager.subscribe(client, createIdBundles("T1", "T2", "T3"));

    Set<LiveDataSubscriptionResponse> responses = new HashSet<>();
    // Server responds with a different id for the ticker - all
    // messages it sends will be against the new id
    responses.add(buildSuccessResponse("T1", "M1"));
    responses.add(buildFailureResponse("T2", "M2"));
    responses.add(buildSuccessResponse("T3", "M3"));

    _manager.subscriptionResultsReceived(responses);

    // Now send the value updates against the mapped id
    MutableFudgeMsg msg = _fudgeContext.newMessage();
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("M1"), msg));
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("M2"), msg));

    // Use the original ticker - should end up still PENDING
    _manager.valueUpdate(new LiveDataValueUpdateBean(1, createLiveDataSpec("T3"), msg));

    Map<ExternalIdBundle, Result<FudgeMsg>> snapshot = _manager.snapshot(client);

    assertThat(snapshot.size(), is(3));

    checkSnapshotEntry(snapshot, "T1", SUCCESS);
    checkSnapshotEntry(snapshot, "T2", SUCCESS);
    checkSnapshotEntry(snapshot, "T3", PENDING_DATA);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSubscribeIsOnlySentOnce() throws InterruptedException {

    LDListener client1 = mock(LDListener.class);
    LDListener client2 = mock(LDListener.class);
    LDListener client3 = mock(LDListener.class);

    // Subscribe for 2 tickers
    _manager.subscribe(client1, createIdBundles("T1", "T2"));
    // Subscribe for 1 additional ticker
    _manager.subscribe(client2, createIdBundles("T1", "T3"));
    // No subscription required
    _manager.subscribe(client3, createIdBundles("T2", "T3"));

    // Sleep as the subscribe/unsubscribe is happening on another thread
    Thread.sleep(10);
    verify(_mockLiveDataClient, times(2))
        .subscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));
    verifyNoMoreInteractions(_mockLiveDataClient);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUnubscribeFromTickerDoesNothingIfTickerIsUsedByOthers() throws InterruptedException {

    LDListener client1 = mock(LDListener.class);
    LDListener client2 = mock(LDListener.class);

    _manager.subscribe(client1, createIdBundles("T1", "T2"));
    _manager.subscribe(client2, createIdBundles("T1", "T3"));

    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T1", "M1"), buildSuccessResponse("T2", "M2"), buildSuccessResponse("T3", "M3")));

    // Client 2 also has subscription
    _manager.unsubscribe(client1, createIdBundles("T1"));

    // Sleep as the subscribe/unsubscribe is happening on another thread
    Thread.sleep(10);

    verify(_mockLiveDataClient, never())
        .unsubscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));

    // Only client 1 subscribed, so should go through
    _manager.unsubscribe(client1, createIdBundles("T2"));
    Thread.sleep(10);

    verify(_mockLiveDataClient)
        .unsubscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));

    // Client 2 is only one with subs
    _manager.unsubscribe(client2, createIdBundles("T1", "T3"));
    Thread.sleep(10);

    // Times(2) as mockito keeps track of all calls - we had 1 previously so now we have 2
    verify(_mockLiveDataClient, times(2))
        .unsubscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCannotUnubscribeSomeoneElsesTicker() throws InterruptedException {

    LDListener client1 = mock(LDListener.class);
    LDListener client2 = mock(LDListener.class);

    _manager.subscribe(client1, createIdBundles("T1", "T2"));
    _manager.subscribe(client2, createIdBundles("T1", "T3"));

    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T1", "M1"), buildSuccessResponse("T2", "M2"), buildSuccessResponse("T3", "M3")));

    // Client 2 does not have subscription to T2
    _manager.unsubscribe(client2, createIdBundles("T2"));

    // Sleep as the subscribe/unsubscribe is happening on another thread
    Thread.sleep(10);

    verify(_mockLiveDataClient, never())
        .unsubscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));
  }

  // test client unregistering with other users wanting ticker

  @Test
  @SuppressWarnings("unchecked")
  public void testClientUnregisteringWithUnusedTickers() throws InterruptedException {

    LDListener client1 = mock(LDListener.class);
    LDListener client2 = mock(LDListener.class);

    _manager.subscribe(client1, createIdBundles("T1", "T2"));
    _manager.subscribe(client2, createIdBundles("T1", "T3"));

    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T1", "M1"), buildSuccessResponse("T2", "M2"), buildSuccessResponse("T3", "M3")));

    _manager.unregister(client1);

    // Sleep as the subscribe/unsubscribe is happening on another thread
    Thread.sleep(10);

    // Unsubscribe from the T2 ticker should happen
    verify(_mockLiveDataClient)
        .unsubscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testClientUnregisteringWithNoUnusedTickers() throws InterruptedException {

    LDListener client1 = mock(LDListener.class);
    LDListener client2 = mock(LDListener.class);

    // All client1's tickers are wanted by client2
    _manager.subscribe(client1, createIdBundles("T1", "T2"));
    _manager.subscribe(client2, createIdBundles("T1", "T2"));

    _manager.subscriptionResultsReceived(ImmutableSet.of(
        buildSuccessResponse("T1", "M1"), buildSuccessResponse("T2", "M2"), buildSuccessResponse("T3", "M3")));

    _manager.unregister(client1);

    // Sleep as the subscribe/unsubscribe is happening on another thread
    Thread.sleep(10);

    // Unsubscribe from the T2 ticker should happen
    verify(_mockLiveDataClient, never())
        .unsubscribe(any(UserPrincipal.class), any(Collection.class), any(LiveDataListener.class));
  }

  // TODO - additional tests:
  // test merging of fudge messages when receiving updates
  // test
  // test multiple clients all on separate threads and data ticking in

  private SecurityManager createSecurityManagerAllowing(final String... permissions) {
    final AuthorizingRealm realm = new AuthorizingRealm() {
      Set<String> perms = ImmutableSet.copyOf(permissions);

      @Override
      protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // this method should possibly be simpler, but this certainly works
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        for (String perm : perms) {
          info.addObjectPermission(createPermission(perm));
        }
        return info;
      }

      private Permission createPermission(final String perm) {
        return new Permission() {
          @Override
          public boolean implies(Permission p) {
            return perm.toLowerCase().equals(p.toString());
          }
        };
      }

      @Override
      protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // Not concerned with authentication for these tests
        return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
      }
    };
    return new PermissioningSecurityManager(realm);
  }

  private MutableFudgeMsg buildPermissionedMsg(String perm) {
    MutableFudgeMsg msg = _fudgeContext.newMessage();
    msg.add(PermissionUtils.LIVE_DATA_PERMISSION_FIELD, perm);
    return msg;
  }

  public class PermissioningSecurityManager extends DefaultWebSecurityManager {

    /**
     * Creates an instance.
     */
    public PermissioningSecurityManager(Realm realm) {
      setRealm(realm);
    }

    //-------------------------------------------------------------------------
    @Override
    protected SubjectContext copy(SubjectContext subjectContext) {
      // this is the only way to trick the superclass into believing subject is always authenticated
      UsernamePasswordToken token = new UsernamePasswordToken("permissive", "nopassword");
      SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(token.getPrincipal(),
                                                                   token.getCredentials(),
                                                                   "Permissive");
      subjectContext.setAuthenticated(true);
      subjectContext.setAuthenticationToken(token);
      subjectContext.setAuthenticationInfo(info);
      return subjectContext;
    }
  }

  private Set<ExternalIdBundle> createIdBundles(String... tickers) {
    Set<ExternalIdBundle> subs = new HashSet<>();
    for (String ticker : tickers) {
      subs.add(createBundle(ticker));
    }
    return subs;
  }

  private ExternalIdBundle createBundle(String ticker) {
    return createTicker(ticker).toBundle();
  }

  private ExternalId createTicker(String ticker) {
    return ExternalId.of("TICKER", ticker);
  }

  /**
   * Helper class that will run on another thread and immediately wait
   * for the manager to indicate that data is available. The state can
   * be monitored to check the behaviour is as expected.
   */
  private static class WaitingClient implements LDListener, Runnable {


    private final LiveDataManager _manager;

    private WaitState _waitState = WaitState.INITIAL;

    public WaitingClient(LiveDataManager manager) {
      _manager = manager;
    }

    @Override
    public void run() {
      _waitState = WaitState.WAITING;
      _manager.waitForAllData(this);
      _waitState = WaitState.DONE;
    }

    public WaitState getWaitState() {
      return _waitState;
    }

    @Override
    public void valueUpdated() {
    }
  }

  /**
   * Helper class that will run on another thread and has different
   * permissions to to indicate that data is available. The state can
   * be monitored to check the behaviour is as expected.
   */
  private class PermissionedClient implements LDListener, Runnable {

    private final LiveDataManager _manager;
    private final String[] _allowedPerms;
    private Map<ExternalIdBundle, Result<FudgeMsg>> _snapshot;

    public PermissionedClient(LiveDataManager manager, String... allowedPerms) {
      _manager = manager;
      _allowedPerms = allowedPerms;
    }

    @Override
    public void run() {
      // This will set the permissions for the thread local and will not
      // affect other clients
      ThreadContext.bind(createSecurityManagerAllowing(_allowedPerms));
      _manager.waitForAllData(this);
      _snapshot = _manager.snapshot(this);
    }

    public Map<ExternalIdBundle, Result<FudgeMsg>> getSnapshot() {
      return _snapshot;
    }

    @Override
    public void valueUpdated() {
    }
  }
}
