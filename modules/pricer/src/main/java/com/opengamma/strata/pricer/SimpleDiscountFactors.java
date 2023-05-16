/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.collect.array.DoubleArray;
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

/**
 * Provides access to discount factors for a currency based on a discount factor curve.
 * <p>
 * This provides discount factors for a single currency.
 * <p>
 * This implementation is based on an underlying curve that is stored with discount factors.
 */
@BeanDefinition(builderScope = "private")
public final class SimpleDiscountFactors
    implements DiscountFactors, ImmutableBean, Serializable {

  /**
   * Year fraction used as an effective zero.
   */
  private static final double EFFECTIVE_ZERO = 1e-8;

  /**
   * The currency that the discount factors are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The valuation date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The underlying curve.
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
   * Obtains an instance based on a discount factor curve.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must contain {@linkplain ValueType#YEAR_FRACTION year fractions}
   * against {@linkplain ValueType#DISCOUNT_FACTOR discount factors}, and the day count must be present.
   * A suitable metadata instance for the curve can be created by {@link Curves#discountFactors(String, DayCount)}.
   * 
   * @param currency  the currency
   * @param valuationDate  the valuation date for which the curve is valid
   * @param underlyingCurve  the underlying curve
   * @return the curve
   */
  public static SimpleDiscountFactors of(Currency currency, LocalDate valuationDate, Curve underlyingCurve) {
    return new SimpleDiscountFactors(currency, valuationDate, underlyingCurve);
  }

  @ImmutableConstructor
  private SimpleDiscountFactors(
      Currency currency,
      LocalDate valuationDate,
      Curve curve) {

    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(curve, "curve");
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for discount curve");
    curve.getMetadata().getYValueType().checkEquals(
        ValueType.DISCOUNT_FACTOR, "Incorrect y-value type for discount curve");
    DayCount dayCount = curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.currency = currency;
    this.valuationDate = valuationDate;
    this.curve = curve;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new SimpleDiscountFactors(currency, valuationDate, curve);
  }

  //-------------------------------------------------------------------------
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
  public OptionalInt findParameterIndex(ParameterMetadata metadata) {
    return curve.findParameterIndex(metadata);
  }

  @Override
  public SimpleDiscountFactors withParameter(int parameterIndex, double newValue) {
    return withCurve(curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public SimpleDiscountFactors withPerturbation(ParameterPerturbation perturbation) {
    return withCurve(curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeYearFraction(LocalDate date) {
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  @Override
  public double discountFactor(double yearFraction) {
    return curve.yValue(yearFraction);
  }

  @Override
  public double discountFactorTimeDerivative(double yearFraction) {
    return curve.firstDerivative(yearFraction);
  }

  @Override
  public double zeroRate(double yearFraction) {
    // zero rate is undefined in general for tiny year fractions.
    double yearFractionMod = modifyYearFraction(yearFraction);
    double discountFactor = discountFactor(yearFractionMod);
    return -Math.log(discountFactor) / yearFractionMod;
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency) {
    // zero rate sensitivity is undefined in general for tiny year fractions.
    double yearFractionMod = modifyYearFraction(yearFraction);
    double discountFactor = discountFactor(yearFractionMod);
    return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, -discountFactor * yearFractionMod);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(ZeroRateSensitivity pointSens) {
    // zero rate sensitivity is undefined in general for tiny year fractions,
    // thus parameter sensitivities will be inaccurate.
    double yearFractionMod = modifyYearFraction(pointSens.getYearFraction());
    double discountFactor = discountFactor(yearFractionMod);
    UnitParameterSensitivity unitSens = curve.yValueParameterSensitivity(yearFractionMod);
    CurrencyParameterSensitivity curSens = unitSens
        .multipliedBy(-1d / (yearFractionMod * discountFactor))
        .multipliedBy(pointSens.getCurrency(), pointSens.getSensitivity());
    return CurrencyParameterSensitivities.of(curSens);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivities.of(curve.createParameterSensitivity(currency, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Override
  public double discountFactorWithSpread(
      double yearFraction,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double df = discountFactor(yearFraction);
    if (compoundedRateType.equals(CompoundedRateType.PERIODIC)) {
      ArgChecker.notNegativeOrZero(periodsPerYear, "periodPerYear");
      double yearFractionMod = modifyYearFraction(yearFraction);
      double ratePeriodicAnnualPlusOne =
          Math.pow(df, -1.0 / periodsPerYear / yearFractionMod) + zSpread / periodsPerYear;
      return Math.pow(ratePeriodicAnnualPlusOne, -periodsPerYear * yearFractionMod);
    } else {
      return df * Math.exp(-zSpread * yearFraction);
    }
  }

  @Override
  public ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      double yearFraction,
      Currency sensitivityCurrency,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    ZeroRateSensitivity sensi = zeroRatePointSensitivity(yearFraction, sensitivityCurrency);
    double factor;
    if (compoundedRateType.equals(CompoundedRateType.PERIODIC)) {
      double yearFractionMod = modifyYearFraction(yearFraction);
      double df = discountFactor(yearFraction);
      double dfRoot = Math.pow(df, -1d / periodsPerYear / yearFractionMod);
      factor = dfRoot / df / Math.pow(dfRoot + zSpread / periodsPerYear, periodsPerYear * yearFractionMod + 1d);
    } else {
      factor = Math.exp(-zSpread * yearFraction);
    }
    return sensi.multipliedBy(factor);
  }

  //-------------------------------------------------------------------------
  private double modifyYearFraction(double yearFraction) {
    if (Math.abs(yearFraction) < EFFECTIVE_ZERO) {
      return yearFraction < 0d ? -EFFECTIVE_ZERO : EFFECTIVE_ZERO;
    }
    return yearFraction;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with a different curve.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public SimpleDiscountFactors withCurve(Curve curve) {
    return new SimpleDiscountFactors(currency, valuationDate, curve);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SimpleDiscountFactors}.
   * @return the meta-bean, not null
   */
  public static SimpleDiscountFactors.Meta meta() {
    return SimpleDiscountFactors.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SimpleDiscountFactors.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SimpleDiscountFactors.Meta metaBean() {
    return SimpleDiscountFactors.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency that the discount factors are for.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying curve.
   * The metadata of the curve must define a day count.
   * @return the value of the property, not null
   */
  public Curve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SimpleDiscountFactors other = (SimpleDiscountFactors) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(curve, other.curve);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SimpleDiscountFactors{");
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SimpleDiscountFactors}.
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
        this, "currency", SimpleDiscountFactors.class, Currency.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", SimpleDiscountFactors.class, LocalDate.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", SimpleDiscountFactors.class, Curve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "valuationDate",
        "curve");

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
        case 113107279:  // valuationDate
          return valuationDate;
        case 95027439:  // curve
          return curve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SimpleDiscountFactors> builder() {
      return new SimpleDiscountFactors.Builder();
    }

    @Override
    public Class<? extends SimpleDiscountFactors> beanType() {
      return SimpleDiscountFactors.class;
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
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
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
        case 575402001:  // currency
          return ((SimpleDiscountFactors) bean).getCurrency();
        case 113107279:  // valuationDate
          return ((SimpleDiscountFactors) bean).getValuationDate();
        case 95027439:  // curve
          return ((SimpleDiscountFactors) bean).getCurve();
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
   * The bean-builder for {@code SimpleDiscountFactors}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SimpleDiscountFactors> {

    private Currency currency;
    private LocalDate valuationDate;
    private Curve curve;

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
        case 113107279:  // valuationDate
          return valuationDate;
        case 95027439:  // curve
          return curve;
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
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
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
    public SimpleDiscountFactors build() {
      return new SimpleDiscountFactors(
          currency,
          valuationDate,
          curve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SimpleDiscountFactors.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
