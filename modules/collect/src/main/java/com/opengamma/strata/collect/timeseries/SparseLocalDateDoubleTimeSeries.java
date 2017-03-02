/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.function.ObjDoublePredicate;

/**
 * A immutable implementation of {@code LocalDateDoubleTimeSeries} where the
 * data stored is expected to be relatively sparse.
 * <p>
 * A sparse time-series has a relatively low density of dates with values.
 * For example, a few points spread throughout a year.
 * If more or less continuous data is being used then {@link DenseLocalDateDoubleTimeSeries}
 * is likely to be a better choice for the data.
 * <p>
 * This implementation uses arrays internally.
 */
@BeanDefinition(builderScope = "private", metaScope = "package")
final class SparseLocalDateDoubleTimeSeries
    implements ImmutableBean, Serializable, LocalDateDoubleTimeSeries {

  /**
   * An empty time-series.
   */
  static final LocalDateDoubleTimeSeries EMPTY =
      new SparseLocalDateDoubleTimeSeries(new LocalDate[0], new double[0]);

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
   * Obtains a time-series from matching arrays of dates and values.
   * <p>
   * The two arrays must be the same size and must be sorted from earliest to latest.
   *
   * @param dates  the date list
   * @param values  the value list
   * @return the time-series
   */
  static SparseLocalDateDoubleTimeSeries of(Collection<LocalDate> dates, Collection<Double> values) {
    ArgChecker.noNulls(dates, "dates");
    ArgChecker.noNulls(values, "values");
    LocalDate[] datesArray = dates.toArray(new LocalDate[dates.size()]);
    double[] valuesArray = Doubles.toArray(values);
    validate(datesArray, valuesArray);
    return createUnsafe(datesArray, valuesArray);
  }

  // creates time-series by directly assigning the input arrays
  // must only be called when safe to do so
  private static SparseLocalDateDoubleTimeSeries createUnsafe(LocalDate[] dates, double[] values) {
    return new SparseLocalDateDoubleTimeSeries(dates, values, true);
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
  private SparseLocalDateDoubleTimeSeries(LocalDate[] dates, double[] values) {
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
  private SparseLocalDateDoubleTimeSeries(LocalDate[] dates, double[] values, boolean trusted) {
    // constructor exists to avoid clones where possible
    // because Joda-Beans owns the main constructor, this one has a weird flag
    // use createUnsafe() instead of calling this directly
    this.dates = dates;
    this.values = values;
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

  //-------------------------------------------------------------------------
  @Override
  public int size() {
    return dates.length;
  }

  @Override
  public boolean isEmpty() {
    return dates.length == 0;
  }

  @Override
  public boolean containsDate(LocalDate date) {
    return (findDatePosition(date) >= 0);
  }

  @Override
  public OptionalDouble get(LocalDate date) {
    int position = findDatePosition(date);
    return (position >= 0 ? OptionalDouble.of(values[position]) : OptionalDouble.empty());
  }

  private int findDatePosition(LocalDate date) {
    return Arrays.binarySearch(dates, date);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getLatestDate() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest, time-series is empty");
    }
    return dates[dates.length - 1];
  }

  @Override
  public double getLatestValue() {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest, time-series is empty");
    }
    return values[values.length - 1];
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries subSeries(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startInclusive");
    ArgChecker.notNull(endExclusive, "endExclusive");
    if (endExclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException(
          "Invalid sub series, end before start: " + startInclusive + " to " + endExclusive);
    }
    // special case when this is empty or when the dates are the same
    if (isEmpty() || startInclusive.equals(endExclusive)) {
      return EMPTY;
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

  @Override
  public LocalDateDoubleTimeSeries headSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");
    if (numPoints == 0) {
      return EMPTY;
    } else if (numPoints >= size()) {
      return this;
    }
    LocalDate[] datesArray = Arrays.copyOfRange(dates, 0, numPoints);
    double[] valuesArray = Arrays.copyOfRange(values, 0, numPoints);
    return createUnsafe(datesArray, valuesArray);
  }

  @Override
  public LocalDateDoubleTimeSeries tailSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");
    if (numPoints == 0) {
      return EMPTY;
    } else if (numPoints >= size()) {
      return this;
    }
    LocalDate[] datesArray = Arrays.copyOfRange(dates, size() - numPoints, size());
    double[] valuesArray = Arrays.copyOfRange(values, size() - numPoints, size());
    return createUnsafe(datesArray, valuesArray);
  }

  //-------------------------------------------------------------------------
  @Override
  public Stream<LocalDateDoublePoint> stream() {
    return IntStream.range(0, size()).mapToObj(i -> LocalDateDoublePoint.of(dates[i], values[i]));
  }

  @Override
  public Stream<LocalDate> dates() {
    return Stream.of(dates);
  }

  @Override
  public DoubleStream values() {
    return DoubleStream.of(values);
  }

  //-------------------------------------------------------------------------
  @Override
  public void forEach(ObjDoubleConsumer<LocalDate> action) {
    ArgChecker.notNull(action, "action");
    for (int i = 0; i < size(); i++) {
      action.accept(dates[i], values[i]);
    }
  }

  @Override
  public LocalDateDoubleTimeSeries mapDates(Function<? super LocalDate, ? extends LocalDate> mapper) {
    ArgChecker.notNull(mapper, "mapper");
    LocalDate[] dates = Arrays.stream(this.dates).map(mapper).toArray(size -> new LocalDate[size]);
    // Check the dates are still in ascending order after the mapping
    Arrays.stream(dates).reduce(this::checkAscending);
    return createUnsafe(dates, values);
  }

  @Override
  public LocalDateDoubleTimeSeries mapValues(DoubleUnaryOperator mapper) {
    ArgChecker.notNull(mapper, "mapper");
    return createUnsafe(dates, DoubleStream.of(values).map(mapper).toArray());
  }

  @Override
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

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeriesBuilder toBuilder() {
    return new LocalDateDoubleTimeSeriesBuilder(dates, values);
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
    if (obj instanceof SparseLocalDateDoubleTimeSeries) {
      SparseLocalDateDoubleTimeSeries other = (SparseLocalDateDoubleTimeSeries) obj;
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

  //--------------------------------------------------------------------------------------------------

  /**
   * Checks the dates are in ascending order, throws an exception if not.
   *
   * @param earlier  the date that should be earlier
   * @param later  the date that should be later
   * @return the later date if it is after the earlier date, otherwise throw an exception
   * @throws IllegalArgumentException if the dates are not in ascending order
   */
  private LocalDate checkAscending(LocalDate earlier, LocalDate later) {
    if (earlier.isBefore(later)) {
      return later;
    }
    throw new IllegalArgumentException(
        Messages.format(
            "Dates must be in ascending order after calling mapDates but {} and {} are not",
            earlier,
            later));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SparseLocalDateDoubleTimeSeries}.
   * @return the meta-bean, not null
   */
  public static SparseLocalDateDoubleTimeSeries.Meta meta() {
    return SparseLocalDateDoubleTimeSeries.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SparseLocalDateDoubleTimeSeries.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SparseLocalDateDoubleTimeSeries.Meta metaBean() {
    return SparseLocalDateDoubleTimeSeries.Meta.INSTANCE;
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
   * The meta-bean for {@code SparseLocalDateDoubleTimeSeries}.
   */
  static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code dates} property.
     */
    private final MetaProperty<LocalDate[]> dates = DirectMetaProperty.ofImmutable(
        this, "dates", SparseLocalDateDoubleTimeSeries.class, LocalDate[].class);
    /**
     * The meta-property for the {@code values} property.
     */
    private final MetaProperty<double[]> values = DirectMetaProperty.ofImmutable(
        this, "values", SparseLocalDateDoubleTimeSeries.class, double[].class);
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
    public BeanBuilder<? extends SparseLocalDateDoubleTimeSeries> builder() {
      return new SparseLocalDateDoubleTimeSeries.Builder();
    }

    @Override
    public Class<? extends SparseLocalDateDoubleTimeSeries> beanType() {
      return SparseLocalDateDoubleTimeSeries.class;
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
          return ((SparseLocalDateDoubleTimeSeries) bean).getDates();
        case -823812830:  // values
          return ((SparseLocalDateDoubleTimeSeries) bean).getValues();
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
   * The bean-builder for {@code SparseLocalDateDoubleTimeSeries}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SparseLocalDateDoubleTimeSeries> {

    private LocalDate[] dates;
    private double[] values;

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
    public SparseLocalDateDoubleTimeSeries build() {
      return new SparseLocalDateDoubleTimeSeries(
          dates,
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("SparseLocalDateDoubleTimeSeries.Builder{");
      buf.append("dates").append('=').append(JodaBeanUtils.toString(dates)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
