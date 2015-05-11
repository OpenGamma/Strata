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
import java.util.Set;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.collect.ArgChecker;

import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * Implementation of a PriceIndexCurve where the curve is represented by an interpolated curve.
 */
@BeanDefinition(builderScope = "private")
public class PriceIndexInterpolatedSeasonalityCurve
    implements  PriceIndexCurve, ImmutableBean, Serializable { 
  
  /** The doubles curve underlying the price index curve. 
   * The X dimension on the curve represent the number of months between. **/
  @PropertyDefinition(validate = "notNull", get = "private")
  private final InterpolatedDoublesCurve curve;
  /** The reference month to compute the number of months from. **/
  @PropertyDefinition(validate = "notNull", get = "private")
  private final YearMonth valuationMonth;
  /** Describes the seasonal adjustments. The array has a dimension of 12, one element for each month, 
   * starting from January. The adjustments are multiplicative. For each month, the price index is the one obtained
   * from the interpolated part of the curve multiplied by the seasonal adjustment. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final List<Double> seasonality;

  /**
   * Creates a new {@code PriceIndexInterpolatedSeasonalityCurve}.
   * <p>
   * The months are represented by the number of month between the reference month and estimation month. 
   * 0 represent the reference month, 1 the next month and so on.
   * 
   * @param curve  the underlying curve
   * @param seasonality  the seasonal adjustment for each month, starting with January. Should be of length 12.
   * @param referenceMonth the reference or valuation month
   */
  public static PriceIndexInterpolatedSeasonalityCurve of(
      InterpolatedDoublesCurve curve, 
      List<Double> seasonality, 
      YearMonth referenceMonth) {
    return new PriceIndexInterpolatedSeasonalityCurve(curve, seasonality, referenceMonth);
  }

  // private constructor
  private PriceIndexInterpolatedSeasonalityCurve(InterpolatedDoublesCurve curve, List<Double> seasonality, 
      YearMonth referenceMonth) {
    this.curve = curve;
    ArgChecker.isTrue(seasonality.size() == 12, "size of seasonality should be 12"); 
    this.seasonality = seasonality;
    this.valuationMonth = referenceMonth;
  }

  @Override
  public String getName() {
    return curve.getName();
  }

  @Override
  public double getPriceIndex(YearMonth month) {
    double nbMonth = valuationMonth.until(month, MONTHS);
    double indexInterpolated = curve.getYValue(nbMonth);
    int monthInt = month.getMonthValue() - 1; // List start at 0 and months start at 1.
    double adjustment = seasonality.get(monthInt);
    return indexInterpolated * adjustment;
  }

  @Override
  public Double[] getPriceIndexParameterSensitivity(YearMonth month) {
    double nbMonth = valuationMonth.until(month, MONTHS); // TODO: review - multiply by seasonality
    int monthInt = month.getMonthValue() - 1;
    double adjustment = seasonality.get(monthInt);
    Double[] unadjustedSensitivity = curve.getYValueParameterSensitivity(nbMonth);
    Double[] adjustedSensitivity = new Double[unadjustedSensitivity.length];
    for(int i=0; i<unadjustedSensitivity.length; i++) {
      adjustedSensitivity[i] = unadjustedSensitivity[i] * adjustment;
    }
    return adjustedSensitivity;
  }

  @Override
  public int getNumberOfParameters() {
    return curve.size();
  }

  @Override
  public PriceIndexCurve shiftedBy(double[] shifts) {
    double[] x = curve.getXDataAsPrimitive();
    ArgChecker.isTrue(shifts.length == x.length, "shifts should and the same length as curve nodes");
    double[] y = curve.getYDataAsPrimitive();
    double[] yShifted = new double[y.length];
    for(int i=0; i<y.length; i++) {
      yShifted[i] = y[i] + shifts[i];
    }
    InterpolatedDoublesCurve curveShifted = new InterpolatedDoublesCurve(x, yShifted, curve.getInterpolator(), true);
    return new PriceIndexInterpolatedSeasonalityCurve(curveShifted, seasonality, valuationMonth);
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

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected PriceIndexInterpolatedSeasonalityCurve(PriceIndexInterpolatedSeasonalityCurve.Builder builder) {
    JodaBeanUtils.notNull(builder.curve, "curve");
    JodaBeanUtils.notNull(builder.valuationMonth, "valuationMonth");
    JodaBeanUtils.notNull(builder.seasonality, "seasonality");
    this.curve = builder.curve;
    this.valuationMonth = builder.valuationMonth;
    this.seasonality = ImmutableList.copyOf(builder.seasonality);
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
   * Gets the curve.
   * @return the value of the property, not null
   */
  private InterpolatedDoublesCurve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month to compute the number of months from. *
   * @return the value of the property, not null
   */
  private YearMonth getValuationMonth() {
    return valuationMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the seasonality.
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
      return JodaBeanUtils.equal(getCurve(), other.getCurve()) &&
          JodaBeanUtils.equal(getValuationMonth(), other.getValuationMonth()) &&
          JodaBeanUtils.equal(getSeasonality(), other.getSeasonality());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurve());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSeasonality());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("PriceIndexInterpolatedSeasonalityCurve{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("curve").append('=').append(JodaBeanUtils.toString(getCurve())).append(',').append(' ');
    buf.append("valuationMonth").append('=').append(JodaBeanUtils.toString(getValuationMonth())).append(',').append(' ');
    buf.append("seasonality").append('=').append(JodaBeanUtils.toString(getSeasonality())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PriceIndexInterpolatedSeasonalityCurve}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<InterpolatedDoublesCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", PriceIndexInterpolatedSeasonalityCurve.class, InterpolatedDoublesCurve.class);
    /**
     * The meta-property for the {@code valuationMonth} property.
     */
    private final MetaProperty<YearMonth> valuationMonth = DirectMetaProperty.ofImmutable(
        this, "valuationMonth", PriceIndexInterpolatedSeasonalityCurve.class, YearMonth.class);
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
        "curve",
        "valuationMonth",
        "seasonality");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return curve;
        case -779918081:  // valuationMonth
          return valuationMonth;
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
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<InterpolatedDoublesCurve> curve() {
      return curve;
    }

    /**
     * The meta-property for the {@code valuationMonth} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> valuationMonth() {
      return valuationMonth;
    }

    /**
     * The meta-property for the {@code seasonality} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> seasonality() {
      return seasonality;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return ((PriceIndexInterpolatedSeasonalityCurve) bean).getCurve();
        case -779918081:  // valuationMonth
          return ((PriceIndexInterpolatedSeasonalityCurve) bean).getValuationMonth();
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
  private static class Builder extends DirectFieldsBeanBuilder<PriceIndexInterpolatedSeasonalityCurve> {

    private InterpolatedDoublesCurve curve;
    private YearMonth valuationMonth;
    private List<Double> seasonality = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return curve;
        case -779918081:  // valuationMonth
          return valuationMonth;
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
        case 95027439:  // curve
          this.curve = (InterpolatedDoublesCurve) newValue;
          break;
        case -779918081:  // valuationMonth
          this.valuationMonth = (YearMonth) newValue;
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
      return new PriceIndexInterpolatedSeasonalityCurve(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("PriceIndexInterpolatedSeasonalityCurve.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("valuationMonth").append('=').append(JodaBeanUtils.toString(valuationMonth)).append(',').append(' ');
      buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
