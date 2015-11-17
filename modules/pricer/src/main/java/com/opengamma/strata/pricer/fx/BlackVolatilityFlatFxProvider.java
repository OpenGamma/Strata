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
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivity;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.FxVolatilitySurfaceYearFractionNodeMetadata;

/**
 * Data provider of volatility for FX options in the lognormal or Black-Scholes model.
 * <p>
 * The volatility is represented by a curve on the expiry and the volatility
 * is flat along the strike direction.
 */
@BeanDefinition
public final class BlackVolatilityFlatFxProvider
    implements BlackVolatilityFxProvider, ImmutableBean {

  /**
   * The volatility term structure.
   * <p>
   * The x-values represent the expiry year-fraction.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve curve;
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
   * Obtains a {@code BlackVolatilityFlatFxProvider}.
   * 
   * @param curve  the term structure of volatility
   * @param currencyPair  the currency pair
   * @param dayCount  the day count applicable to the model
   * @param valuationTime  the valuation date-time
   * @return the provider
   */
  public static BlackVolatilityFlatFxProvider of(
      NodalCurve curve,
      CurrencyPair currencyPair,
      DayCount dayCount,
      ZonedDateTime valuationTime) {

    return new BlackVolatilityFlatFxProvider(curve, currencyPair, dayCount, valuationTime);
  }

  //-------------------------------------------------------------------------
  @Override
  public double getVolatility(CurrencyPair currencyPair, ZonedDateTime expiryDateTime, double strike, double forward) {
    double expiryTime = relativeTime(expiryDateTime);
    return curve.yValue(expiryTime);
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
    // transforms curve sensitivity to surface sensitivity
    double expiryTime = relativeTime(point.getExpiryDateTime());
    CurveUnitParameterSensitivity yValueParameterSensitivity = curve.yValueParameterSensitivity(expiryTime);
    DoubleArray times = curve.getXValues();
    double pointValue = point.getSensitivity();
    int paramCount = times.size();
    double[] sensi = new double[paramCount];
    List<SurfaceParameterMetadata> paramList = new ArrayList<SurfaceParameterMetadata>();
    for (int i = 0; i < paramCount; ++i) {
      sensi[i] = yValueParameterSensitivity.getSensitivity().get(i) * pointValue;
      DeltaStrike absoluteDelta = DeltaStrike.of(0.5d); // ATM
      SurfaceParameterMetadata parameterMetadata =
          FxVolatilitySurfaceYearFractionNodeMetadata.of(times.get(i), absoluteDelta, currencyPair);
      paramList.add(parameterMetadata);
    }
    DefaultSurfaceMetadata metadata = DefaultSurfaceMetadata.builder()
        .dayCount(dayCount)
        .parameterMetadata(paramList)
        .surfaceName(SurfaceName.of(curve.getName().toString()))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.VOLATILITY)
        .build();
    return SurfaceCurrencyParameterSensitivity.of(metadata, point.getCurrency(), DoubleArray.copyOf(sensi));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BlackVolatilityFlatFxProvider}.
   * @return the meta-bean, not null
   */
  public static BlackVolatilityFlatFxProvider.Meta meta() {
    return BlackVolatilityFlatFxProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackVolatilityFlatFxProvider.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackVolatilityFlatFxProvider.Builder builder() {
    return new BlackVolatilityFlatFxProvider.Builder();
  }

  private BlackVolatilityFlatFxProvider(
      NodalCurve curve,
      CurrencyPair currencyPair,
      DayCount dayCount,
      ZonedDateTime valuationDateTime) {
    JodaBeanUtils.notNull(curve, "curve");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    this.curve = curve;
    this.currencyPair = currencyPair;
    this.dayCount = dayCount;
    this.valuationDateTime = valuationDateTime;
  }

  @Override
  public BlackVolatilityFlatFxProvider.Meta metaBean() {
    return BlackVolatilityFlatFxProvider.Meta.INSTANCE;
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
   * Gets the volatility term structure.
   * <p>
   * The x-values represent the expiry year-fraction.
   * @return the value of the property, not null
   */
  public NodalCurve getCurve() {
    return curve;
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
      BlackVolatilityFlatFxProvider other = (BlackVolatilityFlatFxProvider) obj;
      return JodaBeanUtils.equal(curve, other.curve) &&
          JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("BlackVolatilityFlatFxProvider{");
    buf.append("curve").append('=').append(curve).append(',').append(' ');
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackVolatilityFlatFxProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<NodalCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", BlackVolatilityFlatFxProvider.class, NodalCurve.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackVolatilityFlatFxProvider.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", BlackVolatilityFlatFxProvider.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackVolatilityFlatFxProvider.class, ZonedDateTime.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curve",
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
        case 95027439:  // curve
          return curve;
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
    public BlackVolatilityFlatFxProvider.Builder builder() {
      return new BlackVolatilityFlatFxProvider.Builder();
    }

    @Override
    public Class<? extends BlackVolatilityFlatFxProvider> beanType() {
      return BlackVolatilityFlatFxProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalCurve> curve() {
      return curve;
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
        case 95027439:  // curve
          return ((BlackVolatilityFlatFxProvider) bean).getCurve();
        case 1005147787:  // currencyPair
          return ((BlackVolatilityFlatFxProvider) bean).getCurrencyPair();
        case 1905311443:  // dayCount
          return ((BlackVolatilityFlatFxProvider) bean).getDayCount();
        case -949589828:  // valuationDateTime
          return ((BlackVolatilityFlatFxProvider) bean).getValuationDateTime();
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
   * The bean-builder for {@code BlackVolatilityFlatFxProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackVolatilityFlatFxProvider> {

    private NodalCurve curve;
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
    private Builder(BlackVolatilityFlatFxProvider beanToCopy) {
      this.curve = beanToCopy.getCurve();
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.dayCount = beanToCopy.getDayCount();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95027439:  // curve
          return curve;
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
        case 95027439:  // curve
          this.curve = (NodalCurve) newValue;
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
    public BlackVolatilityFlatFxProvider build() {
      return new BlackVolatilityFlatFxProvider(
          curve,
          currencyPair,
          dayCount,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the volatility term structure.
     * <p>
     * The x-values represent the expiry year-fraction.
     * @param curve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curve(NodalCurve curve) {
      JodaBeanUtils.notNull(curve, "curve");
      this.curve = curve;
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
      buf.append("BlackVolatilityFlatFxProvider.Builder{");
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
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
