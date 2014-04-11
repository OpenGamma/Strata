package com.opengamma.sesame.component;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fudgemsg.MutableFudgeMsg;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.component.ComponentFactory;
import com.opengamma.component.ComponentLogger;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.EmbeddedJettyComponentFactory;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.LiveDataListener;
import com.opengamma.livedata.LiveDataSpecification;
import com.opengamma.livedata.LiveDataValueUpdate;
import com.opengamma.livedata.LiveDataValueUpdateBean;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.livedata.msg.LiveDataSubscriptionResponse;
import com.opengamma.livedata.msg.LiveDataSubscriptionResult;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.MarketdataResourcesLoader;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.engine.ResultItem;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.interestrate.InterestRateMockSources;
import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.GlobalCycleOptions;
import com.opengamma.sesame.server.IndividualCycleOptions;
import com.opengamma.sesame.server.RemoteFunctionServer;
import com.opengamma.sesame.server.streaming.RemoteStreamingFunctionServer;
import com.opengamma.sesame.server.streaming.StreamingClient;
import com.opengamma.sesame.server.streaming.StreamingClientResultListener;
import com.opengamma.sesame.server.streaming.StreamingFunctionServer;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.jms.JmsConnector;
import com.opengamma.util.jms.JmsConnectorFactoryBean;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

import net.sf.ehcache.CacheManager;

/**
 * Tests that remoting to the new engine works. Starts up an engine on a
 * separate thread in the setup method and then makes requests to it via
 * REST. This test should run fast as all component parts are local and is
 * therefore classified as a UNIT test. However, in many respects it is
 * closer to an INTEGRATION test and therefore may need to be reclassified.
 */
@Test(groups = TestGroup.UNIT)
public class RemotingTest {

  public static final String CLASSIFIER = "test";

  private ComponentRepository _componentRepository;
  
  private InterestRateMockSources _interestRateMockSources = new InterestRateMockSources();

  @Test
  public void testSingleExecution() {

    String curveBundleOutputName = "Curve Bundle";
    ViewConfig viewConfig = createCurveBundleConfig(curveBundleOutputName);

    // Send the config to the server, along with version
    // correction, MD requirements, valuation date and
    // cycle specifics (once/multiple/infinite)
    // Proxy options?

    FunctionServer functionServer = new RemoteFunctionServer(URI.create("http://localhost:8080/jax"));

    IndividualCycleOptions cycleOptions = IndividualCycleOptions.builder()
        .valuationTime(ZonedDateTime.now())
        .marketDataSpec(new FixedHistoricalMarketDataSpecification(LocalDate.now().minusDays(2)))
        .build();

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(viewConfig)
            //.withVersionCorrection(...)
            //.withSecurities(...)
            .cycleOptions(cycleOptions)
            .build();

    Results results = functionServer.executeSingleCycle(request);
    System.out.println(results);
    assertThat(results, is(not(nullValue())));

    checkCurveBundleResult(curveBundleOutputName, results);
  }

  private void checkCurveBundleResult(String curveBundleOutputName, Results results) {

    ResultItem resultItem = results.get(curveBundleOutputName);
    assertThat(resultItem, is(not(nullValue())));

    Result<?> result = resultItem.getResult();
    assertThat(result.isSuccess(), is(true));

    @SuppressWarnings("unchecked") Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> pair =
        (Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>) result.getValue();
    assertThat(pair.getFirst(), is(not(nullValue())));
    assertThat(pair.getSecond(), is(not(nullValue())));
  }

  @Test
  public void testMultipleExecution() throws InterruptedException {

    String curveBundleOutputName = "Curve Bundle";
    ViewConfig viewConfig = createCurveBundleConfig(curveBundleOutputName);

    // Send the config to the server, along with version
    // correction, MD requirements, valuation date and
    // cycle specifics (once/multiple/infinite)
    // Proxy options?

    FunctionServer functionServer = new RemoteFunctionServer(URI.create("http://localhost:8080/jax"));

    GlobalCycleOptions cycleOptions = GlobalCycleOptions.builder()
        .valuationTime(ZonedDateTime.now())
        .marketDataSpec(new FixedHistoricalMarketDataSpecification(LocalDate.now().minusDays(2)))
        .numCycles(2)
        .build();

    FunctionServerRequest<GlobalCycleOptions> request =
        FunctionServerRequest.<GlobalCycleOptions>builder()
            .viewConfig(viewConfig)
                //.withVersionCorrection(...)
                //.withSecurities(...)
            .cycleOptions(cycleOptions)
            .build();

    List<Results> results = functionServer.executeMultipleCycles(request);
    System.out.println(results);
    assertThat(results, is(not(nullValue())));
    assertThat(results.size(), is(2));

    checkCurveBundleResult(curveBundleOutputName, results.get(0));
    checkCurveBundleResult(curveBundleOutputName, results.get(1));
  }

