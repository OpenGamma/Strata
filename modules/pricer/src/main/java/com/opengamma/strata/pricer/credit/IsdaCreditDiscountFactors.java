/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.market.ValueType.YEAR_FRACTION;
import static com.opengamma.strata.market.ValueType.ZERO_RATE;
import static com.opengamma.strata.market.curve.CurveInfoType.DAY_COUNT;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * ISDA compliant zero rate discount factors.
 * <p>
 * This is used to price credit default swaps under ISDA standard model. 
 * <p>
 * The underlying curve must be zero rate curve represented by {@code InterpolatedNodalCurve} for multiple data points 
 * and {@code InterpolatedNodalCurve} for a single data point. 
 * The zero rates must be interpolated by {@code ProductLinearCurveInterpolator} and extrapolated by 
 * {@code FlatCurveExtrapolator} on the left and {@code ProductLinearCurveExtrapolator} on the right.
 */
@BeanDefinition(builderScope = "private")
public final class IsdaCreditDiscountFactors
    implements CreditDiscountFactors, ImmutableBean, Serializable {

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
   * <p>
   * The metadata of the curve must define a day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve curve;
  /**
   * The day count convention of the curve.
   */
  private final DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from the underlying curve
   * 
   * @param currency  the currency 
   * @param valuationDate  the valuation date
   * @param curve  the underlying curve
   * @return the instance
   */
  public static IsdaCreditDiscountFactors of(Currency currency, LocalDate valuationDate, NodalCurve curve) {

    curve.getMetadata().getXValueType().checkEquals(YEAR_FRACTION, "Incorrect x-value type for zero-rate discount curve");
    curve.getMetadata().getYValueType().checkEquals(ZERO_RATE, "Incorrect y-value type for zero-rate discount curve");
    if (curve instanceof InterpolatedNodalCurve) {
      InterpolatedNodalCurve interpolatedCurve = (InterpolatedNodalCurve) curve;
      ArgChecker.isTrue(interpolatedCurve.getInterpolator().equals(CurveInterpolators.PRODUCT_LINEAR),
          "Interpolator must be PRODUCT_LINEAR");
      ArgChecker.isTrue(interpolatedCurve.getExtrapolatorLeft().equals(CurveExtrapolators.FLAT),
          "Left extrapolator must be FLAT");
      ArgChecker.isTrue(interpolatedCurve.getExtrapolatorRight().equals(CurveExtrapolators.PRODUCT_LINEAR),
          "Right extrapolator must be PRODUCT_LINEAR");
    } else {
      ArgChecker.isTrue(curve instanceof ConstantNodalCurve,
          "the underlying curve must be InterpolatedNodalCurve or ConstantNodalCurve");
    }
    return new IsdaCreditDiscountFactors(currency, valuationDate, curve);
  }

  /**
   * Creates an instance from year fraction and zero rate values.
   * 
   * @param currency  the currency
   * @param valuationDate  the valuation date
   * @param curveName  the curve name
   * @param yearFractions  the year fractions
   * @param zeroRates  the zero rates
   * @param dayCount  the day count
   * @return the instance
   */
  public static IsdaCreditDiscountFactors of(
      Currency currency,
      LocalDate valuationDate,
      CurveName curveName,
      DoubleArray yearFractions,
      DoubleArray zeroRates,
      DayCount dayCount) {

    ArgChecker.notNull(yearFractions, "yearFractions");
    ArgChecker.notNull(zeroRates, "zeroRates");
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .curveName(curveName)
        .dayCount(dayCount)
        .build();
    NodalCurve curve = (yearFractions.size() == 1 && zeroRates.size() == 1) ?
        ConstantNodalCurve.of(metadata, yearFractions.get(0), zeroRates.get(0)) :
        InterpolatedNodalCurve.of(metadata, yearFractions, zeroRates,
            CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);

    return new IsdaCreditDiscountFactors(currency, valuationDate, curve);
  }

  @ImmutableConstructor
  private IsdaCreditDiscountFactors(
      Currency currency,
      LocalDate valuationDate,
      NodalCurve curve) {

    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(curve, "curve");
    DayCount dayCount = curve.getMetadata().findInfo(DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.currency = currency;
    this.valuationDate = valuationDate;
    this.curve = curve;
    this.dayCount = dayCount;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isIsdaCompliant() {
    return true;
  };

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
  public DoubleArray getParameterKeys() {
    return curve.getXValues();
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return curve.getParameterMetadata(parameterIndex);
  }

  @Override
  public IsdaCreditDiscountFactors withParameter(int parameterIndex, double newValue) {
    return withCurve(curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public IsdaCreditDiscountFactors withPerturbation(ParameterPerturbation perturbation) {
    return withCurve(curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  @Override
  public double relativeYearFraction(LocalDate date) {
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  @Override
  public double discountFactor(double yearFraction) {
    // convert zero rate to discount factor
    return Math.exp(-yearFraction * curve.yValue(yearFraction));
  }

  @Override
  public double zeroRate(double yearFraction) {
    return curve.yValue(yearFraction);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency) {
    double discountFactor = discountFactor(yearFraction);
    return ZeroRateSensitivity.of(currency, yearFraction, sensitivityCurrency, -discountFactor * yearFraction);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(ZeroRateSensitivity pointSensitivity) {
    double yearFraction = pointSensitivity.getYearFraction();
    UnitParameterSensitivity unitSens = curve.yValueParameterSensitivity(yearFraction);
    CurrencyParameterSensitivity curSens =
        unitSens.multipliedBy(pointSensitivity.getCurrency(), pointSensitivity.getSensitivity());
    return CurrencyParameterSensitivities.of(curSens);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return CurrencyParameterSensitivities.of(curve.createParameterSensitivity(currency, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors toDiscountFactors() {
    return DiscountFactors.of(currency, valuationDate, curve);
  }

  /**
   * Returns a new instance with a different curve.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public IsdaCreditDiscountFactors withCurve(NodalCurve curve) {
    return IsdaCreditDiscountFactors.of(currency, valuationDate, curve);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IsdaCreditDiscountFactors}.
   * @return the meta-bean, not null
   */
  public static IsdaCreditDiscountFactors.Meta meta() {
    return IsdaCreditDiscountFactors.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IsdaCreditDiscountFactors.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public IsdaCreditDiscountFactors.Meta metaBean() {
    return IsdaCreditDiscountFactors.Meta.INSTANCE;
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
   * <p>
   * The metadata of the curve must define a day count.
   * @return the value of the property, not null
   */
  public NodalCurve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IsdaCreditDiscountFactors other = (IsdaCreditDiscountFactors) obj;
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
    buf.append("IsdaCreditDiscountFactors{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IsdaCreditDiscountFactors}.
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
        this, "currency", IsdaCreditDiscountFactors.class, Currency.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", IsdaCreditDiscountFactors.class, LocalDate.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<NodalCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", IsdaCreditDiscountFactors.class, NodalCurve.class);
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
    public BeanBuilder<? extends IsdaCreditDiscountFactors> builder() {
      return new IsdaCreditDiscountFactors.Builder();
    }

    @Override
    public Class<? extends IsdaCreditDiscountFactors> beanType() {
      return IsdaCreditDiscountFactors.class;
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
    public MetaProperty<NodalCurve> curve() {
      return curve;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((IsdaCreditDiscountFactors) bean).getCurrency();
        case 113107279:  // valuationDate
          return ((IsdaCreditDiscountFactors) bean).getValuationDate();
        case 95027439:  // curve
          return ((IsdaCreditDiscountFactors) bean).getCurve();
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
   * The bean-builder for {@code IsdaCreditDiscountFactors}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<IsdaCreditDiscountFactors> {

    private Currency currency;
    private LocalDate valuationDate;
    private NodalCurve curve;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
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
          this.curve = (NodalCurve) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public IsdaCreditDiscountFactors build() {
      return new IsdaCreditDiscountFactors(
          currency,
          valuationDate,
          curve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IsdaCreditDiscountFactors.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
