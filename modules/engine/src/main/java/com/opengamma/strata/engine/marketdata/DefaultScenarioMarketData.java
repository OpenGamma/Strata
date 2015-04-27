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
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.MarketDataKey;

/**
 * Basic definition of {@link ScenarioMarketData} that is a Joda bean.
 */
@SuppressWarnings("unchecked")
@BeanDefinition
public final class DefaultScenarioMarketData implements ScenarioMarketData, ImmutableBean {

  /** The number of scenarios. */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero", overrideGet = true)
  private final int scenarioCount;

  /** The valuation dates of the scenarios, one for each scenario. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ImmutableList<LocalDate> valuationDates;

  /** Individual items of market data, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableListMultimap<MarketDataId<?>, ?> values;

  /** Time series of observable market data values, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableListMultimap<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  /** Market dat values that are potentially applicable across all scenarios, keyed by ID. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<? extends MarketDataId<?>, Object> globalValues;

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        valuationDates.size() == scenarioCount,
        "The number of valuation dates must equal the number of scenarios");
    // TODO Check the sizes of all the values in the multimaps
  }

  @Override
  public <T, I extends MarketDataId<T>> List<T> getValues(I id) {
    // Special handling of these special ID types to provide more helpful error messages
    if (id instanceof NoMatchingRuleId) {
      MarketDataKey key = ((NoMatchingRuleId) id).getKey();
      throw new IllegalArgumentException("No market data rules were available to build the market data for " + key);
    }
    if (id instanceof MissingMappingId) {
      MarketDataKey<?> key = ((MissingMappingId) id).getKey();
      throw new IllegalArgumentException("No market data mapping found for market data " + key);
    }
    List<?> values = this.values.get(id);

    if (values == null) {
      throw new RuntimeException("No values available for market data ID " + id);
    }
    return (List<T>) values;
  }

  @Override
  public List<LocalDateDoubleTimeSeries> getTimeSeries(ObservableId id) {
    List<LocalDateDoubleTimeSeries> timeSeriesList = timeSeries.get(id);

    if (timeSeriesList == null) {
      throw new RuntimeException("No time series available for market data ID " + id);
    }
    return timeSeriesList;
  }

  @Override
  public <T, I extends MarketDataId<T>> T getGlobalValue(I id) {
    Object value = globalValues.get(id);

    if (value == null) {
      throw new RuntimeException("No values available for market data ID " + id);
    }
    return (T) value;
  }

  @Override
  public List<Observables> getObservables() {
    /*Map<? extends MarketDataId<?>, List<?>> observableValues =
        Seq.seq(values).filter(t -> t.v1().isObservable()).toMap(Tuple2::v1, Tuple2::v2);

    Map<? extends MarketDataId<?>, List<LocalDateDoubleTimeSeries>> observableTimeSeries =
        Seq.seq(timeSeries).filter(t -> t.v1().isObservable()).toMap(Tuple2::v1, Tuple2::v2);*/

