/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculation.MissingMappingId;
import com.opengamma.strata.engine.calculation.NoMatchingRuleId;

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
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class ScenarioCalculationEnvironment implements ImmutableBean, Serializable {

  /** The market data values which are the same in every scenario. */
  @PropertyDefinition(validate = "notNull")
  private final CalculationEnvironment sharedData;

  /** The number of scenarios. */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final int scenarioCount;

  /** The valuation dates of the scenarios, one for each scenario. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<LocalDate> valuationDates;

  // TODO Should there be separate maps for observable and non-observable data?
  /** Individual items of market data, keyed by ID, one for each scenario. */
  @PropertyDefinition(validate = "notNull", get = "private", builderType = "ListMultimap<? extends MarketDataId<?>, ?>")
  private final ImmutableListMultimap<MarketDataId<?>, ?> values;

  /** Market dat values that are potentially applicable across all scenarios, keyed by ID. */
  @PropertyDefinition(validate = "notNull", get = "private", builderType = "Map<? extends MarketDataId<?>, Object>")
  private final ImmutableMap<MarketDataId<?>, Object> globalValues;

  /** Details of failures when building single market data values. */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> singleValueFailures;

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
   * Returns a set of data for a single scenario, taking the data from an instance of {@link CalculationEnvironment}.
   *
   * @param marketData  a set of data for a single scenario
   * @return a set of scenario data for a single scenario taken from {@code marketData}
   */
  public static ScenarioCalculationEnvironment of(CalculationEnvironment marketData) {
    return new ScenarioCalculationEnvironment(
        marketData,
        1,
        ImmutableList.of(marketData.getValuationDate()),
        ImmutableListMultimap.of(),
        ImmutableMap.of(),
        ImmutableMap.of());
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

    if (!values.isEmpty()) {
      return (List<T>) values;
    }
    return Collections.nCopies(scenarioCount, sharedData.getValue(id));
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
    return sharedData.getTimeSeries(id);
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
      throw new IllegalArgumentException("No market data available for " + id);
    }
    return (T) value;
  }

  /**
   * Returns true if this set of data contains value for the specified ID in the shared data or the scenario data.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains values for the specified ID in the shared data or the scenario data
   */
  public boolean containsValues(MarketDataId<?> id) {
    return values.containsKey(id) || sharedData.containsValue(id);
  }

  /**
   * Returns true if this set of data contains value for the specified ID in the scenario data.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains values for the specified ID in the scenario data
   */
  public boolean containsScenarioValues(MarketDataId<?> id) {
    return values.containsKey(id);
  }

  /**
   * Returns true if this set of data contains a value for the specified ID in the shared data.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a value for the specified ID in the shared data
   */
  public boolean containsSharedValue(MarketDataId<?> id) {
    return sharedData.containsValue(id);
  }

  /**
   * Returns true if this set of data contains a time series for the specified market data ID.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a time series for the specified market data ID
   */
  public boolean containsTimeSeries(ObservableId id) {
    return sharedData.containsTimeSeries(id);
  }

  /**
   * Returns a mutable builder containing the data from this object.
   *
   * @return a mutable builder containing the data from this object
   */
  public ScenarioCalculationEnvironmentBuilder toBuilder() {
    return new ScenarioCalculationEnvironmentBuilder(
        sharedData,
        scenarioCount,
        valuationDates,
        values,
        globalValues,
        singleValueFailures);
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

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param sharedData  the value of the property, not null
   * @param scenarioCount  the value of the property
   * @param valuationDates  the value of the property, not null
   * @param values  the value of the property, not null
   * @param globalValues  the value of the property, not null
   * @param singleValueFailures  the value of the property, not null
   */
  ScenarioCalculationEnvironment(
      CalculationEnvironment sharedData,
      int scenarioCount,
      List<LocalDate> valuationDates,
      ListMultimap<? extends MarketDataId<?>, ?> values,
      Map<? extends MarketDataId<?>, Object> globalValues,
      Map<? extends MarketDataId<?>, Failure> singleValueFailures) {
    JodaBeanUtils.notNull(sharedData, "sharedData");
    ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
    JodaBeanUtils.notNull(valuationDates, "valuationDates");
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(globalValues, "globalValues");
    JodaBeanUtils.notNull(singleValueFailures, "singleValueFailures");
    this.sharedData = sharedData;
    this.scenarioCount = scenarioCount;
    this.valuationDates = ImmutableList.copyOf(valuationDates);
    this.values = ImmutableListMultimap.copyOf(values);
    this.globalValues = ImmutableMap.copyOf(globalValues);
    this.singleValueFailures = ImmutableMap.copyOf(singleValueFailures);
    validate();
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
  public CalculationEnvironment getSharedData() {
    return sharedData;
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
   * Gets market dat values that are potentially applicable across all scenarios, keyed by ID.
   * @return the value of the property, not null
   */
  private ImmutableMap<MarketDataId<?>, Object> getGlobalValues() {
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ScenarioCalculationEnvironment other = (ScenarioCalculationEnvironment) obj;
      return JodaBeanUtils.equal(getSharedData(), other.getSharedData()) &&
          (getScenarioCount() == other.getScenarioCount()) &&
          JodaBeanUtils.equal(getValuationDates(), other.getValuationDates()) &&
          JodaBeanUtils.equal(getValues(), other.getValues()) &&
          JodaBeanUtils.equal(getGlobalValues(), other.getGlobalValues()) &&
          JodaBeanUtils.equal(getSingleValueFailures(), other.getSingleValueFailures());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSharedData());
    hash = hash * 31 + JodaBeanUtils.hashCode(getScenarioCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getGlobalValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSingleValueFailures());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ScenarioCalculationEnvironment{");
    buf.append("sharedData").append('=').append(getSharedData()).append(',').append(' ');
    buf.append("scenarioCount").append('=').append(getScenarioCount()).append(',').append(' ');
    buf.append("valuationDates").append('=').append(getValuationDates()).append(',').append(' ');
    buf.append("values").append('=').append(getValues()).append(',').append(' ');
    buf.append("globalValues").append('=').append(getGlobalValues()).append(',').append(' ');
    buf.append("singleValueFailures").append('=').append(JodaBeanUtils.toString(getSingleValueFailures()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ScenarioCalculationEnvironment}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code sharedData} property.
     */
    private final MetaProperty<CalculationEnvironment> sharedData = DirectMetaProperty.ofImmutable(
        this, "sharedData", ScenarioCalculationEnvironment.class, CalculationEnvironment.class);
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
     * The meta-property for the {@code globalValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Object>> globalValues = DirectMetaProperty.ofImmutable(
        this, "globalValues", ScenarioCalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code singleValueFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> singleValueFailures = DirectMetaProperty.ofImmutable(
        this, "singleValueFailures", ScenarioCalculationEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "sharedData",
        "scenarioCount",
        "valuationDates",
        "values",
        "globalValues",
        "singleValueFailures");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1784785489:  // sharedData
          return sharedData;
        case -1203198113:  // scenarioCount
          return scenarioCount;
        case -788641532:  // valuationDates
          return valuationDates;
        case -823812830:  // values
          return values;
        case -591591771:  // globalValues
          return globalValues;
        case -1633495726:  // singleValueFailures
          return singleValueFailures;
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
     * The meta-property for the {@code sharedData} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CalculationEnvironment> sharedData() {
      return sharedData;
    }

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
     * The meta-property for the {@code globalValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, Object>> globalValues() {
      return globalValues;
    }

    /**
     * The meta-property for the {@code singleValueFailures} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> singleValueFailures() {
      return singleValueFailures;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1784785489:  // sharedData
          return ((ScenarioCalculationEnvironment) bean).getSharedData();
        case -1203198113:  // scenarioCount
          return ((ScenarioCalculationEnvironment) bean).getScenarioCount();
        case -788641532:  // valuationDates
          return ((ScenarioCalculationEnvironment) bean).getValuationDates();
        case -823812830:  // values
          return ((ScenarioCalculationEnvironment) bean).getValues();
        case -591591771:  // globalValues
          return ((ScenarioCalculationEnvironment) bean).getGlobalValues();
        case -1633495726:  // singleValueFailures
          return ((ScenarioCalculationEnvironment) bean).getSingleValueFailures();
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
  private static final class Builder extends DirectFieldsBeanBuilder<ScenarioCalculationEnvironment> {

    private CalculationEnvironment sharedData;
    private int scenarioCount;
    private List<LocalDate> valuationDates = ImmutableList.of();
    private ListMultimap<? extends MarketDataId<?>, ?> values = ImmutableListMultimap.of();
    private Map<? extends MarketDataId<?>, Object> globalValues = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Failure> singleValueFailures = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1784785489:  // sharedData
          return sharedData;
        case -1203198113:  // scenarioCount
          return scenarioCount;
        case -788641532:  // valuationDates
          return valuationDates;
        case -823812830:  // values
          return values;
        case -591591771:  // globalValues
          return globalValues;
        case -1633495726:  // singleValueFailures
          return singleValueFailures;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1784785489:  // sharedData
          this.sharedData = (CalculationEnvironment) newValue;
          break;
        case -1203198113:  // scenarioCount
          this.scenarioCount = (Integer) newValue;
          break;
        case -788641532:  // valuationDates
          this.valuationDates = (List<LocalDate>) newValue;
          break;
        case -823812830:  // values
          this.values = (ListMultimap<? extends MarketDataId<?>, ?>) newValue;
          break;
        case -591591771:  // globalValues
          this.globalValues = (Map<? extends MarketDataId<?>, Object>) newValue;
          break;
        case -1633495726:  // singleValueFailures
          this.singleValueFailures = (Map<? extends MarketDataId<?>, Failure>) newValue;
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
          sharedData,
          scenarioCount,
          valuationDates,
          values,
          globalValues,
          singleValueFailures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("ScenarioCalculationEnvironment.Builder{");
      buf.append("sharedData").append('=').append(JodaBeanUtils.toString(sharedData)).append(',').append(' ');
      buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount)).append(',').append(' ');
      buf.append("valuationDates").append('=').append(JodaBeanUtils.toString(valuationDates)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("globalValues").append('=').append(JodaBeanUtils.toString(globalValues)).append(',').append(' ');
      buf.append("singleValueFailures").append('=').append(JodaBeanUtils.toString(singleValueFailures));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
