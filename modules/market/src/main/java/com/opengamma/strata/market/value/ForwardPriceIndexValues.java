/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides values for a Price index from a forward curve.
 * <p>
 * This provides historic and forward rates for a single {@link PriceIndex}, such as 'US-CPI-U'.
 * <p>
 * This implementation is based on an underlying forward curve.
 */
@BeanDefinition(builderScope = "private")
public final class ForwardPriceIndexValues
    implements PriceIndexValues, ImmutableBean, Serializable {

  /**
   * The list used when there is no seasonality.
   * It consists of 12 entries, all of value 1.
   */
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
   * The monthly time-series.
   * This covers known historical fixings and must not be empty.
   * <p>
   * Only one value is stored per month. The value is stored in the time-series on the
   * last date of each month (which may be a non-working day).
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDateDoubleTimeSeries timeSeries;
  /**
   * The underlying curve.
   * Each x-value on the curve is the number of months between the valuation month and the estimation month. 
   * For example, zero represents the valuation month, one the next month and so on.
   */
  @PropertyDefinition(validate = "notNull")
  private final InterpolatedNodalCurve curve;
  /**
   * Describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month, starting from January.
   * The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray seasonality;
  /**
   * The underlying extended curve.
   * This has an additional curve node at the start equal to the last point in the time-series.
   */
  private final InterpolatedNodalCurve extendedCurve;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates a new {@code ForwardPriceIndexValues} with no seasonality adjustment.
   * <p>
   * The curve is specified by an instance of {@link InterpolatedNodalCurve}.
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
   * @param fixings  the known historical fixings
   * @param curve  the underlying forward curve for index estimation
   * @return the values instance
   */
  public static ForwardPriceIndexValues of(
      PriceIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries fixings,
      InterpolatedNodalCurve curve) {

    return new ForwardPriceIndexValues(index, valuationDate, fixings, curve, NO_SEASONALITY);
  }

  /**
   * Creates a new {@code ForwardPriceIndexValues} with seasonality adjustment.
   * <p>
   * The curve is specified by an instance of {@link InterpolatedNodalCurve}.
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
   * @param fixings  the known historical fixings
   * @param curve  the underlying forward curve for index estimation
   * @param seasonality  the seasonality adjustment, size 12, index zero is January,
   *   where the value 1 means no seasonality adjustment
   * @return the values instance
   */
  public static ForwardPriceIndexValues of(
      PriceIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries fixings,
      InterpolatedNodalCurve curve,
      DoubleArray seasonality) {

    return new ForwardPriceIndexValues(index, valuationDate, fixings, curve, seasonality);
  }

  @ImmutableConstructor
  private ForwardPriceIndexValues(
      PriceIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries timeSeries,
      InterpolatedNodalCurve curve,
      DoubleArray seasonality) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(timeSeries, "timeSeries");
    ArgChecker.isFalse(timeSeries.isEmpty(), "timeSeries must not be empty");
    ArgChecker.notNull(curve, "curve");
    ArgChecker.notNull(seasonality, "seasonality");
    ArgChecker.isTrue(seasonality.size() == 12, "Seasonality list must contail 12 entries");
    curve.getMetadata().getXValueType().checkEquals(ValueType.MONTHS, "Incorrect x-value type for price curve");
    curve.getMetadata().getYValueType().checkEquals(ValueType.PRICE_INDEX, "Incorrect y-value type for price curve");
    this.index = index;
    this.valuationDate = valuationDate;
    this.timeSeries = timeSeries;
    this.curve = curve;
    this.seasonality = seasonality;
    // add the latest element of the time series as the first node on the curve
    YearMonth lastMonth = YearMonth.from(timeSeries.getLatestDate());
    double nbMonth = numberOfMonths(lastMonth);
    DoubleArray x = curve.getXValues();
    ArgChecker.isTrue(nbMonth < x.get(0), "The first estimation month should be after the last known index fixing");
    this.extendedCurve = curve.withNode(0, nbMonth, timeSeries.getLatestValue());
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveName getCurveName() {
    return curve.getName();
  }

  @Override
  public int getParameterCount() {
    return curve.getParameterCount();
  }

  //-------------------------------------------------------------------------
  @Override
  public double value(YearMonth month) {
    // returns the historic month price index if present in the time series
    OptionalDouble fixing = timeSeries.get(month.atEndOfMonth());
    if (fixing.isPresent()) {
      return fixing.getAsDouble();
    }
    // otherwise, return the estimate from the curve.
    double nbMonth = numberOfMonths(month);
    double value = extendedCurve.yValue(nbMonth);
    int month0 = month.getMonthValue() - 1; // seasonality list start at 0 and months start at 1
    double adjustment = seasonality.get(month0);
    return value * adjustment;
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder valuePointSensitivity(YearMonth fixingMonth) {
    // no sensitivity if historic month price index present in the time series
    if (timeSeries.get(fixingMonth.atEndOfMonth()).isPresent()) {
      return PointSensitivityBuilder.none();
    }
    return InflationRateSensitivity.of(index, fixingMonth, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveUnitParameterSensitivities unitParameterSensitivity(YearMonth month) {
    // no sensitivity if historic month price index present in the time series
    if (timeSeries.get(month.atEndOfMonth()).isPresent()) {
      return CurveUnitParameterSensitivities.of(
          CurveUnitParameterSensitivity.of(curve.getMetadata(), DoubleArray.filled(getParameterCount())));
    }
    double nbMonth = numberOfMonths(month);
    int month0 = month.getMonthValue() - 1;
    double adjustment = seasonality.get(month0);
    DoubleArray unadjustedSensitivity = extendedCurve.yValueParameterSensitivity(nbMonth).getSensitivity();
    // remove first element which is to the last fixing and multiply by seasonality
    DoubleArray adjustedSensitivity = unadjustedSensitivity.subArray(1).multipliedBy(adjustment);
    return CurveUnitParameterSensitivities.of(CurveUnitParameterSensitivity.of(curve.getMetadata(), adjustedSensitivity));
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveCurrencyParameterSensitivities curveParameterSensitivity(InflationRateSensitivity pointSensitivity) {
    CurveUnitParameterSensitivities sens = unitParameterSensitivity(pointSensitivity.getReferenceMonth());
    return sens.multipliedBy(pointSensitivity.getCurrency(), pointSensitivity.getSensitivity());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with a different curve.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public ForwardPriceIndexValues withCurve(InterpolatedNodalCurve curve) {
    return new ForwardPriceIndexValues(index, valuationDate, timeSeries, curve, seasonality);
  }

  private double numberOfMonths(YearMonth month) {
    return YearMonth.from(valuationDate).until(month, MONTHS);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ForwardPriceIndexValues}.
   * @return the meta-bean, not null
   */
  public static ForwardPriceIndexValues.Meta meta() {
    return ForwardPriceIndexValues.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ForwardPriceIndexValues.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public ForwardPriceIndexValues.Meta metaBean() {
    return ForwardPriceIndexValues.Meta.INSTANCE;
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
   * Gets the monthly time-series.
   * This covers known historical fixings and must not be empty.
   * <p>
   * Only one value is stored per month. The value is stored in the time-series on the
   * last date of each month (which may be a non-working day).
   * @return the value of the property, not null
   */
  @Override
  public LocalDateDoubleTimeSeries getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying curve.
   * Each x-value on the curve is the number of months between the valuation month and the estimation month.
   * For example, zero represents the valuation month, one the next month and so on.
   * @return the value of the property, not null
   */
  public InterpolatedNodalCurve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month, starting from January.
   * The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment.
   * @return the value of the property, not null
   */
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
      ForwardPriceIndexValues other = (ForwardPriceIndexValues) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(timeSeries, other.timeSeries) &&
          JodaBeanUtils.equal(curve, other.curve) &&
          JodaBeanUtils.equal(seasonality, other.seasonality);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeries);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    hash = hash * 31 + JodaBeanUtils.hashCode(seasonality);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ForwardPriceIndexValues{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("timeSeries").append('=').append(timeSeries).append(',').append(' ');
    buf.append("curve").append('=').append(curve).append(',').append(' ');
    buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ForwardPriceIndexValues}.
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
        this, "index", ForwardPriceIndexValues.class, PriceIndex.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ForwardPriceIndexValues.class, LocalDate.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ForwardPriceIndexValues.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<InterpolatedNodalCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", ForwardPriceIndexValues.class, InterpolatedNodalCurve.class);
    /**
     * The meta-property for the {@code seasonality} property.
     */
    private final MetaProperty<DoubleArray> seasonality = DirectMetaProperty.ofImmutable(
        this, "seasonality", ForwardPriceIndexValues.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "valuationDate",
        "timeSeries",
        "curve",
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
        case 779431844:  // timeSeries
          return timeSeries;
        case 95027439:  // curve
          return curve;
        case -857898080:  // seasonality
          return seasonality;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ForwardPriceIndexValues> builder() {
      return new ForwardPriceIndexValues.Builder();
    }

    @Override
    public Class<? extends ForwardPriceIndexValues> beanType() {
      return ForwardPriceIndexValues.class;
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
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<InterpolatedNodalCurve> curve() {
      return curve;
    }

    /**
     * The meta-property for the {@code seasonality} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> seasonality() {
      return seasonality;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((ForwardPriceIndexValues) bean).getIndex();
        case 113107279:  // valuationDate
          return ((ForwardPriceIndexValues) bean).getValuationDate();
        case 779431844:  // timeSeries
          return ((ForwardPriceIndexValues) bean).getTimeSeries();
        case 95027439:  // curve
          return ((ForwardPriceIndexValues) bean).getCurve();
        case -857898080:  // seasonality
          return ((ForwardPriceIndexValues) bean).getSeasonality();
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
   * The bean-builder for {@code ForwardPriceIndexValues}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ForwardPriceIndexValues> {

    private PriceIndex index;
    private LocalDate valuationDate;
    private LocalDateDoubleTimeSeries timeSeries;
    private InterpolatedNodalCurve curve;
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
        case 779431844:  // timeSeries
          return timeSeries;
        case 95027439:  // curve
          return curve;
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
        case 779431844:  // timeSeries
          this.timeSeries = (LocalDateDoubleTimeSeries) newValue;
          break;
        case 95027439:  // curve
          this.curve = (InterpolatedNodalCurve) newValue;
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
    public ForwardPriceIndexValues build() {
      return new ForwardPriceIndexValues(
          index,
          valuationDate,
          timeSeries,
          curve,
          seasonality);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ForwardPriceIndexValues.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
