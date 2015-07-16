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
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;

// TODO Delegate to a CalculationEnvironment containing base data if this doesn't contain values for an ID
/**
 * A set of market data used for performing calculations across a set of scenarios.
 * <p>
 * A scenario calculation environment contains all the data required to perform calculations. This includes the
 * market data a user would expect to see, for example quotes and curves, and also other values
 * derived from market data.
 * <p>
 * The derived values include objects used in calculations encapsulating market data and logic that operates on
 * it, and objects with market data and metadata required by the scenario framework.
 * <p>
 * {@code ScenarioCalculationEnvironment} should be considered an implementation detail and is not intended as a
 * user-facing data structure.
 */
@SuppressWarnings("unchecked")
@BeanDefinition(builderScope = "private")
public class ScenarioCalculationEnvironment implements ImmutableBean {

  /** The market data values which are the same in every scenario. */
  @PropertyDefinition(validate = "notNull")
  private final CalculationEnvironment baseData;

  /** The number of scenarios. */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final int scenarioCount;

  /** The valuation dates of the scenarios, one for each scenario. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<LocalDate> valuationDates;

  // TODO Should there be separate maps for observable and non-observable data?
  /** Individual items of market data, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableListMultimap<MarketDataId<?>, ?> values;

  /** Time series of observable market data values, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

  /** Market dat values that are potentially applicable across all scenarios, keyed by ID. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<? extends MarketDataId<?>, Object> globalValues;

  /** Details of failures when building single market data values. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<MarketDataId<?>, Failure> singleValueFailures;

  /** Details of failures when building time series of market data values. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<MarketDataId<?>, Failure> timeSeriesFailures;

  /**
   * Returns a mutable builder for building a set of scenario market data where every scenario has the
   * same valuation date.
   *
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation date of all scenarios
   * @return a mutable builder for building a set of scenario market data
   */
  public static ScenarioCalculationEnvironmentBuilder builder(int scenarioCount, LocalDate valuationDate) {
    return new ScenarioCalculationEnvironmentBuilder(scenarioCount, valuationDate);
  }

