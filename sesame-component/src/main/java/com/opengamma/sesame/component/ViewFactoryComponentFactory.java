/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.shiro.concurrent.SubjectAwareExecutorService;
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
import org.threeten.bp.Instant;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.opengamma.component.ComponentInfo;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.AbstractComponentFactory;
import com.opengamma.engine.marketdata.live.LiveMarketDataProviderFactory;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultFXReturnSeriesFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.cache.MethodInvocationKey;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.CachingManager;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.DefaultCachingManager;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.engine.FunctionService;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.equity.DefaultEquityPresentValueFn;
import com.opengamma.sesame.equity.EquityPresentValueFn;
import com.opengamma.sesame.fra.FRAFn;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.fxforward.DiscountingFXForwardPVFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardSpotPnLSeriesFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardYCNSPnLSeriesFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.fxforward.FXForwardDiscountingCalculatorFn;
import com.opengamma.sesame.fxforward.FXForwardPVFn;
import com.opengamma.sesame.fxforward.FXForwardPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardYCNSPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.irs.InterestRateSwapFn;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FixedHistoricalMarketDataFactory;
import com.opengamma.sesame.pnl.DefaultHistoricalPnLFXConverterFn;
import com.opengamma.util.auth.AuthUtils;

/**
 * Component factory for the engine.
 */
@BeanDefinition
public class ViewFactoryComponentFactory extends AbstractComponentFactory {

  /**
   * The default maximum size of the view factory cache if none is specified in the config.
   */
  private static final long MAX_CACHE_ENTRIES = 10_000;

  /**
   * The classifier that the factory should publish under.
   */
  @PropertyDefinition(validate = "notNull")
  private String _classifier;
  /**
   * For obtaining the live market data provider names.
   */
  @PropertyDefinition
  private LiveMarketDataProviderFactory _liveMarketDataProviderFactory;
  /**
   * Maximum number of entries to store in the cache.
   */
  @PropertyDefinition
  private long _maxCacheEntries = MAX_CACHE_ENTRIES;

  //-------------------------------------------------------------------------
  @Override
  public void init(ComponentRepository repo, LinkedHashMap<String, String> configuration) throws Exception {

    Map<Class<?>, Object> components = getComponents(repo, configuration);
    ComponentMap componentMap = ComponentMap.of(components);

    // Indicate remaining configuration has been used
    configuration.clear();

    Cache<MethodInvocationKey, FutureTask<Object>> cache = createCache(repo);
    CachingManager cachingManager = new DefaultCachingManager(componentMap, cache);

    // Initialize the service context with the same wrapped components that
    // we use within the engine itself
    initServiceContext(repo, cachingManager.getComponentMap().getComponents());

    ExecutorService executor = createExecutorService(repo);
    AvailableOutputs availableOutputs = createAvailableOutputs(repo);
    AvailableImplementations availableImplementations = createAvailableImplementations(repo);
    ViewFactory viewFactory = new ViewFactory(executor,
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              FunctionService.DEFAULT_SERVICES,
                                              cachingManager);

    ComponentInfo engineInfo = new ComponentInfo(ViewFactory.class, getClassifier());
    repo.registerComponent(engineInfo, viewFactory);

    ComponentInfo outputsInfo = new ComponentInfo(AvailableOutputs.class, getClassifier());
    repo.registerComponent(outputsInfo, availableOutputs);

    ComponentInfo implsInfo = new ComponentInfo(AvailableImplementations.class, getClassifier());
    repo.registerComponent(implsInfo, availableImplementations);
  }

  private Map<Class<?>, Object> getComponents(ComponentRepository repo, LinkedHashMap<String, String> configuration) {
    Map<String, ComponentInfo> infos = repo.findInfos(configuration);
    configuration.keySet().removeAll(infos.keySet());
    Map<Class<?>, Object> components = new HashMap<>();
    for (ComponentInfo info : infos.values()) {
      components.put(info.getType(), repo.getInstance(info));
    }
    return components;
  }

  /**
   * Initializes the static {@code ServiceContext}.
   * 
   * @param repo  the component repository, typically not used, not null
   * @param components  the map of components, not null
   */
  protected void initServiceContext(ComponentRepository repo, Map<Class<?>, Object> components) {
    ServiceContext serviceContext = ServiceContext.of(components)
        .with(VersionCorrectionProvider.class, new FixedInstantVersionCorrectionProvider(Instant.now()));
    ThreadLocalServiceContext.init(serviceContext);
  }

