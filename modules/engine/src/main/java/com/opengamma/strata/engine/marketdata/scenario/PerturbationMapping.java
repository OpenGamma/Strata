/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.scenario;


import static com.opengamma.strata.collect.Guavate.toImmutableList;

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
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.Perturbation;
import com.opengamma.strata.collect.Messages;

/**
 * Contains one or more market data perturbations and a filter that decides what market data they apply to.
 *
 * @param <T>  the type of the market data handled by the mapping
 */
@BeanDefinition
public final class PerturbationMapping<T> implements ImmutableBean {

  /** The type of market data handled by this mapping. */
  @PropertyDefinition(validate = "notNull")
  private final Class<T> marketDataType;

  /** The filter that decides whether the perturbations should be applied to a piece of market data. */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataFilter<T, ?> filter;

  /** Perturbations that should be applied to market data over multiple calculation cycles as part of a scenario. */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<Perturbation<T>> perturbations;

  /**
   * Returns a mapping containing a single perturbation.
   *
   * @param marketDataType the type of market data handled by the mapping
   * @param filter  the filter used to choose the market data
   * @param perturbation  the perturbation applied to any market data matching the filter
   * @return a mapping containing a single perturbation
   */
  public static <T> PerturbationMapping<T> of(
      Class<T> marketDataType,
      MarketDataFilter<T, ?> filter,
      Perturbation<T> perturbation) {

    return new PerturbationMapping<>(marketDataType, filter, ImmutableList.of(perturbation));
  }

  /**
   * Returns a mapping containing multiple perturbations.
   *
   * @param marketDataType the type of market data handled by the mapping
   * @param filter  the filter used to choose the market data
   * @param perturbations  the perturbations applied to any market data matching the filter
   * @return a mapping containing multiple perturbations
   */
  @SafeVarargs
  public static <T> PerturbationMapping<T> of(
      Class<T> marketDataType,
      MarketDataFilter<T, ?> filter,
      Perturbation<T>... perturbations) {

    return new PerturbationMapping<>(marketDataType, filter, ImmutableList.copyOf(perturbations));
  }

  /**
   * Returns a mapping containing multiple perturbations.
   *
   * @param marketDataType the type of market data handled by the mapping
   * @param filter  the filter used to choose the market data
   * @param perturbations  the perturbations applied to any market data matching the filter
   * @return a mapping containing multiple perturbations
   */
  public static <T> PerturbationMapping<T> of(
      Class<T> marketDataType,
      MarketDataFilter<T, ?> filter,
      List<Perturbation<T>> perturbations) {

    return new PerturbationMapping<>(marketDataType, filter, ImmutableList.copyOf(perturbations));
  }

  /**
   * Returns true if the filter matches the market data ID and value.
   *
   * @param marketDataId  the ID of a piece of market data
   * @param marketData  the market data value
   * @return true if the filter matches
   */
  @SuppressWarnings("unchecked")
  public boolean matches(MarketDataId<?> marketDataId, Object marketData) {
    // The raw type is necessary to keep the compiler happy, the call is definitely safe because the
    // type of the ID is checked against the ID type handled by the filter
    @SuppressWarnings("rawtypes")
    MarketDataFilter rawFilter = filter;

    return marketDataType.isInstance(marketData) &&
        filter.getMarketDataIdType().isInstance(marketDataId) &&
        rawFilter.matches(marketDataId, marketData);
  }

