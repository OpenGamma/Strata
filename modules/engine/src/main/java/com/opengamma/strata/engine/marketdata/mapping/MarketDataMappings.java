/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.MarketDataVendor;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.MarketDataKey;
import com.opengamma.strata.marketdata.key.ObservableKey;

// TODO Should this return a result?
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
@BeanDefinition(builderScope = "private")
public final class MarketDataMappings implements ImmutableBean {

  public static final MarketDataMappings EMPTY =
      MarketDataMappings.of(
          ObservablesMapping.of(MarketDataVendor.NONE),
          ImmutableList.of());

  /** Mapping used for IDs that don't have a mapping registered. It always returns failure results. */
  private static final MarketDataMapping<?, ?> NO_MAPPING = new FailureMapping();

  /** Mapping for observable market data. */
  @PropertyDefinition(validate = "notNull")
  private final ObservablesMapping observablesMapping;

  /**
   * Mappings that translate data requests from calculators into requests that can be used to look
   * up the data in the global set of market data. They are keyed by the type of the market data
   * ID they can handle.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings;
  
  /**
   * Returns a mutable builder for building instances of {@link MarketDataMappings}.
   *
   * @return a mutable builder for building instances of {@link MarketDataMappings}
   */
  public static MarketDataMappingsBuilder builder() {
    return new MarketDataMappingsBuilder();
  }

  /**
   * @param mappings  mappings for converting market data requests from calculations into requests that
   *   can be used to query the global set of market data
   */
  public static MarketDataMappings of(
      ObservablesMapping observablesMapping,
      List<? extends MarketDataMapping<?, ?>> mappings) {

    ImmutableMap.Builder<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> builder = ImmutableMap.builder();

    for (MarketDataMapping<?, ?> mapping : mappings) {
      Class<? extends MarketDataKey<?>> keyType = mapping.getMarketDataKeyType();
      builder.put(keyType, mapping);
    }
    return new MarketDataMappings(observablesMapping, builder.build());
  }

  /**
   * Returns a market data ID which uniquely identifies the piece of market data referred to by the key.
   * <p>
   * The key might identify an item of data of which there are many copies in the system, for example
   * the USD discounting curve. The ID identifies a globally unique copy, for example the USD discounting
   * curve from a named curve group.
   *
   * @param key  a key identifying an item of market data
   * @param <K>  the type of the market data key accepted by this method
   * @return an ID uniquely identifying an item of market data
   */
  public <T, K extends MarketDataKey<T>> MarketDataId<T> getIdForKey(K key) {
    MarketDataMapping<T, K> mapping = (MarketDataMapping<T, K>) mappings.getOrDefault(key.getClass(), NO_MAPPING);
    return mapping.getIdForKey(key);
  }

  /**
   * Gets the market data ID for an item of observable market data given its key.
   *
   * @param key  a market data key identifying an item of observable market data
   * @return a market data ID that uniquely identifies the data and its source
   */
  public ObservableId getIdForObservableKey(ObservableKey key) {
    return observablesMapping.getIdForKey(key);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MarketDataMappings}.
   * @return the meta-bean, not null
   */
  public static MarketDataMappings.Meta meta() {
    return MarketDataMappings.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MarketDataMappings.Meta.INSTANCE);
  }

  private MarketDataMappings(
      ObservablesMapping observablesMapping,
      Map<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings) {
    JodaBeanUtils.notNull(observablesMapping, "observablesMapping");
    JodaBeanUtils.notNull(mappings, "mappings");
    this.observablesMapping = observablesMapping;
    this.mappings = ImmutableMap.copyOf(mappings);
  }

  @Override
  public MarketDataMappings.Meta metaBean() {
    return MarketDataMappings.Meta.INSTANCE;
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
   * Gets mapping for observable market data.
   * @return the value of the property, not null
   */
  public ObservablesMapping getObservablesMapping() {
    return observablesMapping;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MarketDataMappings other = (MarketDataMappings) obj;
      return JodaBeanUtils.equal(getObservablesMapping(), other.getObservablesMapping()) &&
          JodaBeanUtils.equal(getMappings(), other.getMappings());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getObservablesMapping());
    hash = hash * 31 + JodaBeanUtils.hashCode(getMappings());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("MarketDataMappings{");
    buf.append("observablesMapping").append('=').append(getObservablesMapping()).append(',').append(' ');
    buf.append("mappings").append('=').append(JodaBeanUtils.toString(getMappings()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MarketDataMappings}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code observablesMapping} property.
     */
    private final MetaProperty<ObservablesMapping> observablesMapping = DirectMetaProperty.ofImmutable(
        this, "observablesMapping", MarketDataMappings.class, ObservablesMapping.class);
    /**
     * The meta-property for the {@code mappings} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>>> mappings = DirectMetaProperty.ofImmutable(
        this, "mappings", MarketDataMappings.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observablesMapping",
        "mappings");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 548133662:  // observablesMapping
          return observablesMapping;
        case 194445669:  // mappings
          return mappings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends MarketDataMappings> builder() {
      return new MarketDataMappings.Builder();
    }

    @Override
    public Class<? extends MarketDataMappings> beanType() {
      return MarketDataMappings.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code observablesMapping} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservablesMapping> observablesMapping() {
      return observablesMapping;
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
        case 548133662:  // observablesMapping
          return ((MarketDataMappings) bean).getObservablesMapping();
        case 194445669:  // mappings
          return ((MarketDataMappings) bean).getMappings();
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
   * The bean-builder for {@code MarketDataMappings}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<MarketDataMappings> {

    private ObservablesMapping observablesMapping;
    private Map<Class<? extends MarketDataKey<?>>, MarketDataMapping<?, ?>> mappings = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 548133662:  // observablesMapping
          return observablesMapping;
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
        case 548133662:  // observablesMapping
          this.observablesMapping = (ObservablesMapping) newValue;
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
    public MarketDataMappings build() {
      return new MarketDataMappings(
          observablesMapping,
          mappings);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("MarketDataMappings.Builder{");
      buf.append("observablesMapping").append('=').append(JodaBeanUtils.toString(observablesMapping)).append(',').append(' ');
      buf.append("mappings").append('=').append(JodaBeanUtils.toString(mappings));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
