/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * An immutable set of market data
 * <p>
 * This is the standard immutable implementation of {@link MarketData}.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class ImmutableMarketData
    implements MarketData, ImmutableBean, Serializable {

  /**
   * The valuation date associated with the market data.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The market data values.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataKey<?>, ?>")
  private final Map<MarketDataKey<?>, Object> values;
  /**
   * The time-series.
   * <p>
   * If a request is made for a time-series that is not in the map, an empty series will be returned.
   */
  @PropertyDefinition(validate = "notNull")
  private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a valuation date and map of values.
   * <p>
   * Use the {@linkplain #builder(LocalDate) builder} more more complex use cases,
   * including setting time-series.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param values  the market data values
   * @return a set of market data containing the values in the map
   */
  public static ImmutableMarketData of(LocalDate valuationDate, Map<? extends MarketDataKey<?>, ?> values) {
    MapStream.of(values).forEach((key, value) -> checkType(key, value));
    return new ImmutableMarketData(valuationDate, values, ImmutableMap.of());
  }

  // checks the value is an instance of the market data type of the id
  static void checkType(MarketDataKey<?> key, Object value) {
    if (!key.getMarketDataType().isInstance(value)) {
      if (value == null) {
        throw new IllegalArgumentException(Messages.format(
            "Value for identifier '{}' must not be null", key));
      }
      throw new ClassCastException(Messages.format(
          "Value for identifier '{}' does not implement expected type '{}': '{}'",
          key, key.getMarketDataType().getSimpleName(), value));
    }
  }

  /**
   * Creates a builder that can be used to build an instance of {@code MarketData}.
   * 
   * @param valuationDate  the valuation date
   * @return the builder, not null
   */
  public static ImmutableMarketDataBuilder builder(LocalDate valuationDate) {
    return new ImmutableMarketDataBuilder(valuationDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a builder populated with the same data as this instance.
   * 
   * @return the mutable builder, not null
   */
  public ImmutableMarketDataBuilder toBuilder() {
    return new ImmutableMarketDataBuilder(valuationDate, values, timeSeries);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsValue(MarketDataKey<?> key) {
    // overridden for performance
    return values.containsKey(key);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getValue(MarketDataKey<T> key) {
    // overridden for performance
    // no type check against id.getMarketDataType() as checked in factory
    @SuppressWarnings("unchecked")
    T value = (T) values.get(key);
    if (value == null) {
      throw new MarketDataNotFoundException(msgValueNotFound(key));
    }
    return value;
  }

  // extracted to aid inlining performance
  private String msgValueNotFound(MarketDataKey<?> key) {
    return Messages.format(
        "Market data not found for identifier '{}' of type '{}'", key, key.getClass().getSimpleName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> findValue(MarketDataKey<T> key) {
    // no type check against id.getMarketDataType() as checked in factory
    @SuppressWarnings("unchecked")
    T value = (T) values.get(key);
    return Optional.ofNullable(value);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    LocalDateDoubleTimeSeries found = timeSeries.get(key);
    return found == null ? LocalDateDoubleTimeSeries.empty() : found;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableMarketData}.
   * @return the meta-bean, not null
   */
  public static ImmutableMarketData.Meta meta() {
    return ImmutableMarketData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableMarketData.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param valuationDate  the value of the property, not null
   * @param values  the value of the property, not null
   * @param timeSeries  the value of the property, not null
   */
  ImmutableMarketData(
      LocalDate valuationDate,
      Map<? extends MarketDataKey<?>, ?> values,
      Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.valuationDate = valuationDate;
    this.values = ImmutableMap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
  }

  @Override
  public ImmutableMarketData.Meta metaBean() {
    return ImmutableMarketData.Meta.INSTANCE;
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
   * Gets the valuation date associated with the market data.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data values.
   * @return the value of the property, not null
   */
  public Map<MarketDataKey<?>, Object> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series.
   * <p>
   * If a request is made for a time-series that is not in the map, an empty series will be returned.
   * @return the value of the property, not null
   */
  public Map<ObservableKey, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableMarketData other = (ImmutableMarketData) obj;
      return JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(values, other.values) &&
          JodaBeanUtils.equal(timeSeries, other.timeSeries);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(values);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeries);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ImmutableMarketData{");
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("values").append('=').append(values).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableMarketData}.
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
        this, "valuationDate", ImmutableMarketData.class, LocalDate.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Map<MarketDataKey<?>, Object>> values = DirectMetaProperty.ofImmutable(
        this, "values", ImmutableMarketData.class, (Class) Map.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Map<ObservableKey, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ImmutableMarketData.class, (Class) Map.class);
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
    public BeanBuilder<? extends ImmutableMarketData> builder() {
      return new ImmutableMarketData.Builder();
    }

    @Override
    public Class<? extends ImmutableMarketData> beanType() {
      return ImmutableMarketData.class;
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
    public MetaProperty<Map<MarketDataKey<?>, Object>> values() {
      return values;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Map<ObservableKey, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((ImmutableMarketData) bean).getValuationDate();
        case -823812830:  // values
          return ((ImmutableMarketData) bean).getValues();
        case 779431844:  // timeSeries
          return ((ImmutableMarketData) bean).getTimeSeries();
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
   * The bean-builder for {@code ImmutableMarketData}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ImmutableMarketData> {

    private LocalDate valuationDate;
    private Map<? extends MarketDataKey<?>, ?> values = ImmutableMap.of();
    private Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();

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
          this.values = (Map<? extends MarketDataKey<?>, ?>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<ObservableKey, LocalDateDoubleTimeSeries>) newValue;
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
    public ImmutableMarketData build() {
      return new ImmutableMarketData(
          valuationDate,
          values,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ImmutableMarketData.Builder{");
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
