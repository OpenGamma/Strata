/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube.interpolator;

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
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;

/**
 * A cube interpolator that is based on three curve interpolators.
 * <p>
 * The interpolation operates in two stages.
 * First, the parameters are grouped into sets, each with the same x value.
 * Second, the y-z surface interpolator is used on each set of y-z values.
 * Finally, the x curve interpolator is used on the results of the y-z surface interpolation.
 * <p>
 * There should be at least two different y-values for each x-value, two different z-values for each y-value.
 * If there is only one, then the associated w-value will always be returned.
 */
@BeanDefinition(builderScope = "private")
public final class GridCubeInterpolator
    implements CubeInterpolator, ImmutableBean, Serializable {

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
  /**
   * The z-value interpolator.
   */
  @PropertyDefinition
  private final CurveInterpolator zInterpolator;
  /**
   * The z-value left extrapolator.
   */
  @PropertyDefinition
  private final CurveExtrapolator zExtrapolatorLeft;
  /**
   * The z-value right extrapolator.
   */
  @PropertyDefinition
  private final CurveExtrapolator zExtrapolatorRight;

  //-------------------------------------------------------------------------

  /**
   * Obtains an instance from the specified interpolators, using flat extrapolation.
   *
   * @param xInterpolator the x-value interpolator
   * @param yInterpolator the y-value interpolator
   * @param zInterpolator the z-value interpolator
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static GridCubeInterpolator of(
      CurveInterpolator xInterpolator,
      CurveInterpolator yInterpolator,
      CurveInterpolator zInterpolator) {

    return new GridCubeInterpolator(xInterpolator, FLAT, FLAT, yInterpolator, FLAT, FLAT, zInterpolator, FLAT, FLAT);
  }

  /**
   * Obtains an instance from the specified interpolators and extrapolators.
   *
   * @param xInterpolator the x-value interpolator
   * @param xExtrapolator the x-value extrapolator
   * @param yInterpolator the y-value interpolator
   * @param yExtrapolator the y-value extrapolator
   * @param zInterpolator the z-value interpolator
   * @param zExtrapolator the z-value extrapolator
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static GridCubeInterpolator of(
      CurveInterpolator xInterpolator,
      CurveExtrapolator xExtrapolator,
      CurveInterpolator yInterpolator,
      CurveExtrapolator yExtrapolator,
      CurveInterpolator zInterpolator,
      CurveExtrapolator zExtrapolator) {

    return new GridCubeInterpolator(
        xInterpolator,
        xExtrapolator,
        xExtrapolator,
        yInterpolator,
        yExtrapolator,
        yExtrapolator,
        zInterpolator,
        zExtrapolator,
        zExtrapolator);
  }

  /**
   * Obtains an instance from the specified interpolators and extrapolators.
   *
   * @param xInterpolator the x-value interpolator
   * @param xExtrapolatorLeft the x-value left extrapolator
   * @param xExtrapolatorRight the x-value right extrapolator
   * @param yInterpolator the y-value interpolator
   * @param yExtrapolatorLeft the y-value left extrapolator
   * @param yExtrapolatorRight the y-value right extrapolator
   * @param zInterpolator the z-value interpolator
   * @param zExtrapolatorLeft the z-value left extrapolator
   * @param zExtrapolatorRight the z-value right extrapolator
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static GridCubeInterpolator of(
      CurveInterpolator xInterpolator,
      CurveExtrapolator xExtrapolatorLeft,
      CurveExtrapolator xExtrapolatorRight,
      CurveInterpolator yInterpolator,
      CurveExtrapolator yExtrapolatorLeft,
      CurveExtrapolator yExtrapolatorRight,
      CurveInterpolator zInterpolator,
      CurveExtrapolator zExtrapolatorLeft,
      CurveExtrapolator zExtrapolatorRight) {

    return new GridCubeInterpolator(
        xInterpolator,
        xExtrapolatorLeft,
        xExtrapolatorRight,
        yInterpolator,
        yExtrapolatorLeft,
        yExtrapolatorRight,
        zInterpolator,
        zExtrapolatorLeft,
        zExtrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public BoundCubeInterpolator bind(
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues) {

    // single loop around all parameters, collecting data
    int size = xValues.size();
    int countUniqueX = 0;
    double[] uniqueX = new double[size];
    double[] tempY = new double[size];
    double[] tempZ = new double[size];
    double[] tempW = new double[size];
    ImmutableList.Builder<BoundSurfaceInterpolator> yzInterpBuilder = ImmutableList.builder();
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
        tempW[countSameX] = wValues.get(i);
        if (countSameX > 0 && tempY[countSameX - 1] > tempY[countSameX]) {
          throw new IllegalArgumentException("Array of y-values must be sorted");
        }
        countSameX++;
        i++;
      }
      // create a surface for the same x-value
      if (countSameX == 1) {
        // when there is only one point, there is not enough data for a surface
        // so the value must be returned without using the configured interpolator or extrapolator
        yzInterpBuilder.add(new GridCubeInterpolator.ConstantSurfaceInterpolator(tempZ[0]));
      } else {
        // normal case, where the surface is created
        DoubleArray yValuesSameX = DoubleArray.ofUnsafe(Arrays.copyOf(tempY, countSameX));
        DoubleArray zValuesSameX = DoubleArray.ofUnsafe(Arrays.copyOf(tempZ, countSameX));
        DoubleArray wValuesSameX = DoubleArray.ofUnsafe(Arrays.copyOf(tempW, countSameX));
        yzInterpBuilder.add(GridSurfaceInterpolator.of(
                yInterpolator,
                yExtrapolatorLeft,
                yExtrapolatorRight,
                zInterpolator,
                zExtrapolatorLeft,
                zExtrapolatorRight)
            .bind(yValuesSameX, zValuesSameX, wValuesSameX));
      }
      countUniqueX++;
    }
    if (countUniqueX == 1) {
      throw new IllegalArgumentException("Cube interpolator requires at least two different x-values");
    }
    DoubleArray uniqueXArray = DoubleArray.ofUnsafe(Arrays.copyOf(uniqueX, countUniqueX));
    BoundSurfaceInterpolator[] yzInterps = yzInterpBuilder.build().toArray(new BoundSurfaceInterpolator[0]);
    return new GridCubeInterpolator.Bound(
        xInterpolator,
        xExtrapolatorLeft,
        xExtrapolatorRight,
        size,
        uniqueXArray,
        yzInterps);
  }

  //-------------------------------------------------------------------------

  /**
   * Bound interpolator.
   */
  static class Bound implements BoundCubeInterpolator {

    private final CurveInterpolator xInterpolator;
    private final CurveExtrapolator xExtrapolatorLeft;
    private final CurveExtrapolator xExtrapolatorRight;
    private final DoubleArray xValuesUnique;
    private final int paramSize;
    private final BoundSurfaceInterpolator[] yzInterpolators;

    Bound(
        CurveInterpolator xInterpolator,
        CurveExtrapolator xExtrapolatorLeft,
        CurveExtrapolator xExtrapolatorRight,
        int paramSize,
        DoubleArray xValuesUnique,
        BoundSurfaceInterpolator[] yzInterpolators) {

      this.xInterpolator = xInterpolator;
      this.xExtrapolatorLeft = xExtrapolatorLeft;
      this.xExtrapolatorRight = xExtrapolatorRight;
      this.xValuesUnique = xValuesUnique;
      this.paramSize = paramSize;
      this.yzInterpolators = yzInterpolators;
    }

    //-------------------------------------------------------------------------
    @Override
    public double interpolate(double x, double y, double z) {
      // use each yz-interpolator to find the w-value for each unique x
      DoubleArray wValuesEffective = DoubleArray.of(yzInterpolators.length, i -> yzInterpolators[i].interpolate(y, z));
      // interpolate unique x-values against derived w-values
      return xInterpolator.bind(xValuesUnique, wValuesEffective, xExtrapolatorLeft, xExtrapolatorRight).interpolate(x);
    }

    @Override
    public DoubleArray parameterSensitivity(double x, double y, double z) {
      int uniqueX = yzInterpolators.length;
      final DoubleArray[] yzSens = new DoubleArray[uniqueX];
      // use each yz-interpolator to find the w-value sensitivity for each unique x
      for (int i = 0; i < uniqueX; i++) {
        yzSens[i] = yzInterpolators[i].parameterSensitivity(y, z);
      }
      // use each yz-interpolator to find the w-value for each unique x
      DoubleArray wValuesEffective = DoubleArray.of(uniqueX, i -> yzInterpolators[i].interpolate(y, z));
      // find the sensitivity of the unique x-values against derived w-values
      DoubleArray xSens = xInterpolator
          .bind(xValuesUnique, wValuesEffective, xExtrapolatorLeft, xExtrapolatorRight)
          .parameterSensitivity(x);

      return project(xSens, yzSens);
    }

    //-------------------------------------------------------------------------
    @Override
    public ValueDerivatives firstPartialDerivatives(double x, double y, double z) {
      int uniqueX = yzInterpolators.length;
      DoubleArray wValuesEffective = DoubleArray.of(uniqueX, i -> yzInterpolators[i].interpolate(y, z));
      double xDerivative =
          xInterpolator.bind(xValuesUnique, wValuesEffective, xExtrapolatorLeft, xExtrapolatorRight).firstDerivative(x);
      DoubleArray yDerivatives =
          DoubleArray.of(uniqueX, i -> yzInterpolators[i].firstPartialDerivatives(y, z).getDerivative(0));
      DoubleArray zDerivatives =
          DoubleArray.of(uniqueX, i -> yzInterpolators[i].firstPartialDerivatives(y, z).getDerivative(1));
      double yDerivative =
          xInterpolator.bind(xValuesUnique, yDerivatives, xExtrapolatorLeft, xExtrapolatorRight).interpolate(x);
      double zDerivative =
          xInterpolator.bind(xValuesUnique, zDerivatives, xExtrapolatorLeft, xExtrapolatorRight).interpolate(x);
      double wValue = interpolate(x, y, z);
      return ValueDerivatives.of(wValue, DoubleArray.of(xDerivative, yDerivative, zDerivative));
    }

    // project sensitivities back to parameters
    private DoubleArray project(DoubleArray xSens, DoubleArray[] yzSens) {
      int countParam = 0;
      double[] paramSens = new double[paramSize];
      for (int i = 0; i < xSens.size(); i++) {
        double xs = xSens.get(i);
        DoubleArray yzs = yzSens[i];
        for (int j = 0; j < yzs.size(); j++) {
          paramSens[countParam++] = xs * yzs.get(j);
        }
      }
      return DoubleArray.ofUnsafe(paramSens);
    }
  }

  //-------------------------------------------------------------------------

  /**
   * An interpolator that returns the single known value.
   */
  static class ConstantSurfaceInterpolator implements BoundSurfaceInterpolator {

    private final double value;

    public ConstantSurfaceInterpolator(double value) {
      this.value = value;
    }

    @Override
    public double interpolate(double x, double y) {
      return value;
    }

    @Override
    public ValueDerivatives firstPartialDerivatives(double x, double y) {
      return ValueDerivatives.of(value, DoubleArray.of(0d, 0d));
    }

    @Override
    public DoubleArray parameterSensitivity(double x, double y) {
      return DoubleArray.of(1);
    }

  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code GridCubeInterpolator}.
   * @return the meta-bean, not null
   */
  public static GridCubeInterpolator.Meta meta() {
    return GridCubeInterpolator.Meta.INSTANCE;
  }

  static {
    MetaBean.register(GridCubeInterpolator.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private GridCubeInterpolator(
      CurveInterpolator xInterpolator,
      CurveExtrapolator xExtrapolatorLeft,
      CurveExtrapolator xExtrapolatorRight,
      CurveInterpolator yInterpolator,
      CurveExtrapolator yExtrapolatorLeft,
      CurveExtrapolator yExtrapolatorRight,
      CurveInterpolator zInterpolator,
      CurveExtrapolator zExtrapolatorLeft,
      CurveExtrapolator zExtrapolatorRight) {
    this.xInterpolator = xInterpolator;
    this.xExtrapolatorLeft = xExtrapolatorLeft;
    this.xExtrapolatorRight = xExtrapolatorRight;
    this.yInterpolator = yInterpolator;
    this.yExtrapolatorLeft = yExtrapolatorLeft;
    this.yExtrapolatorRight = yExtrapolatorRight;
    this.zInterpolator = zInterpolator;
    this.zExtrapolatorLeft = zExtrapolatorLeft;
    this.zExtrapolatorRight = zExtrapolatorRight;
  }

  @Override
  public GridCubeInterpolator.Meta metaBean() {
    return GridCubeInterpolator.Meta.INSTANCE;
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
  /**
   * Gets the z-value interpolator.
   * @return the value of the property
   */
  public CurveInterpolator getZInterpolator() {
    return zInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the z-value left extrapolator.
   * @return the value of the property
   */
  public CurveExtrapolator getZExtrapolatorLeft() {
    return zExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the z-value right extrapolator.
   * @return the value of the property
   */
  public CurveExtrapolator getZExtrapolatorRight() {
    return zExtrapolatorRight;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      GridCubeInterpolator other = (GridCubeInterpolator) obj;
      return JodaBeanUtils.equal(xInterpolator, other.xInterpolator) &&
          JodaBeanUtils.equal(xExtrapolatorLeft, other.xExtrapolatorLeft) &&
          JodaBeanUtils.equal(xExtrapolatorRight, other.xExtrapolatorRight) &&
          JodaBeanUtils.equal(yInterpolator, other.yInterpolator) &&
          JodaBeanUtils.equal(yExtrapolatorLeft, other.yExtrapolatorLeft) &&
          JodaBeanUtils.equal(yExtrapolatorRight, other.yExtrapolatorRight) &&
          JodaBeanUtils.equal(zInterpolator, other.zInterpolator) &&
          JodaBeanUtils.equal(zExtrapolatorLeft, other.zExtrapolatorLeft) &&
          JodaBeanUtils.equal(zExtrapolatorRight, other.zExtrapolatorRight);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(zInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(zExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(zExtrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("GridCubeInterpolator{");
    buf.append("xInterpolator").append('=').append(JodaBeanUtils.toString(xInterpolator)).append(',').append(' ');
    buf.append("xExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(xExtrapolatorLeft)).append(',').append(' ');
    buf.append("xExtrapolatorRight").append('=').append(JodaBeanUtils.toString(xExtrapolatorRight)).append(',').append(' ');
    buf.append("yInterpolator").append('=').append(JodaBeanUtils.toString(yInterpolator)).append(',').append(' ');
    buf.append("yExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(yExtrapolatorLeft)).append(',').append(' ');
    buf.append("yExtrapolatorRight").append('=').append(JodaBeanUtils.toString(yExtrapolatorRight)).append(',').append(' ');
    buf.append("zInterpolator").append('=').append(JodaBeanUtils.toString(zInterpolator)).append(',').append(' ');
    buf.append("zExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(zExtrapolatorLeft)).append(',').append(' ');
    buf.append("zExtrapolatorRight").append('=').append(JodaBeanUtils.toString(zExtrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code GridCubeInterpolator}.
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
        this, "xInterpolator", GridCubeInterpolator.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code xExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> xExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "xExtrapolatorLeft", GridCubeInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code xExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> xExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "xExtrapolatorRight", GridCubeInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code yInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> yInterpolator = DirectMetaProperty.ofImmutable(
        this, "yInterpolator", GridCubeInterpolator.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code yExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> yExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "yExtrapolatorLeft", GridCubeInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code yExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> yExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "yExtrapolatorRight", GridCubeInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code zInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> zInterpolator = DirectMetaProperty.ofImmutable(
        this, "zInterpolator", GridCubeInterpolator.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code zExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> zExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "zExtrapolatorLeft", GridCubeInterpolator.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code zExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> zExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "zExtrapolatorRight", GridCubeInterpolator.class, CurveExtrapolator.class);
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
        "yExtrapolatorRight",
        "zInterpolator",
        "zExtrapolatorLeft",
        "zExtrapolatorRight");

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
        case 825144929:  // zInterpolator
          return zInterpolator;
        case -1971013036:  // zExtrapolatorLeft
          return zExtrapolatorLeft;
        case -966200977:  // zExtrapolatorRight
          return zExtrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends GridCubeInterpolator> builder() {
      return new GridCubeInterpolator.Builder();
    }

    @Override
    public Class<? extends GridCubeInterpolator> beanType() {
      return GridCubeInterpolator.class;
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

    /**
     * The meta-property for the {@code zInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> zInterpolator() {
      return zInterpolator;
    }

    /**
     * The meta-property for the {@code zExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> zExtrapolatorLeft() {
      return zExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code zExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> zExtrapolatorRight() {
      return zExtrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1411950943:  // xInterpolator
          return ((GridCubeInterpolator) bean).getXInterpolator();
        case -382665134:  // xExtrapolatorLeft
          return ((GridCubeInterpolator) bean).getXExtrapolatorLeft();
        case 1027943729:  // xExtrapolatorRight
          return ((GridCubeInterpolator) bean).getXExtrapolatorRight();
        case 1118547936:  // yInterpolator
          return ((GridCubeInterpolator) bean).getYInterpolator();
        case 970644563:  // yExtrapolatorLeft
          return ((GridCubeInterpolator) bean).getYExtrapolatorLeft();
        case 30871376:  // yExtrapolatorRight
          return ((GridCubeInterpolator) bean).getYExtrapolatorRight();
        case 825144929:  // zInterpolator
          return ((GridCubeInterpolator) bean).getZInterpolator();
        case -1971013036:  // zExtrapolatorLeft
          return ((GridCubeInterpolator) bean).getZExtrapolatorLeft();
        case -966200977:  // zExtrapolatorRight
          return ((GridCubeInterpolator) bean).getZExtrapolatorRight();
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
   * The bean-builder for {@code GridCubeInterpolator}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<GridCubeInterpolator> {

    private CurveInterpolator xInterpolator;
    private CurveExtrapolator xExtrapolatorLeft;
    private CurveExtrapolator xExtrapolatorRight;
    private CurveInterpolator yInterpolator;
    private CurveExtrapolator yExtrapolatorLeft;
    private CurveExtrapolator yExtrapolatorRight;
    private CurveInterpolator zInterpolator;
    private CurveExtrapolator zExtrapolatorLeft;
    private CurveExtrapolator zExtrapolatorRight;

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
        case 825144929:  // zInterpolator
          return zInterpolator;
        case -1971013036:  // zExtrapolatorLeft
          return zExtrapolatorLeft;
        case -966200977:  // zExtrapolatorRight
          return zExtrapolatorRight;
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
        case 825144929:  // zInterpolator
          this.zInterpolator = (CurveInterpolator) newValue;
          break;
        case -1971013036:  // zExtrapolatorLeft
          this.zExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case -966200977:  // zExtrapolatorRight
          this.zExtrapolatorRight = (CurveExtrapolator) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public GridCubeInterpolator build() {
      return new GridCubeInterpolator(
          xInterpolator,
          xExtrapolatorLeft,
          xExtrapolatorRight,
          yInterpolator,
          yExtrapolatorLeft,
          yExtrapolatorRight,
          zInterpolator,
          zExtrapolatorLeft,
          zExtrapolatorRight);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("GridCubeInterpolator.Builder{");
      buf.append("xInterpolator").append('=').append(JodaBeanUtils.toString(xInterpolator)).append(',').append(' ');
      buf.append("xExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(xExtrapolatorLeft)).append(',').append(' ');
      buf.append("xExtrapolatorRight").append('=').append(JodaBeanUtils.toString(xExtrapolatorRight)).append(',').append(' ');
      buf.append("yInterpolator").append('=').append(JodaBeanUtils.toString(yInterpolator)).append(',').append(' ');
      buf.append("yExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(yExtrapolatorLeft)).append(',').append(' ');
      buf.append("yExtrapolatorRight").append('=').append(JodaBeanUtils.toString(yExtrapolatorRight)).append(',').append(' ');
      buf.append("zInterpolator").append('=').append(JodaBeanUtils.toString(zInterpolator)).append(',').append(' ');
      buf.append("zExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(zExtrapolatorLeft)).append(',').append(' ');
      buf.append("zExtrapolatorRight").append('=').append(JodaBeanUtils.toString(zExtrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
