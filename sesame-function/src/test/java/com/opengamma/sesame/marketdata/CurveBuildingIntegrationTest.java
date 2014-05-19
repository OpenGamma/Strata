/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import java.net.URI;
import java.util.EnumSet;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fudgemsg.FudgeMsg;
import org.springframework.jms.core.JmsTemplate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.component.ComponentServer;
import com.opengamma.component.rest.RemoteComponentServer;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.VersionCorrection;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.client.JmsLiveDataClient;
import com.opengamma.provider.livedata.LiveDataMetaData;
import com.opengamma.provider.livedata.LiveDataMetaDataProvider;
import com.opengamma.provider.livedata.LiveDataServerTypes;
import com.opengamma.provider.livedata.impl.RemoteLiveDataMetaDataProvider;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DirectExecutorService;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.engine.CachingManager;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.engine.FunctionService;
import com.opengamma.sesame.engine.ResultItem;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.DefaultCachingManager;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.scenarios.curvedata.FunctionTestUtils;
import com.opengamma.transport.ByteArrayFudgeRequestSender;
import com.opengamma.transport.jms.JmsByteArrayMessageSender;
import com.opengamma.transport.jms.JmsByteArrayRequestSender;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.jms.JmsConnector;
import com.opengamma.util.jms.JmsConnectorFactoryBean;
import com.opengamma.util.rest.FudgeRestClient;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.INTEGRATION, enabled = false)
public class CurveBuildingIntegrationTest {

  private static RemoteProviderTestUtils s_testUtils;

  @BeforeClass
  public void setup() {
    s_testUtils = new RemoteProviderTestUtils();
  }

