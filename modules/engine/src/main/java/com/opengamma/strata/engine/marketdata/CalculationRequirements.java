/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;

/**
 * A collection of market data IDs specifying the market data required for performing a set of calculations.
 */
@BeanDefinition(builderScope = "private")
public final class CalculationRequirements implements ImmutableBean {

  /** A set of requirements which specifies that no market data is required. */
  private static final CalculationRequirements EMPTY = CalculationRequirements.builder().build();

  /** Keys identifying the market data values required for the calculations. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<ObservableId> observables;

  /** Keys identifying the market data values required for the calculations. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<MarketDataId<?>> nonObservables;

  /** Keys identifying the time series of market data values required for the calculations. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<ObservableId> timeSeries;

  /**
   * The currencies in the calculation results. The market data must include FX rates in the
   * to allow conversion into the reporting currency. The FX rates must have the output currency as the base
   * currency and the reporting currency as the counter currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<Currency> outputCurrencies;

  /** The local scenarios required by the calculations. */
  @PropertyDefinition(validate = "notNull")
  private final LocalScenarioDefinitions localScenarios;

  /**
   * Returns an empty mutable builder for building up a set of requirements.
   *
   * @return an empty mutable builder for building up a set of requirements
   */
  public static CalculationRequirementsBuilder builder() {
    return new CalculationRequirementsBuilder();
  }

  /**
   * Returns a set of requirements specifying that no market data is required.
   *
   * @return a set of requirements specifying that no market data is required
   */
  public static CalculationRequirements empty() {
    return EMPTY;
  }

  /**
   * Returns a set of calculation requirements built from a set of market data requirements.
   *
   * @param marketDataRequirements  a set of requirements for market data
   * @return a set of calculation requirements built from a set of market data requirements
   */
  public static CalculationRequirements of(MarketDataRequirements marketDataRequirements) {
    return CalculationRequirements.builder()
        .addValues(marketDataRequirements.getObservables())
        .addValues(marketDataRequirements.getNonObservables())
        .addTimeSeries(marketDataRequirements.getTimeSeries())
        .build();
  }

  /**
   * Merges multiple sets of requirements into a single set.
   *
   * @param taskRequirements  market data requirements
   * @return a single set of requirements containing all the requirements from the input sets
   */
  public static CalculationRequirements of(List<CalculationTaskRequirements> taskRequirements) {
    ImmutableSet.Builder<ObservableId> observablesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<MarketDataId<?>> nonObservablesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<ObservableId> timeSeriesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<Currency> outputCurrenciesBuilder = ImmutableSet.builder();

    for (CalculationTaskRequirements req : taskRequirements) {
      observablesBuilder.addAll(req.getObservables());
      nonObservablesBuilder.addAll(req.getNonObservables());
      timeSeriesBuilder.addAll(req.getTimeSeries());
      outputCurrenciesBuilder.addAll(req.getOutputCurrencies());
    }
    return new CalculationRequirements(
        observablesBuilder.build(),
        nonObservablesBuilder.build(),
        timeSeriesBuilder.build(),
        outputCurrenciesBuilder.build(),
        LocalScenarioDefinitions.of(taskRequirements));
  }

