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
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Provides access to discount factors for a currency based on a zero rate periodically-compounded curve.
 * <p>
 * This provides discount factors for a single currency.
 * <p>
 * This implementation is based on an underlying curve that is stored with maturities
 * and zero-coupon periodically-compounded rates.
 */
@BeanDefinition(builderScope = "private")
public final class ZeroRatePeriodicDiscountFactors
    implements DiscountFactors, ImmutableBean, Serializable {

  /**
   * Year fraction used as an effective zero.
   */
  private static final double EFFECTIVE_ZERO = 1e-10;

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
   * The number of compounding periods per year of the zero-coupon rate.
   */
  private final transient int frequency;  // cached, not a property
  /**
   * The day count convention of the curve.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a zero-rates curve.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must contain {@linkplain ValueType#YEAR_FRACTION year fractions}
   * against {@linkplain ValueType#ZERO_RATE zero rates}.
   * The day count and compounding periods per year must be present in the metadata.
   * 
   * @param currency  the currency
   * @param valuationDate  the valuation date for which the curve is valid
   * @param underlyingCurve  the underlying curve
   * @return the curve
   */
  public static ZeroRatePeriodicDiscountFactors of(Currency currency, LocalDate valuationDate, Curve underlyingCurve) {
    return new ZeroRatePeriodicDiscountFactors(currency, valuationDate, underlyingCurve);
  }

  @ImmutableConstructor
  private ZeroRatePeriodicDiscountFactors(
      Currency currency,
      LocalDate valuationDate,
      Curve curve) {

    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(curve, "curve");
    Optional<Integer> frequencyOpt = curve.getMetadata().findInfo(CurveInfoType.COMPOUNDING_PER_YEAR);
    ArgChecker.isTrue(frequencyOpt.isPresent(), "Compounding per year must be present for periodicaly compounded curve ");
    ArgChecker.isTrue(frequencyOpt.get() > 0, "Compounding per year must be positive");
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for zero-rate discount curve");
    curve.getMetadata().getYValueType().checkEquals(
        ValueType.ZERO_RATE, "Incorrect y-value type for zero-rate discount curve");
    DayCount dayCount = curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.currency = currency;
    this.valuationDate = valuationDate;
    this.curve = curve;
    this.dayCount = dayCount;
    this.frequency = frequencyOpt.get();
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ZeroRatePeriodicDiscountFactors(currency, valuationDate, curve);
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
  public ZeroRatePeriodicDiscountFactors withParameter(int parameterIndex, double newValue) {
    return withCurve(curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public ZeroRatePeriodicDiscountFactors withPerturbation(ParameterPerturbation perturbation) {
    return withCurve(curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeYearFraction(LocalDate date) {
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  @Override
  public double discountFactor(double relativeYearFraction) {
    if (relativeYearFraction <= EFFECTIVE_ZERO) {
      return 1d;
    }
    // convert zero rate periodically compounded to discount factor
    return Math.pow(1d + curve.yValue(relativeYearFraction) / frequency, -relativeYearFraction * frequency);
  }

  @Override
  public double discountFactorTimeDerivative(double yearFraction) {
    if (yearFraction <= EFFECTIVE_ZERO) {
      return 0d;
    }
    double zr = curve.yValue(yearFraction);
    double periodIF = 1d + zr / frequency;
    double df = Math.pow(periodIF, -yearFraction * frequency);
    return -frequency * df *
        (Math.log(periodIF) + yearFraction / periodIF * curve.firstDerivative(yearFraction) / frequency);
  }

  @Override
  public double zeroRate(double yearFraction) {
    if (yearFraction <= EFFECTIVE_ZERO) {
      return 0d;
    }
    double ratePeriod = curve.yValue(yearFraction);
    return frequency * Math.log(1d + ratePeriod / frequency);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency) {
    double discountFactor = discountFactor(yearFraction);
    if (yearFraction <= EFFECTIVE_ZERO) {
      return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, 0d);
    }
    return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, -discountFactor * yearFraction);
  }

  @Override
  public ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      double yearFraction,
      Currency sensitivityCurrency,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    if (yearFraction < EFFECTIVE_ZERO) {
      return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, 0);
    }
    if (compoundedRateType.equals(CompoundedRateType.CONTINUOUS)) {
      double discountFactor = discountFactorWithSpread(yearFraction, zSpread, compoundedRateType, periodPerYear);
      return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, -discountFactor * yearFraction);
    }
    double df = discountFactor(yearFraction);
    double df2 = Math.pow(df, -1.0 / (yearFraction * periodPerYear));
    double df3 = df2 + zSpread / periodPerYear;
    double ddfSdz = -yearFraction * Math.pow(df3, -yearFraction * periodPerYear - 1) * df2;
    return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, ddfSdz);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(ZeroRateSensitivity pointSens) {
    double yearFraction = pointSens.getYearFraction();
    UnitParameterSensitivity curveSens = curve.yValueParameterSensitivity(yearFraction);
    if (yearFraction <= EFFECTIVE_ZERO) {
      return CurrencyParameterSensitivities.of(curveSens.multipliedBy(pointSens.getCurrency(), 0d));
      // Discount factor in 0 is always 1, no sensitivity.
    }
    double rp = curve.yValue(yearFraction);
    double rcBar = 1.0;
    double rpBar = 1.0 / (1 + rp / frequency) * rcBar;
    UnitParameterSensitivity unitSens = curveSens.multipliedBy(rpBar);
    CurrencyParameterSensitivity curSens = unitSens.multipliedBy(pointSens.getCurrency(), pointSens.getSensitivity());
    return CurrencyParameterSensitivities.of(curSens);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivities.of(curve.createParameterSensitivity(currency, sensitivities));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with a different curve.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public ZeroRatePeriodicDiscountFactors withCurve(Curve curve) {
    return new ZeroRatePeriodicDiscountFactors(currency, valuationDate, curve);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ZeroRatePeriodicDiscountFactors}.
   * @return the meta-bean, not null
   */
  public static ZeroRatePeriodicDiscountFactors.Meta meta() {
    return ZeroRatePeriodicDiscountFactors.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ZeroRatePeriodicDiscountFactors.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public ZeroRatePeriodicDiscountFactors.Meta metaBean() {
    return ZeroRatePeriodicDiscountFactors.Meta.INSTANCE;
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
      ZeroRatePeriodicDiscountFactors other = (ZeroRatePeriodicDiscountFactors) obj;
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
    buf.append("ZeroRatePeriodicDiscountFactors{");
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ZeroRatePeriodicDiscountFactors}.
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
        this, "currency", ZeroRatePeriodicDiscountFactors.class, Currency.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ZeroRatePeriodicDiscountFactors.class, LocalDate.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", ZeroRatePeriodicDiscountFactors.class, Curve.class);
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
    public BeanBuilder<? extends ZeroRatePeriodicDiscountFactors> builder() {
      return new ZeroRatePeriodicDiscountFactors.Builder();
    }

    @Override
    public Class<? extends ZeroRatePeriodicDiscountFactors> beanType() {
      return ZeroRatePeriodicDiscountFactors.class;
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
          return ((ZeroRatePeriodicDiscountFactors) bean).getCurrency();
        case 113107279:  // valuationDate
          return ((ZeroRatePeriodicDiscountFactors) bean).getValuationDate();
        case 95027439:  // curve
          return ((ZeroRatePeriodicDiscountFactors) bean).getCurve();
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
   * The bean-builder for {@code ZeroRatePeriodicDiscountFactors}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<ZeroRatePeriodicDiscountFactors> {

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
    public ZeroRatePeriodicDiscountFactors build() {
      return new ZeroRatePeriodicDiscountFactors(
          currency,
          valuationDate,
          curve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ZeroRatePeriodicDiscountFactors.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
