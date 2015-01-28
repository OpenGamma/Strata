/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import static com.opengamma.collect.Guavate.toImmutableList;
import static com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.SKIP_WEEKENDS;
import static java.time.temporal.ChronoUnit.DAYS;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.function.ObjDoublePredicate;
import com.opengamma.collect.tuple.Pair;

/**
 * An immutable implementation of {@code DoubleTimeSeries} where the
 * data stored is expected to be dense. For example, points for every
 * working day in a month. If sparser data is being used then
 * {@link SparseLocalDateDoubleTimeSeries} is likely to be a better
 * choice for the data.
 * <p>
 * This implementation uses arrays internally.
 */
@BeanDefinition(builderScope = "private")
public class DenseLocalDateDoubleTimeSeries
    implements ImmutableBean, LocalDateDoubleTimeSeries, Serializable {

  // Arguably, we'd be better using the EMPTY_SERIES from the sparse implementation
  public static final DenseLocalDateDoubleTimeSeries EMPTY_SERIES =
      new DenseLocalDateDoubleTimeSeries(LocalDate.MIN, new double[0]);

  /**
   * Enum indicating whether there are positions in the points
   * array for weekends and providing the different date
   * calculations for each case.
   */
  public enum DenseTimeSeriesCalculation {
    /**
     * Data is not held for weekends.
     */
    SKIP_WEEKENDS {
      @Override
      int calculatePosition(LocalDate startDate, LocalDate toInsert) {
        int unadjusted = (int) DAYS.between(startDate, toInsert);
        int numWeekEnds = unadjusted / 7 + (DayOfWeek.from(startDate).compareTo(DayOfWeek.from(toInsert)) > 0 ? 1 : 0);
        return unadjusted - (2 * numWeekEnds);
      }

      @Override
      LocalDate calculateDateFromPosition(LocalDate startDate, int position) {
        int numWeekEnds = position / 5;
        int remaining = position % 5;
        int endPointAdjustments = remaining < (6 - startDate.getDayOfWeek().getValue()) ? remaining : remaining + 2;
        return startDate.plusDays(7 * numWeekEnds + endPointAdjustments);
      }
    },
    /**
     * Data is held for weekends.
     */
    INCLUDE_WEEKENDS {
      @Override
      int calculatePosition(LocalDate startDate, LocalDate toInsert) {
        return (int) DAYS.between(startDate, toInsert);
      }

      @Override
      LocalDate calculateDateFromPosition(LocalDate startDate, int position) {
        return startDate.plusDays(position);
      }
    };

    /**
     * Calculates the position in the array where the supplied date should
     * be located given a start date. As no information is held about the
     * actual array, callers must check array bounds.
     *
     * @param startDate  the start date for the series (the value for this
     *   entry will be stored at position 0 in the array)
     * @param date  the date to calculate a position for
     * @return the position in the array where the date would be located
     */
    abstract int calculatePosition(LocalDate startDate, LocalDate date);

    /**
     * Given a start date and a position in an array, calculate what date
     * the position holds data for.
     *
     * @param startDate  the start date for the series (the value for this
     *   entry will be stored at position 0 in the array)
     * @param position  the position in the array to calculate a date for
     * @return the date the position in the array holds data for
     */
    abstract LocalDate calculateDateFromPosition(LocalDate startDate, int position);
  }

  /**
   * Date corresponding to first element in the array. All other
   * values can be calculated using date arithmetic to find
   * correct point
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;

  @PropertyDefinition(get = "private", validate = "notNull")
  private final double[] points;

  /**
   * Whether we should store data for the weekends (NaN will be stored
   * if no data is available).
   */
  @PropertyDefinition(get = "private", validate = "notNull")
  private final DenseTimeSeriesCalculation dateCalculation;

  /**
   * Obtains a time-series from a collection of points.
   * The points must not contain weekend dates. If weekend
   * dates are required then use
   * {@link #of(Collection, DenseTimeSeriesCalculation)}.
   *
   * @param points  the list of points
   * @return the time-series
   */
  public static DenseLocalDateDoubleTimeSeries of(Collection<LocalDateDoublePoint> points) {
    return of(points, SKIP_WEEKENDS);
  }

  /**
   * Obtains a time-series from a collection of points
   * using the specified {@code dateCalculation}.
   *
   * @param points  the list of points
   * @return the time-series
   */
  public static DenseLocalDateDoubleTimeSeries of(
      Collection<LocalDateDoublePoint> points,
      DenseTimeSeriesCalculation dateCalculation) {

    ArgChecker.notNull(points, "points");

    ArgChecker.isFalse(
        points.stream().anyMatch(pt ->
            pt == null || (dateCalculation == SKIP_WEEKENDS && isWeekend(pt.getDate()))),
        "dates must not contain nulls or weekend dates");

    if (points.isEmpty()) {
      return EMPTY_SERIES;
    }

    Pair<LocalDate, LocalDate> extremes = points.stream()
        .reduce(Pair.of(LocalDate.MAX, LocalDate.MIN), (pair, pt) -> {
          LocalDate earliest = pair.getFirst();
          LocalDate latest = pair.getSecond();
          LocalDate ld = pt.getDate();
          return Pair.of(ld.isBefore(earliest) ? ld : earliest, ld.isAfter(latest) ? ld : latest);
        }, (p1, p2) -> p1);

    LocalDate startDate = extremes.getFirst();

    int size = dateCalculation.calculatePosition(startDate, extremes.getSecond()) + 1;
    double[] values = new double[size];
    if (points.size() != size) {
      // We have gaps - prefill with NaNs
      Arrays.fill(values, Double.NaN);
    }

    points.stream()
        .forEach(p -> values[dateCalculation.calculatePosition(startDate, p.getDate())] = p.getValue());

    return new DenseLocalDateDoubleTimeSeries(startDate, values, dateCalculation);
  }

  /**
   * Obtains a time-series from a map of dates and values.
   * The points must not contain weekend dates. If weekend
   * dates are required then use
   * {@link #of(Map, DenseTimeSeriesCalculation)}.
   *
   * @param map  the map of dates to values
   * @return the time-series
   */
  public static DenseLocalDateDoubleTimeSeries of(Map<LocalDate, Double> map) {
    return of(map, SKIP_WEEKENDS);
  }

  /**
   * Obtains a time-series from a map of dates and values
   * using the specified {@code dateCalculation}.
   *
   * @param map  the map of dates to values
   * @return the time-series
   */
  public static DenseLocalDateDoubleTimeSeries of(
      Map<LocalDate, Double> map,
      DenseTimeSeriesCalculation dateCalculation) {

    // TODO handle empty map
    ArgChecker.noNulls(map, "map");

    if (map.isEmpty()) {
      return EMPTY_SERIES;
    }

    // First find earliest date
    Pair<LocalDate, LocalDate> extremes = findExtremities(map.keySet());

    LocalDate startDate = extremes.getFirst();

    int size = dateCalculation.calculatePosition(startDate, extremes.getSecond()) + 1;
    double[] values = new double[size];
    if (map.size() != size) {
      // We have gaps - prefill with NaNs
      Arrays.fill(values, Double.NaN);
    }

    map.entrySet()
        .stream()
        .forEach(e -> values[dateCalculation.calculatePosition(startDate, e.getKey())] = e.getValue());

    return new DenseLocalDateDoubleTimeSeries(startDate, values, dateCalculation);
  }

  /**
   * Obtains a time-series from matching collections of dates and values.
   * <p>
   * The two collections must be the same size.
   *
   * @param dates  the date collection
   * @param values  the value collection
   * @return the time-series
   */
  public static DenseLocalDateDoubleTimeSeries of(Collection<LocalDate> dates, Collection<Double> values) {
    return of(dates, values, SKIP_WEEKENDS);
  }

  /**
   * Obtains a time-series from matching collections of dates and values
   * using the specified {@code dateCalculation}.
   * <p>
   * The two collections must be the same size.
   *
   * @param dates  the date collection
   * @param values  the value collection
   * @return the time-series
   */
  public static DenseLocalDateDoubleTimeSeries of(
      Collection<LocalDate> dates,
      Collection<Double> values,
      DenseTimeSeriesCalculation dateCalculation) {

    ArgChecker.notNull(dates, "dates");
    ArgChecker.noNulls(values, "values");
    ArgChecker.isTrue(dates.size() == values.size(), "Dates and values must have same size");

    ArgChecker.isFalse(
        dates.stream().anyMatch(ld ->
            ld == null || (dateCalculation == SKIP_WEEKENDS && isWeekend(ld))),
        "dates must not contain nulls or weekend dates");

    if (dates.isEmpty()) {
      return EMPTY_SERIES;
    }

    Pair<LocalDate, LocalDate> extremes = findExtremities(dates);

    LocalDate startDate = extremes.getFirst();

    int size = dateCalculation.calculatePosition(startDate, extremes.getSecond()) + 1;
    double[] valuesArray = new double[size];
    if (values.size() != size) {
      // We have gaps - prefill with NaNs
      Arrays.fill(valuesArray, Double.NaN);
    }

    // A utility to zip 2 streams would be useful here!

    Iterator<Double> itValues = values.iterator();

    for (LocalDate date : dates) {
      valuesArray[dateCalculation.calculatePosition(startDate, date)] = itValues.next();
    }

    return new DenseLocalDateDoubleTimeSeries(startDate, valuesArray, dateCalculation);
  }

  private static DenseLocalDateDoubleTimeSeries of(
      LocalDate startDate,
      DoubleStream values,
      DenseTimeSeriesCalculation dateCalculation) {
    return new DenseLocalDateDoubleTimeSeries(startDate, values.toArray(), dateCalculation);
  }

  @ImmutableConstructor
  private DenseLocalDateDoubleTimeSeries(
      LocalDate startDate,
      double[] points,
      DenseTimeSeriesCalculation dateCalculation) {

    this.startDate = ArgChecker.notNull(startDate, "startDate");
    this.points = ArgChecker.notNull(points, "points").clone();
    this.dateCalculation = ArgChecker.notNull(dateCalculation, "dateCalculation");
  }

  private DenseLocalDateDoubleTimeSeries(LocalDate startDate, double[] points) {
    this(startDate, points, SKIP_WEEKENDS);
  }

  private static Pair<LocalDate, LocalDate> findExtremities(Collection<LocalDate> dates) {
    return dates
        .stream()
        .reduce(Pair.of(LocalDate.MAX, LocalDate.MIN), (p, ld) -> {
          LocalDate earliest = p.getFirst();
          LocalDate latest = p.getSecond();
          return Pair.of(ld.isBefore(earliest) ? ld : earliest, ld.isAfter(latest) ? ld : latest);
        }, (p1, p2) -> p1);
  }

  public static DenseLocalDateDoubleTimeSeries of(LocalDate date, double value) {
    return of(date, value, SKIP_WEEKENDS);
  }

  public static DenseLocalDateDoubleTimeSeries of(
      LocalDate date,
      double value,
      DenseTimeSeriesCalculation dateCalculation) {
    return new DenseLocalDateDoubleTimeSeries(date, new double[]{value}, dateCalculation);
  }

  /**
   * Create a new time series with the specified additional points.
   *
   * @param additionalPoints  the points to add or update in the time series.
   * @return a new time series containing updated points
   */
  // Should this not be here with the expectation that we always go through the builder?
  public DenseLocalDateDoubleTimeSeries update(List<LocalDateDoublePoint> additionalPoints) {

    // Assume points are at the end for now
    LocalDate latestDate = additionalPoints.stream()
        .reduce(LocalDate.MIN, (ld, pt) -> ld.isAfter(pt.getDate()) ? ld : pt.getDate(), (ld1, ld2) -> ld1);

    int size = dateCalculation.calculatePosition(startDate, latestDate) + 1;

    double[] values = Arrays.copyOf(points, size);
    if (points.length + additionalPoints.size() != size) {
      // We have gaps - prefill with NaNs
      Arrays.fill(values, points.length, size, Double.NaN);
    }

    additionalPoints.stream()
        .forEach(p -> values[dateCalculation.calculatePosition(startDate, p.getDate())] = p.getValue());

    return new DenseLocalDateDoubleTimeSeries(startDate, values);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public int size() {
    return (int) validIndices().count();
  }

  @Override
  public DenseLocalDateDoubleTimeSeries subSeries(LocalDate startInclusive, LocalDate endExclusive) {

    ArgChecker.notNull(startInclusive, "startInclusive");
    ArgChecker.notNull(endExclusive, "endExclusive");
    if (endExclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException(
          "Invalid sub series, end before start: " + startInclusive + " to " + endExclusive);
    }

    // special case when this is empty or when the dates are the same
    // or the series don't intersect
    if (isEmpty() || startInclusive.equals(endExclusive) ||
        !startDate.isBefore(endExclusive) ||
        startInclusive.isAfter(getLatestDate())) {
      return EMPTY_SERIES;
    }

    int startIndex = dateCalculation.calculatePosition(startDate, startInclusive);
    int endIndex = dateCalculation.calculatePosition(startDate, endExclusive);

    return new DenseLocalDateDoubleTimeSeries(
        startInclusive,
        Arrays.copyOfRange(points, startIndex, endIndex),
        dateCalculation);
  }

  @Override
  public DenseLocalDateDoubleTimeSeries headSeries(int numPoints) {

    ArgChecker.notNegative(numPoints, "numPoints");

    if (numPoints == 0) {
      return EMPTY_SERIES;
    } else if (numPoints > size()) {
      return this;
    }

    int endPosn = findHeadPoints(numPoints);

    return new DenseLocalDateDoubleTimeSeries(startDate, Arrays.copyOf(points, endPosn), dateCalculation);
  }

  private int findHeadPoints(int required) {

    // Take enough points that aren't NaN
    // else we need the entire series
    return IntStream.range(0, points.length)
        .filter(i -> isValidPoint(points[i]))
        .skip(required)
        .findFirst()
        .orElse(points.length);
  }

  @Override
  public DenseLocalDateDoubleTimeSeries tailSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");

    if (numPoints == 0) {
      return EMPTY_SERIES;
    } else if (numPoints > size()) {
      return this;
    }

    int startPoint = findTailPoints(numPoints);

    return new DenseLocalDateDoubleTimeSeries(
        dateCalculation.calculateDateFromPosition(startDate, startPoint),
        Arrays.copyOfRange(points, startPoint, points.length),
        dateCalculation);
  }

  private int findTailPoints(int required) {

    // As there is no way of constructing an IntStream from
    // n to m where n > m, we go from -n to m and then
    // take the additive inverse (sigh!)
    return IntStream.rangeClosed(1 - points.length, 0)
        .map(i -> -i)
        .filter(i -> isValidPoint(points[i]))
        .skip(required - 1)
        .findFirst()
        .orElse(0);
  }

  @Override
  public Stream<LocalDateDoublePoint> stream() {
    return validIndices()
        .mapToObj(i -> LocalDateDoublePoint.of(dateCalculation.calculateDateFromPosition(startDate, i), points[i]));
  }

  @Override
  public DoubleStream valueStream() {
    return Arrays.stream(points).filter(this::isValidPoint);
  }

  @Override
  public Stream<LocalDate> dateStream() {
    return validIndices()
        .mapToObj(i -> dateCalculation.calculateDateFromPosition(startDate, i));
  }

  private IntStream validIndices() {
    return IntStream.range(0, points.length)
        .filter(i -> isValidPoint(points[i]));
  }

  @Override
  public DenseLocalDateDoubleTimeSeries filter(ObjDoublePredicate<LocalDate> predicate) {

    DoubleStream filtered = IntStream.range(0, points.length)
        .mapToDouble(i ->
            isValidPoint(points[i]) &&
                predicate.test(dateCalculation.calculateDateFromPosition(startDate, i), points[i]) ?
            points[i] : Double.NaN);

    return DenseLocalDateDoubleTimeSeries.of(startDate, filtered, dateCalculation);
  }

  @Override
  public DenseLocalDateDoubleTimeSeries mapValues(DoubleUnaryOperator mapper) {
    return of(
        startDate,
        Arrays.stream(points).map(d -> isValidPoint(d) ? mapper.applyAsDouble(d) : d),
        dateCalculation);
  }

  private boolean isValidPoint(double d) {
    return !Double.isNaN(d);
  }

  @Override
  public ImmutableSortedMap<LocalDate, Double> toMap() {

    // Unfortunately we can't use the Guavate collector on an IntStream
    ImmutableSortedMap.Builder<LocalDate, Double> builder = ImmutableSortedMap.naturalOrder();

    validIndices().forEach(
        i -> builder.put(dateCalculation.calculateDateFromPosition(startDate, i), points[i])
    );
    return builder.build();
  }

  @Override
  public LocalDateDoubleTimeSeries combineWith(LocalDateDoubleTimeSeries other, DoubleBinaryOperator mapper) {

    ArgChecker.notNull(other, "other");
    ArgChecker.notNull(mapper, "mapper");

    return new LocalDateDoubleTimeSeriesBuilder(
        stream().filter(pt -> other.containsDate(pt.getDate()))
            .map(pt -> LocalDateDoublePoint.of(
                pt.getDate(),
                mapper.applyAsDouble(pt.getValue(), other.get(pt.getDate()).getAsDouble()))))
            .build();
  }

  @Override
  public void forEach(ObjDoubleConsumer<LocalDate> action) {
    validIndices().forEach(i ->
        action.accept(dateCalculation.calculateDateFromPosition(startDate, i), points[i]));
  }

  @Override
  public LocalDateDoubleTimeSeriesBuilder toBuilder() {
    return new LocalDateDoubleTimeSeriesBuilder(stream());
  }


  @Override
  public LocalDate getEarliestDate() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return earliest date, time-series is empty");
    }
    return startDate;
  }

  @Override
  public double getEarliestValue() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return earliest value, time-series is empty");
    }
    return points[0];
  }

  @Override
  public LocalDate getLatestDate() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest date, time-series is empty");
    }
    return dateCalculation.calculateDateFromPosition(startDate, points.length - 1);
  }

  @Override
  public double getLatestValue() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest value, time-series is empty");
    }
    return points[points.length - 1];
  }

  @Override
  public ImmutableList<LocalDate> dates() {
    return dateStream().collect(toImmutableList());
  }

  @Override
  public ImmutableList<Double> values() {

    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    valueStream().forEach(builder::add);
    return builder.build();
  }

  @Override
  public boolean containsDate(LocalDate date) {
    if (isEmpty() || date.isBefore(startDate) ) {
      return false;
    } else {

      // This test feels wrong - push to the enum?
      if (dateCalculation == SKIP_WEEKENDS && isWeekend(date)) {
        return false;
      }
      int position = dateCalculation.calculatePosition(startDate, date);
      return position < points.length && isValidPoint(points[position]);
    }
  }

  @Override
  public OptionalDouble get(LocalDate dt) {

    // This test feels wrong - push to the enum?
    if (dateCalculation == SKIP_WEEKENDS && isWeekend(dt)) {
      return OptionalDouble.empty();
    } else {

      int position = dateCalculation.calculatePosition(startDate, dt);
      if (position >= 0 && position < points.length) {
        double point = points[position];
        return isValidPoint(point) ? OptionalDouble.of(point) : OptionalDouble.empty();
      } else {
        return OptionalDouble.empty();
      }
    }
  }

  // Sufficient for the moment, in the future we may need to
  // vary depending on a non-Western weekend
  private static boolean isWeekend(LocalDate date) {
    return date.get(ChronoField.DAY_OF_WEEK) > 5;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DenseLocalDateDoubleTimeSeries}.
   * @return the meta-bean, not null
   */
  public static DenseLocalDateDoubleTimeSeries.Meta meta() {
    return DenseLocalDateDoubleTimeSeries.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DenseLocalDateDoubleTimeSeries.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public DenseLocalDateDoubleTimeSeries.Meta metaBean() {
    return DenseLocalDateDoubleTimeSeries.Meta.INSTANCE;
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
   * Gets date corresponding to first element in the array. All other
   * values can be calculated using date arithmetic to find
   * correct point
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the points.
   * @return the value of the property, not null
   */
  private double[] getPoints() {
    return (points != null ? points.clone() : null);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether we should store data for the weekends (NaN will be stored
   * if no data is available).
   * @return the value of the property, not null
   */
  private DenseTimeSeriesCalculation getDateCalculation() {
    return dateCalculation;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DenseLocalDateDoubleTimeSeries other = (DenseLocalDateDoubleTimeSeries) obj;
      return JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getPoints(), other.getPoints()) &&
          JodaBeanUtils.equal(getDateCalculation(), other.getDateCalculation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPoints());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDateCalculation());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("DenseLocalDateDoubleTimeSeries{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(getStartDate())).append(',').append(' ');
    buf.append("points").append('=').append(JodaBeanUtils.toString(getPoints())).append(',').append(' ');
    buf.append("dateCalculation").append('=').append(JodaBeanUtils.toString(getDateCalculation())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DenseLocalDateDoubleTimeSeries}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", DenseLocalDateDoubleTimeSeries.class, LocalDate.class);
    /**
     * The meta-property for the {@code points} property.
     */
    private final MetaProperty<double[]> points = DirectMetaProperty.ofImmutable(
        this, "points", DenseLocalDateDoubleTimeSeries.class, double[].class);
    /**
     * The meta-property for the {@code dateCalculation} property.
     */
    private final MetaProperty<DenseTimeSeriesCalculation> dateCalculation = DirectMetaProperty.ofImmutable(
        this, "dateCalculation", DenseLocalDateDoubleTimeSeries.class, DenseTimeSeriesCalculation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "points",
        "dateCalculation");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -982754077:  // points
          return points;
        case -152592837:  // dateCalculation
          return dateCalculation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DenseLocalDateDoubleTimeSeries> builder() {
      return new DenseLocalDateDoubleTimeSeries.Builder();
    }

    @Override
    public Class<? extends DenseLocalDateDoubleTimeSeries> beanType() {
      return DenseLocalDateDoubleTimeSeries.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code points} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> points() {
      return points;
    }

    /**
     * The meta-property for the {@code dateCalculation} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DenseTimeSeriesCalculation> dateCalculation() {
      return dateCalculation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((DenseLocalDateDoubleTimeSeries) bean).getStartDate();
        case -982754077:  // points
          return ((DenseLocalDateDoubleTimeSeries) bean).getPoints();
        case -152592837:  // dateCalculation
          return ((DenseLocalDateDoubleTimeSeries) bean).getDateCalculation();
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
   * The bean-builder for {@code DenseLocalDateDoubleTimeSeries}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<DenseLocalDateDoubleTimeSeries> {

    private LocalDate startDate;
    private double[] points;
    private DenseTimeSeriesCalculation dateCalculation;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -982754077:  // points
          return points;
        case -152592837:  // dateCalculation
          return dateCalculation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -982754077:  // points
          this.points = (double[]) newValue;
          break;
        case -152592837:  // dateCalculation
          this.dateCalculation = (DenseTimeSeriesCalculation) newValue;
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
    public DenseLocalDateDoubleTimeSeries build() {
      return new DenseLocalDateDoubleTimeSeries(
          startDate,
          points,
          dateCalculation);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("DenseLocalDateDoubleTimeSeries.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("points").append('=').append(JodaBeanUtils.toString(points)).append(',').append(' ');
      buf.append("dateCalculation").append('=').append(JodaBeanUtils.toString(dateCalculation)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
