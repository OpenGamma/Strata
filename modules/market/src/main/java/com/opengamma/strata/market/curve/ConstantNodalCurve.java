/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A curve based on a single constant value.
 * <p>
 * This class defines a curve in terms of a single node point. 
 * The resulting curve is a constant curve with the y-value of the node point.
 * When queried, {@link #yValue(double)} always returns the constant value.
 * The x-value is not significant in most use cases.
 * See {@link ConstantCurve} for an alternative that does not have an x-value.
 * <p>
 * The {@link #getXValues()} method returns the single x-value of the node.
 * The {@link #getYValues()} method returns the single y-value of the node.
 * The sensitivity is 1 and the first derivative is 0.
 */
@BeanDefinition
public final class ConstantNodalCurve
    implements NodalCurve, ImmutableBean, Serializable {

  /**
   * The curve metadata.
   * <p>
   * The metadata will have a single parameter metadata.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveMetadata metadata;
  /**
   * The single x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final double xValue;
  /**
   * The single y-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final double yValue;
  /**
   * The parameter metadata.
   */
  private final transient List<ParameterMetadata> parameterMetadata;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates a constant nodal curve with metadata.
   * <p>
   * The curve is defined by a single x and y value.
   * 
   * @param metadata  the curve metadata
   * @param xValue  the x-value
   * @param yValue  the y-value
   * @return the curve
   */
  public static ConstantNodalCurve of(CurveMetadata metadata, double xValue, double yValue) {
    return new ConstantNodalCurve(metadata, xValue, yValue);
  }

  /**
   * Creates a constant nodal curve with metadata.
   * <p>
   * The curve is defined by a single x and y value.
   * 
   * @param metadata  the curve metadata
   * @param xValue  the x-value
   * @param yValue  the y-value
   * @return the curve
   * @deprecated Use {@link #of(CurveMetadata, double, double)}
   */
  @Deprecated
  public static ConstantNodalCurve of(CurveMetadata metadata, DoubleArray xValue, DoubleArray yValue) {
    if (xValue.size() != 1 || yValue.size() != 1) {
      throw new IllegalArgumentException("Length of x-values and y-values must be 1");
    }
    return new ConstantNodalCurve(metadata, xValue.get(0), yValue.get(0));
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private ConstantNodalCurve(
      CurveMetadata metadata,
      double xValue,
      double yValue) {
    JodaBeanUtils.notNull(metadata, "metadata");
    metadata.getParameterMetadata().ifPresent(params -> {
      if (params.size() != 1) {
        throw new IllegalArgumentException("Length of parameter metadata must be 1");
      }
    });
    this.metadata = metadata;
    this.xValue = xValue;
    this.yValue = yValue;
    this.parameterMetadata = ImmutableList.of(getParameterMetadata(0));
  }

  // resolve after deserialization
  private Object readResolve() {
    return new ConstantNodalCurve(metadata, xValue, yValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return 1;
  }

  @Override
  public double getParameter(int parameterIndex) {
    ArgChecker.isTrue(parameterIndex == 0, "single parameter");
    return yValue;
  }

  @Override
  public ConstantNodalCurve withParameter(int parameterIndex, double newValue) {
    ArgChecker.isTrue(parameterIndex == 0, "single parameter");
    return new ConstantNodalCurve(metadata, xValue, newValue);
  }

  @Override
  public ConstantNodalCurve withPerturbation(ParameterPerturbation perturbation) {
    double perturbedValue = perturbation.perturbParameter(0, yValue, getParameterMetadata(0));
    return new ConstantNodalCurve(metadata, xValue, perturbedValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleArray getXValues() {
    return DoubleArray.of(xValue);
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
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    return createParameterSensitivity(DoubleArray.of(1d));
  }

  @Override
  public double firstDerivative(double x) {
    return 0d;
  }

  //-------------------------------------------------------------------------
  @Override
  public ConstantNodalCurve withMetadata(CurveMetadata metadata) {
    return new ConstantNodalCurve(metadata, xValue, yValue);
  }

  @Override
  public ConstantNodalCurve withYValues(DoubleArray yValues) {
    ArgChecker.isTrue(yValues.size() == 1, "Invalid number of parameters, only one allowed");
    return new ConstantNodalCurve(metadata, xValue, yValues.get(0));
  }

  @Override
  public ConstantNodalCurve withValues(DoubleArray xValues, DoubleArray yValues) {
    ArgChecker.isTrue(xValues.size() == 1 && yValues.size() == 1, "Invalid number of parameters, only one allowed");
    return new ConstantNodalCurve(metadata, xValues.get(0), yValues.get(0));
  }

  //-------------------------------------------------------------------------
  @Override
  public ConstantNodalCurve withNode(double x, double y, ParameterMetadata paramMetadata) {
    ArgChecker.isTrue(x == xValue, "x should be equal to the existing x-value");
    CurveMetadata md = metadata.withParameterMetadata(ImmutableList.of(paramMetadata));
    return new ConstantNodalCurve(md, x, y);
  }

  //-------------------------------------------------------------------------
  @Override
  public UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    return UnitParameterSensitivity.of(getName(), parameterMetadata, sensitivities);
  }

  @Override
  public CurrencyParameterSensitivity createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivity.of(getName(), parameterMetadata, currency, sensitivities);
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

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ConstantNodalCurve.Builder builder() {
    return new ConstantNodalCurve.Builder();
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
   * The metadata will have a single parameter metadata.
   * @return the value of the property, not null
   */
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the single x-value.
   * @return the value of the property, not null
   */
  public double getXValue() {
    return xValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the single y-value.
   * @return the value of the property, not null
   */
  public double getYValue() {
    return yValue;
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
      ConstantNodalCurve other = (ConstantNodalCurve) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(xValue, other.xValue) &&
          JodaBeanUtils.equal(yValue, other.yValue);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValue);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ConstantNodalCurve{");
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("xValue").append('=').append(xValue).append(',').append(' ');
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
     * The meta-property for the {@code xValue} property.
     */
    private final MetaProperty<Double> xValue = DirectMetaProperty.ofImmutable(
        this, "xValue", ConstantNodalCurve.class, Double.TYPE);
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
        "xValue",
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
        case -777049127:  // xValue
          return xValue;
        case -748419976:  // yValue
          return yValue;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ConstantNodalCurve.Builder builder() {
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
     * The meta-property for the {@code xValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> xValue() {
      return xValue;
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
        case -777049127:  // xValue
          return ((ConstantNodalCurve) bean).getXValue();
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
  public static final class Builder extends DirectFieldsBeanBuilder<ConstantNodalCurve> {

    private CurveMetadata metadata;
    private double xValue;
    private double yValue;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ConstantNodalCurve beanToCopy) {
      this.metadata = beanToCopy.getMetadata();
      this.xValue = beanToCopy.getXValue();
      this.yValue = beanToCopy.getYValue();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case -777049127:  // xValue
          return xValue;
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
        case -777049127:  // xValue
          this.xValue = (Double) newValue;
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
          xValue,
          yValue);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the curve metadata.
     * <p>
     * The metadata will have a single parameter metadata.
     * @param metadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder metadata(CurveMetadata metadata) {
      JodaBeanUtils.notNull(metadata, "metadata");
      this.metadata = metadata;
      return this;
    }

    /**
     * Sets the single x-value.
     * @param xValue  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder xValue(double xValue) {
      JodaBeanUtils.notNull(xValue, "xValue");
      this.xValue = xValue;
      return this;
    }

    /**
     * Sets the single y-value.
     * @param yValue  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yValue(double yValue) {
      JodaBeanUtils.notNull(yValue, "yValue");
      this.yValue = yValue;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ConstantNodalCurve.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("xValue").append('=').append(JodaBeanUtils.toString(xValue)).append(',').append(' ');
      buf.append("yValue").append('=').append(JodaBeanUtils.toString(yValue));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
