/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;

/**
 * Market data mappings specify which market data from the global set of data should be used for a particular
 * calculation.
 * <p>
 * For example, the global set of market data might contain curves from several curve groups but a
 * calculation needs to request (for example) the USD discounting curve without knowing or caring which
 * group contains it.
 * <p>
 * This class provides the mapping from a general piece of data (the USD discounting
 * curve) to a specific piece of data (the USD discounting curve from the curve group named 'XYZ').
 */
@SuppressWarnings("unchecked")
@BeanDefinition
public final class DefaultMarketDataMappings implements MarketDataMappings, ImmutableBean, Serializable {

  /** An empty set of market data mappings containing no mappers. */
  static final MarketDataMappings EMPTY = new DefaultMarketDataMappings(MarketDataFeed.NONE, ImmutableMap.of());

  /** Market data feed system that is the source of observable market data, for example Bloomberg or Reuters. */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataFeed marketDataFeed;

  /**
   * Mappings that translate data requests from calculators into requests that can be used to look
   * up the data in the global set of market data. They are keyed by the type of the market data
   * ID they can handle.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings;

  /**
   * Returns a set of market data mappings with the specified source of observable data and made up
   * of the specified individual mappings.
   *
   * @param marketDataFeed  the feed that is the source of the market data, for example Bloomberg or Reuters
   * @param mappings  mappings for converting market data requests from calculations into requests that
   *   can be used to query the global set of market data
   * @return a set of mappings containing the specified feed and mapping instances
   */
  public static MarketDataMappings of(MarketDataFeed marketDataFeed, List<? extends MarketDataMapping<?, ?>> mappings) {
    ImmutableMap.Builder<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> builder = ImmutableMap.builder();

    for (MarketDataMapping<?, ?> mapping : mappings) {
      Class<? extends MarketDataKey<?>> keyType = mapping.getMarketDataKeyType();
      builder.put(keyType, mapping);
    }
    return new DefaultMarketDataMappings(marketDataFeed, builder.build());
  }

  /**
   * Returns a set of market data mappings with the specified source of observable data and made up
   * of the specified individual mappings.
   *
   * @param marketDataFeed  the feed that is the source of the market data, for example Bloomberg or Reuters
   * @param mappings  mappings for converting market data requests from calculations into requests that
   *   can be used to query the global set of market data
   * @return a set of mappings containing the specified feed and mapping instances
   */
  public static MarketDataMappings of(MarketDataFeed marketDataFeed, MarketDataMapping<?, ?>... mappings) {
    return of(marketDataFeed, ImmutableList.copyOf(mappings));
  }

  @Override
  @SuppressWarnings("rawtypes")
  public <T, K extends MarketDataKey<T>> MarketDataId<T> getIdForKey(K key) {
    if (key instanceof ObservableKey) {
      return (MarketDataId<T>) getIdForObservableKey((ObservableKey) key);
    }
    if (key instanceof SimpleMarketDataKey) {
      return ((SimpleMarketDataKey) key).toMarketDataId(marketDataFeed);
    }
    MarketDataMapping<T, K> mapping =
        (MarketDataMapping<T, K>) mappings.getOrDefault(key.getClass(), MissingMapping.INSTANCE);
    return mapping.getIdForKey(key);
  }

