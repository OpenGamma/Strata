/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.index.RateIndex;

/**
 * Defines the accrual of a single period of a floating rate swap leg.
 * <p>
 * This defines the data necessary to calculate the amount of a single swap leg accrual period.
 * The amount is based on the observed value of a floating rate index.
 * <p>
 * See {@link FloatingRateCalculation} for more details.
 */
@BeanDefinition
public final class FloatingRateAccrualPeriod
    implements AccrualPeriod, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The start date of the period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate startDate;
  /**
   * The end date of the period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate endDate;
  /**
   * The primary currency of this period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   */
  @PropertyDefinition(validate = "notNull")
  private final double notional;
  /**
   * The year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition
  private final double yearFraction;
  /**
   * The floating rate index to be used.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market rate such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final RateIndex index;
  /**
   * The floating rate index to be used in linear interpolation.
   * <p>
   * The rate to be paid is based on this index and {@code index} with linearly interpolation applied.
   * A fixing will be obtained for both tenors allowing the rate for the period end date to be interpolated.
   * <p>
   * This rate will be a well known market rate such as 'GBP-LIBOR-6M'.
   * It should be in the same currency as {@code index} and for a different tenor.
   * One of the two tenors should be shorter than period, the other should be longer.
   * <p>
   * Defined by the 2006 ISDA definitions article 8.3 as later clarified.
   */
  @PropertyDefinition
  private final RateIndex indexInterpolated;
