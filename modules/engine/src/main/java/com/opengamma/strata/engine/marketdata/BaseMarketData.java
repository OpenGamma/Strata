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
import org.joda.beans.ImmutableConstructor;
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;

/**
 * A source of market data for a single set of calculations.
 */
@BeanDefinition(builderScope = "private")
public final class BaseMarketData implements ImmutableBean, MarketDataLookup {

  /** The valuation date associated with the data. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;

  // TODO Do the values need to include the timestamp as well as the market data item?
  /** The individual items of market data, keyed by ID. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<? extends MarketDataId<?>, Object> values;

  // TODO Do the values need to include the timestamp as well as the time series?
  /** The time series of market data values, keyed by ID. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  /**
   * Returns an empty mutable builder for building a new instance of {@code BaseMarketData}.
   *
   * @param valuationDate  the valuation date
   * @return an empty mutable builder for building a new instance of {@code BaseMarketData}
   */
  public static BaseMarketDataBuilder builder(LocalDate valuationDate) {
    return new BaseMarketDataBuilder(valuationDate);
  }

  /**
   * Returns an empty set of market data with the specified valuation date.
   *
   * @param valuationDate  the valuation date used for the calculations
   * @return an empty set of market data with the specified valuation date
   */
  public static BaseMarketData empty(LocalDate valuationDate) {
    return BaseMarketData.builder(valuationDate).build();
  }

  /**
   * Package-private constructor used by the builder.
   *
   * @param valuationDate  the valuation date of the market data
   * @param values  the single market data values
   * @param timeSeries  the time series of market data values
   */
  @ImmutableConstructor
  BaseMarketData(
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, Object> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.values = ImmutableMap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    Object value = values.get(id);
    return value != null && id.getMarketDataType().isInstance(value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, I extends MarketDataId<T>> T getValue(I id) {
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
    return (T) value;
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
   * Returns the observable market data in this set of data.
   *
   * @return the observable market data in this set of data
   */
  public Observables getObservables() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a market data environment containing the calibrated market data in this data set.
   *
   * @return a market data environment containing the calibrated market data in this data set
   */
  public MarketDataEnvironment getMarketDataEnvironment() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a mutable builder containing the data from this object.
   *
   * @return a mutable builder containing the data from this object
   */
  public BaseMarketDataBuilder toBuilder() {
    return new BaseMarketDataBuilder(valuationDate, values, timeSeries);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BaseMarketData}.
   * @return the meta-bean, not null
   */
  public static BaseMarketData.Meta meta() {
    return BaseMarketData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BaseMarketData.Meta.INSTANCE);
  }

  @Override
  public BaseMarketData.Meta metaBean() {
    return BaseMarketData.Meta.INSTANCE;
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
  public ImmutableMap<? extends MarketDataId<?>, Object> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time series of market data values, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<? extends ObservableId, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BaseMarketData other = (BaseMarketData) obj;
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
    buf.append("BaseMarketData{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("values").append('=').append(getValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BaseMarketData}.
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
        this, "valuationDate", BaseMarketData.class, LocalDate.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> values = DirectMetaProperty.ofImmutable(
        this, "values", BaseMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", BaseMarketData.class, (Class) ImmutableMap.class);
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
    public BeanBuilder<? extends BaseMarketData> builder() {
      return new BaseMarketData.Builder();
    }

    @Override
    public Class<? extends BaseMarketData> beanType() {
      return BaseMarketData.class;
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
    public MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> values() {
      return values;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<? extends ObservableId, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((BaseMarketData) bean).getValuationDate();
        case -823812830:  // values
          return ((BaseMarketData) bean).getValues();
        case 779431844:  // timeSeries
          return ((BaseMarketData) bean).getTimeSeries();
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
   * The bean-builder for {@code BaseMarketData}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<BaseMarketData> {

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
    public BaseMarketData build() {
      return new BaseMarketData(
          valuationDate,
          values,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("BaseMarketData.Builder{");
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