  // Create a view with known curve and try to get market data
  // from the market data server
  //@Test(groups = TestGroup.INTEGRATION)
  public void testCurveHasData() throws InterruptedException {
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    ConfigLink<CurrencyMatrix> currencyMatrixLink = ConfigLink.resolvable("BloombergLiveData", CurrencyMatrix.class);

    FunctionModelConfig defaultConfig =
        config(
            arguments(
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-9),
                         argument("rootFinderRelativeTolerance", 1e-9),
                         argument("rootFinderMaxIterations", 1000)),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of())),
                function(DefaultMarketDataFn.class,
                         argument("currencyMatrix", currencyMatrixLink)),
                function(DefaultHistoricalMarketDataFn.class,
                         argument("currencyMatrix", currencyMatrixLink),
                         argument("dataSource", "BLOOMBERG"))));

    ViewConfig viewConfig =
        configureView("Curve Bundle only",
                      nonPortfolioOutput("Curve Bundle",
                               output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                                      config(
                                          arguments(
                                              function(DefaultDiscountingMulticurveBundleFn.class,
                                                       argument("curveConfig", ConfigLink.resolved(
                                                           CurveConstructionConfiguration.class))))))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(DiscountingMulticurveBundleFn.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DefaultCurrencyPairsFn.class,
                                      ConfigDBInstrumentExposuresProvider.class,
                                      DefaultCurveSpecificationMarketDataFn.class,
                                      DefaultFXMatrixFn.class,
                                      DefaultCurveDefinitionFn.class,
                                      DefaultDiscountingMulticurveBundleFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      ConfigDBCurveConstructionConfigurationSource.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      ConfigDbMarketExposureSelectorFn.class,
                                      DefaultMarketDataFn.class,
                                      DefaultHistoricalMarketDataFn.class);

    ComponentMap componentMap = ComponentMap.loadComponents("http://devsvr-lx-2:8080");

    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider();
    ServiceContext serviceContext =
        ServiceContext.of(componentMap.getComponents()).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    CachingManager cachingManager = new DefaultCachingManager(componentMap, FunctionTestUtils.createCache());
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              defaultConfig,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    View view = viewFactory.createView(viewConfig);

    LiveDataManager liveDataManager = new DefaultLiveDataManager(buildLiveDataClient());
    // TODO provide appropriate mock values
    LDClient liveDataClient = new LDClient(liveDataManager);
    StrategyAwareMarketDataSource liveDataSource = new ResettableLiveMarketDataSource(MarketData.live(), liveDataClient);
    CycleArguments cycleArguments = new CycleArguments(valuationTime, VersionCorrection.LATEST, liveDataSource);
    Results initialResults = view.run(cycleArguments);
    System.out.println(initialResults);

    // First time through we're waiting for market data so expect failure
    ResultItem resultItem = initialResults.get("Curve Bundle");
    Result<?> failureResult = resultItem.getResult();
    assertThat(failureResult.isSuccess(), is(false));
    assertThat(failureResult.getStatus(), is((ResultStatus) FailureStatus.PENDING_DATA));

    // Now try again, resetting the market data first (which should pick up bloomberg data)
    System.out.println("Waiting for market data to catch up");
    liveDataSource = liveDataSource.createPrimedSource();
    cycleArguments = new CycleArguments(valuationTime, VersionCorrection.LATEST, liveDataSource);

    Results results = view.run(cycleArguments);
    System.out.println(results);

    // Second time we should have received the market data
    ResultItem item = results.get("Curve Bundle");
    Result<?> successResult = item.getResult();
    assertThat(successResult.isSuccess(), is(true));
    final Object value = successResult.getValue();
    assertThat(value, is(not(nullValue())));
  }

  private LiveDataClient buildLiveDataClient() {

    final LiveDataMetaDataProvider dataProvider = s_testUtils.getLiveDataMetaDataProvider("bloomberg");

    LiveDataMetaData metaData =  dataProvider.metaData();
    URI jmsUri = metaData.getJmsBrokerUri();

    if (metaData.getServerType() != LiveDataServerTypes.STANDARD || jmsUri == null) {
      throw new OpenGammaRuntimeException("Unsupported live data server type " + metaData.getServerType() + " for " + metaData.getDescription());
    }

    JmsTemplate jmsTemplate = getJmsConnector().getJmsTemplateTopic();

    JmsByteArrayRequestSender jmsSubscriptionRequestSender;

    if (metaData.getJmsSubscriptionQueue() != null) {
      JmsTemplate subscriptionRequestTemplate = getJmsConnector().getJmsTemplateQueue();
      jmsSubscriptionRequestSender = new JmsByteArrayRequestSender(metaData.getJmsSubscriptionQueue(), subscriptionRequestTemplate);
    } else {
      jmsSubscriptionRequestSender = new JmsByteArrayRequestSender(metaData.getJmsSubscriptionTopic(), jmsTemplate);
    }
    ByteArrayFudgeRequestSender fudgeSubscriptionRequestSender = new ByteArrayFudgeRequestSender(jmsSubscriptionRequestSender);

    JmsByteArrayRequestSender jmsEntitlementRequestSender = new JmsByteArrayRequestSender(metaData.getJmsEntitlementTopic(), jmsTemplate);
    ByteArrayFudgeRequestSender fudgeEntitlementRequestSender = new ByteArrayFudgeRequestSender(jmsEntitlementRequestSender);

    final JmsLiveDataClient liveDataClient = new JmsLiveDataClient(fudgeSubscriptionRequestSender,
                                                                   fudgeEntitlementRequestSender, getJmsConnector(), OpenGammaFudgeContext.getInstance(), JmsLiveDataClient.DEFAULT_NUM_SESSIONS);
    liveDataClient.setFudgeContext(OpenGammaFudgeContext.getInstance());
    if (metaData.getJmsHeartbeatTopic() != null) {
      JmsByteArrayMessageSender jmsHeartbeatSender = new JmsByteArrayMessageSender(metaData.getJmsHeartbeatTopic(), jmsTemplate);
      liveDataClient.setHeartbeatMessageSender(jmsHeartbeatSender);
    }
    liveDataClient.start();
    return liveDataClient;
  }

  private JmsConnector getJmsConnector() {

    //ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("failover:(tcp://activemq.hq.opengamma.com:61616?daemon=true)?timeout=3000");
    //PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(connectionFactory);
    //JmsConnectorFactoryBean bean = new JmsConnectorFactoryBean();
    //bean.setName("StandardJms");
    //bean.setConnectionFactory(pooledConnectionFactory);
    //bean.setClientBrokerUri(URI.create("failover:(tcp://activemq.hq.opengamma.com:61616?daemon=true)?timeout=3000"));
    //return bean.getObject();
    return s_testUtils.getJmsConnector();
  }

  private static class RemoteProviderTestUtils {

    private final ComponentServer _components;
    private final JmsConnector _jmsConnector;
    private final FudgeMsg _configuration;

    public RemoteProviderTestUtils() {
      final String baseUrl = "http://devsvr-lx-2:8080/jax";
          //new StringBuilder("http://")
          //.append(System.getProperty("web.host", props.getProperty("opengamma.provider.host"))).append(':')
          //.append(System.getProperty("web.port", props.getProperty("opengamma.provider.port")))
          //.append(System.getProperty("web.path", props.getProperty("opengamma.provider.path")))
          //.append("jax").toString();

      final URI componentsUri = URI.create(baseUrl);
      final RemoteComponentServer remote = new RemoteComponentServer(componentsUri);
      _components = remote.getComponentServer();
      _configuration = FudgeRestClient.create().accessFudge(URI.create(baseUrl + "/configuration/0")).get(FudgeMsg.class);
      final String activeMQBroker = _configuration.getString("activeMQ");
      final JmsConnectorFactoryBean factory = new JmsConnectorFactoryBean();
      factory.setName(getClass().getSimpleName());
      factory.setClientBrokerUri(URI.create(activeMQBroker));
      factory.setConnectionFactory(new ActiveMQConnectionFactory(factory.getClientBrokerUri()));
      _jmsConnector = factory.getObjectCreating();
    }

    public JmsConnector getJmsConnector() {
      return _jmsConnector;
    }

    public LiveDataMetaDataProvider getLiveDataMetaDataProvider(final String classifier) {
      final URI uri = _components.getComponentInfo(LiveDataMetaDataProvider.class, classifier).getUri();
      return new RemoteLiveDataMetaDataProvider(uri);
    }
  }
}
