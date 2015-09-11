/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
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

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculation.MissingMappingId;
import com.opengamma.strata.engine.calculation.NoMatchingRuleId;

/**
 * A set of market data.
 * <p>
 * A market environment contains the basic market data values that are of interest to users. For example, a market
 * environment might contain market quotes, calibrated curves and volatility surfaces.
 * <p>
 * It is anticipated that {@link MarketEnvironment} will be exposed directly to users.
 * <p>
 * The market data used in calculations is provided by {@link CalculationEnvironment} or
 * {@link ScenarioCalculationEnvironment}. These contains the same data as {@link MarketEnvironment} plus
 * additional derived values used by the calculations and scenario framework.
 * <p>
 * {@link CalculationEnvironment} and {@link ScenarioCalculationEnvironment} can be built from a
 * {@link MarketEnvironment} using a {@link MarketDataFactory}.
 *
 * @see MarketDataFactory
 * @see CalculationEnvironment
 * @see ScenarioCalculationEnvironment
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class MarketEnvironment implements ImmutableBean, MarketDataLookup {

  /** The valuation date associated with the data. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;

  // TODO Should there be separate maps for observable and non-observable data?
  // TODO Do the values need to include the timestamp as well as the market data item?
  /** The individual items of market data, keyed by ID. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Object>")
  private final ImmutableMap<MarketDataId<?>, Object> values;

  // TODO Do the values need to include the timestamp as well as the time series?
  /** The time series of market data values, keyed by ID. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends ObservableId, LocalDateDoubleTimeSeries>")
  private final ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  /**
   * Returns an empty mutable builder for building a new instance of {@code MarketEnvironment}.
   *
   * @param valuationDate  the valuation date
   * @return an empty mutable builder for building a new instance of {@code MarketEnvironment}
   */
  public static MarketEnvironmentBuilder builder(LocalDate valuationDate) {
    return new MarketEnvironmentBuilder(valuationDate);
  }

  /**
   * Returns an empty set of market data with the specified valuation date.
   *
   * @param valuationDate  the valuation date used for the calculations
   * @return an empty set of market data with the specified valuation date
   */
  public static MarketEnvironment empty(LocalDate valuationDate) {
    return MarketEnvironment.builder(valuationDate).build();
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    Object value = values.get(id);
    return value != null && id.getMarketDataType().isInstance(value);
  }

  @Override
  public <T> T getValue(MarketDataId<T> id) {
    // Special handling of these special ID types to provide more helpful error messages
    if (id instanceof NoMatchingRuleId) {
      MarketDataKey<?> key = ((NoMatchingRuleId) id).getKey();
      throw new IllegalArgumentException("No market data rules were available to build the market data for " + key);
    }
    if (id instanceof MissingMappingId) {
      MarketDataKey<?> key = ((MissingMappingId) id).getKey();
      throw new IllegalArgumentException("No market data mapping found for " + key);
    }
    Object value = values.get(id);

    if (value == null) {
      throw new IllegalArgumentException("No market data value available for " + id);
    }
    if (!id.getMarketDataType().isInstance(value)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data value for ID {} is not of the expected type. Expected type: {}, actual type: {}, value: {}",
              id,
              id.getMarketDataType().getName(),
              value.getClass().getName(),
              value));
    }
    return id.getMarketDataType().cast(value);
  }

  @Override
  public boolean containsTimeSeries(ObservableId id) {
    return timeSeries.containsKey(id);
  }


  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries timeSeries = this.timeSeries.get(id);

    if (timeSeries == null) {
      throw new IllegalArgumentException("No time series available for ID " + id);
    }
    return timeSeries;
  }

  /**
   * Returns a mutable builder containing the data from this object.
   *
   * @return a mutable builder containing the data from this object
   */
  public MarketEnvironmentBuilder toBuilder() {
    return new MarketEnvironmentBuilder(valuationDate, values, timeSeries);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MarketEnvironment}.
   * @return the meta-bean, not null
   */
  public static MarketEnvironment.Meta meta() {
    return MarketEnvironment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MarketEnvironment.Meta.INSTANCE);
  }

  /**
   * Creates an instance.
   * @param valuationDate  the value of the property, not null
   * @param values  the value of the property, not null
   * @param timeSeries  the value of the property, not null
   */
  MarketEnvironment(
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, Object> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.valuationDate = valuationDate;
    this.values = ImmutableMap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
  }

  @Override
  public MarketEnvironment.Meta metaBean() {
    return MarketEnvironment.Meta.INSTANCE;
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
   * Gets the valuation date associated with the data.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the individual items of market data, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<MarketDataId<?>, Object> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time series of market data values, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MarketEnvironment other = (MarketEnvironment) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getValues(), other.getValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("MarketEnvironment{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("values").append('=').append(getValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MarketEnvironment}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", MarketEnvironment.class, LocalDate.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Object>> values = DirectMetaProperty.ofImmutable(
        this, "values", MarketEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", MarketEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "values",
        "timeSeries");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -823812830:  // values
          return values;
        case 779431844:  // timeSeries
          return timeSeries;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends MarketEnvironment> builder() {
      return new MarketEnvironment.Builder();
    }

    @Override
    public Class<? extends MarketEnvironment> beanType() {
      return MarketEnvironment.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, Object>> values() {
      return values;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((MarketEnvironment) bean).getValuationDate();
        case -823812830:  // values
          return ((MarketEnvironment) bean).getValues();
        case 779431844:  // timeSeries
          return ((MarketEnvironment) bean).getTimeSeries();
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
   * The bean-builder for {@code MarketEnvironment}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<MarketEnvironment> {

    private LocalDate valuationDate;
    private Map<? extends MarketDataId<?>, Object> values = ImmutableMap.of();
    private Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -823812830:  // values
          return values;
        case 779431844:  // timeSeries
          return timeSeries;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case -823812830:  // values
          this.values = (Map<? extends MarketDataId<?>, Object>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<? extends ObservableId, LocalDateDoubleTimeSeries>) newValue;
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
    public MarketEnvironment build() {
      return new MarketEnvironment(
          valuationDate,
          values,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("MarketEnvironment.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
