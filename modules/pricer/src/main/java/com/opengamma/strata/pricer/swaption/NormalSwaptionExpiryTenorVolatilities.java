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
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatility for swaptions in the normal or Bachelier model based on a surface.
 * <p>
 * The volatility is represented by a surface on the expiry and swap tenor dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class NormalSwaptionExpiryTenorVolatilities
    implements NormalSwaptionVolatilities, ImmutableBean, Serializable {

  /**
   * The normal volatility surface. 
   * <p>
   * The x-value of the surface is the expiry.
   * The y-value of the surface is the swap tenor.
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
   * The valuation date-time. 
   * <p>
   * All data items in this environment are calibrated for this date-time. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /** 
   * The day count convention of the surface expiry dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility surface and the date-time for which it is valid.
   * <p>
   * The swap convention and valuation date-time are also specified.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDateTime  the valuation date-time
   * @return the volatilities
   */
  public static NormalSwaptionExpiryTenorVolatilities of(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      DayCount dayCount) {

    return new NormalSwaptionExpiryTenorVolatilities(surface, convention, valuationDateTime, dayCount);
  }

  /**
   * Obtains an instance from the implied volatility surface and the date, time and zone for which it is valid.
   * <p>
   * The swap convention and valuation date-time are also specified.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @return the volatilities
   */
  public static NormalSwaptionExpiryTenorVolatilities of(
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
  public double price(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.price(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceDelta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.delta(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceGamma(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.gamma(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceTheta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.theta(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceVega(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
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
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code NormalSwaptionExpiryTenorVolatilities}.
   * @return the meta-bean, not null
   */
  public static NormalSwaptionExpiryTenorVolatilities.Meta meta() {
    return NormalSwaptionExpiryTenorVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(NormalSwaptionExpiryTenorVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private NormalSwaptionExpiryTenorVolatilities(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      DayCount dayCount) {
    JodaBeanUtils.notNull(surface, "surface");
    JodaBeanUtils.notNull(convention, "convention");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.surface = surface;
    this.convention = convention;
    this.valuationDateTime = valuationDateTime;
    this.dayCount = dayCount;
  }

  @Override
  public NormalSwaptionExpiryTenorVolatilities.Meta metaBean() {
    return NormalSwaptionExpiryTenorVolatilities.Meta.INSTANCE;
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
   * Gets the normal volatility surface.
   * <p>
   * The x-value of the surface is the expiry.
   * The y-value of the surface is the swap tenor.
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
  /**
   * Gets the day count convention of the surface expiry dimension.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      NormalSwaptionExpiryTenorVolatilities other = (NormalSwaptionExpiryTenorVolatilities) obj;
      return JodaBeanUtils.equal(surface, other.surface) &&
          JodaBeanUtils.equal(convention, other.convention) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(dayCount, other.dayCount);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("NormalSwaptionExpiryTenorVolatilities{");
    buf.append("surface").append('=').append(surface).append(',').append(' ');
    buf.append("convention").append('=').append(convention).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalSwaptionExpiryTenorVolatilities}.
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
        this, "surface", NormalSwaptionExpiryTenorVolatilities.class, NodalSurface.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", NormalSwaptionExpiryTenorVolatilities.class, FixedIborSwapConvention.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalSwaptionExpiryTenorVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", NormalSwaptionExpiryTenorVolatilities.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "surface",
        "convention",
        "valuationDateTime",
        "dayCount");

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
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends NormalSwaptionExpiryTenorVolatilities> builder() {
      return new NormalSwaptionExpiryTenorVolatilities.Builder();
    }

    @Override
    public Class<? extends NormalSwaptionExpiryTenorVolatilities> beanType() {
      return NormalSwaptionExpiryTenorVolatilities.class;
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
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return valuationDateTime;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return ((NormalSwaptionExpiryTenorVolatilities) bean).getSurface();
        case 2039569265:  // convention
          return ((NormalSwaptionExpiryTenorVolatilities) bean).getConvention();
        case -949589828:  // valuationDateTime
          return ((NormalSwaptionExpiryTenorVolatilities) bean).getValuationDateTime();
        case 1905311443:  // dayCount
          return ((NormalSwaptionExpiryTenorVolatilities) bean).getDayCount();
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
   * The bean-builder for {@code NormalSwaptionExpiryTenorVolatilities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<NormalSwaptionExpiryTenorVolatilities> {

    private NodalSurface surface;
    private FixedIborSwapConvention convention;
    private ZonedDateTime valuationDateTime;
    private DayCount dayCount;

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
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 1905311443:  // dayCount
          return dayCount;
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
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
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
    public NormalSwaptionExpiryTenorVolatilities build() {
      return new NormalSwaptionExpiryTenorVolatilities(
          surface,
          convention,
          valuationDateTime,
          dayCount);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("NormalSwaptionExpiryTenorVolatilities.Builder{");
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
