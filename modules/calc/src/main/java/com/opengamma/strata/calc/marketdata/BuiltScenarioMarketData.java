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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Market data that has been built.
 * <p>
 * The {@link MarketDataFactory} can be used to build market data from external
 * sources and by calibration. This implementation of {@link ScenarioMarketData}
 * provides the result, and includes all the market data, such as quotes and curves.
 * <p>
 * This implementation differs from {@link ImmutableScenarioMarketData} because it
 * stores the failures that occurred during the build process.
 * These errors are exposed to users when data is queried.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class BuiltScenarioMarketData
    implements ScenarioMarketData, ImmutableBean {

  /** An instance containing no market data. */
  private static final BuiltScenarioMarketData EMPTY = new BuiltScenarioMarketData(
      ImmutableScenarioMarketData.empty(), ImmutableMap.of(), ImmutableMap.of());

  /**
   * The underlying market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableScenarioMarketData underlying;
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
   * Creates a mutable builder that can be used to create an instance of the market data.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return the mutable builder
   */
  static BuiltScenarioMarketDataBuilder builder(LocalDate valuationDate) {
    return new BuiltScenarioMarketDataBuilder(valuationDate);
  }

  /**
   * Creates a mutable builder that can be used to create an instance of the market data.
   *
   * @param valuationDate  the valuation dates associated with the market data, one for each scenario
   * @return the mutable builder
   */
  static BuiltScenarioMarketDataBuilder builder(MarketDataBox<LocalDate> valuationDate) {
    return new BuiltScenarioMarketDataBuilder(valuationDate);
  }

  /**
   * Returns an empty set of market data.
   *
   * @return an empty set of market data
   */
  static BuiltScenarioMarketData empty() {
    return EMPTY;
  }

  //-------------------------------------------------------------------------
  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return underlying.getValuationDate();
  }

  @Override
  public int getScenarioCount() {
    return underlying.getScenarioCount();
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    return underlying.containsValue(id);
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
    Optional<MarketDataBox<T>> opt = underlying.findValue(id);
    if (!opt.isPresent()) {
      Failure failure = valueFailures.get(id);
      if (failure != null) {
        throw new FailureException(failure);
      }
      throw new MarketDataNotFoundException(Messages.format(
          "Market data not found for identifier '{}' of type '{}'", id, id.getClass().getSimpleName()));
    }
    return opt.get();
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    return underlying.findValue(id);
  }

  @Override
  public Set<MarketDataId<?>> getIds() {
    return underlying.getIds();
  }

  @Override
  public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
    return underlying.findIds(name);
  }

  @Override
  public Set<ObservableId> getTimeSeriesIds() {
    return underlying.getTimeSeriesIds();
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    return underlying.getTimeSeries(id);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BuiltScenarioMarketData}.
   * @return the meta-bean, not null
   */
  public static BuiltScenarioMarketData.Meta meta() {
    return BuiltScenarioMarketData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BuiltScenarioMarketData.Meta.INSTANCE);
  }

  /**
   * Creates an instance.
   * @param underlying  the value of the property, not null
   * @param valueFailures  the value of the property, not null
   * @param timeSeriesFailures  the value of the property, not null
   */
  BuiltScenarioMarketData(
      ImmutableScenarioMarketData underlying,
      Map<? extends MarketDataId<?>, Failure> valueFailures,
      Map<? extends MarketDataId<?>, Failure> timeSeriesFailures) {
    JodaBeanUtils.notNull(underlying, "underlying");
    JodaBeanUtils.notNull(valueFailures, "valueFailures");
    JodaBeanUtils.notNull(timeSeriesFailures, "timeSeriesFailures");
    this.underlying = underlying;
    this.valueFailures = ImmutableMap.copyOf(valueFailures);
    this.timeSeriesFailures = ImmutableMap.copyOf(timeSeriesFailures);
  }

  @Override
  public BuiltScenarioMarketData.Meta metaBean() {
    return BuiltScenarioMarketData.Meta.INSTANCE;
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
  public ImmutableScenarioMarketData getUnderlying() {
    return underlying;
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
      BuiltScenarioMarketData other = (BuiltScenarioMarketData) obj;
      return JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(valueFailures, other.valueFailures) &&
          JodaBeanUtils.equal(timeSeriesFailures, other.timeSeriesFailures);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(valueFailures);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeriesFailures);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("BuiltScenarioMarketData{");
    buf.append("underlying").append('=').append(underlying).append(',').append(' ');
    buf.append("valueFailures").append('=').append(valueFailures).append(',').append(' ');
    buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BuiltScenarioMarketData}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<ImmutableScenarioMarketData> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", BuiltScenarioMarketData.class, ImmutableScenarioMarketData.class);
    /**
     * The meta-property for the {@code valueFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> valueFailures = DirectMetaProperty.ofImmutable(
        this, "valueFailures", BuiltScenarioMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeriesFailures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<MarketDataId<?>, Failure>> timeSeriesFailures = DirectMetaProperty.ofImmutable(
        this, "timeSeriesFailures", BuiltScenarioMarketData.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlying",
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
        case -1770633379:  // underlying
          return underlying;
        case -68881222:  // valueFailures
          return valueFailures;
        case -1580093459:  // timeSeriesFailures
          return timeSeriesFailures;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BuiltScenarioMarketData> builder() {
      return new BuiltScenarioMarketData.Builder();
    }

    @Override
    public Class<? extends BuiltScenarioMarketData> beanType() {
      return BuiltScenarioMarketData.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableScenarioMarketData> underlying() {
      return underlying;
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
        case -1770633379:  // underlying
          return ((BuiltScenarioMarketData) bean).getUnderlying();
        case -68881222:  // valueFailures
          return ((BuiltScenarioMarketData) bean).getValueFailures();
        case -1580093459:  // timeSeriesFailures
          return ((BuiltScenarioMarketData) bean).getTimeSeriesFailures();
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
   * The bean-builder for {@code BuiltScenarioMarketData}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<BuiltScenarioMarketData> {

    private ImmutableScenarioMarketData underlying;
    private Map<? extends MarketDataId<?>, Failure> valueFailures = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, Failure> timeSeriesFailures = ImmutableMap.of();

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
        case -1770633379:  // underlying
          return underlying;
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
        case -1770633379:  // underlying
          this.underlying = (ImmutableScenarioMarketData) newValue;
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
    public BuiltScenarioMarketData build() {
      return new BuiltScenarioMarketData(
          underlying,
          valueFailures,
          timeSeriesFailures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("BuiltScenarioMarketData.Builder{");
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("valueFailures").append('=').append(JodaBeanUtils.toString(valueFailures)).append(',').append(' ');
      buf.append("timeSeriesFailures").append('=').append(JodaBeanUtils.toString(timeSeriesFailures));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
