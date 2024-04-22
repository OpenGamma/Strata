/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;

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

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.cube.Cube;
import com.opengamma.strata.market.cube.CubeInfoType;
import com.opengamma.strata.market.cube.Cubes;
import com.opengamma.strata.market.cube.InterpolatedNodalCube;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.type.FixedFloatSwapConvention;

/**
 * Volatility for swaptions in the normal or Bachelier model based on a cube.
 * <p>
 * The volatility is represented by a cube on the expiry, swap tenor and strike dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class NormalSwaptionExpiryTenorStrikeVolatilities
    implements NormalSwaptionVolatilities, ImmutableBean, Serializable {

  /**
   * The swap convention that the volatilities are to be used for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedFloatSwapConvention convention;
  /**
   * The valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /**
   * The normal volatility cube.
   * <p>
   * The x-value of the cube is the expiry, as a year fraction.
   * The y-value of the cube is the swap tenor, as a year fraction rounded to the month.
   * The z-value of the cube is the strike, as a rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final Cube cube;
  /**
   * The day count convention of the cube.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------

  /**
   * Obtains an instance from the implied volatility cube and the date-time for which it is valid.
   * <p>
   * The cube is specified by an instance of {@link Cube}, such as {@link InterpolatedNodalCube}.
   * The cube must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#STRIKE}
   * <li>The w-value type must be {@link ValueType#NORMAL_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link CubeInfoType#DAY_COUNT}
   * </ul>
   * Suitable cube metadata can be created using
   * {@link Cubes#normalVolatilityByExpiryTenorStrike(String, DayCount)}.
   *
   * @param convention the swap convention that the volatilities are to be used for
   * @param valuationDateTime the valuation date-time
   * @param cube the implied volatility cube
   * @return the volatilities
   */
  public static NormalSwaptionExpiryTenorStrikeVolatilities of(
      FixedFloatSwapConvention convention,
      ZonedDateTime valuationDateTime,
      Cube cube) {

    return new NormalSwaptionExpiryTenorStrikeVolatilities(convention, valuationDateTime, cube);
  }

  @ImmutableConstructor
  private NormalSwaptionExpiryTenorStrikeVolatilities(
      FixedFloatSwapConvention convention,
      ZonedDateTime valuationDateTime,
      Cube cube) {

    ArgChecker.notNull(convention, "convention");
    ArgChecker.notNull(cube, "valuationDateTime");
    ArgChecker.notNull(cube, "cube");
    cube.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Normal volatilities");
    cube.getMetadata().getYValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect y-value type for Normal volatilities");
    cube.getMetadata().getZValueType().checkEquals(
        ValueType.STRIKE, "Incorrect z-value type for Normal volatilities");
    cube.getMetadata().getWValueType().checkEquals(
        ValueType.NORMAL_VOLATILITY, "Incorrect w-value type for Normal volatilities");
    DayCount dayCount = cube.getMetadata().findInfo(CubeInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect cube metadata, missing DayCount"));

    this.valuationDateTime = valuationDateTime;
    this.cube = cube;
    this.convention = convention;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new NormalSwaptionExpiryTenorStrikeVolatilities(convention, valuationDateTime, cube);
  }

  //-------------------------------------------------------------------------
  @Override
  public SwaptionVolatilitiesName getName() {
    return SwaptionVolatilitiesName.of(cube.getName().getName());
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (cube.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(cube));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return cube.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return cube.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return cube.getParameterMetadata(parameterIndex);
  }

  @Override
  public OptionalInt findParameterIndex(ParameterMetadata metadata) {
    return cube.findParameterIndex(metadata);
  }

  @Override
  public NormalSwaptionExpiryTenorStrikeVolatilities withParameter(int parameterIndex, double newValue) {
    return new NormalSwaptionExpiryTenorStrikeVolatilities(
        convention, valuationDateTime, cube.withParameter(parameterIndex, newValue));
  }

  @Override
  public NormalSwaptionExpiryTenorStrikeVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new NormalSwaptionExpiryTenorStrikeVolatilities(
        convention, valuationDateTime, cube.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double tenor, double strike, double forwardRate) {
    return cube.wValue(expiry, tenor, strike);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof SwaptionSensitivity) {
        SwaptionSensitivity pt = (SwaptionSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(SwaptionSensitivity point) {
    double expiry = point.getExpiry();
    double tenor = point.getTenor();
    double strike = point.getStrike();
    UnitParameterSensitivity unitSens = cube.wValueParameterSensitivity(expiry, tenor, strike);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.price(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceDelta(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility) {

    return NormalFormulaRepository.delta(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceGamma(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility) {

    return NormalFormulaRepository.gamma(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceTheta(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility) {

    return NormalFormulaRepository.theta(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceVega(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility) {

    return NormalFormulaRepository.vega(forward, strike, expiry, volatility, putCall);
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(ZonedDateTime dateTime) {
    ArgChecker.notNull(dateTime, "dateTime");
    LocalDate valuationDate = valuationDateTime.toLocalDate();
    LocalDate date = dateTime.toLocalDate();
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  @Override
  public double tenor(LocalDate startDate, LocalDate endDate) {
    // rounded number of months. the rounding is to ensure that an integer number of year even with holidays/leap year
    return Math.round((endDate.toEpochDay() - startDate.toEpochDay()) / 365.25 * 12) / 12;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code NormalSwaptionExpiryTenorStrikeVolatilities}.
   * @return the meta-bean, not null
   */
  public static NormalSwaptionExpiryTenorStrikeVolatilities.Meta meta() {
    return NormalSwaptionExpiryTenorStrikeVolatilities.Meta.INSTANCE;
  }

  static {
    MetaBean.register(NormalSwaptionExpiryTenorStrikeVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public NormalSwaptionExpiryTenorStrikeVolatilities.Meta metaBean() {
    return NormalSwaptionExpiryTenorStrikeVolatilities.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap convention that the volatilities are to be used for.
   * @return the value of the property, not null
   */
  @Override
  public FixedFloatSwapConvention getConvention() {
    return convention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the normal volatility cube.
   * <p>
   * The x-value of the cube is the expiry, as a year fraction.
   * The y-value of the cube is the swap tenor, as a year fraction rounded to the month.
   * The z-value of the cube is the strike, as a rate.
   * @return the value of the property, not null
   */
  public Cube getCube() {
    return cube;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      NormalSwaptionExpiryTenorStrikeVolatilities other = (NormalSwaptionExpiryTenorStrikeVolatilities) obj;
      return JodaBeanUtils.equal(convention, other.convention) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(cube, other.cube);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(cube);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("NormalSwaptionExpiryTenorStrikeVolatilities{");
    buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
    buf.append("cube").append('=').append(JodaBeanUtils.toString(cube));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalSwaptionExpiryTenorStrikeVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedFloatSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", NormalSwaptionExpiryTenorStrikeVolatilities.class, FixedFloatSwapConvention.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalSwaptionExpiryTenorStrikeVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code cube} property.
     */
    private final MetaProperty<Cube> cube = DirectMetaProperty.ofImmutable(
        this, "cube", NormalSwaptionExpiryTenorStrikeVolatilities.class, Cube.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "convention",
        "valuationDateTime",
        "cube");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return convention;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 3064885:  // cube
          return cube;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends NormalSwaptionExpiryTenorStrikeVolatilities> builder() {
      return new NormalSwaptionExpiryTenorStrikeVolatilities.Builder();
    }

    @Override
    public Class<? extends NormalSwaptionExpiryTenorStrikeVolatilities> beanType() {
      return NormalSwaptionExpiryTenorStrikeVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedFloatSwapConvention> convention() {
      return convention;
    }

    /**
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return valuationDateTime;
    }

    /**
     * The meta-property for the {@code cube} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Cube> cube() {
      return cube;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return ((NormalSwaptionExpiryTenorStrikeVolatilities) bean).getConvention();
        case -949589828:  // valuationDateTime
          return ((NormalSwaptionExpiryTenorStrikeVolatilities) bean).getValuationDateTime();
        case 3064885:  // cube
          return ((NormalSwaptionExpiryTenorStrikeVolatilities) bean).getCube();
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
   * The bean-builder for {@code NormalSwaptionExpiryTenorStrikeVolatilities}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<NormalSwaptionExpiryTenorStrikeVolatilities> {

    private FixedFloatSwapConvention convention;
    private ZonedDateTime valuationDateTime;
    private Cube cube;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return convention;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 3064885:  // cube
          return cube;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          this.convention = (FixedFloatSwapConvention) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
          break;
        case 3064885:  // cube
          this.cube = (Cube) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public NormalSwaptionExpiryTenorStrikeVolatilities build() {
      return new NormalSwaptionExpiryTenorStrikeVolatilities(
          convention,
          valuationDateTime,
          cube);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("NormalSwaptionExpiryTenorStrikeVolatilities.Builder{");
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("cube").append('=').append(JodaBeanUtils.toString(cube));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
