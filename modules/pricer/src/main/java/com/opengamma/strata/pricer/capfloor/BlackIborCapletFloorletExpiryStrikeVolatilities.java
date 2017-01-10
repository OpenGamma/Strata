/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceInfoType;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Volatility for Ibor caplet/floorlet in the log-normal or Black model based on a surface.
 * <p> 
 * The volatility is represented by a surface on the expiry and strike dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class BlackIborCapletFloorletExpiryStrikeVolatilities
    implements BlackIborCapletFloorletVolatilities, ImmutableBean, Serializable {

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
   * The Black volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the strike.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface surface;
  /**
   * The day count convention of the surface.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility surface and the date-time for which it is valid.
   * <p>
   * The surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surface must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#STRIKE}
   * <li>The z-value type must be {@link ValueType#BLACK_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link SurfaceInfoType#DAY_COUNT}
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#blackVolatilityByExpiryStrike(String, DayCount)}.
   * 
   * @param index  the Ibor index for which the data is valid
   * @param valuationDateTime  the valuation date-time
   * @param surface  the implied volatility surface
   * @return the volatilities
   */
  public static BlackIborCapletFloorletExpiryStrikeVolatilities of(
      IborIndex index,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    return new BlackIborCapletFloorletExpiryStrikeVolatilities(index, valuationDateTime, surface);
  }

  @ImmutableConstructor
  private BlackIborCapletFloorletExpiryStrikeVolatilities(
      IborIndex index,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    ArgChecker.notNull(surface, "surface");
    surface.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Black volatilities");
    surface.getMetadata().getYValueType().checkEquals(
        ValueType.STRIKE, "Incorrect y-value type for Black volatilities");
    surface.getMetadata().getZValueType().checkEquals(
        ValueType.BLACK_VOLATILITY, "Incorrect z-value type for Black volatilities");
    DayCount dayCount = surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));

    this.index = index;
    this.valuationDateTime = valuationDateTime;
    this.surface = surface;
    this.dayCount = dayCount;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new BlackIborCapletFloorletExpiryStrikeVolatilities(index, valuationDateTime, surface);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborCapletFloorletVolatilitiesName getName() {
    return IborCapletFloorletVolatilitiesName.of(surface.getName().getName());
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (surface.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(surface));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return surface.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return surface.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return surface.getParameterMetadata(parameterIndex);
  }

  @Override
  public BlackIborCapletFloorletExpiryStrikeVolatilities withParameter(int parameterIndex, double newValue) {
    return new BlackIborCapletFloorletExpiryStrikeVolatilities(
        index, valuationDateTime, surface.withParameter(parameterIndex, newValue));
  }

  @Override
  public BlackIborCapletFloorletExpiryStrikeVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new BlackIborCapletFloorletExpiryStrikeVolatilities(
        index, valuationDateTime, surface.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double strike, double forward) {
    return surface.zValue(expiry, strike);
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
    double strike = point.getStrike();
    UnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, strike);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.price(forward, strike, expiry, volatility, putCall.isCall());
  }

  @Override
  public double priceDelta(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.delta(forward, strike, expiry, volatility, putCall.isCall());
  }

  @Override
  public double priceGamma(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.gamma(forward, strike, expiry, volatility);
  }

  @Override
  public double priceTheta(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.driftlessTheta(forward, strike, expiry, volatility);
  }

  @Override
  public double priceVega(double expiry, PutCall putCall, double strike, double forward, double volatility) {
    return BlackFormulaRepository.vega(forward, strike, expiry, volatility);
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
   * The meta-bean for {@code BlackIborCapletFloorletExpiryStrikeVolatilities}.
   * @return the meta-bean, not null
   */
  public static BlackIborCapletFloorletExpiryStrikeVolatilities.Meta meta() {
    return BlackIborCapletFloorletExpiryStrikeVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackIborCapletFloorletExpiryStrikeVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public BlackIborCapletFloorletExpiryStrikeVolatilities.Meta metaBean() {
    return BlackIborCapletFloorletExpiryStrikeVolatilities.Meta.INSTANCE;
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
   * Gets the Black volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the strike.
   * @return the value of the property, not null
   */
  public Surface getSurface() {
    return surface;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BlackIborCapletFloorletExpiryStrikeVolatilities other = (BlackIborCapletFloorletExpiryStrikeVolatilities) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(surface, other.surface);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("BlackIborCapletFloorletExpiryStrikeVolatilities{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackIborCapletFloorletExpiryStrikeVolatilities}.
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
        this, "index", BlackIborCapletFloorletExpiryStrikeVolatilities.class, IborIndex.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackIborCapletFloorletExpiryStrikeVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code surface} property.
     */
    private final MetaProperty<Surface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", BlackIborCapletFloorletExpiryStrikeVolatilities.class, Surface.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "valuationDateTime",
        "surface");

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
        case -1853231955:  // surface
          return surface;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BlackIborCapletFloorletExpiryStrikeVolatilities> builder() {
      return new BlackIborCapletFloorletExpiryStrikeVolatilities.Builder();
    }

    @Override
    public Class<? extends BlackIborCapletFloorletExpiryStrikeVolatilities> beanType() {
      return BlackIborCapletFloorletExpiryStrikeVolatilities.class;
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
     * The meta-property for the {@code surface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Surface> surface() {
      return surface;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((BlackIborCapletFloorletExpiryStrikeVolatilities) bean).getIndex();
        case -949589828:  // valuationDateTime
          return ((BlackIborCapletFloorletExpiryStrikeVolatilities) bean).getValuationDateTime();
        case -1853231955:  // surface
          return ((BlackIborCapletFloorletExpiryStrikeVolatilities) bean).getSurface();
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
   * The bean-builder for {@code BlackIborCapletFloorletExpiryStrikeVolatilities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<BlackIborCapletFloorletExpiryStrikeVolatilities> {

    private IborIndex index;
    private ZonedDateTime valuationDateTime;
    private Surface surface;

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
        case -1853231955:  // surface
          return surface;
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
        case -1853231955:  // surface
          this.surface = (Surface) newValue;
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
    public BlackIborCapletFloorletExpiryStrikeVolatilities build() {
      return new BlackIborCapletFloorletExpiryStrikeVolatilities(
          index,
          valuationDateTime,
          surface);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("BlackIborCapletFloorletExpiryStrikeVolatilities.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
