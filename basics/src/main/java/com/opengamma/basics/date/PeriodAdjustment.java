/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
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

import com.opengamma.collect.ArgChecker;

/**
 * An adjustment that alters a date by adding a period of calendar days, months and years.
 * <p>
 * This adjustment adds a {@link Period} to the input date, followed by an adjustment to ensure
 * the result is a valid business day.
 * <p>
 * Addition is performed using standard calendar addition.
 * It is not possible to add a number of business days using this class.
 * See {@link DaysAdjustment} for an alternative that can handle addition of business days.
 * <p>
 * There are two steps in the calculation:
 * <p>
 * In step one, the period is added using standard date arithmetic.
 * Business days are not taken into account.
 * <p>
 * In step two, the result of step one is optionally adjusted to be a business day
 * using a {@code BusinessDayAdjustment}.
 * <p>
 * For example, a rule represented by this class might be: "the end date is 5 years after the start date,
 * adjusted to be a valid London business day using the 'Following' convention".
 * 
 * <h4>Usage</h4>
 * {@code PeriodAdjustment} implements {@code TemporalAdjuster} allowing it to directly adjust a date:
 * <pre>
 *  LocalDate adjusted = baseDate.with(periodAdjustment);
 * </pre>
 */
@BeanDefinition(builderScope = "private")
public final class PeriodAdjustment
    implements ImmutableBean, DateAdjuster, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The period to be added.
   * <p>
   * When the adjustment is performed, this period will be added to the input date.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period period;
  /**
   * The business day adjustment that is performed to the result of the addition.
   * <p>
   * This adjustment is applied to the result of the period addition calculation.
   * <p>
   * If no adjustment is required, use the 'None' business day adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment adjustment;

  //-------------------------------------------------------------------------
  /**
   * Obtains a period adjustment that can adjust a date by the specified period.
   * <p>
   * When the adjustment is performed, the period will be added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   * 
   * @param period  the period to add to the input date
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  public static PeriodAdjustment of(Period period, BusinessDayAdjustment adjustment) {
    return new PeriodAdjustment(period, adjustment);
  }

  /**
   * Obtains a period adjustment that can adjust a date by a specific number of calendar years.
   * <p>
   * When the adjustment is performed, the years will be added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   * 
   * @param numberOfYears  the number of years
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  public static PeriodAdjustment ofYears(int numberOfYears, BusinessDayAdjustment adjustment) {
    return new PeriodAdjustment(Period.ofYears(numberOfYears), adjustment);
  }

  /**
   * Obtains a period adjustment that can adjust a date by a specific number of calendar months.
   * <p>
   * When the adjustment is performed, the months will be added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   * 
   * @param numberOfMonths  the number of months
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  public static PeriodAdjustment ofMonths(int numberOfMonths, BusinessDayAdjustment adjustment) {
    return new PeriodAdjustment(Period.ofMonths(numberOfMonths), adjustment);
  }

  /**
   * Obtains a period adjustment that can adjust a date by a specific number of calendar days.
   * <p>
   * When the adjustment is performed, the days will be added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   * 
   * @param numberOfDays  the number of days
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  public static PeriodAdjustment ofDays(int numberOfDays, BusinessDayAdjustment adjustment) {
    return new PeriodAdjustment(Period.ofDays(numberOfDays), adjustment);
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date, adding the period and then applying the business day adjustment.
   * <p>
   * The calculation is performed in two steps.
   * <p>
   * Step one, use {@link LocalDate#plus(java.time.temporal.TemporalAmount)} to add the period.
   * <p>
   * Step two, use {@link BusinessDayAdjustment#adjust(LocalDate)} to adjust the result of step one.
   * 
   * @param date  the date to adjust
   * @return the adjusted temporal
   */
  @Override
  public LocalDate adjust(LocalDate date) {
    ArgChecker.notNull(date, "date");
    LocalDate added = date.plus(period);
    return adjustment.adjust(added);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the adjustment.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append(period);
    if (adjustment.equals(BusinessDayAdjustment.NONE) == false) {
      buf.append(" then apply ").append(adjustment);
    }
    return buf.toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PeriodAdjustment}.
   * @return the meta-bean, not null
   */
  public static PeriodAdjustment.Meta meta() {
    return PeriodAdjustment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PeriodAdjustment.Meta.INSTANCE);
  }

  private PeriodAdjustment(
      Period period,
      BusinessDayAdjustment adjustment) {
    JodaBeanUtils.notNull(period, "period");
    JodaBeanUtils.notNull(adjustment, "adjustment");
    this.period = period;
    this.adjustment = adjustment;
  }

  @Override
  public PeriodAdjustment.Meta metaBean() {
    return PeriodAdjustment.Meta.INSTANCE;
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
   * Gets the period to be added.
   * <p>
   * When the adjustment is performed, this period will be added to the input date.
   * @return the value of the property, not null
   */
  public Period getPeriod() {
    return period;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment that is performed to the result of the addition.
   * <p>
   * This adjustment is applied to the result of the period addition calculation.
   * <p>
   * If no adjustment is required, use the 'None' business day adjustment.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getAdjustment() {
    return adjustment;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PeriodAdjustment other = (PeriodAdjustment) obj;
      return JodaBeanUtils.equal(getPeriod(), other.getPeriod()) &&
          JodaBeanUtils.equal(getAdjustment(), other.getAdjustment());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getPeriod());
    hash += hash * 31 + JodaBeanUtils.hashCode(getAdjustment());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PeriodAdjustment}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code period} property.
     */
    private final MetaProperty<Period> period = DirectMetaProperty.ofImmutable(
        this, "period", PeriodAdjustment.class, Period.class);
    /**
     * The meta-property for the {@code adjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> adjustment = DirectMetaProperty.ofImmutable(
        this, "adjustment", PeriodAdjustment.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "period",
        "adjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -991726143:  // period
          return period;
        case 1977085293:  // adjustment
          return adjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PeriodAdjustment> builder() {
      return new PeriodAdjustment.Builder();
    }

    @Override
    public Class<? extends PeriodAdjustment> beanType() {
      return PeriodAdjustment.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code period} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> period() {
      return period;
    }

    /**
     * The meta-property for the {@code adjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> adjustment() {
      return adjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -991726143:  // period
          return ((PeriodAdjustment) bean).getPeriod();
        case 1977085293:  // adjustment
          return ((PeriodAdjustment) bean).getAdjustment();
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
   * The bean-builder for {@code PeriodAdjustment}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<PeriodAdjustment> {

    private Period period;
    private BusinessDayAdjustment adjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -991726143:  // period
          return period;
        case 1977085293:  // adjustment
          return adjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -991726143:  // period
          this.period = (Period) newValue;
          break;
        case 1977085293:  // adjustment
          this.adjustment = (BusinessDayAdjustment) newValue;
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
    public PeriodAdjustment build() {
      return new PeriodAdjustment(
          period,
          adjustment);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("PeriodAdjustment.Builder{");
      buf.append("period").append('=').append(JodaBeanUtils.toString(period)).append(',').append(' ');
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