  @Override
  public ObservableId getIdForObservableKey(ObservableKey key) {
    return key.toMarketDataId(marketDataFeed);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultMarketDataMappings}.
   * @return the meta-bean, not null
   */
  public static DefaultMarketDataMappings.Meta meta() {
    return DefaultMarketDataMappings.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DefaultMarketDataMappings.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DefaultMarketDataMappings.Builder builder() {
    return new DefaultMarketDataMappings.Builder();
  }

  private DefaultMarketDataMappings(
      MarketDataFeed marketDataFeed,
      Map<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings) {
    JodaBeanUtils.notNull(marketDataFeed, "marketDataFeed");
    JodaBeanUtils.notNull(mappings, "mappings");
    this.marketDataFeed = marketDataFeed;
    this.mappings = ImmutableMap.copyOf(mappings);
  }

  @Override
  public DefaultMarketDataMappings.Meta metaBean() {
    return DefaultMarketDataMappings.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets market data feed system that is the source of observable market data, for example Bloomberg or Reuters.
   * @return the value of the property, not null
   */
  public MarketDataFeed getMarketDataFeed() {
    return marketDataFeed;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets mappings that translate data requests from calculators into requests that can be used to look
   * up the data in the global set of market data. They are keyed by the type of the market data
   * ID they can handle.
   * @return the value of the property, not null
   */
  public ImmutableMap<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> getMappings() {
    return mappings;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultMarketDataMappings other = (DefaultMarketDataMappings) obj;
      return JodaBeanUtils.equal(marketDataFeed, other.marketDataFeed) &&
          JodaBeanUtils.equal(mappings, other.mappings);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataFeed);
    hash = hash * 31 + JodaBeanUtils.hashCode(mappings);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("DefaultMarketDataMappings{");
    buf.append("marketDataFeed").append('=').append(marketDataFeed).append(',').append(' ');
    buf.append("mappings").append('=').append(JodaBeanUtils.toString(mappings));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DefaultMarketDataMappings}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code marketDataFeed} property.
     */
    private final MetaProperty<MarketDataFeed> marketDataFeed = DirectMetaProperty.ofImmutable(
        this, "marketDataFeed", DefaultMarketDataMappings.class, MarketDataFeed.class);
    /**
     * The meta-property for the {@code mappings} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>>> mappings = DirectMetaProperty.ofImmutable(
        this, "mappings", DefaultMarketDataMappings.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "marketDataFeed",
        "mappings");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 842621124:  // marketDataFeed
          return marketDataFeed;
        case 194445669:  // mappings
          return mappings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public DefaultMarketDataMappings.Builder builder() {
      return new DefaultMarketDataMappings.Builder();
    }

    @Override
    public Class<? extends DefaultMarketDataMappings> beanType() {
      return DefaultMarketDataMappings.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code marketDataFeed} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataFeed> marketDataFeed() {
      return marketDataFeed;
    }

    /**
     * The meta-property for the {@code mappings} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>>> mappings() {
      return mappings;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 842621124:  // marketDataFeed
          return ((DefaultMarketDataMappings) bean).getMarketDataFeed();
        case 194445669:  // mappings
          return ((DefaultMarketDataMappings) bean).getMappings();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code DefaultMarketDataMappings}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<DefaultMarketDataMappings> {

    private MarketDataFeed marketDataFeed;
    private Map<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(DefaultMarketDataMappings beanToCopy) {
      this.marketDataFeed = beanToCopy.getMarketDataFeed();
      this.mappings = beanToCopy.getMappings();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 842621124:  // marketDataFeed
          return marketDataFeed;
        case 194445669:  // mappings
          return mappings;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 842621124:  // marketDataFeed
          this.marketDataFeed = (MarketDataFeed) newValue;
          break;
        case 194445669:  // mappings
          this.mappings = (Map<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public DefaultMarketDataMappings build() {
      return new DefaultMarketDataMappings(
          marketDataFeed,
          mappings);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets market data feed system that is the source of observable market data, for example Bloomberg or Reuters.
     * @param marketDataFeed  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder marketDataFeed(MarketDataFeed marketDataFeed) {
      JodaBeanUtils.notNull(marketDataFeed, "marketDataFeed");
      this.marketDataFeed = marketDataFeed;
      return this;
    }

    /**
     * Sets mappings that translate data requests from calculators into requests that can be used to look
     * up the data in the global set of market data. They are keyed by the type of the market data
     * ID they can handle.
     * @param mappings  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder mappings(Map<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings) {
      JodaBeanUtils.notNull(mappings, "mappings");
      this.mappings = mappings;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("DefaultMarketDataMappings.Builder{");
      buf.append("marketDataFeed").append('=').append(JodaBeanUtils.toString(marketDataFeed)).append(',').append(' ');
      buf.append("mappings").append('=').append(JodaBeanUtils.toString(mappings));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
