/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * A period over which a fixed or floating rate is accrued.
 * <p>
 * A swap leg consists of one or more periods that are the basis of accrual.
 * This class represents one such period.
 * <p>
 * This class specifies the data necessary to calculate the value of the period.
 * The key property is the {@link #getRateComputation() rateComputation} which defines
 * how the rate is observed.
 */
@BeanDefinition
public final class RateAccrualPeriod
    implements ImmutableBean, Serializable {

  /**
   * The start date of the accrual period.
   * <p>
   * This is the first accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the accrual period.
   * <p>
   * This is the last accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The unadjusted start date.
   * <p>
   * The start date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the start date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedStartDate;
  /**
   * The unadjusted end date.
   * <p>
   * The end date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the end date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedEndDate;
  /**
   * The year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;
  /**
   * The rate to be computed.
   * <p>
   * The value of the period is based on this rate.
   * Different implementations of the {@code RateComputation} interface have different
   * approaches to computing the rate, including averaging, overnight and interpolation.
   * For example, it might be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final RateComputation rateComputation;
  /**
   * The gearing multiplier, defaulted to 1.
   * <p>
   * This defines the gearing, which is used to multiply the observed rate.
   * <p>
   * When calculating the rate, the observed rate is multiplied by the gearing.
   * If both gearing and spread exist, then the gearing is applied first.
   * A gearing of 1 has no effect.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   */
  @PropertyDefinition
  private final double gearing;
  /**
   * The spread rate, defaulted to 0.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * This defines the spread, which is used to add an amount the observed rate.
   * <p>
   * When calculating the rate, the spread is added to the observed rate.
   * If both gearing and spread exist, then the gearing is applied first.
   * A spread of 0 has no effect.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   */
  @PropertyDefinition
  private final double spread;
  /**
   * The negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * <p>
   * When observing or calculating the rate, the value may go negative.
   * If it does, then this method is used to validate whether the negative rate is allowed.
   * It is applied after any applicable gearing or spread.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   */
  @PropertyDefinition(validate = "notNull")
  private final NegativeRateMethod negativeRateMethod;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.negativeRateMethod(NegativeRateMethod.ALLOW_NEGATIVE);
    builder.gearing(1d);
  }

  // could use @ImmutablePreBuild and @ImmutableValidate but faster inline
  @ImmutableConstructor
  private RateAccrualPeriod(
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      double yearFraction,
      RateComputation rateComputation,
      double gearing,
      double spread,
      NegativeRateMethod negativeRateMethod) {
    this.startDate = ArgChecker.notNull(startDate, "startDate");
    this.endDate = ArgChecker.notNull(endDate, "endDate");
    this.unadjustedStartDate = firstNonNull(unadjustedStartDate, startDate);
    this.unadjustedEndDate = firstNonNull(unadjustedEndDate, endDate);
    this.yearFraction = ArgChecker.notNegative(yearFraction, "yearFraction");
    this.rateComputation = ArgChecker.notNull(rateComputation, "rateComputation");
    this.gearing = gearing;
    this.spread = spread;
    this.negativeRateMethod = ArgChecker.notNull(negativeRateMethod, "negativeRateMethod");
    // check for unadjusted must be after firstNonNull
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderNotEqual(
        this.unadjustedStartDate, this.unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate");
  }

  // trusted constructor
  RateAccrualPeriod(SchedulePeriod period, double yearFraction, RateComputation rateComputation) {
    this(period, yearFraction, rateComputation, 1d, 0d, NegativeRateMethod.ALLOW_NEGATIVE);
  }

  // trusted constructor
  RateAccrualPeriod(
      SchedulePeriod period,
      double yearFraction,
      RateComputation rateComputation,
      double gearing,
      double spread,
      NegativeRateMethod negativeRateMethod) {

    this.startDate = period.getStartDate();
    this.endDate = period.getEndDate();
    this.unadjustedStartDate = period.getUnadjustedStartDate();
    this.unadjustedEndDate = period.getUnadjustedEndDate();
    this.yearFraction = yearFraction;
    this.rateComputation = rateComputation;
    this.gearing = gearing;
    this.spread = spread;
    this.negativeRateMethod = negativeRateMethod;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a builder used to create an instance of the bean, based on a schedule period.
   * <p>
   * The start date and end date (adjusted and unadjusted) will be set in the builder.
   * 
   * @param period  the schedule period
   * @return the builder, not null
   */
  public static RateAccrualPeriod.Builder builder(SchedulePeriod period) {
    return builder()
        .startDate(period.getStartDate())
        .endDate(period.getEndDate())
        .unadjustedStartDate(period.getUnadjustedStartDate())
        .unadjustedEndDate(period.getUnadjustedEndDate());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RateAccrualPeriod}.
   * @return the meta-bean, not null
   */
  public static RateAccrualPeriod.Meta meta() {
    return RateAccrualPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RateAccrualPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static RateAccrualPeriod.Builder builder() {
    return new RateAccrualPeriod.Builder();
  }

  @Override
  public RateAccrualPeriod.Meta metaBean() {
    return RateAccrualPeriod.Meta.INSTANCE;
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
   * Gets the start date of the accrual period.
   * <p>
   * This is the first accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the accrual period.
   * <p>
   * This is the last accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted start date.
   * <p>
   * The start date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the start date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedStartDate() {
    return unadjustedStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted end date.
   * <p>
   * The end date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the end date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedEndDate() {
    return unadjustedEndDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be computed.
   * <p>
   * The value of the period is based on this rate.
   * Different implementations of the {@code RateComputation} interface have different
   * approaches to computing the rate, including averaging, overnight and interpolation.
   * For example, it might be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public RateComputation getRateComputation() {
    return rateComputation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the gearing multiplier, defaulted to 1.
   * <p>
   * This defines the gearing, which is used to multiply the observed rate.
   * <p>
   * When calculating the rate, the observed rate is multiplied by the gearing.
   * If both gearing and spread exist, then the gearing is applied first.
   * A gearing of 1 has no effect.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   * @return the value of the property
   */
  public double getGearing() {
    return gearing;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the spread rate, defaulted to 0.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * This defines the spread, which is used to add an amount the observed rate.
   * <p>
   * When calculating the rate, the spread is added to the observed rate.
   * If both gearing and spread exist, then the gearing is applied first.
   * A spread of 0 has no effect.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   * @return the value of the property
   */
  public double getSpread() {
    return spread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * <p>
   * When observing or calculating the rate, the value may go negative.
   * If it does, then this method is used to validate whether the negative rate is allowed.
   * It is applied after any applicable gearing or spread.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   * @return the value of the property, not null
   */
  public NegativeRateMethod getNegativeRateMethod() {
    return negativeRateMethod;
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
      RateAccrualPeriod other = (RateAccrualPeriod) obj;
      return JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(rateComputation, other.rateComputation) &&
          JodaBeanUtils.equal(gearing, other.gearing) &&
          JodaBeanUtils.equal(spread, other.spread) &&
          JodaBeanUtils.equal(negativeRateMethod, other.negativeRateMethod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateComputation);
    hash = hash * 31 + JodaBeanUtils.hashCode(gearing);
    hash = hash * 31 + JodaBeanUtils.hashCode(spread);
    hash = hash * 31 + JodaBeanUtils.hashCode(negativeRateMethod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("RateAccrualPeriod{");
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(unadjustedStartDate).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(unadjustedEndDate).append(',').append(' ');
    buf.append("yearFraction").append('=').append(yearFraction).append(',').append(' ');
    buf.append("rateComputation").append('=').append(rateComputation).append(',').append(' ');
    buf.append("gearing").append('=').append(gearing).append(',').append(' ');
    buf.append("spread").append('=').append(spread).append(',').append(' ');
    buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RateAccrualPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", RateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", RateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", RateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", RateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", RateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code rateComputation} property.
     */
    private final MetaProperty<RateComputation> rateComputation = DirectMetaProperty.ofImmutable(
        this, "rateComputation", RateAccrualPeriod.class, RateComputation.class);
    /**
     * The meta-property for the {@code gearing} property.
     */
    private final MetaProperty<Double> gearing = DirectMetaProperty.ofImmutable(
        this, "gearing", RateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code spread} property.
     */
    private final MetaProperty<Double> spread = DirectMetaProperty.ofImmutable(
        this, "spread", RateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code negativeRateMethod} property.
     */
    private final MetaProperty<NegativeRateMethod> negativeRateMethod = DirectMetaProperty.ofImmutable(
        this, "negativeRateMethod", RateAccrualPeriod.class, NegativeRateMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate",
        "yearFraction",
        "rateComputation",
        "gearing",
        "spread",
        "negativeRateMethod");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 625350855:  // rateComputation
          return rateComputation;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public RateAccrualPeriod.Builder builder() {
      return new RateAccrualPeriod.Builder();
    }

    @Override
    public Class<? extends RateAccrualPeriod> beanType() {
      return RateAccrualPeriod.class;
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
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedStartDate() {
      return unadjustedStartDate;
    }

    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedEndDate() {
      return unadjustedEndDate;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code rateComputation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateComputation> rateComputation() {
      return rateComputation;
    }

    /**
     * The meta-property for the {@code gearing} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> gearing() {
      return gearing;
    }

    /**
     * The meta-property for the {@code spread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> spread() {
      return spread;
    }

    /**
     * The meta-property for the {@code negativeRateMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NegativeRateMethod> negativeRateMethod() {
      return negativeRateMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((RateAccrualPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((RateAccrualPeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((RateAccrualPeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((RateAccrualPeriod) bean).getUnadjustedEndDate();
        case -1731780257:  // yearFraction
          return ((RateAccrualPeriod) bean).getYearFraction();
        case 625350855:  // rateComputation
          return ((RateAccrualPeriod) bean).getRateComputation();
        case -91774989:  // gearing
          return ((RateAccrualPeriod) bean).getGearing();
        case -895684237:  // spread
          return ((RateAccrualPeriod) bean).getSpread();
        case 1969081334:  // negativeRateMethod
          return ((RateAccrualPeriod) bean).getNegativeRateMethod();
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
   * The bean-builder for {@code RateAccrualPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<RateAccrualPeriod> {

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private double yearFraction;
    private RateComputation rateComputation;
    private double gearing;
    private double spread;
    private NegativeRateMethod negativeRateMethod;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(RateAccrualPeriod beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.yearFraction = beanToCopy.getYearFraction();
      this.rateComputation = beanToCopy.getRateComputation();
      this.gearing = beanToCopy.getGearing();
      this.spread = beanToCopy.getSpread();
      this.negativeRateMethod = beanToCopy.getNegativeRateMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 625350855:  // rateComputation
          return rateComputation;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
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
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case 1457691881:  // unadjustedStartDate
          this.unadjustedStartDate = (LocalDate) newValue;
          break;
        case 31758114:  // unadjustedEndDate
          this.unadjustedEndDate = (LocalDate) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case 625350855:  // rateComputation
          this.rateComputation = (RateComputation) newValue;
          break;
        case -91774989:  // gearing
          this.gearing = (Double) newValue;
          break;
        case -895684237:  // spread
          this.spread = (Double) newValue;
          break;
        case 1969081334:  // negativeRateMethod
          this.negativeRateMethod = (NegativeRateMethod) newValue;
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
    public RateAccrualPeriod build() {
      return new RateAccrualPeriod(
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          yearFraction,
          rateComputation,
          gearing,
          spread,
          negativeRateMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date of the accrual period.
     * <p>
     * This is the first accrual date in the period.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the accrual period.
     * <p>
     * This is the last accrual date in the period.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the unadjusted start date.
     * <p>
     * The start date before any business day adjustment is applied.
     * <p>
     * When building, this will default to the start date if not specified.
     * @param unadjustedStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedStartDate(LocalDate unadjustedStartDate) {
      JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
      this.unadjustedStartDate = unadjustedStartDate;
      return this;
    }

    /**
     * Sets the unadjusted end date.
     * <p>
     * The end date before any business day adjustment is applied.
     * <p>
     * When building, this will default to the end date if not specified.
     * @param unadjustedEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedEndDate(LocalDate unadjustedEndDate) {
      JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
      this.unadjustedEndDate = unadjustedEndDate;
      return this;
    }

    /**
     * Sets the year fraction that the accrual period represents.
     * <p>
     * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     * The fraction may be greater than 1, but not less than 0.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      ArgChecker.notNegative(yearFraction, "yearFraction");
      this.yearFraction = yearFraction;
      return this;
    }

    /**
     * Sets the rate to be computed.
     * <p>
     * The value of the period is based on this rate.
     * Different implementations of the {@code RateComputation} interface have different
     * approaches to computing the rate, including averaging, overnight and interpolation.
     * For example, it might be a well known market index such as 'GBP-LIBOR-3M'.
     * @param rateComputation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateComputation(RateComputation rateComputation) {
      JodaBeanUtils.notNull(rateComputation, "rateComputation");
      this.rateComputation = rateComputation;
      return this;
    }

    /**
     * Sets the gearing multiplier, defaulted to 1.
     * <p>
     * This defines the gearing, which is used to multiply the observed rate.
     * <p>
     * When calculating the rate, the observed rate is multiplied by the gearing.
     * If both gearing and spread exist, then the gearing is applied first.
     * A gearing of 1 has no effect.
     * <p>
     * Gearing is also known as <i>leverage</i>.
     * @param gearing  the new value
     * @return this, for chaining, not null
     */
    public Builder gearing(double gearing) {
      this.gearing = gearing;
      return this;
    }

    /**
     * Sets the spread rate, defaulted to 0.
     * A 5% rate will be expressed as 0.05.
     * <p>
     * This defines the spread, which is used to add an amount the observed rate.
     * <p>
     * When calculating the rate, the spread is added to the observed rate.
     * If both gearing and spread exist, then the gearing is applied first.
     * A spread of 0 has no effect.
     * <p>
     * Defined by the 2006 ISDA definitions article 6.2e.
     * @param spread  the new value
     * @return this, for chaining, not null
     */
    public Builder spread(double spread) {
      this.spread = spread;
      return this;
    }

    /**
     * Sets the negative rate method, defaulted to 'AllowNegative'.
     * <p>
     * This is used when the interest rate, observed or calculated, goes negative.
     * <p>
     * When observing or calculating the rate, the value may go negative.
     * If it does, then this method is used to validate whether the negative rate is allowed.
     * It is applied after any applicable gearing or spread.
     * <p>
     * Defined by the 2006 ISDA definitions article 6.4.
     * @param negativeRateMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder negativeRateMethod(NegativeRateMethod negativeRateMethod) {
      JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
      this.negativeRateMethod = negativeRateMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("RateAccrualPeriod.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("rateComputation").append('=').append(JodaBeanUtils.toString(rateComputation)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing)).append(',').append(' ');
      buf.append("spread").append('=').append(JodaBeanUtils.toString(spread)).append(',').append(' ');
      buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
