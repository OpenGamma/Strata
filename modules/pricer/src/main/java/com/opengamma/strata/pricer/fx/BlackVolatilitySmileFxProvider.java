/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
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

import com.opengamma.analytics.financial.model.volatility.surface.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;

/**
 * Data provider of volatility for FX options in the lognormal or Black-Scholes model. 
 * <p>
 * The volatility is represented by a term structure of interpolated smile, 
 * {@link SmileDeltaTermStructureParametersStrikeInterpolation}, which represents expiration dependent smile formed of
 * ATM, risk reversal and strangle as used in FX market.
 */
@BeanDefinition
public final class BlackVolatilitySmileFxProvider implements BlackVolatilityFxProvider, ImmutableBean {

  /**
   * The volatility model. 
   * <p>
   * This represents expiration dependent smile which consists of ATM, risk reversal and strangle as used in FX market.
   */
  @PropertyDefinition(validate = "notNull")
  private final SmileDeltaTermStructureParametersStrikeInterpolation smile;
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
   * Obtains an {@code BlackVolatilitySmileFxProvider}.
   * 
   * @param smile  the term structure of smile
   * @param currencyPair  the currency pair
   * @param dayCount  the day count applicable to the model
   * @param valuationTime  the valuation date-time
   * @return the provider
   */
  public static BlackVolatilitySmileFxProvider of(
      SmileDeltaTermStructureParametersStrikeInterpolation smile,
      CurrencyPair currencyPair,
      DayCount dayCount,
      ZonedDateTime valuationTime) {

    return new BlackVolatilitySmileFxProvider(smile, currencyPair, dayCount, valuationTime);
  }

  //-------------------------------------------------------------------------
  @Override
  public double getVolatility(CurrencyPair currencyPair, LocalDate expiryDate, double strike, double forward) {
    double expiryTime = relativeTime(expiryDate, null, null); // TODO: time and zone
    if (currencyPair.isInverse(this.currencyPair)) {
      return smile.getVolatility(expiryTime, 1d / strike, 1d / forward);
    }
    return smile.getVolatility(expiryTime, strike, forward);
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date, LocalTime time, ZoneId zone) {
    ArgChecker.notNull(date, "date");
    return dayCount.relativeYearFraction(valuationDateTime.toLocalDate(), date);
  }

