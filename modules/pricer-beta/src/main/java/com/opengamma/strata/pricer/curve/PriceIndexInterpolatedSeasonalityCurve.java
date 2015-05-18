/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Implementation of a PriceIndexCurve where the curve is represented by an interpolated curve.
 */
@BeanDefinition(builderScope = "private")
public final class PriceIndexInterpolatedSeasonalityCurve
    implements PriceIndexCurve, ImmutableBean, Serializable {

  /**
   * The reference month to compute the number of months from.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final YearMonth valuationMonth;
  /**
   * The doubles curve underlying the price index curve. 
   * The X dimension on the curve represent the number of months between.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final InterpolatedDoublesCurve curve;
  /**
   * The historic data associated with the price index.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final LocalDateDoubleTimeSeries timeSeries;
  /**
   * Describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month, starting from January.
   * The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final List<Double> seasonality;

  //-------------------------------------------------------------------------
  /**
   * Creates a new {@code PriceIndexInterpolatedSeasonalityCurve}.
   * <p>
   * The months are represented by the number of month between the reference month and estimation month. 
   * Zero represents the reference month, 1 the next month and so on.
   * 
   * @param valuationMonth  the valuation date for which the curve is valid
   * @param curve  the underlying curve for index estimation. The last element of the series is added as the first 
   * point of the interpolated curve to ensure a coherent transition.
   * @param seasonality  the seasonal adjustment for each month, starting with January, must be of length 12
   * @param timeSeries  the time series with the already known values of the price index. The monthly data should 
   * be referenced by the last day of the month. Not empty.
   * @return the curve
   */
  public static PriceIndexInterpolatedSeasonalityCurve of(
      YearMonth valuationMonth,
      InterpolatedDoublesCurve curve,
      LocalDateDoubleTimeSeries timeSeries,
      List<Double> seasonality) {
    ArgChecker.isFalse(timeSeries.isEmpty(), "time series should not be empty");
    // Add the latest element of the time series as the first node on the curve
    YearMonth lastMonth = YearMonth.from(timeSeries.getLatestDate());
    double nbMonth = valuationMonth.until(lastMonth, MONTHS);
    double[] x = curve.getXDataAsPrimitive();
    ArgChecker.isTrue(nbMonth < x[0], "the first estimation month should be after the last known index fixing");
    double value = timeSeries.getLatestValue();
    double[] y = curve.getYDataAsPrimitive();
    double[] xExtended = new double[x.length + 1];
    xExtended[0] = nbMonth;
    System.arraycopy(x, 0, xExtended, 1, x.length);
    double[] yExtended = new double[y.length + 1];
    yExtended[0] = value;
    System.arraycopy(y, 0, yExtended, 1, y.length);
    InterpolatedDoublesCurve finalCurve = 
        new InterpolatedDoublesCurve(xExtended, yExtended, curve.getInterpolator(), true, curve.getName());
    return new PriceIndexInterpolatedSeasonalityCurve(valuationMonth, finalCurve, timeSeries, seasonality);
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(seasonality.size() == 12, "Size of the seasonality list must be 12");
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return curve.getName();
  }

  @Override
  public double getPriceIndex(YearMonth month) {
    OptionalDouble fixing = timeSeries.get(month.atEndOfMonth());
    if(fixing.isPresent()) { // Returns the month price index if present in the time series
      return fixing.getAsDouble();
    }
    // otherwise, return the estimate from the curve.
    double nbMonth = valuationMonth.until(month, MONTHS);
    double indexInterpolated = curve.getYValue(nbMonth);
    int month0 = month.getMonthValue() - 1; // List start at 0 and months start at 1.
    double adjustment = seasonality.get(month0);
    return indexInterpolated * adjustment;
  }

  @Override
  public double[] getPriceIndexParameterSensitivity(YearMonth month) {
    OptionalDouble fixing = timeSeries.get(month.atEndOfMonth());
    if(fixing.isPresent()) { // No sensitivity to the parameter of the estimation curve
      return new double[getParameterCount()];
    }
    double nbMonth = valuationMonth.until(month, MONTHS);
    int month0 = month.getMonthValue() - 1;
    double adjustment = seasonality.get(month0);
    Double[] unadjustedSensitivity = curve.getYValueParameterSensitivity(nbMonth);
    double[] adjustedSensitivity = new double[unadjustedSensitivity.length - 1];
    for (int i = 0; i < unadjustedSensitivity.length - 1; i++) {
      // Remove first element which is to the last fixing
      adjustedSensitivity[i] = unadjustedSensitivity[i + 1] * adjustment;
    }
    return adjustedSensitivity;
  }

  @Override
  public int getParameterCount() {
    return curve.size() - 1; // first element is the last fixing
  }

  @Override
  public PriceIndexCurve shiftedBy(List<ValueAdjustment> adjustments) {
    double[] x = curve.getXDataAsPrimitive();
    double[] y = curve.getYDataAsPrimitive();
    double[] yShifted = y.clone();
    int nbAdjust = Math.min(y.length - 1, adjustments.size());
    for (int i = 0; i < nbAdjust; i++) {
      yShifted[i + 1] = adjustments.get(i).adjust(y[i + 1]);
    }
    InterpolatedDoublesCurve curveShifted = new InterpolatedDoublesCurve(x, yShifted, curve.getInterpolator(), true);
    return new PriceIndexInterpolatedSeasonalityCurve(valuationMonth, curveShifted, timeSeries, seasonality);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PriceIndexInterpolatedSeasonalityCurve}.
   * @return the meta-bean, not null
   */
  public static PriceIndexInterpolatedSeasonalityCurve.Meta meta() {
    return PriceIndexInterpolatedSeasonalityCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PriceIndexInterpolatedSeasonalityCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private PriceIndexInterpolatedSeasonalityCurve(
      YearMonth valuationMonth,
      InterpolatedDoublesCurve curve,
      LocalDateDoubleTimeSeries timeSeries,
      List<Double> seasonality) {
    JodaBeanUtils.notNull(valuationMonth, "valuationMonth");
    JodaBeanUtils.notNull(curve, "curve");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(seasonality, "seasonality");
    this.valuationMonth = valuationMonth;
    this.curve = curve;
    this.timeSeries = timeSeries;
    this.seasonality = ImmutableList.copyOf(seasonality);
    validate();
  }

  @Override
  public PriceIndexInterpolatedSeasonalityCurve.Meta metaBean() {
    return PriceIndexInterpolatedSeasonalityCurve.Meta.INSTANCE;
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
   * Gets the reference month to compute the number of months from.
   * @return the value of the property, not null
   */
  private YearMonth getValuationMonth() {
    return valuationMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the doubles curve underlying the price index curve.
   * The X dimension on the curve represent the number of months between.
   * @return the value of the property, not null
   */
  private InterpolatedDoublesCurve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the historic data associated with the price index.
   * @return the value of the property, not null
   */
  private LocalDateDoubleTimeSeries getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month, starting from January.
   * The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment.
   * @return the value of the property, not null
   */
  private List<Double> getSeasonality() {
    return seasonality;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PriceIndexInterpolatedSeasonalityCurve other = (PriceIndexInterpolatedSeasonalityCurve) obj;
      return JodaBeanUtils.equal(getValuationMonth(), other.getValuationMonth()) &&
          JodaBeanUtils.equal(getCurve(), other.getCurve()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getSeasonality(), other.getSeasonality());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurve());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSeasonality());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("PriceIndexInterpolatedSeasonalityCurve{");
    buf.append("valuationMonth").append('=').append(getValuationMonth()).append(',').append(' ');
    buf.append("curve").append('=').append(getCurve()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("seasonality").append('=').append(JodaBeanUtils.toString(getSeasonality()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PriceIndexInterpolatedSeasonalityCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationMonth} property.
     */
    private final MetaProperty<YearMonth> valuationMonth = DirectMetaProperty.ofImmutable(
        this, "valuationMonth", PriceIndexInterpolatedSeasonalityCurve.class, YearMonth.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<InterpolatedDoublesCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", PriceIndexInterpolatedSeasonalityCurve.class, InterpolatedDoublesCurve.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", PriceIndexInterpolatedSeasonalityCurve.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code seasonality} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> seasonality = DirectMetaProperty.ofImmutable(
        this, "seasonality", PriceIndexInterpolatedSeasonalityCurve.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationMonth",
        "curve",
        "timeSeries",
        "seasonality");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -779918081:  // valuationMonth
          return valuationMonth;
        case 95027439:  // curve
          return curve;
        case 779431844:  // timeSeries
          return timeSeries;
        case -857898080:  // seasonality
          return seasonality;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PriceIndexInterpolatedSeasonalityCurve> builder() {
      return new PriceIndexInterpolatedSeasonalityCurve.Builder();
    }

    @Override
    public Class<? extends PriceIndexInterpolatedSeasonalityCurve> beanType() {
      return PriceIndexInterpolatedSeasonalityCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> valuationMonth() {
      return valuationMonth;
    }

    /**
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<InterpolatedDoublesCurve> curve() {
      return curve;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code seasonality} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<Double>> seasonality() {
      return seasonality;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -779918081:  // valuationMonth
          return ((PriceIndexInterpolatedSeasonalityCurve) bean).getValuationMonth();
        case 95027439:  // curve
          return ((PriceIndexInterpolatedSeasonalityCurve) bean).getCurve();
        case 779431844:  // timeSeries
          return ((PriceIndexInterpolatedSeasonalityCurve) bean).getTimeSeries();
        case -857898080:  // seasonality
          return ((PriceIndexInterpolatedSeasonalityCurve) bean).getSeasonality();
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
   * The bean-builder for {@code PriceIndexInterpolatedSeasonalityCurve}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<PriceIndexInterpolatedSeasonalityCurve> {

    private YearMonth valuationMonth;
    private InterpolatedDoublesCurve curve;
    private LocalDateDoubleTimeSeries timeSeries;
    private List<Double> seasonality = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -779918081:  // valuationMonth
          return valuationMonth;
        case 95027439:  // curve
          return curve;
        case 779431844:  // timeSeries
          return timeSeries;
        case -857898080:  // seasonality
          return seasonality;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -779918081:  // valuationMonth
          this.valuationMonth = (YearMonth) newValue;
          break;
        case 95027439:  // curve
          this.curve = (InterpolatedDoublesCurve) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (LocalDateDoubleTimeSeries) newValue;
          break;
        case -857898080:  // seasonality
          this.seasonality = (List<Double>) newValue;
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
    public PriceIndexInterpolatedSeasonalityCurve build() {
      return new PriceIndexInterpolatedSeasonalityCurve(
          valuationMonth,
          curve,
          timeSeries,
          seasonality);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("PriceIndexInterpolatedSeasonalityCurve.Builder{");
      buf.append("valuationMonth").append('=').append(JodaBeanUtils.toString(valuationMonth)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
