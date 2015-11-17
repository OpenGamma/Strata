/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.FxVolatilitySurfaceYearFractionNodeMetadata;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.value.ValueType;

/**
 * Data provider of volatility for FX options in the lognormal or Black-Scholes model. 
 * <p>
 * The volatility is represented by a surface on the expiry and strike value.
 */
@BeanDefinition
public final class BlackVolatilitySurfaceFxProvider
    implements BlackVolatilityFxProvider, ImmutableBean {

  /**
   * The black volatility surface.
   * <p>
   * The x-values represent the expiry year-fraction.
   * The y-values represent the strike.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface surface;
  /**
   * The currency pair for which the volatility data are presented.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurrencyPair currencyPair;
  /**
   * The day count applicable to the model.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The valuation date-time.
   * All data items in this provider is calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code BlackVolatilitySurfaceFxProvider}.
   * 
   * @param surface  the Black volatility surface
   * @param currencyPair  the currency pair
   * @param dayCount  the day count applicable to the model
   * @param valuationTime  the valuation date-time
   * @return the provider
   */
  public static BlackVolatilitySurfaceFxProvider of(
      NodalSurface surface,
      CurrencyPair currencyPair,
      DayCount dayCount,
      ZonedDateTime valuationTime) {

    return new BlackVolatilitySurfaceFxProvider(surface, currencyPair, dayCount, valuationTime);
  }

  //-------------------------------------------------------------------------
  @Override
  public double getVolatility(CurrencyPair currencyPair, ZonedDateTime expiryDateTime, double strike, double forward) {
    double expiryTime = relativeTime(expiryDateTime);
    if (currencyPair.isInverse(this.currencyPair)) {
      return surface.zValue(expiryTime, 1d / strike);
    }
    return surface.zValue(expiryTime, strike);
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(ZonedDateTime zonedDateTime) {
    ArgChecker.notNull(zonedDateTime, "zonedDateTime");
    LocalDate date = zonedDateTime.toLocalDate(); // TODO: time and zone
    return dayCount.relativeYearFraction(valuationDateTime.toLocalDate(), date);
  }

  @Override
  public SurfaceCurrencyParameterSensitivity surfaceParameterSensitivity(FxOptionSensitivity point) {
    double expiryTime = relativeTime(point.getExpiryDateTime());
    double strike = point.getCurrencyPair().isInverse(currencyPair) ? 1d / point.getStrike() : point.getStrike();
    double pointValue = point.getSensitivity();
    Map<DoublesPair, Double> result = surface.zValueParameterSensitivity(DoublesPair.of(expiryTime, strike));
    List<Double> sensiList = new ArrayList<Double>();
    List<SurfaceParameterMetadata> paramList = new ArrayList<SurfaceParameterMetadata>();
    for (DoublesPair pair : result.keySet()) {
      sensiList.add(result.get(pair) * pointValue);
      SurfaceParameterMetadata parameterMetadata = FxVolatilitySurfaceYearFractionNodeMetadata.of(
          pair.getFirst(), SimpleStrike.of(pair.getSecond()), currencyPair);
      paramList.add(parameterMetadata);
    }
    DefaultSurfaceMetadata metadata = DefaultSurfaceMetadata.builder()
        .dayCount(dayCount)
        .parameterMetadata(paramList)
        .surfaceName(surface.getName())
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.VOLATILITY)
        .build();
    return SurfaceCurrencyParameterSensitivity.of(metadata, point.getCurrency(), DoubleArray.copyOf(sensiList));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BlackVolatilitySurfaceFxProvider}.
   * @return the meta-bean, not null
   */
  public static BlackVolatilitySurfaceFxProvider.Meta meta() {
    return BlackVolatilitySurfaceFxProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackVolatilitySurfaceFxProvider.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackVolatilitySurfaceFxProvider.Builder builder() {
    return new BlackVolatilitySurfaceFxProvider.Builder();
  }

  private BlackVolatilitySurfaceFxProvider(
      NodalSurface surface,
      CurrencyPair currencyPair,
      DayCount dayCount,
      ZonedDateTime valuationDateTime) {
    JodaBeanUtils.notNull(surface, "surface");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    this.surface = surface;
    this.currencyPair = currencyPair;
    this.dayCount = dayCount;
    this.valuationDateTime = valuationDateTime;
  }

  @Override
  public BlackVolatilitySurfaceFxProvider.Meta metaBean() {
    return BlackVolatilitySurfaceFxProvider.Meta.INSTANCE;
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
   * Gets the black volatility surface.
   * <p>
   * The x-values represent the expiry year-fraction.
   * The y-values represent the strike.
   * @return the value of the property, not null
   */
  public NodalSurface getSurface() {
    return surface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency pair for which the volatility data are presented.
   * @return the value of the property, not null
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
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
   * All data items in this provider is calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
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
      BlackVolatilitySurfaceFxProvider other = (BlackVolatilitySurfaceFxProvider) obj;
      return JodaBeanUtils.equal(surface, other.surface) &&
          JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("BlackVolatilitySurfaceFxProvider{");
    buf.append("surface").append('=').append(surface).append(',').append(' ');
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackVolatilitySurfaceFxProvider}.
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
        this, "surface", BlackVolatilitySurfaceFxProvider.class, NodalSurface.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackVolatilitySurfaceFxProvider.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", BlackVolatilitySurfaceFxProvider.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackVolatilitySurfaceFxProvider.class, ZonedDateTime.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "surface",
        "currencyPair",
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
        case 1005147787:  // currencyPair
          return currencyPair;
        case 1905311443:  // dayCount
          return dayCount;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BlackVolatilitySurfaceFxProvider.Builder builder() {
      return new BlackVolatilitySurfaceFxProvider.Builder();
    }

    @Override
    public Class<? extends BlackVolatilitySurfaceFxProvider> beanType() {
      return BlackVolatilitySurfaceFxProvider.class;
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
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
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
          return ((BlackVolatilitySurfaceFxProvider) bean).getSurface();
        case 1005147787:  // currencyPair
          return ((BlackVolatilitySurfaceFxProvider) bean).getCurrencyPair();
        case 1905311443:  // dayCount
          return ((BlackVolatilitySurfaceFxProvider) bean).getDayCount();
        case -949589828:  // valuationDateTime
          return ((BlackVolatilitySurfaceFxProvider) bean).getValuationDateTime();
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
   * The bean-builder for {@code BlackVolatilitySurfaceFxProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackVolatilitySurfaceFxProvider> {

    private NodalSurface surface;
    private CurrencyPair currencyPair;
    private DayCount dayCount;
    private ZonedDateTime valuationDateTime;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(BlackVolatilitySurfaceFxProvider beanToCopy) {
      this.surface = beanToCopy.getSurface();
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.dayCount = beanToCopy.getDayCount();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return surface;
        case 1005147787:  // currencyPair
          return currencyPair;
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
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
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
    public BlackVolatilitySurfaceFxProvider build() {
      return new BlackVolatilitySurfaceFxProvider(
          surface,
          currencyPair,
          dayCount,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the black volatility surface.
     * <p>
     * The x-values represent the expiry year-fraction.
     * The y-values represent the strike.
     * @param surface  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder surface(NodalSurface surface) {
      JodaBeanUtils.notNull(surface, "surface");
      this.surface = surface;
      return this;
    }

    /**
     * Sets the currency pair for which the volatility data are presented.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the day count applicable to the model.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
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

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("BlackVolatilitySurfaceFxProvider.Builder{");
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface)).append(',').append(' ');
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
