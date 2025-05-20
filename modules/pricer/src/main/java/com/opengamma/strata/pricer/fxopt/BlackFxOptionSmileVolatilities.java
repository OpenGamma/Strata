/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Data provider of volatility for FX options in the log-normal or Black-Scholes model.
 * <p>
 * The volatility is represented by a term structure of interpolated smile, 
 * {@link SmileDeltaTermStructure}, which represents expiry dependent smile formed of
 * ATM, risk reversal and strangle as used in FX market.
 */
@BeanDefinition
public final class BlackFxOptionSmileVolatilities
    implements BlackFxOptionVolatilities, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxOptionVolatilitiesName name;
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
   * The volatility model.
   * <p>
   * This represents expiry dependent smile which consists of ATM, risk reversal
   * and strangle as used in FX market.
   */
  @PropertyDefinition(validate = "notNull")
  private final SmileDeltaTermStructure smile;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a smile.
   * 
   * @param name  the name of the volatilities
   * @param currencyPair  the currency pair
   * @param valuationTime  the valuation date-time
   * @param smile  the term structure of smile
   * @return the provider
   */
  public static BlackFxOptionSmileVolatilities of(
      FxOptionVolatilitiesName name,
      CurrencyPair currencyPair,
      ZonedDateTime valuationTime,
      SmileDeltaTermStructure smile) {

    return new BlackFxOptionSmileVolatilities(name, currencyPair, valuationTime, smile);
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (this.name.equals(name)) {
      return Optional.of(name.getMarketDataType().cast(this));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return smile.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return smile.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return smile.getParameterMetadata(parameterIndex);
  }

  @Override
  public BlackFxOptionSmileVolatilities withParameter(int parameterIndex, double newValue) {
    return new BlackFxOptionSmileVolatilities(
        name, currencyPair, valuationDateTime, smile.withParameter(parameterIndex, newValue));
  }

  @Override
  public BlackFxOptionSmileVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new BlackFxOptionSmileVolatilities(
        name, currencyPair, valuationDateTime, smile.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(CurrencyPair currencyPair, double expiryTime, double strike, double forward) {
    if (currencyPair.isInverse(this.currencyPair)) {
      return smile.volatility(expiryTime, 1d / strike, 1d / forward);
    }
    return smile.volatility(expiryTime, strike, forward);
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

  @Override
  public ValueDerivatives firstPartialDerivatives(
      CurrencyPair currencyPair,
      double expiry,
      double strike,
      double forward) {

    if (currencyPair.isInverse(this.currencyPair)) {
      ValueDerivatives valueDerivatives = smile.partialFirstDerivatives(expiry, 1d / strike, 1d / forward);
      return inverseDerivatives(valueDerivatives, strike, forward);
    }
    return smile.partialFirstDerivatives(expiry, strike, forward);
  }

  private ValueDerivatives inverseDerivatives(ValueDerivatives valueDerivatives, double strike, double forward) {
    return ValueDerivatives.of(
        valueDerivatives.getValue(),
        DoubleArray.of(
            valueDerivatives.getDerivatives().get(0),
            -valueDerivatives.getDerivatives().get(1) / (strike * strike),
            -valueDerivatives.getDerivatives().get(2) / (forward * forward)));
  }

  private CurrencyParameterSensitivity parameterSensitivity(FxOptionSensitivity point) {
    double expiryTime = point.getExpiry();
    double strike = currencyPair.isInverse(point.getCurrencyPair()) ? 1d / point.getStrike() : point.getStrike();
    double forward = currencyPair.isInverse(point.getCurrencyPair()) ? 1d / point.getForward() : point.getForward();
    double pointValue = point.getSensitivity();
    DoubleMatrix bucketedSensi = smile.volatilityAndSensitivities(expiryTime, strike, forward).getSensitivities();
    DoubleArray smileExpiries = smile.getExpiries();
    List<Optional<Tenor>> smileExpiryTenors = smile.getExpiryTenors();
    int nTimes = smileExpiries.size();
    List<Double> sensiList = new ArrayList<>();
    List<ParameterMetadata> paramList = new ArrayList<>();
    DoubleArray deltas = smile.getDelta();
    int nDeltas = deltas.size();
    // convert sensitivity
    for (int i = 0; i < nTimes; ++i) {
      double smileExpiry = smileExpiries.get(i);
      Optional<Tenor> tenorOpt = smileExpiryTenors.get(i);
      // calculate absolute delta
      int nDeltasTotal = 2 * nDeltas + 1;
      double[] deltasTotal = new double[nDeltasTotal];
      deltasTotal[nDeltas] = 0.5d;
      for (int j = 0; j < nDeltas; ++j) {
        deltasTotal[j] = 1d - deltas.get(j);
        deltasTotal[2 * nDeltas - j] = deltas.get(j);
      }
      // convert sensitivities
      for (int j = 0; j < nDeltasTotal; ++j) {
        sensiList.add(bucketedSensi.get(i, j) * pointValue);
        DeltaStrike absoluteDelta = DeltaStrike.of(deltasTotal[j]);
        ParameterMetadata parameterMetadata = tenorOpt
            .map(tenor -> FxVolatilitySurfaceYearFractionParameterMetadata.of(smileExpiry, tenor, absoluteDelta, currencyPair))
            .orElseGet(() -> FxVolatilitySurfaceYearFractionParameterMetadata.of(smileExpiry, absoluteDelta, currencyPair));
        paramList.add(parameterMetadata);
      }
    }
    return CurrencyParameterSensitivity.of(name, paramList, point.getCurrency(), DoubleArray.copyOf(sensiList));
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
    return smile.getDayCount().relativeYearFraction(valuationDate, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code BlackFxOptionSmileVolatilities}.
   * @return the meta-bean, not null
   */
  public static BlackFxOptionSmileVolatilities.Meta meta() {
    return BlackFxOptionSmileVolatilities.Meta.INSTANCE;
  }

  static {
    MetaBean.register(BlackFxOptionSmileVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackFxOptionSmileVolatilities.Builder builder() {
    return new BlackFxOptionSmileVolatilities.Builder();
  }

  private BlackFxOptionSmileVolatilities(
      FxOptionVolatilitiesName name,
      CurrencyPair currencyPair,
      ZonedDateTime valuationDateTime,
      SmileDeltaTermStructure smile) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    JodaBeanUtils.notNull(smile, "smile");
    this.name = name;
    this.currencyPair = currencyPair;
    this.valuationDateTime = valuationDateTime;
    this.smile = smile;
  }

  @Override
  public BlackFxOptionSmileVolatilities.Meta metaBean() {
    return BlackFxOptionSmileVolatilities.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  @Override
  public FxOptionVolatilitiesName getName() {
    return name;
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
   * Gets the volatility model.
   * <p>
   * This represents expiry dependent smile which consists of ATM, risk reversal
   * and strangle as used in FX market.
   * @return the value of the property, not null
   */
  public SmileDeltaTermStructure getSmile() {
    return smile;
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
      BlackFxOptionSmileVolatilities other = (BlackFxOptionSmileVolatilities) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(smile, other.smile);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(smile);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("BlackFxOptionSmileVolatilities{");
    buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
    buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
    buf.append("smile").append('=').append(JodaBeanUtils.toString(smile));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackFxOptionSmileVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<FxOptionVolatilitiesName> name = DirectMetaProperty.ofImmutable(
        this, "name", BlackFxOptionSmileVolatilities.class, FxOptionVolatilitiesName.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackFxOptionSmileVolatilities.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackFxOptionSmileVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code smile} property.
     */
    private final MetaProperty<SmileDeltaTermStructure> smile = DirectMetaProperty.ofImmutable(
        this, "smile", BlackFxOptionSmileVolatilities.class, SmileDeltaTermStructure.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currencyPair",
        "valuationDateTime",
        "smile");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1005147787:  // currencyPair
          return currencyPair;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 109556488:  // smile
          return smile;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BlackFxOptionSmileVolatilities.Builder builder() {
      return new BlackFxOptionSmileVolatilities.Builder();
    }

    @Override
    public Class<? extends BlackFxOptionSmileVolatilities> beanType() {
      return BlackFxOptionSmileVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxOptionVolatilitiesName> name() {
      return name;
    }

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
     * The meta-property for the {@code smile} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SmileDeltaTermStructure> smile() {
      return smile;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((BlackFxOptionSmileVolatilities) bean).getName();
        case 1005147787:  // currencyPair
          return ((BlackFxOptionSmileVolatilities) bean).getCurrencyPair();
        case -949589828:  // valuationDateTime
          return ((BlackFxOptionSmileVolatilities) bean).getValuationDateTime();
        case 109556488:  // smile
          return ((BlackFxOptionSmileVolatilities) bean).getSmile();
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
   * The bean-builder for {@code BlackFxOptionSmileVolatilities}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackFxOptionSmileVolatilities> {

    private FxOptionVolatilitiesName name;
    private CurrencyPair currencyPair;
    private ZonedDateTime valuationDateTime;
    private SmileDeltaTermStructure smile;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(BlackFxOptionSmileVolatilities beanToCopy) {
      this.name = beanToCopy.getName();
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
      this.smile = beanToCopy.getSmile();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1005147787:  // currencyPair
          return currencyPair;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 109556488:  // smile
          return smile;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (FxOptionVolatilitiesName) newValue;
          break;
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
          break;
        case 109556488:  // smile
          this.smile = (SmileDeltaTermStructure) newValue;
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
    public BlackFxOptionSmileVolatilities build() {
      return new BlackFxOptionSmileVolatilities(
          name,
          currencyPair,
          valuationDateTime,
          smile);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name of the volatilities.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(FxOptionVolatilitiesName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

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
     * Sets the volatility model.
     * <p>
     * This represents expiry dependent smile which consists of ATM, risk reversal
     * and strangle as used in FX market.
     * @param smile  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder smile(SmileDeltaTermStructure smile) {
      JodaBeanUtils.notNull(smile, "smile");
      this.smile = smile;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("BlackFxOptionSmileVolatilities.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("smile").append('=').append(JodaBeanUtils.toString(smile));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
