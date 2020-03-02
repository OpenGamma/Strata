/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
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
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Historic Price index values, used for indices that are no longer active.
 * <p>
 * This allows the time-series to be queried but not the curve.
 */
@BeanDefinition(builderScope = "private")
public final class HistoricPriceIndexValues
    implements PriceIndexValues, ImmutableBean, Serializable {

  /**
   * The index that the values are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PriceIndex index;
  /**
   * The valuation date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The monthly time-series of fixings.
   * This includes the known historical fixings and must not be empty.
   * <p>
   * Only one value is stored per month. The value is stored in the time-series on the
   * last date of each month (which may be a non-working day).
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
  public static HistoricPriceIndexValues of(
      PriceIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries fixings) {

    return new HistoricPriceIndexValues(index, valuationDate, fixings);
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
  public HistoricPriceIndexValues withParameter(int parameterIndex, double newValue) {
    throw new IndexOutOfBoundsException("No parameters for historic index: " + index);
  }

  @Override
  public HistoricPriceIndexValues withPerturbation(ParameterPerturbation perturbation) {
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public double value(PriceIndexObservation observation) {
    YearMonth fixingMonth = observation.getFixingMonth();
    // If fixing in the past, check time series and returns the historic month price index if present
    if (fixingMonth.isBefore(YearMonth.from(valuationDate))) {
      OptionalDouble fixing = fixings.get(fixingMonth.atEndOfMonth());
      if (fixing.isPresent()) {
        return fixing.getAsDouble();
      }
    }
    throw new MarketDataNotFoundException("Unable to query forward value for historic index " + index);
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder valuePointSensitivity(PriceIndexObservation observation) {
    YearMonth fixingMonth = observation.getFixingMonth();
    // If fixing in the past, check time series and returns the historic month price index if present
    if (fixingMonth.isBefore(YearMonth.from(valuationDate))) {
      if (fixings.get(fixingMonth.atEndOfMonth()).isPresent()) {
        return PointSensitivityBuilder.none();
      }
    }
    throw new MarketDataNotFoundException("Unable to query forward value sensitivity for historic index " + index);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(InflationRateSensitivity pointSensitivity) {
    throw new MarketDataNotFoundException("Unable to create sensitivity for historic index " + index);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    throw new MarketDataNotFoundException("Unable to create sensitivity for historic index " + index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code HistoricPriceIndexValues}.
   * @return the meta-bean, not null
   */
  public static HistoricPriceIndexValues.Meta meta() {
    return HistoricPriceIndexValues.Meta.INSTANCE;
  }

  static {
    MetaBean.register(HistoricPriceIndexValues.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private HistoricPriceIndexValues(
      PriceIndex index,
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
  public HistoricPriceIndexValues.Meta metaBean() {
    return HistoricPriceIndexValues.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index that the values are for.
   * @return the value of the property, not null
   */
  @Override
  public PriceIndex getIndex() {
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
   * Gets the monthly time-series of fixings.
   * This includes the known historical fixings and must not be empty.
   * <p>
   * Only one value is stored per month. The value is stored in the time-series on the
   * last date of each month (which may be a non-working day).
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
      HistoricPriceIndexValues other = (HistoricPriceIndexValues) obj;
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
    buf.append("HistoricPriceIndexValues{");
    buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code HistoricPriceIndexValues}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<PriceIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", HistoricPriceIndexValues.class, PriceIndex.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", HistoricPriceIndexValues.class, LocalDate.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", HistoricPriceIndexValues.class, LocalDateDoubleTimeSeries.class);
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
    public BeanBuilder<? extends HistoricPriceIndexValues> builder() {
      return new HistoricPriceIndexValues.Builder();
    }

    @Override
    public Class<? extends HistoricPriceIndexValues> beanType() {
      return HistoricPriceIndexValues.class;
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
    public MetaProperty<PriceIndex> index() {
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
          return ((HistoricPriceIndexValues) bean).getIndex();
        case 113107279:  // valuationDate
          return ((HistoricPriceIndexValues) bean).getValuationDate();
        case -843784602:  // fixings
          return ((HistoricPriceIndexValues) bean).getFixings();
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
   * The bean-builder for {@code HistoricPriceIndexValues}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<HistoricPriceIndexValues> {

    private PriceIndex index;
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
          this.index = (PriceIndex) newValue;
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
    public HistoricPriceIndexValues build() {
      return new HistoricPriceIndexValues(
          index,
          valuationDate,
          fixings);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("HistoricPriceIndexValues.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
