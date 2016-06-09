/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceInfoType;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatility for swaptions in the log-normal or Black model.
 * <p>
 * The volatility is represented by a surface on the expiry and swap tenor dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class BlackSwaptionExpiryTenorVolatilities
    implements BlackSwaptionVolatilities, ImmutableBean, Serializable {

  /**
   * The Black volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the swap tenor, as a year fraction rounded to the month.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface surface;
  /** 
   * The valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /** 
   * The swap convention that the surface is calibrated against.
   */
  private final FixedIborSwapConvention convention;  // cached, not a property
  /**
   * The day count convention of the surface.
   */
  private final DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility surface and the date-time for which it is valid.
   * <p>
   * The surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surface must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#BLACK_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link SurfaceInfoType#DAY_COUNT}
   * <li>The swap convention must be set in the additional information using {@link SurfaceInfoType#SWAP_CONVENTION}
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#swaptionBlackExpiryTenor(String, DayCount, FixedIborSwapConvention)}.
   * 
   * @param surface  the implied volatility surface
   * @param valuationDateTime  the valuation date-time
   * @return the volatilities
   */
  public static BlackSwaptionExpiryTenorVolatilities of(
      Surface surface,
      ZonedDateTime valuationDateTime) {

    return new BlackSwaptionExpiryTenorVolatilities(surface, valuationDateTime);
  }

  /**
   * Obtains an instance from the implied volatility surface and the date, time and zone for which it is valid.
   * <p>
   * The surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surface must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#BLACK_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link SurfaceInfoType#DAY_COUNT}
   * <li>The swap convention must be set in the additional information using {@link SurfaceInfoType#SWAP_CONVENTION}
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#swaptionBlackExpiryTenor(String, DayCount, FixedIborSwapConvention)}.
   * 
   * @param surface  the implied volatility surface
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @return the volatilities
   */
  public static BlackSwaptionExpiryTenorVolatilities of(
      Surface surface,
      LocalDate valuationDate,
      LocalTime valuationTime,
      ZoneId valuationZone) {

    return of(surface, valuationDate.atTime(valuationTime).atZone(valuationZone));
  }

  @ImmutableConstructor
  private BlackSwaptionExpiryTenorVolatilities(
      Surface surface,
      ZonedDateTime valuationDateTime) {

    ArgChecker.notNull(surface, "surface");
    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    surface.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Black volatilities");
    surface.getMetadata().getYValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect y-value type for Black volatilities");
    surface.getMetadata().getZValueType().checkEquals(
        ValueType.BLACK_VOLATILITY, "Incorrect z-value type for Black volatilities");
    FixedIborSwapConvention swapConvention = surface.getMetadata().findInfo(SurfaceInfoType.SWAP_CONVENTION)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing swap convention"));
    DayCount dayCount = surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));

    this.valuationDateTime = valuationDateTime;
    this.surface = surface;
    this.convention = swapConvention;
    this.dayCount = dayCount;
  }

  //-------------------------------------------------------------------------
  @Override
  public SwaptionVolatilitiesName getName() {
    return SwaptionVolatilitiesName.of(surface.getName().getName());
  }

  @Override
  public FixedIborSwapConvention getConvention() {
    return convention;
  }

  @Override
  public int getParameterCount() {
    return surface.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return surface.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return surface.getParameterMetadata(parameterIndex);
  }

  @Override
  public BlackSwaptionExpiryTenorVolatilities withParameter(int parameterIndex, double newValue) {
    return new BlackSwaptionExpiryTenorVolatilities(surface.withParameter(parameterIndex, newValue), valuationDateTime);
  }

  @Override
  public BlackSwaptionExpiryTenorVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new BlackSwaptionExpiryTenorVolatilities(surface.withPerturbation(perturbation), valuationDateTime);
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double tenor, double strike, double forwardRate) {
    return surface.zValue(expiry, tenor);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof SwaptionSensitivity) {
        SwaptionSensitivity pt = (SwaptionSensitivity) point;
        sens = sens.combinedWith(parameterSensitivity(pt));
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(SwaptionSensitivity point) {
    ArgChecker.isTrue(point.getConvention().equals(convention),
        "Swap convention of provider must be the same as swap convention of swaption sensitivity");
    double expiry = relativeTime(point.getExpiry());
    double tenor = point.getTenor();
    UnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, tenor);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, double tenor, PutCall putCall, double strike, double forwardRate, double volatility) {
    return BlackFormulaRepository.price(forwardRate, strike, expiry, volatility, putCall.isCall());
  }

  @Override
  public double priceDelta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.delta(forward, strike, expiry, volatility, putCall.isCall());
  }

  @Override
  public double priceGamma(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.gamma(forward, strike, expiry, volatility);
  }

  @Override
  public double priceTheta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.driftlessTheta(forward, strike, expiry, volatility);
  }

  @Override
  public double priceVega(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.vega(forward, strike, expiry, volatility);
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
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BlackSwaptionExpiryTenorVolatilities}.
   * @return the meta-bean, not null
   */
  public static BlackSwaptionExpiryTenorVolatilities.Meta meta() {
    return BlackSwaptionExpiryTenorVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackSwaptionExpiryTenorVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public BlackSwaptionExpiryTenorVolatilities.Meta metaBean() {
    return BlackSwaptionExpiryTenorVolatilities.Meta.INSTANCE;
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
   * Gets the Black volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the swap tenor, as a year fraction rounded to the month.
   * @return the value of the property, not null
   */
  public Surface getSurface() {
    return surface;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BlackSwaptionExpiryTenorVolatilities other = (BlackSwaptionExpiryTenorVolatilities) obj;
      return JodaBeanUtils.equal(surface, other.surface) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("BlackSwaptionExpiryTenorVolatilities{");
    buf.append("surface").append('=').append(surface).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackSwaptionExpiryTenorVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code surface} property.
     */
    private final MetaProperty<Surface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", BlackSwaptionExpiryTenorVolatilities.class, Surface.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackSwaptionExpiryTenorVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "surface",
        "valuationDateTime");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return surface;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BlackSwaptionExpiryTenorVolatilities> builder() {
      return new BlackSwaptionExpiryTenorVolatilities.Builder();
    }

    @Override
    public Class<? extends BlackSwaptionExpiryTenorVolatilities> beanType() {
      return BlackSwaptionExpiryTenorVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code surface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Surface> surface() {
      return surface;
    }

    /**
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return valuationDateTime;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return ((BlackSwaptionExpiryTenorVolatilities) bean).getSurface();
        case -949589828:  // valuationDateTime
          return ((BlackSwaptionExpiryTenorVolatilities) bean).getValuationDateTime();
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
   * The bean-builder for {@code BlackSwaptionExpiryTenorVolatilities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<BlackSwaptionExpiryTenorVolatilities> {

    private Surface surface;
    private ZonedDateTime valuationDateTime;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return surface;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          this.surface = (Surface) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
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
    public BlackSwaptionExpiryTenorVolatilities build() {
      return new BlackSwaptionExpiryTenorVolatilities(
          surface,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("BlackSwaptionExpiryTenorVolatilities.Builder{");
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
