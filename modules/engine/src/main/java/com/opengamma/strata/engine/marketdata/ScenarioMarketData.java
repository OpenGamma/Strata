/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;

/**
 * A set of market data used for performing calculations across a set of scenarios.
 */
@SuppressWarnings("unchecked")
@BeanDefinition(builderScope = "private")
public class ScenarioMarketData implements ImmutableBean {

  /** The number of scenarios. */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final int scenarioCount;

  /** The valuation dates of the scenarios, one for each scenario. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<LocalDate> valuationDates;

  /** Individual items of market data, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableListMultimap<MarketDataId<?>, ?> values;

  /** Time series of observable market data values, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  /** Market dat values that are potentially applicable across all scenarios, keyed by ID. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<? extends MarketDataId<?>, Object> globalValues;

  /**
   * Returns a mutable builder for building a set of scenario market data.
   *
   * @param scenarioCount  the number of scenarios
   * @return a mutable builder for building a set of scenario market data
   */
  public static ScenarioMarketDataBuilder builder(int scenarioCount) {
    return new ScenarioMarketDataBuilder(scenarioCount);
  }

  /**
   * Returns a mutable builder for building a set of scenario market data where every scenario has the
   * same valuation date.
   *
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation date of all scenarios
   * @return a mutable builder for building a set of scenario market data
   */
  public static ScenarioMarketDataBuilder builder(int scenarioCount, LocalDate valuationDate) {
    return new ScenarioMarketDataBuilder(scenarioCount, valuationDate);
  }

  /**
   * Returns a set of data for a single scenario, taking the data from an instance of {@link BaseMarketData}.
   *
   * @param marketData  a set of data for a single scenario
   * @return a set of scenario data for a single scenario taken from {@code marketData}
   */
  public static ScenarioMarketData of(BaseMarketData marketData) {
    ScenarioMarketDataBuilder builder = builder(1, marketData.getValuationDate());
    builder.addTimeSeries(marketData.getTimeSeries());

    for (Map.Entry<? extends MarketDataId<?>, ?> entry : marketData.getValues().entrySet()) {
      MarketDataId<Object> id = (MarketDataId<Object>) entry.getKey();
      Object value = entry.getValue();
      builder.addValues(id, value);
    }
    return builder.build();
  }

