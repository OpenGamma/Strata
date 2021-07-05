/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.interpolator;

import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.interpolator.BoundCurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;

/**
 * A surface interpolator that is based on two curve interpolators.
 * <p>
 * The surface parameters are divided into rows and columns based on the x-values
 * and y-values. There must be at least two y-values for each x-value.
 * In most cases, the parameters will form a rectangular grid.
 * <p>
 * The interpolation operates in two stages.
 * First, the parameters are grouped into sets, each with the same x value.
 * Second, the y curve interpolator is used on each set of y values.
 * Finally, the x curve interpolator is used on the results of the y interpolation.
 * <p>
 * There should be at least two different y-values for each x-value.
 * If there is only one, then the associated z-value will always be returned.
 */
@BeanDefinition(builderScope = "private")
public final class GridSurfaceInterpolator
    implements SurfaceInterpolator, ImmutableBean, Serializable {

  /**
   * The x-value interpolator.
   */
  @PropertyDefinition
  private final CurveInterpolator xInterpolator;
  /**
   * The x-value left extrapolator.
   */
  @PropertyDefinition
  private final CurveExtrapolator xExtrapolatorLeft;
  /**
   * The x-value right extrapolator.
   */
  @PropertyDefinition
  private final CurveExtrapolator xExtrapolatorRight;
  /**
   * The y-value interpolator.
   */
  @PropertyDefinition
  private final CurveInterpolator yInterpolator;
  /**
   * The y-value left extrapolator.
   */
  @PropertyDefinition
  private final CurveExtrapolator yExtrapolatorLeft;
  /**
   * The y-value right extrapolator.
   */
  @PropertyDefinition
  private final CurveExtrapolator yExtrapolatorRight;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified interpolators, using flat extrapolation.
   * 
   * @param xInterpolator  the x-value interpolator
   * @param yInterpolator  the y-value interpolator
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static GridSurfaceInterpolator of(CurveInterpolator xInterpolator, CurveInterpolator yInterpolator) {
    return new GridSurfaceInterpolator(xInterpolator, FLAT, FLAT, yInterpolator, FLAT, FLAT);
  }

  /**
   * Obtains an instance from the specified interpolators and extrapolators.
   * 
   * @param xInterpolator  the x-value interpolator
   * @param xExtrapolator  the x-value extrapolator
   * @param yInterpolator  the y-value interpolator
   * @param yExtrapolator  the y-value extrapolator
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static GridSurfaceInterpolator of(
      CurveInterpolator xInterpolator,
      CurveExtrapolator xExtrapolator,
      CurveInterpolator yInterpolator,
      CurveExtrapolator yExtrapolator) {

    return new GridSurfaceInterpolator(
        xInterpolator, xExtrapolator, xExtrapolator, yInterpolator, yExtrapolator, yExtrapolator);
  }

  /**
   * Obtains an instance from the specified interpolators and extrapolators.
   * 
   * @param xInterpolator  the x-value interpolator
   * @param xExtrapolatorLeft  the x-value left extrapolator
   * @param xExtrapolatorRight  the x-value right extrapolator
   * @param yInterpolator  the y-value interpolator
   * @param yExtrapolatorLeft  the y-value left extrapolator
   * @param yExtrapolatorRight  the y-value right extrapolator
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static GridSurfaceInterpolator of(
      CurveInterpolator xInterpolator,
      CurveExtrapolator xExtrapolatorLeft,
      CurveExtrapolator xExtrapolatorRight,
      CurveInterpolator yInterpolator,
      CurveExtrapolator yExtrapolatorLeft,
      CurveExtrapolator yExtrapolatorRight) {

    return new GridSurfaceInterpolator(
        xInterpolator, xExtrapolatorLeft, xExtrapolatorRight, yInterpolator, yExtrapolatorLeft, yExtrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public BoundSurfaceInterpolator bind(DoubleArray xValues, DoubleArray yValues, DoubleArray zValues) {
    // single loop around all parameters, collecting data
    int size = xValues.size();
    int countUniqueX = 0;
    double[] uniqueX = new double[size];
    double[] tempY = new double[size];
    double[] tempZ = new double[size];
    ImmutableList.Builder<BoundCurveInterpolator> yInterpBuilder = ImmutableList.builder();
    int i = 0;
    while (i < size) {
      double currentX = xValues.get(i);
      uniqueX[countUniqueX] = currentX;
      if (countUniqueX > 0 && uniqueX[countUniqueX - 1] > uniqueX[countUniqueX]) {
        throw new IllegalArgumentException("Array of x-values must be sorted");
      }
      int countSameX = 0;
      while (i < size && xValues.get(i) == currentX) {
        tempY[countSameX] = yValues.get(i);
        tempZ[countSameX] = zValues.get(i);
        if (countSameX > 0 && tempY[countSameX - 1] >= tempY[countSameX]) {
          throw new IllegalArgumentException("Array of y-values must be sorted and unique within x-values");
        }
        countSameX++;
        i++;
      }
      // create a curve for the same x-value
      if (countSameX == 1) {
        // when there is only one point, there is not enough data for a curve
        // so the value must be returned without using the configured interpolator or extrapolator
        yInterpBuilder.add(new ConstantCurveInterpolator(tempZ[0]));
      } else {
        // normal case, where the curve is created
        DoubleArray yValuesSameX = DoubleArray.ofUnsafe(Arrays.copyOf(tempY, countSameX));
        DoubleArray zValuesSameX = DoubleArray.ofUnsafe(Arrays.copyOf(tempZ, countSameX));
        yInterpBuilder.add(yInterpolator.bind(yValuesSameX, zValuesSameX, yExtrapolatorLeft, yExtrapolatorRight));
      }
      countUniqueX++;
    }
    if (countUniqueX == 1) {
      throw new IllegalArgumentException("Surface interpolator requires at least two different x-values");
    }
    DoubleArray uniqueXArray = DoubleArray.ofUnsafe(Arrays.copyOf(uniqueX, countUniqueX));
    BoundCurveInterpolator[] yInterps = yInterpBuilder.build().toArray(new BoundCurveInterpolator[0]);
    return new Bound(xInterpolator, xExtrapolatorLeft, xExtrapolatorRight, size, uniqueXArray, yInterps);
  }

  //-------------------------------------------------------------------------
  /**
   * Bound interpolator.
   */
  static class Bound implements BoundSurfaceInterpolator {
    private final CurveInterpolator xInterpolator;
    private final CurveExtrapolator xExtrapolatorLeft;
    private final CurveExtrapolator xExtrapolatorRight;
    private final DoubleArray xValuesUnique;
    private final int paramSize;
    private final BoundCurveInterpolator[] yInterpolators;

    Bound(
        CurveInterpolator xInterpolator,
        CurveExtrapolator xExtrapolatorLeft,
        CurveExtrapolator xExtrapolatorRight,
        int paramSize,
        DoubleArray xValuesUnique,
        BoundCurveInterpolator[] yInterpolators) {

      this.xInterpolator = xInterpolator;
      this.xExtrapolatorLeft = xExtrapolatorLeft;
      this.xExtrapolatorRight = xExtrapolatorRight;
      this.xValuesUnique = xValuesUnique;
      this.paramSize = paramSize;
      this.yInterpolators = yInterpolators;
    }

    //-------------------------------------------------------------------------
    @Override
    public double interpolate(double x, double y) {
      // use each y-interpolator to find the z-value for each unique x
      DoubleArray zValuesEffective = DoubleArray.of(yInterpolators.length, i -> yInterpolators[i].interpolate(y));
      // interpolate unique x-values against derived z-values
      return xInterpolator.bind(xValuesUnique, zValuesEffective, xExtrapolatorLeft, xExtrapolatorRight).interpolate(x);
    }

    @Override
    public DoubleArray parameterSensitivity(double x, double y) {
      int uniqueX = yInterpolators.length;
      final DoubleArray[] ySens = new DoubleArray[uniqueX];
      // use each y-interpolator to find the z-value sensitivity for each unique x
      for (int i = 0; i < uniqueX; i++) {
        ySens[i] = yInterpolators[i].parameterSensitivity(y);
      }
      // use each y-interpolator to find the z-value for each unique x
      DoubleArray zValuesEffective = DoubleArray.of(uniqueX, i -> yInterpolators[i].interpolate(y));
      // find the sensitivity of the unique x-values against derived z-values
      DoubleArray xSens = xInterpolator
          .bind(xValuesUnique, zValuesEffective, xExtrapolatorLeft, xExtrapolatorRight)
          .parameterSensitivity(x);

      return project(xSens, ySens);
    }

    //-------------------------------------------------------------------------
    @Override
    public ValueDerivatives firstPartialDerivatives(double x, double y) {
      int uniqueX = yInterpolators.length;
      DoubleArray zValuesEffective = DoubleArray.of(uniqueX, i -> yInterpolators[i].interpolate(y));
      yInterpolators[0].interpolate(y);
      double xDerivative =
          xInterpolator.bind(xValuesUnique, zValuesEffective, xExtrapolatorLeft, xExtrapolatorRight).firstDerivative(x);
      DoubleArray yDerivatives = DoubleArray.of(uniqueX, i -> yInterpolators[i].firstDerivative(y));
      double yDerivative =
          xInterpolator.bind(xValuesUnique, yDerivatives, xExtrapolatorLeft, xExtrapolatorRight).interpolate(x);
      double zValue = interpolate(x, y);
      return ValueDerivatives.of(zValue, DoubleArray.of(xDerivative, yDerivative));
    }

    // project sensitivities back to parameters
    private DoubleArray project(DoubleArray xSens, DoubleArray[] ySens) {
      int countParam = 0;
      double[] paramSens = new double[paramSize];
      for (int i = 0; i < xSens.size(); i++) {
        double xs = xSens.get(i);
        DoubleArray ys = ySens[i];
        for (int j = 0; j < ys.size(); j++) {
          paramSens[countParam++] = xs * ys.get(j);
        }
      }
      return DoubleArray.ofUnsafe(paramSens);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * An interpolator that returns the single known value.
   */
  static class ConstantCurveInterpolator implements BoundCurveInterpolator {
    private final double value;

    public ConstantCurveInterpolator(double value) {
      this.value = value;
    }

    @Override
    public double interpolate(double x) {
      return value;
    }

    @Override
    public double firstDerivative(double x) {
      return 0;
    }

    @Override
    public DoubleArray parameterSensitivity(double x) {
      return DoubleArray.of(1);
    }

    @Override
    public BoundCurveInterpolator bind(BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      return this;
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code GridSurfaceInterpolator}.
   * @return the meta-bean, not null
   */
  public static GridSurfaceInterpolator.Meta meta() {
    return GridSurfaceInterpolator.Meta.INSTANCE;
  }

  static {
    MetaBean.register(GridSurfaceInterpolator.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private GridSurfaceInterpolator(
      CurveInterpolator xInterpolator,
      CurveExtrapolator xExtrapolatorLeft,
      CurveExtrapolator xExtrapolatorRight,
      CurveInterpolator yInterpolator,
      CurveExtrapolator yExtrapolatorLeft,
      CurveExtrapolator yExtrapolatorRight) {
    this.xInterpolator = xInterpolator;
    this.xExtrapolatorLeft = xExtrapolatorLeft;
    this.xExtrapolatorRight = xExtrapolatorRight;
    this.yInterpolator = yInterpolator;
    this.yExtrapolatorLeft = yExtrapolatorLeft;
    this.yExtrapolatorRight = yExtrapolatorRight;
  }

  @Override
  public GridSurfaceInterpolator.Meta metaBean() {
    return GridSurfaceInterpolator.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the x-value interpolator.
   * @return the value of the property
   */
  public CurveInterpolator getXInterpolator() {
    return xInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the x-value left extrapolator.
   * @return the value of the property
   */
  public CurveExtrapolator getXExtrapolatorLeft() {
    return xExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the x-value right extrapolator.
   * @return the value of the property
   */
  public CurveExtrapolator getXExtrapolatorRight() {
    return xExtrapolatorRight;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value interpolator.
   * @return the value of the property
   */
  public CurveInterpolator getYInterpolator() {
    return yInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value left extrapolator.
   * @return the value of the property
   */
  public CurveExtrapolator getYExtrapolatorLeft() {
    return yExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value right extrapolator.
   * @return the value of the property
   */
  public CurveExtrapolator getYExtrapolatorRight() {
    return yExtrapolatorRight;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      GridSurfaceInterpolator other = (GridSurfaceInterpolator) obj;
      return JodaBeanUtils.equal(xInterpolator, other.xInterpolator) &&
          JodaBeanUtils.equal(xExtrapolatorLeft, other.xExtrapolatorLeft) &&
          JodaBeanUtils.equal(xExtrapolatorRight, other.xExtrapolatorRight) &&
          JodaBeanUtils.equal(yInterpolator, other.yInterpolator) &&
          JodaBeanUtils.equal(yExtrapolatorLeft, other.yExtrapolatorLeft) &&
          JodaBeanUtils.equal(yExtrapolatorRight, other.yExtrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(xInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(xExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(xExtrapolatorRight);
    hash = hash * 31 + JodaBeanUtils.hashCode(yInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(yExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(yExtrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("GridSurfaceInterpolator{");
    buf.append("xInterpolator").append('=').append(JodaBeanUtils.toString(xInterpolator)).append(',').append(' ');
    buf.append("xExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(xExtrapolatorLeft)).append(',').append(' ');
    buf.append("xExtrapolatorRight").append('=').append(JodaBeanUtils.toString(xExtrapolatorRight)).append(',').append(' ');
    buf.append("yInterpolator").append('=').append(JodaBeanUtils.toString(yInterpolator)).append(',').append(' ');
    buf.append("yExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(yExtrapolatorLeft)).append(',').append(' ');
    buf.append("yExtrapolatorRight").append('=').append(JodaBeanUtils.toString(yExtrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code GridSurfaceInterpolator}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code xInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> xInterpolator = DirectMetaProperty.ofImmutable(
        this, "xInterpolator", GridSurfaceInterpolator.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code xExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> xExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "xExtrapolatorLeft", GridSurfaceInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code xExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> xExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "xExtrapolatorRight", GridSurfaceInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code yInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> yInterpolator = DirectMetaProperty.ofImmutable(
        this, "yInterpolator", GridSurfaceInterpolator.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code yExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> yExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "yExtrapolatorLeft", GridSurfaceInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code yExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> yExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "yExtrapolatorRight", GridSurfaceInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "xInterpolator",
        "xExtrapolatorLeft",
        "xExtrapolatorRight",
        "yInterpolator",
        "yExtrapolatorLeft",
        "yExtrapolatorRight");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1411950943:  // xInterpolator
          return xInterpolator;
        case -382665134:  // xExtrapolatorLeft
          return xExtrapolatorLeft;
        case 1027943729:  // xExtrapolatorRight
          return xExtrapolatorRight;
        case 1118547936:  // yInterpolator
          return yInterpolator;
        case 970644563:  // yExtrapolatorLeft
          return yExtrapolatorLeft;
        case 30871376:  // yExtrapolatorRight
          return yExtrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends GridSurfaceInterpolator> builder() {
      return new GridSurfaceInterpolator.Builder();
    }

    @Override
    public Class<? extends GridSurfaceInterpolator> beanType() {
      return GridSurfaceInterpolator.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code xInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> xInterpolator() {
      return xInterpolator;
    }

    /**
     * The meta-property for the {@code xExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> xExtrapolatorLeft() {
      return xExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code xExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> xExtrapolatorRight() {
      return xExtrapolatorRight;
    }

    /**
     * The meta-property for the {@code yInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> yInterpolator() {
      return yInterpolator;
    }

    /**
     * The meta-property for the {@code yExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> yExtrapolatorLeft() {
      return yExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code yExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> yExtrapolatorRight() {
      return yExtrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1411950943:  // xInterpolator
          return ((GridSurfaceInterpolator) bean).getXInterpolator();
        case -382665134:  // xExtrapolatorLeft
          return ((GridSurfaceInterpolator) bean).getXExtrapolatorLeft();
        case 1027943729:  // xExtrapolatorRight
          return ((GridSurfaceInterpolator) bean).getXExtrapolatorRight();
        case 1118547936:  // yInterpolator
          return ((GridSurfaceInterpolator) bean).getYInterpolator();
        case 970644563:  // yExtrapolatorLeft
          return ((GridSurfaceInterpolator) bean).getYExtrapolatorLeft();
        case 30871376:  // yExtrapolatorRight
          return ((GridSurfaceInterpolator) bean).getYExtrapolatorRight();
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
   * The bean-builder for {@code GridSurfaceInterpolator}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<GridSurfaceInterpolator> {

    private CurveInterpolator xInterpolator;
    private CurveExtrapolator xExtrapolatorLeft;
    private CurveExtrapolator xExtrapolatorRight;
    private CurveInterpolator yInterpolator;
    private CurveExtrapolator yExtrapolatorLeft;
    private CurveExtrapolator yExtrapolatorRight;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1411950943:  // xInterpolator
          return xInterpolator;
        case -382665134:  // xExtrapolatorLeft
          return xExtrapolatorLeft;
        case 1027943729:  // xExtrapolatorRight
          return xExtrapolatorRight;
        case 1118547936:  // yInterpolator
          return yInterpolator;
        case 970644563:  // yExtrapolatorLeft
          return yExtrapolatorLeft;
        case 30871376:  // yExtrapolatorRight
          return yExtrapolatorRight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1411950943:  // xInterpolator
          this.xInterpolator = (CurveInterpolator) newValue;
          break;
        case -382665134:  // xExtrapolatorLeft
          this.xExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case 1027943729:  // xExtrapolatorRight
          this.xExtrapolatorRight = (CurveExtrapolator) newValue;
          break;
        case 1118547936:  // yInterpolator
          this.yInterpolator = (CurveInterpolator) newValue;
          break;
        case 970644563:  // yExtrapolatorLeft
          this.yExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case 30871376:  // yExtrapolatorRight
          this.yExtrapolatorRight = (CurveExtrapolator) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public GridSurfaceInterpolator build() {
      return new GridSurfaceInterpolator(
          xInterpolator,
          xExtrapolatorLeft,
          xExtrapolatorRight,
          yInterpolator,
          yExtrapolatorLeft,
          yExtrapolatorRight);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("GridSurfaceInterpolator.Builder{");
      buf.append("xInterpolator").append('=').append(JodaBeanUtils.toString(xInterpolator)).append(',').append(' ');
      buf.append("xExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(xExtrapolatorLeft)).append(',').append(' ');
      buf.append("xExtrapolatorRight").append('=').append(JodaBeanUtils.toString(xExtrapolatorRight)).append(',').append(' ');
      buf.append("yInterpolator").append('=').append(JodaBeanUtils.toString(yInterpolator)).append(',').append(' ');
      buf.append("yExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(yExtrapolatorLeft)).append(',').append(' ');
      buf.append("yExtrapolatorRight").append('=').append(JodaBeanUtils.toString(yExtrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
