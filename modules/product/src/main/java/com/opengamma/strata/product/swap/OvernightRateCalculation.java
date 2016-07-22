/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_0;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Defines the calculation of a floating rate swap leg based on an Overnight index.
 * <p>
 * This defines the data necessary to calculate the amount payable on the leg.
 * The amount is based on the observed value of an Overnight index such as 'GBP-SONIA' or 'USD-FED-FUND'.
 * <p>
 * The index is observed for each business day and averaged or compounded to produce a rate.
 * The reset periods correspond to each business day and are inferred from the accrual period dates.
 */
@BeanDefinition
public final class OvernightRateCalculation
    implements RateCalculation, ImmutableBean, Serializable {

  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;
  /**
   * The Overnight index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightIndex index;
  /**
   * The method of accruing overnight interest, defaulted to 'Compounded'.
   * <p>
   * Two methods of accrual are supported - compounding and averaging.
   * Averaging is primarily related to the 'USD-FED-FUND' index.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightAccrualMethod accrualMethod;
  /**
   * The negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * It does not apply if the rate is fixed, such as in a stub or using {@code firstRegularRate}.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   */
  @PropertyDefinition(validate = "notNull")
  private final NegativeRateMethod negativeRateMethod;
  /**
   * The number of business days before the end of the period that the rate is cut off, defaulted to zero.
   * <p>
   * When a rate cut-off applies, the final daily rate is determined this number of days
   * before the end of the period, with any subsequent days having the same rate.
   * <p>
   * The amount must be zero or positive.
   * A value of zero or one will have no effect on the standard calculation.
   * The fixing holiday calendar of the index is used to determine business days.
   * <p>
   * For example, a value of {@code 3} means that the rate observed on
   * {@code (periodEndDate - 3 business days)} is also to be used on
   * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
   * <p>
   * If there are multiple accrual periods in the payment period, then this
   * will only apply to the last accrual period in the payment period.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int rateCutOffDays;

  /**
   * The gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * The gearing is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the fixing rate is multiplied by the gearing.
   * A gearing of 1 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule gearing;
  /**
   * The spread rate, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * This defines the spread as an initial value and a list of adjustments.
   * The spread is only permitted to change at accrual period boundaries.
   * Spread is a per annum rate.
   * <p>
   * When calculating the rate, the spread is added to the fixing rate.
   * A spread of 0 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is not present, then no spread applies.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule spread;

  //-------------------------------------------------------------------------
  /**
   * Obtains a rate calculation for the specified index with accrual by compounding.
   * <p>
   * The calculation will use the day count of the index.
   * All optional fields will be set to their default values.
   * Thus, there will be no spread, gearing or rate cut-off.
   * If this method provides insufficient control, use the {@linkplain #builder() builder}.
   * 
   * @param index  the index
   * @return the calculation
   */
  public static OvernightRateCalculation of(OvernightIndex index) {
    return OvernightRateCalculation.builder().index(index).build();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.accrualMethod(OvernightAccrualMethod.COMPOUNDED);
    builder.negativeRateMethod(NegativeRateMethod.ALLOW_NEGATIVE);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.dayCount == null) {
        builder.dayCount = builder.index.getDayCount();
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public SwapLegType getType() {
    return SwapLegType.OVERNIGHT;
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(index);
  }

  @Override
  public ImmutableList<RateAccrualPeriod> createAccrualPeriods(
      Schedule accrualSchedule,
      Schedule paymentSchedule,
      ReferenceData refData) {

    // resolve data by schedule
    DoubleArray resolvedGearings = firstNonNull(gearing, ALWAYS_1).resolveValues(accrualSchedule);
    DoubleArray resolvedSpreads = firstNonNull(spread, ALWAYS_0).resolveValues(accrualSchedule);
    // build accrual periods
    ImmutableList.Builder<RateAccrualPeriod> accrualPeriods = ImmutableList.builder();
    for (int i = 0; i < accrualSchedule.size(); i++) {
      SchedulePeriod period = accrualSchedule.getPeriod(i);
      accrualPeriods.add(RateAccrualPeriod.builder(period)
          .yearFraction(period.yearFraction(dayCount, accrualSchedule))
          .rateComputation(createRateComputation(period, paymentSchedule, refData))
          .negativeRateMethod(negativeRateMethod)
          .gearing(resolvedGearings.get(i))
          .spread(resolvedSpreads.get(i))
          .build());
    }
    return accrualPeriods.build();
  }

  // creates the rate computation
  private RateComputation createRateComputation(SchedulePeriod period, Schedule paymentSchedule, ReferenceData refData) {
    int effectiveRateCutOffDaysOffset = (isLastAccrualInPaymentPeriod(period, paymentSchedule) ? rateCutOffDays : 0);
    if (accrualMethod == OvernightAccrualMethod.AVERAGED) {
      return OvernightAveragedRateComputation.of(
          index, period.getStartDate(), period.getEndDate(), effectiveRateCutOffDaysOffset, refData);
    } else {
      return OvernightCompoundedRateComputation.of(
          index, period.getStartDate(), period.getEndDate(), effectiveRateCutOffDaysOffset, refData);
    }
  }

  // rate cut-off only applies to the last accrual period in a payment period
  private boolean isLastAccrualInPaymentPeriod(SchedulePeriod period, Schedule paymentSchedule) {
    if (rateCutOffDays == 0) {
      return true;
    }
    return paymentSchedule.getPeriods().stream()
        .anyMatch(pp -> pp.getUnadjustedEndDate().equals(period.getUnadjustedEndDate()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code OvernightRateCalculation}.
   * @return the meta-bean, not null
   */
  public static OvernightRateCalculation.Meta meta() {
    return OvernightRateCalculation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(OvernightRateCalculation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightRateCalculation.Builder builder() {
    return new OvernightRateCalculation.Builder();
  }

  private OvernightRateCalculation(
      DayCount dayCount,
      OvernightIndex index,
      OvernightAccrualMethod accrualMethod,
      NegativeRateMethod negativeRateMethod,
      int rateCutOffDays,
      ValueSchedule gearing,
      ValueSchedule spread) {
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
    JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
    ArgChecker.notNegative(rateCutOffDays, "rateCutOffDays");
    this.dayCount = dayCount;
    this.index = index;
    this.accrualMethod = accrualMethod;
    this.negativeRateMethod = negativeRateMethod;
    this.rateCutOffDays = rateCutOffDays;
    this.gearing = gearing;
    this.spread = spread;
  }

  @Override
  public OvernightRateCalculation.Meta metaBean() {
    return OvernightRateCalculation.Meta.INSTANCE;
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
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the index if not specified.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Overnight index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   * @return the value of the property, not null
   */
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method of accruing overnight interest, defaulted to 'Compounded'.
   * <p>
   * Two methods of accrual are supported - compounding and averaging.
   * Averaging is primarily related to the 'USD-FED-FUND' index.
   * @return the value of the property, not null
   */
  public OvernightAccrualMethod getAccrualMethod() {
    return accrualMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * It does not apply if the rate is fixed, such as in a stub or using {@code firstRegularRate}.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   * @return the value of the property, not null
   */
  public NegativeRateMethod getNegativeRateMethod() {
    return negativeRateMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of business days before the end of the period that the rate is cut off, defaulted to zero.
   * <p>
   * When a rate cut-off applies, the final daily rate is determined this number of days
   * before the end of the period, with any subsequent days having the same rate.
   * <p>
   * The amount must be zero or positive.
   * A value of zero or one will have no effect on the standard calculation.
   * The fixing holiday calendar of the index is used to determine business days.
   * <p>
   * For example, a value of {@code 3} means that the rate observed on
   * {@code (periodEndDate - 3 business days)} is also to be used on
   * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
   * <p>
   * If there are multiple accrual periods in the payment period, then this
   * will only apply to the last accrual period in the payment period.
   * @return the value of the property
   */
  public int getRateCutOffDays() {
    return rateCutOffDays;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * The gearing is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the fixing rate is multiplied by the gearing.
   * A gearing of 1 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getGearing() {
    return Optional.ofNullable(gearing);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the spread rate, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * This defines the spread as an initial value and a list of adjustments.
   * The spread is only permitted to change at accrual period boundaries.
   * Spread is a per annum rate.
   * <p>
   * When calculating the rate, the spread is added to the fixing rate.
   * A spread of 0 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is not present, then no spread applies.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getSpread() {
    return Optional.ofNullable(spread);
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
      OvernightRateCalculation other = (OvernightRateCalculation) obj;
      return JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(accrualMethod, other.accrualMethod) &&
          JodaBeanUtils.equal(negativeRateMethod, other.negativeRateMethod) &&
          (rateCutOffDays == other.rateCutOffDays) &&
          JodaBeanUtils.equal(gearing, other.gearing) &&
          JodaBeanUtils.equal(spread, other.spread);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(negativeRateMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateCutOffDays);
    hash = hash * 31 + JodaBeanUtils.hashCode(gearing);
    hash = hash * 31 + JodaBeanUtils.hashCode(spread);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("OvernightRateCalculation{");
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("accrualMethod").append('=').append(accrualMethod).append(',').append(' ');
    buf.append("negativeRateMethod").append('=').append(negativeRateMethod).append(',').append(' ');
    buf.append("rateCutOffDays").append('=').append(rateCutOffDays).append(',').append(' ');
    buf.append("gearing").append('=').append(gearing).append(',').append(' ');
    buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightRateCalculation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", OvernightRateCalculation.class, DayCount.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<OvernightIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", OvernightRateCalculation.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code accrualMethod} property.
     */
    private final MetaProperty<OvernightAccrualMethod> accrualMethod = DirectMetaProperty.ofImmutable(
        this, "accrualMethod", OvernightRateCalculation.class, OvernightAccrualMethod.class);
    /**
     * The meta-property for the {@code negativeRateMethod} property.
     */
    private final MetaProperty<NegativeRateMethod> negativeRateMethod = DirectMetaProperty.ofImmutable(
        this, "negativeRateMethod", OvernightRateCalculation.class, NegativeRateMethod.class);
    /**
     * The meta-property for the {@code rateCutOffDays} property.
     */
    private final MetaProperty<Integer> rateCutOffDays = DirectMetaProperty.ofImmutable(
        this, "rateCutOffDays", OvernightRateCalculation.class, Integer.TYPE);
    /**
     * The meta-property for the {@code gearing} property.
     */
    private final MetaProperty<ValueSchedule> gearing = DirectMetaProperty.ofImmutable(
        this, "gearing", OvernightRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code spread} property.
     */
    private final MetaProperty<ValueSchedule> spread = DirectMetaProperty.ofImmutable(
        this, "spread", OvernightRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "dayCount",
        "index",
        "accrualMethod",
        "negativeRateMethod",
        "rateCutOffDays",
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
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case -92095804:  // rateCutOffDays
          return rateCutOffDays;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightRateCalculation.Builder builder() {
      return new OvernightRateCalculation.Builder();
    }

    @Override
    public Class<? extends OvernightRateCalculation> beanType() {
      return OvernightRateCalculation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code accrualMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightAccrualMethod> accrualMethod() {
      return accrualMethod;
    }

    /**
     * The meta-property for the {@code negativeRateMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NegativeRateMethod> negativeRateMethod() {
      return negativeRateMethod;
    }

    /**
     * The meta-property for the {@code rateCutOffDays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> rateCutOffDays() {
      return rateCutOffDays;
    }

    /**
     * The meta-property for the {@code gearing} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> gearing() {
      return gearing;
    }

    /**
     * The meta-property for the {@code spread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> spread() {
      return spread;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1905311443:  // dayCount
          return ((OvernightRateCalculation) bean).getDayCount();
        case 100346066:  // index
          return ((OvernightRateCalculation) bean).getIndex();
        case -1335729296:  // accrualMethod
          return ((OvernightRateCalculation) bean).getAccrualMethod();
        case 1969081334:  // negativeRateMethod
          return ((OvernightRateCalculation) bean).getNegativeRateMethod();
        case -92095804:  // rateCutOffDays
          return ((OvernightRateCalculation) bean).getRateCutOffDays();
        case -91774989:  // gearing
          return ((OvernightRateCalculation) bean).gearing;
        case -895684237:  // spread
          return ((OvernightRateCalculation) bean).spread;
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
   * The bean-builder for {@code OvernightRateCalculation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightRateCalculation> {

    private DayCount dayCount;
    private OvernightIndex index;
    private OvernightAccrualMethod accrualMethod;
    private NegativeRateMethod negativeRateMethod;
    private int rateCutOffDays;
    private ValueSchedule gearing;
    private ValueSchedule spread;

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
    private Builder(OvernightRateCalculation beanToCopy) {
      this.dayCount = beanToCopy.getDayCount();
      this.index = beanToCopy.getIndex();
      this.accrualMethod = beanToCopy.getAccrualMethod();
      this.negativeRateMethod = beanToCopy.getNegativeRateMethod();
      this.rateCutOffDays = beanToCopy.getRateCutOffDays();
      this.gearing = beanToCopy.gearing;
      this.spread = beanToCopy.spread;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case -92095804:  // rateCutOffDays
          return rateCutOffDays;
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
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 100346066:  // index
          this.index = (OvernightIndex) newValue;
          break;
        case -1335729296:  // accrualMethod
          this.accrualMethod = (OvernightAccrualMethod) newValue;
          break;
        case 1969081334:  // negativeRateMethod
          this.negativeRateMethod = (NegativeRateMethod) newValue;
          break;
        case -92095804:  // rateCutOffDays
          this.rateCutOffDays = (Integer) newValue;
          break;
        case -91774989:  // gearing
          this.gearing = (ValueSchedule) newValue;
          break;
        case -895684237:  // spread
          this.spread = (ValueSchedule) newValue;
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
    public OvernightRateCalculation build() {
      preBuild(this);
      return new OvernightRateCalculation(
          dayCount,
          index,
          accrualMethod,
          negativeRateMethod,
          rateCutOffDays,
          gearing,
          spread);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * <p>
     * When building, this will default to the day count of the index if not specified.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the Overnight index.
     * <p>
     * The rate to be paid is based on this index
     * It will be a well known market index such as 'GBP-SONIA'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(OvernightIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the method of accruing overnight interest, defaulted to 'Compounded'.
     * <p>
     * Two methods of accrual are supported - compounding and averaging.
     * Averaging is primarily related to the 'USD-FED-FUND' index.
     * @param accrualMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualMethod(OvernightAccrualMethod accrualMethod) {
      JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
      this.accrualMethod = accrualMethod;
      return this;
    }

    /**
     * Sets the negative rate method, defaulted to 'AllowNegative'.
     * <p>
     * This is used when the interest rate, observed or calculated, goes negative.
     * It does not apply if the rate is fixed, such as in a stub or using {@code firstRegularRate}.
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

    /**
     * Sets the number of business days before the end of the period that the rate is cut off, defaulted to zero.
     * <p>
     * When a rate cut-off applies, the final daily rate is determined this number of days
     * before the end of the period, with any subsequent days having the same rate.
     * <p>
     * The amount must be zero or positive.
     * A value of zero or one will have no effect on the standard calculation.
     * The fixing holiday calendar of the index is used to determine business days.
     * <p>
     * For example, a value of {@code 3} means that the rate observed on
     * {@code (periodEndDate - 3 business days)} is also to be used on
     * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
     * <p>
     * If there are multiple accrual periods in the payment period, then this
     * will only apply to the last accrual period in the payment period.
     * @param rateCutOffDays  the new value
     * @return this, for chaining, not null
     */
    public Builder rateCutOffDays(int rateCutOffDays) {
      ArgChecker.notNegative(rateCutOffDays, "rateCutOffDays");
      this.rateCutOffDays = rateCutOffDays;
      return this;
    }

    /**
     * Sets the gearing multiplier, optional.
     * <p>
     * This defines the gearing as an initial value and a list of adjustments.
     * The gearing is only permitted to change at accrual period boundaries.
     * <p>
     * When calculating the rate, the fixing rate is multiplied by the gearing.
     * A gearing of 1 has no effect.
     * If both gearing and spread exist, then the gearing is applied first.
     * <p>
     * If this property is not present, then no gearing applies.
     * <p>
     * Gearing is also known as <i>leverage</i>.
     * @param gearing  the new value
     * @return this, for chaining, not null
     */
    public Builder gearing(ValueSchedule gearing) {
      this.gearing = gearing;
      return this;
    }

    /**
     * Sets the spread rate, optional.
     * A 5% rate will be expressed as 0.05.
     * <p>
     * This defines the spread as an initial value and a list of adjustments.
     * The spread is only permitted to change at accrual period boundaries.
     * Spread is a per annum rate.
     * <p>
     * When calculating the rate, the spread is added to the fixing rate.
     * A spread of 0 has no effect.
     * If both gearing and spread exist, then the gearing is applied first.
     * <p>
     * If this property is not present, then no spread applies.
     * <p>
     * Defined by the 2006 ISDA definitions article 6.2e.
     * @param spread  the new value
     * @return this, for chaining, not null
     */
    public Builder spread(ValueSchedule spread) {
      this.spread = spread;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("OvernightRateCalculation.Builder{");
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("accrualMethod").append('=').append(JodaBeanUtils.toString(accrualMethod)).append(',').append(' ');
      buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod)).append(',').append(' ');
      buf.append("rateCutOffDays").append('=').append(JodaBeanUtils.toString(rateCutOffDays)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing)).append(',').append(' ');
      buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
