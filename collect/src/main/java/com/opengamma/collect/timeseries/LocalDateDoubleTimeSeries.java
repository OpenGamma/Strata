/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
import com.google.common.primitives.Doubles;
import com.opengamma.collect.Guavate;
import com.opengamma.collect.function.ObjDoublePredicate;
import com.opengamma.collect.validate.ArgChecker;

/**
 * Standard immutable implementation of {@code DoubleTimeSeries}.
 * <p>
 * This implementation uses arrays internally.
 */
@BeanDefinition(builderScope = "private")
public final class LocalDateDoubleTimeSeries
    implements ImmutableBean, DoubleTimeSeries<LocalDate>, Serializable {

  /** Empty instance. */
  public static final LocalDateDoubleTimeSeries EMPTY_SERIES = createUnsafe(new LocalDate[0], new double[0]);

  /** Serialization version. */
  private static final long serialVersionUID = -43654613865187568L;

  /**
   * The dates in the series.
   * The dates are ordered from earliest to latest.
   */
  @PropertyDefinition(get = "manual", validate = "notNull")
  private final LocalDate[] dates;
  /**
   * The values in the series.
   * The date for each value is at the matching array index.
   */
  @PropertyDefinition(get = "manual", validate = "notNull")
  private final double[] values;

  //-------------------------------------------------------------------------
  /**
   * Creates an empty builder, used to create time-series.
   * <p>
   * The builder has methods to create and modify a time-series.
   *
   * @return the time-series builder
   */
  public static LocalDateDoubleTimeSeriesBuilder builder() {
    return new LocalDateDoubleTimeSeriesBuilder();
  }

  /**
   * Returns a collector that can be used to create a time-series from a stream of points.
   *
   * @return the time-series builder
   */
  public static Collector<LocalDateDoublePoint, LocalDateDoubleTimeSeriesBuilder, LocalDateDoubleTimeSeries> collector() {
    return Collector.of(
        LocalDateDoubleTimeSeriesBuilder::new,
        LocalDateDoubleTimeSeriesBuilder::put,
        LocalDateDoubleTimeSeriesBuilder::putAll,
        LocalDateDoubleTimeSeriesBuilder::build);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a time-series from a single date and value.
   *
   * @param date  the singleton date
   * @param value  the singleton value
   * @return the time-series
   */
  public static LocalDateDoubleTimeSeries of(LocalDate date, double value) {
    return new LocalDateDoubleTimeSeries(date, value);  // validated in constructor
  }

  /**
   * Obtains a time-series from matching arrays of dates and values.
   * <p>
   * The two arrays must be the same size and must be sorted from earliest to latest.
   *
   * @param dates  the date list
   * @param values  the value list
   * @return the time-series
   */
  public static LocalDateDoubleTimeSeries of(Collection<LocalDate> dates, Collection<Double> values) {
    ArgChecker.noNulls(dates, "dates");
    ArgChecker.noNulls(values, "values");
    LocalDate[] datesArray = dates.toArray(new LocalDate[dates.size()]);
    double[] valuesArray = Doubles.toArray(values);
    validate(datesArray, valuesArray);
    return createUnsafe(datesArray, valuesArray);
  }

  /**
   * Obtains a time-series from a map of dates and values.
   *
   * @param map  the map of dates to values
   * @return the time-series
   */
  public static LocalDateDoubleTimeSeries of(Map<LocalDate, Double> map) {
    ArgChecker.noNulls(map, "map");
    Set<Entry<LocalDate, Double>> set = map.entrySet();
    LocalDate[] datesArray = new LocalDate[set.size()];
    double[] valuesArray = new double[set.size()];
    int i = 0;
    for (Entry<LocalDate, Double> entry : set) {
      datesArray[i] = entry.getKey();
      valuesArray[i] = entry.getValue();
      i++;
    }
    validate(datesArray, valuesArray);
    return createUnsafe(datesArray, valuesArray);
  }

  /**
   * Obtains a time-series from a collection of points.
   * <p>
   * The collection must be sorted from earliest to latest.
   *
   * @param points  the list of points
   * @return the time-series
   */
  public static LocalDateDoubleTimeSeries of(Collection<LocalDateDoublePoint> points) {
    ArgChecker.noNulls(points, "points");
    LocalDate[] datesArray = points.stream()
        .map(LocalDateDoublePoint::getDate)
        .toArray(LocalDate[]::new);
    double[] valuesArray = points.stream()
        .mapToDouble(LocalDateDoublePoint::getValue)
        .toArray();
    validate(datesArray, valuesArray);
    return createUnsafe(datesArray, valuesArray);
  }

  // creates time-series by directly assigning the input arrays
  // must only be called when safe to do so
  private static LocalDateDoubleTimeSeries createUnsafe(LocalDate[] dates, double[] values) {
    return new LocalDateDoubleTimeSeries(dates, values, true);
  }

  // validates the arrays are same length and in order
  private static void validate(LocalDate[] dates, double[] values) {
    ArgChecker.isTrue(dates.length == values.length,
        "Arrays are of different sizes - dates: {}, values: {}", dates.length, values.length);
    LocalDate maxDate = LocalDate.MIN;
    for (LocalDate date : dates) {
      ArgChecker.isTrue(date.isAfter(maxDate),
          "Dates must be in ascending order but: {} is not after: {}", date, maxDate);
      maxDate = date;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, validating the supplied arrays.
   * <p>
   * The arrays are cloned as this constructor is called from Joda-Beans.
   *
   * @param dates  the dates
   * @param values  the values
   */
  @ImmutableConstructor
  private LocalDateDoubleTimeSeries(LocalDate[] dates, double[] values) {
    ArgChecker.noNulls(dates, "dates");
    ArgChecker.notNull(values, "values");
    validate(dates, values);
    this.dates = dates.clone();
    this.values = values.clone();
  }

  /**
   * Creates an instance without validating the supplied arrays.
   *
   * @param dates  the dates
   * @param values  the values
   * @param trusted  flag to distinguish constructor
   */
  private LocalDateDoubleTimeSeries(LocalDate[] dates, double[] values, boolean trusted) {
    // constructor exists to avoid clones where possible
    // because Joda-Beans owns the main constructor, this one has a weird flag
    // use createUnsafe() instead of calling this directly
    this.dates = dates;
    this.values = values;
  }

  /**
   * Creates an instance validating the supplied arguments.
   *
   * @param date  the date
   * @param value  the value
   */
  private LocalDateDoubleTimeSeries(LocalDate date, double value) {
    dates = new LocalDate[] {ArgChecker.notNull(date, "date")};
    values = new double[] {value};
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the dates in the series.
   * The dates are ordered from earliest to latest.
   * @return the value of the property, not null
   */
  private LocalDate[] getDates() {
    return dates.clone();
  }

  /**
   * Gets the values in the series.
   * The date for each value is at the matching array index.
   * @return the value of the property, not null
   */
  private double[] getValues() {
    return values.clone();
  }

  /**
   * Returns the list of dates in this time-series.
   * <p>
   * This provides low-level access to the internal data.
   * The stream and lambda methods should be used in preference to this method.
   *
   * @return the list of dates in this time-series
   */
  public ImmutableList<LocalDate> dates() {
    return ImmutableList.copyOf(dates);
  }

  /**
   * Returns the list of values in this time-series.
   * <p>
   * This provides low-level access to the internal data.
   * The stream and lambda methods should be used in preference to this method.
   *
   * @return the list of values in this time-series
   */
  public ImmutableList<Double> values() {
    return ImmutableList.copyOf(Doubles.asList(values));
  }

  //-------------------------------------------------------------------------
  /**
   * Return the size of this time-series.
   *
   * @return the size of the time-series
   */
  @Override
  public int size() {
    return dates.length;
  }

  /**
   * Indicates if this time-series is empty.
   *
   * @return true if the time-series contains no entries
   */
  public boolean isEmpty() {
    return dates.length == 0;
  }

  /**
   * Checks if this time-series contains a value for the specified date.
   *
   * @param date  the date to check for
   * @return true if there is a value associated with the date
   */
  public boolean containsDate(LocalDate date) {
    return (findDatePosition(date) >= 0);
  }

  /**
   * Gets the value associated with the specified date.
   * <p>
   * The result is an {@link OptionalDouble} which avoids the need to handle null
   * or exceptions. Use {@code isPresent()} to check whether the value is present.
   * Use {@code orElse(double)} to default a missing value.
   *
   * @param date  the date to get the value for
   * @return the value associated with the date, empty if the date is not present
   */
  public OptionalDouble get(LocalDate date) {
    int position = findDatePosition(date);
    return (position >= 0 ? OptionalDouble.of(values[position]) : OptionalDouble.empty());
  }

  private int findDatePosition(LocalDate date) {
    return Arrays.binarySearch(dates, date);
  }

  //-------------------------------------------------------------------------
  /**
   * Get the earliest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the earliest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public LocalDate getEarliestDate() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return earliest, time-series is empty");
    }
    return dates[0];
  }

  /**
   * Get the value held for the earliest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the value held for the earliest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public double getEarliestValue() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return earliest, time-series is empty");
    }
    return values[0];
  }

  /**
   * Get the latest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the latest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public LocalDate getLatestDate() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest, time-series is empty");
    }
    return dates[dates.length - 1];
  }

  /**
   * Get the value held for the latest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the value held for the latest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public double getLatestValue() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest, time-series is empty");
    }
    return values[values.length - 1];
  }

  //-------------------------------------------------------------------------
  /**
   * Gets part of this series as a sub-series between two dates.
   * <p>
   * The date-times do not have to match exactly.
   * The sub-series contains all entries between the two dates using a half-open interval.
   * The start date is included, the end date is excluded.
   * The dates may be before or after the end of the time-series.
   * <p>
   * To obtain the series before a specific date, used {@code LocalDate.MIN} as the first argument.
   * To obtain the series from a specific date, used {@code LocalDate.MAX} as the second argument.
   *
   * @param startInclusive  the start date, inclusive
   * @param endExclusive  the end date, exclusive
   * @return the sub-series between the dates
   * @throws IllegalArgumentException if the end is before the start
   */
  public LocalDateDoubleTimeSeries subSeries(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startInclusive");
    ArgChecker.notNull(endExclusive, "endExclusive");
    if (endExclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException(
          "Invalid sub series, end before start: " + startInclusive + " to " + endExclusive);
    }
    // special case when this is empty or when the dates are the same
    if (isEmpty() || startInclusive.equals(endExclusive)) {
      return EMPTY_SERIES;
    }
    // where in the array would start/end be (whether or not it's actually in the series)
    int startPos = Arrays.binarySearch(dates, startInclusive);
    startPos = startPos >= 0 ? startPos : -startPos - 1;
    int endPos = Arrays.binarySearch(dates, endExclusive);
    endPos = endPos >= 0 ? endPos : -endPos - 1;
    // create sub-series
    LocalDate[] timesArray = Arrays.copyOfRange(dates, startPos, endPos);
    double[] valuesArray = Arrays.copyOfRange(values, startPos, endPos);
    return createUnsafe(timesArray, valuesArray);
  }

  /**
   * Gets part of this series as a sub-series, choosing the earliest entries.
   * <p>
   * The sub-series contains the earliest part of the series up to the specified number of points.
   * If the series contains less points than the number requested, the whole time-series is returned.
   *
   * @param numPoints  the number of items to select, zero or greater
   * @return the sub-series of the requested size starting with the earliest entry
   * @throws IllegalArgumentException if the number of items is less than zero
   */
  public LocalDateDoubleTimeSeries headSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");
    if (numPoints == 0) {
      return EMPTY_SERIES;
    } else if (numPoints >= size()) {
      return this;
    }
    LocalDate[] datesArray = Arrays.copyOfRange(dates, 0, numPoints);
    double[] valuesArray = Arrays.copyOfRange(values, 0, numPoints);
    return createUnsafe(datesArray, valuesArray);
  }

  /**
   * Gets part of this series as a sub-series, choosing the latest entries.
   * <p>
   * The sub-series contains the latest part of the series up to the specified number of points.
   * If the series contains less points than the number requested, the whole time-series is returned.
   *
   * @param numPoints  the number of items to select, zero or greater
   * @return the sub-series of the requested size ending with the latest entry
   * @throws IllegalArgumentException if the number of items is less than zero
   */
  public LocalDateDoubleTimeSeries tailSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");
    if (numPoints == 0) {
      return EMPTY_SERIES;
    } else if (numPoints >= size()) {
      return this;
    }
    LocalDate[] datesArray = Arrays.copyOfRange(dates, size() - numPoints, size());
    double[] valuesArray = Arrays.copyOfRange(values, size() - numPoints, size());
    return createUnsafe(datesArray, valuesArray);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a stream over the points of this time-series.
   * <p>
   * This provides access to the entire time-series.
   *
   * @return a stream over the points of this time-series
   */
  public Stream<LocalDateDoublePoint> stream() {
    return IntStream.range(0, size()).mapToObj(i -> LocalDateDoublePoint.of(dates[i], values[i]));
  }

  /**
   * Returns a stream over the dates of this time-series.
   * <p>
   * This is most useful to summarize the dates in the stream, such as calculating
   * the maximum or minimum date, or searching for a specific value.
   *
   * @return a stream over the values of this time-series
   */
  public Stream<LocalDate> dateStream() {
    return Stream.of(dates);
  }

  /**
   * Returns a stream over the values of this time-series.
   * <p>
   * This is most useful to summarize the values in the stream, such as calculating
   * the maximum, minimum or average value, or searching for a specific value.
   *
   * @return a stream over the values of this time-series
   */
  public DoubleStream valueStream() {
    return DoubleStream.of(values);
  }

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each pair in the time series.
   * <p>
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.forEach((date, value) -> System.out.println(date + "=" + value));
   * </pre>
   *
   * @param action  the action to be applied to each pair
   */
  public void forEach(ObjDoubleConsumer<LocalDate> action) {
    ArgChecker.notNull(action, "action");
    for (int i = 0; i < size(); i++) {
      action.accept(dates[i], values[i]);
    }
  }

  /**
   * Applies an operation to each value in the time series.
   * <p>
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.map(value -> value * 3);
   * </pre>
   *
   * @param mapper  the operator to be applied to the values
   * @return a copy of this series with the mapping applied to the original values
   */
  public LocalDateDoubleTimeSeries mapValues(DoubleUnaryOperator mapper) {
    ArgChecker.notNull(mapper, "mapper");
    return createUnsafe(dates, DoubleStream.of(values).map(mapper).toArray());
  }

  /**
   * Create a new time-series by filtering this one.
   * <p>
   * The time-series can be filtered by both date and value.
   * Note that if filtering by date range is required, it is likely to be more efficient to
   * use {@link #subSeries(LocalDate, LocalDate)} as that avoids traversal of the whole series.
   *
   * @param predicate  the predicate to use to the filter the elements of the series
   * @return a filtered version of the series
   */
  public LocalDateDoubleTimeSeries filter(ObjDoublePredicate<LocalDate> predicate) {
    ArgChecker.notNull(predicate, "predicate");
    // build up result in arrays keeping track of count of retained dates
    LocalDate[] resDates = new LocalDate[size()];
    double[] resValues = new double[size()];
    int resCount = 0;
    for (int i = 0; i < size(); i++) {
      if (predicate.test(dates[i], values[i])) {
        resDates[resCount] = dates[i];
        resValues[resCount] = values[i];
        resCount++;
      }
    }
    return createUnsafe(Arrays.copyOf(resDates, resCount), Arrays.copyOf(resValues, resCount));
  }

  /**
   * Combines a pair of time series, extracting the dates common to both and
   * applying a function to combine the values.
   *
   * @param other  the time-series to combine with
   * @param mapper  the function to be used to combine the values
   * @return a new time-series containing the dates in common between the
   *  input series with their values combined together using the function
   */
  public LocalDateDoubleTimeSeries combineWith(LocalDateDoubleTimeSeries other, DoubleBinaryOperator mapper) {
    ArgChecker.notNull(other, "other");
    ArgChecker.notNull(mapper, "mapper");
    // build up result in arrays keeping track of actual matching dates
    LocalDate[] resDates = new LocalDate[Math.min(size(), other.size())];
    double[] resValues = new double[resDates.length];
    int resCount = 0;
    // index into the arrays in this time-series
    int i = 0;
    int iMax = size();
    // index into the arrays in the other time-series
    int j = 0;
    int jMax = other.size();
    // loop around and exhaust each input
    while (i < iMax && j < jMax) {
      LocalDate date = dates[i];
      LocalDate otherDate = other.dates[j];
      if (date.equals(otherDate)) {
        resDates[resCount] = date;
        resValues[resCount] = mapper.applyAsDouble(values[i], other.values[j]);
        resCount++;
        i++;
        j++;
      } else if (date.isBefore(otherDate)) {
        i++;
      } else { // date is after otherDate
        j++;
      }
    }
    return createUnsafe(Arrays.copyOf(resDates, resCount), Arrays.copyOf(resValues, resCount));
  }

  //-------------------------------------------------------------------------
  /**
   * Return a builder populated with the values from this series.
   *
   * @return a time-series builder
   */
  public LocalDateDoubleTimeSeriesBuilder toBuilder() {
    return new LocalDateDoubleTimeSeriesBuilder(dates, values);
  }

  /**
   * Returns a map containing the dates and values of this time-series.
   * 
   * @return a map of the elements of this time-series
   */
  @Override
  public ImmutableSortedMap<LocalDate, Double> toMap() {
    return stream()
        .collect(Guavate.toImmutableSortedMap(LocalDateDoublePoint::getDate, LocalDateDoublePoint::getValue));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this time-series is equal to another time-series.
   * <p>
   * Compares this {@code LocalDateDoubleTimeSeries} with another ensuring
   * that the dates and values are the same.
   *
   * @param obj  the object to check, null returns false
   * @return true if this is equal to the other date
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LocalDateDoubleTimeSeries) {
      LocalDateDoubleTimeSeries other = (LocalDateDoubleTimeSeries) obj;
      return Arrays.equals(dates, other.dates) && Arrays.equals(values, other.values);
    }
    return false;
  }

  /**
   * A hash code for this time-series.
   *
   * @return a suitable hash code
   */
  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(dates) + Arrays.hashCode(values);
  }

  /**
   * Returns a string representation of the time-series.
   * 
   * @return the string
   */
  @Override
  public String toString() {
    return stream()
        .map(LocalDateDoublePoint::toString)
        .collect(Collectors.joining(", ", "[", "]"));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LocalDateDoubleTimeSeries}.
   * @return the meta-bean, not null
   */
  public static LocalDateDoubleTimeSeries.Meta meta() {
    return LocalDateDoubleTimeSeries.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(LocalDateDoubleTimeSeries.Meta.INSTANCE);
  }

  @Override
  public LocalDateDoubleTimeSeries.Meta metaBean() {
    return LocalDateDoubleTimeSeries.Meta.INSTANCE;
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
   * The meta-bean for {@code LocalDateDoubleTimeSeries}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code dates} property.
     */
    private final MetaProperty<LocalDate[]> dates = DirectMetaProperty.ofImmutable(
        this, "dates", LocalDateDoubleTimeSeries.class, LocalDate[].class);
    /**
     * The meta-property for the {@code values} property.
     */
    private final MetaProperty<double[]> values = DirectMetaProperty.ofImmutable(
        this, "values", LocalDateDoubleTimeSeries.class, double[].class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "dates",
        "values");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95356549:  // dates
          return dates;
        case -823812830:  // values
          return values;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends LocalDateDoubleTimeSeries> builder() {
      return new LocalDateDoubleTimeSeries.Builder();
    }

    @Override
    public Class<? extends LocalDateDoubleTimeSeries> beanType() {
      return LocalDateDoubleTimeSeries.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code dates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate[]> dates() {
      return dates;
    }

    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> values() {
      return values;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 95356549:  // dates
          return ((LocalDateDoubleTimeSeries) bean).getDates();
        case -823812830:  // values
          return ((LocalDateDoubleTimeSeries) bean).getValues();
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
   * The bean-builder for {@code LocalDateDoubleTimeSeries}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<LocalDateDoubleTimeSeries> {

    private LocalDate[] dates;
    private double[] values;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 95356549:  // dates
          return dates;
        case -823812830:  // values
          return values;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 95356549:  // dates
          this.dates = (LocalDate[]) newValue;
          break;
        case -823812830:  // values
          this.values = (double[]) newValue;
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
    public LocalDateDoubleTimeSeries build() {
      return new LocalDateDoubleTimeSeries(
          dates,
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("LocalDateDoubleTimeSeries.Builder{");
      buf.append("dates").append('=').append(JodaBeanUtils.toString(dates)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
