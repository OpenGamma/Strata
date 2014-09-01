/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.threeten.bp.Duration;
import org.threeten.bp.temporal.ChronoUnit;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.AbstractComponentFactory;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.server.CycleRunnerFactory;
import com.opengamma.sesame.server.DataFunctionServerResource;
import com.opengamma.sesame.server.DefaultFunctionServer;
import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.sesame.server.streaming.DataStreamingFunctionServerResource;
import com.opengamma.sesame.server.streaming.DefaultStreamingFunctionServer;
import com.opengamma.util.jms.JmsConnector;

/**
 * Component factory for the function server.
 */
@BeanDefinition
public class FunctionServerComponentFactory extends AbstractComponentFactory {

  private static final int DEFAULT_MINIMUM_TIME_BETWEEN_CYCLES = 5000;

  /**
   * The classifier that the factory should publish under.
   */
  @PropertyDefinition(validate = "notNull")
  private String _classifier;
  /**
   * Should the component be published over a REST interface. Set to false
   * if there is no desire to connect to the component remotely.
   */
  @PropertyDefinition(validate = "notNull")
  private boolean _publishRest = true;
  /**
   * Should the component enable streaming results. If set to true, then the
   * {@link #_jmsConnector} and {@link #_scheduledExecutor} should not be null.
   */
  @PropertyDefinition(validate = "notNull")
  private boolean _enableStreamedResults = true;
  /**
   * The view factory that is responsible for creating views.
   */
  @PropertyDefinition(validate = "notNull")
  private ViewFactory _viewFactory;
  /**
   * The factory for market data sources.
   */
  @PropertyDefinition(validate = "notNull")
  private MarketDataFactory _marketDataFactory;
  /**
   * The JMS connector used for streaming of live results to clients. If null, then
   * streaming will not be available.
   */
  @PropertyDefinition
  private JmsConnector _jmsConnector;
  /**
   * The client to use for connecting to live market data.
   */
  @PropertyDefinition
  private LiveDataClient _liveDataClient;
  /**
   * The scheduled executor service.
   */
  @PropertyDefinition
  private ScheduledExecutorService _scheduledExecutor = Executors.newScheduledThreadPool(5);
  /**
   * The minimum time between cycles (in milliseconds).
   */
  @PropertyDefinition
  private long _minimumTimeBetweenCycles = DEFAULT_MINIMUM_TIME_BETWEEN_CYCLES;

  //-------------------------------------------------------------------------
  @Override
  public void init(ComponentRepository repo, LinkedHashMap<String, String> configuration) throws Exception {

    CycleRunnerFactory cycleRunnerFactory = new CycleRunnerFactory(
        getViewFactory(), getMarketDataFactory(), Duration.of(_minimumTimeBetweenCycles, ChronoUnit.MILLIS));
    DefaultFunctionServer server = initFunctionServer(repo, cycleRunnerFactory);
    if (isEnableStreamedResults()) {
      initStreamingServer(repo, server, cycleRunnerFactory);
    }
  }

  private DefaultFunctionServer initFunctionServer(ComponentRepository repo, CycleRunnerFactory cycleRunnerFactory) {
    DefaultFunctionServer server = new DefaultFunctionServer(cycleRunnerFactory);
    repo.registerComponent(FunctionServer.class, getClassifier(), server);
    if (isPublishRest()) {
      repo.getRestComponents().publishResource(new DataFunctionServerResource(server));
    }
    return server;
  }

  private void initStreamingServer(ComponentRepository repo,
                                   DefaultFunctionServer server,
                                   CycleRunnerFactory cycleRunnerFactory) {

    String msg = "Streaming results have been requested but %s is null - streaming will not be available";
    if (getJmsConnector() == null) {
      throw new OpenGammaRuntimeException(String.format(msg, "jmsConnector"));
    } else if (getScheduledExecutor() == null) {
      throw new OpenGammaRuntimeException(String.format(msg, "scheduledExecutor"));
    }

    DefaultStreamingFunctionServer streamingServer = new DefaultStreamingFunctionServer(server, cycleRunnerFactory);
    repo.registerComponent(DefaultStreamingFunctionServer.class, getClassifier(), streamingServer);
    if (isPublishRest()) {
      repo.getRestComponents().publishResource(new DataStreamingFunctionServerResource(streamingServer, getJmsConnector(), getScheduledExecutor()));
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FunctionServerComponentFactory}.
   * @return the meta-bean, not null
   */
  public static FunctionServerComponentFactory.Meta meta() {
    return FunctionServerComponentFactory.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FunctionServerComponentFactory.Meta.INSTANCE);
  }