  /**
   * Create the executor service.
   * <p>
   * This implementation uses a fixed size thread pool based on the number of available processors.
   * If authentication is in use, Apache Shiro is attached to the executor service.
   * 
   * @param repo  the component repository, typically not used, not null
   * @return the executor service, not null
   */
  protected ExecutorService createExecutorService(ComponentRepository repo) {
    // TODO allow the thread pool to grow to allow for threads that block waiting for a cache value to be calculated?
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    return AuthUtils.isPermissive() ? executor : new SubjectAwareExecutorService(executor);
  }

  /**
   * Create the available outputs.
   * 
   * @param repo  the component repository, typically not used, not null
   * @return the available outputs, not null
   */
  protected AvailableOutputs createAvailableOutputs(ComponentRepository repo) {
    AvailableOutputs available = new AvailableOutputsImpl();
    available.register(EquityPresentValueFn.class,
        FRAFn.class,
        InterestRateSwapFn.class,
        DiscountingMulticurveBundleResolverFn.class,
        FXForwardPnLSeriesFn.class,
        FXForwardPVFn.class,
        FXForwardYCNSPnLSeriesFn.class,
        FXForwardYieldCurveNodeSensitivitiesFn.class);
    return available;
  }

  /**
   * Create the available implementations.
   * 
   * @param repo  the component repository, typically not used, not null
   * @return the available implementations, not null
   */
  protected AvailableImplementations createAvailableImplementations(ComponentRepository repo) {
    AvailableImplementations available = new AvailableImplementationsImpl();
    available.register(
        DiscountingFXForwardYieldCurveNodeSensitivitiesFn.class,
        DiscountingFXForwardSpotPnLSeriesFn.class,
        DiscountingFXForwardYCNSPnLSeriesFn.class,
        DiscountingFXForwardPVFn.class,
        DefaultFXReturnSeriesFn.class,
        DefaultCurrencyPairsFn.class,
        DefaultEquityPresentValueFn.class,
        FXForwardSecurityConverter.class,
        ConfigDBInstrumentExposuresProvider.class,
        DefaultCurveSpecificationMarketDataFn.class,
        DefaultFXMatrixFn.class,
        DefaultCurveDefinitionFn.class,
        DefaultDiscountingMulticurveBundleFn.class,
        DefaultDiscountingMulticurveBundleResolverFn.class,
        DefaultCurveSpecificationFn.class,
        ConfigDBCurveConstructionConfigurationSource.class,
        DefaultHistoricalTimeSeriesFn.class,
        FXForwardDiscountingCalculatorFn.class,
        ConfigDbMarketExposureSelectorFn.class,
        ExposureFunctionsDiscountingMulticurveCombinerFn.class,
        FixedHistoricalMarketDataFactory.class,
        DefaultMarketDataFn.class,
        DefaultHistoricalMarketDataFn.class,
        DefaultCurveNodeConverterFn.class,
        DefaultHistoricalPnLFXConverterFn.class);
    return available;
  }