//  /**
//   * The list of reset dates applicable to this accrual period.
//   * <p>
//   * A fixing will be taken at each fixing date.
//   * There is at least one fixing date in the accrual period.
//   * If there is more than one then averaging will occur.
//   */
//  @PropertyDefinition(validate = "notEmpty")
//  private final ImmutableList<SchedulePeriod> resetPeriods;
//  /**
//   * The list of fixing dates applicable to this accrual period.
//   * <p>
//   * A fixing will be taken at each fixing date.
//   * There is at least one fixing date in the accrual period.
//   * If there is more than one then averaging will occur.
//   */
//  @PropertyDefinition(validate = "notEmpty")
//  private final ImmutableList<LocalDate> fixingDates;
  /**
   * The fixing date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   */
  @PropertyDefinition(validate = "notNull")
  private final NegativeRateMethod negativeRateMethod;
  /**
   * The gearing multiplier.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * The gearing is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the fixing rate is multiplied by the gearing.
   * A gearing of 1 has no effect.
   */
  @PropertyDefinition
  private final double gearing;
  /**
   * The spread rate, with a 5% rate expressed as 0.05.
   * <p>
   * This defines the spread as an initial value and a list of adjustments.
   * The spread is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the spread is added to the fixing rate.
   * A spread of 0 has no effect.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   */
  @PropertyDefinition
  private final double spread;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.negativeRateMethod(NegativeRateMethod.ALLOW_NEGATIVE);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FloatingRateAccrualPeriod}.
   * @return the meta-bean, not null
   */
  public static FloatingRateAccrualPeriod.Meta meta() {
    return FloatingRateAccrualPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FloatingRateAccrualPeriod.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FloatingRateAccrualPeriod.Builder builder() {
    return new FloatingRateAccrualPeriod.Builder();
  }

  private FloatingRateAccrualPeriod(
      LocalDate startDate,
      LocalDate endDate,
      Currency currency,
      double notional,
      double yearFraction,
      RateIndex index,
      RateIndex indexInterpolated,
      LocalDate fixingDate,
      NegativeRateMethod negativeRateMethod,
      double gearing,
      double spread) {
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(notional, "notional");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
    this.startDate = startDate;
    this.endDate = endDate;
    this.currency = currency;
    this.notional = notional;
    this.yearFraction = yearFraction;
    this.index = index;
    this.indexInterpolated = indexInterpolated;
    this.fixingDate = fixingDate;
    this.negativeRateMethod = negativeRateMethod;
    this.gearing = gearing;
    this.spread = spread;
  }

  @Override
  public FloatingRateAccrualPeriod.Meta metaBean() {
    return FloatingRateAccrualPeriod.Meta.INSTANCE;
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
   * Gets the start date of the period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency of this period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * @return the value of the property, not null
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the floating rate index to be used.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market rate such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public RateIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the floating rate index to be used in linear interpolation.
   * <p>
   * The rate to be paid is based on this index and {@code index} with linearly interpolation applied.
   * A fixing will be obtained for both tenors allowing the rate for the period end date to be interpolated.
   * <p>
   * This rate will be a well known market rate such as 'GBP-LIBOR-6M'.
   * It should be in the same currency as {@code index} and for a different tenor.
   * One of the two tenors should be shorter than period, the other should be longer.
   * <p>
   * Defined by the 2006 ISDA definitions article 8.3 as later clarified.
   * @return the value of the property
   */
  public RateIndex getIndexInterpolated() {
    return indexInterpolated;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing date.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   * @return the value of the property, not null
   */
  public NegativeRateMethod getNegativeRateMethod() {
    return negativeRateMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the gearing multiplier.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * The gearing is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the fixing rate is multiplied by the gearing.
   * A gearing of 1 has no effect.
   * @return the value of the property
   */
  public double getGearing() {
    return gearing;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the spread rate, with a 5% rate expressed as 0.05.
   * <p>
   * This defines the spread as an initial value and a list of adjustments.
   * The spread is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the spread is added to the fixing rate.
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
      FloatingRateAccrualPeriod other = (FloatingRateAccrualPeriod) obj;
      return JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getYearFraction(), other.getYearFraction()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getIndexInterpolated(), other.getIndexInterpolated()) &&
          JodaBeanUtils.equal(getFixingDate(), other.getFixingDate()) &&
          JodaBeanUtils.equal(getNegativeRateMethod(), other.getNegativeRateMethod()) &&
          JodaBeanUtils.equal(getGearing(), other.getGearing()) &&
          JodaBeanUtils.equal(getSpread(), other.getSpread());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash += hash * 31 + JodaBeanUtils.hashCode(getYearFraction());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndexInterpolated());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNegativeRateMethod());
    hash += hash * 31 + JodaBeanUtils.hashCode(getGearing());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSpread());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(384);
    buf.append("FloatingRateAccrualPeriod{");
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("yearFraction").append('=').append(getYearFraction()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("indexInterpolated").append('=').append(getIndexInterpolated()).append(',').append(' ');
    buf.append("fixingDate").append('=').append(getFixingDate()).append(',').append(' ');
    buf.append("negativeRateMethod").append('=').append(getNegativeRateMethod()).append(',').append(' ');
    buf.append("gearing").append('=').append(getGearing()).append(',').append(' ');
    buf.append("spread").append('=').append(JodaBeanUtils.toString(getSpread()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FloatingRateAccrualPeriod}.
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
        this, "startDate", FloatingRateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", FloatingRateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FloatingRateAccrualPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FloatingRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", FloatingRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<RateIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FloatingRateAccrualPeriod.class, RateIndex.class);
    /**
     * The meta-property for the {@code indexInterpolated} property.
     */
    private final MetaProperty<RateIndex> indexInterpolated = DirectMetaProperty.ofImmutable(
        this, "indexInterpolated", FloatingRateAccrualPeriod.class, RateIndex.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", FloatingRateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code negativeRateMethod} property.
     */
    private final MetaProperty<NegativeRateMethod> negativeRateMethod = DirectMetaProperty.ofImmutable(
        this, "negativeRateMethod", FloatingRateAccrualPeriod.class, NegativeRateMethod.class);
    /**
     * The meta-property for the {@code gearing} property.
     */
    private final MetaProperty<Double> gearing = DirectMetaProperty.ofImmutable(
        this, "gearing", FloatingRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code spread} property.
     */
    private final MetaProperty<Double> spread = DirectMetaProperty.ofImmutable(
        this, "spread", FloatingRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "currency",
        "notional",
        "yearFraction",
        "index",
        "indexInterpolated",
        "fixingDate",
        "negativeRateMethod",
        "gearing",
        "spread");

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
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 100346066:  // index
          return index;
        case -1934091915:  // indexInterpolated
          return indexInterpolated;
        case 1255202043:  // fixingDate
          return fixingDate;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FloatingRateAccrualPeriod.Builder builder() {
      return new FloatingRateAccrualPeriod.Builder();
    }

    @Override
    public Class<? extends FloatingRateAccrualPeriod> beanType() {
      return FloatingRateAccrualPeriod.class;
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
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code indexInterpolated} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateIndex> indexInterpolated() {
      return indexInterpolated;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
    }

    /**
     * The meta-property for the {@code negativeRateMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NegativeRateMethod> negativeRateMethod() {
      return negativeRateMethod;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((FloatingRateAccrualPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((FloatingRateAccrualPeriod) bean).getEndDate();
        case 575402001:  // currency
          return ((FloatingRateAccrualPeriod) bean).getCurrency();
        case 1585636160:  // notional
          return ((FloatingRateAccrualPeriod) bean).getNotional();
        case -1731780257:  // yearFraction
          return ((FloatingRateAccrualPeriod) bean).getYearFraction();
        case 100346066:  // index
          return ((FloatingRateAccrualPeriod) bean).getIndex();
        case -1934091915:  // indexInterpolated
          return ((FloatingRateAccrualPeriod) bean).getIndexInterpolated();
        case 1255202043:  // fixingDate
          return ((FloatingRateAccrualPeriod) bean).getFixingDate();
        case 1969081334:  // negativeRateMethod
          return ((FloatingRateAccrualPeriod) bean).getNegativeRateMethod();
        case -91774989:  // gearing
          return ((FloatingRateAccrualPeriod) bean).getGearing();
        case -895684237:  // spread
          return ((FloatingRateAccrualPeriod) bean).getSpread();
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
   * The bean-builder for {@code FloatingRateAccrualPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FloatingRateAccrualPeriod> {

    private LocalDate startDate;
    private LocalDate endDate;
    private Currency currency;
    private double notional;
    private double yearFraction;
    private RateIndex index;
    private RateIndex indexInterpolated;
    private LocalDate fixingDate;
    private NegativeRateMethod negativeRateMethod;
    private double gearing;
    private double spread;

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
    private Builder(FloatingRateAccrualPeriod beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.yearFraction = beanToCopy.getYearFraction();
      this.index = beanToCopy.getIndex();
      this.indexInterpolated = beanToCopy.getIndexInterpolated();
      this.fixingDate = beanToCopy.getFixingDate();
      this.negativeRateMethod = beanToCopy.getNegativeRateMethod();
      this.gearing = beanToCopy.getGearing();
      this.spread = beanToCopy.getSpread();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 100346066:  // index
          return index;
        case -1934091915:  // indexInterpolated
          return indexInterpolated;
        case 1255202043:  // fixingDate
          return fixingDate;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
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
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case 100346066:  // index
          this.index = (RateIndex) newValue;
          break;
        case -1934091915:  // indexInterpolated
          this.indexInterpolated = (RateIndex) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case 1969081334:  // negativeRateMethod
          this.negativeRateMethod = (NegativeRateMethod) newValue;
          break;
        case -91774989:  // gearing
          this.gearing = (Double) newValue;
          break;
        case -895684237:  // spread
          this.spread = (Double) newValue;
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
    public FloatingRateAccrualPeriod build() {
      return new FloatingRateAccrualPeriod(
          startDate,
          endDate,
          currency,
          notional,
          yearFraction,
          index,
          indexInterpolated,
          fixingDate,
          negativeRateMethod,
          gearing,
          spread);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code startDate} property in the builder.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the {@code endDate} property in the builder.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the {@code currency} property in the builder.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the {@code notional} property in the builder.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code yearFraction} property in the builder.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      this.yearFraction = yearFraction;
      return this;
    }

    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(RateIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code indexInterpolated} property in the builder.
     * @param indexInterpolated  the new value
     * @return this, for chaining, not null
     */
    public Builder indexInterpolated(RateIndex indexInterpolated) {
      this.indexInterpolated = indexInterpolated;
      return this;
    }

    /**
     * Sets the {@code fixingDate} property in the builder.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDate(LocalDate fixingDate) {
      JodaBeanUtils.notNull(fixingDate, "fixingDate");
      this.fixingDate = fixingDate;
      return this;
    }

    /**
     * Sets the {@code negativeRateMethod} property in the builder.
     * @param negativeRateMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder negativeRateMethod(NegativeRateMethod negativeRateMethod) {
      JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
      this.negativeRateMethod = negativeRateMethod;
      return this;
    }

    /**
     * Sets the {@code gearing} property in the builder.
     * @param gearing  the new value
     * @return this, for chaining, not null
     */
    public Builder gearing(double gearing) {
      this.gearing = gearing;
      return this;
    }

    /**
     * Sets the {@code spread} property in the builder.
     * @param spread  the new value
     * @return this, for chaining, not null
     */
    public Builder spread(double spread) {
      this.spread = spread;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(384);
      buf.append("FloatingRateAccrualPeriod.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("indexInterpolated").append('=').append(JodaBeanUtils.toString(indexInterpolated)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing)).append(',').append(' ');
      buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