  /**
   * Returns a set of data for a single scenario, taking the data from an instance of {@link MarketEnvironment}.
   *
   * @param marketData  a set of data for a single scenario
   * @return a set of scenario data for a single scenario taken from {@code marketData}
   */
  public static ScenarioCalculationEnvironment of(CalculationEnvironment marketData) {
    // TODO Can the argument can just become the base data?
    ScenarioCalculationEnvironmentBuilder builder = builder(1, marketData.getValuationDate());
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
   * @param singleValueFailures  the single value failures
   * @param timeSeriesFailures  the time-series failures
   */
  @ImmutableConstructor
  ScenarioCalculationEnvironment(
      CalculationEnvironment baseData,
      int scenarioCount,
      List<LocalDate> valuationDates,
      ListMultimap<MarketDataId<?>, ?> values,
      Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<? extends MarketDataId<?>, Object> globalValues,
      Map<MarketDataId<?>, Failure> singleValueFailures,
      Map<MarketDataId<?>, Failure> timeSeriesFailures) {

    ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
    JodaBeanUtils.notNull(baseData, "baseData");
    JodaBeanUtils.notNull(valuationDates, "valuationDates");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(globalValues, "globalValues");
    this.baseData = baseData;
    this.scenarioCount = scenarioCount;
    this.valuationDates = ImmutableList.copyOf(valuationDates);
    this.values = ImmutableListMultimap.copyOf(values);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.globalValues = ImmutableMap.copyOf(globalValues);
    this.singleValueFailures = ImmutableMap.copyOf(singleValueFailures);
    this.timeSeriesFailures = ImmutableMap.copyOf(timeSeriesFailures);
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
   * @return a list of market data values, one from each scenario
   * @throws IllegalArgumentException if there are no values for the specified ID
   */
  public <T> List<T> getValues(MarketDataId<T> id) {
    // Special handling of these special ID types to provide more helpful error messages
    if (id instanceof NoMatchingRuleId) {
      MarketDataKey<?> key = ((NoMatchingRuleId) id).getKey();
      throw new IllegalArgumentException("No market data rules were available to build the market data for " + key);
    }
    if (id instanceof MissingMappingId) {
      MarketDataKey<?> key = ((MissingMappingId) id).getKey();
      throw new IllegalArgumentException("No market data mapping found for " + key);
    }
    List<?> values = this.values.get(id);

    // TODO Try the base data
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
   * @return the market data value
   * @throws IllegalArgumentException if there is no value for the specified ID
   */
  public <T> T getGlobalValue(MarketDataId<T> id) {
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
  public ScenarioCalculationEnvironmentBuilder toBuilder() {
    return new ScenarioCalculationEnvironmentBuilder(
        baseData,
        scenarioCount,
        valuationDates,
        values,
        timeSeries,
        globalValues,
        singleValueFailures,
        timeSeriesFailures);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ScenarioCalculationEnvironment}.
   * @return the meta-bean, not null
   */
  public static ScenarioCalculationEnvironment.Meta meta() {
    return ScenarioCalculationEnvironment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ScenarioCalculationEnvironment.Meta.INSTANCE);
  }

  @Override
  public ScenarioCalculationEnvironment.Meta metaBean() {
    return ScenarioCalculationEnvironment.Meta.INSTANCE;
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
   * Gets the market data values which are the same in every scenario.
   * @return the value of the property, not null
   */
  public CalculationEnvironment getBaseData() {
    return baseData;
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
      ScenarioCalculationEnvironment other = (ScenarioCalculationEnvironment) obj;
      return JodaBeanUtils.equal(getBaseData(), other.getBaseData()) &&
          (getScenarioCount() == other.getScenarioCount()) &&
          JodaBeanUtils.equal(getValuationDates(), other.getValuationDates()) &&
          JodaBeanUtils.equal(getValues(), other.getValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getGlobalValues(), other.getGlobalValues()) &&
          JodaBeanUtils.equal(getSingleValueFailures(), other.getSingleValueFailures()) &&
          JodaBeanUtils.equal(getTimeSeriesFailures(), other.getTimeSeriesFailures());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseData());
    hash = hash * 31 + JodaBeanUtils.hashCode(getScenarioCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getGlobalValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSingleValueFailures());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeriesFailures());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("ScenarioCalculationEnvironment{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("baseData").append('=').append(JodaBeanUtils.toString(getBaseData())).append(',').append(' ');
    buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(getScenarioCount())).append(',').append(' ');
    buf.append("valuationDates").append('=').append(JodaBeanUtils.toString(getValuationDates())).append(',').append(' ');
    buf.append("values").append('=').append(JodaBeanUtils.toString(getValues())).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries())).append(',').append(' ');
    buf.append("globalValues").append('=').append(JodaBeanUtils.toString(getGlobalValues())).append(',').append(' ');
    buf.append("singleValueFailures").append('=').append(JodaBeanUtils.toString(getSingleValueFailures())).append(',').append(' ');
    buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(getTimeSeriesFailures())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ScenarioCalculationEnvironment}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code baseData} property.
     */
    private final MetaProperty<CalculationEnvironment> baseData = DirectMetaProperty.ofImmutable(
        this, "baseData", ScenarioCalculationEnvironment.class, CalculationEnvironment.class);
    /**
     * The meta-property for the {@code scenarioCount} property.
     */
    private final MetaProperty<Integer> scenarioCount = DirectMetaProperty.ofImmutable(
        this, "scenarioCount", ScenarioCalculationEnvironment.class, Integer.TYPE);
    /**
     * The meta-property for the {@code valuationDates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<LocalDate>> valuationDates = DirectMetaProperty.ofImmutable(
        this, "valuationDates", ScenarioCalculationEnvironment.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableListMultimap<MarketDataId<?>, ?>> values = DirectMetaProperty.ofImmutable(
        this, "values", ScenarioCalculationEnvironment.class, (Class) ImmutableListMultimap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ObservableId, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ScenarioCalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code globalValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> globalValues = DirectMetaProperty.ofImmutable(
        this, "globalValues", ScenarioCalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code singleValueFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> singleValueFailures = DirectMetaProperty.ofImmutable(
        this, "singleValueFailures", ScenarioCalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeriesFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> timeSeriesFailures = DirectMetaProperty.ofImmutable(
        this, "timeSeriesFailures", ScenarioCalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "baseData",
        "scenarioCount",
        "valuationDates",
        "values",
        "timeSeries",
        "globalValues",
        "singleValueFailures",
        "timeSeriesFailures");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1721984485:  // baseData
          return baseData;
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
        case -1633495726:  // singleValueFailures
          return singleValueFailures;
        case -1580093459:  // timeSeriesFailures
          return timeSeriesFailures;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ScenarioCalculationEnvironment> builder() {
      return new ScenarioCalculationEnvironment.Builder();
    }

    @Override
    public Class<? extends ScenarioCalculationEnvironment> beanType() {
      return ScenarioCalculationEnvironment.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code baseData} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CalculationEnvironment> baseData() {
      return baseData;
    }

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

    /**
     * The meta-property for the {@code singleValueFailures} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> singleValueFailures() {
      return singleValueFailures;
    }

    /**
     * The meta-property for the {@code timeSeriesFailures} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> timeSeriesFailures() {
      return timeSeriesFailures;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1721984485:  // baseData
          return ((ScenarioCalculationEnvironment) bean).getBaseData();
        case -1203198113:  // scenarioCount
          return ((ScenarioCalculationEnvironment) bean).getScenarioCount();
        case -788641532:  // valuationDates
          return ((ScenarioCalculationEnvironment) bean).getValuationDates();
        case -823812830:  // values
          return ((ScenarioCalculationEnvironment) bean).getValues();
        case 779431844:  // timeSeries
          return ((ScenarioCalculationEnvironment) bean).getTimeSeries();
        case -591591771:  // globalValues
          return ((ScenarioCalculationEnvironment) bean).getGlobalValues();
        case -1633495726:  // singleValueFailures
          return ((ScenarioCalculationEnvironment) bean).getSingleValueFailures();
        case -1580093459:  // timeSeriesFailures
          return ((ScenarioCalculationEnvironment) bean).getTimeSeriesFailures();
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
   * The bean-builder for {@code ScenarioCalculationEnvironment}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<ScenarioCalculationEnvironment> {

    private CalculationEnvironment baseData;
    private int scenarioCount;
    private List<LocalDate> valuationDates = ImmutableList.of();
    private ListMultimap<MarketDataId<?>, ?> values = ImmutableListMultimap.of();
    private Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Object> globalValues = ImmutableMap.of();
    private Map<MarketDataId<?>, Failure> singleValueFailures = ImmutableMap.of();
    private Map<MarketDataId<?>, Failure> timeSeriesFailures = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1721984485:  // baseData
          return baseData;
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
        case -1721984485:  // baseData
          this.baseData = (CalculationEnvironment) newValue;
          break;
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
        case -1633495726:  // singleValueFailures
          this.singleValueFailures = (Map<MarketDataId<?>, Failure>) newValue;
          break;
        case -1580093459:  // timeSeriesFailures
          this.timeSeriesFailures = (Map<MarketDataId<?>, Failure>) newValue;
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
    public ScenarioCalculationEnvironment build() {
      return new ScenarioCalculationEnvironment(
          baseData,
          scenarioCount,
          valuationDates,
          values,
          timeSeries,
          globalValues,
          singleValueFailures,
          timeSeriesFailures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("ScenarioCalculationEnvironment.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("baseData").append('=').append(JodaBeanUtils.toString(baseData)).append(',').append(' ');
      buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount)).append(',').append(' ');
      buf.append("valuationDates").append('=').append(JodaBeanUtils.toString(valuationDates)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("globalValues").append('=').append(JodaBeanUtils.toString(globalValues)).append(',').append(' ');
      buf.append("singleValueFailures").append('=').append(JodaBeanUtils.toString(singleValueFailures)).append(',').append(' ');
      buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
