/*
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.param.LabelParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

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
 * A shift amount of -0.2 is a shift of -20% which multiplies the value by 0.8.
 * <p>
 * The parameters consist of the parameters of the underlying curve, followed by the shift.
 */
@BeanDefinition(builderScope = "private")
public final class ParallelShiftedCurve
    implements Curve, ImmutableBean, Serializable {

  /**
   * The underlying curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve underlyingCurve;
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
    return underlyingCurve.getMetadata();
  }

  @Override
  public ParallelShiftedCurve withMetadata(CurveMetadata metadata) {
    return new ParallelShiftedCurve(underlyingCurve.withMetadata(metadata), shiftType, shiftAmount);
  }

  @Override
  public CurveName getName() {
    return underlyingCurve.getName();
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return underlyingCurve.getParameterCount() + 1;
  }

  @Override
  public double getParameter(int parameterIndex) {
    if (parameterIndex == underlyingCurve.getParameterCount()) {
      return shiftAmount;
    }
    return underlyingCurve.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    if (parameterIndex == underlyingCurve.getParameterCount()) {
      return LabelParameterMetadata.of(shiftType + "Shift");
    }
    return underlyingCurve.getParameterMetadata(parameterIndex);
  }

  @Override
  public ParallelShiftedCurve withParameter(int parameterIndex, double newValue) {
    if (parameterIndex == underlyingCurve.getParameterCount()) {
      return new ParallelShiftedCurve(underlyingCurve, shiftType, newValue);
    }
    return new ParallelShiftedCurve(underlyingCurve.withParameter(parameterIndex, newValue), shiftType, shiftAmount);
  }

  @Override
  public ParallelShiftedCurve withPerturbation(ParameterPerturbation perturbation) {
    Curve bumpedCurve = underlyingCurve.withPerturbation(perturbation);
    int shiftIndex = underlyingCurve.getParameterCount();
    double bumpedShift = perturbation.perturbParameter(shiftIndex, shiftAmount, getParameterMetadata(shiftIndex));
    return new ParallelShiftedCurve(bumpedCurve, shiftType, bumpedShift);
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    return shiftType.applyShift(underlyingCurve.yValue(x), shiftAmount);
  }

  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    return underlyingCurve.yValueParameterSensitivity(x);
  }

  @Override
  public double firstDerivative(double x) {
    double firstDerivative = underlyingCurve.firstDerivative(x);
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
      Curve underlyingCurve,
      ShiftType shiftType,
      double shiftAmount) {
    JodaBeanUtils.notNull(underlyingCurve, "underlyingCurve");
    JodaBeanUtils.notNull(shiftType, "shiftType");
    JodaBeanUtils.notNull(shiftAmount, "shiftAmount");
    this.underlyingCurve = underlyingCurve;
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
  public Curve getUnderlyingCurve() {
    return underlyingCurve;
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
      return JodaBeanUtils.equal(underlyingCurve, other.underlyingCurve) &&
          JodaBeanUtils.equal(shiftType, other.shiftType) &&
          JodaBeanUtils.equal(shiftAmount, other.shiftAmount);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftType);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftAmount);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ParallelShiftedCurve{");
    buf.append("underlyingCurve").append('=').append(underlyingCurve).append(',').append(' ');
    buf.append("shiftType").append('=').append(shiftType).append(',').append(' ');
    buf.append("shiftAmount").append('=').append(JodaBeanUtils.toString(shiftAmount));
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
     * The meta-property for the {@code underlyingCurve} property.
     */
    private final MetaProperty<Curve> underlyingCurve = DirectMetaProperty.ofImmutable(
        this, "underlyingCurve", ParallelShiftedCurve.class, Curve.class);
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
        "underlyingCurve",
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
        case -839394414:  // underlyingCurve
          return underlyingCurve;
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
     * The meta-property for the {@code underlyingCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> underlyingCurve() {
      return underlyingCurve;
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
        case -839394414:  // underlyingCurve
          return ((ParallelShiftedCurve) bean).getUnderlyingCurve();
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
  private static final class Builder extends DirectPrivateBeanBuilder<ParallelShiftedCurve> {

    private Curve underlyingCurve;
    private ShiftType shiftType;
    private double shiftAmount;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -839394414:  // underlyingCurve
          return underlyingCurve;
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
        case -839394414:  // underlyingCurve
          this.underlyingCurve = (Curve) newValue;
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
    public ParallelShiftedCurve build() {
      return new ParallelShiftedCurve(
          underlyingCurve,
          shiftType,
          shiftAmount);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ParallelShiftedCurve.Builder{");
      buf.append("underlyingCurve").append('=').append(JodaBeanUtils.toString(underlyingCurve)).append(',').append(' ');
      buf.append("shiftType").append('=').append(JodaBeanUtils.toString(shiftType)).append(',').append(' ');
      buf.append("shiftAmount").append('=').append(JodaBeanUtils.toString(shiftAmount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
