/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.range;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.collect.ArgChecker;

/**
 * A range of local dates.
 * <p>
 * Provides a mechanism to represent a range of dates.
 * Instances can be constructed from either a half-open or a closed range of dates.
 * Internally, both are unified to a single representation.
 * <p>
 * The constants {@link LocalDate#MIN} and {@link LocalDate#MAX} can be used
 * to indicate an unbounded far-past or far-future. Note that there is no difference
 * between a half-open and a closed range when the end is {@link LocalDate#MAX}.
 * 
 * This holds a range of dates.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class LocalDateRange
    implements ImmutableBean, Serializable {

  /**
   * A range over the whole time-line.
   */
  public static final LocalDateRange ALL = new LocalDateRange(LocalDate.MIN, LocalDate.MAX);

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The start date, inclusive.
   */
  @PropertyDefinition(validate = "notNull", get = "manual")
  private final LocalDate start;
  /**
   * The end date, inclusive.
   */
  @PropertyDefinition(validate = "notNull", get = "manual")
  private final LocalDate endInclusive;

  //-------------------------------------------------------------------------
  /**
   * Obtains a half-open range of dates, including the start and excluding the end.
   * <p>
   * The range includes the start date and excludes the end date, unless the end
   * is {@link LocalDate#MAX}. The start date must be before the end date.
   * 
   * @param startInclusive  the inclusive start date, MIN_DATE treated as unbounded
   * @param endExclusive  the exclusive end date, MAX_DATE treated as unbounded
   * @return the half-open range
   */
  public static LocalDateRange halfOpen(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startDate");
    ArgChecker.notNull(endExclusive, "endExclusive");
    LocalDate endInclusive = (endExclusive.isBefore(LocalDate.MAX) ? endExclusive.minusDays(1) : endExclusive);
    return new LocalDateRange(startInclusive, endInclusive);
  }

  /**
   * Obtains a closed range of dates, including the start and end.
   * <p>
   * The range includes the start date and the end date.
   * The start date must be equal to or before the end date.
   * 
   * @param startInclusive  the inclusive start date, MIN_DATE treated as unbounded
   * @param endInclusive  the inclusive end date, MAX_DATE treated as unbounded
   * @return the closed range
   */
  public static LocalDateRange closed(LocalDate startInclusive, LocalDate endInclusive) {
    ArgChecker.notNull(startInclusive, "startDate");
    ArgChecker.notNull(endInclusive, "endExclusive");
    return new LocalDateRange(startInclusive, endInclusive);
  }

  /**
   * Obtains a range consisting of a single date.
   * <p>
   * This is equivalent to calling {@link #closed(LocalDate, LocalDate)} with
   * the single date passed as the start and end.
   * 
   * @param singleDate  the single date in the range, must not be the MIN or MAX date
   * @return the single date range
   */
  public static LocalDateRange single(LocalDate singleDate) {
    ArgChecker.notNull(singleDate, "singleDate");
    return new LocalDateRange(singleDate, singleDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Validates that the end is not before the start.
   */
  @ImmutableValidator
  private void validate() {
    if (endInclusive.isBefore(start)) {
      throw new IllegalArgumentException("Start date must be on or after end date");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date, inclusive.
   * <p>
   * This will return {@link LocalDate#MIN} if the range is unbounded at the
   * start, including all dates into the far-past.
   * 
   * @return the start date
   */
  public LocalDate getStart() {
    return start;
  }

  /**
   * Gets the end date, inclusive.
   * <p>
   * This will return {@link LocalDate#MAX} if the range is unbounded at the
   * end, including all dates into the far-future.
   * 
   * @return the end date
   */
  public LocalDate getEndInclusive() {
    return endInclusive;
  }

  /**
   * Gets the end date, exclusive.
   * <p>
   * This will return {@link LocalDate#MAX} if the range is unbounded at the
   * end, including all dates into the far-future.
   * 
   * @return the end date
   */
  public LocalDate getEndExclusive() {
    if (isUnboundedEnd()) {
      return endInclusive;
    }
    return endInclusive.plusDays(1);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the start date is unbounded.
   * 
   * @return true if start is unbounded
   */
  public boolean isUnboundedStart() {
    return start.equals(LocalDate.MIN);
  }

  /**
   * Checks if the end date is unbounded.
   * 
   * @return true if end is unbounded
   */
  public boolean isUnboundedEnd() {
    return endInclusive.equals(LocalDate.MAX);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this range with the start date adjusted.
   * <p>
   * This returns a new instance with the start date altered.
   * Since {@code LocalDate} implements {@code TemporalAdjuster} any
   * local date can simply be passed in.
   * <p>
   * For example, to adjust the start to one week earlier:
   * <pre>
   *  range = range.withStart(date -> date.minus(1, ChronoUnit.WEEKS));
   * </pre>
   * 
   * @param adjuster  the adjuster to use
   * @return a copy of this range with the start date adjusted
   */
  public LocalDateRange withStart(TemporalAdjuster adjuster) {
    ArgChecker.notNull(adjuster, "adjuster");
    return LocalDateRange.closed(start.with(adjuster), endInclusive);
  }

  /**
   * Returns a copy of this range with the end date adjusted.
   * <p>
   * This returns a new instance with the end date altered.
   * Since {@code LocalDate} implements {@code TemporalAdjuster} any
   * local date can simply be passed in.
   * <p>
   * For example, to adjust the end to one week later:
   * <pre>
   *  range = range.withEndInclusive(date -> date.plus(1, ChronoUnit.WEEKS));
   * </pre>
   * 
   * @param adjuster  the adjuster to use
   * @return a copy of this range with the end date adjusted
   */
  public LocalDateRange withEndInclusive(TemporalAdjuster adjuster) {
    ArgChecker.notNull(adjuster, "adjuster");
    return LocalDateRange.closed(start, endInclusive.with(adjuster));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range contains the specified date.
   * <p>
   * If this range has an unbounded start then {@code contains(LocalDate#MIN)} returns true.
   * If this range has an unbounded end then {@code contains(LocalDate#MAX)} returns true.
   * 
   * @param date  the date to check for
   * @return true if this range contains the date
   */
  public boolean contains(LocalDate date) {
    ArgChecker.notNull(date, "date");
    // start <= date && date <= end
    return start.compareTo(date) <= 0 && date.compareTo(endInclusive) <= 0;
  }

  /**
   * Checks if this range contains all dates in the specified range.
   * <p>
   * This checks that the start date of this range is before or equal the specified
   * start date and the end date of this range is before or equal the specified end date.
   * 
   * @param other  the other range to check for
   * @return true if this range contains all dates in the other range
   */
  public boolean encloses(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // start <= other.start && other.end <= end
    return start.compareTo(other.start) <= 0 && other.endInclusive.compareTo(endInclusive) <= 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range is entirely before the specified range.
   * 
   * @param other  the other range to check for
   * @return true if every date in this range is before every date in the other range
   */
  public boolean isBefore(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // end < other.start
    return endInclusive.isBefore(other.start);
  }

  /**
   * Checks if this range is entirely after the specified range.
   * 
   * @param other  the other range to check for
   * @return true if every date in this range is after every date in the other range
   */
  public boolean isAfter(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // start > other.end
    return start.isAfter(other.endInclusive);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range equals another.
   * 
   * @param obj  the other object
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof LocalDateRange) {
      LocalDateRange other = (LocalDateRange) obj;
      return start.equals(other.start) && endInclusive.equals(other.endInclusive);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return start.hashCode() ^ endInclusive.hashCode();
  }

  /**
   * Returns this range as a string, such as {@code [2009-12-03,2014-06-30]}.
   * <p>
   * The string will be one of these formats:<br />
   *  {@code [2009-12-03,2014-06-30]}<br />
   *  {@code [2009-12-03,+INFINITY]} - if the end is unbounded<br />
   *  {@code [-INFINITY,2014-06-30]} - if the start is unbounded<br />
   *  {@code [-INFINITY,+INFINITY]} - if the start and end are unbounded<br />
   *
   * @return the standard string
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(23);
    if (isUnboundedStart()) {
      buf.append("[-INFINITY,");
    } else {
      buf.append('[').append(start).append(',');
    }
    if (isUnboundedEnd()) {
      buf.append("+INFINITY]");
    } else {
      buf.append(endInclusive).append(']');
    }
    return buf.toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LocalDateRange}.
   * @return the meta-bean, not null
   */
  public static LocalDateRange.Meta meta() {
    return LocalDateRange.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(LocalDateRange.Meta.INSTANCE);
  }

  private LocalDateRange(
      LocalDate start,
      LocalDate endInclusive) {
    JodaBeanUtils.notNull(start, "start");
    JodaBeanUtils.notNull(endInclusive, "endInclusive");
    this.start = start;
    this.endInclusive = endInclusive;
    validate();
  }

  @Override
  public LocalDateRange.Meta metaBean() {
    return LocalDateRange.Meta.INSTANCE;
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
   * The meta-bean for {@code LocalDateRange}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code start} property.
     */
    private final MetaProperty<LocalDate> start = DirectMetaProperty.ofImmutable(
        this, "start", LocalDateRange.class, LocalDate.class);
    /**
     * The meta-property for the {@code endInclusive} property.
     */
    private final MetaProperty<LocalDate> endInclusive = DirectMetaProperty.ofImmutable(
        this, "endInclusive", LocalDateRange.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "start",
        "endInclusive");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 109757538:  // start
          return start;
        case -1907681455:  // endInclusive
          return endInclusive;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends LocalDateRange> builder() {
      return new LocalDateRange.Builder();
    }

    @Override
    public Class<? extends LocalDateRange> beanType() {
      return LocalDateRange.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code start} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> start() {
      return start;
    }

    /**
     * The meta-property for the {@code endInclusive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endInclusive() {
      return endInclusive;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 109757538:  // start
          return ((LocalDateRange) bean).getStart();
        case -1907681455:  // endInclusive
          return ((LocalDateRange) bean).getEndInclusive();
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
   * The bean-builder for {@code LocalDateRange}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<LocalDateRange> {

    private LocalDate start;
    private LocalDate endInclusive;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 109757538:  // start
          return start;
        case -1907681455:  // endInclusive
          return endInclusive;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 109757538:  // start
          this.start = (LocalDate) newValue;
          break;
        case -1907681455:  // endInclusive
          this.endInclusive = (LocalDate) newValue;
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
    public LocalDateRange build() {
      return new LocalDateRange(
          start,
          endInclusive);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("LocalDateRange.Builder{");
      buf.append("start").append('=').append(JodaBeanUtils.toString(start)).append(',').append(' ');
      buf.append("endInclusive").append('=').append(JodaBeanUtils.toString(endInclusive));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
