/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

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

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.model.MoneynessType;
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

/**
 * Data provider of volatility for Ibor future options in the normal or Bachelier model.
 * <p>
 * The volatility is represented by a surface on the expiry and simple moneyness.
 * The expiry is measured in number of days (not time) according to a day-count convention.
 * The simple moneyness can be on the price or on the rate (1-price).
 */
@BeanDefinition
public final class NormalIborFutureOptionExpirySimpleMoneynessVolatilities
    implements NormalIborFutureOptionVolatilities, ImmutableBean, Serializable {

  /**
   * The index of the underlying future.
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
   * The normal volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the simple moneyness.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface surface;
  /**
   * Whether the moneyness is on the price (true) or on the rate (false).
   */
  private final boolean moneynessOnPrice;  // cached, not a property
  /**
   * The day count convention of the surface.
   */
  private final DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the volatility surface and the date-time for which it is valid.
   * <p>
   * The surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surface must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#SIMPLE_MONEYNESS}
   * <li>The z-value type must be {@link ValueType#NORMAL_VOLATILITY}
   * <li>The day count must be set in the additional information using {@link SurfaceInfoType#DAY_COUNT}
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#normalVolatilityByExpirySimpleMoneyness(String, DayCount, MoneynessType)}.
   * 
   * @param index  the Ibor index
   * @param surface  the implied volatility surface
   * @param valuationDateTime  the valuation date-time
   * @return the volatilities
   */
  public static NormalIborFutureOptionExpirySimpleMoneynessVolatilities of(
      IborIndex index,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    return new NormalIborFutureOptionExpirySimpleMoneynessVolatilities(index, valuationDateTime, surface);
  }

  @ImmutableConstructor
  private NormalIborFutureOptionExpirySimpleMoneynessVolatilities(
      IborIndex index,
      ZonedDateTime valuationDateTime,
      Surface surface) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(valuationDateTime, "valuationDateTime");
    ArgChecker.notNull(surface, "surface");
    surface.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for Normal volatilities");
    surface.getMetadata().getYValueType().checkEquals(
        ValueType.SIMPLE_MONEYNESS, "Incorrect y-value type for Normal volatilities");
    surface.getMetadata().getZValueType().checkEquals(
        ValueType.NORMAL_VOLATILITY, "Incorrect z-value type for Normal volatilities");
    DayCount dayCount = surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));
    MoneynessType moneynessType = surface.getMetadata().findInfo(SurfaceInfoType.MONEYNESS_TYPE)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing MoneynessType"));

    this.index = index;
    this.valuationDateTime = valuationDateTime;
    this.surface = surface;
    this.moneynessOnPrice = moneynessType == MoneynessType.PRICE;
    this.dayCount = dayCount;
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureOptionVolatilitiesName getName() {
    return IborFutureOptionVolatilitiesName.of(surface.getName().getName());
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
  public NormalIborFutureOptionExpirySimpleMoneynessVolatilities withParameter(int parameterIndex, double newValue) {
    return new NormalIborFutureOptionExpirySimpleMoneynessVolatilities(
        index, valuationDateTime, surface.withParameter(parameterIndex, newValue));
  }

  @Override
  public NormalIborFutureOptionExpirySimpleMoneynessVolatilities withPerturbation(ParameterPerturbation perturbation) {
    return new NormalIborFutureOptionExpirySimpleMoneynessVolatilities(
        index, valuationDateTime, surface.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, LocalDate fixingDate, double strikePrice, double futurePrice) {
    double simpleMoneyness = moneynessOnPrice ? strikePrice - futurePrice : futurePrice - strikePrice;
    return surface.zValue(expiry, simpleMoneyness);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof IborFutureOptionSensitivity) {
        IborFutureOptionSensitivity pt = (IborFutureOptionSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(IborFutureOptionSensitivity point) {
    double simpleMoneyness = moneynessOnPrice ?
        point.getStrikePrice() - point.getFuturePrice() :
        point.getFuturePrice() - point.getStrikePrice();
    UnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(point.getExpiry(), simpleMoneyness);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(ZonedDateTime zonedDateTime) {
    ArgChecker.notNull(zonedDateTime, "date");
    return dayCount.relativeYearFraction(valuationDateTime.toLocalDate(), zonedDateTime.toLocalDate());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code NormalIborFutureOptionExpirySimpleMoneynessVolatilities}.
   * @return the meta-bean, not null
   */
  public static NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Meta meta() {
    return NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Builder builder() {
    return new NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Builder();
  }

  @Override
  public NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Meta metaBean() {
    return NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Meta.INSTANCE;
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
   * Gets the index of the underlying future.
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
   * Gets the normal volatility surface.
   * <p>
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the simple moneyness.
   * @return the value of the property, not null
   */
  public Surface getSurface() {
    return surface;
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
      NormalIborFutureOptionExpirySimpleMoneynessVolatilities other = (NormalIborFutureOptionExpirySimpleMoneynessVolatilities) obj;
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
    buf.append("NormalIborFutureOptionExpirySimpleMoneynessVolatilities{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("surface").append('=').append(JodaBeanUtils.toString(surface));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalIborFutureOptionExpirySimpleMoneynessVolatilities}.
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
        this, "index", NormalIborFutureOptionExpirySimpleMoneynessVolatilities.class, IborIndex.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalIborFutureOptionExpirySimpleMoneynessVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code surface} property.
     */
    private final MetaProperty<Surface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", NormalIborFutureOptionExpirySimpleMoneynessVolatilities.class, Surface.class);
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
    public NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Builder builder() {
      return new NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Builder();
    }

    @Override
    public Class<? extends NormalIborFutureOptionExpirySimpleMoneynessVolatilities> beanType() {
      return NormalIborFutureOptionExpirySimpleMoneynessVolatilities.class;
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
          return ((NormalIborFutureOptionExpirySimpleMoneynessVolatilities) bean).getIndex();
        case -949589828:  // valuationDateTime
          return ((NormalIborFutureOptionExpirySimpleMoneynessVolatilities) bean).getValuationDateTime();
        case -1853231955:  // surface
          return ((NormalIborFutureOptionExpirySimpleMoneynessVolatilities) bean).getSurface();
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
   * The bean-builder for {@code NormalIborFutureOptionExpirySimpleMoneynessVolatilities}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<NormalIborFutureOptionExpirySimpleMoneynessVolatilities> {

    private IborIndex index;
    private ZonedDateTime valuationDateTime;
    private Surface surface;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(NormalIborFutureOptionExpirySimpleMoneynessVolatilities beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
      this.surface = beanToCopy.getSurface();
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
    public NormalIborFutureOptionExpirySimpleMoneynessVolatilities build() {
      return new NormalIborFutureOptionExpirySimpleMoneynessVolatilities(
          index,
          valuationDateTime,
          surface);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index of the underlying future.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the valuation date-time.
     * <p>
     * The volatilities are calibrated for this date-time.
     * @param valuationDateTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDateTime(ZonedDateTime valuationDateTime) {
      JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
      this.valuationDateTime = valuationDateTime;
      return this;
    }

    /**
     * Sets the normal volatility surface.
     * <p>
     * The x-value of the surface is the expiry, as a year fraction.
     * The y-value of the surface is the simple moneyness.
     * @param surface  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder surface(Surface surface) {
      JodaBeanUtils.notNull(surface, "surface");
      this.surface = surface;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("NormalIborFutureOptionExpirySimpleMoneynessVolatilities.Builder{");
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