  /**
   * Create the cache.
   * 
   * @param repo  the component repository, typically not used, not null
   * @return the cache, not null
   */
  protected Cache<MethodInvocationKey, FutureTask<Object>> createCache(ComponentRepository repo) {
    int concurrencyLevel = Runtime.getRuntime().availableProcessors() + 2;
    return CacheBuilder.newBuilder()
        .maximumSize(getMaxCacheEntries())
        .softValues()
        .concurrencyLevel(concurrencyLevel)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ViewFactoryComponentFactory}.
   * @return the meta-bean, not null
   */
  public static ViewFactoryComponentFactory.Meta meta() {
    return ViewFactoryComponentFactory.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ViewFactoryComponentFactory.Meta.INSTANCE);
  }

  @Override
  public ViewFactoryComponentFactory.Meta metaBean() {
    return ViewFactoryComponentFactory.Meta.INSTANCE;
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
   * Gets for obtaining the live market data provider names.
   * @return the value of the property
   */
  public LiveMarketDataProviderFactory getLiveMarketDataProviderFactory() {
    return _liveMarketDataProviderFactory;
  }

  /**
   * Sets for obtaining the live market data provider names.
   * @param liveMarketDataProviderFactory  the new value of the property
   */
  public void setLiveMarketDataProviderFactory(LiveMarketDataProviderFactory liveMarketDataProviderFactory) {
    this._liveMarketDataProviderFactory = liveMarketDataProviderFactory;
  }

  /**
   * Gets the the {@code liveMarketDataProviderFactory} property.
   * @return the property, not null
   */
  public final Property<LiveMarketDataProviderFactory> liveMarketDataProviderFactory() {
    return metaBean().liveMarketDataProviderFactory().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets maximum number of entries to store in the cache.
   * @return the value of the property
   */
  public long getMaxCacheEntries() {
    return _maxCacheEntries;
  }

  /**
   * Sets maximum number of entries to store in the cache.
   * @param maxCacheEntries  the new value of the property
   */
  public void setMaxCacheEntries(long maxCacheEntries) {
    this._maxCacheEntries = maxCacheEntries;
  }

  /**
   * Gets the the {@code maxCacheEntries} property.
   * @return the property, not null
   */
  public final Property<Long> maxCacheEntries() {
    return metaBean().maxCacheEntries().createProperty(this);
  }

  //-----------------------------------------------------------------------
  @Override
  public ViewFactoryComponentFactory clone() {
    return JodaBeanUtils.cloneAlways(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ViewFactoryComponentFactory other = (ViewFactoryComponentFactory) obj;
      return JodaBeanUtils.equal(getClassifier(), other.getClassifier()) &&
          JodaBeanUtils.equal(getLiveMarketDataProviderFactory(), other.getLiveMarketDataProviderFactory()) &&
          (getMaxCacheEntries() == other.getMaxCacheEntries()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getClassifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(getLiveMarketDataProviderFactory());
    hash += hash * 31 + JodaBeanUtils.hashCode(getMaxCacheEntries());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ViewFactoryComponentFactory{");
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
    buf.append("liveMarketDataProviderFactory").append('=').append(JodaBeanUtils.toString(getLiveMarketDataProviderFactory())).append(',').append(' ');
    buf.append("maxCacheEntries").append('=').append(JodaBeanUtils.toString(getMaxCacheEntries())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ViewFactoryComponentFactory}.
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
        this, "classifier", ViewFactoryComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code liveMarketDataProviderFactory} property.
     */
    private final MetaProperty<LiveMarketDataProviderFactory> _liveMarketDataProviderFactory = DirectMetaProperty.ofReadWrite(
        this, "liveMarketDataProviderFactory", ViewFactoryComponentFactory.class, LiveMarketDataProviderFactory.class);
    /**
     * The meta-property for the {@code maxCacheEntries} property.
     */
    private final MetaProperty<Long> _maxCacheEntries = DirectMetaProperty.ofReadWrite(
        this, "maxCacheEntries", ViewFactoryComponentFactory.class, Long.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "classifier",
        "liveMarketDataProviderFactory",
        "maxCacheEntries");

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
        case -301472921:  // liveMarketDataProviderFactory
          return _liveMarketDataProviderFactory;
        case -949200334:  // maxCacheEntries
          return _maxCacheEntries;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ViewFactoryComponentFactory> builder() {
      return new DirectBeanBuilder<ViewFactoryComponentFactory>(new ViewFactoryComponentFactory());
    }

    @Override
    public Class<? extends ViewFactoryComponentFactory> beanType() {
      return ViewFactoryComponentFactory.class;
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
     * The meta-property for the {@code liveMarketDataProviderFactory} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LiveMarketDataProviderFactory> liveMarketDataProviderFactory() {
      return _liveMarketDataProviderFactory;
    }

    /**
     * The meta-property for the {@code maxCacheEntries} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> maxCacheEntries() {
      return _maxCacheEntries;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          return ((ViewFactoryComponentFactory) bean).getClassifier();
        case -301472921:  // liveMarketDataProviderFactory
          return ((ViewFactoryComponentFactory) bean).getLiveMarketDataProviderFactory();
        case -949200334:  // maxCacheEntries
          return ((ViewFactoryComponentFactory) bean).getMaxCacheEntries();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          ((ViewFactoryComponentFactory) bean).setClassifier((String) newValue);
          return;
        case -301472921:  // liveMarketDataProviderFactory
          ((ViewFactoryComponentFactory) bean).setLiveMarketDataProviderFactory((LiveMarketDataProviderFactory) newValue);
          return;
        case -949200334:  // maxCacheEntries
          ((ViewFactoryComponentFactory) bean).setMaxCacheEntries((Long) newValue);
          return;
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

    @Override
    protected void validate(Bean bean) {
      JodaBeanUtils.notNull(((ViewFactoryComponentFactory) bean)._classifier, "classifier");
      super.validate(bean);
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
