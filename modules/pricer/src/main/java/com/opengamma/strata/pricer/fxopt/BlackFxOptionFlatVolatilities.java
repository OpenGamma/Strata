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
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Volatility for FX options in the log-normal or Black model based on a curve.
 * <p> 
 * The volatility is represented by a curve on the expiry and the volatility
 * is flat along the strike direction.
 */
@BeanDefinition
public final class BlackFxOptionFlatVolatilities
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
   * The Black volatility curve.
   * <p>
   * The x-values represent the expiry year-fraction.
   * The metadata of the curve must define a day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve curve;
  /**
   * The day count convention of the curve.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an expiry-volatility curve and the date-time for which it is valid.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#BLACK_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link CurveInfoType#DAY_COUNT}
   * </ul>
   * Suitable curve metadata can be created using
   * {@link Curves#blackVolatilityByExpiry(String, DayCount)}.
   * 
   * @param currencyPair  the currency pair
   * @param valuationDateTime  the valuation date-time
   * @param curve  the volatility curve
   * @return the volatilities
   */
  public static BlackFxOptionFlatVolatilities of(
      CurrencyPair currencyPair,
      ZonedDateTime valuationDateTime,
      Curve curve) {

    return new BlackFxOptionFlatVolatilities(currencyPair, valuationDateTime, curve);
  }

  @ImmutableConstructor
  private BlackFxOptionFlatVolatilities(
      CurrencyPair currencyPair,
      ZonedDateTime valuationDateTime,
      Curve curve) {

    ArgChecker.notNull(currencyPair, "currencyPair");
    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    ArgChecker.notNull(curve, "curve");
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Black volatilities");
    curve.getMetadata().getYValueType().checkEquals(
        ValueType.BLACK_VOLATILITY, "Incorrect y-value type for Black volatilities");
    DayCount dayCount = curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.currencyPair = currencyPair;
    this.valuationDateTime = valuationDateTime;
    this.curve = curve;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new BlackFxOptionFlatVolatilities(currencyPair, valuationDateTime, curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxOptionVolatilitiesName getName() {
    return FxOptionVolatilitiesName.of(curve.getName().getName());
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (curve.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(curve));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return curve.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return curve.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return curve.getParameterMetadata(parameterIndex);
  }

  @Override
  public BlackFxOptionFlatVolatilities withParameter(int parameterIndex, double newValue) {
    return new BlackFxOptionFlatVolatilities(
        currencyPair, valuationDateTime, curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public BlackFxOptionFlatVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new BlackFxOptionFlatVolatilities(
        currencyPair, valuationDateTime, curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(CurrencyPair currencyPair, double expiry, double strike, double forward) {
    return curve.yValue(expiry);
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
    UnitParameterSensitivity unitSens = curve.yValueParameterSensitivity(expiry);
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
   * The meta-bean for {@code BlackFxOptionFlatVolatilities}.
   * @return the meta-bean, not null
   */
  public static BlackFxOptionFlatVolatilities.Meta meta() {
    return BlackFxOptionFlatVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackFxOptionFlatVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackFxOptionFlatVolatilities.Builder builder() {
    return new BlackFxOptionFlatVolatilities.Builder();
  }

  @Override
  public BlackFxOptionFlatVolatilities.Meta metaBean() {
    return BlackFxOptionFlatVolatilities.Meta.INSTANCE;
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
   * Gets the Black volatility curve.
   * <p>
   * The x-values represent the expiry year-fraction.
   * The metadata of the curve must define a day count.
   * @return the value of the property, not null
   */
  public Curve getCurve() {
    return curve;
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
      BlackFxOptionFlatVolatilities other = (BlackFxOptionFlatVolatilities) obj;
      return JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(curve, other.curve);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("BlackFxOptionFlatVolatilities{");
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackFxOptionFlatVolatilities}.
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
        this, "currencyPair", BlackFxOptionFlatVolatilities.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackFxOptionFlatVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", BlackFxOptionFlatVolatilities.class, Curve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencyPair",
        "valuationDateTime",
        "curve");

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
        case 95027439:  // curve
          return curve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BlackFxOptionFlatVolatilities.Builder builder() {
      return new BlackFxOptionFlatVolatilities.Builder();
    }

    @Override
    public Class<? extends BlackFxOptionFlatVolatilities> beanType() {
      return BlackFxOptionFlatVolatilities.class;
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
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Curve> curve() {
      return curve;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return ((BlackFxOptionFlatVolatilities) bean).getCurrencyPair();
        case -949589828:  // valuationDateTime
          return ((BlackFxOptionFlatVolatilities) bean).getValuationDateTime();
        case 95027439:  // curve
          return ((BlackFxOptionFlatVolatilities) bean).getCurve();
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
   * The bean-builder for {@code BlackFxOptionFlatVolatilities}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackFxOptionFlatVolatilities> {

    private CurrencyPair currencyPair;
    private ZonedDateTime valuationDateTime;
    private Curve curve;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(BlackFxOptionFlatVolatilities beanToCopy) {
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
      this.curve = beanToCopy.getCurve();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 95027439:  // curve
          return curve;
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
        case 95027439:  // curve
          this.curve = (Curve) newValue;
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
    public BlackFxOptionFlatVolatilities build() {
      return new BlackFxOptionFlatVolatilities(
          currencyPair,
          valuationDateTime,
          curve);
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
     * Sets the Black volatility curve.
     * <p>
     * The x-values represent the expiry year-fraction.
     * The metadata of the curve must define a day count.
     * @param curve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curve(Curve curve) {
      JodaBeanUtils.notNull(curve, "curve");
      this.curve = curve;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("BlackFxOptionFlatVolatilities.Builder{");
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
