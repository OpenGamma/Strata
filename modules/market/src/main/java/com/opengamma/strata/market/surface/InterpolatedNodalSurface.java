/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.collect.tuple.ObjDoublePair;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;

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
  private final DoubleArray xValues;
  /**
   * The array of y-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DoubleArray yValues;
  /**
   * The array of z-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DoubleArray zValues;
  /**
   * The underlying interpolator.
   */
  @PropertyDefinition(validate = "notNull")
  private final SurfaceInterpolator interpolator;
  /**
   * The bound interpolator.
   */
  private final transient BoundSurfaceInterpolator boundInterpolator;  // derived and cached, not a property
  /**
   * The parameter metadata.
   */
  private final transient List<ParameterMetadata> parameterMetadata;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates an interpolated surface with metadata.
   * <p>
   * The value arrays must be sorted, by x-values then y-values.
   * An exception is thrown if they are not sorted.
   * 
   * @param metadata  the surface metadata
   * @param xValues  the x-values, must be sorted from low to high
   * @param yValues  the y-values, must be sorted from low to high within x
   * @param zValues  the z-values
   * @param interpolator  the interpolator
   * @return the surface
   */
  public static InterpolatedNodalSurface of(
      SurfaceMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      SurfaceInterpolator interpolator) {

    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator);
  }

  /**
   * Creates an interpolated surface with metadata, where the values are not sorted.
   * <p>
   * The value arrays will be sorted, by x-values then y-values.
   * Both the z-values and parameter metadata will be sorted along with the x and y values.
   * 
   * @param metadata  the surface metadata
   * @param xValues  the x-values
   * @param yValues  the y-values
   * @param zValues  the z-values
   * @param interpolator  the interpolator
   * @return the surface
   */
  public static InterpolatedNodalSurface ofUnsorted(
      SurfaceMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      SurfaceInterpolator interpolator) {

    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator, true);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private InterpolatedNodalSurface(
      SurfaceMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      SurfaceInterpolator interpolator) {

    validateInputs(metadata, xValues, yValues, zValues, interpolator);
    for (int i = 1; i < xValues.size(); i++) {
      if (xValues.get(i) < xValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of x-values must be sorted");
      }
      if (xValues.get(i) == xValues.get(i - 1) && yValues.get(i) <= yValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of y-values must be sorted and unique within x-values");
      }
    }
    this.metadata = metadata;
    this.xValues = xValues;
    this.yValues = yValues;
    this.zValues = zValues;
    this.interpolator = interpolator;
    this.boundInterpolator = interpolator.bind(xValues, yValues, zValues);
    this.parameterMetadata = IntStream.range(0, getParameterCount())
        .mapToObj(i -> metadata.getParameterMetadata(i))
        .collect(toImmutableList());
  }

  // constructor that sorts (artificial boolean flag)
  private InterpolatedNodalSurface(
      SurfaceMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      SurfaceInterpolator interpolator,
      boolean sort) {

    validateInputs(metadata, xValues, yValues, zValues, interpolator);
    // sort inputs
    Map<DoublesPair, ObjDoublePair<ParameterMetadata>> sorted = new TreeMap<>();
    for (int i = 0; i < xValues.size(); i++) {
      ParameterMetadata pm = metadata.getParameterMetadata(i);
      sorted.put(DoublesPair.of(xValues.get(i), yValues.get(i)), ObjDoublePair.of(pm, zValues.get(i)));
    }
    double[] sortedX = new double[sorted.size()];
    double[] sortedY = new double[sorted.size()];
    double[] sortedZ = new double[sorted.size()];
    ParameterMetadata[] sortedPm = new ParameterMetadata[sorted.size()];
    int pos = 0;
    for (Entry<DoublesPair, ObjDoublePair<ParameterMetadata>> entry : sorted.entrySet()) {
      sortedX[pos] = entry.getKey().getFirst();
      sortedY[pos] = entry.getKey().getSecond();
      sortedZ[pos] = entry.getValue().getSecond();
      sortedPm[pos] = entry.getValue().getFirst();
      pos++;
    }
    // assign
    SurfaceMetadata sortedMetadata = metadata.withParameterMetadata(Arrays.asList(sortedPm));
    this.metadata = sortedMetadata;
    this.xValues = DoubleArray.ofUnsafe(sortedX);
    this.yValues = DoubleArray.ofUnsafe(sortedY);
    this.zValues = DoubleArray.ofUnsafe(sortedZ);
    Map<DoublesPair, Double> pairs = new HashMap<>();
    for (int i = 0; i < xValues.size(); i++) {
      pairs.put(DoublesPair.of(xValues.get(i), yValues.get(i)), zValues.get(i));
    }
    this.interpolator = interpolator;
    this.boundInterpolator = interpolator.bind(this.xValues, this.yValues, this.zValues);
    this.parameterMetadata = IntStream.range(0, getParameterCount())
        .mapToObj(i -> sortedMetadata.getParameterMetadata(i))
        .collect(toImmutableList());
  }

  // basic validation
  private void validateInputs(
      SurfaceMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      SurfaceInterpolator interpolator) {

    ArgChecker.notNull(metadata, "metadata");
    ArgChecker.notNull(xValues, "times");
    ArgChecker.notNull(yValues, "values");
    ArgChecker.notNull(interpolator, "interpolator");
    if (xValues.size() < 2) {
      throw new IllegalArgumentException("Length of x-values must be at least 2");
    }
    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException("Length of x-values and y-values must match");
    }
    if (xValues.size() != zValues.size()) {
      throw new IllegalArgumentException("Length of x-values and z-values must match");
    }
    metadata.getParameterMetadata().ifPresent(params -> {
      if (xValues.size() != params.size()) {
        throw new IllegalArgumentException("Length of x-values and parameter metadata must match when metadata present");
      }
    });
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return zValues.size();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return zValues.get(parameterIndex);
  }

  @Override
  public InterpolatedNodalSurface withParameter(int parameterIndex, double newValue) {
    return withZValues(zValues.with(parameterIndex, newValue));
  }

  @Override
  public InterpolatedNodalSurface withPerturbation(ParameterPerturbation perturbation) {
    int size = zValues.size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, zValues.get(i), getParameterMetadata(i)));
    return withZValues(perturbedValues);
  }

  //-------------------------------------------------------------------------
  @Override
  public double zValue(double x, double y) {
    return boundInterpolator.interpolate(x, y);
  }

  @Override
  public UnitParameterSensitivity zValueParameterSensitivity(double x, double y) {
    DoubleArray sensitivityValues = boundInterpolator.parameterSensitivity(x, y);
    return createParameterSensitivity(sensitivityValues);
  }

  //-------------------------------------------------------------------------
  @Override
  public ValueDerivatives firstPartialDerivatives(double x, double y) {
    return boundInterpolator.firstPartialDerivatives(x, y);
  }

  //-------------------------------------------------------------------------
  @Override
  public InterpolatedNodalSurface withMetadata(SurfaceMetadata metadata) {
    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator);
  }

  @Override
  public InterpolatedNodalSurface withZValues(DoubleArray zValues) {
    return new InterpolatedNodalSurface(metadata, xValues, yValues, zValues, interpolator);
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
  /**
   * The meta-bean for {@code InterpolatedNodalSurface}.
   * @return the meta-bean, not null
   */
  public static InterpolatedNodalSurface.Meta meta() {
    return InterpolatedNodalSurface.Meta.INSTANCE;
  }

  static {
    MetaBean.register(InterpolatedNodalSurface.Meta.INSTANCE);
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
  public DoubleArray getXValues() {
    return xValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of y-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   * @return the value of the property, not null
   */
  @Override
  public DoubleArray getYValues() {
    return yValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of z-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   * @return the value of the property, not null
   */
  @Override
  public DoubleArray getZValues() {
    return zValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying interpolator.
   * @return the value of the property, not null
   */
  public SurfaceInterpolator getInterpolator() {
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
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(xValues, other.xValues) &&
          JodaBeanUtils.equal(yValues, other.yValues) &&
          JodaBeanUtils.equal(zValues, other.zValues) &&
          JodaBeanUtils.equal(interpolator, other.interpolator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(zValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("InterpolatedNodalSurface{");
    buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
    buf.append("xValues").append('=').append(JodaBeanUtils.toString(xValues)).append(',').append(' ');
    buf.append("yValues").append('=').append(JodaBeanUtils.toString(yValues)).append(',').append(' ');
    buf.append("zValues").append('=').append(JodaBeanUtils.toString(zValues)).append(',').append(' ');
    buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator));
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
    private final MetaProperty<DoubleArray> xValues = DirectMetaProperty.ofImmutable(
        this, "xValues", InterpolatedNodalSurface.class, DoubleArray.class);
    /**
     * The meta-property for the {@code yValues} property.
     */
    private final MetaProperty<DoubleArray> yValues = DirectMetaProperty.ofImmutable(
        this, "yValues", InterpolatedNodalSurface.class, DoubleArray.class);
    /**
     * The meta-property for the {@code zValues} property.
     */
    private final MetaProperty<DoubleArray> zValues = DirectMetaProperty.ofImmutable(
        this, "zValues", InterpolatedNodalSurface.class, DoubleArray.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<SurfaceInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", InterpolatedNodalSurface.class, SurfaceInterpolator.class);
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
    public MetaProperty<DoubleArray> xValues() {
      return xValues;
    }

    /**
     * The meta-property for the {@code yValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> yValues() {
      return yValues;
    }

    /**
     * The meta-property for the {@code zValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> zValues() {
      return zValues;
    }

    /**
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SurfaceInterpolator> interpolator() {
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
    private DoubleArray xValues;
    private DoubleArray yValues;
    private DoubleArray zValues;
    private SurfaceInterpolator interpolator;

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
      this.xValues = beanToCopy.getXValues();
      this.yValues = beanToCopy.getYValues();
      this.zValues = beanToCopy.getZValues();
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
          this.xValues = (DoubleArray) newValue;
          break;
        case -1726182661:  // yValues
          this.yValues = (DoubleArray) newValue;
          break;
        case -838678980:  // zValues
          this.zValues = (DoubleArray) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (SurfaceInterpolator) newValue;
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
    public Builder xValues(DoubleArray xValues) {
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
    public Builder yValues(DoubleArray yValues) {
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
    public Builder zValues(DoubleArray zValues) {
      JodaBeanUtils.notNull(zValues, "zValues");
      this.zValues = zValues;
      return this;
    }

    /**
     * Sets the underlying interpolator.
     * @param interpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolator(SurfaceInterpolator interpolator) {
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

  //-------------------------- AUTOGENERATED END --------------------------
}
