/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.NamedMarketDataId;
import com.opengamma.strata.data.ObservableId;

/**
 * An immutable set of market data across one or more scenarios.
 * <p>
 * This is the standard immutable implementation of {@link ScenarioMarketData}.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class ImmutableScenarioMarketData
    implements ScenarioMarketData, ImmutableBean, Serializable {

  /** An empty instance. */
  private static final ImmutableScenarioMarketData EMPTY =
      new ImmutableScenarioMarketData(0, MarketDataBox.empty(), ImmutableMap.of(), ImmutableMap.of());

  /**
   * The number of scenarios.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative", overrideGet = true)
  private final int scenarioCount;
  /**
   * The valuation date associated with each scenario.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final MarketDataBox<LocalDate> valuationDate;
  /**
   * The individual items of market data.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, MarketDataBox<?>>")
  private final ImmutableMap<MarketDataId<?>, MarketDataBox<?>> values;
  /**
   * The time-series of market data values.
   * <p>
   * If a request is made for a time-series that is not in the map, an empty series will be returned.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends ObservableId, LocalDateDoubleTimeSeries>")
  private final ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   * <p>
   * The valuation date and map of values must have the same number of scenarios.
   * 
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation dates associated with all scenarios
   * @param values  the market data values, one for each scenario
   * @param timeSeries  the time-series
   * @return a set of market data containing the values in the map
   */
  public static ImmutableScenarioMarketData of(
      int scenarioCount,
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    return of(scenarioCount, MarketDataBox.ofSingleValue(valuationDate), values, timeSeries);
  }

  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   * <p>
   * The valuation date and map of values must have the same number of scenarios.
   * 
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation dates associated with the market data, one for each scenario
   * @param values  the market data values, one for each scenario
   * @param timeSeries  the time-series
   * @return a set of market data containing the values in the map
   */
  public static ImmutableScenarioMarketData of(
      int scenarioCount,
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    MapStream.of(values).forEach((key, value) -> checkType(key, value, scenarioCount));
    return new ImmutableScenarioMarketData(scenarioCount, valuationDate, values, timeSeries);
  }

  // checks the value is an instance of the market data type of the id
  static void checkType(MarketDataId<?> id, MarketDataBox<?> box, int scenarioCount) {
    if (box == null) {
      throw new IllegalArgumentException(Messages.format(
          "Value for identifier '{}' must not be null", id));
    }
    if (box.isScenarioValue() && box.getScenarioCount() != scenarioCount) {
      throw new IllegalArgumentException(Messages.format(
          "Value for identifier '{}' should have had {} scenarios but had {}", id, scenarioCount, box.getScenarioCount()));
    }
    if (box.getScenarioCount() > 0 && !id.getMarketDataType().isInstance(box.getValue(0))) {
      throw new ClassCastException(Messages.format(
          "Value for identifier '{}' does not implement expected type '{}': '{}'",
          id, id.getMarketDataType().getSimpleName(), box));
    }
  }

  /**
   * Obtains a market data instance that contains no data and has no scenarios.
   *
   * @return an empty instance
   */
  public static ImmutableScenarioMarketData empty() {
    return EMPTY;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a mutable builder that can be used to create an instance of the market data.
   * 
   * @param valuationDate  the valuation date associated with the market data
   * @return the mutable builder
   */
  public static ImmutableScenarioMarketDataBuilder builder(LocalDate valuationDate) {
    return new ImmutableScenarioMarketDataBuilder(valuationDate);
  }

  /**
   * Creates a mutable builder that can be used to create an instance of the market data.
   * 
   * @param valuationDate  the valuation dates associated with the market data, one for each scenario
   * @return the mutable builder
   */
  public static ImmutableScenarioMarketDataBuilder builder(MarketDataBox<LocalDate> valuationDate) {

    return new ImmutableScenarioMarketDataBuilder(valuationDate);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsValue(MarketDataId<?> id) {
    // overridden for performance
    return values.containsKey(id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    // overridden for performance
    // no type check against id.getMarketDataType() as checked in factory
    @SuppressWarnings("unchecked")
    MarketDataBox<T> value = (MarketDataBox<T>) values.get(id);
    if (value == null) {
      throw new MarketDataNotFoundException(msgValueNotFound(id));
    }
    return value;
  }

  // extracted to aid inlining performance
  private String msgValueNotFound(MarketDataId<?> id) {
    return Messages.format(
        "Market data not found for identifier '{}' of type '{}'", id, id.getClass().getSimpleName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    // no type check against id.getMarketDataType() as checked in factory
    @SuppressWarnings("unchecked")
    MarketDataBox<T> value = (MarketDataBox<T>) values.get(id);
    return Optional.ofNullable(value);
  }

  @Override
  public Set<MarketDataId<?>> getIds() {
    return values.keySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
    // no type check against id.getMarketDataType() as checked in factory
    return values.keySet().stream()
        .filter(id -> id instanceof NamedMarketDataId)
        .filter(id -> ((NamedMarketDataId<?>) id).getMarketDataName().equals(name))
        .map(id -> (MarketDataId<T>) id)
        .collect(toImmutableSet());
  }

  @Override
  public Set<ObservableId> getTimeSeriesIds() {
    return timeSeries.keySet();
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries found = timeSeries.get(id);
    return found == null ? LocalDateDoubleTimeSeries.empty() : found;
  }

  /**
   * Returns set of market data which combines the data from this set of data with another set.
   * <p>
   * If the same item of data is available in both sets, it will be taken from this set.
   * <p>
   * Both sets of data must contain the same number of scenarios, or one of them must have one scenario.
   * If one of the sets of data has one scenario, the combined set will have the scenario count
   * of the other set.
   * <p>
   * The valuation dates are taken from this set of data.
   *
   * @param other  another set of market data
   * @return a set of market data combining the data in this set with the data in the other
   */
  public ImmutableScenarioMarketData combinedWith(ImmutableScenarioMarketData other) {
    if (scenarioCount != 1 && other.scenarioCount != 1 && scenarioCount != other.scenarioCount) {
      throw new IllegalArgumentException(
          Messages.format(
              "When merging scenario market data, both sets of data must have the same number of scenarios or " +
                  "at least one of them must have one scenario. Number of scenarios: {} and {}",
              scenarioCount,
              other.scenarioCount));
    }
    int mergedCount = Math.max(scenarioCount, other.scenarioCount);
    // Use HashMap because it allows values to be overwritten. ImmutableMap builders throw an exception if a value
    // is added using a key which is already present
    Map<MarketDataId<?>, MarketDataBox<?>> values = new HashMap<>(other.values);
    values.putAll(this.values);
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>(other.timeSeries);
    timeSeries.putAll(this.timeSeries);
    return new ImmutableScenarioMarketData(mergedCount, valuationDate, values, timeSeries);
  }

  @Override
  public ScenarioMarketData combinedWith(ScenarioMarketData other) {
    if (other instanceof ImmutableScenarioMarketData) {
      return combinedWith((ImmutableScenarioMarketData) other);
    } else {
      return ScenarioMarketData.super.combinedWith(other);
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableScenarioMarketData}.
   * @return the meta-bean, not null
   */
  public static ImmutableScenarioMarketData.Meta meta() {
    return ImmutableScenarioMarketData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableScenarioMarketData.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param scenarioCount  the value of the property
   * @param valuationDate  the value of the property, not null
   * @param values  the value of the property, not null
   * @param timeSeries  the value of the property, not null
   */
  ImmutableScenarioMarketData(
      int scenarioCount,
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
    ArgChecker.notNegative(scenarioCount, "scenarioCount");
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.scenarioCount = scenarioCount;
    this.valuationDate = valuationDate;
    this.values = ImmutableMap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
  }

  @Override
  public ImmutableScenarioMarketData.Meta metaBean() {
    return ImmutableScenarioMarketData.Meta.INSTANCE;
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
   * Gets the number of scenarios.
   * @return the value of the property
   */
  @Override
  public int getScenarioCount() {
    return scenarioCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date associated with each scenario.
   * @return the value of the property, not null
   */
  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the individual items of market data.
   * @return the value of the property, not null
   */
  public ImmutableMap<MarketDataId<?>, MarketDataBox<?>> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series of market data values.
   * <p>
   * If a request is made for a time-series that is not in the map, an empty series will be returned.
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
      ImmutableScenarioMarketData other = (ImmutableScenarioMarketData) obj;
      return (scenarioCount == other.scenarioCount) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(values, other.values) &&
          JodaBeanUtils.equal(timeSeries, other.timeSeries);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(scenarioCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(values);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeries);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ImmutableScenarioMarketData{");
    buf.append("scenarioCount").append('=').append(scenarioCount).append(',').append(' ');
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("values").append('=').append(values).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableScenarioMarketData}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code scenarioCount} property.
     */
    private final MetaProperty<Integer> scenarioCount = DirectMetaProperty.ofImmutable(
        this, "scenarioCount", ImmutableScenarioMarketData.class, Integer.TYPE);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<MarketDataBox<LocalDate>> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ImmutableScenarioMarketData.class, (Class) MarketDataBox.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, MarketDataBox<?>>> values = DirectMetaProperty.ofImmutable(
        this, "values", ImmutableScenarioMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ImmutableScenarioMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "scenarioCount",
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
        case -1203198113:  // scenarioCount
          return scenarioCount;
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
    public BeanBuilder<? extends ImmutableScenarioMarketData> builder() {
      return new ImmutableScenarioMarketData.Builder();
    }

    @Override
    public Class<? extends ImmutableScenarioMarketData> beanType() {
      return ImmutableScenarioMarketData.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code scenarioCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> scenarioCount() {
      return scenarioCount;
    }

    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataBox<LocalDate>> valuationDate() {
      return valuationDate;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1203198113:  // scenarioCount
          return ((ImmutableScenarioMarketData) bean).getScenarioCount();
        case 113107279:  // valuationDate
          return ((ImmutableScenarioMarketData) bean).getValuationDate();
        case -823812830:  // values
          return ((ImmutableScenarioMarketData) bean).getValues();
        case 779431844:  // timeSeries
          return ((ImmutableScenarioMarketData) bean).getTimeSeries();
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
   * The bean-builder for {@code ImmutableScenarioMarketData}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<ImmutableScenarioMarketData> {

    private int scenarioCount;
    private MarketDataBox<LocalDate> valuationDate;
    private Map<? extends MarketDataId<?>, MarketDataBox<?>> values = ImmutableMap.of();
    private Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1203198113:  // scenarioCount
          return scenarioCount;
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
        case -1203198113:  // scenarioCount
          this.scenarioCount = (Integer) newValue;
          break;
        case 113107279:  // valuationDate
          this.valuationDate = (MarketDataBox<LocalDate>) newValue;
          break;
        case -823812830:  // values
          this.values = (Map<? extends MarketDataId<?>, MarketDataBox<?>>) newValue;
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
    public ImmutableScenarioMarketData build() {
      return new ImmutableScenarioMarketData(
          scenarioCount,
          valuationDate,
          values,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutableScenarioMarketData.Builder{");
      buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount)).append(',').append(' ');
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
