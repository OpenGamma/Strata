/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;

import com.opengamma.analytics.math.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.pricer.sensitivity.option.IborFutureOptionSensitivityKey;
import com.opengamma.strata.pricer.sensitivity.option.OptionPointSensitivity;
import com.opengamma.strata.pricer.sensitivity.option.OptionSensitivityKey;

import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * Volatility environment for Ibor future options in the normal or Bachelier model. 
 * The volatility is represented by a surface on the expiration and simple moneyness. 
 * The simple moneyness can be on the price or on the rate (1-price).
 */
@BeanDefinition
public class NormalVolatilityExpSimpleMoneynessIborFutureParameters 
    implements  NormalVolatilityIborFutureParameters, ImmutableBean, Serializable { 
  
  /** The normal volatility surface. The order of the dimensions is expiry/simple moneyness. Not null. */
  @PropertyDefinition(validate = "notNull")
  private final InterpolatedDoublesSurface parameters;
  /** Flag indicating if the moneyness is on the price (true) or on the rate (false). */
  @PropertyDefinition
  private final boolean isMoneynessOnPrice;
  /** The Ibor index of the underlying future. Not null. */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /** The day count applicable to the model. TODO: To be changed to incorporate time, not only dates. */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /** The valuation date. All data items in this environment are calibrated for this date. */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate valuationDate;
  /** The valuation time. All data items in this environment are calibrated for this time. */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime valuationTime;
  /** The valuation zone.*/
  @PropertyDefinition(validate = "notNull")
  private final ZoneId valuationZone;
  
  /**
   * Constructor.
   * @param surface  the normal volatility surface
   * @param isMoneynessOnPrice  flag indicating if the moneyness is on the price (true) or on the rate (false)
   * @param index  the Ibor index of the underlying future
   * @param dayCount  the day count applicable to the model
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the zone related to the valuation time
   */
  public NormalVolatilityExpSimpleMoneynessIborFutureParameters(InterpolatedDoublesSurface surface, 
      boolean isMoneynessOnPrice, IborIndex index, DayCount dayCount, LocalDate valuationDate, LocalTime valuationTime,
      ZoneId valuationZone) {
    this.parameters = ArgChecker.notNull(surface, "volatility surface should not be null");
    this.isMoneynessOnPrice = isMoneynessOnPrice;
    this.index = ArgChecker.notNull(index, "Ibor index should not be null");
    this.dayCount = ArgChecker.notNull(dayCount, "day count");
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuation date");
    this.valuationTime = ArgChecker.notNull(valuationTime, "valuation time");
    this.valuationZone = ArgChecker.notNull(valuationZone, "valuation zone");
  }

  @Override
  public double getVolatility(LocalDate expiryDate, LocalDate fixingDate, double strikePrice, double futurePrice) {
    double simpleMoneyness = isMoneynessOnPrice ? strikePrice - futurePrice : futurePrice - strikePrice;
    double expiryTime = relativeTime(expiryDate, null, null); // TODO: time and zone
    return parameters.getZValue(expiryTime, simpleMoneyness);
  }

  @Override
  public IborIndex getFutureIndex() {
    return index;
  }  
  
  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date, LocalTime time, ZoneId zone) {
    ArgChecker.notNull(date, "date");
    return dayCount.relativeYearFraction(valuationDate, date);
  }
  
  /**
   * Computes the sensitivity to the nodes used in the description of the normal volatility from a point 
   * sensitivity.
   * @param point  the point sensitivity at a given key
   * @return the sensitivity to the surface nodes
   */
  public Map<DoublesPair, Double> nodeSensitivity(OptionPointSensitivity point) {
    OptionSensitivityKey key = point.getKey();
    ArgChecker.isTrue(key instanceof IborFutureOptionSensitivityKey, 
        "key should be of type IborFutureOptionSensitivityKey");
    IborFutureOptionSensitivityKey keyOpt = (IborFutureOptionSensitivityKey) key;
    double simpleMoneyness = isMoneynessOnPrice ? keyOpt.getStrikePrice() - keyOpt.getFuturePrice() 
        : keyOpt.getFuturePrice() - keyOpt.getStrikePrice();
    double expiryTime = relativeTime(keyOpt.getExpiryDate(), null, null); // TODO: time and zone
    @SuppressWarnings("unchecked")
    Map<DoublesPair, Double> result = parameters.getInterpolator().getNodeSensitivitiesForValue(
        (Map<Double, Interpolator1DDataBundle>) parameters.getInterpolatorData(), 
        DoublesPair.of(expiryTime, simpleMoneyness));
    return result;
  }
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code NormalVolatilityExpSimpleMoneynessIborFutureParameters}.
   * @return the meta-bean, not null
   */
  public static NormalVolatilityExpSimpleMoneynessIborFutureParameters.Meta meta() {
    return NormalVolatilityExpSimpleMoneynessIborFutureParameters.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(NormalVolatilityExpSimpleMoneynessIborFutureParameters.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static NormalVolatilityExpSimpleMoneynessIborFutureParameters.Builder builder() {
    return new NormalVolatilityExpSimpleMoneynessIborFutureParameters.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected NormalVolatilityExpSimpleMoneynessIborFutureParameters(NormalVolatilityExpSimpleMoneynessIborFutureParameters.Builder builder) {
    JodaBeanUtils.notNull(builder.parameters, "parameters");
    JodaBeanUtils.notNull(builder.index, "index");
    JodaBeanUtils.notNull(builder.dayCount, "dayCount");
    JodaBeanUtils.notNull(builder.valuationDate, "valuationDate");
    JodaBeanUtils.notNull(builder.valuationTime, "valuationTime");
    JodaBeanUtils.notNull(builder.valuationZone, "valuationZone");
    this.parameters = builder.parameters;
    this.isMoneynessOnPrice = builder.isMoneynessOnPrice;
    this.index = builder.index;
    this.dayCount = builder.dayCount;
    this.valuationDate = builder.valuationDate;
    this.valuationTime = builder.valuationTime;
    this.valuationZone = builder.valuationZone;
  }

  @Override
  public NormalVolatilityExpSimpleMoneynessIborFutureParameters.Meta metaBean() {
    return NormalVolatilityExpSimpleMoneynessIborFutureParameters.Meta.INSTANCE;
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
   * Gets the normal volatility surface. The order of the dimensions is expiry/simple moneyness. Not null.
   * @return the value of the property, not null
   */
  public InterpolatedDoublesSurface getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets flag indicating if the moneyness is on the price (true) or on the rate (false).
   * @return the value of the property
   */
  public boolean isIsMoneynessOnPrice() {
    return isMoneynessOnPrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor index of the underlying future. Not null.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count applicable to the model. TODO: To be changed to incorporate time, not only dates.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date. All data items in this environment are calibrated for this date.
   * @return the value of the property, not null
   */
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation time. All data items in this environment are calibrated for this time.
   * @return the value of the property, not null
   */
  public LocalTime getValuationTime() {
    return valuationTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation zone.
   * @return the value of the property, not null
   */
  public ZoneId getValuationZone() {
    return valuationZone;
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
      NormalVolatilityExpSimpleMoneynessIborFutureParameters other = (NormalVolatilityExpSimpleMoneynessIborFutureParameters) obj;
      return JodaBeanUtils.equal(getParameters(), other.getParameters()) &&
          (isIsMoneynessOnPrice() == other.isIsMoneynessOnPrice()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getValuationTime(), other.getValuationTime()) &&
          JodaBeanUtils.equal(getValuationZone(), other.getValuationZone());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getParameters());
    hash = hash * 31 + JodaBeanUtils.hashCode(isIsMoneynessOnPrice());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationTime());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationZone());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("NormalVolatilityExpSimpleMoneynessIborFutureParameters{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(getParameters())).append(',').append(' ');
    buf.append("isMoneynessOnPrice").append('=').append(JodaBeanUtils.toString(isIsMoneynessOnPrice())).append(',').append(' ');
    buf.append("index").append('=').append(JodaBeanUtils.toString(getIndex())).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount())).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(getValuationDate())).append(',').append(' ');
    buf.append("valuationTime").append('=').append(JodaBeanUtils.toString(getValuationTime())).append(',').append(' ');
    buf.append("valuationZone").append('=').append(JodaBeanUtils.toString(getValuationZone())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NormalVolatilityExpSimpleMoneynessIborFutureParameters}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<InterpolatedDoublesSurface> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, InterpolatedDoublesSurface.class);
    /**
     * The meta-property for the {@code isMoneynessOnPrice} property.
     */
    private final MetaProperty<Boolean> isMoneynessOnPrice = DirectMetaProperty.ofImmutable(
        this, "isMoneynessOnPrice", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, IborIndex.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, LocalDate.class);
    /**
     * The meta-property for the {@code valuationTime} property.
     */
    private final MetaProperty<LocalTime> valuationTime = DirectMetaProperty.ofImmutable(
        this, "valuationTime", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, LocalTime.class);
    /**
     * The meta-property for the {@code valuationZone} property.
     */
    private final MetaProperty<ZoneId> valuationZone = DirectMetaProperty.ofImmutable(
        this, "valuationZone", NormalVolatilityExpSimpleMoneynessIborFutureParameters.class, ZoneId.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "parameters",
        "isMoneynessOnPrice",
        "index",
        "dayCount",
        "valuationDate",
        "valuationTime",
        "valuationZone");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return parameters;
        case 681457885:  // isMoneynessOnPrice
          return isMoneynessOnPrice;
        case 100346066:  // index
          return index;
        case 1905311443:  // dayCount
          return dayCount;
        case 113107279:  // valuationDate
          return valuationDate;
        case 113591406:  // valuationTime
          return valuationTime;
        case 113775949:  // valuationZone
          return valuationZone;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public NormalVolatilityExpSimpleMoneynessIborFutureParameters.Builder builder() {
      return new NormalVolatilityExpSimpleMoneynessIborFutureParameters.Builder();
    }

    @Override
    public Class<? extends NormalVolatilityExpSimpleMoneynessIborFutureParameters> beanType() {
      return NormalVolatilityExpSimpleMoneynessIborFutureParameters.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code parameters} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<InterpolatedDoublesSurface> parameters() {
      return parameters;
    }

    /**
     * The meta-property for the {@code isMoneynessOnPrice} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> isMoneynessOnPrice() {
      return isMoneynessOnPrice;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code valuationTime} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalTime> valuationTime() {
      return valuationTime;
    }

    /**
     * The meta-property for the {@code valuationZone} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ZoneId> valuationZone() {
      return valuationZone;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).getParameters();
        case 681457885:  // isMoneynessOnPrice
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).isIsMoneynessOnPrice();
        case 100346066:  // index
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).getIndex();
        case 1905311443:  // dayCount
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).getDayCount();
        case 113107279:  // valuationDate
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).getValuationDate();
        case 113591406:  // valuationTime
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).getValuationTime();
        case 113775949:  // valuationZone
          return ((NormalVolatilityExpSimpleMoneynessIborFutureParameters) bean).getValuationZone();
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
   * The bean-builder for {@code NormalVolatilityExpSimpleMoneynessIborFutureParameters}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<NormalVolatilityExpSimpleMoneynessIborFutureParameters> {

    private InterpolatedDoublesSurface parameters;
    private boolean isMoneynessOnPrice;
    private IborIndex index;
    private DayCount dayCount;
    private LocalDate valuationDate;
    private LocalTime valuationTime;
    private ZoneId valuationZone;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(NormalVolatilityExpSimpleMoneynessIborFutureParameters beanToCopy) {
      this.parameters = beanToCopy.getParameters();
      this.isMoneynessOnPrice = beanToCopy.isIsMoneynessOnPrice();
      this.index = beanToCopy.getIndex();
      this.dayCount = beanToCopy.getDayCount();
      this.valuationDate = beanToCopy.getValuationDate();
      this.valuationTime = beanToCopy.getValuationTime();
      this.valuationZone = beanToCopy.getValuationZone();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return parameters;
        case 681457885:  // isMoneynessOnPrice
          return isMoneynessOnPrice;
        case 100346066:  // index
          return index;
        case 1905311443:  // dayCount
          return dayCount;
        case 113107279:  // valuationDate
          return valuationDate;
        case 113591406:  // valuationTime
          return valuationTime;
        case 113775949:  // valuationZone
          return valuationZone;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          this.parameters = (InterpolatedDoublesSurface) newValue;
          break;
        case 681457885:  // isMoneynessOnPrice
          this.isMoneynessOnPrice = (Boolean) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case 113591406:  // valuationTime
          this.valuationTime = (LocalTime) newValue;
          break;
        case 113775949:  // valuationZone
          this.valuationZone = (ZoneId) newValue;
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
    public NormalVolatilityExpSimpleMoneynessIborFutureParameters build() {
      return new NormalVolatilityExpSimpleMoneynessIborFutureParameters(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code parameters} property in the builder.
     * @param parameters  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder parameters(InterpolatedDoublesSurface parameters) {
      JodaBeanUtils.notNull(parameters, "parameters");
      this.parameters = parameters;
      return this;
    }

    /**
     * Sets the {@code isMoneynessOnPrice} property in the builder.
     * @param isMoneynessOnPrice  the new value
     * @return this, for chaining, not null
     */
    public Builder isMoneynessOnPrice(boolean isMoneynessOnPrice) {
      this.isMoneynessOnPrice = isMoneynessOnPrice;
      return this;
    }

    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the {@code valuationDate} property in the builder.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the {@code valuationTime} property in the builder.
     * @param valuationTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationTime(LocalTime valuationTime) {
      JodaBeanUtils.notNull(valuationTime, "valuationTime");
      this.valuationTime = valuationTime;
      return this;
    }

    /**
     * Sets the {@code valuationZone} property in the builder.
     * @param valuationZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationZone(ZoneId valuationZone) {
      JodaBeanUtils.notNull(valuationZone, "valuationZone");
      this.valuationZone = valuationZone;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("NormalVolatilityExpSimpleMoneynessIborFutureParameters.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters)).append(',').append(' ');
      buf.append("isMoneynessOnPrice").append('=').append(JodaBeanUtils.toString(isMoneynessOnPrice)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("valuationTime").append('=').append(JodaBeanUtils.toString(valuationTime)).append(',').append(' ');
      buf.append("valuationZone").append('=').append(JodaBeanUtils.toString(valuationZone)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
