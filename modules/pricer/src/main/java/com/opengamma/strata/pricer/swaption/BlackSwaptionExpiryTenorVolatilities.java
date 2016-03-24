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
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceUnitParameterSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatility for swaptions in the log-normal or Black model. 
 * The volatility is represented by a surface on the expiry and swap tenor dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class BlackSwaptionExpiryTenorVolatilities
    implements BlackSwaptionVolatilities, ImmutableBean, Serializable {

  /** 
   * The Black volatility surface. 
   * <p>
   * The order of the dimensions is expiry/swap tenor. 
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface surface;
  /** 
   * The swap convention. 
   * <p>
   * The data must valid in terms of this swap convention. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedIborSwapConvention convention;
  /** 
   * The day count applicable to the model. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /** 
   * The valuation date-time. 
   * <p>
   * All data items in this environment are calibrated for this date-time. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility surface and the date-time for which it is valid.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param valuationDateTime  the valuation date-time
   * @param dayCount  the day count applicable to the model
   * @return the volatilities
   */
  public static BlackSwaptionExpiryTenorVolatilities of(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      DayCount dayCount) {

    return new BlackSwaptionExpiryTenorVolatilities(surface, convention, dayCount, valuationDateTime);
  }

  /**
   * Obtains an instance from the implied volatility surface and the date, time and zone for which it is valid.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @param dayCount  the day count applicable to the model
   * @return the volatilities
   */
  public static BlackSwaptionExpiryTenorVolatilities of(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      LocalDate valuationDate,
      LocalTime valuationTime,
      ZoneId valuationZone,
      DayCount dayCount) {

    return of(surface, convention, valuationDate.atTime(valuationTime).atZone(valuationZone), dayCount);
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double tenor, double strike, double forwardRate) {
    return surface.zValue(expiry, tenor);
  }

  @Override
  public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(SwaptionSensitivity point) {
    ArgChecker.isTrue(point.getConvention().equals(convention),
        "Swap convention of provider must be the same as swap convention of swaption sensitivity");
    double expiry = relativeTime(point.getExpiry());
    double tenor = point.getTenor();
    SurfaceUnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, tenor);
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

  private BlackSwaptionExpiryTenorVolatilities(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      DayCount dayCount,
      ZonedDateTime valuationDateTime) {
    JodaBeanUtils.notNull(surface, "surface");
    JodaBeanUtils.notNull(convention, "convention");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    this.surface = surface;
    this.convention = convention;
    this.dayCount = dayCount;
    this.valuationDateTime = valuationDateTime;
  }

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
   * The order of the dimensions is expiry/swap tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getSurface() {
    return surface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap convention.
   * <p>
   * The data must valid in terms of this swap convention.
   * @return the value of the property, not null
   */
  @Override
  public FixedIborSwapConvention getConvention() {
    return convention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count applicable to the model.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date-time.
   * <p>
   * All data items in this environment are calibrated for this date-time.
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
          JodaBeanUtils.equal(convention, other.convention) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("BlackSwaptionExpiryTenorVolatilities{");
    buf.append("surface").append('=').append(surface).append(',').append(' ');
    buf.append("convention").append('=').append(convention).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
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
    private final MetaProperty<NodalSurface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", BlackSwaptionExpiryTenorVolatilities.class, NodalSurface.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", BlackSwaptionExpiryTenorVolatilities.class, FixedIborSwapConvention.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", BlackSwaptionExpiryTenorVolatilities.class, DayCount.class);
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
        "convention",
        "dayCount",
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
        case 2039569265:  // convention
          return convention;
        case 1905311443:  // dayCount
          return dayCount;
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
    public MetaProperty<NodalSurface> surface() {
      return surface;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedIborSwapConvention> convention() {
      return convention;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
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
        case 2039569265:  // convention
          return ((BlackSwaptionExpiryTenorVolatilities) bean).getConvention();
        case 1905311443:  // dayCount
          return ((BlackSwaptionExpiryTenorVolatilities) bean).getDayCount();
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

    private NodalSurface surface;
    private FixedIborSwapConvention convention;
    private DayCount dayCount;
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
        case 2039569265:  // convention
          return convention;
        case 1905311443:  // dayCount
          return dayCount;
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
          this.surface = (NodalSurface) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FixedIborSwapConvention) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
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
          convention,
          dayCount,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("BlackSwaptionExpiryTenorVolatilities.Builder{");
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