  @Test
  public void testStreamingExecution() throws InterruptedException {

    final String curveBundleOutputName = "Curve Bundle";
    ViewConfig viewConfig = createCurveBundleConfig(curveBundleOutputName);

    // Send the config to the server, along with version
    // correction, MD requirements, valuation date and
    // cycle specifics (once/multiple/infinite)
    // Proxy options?

    StreamingFunctionServer functionServer = new RemoteStreamingFunctionServer(
        URI.create("http://localhost:8080/jax"),
        createJmsConnector(),
        Executors.newSingleThreadScheduledExecutor());

    GlobalCycleOptions cycleOptions = GlobalCycleOptions.builder()
        .valuationTime(ZonedDateTime.now())
        .marketDataSpec(new FixedHistoricalMarketDataSpecification(LocalDate.now().minusDays(2)))
        .numCycles(2)
        .build();

    FunctionServerRequest<GlobalCycleOptions> request =
        FunctionServerRequest.<GlobalCycleOptions>builder()
            .viewConfig(viewConfig)
                //.withVersionCorrection(...)
                //.withSecurities(...)
            .cycleOptions(cycleOptions)
            .build();

    StreamingClient streamingClient = functionServer.createStreamingClient(request);
    assertThat(streamingClient.getUniqueId(), is(not(nullValue())));
    assertThat(streamingClient.isRunning(), is(true));
    assertThat(streamingClient.isStopped(), is(false));

    final CountDownLatch resultsLatch = new CountDownLatch(2);
    final CountDownLatch completedLatch = new CountDownLatch(1);

    streamingClient.registerListener(new StreamingClientResultListener() {

      @Override
      public void resultsReceived(Results results) {
        checkCurveBundleResult(curveBundleOutputName, results);
        resultsLatch.countDown();
      }

      @Override
      public void processCompleted() {
        completedLatch.countDown();
      }

      @Override
      public void serverConnectionFailed(Exception e) {
      }
    });
    assertThat(resultsLatch.await(10, TimeUnit.SECONDS), is(true));
    assertThat(completedLatch.await(10, TimeUnit.SECONDS), is(true));

    assertThat(streamingClient.isRunning(), is(false));
    assertThat(streamingClient.isStopped(), is(true));
  }

  @Test
  public void testStreamingExecutionCanBeStopped() throws InterruptedException {

    final String curveBundleOutputName = "Curve Bundle";
    ViewConfig viewConfig = createCurveBundleConfig(curveBundleOutputName);

    // Send the config to the server, along with version
    // correction, MD requirements, valuation date and
    // cycle specifics (once/multiple/infinite)
    // Proxy options?

    StreamingFunctionServer functionServer = new RemoteStreamingFunctionServer(
        URI.create("http://localhost:8080/jax"),
        createJmsConnector(),
        Executors.newSingleThreadScheduledExecutor());

    GlobalCycleOptions cycleOptions = GlobalCycleOptions.builder()
        .valuationTime(ZonedDateTime.now())
        .marketDataSpec(new FixedHistoricalMarketDataSpecification(LocalDate.now().minusDays(2)))
        .numCycles(0)
        .build();

    FunctionServerRequest<GlobalCycleOptions> request =
        FunctionServerRequest.<GlobalCycleOptions>builder()
            .viewConfig(viewConfig)
                //.withVersionCorrection(...)
                //.withSecurities(...)
            .cycleOptions(cycleOptions)
            .build();

    StreamingClient streamingClient = functionServer.createStreamingClient(request);

    // Get some results first
    final CountDownLatch resultsLatch = new CountDownLatch(10);

    streamingClient.registerListener(new StreamingClientResultListener() {
      @Override
      public void resultsReceived(Results results) {
        checkCurveBundleResult(curveBundleOutputName, results);
        resultsLatch.countDown();
      }

      @Override
      public void processCompleted() { }

      @Override
      public void serverConnectionFailed(Exception e) { }
    });

    assertThat(resultsLatch.await(10, TimeUnit.SECONDS), is(true));

    streamingClient.stop();

    Thread.sleep(1000);

    assertThat(streamingClient.isRunning(), is(false));
    assertThat(streamingClient.isStopped(), is(true));
  }