  // package-private constructor, used by the builder
  @ImmutableConstructor
  CalculationRequirements(
      Set<? extends ObservableId> observables,
      Set<? extends MarketDataId<?>> nonObservables,
      Set<ObservableId> timeSeries,
      Set<Currency> outputCurrencies,
      LocalScenarioDefinitions localScenarios) {

    this.observables = ImmutableSet.copyOf(observables);
    this.nonObservables = ImmutableSet.copyOf(nonObservables);
    this.timeSeries = ImmutableSet.copyOf(timeSeries);
    this.outputCurrencies = ImmutableSet.copyOf(outputCurrencies);
    this.localScenarios = localScenarios;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationRequirements}.
   * @return the meta-bean, not null
   */
  public static CalculationRequirements.Meta meta() {
    return CalculationRequirements.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CalculationRequirements.Meta.INSTANCE);
  }

  @Override
  public CalculationRequirements.Meta metaBean() {
    return CalculationRequirements.Meta.INSTANCE;
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
   * Gets keys identifying the market data values required for the calculations.
   * @return the value of the property, not null
   */
  public ImmutableSet<ObservableId> getObservables() {
    return observables;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets keys identifying the market data values required for the calculations.
   * @return the value of the property, not null
   */
  public ImmutableSet<MarketDataId<?>> getNonObservables() {
    return nonObservables;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets keys identifying the time series of market data values required for the calculations.
   * @return the value of the property, not null
   */
  public ImmutableSet<ObservableId> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currencies in the calculation results. The market data must include FX rates in the
   * to allow conversion into the reporting currency. The FX rates must have the output currency as the base
   * currency and the reporting currency as the counter currency.
   * @return the value of the property, not null
   */
  public ImmutableSet<Currency> getOutputCurrencies() {
    return outputCurrencies;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the local scenarios required by the calculations.
   * @return the value of the property, not null
   */
  public LocalScenarioDefinitions getLocalScenarios() {
    return localScenarios;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationRequirements other = (CalculationRequirements) obj;
      return JodaBeanUtils.equal(getObservables(), other.getObservables()) &&
          JodaBeanUtils.equal(getNonObservables(), other.getNonObservables()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getOutputCurrencies(), other.getOutputCurrencies()) &&
          JodaBeanUtils.equal(getLocalScenarios(), other.getLocalScenarios());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getObservables());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNonObservables());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getOutputCurrencies());
    hash = hash * 31 + JodaBeanUtils.hashCode(getLocalScenarios());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CalculationRequirements{");
    buf.append("observables").append('=').append(getObservables()).append(',').append(' ');
    buf.append("nonObservables").append('=').append(getNonObservables()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("outputCurrencies").append('=').append(getOutputCurrencies()).append(',').append(' ');
    buf.append("localScenarios").append('=').append(JodaBeanUtils.toString(getLocalScenarios()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CalculationRequirements}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code observables} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<ObservableId>> observables = DirectMetaProperty.ofImmutable(
        this, "observables", CalculationRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code nonObservables} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<MarketDataId<?>>> nonObservables = DirectMetaProperty.ofImmutable(
        this, "nonObservables", CalculationRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<ObservableId>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", CalculationRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code outputCurrencies} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<Currency>> outputCurrencies = DirectMetaProperty.ofImmutable(
        this, "outputCurrencies", CalculationRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code localScenarios} property.
     */
    private final MetaProperty<LocalScenarioDefinitions> localScenarios = DirectMetaProperty.ofImmutable(
        this, "localScenarios", CalculationRequirements.class, LocalScenarioDefinitions.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observables",
        "nonObservables",
        "timeSeries",
        "outputCurrencies",
        "localScenarios");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 121811856:  // observables
          return observables;
        case 824041091:  // nonObservables
          return nonObservables;
        case 779431844:  // timeSeries
          return timeSeries;
        case -1022597040:  // outputCurrencies
          return outputCurrencies;
        case 101869496:  // localScenarios
          return localScenarios;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CalculationRequirements> builder() {
      return new CalculationRequirements.Builder();
    }

    @Override
    public Class<? extends CalculationRequirements> beanType() {
      return CalculationRequirements.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code observables} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<ObservableId>> observables() {
      return observables;
    }

    /**
     * The meta-property for the {@code nonObservables} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<MarketDataId<?>>> nonObservables() {
      return nonObservables;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<ObservableId>> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code outputCurrencies} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<Currency>> outputCurrencies() {
      return outputCurrencies;
    }

    /**
     * The meta-property for the {@code localScenarios} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalScenarioDefinitions> localScenarios() {
      return localScenarios;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 121811856:  // observables
          return ((CalculationRequirements) bean).getObservables();
        case 824041091:  // nonObservables
          return ((CalculationRequirements) bean).getNonObservables();
        case 779431844:  // timeSeries
          return ((CalculationRequirements) bean).getTimeSeries();
        case -1022597040:  // outputCurrencies
          return ((CalculationRequirements) bean).getOutputCurrencies();
        case 101869496:  // localScenarios
          return ((CalculationRequirements) bean).getLocalScenarios();
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
   * The bean-builder for {@code CalculationRequirements}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CalculationRequirements> {

    private Set<ObservableId> observables = ImmutableSet.of();
    private Set<MarketDataId<?>> nonObservables = ImmutableSet.of();
    private Set<ObservableId> timeSeries = ImmutableSet.of();
    private Set<Currency> outputCurrencies = ImmutableSet.of();
    private LocalScenarioDefinitions localScenarios;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 121811856:  // observables
          return observables;
        case 824041091:  // nonObservables
          return nonObservables;
        case 779431844:  // timeSeries
          return timeSeries;
        case -1022597040:  // outputCurrencies
          return outputCurrencies;
        case 101869496:  // localScenarios
          return localScenarios;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 121811856:  // observables
          this.observables = (Set<ObservableId>) newValue;
          break;
        case 824041091:  // nonObservables
          this.nonObservables = (Set<MarketDataId<?>>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Set<ObservableId>) newValue;
          break;
        case -1022597040:  // outputCurrencies
          this.outputCurrencies = (Set<Currency>) newValue;
          break;
        case 101869496:  // localScenarios
          this.localScenarios = (LocalScenarioDefinitions) newValue;
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
    public CalculationRequirements build() {
      return new CalculationRequirements(
          observables,
          nonObservables,
          timeSeries,
          outputCurrencies,
          localScenarios);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CalculationRequirements.Builder{");
      buf.append("observables").append('=').append(JodaBeanUtils.toString(observables)).append(',').append(' ');
      buf.append("nonObservables").append('=').append(JodaBeanUtils.toString(nonObservables)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("outputCurrencies").append('=').append(JodaBeanUtils.toString(outputCurrencies)).append(',').append(' ');
      buf.append("localScenarios").append('=').append(JodaBeanUtils.toString(localScenarios));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
