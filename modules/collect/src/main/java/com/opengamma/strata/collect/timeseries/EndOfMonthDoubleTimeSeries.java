/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.function.ObjDoublePredicate;

/**
 * An immutable implementation of {@code LocalDateDoubleTimeSeries} that stores
 * one value for each month, returning the value for all dates in the month.
 * <p>
 * This is most useful for inflation indices, where data is produced monthly.
 */
@BeanDefinition(builderScope = "private", metaScope = "private")
final class EndOfMonthDoubleTimeSeries
    implements ImmutableBean, LocalDateDoubleTimeSeries, Serializable {

  /**
   * An empty instance.
   */
  private static final LocalDateDoubleTimeSeries EMPTY =
      new EndOfMonthDoubleTimeSeries(YearMonth.of(2000, 1), DoubleArray.EMPTY);

  /**
   * The first month that data is available for.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth startMonth;
  /**
   * The values in the series, one point for each month, relative to the start month.
   */
  @PropertyDefinition(get = "private", validate = "notNull")
  private final DoubleArray points;

//  /**
//   * Package protected factory method intended to be called
//   * by the {@link LocalDateDoubleTimeSeriesBuilder}. As such
//   * all the information passed is assumed to be consistent.
//   *
//   * @param startDate  the earliest date included in the time-series
//   * @param endDate  the latest date included in the time-series
//   * @param values  stream holding the time-series points
//   * @param dateCalculation  the date calculation method to be used
//   * @return a new time-series
//   */
//  static LocalDateDoubleTimeSeries of(
//      LocalDate startDate,
//      LocalDate endDate,
//      Stream<LocalDateDoublePoint> values,
//      DenseTimeSeriesCalculation dateCalculation) {
//
//    double[] points = new double[dateCalculation.calculatePosition(startDate, endDate) + 1];
//    Arrays.fill(points, Double.NaN);
//    values.forEach(pt -> points[dateCalculation.calculatePosition(startDate, pt.getDate())] = pt.getValue());
//    return new MonthlyLocalDateDoubleTimeSeries(startDate, points, dateCalculation, true);
//  }

  /**
   * Returns an empty time-series.
   *
   * @return an empty time-series
   */
  public static LocalDateDoubleTimeSeries empty() {
    return EMPTY;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isEmpty() {
    return !validIndices().findFirst().isPresent();
  }

  @Override
  public int size() {
    return (int) validIndices().count();
  }

  @Override
  public boolean containsDate(LocalDate date) {
    long position = calculateRawPositionFromDate(date);
    return position >= 0 && position < points.size();
  }

  @Override
  public OptionalDouble get(LocalDate date) {
    long position = calculateRawPositionFromDate(date);
    if (position >= 0 && position < points.size()) {
      double value = points.get((int) position);
      if (isValidPoint(value)) {
        return OptionalDouble.of(value);
      }
    }
    return OptionalDouble.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getLatestDate() {
    for (int i = points.size() - 1; i >= 0; i--) {
      if (isValidPoint(points.get(i))) {
        return calculateDateFromPosition(i);
      }
    }
    throw new NoSuchElementException("Unable to return latest date, time-series is empty");
  }

  @Override
  public double getLatestValue() {
    for (int i = points.size() - 1; i >= 0; i--) {
      double point = points.get(i);
      if (isValidPoint(point)) {
        return point;
      }
    }
    throw new NoSuchElementException("Unable to return latest value, time-series is empty");
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries subSeries(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startInclusive");
    ArgChecker.notNull(endExclusive, "endExclusive");
    YearMonth startMonthInclusive = YearMonth.from(startInclusive);
    YearMonth endMonthExclusive = YearMonth.from(endExclusive);
    if (endMonthExclusive.isBefore(startMonthInclusive)) {
      throw new IllegalArgumentException(
          "Invalid sub series, end before start: " + startInclusive + " to " + endExclusive);
    }

    // will the result be empty
    int size = points.size();
    long startPos = calculateRawPositionFromDate(startInclusive);
    long endPos = calculateRawPositionFromDate(endExclusive);
    if (isEmpty() || startPos == endPos || endPos <= 0 || startPos > size) {
      return EMPTY;
    }
    return new EndOfMonthDoubleTimeSeries(
        startMonth.plusMonths(startPos),
        points.subArray((int) Math.max(startPos, 0), (int) Math.min(endPos, size)));
  }

  @Override
  public LocalDateDoubleTimeSeries headSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");
    if (numPoints == 0) {
      return EMPTY;
    } else if (numPoints > size()) {
      return this;
    }
    int endPos = findHeadPoints(numPoints);
    return new EndOfMonthDoubleTimeSeries(startMonth, points.subArray(0, endPos));
  }

  private int findHeadPoints(int required) {
    int found = 0;
    for (int i = 0; i < points.size(); i++) {
      double point = points.get(i);
      if (isValidPoint(point)) {
        found++;
        if (found >= required) {
          return i;
        }
      }
    }
    return points.size();
  }

  @Override
  public LocalDateDoubleTimeSeries tailSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");
    if (numPoints == 0) {
      return EMPTY;
    } else if (numPoints > size()) {
      return this;
    }
    int startPos = findTailPoints(numPoints);
    return new EndOfMonthDoubleTimeSeries(startMonth.plusMonths(startPos), points.subArray(startPos, points.size()));
  }

  private int findTailPoints(int required) {
    int found = 0;
    for (int i = points.size() - 1; i >= 0; i--) {
      if (isValidPoint(points.get(i))) {
        found++;
        if (found >= required) {
          return i;
        }
      }
    }
    return 0;
  }

  //-------------------------------------------------------------------------
  @Override
  public Stream<LocalDateDoublePoint> stream() {
    return validIndices()
        .mapToObj(i -> LocalDateDoublePoint.of(calculateDateFromPosition(i), points.get(i)));
  }

  @Override
  public DoubleStream values() {
    return points.stream().filter(this::isValidPoint);
  }

  @Override
  public Stream<LocalDate> dates() {
    return validIndices().mapToObj(this::calculateDateFromPosition);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries filter(ObjDoublePredicate<LocalDate> predicate) {
    DoubleArray filtered = points.mapWithIndex((i, v) -> filter(predicate, i, v));
    return new EndOfMonthDoubleTimeSeries(startMonth, filtered);
  }

  private double filter(ObjDoublePredicate<LocalDate> predicate, int index, double value) {
    if (!isValidPoint(value)) {
      return value;
    }
    return predicate.test(calculateDateFromPosition(index), value) ? value : Double.NaN;
  }

  @Override
  public LocalDateDoubleTimeSeries mapDates(Function<? super LocalDate, ? extends LocalDate> mapper) {
    YearMonth ym = startMonth;
    long baseMonth = Long.MIN_VALUE;
    long lastSeenMonth = Long.MIN_VALUE;
    double[] mappedPoints = new double[points.size()];
    Arrays.fill(mappedPoints, Double.NaN);
    for (int i = 0; i < points.size(); i++) {
      double value = points.get(i);
      if (isValidPoint(value)) {
        LocalDate originalDate = calculateDateFromPosition(i);
        LocalDate mappedDate = mapper.apply(originalDate).with(lastDayOfMonth());
        long mappedMonth = mappedDate.getLong(PROLEPTIC_MONTH);
        if (mappedMonth < lastSeenMonth) {
          throw new IllegalArgumentException(
              Messages.format(
                  "Dates must be in ascending order and in separate months after calling mapDates but {} and {} are not",
                  mappedDate.with(PROLEPTIC_MONTH, lastSeenMonth).with(lastDayOfMonth()),
                  mappedDate));
        }
        lastSeenMonth = mappedMonth;
        if (baseMonth == Long.MIN_VALUE) {
          baseMonth = mappedMonth;
        }
        mappedPoints[(int) (mappedMonth - baseMonth)] = value;
      }
      ym = ym.plusMonths(1);
    }
    DoubleArray newPoints = DoubleArray.copyOf(mappedPoints, 0, (int) (lastSeenMonth - baseMonth));
    YearMonth newStartMonth = YearMonth.of(2000, 1).with(PROLEPTIC_MONTH, baseMonth);
    return new EndOfMonthDoubleTimeSeries(newStartMonth, newPoints);
  }

  @Override
  public LocalDateDoubleTimeSeries mapValues(DoubleUnaryOperator mapper) {
    DoubleArray values = points.map(d -> isValidPoint(d) ? applyMapper(mapper, d) : d);
    return new EndOfMonthDoubleTimeSeries(startMonth, values);
  }

  private double applyMapper(DoubleUnaryOperator mapper, double d) {
    double value = mapper.applyAsDouble(d);
    if (!isValidPoint(value)) {
      throw new IllegalArgumentException("Mapper must not map to NaN");
    }
    return value;
  }

  @Override
  public void forEach(ObjDoubleConsumer<LocalDate> action) {
    validIndices().forEach(i -> action.accept(calculateDateFromPosition(i), points.get(i)));
  }

  @Override
  public LocalDateDoubleTimeSeriesBuilder toBuilder() {
    return new LocalDateDoubleTimeSeriesBuilder(stream());
  }

  //-------------------------------------------------------------------------
  private long calculateRawPositionFromDate(LocalDate date) {
    return date.getLong(PROLEPTIC_MONTH) - startMonth.getLong(PROLEPTIC_MONTH);
  }

  private LocalDate calculateDateFromPosition(int i) {
    return startMonth.plusMonths(i).atEndOfMonth();
  }

  private IntStream validIndices() {
    return IntStream.range(0, points.size()).filter(this::isValidIndex);
  }

  private boolean isValidIndex(int i) {
    return isValidPoint(points.get(i));
  }

  private boolean isValidPoint(double d) {
    return !Double.isNaN(d);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code EndOfMonthDoubleTimeSeries}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return EndOfMonthDoubleTimeSeries.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(EndOfMonthDoubleTimeSeries.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private EndOfMonthDoubleTimeSeries(
      YearMonth startMonth,
      DoubleArray points) {
    JodaBeanUtils.notNull(startMonth, "startMonth");
    JodaBeanUtils.notNull(points, "points");
    this.startMonth = startMonth;
    this.points = points;
  }

  @Override
  public MetaBean metaBean() {
    return EndOfMonthDoubleTimeSeries.Meta.INSTANCE;
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
   * Gets the first month that data is available for.
   * @return the value of the property, not null
   */
  public YearMonth getStartMonth() {
    return startMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the values in the series, one point for each month, relative to the start month.
   * @return the value of the property, not null
   */
  private DoubleArray getPoints() {
    return points;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      EndOfMonthDoubleTimeSeries other = (EndOfMonthDoubleTimeSeries) obj;
      return JodaBeanUtils.equal(startMonth, other.startMonth) &&
          JodaBeanUtils.equal(points, other.points);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(points);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("EndOfMonthDoubleTimeSeries{");
    buf.append("startMonth").append('=').append(startMonth).append(',').append(' ');
    buf.append("points").append('=').append(JodaBeanUtils.toString(points));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code EndOfMonthDoubleTimeSeries}.
   */
  private static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startMonth} property.
     */
    private final MetaProperty<YearMonth> startMonth = DirectMetaProperty.ofImmutable(
        this, "startMonth", EndOfMonthDoubleTimeSeries.class, YearMonth.class);
    /**
     * The meta-property for the {@code points} property.
     */
    private final MetaProperty<DoubleArray> points = DirectMetaProperty.ofImmutable(
        this, "points", EndOfMonthDoubleTimeSeries.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startMonth",
        "points");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1589912770:  // startMonth
          return startMonth;
        case -982754077:  // points
          return points;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends EndOfMonthDoubleTimeSeries> builder() {
      return new EndOfMonthDoubleTimeSeries.Builder();
    }

    @Override
    public Class<? extends EndOfMonthDoubleTimeSeries> beanType() {
      return EndOfMonthDoubleTimeSeries.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1589912770:  // startMonth
          return ((EndOfMonthDoubleTimeSeries) bean).getStartMonth();
        case -982754077:  // points
          return ((EndOfMonthDoubleTimeSeries) bean).getPoints();
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
   * The bean-builder for {@code EndOfMonthDoubleTimeSeries}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<EndOfMonthDoubleTimeSeries> {

    private YearMonth startMonth;
    private DoubleArray points;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1589912770:  // startMonth
          return startMonth;
        case -982754077:  // points
          return points;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1589912770:  // startMonth
          this.startMonth = (YearMonth) newValue;
          break;
        case -982754077:  // points
          this.points = (DoubleArray) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public EndOfMonthDoubleTimeSeries build() {
      return new EndOfMonthDoubleTimeSeries(
          startMonth,
          points);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("EndOfMonthDoubleTimeSeries.Builder{");
      buf.append("startMonth").append('=').append(JodaBeanUtils.toString(startMonth)).append(',').append(' ');
      buf.append("points").append('=').append(JodaBeanUtils.toString(points));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