    // TODO implement DefaultScenarioMarketData.getObservables
    throw new UnsupportedOperationException("getObservables not implemented");
  }

  @Override
  public List<MarketDataEnvironment> getMarketDataEnvironment() {
    // TODO implement DefaultScenarioMarketData.getMarketDataEnvironment
    throw new UnsupportedOperationException("getMarketDataEnvironment not implemented");
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultScenarioMarketData}.
   * @return the meta-bean, not null
   */
  public static DefaultScenarioMarketData.Meta meta() {
    return DefaultScenarioMarketData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DefaultScenarioMarketData.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DefaultScenarioMarketData.Builder builder() {
    return new DefaultScenarioMarketData.Builder();
  }

  private DefaultScenarioMarketData(
      int scenarioCount,
      List<LocalDate> valuationDates,
      ListMultimap<MarketDataId<?>, ?> values,
      ListMultimap<ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<? extends MarketDataId<?>, Object> globalValues) {
    ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
    JodaBeanUtils.notNull(valuationDates, "valuationDates");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(globalValues, "globalValues");
    this.scenarioCount = scenarioCount;
    this.valuationDates = ImmutableList.copyOf(valuationDates);
    this.values = ImmutableListMultimap.copyOf(values);
    this.timeSeries = ImmutableListMultimap.copyOf(timeSeries);
    this.globalValues = ImmutableMap.copyOf(globalValues);
    validate();
  }

  @Override
  public DefaultScenarioMarketData.Meta metaBean() {
    return DefaultScenarioMarketData.Meta.INSTANCE;
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
   * Gets the valuation dates of the scenarios, one for each scenario.
   * @return the value of the property, not null
   */
  @Override
  public ImmutableList<LocalDate> getValuationDates() {
    return valuationDates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets individual items of market data, keyed by ID, one for each scenario.
   * @return the value of the property, not null
   */
  public ImmutableListMultimap<MarketDataId<?>, ?> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets time series of observable market data values, keyed by ID, one for each scenario.
   * @return the value of the property, not null
   */
  public ImmutableListMultimap<ObservableId, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets market dat values that are potentially applicable across all scenarios, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<? extends MarketDataId<?>, Object> getGlobalValues() {
    return globalValues;
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
      DefaultScenarioMarketData other = (DefaultScenarioMarketData) obj;
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
    buf.append("DefaultScenarioMarketData{");
    buf.append("scenarioCount").append('=').append(getScenarioCount()).append(',').append(' ');
    buf.append("valuationDates").append('=').append(getValuationDates()).append(',').append(' ');
    buf.append("values").append('=').append(getValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("globalValues").append('=').append(JodaBeanUtils.toString(getGlobalValues()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DefaultScenarioMarketData}.
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
        this, "scenarioCount", DefaultScenarioMarketData.class, Integer.TYPE);
    /**
     * The meta-property for the {@code valuationDates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<LocalDate>> valuationDates = DirectMetaProperty.ofImmutable(
        this, "valuationDates", DefaultScenarioMarketData.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableListMultimap<MarketDataId<?>, ?>> values = DirectMetaProperty.ofImmutable(
        this, "values", DefaultScenarioMarketData.class, (Class) ImmutableListMultimap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableListMultimap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", DefaultScenarioMarketData.class, (Class) ImmutableListMultimap.class);
    /**
     * The meta-property for the {@code globalValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> globalValues = DirectMetaProperty.ofImmutable(
        this, "globalValues", DefaultScenarioMarketData.class, (Class) ImmutableMap.class);
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
    private Meta() {
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
    public DefaultScenarioMarketData.Builder builder() {
      return new DefaultScenarioMarketData.Builder();
    }

    @Override
    public Class<? extends DefaultScenarioMarketData> beanType() {
      return DefaultScenarioMarketData.class;
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
     * The meta-property for the {@code valuationDates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<LocalDate>> valuationDates() {
      return valuationDates;
    }

    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableListMultimap<MarketDataId<?>, ?>> values() {
      return values;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableListMultimap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code globalValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> globalValues() {
      return globalValues;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1203198113:  // scenarioCount
          return ((DefaultScenarioMarketData) bean).getScenarioCount();
        case -788641532:  // valuationDates
          return ((DefaultScenarioMarketData) bean).getValuationDates();
        case -823812830:  // values
          return ((DefaultScenarioMarketData) bean).getValues();
        case 779431844:  // timeSeries
          return ((DefaultScenarioMarketData) bean).getTimeSeries();
        case -591591771:  // globalValues
          return ((DefaultScenarioMarketData) bean).getGlobalValues();
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
   * The bean-builder for {@code DefaultScenarioMarketData}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<DefaultScenarioMarketData> {

    private int scenarioCount;
    private List<LocalDate> valuationDates = ImmutableList.of();
    private ListMultimap<MarketDataId<?>, ?> values = ImmutableListMultimap.of();
    private ListMultimap<ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableListMultimap.of();
    private Map<? extends MarketDataId<?>, Object> globalValues = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(DefaultScenarioMarketData beanToCopy) {
      this.scenarioCount = beanToCopy.getScenarioCount();
      this.valuationDates = beanToCopy.getValuationDates();
      this.values = beanToCopy.getValues();
      this.timeSeries = beanToCopy.getTimeSeries();
      this.globalValues = beanToCopy.getGlobalValues();
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
          this.timeSeries = (ListMultimap<ObservableId, LocalDateDoubleTimeSeries>) newValue;
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
    public DefaultScenarioMarketData build() {
      return new DefaultScenarioMarketData(
          scenarioCount,
          valuationDates,
          values,
          timeSeries,
          globalValues);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code scenarioCount} property in the builder.
     * @param scenarioCount  the new value
     * @return this, for chaining, not null
     */
    public Builder scenarioCount(int scenarioCount) {
      ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
      this.scenarioCount = scenarioCount;
      return this;
    }

    /**
     * Sets the {@code valuationDates} property in the builder.
     * @param valuationDates  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDates(List<LocalDate> valuationDates) {
      JodaBeanUtils.notNull(valuationDates, "valuationDates");
      this.valuationDates = valuationDates;
      return this;
    }

    /**
     * Sets the {@code valuationDates} property in the builder
     * from an array of objects.
     * @param valuationDates  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDates(LocalDate... valuationDates) {
      return valuationDates(ImmutableList.copyOf(valuationDates));
    }

    /**
     * Sets the {@code values} property in the builder.
     * @param values  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder values(ListMultimap<MarketDataId<?>, ?> values) {
      JodaBeanUtils.notNull(values, "values");
      this.values = values;
      return this;
    }

    /**
     * Sets the {@code timeSeries} property in the builder.
     * @param timeSeries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeSeries(ListMultimap<ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
      JodaBeanUtils.notNull(timeSeries, "timeSeries");
      this.timeSeries = timeSeries;
      return this;
    }

    /**
     * Sets the {@code globalValues} property in the builder.
     * @param globalValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder globalValues(Map<? extends MarketDataId<?>, Object> globalValues) {
      JodaBeanUtils.notNull(globalValues, "globalValues");
      this.globalValues = globalValues;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("DefaultScenarioMarketData.Builder{");
      buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount)).append(',').append(' ');
      buf.append("valuationDates").append('=').append(JodaBeanUtils.toString(valuationDates)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("globalValues").append('=').append(JodaBeanUtils.toString(globalValues));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
