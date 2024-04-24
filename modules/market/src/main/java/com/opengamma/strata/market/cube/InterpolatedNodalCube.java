/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import com.opengamma.strata.collect.tuple.ObjDoublePair;
import com.opengamma.strata.collect.tuple.Triple;
import com.opengamma.strata.market.cube.interpolator.BoundCubeInterpolator;
import com.opengamma.strata.market.cube.interpolator.CubeInterpolator;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A cube based on interpolation between a number of nodal points.
 * <p>
 * This class defines a cube in terms of a fixed number of nodes, referred to as <i>parameters</i>.
 * <p>
 * Each node has x-value, y-value, z-value, w-value.
 * The interface is focused on finding the w-value for a given x-value, y-value, z-value.
 * An interpolator is used to find w-values for x-values, y-values, z-values between nodes.
 */
@BeanDefinition
public final class InterpolatedNodalCube
    implements NodalCube, ImmutableBean, Serializable {

  /**
   * The cube metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this cube.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CubeMetadata metadata;
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
   * The array of w-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DoubleArray wValues;
  /**
   * The underlying interpolator.
   */
  @PropertyDefinition(validate = "notNull")
  private final CubeInterpolator interpolator;
  /**
   * The bound interpolator.
   */
  private final transient BoundCubeInterpolator boundInterpolator;  // derived and cached, not a property
  /**
   * The parameter metadata.
   */
  private final transient List<ParameterMetadata> parameterMetadata;  // derived, not a property

  //-------------------------------------------------------------------------

  /**
   * Creates an interpolated cube with metadata.
   * <p>
   * The value arrays must be sorted, by x-values then y-values, z-values.
   * An exception is thrown if they are not sorted.
   *
   * @param metadata the cube metadata
   * @param xValues the x-values, must be sorted from low to high
   * @param yValues the y-values, must be sorted from low to high within x
   * @param zValues the z-values, must be sorted from low to high within x, y
   * @param wValues the w-values
   * @param interpolator the interpolator
   * @return the cube
   */
  public static InterpolatedNodalCube of(
      CubeMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues,
      CubeInterpolator interpolator) {

    return new InterpolatedNodalCube(metadata, xValues, yValues, zValues, wValues, interpolator);
  }

  /**
   * Creates an interpolated cube with metadata, where the values are not sorted.
   * <p>
   * The value arrays will be sorted, by x-values then y-values, z-values.
   * Both the w-values and parameter metadata will be sorted along with the x, y, z values.
   *
   * @param metadata the cube metadata
   * @param xValues the x-values
   * @param yValues the y-values
   * @param zValues the z-values
   * @param wValues the w-values
   * @param interpolator the interpolator
   * @return the cube
   */
  public static InterpolatedNodalCube ofUnsorted(
      CubeMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues,
      CubeInterpolator interpolator) {

    return new InterpolatedNodalCube(metadata, xValues, yValues, zValues, wValues, interpolator, true);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private InterpolatedNodalCube(
      CubeMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues,
      CubeInterpolator interpolator) {

    validateInputs(metadata, xValues, yValues, zValues, wValues, interpolator);
    for (int i = 1; i < xValues.size(); i++) {
      if (xValues.get(i) < xValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of x-values must be sorted");
      }
      if (xValues.get(i) == xValues.get(i - 1) && yValues.get(i) < yValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of y-values must be sorted");
      }
      if (xValues.get(i) == xValues.get(i - 1) && yValues.get(i) == yValues.get(i - 1) && zValues.get(i) < zValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of z-values must be sorted");
      }
    }
    this.metadata = metadata;
    this.xValues = xValues;
    this.yValues = yValues;
    this.zValues = zValues;
    this.wValues = wValues;
    this.interpolator = interpolator;
    this.boundInterpolator = interpolator.bind(xValues, yValues, zValues, wValues);
    this.parameterMetadata = IntStream.range(0, getParameterCount())
        .mapToObj(i -> metadata.getParameterMetadata(i))
        .collect(toImmutableList());
  }

  // constructor that sorts (artificial boolean flag)
  private InterpolatedNodalCube(
      CubeMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues,
      CubeInterpolator interpolator,
      boolean sort) {

    validateInputs(metadata, xValues, yValues, zValues, wValues, interpolator);
    // sort inputs
    Map<Triple<Double, Double, Double>, ObjDoublePair<ParameterMetadata>> sorted = new TreeMap<>();
    for (int i = 0; i < xValues.size(); i++) {
      ParameterMetadata pm = metadata.getParameterMetadata(i);
      sorted.put(Triple.of(xValues.get(i), yValues.get(i), zValues.get(i)), ObjDoublePair.of(pm, wValues.get(i)));
    }
    double[] sortedX = new double[sorted.size()];
    double[] sortedY = new double[sorted.size()];
    double[] sortedZ = new double[sorted.size()];
    double[] sortedW = new double[sorted.size()];
    ParameterMetadata[] sortedPm = new ParameterMetadata[sorted.size()];
    int pos = 0;
    for (Map.Entry<Triple<Double, Double, Double>, ObjDoublePair<ParameterMetadata>> entry : sorted.entrySet()) {
      sortedX[pos] = entry.getKey().getFirst();
      sortedY[pos] = entry.getKey().getSecond();
      sortedZ[pos] = entry.getKey().getThird();
      sortedW[pos] = entry.getValue().getSecond();
      sortedPm[pos] = entry.getValue().getFirst();
      pos++;
    }
    // assign
    CubeMetadata sortedMetadata = metadata.withParameterMetadata(Arrays.asList(sortedPm));
    this.metadata = sortedMetadata;
    this.xValues = DoubleArray.ofUnsafe(sortedX);
    this.yValues = DoubleArray.ofUnsafe(sortedY);
    this.zValues = DoubleArray.ofUnsafe(sortedZ);
    this.wValues = DoubleArray.ofUnsafe(sortedW);
    this.interpolator = interpolator;
    this.boundInterpolator = interpolator.bind(this.xValues, this.yValues, this.zValues, this.wValues);
    this.parameterMetadata = IntStream.range(0, getParameterCount())
        .mapToObj(i -> sortedMetadata.getParameterMetadata(i))
        .collect(toImmutableList());
  }

  // basic validation
  private void validateInputs(
      CubeMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues,
      CubeInterpolator interpolator) {

    ArgChecker.notNull(metadata, "metadata");
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.notNull(zValues, "zValues");
    ArgChecker.notNull(wValues, "wValues");
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
    if (xValues.size() != wValues.size()) {
      throw new IllegalArgumentException("Length of x-values and w-values must match");
    }
    metadata.getParameterMetadata().ifPresent(params -> {
      if (xValues.size() != params.size()) {
        throw new IllegalArgumentException("Length of x-values and parameter metadata must match when metadata present");
      }
    });
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new InterpolatedNodalCube(metadata, xValues, yValues, zValues, wValues, interpolator);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return wValues.size();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return wValues.get(parameterIndex);
  }

  @Override
  public InterpolatedNodalCube withParameter(int parameterIndex, double newValue) {
    return withWValues(wValues.with(parameterIndex, newValue));
  }

  @Override
  public InterpolatedNodalCube withPerturbation(ParameterPerturbation perturbation) {
    int size = wValues.size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, wValues.get(i), getParameterMetadata(i)));
    return withWValues(perturbedValues);
  }

  //-------------------------------------------------------------------------
  @Override
  public double wValue(double x, double y, double z) {
    return boundInterpolator.interpolate(x, y, z);
  }

  @Override
  public UnitParameterSensitivity wValueParameterSensitivity(double x, double y, double z) {
    DoubleArray sensitivityValues = boundInterpolator.parameterSensitivity(x, y, z);
    return createParameterSensitivity(sensitivityValues);
  }

  //-------------------------------------------------------------------------
  @Override
  public ValueDerivatives firstPartialDerivatives(double x, double y, double z) {
    return boundInterpolator.firstPartialDerivatives(x, y, z);
  }

  //-------------------------------------------------------------------------
  @Override
  public InterpolatedNodalCube withMetadata(CubeMetadata metadata) {
    return new InterpolatedNodalCube(metadata, xValues, yValues, zValues, wValues, interpolator);
  }

  @Override
  public InterpolatedNodalCube withWValues(DoubleArray wValues) {
    return new InterpolatedNodalCube(metadata, xValues, yValues, zValues, wValues, interpolator);
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
   * The meta-bean for {@code InterpolatedNodalCube}.
   * @return the meta-bean, not null
   */
  public static InterpolatedNodalCube.Meta meta() {
    return InterpolatedNodalCube.Meta.INSTANCE;
  }

  static {
    MetaBean.register(InterpolatedNodalCube.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InterpolatedNodalCube.Builder builder() {
    return new InterpolatedNodalCube.Builder();
  }

  @Override
  public InterpolatedNodalCube.Meta metaBean() {
    return InterpolatedNodalCube.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cube metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this cube.
   * @return the value of the property, not null
   */
  @Override
  public CubeMetadata getMetadata() {
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
   * Gets the array of w-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   * @return the value of the property, not null
   */
  @Override
  public DoubleArray getWValues() {
    return wValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying interpolator.
   * @return the value of the property, not null
   */
  public CubeInterpolator getInterpolator() {
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
      InterpolatedNodalCube other = (InterpolatedNodalCube) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(xValues, other.xValues) &&
          JodaBeanUtils.equal(yValues, other.yValues) &&
          JodaBeanUtils.equal(zValues, other.zValues) &&
          JodaBeanUtils.equal(wValues, other.wValues) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(wValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("InterpolatedNodalCube{");
    buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
    buf.append("xValues").append('=').append(JodaBeanUtils.toString(xValues)).append(',').append(' ');
    buf.append("yValues").append('=').append(JodaBeanUtils.toString(yValues)).append(',').append(' ');
    buf.append("zValues").append('=').append(JodaBeanUtils.toString(zValues)).append(',').append(' ');
    buf.append("wValues").append('=').append(JodaBeanUtils.toString(wValues)).append(',').append(' ');
    buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InterpolatedNodalCube}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<CubeMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", InterpolatedNodalCube.class, CubeMetadata.class);
    /**
     * The meta-property for the {@code xValues} property.
     */
    private final MetaProperty<DoubleArray> xValues = DirectMetaProperty.ofImmutable(
        this, "xValues", InterpolatedNodalCube.class, DoubleArray.class);
    /**
     * The meta-property for the {@code yValues} property.
     */
    private final MetaProperty<DoubleArray> yValues = DirectMetaProperty.ofImmutable(
        this, "yValues", InterpolatedNodalCube.class, DoubleArray.class);
    /**
     * The meta-property for the {@code zValues} property.
     */
    private final MetaProperty<DoubleArray> zValues = DirectMetaProperty.ofImmutable(
        this, "zValues", InterpolatedNodalCube.class, DoubleArray.class);
    /**
     * The meta-property for the {@code wValues} property.
     */
    private final MetaProperty<DoubleArray> wValues = DirectMetaProperty.ofImmutable(
        this, "wValues", InterpolatedNodalCube.class, DoubleArray.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<CubeInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", InterpolatedNodalCube.class, CubeInterpolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "xValues",
        "yValues",
        "zValues",
        "wValues",
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
        case 793777273:  // wValues
          return wValues;
        case 2096253127:  // interpolator
          return interpolator;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InterpolatedNodalCube.Builder builder() {
      return new InterpolatedNodalCube.Builder();
    }

    @Override
    public Class<? extends InterpolatedNodalCube> beanType() {
      return InterpolatedNodalCube.class;
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
    public MetaProperty<CubeMetadata> metadata() {
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
     * The meta-property for the {@code wValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> wValues() {
      return wValues;
    }

    /**
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CubeInterpolator> interpolator() {
      return interpolator;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((InterpolatedNodalCube) bean).getMetadata();
        case 1681280954:  // xValues
          return ((InterpolatedNodalCube) bean).getXValues();
        case -1726182661:  // yValues
          return ((InterpolatedNodalCube) bean).getYValues();
        case -838678980:  // zValues
          return ((InterpolatedNodalCube) bean).getZValues();
        case 793777273:  // wValues
          return ((InterpolatedNodalCube) bean).getWValues();
        case 2096253127:  // interpolator
          return ((InterpolatedNodalCube) bean).getInterpolator();
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
   * The bean-builder for {@code InterpolatedNodalCube}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InterpolatedNodalCube> {

    private CubeMetadata metadata;
    private DoubleArray xValues;
    private DoubleArray yValues;
    private DoubleArray zValues;
    private DoubleArray wValues;
    private CubeInterpolator interpolator;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InterpolatedNodalCube beanToCopy) {
      this.metadata = beanToCopy.getMetadata();
      this.xValues = beanToCopy.getXValues();
      this.yValues = beanToCopy.getYValues();
      this.zValues = beanToCopy.getZValues();
      this.wValues = beanToCopy.getWValues();
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
        case 793777273:  // wValues
          return wValues;
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
          this.metadata = (CubeMetadata) newValue;
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
        case 793777273:  // wValues
          this.wValues = (DoubleArray) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (CubeInterpolator) newValue;
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
    public InterpolatedNodalCube build() {
      return new InterpolatedNodalCube(
          metadata,
          xValues,
          yValues,
          zValues,
          wValues,
          interpolator);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the cube metadata.
     * <p>
     * The metadata includes an optional list of parameter metadata.
     * If present, the size of the parameter metadata list will match the number of parameters of this cube.
     * @param metadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder metadata(CubeMetadata metadata) {
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
     * Sets the array of w-values, one for each point.
     * <p>
     * This array will contains at least two elements and be of the same length as x-values.
     * @param wValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder wValues(DoubleArray wValues) {
      JodaBeanUtils.notNull(wValues, "wValues");
      this.wValues = wValues;
      return this;
    }

    /**
     * Sets the underlying interpolator.
     * @param interpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolator(CubeInterpolator interpolator) {
      JodaBeanUtils.notNull(interpolator, "interpolator");
      this.interpolator = interpolator;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("InterpolatedNodalCube.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("xValues").append('=').append(JodaBeanUtils.toString(xValues)).append(',').append(' ');
      buf.append("yValues").append('=').append(JodaBeanUtils.toString(yValues)).append(',').append(' ');
      buf.append("zValues").append('=').append(JodaBeanUtils.toString(zValues)).append(',').append(' ');
      buf.append("wValues").append('=').append(JodaBeanUtils.toString(wValues)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
