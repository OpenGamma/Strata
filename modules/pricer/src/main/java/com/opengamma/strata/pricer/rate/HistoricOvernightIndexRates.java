/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Historic Overnight index rates, used for indices that are no longer active.
 * <p>
 * This allows the time-series to be queried but not the curve.
 */
@BeanDefinition(builderScope = "private")
public final class HistoricOvernightIndexRates
    implements OvernightIndexRates, ImmutableBean, Serializable {

  /**
   * The index that the rates are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightIndex index;
  /**
   * The valuation date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The time-series of fixings, defaulted to an empty time-series.
   * This includes the known historical fixings and may be empty.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDateDoubleTimeSeries fixings;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a time-series of fixings.
   * 
   * @param index  the index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param fixings  the time-series of fixings 
   * @return the rates view
   */
  public static HistoricOvernightIndexRates of(
      OvernightIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries fixings) {

    return new HistoricOvernightIndexRates(index, valuationDate, fixings);
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return 0;
  }

  @Override
  public double getParameter(int parameterIndex) {
    throw new IndexOutOfBoundsException("No parameters for historic index: " + index);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    throw new IndexOutOfBoundsException("No parameters for historic index: " + index);
  }

  @Override
  public HistoricOvernightIndexRates withParameter(int parameterIndex, double newValue) {
    throw new IndexOutOfBoundsException("No parameters for historic index: " + index);
  }

  @Override
  public HistoricOvernightIndexRates withPerturbation(ParameterPerturbation perturbation) {
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(OvernightIndexObservation observation) {
    if (!observation.getFixingDate().isAfter(getValuationDate())) {
      return historicRate(observation);
    }
    return rateIgnoringFixings(observation);
  }

  // historic rate
  private double historicRate(OvernightIndexObservation observation) {
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
      return rateIgnoringFixings(observation);
    }
  }

  @Override
  public double rateIgnoringFixings(OvernightIndexObservation observation) {
    throw new MarketDataNotFoundException("Unable to query forward rate for historic index " + index);
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder ratePointSensitivity(OvernightIndexObservation observation) {
    LocalDate fixingDate = observation.getFixingDate();
    LocalDate valuationDate = getValuationDate();
    if (fixingDate.isBefore(valuationDate) ||
        (fixingDate.equals(valuationDate) && fixings.get(fixingDate).isPresent())) {
      return PointSensitivityBuilder.none();
    }
    throw new MarketDataNotFoundException("Unable to query forward rate sensitivity for historic index " + index);
  }

  @Override
  public PointSensitivityBuilder rateIgnoringFixingsPointSensitivity(OvernightIndexObservation observation) {
    throw new MarketDataNotFoundException("Unable to query forward rate sensitivity for historic index " + index);
  }

  //-------------------------------------------------------------------------
  @Override
  public double periodRate(OvernightIndexObservation startDateObservation, LocalDate endDate) {
    throw new MarketDataNotFoundException("Unable to query forward rate for historic index " + index);
  }

  @Override
  public PointSensitivityBuilder periodRatePointSensitivity(
      OvernightIndexObservation startDateObservation,
      LocalDate endDate) {

    throw new MarketDataNotFoundException("Unable to query forward rate sensitivity for historic index " + index);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(OvernightRateSensitivity pointSensitivity) {
    throw new MarketDataNotFoundException("Unable to create sensitivity for historic index " + index);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    throw new MarketDataNotFoundException("Unable to create sensitivity for historic index " + index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code HistoricOvernightIndexRates}.
   * @return the meta-bean, not null
   */
  public static HistoricOvernightIndexRates.Meta meta() {
    return HistoricOvernightIndexRates.Meta.INSTANCE;
  }

  static {
    MetaBean.register(HistoricOvernightIndexRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private HistoricOvernightIndexRates(
      OvernightIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries fixings) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(fixings, "fixings");
    this.index = index;
    this.valuationDate = valuationDate;
    this.fixings = fixings;
  }

  @Override
  public HistoricOvernightIndexRates.Meta metaBean() {
    return HistoricOvernightIndexRates.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index that the rates are for.
   * @return the value of the property, not null
   */
  @Override
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
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
      HistoricOvernightIndexRates other = (HistoricOvernightIndexRates) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(fixings, other.fixings);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixings);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("HistoricOvernightIndexRates{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code HistoricOvernightIndexRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<OvernightIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", HistoricOvernightIndexRates.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", HistoricOvernightIndexRates.class, LocalDate.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", HistoricOvernightIndexRates.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "valuationDate",
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
        case 113107279:  // valuationDate
          return valuationDate;
        case -843784602:  // fixings
          return fixings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends HistoricOvernightIndexRates> builder() {
      return new HistoricOvernightIndexRates.Builder();
    }

    @Override
    public Class<? extends HistoricOvernightIndexRates> beanType() {
      return HistoricOvernightIndexRates.class;
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
    public MetaProperty<OvernightIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
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
          return ((HistoricOvernightIndexRates) bean).getIndex();
        case 113107279:  // valuationDate
          return ((HistoricOvernightIndexRates) bean).getValuationDate();
        case -843784602:  // fixings
          return ((HistoricOvernightIndexRates) bean).getFixings();
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
   * The bean-builder for {@code HistoricOvernightIndexRates}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<HistoricOvernightIndexRates> {

    private OvernightIndex index;
    private LocalDate valuationDate;
    private LocalDateDoubleTimeSeries fixings;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 113107279:  // valuationDate
          return valuationDate;
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
          this.index = (OvernightIndex) newValue;
          break;
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
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
    public HistoricOvernightIndexRates build() {
      return new HistoricOvernightIndexRates(
          index,
          valuationDate,
          fixings);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("HistoricOvernightIndexRates.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
