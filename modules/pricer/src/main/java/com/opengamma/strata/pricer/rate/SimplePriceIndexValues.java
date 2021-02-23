/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InflationNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides values for a Price index from a forward curve.
 * <p>
 * This provides historic and forward rates for a single {@link PriceIndex}, such as 'US-CPI-U'.
 * <p>
 * This implementation is based on an underlying forward curve.
 * Seasonality is included in the curve, see {@link InflationNodalCurve}.
 */
@BeanDefinition(builderScope = "private")
public final class SimplePriceIndexValues
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
   * The underlying curve.
   * Each x-value on the curve is the number of months between the valuation month and the estimation month.
   * For example, zero represents the valuation month, one the next month and so on.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve curve;
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
   * Obtains an instance based on a curve with no seasonality adjustment.
   * <p>
   * Each x-value on the curve is the number of months between the valuation month and the estimation month.
   * For example, zero represents the valuation month, one the next month and so on.
   * <p>
   * The time-series contains one value per month and must have at least one entry.
   * The value is stored in the time-series on the last date of each month (which may be a non-working day).
   * <p>
   * The curve will be altered to be consistent with the time-series. The last element of the
   * series is added as the first point of the interpolated curve to ensure a coherent transition.
   * 
   * @param index  the Price index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param fixings  the time-series of fixings
   * @param curve  the underlying forward curve for index estimation
   * @return the values instance
   */
  public static SimplePriceIndexValues of(
      PriceIndex index,
      LocalDate valuationDate,
      Curve curve,
      LocalDateDoubleTimeSeries fixings) {

    return new SimplePriceIndexValues(index, valuationDate, curve, fixings);
  }

  @ImmutableConstructor
  private SimplePriceIndexValues(
      PriceIndex index,
      LocalDate valuationDate,
      Curve curve,
      LocalDateDoubleTimeSeries fixings) {

    ArgChecker.isFalse(fixings.isEmpty(), "Fixings must not be empty");
    curve.getMetadata().getXValueType().checkEquals(ValueType.MONTHS, "Incorrect x-value type for price curve");
    curve.getMetadata().getYValueType().checkEquals(ValueType.PRICE_INDEX, "Incorrect y-value type for price curve");
    this.index = ArgChecker.notNull(index, "index");
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.fixings = ArgChecker.notNull(fixings, "fixings");
    this.curve = ArgChecker.notNull(curve, "curve");
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (curve.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(curve));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return curve.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return curve.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return curve.getParameterMetadata(parameterIndex);
  }

  @Override
  public OptionalInt findParameterIndex(ParameterMetadata metadata) {
    return curve.findParameterIndex(metadata);
  }

  @Override
  public SimplePriceIndexValues withParameter(int parameterIndex, double newValue) {
    return withCurve(curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public SimplePriceIndexValues withPerturbation(ParameterPerturbation perturbation) {
    return withCurve(curve.withPerturbation(perturbation));
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
    // otherwise, return the estimate from the curve.
    double nbMonth = numberOfMonths(fixingMonth);
    return curve.yValue(nbMonth);
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
    return InflationRateSensitivity.of(observation, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(InflationRateSensitivity pointSensitivity) {
    UnitParameterSensitivities sens = unitParameterSensitivity(pointSensitivity.getObservation().getFixingMonth());
    return sens.multipliedBy(pointSensitivity.getCurrency(), pointSensitivity.getSensitivity());
  }

  private UnitParameterSensitivities unitParameterSensitivity(YearMonth month) {
    // If fixing in the past, check time series and returns the historic month price index if present
    if (month.isBefore(YearMonth.from(valuationDate))) {
      if (fixings.get(month.atEndOfMonth()).isPresent()) {
        return UnitParameterSensitivities.empty();
      }
    }
    double nbMonth = numberOfMonths(month);
    return UnitParameterSensitivities.of(curve.yValueParameterSensitivity(nbMonth));
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivities.of(curve.createParameterSensitivity(currency, sensitivities));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with a different curve. The new curve must include fixing.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public SimplePriceIndexValues withCurve(Curve curve) {
    return new SimplePriceIndexValues(index, valuationDate, curve, fixings);
  }

  private double numberOfMonths(YearMonth month) {
    return YearMonth.from(valuationDate).until(month, MONTHS);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SimplePriceIndexValues}.
   * @return the meta-bean, not null
   */
  public static SimplePriceIndexValues.Meta meta() {
    return SimplePriceIndexValues.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SimplePriceIndexValues.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SimplePriceIndexValues.Meta metaBean() {
    return SimplePriceIndexValues.Meta.INSTANCE;
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
   * Gets the underlying curve.
   * Each x-value on the curve is the number of months between the valuation month and the estimation month.
   * For example, zero represents the valuation month, one the next month and so on.
   * @return the value of the property, not null
   */
  public Curve getCurve() {
    return curve;
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
      SimplePriceIndexValues other = (SimplePriceIndexValues) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(curve, other.curve) &&
          JodaBeanUtils.equal(fixings, other.fixings);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixings);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SimplePriceIndexValues{");
    buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
    buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SimplePriceIndexValues}.
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
        this, "index", SimplePriceIndexValues.class, PriceIndex.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", SimplePriceIndexValues.class, LocalDate.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", SimplePriceIndexValues.class, Curve.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", SimplePriceIndexValues.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "valuationDate",
        "curve",
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
        case 95027439:  // curve
          return curve;
        case -843784602:  // fixings
          return fixings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SimplePriceIndexValues> builder() {
      return new SimplePriceIndexValues.Builder();
    }

    @Override
    public Class<? extends SimplePriceIndexValues> beanType() {
      return SimplePriceIndexValues.class;
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
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> curve() {
      return curve;
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
          return ((SimplePriceIndexValues) bean).getIndex();
        case 113107279:  // valuationDate
          return ((SimplePriceIndexValues) bean).getValuationDate();
        case 95027439:  // curve
          return ((SimplePriceIndexValues) bean).getCurve();
        case -843784602:  // fixings
          return ((SimplePriceIndexValues) bean).getFixings();
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
   * The bean-builder for {@code SimplePriceIndexValues}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SimplePriceIndexValues> {

    private PriceIndex index;
    private LocalDate valuationDate;
    private Curve curve;
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
        case 95027439:  // curve
          return curve;
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
        case 95027439:  // curve
          this.curve = (Curve) newValue;
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
    public SimplePriceIndexValues build() {
      return new SimplePriceIndexValues(
          index,
          valuationDate,
          curve,
          fixings);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("SimplePriceIndexValues.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
