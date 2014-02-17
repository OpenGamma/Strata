/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.config.ConfigBuilder.viewDef;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.JPY;
import static com.opengamma.util.money.Currency.USD;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;

import net.sf.ehcache.CacheManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeMsg;
import org.springframework.jms.core.JmsTemplate;
import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableSet;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.component.ComponentServer;
import com.opengamma.component.rest.RemoteComponentServer;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.currency.CurrencyPair;
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
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Engine;
import com.opengamma.sesame.engine.EngineService;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.engine.ResultItem;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
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

  private static final RemoteProviderTestUtils TEST_UTILS = new RemoteProviderTestUtils();

  // Create a view with known curve and try to get market data
  // from the market data server
  public void testCurveHasData() throws InterruptedException {
    ViewDef viewDef =
        viewDef("Curve Bundle only",
                nonPortfolioOutput("Curve Bundle",
                                   output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                                          config(
                                              arguments(
                                                  function(RootFinderConfiguration.class,
                                                           argument("rootFinderAbsoluteTolerance", 1e-9),
                                                           argument("rootFinderRelativeTolerance", 1e-9),
                                                           argument("rootFinderMaxIterations", 1000)),
                                                  function(DefaultCurrencyPairsFn.class,
                                                           argument("currencyPairs",
                                                                    ImmutableSet.of(CurrencyPair.of(USD, JPY),
                                                                                    CurrencyPair.of(EUR, USD),
                                                                                    CurrencyPair.of(GBP, USD)))),
                                                  function(DefaultHistoricalTimeSeriesFn.class,
                                                           argument("resolutionKey", "DEFAULT_TSS"),
                                                           argument("htsRetrievalPeriod", Period.ofYears(1))),
                                                  function(DefaultDiscountingMulticurveBundleFn.class,
                                                           argument("impliedCurveNames", Collections.emptySet()),
                                                           argument("curveConfig",
                                                                    ConfigLink.of("Temple USD",
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
                                      ConfigDbMarketExposureSelectorFn.class);

    ComponentMap componentMap = ComponentMap.loadComponents("http://devsvr-lx-2:8080");
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider();
    ServiceContext serviceContext =
        ServiceContext.of(componentMap.getComponents()).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    Engine engine = new Engine(new DirectExecutorService(),
                               componentMap,
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(EngineService.class));

    View view = engine.createView(viewDef, Collections.emptyList());
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    ResettableLiveRawMarketDataSource rawDataSource = buildRawDataSource();
    MarketDataFn marketDataFn = new EagerMarketDataFn(rawDataSource,
                                                      componentMap.getComponent(ConfigSource.class),
                                                      "BloombergLiveData");
    MarketDataFactory marketDataFactory = new SimpleMarketDataFactory(marketDataFn);
    Results initialResults = view.run(new CycleArguments(valuationTime, VersionCorrection.LATEST, marketDataFactory));
    System.out.println(initialResults);

    // First time through we're waiting for market data so expect failure
    ResultItem resultItem = initialResults.get("Curve Bundle");
    Result<?> failureResult = resultItem.getResult();
    assertThat(failureResult.isValueAvailable(), is(false));
    assertThat(failureResult.getStatus(), is((ResultStatus) FailureStatus.MISSING_DATA));

    // Now try again, resetting the market data first (which should pick up bloomberg data)
    System.out.println("Waiting for market data to catch up");
    //Thread.sleep(5000);
    rawDataSource.waitForData();

    Results results = view.run(new CycleArguments(valuationTime, VersionCorrection.LATEST, marketDataFactory));
    System.out.println(results);

    // Second time we should have received the market data
    ResultItem item = results.get("Curve Bundle");
    Result<?> successResult = item.getResult();
    assertThat(successResult.isValueAvailable(), is(true));
    final Object value = successResult.getValue();
    assertThat(value, is(not(nullValue())));
  }

  private ResettableLiveRawMarketDataSource buildRawDataSource() {
    return new ResettableLiveRawMarketDataSource(new LiveDataManager(buildLiveDataClient()));
  }

  private LiveDataClient buildLiveDataClient() {

    final LiveDataMetaDataProvider dataProvider = TEST_UTILS.getLiveDataMetaDataProvider("bloomberg");

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
    return TEST_UTILS.getJmsConnector();
  }

  private static class RemoteProviderTestUtils {

    private final FudgeContext _fudgeContext;
    private final ComponentServer _components;
    private final JmsConnector _jmsConnector;
    private final FudgeMsg _configuration;

    public RemoteProviderTestUtils() {
      _fudgeContext = OpenGammaFudgeContext.getInstance();
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
