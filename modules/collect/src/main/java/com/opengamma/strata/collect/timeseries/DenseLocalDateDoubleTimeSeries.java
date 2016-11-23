/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoUnit.DAYS;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.function.ObjDoublePredicate;

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
class DenseLocalDateDoubleTimeSeries
    implements ImmutableBean, LocalDateDoubleTimeSeries, Serializable {

  /**
   * Enum indicating whether there are positions in the points
   * array for weekends and providing the different date
   * calculations for each case.
   */
  enum DenseTimeSeriesCalculation {
    /**
     * Data is not held for weekends.
     */
    SKIP_WEEKENDS {
      @Override
      int calculatePosition(LocalDate startDate, LocalDate date) {
        int unadjusted = (int) DAYS.between(startDate, date);
        // If the day for the start date is after the day of the date of
        // interest then there is an additional weekend that the
        // integer division will not handle
        // compare:
        //   Tues 8th -> Wed 16th, 8 days span, 8 / 7 = 1 weekend, correct so not further adjustment
        //   Tues 8th -> Mon 14th, 6 days span, 6 / 7 = 0 weekend, incorrect so we need to add adjustment
        int weekendAdjustment = startDate.getDayOfWeek().compareTo(date.getDayOfWeek()) > 0 ? 1 : 0;
        int numWeekends = (unadjusted / 7) + weekendAdjustment;
        return unadjusted - (2 * numWeekends);
      }

      @Override
      LocalDate calculateDateFromPosition(LocalDate startDate, int position) {
        int numWeekends = position / 5;
        int remaining = position % 5;
        // As above we add adjustment for an uncaptured weekend
        int endPointAdjustment = (remaining < (6 - startDate.get(DAY_OF_WEEK))) ? 0 : 2;
        return startDate.plusDays((7 * numWeekends) + remaining + endPointAdjustment);
      }

      @Override
      boolean allowsDate(LocalDate date) {
        return !isWeekend(date);
      }

      @Override
      public LocalDate adjustDate(LocalDate date) {
        return allowsDate(date) ? date : date.plusDays(8 - date.get(DAY_OF_WEEK));
      }

    },
    /**
     * Data is held for weekends.
     */
    INCLUDE_WEEKENDS {
      @Override
      int calculatePosition(LocalDate startDate, LocalDate date) {
        return (int) DAYS.between(startDate, date);
      }

      @Override
      LocalDate calculateDateFromPosition(LocalDate startDate, int position) {
        return startDate.plusDays(position);
      }

      @Override
      boolean allowsDate(LocalDate date) {
        return true;
      }

      @Override
      public LocalDate adjustDate(LocalDate date) {
        return date;
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

    /**
     * Indicates if the specified date would be a possible date
     * for the calculation.
     *
     * @param date  the date to check
     * @return true if the calculation would allow the date
     */
    abstract boolean allowsDate(LocalDate date);

    /**
     * Adjusts the supplied data such that it is a valid
     * date from the calculation's point of view.
     *
     * @param date  the date to adjust
     * @return the adjusted date
     */
    public abstract LocalDate adjustDate(LocalDate date);

    // Sufficient for the moment, in the future we may need to
    // vary depending on a non-Western weekend
    private static boolean isWeekend(LocalDate date) {
      return date.get(DAY_OF_WEEK) > 5;
    }
  }

  /**
   * Date corresponding to first element in the array. All other
   * values can be calculated using date arithmetic to find
   * correct point.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;

  /**
   * The values in the series.
   * The date for each value is calculated using the position
   * in the array and the start date.
   */
  @PropertyDefinition(get = "private", validate = "notNull")
  private final double[] points;

  /**
   * Whether we should store data for the weekends (NaN will be stored
   * if no data is available).
   */
  @PropertyDefinition(get = "private", validate = "notNull")
  private final DenseTimeSeriesCalculation dateCalculation;

  /**
   * Package protected factory method intended to be called
   * by the {@link LocalDateDoubleTimeSeriesBuilder}. As such
   * all the information passed is assumed to be consistent.
   *
   * @param startDate  the earliest date included in the time-series
   * @param endDate  the latest date included in the time-series
   * @param values  stream holding the time-series points
   * @param dateCalculation  the date calculation method to be used
   * @return a new time-series
   */
  static LocalDateDoubleTimeSeries of(
      LocalDate startDate,
      LocalDate endDate,
      Stream<LocalDateDoublePoint> values,
      DenseTimeSeriesCalculation dateCalculation) {

    double[] points = new double[dateCalculation.calculatePosition(startDate, endDate) + 1];
    Arrays.fill(points, Double.NaN);
    values.forEach(pt -> points[dateCalculation.calculatePosition(startDate, pt.getDate())] = pt.getValue());
    return new DenseLocalDateDoubleTimeSeries(startDate, points, dateCalculation, true);
  }

  // Private constructor, the trusted flag indicates whether the
  // points array should be cloned. If trusted, it will not be cloned.
  private DenseLocalDateDoubleTimeSeries(
      LocalDate startDate,
      double[] points,
      DenseTimeSeriesCalculation dateCalculation,
      boolean trusted) {

    ArgChecker.notNull(points, "points");
    this.startDate = ArgChecker.notNull(startDate, "startDate");
    this.points = trusted ? points : points.clone();
    this.dateCalculation = ArgChecker.notNull(dateCalculation, "dateCalculation");
  }

  @ImmutableConstructor
  private DenseLocalDateDoubleTimeSeries(
      LocalDate startDate,
      double[] points,
      DenseTimeSeriesCalculation dateCalculation) {
    this(startDate, points, dateCalculation, false);
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
    return get(date).isPresent();
  }

  @Override
  public OptionalDouble get(LocalDate date) {
    if (!isEmpty() && !date.isBefore(startDate) && dateCalculation.allowsDate(date)) {
      int position = dateCalculation.calculatePosition(startDate, date);
      if (position < points.length) {
        double value = points[position];
        if (isValidPoint(value)) {
          return OptionalDouble.of(value);
        }
      }
    }
    return OptionalDouble.empty();
  }

  //-------------------------------------------------------------------------
  private IntStream reversedValidIndices() {
    // As there is no way of constructing an IntStream from
    // n to m where n > m, we go from -n to m and then
    // take the additive inverse (sigh!)
    return IntStream.rangeClosed(1 - points.length, 0)
        .map(i -> -i)
        .filter(this::isValidIndex);
  }

  private LocalDate calculateDateFromPosition(int i) {
    return dateCalculation.calculateDateFromPosition(startDate, i);
  }

  @Override
  public LocalDate getLatestDate() {
    return reversedValidIndices()
        .mapToObj(this::calculateDateFromPosition)
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Unable to return latest date, time-series is empty"));
  }

  @Override
  public double getLatestValue() {
    return reversedValidIndices()
        .mapToDouble(i -> points[i])
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Unable to return latest value, time-series is empty"));
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
    // or the series don't intersect
    if (isEmpty() || startInclusive.equals(endExclusive) ||
        !startDate.isBefore(endExclusive) ||
        startInclusive.isAfter(getLatestDate())) {
      return LocalDateDoubleTimeSeries.empty();
    }

    LocalDate resolvedStart = dateCalculation.adjustDate(Ordering.natural().max(startInclusive, startDate));
    int startIndex = dateCalculation.calculatePosition(startDate, resolvedStart);
    int endIndex = dateCalculation.calculatePosition(startDate, endExclusive);
    return new DenseLocalDateDoubleTimeSeries(
        resolvedStart,
        Arrays.copyOfRange(points, Math.max(0, startIndex), Math.min(points.length, endIndex)),
        dateCalculation,
        true);
  }

  @Override
  public LocalDateDoubleTimeSeries headSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");

    if (numPoints == 0) {
      return LocalDateDoubleTimeSeries.empty();
    } else if (numPoints > size()) {
      return this;
    }
    int endPosition = findHeadPoints(numPoints);
    return new DenseLocalDateDoubleTimeSeries(startDate, Arrays.copyOf(points, endPosition), dateCalculation);
  }

  private int findHeadPoints(int required) {
    // Take enough points that aren't NaN
    // else we need the entire series
    return validIndices()
        .skip(required)
        .findFirst()
        .orElse(points.length);
  }

  @Override
  public LocalDateDoubleTimeSeries tailSeries(int numPoints) {
    ArgChecker.notNegative(numPoints, "numPoints");

    if (numPoints == 0) {
      return LocalDateDoubleTimeSeries.empty();
    } else if (numPoints > size()) {
      return this;
    }

    int startPoint = findTailPoints(numPoints);

    return new DenseLocalDateDoubleTimeSeries(
        calculateDateFromPosition(startPoint),
        Arrays.copyOfRange(points, startPoint, points.length),
        dateCalculation);
  }

  private int findTailPoints(int required) {
    return reversedValidIndices()
        .skip(required - 1)
        .findFirst()
        .orElse(0);
  }

  //-------------------------------------------------------------------------
  @Override
  public Stream<LocalDateDoublePoint> stream() {
    return validIndices()
        .mapToObj(i -> LocalDateDoublePoint.of(calculateDateFromPosition(i), points[i]));
  }

  @Override
  public DoubleStream values() {
    return Arrays.stream(points).filter(this::isValidPoint);
  }

  @Override
  public Stream<LocalDate> dates() {
    return validIndices()
        .mapToObj(this::calculateDateFromPosition);
  }

  private IntStream validIndices() {
    return IntStream.range(0, points.length)
        .filter(this::isValidIndex);
  }

  private boolean isValidIndex(int i) {
    return isValidPoint(points[i]);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries filter(ObjDoublePredicate<LocalDate> predicate) {
    Stream<LocalDateDoublePoint> filteredPoints =
        stream().filter(pt -> predicate.test(pt.getDate(), pt.getValue()));

    // As we may have changed the density of the series by filtering
    // go via the builder to get the best implementation
    return new LocalDateDoubleTimeSeriesBuilder(filteredPoints).build();
  }

  @Override
  public LocalDateDoubleTimeSeries mapDates(Function<? super LocalDate, ? extends LocalDate> mapper) {
    List<LocalDate> dates = dates().map(mapper).collect(toImmutableList());
    dates.stream().reduce(this::checkAscending);
    return LocalDateDoubleTimeSeries.builder().putAll(dates, Doubles.asList(points)).build();
  }

  @Override
  public LocalDateDoubleTimeSeries mapValues(DoubleUnaryOperator mapper) {
    DoubleStream values = DoubleStream.of(points).map(d -> isValidPoint(d) ? applyMapper(mapper, d) : d);
    return new DenseLocalDateDoubleTimeSeries(startDate, values.toArray(), dateCalculation, true);
  }

  private double applyMapper(DoubleUnaryOperator mapper, double d) {
    double value = mapper.applyAsDouble(d);
    if (!isValidPoint(value)) {
      throw new IllegalArgumentException("Mapper must not map to NaN");
    }
    return value;
  }

  private boolean isValidPoint(double d) {
    return !Double.isNaN(d);
  }

  @Override
  public void forEach(ObjDoubleConsumer<LocalDate> action) {
    validIndices().forEach(i -> action.accept(calculateDateFromPosition(i), points[i]));
  }

  @Override
  public LocalDateDoubleTimeSeriesBuilder toBuilder() {
    return new LocalDateDoubleTimeSeriesBuilder(stream());
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
   * correct point.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the values in the series.
   * The date for each value is calculated using the position
   * in the array and the start date.
   * @return the value of the property, not null
   */
  private double[] getPoints() {
    return points.clone();
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
      return JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(points, other.points) &&
          JodaBeanUtils.equal(dateCalculation, other.dateCalculation);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(points);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateCalculation);
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
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
    buf.append("points").append('=').append(JodaBeanUtils.toString(points)).append(',').append(' ');
    buf.append("dateCalculation").append('=').append(JodaBeanUtils.toString(dateCalculation)).append(',').append(' ');
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
