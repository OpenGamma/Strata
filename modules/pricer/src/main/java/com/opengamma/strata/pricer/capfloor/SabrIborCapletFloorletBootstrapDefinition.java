/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.SABR_ALPHA;
import static com.opengamma.strata.market.ValueType.SABR_BETA;
import static com.opengamma.strata.market.ValueType.SABR_NU;
import static com.opengamma.strata.market.ValueType.SABR_RHO;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.option.RawOptionData;

/**
 * Definition of caplet volatilities calibration.
 * <p>
 * This definition is used with {@link SabrIborCapletFloorletVolatilityBootstrapper}. 
 * The SABR parameters are computed by bootstrap along the time direction, 
 * thus the interpolation and left extrapolation for the time dimension must be local. 
 * The resulting volatilities object will be {@link SabrParametersIborCapletFloorletVolatilities}.
 */
@BeanDefinition
public final class SabrIborCapletFloorletBootstrapDefinition
    implements IborCapletFloorletDefinition, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborCapletFloorletVolatilitiesName name;
  /**
   * The Ibor index for which the data is valid.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;
  /**
   * The day count to use.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;
  /**
   * The beta (elasticity) curve.
   * <p>
   * This represents the beta parameter of SABR model.
   * The x value of the curve is the expiry.
   * <p>
   * The beta will be treated as one of the calibration parameters if this field is not specified.
   */
  @PropertyDefinition(get = "optional")
  private final Curve betaCurve;
  /**
   * The shift curve.
   * <p>
   * This represents the shift parameter of shifted SABR model.
   * The x value of the curve is the expiry.
   * <p>
   * The shift is set to be zero if this field is not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve shiftCurve;
  /**
   * The interpolator for the SABR parameters.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator interpolator;
  /**
   * The left extrapolator for the SABR parameters.
   * <p>
   * The flat extrapolation is used if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorLeft;
  /**
   * The right extrapolator for the SABR parameters.
   * <p>
   * The flat extrapolation is used if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorRight;
  /**
   * The SABR formula.
   */
  @PropertyDefinition(validate = "notNull")
  private final SabrVolatilityFormula sabrVolatilityFormula;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with zero shift.
   * 
   * @param name  the name of volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count
   * @param interpolator  the interpolator
   * @param sabrVolatilityFormula  the SABR formula
   * @return the instance
   */
  public static SabrIborCapletFloorletBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      CurveInterpolator interpolator,
      SabrVolatilityFormula sabrVolatilityFormula) {

    return of(name, index, dayCount, interpolator, FLAT, FLAT, sabrVolatilityFormula);
  }

  /**
   * Obtains an instance with zero shift.
   * 
   * @param name  the name of volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count
   * @param interpolator  the interpolator
   * @param extrapolatorLeft  the left extrapolator
   * @param extrapolatorRight  the right extrapolator
   * @param sabrVolatilityFormula  the SABR formula
   * @return the instance
   */
  public static SabrIborCapletFloorletBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight,
      SabrVolatilityFormula sabrVolatilityFormula) {

    Curve shiftCurve = ConstantCurve.of("Zero shift", 0d);
    return new SabrIborCapletFloorletBootstrapDefinition(
        name, index, dayCount, null, shiftCurve, interpolator, extrapolatorLeft, extrapolatorRight, sabrVolatilityFormula);
  }

  /**
   * Obtains an instance with zero shift and constant beta.
   * 
   * @param name  the name of volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count
   * @param beta  the beta value
   * @param interpolator  the interpolator
   * @param sabrVolatilityFormula  the SABR formula
   * @return the instance
   */
  public static SabrIborCapletFloorletBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      double beta,
      CurveInterpolator interpolator,
      SabrVolatilityFormula sabrVolatilityFormula) {

    return of(name, index, dayCount, beta, interpolator, FLAT, FLAT, sabrVolatilityFormula);
  }

  /**
   * Obtains an instance with zero shift and constant beta.
   * 
   * @param name  the name of volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count
   * @param beta  the beta value
   * @param interpolator  the interpolator
   * @param extrapolatorLeft  the left extrapolator
   * @param extrapolatorRight  the right extrapolator
   * @param sabrVolatilityFormula  the SABR formula
   * @return the instance
   */
  public static SabrIborCapletFloorletBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      double beta,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight,
      SabrVolatilityFormula sabrVolatilityFormula) {

    Curve shiftCurve = ConstantCurve.of("Zero shift", 0d);
    ConstantCurve betaCurve = ConstantCurve.of(
        Curves.sabrParameterByExpiry(name.getName() + "-Beta", dayCount, SABR_BETA), beta);
    return new SabrIborCapletFloorletBootstrapDefinition(
        name, index, dayCount, betaCurve, shiftCurve, interpolator, extrapolatorLeft, extrapolatorRight, sabrVolatilityFormula);
  }

  /**
   * Obtains an instance with constant beta and shift. 
   * 
   * @param name  the name of volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count
   * @param beta  the beta value
   * @param shift  the shift value
   * @param interpolator  the interpolator
   * @param sabrVolatilityFormula  the SABR formula
   * @return the instance
   */
  public static SabrIborCapletFloorletBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      double beta,
      double shift,
      CurveInterpolator interpolator,
      SabrVolatilityFormula sabrVolatilityFormula) {

    return of(name, index, dayCount, beta, shift, interpolator, FLAT, FLAT, sabrVolatilityFormula);
  }

  /**
   * Obtains an instance with constant beta and shift. 
   * 
   * @param name  the name of volatilities
   * @param index  the Ibor index
   * @param dayCount  the day count
   * @param beta  the beta value
   * @param shift  the shift value
   * @param interpolator  the interpolator
   * @param extrapolatorLeft  the left extrapolator
   * @param extrapolatorRight  the right extrapolator
   * @param sabrVolatilityFormula  the SABR formula
   * @return the instance
   */
  public static SabrIborCapletFloorletBootstrapDefinition of(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      double beta,
      double shift,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight,
      SabrVolatilityFormula sabrVolatilityFormula) {

    ConstantCurve shiftCurve = ConstantCurve.of("Shift curve", shift);
    ConstantCurve betaCurve = ConstantCurve.of(
        Curves.sabrParameterByExpiry(name.getName() + "-Beta", dayCount, SABR_BETA), beta);
    return new SabrIborCapletFloorletBootstrapDefinition(
        name, index, dayCount, betaCurve, shiftCurve, interpolator, extrapolatorLeft, extrapolatorRight, sabrVolatilityFormula);
  }

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.shiftCurve == null) {
      builder.shiftCurve = ConstantCurve.of("Zero shift", 0d);
    }
    if (builder.extrapolatorLeft == null) {
      builder.extrapolatorLeft = FLAT;
    }
    if (builder.extrapolatorRight == null) {
      builder.extrapolatorRight = FLAT;
    }
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(extrapolatorLeft.equals(FLAT), "extrapolator left must be flat extrapolator");
    ArgChecker.isTrue(
        interpolator.equals(CurveInterpolators.LINEAR) || interpolator.equals(CurveInterpolators.STEP_UPPER),
        "interpolator must be local interpolator");
  }

  //-------------------------------------------------------------------------
  @Override
  public SurfaceMetadata createMetadata(RawOptionData capFloorData) {
    SurfaceMetadata metadata;
    if (capFloorData.getDataType().equals(BLACK_VOLATILITY)) {
      metadata = Surfaces.blackVolatilityByExpiryStrike(name.getName(), dayCount);
    } else if (capFloorData.getDataType().equals(NORMAL_VOLATILITY)) {
      metadata = Surfaces.normalVolatilityByExpiryStrike(name.getName(), dayCount);
    } else {
      throw new IllegalArgumentException("Data type not supported");
    }
    return metadata;
  }

  /**
   * Creates curve metadata for SABR parameters.
   * 
   * @return the curve metadata
   */
  public ImmutableList<CurveMetadata> createSabrParameterMetadata() {
    CurveMetadata alphaMetadata = Curves.sabrParameterByExpiry(name.getName() + "-Alpha", dayCount, SABR_ALPHA);
    CurveMetadata betaMetadata = Curves.sabrParameterByExpiry(name.getName() + "-Beta", dayCount, SABR_BETA);
    CurveMetadata rhoMetadata = Curves.sabrParameterByExpiry(name.getName() + "-Nu", dayCount, SABR_RHO);
    CurveMetadata nuMetadata = Curves.sabrParameterByExpiry(name.getName() + "-Rho", dayCount, SABR_NU);
    return ImmutableList.of(alphaMetadata, betaMetadata, rhoMetadata, nuMetadata);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrIborCapletFloorletBootstrapDefinition}.
   * @return the meta-bean, not null
   */
  public static SabrIborCapletFloorletBootstrapDefinition.Meta meta() {
    return SabrIborCapletFloorletBootstrapDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SabrIborCapletFloorletBootstrapDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SabrIborCapletFloorletBootstrapDefinition.Builder builder() {
    return new SabrIborCapletFloorletBootstrapDefinition.Builder();
  }

  private SabrIborCapletFloorletBootstrapDefinition(
      IborCapletFloorletVolatilitiesName name,
      IborIndex index,
      DayCount dayCount,
      Curve betaCurve,
      Curve shiftCurve,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight,
      SabrVolatilityFormula sabrVolatilityFormula) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(shiftCurve, "shiftCurve");
    JodaBeanUtils.notNull(interpolator, "interpolator");
    JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
    JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
    JodaBeanUtils.notNull(sabrVolatilityFormula, "sabrVolatilityFormula");
    this.name = name;
    this.index = index;
    this.dayCount = dayCount;
    this.betaCurve = betaCurve;
    this.shiftCurve = shiftCurve;
    this.interpolator = interpolator;
    this.extrapolatorLeft = extrapolatorLeft;
    this.extrapolatorRight = extrapolatorRight;
    this.sabrVolatilityFormula = sabrVolatilityFormula;
    validate();
  }

  @Override
  public SabrIborCapletFloorletBootstrapDefinition.Meta metaBean() {
    return SabrIborCapletFloorletBootstrapDefinition.Meta.INSTANCE;
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
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  @Override
  public IborCapletFloorletVolatilitiesName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor index for which the data is valid.
   * @return the value of the property, not null
   */
  @Override
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count to use.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the beta (elasticity) curve.
   * <p>
   * This represents the beta parameter of SABR model.
   * The x value of the curve is the expiry.
   * <p>
   * The beta will be treated as one of the calibration parameters if this field is not specified.
   * @return the optional value of the property, not null
   */
  public Optional<Curve> getBetaCurve() {
    return Optional.ofNullable(betaCurve);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift curve.
   * <p>
   * This represents the shift parameter of shifted SABR model.
   * The x value of the curve is the expiry.
   * <p>
   * The shift is set to be zero if this field is not specified.
   * @return the value of the property, not null
   */
  public Curve getShiftCurve() {
    return shiftCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator for the SABR parameters.
   * @return the value of the property, not null
   */
  public CurveInterpolator getInterpolator() {
    return interpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left extrapolator for the SABR parameters.
   * <p>
   * The flat extrapolation is used if not specified.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorLeft() {
    return extrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right extrapolator for the SABR parameters.
   * <p>
   * The flat extrapolation is used if not specified.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorRight() {
    return extrapolatorRight;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the SABR formula.
   * @return the value of the property, not null
   */
  public SabrVolatilityFormula getSabrVolatilityFormula() {
    return sabrVolatilityFormula;
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
      SabrIborCapletFloorletBootstrapDefinition other = (SabrIborCapletFloorletBootstrapDefinition) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(betaCurve, other.betaCurve) &&
          JodaBeanUtils.equal(shiftCurve, other.shiftCurve) &&
          JodaBeanUtils.equal(interpolator, other.interpolator) &&
          JodaBeanUtils.equal(extrapolatorLeft, other.extrapolatorLeft) &&
          JodaBeanUtils.equal(extrapolatorRight, other.extrapolatorRight) &&
          JodaBeanUtils.equal(sabrVolatilityFormula, other.sabrVolatilityFormula);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(betaCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorRight);
    hash = hash * 31 + JodaBeanUtils.hashCode(sabrVolatilityFormula);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("SabrIborCapletFloorletBootstrapDefinition{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("betaCurve").append('=').append(betaCurve).append(',').append(' ');
    buf.append("shiftCurve").append('=').append(shiftCurve).append(',').append(' ');
    buf.append("interpolator").append('=').append(interpolator).append(',').append(' ');
    buf.append("extrapolatorLeft").append('=').append(extrapolatorLeft).append(',').append(' ');
    buf.append("extrapolatorRight").append('=').append(extrapolatorRight).append(',').append(' ');
    buf.append("sabrVolatilityFormula").append('=').append(JodaBeanUtils.toString(sabrVolatilityFormula));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrIborCapletFloorletBootstrapDefinition}.
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
        this, "name", SabrIborCapletFloorletBootstrapDefinition.class, IborCapletFloorletVolatilitiesName.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", SabrIborCapletFloorletBootstrapDefinition.class, IborIndex.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", SabrIborCapletFloorletBootstrapDefinition.class, DayCount.class);
    /**
     * The meta-property for the {@code betaCurve} property.
     */
    private final MetaProperty<Curve> betaCurve = DirectMetaProperty.ofImmutable(
        this, "betaCurve", SabrIborCapletFloorletBootstrapDefinition.class, Curve.class);
    /**
     * The meta-property for the {@code shiftCurve} property.
     */
    private final MetaProperty<Curve> shiftCurve = DirectMetaProperty.ofImmutable(
        this, "shiftCurve", SabrIborCapletFloorletBootstrapDefinition.class, Curve.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<CurveInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", SabrIborCapletFloorletBootstrapDefinition.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "extrapolatorLeft", SabrIborCapletFloorletBootstrapDefinition.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code extrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "extrapolatorRight", SabrIborCapletFloorletBootstrapDefinition.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code sabrVolatilityFormula} property.
     */
    private final MetaProperty<SabrVolatilityFormula> sabrVolatilityFormula = DirectMetaProperty.ofImmutable(
        this, "sabrVolatilityFormula", SabrIborCapletFloorletBootstrapDefinition.class, SabrVolatilityFormula.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "index",
        "dayCount",
        "betaCurve",
        "shiftCurve",
        "interpolator",
        "extrapolatorLeft",
        "extrapolatorRight",
        "sabrVolatilityFormula");

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
        case 1607020767:  // betaCurve
          return betaCurve;
        case 1908090253:  // shiftCurve
          return shiftCurve;
        case 2096253127:  // interpolator
          return interpolator;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
        case -683564541:  // sabrVolatilityFormula
          return sabrVolatilityFormula;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SabrIborCapletFloorletBootstrapDefinition.Builder builder() {
      return new SabrIborCapletFloorletBootstrapDefinition.Builder();
    }

    @Override
    public Class<? extends SabrIborCapletFloorletBootstrapDefinition> beanType() {
      return SabrIborCapletFloorletBootstrapDefinition.class;
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
     * The meta-property for the {@code betaCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> betaCurve() {
      return betaCurve;
    }

    /**
     * The meta-property for the {@code shiftCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> shiftCurve() {
      return shiftCurve;
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

    /**
     * The meta-property for the {@code sabrVolatilityFormula} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SabrVolatilityFormula> sabrVolatilityFormula() {
      return sabrVolatilityFormula;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getName();
        case 100346066:  // index
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getIndex();
        case 1905311443:  // dayCount
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getDayCount();
        case 1607020767:  // betaCurve
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).betaCurve;
        case 1908090253:  // shiftCurve
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getShiftCurve();
        case 2096253127:  // interpolator
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getInterpolator();
        case 1271703994:  // extrapolatorLeft
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getExtrapolatorLeft();
        case 773779145:  // extrapolatorRight
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getExtrapolatorRight();
        case -683564541:  // sabrVolatilityFormula
          return ((SabrIborCapletFloorletBootstrapDefinition) bean).getSabrVolatilityFormula();
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
   * The bean-builder for {@code SabrIborCapletFloorletBootstrapDefinition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SabrIborCapletFloorletBootstrapDefinition> {

    private IborCapletFloorletVolatilitiesName name;
    private IborIndex index;
    private DayCount dayCount;
    private Curve betaCurve;
    private Curve shiftCurve;
    private CurveInterpolator interpolator;
    private CurveExtrapolator extrapolatorLeft;
    private CurveExtrapolator extrapolatorRight;
    private SabrVolatilityFormula sabrVolatilityFormula;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SabrIborCapletFloorletBootstrapDefinition beanToCopy) {
      this.name = beanToCopy.getName();
      this.index = beanToCopy.getIndex();
      this.dayCount = beanToCopy.getDayCount();
      this.betaCurve = beanToCopy.betaCurve;
      this.shiftCurve = beanToCopy.getShiftCurve();
      this.interpolator = beanToCopy.getInterpolator();
      this.extrapolatorLeft = beanToCopy.getExtrapolatorLeft();
      this.extrapolatorRight = beanToCopy.getExtrapolatorRight();
      this.sabrVolatilityFormula = beanToCopy.getSabrVolatilityFormula();
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
        case 1607020767:  // betaCurve
          return betaCurve;
        case 1908090253:  // shiftCurve
          return shiftCurve;
        case 2096253127:  // interpolator
          return interpolator;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
        case -683564541:  // sabrVolatilityFormula
          return sabrVolatilityFormula;
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
        case 1607020767:  // betaCurve
          this.betaCurve = (Curve) newValue;
          break;
        case 1908090253:  // shiftCurve
          this.shiftCurve = (Curve) newValue;
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
        case -683564541:  // sabrVolatilityFormula
          this.sabrVolatilityFormula = (SabrVolatilityFormula) newValue;
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
    public SabrIborCapletFloorletBootstrapDefinition build() {
      preBuild(this);
      return new SabrIborCapletFloorletBootstrapDefinition(
          name,
          index,
          dayCount,
          betaCurve,
          shiftCurve,
          interpolator,
          extrapolatorLeft,
          extrapolatorRight,
          sabrVolatilityFormula);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name of the volatilities.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(IborCapletFloorletVolatilitiesName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the Ibor index for which the data is valid.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the day count to use.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the beta (elasticity) curve.
     * <p>
     * This represents the beta parameter of SABR model.
     * The x value of the curve is the expiry.
     * <p>
     * The beta will be treated as one of the calibration parameters if this field is not specified.
     * @param betaCurve  the new value
     * @return this, for chaining, not null
     */
    public Builder betaCurve(Curve betaCurve) {
      this.betaCurve = betaCurve;
      return this;
    }

    /**
     * Sets the shift curve.
     * <p>
     * This represents the shift parameter of shifted SABR model.
     * The x value of the curve is the expiry.
     * <p>
     * The shift is set to be zero if this field is not specified.
     * @param shiftCurve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder shiftCurve(Curve shiftCurve) {
      JodaBeanUtils.notNull(shiftCurve, "shiftCurve");
      this.shiftCurve = shiftCurve;
      return this;
    }

    /**
     * Sets the interpolator for the SABR parameters.
     * @param interpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolator(CurveInterpolator interpolator) {
      JodaBeanUtils.notNull(interpolator, "interpolator");
      this.interpolator = interpolator;
      return this;
    }

    /**
     * Sets the left extrapolator for the SABR parameters.
     * <p>
     * The flat extrapolation is used if not specified.
     * @param extrapolatorLeft  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder extrapolatorLeft(CurveExtrapolator extrapolatorLeft) {
      JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
      this.extrapolatorLeft = extrapolatorLeft;
      return this;
    }

    /**
     * Sets the right extrapolator for the SABR parameters.
     * <p>
     * The flat extrapolation is used if not specified.
     * @param extrapolatorRight  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder extrapolatorRight(CurveExtrapolator extrapolatorRight) {
      JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
      this.extrapolatorRight = extrapolatorRight;
      return this;
    }

    /**
     * Sets the SABR formula.
     * @param sabrVolatilityFormula  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sabrVolatilityFormula(SabrVolatilityFormula sabrVolatilityFormula) {
      JodaBeanUtils.notNull(sabrVolatilityFormula, "sabrVolatilityFormula");
      this.sabrVolatilityFormula = sabrVolatilityFormula;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("SabrIborCapletFloorletBootstrapDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("betaCurve").append('=').append(JodaBeanUtils.toString(betaCurve)).append(',').append(' ');
      buf.append("shiftCurve").append('=').append(JodaBeanUtils.toString(shiftCurve)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator)).append(',').append(' ');
      buf.append("extrapolatorLeft").append('=').append(JodaBeanUtils.toString(extrapolatorLeft)).append(',').append(' ');
      buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight)).append(',').append(' ');
      buf.append("sabrVolatilityFormula").append('=').append(JodaBeanUtils.toString(sabrVolatilityFormula));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