  @Override
  public Map<DoublesPair, Double> nodeSensitivity(FxOptionSensitivity point) {
    double expiryTime = relativeTime(point.getExpiryDate(), null, null); // TODO: time and zone
    double strike = currencyPair.isInverse(point.getCurrencyPair()) ? 1d / point.getStrike() : point.getStrike();
    double forward = currencyPair.isInverse(point.getCurrencyPair()) ? 1d / point.getForward() : point.getForward();
    Map<DoublesPair, Double> result = new HashMap<>();
    double[][] bucketedSensi = smile.getVolatilityAndSensitivities(
        expiryTime, strike, forward).getBucketedSensitivities();
    double[] times = smile.getTimeToExpiration();
    int nTimes = times.length;
    for (int i = 0; i < nTimes; ++i) {
      double[] deltas = smile.getVolatilityTerm()[i].getDelta();
      int nDeltas = deltas.length;
      int nDeltasTotal = 2 * nDeltas + 1;
      double[] deltasTotal = new double[nDeltasTotal];
      for (int j = 0; j < nDeltas; ++j) {
        deltasTotal[j] = -deltas[j];
        deltasTotal[2 * nDeltas - j] = deltas[j];
      }
      for (int j = 0; j < nDeltasTotal; ++j) {
        result.put(DoublesPair.of(times[i], deltasTotal[j]), bucketedSensi[i][j]);
      }
    }
    return result;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BlackVolatilitySmileFxProvider}.
   * @return the meta-bean, not null
   */
  public static BlackVolatilitySmileFxProvider.Meta meta() {
    return BlackVolatilitySmileFxProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackVolatilitySmileFxProvider.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackVolatilitySmileFxProvider.Builder builder() {
    return new BlackVolatilitySmileFxProvider.Builder();
  }

  private BlackVolatilitySmileFxProvider(
      SmileDeltaTermStructureParametersStrikeInterpolation smile,
      CurrencyPair currencyPair,
      DayCount dayCount,
      ZonedDateTime valuationDateTime) {
    JodaBeanUtils.notNull(smile, "smile");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    this.smile = smile;
    this.currencyPair = currencyPair;
    this.dayCount = dayCount;
    this.valuationDateTime = valuationDateTime;
  }

  @Override
  public BlackVolatilitySmileFxProvider.Meta metaBean() {
    return BlackVolatilitySmileFxProvider.Meta.INSTANCE;
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
   * Gets the volatility model.
   * <p>
   * This represents expiration dependent smile which consists of ATM, risk reversal and strangle as used in FX market.
   * @return the value of the property, not null
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation getSmile() {
    return smile;
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
      BlackVolatilitySmileFxProvider other = (BlackVolatilitySmileFxProvider) obj;
      return JodaBeanUtils.equal(getSmile(), other.getSmile()) &&
          JodaBeanUtils.equal(getCurrencyPair(), other.getCurrencyPair()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getValuationDateTime(), other.getValuationDateTime());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSmile());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrencyPair());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDateTime());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("BlackVolatilitySmileFxProvider{");
    buf.append("smile").append('=').append(getSmile()).append(',').append(' ');
    buf.append("currencyPair").append('=').append(getCurrencyPair()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(getValuationDateTime()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackVolatilitySmileFxProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code smile} property.
     */
    private final MetaProperty<SmileDeltaTermStructureParametersStrikeInterpolation> smile = DirectMetaProperty.ofImmutable(
        this, "smile", BlackVolatilitySmileFxProvider.class, SmileDeltaTermStructureParametersStrikeInterpolation.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackVolatilitySmileFxProvider.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", BlackVolatilitySmileFxProvider.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackVolatilitySmileFxProvider.class, ZonedDateTime.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "smile",
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
        case 109556488:  // smile
          return smile;
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
    public BlackVolatilitySmileFxProvider.Builder builder() {
      return new BlackVolatilitySmileFxProvider.Builder();
    }

    @Override
    public Class<? extends BlackVolatilitySmileFxProvider> beanType() {
      return BlackVolatilitySmileFxProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code smile} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SmileDeltaTermStructureParametersStrikeInterpolation> smile() {
      return smile;
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
        case 109556488:  // smile
          return ((BlackVolatilitySmileFxProvider) bean).getSmile();
        case 1005147787:  // currencyPair
          return ((BlackVolatilitySmileFxProvider) bean).getCurrencyPair();
        case 1905311443:  // dayCount
          return ((BlackVolatilitySmileFxProvider) bean).getDayCount();
        case -949589828:  // valuationDateTime
          return ((BlackVolatilitySmileFxProvider) bean).getValuationDateTime();
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
   * The bean-builder for {@code BlackVolatilitySmileFxProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackVolatilitySmileFxProvider> {

    private SmileDeltaTermStructureParametersStrikeInterpolation smile;
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
    private Builder(BlackVolatilitySmileFxProvider beanToCopy) {
      this.smile = beanToCopy.getSmile();
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.dayCount = beanToCopy.getDayCount();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 109556488:  // smile
          return smile;
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
        case 109556488:  // smile
          this.smile = (SmileDeltaTermStructureParametersStrikeInterpolation) newValue;
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
    public BlackVolatilitySmileFxProvider build() {
      return new BlackVolatilitySmileFxProvider(
          smile,
          currencyPair,
          dayCount,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code smile} property in the builder.
     * @param smile  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder smile(SmileDeltaTermStructureParametersStrikeInterpolation smile) {
      JodaBeanUtils.notNull(smile, "smile");
      this.smile = smile;
      return this;
    }

    /**
     * Sets the {@code currencyPair} property in the builder.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the {@code valuationDateTime} property in the builder.
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
      buf.append("BlackVolatilitySmileFxProvider.Builder{");
      buf.append("smile").append('=').append(JodaBeanUtils.toString(smile)).append(',').append(' ');
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
