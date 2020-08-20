/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
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
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides FX forward rates directly from a curve.
 * <p>
 * This provides historic and forward rates for a single {@link FxIndex}, such as 'GBP/USD-WM'.
 * <p>
 * This implementation is based on an underlying curve that is stored with fixing and direct forward rates.
 */
@BeanDefinition(builderScope = "private")
public final class SimpleFxForwardRates
    implements FxForwardRates, ImmutableBean, Serializable {

  /**
   * The index that the rates are for.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndex index;
  /**
   * The valuation date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The underlying forward curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve curve;
  /**
   * The time-series of fixings, defaulted to an empty time-series.
   * This includes the known historical fixings and may be empty.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDateDoubleTimeSeries fixings;
  /**
   * The day count convention of the curve.
   */
  private final transient DayCount dayCount;  // cached, not a property

  /**
   * Obtains an instance from a curve, with an empty time-series of fixings.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must have x-values of {@linkplain ValueType#YEAR_FRACTION year fractions} with
   * the day count specified. The y-values must be {@linkplain ValueType#FORWARD_RATE forward rates}.
   * A suitable metadata instance for the curve can be created by {@link Curves#forwardRates(String, DayCount)}.
   * 
   * @param index  the index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param curve  the curve of forward rates
   * @return the rates view
   */
  public static SimpleFxForwardRates of(FxIndex index, LocalDate valuationDate, Curve curve) {
    return new SimpleFxForwardRates(index, valuationDate, curve, LocalDateDoubleTimeSeries.empty());
  }

  /**
   * Obtains an instance from a curve and time-series of fixing.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must have x-values of {@linkplain ValueType#YEAR_FRACTION year fractions} with
   * the day count specified. The y-values must be {@linkplain ValueType#FORWARD_RATE forward rates}.
   * 
   * @param index  the index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param curve  the curve of forward rates
   * @param fixings  the time-series of fixings 
   * @return the rates view
   */
  public static SimpleFxForwardRates of(
      FxIndex index,
      LocalDate valuationDate,
      Curve curve,
      LocalDateDoubleTimeSeries fixings) {

    return new SimpleFxForwardRates(index, valuationDate, curve, fixings);
  }

  @ImmutableConstructor
  private SimpleFxForwardRates(
      FxIndex index,
      LocalDate valuationDate,
      Curve curve,
      LocalDateDoubleTimeSeries fixings) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(curve, "curve");
    ArgChecker.notNull(fixings, "fixings");
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for ibor curve");
    curve.getMetadata().getYValueType().checkEquals(
        ValueType.FORWARD_RATE, "Incorrect y-value type for ibor curve");
    DayCount dayCount = curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.valuationDate = valuationDate;
    this.index = index;
    this.curve = curve;
    this.fixings = fixings;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new SimpleFxForwardRates(index, valuationDate, curve, fixings);
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
  public SimpleFxForwardRates withParameter(int parameterIndex, double newValue) {
    return withCurve(curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public SimpleFxForwardRates withPerturbation(ParameterPerturbation perturbation) {
    return withCurve(curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyPair getCurrencyPair() {
    return index.getCurrencyPair();
  }

  @Override
  public double rate(Currency baseCurrency, LocalDate referenceDate) {
    CurrencyPair currencyPair = index.getCurrencyPair();
    ArgChecker.isTrue(currencyPair.contains(baseCurrency), "Currency {} invalid for index {}", baseCurrency, index);
    boolean inverse = baseCurrency.equals(currencyPair.getCounter());
    double forwardRate = !referenceDate.isAfter(valuationDate) ? historicRate(referenceDate) : rateIgnoringFixings(referenceDate);
    return inverse ? 1d / forwardRate : forwardRate;
  }

  // historic rate
  private double historicRate(LocalDate referenceDate) {
    OptionalDouble fixedRate = fixings.get(referenceDate);
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else if (referenceDate.isBefore(getValuationDate())) { // the fixing is required
      if (fixings.isEmpty()) {
        throw new IllegalArgumentException(
            Messages.format("Unable to get fixing for {} on date {}, no time-series supplied", index, referenceDate));
      }
      throw new IllegalArgumentException(Messages.format("Unable to get fixing for {} on date {}", index, referenceDate));
    } else {
      return rateIgnoringFixings(referenceDate);
    }
  }

  // the rate just using the curve
  private double rateIgnoringFixings(LocalDate referenceDate) {
    double relativeYearFraction = relativeYearFraction(referenceDate);
    return curve.yValue(relativeYearFraction);
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder ratePointSensitivity(Currency baseCurrency, LocalDate referenceDate) {
    CurrencyPair currencyPair = index.getCurrencyPair();
    ArgChecker.isTrue(currencyPair.contains(baseCurrency), "Currency {} invalid for index {}", baseCurrency, index);
    if (referenceDate.isBefore(valuationDate) ||
        (referenceDate.equals(valuationDate) && fixings.get(referenceDate).isPresent())) {
      return PointSensitivityBuilder.none();
    }
    return FxForwardSensitivity.of(currencyPair, baseCurrency, referenceDate, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public double rateFxSpotSensitivity(Currency baseCurrency, LocalDate referenceDate) {
    CurrencyPair currencyPair = index.getCurrencyPair();
    ArgChecker.isTrue(currencyPair.contains(baseCurrency), "Currency {} invalid for index {}", baseCurrency, index);
    return 0;
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(FxForwardSensitivity pointSensitivity) {
    LocalDate referenceDate = pointSensitivity.getReferenceDate();
    double relativeYearFraction = relativeYearFraction(referenceDate);
    UnitParameterSensitivity unitSensitivity = curve.yValueParameterSensitivity(relativeYearFraction);
    CurrencyParameterSensitivity sensitivity =
        unitSensitivity.multipliedBy(pointSensitivity.getCurrency(), pointSensitivity.getSensitivity());
    return CurrencyParameterSensitivities.of(sensitivity);
  }

  @Override
  public MultiCurrencyAmount currencyExposure(FxForwardSensitivity pointSensitivity) {
    ArgChecker.isTrue(pointSensitivity.getCurrency().equals(pointSensitivity.getReferenceCurrency()),
        "Currency exposure defined only when sensitivity currency equal reference currency");
    // TODO: this is the logic from DiscountFxForwardRates, but what should go here?
//    Currency ccyRef = pointSensitivity.getReferenceCurrency();
//    CurrencyPair pair = pointSensitivity.getCurrencyPair();
//    double s = pointSensitivity.getSensitivity();
//    LocalDate referenceDate = pointSensitivity.getReferenceDate();
//
//    double f = fxRateProvider.fxRate(pair.getBase(), pair.getCounter());
//    double pA = baseCurrencyDiscountFactors.discountFactor(referenceDate);
//    double pB = counterCurrencyDiscountFactors.discountFactor(referenceDate);
//    if (ccyRef.equals(pair.getBase())) {
//      CurrencyAmount amountCounter = CurrencyAmount.of(pair.getBase(), s * f * pA / pB);
//      CurrencyAmount amountBase = CurrencyAmount.of(pair.getCounter(), -s * f * f * pA / pB);
//      return MultiCurrencyAmount.of(amountBase, amountCounter);
//    } else {
//      CurrencyAmount amountBase = CurrencyAmount.of(pair.getBase(), -s * pB / (pA * f * f));
//      CurrencyAmount amountCounter = CurrencyAmount.of(pair.getCounter(), s * pB / (pA * f));
//      return MultiCurrencyAmount.of(amountBase, amountCounter);
//    }
    return MultiCurrencyAmount.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with a different curve.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public SimpleFxForwardRates withCurve(Curve curve) {
    return new SimpleFxForwardRates(index, valuationDate, curve, fixings);
  }

  // calculate the relative time between the valuation date and the specified date using the day count of the curve
  private double relativeYearFraction(LocalDate date) {
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SimpleFxForwardRates}.
   * @return the meta-bean, not null
   */
  public static SimpleFxForwardRates.Meta meta() {
    return SimpleFxForwardRates.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SimpleFxForwardRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SimpleFxForwardRates.Meta metaBean() {
    return SimpleFxForwardRates.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index that the rates are for.
   * @return the value of the property, not null
   */
  public FxIndex getIndex() {
    return index;
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
   * Gets the underlying forward curve.
   * @return the value of the property, not null
   */
  public Curve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series of fixings, defaulted to an empty time-series.
   * This includes the known historical fixings and may be empty.
   * @return the value of the property, not null
   */
  public LocalDateDoubleTimeSeries getFixings() {
    return fixings;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SimpleFxForwardRates other = (SimpleFxForwardRates) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(curve, other.curve) &&
          JodaBeanUtils.equal(fixings, other.fixings);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixings);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SimpleFxForwardRates{");
    buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
    buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SimpleFxForwardRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", SimpleFxForwardRates.class, FxIndex.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", SimpleFxForwardRates.class, LocalDate.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", SimpleFxForwardRates.class, Curve.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", SimpleFxForwardRates.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "valuationDate",
        "curve",
        "fixings");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 113107279:  // valuationDate
          return valuationDate;
        case 95027439:  // curve
          return curve;
        case -843784602:  // fixings
          return fixings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SimpleFxForwardRates> builder() {
      return new SimpleFxForwardRates.Builder();
    }

    @Override
    public Class<? extends SimpleFxForwardRates> beanType() {
      return SimpleFxForwardRates.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
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

    /**
     * The meta-property for the {@code fixings} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> fixings() {
      return fixings;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((SimpleFxForwardRates) bean).getIndex();
        case 113107279:  // valuationDate
          return ((SimpleFxForwardRates) bean).getValuationDate();
        case 95027439:  // curve
          return ((SimpleFxForwardRates) bean).getCurve();
        case -843784602:  // fixings
          return ((SimpleFxForwardRates) bean).getFixings();
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
   * The bean-builder for {@code SimpleFxForwardRates}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SimpleFxForwardRates> {

    private FxIndex index;
    private LocalDate valuationDate;
    private Curve curve;
    private LocalDateDoubleTimeSeries fixings;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 113107279:  // valuationDate
          return valuationDate;
        case 95027439:  // curve
          return curve;
        case -843784602:  // fixings
          return fixings;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case 95027439:  // curve
          this.curve = (Curve) newValue;
          break;
        case -843784602:  // fixings
          this.fixings = (LocalDateDoubleTimeSeries) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SimpleFxForwardRates build() {
      return new SimpleFxForwardRates(
          index,
          valuationDate,
          curve,
          fixings);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("SimpleFxForwardRates.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
