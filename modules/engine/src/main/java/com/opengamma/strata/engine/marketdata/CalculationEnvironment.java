/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.io.Serializable;
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
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculation.MissingMappingId;
import com.opengamma.strata.engine.calculation.NoMatchingRuleId;

/**
 * A collection of market data for a single set of calculations.
 * <p>
 * A calculation environment contains all the data required to perform calculations. This includes the market data
 * a user would expect to see, for example quotes and curves, and also other values derived from market data.
 * <p>
 * The derived values include objects used in calculations encapsulating market data and logic that operates on
 * it, and objects with market data and metadata required by the scenario framework.
 * <p>
 * {@code CalculationEnvironment} should be considered an implementation detail and is not intended as a
 * user-facing data structure.
 * <p>
 * {@link MarketEnvironment} should be used to store market data values that users wish to interact with.
 *
 * @see MarketEnvironment
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class CalculationEnvironment implements ImmutableBean, MarketDataLookup, Serializable {

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

  /** Details of failures when building single market data values. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> singleValueFailures;

  /** Details of failures when building time series of market data values. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> timeSeriesFailures;

  /**
   * Returns a calculation environment containing the market data from a market environment.
   *
   * @param marketEnvironment  an environment whose market data is used to build a calculation environment
   * @return a calculation environment containing the market data from a market environment
   */
  public static CalculationEnvironment of(MarketEnvironment marketEnvironment) {
    return new CalculationEnvironment(
        marketEnvironment.getValuationDate(),
        marketEnvironment.getValues(),
        marketEnvironment.getTimeSeries(),
        ImmutableMap.of(),
        ImmutableMap.of());
  }

  /**
   * Returns an empty mutable builder for building a new instance of {@code MarketEnvironment}.
   *
   * @param valuationDate  the valuation date
   * @return an empty mutable builder for building a new instance of {@code MarketEnvironment}
   */
  public static CalculationEnvironmentBuilder builder(LocalDate valuationDate) {
    return new CalculationEnvironmentBuilder(valuationDate);
  }

  /**
   * Returns an empty set of market data with the specified valuation date.
   *
   * @param valuationDate  the valuation date used for the calculations
   * @return an empty set of market data with the specified valuation date
   */
  public static CalculationEnvironment empty(LocalDate valuationDate) {
    return CalculationEnvironment.builder(valuationDate).build();
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
      Failure failure = singleValueFailures.get(id);

      if (failure != null) {
        throw new FailureException(failure);
      }
      throw new IllegalArgumentException("No market data available for " + id);
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
      throw new IllegalArgumentException("No time series available for " + id);
    }
    return timeSeries;
  }

  /**
   * Returns a mutable builder containing the data from this object.
   *
   * @return a mutable builder containing the data from this object
   */
  public CalculationEnvironmentBuilder toBuilder() {
    return new CalculationEnvironmentBuilder(valuationDate, values, timeSeries, singleValueFailures, timeSeriesFailures);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationEnvironment}.
   * @return the meta-bean, not null
   */
  public static CalculationEnvironment.Meta meta() {
    return CalculationEnvironment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CalculationEnvironment.Meta.INSTANCE);
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
   * @param singleValueFailures  the value of the property, not null
   * @param timeSeriesFailures  the value of the property, not null
   */
  CalculationEnvironment(
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, Object> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<? extends MarketDataId<?>, Failure> singleValueFailures,
      Map<? extends MarketDataId<?>, Failure> timeSeriesFailures) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(singleValueFailures, "singleValueFailures");
    JodaBeanUtils.notNull(timeSeriesFailures, "timeSeriesFailures");
    this.valuationDate = valuationDate;
    this.values = ImmutableMap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.singleValueFailures = ImmutableMap.copyOf(singleValueFailures);
    this.timeSeriesFailures = ImmutableMap.copyOf(timeSeriesFailures);
  }

  @Override
  public CalculationEnvironment.Meta metaBean() {
    return CalculationEnvironment.Meta.INSTANCE;
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
  /**
   * Gets details of failures when building single market data values.
   * @return the value of the property, not null
   */
  public ImmutableMap<MarketDataId<?>, Failure> getSingleValueFailures() {
    return singleValueFailures;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets details of failures when building time series of market data values.
   * @return the value of the property, not null
   */
  public ImmutableMap<MarketDataId<?>, Failure> getTimeSeriesFailures() {
    return timeSeriesFailures;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationEnvironment other = (CalculationEnvironment) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getValues(), other.getValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getSingleValueFailures(), other.getSingleValueFailures()) &&
          JodaBeanUtils.equal(getTimeSeriesFailures(), other.getTimeSeriesFailures());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSingleValueFailures());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeriesFailures());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CalculationEnvironment{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("values").append('=').append(getValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("singleValueFailures").append('=').append(getSingleValueFailures()).append(',').append(' ');
    buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(getTimeSeriesFailures()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CalculationEnvironment}.
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
        this, "valuationDate", CalculationEnvironment.class, LocalDate.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Object>> values = DirectMetaProperty.ofImmutable(
        this, "values", CalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", CalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code singleValueFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> singleValueFailures = DirectMetaProperty.ofImmutable(
        this, "singleValueFailures", CalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeriesFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> timeSeriesFailures = DirectMetaProperty.ofImmutable(
        this, "timeSeriesFailures", CalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "values",
        "timeSeries",
        "singleValueFailures",
        "timeSeriesFailures");

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
        case -1633495726:  // singleValueFailures
          return singleValueFailures;
        case -1580093459:  // timeSeriesFailures
          return timeSeriesFailures;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CalculationEnvironment> builder() {
      return new CalculationEnvironment.Builder();
    }

    @Override
    public Class<? extends CalculationEnvironment> beanType() {
      return CalculationEnvironment.class;
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

    /**
     * The meta-property for the {@code singleValueFailures} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> singleValueFailures() {
      return singleValueFailures;
    }

    /**
     * The meta-property for the {@code timeSeriesFailures} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> timeSeriesFailures() {
      return timeSeriesFailures;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((CalculationEnvironment) bean).getValuationDate();
        case -823812830:  // values
          return ((CalculationEnvironment) bean).getValues();
        case 779431844:  // timeSeries
          return ((CalculationEnvironment) bean).getTimeSeries();
        case -1633495726:  // singleValueFailures
          return ((CalculationEnvironment) bean).getSingleValueFailures();
        case -1580093459:  // timeSeriesFailures
          return ((CalculationEnvironment) bean).getTimeSeriesFailures();
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
   * The bean-builder for {@code CalculationEnvironment}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CalculationEnvironment> {

    private LocalDate valuationDate;
    private Map<? extends MarketDataId<?>, Object> values = ImmutableMap.of();
    private Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Failure> singleValueFailures = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Failure> timeSeriesFailures = ImmutableMap.of();

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
        case -1633495726:  // singleValueFailures
          return singleValueFailures;
        case -1580093459:  // timeSeriesFailures
          return timeSeriesFailures;
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
        case -1633495726:  // singleValueFailures
          this.singleValueFailures = (Map<? extends MarketDataId<?>, Failure>) newValue;
          break;
        case -1580093459:  // timeSeriesFailures
          this.timeSeriesFailures = (Map<? extends MarketDataId<?>, Failure>) newValue;
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
    public CalculationEnvironment build() {
      return new CalculationEnvironment(
          valuationDate,
          values,
          timeSeries,
          singleValueFailures,
          timeSeriesFailures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CalculationEnvironment.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("singleValueFailures").append('=').append(JodaBeanUtils.toString(singleValueFailures)).append(',').append(' ');
      buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
