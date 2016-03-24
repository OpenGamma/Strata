/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.IborCapletFloorletSensitivity;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceUnitParameterSensitivity;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;

/**
 * Volatility for Ibor caplet/floorlet in the normal or Bachelier model based on a surface.
 * <p>
 * The volatility is represented by a surface on the expiry and strike dimensions.
 */
@BeanDefinition(builderScope = "private")
public final class NormalIborCapletFloorletExpiryStrikeVolatilities
    implements NormalIborCapletFloorletVolatilities, ImmutableBean, Serializable {

  /** 
   * The normal volatility surface.  
   * <p>
   * The order of the dimensions is expiry/strike.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface surface;
  /**
   * The Ibor index.
   * <p>
   * The data must valid in terms of this Ibor index. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;
  /** 
   * The day count applicable to the model. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /** 
   * The valuation date-time. 
   * <p>
   * All data items in this environment are calibrated for this date-time. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the implied volatility surface and the date-time for which it is valid.
   * 
   * @param surface  the implied volatility surface
   * @param index  the Ibor index for which the data is valid
   * @param valuationDateTime  the valuation date-time
   * @param dayCount  the day count applicable to the model
   * @return the volatilities
   */
  public static NormalIborCapletFloorletExpiryStrikeVolatilities of(
      NodalSurface surface,
      IborIndex index,
      ZonedDateTime valuationDateTime,
      DayCount dayCount) {

    return new NormalIborCapletFloorletExpiryStrikeVolatilities(surface, index, dayCount, valuationDateTime);
  }

  /**
   * Obtains an instance from the implied volatility surface and the date, time and zone for which it is valid.
   * 
   * @param surface  the implied volatility surface
   * @param index  the Ibor index for which the data is valid
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @param dayCount  the day count applicable to the model
   * @return the volatilities
   */
  public static NormalIborCapletFloorletExpiryStrikeVolatilities of(
      NodalSurface surface,
      IborIndex index,
      LocalDate valuationDate,
      LocalTime valuationTime,
      ZoneId valuationZone,
      DayCount dayCount) {

    return of(surface, index, valuationDate.atTime(valuationTime).atZone(valuationZone), dayCount);
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double strike, double forward) {
    return surface.zValue(expiry, strike);
  }

  @Override
  public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(IborCapletFloorletSensitivity point) {
    ArgChecker.isTrue(point.getIndex().equals(index),
        "Ibor index of provider must be the same as Ibor index of point sensitivity");
    double expiry = relativeTime(point.getExpiry());
    double strike = point.getStrike();
    SurfaceUnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, strike);
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, PutCall putCall, double strike, double forwardRate, double volatility) {
    return NormalFormulaRepository.price(forwardRate, strike, expiry, volatility, putCall);
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
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code NormalIborCapletFloorletExpiryStrikeVolatilities}.
   * @return the meta-bean, not null
   */
  public static NormalIborCapletFloorletExpiryStrikeVolatilities.Meta meta() {
    return NormalIborCapletFloorletExpiryStrikeVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(NormalIborCapletFloorletExpiryStrikeVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private NormalIborCapletFloorletExpiryStrikeVolatilities(
      NodalSurface surface,
      IborIndex index,
      DayCount dayCount,
      ZonedDateTime valuationDateTime) {
    JodaBeanUtils.notNull(surface, "surface");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    this.surface = surface;
    this.index = index;
    this.dayCount = dayCount;
    this.valuationDateTime = valuationDateTime;
  }

  @Override
  public NormalIborCapletFloorletExpiryStrikeVolatilities.Meta metaBean() {
    return NormalIborCapletFloorletExpiryStrikeVolatilities.Meta.INSTANCE;
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
   * Gets the normal volatility surface.
   * <p>
   * The order of the dimensions is expiry/strike.
   * @return the value of the property, not null
   */
  public NodalSurface getSurface() {
    return surface;
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
   * Gets the day count applicable to the model.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date-time.
   * <p>
   * All data items in this environment are calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      NormalIborCapletFloorletExpiryStrikeVolatilities other = (NormalIborCapletFloorletExpiryStrikeVolatilities) obj;
      return JodaBeanUtils.equal(surface, other.surface) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(surface);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("NormalIborCapletFloorletExpiryStrikeVolatilities{");
    buf.append("surface").append('=').append(surface).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalIborCapletFloorletExpiryStrikeVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code surface} property.
     */
    private final MetaProperty<NodalSurface> surface = DirectMetaProperty.ofImmutable(
        this, "surface", NormalIborCapletFloorletExpiryStrikeVolatilities.class, NodalSurface.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", NormalIborCapletFloorletExpiryStrikeVolatilities.class, IborIndex.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", NormalIborCapletFloorletExpiryStrikeVolatilities.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", NormalIborCapletFloorletExpiryStrikeVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "surface",
        "index",
        "dayCount",
        "valuationDateTime");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return surface;
        case 100346066:  // index
          return index;
        case 1905311443:  // dayCount
          return dayCount;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends NormalIborCapletFloorletExpiryStrikeVolatilities> builder() {
      return new NormalIborCapletFloorletExpiryStrikeVolatilities.Builder();
    }

    @Override
    public Class<? extends NormalIborCapletFloorletExpiryStrikeVolatilities> beanType() {
      return NormalIborCapletFloorletExpiryStrikeVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code surface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalSurface> surface() {
      return surface;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return valuationDateTime;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return ((NormalIborCapletFloorletExpiryStrikeVolatilities) bean).getSurface();
        case 100346066:  // index
          return ((NormalIborCapletFloorletExpiryStrikeVolatilities) bean).getIndex();
        case 1905311443:  // dayCount
          return ((NormalIborCapletFloorletExpiryStrikeVolatilities) bean).getDayCount();
        case -949589828:  // valuationDateTime
          return ((NormalIborCapletFloorletExpiryStrikeVolatilities) bean).getValuationDateTime();
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
   * The bean-builder for {@code NormalIborCapletFloorletExpiryStrikeVolatilities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<NormalIborCapletFloorletExpiryStrikeVolatilities> {

    private NodalSurface surface;
    private IborIndex index;
    private DayCount dayCount;
    private ZonedDateTime valuationDateTime;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          return surface;
        case 100346066:  // index
          return index;
        case 1905311443:  // dayCount
          return dayCount;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1853231955:  // surface
          this.surface = (NodalSurface) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
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
    public NormalIborCapletFloorletExpiryStrikeVolatilities build() {
      return new NormalIborCapletFloorletExpiryStrikeVolatilities(
          surface,
          index,
          dayCount,
          valuationDateTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("NormalIborCapletFloorletExpiryStrikeVolatilities.Builder{");
      buf.append("surface").append('=').append(JodaBeanUtils.toString(surface)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
