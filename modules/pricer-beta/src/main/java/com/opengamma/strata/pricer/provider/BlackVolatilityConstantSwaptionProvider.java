/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.provider;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;

/**
 * Volatility environment for swaptions in the log-normal or Black model. 
 * The volatility is represented by a constant, independent of any swaption details.
 * The expiry is measured in days according to a day count convention; the expiry time is ignored.
 */
@BeanDefinition(builderScope = "private")
public class BlackVolatilityConstantSwaptionProvider
  implements  BlackVolatilitySwaptionProvider, ImmutableBean, Serializable { 
  
  /** The normal volatility. */
  @PropertyDefinition(validate = "notNull")
  private final double volatility;
  /** The swap convention for which the data is valid. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedIborSwapConvention convention;
  /** The day count applicable to the model. */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /** The valuation date. All data items in this environment are calibrated for this date. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /** The valuation time. All data items in this environment are calibrated for this time. */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime valuationTime;
  /** The valuation zone.*/
  @PropertyDefinition(validate = "notNull")
  private final ZoneId valuationZone;
  
  /**
   * Creates a provider from the implied volatility surface and the date, time and zone for which it is valid.
   * @param volatility  the implied volatility
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @return the provider
   */
  public static BlackVolatilityConstantSwaptionProvider of(
      double volatility, 
      FixedIborSwapConvention convention, 
      DayCount dayCount, 
      LocalDate valuationDate, 
      LocalTime valuationTime, 
      ZoneId valuationZone) {
    return new BlackVolatilityConstantSwaptionProvider(volatility, convention, dayCount, valuationDate, 
        valuationTime, valuationZone);
  }
  
  /**
   * Creates a provider from the implied volatility surface and the date. 
   * The valuation time and zone are defaulted to noon UTC.
   * @param volatility  the implied volatility
   * @param convention  the swap convention for which the data is valid
   * @param dayCount  the day count applicable to the model
   * @param valuationDate  the valuation date
   * @return the provider
   */
  public static BlackVolatilityConstantSwaptionProvider of(
      double volatility, 
      FixedIborSwapConvention convention, 
      DayCount dayCount, 
      LocalDate valuationDate) {
    return new BlackVolatilityConstantSwaptionProvider(volatility, convention, dayCount, valuationDate, LocalTime.NOON,
        ZoneOffset.UTC);
  }
  
  // private constructor
  private BlackVolatilityConstantSwaptionProvider(
      double volatility, 
      FixedIborSwapConvention convention, 
      DayCount dayCount, 
      LocalDate valuationDate, 
      LocalTime valuationTime, 
      ZoneId valuationZone) {
    this.volatility = volatility;
    this.convention = convention;
    this.dayCount = dayCount;
    this.valuationDate = valuationDate;
    this.valuationTime = valuationTime;
    this.valuationZone = valuationZone;
  }  

  @Override
  public double getVolatility(ZonedDateTime expiryDate, double tenor, double strike, double forwardRate) {
    return volatility;
  }

  @Override
  public double relativeYearFraction(ZonedDateTime dateTime) {
    LocalDate date = dateTime.toLocalDate();
    boolean timeIsNegative = valuationDate.isAfter(date);
    if (timeIsNegative) {
      return -dayCount.yearFraction(date, valuationDate);
    }
    return dayCount.yearFraction(valuationDate, date);
  }

  @Override
  public double tenor(LocalDate startDate, LocalDate endDate) {
    // rounded number of months. the rounding is to ensure that an integer number of year even with holidays/leap year
    return Math.round((endDate.toEpochDay() - startDate.toEpochDay()) / 365.25 * 12) / 12  ;
  }
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BlackVolatilityConstantSwaptionProvider}.
   * @return the meta-bean, not null
   */
  public static BlackVolatilityConstantSwaptionProvider.Meta meta() {
    return BlackVolatilityConstantSwaptionProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BlackVolatilityConstantSwaptionProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected BlackVolatilityConstantSwaptionProvider(BlackVolatilityConstantSwaptionProvider.Builder builder) {
    JodaBeanUtils.notNull(builder.volatility, "volatility");
    JodaBeanUtils.notNull(builder.convention, "convention");
    JodaBeanUtils.notNull(builder.dayCount, "dayCount");
    JodaBeanUtils.notNull(builder.valuationDate, "valuationDate");
    JodaBeanUtils.notNull(builder.valuationTime, "valuationTime");
    JodaBeanUtils.notNull(builder.valuationZone, "valuationZone");
    this.volatility = builder.volatility;
    this.convention = builder.convention;
    this.dayCount = builder.dayCount;
    this.valuationDate = builder.valuationDate;
    this.valuationTime = builder.valuationTime;
    this.valuationZone = builder.valuationZone;
  }

  @Override
  public BlackVolatilityConstantSwaptionProvider.Meta metaBean() {
    return BlackVolatilityConstantSwaptionProvider.Meta.INSTANCE;
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
   * Gets the normal volatility.
   * @return the value of the property, not null
   */
  public double getVolatility() {
    return volatility;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap convention for which the data is valid.
   * @return the value of the property, not null
   */
  @Override
  public FixedIborSwapConvention getConvention() {
    return convention;
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
   * Gets the valuation date. All data items in this environment are calibrated for this date.
   * @return the value of the property, not null
   */
  @Override
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BlackVolatilityConstantSwaptionProvider other = (BlackVolatilityConstantSwaptionProvider) obj;
      return JodaBeanUtils.equal(getVolatility(), other.getVolatility()) &&
          JodaBeanUtils.equal(getConvention(), other.getConvention()) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(getVolatility());
    hash = hash * 31 + JodaBeanUtils.hashCode(getConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationTime());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationZone());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("BlackVolatilityConstantSwaptionProvider{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("volatility").append('=').append(JodaBeanUtils.toString(getVolatility())).append(',').append(' ');
    buf.append("convention").append('=').append(JodaBeanUtils.toString(getConvention())).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount())).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(getValuationDate())).append(',').append(' ');
    buf.append("valuationTime").append('=').append(JodaBeanUtils.toString(getValuationTime())).append(',').append(' ');
    buf.append("valuationZone").append('=').append(JodaBeanUtils.toString(getValuationZone())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackVolatilityConstantSwaptionProvider}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code volatility} property.
     */
    private final MetaProperty<Double> volatility = DirectMetaProperty.ofImmutable(
        this, "volatility", BlackVolatilityConstantSwaptionProvider.class, Double.TYPE);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", BlackVolatilityConstantSwaptionProvider.class, FixedIborSwapConvention.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", BlackVolatilityConstantSwaptionProvider.class, DayCount.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", BlackVolatilityConstantSwaptionProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code valuationTime} property.
     */
    private final MetaProperty<LocalTime> valuationTime = DirectMetaProperty.ofImmutable(
        this, "valuationTime", BlackVolatilityConstantSwaptionProvider.class, LocalTime.class);
    /**
     * The meta-property for the {@code valuationZone} property.
     */
    private final MetaProperty<ZoneId> valuationZone = DirectMetaProperty.ofImmutable(
        this, "valuationZone", BlackVolatilityConstantSwaptionProvider.class, ZoneId.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "volatility",
        "convention",
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
        case -1917967323:  // volatility
          return volatility;
        case 2039569265:  // convention
          return convention;
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
    public BeanBuilder<? extends BlackVolatilityConstantSwaptionProvider> builder() {
      return new BlackVolatilityConstantSwaptionProvider.Builder();
    }

    @Override
    public Class<? extends BlackVolatilityConstantSwaptionProvider> beanType() {
      return BlackVolatilityConstantSwaptionProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code volatility} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> volatility() {
      return volatility;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<FixedIborSwapConvention> convention() {
      return convention;
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
        case -1917967323:  // volatility
          return ((BlackVolatilityConstantSwaptionProvider) bean).getVolatility();
        case 2039569265:  // convention
          return ((BlackVolatilityConstantSwaptionProvider) bean).getConvention();
        case 1905311443:  // dayCount
          return ((BlackVolatilityConstantSwaptionProvider) bean).getDayCount();
        case 113107279:  // valuationDate
          return ((BlackVolatilityConstantSwaptionProvider) bean).getValuationDate();
        case 113591406:  // valuationTime
          return ((BlackVolatilityConstantSwaptionProvider) bean).getValuationTime();
        case 113775949:  // valuationZone
          return ((BlackVolatilityConstantSwaptionProvider) bean).getValuationZone();
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
   * The bean-builder for {@code BlackVolatilityConstantSwaptionProvider}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<BlackVolatilityConstantSwaptionProvider> {

    private double volatility;
    private FixedIborSwapConvention convention;
    private DayCount dayCount;
    private LocalDate valuationDate;
    private LocalTime valuationTime;
    private ZoneId valuationZone;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1917967323:  // volatility
          return volatility;
        case 2039569265:  // convention
          return convention;
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
        case -1917967323:  // volatility
          this.volatility = (Double) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FixedIborSwapConvention) newValue;
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
    public BlackVolatilityConstantSwaptionProvider build() {
      return new BlackVolatilityConstantSwaptionProvider(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("BlackVolatilityConstantSwaptionProvider.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("valuationTime").append('=').append(JodaBeanUtils.toString(valuationTime)).append(',').append(' ');
      buf.append("valuationZone").append('=').append(JodaBeanUtils.toString(valuationZone)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
