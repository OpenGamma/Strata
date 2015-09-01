/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;

/**
 * A curve with a parallel shift applied to its y-values.
 * <p>
 * This class decorates another curve and applies an adjustment to the y-values when they are queried.
 * The shift is either absolute or relative.
 * <p>
 * When the shift is absolute the shift amount is added to the y-value.
 * <p>
 * When the shift is relative the y-value is scaled by the shift amount.
 * The shift amount is interpreted as a percentage.
 * For example, a shift amount of 0.1 is a shift of +10% which multiplies the value by 1.1.
 * A shift amount of -0.2 is a shift of -20% which multiplies the value by 0.8
 */
@BeanDefinition(builderScope = "private")
public final class ParallelShiftedCurve
    implements Curve, ImmutableBean, Serializable {

  /**
   * The underlying curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve curve;
  /**
   * The type of shift to apply to the y-values of the curve.
   * The amount of the shift is determined by {@code #getShiftAmount()}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ShiftType shiftType;
  /**
   * The amount by which y-values are shifted.
   * The meaning of this amount is determined by {@code #getShiftType()}.
   */
  @PropertyDefinition(validate = "notNull")
  private final double shiftAmount;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve based on an underlying curve with a fixed amount added to the Y values.
   *
   * @param curve  the underlying curve
   * @param shiftAmount  the amount added to the Y values of the curve
   * @return a curve based on an underlying curve with a fixed amount added to the Y values.
   */
  public static ParallelShiftedCurve absolute(Curve curve, double shiftAmount) {
    return new ParallelShiftedCurve(curve, ShiftType.ABSOLUTE, shiftAmount);
  }

  /**
   * Returns a curve based on an underlying curve with a scaling applied to the Y values.
   * <p>
   * The shift amount is interpreted as a percentage. For example, a shift amount of 0.1 is a
   * shift of +10% which multiplies the value by 1.1. A shift amount of -0.2 is a shift of -20%
   * which multiplies the value by 0.8
   *
   * @param curve  the underlying curve
   * @param shiftAmount  the percentage by which the Y values are scaled
   * @return a curve based on an underlying curve with a scaling applied to the Y values.
   */
  public static ParallelShiftedCurve relative(Curve curve, double shiftAmount) {
    return new ParallelShiftedCurve(curve, ShiftType.RELATIVE, shiftAmount);
  }

  /**
   * Returns a curve based on an underlying curve with a parallel shift applied to the Y values.
   *
   * @param curve  the underlying curve
   * @param shiftType  the type of shift which specifies how the shift amount is applied to the Y values
   * @param shiftAmount  the magnitude of the shift
   * @return a curve based on an underlying curve with a parallel shift applied to the Y values
   */
  public static ParallelShiftedCurve of(Curve curve, ShiftType shiftType, double shiftAmount) {
    return new ParallelShiftedCurve(curve, shiftType, shiftAmount);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveMetadata getMetadata() {
    return curve.getMetadata();
  }

  @Override
  public CurveName getName() {
    return curve.getName();
  }

  @Override
  public int getParameterCount() {
    return curve.getParameterCount();
  }

  @Override
  public double yValue(double x) {
    return shiftType.applyShift(curve.yValue(x), shiftAmount);
  }

  @Override
  public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
    return curve.yValueParameterSensitivity(x);
  }

  @Override
  public double firstDerivative(double x) {
    double firstDerivative = curve.firstDerivative(x);
    switch (shiftType) {
      case ABSOLUTE:
        // If all Y values have been shifted the same amount the derivative is unaffected
        return firstDerivative;
      case RELATIVE:
        // If all Y values have been scaled by the same factor the first derivative is scaled in the same way
        return shiftType.applyShift(firstDerivative, shiftAmount);
      default:
        throw new IllegalArgumentException("Unsupported shift type " + shiftType);
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ParallelShiftedCurve}.
   * @return the meta-bean, not null
   */
  public static ParallelShiftedCurve.Meta meta() {
    return ParallelShiftedCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ParallelShiftedCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ParallelShiftedCurve(
      Curve curve,
      ShiftType shiftType,
      double shiftAmount) {
    JodaBeanUtils.notNull(curve, "curve");
    JodaBeanUtils.notNull(shiftType, "shiftType");
    JodaBeanUtils.notNull(shiftAmount, "shiftAmount");
    this.curve = curve;
    this.shiftType = shiftType;
    this.shiftAmount = shiftAmount;
  }

  @Override
  public ParallelShiftedCurve.Meta metaBean() {
    return ParallelShiftedCurve.Meta.INSTANCE;
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
   * Gets the underlying curve.
   * @return the value of the property, not null
   */
  public Curve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of shift to apply to the y-values of the curve.
   * The amount of the shift is determined by {@code #getShiftAmount()}.
   * @return the value of the property, not null
   */
  public ShiftType getShiftType() {
    return shiftType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the amount by which y-values are shifted.
   * The meaning of this amount is determined by {@code #getShiftType()}.
   * @return the value of the property, not null
   */
  public double getShiftAmount() {
    return shiftAmount;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ParallelShiftedCurve other = (ParallelShiftedCurve) obj;
      return JodaBeanUtils.equal(getCurve(), other.getCurve()) &&
          JodaBeanUtils.equal(getShiftType(), other.getShiftType()) &&
          JodaBeanUtils.equal(getShiftAmount(), other.getShiftAmount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurve());
    hash = hash * 31 + JodaBeanUtils.hashCode(getShiftType());
    hash = hash * 31 + JodaBeanUtils.hashCode(getShiftAmount());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ParallelShiftedCurve{");
    buf.append("curve").append('=').append(getCurve()).append(',').append(' ');
    buf.append("shiftType").append('=').append(getShiftType()).append(',').append(' ');
    buf.append("shiftAmount").append('=').append(JodaBeanUtils.toString(getShiftAmount()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ParallelShiftedCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", ParallelShiftedCurve.class, Curve.class);
    /**
     * The meta-property for the {@code shiftType} property.
     */
    private final MetaProperty<ShiftType> shiftType = DirectMetaProperty.ofImmutable(
        this, "shiftType", ParallelShiftedCurve.class, ShiftType.class);
    /**
     * The meta-property for the {@code shiftAmount} property.
     */
    private final MetaProperty<Double> shiftAmount = DirectMetaProperty.ofImmutable(
        this, "shiftAmount", ParallelShiftedCurve.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curve",
        "shiftType",
        "shiftAmount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return curve;
        case 893345500:  // shiftType
          return shiftType;
        case -1043480710:  // shiftAmount
          return shiftAmount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ParallelShiftedCurve> builder() {
      return new ParallelShiftedCurve.Builder();
    }

    @Override
    public Class<? extends ParallelShiftedCurve> beanType() {
      return ParallelShiftedCurve.class;
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
    public MetaProperty<Curve> curve() {
      return curve;
    }

    /**
     * The meta-property for the {@code shiftType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ShiftType> shiftType() {
      return shiftType;
    }

    /**
     * The meta-property for the {@code shiftAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> shiftAmount() {
      return shiftAmount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return ((ParallelShiftedCurve) bean).getCurve();
        case 893345500:  // shiftType
          return ((ParallelShiftedCurve) bean).getShiftType();
        case -1043480710:  // shiftAmount
          return ((ParallelShiftedCurve) bean).getShiftAmount();
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
   * The bean-builder for {@code ParallelShiftedCurve}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ParallelShiftedCurve> {

    private Curve curve;
    private ShiftType shiftType;
    private double shiftAmount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return curve;
        case 893345500:  // shiftType
          return shiftType;
        case -1043480710:  // shiftAmount
          return shiftAmount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          this.curve = (Curve) newValue;
          break;
        case 893345500:  // shiftType
          this.shiftType = (ShiftType) newValue;
          break;
        case -1043480710:  // shiftAmount
          this.shiftAmount = (Double) newValue;
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
    public ParallelShiftedCurve build() {
      return new ParallelShiftedCurve(
          curve,
          shiftType,
          shiftAmount);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ParallelShiftedCurve.Builder{");
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("shiftType").append('=').append(JodaBeanUtils.toString(shiftType)).append(',').append(' ');
      buf.append("shiftAmount").append('=').append(JodaBeanUtils.toString(shiftAmount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
