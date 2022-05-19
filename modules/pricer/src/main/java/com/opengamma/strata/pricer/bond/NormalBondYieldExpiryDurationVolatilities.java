/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;
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

/**
 * Volatility for swaptions in the normal or Bachelier model based on a surface.
 * <p>
 * The volatility is represented by a surface on the expiry and bond duration dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class NormalBondYieldExpiryDurationVolatilities
    implements BondYieldVolatilities, ImmutableBean, Serializable {

  /**
   * The currency.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /**
   * The normal volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the duration, as a year fraction.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface surface;
  /**
   * The day count convention of the surface.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility surface and the date-time for which it is valid.
   * <p>
   * The surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surface must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#NORMAL_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link SurfaceInfoType#DAY_COUNT}
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#normalVolatilityByExpiryTenor(String, DayCount)}.
   * 
   * @param currency  the currency
   * @param valuationDateTime  the valuation date-time
   * @param surface  the implied volatility surface
   * @return the volatilities
   */
  public static NormalBondYieldExpiryDurationVolatilities of(
      Currency currency,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    return new NormalBondYieldExpiryDurationVolatilities(currency, valuationDateTime, surface);
  }

  @ImmutableConstructor
  private NormalBondYieldExpiryDurationVolatilities(
      Currency currency,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    ArgChecker.notNull(surface, "surface");
    surface.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Normal volatilities");
    surface.getMetadata().getYValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect y-value type for Normal volatilities");
    surface.getMetadata().getZValueType().checkEquals(
        ValueType.NORMAL_VOLATILITY, "Incorrect z-value type for Normal volatilities");
    DayCount dayCount = surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));

    this.currency = ArgChecker.notNull(currency, "currency");
    this.valuationDateTime = valuationDateTime;
    this.surface = surface;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new NormalBondYieldExpiryDurationVolatilities(currency, valuationDateTime, surface);
  }

  @Override
  public ValueType getVolatilityType() {
    return ValueType.NORMAL_VOLATILITY;
  }

  //-------------------------------------------------------------------------
  @Override
  public BondVolatilitiesName getName() {
    return BondVolatilitiesName.of(surface.getName().getName());
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (surface.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(surface));
    }
    return Optional.empty();
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
  public OptionalInt findParameterIndex(ParameterMetadata metadata) {
    return surface.findParameterIndex(metadata);
  }

  @Override
  public NormalBondYieldExpiryDurationVolatilities withParameter(int parameterIndex, double newValue) {
    return new NormalBondYieldExpiryDurationVolatilities(
        currency, valuationDateTime, surface.withParameter(parameterIndex, newValue));
  }

  @Override
  public NormalBondYieldExpiryDurationVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new NormalBondYieldExpiryDurationVolatilities(
        currency, valuationDateTime, surface.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double duration, double strike, double forwardRate) {
    return surface.zValue(expiry, duration);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof BondYieldSensitivity) {
        BondYieldSensitivity pt = (BondYieldSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(BondYieldSensitivity point) {
    double expiry = point.getExpiry();
    double duration = point.getDuration();
    UnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, duration);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(ZonedDateTime dateTime) {
    ArgChecker.notNull(dateTime, "dateTime");
    LocalDate valuationDate = valuationDateTime.toLocalDate();
    LocalDate date = dateTime.toLocalDate();
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code NormalBondYieldExpiryDurationVolatilities}.
   * @return the meta-bean, not null
   */
  public static NormalBondYieldExpiryDurationVolatilities.Meta meta() {
    return NormalBondYieldExpiryDurationVolatilities.Meta.INSTANCE;
  }

  static {
    MetaBean.register(NormalBondYieldExpiryDurationVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public NormalBondYieldExpiryDurationVolatilities.Meta metaBean() {
    return NormalBondYieldExpiryDurationVolatilities.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
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
   * Gets the normal volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the duration, as a year fraction.
   * @return the value of the property, not null
   */
  public Surface getSurface() {
    return surface;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      NormalBondYieldExpiryDurationVolatilities other = (NormalBondYieldExpiryDurationVolatilities) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(surface, other.surface);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("NormalBondYieldExpiryDurationVolatilities{");
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
    buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalBondYieldExpiryDurationVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", NormalBondYieldExpiryDurationVolatilities.class, Currency.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalBondYieldExpiryDurationVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code surface} property.
     */
    private final MetaProperty<Surface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", NormalBondYieldExpiryDurationVolatilities.class, Surface.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "valuationDateTime",
        "surface");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case -1853231955:  // surface
          return surface;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends NormalBondYieldExpiryDurationVolatilities> builder() {
      return new NormalBondYieldExpiryDurationVolatilities.Builder();
    }

    @Override
    public Class<? extends NormalBondYieldExpiryDurationVolatilities> beanType() {
      return NormalBondYieldExpiryDurationVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return valuationDateTime;
    }

    /**
     * The meta-property for the {@code surface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Surface> surface() {
      return surface;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((NormalBondYieldExpiryDurationVolatilities) bean).getCurrency();
        case -949589828:  // valuationDateTime
          return ((NormalBondYieldExpiryDurationVolatilities) bean).getValuationDateTime();
        case -1853231955:  // surface
          return ((NormalBondYieldExpiryDurationVolatilities) bean).getSurface();
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
   * The bean-builder for {@code NormalBondYieldExpiryDurationVolatilities}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<NormalBondYieldExpiryDurationVolatilities> {

    private Currency currency;
    private ZonedDateTime valuationDateTime;
    private Surface surface;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case -1853231955:  // surface
          return surface;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
          break;
        case -1853231955:  // surface
          this.surface = (Surface) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public NormalBondYieldExpiryDurationVolatilities build() {
      return new NormalBondYieldExpiryDurationVolatilities(
          currency,
          valuationDateTime,
          surface);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("NormalBondYieldExpiryDurationVolatilities.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
