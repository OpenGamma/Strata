/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

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

import com.google.common.base.Preconditions;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * A curve based on a single constant value.
 * <p>
 * This class defines a curve in terms of a single parameter, the constant value.
 * When queried, {@link #yValue(double)} always returns the constant value.
 * <p>
 * The {@link #getXValues()} method returns a single x-value of 0.
 * The {@link #getYValues()} method returns a single y-value of the constant.
 * The sensitivity is 1 and the first derivative is 0.
 */
@BeanDefinition(builderScope = "private")
public final class ConstantNodalCurve
    implements NodalCurve, ImmutableBean, Serializable {

  /**
   * X-values does not vary.
   */
  private static final DoubleArray X_VALUES = DoubleArray.of(0d);
  /**
   * Sensitivity does not vary.
   */
  private static final DoubleArray SENSITIVITY = DoubleArray.of(1d);

  /**
   * The curve metadata.
   * <p>
   * The metadata will not normally have parameter metadata.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveMetadata metadata;
  /**
   * The single y-value.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final double yValue;

  //-------------------------------------------------------------------------
  /**
   * Creates a constant curve with a specific value.
   * 
   * @param name  the curve name
   * @param yValue  the constant y-value
   * @return the curve
   */
  public static ConstantNodalCurve of(String name, double yValue) {
    return of(CurveName.of(name), yValue);
  }

  /**
   * Creates a constant curve with a specific value.
   * 
   * @param name  the curve name
   * @param yValue  the constant y-value
   * @return the curve
   */
  public static ConstantNodalCurve of(CurveName name, double yValue) {
    return new ConstantNodalCurve(DefaultCurveMetadata.of(name), yValue);
  }

  /**
   * Creates a constant curve with a specific value.
   * 
   * @param metadata  the curve metadata
   * @param yValue  the constant y-value
   * @return the curve
   */
  public static ConstantNodalCurve of(CurveMetadata metadata, double yValue) {
    return new ConstantNodalCurve(metadata, yValue);
  }

  //-------------------------------------------------------------------------
  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ConstantNodalCurve(metadata, yValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return 1;
  }

  @Override
  public double getParameter(int parameterIndex) {
    Preconditions.checkPositionIndex(parameterIndex, 1);
    return yValue;
  }

  @Override
  public ConstantNodalCurve withParameter(int parameterIndex, double newValue) {
    Preconditions.checkPositionIndex(parameterIndex, 1);
    return new ConstantNodalCurve(metadata, newValue);
  }

  @Override
  public ConstantNodalCurve withPerturbation(ParameterPerturbation perturbation) {
    return new ConstantNodalCurve(metadata, perturbation.perturbParameter(0, yValue, getParameterMetadata(0)));
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleArray getXValues() {
    return X_VALUES;
  }

  @Override
  public DoubleArray getYValues() {
    return DoubleArray.of(yValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    return yValue;
  }

  @Override
  public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
    return CurveUnitParameterSensitivity.of(metadata, SENSITIVITY);
  }

  @Override
  public double firstDerivative(double x) {
    return 0d;
  }

  //-------------------------------------------------------------------------
  @Override
  public ConstantNodalCurve withMetadata(CurveMetadata metadata) {
    return new ConstantNodalCurve(metadata.withParameterMetadata(null), yValue);
  }

  @Override
  public ConstantNodalCurve withYValues(DoubleArray yValues) {
    ArgChecker.isTrue(yValues.size() == 1, "YValues array must be size one");
    return new ConstantNodalCurve(metadata, yValues.get(0));
  }

  @Override
  public ConstantNodalCurve shiftedBy(DoubleBinaryOperator operator) {
    return (ConstantNodalCurve) NodalCurve.super.shiftedBy(operator);
  }

  @Override
  public ConstantNodalCurve shiftedBy(List<ValueAdjustment> adjustments) {
    return (ConstantNodalCurve) NodalCurve.super.shiftedBy(adjustments);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ConstantNodalCurve}.
   * @return the meta-bean, not null
   */
  public static ConstantNodalCurve.Meta meta() {
    return ConstantNodalCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ConstantNodalCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ConstantNodalCurve(
      CurveMetadata metadata,
      double yValue) {
    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(yValue, "yValue");
    this.metadata = metadata;
    this.yValue = yValue;
  }

  @Override
  public ConstantNodalCurve.Meta metaBean() {
    return ConstantNodalCurve.Meta.INSTANCE;
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
   * Gets the curve metadata.
   * <p>
   * The metadata will not normally have parameter metadata.
   * @return the value of the property, not null
   */
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the single y-value.
   * @return the value of the property, not null
   */
  private double getYValue() {
    return yValue;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ConstantNodalCurve other = (ConstantNodalCurve) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(yValue, other.yValue);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValue);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ConstantNodalCurve{");
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("yValue").append('=').append(JodaBeanUtils.toString(yValue));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ConstantNodalCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<CurveMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", ConstantNodalCurve.class, CurveMetadata.class);
    /**
     * The meta-property for the {@code yValue} property.
     */
    private final MetaProperty<Double> yValue = DirectMetaProperty.ofImmutable(
        this, "yValue", ConstantNodalCurve.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "yValue");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case -748419976:  // yValue
          return yValue;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ConstantNodalCurve> builder() {
      return new ConstantNodalCurve.Builder();
    }

    @Override
    public Class<? extends ConstantNodalCurve> beanType() {
      return ConstantNodalCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code metadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveMetadata> metadata() {
      return metadata;
    }

    /**
     * The meta-property for the {@code yValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yValue() {
      return yValue;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((ConstantNodalCurve) bean).getMetadata();
        case -748419976:  // yValue
          return ((ConstantNodalCurve) bean).getYValue();
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
   * The bean-builder for {@code ConstantNodalCurve}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ConstantNodalCurve> {

    private CurveMetadata metadata;
    private double yValue;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case -748419976:  // yValue
          return yValue;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          this.metadata = (CurveMetadata) newValue;
          break;
        case -748419976:  // yValue
          this.yValue = (Double) newValue;
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
    public ConstantNodalCurve build() {
      return new ConstantNodalCurve(
          metadata,
          yValue);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ConstantNodalCurve.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("yValue").append('=').append(JodaBeanUtils.toString(yValue));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
