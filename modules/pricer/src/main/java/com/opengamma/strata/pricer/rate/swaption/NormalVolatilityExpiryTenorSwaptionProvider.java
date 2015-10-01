/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

import com.google.common.primitives.Doubles;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.market.sensitivity.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.SwaptionVolatilitySurfaceExpiryTenorNodeMetadata;

/**
 * Volatility environment for swaptions in the normal or Bachelier model. 
 * The volatility is represented by a surface on the expiration and swap tenor dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class NormalVolatilityExpiryTenorSwaptionProvider
    implements NormalVolatilitySwaptionProvider, ImmutableBean, Serializable {

  /** The normal volatility surface. 
   * <p>
   * The order of the dimensions is expiry/swap tenor 
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
   * Creates a provider from the implied volatility surface and the date-time for which it is valid.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDateTime  the valuation date-time
   * @return the provider
   */
  public static NormalVolatilityExpiryTenorSwaptionProvider of(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      DayCount dayCount,
      ZonedDateTime valuationDateTime) {

    return new NormalVolatilityExpiryTenorSwaptionProvider(surface, convention, dayCount, valuationDateTime);
  }

  /**
   * Creates a provider from the implied volatility surface and the date, time and zone for which it is valid.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @return the provider
   */
  public static NormalVolatilityExpiryTenorSwaptionProvider of(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      DayCount dayCount,
      LocalDate valuationDate,
      LocalTime valuationTime,
      ZoneId valuationZone) {

    return of(surface, convention, dayCount, valuationDate.atTime(valuationTime).atZone(valuationZone));
  }

  /**
   * Creates a provider from the implied volatility surface and the date. 
   * <p>
   * The valuation time and zone are defaulted to noon UTC.
   * 
   * @param surface  the implied volatility surface
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDate  the valuation date
   * @return the provider
   */
  public static NormalVolatilityExpiryTenorSwaptionProvider of(
      NodalSurface surface,
      FixedIborSwapConvention convention,
      DayCount dayCount,
      LocalDate valuationDate) {

    return of(surface, convention, dayCount, valuationDate.atTime(LocalTime.NOON).atZone(ZoneOffset.UTC));
  }

  //-------------------------------------------------------------------------
  @Override
  public double getVolatility(ZonedDateTime expiryDate, double tenor, double strike, double forwardRate) {
    double expiryTime = relativeTime(expiryDate);
    double volatility = surface.zValue(expiryTime, tenor);
    return volatility;
  }

  @Override
  public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(SwaptionSensitivity point) {
    ArgChecker.isTrue(point.getConvention().equals(convention),
        "Swap convention of provider should be the same as swap convention of swaption sensitivity");
    double expiry = relativeTime(point.getExpiry());
    double tenor = point.getTenor();
    Map<DoublesPair, Double> result = surface.zValueParameterSensitivity(expiry, tenor);
    SurfaceCurrencyParameterSensitivity parameterSensi = SurfaceCurrencyParameterSensitivity.of(
        updateSurfaceMetadata(result.keySet()), point.getCurrency(), Doubles.toArray(result.values()));
    return parameterSensi.multipliedBy(point.getSensitivity());
  }

  private SurfaceMetadata updateSurfaceMetadata(Set<DoublesPair> pairs) {
    SurfaceMetadata surfaceMetadata = surface.getMetadata();
    List<SurfaceParameterMetadata> sortedMetaList = new ArrayList<SurfaceParameterMetadata>();
    if (surfaceMetadata.getParameterMetadata().isPresent()) {
      List<SurfaceParameterMetadata> metaList =
          new ArrayList<SurfaceParameterMetadata>(surfaceMetadata.getParameterMetadata().get());
      for (DoublesPair pair : pairs) {
        metadataLoop:
        for (SurfaceParameterMetadata parameterMetadata : metaList) {
          ArgChecker.isTrue(parameterMetadata instanceof SwaptionVolatilitySurfaceExpiryTenorNodeMetadata,
              "surface parameter metadata must be instance of SwaptionVolatilitySurfaceExpiryTenorNodeMetadata");
          SwaptionVolatilitySurfaceExpiryTenorNodeMetadata casted =
              (SwaptionVolatilitySurfaceExpiryTenorNodeMetadata) parameterMetadata;
          if (pair.getFirst() == casted.getYearFraction() && pair.getSecond() == casted.getTenor()) {
            sortedMetaList.add(casted);
            metaList.remove(parameterMetadata);
            break metadataLoop;
          }
        }
      }
      ArgChecker.isTrue(metaList.size() == 0, "mismatch between surface parameter metadata list and doubles pair list");
    } else {
      for (DoublesPair pair : pairs) {
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata parameterMetadata =
            SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(pair.getFirst(), pair.getSecond());
        sortedMetaList.add(parameterMetadata);
      }
    }
    return surfaceMetadata.withParameterMetadata(sortedMetaList);
  }

  @Override
  public double relativeTime(ZonedDateTime dateTime) {
    ArgChecker.notNull(dateTime, "dateTime");
    LocalDate valuationDate = valuationDateTime.toLocalDate();
    LocalDate date = dateTime.toLocalDate();
    boolean timeIsNegative = valuationDate.isAfter(date);
    if (timeIsNegative) {
      return -dayCount.yearFraction(date, valuationDate);
    }
    return dayCount.yearFraction(valuationDate, date);
  }

  @Override
  public double tenor(LocalDate startDate, LocalDate endDate) {
    // rounded number of months. the rounding is to ensure that an integer number of year even with holidays/leap year
    return Math.round((endDate.toEpochDay() - startDate.toEpochDay()) / 365.25 * 12) / 12;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code NormalVolatilityExpiryTenorSwaptionProvider}.
   * @return the meta-bean, not null
   */
  public static NormalVolatilityExpiryTenorSwaptionProvider.Meta meta() {
    return NormalVolatilityExpiryTenorSwaptionProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(NormalVolatilityExpiryTenorSwaptionProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private NormalVolatilityExpiryTenorSwaptionProvider(
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
  public NormalVolatilityExpiryTenorSwaptionProvider.Meta metaBean() {
    return NormalVolatilityExpiryTenorSwaptionProvider.Meta.INSTANCE;
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
   * Gets volatility environment for swaptions in the normal or Bachelier model.
   * The volatility is represented by a surface on the expiration and swap tenor dimensions.
   * /
   * @BeanDefinition(builderScope = "private")
   * public final class NormalVolatilityExpiryTenorSwaptionProvider
   * implements NormalVolatilitySwaptionProvider, ImmutableBean, Serializable {
   * 
   * /** The normal volatility surface.
   * <p>
   * The order of the dimensions is expiry/swap tenor
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
      NormalVolatilityExpiryTenorSwaptionProvider other = (NormalVolatilityExpiryTenorSwaptionProvider) obj;
      return JodaBeanUtils.equal(getSurface(), other.getSurface()) &&
          JodaBeanUtils.equal(getConvention(), other.getConvention()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getValuationDateTime(), other.getValuationDateTime());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSurface());
    hash = hash * 31 + JodaBeanUtils.hashCode(getConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDateTime());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("NormalVolatilityExpiryTenorSwaptionProvider{");
    buf.append("surface").append('=').append(getSurface()).append(',').append(' ');
    buf.append("convention").append('=').append(getConvention()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(getValuationDateTime()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalVolatilityExpiryTenorSwaptionProvider}.
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
        this, "surface", NormalVolatilityExpiryTenorSwaptionProvider.class, NodalSurface.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", NormalVolatilityExpiryTenorSwaptionProvider.class, FixedIborSwapConvention.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", NormalVolatilityExpiryTenorSwaptionProvider.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalVolatilityExpiryTenorSwaptionProvider.class, ZonedDateTime.class);
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
    public BeanBuilder<? extends NormalVolatilityExpiryTenorSwaptionProvider> builder() {
      return new NormalVolatilityExpiryTenorSwaptionProvider.Builder();
    }

    @Override
    public Class<? extends NormalVolatilityExpiryTenorSwaptionProvider> beanType() {
      return NormalVolatilityExpiryTenorSwaptionProvider.class;
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
          return ((NormalVolatilityExpiryTenorSwaptionProvider) bean).getSurface();
        case 2039569265:  // convention
          return ((NormalVolatilityExpiryTenorSwaptionProvider) bean).getConvention();
        case 1905311443:  // dayCount
          return ((NormalVolatilityExpiryTenorSwaptionProvider) bean).getDayCount();
        case -949589828:  // valuationDateTime
          return ((NormalVolatilityExpiryTenorSwaptionProvider) bean).getValuationDateTime();
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
   * The bean-builder for {@code NormalVolatilityExpiryTenorSwaptionProvider}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<NormalVolatilityExpiryTenorSwaptionProvider> {

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
    public NormalVolatilityExpiryTenorSwaptionProvider build() {
      return new NormalVolatilityExpiryTenorSwaptionProvider(
          surface,
          convention,
          dayCount,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("NormalVolatilityExpiryTenorSwaptionProvider.Builder{");
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
