/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.List;
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;

/**
 * Requirements for market data.
 * <p>
 * This class is used as the input to {@link MarketDataFactory}.
 * It includes the market data identifiers that the application needs.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class MarketDataRequirements implements ImmutableBean {

  /** A set of requirements which specifies that no market data is required. */
  private static final MarketDataRequirements EMPTY = MarketDataRequirements.builder().build();

  /** Keys identifying the market data values required for the calculations. */
  @PropertyDefinition(validate = "notNull", builderType = "Set<? extends ObservableId>")
  private final ImmutableSet<ObservableId> observables;

  /** Keys identifying the market data values required for the calculations. */
  @PropertyDefinition(validate = "notNull", builderType = "Set<? extends MarketDataId<?>>")
  private final ImmutableSet<MarketDataId<?>> nonObservables;

  /** Keys identifying the time series of market data values required for the calculations. */
  @PropertyDefinition(validate = "notNull", builderType = "Set<? extends ObservableId>")
  private final ImmutableSet<ObservableId> timeSeries;

  /**
   * The currencies in the calculation results. The market data must include FX rates in the
   * to allow conversion into the reporting currency. The FX rates must have the output currency as the base
   * currency and the reporting currency as the counter currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<Currency> outputCurrencies;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a set of targets, columns and rules.
   * <p>
   * The targets will typically be trades.
   * The columns represent the measures to calculate.
   * 
   * @param calculationRules  the rules defining how the calculation is performed
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the columns that will be calculated
   * @param refData  the reference data
   * @return the market data requirements
   */
  public static MarketDataRequirements of(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      ReferenceData refData) {

    return CalculationTasks.of(calculationRules, targets, columns).requirements(refData);
  }

  /**
   * Obtains an instance containing a single market data ID.
   *
   * @param id  the ID of the only market data value required
   * @return a set of requirements containing a single market data ID
   */
  public static MarketDataRequirements of(MarketDataId<?> id) {
    return builder().addValues(id).build();
  }

  /**
   * Obtains an instance specifying that no market data is required.
   *
   * @return a set of requirements specifying that no market data is required
   */
  public static MarketDataRequirements empty() {
    return EMPTY;
  }

  /**
   * Returns an empty mutable builder for building up a set of requirements.
   *
   * @return an empty mutable builder for building up a set of requirements
   */
  public static MarketDataRequirementsBuilder builder() {
    return new MarketDataRequirementsBuilder();
  }

  //-------------------------------------------------------------------------
  /**
   * Merges multiple sets of requirements into a single set.
   *
   * @param requirements  market data requirements
   * @return a single set of requirements containing all the requirements from the input sets
   */
  public static MarketDataRequirements combine(List<MarketDataRequirements> requirements) {
    ImmutableSet.Builder<ObservableId> observablesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<MarketDataId<?>> nonObservablesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<ObservableId> timeSeriesBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<Currency> outputCurrenciesBuilder = ImmutableSet.builder();

    for (MarketDataRequirements req : requirements) {
      observablesBuilder.addAll(req.observables);
      nonObservablesBuilder.addAll(req.nonObservables);
      timeSeriesBuilder.addAll(req.timeSeries);
      outputCurrenciesBuilder.addAll(req.outputCurrencies);
    }
    return new MarketDataRequirements(
        observablesBuilder.build(),
        nonObservablesBuilder.build(),
        timeSeriesBuilder.build(),
        outputCurrenciesBuilder.build());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MarketDataRequirements}.
   * @return the meta-bean, not null
   */
  public static MarketDataRequirements.Meta meta() {
    return MarketDataRequirements.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MarketDataRequirements.Meta.INSTANCE);
  }

  /**
   * Creates an instance.
   * @param observables  the value of the property, not null
   * @param nonObservables  the value of the property, not null
   * @param timeSeries  the value of the property, not null
   * @param outputCurrencies  the value of the property, not null
   */
  MarketDataRequirements(
      Set<? extends ObservableId> observables,
      Set<? extends MarketDataId<?>> nonObservables,
      Set<? extends ObservableId> timeSeries,
      Set<Currency> outputCurrencies) {
    JodaBeanUtils.notNull(observables, "observables");
    JodaBeanUtils.notNull(nonObservables, "nonObservables");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(outputCurrencies, "outputCurrencies");
    this.observables = ImmutableSet.copyOf(observables);
    this.nonObservables = ImmutableSet.copyOf(nonObservables);
    this.timeSeries = ImmutableSet.copyOf(timeSeries);
    this.outputCurrencies = ImmutableSet.copyOf(outputCurrencies);
  }

  @Override
  public MarketDataRequirements.Meta metaBean() {
    return MarketDataRequirements.Meta.INSTANCE;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MarketDataRequirements other = (MarketDataRequirements) obj;
      return JodaBeanUtils.equal(observables, other.observables) &&
          JodaBeanUtils.equal(nonObservables, other.nonObservables) &&
          JodaBeanUtils.equal(timeSeries, other.timeSeries) &&
          JodaBeanUtils.equal(outputCurrencies, other.outputCurrencies);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(observables);
    hash = hash * 31 + JodaBeanUtils.hashCode(nonObservables);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeries);
    hash = hash * 31 + JodaBeanUtils.hashCode(outputCurrencies);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("MarketDataRequirements{");
    buf.append("observables").append('=').append(observables).append(',').append(' ');
    buf.append("nonObservables").append('=').append(nonObservables).append(',').append(' ');
    buf.append("timeSeries").append('=').append(timeSeries).append(',').append(' ');
    buf.append("outputCurrencies").append('=').append(JodaBeanUtils.toString(outputCurrencies));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MarketDataRequirements}.
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
        this, "observables", MarketDataRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code nonObservables} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<MarketDataId<?>>> nonObservables = DirectMetaProperty.ofImmutable(
        this, "nonObservables", MarketDataRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<ObservableId>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", MarketDataRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code outputCurrencies} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<Currency>> outputCurrencies = DirectMetaProperty.ofImmutable(
        this, "outputCurrencies", MarketDataRequirements.class, (Class) ImmutableSet.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observables",
        "nonObservables",
        "timeSeries",
        "outputCurrencies");

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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends MarketDataRequirements> builder() {
      return new MarketDataRequirements.Builder();
    }

    @Override
    public Class<? extends MarketDataRequirements> beanType() {
      return MarketDataRequirements.class;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 121811856:  // observables
          return ((MarketDataRequirements) bean).getObservables();
        case 824041091:  // nonObservables
          return ((MarketDataRequirements) bean).getNonObservables();
        case 779431844:  // timeSeries
          return ((MarketDataRequirements) bean).getTimeSeries();
        case -1022597040:  // outputCurrencies
          return ((MarketDataRequirements) bean).getOutputCurrencies();
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
   * The bean-builder for {@code MarketDataRequirements}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<MarketDataRequirements> {

    private Set<? extends ObservableId> observables = ImmutableSet.of();
    private Set<? extends MarketDataId<?>> nonObservables = ImmutableSet.of();
    private Set<? extends ObservableId> timeSeries = ImmutableSet.of();
    private Set<Currency> outputCurrencies = ImmutableSet.of();

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
        case 121811856:  // observables
          return observables;
        case 824041091:  // nonObservables
          return nonObservables;
        case 779431844:  // timeSeries
          return timeSeries;
        case -1022597040:  // outputCurrencies
          return outputCurrencies;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 121811856:  // observables
          this.observables = (Set<? extends ObservableId>) newValue;
          break;
        case 824041091:  // nonObservables
          this.nonObservables = (Set<? extends MarketDataId<?>>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Set<? extends ObservableId>) newValue;
          break;
        case -1022597040:  // outputCurrencies
          this.outputCurrencies = (Set<Currency>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public MarketDataRequirements build() {
      return new MarketDataRequirements(
          observables,
          nonObservables,
          timeSeries,
          outputCurrencies);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("MarketDataRequirements.Builder{");
      buf.append("observables").append('=').append(JodaBeanUtils.toString(observables)).append(',').append(' ');
      buf.append("nonObservables").append('=').append(JodaBeanUtils.toString(nonObservables)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("outputCurrencies").append('=').append(JodaBeanUtils.toString(outputCurrencies));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
