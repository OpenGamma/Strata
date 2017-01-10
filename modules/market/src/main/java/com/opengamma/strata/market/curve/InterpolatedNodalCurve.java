/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A curve based on interpolation between a number of nodal points.
 * <p>
 * This class defines a curve in terms of a fixed number of nodes, referred to as <i>parameters</i>.
 * <p>
 * Each node has an x-value and a y-value.
 * The interface is focused on finding the y-value for a given x-value.
 * An interpolator is used to find y-values for x-values between two nodes.
 * Two extrapolators are used to find y-values, one when the x-value is to the left
 * of the first node, and one where the x-value is to the right of the last node.
 */
@BeanDefinition
public final class InterpolatedNodalCurve
    implements NodalCurve, ImmutableBean, Serializable {

  /**
   * The curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveMetadata metadata;
  /**
   * The array of x-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as y-values.
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
   * The interpolator.
   * This is used for x-values between the smallest and largest known x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator interpolator;
  /**
   * The extrapolator for x-values on the left, defaulted to 'Flat".
   * This is used for x-values smaller than the smallest known x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorLeft;
  /**
   * The extrapolator for x-values on the right, defaulted to 'Flat".
   * This is used for x-values larger than the largest known x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorRight;
  /**
   * The bound interpolator.
   */
  private final transient BoundCurveInterpolator boundInterpolator;  // derived and cached, not a property
  /**
   * The parameter metadata.
   */
  private final transient List<ParameterMetadata> parameterMetadata;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates an interpolated curve with metadata.
   * 
   * @param metadata  the curve metadata
   * @param xValues  the x-values
   * @param yValues  the y-values
   * @param interpolator  the interpolator
   * @return the curve
   */
  public static InterpolatedNodalCurve of(
      CurveMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      CurveInterpolator interpolator) {

    return InterpolatedNodalCurve.builder()
        .metadata(metadata)
        .xValues(xValues)
        .yValues(yValues)
        .interpolator(interpolator)
        .build();
  }

  /**
   * Creates an interpolated curve with metadata.
   *
   * @param metadata  the curve metadata
   * @param xValues  the x-values
   * @param yValues  the y-values
   * @param interpolator  the interpolator
   * @param extrapolatorLeft  the extrapolator for extrapolating off the left-hand end of the curve
   * @param extrapolatorRight  the extrapolator for extrapolating off the right-hand end of the curve
   * @return the curve
   */
  public static InterpolatedNodalCurve of(
      CurveMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {

    return InterpolatedNodalCurve.builder()
        .metadata(metadata)
        .xValues(xValues)
        .yValues(yValues)
        .interpolator(interpolator)
        .extrapolatorLeft(extrapolatorLeft)
        .extrapolatorRight(extrapolatorRight)
        .build();
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private InterpolatedNodalCurve(
      CurveMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {

    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(xValues, "times");
    JodaBeanUtils.notNull(yValues, "values");
    JodaBeanUtils.notNull(interpolator, "interpolator");
    JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
    JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
    if (xValues.size() < 2) {
      throw new IllegalArgumentException("Length of x-values must be at least 2");
    }
    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException("Length of x-values and y-values must match");
    }
    metadata.getParameterMetadata().ifPresent(params -> {
      if (xValues.size() != params.size()) {
        throw new IllegalArgumentException("Length of x-values and parameter metadata must match when metadata present");
      }
    });
    for (int i = 1; i < xValues.size(); i++) {
      if (xValues.get(i) <= xValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of x-values must be sorted and unique");
      }
    }
    this.metadata = metadata;
    this.xValues = xValues;
    this.yValues = yValues;
    this.extrapolatorLeft = extrapolatorLeft;
    this.interpolator = interpolator;
    this.extrapolatorRight = extrapolatorRight;
    this.boundInterpolator = interpolator.bind(xValues, yValues, extrapolatorLeft, extrapolatorRight);
    this.parameterMetadata = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.extrapolatorLeft = CurveExtrapolators.FLAT;
    builder.extrapolatorRight = CurveExtrapolators.FLAT;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new InterpolatedNodalCurve(metadata, xValues, yValues, interpolator, extrapolatorLeft, extrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return yValues.size();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return yValues.get(parameterIndex);
  }

  @Override
  public InterpolatedNodalCurve withParameter(int parameterIndex, double newValue) {
    return withYValues(yValues.with(parameterIndex, newValue));
  }

  @Override
  public InterpolatedNodalCurve withPerturbation(ParameterPerturbation perturbation) {
    int size = yValues.size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, yValues.get(i), getParameterMetadata(i)));
    return withYValues(perturbedValues);
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    return boundInterpolator.interpolate(x);
  }

  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    return createParameterSensitivity(boundInterpolator.parameterSensitivity(x));
  }

  @Override
  public double firstDerivative(double x) {
    return boundInterpolator.firstDerivative(x);
  }

  //-------------------------------------------------------------------------
  @Override
  public InterpolatedNodalCurve withMetadata(CurveMetadata metadata) {
    return new InterpolatedNodalCurve(metadata, xValues, yValues, interpolator, extrapolatorLeft, extrapolatorRight);
  }

  @Override
  public InterpolatedNodalCurve withYValues(DoubleArray yValues) {
    return new InterpolatedNodalCurve(metadata, xValues, yValues, interpolator, extrapolatorLeft, extrapolatorRight);
  }

  @Override
  public InterpolatedNodalCurve withValues(DoubleArray xValues, DoubleArray yValues) {
    return new InterpolatedNodalCurve(metadata, xValues, yValues, interpolator, extrapolatorLeft, extrapolatorRight);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new curve with an additional node, specifying the parameter metadata.
   * <p>
   * The result will contain the specified node.
   * If the x-value equals an existing x-value, the y-value will be changed.
   * If the x-value does not equal an existing x-value, the node will be added.
   * <p>
   * The result will only contain the specified parameter metadata if this curve also has parameter meta-data.
   * 
   * @param x  the new x-value
   * @param y  the new y-value
   * @param paramMetadata  the new parameter metadata
   * @return the updated curve
   */
  @Override
  public InterpolatedNodalCurve withNode(double x, double y, ParameterMetadata paramMetadata) {
    int index = Arrays.binarySearch(xValues.toArrayUnsafe(), x);
    if (index >= 0) {
      CurveMetadata md = metadata.getParameterMetadata()
          .map(params -> {
            List<ParameterMetadata> extended = new ArrayList<>(params);
            extended.set(index, paramMetadata);
            return metadata.withParameterMetadata(extended);
          })
          .orElse(metadata);
      DoubleArray yUpdated = yValues.with(index, y);
      return new InterpolatedNodalCurve(md, xValues, yUpdated, interpolator, extrapolatorLeft, extrapolatorRight);
    }
    int insertion = -(index + 1);
    DoubleArray xExtended = xValues.subArray(0, insertion).concat(x).concat(xValues.subArray(insertion));
    DoubleArray yExtended = yValues.subArray(0, insertion).concat(y).concat(yValues.subArray(insertion));
    // add to existing metadata, or do nothing if no existing metadata
    CurveMetadata md = metadata.getParameterMetadata()
        .map(params -> {
          List<ParameterMetadata> extended = new ArrayList<>(params);
          extended.add(insertion, paramMetadata);
          return metadata.withParameterMetadata(extended);
        })
        .orElse(metadata);
    return new InterpolatedNodalCurve(md, xExtended, yExtended, interpolator, extrapolatorLeft, extrapolatorRight);
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
   * The meta-bean for {@code InterpolatedNodalCurve}.
   * @return the meta-bean, not null
   */
  public static InterpolatedNodalCurve.Meta meta() {
    return InterpolatedNodalCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InterpolatedNodalCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InterpolatedNodalCurve.Builder builder() {
    return new InterpolatedNodalCurve.Builder();
  }

  @Override
  public InterpolatedNodalCurve.Meta metaBean() {
    return InterpolatedNodalCurve.Meta.INSTANCE;
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
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   * @return the value of the property, not null
   */
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of x-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as y-values.
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
   * Gets the interpolator.
   * This is used for x-values between the smallest and largest known x-value.
   * @return the value of the property, not null
   */
  public CurveInterpolator getInterpolator() {
    return interpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator for x-values on the left, defaulted to 'Flat".
   * This is used for x-values smaller than the smallest known x-value.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorLeft() {
    return extrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator for x-values on the right, defaulted to 'Flat".
   * This is used for x-values larger than the largest known x-value.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorRight() {
    return extrapolatorRight;
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
      InterpolatedNodalCurve other = (InterpolatedNodalCurve) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(xValues, other.xValues) &&
          JodaBeanUtils.equal(yValues, other.yValues) &&
          JodaBeanUtils.equal(interpolator, other.interpolator) &&
          JodaBeanUtils.equal(extrapolatorLeft, other.extrapolatorLeft) &&
          JodaBeanUtils.equal(extrapolatorRight, other.extrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("InterpolatedNodalCurve{");
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("xValues").append('=').append(xValues).append(',').append(' ');
    buf.append("yValues").append('=').append(yValues).append(',').append(' ');
    buf.append("interpolator").append('=').append(interpolator).append(',').append(' ');
    buf.append("extrapolatorLeft").append('=').append(extrapolatorLeft).append(',').append(' ');
    buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InterpolatedNodalCurve}.
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
        this, "metadata", InterpolatedNodalCurve.class, CurveMetadata.class);
    /**
     * The meta-property for the {@code xValues} property.
     */
    private final MetaProperty<DoubleArray> xValues = DirectMetaProperty.ofImmutable(
        this, "xValues", InterpolatedNodalCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code yValues} property.
     */
    private final MetaProperty<DoubleArray> yValues = DirectMetaProperty.ofImmutable(
        this, "yValues", InterpolatedNodalCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<CurveInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", InterpolatedNodalCurve.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "extrapolatorLeft", InterpolatedNodalCurve.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code extrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "extrapolatorRight", InterpolatedNodalCurve.class, CurveExtrapolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "xValues",
        "yValues",
        "interpolator",
        "extrapolatorLeft",
        "extrapolatorRight");

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
        case 2096253127:  // interpolator
          return interpolator;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InterpolatedNodalCurve.Builder builder() {
      return new InterpolatedNodalCurve.Builder();
    }

    @Override
    public Class<? extends InterpolatedNodalCurve> beanType() {
      return InterpolatedNodalCurve.class;
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
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> interpolator() {
      return interpolator;
    }

    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> extrapolatorLeft() {
      return extrapolatorLeft;
    }

    /**
     * The meta-property for the {@code extrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> extrapolatorRight() {
      return extrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((InterpolatedNodalCurve) bean).getMetadata();
        case 1681280954:  // xValues
          return ((InterpolatedNodalCurve) bean).getXValues();
        case -1726182661:  // yValues
          return ((InterpolatedNodalCurve) bean).getYValues();
        case 2096253127:  // interpolator
          return ((InterpolatedNodalCurve) bean).getInterpolator();
        case 1271703994:  // extrapolatorLeft
          return ((InterpolatedNodalCurve) bean).getExtrapolatorLeft();
        case 773779145:  // extrapolatorRight
          return ((InterpolatedNodalCurve) bean).getExtrapolatorRight();
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
   * The bean-builder for {@code InterpolatedNodalCurve}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InterpolatedNodalCurve> {

    private CurveMetadata metadata;
    private DoubleArray xValues;
    private DoubleArray yValues;
    private CurveInterpolator interpolator;
    private CurveExtrapolator extrapolatorLeft;
    private CurveExtrapolator extrapolatorRight;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InterpolatedNodalCurve beanToCopy) {
      this.metadata = beanToCopy.getMetadata();
      this.xValues = beanToCopy.getXValues();
      this.yValues = beanToCopy.getYValues();
      this.interpolator = beanToCopy.getInterpolator();
      this.extrapolatorLeft = beanToCopy.getExtrapolatorLeft();
      this.extrapolatorRight = beanToCopy.getExtrapolatorRight();
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
        case 2096253127:  // interpolator
          return interpolator;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
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
        case 1681280954:  // xValues
          this.xValues = (DoubleArray) newValue;
          break;
        case -1726182661:  // yValues
          this.yValues = (DoubleArray) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (CurveInterpolator) newValue;
          break;
        case 1271703994:  // extrapolatorLeft
          this.extrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case 773779145:  // extrapolatorRight
          this.extrapolatorRight = (CurveExtrapolator) newValue;
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
    public InterpolatedNodalCurve build() {
      return new InterpolatedNodalCurve(
          metadata,
          xValues,
          yValues,
          interpolator,
          extrapolatorLeft,
          extrapolatorRight);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the curve metadata.
     * <p>
     * The metadata includes an optional list of parameter metadata.
     * If present, the size of the parameter metadata list will match the number of parameters of this curve.
     * @param metadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder metadata(CurveMetadata metadata) {
      JodaBeanUtils.notNull(metadata, "metadata");
      this.metadata = metadata;
      return this;
    }

    /**
     * Sets the array of x-values, one for each point.
     * <p>
     * This array will contains at least two elements and be of the same length as y-values.
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
     * Sets the interpolator.
     * This is used for x-values between the smallest and largest known x-value.
     * @param interpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolator(CurveInterpolator interpolator) {
      JodaBeanUtils.notNull(interpolator, "interpolator");
      this.interpolator = interpolator;
      return this;
    }

    /**
     * Sets the extrapolator for x-values on the left, defaulted to 'Flat".
     * This is used for x-values smaller than the smallest known x-value.
     * @param extrapolatorLeft  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder extrapolatorLeft(CurveExtrapolator extrapolatorLeft) {
      JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
      this.extrapolatorLeft = extrapolatorLeft;
      return this;
    }

    /**
     * Sets the extrapolator for x-values on the right, defaulted to 'Flat".
     * This is used for x-values larger than the largest known x-value.
     * @param extrapolatorRight  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder extrapolatorRight(CurveExtrapolator extrapolatorRight) {
      JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
      this.extrapolatorRight = extrapolatorRight;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("InterpolatedNodalCurve.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("xValues").append('=').append(JodaBeanUtils.toString(xValues)).append(',').append(' ');
      buf.append("yValues").append('=').append(JodaBeanUtils.toString(yValues)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator)).append(',').append(' ');
      buf.append("extrapolatorLeft").append('=').append(JodaBeanUtils.toString(extrapolatorLeft)).append(',').append(' ');
      buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
