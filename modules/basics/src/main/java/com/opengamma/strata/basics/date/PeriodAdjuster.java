/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.market.ReferenceData;

/**
 * An adjuster that alters a date by adding a period of calendar days, months and years,
 * resolved to specific holiday calendars.
 * <p>
 * This is the resolved form of {@link PeriodAdjustment} which describes the adjustment in detail.
 * Applications will typically create a {@code PeriodAdjuster} from a {@code PeriodAdjustment}
 * using {@link PeriodAdjustment#resolve(ReferenceData)}.
 * <p>
 * A {@code PeriodAdjuster} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition(constructorScope = "package")
public final class PeriodAdjuster
    implements ImmutableBean, DateAdjuster, Serializable {

  /**
   * An instance that performs no adjustment.
   */
  public static final PeriodAdjuster NONE =
      new PeriodAdjuster(Period.ZERO, PeriodAdditionConventions.NONE, BusinessDayAdjuster.NONE);

  /**
   * The period to be added.
   * <p>
   * When the adjustment is performed, this period will be added to the input date.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period period;
  /**
   * The addition convention to apply.
   * <p>
   * When the adjustment is performed, this convention is used to refine the adjusted date.
   * The most common convention is to move the end date to the last business day of the month
   * if the start date is the last business day of the month.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodAdditionConvention additionConvention;
  /**
   * The business day adjuster that is applied to the result of the addition.
   * <p>
   * This adjuster is applied to the result of the period addition calculation.
   * <p>
   * If no adjuster is required, use the 'None' business day adjuster.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjuster adjuster;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that can adjust a date by the specified period.
   * <p>
   * When adjusting a date, the specified period is added to the input date.
   * The business day adjuster will then be used to ensure the result is a valid business day.
   * 
   * @param period  the period to add to the input date
   * @param additionConvention  the convention used to perform the addition
   * @param adjuster  the business day adjuster to apply to the result of the addition
   * @return the period adjuster
   */
  public static PeriodAdjuster of(
      Period period, PeriodAdditionConvention additionConvention, BusinessDayAdjuster adjuster) {
    return new PeriodAdjuster(period, additionConvention, adjuster);
  }

  /**
   * Obtains an instance that can adjust a date by the specified period using the
   * last day of month convention.
   * <p>
   * When adjusting a date, the specified period is added to the input date.
   * The business day adjuster will then be used to ensure the result is a valid business day.
   * <p>
   * The period must consist only of months and/or years.
   * 
   * @param period  the period to add to the input date
   * @param adjuster  the business day adjuster to apply to the result of the addition
   * @return the period adjuster
   */
  public static PeriodAdjuster ofLastDay(Period period, BusinessDayAdjuster adjuster) {
    return new PeriodAdjuster(period, PeriodAdditionConventions.LAST_DAY, adjuster);
  }

  /**
   * Obtains an instance that can adjust a date by the specified period using the
   * last business day of month convention.
   * <p>
   * When adjusting a date, the specified period is added to the input date.
   * The business day adjuster will then be used to ensure the result is a valid business day.
   * <p>
   * The period must consist only of months and/or years.
   * 
   * @param period  the period to add to the input date
   * @param adjuster  the business day adjuster to apply to the result of the addition
   * @return the period adjuster
   */
  public static PeriodAdjuster ofLastBusinessDay(Period period, BusinessDayAdjuster adjuster) {
    return new PeriodAdjuster(period, PeriodAdditionConventions.LAST_BUSINESS_DAY, adjuster);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (additionConvention.isMonthBased() && period.getDays() != 0) {
      throw new IllegalArgumentException("Period must not contain days when addition convention is month-based");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date, adding the period and then applying the business day adjuster.
   * <p>
   * The calculation is performed in two steps.
   * <p>
   * Step one, use {@link PeriodAdditionConvention#adjust(LocalDate, Period, HolidayCalendar)} to add the period.
   * <p>
   * Step two, use {@link BusinessDayAdjuster#adjust(LocalDate)} to adjust the result of step one.
   * 
   * @param date  the date to adjust
   * @return the adjusted date
   */
  @Override
  public LocalDate adjust(LocalDate date) {
    LocalDate unadjusted = additionConvention.adjust(date, period, adjuster.getCalendar());
    return adjuster.adjust(unadjusted);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the adjuster.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append(period);
    if (additionConvention != PeriodAdditionConventions.NONE) {
      buf.append(" with ").append(additionConvention);
    }
    if (adjuster.equals(BusinessDayAdjuster.NONE) == false) {
      buf.append(" then apply ").append(adjuster);
    }
    return buf.toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PeriodAdjuster}.
   * @return the meta-bean, not null
   */
  public static PeriodAdjuster.Meta meta() {
    return PeriodAdjuster.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PeriodAdjuster.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PeriodAdjuster.Builder builder() {
    return new PeriodAdjuster.Builder();
  }

  /**
   * Creates an instance.
   * @param period  the value of the property, not null
   * @param additionConvention  the value of the property, not null
   * @param adjuster  the value of the property, not null
   */
  PeriodAdjuster(
      Period period,
      PeriodAdditionConvention additionConvention,
      BusinessDayAdjuster adjuster) {
    JodaBeanUtils.notNull(period, "period");
    JodaBeanUtils.notNull(additionConvention, "additionConvention");
    JodaBeanUtils.notNull(adjuster, "adjuster");
    this.period = period;
    this.additionConvention = additionConvention;
    this.adjuster = adjuster;
    validate();
  }

  @Override
  public PeriodAdjuster.Meta metaBean() {
    return PeriodAdjuster.Meta.INSTANCE;
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
   * Gets the addition convention to apply.
   * <p>
   * When the adjustment is performed, this convention is used to refine the adjusted date.
   * The most common convention is to move the end date to the last business day of the month
   * if the start date is the last business day of the month.
   * @return the value of the property, not null
   */
  public PeriodAdditionConvention getAdditionConvention() {
    return additionConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjuster that is applied to the result of the addition.
   * <p>
   * This adjuster is applied to the result of the period addition calculation.
   * <p>
   * If no adjuster is required, use the 'None' business day adjuster.
   * @return the value of the property, not null
   */
  public BusinessDayAdjuster getAdjuster() {
    return adjuster;
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
      PeriodAdjuster other = (PeriodAdjuster) obj;
      return JodaBeanUtils.equal(period, other.period) &&
          JodaBeanUtils.equal(additionConvention, other.additionConvention) &&
          JodaBeanUtils.equal(adjuster, other.adjuster);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(period);
    hash = hash * 31 + JodaBeanUtils.hashCode(additionConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjuster);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PeriodAdjuster}.
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
        this, "period", PeriodAdjuster.class, Period.class);
    /**
     * The meta-property for the {@code additionConvention} property.
     */
    private final MetaProperty<PeriodAdditionConvention> additionConvention = DirectMetaProperty.ofImmutable(
        this, "additionConvention", PeriodAdjuster.class, PeriodAdditionConvention.class);
    /**
     * The meta-property for the {@code adjuster} property.
     */
    private final MetaProperty<BusinessDayAdjuster> adjuster = DirectMetaProperty.ofImmutable(
        this, "adjuster", PeriodAdjuster.class, BusinessDayAdjuster.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "period",
        "additionConvention",
        "adjuster");

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
        case 1652975501:  // additionConvention
          return additionConvention;
        case -1043751812:  // adjuster
          return adjuster;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PeriodAdjuster.Builder builder() {
      return new PeriodAdjuster.Builder();
    }

    @Override
    public Class<? extends PeriodAdjuster> beanType() {
      return PeriodAdjuster.class;
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
     * The meta-property for the {@code additionConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodAdditionConvention> additionConvention() {
      return additionConvention;
    }

    /**
     * The meta-property for the {@code adjuster} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjuster> adjuster() {
      return adjuster;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -991726143:  // period
          return ((PeriodAdjuster) bean).getPeriod();
        case 1652975501:  // additionConvention
          return ((PeriodAdjuster) bean).getAdditionConvention();
        case -1043751812:  // adjuster
          return ((PeriodAdjuster) bean).getAdjuster();
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
   * The bean-builder for {@code PeriodAdjuster}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PeriodAdjuster> {

    private Period period;
    private PeriodAdditionConvention additionConvention;
    private BusinessDayAdjuster adjuster;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PeriodAdjuster beanToCopy) {
      this.period = beanToCopy.getPeriod();
      this.additionConvention = beanToCopy.getAdditionConvention();
      this.adjuster = beanToCopy.getAdjuster();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -991726143:  // period
          return period;
        case 1652975501:  // additionConvention
          return additionConvention;
        case -1043751812:  // adjuster
          return adjuster;
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
        case 1652975501:  // additionConvention
          this.additionConvention = (PeriodAdditionConvention) newValue;
          break;
        case -1043751812:  // adjuster
          this.adjuster = (BusinessDayAdjuster) newValue;
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
    public PeriodAdjuster build() {
      return new PeriodAdjuster(
          period,
          additionConvention,
          adjuster);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the period to be added.
     * <p>
     * When the adjustment is performed, this period will be added to the input date.
     * @param period  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder period(Period period) {
      JodaBeanUtils.notNull(period, "period");
      this.period = period;
      return this;
    }

    /**
     * Sets the addition convention to apply.
     * <p>
     * When the adjustment is performed, this convention is used to refine the adjusted date.
     * The most common convention is to move the end date to the last business day of the month
     * if the start date is the last business day of the month.
     * @param additionConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder additionConvention(PeriodAdditionConvention additionConvention) {
      JodaBeanUtils.notNull(additionConvention, "additionConvention");
      this.additionConvention = additionConvention;
      return this;
    }

    /**
     * Sets the business day adjuster that is applied to the result of the addition.
     * <p>
     * This adjuster is applied to the result of the period addition calculation.
     * <p>
     * If no adjuster is required, use the 'None' business day adjuster.
     * @param adjuster  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder adjuster(BusinessDayAdjuster adjuster) {
      JodaBeanUtils.notNull(adjuster, "adjuster");
      this.adjuster = adjuster;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("PeriodAdjuster.Builder{");
      buf.append("period").append('=').append(JodaBeanUtils.toString(period)).append(',').append(' ');
      buf.append("additionConvention").append('=').append(JodaBeanUtils.toString(additionConvention)).append(',').append(' ');
      buf.append("adjuster").append('=').append(JodaBeanUtils.toString(adjuster));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