  /**
   * Package-private constructor used by the builder.
   *
   * @param scenarioCount  the number of scenarios
   * @param valuationDates  the valuation date of each scenario
   * @param values  the market data values
   * @param timeSeries  the time series of market data values
   * @param globalValues  the single values that apply across all scenarios
   */
  @ImmutableConstructor
  ScenarioMarketData(
      int scenarioCount,
      List<LocalDate> valuationDates,
      ListMultimap<MarketDataId<?>, ?> values,
      Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<? extends MarketDataId<?>, Object> globalValues) {

    ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
    JodaBeanUtils.notNull(valuationDates, "valuationDates");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(globalValues, "globalValues");
    this.scenarioCount = scenarioCount;
    this.valuationDates = ImmutableList.copyOf(valuationDates);
    this.values = ImmutableListMultimap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.globalValues = ImmutableMap.copyOf(globalValues);
    validate();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        valuationDates.size() == scenarioCount,
        "The number of valuation dates must equal the number of scenarios");
    // TODO Check the sizes of all the values in the multimaps
  }


  /**
   * Returns a list of market data values, one from each scenario.
   * <p>
   * The date of the market data is the same as the valuation date of the scenario.
   *
   * @param id  ID of the market data
   * @param <T>  type of the market data
   * @param <I>  type of the market data ID
   * @return a list of market data values, one from each scenario
   * @throws IllegalArgumentException if there are no values for the specified ID
   */
  public <T, I extends MarketDataId<T>> List<T> getValues(I id) {
    // Special handling of these special ID types to provide more helpful error messages
    if (id instanceof NoMatchingRuleId) {
      MarketDataKey key = ((NoMatchingRuleId) id).getKey();
      throw new IllegalArgumentException("No market data rules were available to build the market data for " + key);
    }
    if (id instanceof MissingMappingId) {
      MarketDataKey<?> key = ((MissingMappingId) id).getKey();
      throw new IllegalArgumentException("No market data mapping found for " + key);
    }
    List<?> values = this.values.get(id);

    if (values.isEmpty()) {
      throw new IllegalArgumentException("No values available for market data ID " + id);
    }
    return (List<T>) values;
  }

  /**
   * Returns a time series of market data values.
   * <p>
   * Time series are not affected by scenarios, therefore there is a single time series for each key
   * which is shared between all scenarios.
   *
   * @param id  ID of the market data
   * @return a time series of market data values
   * @throws IllegalArgumentException if there is no time series for the specified ID
   */
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries series = timeSeries.get(id);

    if (series == null) {
      throw new IllegalArgumentException("No time series available for market data ID " + id);
    }
    return series;
  }

  /**
   * Returns a single value that is valid for all scenarios.
   * <p>
   * This allows optimizations such as pre-processing of items market data to create a single composite
   * value that can be processed more efficiently.
   *
   * @param id  ID of the market data
   * @param <T>  type of the market data
   * @param <I>  type of the market data ID
   * @return the market data value
   * @throws IllegalArgumentException if there is no value for the specified ID
   */
  public <T, I extends MarketDataId<T>> T getGlobalValue(I id) {
    Object value = globalValues.get(id);

    if (value == null) {
      throw new IllegalArgumentException("No values available for market data ID " + id);
    }
    return (T) value;
  }

  /**
   * Returns true if this set of data contains value for the specified ID.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains values for the specified ID
   */
  public boolean containsValues(MarketDataId<?> id) {
    return values.containsKey(id);
  }

  /**
   * Returns true if this set of data contains a time series for the specified market data ID.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a time series for the specified market data ID
   */
  public boolean containsTimeSeries(ObservableId id) {
    return timeSeries.containsKey(id);
  }

  /**
   * Returns a mutable builder containing the data from this object.
   *
   * @return a mutable builder containing the data from this object
   */
  public ScenarioMarketDataBuilder toBuilder() {
    return new ScenarioMarketDataBuilder(scenarioCount, valuationDates, values, timeSeries, globalValues);
  }

  /**
   * Returns the observable market data in this set of data.
   *
   * @return the observable market data in this set of data
   */
  public List<Observables> getObservables() {
    /*Map<? extends MarketDataId<?>, List<?>> observableValues =
        Seq.seq(values).filter(t -> t.v1().isObservable()).toMap(Tuple2::v1, Tuple2::v2);

    Map<? extends MarketDataId<?>, List<LocalDateDoubleTimeSeries>> observableTimeSeries =
        Seq.seq(timeSeries).filter(t -> t.v1().isObservable()).toMap(Tuple2::v1, Tuple2::v2);*/

    // TODO implement DefaultScenarioMarketData.getObservables
    throw new UnsupportedOperationException("getObservables not implemented");
  }

  /**
   * Returns a market data environment containing the calibrated market data in this data set.
   *
   * @return a market data environment containing the calibrated market data in this data set
   */
  public List<MarketDataEnvironment> getMarketDataEnvironment() {
    // TODO implement DefaultScenarioMarketData.getMarketDataEnvironment
    throw new UnsupportedOperationException("getMarketDataEnvironment not implemented");
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ScenarioMarketData}.
   * @return the meta-bean, not null
   */
  public static ScenarioMarketData.Meta meta() {
    return ScenarioMarketData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ScenarioMarketData.Meta.INSTANCE);
  }

  @Override
  public ScenarioMarketData.Meta metaBean() {
    return ScenarioMarketData.Meta.INSTANCE;
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
  public int getScenarioCount() {
    return scenarioCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation dates of the scenarios, one for each scenario.
   * @return the value of the property, not null
   */
  public ImmutableList<LocalDate> getValuationDates() {
    return valuationDates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets individual items of market data, keyed by ID, one for each scenario.
   * @return the value of the property, not null
   */
  private ImmutableListMultimap<MarketDataId<?>, ?> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets time series of observable market data values, keyed by ID, one for each scenario.
   * @return the value of the property, not null
   */
  private ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets market dat values that are potentially applicable across all scenarios, keyed by ID.
   * @return the value of the property, not null
   */
  private ImmutableMap<? extends MarketDataId<?>, Object> getGlobalValues() {
    return globalValues;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ScenarioMarketData other = (ScenarioMarketData) obj;
      return (getScenarioCount() == other.getScenarioCount()) &&
          JodaBeanUtils.equal(getValuationDates(), other.getValuationDates()) &&
          JodaBeanUtils.equal(getValues(), other.getValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getGlobalValues(), other.getGlobalValues());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getScenarioCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getGlobalValues());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ScenarioMarketData{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(getScenarioCount())).append(',').append(' ');
    buf.append("valuationDates").append('=').append(JodaBeanUtils.toString(getValuationDates())).append(',').append(' ');
    buf.append("values").append('=').append(JodaBeanUtils.toString(getValues())).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries())).append(',').append(' ');
    buf.append("globalValues").append('=').append(JodaBeanUtils.toString(getGlobalValues())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ScenarioMarketData}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code scenarioCount} property.
     */
    private final MetaProperty<Integer> scenarioCount = DirectMetaProperty.ofImmutable(
        this, "scenarioCount", ScenarioMarketData.class, Integer.TYPE);
    /**
     * The meta-property for the {@code valuationDates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<LocalDate>> valuationDates = DirectMetaProperty.ofImmutable(
        this, "valuationDates", ScenarioMarketData.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableListMultimap<MarketDataId<?>, ?>> values = DirectMetaProperty.ofImmutable(
        this, "values", ScenarioMarketData.class, (Class) ImmutableListMultimap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ScenarioMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code globalValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> globalValues = DirectMetaProperty.ofImmutable(
        this, "globalValues", ScenarioMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "scenarioCount",
        "valuationDates",
        "values",
        "timeSeries",
        "globalValues");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1203198113:  // scenarioCount
          return scenarioCount;
        case -788641532:  // valuationDates
          return valuationDates;
        case -823812830:  // values
          return values;
        case 779431844:  // timeSeries
          return timeSeries;
        case -591591771:  // globalValues
          return globalValues;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ScenarioMarketData> builder() {
      return new ScenarioMarketData.Builder();
    }

    @Override
    public Class<? extends ScenarioMarketData> beanType() {
      return ScenarioMarketData.class;
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
    public final MetaProperty<Integer> scenarioCount() {
      return scenarioCount;
    }

    /**
     * The meta-property for the {@code valuationDates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ImmutableList<LocalDate>> valuationDates() {
      return valuationDates;
    }

    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ImmutableListMultimap<MarketDataId<?>, ?>> values() {
      return values;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code globalValues} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> globalValues() {
      return globalValues;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1203198113:  // scenarioCount
          return ((ScenarioMarketData) bean).getScenarioCount();
        case -788641532:  // valuationDates
          return ((ScenarioMarketData) bean).getValuationDates();
        case -823812830:  // values
          return ((ScenarioMarketData) bean).getValues();
        case 779431844:  // timeSeries
          return ((ScenarioMarketData) bean).getTimeSeries();
        case -591591771:  // globalValues
          return ((ScenarioMarketData) bean).getGlobalValues();
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
   * The bean-builder for {@code ScenarioMarketData}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<ScenarioMarketData> {

    private int scenarioCount;
    private List<LocalDate> valuationDates = ImmutableList.of();
    private ListMultimap<MarketDataId<?>, ?> values = ImmutableListMultimap.of();
    private Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Object> globalValues = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1203198113:  // scenarioCount
          return scenarioCount;
        case -788641532:  // valuationDates
          return valuationDates;
        case -823812830:  // values
          return values;
        case 779431844:  // timeSeries
          return timeSeries;
        case -591591771:  // globalValues
          return globalValues;
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
        case -788641532:  // valuationDates
          this.valuationDates = (List<LocalDate>) newValue;
          break;
        case -823812830:  // values
          this.values = (ListMultimap<MarketDataId<?>, ?>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<ObservableId, LocalDateDoubleTimeSeries>) newValue;
          break;
        case -591591771:  // globalValues
          this.globalValues = (Map<? extends MarketDataId<?>, Object>) newValue;
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
    public ScenarioMarketData build() {
      return new ScenarioMarketData(
          scenarioCount,
          valuationDates,
          values,
          timeSeries,
          globalValues);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ScenarioMarketData.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount)).append(',').append(' ');
      buf.append("valuationDates").append('=').append(JodaBeanUtils.toString(valuationDates)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("globalValues").append('=').append(JodaBeanUtils.toString(globalValues)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
