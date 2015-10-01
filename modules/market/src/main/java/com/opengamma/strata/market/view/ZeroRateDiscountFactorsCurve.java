/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivity;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.CompoundedRateType;

/**
 * Provides access to discount factors for a currency based on a zero rate curve.
 * <p>
 * This provides discount factors for a single currency.
 * <p>
 * This implementation is based on an underlying curve that is stored with maturities
 * and zero-coupon continuously-compounded rates.
 */
@BeanDefinition(builderScope = "private")
public final class ZeroRateDiscountFactorsCurve
    implements DiscountFactors, NodalCurve, ImmutableBean, Serializable {

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
   * The curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveMetadata metadata;
  /**
   * The array of x-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as y-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DoubleArray xValues;
  /**
   * The array of y-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DoubleArray yValues;
  /**
   * The extrapolator for x-values on the left, defaulted to 'Flat".
   * This is used for x-values smaller than the smallest known x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorLeft;
  /**
   * The interpolator.
   * This is used for x-values between the smallest and largest known x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator interpolator;
  /**
   * The extrapolator for x-values on the right, defaulted to 'Flat".
   * This is used for x-values larger than the largest known x-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorRight;
  /**
   * The day count convention of the curve.
   */
  private transient final DayCount dayCount;  // derived and cached, not a property
  /**
   * The bound interpolator.
   */
  private transient final BoundCurveInterpolator boundInterpolator;  // derived and cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a zero-rates curve.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must contain {@linkplain ValueType#YEAR_FRACTION year fractions}
   * against {@linkplain ValueType#ZERO_RATE zero rates}, and the day count must be present.
   * 
   * @param currency  the currency
   * @param valuationDate  the valuation date for which the curve is valid
   * @param underlyingCurve  the underlying curve
   * @return the curve
   */
  public static ZeroRateDiscountFactorsCurve of(
      Currency currency,
      LocalDate valuationDate,
      InterpolatedNodalCurve underlyingCurve) {

    return new ZeroRateDiscountFactorsCurve(
        currency,
        valuationDate,
        underlyingCurve.getMetadata(),
        underlyingCurve.getXValues(),
        underlyingCurve.getYValues(),
        underlyingCurve.getExtrapolatorLeft(),
        underlyingCurve.getInterpolator(),
        underlyingCurve.getExtrapolatorRight());
  }

  @ImmutableConstructor
  private ZeroRateDiscountFactorsCurve(
      Currency currency,
      LocalDate valuationDate,
      CurveMetadata metadata,
      DoubleArray xValues,
      DoubleArray yValues,
      CurveExtrapolator extrapolatorLeft,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorRight) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(metadata, "metadata");
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.notNull(extrapolatorLeft, "extrapolatorLeft");
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notNull(extrapolatorRight, "extrapolatorRight");
    if (xValues.size() < 2) {
      throw new IllegalArgumentException("Length of x-values must be at least 2");
    }
    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException("Length of x-values and y-values must match");
    }
    metadata.getParameterMetadata().ifPresent(params -> {
      if (xValues.size() != params.size()) {
        throw new IllegalArgumentException("Length of x-values and parameter metadata must match when metadata present");
      }
    });
    for (int i = 1; i < xValues.size(); i++) {
      if (xValues.get(i) <= xValues.get(i - 1)) {
        throw new IllegalArgumentException("Array of x-values must be sorted and unique");
      }
    }
    metadata.getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for zero-rate discount curve");
    metadata.getYValueType().checkEquals(
        ValueType.ZERO_RATE, "Incorrect y-value type for zero-rate discount curve");
    if (!metadata.findInfo(CurveInfoType.DAY_COUNT).isPresent()) {
      throw new IllegalArgumentException("Incorrect curve metadata, missing DayCount");
    }
    this.currency = currency;
    this.valuationDate = valuationDate;
    this.metadata = metadata;
    this.xValues = xValues;
    this.yValues = yValues;
    this.dayCount = metadata.getInfo(CurveInfoType.DAY_COUNT);
    this.extrapolatorLeft = extrapolatorLeft;
    this.interpolator = interpolator;
    this.extrapolatorRight = extrapolatorRight;
    this.boundInterpolator = interpolator.bind(xValues, yValues, extrapolatorLeft, extrapolatorRight);
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ZeroRateDiscountFactorsCurve(
        currency, valuationDate, metadata, xValues, yValues, extrapolatorLeft, interpolator, extrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveName getCurveName() {
    return metadata.getCurveName();
  }

  @Override
  public int getParameterCount() {
    return xValues.size();
  }

  //-------------------------------------------------------------------------
  @Override
  public double discountFactor(LocalDate date) {
    double relativeYearFraction = relativeYearFraction(date);
    return discountFactor(relativeYearFraction);
  }

  @Override
  public double discountFactorWithSpread(
      LocalDate date,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    double yearFraction = relativeYearFraction(date);
    if (Math.abs(yearFraction) < EFFECTIVE_ZERO) {
      return 1d;
    }
    double df = discountFactor(date);
    if (compoundedRateType.equals(CompoundedRateType.PERIODIC)) {
      ArgChecker.notNegativeOrZero(periodPerYear, "periodPerYear");
      double ratePeriodicAnnualPlusOne =
          Math.pow(df, -1.0 / periodPerYear / yearFraction) + zSpread / periodPerYear;
      return Math.pow(ratePeriodicAnnualPlusOne, -periodPerYear * yearFraction);
    } else {
      return df * Math.exp(-zSpread * yearFraction);
    }
  }

  // calculates the discount factor at a given time
  private double discountFactor(double relativeYearFraction) {
    // convert zero rate to discount factor
    return Math.exp(-relativeYearFraction * yValue(relativeYearFraction));
  }

  // calculate the relative time between the valuation date and the specified date
  private double relativeYearFraction(LocalDate date) {
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity zeroRatePointSensitivity(LocalDate date, Currency sensitivityCurrency) {
    double relativeYearFraction = relativeYearFraction(date);
    double discountFactor = discountFactor(relativeYearFraction);
    return ZeroRateSensitivity.of(currency, date, sensitivityCurrency, -discountFactor * relativeYearFraction);
  }

  @Override
  public ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      LocalDate date,
      Currency sensitivityCurrency,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    double yearFraction = relativeYearFraction(date);
    ZeroRateSensitivity sensi = zeroRatePointSensitivity(date, sensitivityCurrency);
    if (Math.abs(yearFraction) < EFFECTIVE_ZERO) {
      return sensi;
    }
    double factor;
    if (compoundedRateType.equals(CompoundedRateType.PERIODIC)) {
      double df = discountFactor(date);
      double dfRoot = Math.pow(df, -1d / periodPerYear / yearFraction);
      factor = dfRoot / df / Math.pow(dfRoot + zSpread / periodPerYear, periodPerYear * yearFraction + 1d);
    } else {
      factor = Math.exp(-zSpread * yearFraction);
    }
    return sensi.multipliedBy(factor);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveUnitParameterSensitivities unitParameterSensitivity(LocalDate date) {
    double relativeYearFraction = relativeYearFraction(date);
    return CurveUnitParameterSensitivities.of(yValueParameterSensitivity(relativeYearFraction));
  }

  @Override
  public CurveCurrencyParameterSensitivities curveParameterSensitivity(ZeroRateSensitivity pointSensitivity) {
    CurveUnitParameterSensitivities sens = unitParameterSensitivity(pointSensitivity.getDate());
    return sens.multipliedBy(pointSensitivity.getCurrency(), pointSensitivity.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateDiscountFactorsCurve applyPerturbation(Perturbation<Curve> perturbation) {
    return withCurve(perturbation.applyTo(this));
  }

  /**
   * Returns a new instance with a different curve.
   * 
   * @param curve  the new curve
   * @return the new instance
   */
  public ZeroRateDiscountFactorsCurve withCurve(Curve curve) {
    if (curve instanceof ZeroRateDiscountFactorsCurve) {
      return (ZeroRateDiscountFactorsCurve) curve;
    }
    if (curve instanceof InterpolatedNodalCurve) {
      return ZeroRateDiscountFactorsCurve.of(currency, valuationDate, (InterpolatedNodalCurve) curve);
    }
    throw new UnsupportedOperationException("Unsupported curve type: " + curve.getClass().getName());
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    return boundInterpolator.interpolate(x);
  }

  @Override
  public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
    DoubleArray array = boundInterpolator.parameterSensitivity(x);
    return CurveUnitParameterSensitivity.of(metadata, array);
  }

  @Override
  public double firstDerivative(double x) {
    return boundInterpolator.firstDerivative(x);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateDiscountFactorsCurve withYValues(DoubleArray yValues) {
    return new ZeroRateDiscountFactorsCurve(
        currency, valuationDate, metadata, xValues, yValues, extrapolatorLeft, interpolator, extrapolatorRight);
  }

  @Override
  public InterpolatedNodalCurve shiftedBy(DoubleBinaryOperator operator) {
    return (InterpolatedNodalCurve) NodalCurve.super.shiftedBy(operator);
  }

  @Override
  public InterpolatedNodalCurve shiftedBy(List<ValueAdjustment> adjustments) {
    return (InterpolatedNodalCurve) NodalCurve.super.shiftedBy(adjustments);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new curve with an additional node with no parameter metadata.
   * <p>
   * The result will contain the additional node.
   * The result will have no parameter metadata, even if this curve does.
   * 
   * @param index  the index to insert at
   * @param x  the new x-value
   * @param y  the new y-value
   * @return the updated curve
   */
  public ZeroRateDiscountFactorsCurve withNode(int index, double x, double y) {
    DoubleArray xExtended = xValues.subArray(0, index).concat(new double[] {x}).concat(xValues.subArray(index));
    DoubleArray yExtended = yValues.subArray(0, index).concat(new double[] {y}).concat(yValues.subArray(index));
    CurveMetadata metadata = getMetadata().withParameterMetadata(null);
    return new ZeroRateDiscountFactorsCurve(
        currency, valuationDate, metadata, xExtended, yExtended, extrapolatorLeft, interpolator, extrapolatorRight);
  }

  /**
   * Returns a new curve with an additional node, specifying the parameter metadata.
   * <p>
   * The result will contain the additional node. The result will only contain the
   * specified parameter meta-data if this curve also has parameter meta-data.
   * 
   * @param index  the index to insert at
   * @param paramMetadata  the new parameter metadata
   * @param x  the new x-value
   * @param y  the new y-value
   * @return the updated curve
   */
  public ZeroRateDiscountFactorsCurve withNode(int index, CurveParameterMetadata paramMetadata, double x, double y) {
    DoubleArray xExtended = xValues.subArray(0, index).concat(new double[] {x}).concat(xValues.subArray(index));
    DoubleArray yExtended = yValues.subArray(0, index).concat(new double[] {y}).concat(yValues.subArray(index));
    // add to existing metadata, or do nothing if no existing metadata
    CurveMetadata md = metadata.getParameterMetadata()
        .map(params -> {
          List<CurveParameterMetadata> extended = new ArrayList<>(params);
          extended.add(index, paramMetadata);
          return metadata.withParameterMetadata(extended);
        })
        .orElse(metadata);
    return new ZeroRateDiscountFactorsCurve(
        currency, valuationDate, md, xExtended, yExtended, extrapolatorLeft, interpolator, extrapolatorRight);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ZeroRateDiscountFactorsCurve}.
   * @return the meta-bean, not null
   */
  public static ZeroRateDiscountFactorsCurve.Meta meta() {
    return ZeroRateDiscountFactorsCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ZeroRateDiscountFactorsCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public ZeroRateDiscountFactorsCurve.Meta metaBean() {
    return ZeroRateDiscountFactorsCurve.Meta.INSTANCE;
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
   * Gets the curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   * @return the value of the property, not null
   */
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of x-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as y-values.
   * @return the value of the property, not null
   */
  @Override
  public DoubleArray getXValues() {
    return xValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the array of y-values, one for each point.
   * <p>
   * This array will contains at least two elements and be of the same length as x-values.
   * @return the value of the property, not null
   */
  @Override
  public DoubleArray getYValues() {
    return yValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator for x-values on the left, defaulted to 'Flat".
   * This is used for x-values smaller than the smallest known x-value.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorLeft() {
    return extrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator.
   * This is used for x-values between the smallest and largest known x-value.
   * @return the value of the property, not null
   */
  public CurveInterpolator getInterpolator() {
    return interpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator for x-values on the right, defaulted to 'Flat".
   * This is used for x-values larger than the largest known x-value.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorRight() {
    return extrapolatorRight;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ZeroRateDiscountFactorsCurve other = (ZeroRateDiscountFactorsCurve) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(xValues, other.xValues) &&
          JodaBeanUtils.equal(yValues, other.yValues) &&
          JodaBeanUtils.equal(extrapolatorLeft, other.extrapolatorLeft) &&
          JodaBeanUtils.equal(interpolator, other.interpolator) &&
          JodaBeanUtils.equal(extrapolatorRight, other.extrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValues);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("ZeroRateDiscountFactorsCurve{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("xValues").append('=').append(xValues).append(',').append(' ');
    buf.append("yValues").append('=').append(yValues).append(',').append(' ');
    buf.append("extrapolatorLeft").append('=').append(extrapolatorLeft).append(',').append(' ');
    buf.append("interpolator").append('=').append(interpolator).append(',').append(' ');
    buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ZeroRateDiscountFactorsCurve}.
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
        this, "currency", ZeroRateDiscountFactorsCurve.class, Currency.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ZeroRateDiscountFactorsCurve.class, LocalDate.class);
    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<CurveMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", ZeroRateDiscountFactorsCurve.class, CurveMetadata.class);
    /**
     * The meta-property for the {@code xValues} property.
     */
    private final MetaProperty<DoubleArray> xValues = DirectMetaProperty.ofImmutable(
        this, "xValues", ZeroRateDiscountFactorsCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code yValues} property.
     */
    private final MetaProperty<DoubleArray> yValues = DirectMetaProperty.ofImmutable(
        this, "yValues", ZeroRateDiscountFactorsCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "extrapolatorLeft", ZeroRateDiscountFactorsCurve.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<CurveInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", ZeroRateDiscountFactorsCurve.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code extrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "extrapolatorRight", ZeroRateDiscountFactorsCurve.class, CurveExtrapolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "valuationDate",
        "metadata",
        "xValues",
        "yValues",
        "extrapolatorLeft",
        "interpolator",
        "extrapolatorRight");

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
        case -450004177:  // metadata
          return metadata;
        case 1681280954:  // xValues
          return xValues;
        case -1726182661:  // yValues
          return yValues;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 2096253127:  // interpolator
          return interpolator;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ZeroRateDiscountFactorsCurve> builder() {
      return new ZeroRateDiscountFactorsCurve.Builder();
    }

    @Override
    public Class<? extends ZeroRateDiscountFactorsCurve> beanType() {
      return ZeroRateDiscountFactorsCurve.class;
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
     * The meta-property for the {@code metadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveMetadata> metadata() {
      return metadata;
    }

    /**
     * The meta-property for the {@code xValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> xValues() {
      return xValues;
    }

    /**
     * The meta-property for the {@code yValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> yValues() {
      return yValues;
    }

    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> extrapolatorLeft() {
      return extrapolatorLeft;
    }

    /**
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> interpolator() {
      return interpolator;
    }

    /**
     * The meta-property for the {@code extrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> extrapolatorRight() {
      return extrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((ZeroRateDiscountFactorsCurve) bean).getCurrency();
        case 113107279:  // valuationDate
          return ((ZeroRateDiscountFactorsCurve) bean).getValuationDate();
        case -450004177:  // metadata
          return ((ZeroRateDiscountFactorsCurve) bean).getMetadata();
        case 1681280954:  // xValues
          return ((ZeroRateDiscountFactorsCurve) bean).getXValues();
        case -1726182661:  // yValues
          return ((ZeroRateDiscountFactorsCurve) bean).getYValues();
        case 1271703994:  // extrapolatorLeft
          return ((ZeroRateDiscountFactorsCurve) bean).getExtrapolatorLeft();
        case 2096253127:  // interpolator
          return ((ZeroRateDiscountFactorsCurve) bean).getInterpolator();
        case 773779145:  // extrapolatorRight
          return ((ZeroRateDiscountFactorsCurve) bean).getExtrapolatorRight();
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
   * The bean-builder for {@code ZeroRateDiscountFactorsCurve}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ZeroRateDiscountFactorsCurve> {

    private Currency currency;
    private LocalDate valuationDate;
    private CurveMetadata metadata;
    private DoubleArray xValues;
    private DoubleArray yValues;
    private CurveExtrapolator extrapolatorLeft;
    private CurveInterpolator interpolator;
    private CurveExtrapolator extrapolatorRight;

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
        case -450004177:  // metadata
          return metadata;
        case 1681280954:  // xValues
          return xValues;
        case -1726182661:  // yValues
          return yValues;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 2096253127:  // interpolator
          return interpolator;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
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
        case -450004177:  // metadata
          this.metadata = (CurveMetadata) newValue;
          break;
        case 1681280954:  // xValues
          this.xValues = (DoubleArray) newValue;
          break;
        case -1726182661:  // yValues
          this.yValues = (DoubleArray) newValue;
          break;
        case 1271703994:  // extrapolatorLeft
          this.extrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (CurveInterpolator) newValue;
          break;
        case 773779145:  // extrapolatorRight
          this.extrapolatorRight = (CurveExtrapolator) newValue;
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
    public ZeroRateDiscountFactorsCurve build() {
      return new ZeroRateDiscountFactorsCurve(
          currency,
          valuationDate,
          metadata,
          xValues,
          yValues,
          extrapolatorLeft,
          interpolator,
          extrapolatorRight);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("ZeroRateDiscountFactorsCurve.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("xValues").append('=').append(JodaBeanUtils.toString(xValues)).append(',').append(' ');
      buf.append("yValues").append('=').append(JodaBeanUtils.toString(yValues)).append(',').append(' ');
      buf.append("extrapolatorLeft").append('=').append(JodaBeanUtils.toString(extrapolatorLeft)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator)).append(',').append(' ');
      buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
