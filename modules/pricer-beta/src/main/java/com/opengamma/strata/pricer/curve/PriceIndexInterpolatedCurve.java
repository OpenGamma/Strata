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
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Implementation of a price index curve where the curve is represented by an interpolated curve.
 */
@BeanDefinition(builderScope = "private")
public final class PriceIndexInterpolatedCurve
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

  //-------------------------------------------------------------------------
  /**
   * Creates a new {@code PriceIndexInterpolatedCurve}.
   * <p>
   * The months are represented by the number of month between the reference month and estimation month. 
   * Zero represents the reference month, 1 the next month and so on.
   * 
   * @param valuationMonth  the valuation date for which the curve is valid
   * @param curve  the underlying curve for index estimation. The last element of the series is added as the first 
   * point of the interpolated curve to ensure a coherent transition.
   * @param timeSeries  the time series with the already known values of the price index. The monthly data should 
   * be referenced by the last day of the month. Not empty.
   * @return the curve
   */
  public static PriceIndexInterpolatedCurve of(
      YearMonth valuationMonth,
      InterpolatedDoublesCurve curve,
      LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.isFalse(timeSeries.isEmpty(), "time series should not be empty");
    // Add the latest element of the time series as the first node on the curve
    YearMonth lastMonth = YearMonth.from(timeSeries.getLatestDate());
    double nbMonth = valuationMonth.until(lastMonth, MONTHS);
    double[] x = curve.getXDataAsPrimitive();
    ArgChecker.isTrue(nbMonth < x[0], "the first estimation month should be after the last known index fixing");
    double[] y = curve.getYDataAsPrimitive();
    double[] xExtended = new double[x.length + 1];
    xExtended[0] = nbMonth;
    System.arraycopy(x, 0, xExtended, 1, x.length);
    double[] yExtended = new double[y.length + 1];
    yExtended[0] = timeSeries.getLatestValue();;
    System.arraycopy(y, 0, yExtended, 1, y.length);
    InterpolatedDoublesCurve finalCurve = 
        new InterpolatedDoublesCurve(xExtended, yExtended, curve.getInterpolator(), true, curve.getName());
    return new PriceIndexInterpolatedCurve(valuationMonth, finalCurve, timeSeries);
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
    return curve.getYValue(nbMonth);
  }

  @Override
  public double[] getPriceIndexParameterSensitivity(YearMonth month) {
    OptionalDouble fixing = timeSeries.get(month.atEndOfMonth());
    if (fixing.isPresent()) { // No sensitivity to the parameter of the estimation curve
      return new double[getParameterCount()];
    }
    double nbMonth = valuationMonth.until(month, MONTHS);
    Double[] sensi1 = curve.getYValueParameterSensitivity(nbMonth);
    double[] sensiFinal = new double[sensi1.length - 1];
    // Remove first element which is to the last fixing
    for (int i = 0; i < sensi1.length - 1; i++) {
      sensiFinal[i] = sensi1[i + 1];
    }
    return sensiFinal;
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
    return new PriceIndexInterpolatedCurve(valuationMonth, curveShifted, timeSeries);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PriceIndexInterpolatedCurve}.
   * @return the meta-bean, not null
   */
  public static PriceIndexInterpolatedCurve.Meta meta() {
    return PriceIndexInterpolatedCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PriceIndexInterpolatedCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private PriceIndexInterpolatedCurve(
      YearMonth valuationMonth,
      InterpolatedDoublesCurve curve,
      LocalDateDoubleTimeSeries timeSeries) {
    JodaBeanUtils.notNull(valuationMonth, "valuationMonth");
    JodaBeanUtils.notNull(curve, "curve");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.valuationMonth = valuationMonth;
    this.curve = curve;
    this.timeSeries = timeSeries;
  }

  @Override
  public PriceIndexInterpolatedCurve.Meta metaBean() {
    return PriceIndexInterpolatedCurve.Meta.INSTANCE;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PriceIndexInterpolatedCurve other = (PriceIndexInterpolatedCurve) obj;
      return JodaBeanUtils.equal(getValuationMonth(), other.getValuationMonth()) &&
          JodaBeanUtils.equal(getCurve(), other.getCurve()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurve());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("PriceIndexInterpolatedCurve{");
    buf.append("valuationMonth").append('=').append(getValuationMonth()).append(',').append(' ');
    buf.append("curve").append('=').append(getCurve()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PriceIndexInterpolatedCurve}.
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
        this, "valuationMonth", PriceIndexInterpolatedCurve.class, YearMonth.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<InterpolatedDoublesCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", PriceIndexInterpolatedCurve.class, InterpolatedDoublesCurve.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", PriceIndexInterpolatedCurve.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationMonth",
        "curve",
        "timeSeries");

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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PriceIndexInterpolatedCurve> builder() {
      return new PriceIndexInterpolatedCurve.Builder();
    }

    @Override
    public Class<? extends PriceIndexInterpolatedCurve> beanType() {
      return PriceIndexInterpolatedCurve.class;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -779918081:  // valuationMonth
          return ((PriceIndexInterpolatedCurve) bean).getValuationMonth();
        case 95027439:  // curve
          return ((PriceIndexInterpolatedCurve) bean).getCurve();
        case 779431844:  // timeSeries
          return ((PriceIndexInterpolatedCurve) bean).getTimeSeries();
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
   * The bean-builder for {@code PriceIndexInterpolatedCurve}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<PriceIndexInterpolatedCurve> {

    private YearMonth valuationMonth;
    private InterpolatedDoublesCurve curve;
    private LocalDateDoubleTimeSeries timeSeries;

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
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

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
    public PriceIndexInterpolatedCurve build() {
      return new PriceIndexInterpolatedCurve(
          valuationMonth,
          curve,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("PriceIndexInterpolatedCurve.Builder{");
      buf.append("valuationMonth").append('=').append(JodaBeanUtils.toString(valuationMonth)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
