/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A hybrid curve which combines two underlying nodal curves,
 * allowing different interpolators to be used for different parts of the curve.
 * <P>
 * The left curve is used for all points up to and including a certain x-value, the right curve is used for higher x-values.
 */
@BeanDefinition(builderScope = "private")
public final class HybridNodalCurve
    implements NodalCurve, ImmutableBean, Serializable {

  /**
   * The left nodal curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve leftCurve;
  /**
   * The right nodal curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve rightCurve;
  /**
   * The x axis index where the two curves are joined.
   */
  private final transient int spliceIndex;
  /**
   * The x axis splice; the x value where the two curves are joined.
   */
  private final transient double xSplice;
  /**
   * The metadata for the combined curve.
   */
  private final transient CurveMetadata combinedMetadata;

  //-------------------------------------------------------------------------
  /**
   * Create a new hybrid nodal curve.
   *
   * @param metadata  the common metadata, containing parameter metadata for all of the values
   * @param xValues  the full set of x values 
   * @param yValues  the full set of y values
   * @param spliceIndex  the x index at which the curves should be split
   * @param interpolatorLeft  the interpolator for the left curve
   * @param interpolatorRight  the interpolator for the right curve
   * @param extrapolatorLeft  the extrapolator for x-values less than the smallest defined x-value
   * @param extrapolatorRight  the extrapolator for x-values greater than the largest defined x-value
   * @return the hybrid nodal curve
   */
  public static HybridNodalCurve of(
      CurveMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      int spliceIndex,
      CurveInterpolator interpolatorLeft,
      CurveInterpolator interpolatorRight,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {

    if (spliceIndex > xValues.size() - 1 || spliceIndex < 0) {
      throw new IllegalArgumentException(Messages.format(
          "Hybrid curve splice index [{}] must be less than number of parameters [{}] and non-negative",
          spliceIndex,
          xValues.size()));
    }

    // splice value present in both curves to keep correct interpolation
    DoubleArray xLeft = xValues.subArray(0, spliceIndex + 1);
    DoubleArray yLeft = yValues.subArray(0, spliceIndex + 1);

    DoubleArray xRight = xValues.subArray(spliceIndex, xValues.size());
    DoubleArray yRight = yValues.subArray(spliceIndex, yValues.size());

    Pair<CurveMetadata, CurveMetadata> splicedMetaData = splicedMetaData(metadata, spliceIndex);

    InterpolatedNodalCurve leftCurve = InterpolatedNodalCurve.builder()
        .metadata(splicedMetaData.getFirst())
        .xValues(xLeft)
        .yValues(yLeft)
        .interpolator(interpolatorLeft)
        .extrapolatorLeft(extrapolatorLeft)
        .extrapolatorRight(extrapolatorRight)
        .build();

    InterpolatedNodalCurve rightCurve = InterpolatedNodalCurve.builder()
        .metadata(splicedMetaData.getSecond())
        .xValues(xRight)
        .yValues(yRight)
        .interpolator(interpolatorRight)
        .extrapolatorLeft(extrapolatorLeft)
        .extrapolatorRight(extrapolatorRight)
        .build();

    return new HybridNodalCurve(leftCurve, rightCurve);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private HybridNodalCurve(NodalCurve leftCurve, NodalCurve rightCurve) {
    this.leftCurve = ArgChecker.notNull(leftCurve, "leftCurve");
    this.rightCurve = ArgChecker.notNull(rightCurve, "rightCurve");
    this.xSplice = rightCurve.getXValues().get(0);
    this.spliceIndex = leftCurve.getParameterCount() - 1;

    ImmutableList.Builder<ParameterMetadata> combinedMetadataBuilder = ImmutableList.builder();
    for (int i = 0; i < leftCurve.getParameterCount(); i++) {
      combinedMetadataBuilder.add(leftCurve.getParameterMetadata(i));
    }

    // start from 1 as splice index present in both curves
    for (int i = 1; i < rightCurve.getParameterCount(); i++) {
      combinedMetadataBuilder.add(rightCurve.getParameterMetadata(i));
    }

    this.combinedMetadata = leftCurve.getMetadata().withParameterMetadata(combinedMetadataBuilder.build());
  }

  private static Pair<CurveMetadata, CurveMetadata> splicedMetaData(CurveMetadata curveMetadata, int spliceIndex) {
    CurveMetadata leftMetadata;
    CurveMetadata rightMetadata;
    if (curveMetadata.getParameterMetadata().isPresent()) {
      List<ParameterMetadata> parameterMetadata = curveMetadata.getParameterMetadata().get();
      leftMetadata = curveMetadata.withParameterMetadata(parameterMetadata.subList(0, spliceIndex + 1));
      rightMetadata = curveMetadata.withParameterMetadata(
          parameterMetadata.subList(spliceIndex, parameterMetadata.size()));
    } else {
      leftMetadata = curveMetadata;
      rightMetadata = curveMetadata;
    }
    return Pair.of(leftMetadata, rightMetadata);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return getYValues().size();
  }

  @Override
  public DoubleArray getXValues() {
    // splice value present in both curves, ignore from left
    return leftCurve.getXValues().subArray(0, spliceIndex)
        .concat(rightCurve.getXValues());
  }

  @Override
  public DoubleArray getYValues() {
    // splice value present in both curves, ignore from left
    return leftCurve.getYValues().subArray(0, spliceIndex)
        .concat(rightCurve.getYValues());
  }

  @Override
  public CurveMetadata getMetadata() {
    return combinedMetadata;
  }

  @Override
  public double getParameter(int parameterIndex) {
    if (parameterIndex < 0 || parameterIndex > getYValues().size()) {
      throw new IllegalArgumentException(Messages.format(
          "Parameter index [] out of bounds of yValues array of size []",
          parameterIndex,
          getYValues().size()));
    } else {
      return getYValues().get(parameterIndex);
    }
  }

  @Override
  public double yValue(double x) {
    return x < xSplice ? leftCurve.yValue(x) : rightCurve.yValue(x);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the sensitivity of the y-value with respect to the curve parameters
   * <p>
   * The result will be UnitParameterSensitivity, which stores the
   * sensitivity in a DoubleArray of size getParameterCount()
   * <p>
   * If x<= xSplice, then sensitive to all nodes from 0 to spliceIndex,
   * with zero sensitivity to all nodes from spliceIndex+1 to getParameterCount()-1
   * <p>
   * If x > xSplice, then sensitive to all nodes from spliceIndex to getParameterCount()-1,
   * with zero sensitivity to all nodes from 0 to spliceIndex-1
   * <p>
   *
   * @param x  the new x-value
   * @return the unit parameter sensitivity
   */
  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    if (x <= this.xSplice) {
      UnitParameterSensitivity leftSensi = leftCurve.yValueParameterSensitivity(x);
      DoubleArray rightArray = DoubleArray.filled(rightCurve.getXValues().size() - 1);
      return UnitParameterSensitivity.of(getName(), leftSensi.getSensitivity().concat(rightArray));
    } else {
      DoubleArray leftArray = DoubleArray.filled(leftCurve.getXValues().size() - 1);
      UnitParameterSensitivity rightSensi = rightCurve.yValueParameterSensitivity(x);
      return UnitParameterSensitivity.of(getName(), leftArray.concat(rightSensi.getSensitivity()));
    }
  }

  @Override
  public double firstDerivative(double x) {
    if (x < xSplice) {
      return leftCurve.firstDerivative(x);
    } else if (x > xSplice) {
      return rightCurve.firstDerivative(x);
    } else {
      // at the splice location take the average of the first derivatives
      return 0.5 * (leftCurve.firstDerivative(x) + rightCurve.firstDerivative(x));
    }
  }

  @Override
  public HybridNodalCurve withNode(double x, double y, ParameterMetadata paramMetadata) {
    throw new IllegalArgumentException(Messages.format(
        "{} does not support withNode()",
        this.getClass().getSimpleName()));
  }

  @Override
  public HybridNodalCurve withYValues(DoubleArray yValues) {
    if (yValues.size() != getYValues().size()) {
      throw new IllegalArgumentException(Messages.format(
          "Size of new y values [] does not match current size []",
          yValues.size(),
          getYValues().size()));
    }
    NodalCurve updatedLeftCurve = leftCurve.withYValues(yValues.subArray(0, spliceIndex + 1));
    NodalCurve updatedRightCurve = rightCurve.withYValues(yValues.subArray(spliceIndex, yValues.size()));
    return new HybridNodalCurve(updatedLeftCurve, updatedRightCurve);
  }

  @Override
  public HybridNodalCurve withMetadata(CurveMetadata metadata) {
    Pair<CurveMetadata, CurveMetadata> splicedMetaData = splicedMetaData(metadata, this.spliceIndex);
    NodalCurve updatedLeftCurve = leftCurve.withMetadata(splicedMetaData.getFirst());
    NodalCurve updatedRightCurve = rightCurve.withMetadata(splicedMetaData.getSecond());
    return new HybridNodalCurve(updatedLeftCurve, updatedRightCurve);
  }

  @Override
  public HybridNodalCurve withParameter(int parameterIndex, double newValue) {
    NodalCurve updatedLeftCurve = parameterIndex <= spliceIndex ?
        leftCurve.withParameter(parameterIndex, newValue) :
        leftCurve;
    NodalCurve updatedRightCurve = parameterIndex >= spliceIndex ?
        rightCurve.withParameter(parameterIndex - spliceIndex, newValue) :
        rightCurve;
    return new HybridNodalCurve(updatedLeftCurve, updatedRightCurve);
  }

  @Override
  public UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    return UnitParameterSensitivity.of(
        getName(),
        combinedMetadata.getParameterMetadata().orElse(ImmutableList.of()),
        sensitivities);
  }

  @Override
  public CurrencyParameterSensitivity createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivity.of(
        getName(),
        combinedMetadata.getParameterMetadata().orElse(ImmutableList.of()),
        currency,
        sensitivities);
  }

  @Override
  public HybridNodalCurve withPerturbation(ParameterPerturbation perturbation) {
    int size = getYValues().size();
    DoubleArray perturbedValues = DoubleArray.of(
        size, i -> perturbation.perturbParameter(i, getYValues().get(i), getParameterMetadata(i)));
    return withYValues(perturbedValues);
  }

  @Override
  public HybridNodalCurve withValues(DoubleArray xValues, DoubleArray yValues) {
    if (xValues.size() == getXValues().size()) {
      DoubleArray xValuesLeft = xValues.subArray(0, this.spliceIndex + 1);
      DoubleArray yValuesLeft = yValues.subArray(0, this.spliceIndex + 1);

      DoubleArray xValuesRight = xValues.subArray(this.spliceIndex, getXValues().size());
      DoubleArray yValuesRight = yValues.subArray(this.spliceIndex, getXValues().size());

      NodalCurve updatedLeftCurve = this.leftCurve.withValues(xValuesLeft, yValuesLeft);
      NodalCurve updatedRightCurve = this.rightCurve.withValues(xValuesRight, yValuesRight);
      return new HybridNodalCurve(updatedLeftCurve, updatedRightCurve);
    } else {
      throw new IllegalArgumentException(Messages.format(
          "{} does not support withValues() when the size of new x values [] does not match current size []; " +
              "in this case a splice index must also be provided",
          this.getClass().getSimpleName(),
          xValues.size(),
          getXValues().size()));
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code HybridNodalCurve}.
   * @return the meta-bean, not null
   */
  public static HybridNodalCurve.Meta meta() {
    return HybridNodalCurve.Meta.INSTANCE;
  }

  static {
    MetaBean.register(HybridNodalCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public HybridNodalCurve.Meta metaBean() {
    return HybridNodalCurve.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left nodal curve.
   * @return the value of the property, not null
   */
  public NodalCurve getLeftCurve() {
    return leftCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right nodal curve.
   * @return the value of the property, not null
   */
  public NodalCurve getRightCurve() {
    return rightCurve;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      HybridNodalCurve other = (HybridNodalCurve) obj;
      return JodaBeanUtils.equal(leftCurve, other.leftCurve) &&
          JodaBeanUtils.equal(rightCurve, other.rightCurve);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(leftCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(rightCurve);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("HybridNodalCurve{");
    buf.append("leftCurve").append('=').append(JodaBeanUtils.toString(leftCurve)).append(',').append(' ');
    buf.append("rightCurve").append('=').append(JodaBeanUtils.toString(rightCurve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code HybridNodalCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code leftCurve} property.
     */
    private final MetaProperty<NodalCurve> leftCurve = DirectMetaProperty.ofImmutable(
        this, "leftCurve", HybridNodalCurve.class, NodalCurve.class);
    /**
     * The meta-property for the {@code rightCurve} property.
     */
    private final MetaProperty<NodalCurve> rightCurve = DirectMetaProperty.ofImmutable(
        this, "rightCurve", HybridNodalCurve.class, NodalCurve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "leftCurve",
        "rightCurve");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1716149544:  // leftCurve
          return leftCurve;
        case -1413464013:  // rightCurve
          return rightCurve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends HybridNodalCurve> builder() {
      return new HybridNodalCurve.Builder();
    }

    @Override
    public Class<? extends HybridNodalCurve> beanType() {
      return HybridNodalCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code leftCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalCurve> leftCurve() {
      return leftCurve;
    }

    /**
     * The meta-property for the {@code rightCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalCurve> rightCurve() {
      return rightCurve;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1716149544:  // leftCurve
          return ((HybridNodalCurve) bean).getLeftCurve();
        case -1413464013:  // rightCurve
          return ((HybridNodalCurve) bean).getRightCurve();
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
   * The bean-builder for {@code HybridNodalCurve}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<HybridNodalCurve> {

    private NodalCurve leftCurve;
    private NodalCurve rightCurve;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1716149544:  // leftCurve
          return leftCurve;
        case -1413464013:  // rightCurve
          return rightCurve;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1716149544:  // leftCurve
          this.leftCurve = (NodalCurve) newValue;
          break;
        case -1413464013:  // rightCurve
          this.rightCurve = (NodalCurve) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public HybridNodalCurve build() {
      return new HybridNodalCurve(
          leftCurve,
          rightCurve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("HybridNodalCurve.Builder{");
      buf.append("leftCurve").append('=').append(JodaBeanUtils.toString(leftCurve)).append(',').append(' ');
      buf.append("rightCurve").append('=').append(JodaBeanUtils.toString(rightCurve));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
