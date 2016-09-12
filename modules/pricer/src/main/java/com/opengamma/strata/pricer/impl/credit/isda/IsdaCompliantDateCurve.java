/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.util.Map;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An ISDA compliant date curve.
 */
@BeanDefinition
public class IsdaCompliantDateCurve
    extends IsdaCompliantCurve
    implements IsdaCompliantCurveWithDates {

  /**
   * The standard ACT/365 day count.
   */
  private static final DayCount ACT_365 = DayCounts.ACT_365F;

  /**
   * The base date.
   */
  @PropertyDefinition(set = "private", overrideGet = true)
  private LocalDate baseDate;
  /**
   * The knot dates on the curve.
   */
  @PropertyDefinition(get = "private", set = "private")
  private LocalDate[] dates;
  /**
   * The day count.
   */
  @PropertyDefinition(get = "private", set = "private")
  private DayCount dayCount;

  //-------------------------------------------------------------------------
  protected static IsdaCompliantCurve makeIsdaCompliantCurve(
      LocalDate baseDate, LocalDate[] dates, double[] rates, DayCount dayCount) {
    double[] t = checkAndGetTimes(baseDate, dates, rates, dayCount);
    return new IsdaCompliantCurve(t, rates);
  }

  //-------------------------------------------------------------------------
  /**
   * Constructor for Joda-Beans.
   */
  protected IsdaCompliantDateCurve() {
  }

  /**
   * Builds a curve from a baseDate with a set of <b>continually compounded</b> zero rates at given knot dates
   * The times (year-fractions) between the baseDate and the knot dates is calculated using ACT/365.
   * 
   * @param baseDate  the base date for the curve (i.e. this is time zero), not null
   * @param dates  the knot dates on the curve. These must be ascending with the first date after the baseDate, not null
   * @param rates  the continually compounded zero rates at given knot dates, not null
   */
  public IsdaCompliantDateCurve(LocalDate baseDate, LocalDate[] dates, double[] rates) {
    this(baseDate, dates, rates, ACT_365);
  }

  /**
   * Builds a curve from a baseDate with a set of <b>continually compounded</b> zero rates at given knot dates.
   * The times (year-fractions) between the baseDate and the knot dates is calculated using the specified day-count-convention.
   * 
   * @param baseDate  the base date for the curve (i.e. this is time zero), not null
   * @param dates  the knot dates on the curve. These must be ascending with the first date after the baseDate, not null
   * @param rates  the continually compounded zero rates at given knot dates, not null
   * @param dayCount  the day-count-convention, not null
   */
  public IsdaCompliantDateCurve(LocalDate baseDate, LocalDate[] dates, double[] rates, DayCount dayCount) {
    this(baseDate, dates, dayCount, makeIsdaCompliantCurve(baseDate, dates, rates, dayCount));
  }

  private IsdaCompliantDateCurve(LocalDate baseDate, LocalDate[] dates, DayCount dayCount, IsdaCompliantCurve baseCurve) {
    super(baseCurve);
    this.baseDate = baseDate;
    this.dates = dates;
    this.dayCount = dayCount;
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getCurveDate(int index) {
    return dates[index];
  }

  @Override
  public LocalDate[] getCurveDates() {
    return dates.clone();
  }

  @Override
  public IsdaCompliantDateCurve withParameter(int parameterIndex, double newValue) {
    IsdaCompliantCurve temp = super.withParameter(parameterIndex, newValue);
    return new IsdaCompliantDateCurve(baseDate, dates, dayCount, temp);
  }

  @Override
  public IsdaCompliantDateCurve withRate(double rate, int index) {
    IsdaCompliantCurve temp = super.withRate(rate, index);
    return new IsdaCompliantDateCurve(baseDate, dates, dayCount, temp);
  }

  protected static double[] checkAndGetTimes(LocalDate baseDate, LocalDate[] dates, double[] rates) {
    return checkAndGetTimes(baseDate, dates, rates, ACT_365);
  }

  protected static double[] checkAndGetTimes(LocalDate baseDate, LocalDate[] dates, double[] rates, DayCount dayCount) {
    ArgChecker.notNull(baseDate, "null baseDate");
    ArgChecker.notNull(dayCount, "null dayCount");
    ArgChecker.noNulls(dates, "null dates");
    ArgChecker.notEmpty(rates, "empty rates");
    ArgChecker.isTrue(dates[0].isAfter(baseDate), "first date is not after base date");
    int n = dates.length;
    ArgChecker.isTrue(rates.length == n, "rates and dates different lengths");
    double[] t = new double[n];
    for (int i = 0; i < n; i++) {
      t[i] = dayCount.yearFraction(baseDate, dates[i]);
      if (i > 0) {
        ArgChecker.isTrue(t[i] > t[i - 1], "dates are not ascending");
      }
    }
    return t;
  }

  @Override
  public double getZeroRate(LocalDate date) {
    double t = dayCount.yearFraction(baseDate, date);
    return getZeroRate(t);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IsdaCompliantDateCurve}.
   * @return the meta-bean, not null
   */
  public static IsdaCompliantDateCurve.Meta meta() {
    return IsdaCompliantDateCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IsdaCompliantDateCurve.Meta.INSTANCE);
  }

  @Override
  public IsdaCompliantDateCurve.Meta metaBean() {
    return IsdaCompliantDateCurve.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base date.
   * @return the value of the property
   */
  @Override
  public LocalDate getBaseDate() {
    return baseDate;
  }

  /**
   * Sets the base date.
   * @param baseDate  the new value of the property
   */
  private void setBaseDate(LocalDate baseDate) {
    this.baseDate = baseDate;
  }

  /**
   * Gets the the {@code baseDate} property.
   * @return the property, not null
   */
  public final Property<LocalDate> baseDate() {
    return metaBean().baseDate().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the knot dates on the curve.
   * @return the value of the property
   */
  private LocalDate[] getDates() {
    return dates;
  }

  /**
   * Sets the knot dates on the curve.
   * @param dates  the new value of the property
   */
  private void setDates(LocalDate[] dates) {
    this.dates = dates;
  }

  /**
   * Gets the the {@code dates} property.
   * @return the property, not null
   */
  public final Property<LocalDate[]> dates() {
    return metaBean().dates().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count.
   * @return the value of the property
   */
  private DayCount getDayCount() {
    return dayCount;
  }

  /**
   * Sets the day count.
   * @param dayCount  the new value of the property
   */
  private void setDayCount(DayCount dayCount) {
    this.dayCount = dayCount;
  }

  /**
   * Gets the the {@code dayCount} property.
   * @return the property, not null
   */
  public final Property<DayCount> dayCount() {
    return metaBean().dayCount().createProperty(this);
  }

  //-----------------------------------------------------------------------
  @Override
  public IsdaCompliantDateCurve clone() {
    return JodaBeanUtils.cloneAlways(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IsdaCompliantDateCurve other = (IsdaCompliantDateCurve) obj;
      return JodaBeanUtils.equal(getBaseDate(), other.getBaseDate()) &&
          JodaBeanUtils.equal(getDates(), other.getDates()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IsdaCompliantDateCurve{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  @Override
  protected void toString(StringBuilder buf) {
    super.toString(buf);
    buf.append("baseDate").append('=').append(JodaBeanUtils.toString(getBaseDate())).append(',').append(' ');
    buf.append("dates").append('=').append(JodaBeanUtils.toString(getDates())).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IsdaCompliantDateCurve}.
   */
  public static class Meta extends IsdaCompliantCurve.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code baseDate} property.
     */
    private final MetaProperty<LocalDate> baseDate = DirectMetaProperty.ofReadWrite(
        this, "baseDate", IsdaCompliantDateCurve.class, LocalDate.class);
    /**
     * The meta-property for the {@code dates} property.
     */
    private final MetaProperty<LocalDate[]> dates = DirectMetaProperty.ofReadWrite(
        this, "dates", IsdaCompliantDateCurve.class, LocalDate[].class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofReadWrite(
        this, "dayCount", IsdaCompliantDateCurve.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "baseDate",
        "dates",
        "dayCount");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1721984481:  // baseDate
          return baseDate;
        case 95356549:  // dates
          return dates;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IsdaCompliantDateCurve> builder() {
      return new DirectBeanBuilder<IsdaCompliantDateCurve>(new IsdaCompliantDateCurve());
    }

    @Override
    public Class<? extends IsdaCompliantDateCurve> beanType() {
      return IsdaCompliantDateCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code baseDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> baseDate() {
      return baseDate;
    }

    /**
     * The meta-property for the {@code dates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate[]> dates() {
      return dates;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1721984481:  // baseDate
          return ((IsdaCompliantDateCurve) bean).getBaseDate();
        case 95356549:  // dates
          return ((IsdaCompliantDateCurve) bean).getDates();
        case 1905311443:  // dayCount
          return ((IsdaCompliantDateCurve) bean).getDayCount();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1721984481:  // baseDate
          ((IsdaCompliantDateCurve) bean).setBaseDate((LocalDate) newValue);
          return;
        case 95356549:  // dates
          ((IsdaCompliantDateCurve) bean).setDates((LocalDate[]) newValue);
          return;
        case 1905311443:  // dayCount
          ((IsdaCompliantDateCurve) bean).setDayCount((DayCount) newValue);
          return;
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
