/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for an FX index.
 * <p>
 * This provides rates for a single currency pair FX index.
 * <p>
 * This implementation is based on an underlying {@link FxForwardRates} instance.
 */
@BeanDefinition(builderScope = "private")
public final class ForwardFxIndexRates
    implements FxIndexRates, ImmutableBean, Serializable {

  /**
   * The index that the rates are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxIndex index;
  /**
   * The underlying FX forward rates.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxForwardRates fxForwardRates;
  /**
   * The time-series of fixings, defaulted to an empty time-series.
   * This includes the known historical fixings and may be empty.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDateDoubleTimeSeries fixings;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on discount factors with no historic fixings.
   * <p>
   * The instance is based on the discount factors for each currency.
   * 
   * @param index  the index
   * @param fxForwardRates  the underlying forward FX rates
   * @return the rates instance
   */
  public static ForwardFxIndexRates of(FxIndex index, FxForwardRates fxForwardRates) {
    return of(index, fxForwardRates, LocalDateDoubleTimeSeries.empty());
  }

  /**
   * Obtains an instance based on discount factors and historic fixings.
   * <p>
   * The instance is based on the discount factors for each currency.
   * 
   * @param index  the index
   * @param fxForwardRates  the underlying forward FX rates
   * @param fixings  the time-series of fixings
   * @return the rates instance
   */
  public static ForwardFxIndexRates of(FxIndex index, FxForwardRates fxForwardRates, LocalDateDoubleTimeSeries fixings) {
    return new ForwardFxIndexRates(index, fxForwardRates, fixings);
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fixings = LocalDateDoubleTimeSeries.empty();
  }

  @ImmutableValidator
  private void validate() {
    if (!index.getCurrencyPair().equals(fxForwardRates.getCurrencyPair())) {
      throw new IllegalArgumentException("Underlying FxForwardRates must have same currency pair");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return fxForwardRates.getValuationDate();
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return fxForwardRates.findData(name);
  }

  @Override
  public int getParameterCount() {
    return fxForwardRates.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return fxForwardRates.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return fxForwardRates.getParameterMetadata(parameterIndex);
  }

  @Override
  public ForwardFxIndexRates withParameter(int parameterIndex, double newValue) {
    return withFxForwardRates(fxForwardRates.withParameter(parameterIndex, newValue));
  }

  @Override
  public ForwardFxIndexRates withPerturbation(ParameterPerturbation perturbation) {
    return withFxForwardRates(fxForwardRates.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(FxIndexObservation observation, Currency baseCurrency) {
    ArgChecker.isTrue(
        index.getCurrencyPair().contains(baseCurrency),
        "Currency {} invalid for FxIndex {}", baseCurrency, index);
    LocalDate fixingDate = observation.getFixingDate();
    double fxIndexRate = !fixingDate.isAfter(getValuationDate()) ? historicRate(observation) : forwardRate(observation);
    boolean inverse = baseCurrency.equals(index.getCurrencyPair().getCounter());
    return (inverse ? 1d / fxIndexRate : fxIndexRate);
  }

  // historic rate
  private double historicRate(FxIndexObservation observation) {
    LocalDate fixingDate = observation.getFixingDate();
    OptionalDouble fixedRate = fixings.get(fixingDate);
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else if (fixingDate.isBefore(getValuationDate())) { // the fixing is required
      if (fixings.isEmpty()) {
        throw new IllegalArgumentException(
            Messages.format("Unable to get fixing for {} on date {}, no time-series supplied", index, fixingDate));
      }
      throw new IllegalArgumentException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
    } else {
      return forwardRate(observation);
    }
  }

  // forward rate
  private double forwardRate(FxIndexObservation observation) {
    return fxForwardRates.rate(index.getCurrencyPair().getBase(), observation.getMaturityDate());
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder ratePointSensitivity(FxIndexObservation observation, Currency baseCurrency) {
    ArgChecker.isTrue(
        index.getCurrencyPair().contains(baseCurrency),
        "Currency {} invalid for FxIndex {}", baseCurrency, index);

    LocalDate fixingDate = observation.getFixingDate();
    if (fixingDate.isBefore(getValuationDate()) ||
        (fixingDate.equals(getValuationDate()) && fixings.get(fixingDate).isPresent())) {
      return PointSensitivityBuilder.none();
    }
    return FxIndexSensitivity.of(observation, baseCurrency, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(FxIndexSensitivity pointSensitivity) {
    return fxForwardRates.parameterSensitivity(pointSensitivity.toFxForwardSensitivity());
  }

  @Override
  public MultiCurrencyAmount currencyExposure(FxIndexSensitivity pointSensitivity) {
    return fxForwardRates.currencyExposure(pointSensitivity.toFxForwardSensitivity());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with different FX forward rates.
   * 
   * @param fxForwardRates  the new FX forward rates
   * @return the new instance
   */
  public ForwardFxIndexRates withFxForwardRates(FxForwardRates fxForwardRates) {
    return new ForwardFxIndexRates(index, fxForwardRates, fixings);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ForwardFxIndexRates}.
   * @return the meta-bean, not null
   */
  public static ForwardFxIndexRates.Meta meta() {
    return ForwardFxIndexRates.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ForwardFxIndexRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ForwardFxIndexRates(
      FxIndex index,
      FxForwardRates fxForwardRates,
      LocalDateDoubleTimeSeries fixings) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fxForwardRates, "fxForwardRates");
    JodaBeanUtils.notNull(fixings, "fixings");
    this.index = index;
    this.fxForwardRates = fxForwardRates;
    this.fixings = fixings;
    validate();
  }

  @Override
  public ForwardFxIndexRates.Meta metaBean() {
    return ForwardFxIndexRates.Meta.INSTANCE;
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
   * Gets the index that the rates are for.
   * @return the value of the property, not null
   */
  @Override
  public FxIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying FX forward rates.
   * @return the value of the property, not null
   */
  @Override
  public FxForwardRates getFxForwardRates() {
    return fxForwardRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series of fixings, defaulted to an empty time-series.
   * This includes the known historical fixings and may be empty.
   * @return the value of the property, not null
   */
  @Override
  public LocalDateDoubleTimeSeries getFixings() {
    return fixings;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ForwardFxIndexRates other = (ForwardFxIndexRates) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fxForwardRates, other.fxForwardRates) &&
          JodaBeanUtils.equal(fixings, other.fixings);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxForwardRates);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixings);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ForwardFxIndexRates{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("fxForwardRates").append('=').append(fxForwardRates).append(',').append(' ');
    buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ForwardFxIndexRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", ForwardFxIndexRates.class, FxIndex.class);
    /**
     * The meta-property for the {@code fxForwardRates} property.
     */
    private final MetaProperty<FxForwardRates> fxForwardRates = DirectMetaProperty.ofImmutable(
        this, "fxForwardRates", ForwardFxIndexRates.class, FxForwardRates.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", ForwardFxIndexRates.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fxForwardRates",
        "fixings");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1002932800:  // fxForwardRates
          return fxForwardRates;
        case -843784602:  // fixings
          return fixings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ForwardFxIndexRates> builder() {
      return new ForwardFxIndexRates.Builder();
    }

    @Override
    public Class<? extends ForwardFxIndexRates> beanType() {
      return ForwardFxIndexRates.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fxForwardRates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxForwardRates> fxForwardRates() {
      return fxForwardRates;
    }

    /**
     * The meta-property for the {@code fixings} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> fixings() {
      return fixings;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((ForwardFxIndexRates) bean).getIndex();
        case -1002932800:  // fxForwardRates
          return ((ForwardFxIndexRates) bean).getFxForwardRates();
        case -843784602:  // fixings
          return ((ForwardFxIndexRates) bean).getFixings();
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
   * The bean-builder for {@code ForwardFxIndexRates}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ForwardFxIndexRates> {

    private FxIndex index;
    private FxForwardRates fxForwardRates;
    private LocalDateDoubleTimeSeries fixings;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1002932800:  // fxForwardRates
          return fxForwardRates;
        case -843784602:  // fixings
          return fixings;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case -1002932800:  // fxForwardRates
          this.fxForwardRates = (FxForwardRates) newValue;
          break;
        case -843784602:  // fixings
          this.fixings = (LocalDateDoubleTimeSeries) newValue;
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
    public ForwardFxIndexRates build() {
      return new ForwardFxIndexRates(
          index,
          fxForwardRates,
          fixings);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ForwardFxIndexRates.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fxForwardRates").append('=').append(JodaBeanUtils.toString(fxForwardRates)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
