/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;

import java.io.Serializable;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfacePeriodParameterMetadata;
import com.opengamma.strata.pricer.option.RawOptionData;

/**
 * Definition of caplet volatilities calibration.
 * <p>
 * This definition is used with {@link SurfaceIborCapletFloorletVolatilityBootstrapper}. 
 * The caplet volatilities are computed by bootstrap along the time direction, 
 * thus the interpolation and left extrapolation for the time dimension must be local. 
 * The resulting volatilities object will be a set of caplet volatilities interpolated by {@link GridSurfaceInterpolator}.
 */
@BeanDefinition(builderScope = "private")
public final class SurfaceIborCapletFloorletVolatilityBootstrapDefinition
    implements IborCapletFloorletVolatilityDefinition, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborCapletFloorletVolatilitiesName name;
  /**
   * The Ibor index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;
  /**
   * The day count to measure the time in the expiry dimension.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;
  /**
   * The interpolator for the caplet volatilities.
   */
  @PropertyDefinition(validate = "notNull")
  private final GridSurfaceInterpolator interpolator;
  /**
   * The shift parameter of shifted Black model.
   * <p>
   * The market volatilities are calibrated to shifted Black model if this field is not null.
   */
  @PropertyDefinition(get = "optional")
  private final Curve shiftCurve;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with gird surface interpolator.
   * 
   * @param name  the name of the volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count to use
   * @param interpolator  the surface interpolator
   * @return the instance
   */
  public static SurfaceIborCapletFloorletVolatilityBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      GridSurfaceInterpolator interpolator) {

    return of(name, index, dayCount, interpolator, null);
  }

  /**
   * Obtains an instance with gird surface interpolator and shift curve.
   * 
   * @param name  the name of the volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count to use
   * @param interpolator  the surface interpolator
   * @param shiftCurve  the shift curve
   * @return the instance
   */
  public static SurfaceIborCapletFloorletVolatilityBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      GridSurfaceInterpolator interpolator,
      Curve shiftCurve) {

    return new SurfaceIborCapletFloorletVolatilityBootstrapDefinition(name, index, dayCount, interpolator, shiftCurve);
  }

  /**
   * Obtains an instance with time interpolator and strike interpolator. 
   * <p>
   * The extrapolation is completed by default extrapolators in {@code GridSurfaceInterpolator}.
   * 
   * @param name  the name of the volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count to use
   * @param timeInterpolator  the time interpolator
   * @param strikeInterpolator  the strike interpolator
   * @return the instance
   */
  public static SurfaceIborCapletFloorletVolatilityBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveInterpolator strikeInterpolator) {

    return of(name, index, dayCount, timeInterpolator, strikeInterpolator, null);
  }

  /**
   * Obtains an instance with time interpolator, strike interpolator and shift curve.
   * <p>
   * The extrapolation is completed by default extrapolators in {@code GridSurfaceInterpolator}.
   * 
   * @param name  the name of the volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count to use
   * @param timeInterpolator  the time interpolator
   * @param strikeInterpolator  the strike interpolator
   * @param shiftCurve  the shift curve 
   * @return the instance
   */
  public static SurfaceIborCapletFloorletVolatilityBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveInterpolator strikeInterpolator,
      Curve shiftCurve) {

    GridSurfaceInterpolator gridInterpolator = GridSurfaceInterpolator.of(timeInterpolator, strikeInterpolator);
    return of(name, index, dayCount, gridInterpolator, shiftCurve);
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(interpolator.getXExtrapolatorLeft().equals(FLAT), "x extrapolator left must be flat extrapolator");
    ArgChecker.isTrue(interpolator.getXInterpolator().equals(CurveInterpolators.LINEAR) ||
        interpolator.getXInterpolator().equals(CurveInterpolators.STEP_UPPER) ||
        interpolator.getXInterpolator().equals(CurveInterpolators.TIME_SQUARE),
        "x interpolator must be local interpolator");
  }

  //-------------------------------------------------------------------------
  @Override
  public SurfaceMetadata createMetadata(RawOptionData capFloorData) {
    List<GenericVolatilitySurfacePeriodParameterMetadata> list = new ArrayList<>();
    ImmutableList<Period> expiries = capFloorData.getExpiries();
    int nExpiries = expiries.size();
    DoubleArray strikes = capFloorData.getStrikes();
    int nStrikes = strikes.size();
    for (int i = 0; i < nExpiries; ++i) {
      for (int j = 0; j < nStrikes; ++j) {
        if (Double.isFinite(capFloorData.getData().get(i, j))) {
          list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(expiries.get(i), SimpleStrike.of(strikes.get(j))));
        }
      }
    }
    SurfaceMetadata metadata;
    if (capFloorData.getDataType().equals(ValueType.BLACK_VOLATILITY)) {
      metadata = Surfaces.blackVolatilityByExpiryStrike(name.getName(), dayCount);
    } else if (capFloorData.getDataType().equals(ValueType.NORMAL_VOLATILITY)) {
      metadata = Surfaces.normalVolatilityByExpiryStrike(name.getName(), dayCount);
    } else {
      throw new IllegalArgumentException("Data type not supported");
    }
    return metadata.withParameterMetadata(list);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SurfaceIborCapletFloorletVolatilityBootstrapDefinition}.
   * @return the meta-bean, not null
   */
  public static SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Meta meta() {
    return SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SurfaceIborCapletFloorletVolatilityBootstrapDefinition(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      GridSurfaceInterpolator interpolator,
      Curve shiftCurve) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(interpolator, "interpolator");
    this.name = name;
    this.index = index;
    this.dayCount = dayCount;
    this.interpolator = interpolator;
    this.shiftCurve = shiftCurve;
    validate();
  }

  @Override
  public SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Meta metaBean() {
    return SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  @Override
  public IborCapletFloorletVolatilitiesName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * @return the value of the property, not null
   */
  @Override
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count to measure the time in the expiry dimension.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator for the caplet volatilities.
   * @return the value of the property, not null
   */
  public GridSurfaceInterpolator getInterpolator() {
    return interpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift parameter of shifted Black model.
   * <p>
   * The market volatilities are calibrated to shifted Black model if this field is not null.
   * @return the optional value of the property, not null
   */
  public Optional<Curve> getShiftCurve() {
    return Optional.ofNullable(shiftCurve);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SurfaceIborCapletFloorletVolatilityBootstrapDefinition other = (SurfaceIborCapletFloorletVolatilityBootstrapDefinition) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(interpolator, other.interpolator) &&
          JodaBeanUtils.equal(shiftCurve, other.shiftCurve);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftCurve);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("SurfaceIborCapletFloorletVolatilityBootstrapDefinition{");
    buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
    buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
    buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator)).append(',').append(' ');
    buf.append("shiftCurve").append('=').append(JodaBeanUtils.toString(shiftCurve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SurfaceIborCapletFloorletVolatilityBootstrapDefinition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<IborCapletFloorletVolatilitiesName> name = DirectMetaProperty.ofImmutable(
        this, "name", SurfaceIborCapletFloorletVolatilityBootstrapDefinition.class, IborCapletFloorletVolatilitiesName.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", SurfaceIborCapletFloorletVolatilityBootstrapDefinition.class, IborIndex.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", SurfaceIborCapletFloorletVolatilityBootstrapDefinition.class, DayCount.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<GridSurfaceInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", SurfaceIborCapletFloorletVolatilityBootstrapDefinition.class, GridSurfaceInterpolator.class);
    /**
     * The meta-property for the {@code shiftCurve} property.
     */
    private final MetaProperty<Curve> shiftCurve = DirectMetaProperty.ofImmutable(
        this, "shiftCurve", SurfaceIborCapletFloorletVolatilityBootstrapDefinition.class, Curve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "index",
        "dayCount",
        "interpolator",
        "shiftCurve");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 100346066:  // index
          return index;
        case 1905311443:  // dayCount
          return dayCount;
        case 2096253127:  // interpolator
          return interpolator;
        case 1908090253:  // shiftCurve
          return shiftCurve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SurfaceIborCapletFloorletVolatilityBootstrapDefinition> builder() {
      return new SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Builder();
    }

    @Override
    public Class<? extends SurfaceIborCapletFloorletVolatilityBootstrapDefinition> beanType() {
      return SurfaceIborCapletFloorletVolatilityBootstrapDefinition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborCapletFloorletVolatilitiesName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<GridSurfaceInterpolator> interpolator() {
      return interpolator;
    }

    /**
     * The meta-property for the {@code shiftCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> shiftCurve() {
      return shiftCurve;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((SurfaceIborCapletFloorletVolatilityBootstrapDefinition) bean).getName();
        case 100346066:  // index
          return ((SurfaceIborCapletFloorletVolatilityBootstrapDefinition) bean).getIndex();
        case 1905311443:  // dayCount
          return ((SurfaceIborCapletFloorletVolatilityBootstrapDefinition) bean).getDayCount();
        case 2096253127:  // interpolator
          return ((SurfaceIborCapletFloorletVolatilityBootstrapDefinition) bean).getInterpolator();
        case 1908090253:  // shiftCurve
          return ((SurfaceIborCapletFloorletVolatilityBootstrapDefinition) bean).shiftCurve;
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
   * The bean-builder for {@code SurfaceIborCapletFloorletVolatilityBootstrapDefinition}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SurfaceIborCapletFloorletVolatilityBootstrapDefinition> {

    private IborCapletFloorletVolatilitiesName name;
    private IborIndex index;
    private DayCount dayCount;
    private GridSurfaceInterpolator interpolator;
    private Curve shiftCurve;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 100346066:  // index
          return index;
        case 1905311443:  // dayCount
          return dayCount;
        case 2096253127:  // interpolator
          return interpolator;
        case 1908090253:  // shiftCurve
          return shiftCurve;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (IborCapletFloorletVolatilitiesName) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (GridSurfaceInterpolator) newValue;
          break;
        case 1908090253:  // shiftCurve
          this.shiftCurve = (Curve) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SurfaceIborCapletFloorletVolatilityBootstrapDefinition build() {
      return new SurfaceIborCapletFloorletVolatilityBootstrapDefinition(
          name,
          index,
          dayCount,
          interpolator,
          shiftCurve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("SurfaceIborCapletFloorletVolatilityBootstrapDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator)).append(',').append(' ');
      buf.append("shiftCurve").append('=').append(JodaBeanUtils.toString(shiftCurve));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
