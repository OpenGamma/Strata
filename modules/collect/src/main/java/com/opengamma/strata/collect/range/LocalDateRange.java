/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.range;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

import com.opengamma.strata.collect.ArgChecker;

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

  /**
   * The start date, inclusive.
   */
  @PropertyDefinition(validate = "notNull", get = "manual")
  private final LocalDate start;
  /**
   * The end date, exclusive.
   */
  @PropertyDefinition(validate = "notNull", get = "manual")
  private final LocalDate endExclusive;

  //-------------------------------------------------------------------------
  /**
   * Obtains a half-open range of dates, including the start and excluding the end.
   * <p>
   * The range includes the start date and excludes the end date, unless the end
   * is {@link LocalDate#MAX}.
   * The end date must be equal to or after the start date.
   * This definition permits an empty range located at a specific date.
   * 
   * @param startInclusive  the inclusive start date, MIN_DATE treated as unbounded
   * @param endExclusive  the exclusive end date, MAX_DATE treated as unbounded
   * @return the half-open range
   * @throws IllegalArgumentException if the end date is before the start date
   */
  public static LocalDateRange of(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startDate");
    ArgChecker.notNull(endExclusive, "endExclusive");
    return new LocalDateRange(startInclusive, endExclusive);
  }

  /**
   * Obtains a closed range of dates, including the start and end.
   * <p>
   * The range includes the start date and the end date.
   * The end date must be equal to or after the start date.
   * 
   * @param startInclusive  the inclusive start date, MIN_DATE treated as unbounded
   * @param endInclusive  the inclusive end date, MAX_DATE treated as unbounded
   * @return the closed range
   * @throws IllegalArgumentException if the end date is before the start date
   */
  public static LocalDateRange ofClosed(LocalDate startInclusive, LocalDate endInclusive) {
    ArgChecker.notNull(startInclusive, "startDate");
    ArgChecker.notNull(endInclusive, "endExclusive");
    if (endInclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException("End date must not be before start date");
    }
    LocalDate endExclusive = (endInclusive.equals(LocalDate.MAX) ? LocalDate.MAX : endInclusive.plusDays(1));
    return new LocalDateRange(startInclusive, endExclusive);
  }

  //-------------------------------------------------------------------------
  /**
   * Validates that the end is not before the start.
   */
  @ImmutableValidator
  private void validate() {
    if (endExclusive.isBefore(start)) {
      throw new IllegalArgumentException("End date must not be before start date");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date, inclusive.
   * <p>
   * This will return {@link LocalDate#MIN} if the range is unbounded at the start.
   * In this case, the range includes all dates into the far-past.
   * 
   * @return the start date
   */
  public LocalDate getStart() {
    return start;
  }

  /**
   * Gets the end date, exclusive.
   * <p>
   * This will return {@link LocalDate#MAX} if the range is unbounded at the end.
   * In this case, the range includes all dates into the far-future.
   * 
   * @return the end date, exclusive
   */
  public LocalDate getEndExclusive() {
    return endExclusive;
  }

  /**
   * Gets the end date, inclusive.
   * <p>
   * This will return {@link LocalDate#MAX} if the range is unbounded at the end.
   * In this case, the range includes all dates into the far-future.
   * 
   * @return the end date, inclusive
   */
  public LocalDate getEndInclusive() {
    if (isUnboundedEnd()) {
      return LocalDate.MAX;
    }
    return endExclusive.minusDays(1);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the range is empty.
   * 
   * @return true if the range is empty
   */
  public boolean isEmpty() {
    return start.equals(endExclusive);
  }

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
    return endExclusive.equals(LocalDate.MAX);
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
   * @throws IllegalArgumentException if the new start date is after the current end date
   */
  public LocalDateRange withStart(TemporalAdjuster adjuster) {
    ArgChecker.notNull(adjuster, "adjuster");
    return LocalDateRange.of(start.with(adjuster), endExclusive);
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
   *  range = range.withEndExclusive(date -> date.plus(1, ChronoUnit.WEEKS));
   * </pre>
   * 
   * @param adjuster  the adjuster to use
   * @return a copy of this range with the end date adjusted
   * @throws IllegalArgumentException if the new end date is before the current start date
   */
  public LocalDateRange withEndExclusive(TemporalAdjuster adjuster) {
    ArgChecker.notNull(adjuster, "adjuster");
    return LocalDateRange.of(start, endExclusive.with(adjuster));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range contains the specified date.
   * <p>
   * If this range has an unbounded start then {@code contains(LocalDate#MIN)} returns true.
   * If this range has an unbounded end then {@code contains(LocalDate#MAX)} returns true.
   * If this range is empty then this method always returns false.
   * 
   * @param date  the date to check for
   * @return true if this range contains the date
   */
  public boolean contains(LocalDate date) {
    ArgChecker.notNull(date, "date");
    // start <= date && date < endExclusive
    return start.compareTo(date) <= 0 && (date.compareTo(endExclusive) < 0 || isUnboundedEnd());
  }

  /**
   * Checks if this range contains all dates in the specified range.
   * <p>
   * This checks that the start date of this range is before or equal the specified start date,
   * and the end date of this range is after or equal the specified end date.
   * If this range is empty then it only encloses an equal range.
   * 
   * @param other  the other range to check for
   * @return true if this range contains all dates in the other range
   */
  public boolean encloses(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // start <= other.start && endExclusive >= other.endExclusive
    return start.compareTo(other.start) <= 0 && endExclusive.compareTo(other.endExclusive) >= 0;
  }

  /**
   * Checks if this range overlaps any dates in the specified range.
   * <p>
   * This checks that the two ranges overlap.
   * An empty range overlaps at dates where any date is in common.
   * Thus [2014-06-20,2014-06-25) overlaps both [2014-06-20,2014-06-20) and [2014-06-25,2014-06-25).
   * 
   * @param other  the other range to check for
   * @return true if this range contains all dates in the other range
   */
  public boolean overlaps(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // start <= other.endExclusive && endExclusive >= other.start
    return start.compareTo(other.endExclusive) <= 0 && endExclusive.compareTo(other.start) >= 0;
  }

  /**
   * Calculates the range that is the intersection of this range and the specified range.
   * <p>
   * This finds the intersection of two ranges.
   * This returns an exception if the two ranges do not {@linkplain #overlaps(LocalDateRange) overlap}.
   * <p>
   * If the two ranges are adjacent but have no whole dates in common, an empty range is returned.
   * Thus the intersection of [2014-06-20,2014-06-25) and [2014-06-25,2014-06-30) is [2014-06-25,2014-06-25).
   * 
   * @param other  the other range to check for
   * @return the range that is the intersection of the two ranges
   * @throws IllegalArgumentException if the ranges do not overlap
   */
  public LocalDateRange intersection(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    if (overlaps(other) == false) {
      throw new IllegalArgumentException("Ranges do not overlap: " + this + " and " + other);
    }
    int cmpStart = start.compareTo(other.start);
    int cmpEnd = endExclusive.compareTo(other.endExclusive);
    if (cmpStart >= 0 && cmpEnd <= 0) {
      return this;
    } else if (cmpStart <= 0 && cmpEnd >= 0) {
      return other;
    } else {
      LocalDate newStart = (cmpStart >= 0 ? start : other.start);
      LocalDate newEnd = (cmpEnd <= 0 ? endExclusive : other.endExclusive);
      return LocalDateRange.of(newStart, newEnd);
    }
  }

  /**
   * Calculates the range that is the union of this range and the specified range.
   * <p>
   * This finds the union of two ranges.
   * This returns an exception if the two ranges do not {@linkplain #overlaps(LocalDateRange) overlap}.
   * <p>
   * If the two ranges are adjacent but have no whole dates in common, the union is still returned.
   * Thus the union of [2014-06-20,2014-06-25) and [2014-06-25,2014-06-30) is [2014-06-20,2014-06-30).
   * 
   * @param other  the other range to check for
   * @return the range that is the union of the two ranges
   * @throws IllegalArgumentException if the ranges do not overlap
   */
  public LocalDateRange union(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    if (overlaps(other) == false) {
      throw new IllegalArgumentException("Ranges do not overlap: " + this + " and " + other);
    }
    int cmpStart = start.compareTo(other.start);
    int cmpEnd = endExclusive.compareTo(other.endExclusive);
    if (cmpStart >= 0 && cmpEnd <= 0) {
      return other;
    } else if (cmpStart <= 0 && cmpEnd >= 0) {
      return this;
    } else {
      LocalDate newStart = (cmpStart >= 0 ? other.start : start);
      LocalDate newEnd = (cmpEnd <= 0 ? other.endExclusive : endExclusive);
      return LocalDateRange.of(newStart, newEnd);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Streams the set of dates included in the range.
   * <p>
   * This returns a stream consisting of each date in the range.
   * The stream is ordered.
   * 
   * @return the stream of dates from the start to the end
   */
  public Stream<LocalDate> stream() {
    Iterator<LocalDate> it = new Iterator<LocalDate>() {
      private LocalDate current = start;

      @Override
      public LocalDate next() {
        LocalDate result = current;
        current = current.plusDays(1);
        return result;
      }

      @Override
      public boolean hasNext() {
        return current.isBefore(endExclusive);
      }
    };
    long count = endExclusive.toEpochDay() - start.toEpochDay() + 1;
    Spliterator<LocalDate> spliterator = Spliterators.spliterator(it, count,
        Spliterator.IMMUTABLE | Spliterator.NONNULL |
            Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED |
            Spliterator.SIZED | Spliterator.SUBSIZED);
    return StreamSupport.stream(spliterator, false);
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
    if (isEmpty() && this.equals(other)) {
      return false;
    }
    // endExclusive <= other.start
    return endExclusive.compareTo(other.start) <= 0;
  }

  /**
   * Checks if this range is entirely after the specified range.
   * 
   * @param other  the other range to check for
   * @return true if every date in this range is after every date in the other range
   */
  public boolean isAfter(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    if (isEmpty() && this.equals(other)) {
      return false;
    }
    // start >= other.endExclusive
    return start.compareTo(other.endExclusive) >= 0;
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
      return start.equals(other.start) && endExclusive.equals(other.endExclusive);
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
    return start.hashCode() ^ endExclusive.hashCode();
  }

  /**
   * Returns this range as a string, such as {@code [2009-12-03,2014-06-30)}.
   * <p>
   * The string will be one of these formats:<br />
   *  {@code [2009-12-03,2014-06-30)}<br />
   *  {@code [2009-12-03,+INFINITY]} - if the end is unbounded<br />
   *  {@code [-INFINITY,2014-06-30)} - if the start is unbounded<br />
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
      buf.append(endExclusive).append(')');
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

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private LocalDateRange(
      LocalDate start,
      LocalDate endExclusive) {
    JodaBeanUtils.notNull(start, "start");
    JodaBeanUtils.notNull(endExclusive, "endExclusive");
    this.start = start;
    this.endExclusive = endExclusive;
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
     * The meta-property for the {@code endExclusive} property.
     */
    private final MetaProperty<LocalDate> endExclusive = DirectMetaProperty.ofImmutable(
        this, "endExclusive", LocalDateRange.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "start",
        "endExclusive");

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
        case 1275403267:  // endExclusive
          return endExclusive;
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
     * The meta-property for the {@code endExclusive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endExclusive() {
      return endExclusive;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 109757538:  // start
          return ((LocalDateRange) bean).getStart();
        case 1275403267:  // endExclusive
          return ((LocalDateRange) bean).getEndExclusive();
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
    private LocalDate endExclusive;

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
        case 1275403267:  // endExclusive
          return endExclusive;
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
        case 1275403267:  // endExclusive
          this.endExclusive = (LocalDate) newValue;
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
          endExclusive);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("LocalDateRange.Builder{");
      buf.append("start").append('=').append(JodaBeanUtils.toString(start)).append(',').append(' ');
      buf.append("endExclusive").append('=').append(JodaBeanUtils.toString(endExclusive));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
