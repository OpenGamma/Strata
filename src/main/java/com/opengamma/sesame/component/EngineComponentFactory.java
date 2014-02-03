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

import org.apache.commons.lang.StringUtils;
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

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.component.ComponentInfo;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.AbstractComponentFactory;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.id.VersionCorrection;
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
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.Engine;
import com.opengamma.sesame.engine.EngineService;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.fxforward.DiscountingFXForwardPVFn;
import com.opengamma.sesame.fxforward.FXForwardDiscountingCalculatorFn;
import com.opengamma.sesame.fxforward.FXForwardPVFn;
import com.opengamma.util.ArgumentChecker;

import net.sf.ehcache.CacheManager;

/**
 * Component factory for the engine.
 */
@BeanDefinition
public class EngineComponentFactory extends AbstractComponentFactory {

  /**
   * The classifier that the factory should publish under.
   */
  @PropertyDefinition(validate = "notNull")
  private String _classifier;

  /**
   * The cache manager.
   */
  @PropertyDefinition(validate = "notNull")
  private CacheManager _cacheManager;

  @Override
  public void init(ComponentRepository repo, LinkedHashMap<String, String> configuration) throws Exception {
    // TODO allow the thread pool to grow to allow for threads that block waiting for a cache value to be calculated?
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    Map<Class<?>, Object> components = getComponents(repo, configuration);
    ComponentMap componentMap = ComponentMap.of(components);

    // Indicate remaining configuration has been used
    configuration.clear();

    initServiceContext(components);

    AvailableOutputs availableOutputs = initAvailableOutputs();
    AvailableImplementations availableImplementations = initAvailableImplementations();

    Engine engine = new Engine(executor,
                               componentMap,
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               getCacheManager(),
                               //EnumSet.of(EngineService.CACHING, EngineService.TIMING));
                               EngineService.DEFAULT_SERVICES);
    ComponentInfo engineInfo = new ComponentInfo(Engine.class, getClassifier());
    repo.registerComponent(engineInfo, engine);

    ComponentInfo outputsInfo = new ComponentInfo(AvailableOutputs.class, getClassifier());
    repo.registerComponent(outputsInfo, availableOutputs);

    ComponentInfo implsInfo = new ComponentInfo(AvailableImplementations.class, getClassifier());
    repo.registerComponent(implsInfo, availableImplementations);
  }

  private void initServiceContext(Map<Class<?>, Object> components) {

    VersionCorrectionProvider vcProvider = new EngineVersionCorrectionProvider(Instant.now());
    final ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
  }

  private Map<Class<?>, Object> getComponents(ComponentRepository repo, LinkedHashMap<String, String> configuration) {
    Map<Class<?>, Object> components = new HashMap<>();
    for (Map.Entry<String, String> entry : configuration.entrySet()) {
      String key = entry.getKey();
      String valueStr = entry.getValue();
      if (!valueStr.contains("::")) {
        throw new OpenGammaRuntimeException("Property " + key + " does not reference a component: " + valueStr);
      }
      final String type = StringUtils.substringBefore(valueStr, "::");
      final String classifier = StringUtils.substringAfter(valueStr, "::");
      final ComponentInfo info = repo.findInfo(type, classifier);
      if (info == null) {
        throw new IllegalArgumentException("Component not found: " + valueStr);
      }
      final Object instance = repo.getInstance(info);
      components.put(info.getType(), instance);
    }
    return components;
  }

  protected AvailableOutputs initAvailableOutputs() {
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(FXForwardPVFn.class);
    return availableOutputs;
  }

