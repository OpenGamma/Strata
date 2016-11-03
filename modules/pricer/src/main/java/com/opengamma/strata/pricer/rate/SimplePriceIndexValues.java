/**
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.InflationNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.SeasonalityDefinition;
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
   * The list used when there is no seasonality.
   * It consists of 12 entries, all of value 1.
   * @deprecated Kept for backward compatibility. The seasonality should be in the curve. See {@link InflationNodalCurve}.
   */
  @Deprecated
  public static final DoubleArray NO_SEASONALITY = DoubleArray.filled(12, 1d);

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
  private final NodalCurve curve;
  /**
   * The monthly time-series of fixings.
   * This includes the known historical fixings and must not be empty.
   * <p>
   * Only one value is stored per month. The value is stored in the time-series on the
   * last date of each month (which may be a non-working day).
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDateDoubleTimeSeries fixings;
  /**
   * Describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month, starting from January.
   * The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment.
   * @deprecated Kept for backward compatibility. The seasonality should be in the curve. See {@link InflationNodalCurve}.
   */
  @Deprecated
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray seasonality;

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
      NodalCurve curve,
      LocalDateDoubleTimeSeries fixings) {

    return new SimplePriceIndexValues(index, valuationDate, curve, fixings, NO_SEASONALITY);
  }

  /**
   * Obtains an instance based on a curve with seasonality adjustment.
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
   * @param seasonality  the seasonality adjustment, size 12, index zero is January,
   *   where the value 1 means no seasonality adjustment
   * @return the values instance
   * @deprecated the seasonality should not be added at this level anymore but in the nodal curve
   */
  @Deprecated
  public static SimplePriceIndexValues of(
      PriceIndex index,
      LocalDate valuationDate,
      NodalCurve curve,
      LocalDateDoubleTimeSeries fixings,
      DoubleArray seasonality) {

    ArgChecker.isFalse(curve instanceof InflationNodalCurve, "Curve cannot be adjusted twice for seasonality");
    // add the latest element of the time series as the first node on the curve
    YearMonth lastMonth = YearMonth.from(fixings.getLatestDate());
    double nbMonth = YearMonth.from(valuationDate).until(lastMonth, MONTHS);
    DoubleArray x = curve.getXValues();
    ArgChecker.isTrue(nbMonth < x.get(0), "The first estimation month should be after the last known index fixing");
    InflationNodalCurve seasonalCurve = InflationNodalCurve.of(
        curve, valuationDate, lastMonth, nbMonth, SeasonalityDefinition.of(seasonality, ShiftType.SCALED));
    return new SimplePriceIndexValues(index, valuationDate, seasonalCurve, fixings, seasonality);
  }

  @ImmutableConstructor
  private SimplePriceIndexValues(
      PriceIndex index,
      LocalDate valuationDate,
      NodalCurve curve,
      LocalDateDoubleTimeSeries fixings,
      DoubleArray seasonality) {

    ArgChecker.isFalse(fixings.isEmpty(), "Fixings must not be empty");
    curve.getMetadata().getXValueType().checkEquals(ValueType.MONTHS, "Incorrect x-value type for price curve");
    curve.getMetadata().getYValueType().checkEquals(ValueType.PRICE_INDEX, "Incorrect y-value type for price curve");
    this.index = ArgChecker.notNull(index, "index");
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.fixings = ArgChecker.notNull(fixings, "fixings");
    this.curve = ArgChecker.notNull(curve, "curve");
    this.seasonality = ArgChecker.notNull(seasonality, "seasonality");
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
    // returns the historic month price index if present in the time series
    OptionalDouble fixing = fixings.get(fixingMonth.atEndOfMonth());
    if (fixing.isPresent()) {
      return fixing.getAsDouble();
    }
    // otherwise, return the estimate from the curve.
    double nbMonth = numberOfMonths(fixingMonth);
    return curve.yValue(nbMonth);
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder valuePointSensitivity(PriceIndexObservation observation) {
    YearMonth fixingMonth = observation.getFixingMonth();
    // no sensitivity if historic month price index present in the time series
    if (fixings.get(fixingMonth.atEndOfMonth()).isPresent()) {
      return PointSensitivityBuilder.none();
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
    // no sensitivity if historic month price index present in the time series
    if (fixings.get(month.atEndOfMonth()).isPresent()) {
      return UnitParameterSensitivities.empty();
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
  public SimplePriceIndexValues withCurve(NodalCurve curve) {
    return new SimplePriceIndexValues(index, valuationDate, curve, fixings, seasonality);
  }

  private double numberOfMonths(YearMonth month) {
    return YearMonth.from(valuationDate).until(month, MONTHS);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SimplePriceIndexValues}.
   * @return the meta-bean, not null
   */
  public static SimplePriceIndexValues.Meta meta() {
    return SimplePriceIndexValues.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SimplePriceIndexValues.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SimplePriceIndexValues.Meta metaBean() {
    return SimplePriceIndexValues.Meta.INSTANCE;
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
  public NodalCurve getCurve() {
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
  /**
   * Gets describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month, starting from January.
   * The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment.
   * @deprecated Kept for backward compatibility. The seasonality should be in the curve. See {@link InflationNodalCurve}.
   * @return the value of the property, not null
   */
  @Deprecated
  public DoubleArray getSeasonality() {
    return seasonality;
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
          JodaBeanUtils.equal(fixings, other.fixings) &&
          JodaBeanUtils.equal(seasonality, other.seasonality);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(seasonality);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("SimplePriceIndexValues{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("curve").append('=').append(curve).append(',').append(' ');
    buf.append("fixings").append('=').append(fixings).append(',').append(' ');
    buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality));
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
    private final MetaProperty<NodalCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", SimplePriceIndexValues.class, NodalCurve.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", SimplePriceIndexValues.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code seasonality} property.
     */
    private final MetaProperty<DoubleArray> seasonality = DirectMetaProperty.ofImmutable(
        this, "seasonality", SimplePriceIndexValues.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "valuationDate",
        "curve",
        "fixings",
        "seasonality");

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
        case -857898080:  // seasonality
          return seasonality;
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
    public MetaProperty<NodalCurve> curve() {
      return curve;
    }

    /**
     * The meta-property for the {@code fixings} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> fixings() {
      return fixings;
    }

    /**
     * The meta-property for the {@code seasonality} property.
     * @deprecated Kept for backward compatibility. The seasonality should be in the curve. See {@link InflationNodalCurve}.
     * @return the meta-property, not null
     */
    @Deprecated
    public MetaProperty<DoubleArray> seasonality() {
      return seasonality;
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
        case -857898080:  // seasonality
          return ((SimplePriceIndexValues) bean).getSeasonality();
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
  private static final class Builder extends DirectFieldsBeanBuilder<SimplePriceIndexValues> {

    private PriceIndex index;
    private LocalDate valuationDate;
    private NodalCurve curve;
    private LocalDateDoubleTimeSeries fixings;
    private DoubleArray seasonality;

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
        case -857898080:  // seasonality
          return seasonality;
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
          this.curve = (NodalCurve) newValue;
          break;
        case -843784602:  // fixings
          this.fixings = (LocalDateDoubleTimeSeries) newValue;
          break;
        case -857898080:  // seasonality
          this.seasonality = (DoubleArray) newValue;
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
    public SimplePriceIndexValues build() {
      return new SimplePriceIndexValues(
          index,
          valuationDate,
          curve,
          fixings,
          seasonality);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("SimplePriceIndexValues.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings)).append(',').append(' ');
      buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
