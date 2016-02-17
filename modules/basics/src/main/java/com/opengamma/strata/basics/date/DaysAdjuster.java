/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.strata.basics.market.ReferenceData;

/**
 * An adjuster that alters a date by adding a period of days,
 * resolved to specific holiday calendars.
 * <p>
 * This is the resolved form of {@link DaysAdjustment} which describes the adjustment in detail.
 * Applications will typically create a {@code DaysAdjuster} from a {@code DaysAdjustment}
 * using {@link DaysAdjustment#resolve(ReferenceData)}.
 * <p>
 * A {@code DaysAdjuster} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition(constructorScope = "package")
public final class DaysAdjuster
    implements ImmutableBean, DateAdjuster, Serializable {

  /**
   * An instance that performs no adjustment.
   */
  public static final DaysAdjuster NONE =
      new DaysAdjuster(0, HolidayCalendars.NO_HOLIDAYS, BusinessDayAdjuster.NONE);

  /**
   * The number of days to be added.
   * <p>
   * When the adjustment is performed, this amount will be added to the input date
   * using the calendar to determine the addition type.
   */
  @PropertyDefinition(validate = "notNull")
  private final int days;
  /**
   * The holiday calendar that defines the meaning of a day when performing the addition.
   * <p>
   * When the adjustment is performed, this calendar is used to determine which days are business days.
   * <p>
   * If the holiday calendar is 'None' then addition uses simple date addition arithmetic without
   * considering any days as holidays or weekends.
   * If the holiday calendar is anything other than 'None' then addition uses that calendar,
   * effectively repeatedly finding the next business day.
   * <p>
   * See the class-level documentation for more information.
   */
  @PropertyDefinition(validate = "notNull")
  private final HolidayCalendar calendar;
  /**
   * The business day adjuster that is applied to the result of the addition.
   * <p>
   * This adjuster is applied to the result of the period addition calculation.
   * If the addition is performed using business days then any adjustment here is expected to
   * have a different holiday calendar to that used during addition.
   * <p>
   * If no adjustment is required, use the 'None' business day adjuster.
   * <p>
   * See the class-level documentation for more information.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjuster adjuster;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that can adjust a date by a specific number of calendar days.
   * <p>
   * When adjusting a date, the specified number of calendar days is added.
   * Holidays and weekends are not taken into account in the calculation.
   * <p>
   * No business day adjuster is applied to the result of the addition.
   * 
   * @param numberOfDays  the number of days
   * @return the days adjustment
   */
  public static DaysAdjuster ofCalendarDays(int numberOfDays) {
    return new DaysAdjuster(numberOfDays, HolidayCalendars.NO_HOLIDAYS, BusinessDayAdjuster.NONE);
  }

  /**
   * Obtains an instance that can adjust a date by a specific number of calendar days.
   * <p>
   * When adjusting a date, the specified number of calendar days is added.
   * Holidays and weekends are not taken into account in the calculation.
   * <p>
   * The business day adjuster is applied to the result of the addition.
   * 
   * @param numberOfDays  the number of days
   * @param adjuster  the business day adjuster to apply to the result of the addition
   * @return the days adjustment
   */
  public static DaysAdjuster ofCalendarDays(int numberOfDays, BusinessDayAdjuster adjuster) {
    return new DaysAdjuster(numberOfDays, HolidayCalendars.NO_HOLIDAYS, adjuster);
  }

  /**
   * Obtains an instance that can adjust a date by a specific number of business days.
   * <p>
   * When adjusting a date, the specified number of business days is added.
   * This is equivalent to repeatedly finding the next business day.
   * <p>
   * No business day adjuster is applied to the result of the addition.
   * 
   * @param numberOfDays  the number of days
   * @param holidayCalendar  the calendar that defines holidays and business days
   * @return the days adjuster
   */
  public static DaysAdjuster ofBusinessDays(int numberOfDays, HolidayCalendar holidayCalendar) {
    return new DaysAdjuster(numberOfDays, holidayCalendar, BusinessDayAdjuster.NONE);
  }

  /**
   * Obtains an instance that can adjust a date by a specific number of business days.
   * <p>
   * When adjusting a date, the specified number of business days is added.
   * This is equivalent to repeatedly finding the next business day.
   * <p>
   * The business day adjuster is applied to the result of the addition.
   * 
   * @param numberOfDays  the number of days
   * @param holidayCalendar  the calendar that defines holidays and business days
   * @param adjuster  the business day adjuster to apply to the result of the addition
   * @return the days adjuster
   */
  public static DaysAdjuster ofBusinessDays(
      int numberOfDays,
      HolidayCalendar holidayCalendar,
      BusinessDayAdjuster adjuster) {
    return new DaysAdjuster(numberOfDays, holidayCalendar, adjuster);
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date, adding the period in days using the holiday calendar
   * and then applying the business day adjuster.
   * <p>
   * The calculation is performed in two steps.
   * <p>
   * Step one, use {@link HolidayCalendar#shift(LocalDate, int)} to add the number of days.
   * If the holiday calendar is 'None' this will effectively add calendar days.
   * <p>
   * Step two, use {@link BusinessDayAdjuster#adjust(LocalDate)} to adjust the result of step one.
   * 
   * @param date  the date to adjust
   * @return the adjusted date
   */
  @Override
  public LocalDate adjust(LocalDate date) {
    LocalDate added = calendar.shift(date, days);
    return adjuster.adjust(added);
  }

  /**
   * Gets the holiday calendar that will be applied to the result.
   * <p>
   * This adjustment may contain more than one holiday calendar.
   * This method returns the calendar used last.
   * As such, the adjusted date will always be valid according to this calendar.
   * 
   * @return the result holiday calendar
   */
  public HolidayCalendar getResultCalendar() {
    HolidayCalendar cal = adjuster.getCalendar();
    if (cal == HolidayCalendars.NO_HOLIDAYS) {
      cal = calendar;
    }
    return cal;
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
    buf.append(days);
    if (calendar == HolidayCalendars.NO_HOLIDAYS) {
      buf.append(" calendar day");
      if (days != 1) {
        buf.append("s");
      }
    } else {
      buf.append(" business day");
      if (days != 1) {
        buf.append("s");
      }
      buf.append(" using calendar ").append(calendar);
    }
    if (adjuster.equals(BusinessDayAdjuster.NONE) == false) {
      buf.append(" then apply ").append(adjuster);
    }
    return buf.toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DaysAdjuster}.
   * @return the meta-bean, not null
   */
  public static DaysAdjuster.Meta meta() {
    return DaysAdjuster.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DaysAdjuster.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DaysAdjuster.Builder builder() {
    return new DaysAdjuster.Builder();
  }

  /**
   * Creates an instance.
   * @param days  the value of the property, not null
   * @param calendar  the value of the property, not null
   * @param adjuster  the value of the property, not null
   */
  DaysAdjuster(
      int days,
      HolidayCalendar calendar,
      BusinessDayAdjuster adjuster) {
    JodaBeanUtils.notNull(days, "days");
    JodaBeanUtils.notNull(calendar, "calendar");
    JodaBeanUtils.notNull(adjuster, "adjuster");
    this.days = days;
    this.calendar = calendar;
    this.adjuster = adjuster;
  }

  @Override
  public DaysAdjuster.Meta metaBean() {
    return DaysAdjuster.Meta.INSTANCE;
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
   * Gets the number of days to be added.
   * <p>
   * When the adjustment is performed, this amount will be added to the input date
   * using the calendar to determine the addition type.
   * @return the value of the property, not null
   */
  public int getDays() {
    return days;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the holiday calendar that defines the meaning of a day when performing the addition.
   * <p>
   * When the adjustment is performed, this calendar is used to determine which days are business days.
   * <p>
   * If the holiday calendar is 'None' then addition uses simple date addition arithmetic without
   * considering any days as holidays or weekends.
   * If the holiday calendar is anything other than 'None' then addition uses that calendar,
   * effectively repeatedly finding the next business day.
   * <p>
   * See the class-level documentation for more information.
   * @return the value of the property, not null
   */
  public HolidayCalendar getCalendar() {
    return calendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjuster that is applied to the result of the addition.
   * <p>
   * This adjuster is applied to the result of the period addition calculation.
   * If the addition is performed using business days then any adjustment here is expected to
   * have a different holiday calendar to that used during addition.
   * <p>
   * If no adjustment is required, use the 'None' business day adjuster.
   * <p>
   * See the class-level documentation for more information.
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
      DaysAdjuster other = (DaysAdjuster) obj;
      return (days == other.days) &&
          JodaBeanUtils.equal(calendar, other.calendar) &&
          JodaBeanUtils.equal(adjuster, other.adjuster);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(days);
    hash = hash * 31 + JodaBeanUtils.hashCode(calendar);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjuster);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DaysAdjuster}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code days} property.
     */
    private final MetaProperty<Integer> days = DirectMetaProperty.ofImmutable(
        this, "days", DaysAdjuster.class, Integer.TYPE);
    /**
     * The meta-property for the {@code calendar} property.
     */
    private final MetaProperty<HolidayCalendar> calendar = DirectMetaProperty.ofImmutable(
        this, "calendar", DaysAdjuster.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code adjuster} property.
     */
    private final MetaProperty<BusinessDayAdjuster> adjuster = DirectMetaProperty.ofImmutable(
        this, "adjuster", DaysAdjuster.class, BusinessDayAdjuster.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "days",
        "calendar",
        "adjuster");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3076183:  // days
          return days;
        case -178324674:  // calendar
          return calendar;
        case -1043751812:  // adjuster
          return adjuster;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public DaysAdjuster.Builder builder() {
      return new DaysAdjuster.Builder();
    }

    @Override
    public Class<? extends DaysAdjuster> beanType() {
      return DaysAdjuster.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code days} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> days() {
      return days;
    }

    /**
     * The meta-property for the {@code calendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> calendar() {
      return calendar;
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
        case 3076183:  // days
          return ((DaysAdjuster) bean).getDays();
        case -178324674:  // calendar
          return ((DaysAdjuster) bean).getCalendar();
        case -1043751812:  // adjuster
          return ((DaysAdjuster) bean).getAdjuster();
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
   * The bean-builder for {@code DaysAdjuster}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<DaysAdjuster> {

    private int days;
    private HolidayCalendar calendar;
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
    private Builder(DaysAdjuster beanToCopy) {
      this.days = beanToCopy.getDays();
      this.calendar = beanToCopy.getCalendar();
      this.adjuster = beanToCopy.getAdjuster();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3076183:  // days
          return days;
        case -178324674:  // calendar
          return calendar;
        case -1043751812:  // adjuster
          return adjuster;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3076183:  // days
          this.days = (Integer) newValue;
          break;
        case -178324674:  // calendar
          this.calendar = (HolidayCalendar) newValue;
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
    public DaysAdjuster build() {
      return new DaysAdjuster(
          days,
          calendar,
          adjuster);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the number of days to be added.
     * <p>
     * When the adjustment is performed, this amount will be added to the input date
     * using the calendar to determine the addition type.
     * @param days  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder days(int days) {
      JodaBeanUtils.notNull(days, "days");
      this.days = days;
      return this;
    }

    /**
     * Sets the holiday calendar that defines the meaning of a day when performing the addition.
     * <p>
     * When the adjustment is performed, this calendar is used to determine which days are business days.
     * <p>
     * If the holiday calendar is 'None' then addition uses simple date addition arithmetic without
     * considering any days as holidays or weekends.
     * If the holiday calendar is anything other than 'None' then addition uses that calendar,
     * effectively repeatedly finding the next business day.
     * <p>
     * See the class-level documentation for more information.
     * @param calendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calendar(HolidayCalendar calendar) {
      JodaBeanUtils.notNull(calendar, "calendar");
      this.calendar = calendar;
      return this;
    }

    /**
     * Sets the business day adjuster that is applied to the result of the addition.
     * <p>
     * This adjuster is applied to the result of the period addition calculation.
     * If the addition is performed using business days then any adjustment here is expected to
     * have a different holiday calendar to that used during addition.
     * <p>
     * If no adjustment is required, use the 'None' business day adjuster.
     * <p>
     * See the class-level documentation for more information.
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
      buf.append("DaysAdjuster.Builder{");
      buf.append("days").append('=').append(JodaBeanUtils.toString(days)).append(',').append(' ');
      buf.append("calendar").append('=').append(JodaBeanUtils.toString(calendar)).append(',').append(' ');
      buf.append("adjuster").append('=').append(JodaBeanUtils.toString(adjuster));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