  protected AvailableImplementations initAvailableImplementations() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DiscountingFXForwardPVFn.class,
                                      DefaultCurrencyPairsFn.class,
                                      FXForwardSecurityConverter.class,
                                      ConfigDBInstrumentExposuresProvider.class,
                                      DefaultCurveSpecificationMarketDataFn.class,
                                      DefaultFXMatrixFn.class,
                                      DefaultCurveDefinitionFn.class,
                                      DefaultDiscountingMulticurveBundleFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      ConfigDBCurveConstructionConfigurationSource.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      FXForwardDiscountingCalculatorFn.class,
                                      ConfigDbMarketExposureSelectorFn.class);
    return availableImplementations;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code EngineComponentFactory}.
   * @return the meta-bean, not null
   */
  public static EngineComponentFactory.Meta meta() {
    return EngineComponentFactory.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(EngineComponentFactory.Meta.INSTANCE);
  }

  @Override
  public EngineComponentFactory.Meta metaBean() {
    return EngineComponentFactory.Meta.INSTANCE;
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
   * Gets the cache manager.
   * @return the value of the property, not null
   */
  public CacheManager getCacheManager() {
    return _cacheManager;
  }

  /**
   * Sets the cache manager.
   * @param cacheManager  the new value of the property, not null
   */
  public void setCacheManager(CacheManager cacheManager) {
    JodaBeanUtils.notNull(cacheManager, "cacheManager");
    this._cacheManager = cacheManager;
  }

  /**
   * Gets the the {@code cacheManager} property.
   * @return the property, not null
   */
  public final Property<CacheManager> cacheManager() {
    return metaBean().cacheManager().createProperty(this);
  }

  //-----------------------------------------------------------------------
  @Override
  public EngineComponentFactory clone() {
    return (EngineComponentFactory) super.clone();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      EngineComponentFactory other = (EngineComponentFactory) obj;
      return JodaBeanUtils.equal(getClassifier(), other.getClassifier()) &&
          JodaBeanUtils.equal(getCacheManager(), other.getCacheManager()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getClassifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCacheManager());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("EngineComponentFactory{");
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
    buf.append("cacheManager").append('=').append(JodaBeanUtils.toString(getCacheManager())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code EngineComponentFactory}.
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
        this, "classifier", EngineComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code cacheManager} property.
     */
    private final MetaProperty<CacheManager> _cacheManager = DirectMetaProperty.ofReadWrite(
        this, "cacheManager", EngineComponentFactory.class, CacheManager.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "classifier",
        "cacheManager");

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
        case -1452875317:  // cacheManager
          return _cacheManager;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends EngineComponentFactory> builder() {
      return new DirectBeanBuilder<EngineComponentFactory>(new EngineComponentFactory());
    }

    @Override
    public Class<? extends EngineComponentFactory> beanType() {
      return EngineComponentFactory.class;
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
     * The meta-property for the {@code cacheManager} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CacheManager> cacheManager() {
      return _cacheManager;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          return ((EngineComponentFactory) bean).getClassifier();
        case -1452875317:  // cacheManager
          return ((EngineComponentFactory) bean).getCacheManager();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          ((EngineComponentFactory) bean).setClassifier((String) newValue);
          return;
        case -1452875317:  // cacheManager
          ((EngineComponentFactory) bean).setCacheManager((CacheManager) newValue);
          return;
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

    @Override
    protected void validate(Bean bean) {
      JodaBeanUtils.notNull(((EngineComponentFactory) bean)._classifier, "classifier");
      JodaBeanUtils.notNull(((EngineComponentFactory) bean)._cacheManager, "cacheManager");
      super.validate(bean);
    }

  }

  private static class EngineVersionCorrectionProvider implements VersionCorrectionProvider {

    private final Instant _versionAsOf;

    public EngineVersionCorrectionProvider(Instant versionAsOf) {
      _versionAsOf = ArgumentChecker.notNull(versionAsOf, "versionAsOf");
    }

    @Override
    public VersionCorrection getPortfolioVersionCorrection() {
      // todo - this needs to be integrated with the new engine caching, atm this will not respond to portfolio updates
      return VersionCorrection.ofVersionAsOf(_versionAsOf);
    }

    @Override
    public VersionCorrection getConfigVersionCorrection() {
      // todo - this needs to be integrated with the new engine caching, atm this will not respond to config updates
      return VersionCorrection.ofVersionAsOf(_versionAsOf);
    }
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
