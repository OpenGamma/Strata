/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

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

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
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
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Volatility for Ibor caplet/floorlet in the normal or Bachelier model based on a curve.
 * <p>
 * The volatility is represented by a curve on the expiry dimension.
 */
@BeanDefinition(builderScope = "private")
public final class NormalIborCapletFloorletExpiryFlatVolatilities
    implements NormalIborCapletFloorletVolatilities, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The data must valid in terms of this Ibor index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;
  /**
   * The valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /**
   * The normal volatility curve.
   * <p>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#NORMAL_VOLATILITY}
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve curve;
  /**
   * The day count convention of the curve.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility curve and the date-time for which it is valid.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#NORMAL_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link CurveInfoType#DAY_COUNT}
   * </ul>
   * 
   * @param index  the Ibor index for which the data is valid
   * @param valuationDateTime  the valuation date-time
   * @param curve  the implied volatility curve
   * @return the volatilities
   */
  public static NormalIborCapletFloorletExpiryFlatVolatilities of(
      IborIndex index,
      ZonedDateTime valuationDateTime,
      Curve curve) {

    return new NormalIborCapletFloorletExpiryFlatVolatilities(index, valuationDateTime, curve);
  }

  @ImmutableConstructor
  private NormalIborCapletFloorletExpiryFlatVolatilities(
      IborIndex index,
      ZonedDateTime valuationDateTime,
      Curve curve) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    ArgChecker.notNull(curve, "curve");
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Normal volatilities");
    curve.getMetadata().getYValueType().checkEquals(
        ValueType.NORMAL_VOLATILITY, "Incorrect y-value type for Normal volatilities");
    DayCount dayCount = curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.index = index;
    this.valuationDateTime = valuationDateTime;
    this.curve = curve;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new NormalIborCapletFloorletExpiryFlatVolatilities(index, valuationDateTime, curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborCapletFloorletVolatilitiesName getName() {
    return IborCapletFloorletVolatilitiesName.of(curve.getName().getName());
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
  public NormalIborCapletFloorletExpiryFlatVolatilities withParameter(int parameterIndex, double newValue) {
    return new NormalIborCapletFloorletExpiryFlatVolatilities(
        index, valuationDateTime, curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public NormalIborCapletFloorletExpiryFlatVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new NormalIborCapletFloorletExpiryFlatVolatilities(
        index, valuationDateTime, curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double strike, double forward) {
    return curve.yValue(expiry);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof IborCapletFloorletSensitivity) {
        IborCapletFloorletSensitivity pt = (IborCapletFloorletSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(IborCapletFloorletSensitivity point) {
    double expiry = point.getExpiry();
    UnitParameterSensitivity unitSens = curve.yValueParameterSensitivity(expiry);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.price(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceDelta(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.delta(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceGamma(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.gamma(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceTheta(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.theta(forward, strike, expiry, volatility, putCall);
  }

  @Override
  public double priceVega(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return NormalFormulaRepository.vega(forward, strike, expiry, volatility, putCall);
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
  /**
   * The meta-bean for {@code NormalIborCapletFloorletExpiryFlatVolatilities}.
   * @return the meta-bean, not null
   */
  public static NormalIborCapletFloorletExpiryFlatVolatilities.Meta meta() {
    return NormalIborCapletFloorletExpiryFlatVolatilities.Meta.INSTANCE;
  }

  static {
    MetaBean.register(NormalIborCapletFloorletExpiryFlatVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public NormalIborCapletFloorletExpiryFlatVolatilities.Meta metaBean() {
    return NormalIborCapletFloorletExpiryFlatVolatilities.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * <p>
   * The data must valid in terms of this Ibor index.
   * @return the value of the property, not null
   */
  @Override
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the normal volatility curve.
   * <p>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#NORMAL_VOLATILITY}
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
      NormalIborCapletFloorletExpiryFlatVolatilities other = (NormalIborCapletFloorletExpiryFlatVolatilities) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(curve, other.curve);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("NormalIborCapletFloorletExpiryFlatVolatilities{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalIborCapletFloorletExpiryFlatVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", NormalIborCapletFloorletExpiryFlatVolatilities.class, IborIndex.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalIborCapletFloorletExpiryFlatVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<Curve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", NormalIborCapletFloorletExpiryFlatVolatilities.class, Curve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
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
        case 100346066:  // index
          return index;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 95027439:  // curve
          return curve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends NormalIborCapletFloorletExpiryFlatVolatilities> builder() {
      return new NormalIborCapletFloorletExpiryFlatVolatilities.Builder();
    }

    @Override
    public Class<? extends NormalIborCapletFloorletExpiryFlatVolatilities> beanType() {
      return NormalIborCapletFloorletExpiryFlatVolatilities.class;
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
    public MetaProperty<IborIndex> index() {
      return index;
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
        case 100346066:  // index
          return ((NormalIborCapletFloorletExpiryFlatVolatilities) bean).getIndex();
        case -949589828:  // valuationDateTime
          return ((NormalIborCapletFloorletExpiryFlatVolatilities) bean).getValuationDateTime();
        case 95027439:  // curve
          return ((NormalIborCapletFloorletExpiryFlatVolatilities) bean).getCurve();
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
   * The bean-builder for {@code NormalIborCapletFloorletExpiryFlatVolatilities}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<NormalIborCapletFloorletExpiryFlatVolatilities> {

    private IborIndex index;
    private ZonedDateTime valuationDateTime;
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
        case 100346066:  // index
          return index;
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
        case 100346066:  // index
          this.index = (IborIndex) newValue;
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
    public NormalIborCapletFloorletExpiryFlatVolatilities build() {
      return new NormalIborCapletFloorletExpiryFlatVolatilities(
          index,
          valuationDateTime,
          curve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("NormalIborCapletFloorletExpiryFlatVolatilities.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