  @Override
  public FunctionServerComponentFactory.Meta metaBean() {
    return FunctionServerComponentFactory.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the classifier that the factory should publish under.
   * @return the value of the property, not null
   */
  public String getClassifier() {
    return _classifier;
  }

  /**
   * Sets the classifier that the factory should publish under.
   * @param classifier  the new value of the property, not null
   */
  public void setClassifier(String classifier) {
    JodaBeanUtils.notNull(classifier, "classifier");
    this._classifier = classifier;
  }

  /**
   * Gets the the {@code classifier} property.
   * @return the property, not null
   */
  public final Property<String> classifier() {
    return metaBean().classifier().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets should the component be published over a REST interface. Set to false
   * if there is no desire to connect to the component remotely.
   * @return the value of the property, not null
   */
  public boolean isPublishRest() {
    return _publishRest;
  }

  /**
   * Sets should the component be published over a REST interface. Set to false
   * if there is no desire to connect to the component remotely.
   * @param publishRest  the new value of the property, not null
   */
  public void setPublishRest(boolean publishRest) {
    JodaBeanUtils.notNull(publishRest, "publishRest");
    this._publishRest = publishRest;
  }

  /**
   * Gets the the {@code publishRest} property.
   * if there is no desire to connect to the component remotely.
   * @return the property, not null
   */
  public final Property<Boolean> publishRest() {
    return metaBean().publishRest().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets should the component enable streaming results. If set to true, then the
   * {@link #_jmsConnector} and {@link #_scheduledExecutor} should not be null.
   * @return the value of the property, not null
   */
  public boolean isEnableStreamedResults() {
    return _enableStreamedResults;
  }

  /**
   * Sets should the component enable streaming results. If set to true, then the
   * {@link #_jmsConnector} and {@link #_scheduledExecutor} should not be null.
   * @param enableStreamedResults  the new value of the property, not null
   */
  public void setEnableStreamedResults(boolean enableStreamedResults) {
    JodaBeanUtils.notNull(enableStreamedResults, "enableStreamedResults");
    this._enableStreamedResults = enableStreamedResults;
  }

  /**
   * Gets the the {@code enableStreamedResults} property.
   * {@link #_jmsConnector} and {@link #_scheduledExecutor} should not be null.
   * @return the property, not null
   */
  public final Property<Boolean> enableStreamedResults() {
    return metaBean().enableStreamedResults().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the view factory that is responsible for creating views.
   * @return the value of the property, not null
   */
  public ViewFactory getViewFactory() {
    return _viewFactory;
  }

  /**
   * Sets the view factory that is responsible for creating views.
   * @param viewFactory  the new value of the property, not null
   */
  public void setViewFactory(ViewFactory viewFactory) {
    JodaBeanUtils.notNull(viewFactory, "viewFactory");
    this._viewFactory = viewFactory;
  }

  /**
   * Gets the the {@code viewFactory} property.
   * @return the property, not null
   */
  public final Property<ViewFactory> viewFactory() {
    return metaBean().viewFactory().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the factory for market data sources.
   * @return the value of the property, not null
   */
  public MarketDataFactory getMarketDataFactory() {
    return _marketDataFactory;
  }

  /**
   * Sets the factory for market data sources.
   * @param marketDataFactory  the new value of the property, not null
   */
  public void setMarketDataFactory(MarketDataFactory marketDataFactory) {
    JodaBeanUtils.notNull(marketDataFactory, "marketDataFactory");
    this._marketDataFactory = marketDataFactory;
  }

  /**
   * Gets the the {@code marketDataFactory} property.
   * @return the property, not null
   */
  public final Property<MarketDataFactory> marketDataFactory() {
    return metaBean().marketDataFactory().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the JMS connector used for streaming of live results to clients. If null, then
   * streaming will not be available.
   * @return the value of the property
   */
  public JmsConnector getJmsConnector() {
    return _jmsConnector;
  }

  /**
   * Sets the JMS connector used for streaming of live results to clients. If null, then
   * streaming will not be available.
   * @param jmsConnector  the new value of the property
   */
  public void setJmsConnector(JmsConnector jmsConnector) {
    this._jmsConnector = jmsConnector;
  }

  /**
   * Gets the the {@code jmsConnector} property.
   * streaming will not be available.
   * @return the property, not null
   */
  public final Property<JmsConnector> jmsConnector() {
    return metaBean().jmsConnector().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the client to use for connecting to live market data.
   * @return the value of the property
   */
  public LiveDataClient getLiveDataClient() {
    return _liveDataClient;
  }

  /**
   * Sets the client to use for connecting to live market data.
   * @param liveDataClient  the new value of the property
   */
  public void setLiveDataClient(LiveDataClient liveDataClient) {
    this._liveDataClient = liveDataClient;
  }

  /**
   * Gets the the {@code liveDataClient} property.
   * @return the property, not null
   */
  public final Property<LiveDataClient> liveDataClient() {
    return metaBean().liveDataClient().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the scheduled executor service.
   * @return the value of the property
   */
  public ScheduledExecutorService getScheduledExecutor() {
    return _scheduledExecutor;
  }

  /**
   * Sets the scheduled executor service.
   * @param scheduledExecutor  the new value of the property
   */
  public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
    this._scheduledExecutor = scheduledExecutor;
  }

  /**
   * Gets the the {@code scheduledExecutor} property.
   * @return the property, not null
   */
  public final Property<ScheduledExecutorService> scheduledExecutor() {
    return metaBean().scheduledExecutor().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the minimum time between cycles (in milliseconds).
   * @return the value of the property
   */
  public long getMinimumTimeBetweenCycles() {
    return _minimumTimeBetweenCycles;
  }

  /**
   * Sets the minimum time between cycles (in milliseconds).
   * @param minimumTimeBetweenCycles  the new value of the property
   */
  public void setMinimumTimeBetweenCycles(long minimumTimeBetweenCycles) {
    this._minimumTimeBetweenCycles = minimumTimeBetweenCycles;
  }

  /**
   * Gets the the {@code minimumTimeBetweenCycles} property.
   * @return the property, not null
   */
  public final Property<Long> minimumTimeBetweenCycles() {
    return metaBean().minimumTimeBetweenCycles().createProperty(this);
  }

  //-----------------------------------------------------------------------
  @Override
  public FunctionServerComponentFactory clone() {
    return JodaBeanUtils.cloneAlways(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FunctionServerComponentFactory other = (FunctionServerComponentFactory) obj;
      return JodaBeanUtils.equal(getClassifier(), other.getClassifier()) &&
          (isPublishRest() == other.isPublishRest()) &&
          (isEnableStreamedResults() == other.isEnableStreamedResults()) &&
          JodaBeanUtils.equal(getViewFactory(), other.getViewFactory()) &&
          JodaBeanUtils.equal(getMarketDataFactory(), other.getMarketDataFactory()) &&
          JodaBeanUtils.equal(getJmsConnector(), other.getJmsConnector()) &&
          JodaBeanUtils.equal(getLiveDataClient(), other.getLiveDataClient()) &&
          JodaBeanUtils.equal(getScheduledExecutor(), other.getScheduledExecutor()) &&
          (getMinimumTimeBetweenCycles() == other.getMinimumTimeBetweenCycles()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getClassifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(isPublishRest());
    hash += hash * 31 + JodaBeanUtils.hashCode(isEnableStreamedResults());
    hash += hash * 31 + JodaBeanUtils.hashCode(getViewFactory());
    hash += hash * 31 + JodaBeanUtils.hashCode(getMarketDataFactory());
    hash += hash * 31 + JodaBeanUtils.hashCode(getJmsConnector());
    hash += hash * 31 + JodaBeanUtils.hashCode(getLiveDataClient());
    hash += hash * 31 + JodaBeanUtils.hashCode(getScheduledExecutor());
    hash += hash * 31 + JodaBeanUtils.hashCode(getMinimumTimeBetweenCycles());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("FunctionServerComponentFactory{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  @Override
  protected void toString(StringBuilder buf) {
    super.toString(buf);
    buf.append("classifier").append('=').append(JodaBeanUtils.toString(getClassifier())).append(',').append(' ');
    buf.append("publishRest").append('=').append(JodaBeanUtils.toString(isPublishRest())).append(',').append(' ');
    buf.append("enableStreamedResults").append('=').append(JodaBeanUtils.toString(isEnableStreamedResults())).append(',').append(' ');
    buf.append("viewFactory").append('=').append(JodaBeanUtils.toString(getViewFactory())).append(',').append(' ');
    buf.append("marketDataFactory").append('=').append(JodaBeanUtils.toString(getMarketDataFactory())).append(',').append(' ');
    buf.append("jmsConnector").append('=').append(JodaBeanUtils.toString(getJmsConnector())).append(',').append(' ');
    buf.append("liveDataClient").append('=').append(JodaBeanUtils.toString(getLiveDataClient())).append(',').append(' ');
    buf.append("scheduledExecutor").append('=').append(JodaBeanUtils.toString(getScheduledExecutor())).append(',').append(' ');
    buf.append("minimumTimeBetweenCycles").append('=').append(JodaBeanUtils.toString(getMinimumTimeBetweenCycles())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FunctionServerComponentFactory}.
   */
  public static class Meta extends AbstractComponentFactory.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code classifier} property.
     */
    private final MetaProperty<String> _classifier = DirectMetaProperty.ofReadWrite(
        this, "classifier", FunctionServerComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code publishRest} property.
     */
    private final MetaProperty<Boolean> _publishRest = DirectMetaProperty.ofReadWrite(
        this, "publishRest", FunctionServerComponentFactory.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code enableStreamedResults} property.
     */
    private final MetaProperty<Boolean> _enableStreamedResults = DirectMetaProperty.ofReadWrite(
        this, "enableStreamedResults", FunctionServerComponentFactory.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code viewFactory} property.
     */
    private final MetaProperty<ViewFactory> _viewFactory = DirectMetaProperty.ofReadWrite(
        this, "viewFactory", FunctionServerComponentFactory.class, ViewFactory.class);
    /**
     * The meta-property for the {@code marketDataFactory} property.
     */
    private final MetaProperty<MarketDataFactory> _marketDataFactory = DirectMetaProperty.ofReadWrite(
        this, "marketDataFactory", FunctionServerComponentFactory.class, MarketDataFactory.class);
    /**
     * The meta-property for the {@code jmsConnector} property.
     */
    private final MetaProperty<JmsConnector> _jmsConnector = DirectMetaProperty.ofReadWrite(
        this, "jmsConnector", FunctionServerComponentFactory.class, JmsConnector.class);
    /**
     * The meta-property for the {@code liveDataClient} property.
     */
    private final MetaProperty<LiveDataClient> _liveDataClient = DirectMetaProperty.ofReadWrite(
        this, "liveDataClient", FunctionServerComponentFactory.class, LiveDataClient.class);
    /**
     * The meta-property for the {@code scheduledExecutor} property.
     */
    private final MetaProperty<ScheduledExecutorService> _scheduledExecutor = DirectMetaProperty.ofReadWrite(
        this, "scheduledExecutor", FunctionServerComponentFactory.class, ScheduledExecutorService.class);
    /**
     * The meta-property for the {@code minimumTimeBetweenCycles} property.
     */
    private final MetaProperty<Long> _minimumTimeBetweenCycles = DirectMetaProperty.ofReadWrite(
        this, "minimumTimeBetweenCycles", FunctionServerComponentFactory.class, Long.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "classifier",
        "publishRest",
        "enableStreamedResults",
        "viewFactory",
        "marketDataFactory",
        "jmsConnector",
        "liveDataClient",
        "scheduledExecutor",
        "minimumTimeBetweenCycles");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          return _classifier;
        case -614707837:  // publishRest
          return _publishRest;
        case -1827093804:  // enableStreamedResults
          return _enableStreamedResults;
        case -1101448539:  // viewFactory
          return _viewFactory;
        case -1673716700:  // marketDataFactory
          return _marketDataFactory;
        case -1495762275:  // jmsConnector
          return _jmsConnector;
        case 244858401:  // liveDataClient
          return _liveDataClient;
        case 1586176160:  // scheduledExecutor
          return _scheduledExecutor;
        case -1691789766:  // minimumTimeBetweenCycles
          return _minimumTimeBetweenCycles;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FunctionServerComponentFactory> builder() {
      return new DirectBeanBuilder<FunctionServerComponentFactory>(new FunctionServerComponentFactory());
    }

    @Override
    public Class<? extends FunctionServerComponentFactory> beanType() {
      return FunctionServerComponentFactory.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code classifier} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> classifier() {
      return _classifier;
    }

    /**
     * The meta-property for the {@code publishRest} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> publishRest() {
      return _publishRest;
    }

    /**
     * The meta-property for the {@code enableStreamedResults} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> enableStreamedResults() {
      return _enableStreamedResults;
    }

    /**
     * The meta-property for the {@code viewFactory} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ViewFactory> viewFactory() {
      return _viewFactory;
    }

    /**
     * The meta-property for the {@code marketDataFactory} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<MarketDataFactory> marketDataFactory() {
      return _marketDataFactory;
    }

    /**
     * The meta-property for the {@code jmsConnector} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<JmsConnector> jmsConnector() {
      return _jmsConnector;
    }

    /**
     * The meta-property for the {@code liveDataClient} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LiveDataClient> liveDataClient() {
      return _liveDataClient;
    }

    /**
     * The meta-property for the {@code scheduledExecutor} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ScheduledExecutorService> scheduledExecutor() {
      return _scheduledExecutor;
    }

    /**
     * The meta-property for the {@code minimumTimeBetweenCycles} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> minimumTimeBetweenCycles() {
      return _minimumTimeBetweenCycles;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          return ((FunctionServerComponentFactory) bean).getClassifier();
        case -614707837:  // publishRest
          return ((FunctionServerComponentFactory) bean).isPublishRest();
        case -1827093804:  // enableStreamedResults
          return ((FunctionServerComponentFactory) bean).isEnableStreamedResults();
        case -1101448539:  // viewFactory
          return ((FunctionServerComponentFactory) bean).getViewFactory();
        case -1673716700:  // marketDataFactory
          return ((FunctionServerComponentFactory) bean).getMarketDataFactory();
        case -1495762275:  // jmsConnector
          return ((FunctionServerComponentFactory) bean).getJmsConnector();
        case 244858401:  // liveDataClient
          return ((FunctionServerComponentFactory) bean).getLiveDataClient();
        case 1586176160:  // scheduledExecutor
          return ((FunctionServerComponentFactory) bean).getScheduledExecutor();
        case -1691789766:  // minimumTimeBetweenCycles
          return ((FunctionServerComponentFactory) bean).getMinimumTimeBetweenCycles();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          ((FunctionServerComponentFactory) bean).setClassifier((String) newValue);
          return;
        case -614707837:  // publishRest
          ((FunctionServerComponentFactory) bean).setPublishRest((Boolean) newValue);
          return;
        case -1827093804:  // enableStreamedResults
          ((FunctionServerComponentFactory) bean).setEnableStreamedResults((Boolean) newValue);
          return;
        case -1101448539:  // viewFactory
          ((FunctionServerComponentFactory) bean).setViewFactory((ViewFactory) newValue);
          return;
        case -1673716700:  // marketDataFactory
          ((FunctionServerComponentFactory) bean).setMarketDataFactory((MarketDataFactory) newValue);
          return;
        case -1495762275:  // jmsConnector
          ((FunctionServerComponentFactory) bean).setJmsConnector((JmsConnector) newValue);
          return;
        case 244858401:  // liveDataClient
          ((FunctionServerComponentFactory) bean).setLiveDataClient((LiveDataClient) newValue);
          return;
        case 1586176160:  // scheduledExecutor
          ((FunctionServerComponentFactory) bean).setScheduledExecutor((ScheduledExecutorService) newValue);
          return;
        case -1691789766:  // minimumTimeBetweenCycles
          ((FunctionServerComponentFactory) bean).setMinimumTimeBetweenCycles((Long) newValue);
          return;
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

    @Override
    protected void validate(Bean bean) {
      JodaBeanUtils.notNull(((FunctionServerComponentFactory) bean)._classifier, "classifier");
      JodaBeanUtils.notNull(((FunctionServerComponentFactory) bean)._publishRest, "publishRest");
      JodaBeanUtils.notNull(((FunctionServerComponentFactory) bean)._enableStreamedResults, "enableStreamedResults");
      JodaBeanUtils.notNull(((FunctionServerComponentFactory) bean)._viewFactory, "viewFactory");
      JodaBeanUtils.notNull(((FunctionServerComponentFactory) bean)._marketDataFactory, "marketDataFactory");
      super.validate(bean);
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
