/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.io.Serializable;
import java.util.HashMap;
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

import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.function.DoubleTenaryOperator;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A surface based on interpolation between a number of nodal points.
 * <p>
 * This class defines a surface in terms of a fixed number of nodes, referred to as <i>parameters</i>.
 * <p>
 * Each node has an x-value and a y-value.
 * The interface is focused on finding the z-value for a given x-value and y-value.
 * An interpolator is used to find z-values for x-values and y-values between two nodes.
 */
@BeanDefinition
public final class InterpolatedNodalSurface
    implements NodalSurface, ImmutableBean, Serializable {

  /**
   * The surface metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this surface.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SurfaceMetadata metadata;
  /**
   * The array of x-values, one for each point.
   * <p>
   * This array will contains at least two elements.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final double[] xValues;
  /**
   * The array of y-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final double[] yValues;
  /**
   * The array of z-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final double[] zValues;
  /**
   * The underlying interpolator.
   */
  @PropertyDefinition(validate = "notNull")
  private final GridInterpolator2D interpolator;
  /**
   * The underlying data bundle.
   */
  private transient final Map<Double, Interpolator1DDataBundle> underlyingDataBundle;  // derived and cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates an interpolated surface with metadata.
   * <p>
   * The extrapolators will be flat.
   * For more control, use the builder.
   * 
   * @param metadata  the surface metadata
   * @param xValues  the x-values
   * @param yValues  the y-values
   * @param zValues  the z-values
   * @param interpolator  the interpolator
   * @return the surface
   */
  public static InterpolatedNodalSurface of(
      SurfaceMetadata metadata,
      double[] xValues,
      double[] yValues,
      double[] zValues,
      GridInterpolator2D interpolator) {

    return InterpolatedNodalSurface.builder()
        .metadata(metadata)
        .xValues(xValues)
        .yValues(yValues)
        .zValues(zValues)
        .interpolator(interpolator)
        .build();
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private InterpolatedNodalSurface(
      SurfaceMetadata metadata,
      double[] xValues,
      double[] yValues,
      double[] zValues,
      GridInterpolator2D interpolator) {
    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(xValues, "times");
    JodaBeanUtils.notNull(yValues, "values");
    JodaBeanUtils.notNull(interpolator, "interpolator");
    if (xValues.length < 2) {
      throw new IllegalArgumentException("Length of x-values must be at least 2");
    }
    if (xValues.length != yValues.length) {
      throw new IllegalArgumentException("Length of x-values and y-values must match");
    }
    if (xValues.length != zValues.length) {
      throw new IllegalArgumentException("Length of x-values and z-values must match");
    }
    metadata.getParameterMetadata().ifPresent(params -> {
      if (xValues.length != params.size()) {
        throw new IllegalArgumentException("Length of x-values and parameter metadata must match when metadata present");
      }
    });
    this.metadata = metadata;
    this.xValues = xValues.clone();
    this.yValues = yValues.clone();
    this.zValues = zValues.clone();
    Map<DoublesPair, Double> pairs = new HashMap<>();
    for (int i = 0; i < xValues.length; i++) {
      pairs.put(DoublesPair.of(xValues[i], yValues[i]), zValues[i]);
    }
    this.interpolator = interpolator;
    underlyingDataBundle = interpolator.getDataBundle(pairs);
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return xValues.length;
  }

  //-------------------------------------------------------------------------
  @Override
  public double zValue(double x, double y) {
    return zValue(DoublesPair.of(x, y));
  }

  @Override
  public double zValue(DoublesPair xyPair) {
    return interpolator.interpolate(underlyingDataBundle, xyPair);
  }

  @Override
  public Map<DoublesPair, Double> zValueParameterSensitivity(double x, double y) {
    return zValueParameterSensitivity(DoublesPair.of(x, y));
  }

  @Override
  public Map<DoublesPair, Double> zValueParameterSensitivity(DoublesPair xyPair) {
    return interpolator.getNodeSensitivitiesForValue(underlyingDataBundle, xyPair);
  }

  //-------------------------------------------------------------------------
  @Override
  public InterpolatedNodalSurface withZValues(double[] zValues) {
    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator);
  }

  @Override
  public InterpolatedNodalSurface shiftedBy(DoubleTenaryOperator operator) {
    return (InterpolatedNodalSurface) NodalSurface.super.shiftedBy(operator);
  }

  @Override
  public InterpolatedNodalSurface shiftedBy(List<ValueAdjustment> adjustments) {
    return (InterpolatedNodalSurface) NodalSurface.super.shiftedBy(adjustments);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InterpolatedNodalSurface}.
   * @return the meta-bean, not null
   */
  public static InterpolatedNodalSurface.Meta meta() {
    return InterpolatedNodalSurface.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InterpolatedNodalSurface.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InterpolatedNodalSurface.Builder builder() {
    return new InterpolatedNodalSurface.Builder();
  }

  @Override
  public InterpolatedNodalSurface.Meta metaBean() {
    return InterpolatedNodalSurface.Meta.INSTANCE;
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
   * Gets the surface metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this surface.
   * @return the value of the property, not null
   */
  @Override
  public SurfaceMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of x-values, one for each point.
   * <p>
   * This array will contains at least two elements.
   * @return the value of the property, not null
   */
  @Override
  public double[] getXValues() {
    return (xValues != null ? xValues.clone() : null);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of y-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   * @return the value of the property, not null
   */
  @Override
  public double[] getYValues() {
    return (yValues != null ? yValues.clone() : null);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of z-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   * @return the value of the property, not null
   */
  @Override
  public double[] getZValues() {
    return (zValues != null ? zValues.clone() : null);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying interpolator.
   * @return the value of the property, not null
   */
  public GridInterpolator2D getInterpolator() {
    return interpolator;
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
      InterpolatedNodalSurface other = (InterpolatedNodalSurface) obj;
      return JodaBeanUtils.equal(getMetadata(), other.getMetadata()) &&
          JodaBeanUtils.equal(getXValues(), other.getXValues()) &&
          JodaBeanUtils.equal(getYValues(), other.getYValues()) &&
          JodaBeanUtils.equal(getZValues(), other.getZValues()) &&
          JodaBeanUtils.equal(getInterpolator(), other.getInterpolator());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getMetadata());
    hash = hash * 31 + JodaBeanUtils.hashCode(getXValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getYValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getZValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getInterpolator());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("InterpolatedNodalSurface{");
    buf.append("metadata").append('=').append(getMetadata()).append(',').append(' ');
    buf.append("xValues").append('=').append(getXValues()).append(',').append(' ');
    buf.append("yValues").append('=').append(getYValues()).append(',').append(' ');
    buf.append("zValues").append('=').append(getZValues()).append(',').append(' ');
    buf.append("interpolator").append('=').append(JodaBeanUtils.toString(getInterpolator()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InterpolatedNodalSurface}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<SurfaceMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", InterpolatedNodalSurface.class, SurfaceMetadata.class);
    /**
     * The meta-property for the {@code xValues} property.
     */
    private final MetaProperty<double[]> xValues = DirectMetaProperty.ofImmutable(
        this, "xValues", InterpolatedNodalSurface.class, double[].class);
    /**
     * The meta-property for the {@code yValues} property.
     */
    private final MetaProperty<double[]> yValues = DirectMetaProperty.ofImmutable(
        this, "yValues", InterpolatedNodalSurface.class, double[].class);
    /**
     * The meta-property for the {@code zValues} property.
     */
    private final MetaProperty<double[]> zValues = DirectMetaProperty.ofImmutable(
        this, "zValues", InterpolatedNodalSurface.class, double[].class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<GridInterpolator2D> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", InterpolatedNodalSurface.class, GridInterpolator2D.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "xValues",
        "yValues",
        "zValues",
        "interpolator");

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
        case 1681280954:  // xValues
          return xValues;
        case -1726182661:  // yValues
          return yValues;
        case -838678980:  // zValues
          return zValues;
        case 2096253127:  // interpolator
          return interpolator;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InterpolatedNodalSurface.Builder builder() {
      return new InterpolatedNodalSurface.Builder();
    }

    @Override
    public Class<? extends InterpolatedNodalSurface> beanType() {
      return InterpolatedNodalSurface.class;
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
    public MetaProperty<SurfaceMetadata> metadata() {
      return metadata;
    }

    /**
     * The meta-property for the {@code xValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> xValues() {
      return xValues;
    }

    /**
     * The meta-property for the {@code yValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> yValues() {
      return yValues;
    }

    /**
     * The meta-property for the {@code zValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> zValues() {
      return zValues;
    }

    /**
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<GridInterpolator2D> interpolator() {
      return interpolator;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((InterpolatedNodalSurface) bean).getMetadata();
        case 1681280954:  // xValues
          return ((InterpolatedNodalSurface) bean).getXValues();
        case -1726182661:  // yValues
          return ((InterpolatedNodalSurface) bean).getYValues();
        case -838678980:  // zValues
          return ((InterpolatedNodalSurface) bean).getZValues();
        case 2096253127:  // interpolator
          return ((InterpolatedNodalSurface) bean).getInterpolator();
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
   * The bean-builder for {@code InterpolatedNodalSurface}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InterpolatedNodalSurface> {

    private SurfaceMetadata metadata;
    private double[] xValues;
    private double[] yValues;
    private double[] zValues;
    private GridInterpolator2D interpolator;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InterpolatedNodalSurface beanToCopy) {
      this.metadata = beanToCopy.getMetadata();
      this.xValues = beanToCopy.getXValues().clone();
      this.yValues = beanToCopy.getYValues().clone();
      this.zValues = beanToCopy.getZValues().clone();
      this.interpolator = beanToCopy.getInterpolator();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case 1681280954:  // xValues
          return xValues;
        case -1726182661:  // yValues
          return yValues;
        case -838678980:  // zValues
          return zValues;
        case 2096253127:  // interpolator
          return interpolator;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          this.metadata = (SurfaceMetadata) newValue;
          break;
        case 1681280954:  // xValues
          this.xValues = (double[]) newValue;
          break;
        case -1726182661:  // yValues
          this.yValues = (double[]) newValue;
          break;
        case -838678980:  // zValues
          this.zValues = (double[]) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (GridInterpolator2D) newValue;
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
    public InterpolatedNodalSurface build() {
      return new InterpolatedNodalSurface(
          metadata,
          xValues,
          yValues,
          zValues,
          interpolator);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the surface metadata.
     * <p>
     * The metadata includes an optional list of parameter metadata.
     * If present, the size of the parameter metadata list will match the number of parameters of this surface.
     * @param metadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder metadata(SurfaceMetadata metadata) {
      JodaBeanUtils.notNull(metadata, "metadata");
      this.metadata = metadata;
      return this;
    }

    /**
     * Sets the array of x-values, one for each point.
     * <p>
     * This array will contains at least two elements.
     * @param xValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder xValues(double... xValues) {
      JodaBeanUtils.notNull(xValues, "xValues");
      this.xValues = xValues;
      return this;
    }

    /**
     * Sets the array of y-values, one for each point.
     * <p>
     * This array will contains at least two elements and be of the same length as x-values.
     * @param yValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yValues(double... yValues) {
      JodaBeanUtils.notNull(yValues, "yValues");
      this.yValues = yValues;
      return this;
    }

    /**
     * Sets the array of z-values, one for each point.
     * <p>
     * This array will contains at least two elements and be of the same length as x-values.
     * @param zValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder zValues(double... zValues) {
      JodaBeanUtils.notNull(zValues, "zValues");
      this.zValues = zValues;
      return this;
    }

    /**
     * Sets the underlying interpolator.
     * @param interpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolator(GridInterpolator2D interpolator) {
      JodaBeanUtils.notNull(interpolator, "interpolator");
      this.interpolator = interpolator;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("InterpolatedNodalSurface.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("xValues").append('=').append(JodaBeanUtils.toString(xValues)).append(',').append(' ');
      buf.append("yValues").append('=').append(JodaBeanUtils.toString(yValues)).append(',').append(' ');
      buf.append("zValues").append('=').append(JodaBeanUtils.toString(zValues)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
