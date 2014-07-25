/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.livedata.client.JmsLiveDataClient;
import com.opengamma.provider.livedata.LiveDataMetaData;
import com.opengamma.provider.livedata.LiveDataMetaDataProvider;
import com.opengamma.provider.livedata.LiveDataServerTypes;
import com.opengamma.sesame.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.sesame.marketdata.spec.MarketDataSpecification;
import com.opengamma.transport.ByteArrayFudgeRequestSender;
import com.opengamma.transport.jms.JmsByteArrayMessageSender;
import com.opengamma.transport.jms.JmsByteArrayRequestSender;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.jms.JmsConnector;
import com.opengamma.util.jms.JmsConnectorFactoryBean;
import com.opengamma.util.metric.OpenGammaMetricRegistry;

/**
 * Factory for {@link MarketDataSource} instances which use live market data from any available provider.
 */
public class LiveMarketDataFactory implements MarketDataFactory {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(LiveMarketDataFactory.class);

  private final Map<String, LiveDataManager> _liveDataManagerBySource;
  
  public LiveMarketDataFactory(Collection<LiveDataMetaDataProvider> providers, JmsConnector jmsConnector) {
    ImmutableMap.Builder<String, LiveDataManager> builder = ImmutableMap.builder();
    for (LiveDataMetaDataProvider provider : providers) {
      LiveDataClient liveDataClient = createLiveDataClient(provider, jmsConnector);
      builder.put(provider.metaData().getDescription(), new DefaultLiveDataManager(liveDataClient));
    }
    _liveDataManagerBySource = builder.build();
  }
  
  @Override
  public StrategyAwareMarketDataSource create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof LiveMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + LiveMarketDataSpecification.class + " but was " + spec.getClass());
    }
    LiveMarketDataSpecification liveMarketDataSpec = (LiveMarketDataSpecification) spec;
    LiveDataManager liveDataManager = _liveDataManagerBySource.get(liveMarketDataSpec.getDataSource());
    if (liveDataManager == null) {
      throw new IllegalArgumentException("Unsupported live data source: " + liveMarketDataSpec.getDataSource());
    }
    LDClient liveDataClient = new LDClient(liveDataManager);
    return new ResettableLiveMarketDataSource(spec, liveDataClient);
  }

  /**
   * Creates a live data client based on the information in the remote metadata.
   * 
   * @param provider the metadata provider, null returns null
   * @param jmsConnector the JMS connector, not null
   * @return the client
   */
  @SuppressWarnings("deprecation")
  public static LiveDataClient createLiveDataClient(LiveDataMetaDataProvider provider, JmsConnector jmsConnector) {
    ArgumentChecker.notNull(jmsConnector, "jmsConnector");
    LiveDataMetaData metaData = provider.metaData();
    URI jmsUri = metaData.getJmsBrokerUri();
    if (metaData.getServerType() != LiveDataServerTypes.STANDARD || jmsUri == null) {
      s_logger.warn("Unsupported live data server type " + metaData.getServerType() + " for " +
          metaData.getDescription() + " live data provider. This provider will not be available.");
      return null;
    }
    if (!jmsConnector.getClientBrokerUri().equals(jmsUri)) {
      JmsConnectorFactoryBean jmsFactory = new JmsConnectorFactoryBean(jmsConnector);
      jmsFactory.setClientBrokerUri(jmsUri);
      jmsConnector = jmsFactory.getObjectCreating();
    }
    
    JmsTemplate jmsTemplate = jmsConnector.getJmsTemplateTopic();
    JmsByteArrayRequestSender jmsSubsReqSender;
    if (metaData.getJmsSubscriptionQueue() != null) {
      JmsTemplate subsReqTemplate = jmsConnector.getJmsTemplateQueue();
      jmsSubsReqSender = new JmsByteArrayRequestSender(metaData.getJmsSubscriptionQueue(), subsReqTemplate);
    } else {
      jmsSubsReqSender = new JmsByteArrayRequestSender(metaData.getJmsSubscriptionTopic(), jmsTemplate);
    }
    ByteArrayFudgeRequestSender fudgeSubscriptionReqSender = new ByteArrayFudgeRequestSender(jmsSubsReqSender);
    
    JmsByteArrayRequestSender jmsEntReqSender = new JmsByteArrayRequestSender(
        metaData.getJmsEntitlementTopic(), jmsTemplate);
    ByteArrayFudgeRequestSender fudgeEntReqSender = new ByteArrayFudgeRequestSender(jmsEntReqSender);
    
    final JmsLiveDataClient liveDataClient = new JmsLiveDataClient(fudgeSubscriptionReqSender,
        fudgeEntReqSender, jmsConnector, OpenGammaFudgeContext.getInstance(), JmsLiveDataClient.DEFAULT_NUM_SESSIONS);
    liveDataClient.setFudgeContext(OpenGammaFudgeContext.getInstance());
    if (metaData.getJmsHeartbeatTopic() != null) {
      JmsByteArrayMessageSender jmsHeartbeatSender = new JmsByteArrayMessageSender(
          metaData.getJmsHeartbeatTopic(), jmsTemplate);
      liveDataClient.setHeartbeatMessageSender(jmsHeartbeatSender);
    }
    liveDataClient.start();
    liveDataClient.registerMetrics(
        OpenGammaMetricRegistry.getSummaryInstance(),
        OpenGammaMetricRegistry.getDetailedInstance(),
        "LiveDataClient - " + provider.metaData().getDescription());
    return liveDataClient;
  }

}