  @Test
  public void testLiveExecution() {

    String curveBundleOutputName = "Curve Bundle";
    ViewConfig viewConfig = createCurveBundleConfig(curveBundleOutputName);

    // Send the config to the server, along with version
    // correction, MD requirements, valuation date and
    // cycle specifics (once/multiple/infinite)
    // Proxy options?

    FunctionServer functionServer = new RemoteFunctionServer(URI.create("http://localhost:8080/jax"));

    IndividualCycleOptions cycleOptions = IndividualCycleOptions.builder()
        .valuationTime(ZonedDateTime.now())
        .marketDataSpec(LiveMarketDataSpecification.LIVE_SPEC)
        .build();

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(viewConfig)
            //.withVersionCorrection(...)
            //.withSecurities(...)
            .cycleOptions(cycleOptions)
            .build();

    Results results = functionServer.executeSingleCycle(request);
    System.out.println(results);
    assertThat(results, is(not(nullValue())));

    checkCurveBundleResult(curveBundleOutputName, results);
  }

  private ViewConfig createCurveBundleConfig(String curveBundleOutputName) {

    CurveConstructionConfiguration curveConstructionConfiguration =
        ConfigLink.of("USD_ON-OIS_LIBOR3M-FRAIRS_1U", CurveConstructionConfiguration.class).resolve();

    return configureView("Curve Bundle only",
                  nonPortfolioOutput(curveBundleOutputName,
                                     output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                                            config(
                                                arguments(
                                                    function(
                                                        RootFinderConfiguration.class,
                                                        argument("rootFinderAbsoluteTolerance", 1e-9),
                                                        argument("rootFinderRelativeTolerance", 1e-9),
                                                        argument("rootFinderMaxIterations", 1000)),
                                                    function(
                                                        DefaultHistoricalTimeSeriesFn.class,
                                                        argument("resolutionKey", "DEFAULT_TSS"),
                                                        argument("htsRetrievalPeriod",
                                                                 RetrievalPeriod.of((Period.ofYears(1))))),
                                                    function(
                                                        DefaultDiscountingMulticurveBundleFn.class,
                                                        argument("impliedCurveNames", StringSet.of()),
                                                        argument("curveConfig",
                                                                 curveConstructionConfiguration)))))));
  }

  // test execution with streaming results

  @BeforeClass
  public void setUp() throws Exception {

    _componentRepository = new ComponentRepository(new ComponentLogger.Console(1));

    Map<Class<?>, Object> componentMap = _interestRateMockSources.generateBaseComponents();
    LinkedHashMap<String, String> properties = addComponentsToRepo(componentMap);

    //  initialise engine
    ViewFactoryComponentFactory engineComponentFactory = new ViewFactoryComponentFactory();
    engineComponentFactory.setClassifier(CLASSIFIER);
    engineComponentFactory.setCacheManager(CacheManager.create());
    register(engineComponentFactory, _componentRepository, properties);

    registerFunctionServerComponentFactory();

    // todo - keeping the below as we should port temple tests to be like this one
/*


    // initialise pricer
    NewEngineFXForwardPricingManagerComponentFactory pricingManagerComponentFactory =
        new NewEngineFXForwardPricingManagerComponentFactory();
    pricingManagerComponentFactory.setEngine(_componentRepository.getInstance(Engine.class, "main"));
    pricingManagerComponentFactory.setAvailableImplementations(_componentRepository.getInstance(AvailableImplementations.class,
                                                                                                "main"));
    pricingManagerComponentFactory.setAvailableOutputs(_componentRepository.getInstance(AvailableOutputs.class, "main"));
    register(pricingManagerComponentFactory, _componentRepository);*/

    // initialize server


    // initialise Jetty server
    EmbeddedJettyComponentFactory jettyComponentFactory = new EmbeddedJettyComponentFactory();

    // TODO - can we supply the config required directly rather than a file?
    Resource resource = new ClassPathResource("web-engine");
    jettyComponentFactory.setResourceBase(resource);
    register(jettyComponentFactory, _componentRepository);
    _componentRepository.start();
  }

  private void registerFunctionServerComponentFactory() throws Exception {

    FunctionServerComponentFactory serverComponentFactory = new FunctionServerComponentFactory();
    serverComponentFactory.setClassifier(CLASSIFIER);
    serverComponentFactory.setViewFactory(_componentRepository.getInstance(ViewFactory.class, CLASSIFIER));
    serverComponentFactory.setMarketDataFactory(new InterestRateMockSources().createMarketDataFactory());
    serverComponentFactory.setLiveDataClient(createMockLiveDataClient());
    serverComponentFactory.setJmsConnector(createJmsConnector());

    register(serverComponentFactory, _componentRepository);
  }

  private LiveDataClient createMockLiveDataClient() throws IOException {

    return new LiveDataClient() {

      final Map<ExternalIdBundle, Double> marketData = MarketdataResourcesLoader.getData(
          "/usdMarketQuotes-20140122.properties", "Ticker");
      long counter = 0;

      @Override
      public void subscribe(UserPrincipal user,
                            LiveDataSpecification requestedSpecification,
                            LiveDataListener listener) { }

      @Override
      public void subscribe(UserPrincipal user,
                            Collection<LiveDataSpecification> requestedSpecifications,
                            LiveDataListener listener) {

        List<LiveDataSubscriptionResponse> subResponses = new ArrayList<>();
        List<LiveDataValueUpdate> dataValues = new ArrayList<>();

        for (LiveDataSpecification specification : requestedSpecifications) {
          if (marketData.containsKey(specification.getIdentifiers())) {
            subResponses.add(new LiveDataSubscriptionResponse(specification, LiveDataSubscriptionResult.SUCCESS, null, specification, null, null));
            MutableFudgeMsg msg = OpenGammaFudgeContext.getInstance().newMessage();
            msg.add(MarketDataRequirementNames.MARKET_VALUE, marketData.get(specification.getIdentifiers()));
            dataValues.add(new LiveDataValueUpdateBean(counter++, specification, msg));
          } else {
            subResponses.add(new LiveDataSubscriptionResponse(specification, LiveDataSubscriptionResult.NOT_PRESENT));
          }
        }
        listener.subscriptionResultsReceived(subResponses);

        for (LiveDataValueUpdate value : dataValues) {
          listener.valueUpdate(value);
        }
      }

      @Override
      public void unsubscribe(UserPrincipal user,
                              LiveDataSpecification fullyQualifiedSpecification,
                              LiveDataListener listener) { }

      @Override
      public void unsubscribe(UserPrincipal user,
                              Collection<LiveDataSpecification> fullyQualifiedSpecifications,
                              LiveDataListener listener) { }

      @Override
      public LiveDataSubscriptionResponse snapshot(UserPrincipal user,
                                                   LiveDataSpecification requestedSpecification,
                                                   long timeout) {
        return null;
      }

      @Override
      public Collection<LiveDataSubscriptionResponse> snapshot(UserPrincipal user,
                                                               Collection<LiveDataSpecification> requestedSpecifications,
                                                               long timeout) {
        return null;
      }

      @Override
      public String getDefaultNormalizationRuleSetId() {
        return null;
      }

      @Override
      public void close() {

      }

      @Override
      public boolean isEntitled(UserPrincipal user, LiveDataSpecification requestedSpecification) {
        return false;
      }

      @Override
      public Map<LiveDataSpecification, Boolean> isEntitled(UserPrincipal user,
                                                            Collection<LiveDataSpecification> requestedSpecifications) {
        return null;
      }
    };
  }

  private JmsConnector createJmsConnector() {
    final JmsConnectorFactoryBean factory = new JmsConnectorFactoryBean();
    factory.setName(getClass().getSimpleName());
    factory.setClientBrokerUri(URI.create("vm://remotingTestBroker"));
    factory.setConnectionFactory(new ActiveMQConnectionFactory(factory.getClientBrokerUri()));
    return factory.getObjectCreating();
  }

  @SuppressWarnings("unchecked")
  private LinkedHashMap<String, String> addComponentsToRepo(Map<Class<?>, Object> componentMap) {

    LinkedHashMap<String, String> props = new LinkedHashMap<>();

    for (Map.Entry<Class<?>, Object> entry : componentMap.entrySet()) {
      final Class<?> clss = entry.getKey();
      _componentRepository.registerComponent((Class<Object>) clss, CLASSIFIER, entry.getValue());
      props.put(clss.getSimpleName(), clss.getSimpleName() + "::" + CLASSIFIER);
    }
    return props;
  }

  @AfterClass
  public void tearDown() {
    System.out.println("Shutting down components");
    _componentRepository.stop();
  }

  private void register(ComponentFactory componentFactory,
                        ComponentRepository repo) throws Exception {
    register(componentFactory, repo, new LinkedHashMap<String, String>());
  }

  private void register(ComponentFactory componentFactory,
                        ComponentRepository repo, LinkedHashMap<String, String> configuration) throws Exception {
    componentFactory.init(repo, configuration);
  }

}
