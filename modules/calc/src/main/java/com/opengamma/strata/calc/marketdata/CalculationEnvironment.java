/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

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
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.calc.runner.MissingMappingId;
import com.opengamma.strata.calc.runner.NoMatchingRuleId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

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

  /** The valuation date associated with the market data. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final MarketDataBox<LocalDate> valuationDate;

  /** The number of scenarios for which the environment contains market data. */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int scenarioCount;

  // TODO Should there be separate maps for observable and non-observable data?
  // TODO Do the values need to include the timestamp as well as the market data item?
  /** The individual items of market data, keyed by ID. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, MarketDataBox<?>>")
  private final ImmutableMap<MarketDataId<?>, MarketDataBox<?>> values;

  // TODO Do the values need to include the timestamp as well as the time series?
  /** The time series of market data values, keyed by ID. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends ObservableId, LocalDateDoubleTimeSeries>")
  private final ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  /** Details of failures when building single market data values. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> valueFailures;

  /** Details of failures when building time series of market data values. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> timeSeriesFailures;

  /**
   * Returns an empty mutable builder for building a new instance of {@code CalculationEnvironment}.
   *
   * @return an empty mutable builder for building a new instance of {@code CalculationEnvironment}
   */
  public static CalculationEnvironmentBuilder builder() {
    return new CalculationEnvironmentBuilder();
  }

  /**
   * Returns an empty set of market data with the specified valuation date.
   *
   * @return an empty set of market data with the specified valuation date
   */
  public static CalculationEnvironment empty() {
    return CalculationEnvironment.builder().valuationDate(MarketDataBox.ofScenarioValues()).build();
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    return values.containsKey(id);
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    // Special handling of these special ID types to provide more helpful error messages
    if (id instanceof NoMatchingRuleId) {
      MarketDataKey<?> key = ((NoMatchingRuleId) id).getKey();
      throw new IllegalArgumentException("No market data rules were available to build the market data for " + key);
    }
    if (id instanceof MissingMappingId) {
      MarketDataKey<?> key = ((MissingMappingId) id).getKey();
      throw new IllegalArgumentException("No market data mapping found for " + key);
    }
    @SuppressWarnings("unchecked")
    MarketDataBox<T> value = (MarketDataBox<T>) values.get(id);

    if (value == null) {
      Failure failure = valueFailures.get(id);

      if (failure != null) {
        throw new FailureException(failure);
      }
      throw new IllegalArgumentException("No market data available for " + id);
    }
    if (!id.getMarketDataType().isAssignableFrom(value.getMarketDataType())) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data value for ID {} is not of the expected type. Expected type: {}, actual type: {}, value: {}",
              id,
              id.getMarketDataType().getName(),
              value.getMarketDataType().getName(),
              value));
    }
    return value;
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
    return new CalculationEnvironmentBuilder(valuationDate, values, timeSeries, valueFailures, timeSeriesFailures);
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
   * @param scenarioCount  the value of the property
   * @param values  the value of the property, not null
   * @param timeSeries  the value of the property, not null
   * @param valueFailures  the value of the property, not null
   * @param timeSeriesFailures  the value of the property, not null
   */
  CalculationEnvironment(
      MarketDataBox<LocalDate> valuationDate,
      int scenarioCount,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<? extends MarketDataId<?>, Failure> valueFailures,
      Map<? extends MarketDataId<?>, Failure> timeSeriesFailures) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    ArgChecker.notNegative(scenarioCount, "scenarioCount");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(valueFailures, "valueFailures");
    JodaBeanUtils.notNull(timeSeriesFailures, "timeSeriesFailures");
    this.valuationDate = valuationDate;
    this.scenarioCount = scenarioCount;
    this.values = ImmutableMap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.valueFailures = ImmutableMap.copyOf(valueFailures);
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
   * Gets the valuation date associated with the market data.
   * @return the value of the property, not null
   */
  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of scenarios for which the environment contains market data.
   * @return the value of the property
   */
  public int getScenarioCount() {
    return scenarioCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the individual items of market data, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<MarketDataId<?>, MarketDataBox<?>> getValues() {
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
  public ImmutableMap<MarketDataId<?>, Failure> getValueFailures() {
    return valueFailures;
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
          (getScenarioCount() == other.getScenarioCount()) &&
          JodaBeanUtils.equal(getValues(), other.getValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getValueFailures(), other.getValueFailures()) &&
          JodaBeanUtils.equal(getTimeSeriesFailures(), other.getTimeSeriesFailures());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getScenarioCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValueFailures());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeriesFailures());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("CalculationEnvironment{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("scenarioCount").append('=').append(getScenarioCount()).append(',').append(' ');
    buf.append("values").append('=').append(getValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("valueFailures").append('=').append(getValueFailures()).append(',').append(' ');
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
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<MarketDataBox<LocalDate>> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", CalculationEnvironment.class, (Class) MarketDataBox.class);
    /**
     * The meta-property for the {@code scenarioCount} property.
     */
    private final MetaProperty<Integer> scenarioCount = DirectMetaProperty.ofImmutable(
        this, "scenarioCount", CalculationEnvironment.class, Integer.TYPE);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, MarketDataBox<?>>> values = DirectMetaProperty.ofImmutable(
        this, "values", CalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", CalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code valueFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> valueFailures = DirectMetaProperty.ofImmutable(
        this, "valueFailures", CalculationEnvironment.class, (Class) ImmutableMap.class);
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
        "scenarioCount",
        "values",
        "timeSeries",
        "valueFailures",
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
        case -1203198113:  // scenarioCount
          return scenarioCount;
        case -823812830:  // values
          return values;
        case 779431844:  // timeSeries
          return timeSeries;
        case -68881222:  // valueFailures
          return valueFailures;
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
    public MetaProperty<MarketDataBox<LocalDate>> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code scenarioCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> scenarioCount() {
      return scenarioCount;
    }

    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, MarketDataBox<?>>> values() {
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
     * The meta-property for the {@code valueFailures} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> valueFailures() {
      return valueFailures;
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
        case -1203198113:  // scenarioCount
          return ((CalculationEnvironment) bean).getScenarioCount();
        case -823812830:  // values
          return ((CalculationEnvironment) bean).getValues();
        case 779431844:  // timeSeries
          return ((CalculationEnvironment) bean).getTimeSeries();
        case -68881222:  // valueFailures
          return ((CalculationEnvironment) bean).getValueFailures();
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

    private MarketDataBox<LocalDate> valuationDate;
    private int scenarioCount;
    private Map<? extends MarketDataId<?>, MarketDataBox<?>> values = ImmutableMap.of();
    private Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Failure> valueFailures = ImmutableMap.of();
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
        case -1203198113:  // scenarioCount
          return scenarioCount;
        case -823812830:  // values
          return values;
        case 779431844:  // timeSeries
          return timeSeries;
        case -68881222:  // valueFailures
          return valueFailures;
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
          this.valuationDate = (MarketDataBox<LocalDate>) newValue;
          break;
        case -1203198113:  // scenarioCount
          this.scenarioCount = (Integer) newValue;
          break;
        case -823812830:  // values
          this.values = (Map<? extends MarketDataId<?>, MarketDataBox<?>>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<? extends ObservableId, LocalDateDoubleTimeSeries>) newValue;
          break;
        case -68881222:  // valueFailures
          this.valueFailures = (Map<? extends MarketDataId<?>, Failure>) newValue;
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
          scenarioCount,
          values,
          timeSeries,
          valueFailures,
          timeSeriesFailures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("CalculationEnvironment.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("valueFailures").append('=').append(JodaBeanUtils.toString(valueFailures)).append(',').append(' ');
      buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