  /**
   * Applies the perturbations in this mapping to an item of market data and returns the results.
   * <p>
   * This method should only be called after calling {@code #matches} and receiving a result of {@code true}.
   *
   * @param marketData  the market data value
   * @return a list of market data values derived from the input value by applying the perturbations
   */
  @SuppressWarnings("unchecked")
  public List<T> applyPerturbations(T marketData) {
    // Check that T and U are the same type
    if (!marketDataType.isInstance(marketData)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data {} is not an instance of the required type {}",
              marketData,
              marketDataType.getName()));
    }
    // T and U are the same type so the casts are safe
    return perturbations.stream()
        .map(perturbation -> perturbation.applyTo(marketData))
        .collect(toImmutableList());
  }

  /**
   * Returns the number of perturbations in this mapping.
   *
   * @return the number of perturbations in this mapping
   */
  public int getPerturbationCount() {
    return perturbations.size();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PerturbationMapping}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static PerturbationMapping.Meta meta() {
    return PerturbationMapping.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code PerturbationMapping}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R> PerturbationMapping.Meta<R> metaPerturbationMapping(Class<R> cls) {
    return PerturbationMapping.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PerturbationMapping.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @param <T>  the type
   * @return the builder, not null
   */
  public static <T> PerturbationMapping.Builder<T> builder() {
    return new PerturbationMapping.Builder<T>();
  }

  private PerturbationMapping(
      Class<T> marketDataType,
      MarketDataFilter<T, ?> filter,
      List<Perturbation<T>> perturbations) {
    JodaBeanUtils.notNull(marketDataType, "marketDataType");
    JodaBeanUtils.notNull(filter, "filter");
    JodaBeanUtils.notEmpty(perturbations, "perturbations");
    this.marketDataType = marketDataType;
    this.filter = filter;
    this.perturbations = ImmutableList.copyOf(perturbations);
  }

  @SuppressWarnings("unchecked")
  @Override
  public PerturbationMapping.Meta<T> metaBean() {
    return PerturbationMapping.Meta.INSTANCE;
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
   * Gets the type of market data handled by this mapping.
   * @return the value of the property, not null
   */
  public Class<T> getMarketDataType() {
    return marketDataType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the filter that decides whether the perturbations should be applied to a piece of market data.
   * @return the value of the property, not null
   */
  public MarketDataFilter<T, ?> getFilter() {
    return filter;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets perturbations that should be applied to market data over multiple calculation cycles as part of a scenario.
   * @return the value of the property, not empty
   */
  public ImmutableList<Perturbation<T>> getPerturbations() {
    return perturbations;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder<T> toBuilder() {
    return new Builder<T>(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PerturbationMapping<?> other = (PerturbationMapping<?>) obj;
      return JodaBeanUtils.equal(getMarketDataType(), other.getMarketDataType()) &&
          JodaBeanUtils.equal(getFilter(), other.getFilter()) &&
          JodaBeanUtils.equal(getPerturbations(), other.getPerturbations());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getMarketDataType());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFilter());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPerturbations());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("PerturbationMapping{");
    buf.append("marketDataType").append('=').append(getMarketDataType()).append(',').append(' ');
    buf.append("filter").append('=').append(getFilter()).append(',').append(' ');
    buf.append("perturbations").append('=').append(JodaBeanUtils.toString(getPerturbations()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PerturbationMapping}.
   * @param <T>  the type
   */
  public static final class Meta<T> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code marketDataType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<T>> marketDataType = DirectMetaProperty.ofImmutable(
        this, "marketDataType", PerturbationMapping.class, (Class) Class.class);
    /**
     * The meta-property for the {@code filter} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<MarketDataFilter<T, ?>> filter = DirectMetaProperty.ofImmutable(
        this, "filter", PerturbationMapping.class, (Class) MarketDataFilter.class);
    /**
     * The meta-property for the {@code perturbations} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<Perturbation<T>>> perturbations = DirectMetaProperty.ofImmutable(
        this, "perturbations", PerturbationMapping.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "marketDataType",
        "filter",
        "perturbations");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 843057760:  // marketDataType
          return marketDataType;
        case -1274492040:  // filter
          return filter;
        case 1397849260:  // perturbations
          return perturbations;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PerturbationMapping.Builder<T> builder() {
      return new PerturbationMapping.Builder<T>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends PerturbationMapping<T>> beanType() {
      return (Class) PerturbationMapping.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code marketDataType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<T>> marketDataType() {
      return marketDataType;
    }

    /**
     * The meta-property for the {@code filter} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataFilter<T, ?>> filter() {
      return filter;
    }

    /**
     * The meta-property for the {@code perturbations} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<Perturbation<T>>> perturbations() {
      return perturbations;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 843057760:  // marketDataType
          return ((PerturbationMapping<?>) bean).getMarketDataType();
        case -1274492040:  // filter
          return ((PerturbationMapping<?>) bean).getFilter();
        case 1397849260:  // perturbations
          return ((PerturbationMapping<?>) bean).getPerturbations();
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
   * The bean-builder for {@code PerturbationMapping}.
   * @param <T>  the type
   */
  public static final class Builder<T> extends DirectFieldsBeanBuilder<PerturbationMapping<T>> {

    private Class<T> marketDataType;
    private MarketDataFilter<T, ?> filter;
    private List<Perturbation<T>> perturbations = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PerturbationMapping<T> beanToCopy) {
      this.marketDataType = beanToCopy.getMarketDataType();
      this.filter = beanToCopy.getFilter();
      this.perturbations = beanToCopy.getPerturbations();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 843057760:  // marketDataType
          return marketDataType;
        case -1274492040:  // filter
          return filter;
        case 1397849260:  // perturbations
          return perturbations;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<T> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 843057760:  // marketDataType
          this.marketDataType = (Class<T>) newValue;
          break;
        case -1274492040:  // filter
          this.filter = (MarketDataFilter<T, ?>) newValue;
          break;
        case 1397849260:  // perturbations
          this.perturbations = (List<Perturbation<T>>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder<T> set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder<T> setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder<T> setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder<T> setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public PerturbationMapping<T> build() {
      return new PerturbationMapping<T>(
          marketDataType,
          filter,
          perturbations);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the type of market data handled by this mapping.
     * @param marketDataType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder<T> marketDataType(Class<T> marketDataType) {
      JodaBeanUtils.notNull(marketDataType, "marketDataType");
      this.marketDataType = marketDataType;
      return this;
    }

    /**
     * Sets the filter that decides whether the perturbations should be applied to a piece of market data.
     * @param filter  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder<T> filter(MarketDataFilter<T, ?> filter) {
      JodaBeanUtils.notNull(filter, "filter");
      this.filter = filter;
      return this;
    }

    /**
     * Sets perturbations that should be applied to market data over multiple calculation cycles as part of a scenario.
     * @param perturbations  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder<T> perturbations(List<Perturbation<T>> perturbations) {
      JodaBeanUtils.notEmpty(perturbations, "perturbations");
      this.perturbations = perturbations;
      return this;
    }

    /**
     * Sets the {@code perturbations} property in the builder
     * from an array of objects.
     * @param perturbations  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder<T> perturbations(Perturbation<T>... perturbations) {
      return perturbations(ImmutableList.copyOf(perturbations));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("PerturbationMapping.Builder{");
      buf.append("marketDataType").append('=').append(JodaBeanUtils.toString(marketDataType)).append(',').append(' ');
      buf.append("filter").append('=').append(JodaBeanUtils.toString(filter)).append(',').append(' ');
      buf.append("perturbations").append('=').append(JodaBeanUtils.toString(perturbations));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
