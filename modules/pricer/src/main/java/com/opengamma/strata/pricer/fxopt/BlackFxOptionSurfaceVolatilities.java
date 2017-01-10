/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.strata.basics.currency.CurrencyPair;
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
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Volatility for FX options in the log-normal or Black model based on a surface.
 * <p> 
 * The volatility is represented by a surface on the expiry and strike value.
 */
@BeanDefinition
public final class BlackFxOptionSurfaceVolatilities
    implements BlackFxOptionVolatilities, ImmutableBean, Serializable {

  /**
   * The currency pair that the volatilities are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurrencyPair currencyPair;
  /**
   * The valuation date-time.
   * All data items in this provider is calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /**
   * The Black volatility surface.
   * <p>
   * The x-values represent the expiry year-fraction.
   * The y-values represent the strike.
   * The metadata of the surface must define a day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface surface;
  /**
   * The day count convention of the curve.
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
   * <li>The y-value type must be {@link ValueType#STRIKE}
   * <li>The z-value type must be {@link ValueType#BLACK_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link SurfaceInfoType#DAY_COUNT}
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#blackVolatilityByExpiryStrike(String, DayCount)}.
   * 
   * @param currencyPair  the currency pair
   * @param valuationDateTime  the valuation date-time
   * @param surface  the volatility surface
   * @return the volatilities
   */
  public static BlackFxOptionSurfaceVolatilities of(
      CurrencyPair currencyPair,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    return new BlackFxOptionSurfaceVolatilities(currencyPair, valuationDateTime, surface);
  }

  @ImmutableConstructor
  private BlackFxOptionSurfaceVolatilities(
      CurrencyPair currencyPair,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    ArgChecker.notNull(currencyPair, "currencyPair");
    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    ArgChecker.notNull(surface, "surface");
    surface.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Black volatilities");
    surface.getMetadata().getYValueType().checkEquals(
        ValueType.STRIKE, "Incorrect y-value type for Black volatilities");
    surface.getMetadata().getZValueType().checkEquals(
        ValueType.BLACK_VOLATILITY, "Incorrect z-value type for Black volatilities");
    DayCount dayCount = surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));

    this.currencyPair = currencyPair;
    this.valuationDateTime = valuationDateTime;
    this.surface = surface;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new BlackFxOptionSurfaceVolatilities(currencyPair, valuationDateTime, surface);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxOptionVolatilitiesName getName() {
    return FxOptionVolatilitiesName.of(surface.getName().getName());
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
  public BlackFxOptionSurfaceVolatilities withParameter(int parameterIndex, double newValue) {
    return new BlackFxOptionSurfaceVolatilities(
        currencyPair, valuationDateTime, surface.withParameter(parameterIndex, newValue));
  }

  @Override
  public BlackFxOptionSurfaceVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new BlackFxOptionSurfaceVolatilities(
        currencyPair, valuationDateTime, surface.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(CurrencyPair currencyPair, double expiry, double strike, double forward) {
    if (currencyPair.isInverse(this.currencyPair)) {
      return surface.zValue(expiry, 1d / strike);
    }
    return surface.zValue(expiry, strike);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof FxOptionSensitivity) {
        FxOptionSensitivity pt = (FxOptionSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(FxOptionSensitivity point) {
    double expiry = point.getExpiry();
    double strike = point.getCurrencyPair().isInverse(currencyPair) ? 1d / point.getStrike() : point.getStrike();
    UnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, strike);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.price(forward, strike, expiry, volatility, putCall.isCall());
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
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BlackFxOptionSurfaceVolatilities}.
   * @return the meta-bean, not null
   */
  public static BlackFxOptionSurfaceVolatilities.Meta meta() {
    return BlackFxOptionSurfaceVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackFxOptionSurfaceVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackFxOptionSurfaceVolatilities.Builder builder() {
    return new BlackFxOptionSurfaceVolatilities.Builder();
  }

  @Override
  public BlackFxOptionSurfaceVolatilities.Meta metaBean() {
    return BlackFxOptionSurfaceVolatilities.Meta.INSTANCE;
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
   * Gets the currency pair that the volatilities are for.
   * @return the value of the property, not null
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date-time.
   * All data items in this provider is calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Black volatility surface.
   * <p>
   * The x-values represent the expiry year-fraction.
   * The y-values represent the strike.
   * The metadata of the surface must define a day count.
   * @return the value of the property, not null
   */
  public Surface getSurface() {
    return surface;
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
      BlackFxOptionSurfaceVolatilities other = (BlackFxOptionSurfaceVolatilities) obj;
      return JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(surface, other.surface);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("BlackFxOptionSurfaceVolatilities{");
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackFxOptionSurfaceVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackFxOptionSurfaceVolatilities.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackFxOptionSurfaceVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code surface} property.
     */
    private final MetaProperty<Surface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", BlackFxOptionSurfaceVolatilities.class, Surface.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencyPair",
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
        case 1005147787:  // currencyPair
          return currencyPair;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case -1853231955:  // surface
          return surface;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BlackFxOptionSurfaceVolatilities.Builder builder() {
      return new BlackFxOptionSurfaceVolatilities.Builder();
    }

    @Override
    public Class<? extends BlackFxOptionSurfaceVolatilities> beanType() {
      return BlackFxOptionSurfaceVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
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
        case 1005147787:  // currencyPair
          return ((BlackFxOptionSurfaceVolatilities) bean).getCurrencyPair();
        case -949589828:  // valuationDateTime
          return ((BlackFxOptionSurfaceVolatilities) bean).getValuationDateTime();
        case -1853231955:  // surface
          return ((BlackFxOptionSurfaceVolatilities) bean).getSurface();
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
   * The bean-builder for {@code BlackFxOptionSurfaceVolatilities}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackFxOptionSurfaceVolatilities> {

    private CurrencyPair currencyPair;
    private ZonedDateTime valuationDateTime;
    private Surface surface;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(BlackFxOptionSurfaceVolatilities beanToCopy) {
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
      this.surface = beanToCopy.getSurface();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
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
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
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
    public BlackFxOptionSurfaceVolatilities build() {
      return new BlackFxOptionSurfaceVolatilities(
          currencyPair,
          valuationDateTime,
          surface);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the currency pair that the volatilities are for.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the valuation date-time.
     * All data items in this provider is calibrated for this date-time.
     * @param valuationDateTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDateTime(ZonedDateTime valuationDateTime) {
      JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
      this.valuationDateTime = valuationDateTime;
      return this;
    }

    /**
     * Sets the Black volatility surface.
     * <p>
     * The x-values represent the expiry year-fraction.
     * The y-values represent the strike.
     * The metadata of the surface must define a day count.
     * @param surface  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder surface(Surface surface) {
      JodaBeanUtils.notNull(surface, "surface");
      this.surface = surface;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("BlackFxOptionSurfaceVolatilities.Builder{");
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
