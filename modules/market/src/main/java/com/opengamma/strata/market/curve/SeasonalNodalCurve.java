/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.DoubleBinaryOperator;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.PropertyDefinition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * Specific for inflation. Monthly + start value.
 * <p>
 * The correct month has to be done before this object is constructed.
 */
@BeanDefinition
public class SeasonalNodalCurve
    implements NodalCurve, ImmutableBean, Serializable {

  /**
   * The underlying curve, before the adjustment. 
   * With fixed initial value. Not a parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve curve;
  /**
   * Describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month.
   * The adjustments are described as a perturbation to the existing values.
   * No adjustment to the fixing value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray seasonality;
  /**
   * The function applied to the unadjusted value and the adjustment. (value, monthly adjustment) -> adjustedValue.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleBinaryOperator adjustmentFunction;
  /**
   * The derivative with respect to the first variable of the adjustment function.
   */
  @PropertyDefinition
  private final DoubleBinaryOperator adjustmentDerivative;
  
  private final double xFixing;  // cached, not a property
  private final double yFixing;  // cached, not a property

  public static SeasonalNodalCurve of(
      NodalCurve curve,
      DoubleArray seasonality,
      DoubleBinaryOperator adjustmentFunction) {
    
    return new SeasonalNodalCurve(curve, seasonality, adjustmentFunction, null);
  }
  
  public static SeasonalNodalCurve of(
      NodalCurve curve, 
      DoubleArray seasonality, 
      DoubleBinaryOperator adjustmentFunction,
      DoubleBinaryOperator adjustmentDerivative) {
    
    return new SeasonalNodalCurve(curve, seasonality, adjustmentFunction, adjustmentDerivative);
  }
  
  public static SeasonalNodalCurve of(
      NodalCurve curveWithoutFixing, 
      LocalDate valuationDate,
      YearMonth lastMonth,
      double lastFixingValue,
      DoubleArray seasonalityMonthly, 
      DoubleBinaryOperator adjustmentFunction,
      DoubleBinaryOperator adjustmentDerivative) {

    double nbMonth = YearMonth.from(valuationDate).until(lastMonth, MONTHS);
    DoubleArray x = curveWithoutFixing.getXValues();
    ArgChecker.isTrue(nbMonth < x.get(0), "The first estimation month should be after the last known index fixing");
    NodalCurve extendedCurve = curveWithoutFixing.withNode(nbMonth, lastFixingValue, ParameterMetadata.empty());
    double[] seasonalityCompoundedArray = new double[12];
    int lastMonthIndex = lastMonth.getMonth().getValue() - 2;
    seasonalityCompoundedArray[(int) ((nbMonth + 12 + 1) % 12)] = seasonalityMonthly.get((lastMonthIndex + 1) % 12);
    for (int i = 1; i < 12; i++) {
      int j = (int) ((nbMonth + 12 + 1 + i) % 12);
      seasonalityCompoundedArray[j] = adjustmentFunction
          .applyAsDouble(seasonalityCompoundedArray[(j - 1 + 12) % 12], 
              seasonalityMonthly.get((lastMonthIndex + 1 + i) % 12));
    }
    return new SeasonalNodalCurve(extendedCurve, DoubleArray.ofUnsafe(seasonalityCompoundedArray), 
        adjustmentFunction, adjustmentDerivative);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private SeasonalNodalCurve(
      NodalCurve curve, 
      DoubleArray seasonality, 
      DoubleBinaryOperator adjustmentFunction,
      DoubleBinaryOperator adjustmentDerivative) {
    this.curve = curve;
    this.seasonality = seasonality;
    this.xFixing = curve.getXValues().get(0);
    this.yFixing = curve.getYValues().get(0);
    int i = seasonalityIndex(xFixing);
    ArgChecker.isTrue(adjustmentFunction.applyAsDouble(yFixing, seasonality.get(i) ) - yFixing < 1.0E-10, 
        "Fixing value should be unadjusted");
    this.adjustmentFunction = adjustmentFunction;
    this.adjustmentDerivative = adjustmentDerivative;
  }

  @Override
  public CurveMetadata getMetadata() {
    return curve.getMetadata();
  }

  @Override
  public double yValue(double x) {
    int i = seasonalityIndex(x);
    double adjustment = seasonality.get(i);
    return adjustmentFunction.applyAsDouble(curve.yValue(x), adjustment);
  }
  
  // The index on the seasonality vector associated to a time (nb months).
  private int seasonalityIndex(double x) {
    long xLong = Math.round(x);
    return (int) ((xLong + 12) % 12); // Shift by 12 has java compute the remainder of negative numbers as negative
  }

  @Override
  public int getParameterCount() {
    return curve.getParameterCount() - 1;
  }

  @Override
  public double getParameter(int parameterIndex) {
    return curve.getParameter(parameterIndex + 1);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return curve.getParameterMetadata(parameterIndex + 1);
  }

  @Override
  public DoubleArray getXValues() {
    return curve.getXValues().subArray(1);
  }

  @Override
  public DoubleArray getYValues() {
    return curve.getYValues().subArray(1);
  }

  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    ArgChecker.notNull(adjustmentDerivative, "sensitivity can be computed only when adjustmentDerivative is present");
    int i = seasonalityIndex(x);
    double adjustment = seasonality.get(i);
    double value = curve.yValue(x);
    double derivativeFactor = adjustmentFunction.applyAsDouble(value, adjustment);
    UnitParameterSensitivity u = curve.yValueParameterSensitivity(x);
    UnitParameterSensitivity u2 = 
        UnitParameterSensitivity.of(u.getMarketDataName(), 
            u.getParameterMetadata().subList(1, u.getParameterMetadata().size()), u.getSensitivity().subArray(1));
    return u2.multipliedBy(derivativeFactor);
  }

  @Override
  public double firstDerivative(double x) {
    throw new UnsupportedOperationException("Value implemented only at discrete (monthly) values; no derivative available");
  }

  @Override
  public NodalCurve withMetadata(CurveMetadata metadata) {
    return new SeasonalNodalCurve(curve.withMetadata(metadata), seasonality, adjustmentFunction, adjustmentDerivative);
  }

  @Override
  public NodalCurve withYValues(DoubleArray values) {
    DoubleArray yExtended = DoubleArray.of(yFixing).concat(values);
    return new SeasonalNodalCurve(curve.withYValues(yExtended), seasonality, adjustmentFunction, adjustmentDerivative);
  }

  @Override
  public NodalCurve withValues(DoubleArray xValues, DoubleArray yValues) {
    DoubleArray xExtended = DoubleArray.of(xFixing).concat(xValues);
    DoubleArray yExtended = DoubleArray.of(yFixing).concat(yValues);
    return new SeasonalNodalCurve(curve.withValues(xExtended, yExtended), seasonality, adjustmentFunction, adjustmentDerivative);
  }

  @Override
  public NodalCurve withParameter(int parameterIndex, double newValue) {
    return new SeasonalNodalCurve(curve.withParameter(parameterIndex + 1, newValue), seasonality, adjustmentFunction,
        adjustmentDerivative);
  }

  @Override
  public NodalCurve withNode(double x, double y, ParameterMetadata paramMetadata) {
    ArgChecker.isTrue(xFixing < x, "node can be added only after the fixing anchor");
    return new SeasonalNodalCurve(curve.withNode(x, y, paramMetadata), seasonality, adjustmentFunction, adjustmentDerivative);
  }
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SeasonalNodalCurve}.
   * @return the meta-bean, not null
   */
  public static SeasonalNodalCurve.Meta meta() {
    return SeasonalNodalCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SeasonalNodalCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SeasonalNodalCurve.Builder builder() {
    return new SeasonalNodalCurve.Builder();
  }

  @Override
  public SeasonalNodalCurve.Meta metaBean() {
    return SeasonalNodalCurve.Meta.INSTANCE;
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
   * Gets the underlying curve, before the adjustment.
   * With fixed initial value. Not a parameter.
   * @return the value of the property, not null
   */
  public NodalCurve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month.
   * The adjustments are described as a perturbation to the existing values.
   * No adjustment to the fixing value.
   * @return the value of the property, not null
   */
  public DoubleArray getSeasonality() {
    return seasonality;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the function applied to the unadjusted value and the adjustment. (value, monthly adjustment) -> adjustedValue.
   * @return the value of the property, not null
   */
  public DoubleBinaryOperator getAdjustmentFunction() {
    return adjustmentFunction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the derivative with respect to the first variable of the adjustment function.
   * @return the value of the property
   */
  public DoubleBinaryOperator getAdjustmentDerivative() {
    return adjustmentDerivative;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SeasonalNodalCurve other = (SeasonalNodalCurve) obj;
      return JodaBeanUtils.equal(curve, other.curve) &&
          JodaBeanUtils.equal(seasonality, other.seasonality) &&
          JodaBeanUtils.equal(adjustmentFunction, other.adjustmentFunction) &&
          JodaBeanUtils.equal(adjustmentDerivative, other.adjustmentDerivative);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    hash = hash * 31 + JodaBeanUtils.hashCode(seasonality);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustmentFunction);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustmentDerivative);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SeasonalNodalCurve{");
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
    buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality)).append(',').append(' ');
    buf.append("adjustmentFunction").append('=').append(JodaBeanUtils.toString(adjustmentFunction)).append(',').append(' ');
    buf.append("adjustmentDerivative").append('=').append(JodaBeanUtils.toString(adjustmentDerivative)).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SeasonalNodalCurve}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<NodalCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", SeasonalNodalCurve.class, NodalCurve.class);
    /**
     * The meta-property for the {@code seasonality} property.
     */
    private final MetaProperty<DoubleArray> seasonality = DirectMetaProperty.ofImmutable(
        this, "seasonality", SeasonalNodalCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code adjustmentFunction} property.
     */
    private final MetaProperty<DoubleBinaryOperator> adjustmentFunction = DirectMetaProperty.ofImmutable(
        this, "adjustmentFunction", SeasonalNodalCurve.class, DoubleBinaryOperator.class);
    /**
     * The meta-property for the {@code adjustmentDerivative} property.
     */
    private final MetaProperty<DoubleBinaryOperator> adjustmentDerivative = DirectMetaProperty.ofImmutable(
        this, "adjustmentDerivative", SeasonalNodalCurve.class, DoubleBinaryOperator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curve",
        "seasonality",
        "adjustmentFunction",
        "adjustmentDerivative");

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
        case -857898080:  // seasonality
          return seasonality;
        case -2132277147:  // adjustmentFunction
          return adjustmentFunction;
        case -1710031148:  // adjustmentDerivative
          return adjustmentDerivative;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SeasonalNodalCurve.Builder builder() {
      return new SeasonalNodalCurve.Builder();
    }

    @Override
    public Class<? extends SeasonalNodalCurve> beanType() {
      return SeasonalNodalCurve.class;
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
    public final MetaProperty<NodalCurve> curve() {
      return curve;
    }

    /**
     * The meta-property for the {@code seasonality} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DoubleArray> seasonality() {
      return seasonality;
    }

    /**
     * The meta-property for the {@code adjustmentFunction} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DoubleBinaryOperator> adjustmentFunction() {
      return adjustmentFunction;
    }

    /**
     * The meta-property for the {@code adjustmentDerivative} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DoubleBinaryOperator> adjustmentDerivative() {
      return adjustmentDerivative;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return ((SeasonalNodalCurve) bean).getCurve();
        case -857898080:  // seasonality
          return ((SeasonalNodalCurve) bean).getSeasonality();
        case -2132277147:  // adjustmentFunction
          return ((SeasonalNodalCurve) bean).getAdjustmentFunction();
        case -1710031148:  // adjustmentDerivative
          return ((SeasonalNodalCurve) bean).getAdjustmentDerivative();
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
   * The bean-builder for {@code SeasonalNodalCurve}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<SeasonalNodalCurve> {

    private NodalCurve curve;
    private DoubleArray seasonality;
    private DoubleBinaryOperator adjustmentFunction;
    private DoubleBinaryOperator adjustmentDerivative;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(SeasonalNodalCurve beanToCopy) {
      this.curve = beanToCopy.getCurve();
      this.seasonality = beanToCopy.getSeasonality();
      this.adjustmentFunction = beanToCopy.getAdjustmentFunction();
      this.adjustmentDerivative = beanToCopy.getAdjustmentDerivative();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return curve;
        case -857898080:  // seasonality
          return seasonality;
        case -2132277147:  // adjustmentFunction
          return adjustmentFunction;
        case -1710031148:  // adjustmentDerivative
          return adjustmentDerivative;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          this.curve = (NodalCurve) newValue;
          break;
        case -857898080:  // seasonality
          this.seasonality = (DoubleArray) newValue;
          break;
        case -2132277147:  // adjustmentFunction
          this.adjustmentFunction = (DoubleBinaryOperator) newValue;
          break;
        case -1710031148:  // adjustmentDerivative
          this.adjustmentDerivative = (DoubleBinaryOperator) newValue;
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
    public SeasonalNodalCurve build() {
      return new SeasonalNodalCurve(
          curve,
          seasonality,
          adjustmentFunction,
          adjustmentDerivative);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the underlying curve, before the adjustment.
     * With fixed initial value. Not a parameter.
     * @param curve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curve(NodalCurve curve) {
      JodaBeanUtils.notNull(curve, "curve");
      this.curve = curve;
      return this;
    }

    /**
     * Sets describes the seasonal adjustments.
     * The array has a dimension of 12, one element for each month.
     * The adjustments are described as a perturbation to the existing values.
     * No adjustment to the fixing value.
     * @param seasonality  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder seasonality(DoubleArray seasonality) {
      JodaBeanUtils.notNull(seasonality, "seasonality");
      this.seasonality = seasonality;
      return this;
    }

    /**
     * Sets the function applied to the unadjusted value and the adjustment. (value, monthly adjustment) -> adjustedValue.
     * @param adjustmentFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder adjustmentFunction(DoubleBinaryOperator adjustmentFunction) {
      JodaBeanUtils.notNull(adjustmentFunction, "adjustmentFunction");
      this.adjustmentFunction = adjustmentFunction;
      return this;
    }

    /**
     * Sets the derivative with respect to the first variable of the adjustment function.
     * @param adjustmentDerivative  the new value
     * @return this, for chaining, not null
     */
    public Builder adjustmentDerivative(DoubleBinaryOperator adjustmentDerivative) {
      this.adjustmentDerivative = adjustmentDerivative;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("SeasonalNodalCurve.Builder{");
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
      buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality)).append(',').append(' ');
      buf.append("adjustmentFunction").append('=').append(JodaBeanUtils.toString(adjustmentFunction)).append(',').append(' ');
      buf.append("adjustmentDerivative").append('=').append(JodaBeanUtils.toString(adjustmentDerivative)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
