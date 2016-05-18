/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

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
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataNotFoundException;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.ScenarioMarketData;
import com.opengamma.strata.calc.ImmutableScenarioMarketData;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A set of market data.
 * <p>
 * A market environment contains all the data required to perform calculations. This includes the market data
 * a user would expect to see, for example quotes and curves, and also other values derived from market data.
 * <p>
 * The derived values include objects used in calculations encapsulating market data and logic that operates on
 * it, and objects with market data and metadata required by the scenario framework.
 *
 * @see MarketDataFactory
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class MarketEnvironment
    implements ScenarioMarketData, ImmutableBean {

  /** An instance containing no market data. */
  private static final MarketEnvironment EMPTY = new MarketEnvironment(
      ImmutableScenarioMarketData.empty(), ImmutableMap.of(), ImmutableMap.of());

  /**
   * The underlying market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableScenarioMarketData marketData;
  /**
   * The failures when building single market data values.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> valueFailures;
  /**
   * The failures that occurred when building time series of market data values.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends MarketDataId<?>, Failure>")
  private final ImmutableMap<MarketDataId<?>, Failure> timeSeriesFailures;

  //-------------------------------------------------------------------------
  /**
   * Returns a mutable builder, with the valuation date set, for building a new instance of {@code MarketEnvironment}.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return an mutable builder for building a new instance of {@code MarketEnvironment}
   */
  static MarketEnvironmentBuilder builder(LocalDate valuationDate) {
    return new MarketEnvironmentBuilder(valuationDate);
  }

  /**
   * Returns a mutable builder, with the valuation date set, for building a new instance of {@code MarketEnvironment}.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return an mutable builder for building a new instance of {@code MarketEnvironment}
   */
  static MarketEnvironmentBuilder builder(MarketDataBox<LocalDate> valuationDate) {
    return new MarketEnvironmentBuilder(valuationDate);
  }

  /**
   * Returns an empty set of market data.
   *
   * @return an empty set of market data
   */
  static MarketEnvironment empty() {
    return EMPTY;
  }

  //-------------------------------------------------------------------------
  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return marketData.getValuationDate();
  }

  @Override
  public int getScenarioCount() {
    return marketData.getScenarioCount();
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    return marketData.containsValue(id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    // this code exists to ensure that the error messages from market data building
    // are exposed to users when the failures are not checked

    // a special case for FX rates containing the same currency twice
    if (id instanceof FxRateId && ((FxRateId) id).getPair().isIdentity()) {
      FxRateId fxRateId = (FxRateId) id;
      FxRate identityRate = FxRate.of(fxRateId.getPair(), 1);
      return MarketDataBox.ofSingleValue((T) identityRate);
    }

    // find the data and check it against the failures
    Optional<MarketDataBox<T>> opt = marketData.findValue(id);
    if (!opt.isPresent()) {
      Failure failure = valueFailures.get(id);
      if (failure != null) {
        throw new FailureException(failure);
      }
      throw new MarketDataNotFoundException(Messages.format(
          "Market data not found for identifier '{}' of type '{}'", id, id.getClass().getSimpleName()));
    }
    @SuppressWarnings("unchecked")
    MarketDataBox<T> value = (MarketDataBox<T>) opt.get();
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
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    return marketData.findValue(id);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    return marketData.getTimeSeries(id);
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
   * @param marketData  the value of the property, not null
   * @param valueFailures  the value of the property, not null
   * @param timeSeriesFailures  the value of the property, not null
   */
  MarketEnvironment(
      ImmutableScenarioMarketData marketData,
      Map<? extends MarketDataId<?>, Failure> valueFailures,
      Map<? extends MarketDataId<?>, Failure> timeSeriesFailures) {
    JodaBeanUtils.notNull(marketData, "marketData");
    JodaBeanUtils.notNull(valueFailures, "valueFailures");
    JodaBeanUtils.notNull(timeSeriesFailures, "timeSeriesFailures");
    this.marketData = marketData;
    this.valueFailures = ImmutableMap.copyOf(valueFailures);
    this.timeSeriesFailures = ImmutableMap.copyOf(timeSeriesFailures);
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
   * Gets the underlying market data.
   * @return the value of the property, not null
   */
  public ImmutableScenarioMarketData getMarketData() {
    return marketData;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the failures when building single market data values.
   * @return the value of the property, not null
   */
  public ImmutableMap<MarketDataId<?>, Failure> getValueFailures() {
    return valueFailures;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the failures that occurred when building time series of market data values.
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
      MarketEnvironment other = (MarketEnvironment) obj;
      return JodaBeanUtils.equal(marketData, other.marketData) &&
          JodaBeanUtils.equal(valueFailures, other.valueFailures) &&
          JodaBeanUtils.equal(timeSeriesFailures, other.timeSeriesFailures);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(marketData);
    hash = hash * 31 + JodaBeanUtils.hashCode(valueFailures);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeriesFailures);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("MarketEnvironment{");
    buf.append("marketData").append('=').append(marketData).append(',').append(' ');
    buf.append("valueFailures").append('=').append(valueFailures).append(',').append(' ');
    buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures));
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
     * The meta-property for the {@code marketData} property.
     */
    private final MetaProperty<ImmutableScenarioMarketData> marketData = DirectMetaProperty.ofImmutable(
        this, "marketData", MarketEnvironment.class, ImmutableScenarioMarketData.class);
    /**
     * The meta-property for the {@code valueFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> valueFailures = DirectMetaProperty.ofImmutable(
        this, "valueFailures", MarketEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeriesFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> timeSeriesFailures = DirectMetaProperty.ofImmutable(
        this, "timeSeriesFailures", MarketEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "marketData",
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
        case 1116764678:  // marketData
          return marketData;
        case -68881222:  // valueFailures
          return valueFailures;
        case -1580093459:  // timeSeriesFailures
          return timeSeriesFailures;
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
     * The meta-property for the {@code marketData} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableScenarioMarketData> marketData() {
      return marketData;
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
        case 1116764678:  // marketData
          return ((MarketEnvironment) bean).getMarketData();
        case -68881222:  // valueFailures
          return ((MarketEnvironment) bean).getValueFailures();
        case -1580093459:  // timeSeriesFailures
          return ((MarketEnvironment) bean).getTimeSeriesFailures();
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

    private ImmutableScenarioMarketData marketData;
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
        case 1116764678:  // marketData
          return marketData;
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
        case 1116764678:  // marketData
          this.marketData = (ImmutableScenarioMarketData) newValue;
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
    public MarketEnvironment build() {
      return new MarketEnvironment(
          marketData,
          valueFailures,
          timeSeriesFailures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("MarketEnvironment.Builder{");
      buf.append("marketData").append('=').append(JodaBeanUtils.toString(marketData)).append(',').append(' ');
      buf.append("valueFailures").append('=').append(JodaBeanUtils.toString(valueFailures)).append(',').append(' ');
      buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
