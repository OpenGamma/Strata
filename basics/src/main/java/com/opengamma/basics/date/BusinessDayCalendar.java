/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.range.LocalDateRange;

/**
 * A pre-calculated set of dates classifying business days, weekends and holidays.
 * <p>
 * Many calculations in finance require knowledge of whether a date is a business day or not.
 * Non business days can be specified as either weekends or holidays.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class BusinessDayCalendar
    implements ImmutableBean, Serializable {

  /**
   * An instance declaring all days as business days.
   * This instance has no holidays and no weekends.
   */
  public static final BusinessDayCalendar ALL = new BusinessDayCalendar(ImmutableSet.of());
  /**
   * An instance declaring all days as business days except Saturday/Sunday weekends.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  public static final BusinessDayCalendar WEEKENDS = new BusinessDayCalendar(ImmutableSet.of(SATURDAY, SUNDAY));

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The supported range of dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDateRange range;
  /**
   * The set of holiday dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSortedSet<LocalDate> holidays;
  /**
   * The set of weekend days.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<DayOfWeek> weekendDays;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code BusinessDayCalendar} from a set of holiday dates.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may both be the same.
   * 
   * @param holidays  the set of holiday dates
   * @param firstWeekendDay  the first weekend day
   * @param secondWeekendDay  the second weekend day, may be same as first
   * @return the business days instance
   */
  public static BusinessDayCalendar of(Iterable<LocalDate> holidays, DayOfWeek firstWeekendDay, DayOfWeek secondWeekendDay) {
    ArgChecker.noNulls(holidays, "holidays");
    ArgChecker.notNull(firstWeekendDay, "firstWeekendDay");
    ArgChecker.notNull(secondWeekendDay, "secondWeekendDay");
    return new BusinessDayCalendar(
        ImmutableSortedSet.copyOf(holidays),
        Sets.immutableEnumSet(firstWeekendDay, secondWeekendDay));
  }

  /**
   * Obtains a {@code BusinessDayCalendar} from a set of holiday dates, using Saturday
   * and Sunday as the weekend days.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may be empty, in which case the holiday dates should contain any weekends.
   * 
   * @param holidays  the set of holiday dates
   * @param weekendDays  the days that define the weekend, if empty then weekends are treated as business days
   * @return the business days instance
   */
  public static BusinessDayCalendar of(Iterable<LocalDate> holidays, Iterable<DayOfWeek> weekendDays) {
    ArgChecker.noNulls(holidays, "holidays");
    ArgChecker.noNulls(weekendDays, "weekendDays");
    return new BusinessDayCalendar(ImmutableSortedSet.copyOf(holidays), Sets.immutableEnumSet(weekendDays));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance with no specific holiday dates.
   * 
   * @param weekendDays  the set of weekend days, validated non-null
   */
  private BusinessDayCalendar(ImmutableSet<DayOfWeek> weekendDays) {
    this.range = LocalDateRange.ALL;
    this.holidays = ImmutableSortedSet.of();
    this.weekendDays = weekendDays;
  }

  /**
   * Creates an instance calculating the supported range.
   * 
   * @param holidays  the set of holidays, validated non-null
   * @param weekendDays  the set of weekend days, validated non-null
   */
  private BusinessDayCalendar(ImmutableSortedSet<LocalDate> holidays, ImmutableSet<DayOfWeek> weekendDays) {
    this.range = LocalDateRange.closed(
        holidays.first().with(TemporalAdjusters.firstDayOfYear()),
        holidays.last().with(TemporalAdjusters.lastDayOfYear()));
    this.holidays = holidays;
    this.weekendDays = weekendDays;
  }

  @ImmutableValidator
  private void validate() {
    if (holidays.isEmpty()) {
      if (range.equals(LocalDateRange.ALL) == false) {
        throw new IllegalArgumentException("Range must be ALL if holiday set is empty");
      }
    } else {
      if (range.contains(holidays.first()) == false || range.contains(holidays.last()) == false) {
        throw new IllegalArgumentException("All holidays must be within the supported range");
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the specified date is a business day.
   * 
   * @param date  the date to check
   * @return true if the specified date is a business day
   */
  public boolean isBusinessDay(LocalDate date) {
    return !isNonBusinessDay(date);
  }

  /**
   * Checks if the specified date is a holiday.
   * 
   * @param date  the date to check
   * @return true if the specified date is a holiday
   */
  public boolean isNonBusinessDay(LocalDate date) {
    ArgChecker.notNull(date, "date");
    if (range.contains(date) == false) {
      throw new IllegalArgumentException("Date is not within the range of known holidays: " + date + ", " + range);
    }
    return holidays.contains(date) || weekendDays.contains(date.getDayOfWeek());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an adjuster that changes the date.
   * <p>
   * The adjuster is intended to be used with the method {@link Temporal#with(TemporalAdjuster)}.
   * For example:
   * <pre>
   * threeDaysLater = date.with(businessDays.adjustBy(3));
   * twoDaysEarlier = date.with(businessDays.adjustBy(-2));
   * </pre>
   * 
   * @param amount  the number of business days to adjust by
   * @return the first business day after this one
   */
  public TemporalAdjuster adjustBy(int amount) {
    return TemporalAdjusters.ofDateAdjuster(date -> shift(date, amount));
  }

  //-------------------------------------------------------------------------
  /**
   * Shifts the date by the specified number of business days.
   * <p>
   * If the amount is positive, later business days are chosen.
   * If the amount is negative, earlier business days are chosen.
   * 
   * @param date  the date to adjust
   * @param amount  the number of business days to adjust by
   * @return the shifted date
   */
  public LocalDate shift(LocalDate date, int amount) {
    ArgChecker.notNull(date, "date");
    LocalDate adjusted = date;
    if (amount > 0) {
      for (int i = 0; i < amount; i++) {
        adjusted = next(adjusted);
      }
    } else if (amount < 0) {
      for (int i = 0; i > amount; i--) {
        adjusted = previous(adjusted);
      }
    }
    return adjusted;
  }

  /**
   * Finds the next business day.
   * <p>
   * Given a date, this method returns the next business day.
   * 
   * @param date  the date to adjust
   * @return the first business day after this one
   */
  public LocalDate next(LocalDate date) {
    ArgChecker.notNull(date, "date");
    LocalDate next = date.plusDays(1);
    return isNonBusinessDay(next) ? next(next) : next;
  }

  /**
   * Finds the previous business day.
   * <p>
   * Given a date, this method returns the previous business day.
   * 
   * @param date  the date to adjust
   * @return the first business day before this one
   */
  public LocalDate previous(LocalDate date) {
    ArgChecker.notNull(date, "date");
    LocalDate previous = date.minusDays(1);
    return isNonBusinessDay(previous) ? previous(previous) : previous;
  }

  /**
   * Ensures that the returned date is a business day.
   * <p>
   * This takes the input date and ensures it is a business day.
   * If it is not a business day, then the convention is used to adjust it.
   * 
   * @param date  the date to check
   * @param convention  the convention to use to locate a business day
   * @return the first business day before this one
   */
  public LocalDate ensure(LocalDate date, BusinessDayConvention convention) {
    ArgChecker.notNull(date, "date");
    ArgChecker.notNull(convention, "convention");
    return convention.adjust(date, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number of business days between two dates.
   * <p>
   * This calculates the number of business days within the range.
   * If the dates are equal, zero is returned.
   * If the end is before the start, an exception is thrown.
   * 
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the total number of business days between the start and end date
   */
  public int daysBetween(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startInclusive");
    ArgChecker.notNull(endExclusive, "endExclusive");
    if (startInclusive.equals(endExclusive)) {
      return 0;
    }
    return daysBetween(LocalDateRange.halfOpen(startInclusive, endExclusive));
  }

  /**
   * Calculates the number of business days in a date range.
   * <p>
   * This calculates the number of business days within the range.
   * 
   * @param dateRange  the date range to calculate business days for
   * @return the total number of business days between the start and end date
   */
  public int daysBetween(LocalDateRange dateRange) {
    ArgChecker.notNull(dateRange, "dateRange");
    return Math.toIntExact(dateRange.stream()
        .filter(this::isBusinessDay)
        .count());
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this definition of business days with another.
   * <p>
   * The combined business days are formed by merging the set of holidays and weekend days.
   * If the supported ranges do not match the range will be reduced to that
   * specified by both objects. If there is no overlap, then an exception is thrown.
   * 
   * @param other  the other business days
   * @return the combined set of business days
   */
  public BusinessDayCalendar combineWith(BusinessDayCalendar other) {
    ArgChecker.notNull(other, "other");
    if (this.equals(other)) {
      return this;
    }
    LocalDateRange newRange = range.union(other.range);  // exception if no overlap
    ImmutableSortedSet<LocalDate> newHolidays =
        ImmutableSortedSet.copyOf(Iterables.concat(holidays, other.holidays))
            .subSet(newRange.getStart(), newRange.getEndExclusive());
    ImmutableSet<DayOfWeek> newWeekends = ImmutableSet.copyOf(Iterables.concat(weekendDays, other.weekendDays));
    return new BusinessDayCalendar(newHolidays, newWeekends);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the amount as a string.
   * 
   * @return a description of the business days
   */
  @Override
  public String toString() {
    return "BusinessDayCalendar" + range;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BusinessDayCalendar}.
   * @return the meta-bean, not null
   */
  public static BusinessDayCalendar.Meta meta() {
    return BusinessDayCalendar.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BusinessDayCalendar.Meta.INSTANCE);
  }

  private BusinessDayCalendar(
      LocalDateRange range,
      SortedSet<LocalDate> holidays,
      Set<DayOfWeek> weekendDays) {
    JodaBeanUtils.notNull(range, "range");
    JodaBeanUtils.notNull(holidays, "holidays");
    JodaBeanUtils.notNull(weekendDays, "weekendDays");
    this.range = range;
    this.holidays = ImmutableSortedSet.copyOfSorted(holidays);
    this.weekendDays = ImmutableSet.copyOf(weekendDays);
    validate();
  }

  @Override
  public BusinessDayCalendar.Meta metaBean() {
    return BusinessDayCalendar.Meta.INSTANCE;
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
   * Gets the supported range of dates.
   * @return the value of the property, not null
   */
  public LocalDateRange getRange() {
    return range;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of holiday dates.
   * @return the value of the property, not null
   */
  public ImmutableSortedSet<LocalDate> getHolidays() {
    return holidays;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of weekend days.
   * @return the value of the property, not null
   */
  public ImmutableSet<DayOfWeek> getWeekendDays() {
    return weekendDays;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BusinessDayCalendar other = (BusinessDayCalendar) obj;
      return JodaBeanUtils.equal(getRange(), other.getRange()) &&
          JodaBeanUtils.equal(getHolidays(), other.getHolidays()) &&
          JodaBeanUtils.equal(getWeekendDays(), other.getWeekendDays());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getRange());
    hash += hash * 31 + JodaBeanUtils.hashCode(getHolidays());
    hash += hash * 31 + JodaBeanUtils.hashCode(getWeekendDays());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BusinessDayCalendar}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code range} property.
     */
    private final MetaProperty<LocalDateRange> range = DirectMetaProperty.ofImmutable(
        this, "range", BusinessDayCalendar.class, LocalDateRange.class);
    /**
     * The meta-property for the {@code holidays} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSortedSet<LocalDate>> holidays = DirectMetaProperty.ofImmutable(
        this, "holidays", BusinessDayCalendar.class, (Class) ImmutableSortedSet.class);
    /**
     * The meta-property for the {@code weekendDays} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<DayOfWeek>> weekendDays = DirectMetaProperty.ofImmutable(
        this, "weekendDays", BusinessDayCalendar.class, (Class) ImmutableSet.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "range",
        "holidays",
        "weekendDays");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 108280125:  // range
          return range;
        case -510663909:  // holidays
          return holidays;
        case 563236190:  // weekendDays
          return weekendDays;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BusinessDayCalendar> builder() {
      return new BusinessDayCalendar.Builder();
    }

    @Override
    public Class<? extends BusinessDayCalendar> beanType() {
      return BusinessDayCalendar.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code range} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateRange> range() {
      return range;
    }

    /**
     * The meta-property for the {@code holidays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSortedSet<LocalDate>> holidays() {
      return holidays;
    }

    /**
     * The meta-property for the {@code weekendDays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<DayOfWeek>> weekendDays() {
      return weekendDays;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 108280125:  // range
          return ((BusinessDayCalendar) bean).getRange();
        case -510663909:  // holidays
          return ((BusinessDayCalendar) bean).getHolidays();
        case 563236190:  // weekendDays
          return ((BusinessDayCalendar) bean).getWeekendDays();
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
   * The bean-builder for {@code BusinessDayCalendar}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<BusinessDayCalendar> {

    private LocalDateRange range;
    private SortedSet<LocalDate> holidays = new TreeSet<LocalDate>();
    private Set<DayOfWeek> weekendDays = new HashSet<DayOfWeek>();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 108280125:  // range
          return range;
        case -510663909:  // holidays
          return holidays;
        case 563236190:  // weekendDays
          return weekendDays;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 108280125:  // range
          this.range = (LocalDateRange) newValue;
          break;
        case -510663909:  // holidays
          this.holidays = (SortedSet<LocalDate>) newValue;
          break;
        case 563236190:  // weekendDays
          this.weekendDays = (Set<DayOfWeek>) newValue;
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
    public BusinessDayCalendar build() {
      return new BusinessDayCalendar(
          range,
          holidays,
          weekendDays);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("BusinessDayCalendar.Builder{");
      buf.append("range").append('=').append(JodaBeanUtils.toString(range)).append(',').append(' ');
      buf.append("holidays").append('=').append(JodaBeanUtils.toString(holidays)).append(',').append(' ');
      buf.append("weekendDays").append('=').append(JodaBeanUtils.toString(weekendDays));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
