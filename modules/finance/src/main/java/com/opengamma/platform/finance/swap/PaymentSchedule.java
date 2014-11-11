/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.Schedule;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.Guavate;

/**
 * Defines the schedule of payment dates relative to the accrual periods.
 * <p>
 * This defines the data necessary to create a schedule of payment periods.
 * Each payment period contains one or more accrual periods.
 * If a payment period contains more than one accrual period then the compounding
 * method will be used to combine the amounts.
 * <p>
 * This class defines payment periods using a periodic frequency.
 * The frequency must match or be a multiple of the accrual periodic frequency.
 * <p>
 * The initial and final payment period may be shorter or longer than the regular payment period.
 * These may be specified in terms of the number of accrual periods in each period.
 * <p>
 * When applying the frequency, it is converted into an integer value, representing the
 * number of accrual periods per payment period. If one or both of the properties
 * {@code initialPaymentAccrualPeriods} and {@code finalPaymentAccrualPeriods} are non-null
 * then the remaining number of periods must be exactly divisible by the frequency.
 * If both are null then the accrual periods are allocated by rolling forwards or
 * backwards, applying the same direction as accrual schedule generation.
 */
@BeanDefinition
public final class PaymentSchedule
    implements ImmutableBean, Serializable {
  // may need to add date-based initial/final stubs in future, thus use Integer instead of int now

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The periodic frequency of payments.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
   * <p>
   * Compounding applies if the payment frequency does not equal the accrual frequency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency paymentFrequency;
  /**
   * The number of accrual periods forming the initial payment period, optional.
   * <p>
   * If null or zero then the regular payment period applies from the start of the swap.
   * This must not be negative.
   */
  @PropertyDefinition
  private final Integer initialPaymentAccrualPeriods;
  /**
   * The number of accrual periods forming the final payment period, optional.
   * <p>
   * If null or zero then the regular payment period applies up to the end of the swap.
   * This must not be negative.
   */
  @PropertyDefinition
  private final Integer finalPaymentAccrualPeriods;
  /**
   * The base date that each payment is made relative to, defaulted to 'PeriodEnd'.
   * <p>
   * The payment date is relative to either the start or end of the payment period.
   */
  @PropertyDefinition(validate = "notNull")
  private final PaymentRelativeTo paymentRelativeTo;
  /**
   * The offset of payment from the base calculation period date.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment paymentOffset;
  /**
   * The compounding method to use when there is more than one accrual period, defaulted to 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   */
  @PropertyDefinition(validate = "notNull")
  private final CompoundingMethod compoundingMethod;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.paymentRelativeTo(PaymentRelativeTo.PERIOD_END);
    builder.compoundingMethod(CompoundingMethod.NONE);
  }

  @ImmutableValidator
  private void validate() {
    if (initialPaymentAccrualPeriods != null) {
      ArgChecker.notNegative(initialPaymentAccrualPeriods.intValue(), "initialPaymentAccrualPeriods");
    }
    if (finalPaymentAccrualPeriods != null) {
      ArgChecker.notNegative(finalPaymentAccrualPeriods.intValue(), "finalPaymentAccrualPeriods");
    }
  }

  /**
   * Builds the list of payment periods from the list of accrual periods.
   * <p>
   * This applies the payment schedule.
   * 
   * @param accrualPeriods  the list of accrual periods
   * @param schedule  the accrual schedule
   * @return the list of payment periods
   */
  ImmutableList<PaymentPeriod> createPaymentPeriods(
      List<RateAccrualPeriod> accrualPeriods,
      Schedule schedule,
      RateCalculation rateCalculation) {
    // payment periods contain one accrual period
    Frequency accrualFrequency = schedule.getFrequency();
    if (accrualFrequency.equals(paymentFrequency)) {
      return accrualPeriods.stream()
          .map(accrualPeriod -> createPaymentPeriod(ImmutableList.of(accrualPeriod), rateCalculation))
          .collect(Guavate.toImmutableList());
    }
    // only one payment
    if (paymentFrequency.equals(Frequency.TERM)) {
      return ImmutableList.of(createPaymentPeriod(accrualPeriods, rateCalculation));
    }
    // payment periods contain more than one accrual period
    return groupAccrualPeriods(accrualPeriods, schedule, accrualFrequency, rateCalculation);
  }

  // group accrual periods based on payment schedule
  private ImmutableList<PaymentPeriod> groupAccrualPeriods(
      List<RateAccrualPeriod> accrualPeriods, Schedule schedule, Frequency accrualFrequency, RateCalculation rateCalculation) {
    
    int freqMultiple = accrualPeriodsPerPayment(paymentFrequency, accrualFrequency);
    int accrualCount = accrualPeriods.size();
    if (initialPaymentAccrualPeriods != null || finalPaymentAccrualPeriods != null) {
      int initialGroup = (initialPaymentAccrualPeriods != null ? initialPaymentAccrualPeriods.intValue() : 0);
      int finalGroup = (finalPaymentAccrualPeriods != null ? finalPaymentAccrualPeriods.intValue() : 0);
      int multipleSize = accrualPeriods.size() - initialGroup - finalGroup;
      if ((multipleSize % freqMultiple) != 0 || multipleSize < 0) {
        throw new IllegalArgumentException(ArgChecker.formatMessage(
            "Payment frequency '{}' must exactly divide remaining accrual periods when specifying " +
            "initialPaymentAccrualPeriods and/or finalPaymentAccrualPeriods", paymentFrequency));
      }
      return groupAccrualPeriods(accrualPeriods, initialGroup, freqMultiple, finalGroup, rateCalculation);
    }
    // accrual periods divide exactly into payment periods
    int multipleRemainder = accrualCount % freqMultiple;
    if (multipleRemainder == 0) {
      return groupAccrualPeriods(accrualPeriods, 0, freqMultiple, 0, rateCalculation);
    }
    // determine by stub direction, default is to roll backwards
    boolean finalStub = schedule.getLastPeriod().isStub();
    if (finalStub) {
      return groupAccrualPeriods(accrualPeriods, 0, freqMultiple, multipleRemainder, rateCalculation);
    } else {
      return groupAccrualPeriods(accrualPeriods, multipleRemainder, freqMultiple, 0, rateCalculation);
    }
  }

  // group accrual periods by initial, multiple and final
  private ImmutableList<PaymentPeriod> groupAccrualPeriods(
      List<RateAccrualPeriod> accrualPeriods, int initialGroup, int multiple, int finalGroup, RateCalculation rateCalculation) {
    
    int accrualCount = accrualPeriods.size();
    int finalIndex = accrualCount - finalGroup;
    ImmutableList.Builder<PaymentPeriod> paymentPeriods = ImmutableList.builder();
    if (initialGroup > 0) {
      paymentPeriods.add(createPaymentPeriod(accrualPeriods.subList(0, initialGroup), rateCalculation));
    }
    for (int i = initialGroup; i < finalIndex; i += multiple) {
      paymentPeriods.add(createPaymentPeriod(accrualPeriods.subList(i, i + multiple), rateCalculation));
    }
    if (finalGroup > 0) {
      paymentPeriods.add(createPaymentPeriod(accrualPeriods.subList(finalIndex, accrualCount), rateCalculation));
    }
    return paymentPeriods.build();
  }

  // create the payment period
  private PaymentPeriod createPaymentPeriod(List<RateAccrualPeriod> periods, RateCalculation rateCalculation) {
//    List<Double> resolvedNotionals = notional.getAmount().resolveValues(schedule.getPeriods());
//    Currency currency = notional.getCurrency();
//    FxResetNotional fxResetNotional = notional.getFxReset();
    
    double notional = rateCalculation.getNotional().getAmount().getInitialValue();  // TODO schedule
    notional = rateCalculation.getPayReceive().normalize(notional);
    return RatePaymentPeriod.builder()
        .paymentDate(createPaymentDate(periods))
        .accrualPeriods(periods)
        .currency(rateCalculation.getNotional().getCurrency())
//        .fxReset(periods.get(0).getFxReset())
        .notional(notional)
        .compoundingMethod(compoundingMethod)
        .build();
  }

//  // determine the FX reset
//  private FxReset createFxReset(SchedulePeriod period, FxResetNotional fxResetNotional, Currency currency) {
//    if (fxResetNotional == null || fxResetNotional.getReferenceCurrency().equals(currency)) {
//      return null;
//    }
//    return fxResetNotional.createFxReset(period);
//  }

  // determine the fixing date
  private LocalDate createPaymentDate(List<RateAccrualPeriod> periods) {
    switch (paymentRelativeTo) {
      case PERIOD_END:
        return paymentOffset.adjust(periods.get(periods.size() - 1).getEndDate());
      case PERIOD_START:
      default:
        return paymentOffset.adjust(periods.get(0).getStartDate());
    }
  }

  // how many accrual periods are there per payment
  private static int accrualPeriodsPerPayment(Frequency paymentFrequency, Frequency accrualFrequency) {
    if (paymentFrequency.isMonthBased() && accrualFrequency.isMonthBased()) {
      long paymentMonths = paymentFrequency.getPeriod().toTotalMonths();
      long accrualMonths = accrualFrequency.getPeriod().toTotalMonths();
      if ((paymentMonths % accrualMonths) == 0) {
        return Math.toIntExact(paymentMonths / accrualMonths);
      }
    } else if (paymentFrequency.isWeekBased() && accrualFrequency.isWeekBased()) {
      long paymentDays = paymentFrequency.getPeriod().getDays();
      long accrualDays = accrualFrequency.getPeriod().getDays();
      if ((paymentDays % accrualDays) == 0) {
        return Math.toIntExact(paymentDays / accrualDays);
      }
    }
    throw new IllegalArgumentException(ArgChecker.formatMessage(
        "Payment frequency '{}' must be a multiple of the accrual frequency '{}'", paymentFrequency, accrualFrequency));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PaymentSchedule}.
   * @return the meta-bean, not null
   */
  public static PaymentSchedule.Meta meta() {
    return PaymentSchedule.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PaymentSchedule.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PaymentSchedule.Builder builder() {
    return new PaymentSchedule.Builder();
  }

  private PaymentSchedule(
      Frequency paymentFrequency,
      Integer initialPaymentAccrualPeriods,
      Integer finalPaymentAccrualPeriods,
      PaymentRelativeTo paymentRelativeTo,
      DaysAdjustment paymentOffset,
      CompoundingMethod compoundingMethod) {
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    JodaBeanUtils.notNull(paymentRelativeTo, "paymentRelativeTo");
    JodaBeanUtils.notNull(paymentOffset, "paymentOffset");
    JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
    this.paymentFrequency = paymentFrequency;
    this.initialPaymentAccrualPeriods = initialPaymentAccrualPeriods;
    this.finalPaymentAccrualPeriods = finalPaymentAccrualPeriods;
    this.paymentRelativeTo = paymentRelativeTo;
    this.paymentOffset = paymentOffset;
    this.compoundingMethod = compoundingMethod;
    validate();
  }

  @Override
  public PaymentSchedule.Meta metaBean() {
    return PaymentSchedule.Meta.INSTANCE;
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
   * Gets the periodic frequency of payments.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
   * <p>
   * Compounding applies if the payment frequency does not equal the accrual frequency.
   * @return the value of the property, not null
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of accrual periods forming the initial payment period, optional.
   * <p>
   * If null or zero then the regular payment period applies from the start of the swap.
   * This must not be negative.
   * @return the value of the property
   */
  public Integer getInitialPaymentAccrualPeriods() {
    return initialPaymentAccrualPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of accrual periods forming the final payment period, optional.
   * <p>
   * If null or zero then the regular payment period applies up to the end of the swap.
   * This must not be negative.
   * @return the value of the property
   */
  public Integer getFinalPaymentAccrualPeriods() {
    return finalPaymentAccrualPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base date that each payment is made relative to, defaulted to 'PeriodEnd'.
   * <p>
   * The payment date is relative to either the start or end of the payment period.
   * @return the value of the property, not null
   */
  public PaymentRelativeTo getPaymentRelativeTo() {
    return paymentRelativeTo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of payment from the base calculation period date.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   * @return the value of the property, not null
   */
  public DaysAdjustment getPaymentOffset() {
    return paymentOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the compounding method to use when there is more than one accrual period, defaulted to 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   * @return the value of the property, not null
   */
  public CompoundingMethod getCompoundingMethod() {
    return compoundingMethod;
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
      PaymentSchedule other = (PaymentSchedule) obj;
      return JodaBeanUtils.equal(getPaymentFrequency(), other.getPaymentFrequency()) &&
          JodaBeanUtils.equal(getInitialPaymentAccrualPeriods(), other.getInitialPaymentAccrualPeriods()) &&
          JodaBeanUtils.equal(getFinalPaymentAccrualPeriods(), other.getFinalPaymentAccrualPeriods()) &&
          JodaBeanUtils.equal(getPaymentRelativeTo(), other.getPaymentRelativeTo()) &&
          JodaBeanUtils.equal(getPaymentOffset(), other.getPaymentOffset()) &&
          JodaBeanUtils.equal(getCompoundingMethod(), other.getCompoundingMethod());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentFrequency());
    hash += hash * 31 + JodaBeanUtils.hashCode(getInitialPaymentAccrualPeriods());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFinalPaymentAccrualPeriods());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentRelativeTo());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentOffset());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCompoundingMethod());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("PaymentSchedule{");
    buf.append("paymentFrequency").append('=').append(getPaymentFrequency()).append(',').append(' ');
    buf.append("initialPaymentAccrualPeriods").append('=').append(getInitialPaymentAccrualPeriods()).append(',').append(' ');
    buf.append("finalPaymentAccrualPeriods").append('=').append(getFinalPaymentAccrualPeriods()).append(',').append(' ');
    buf.append("paymentRelativeTo").append('=').append(getPaymentRelativeTo()).append(',').append(' ');
    buf.append("paymentOffset").append('=').append(getPaymentOffset()).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(getCompoundingMethod()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PaymentSchedule}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", PaymentSchedule.class, Frequency.class);
    /**
     * The meta-property for the {@code initialPaymentAccrualPeriods} property.
     */
    private final MetaProperty<Integer> initialPaymentAccrualPeriods = DirectMetaProperty.ofImmutable(
        this, "initialPaymentAccrualPeriods", PaymentSchedule.class, Integer.class);
    /**
     * The meta-property for the {@code finalPaymentAccrualPeriods} property.
     */
    private final MetaProperty<Integer> finalPaymentAccrualPeriods = DirectMetaProperty.ofImmutable(
        this, "finalPaymentAccrualPeriods", PaymentSchedule.class, Integer.class);
    /**
     * The meta-property for the {@code paymentRelativeTo} property.
     */
    private final MetaProperty<PaymentRelativeTo> paymentRelativeTo = DirectMetaProperty.ofImmutable(
        this, "paymentRelativeTo", PaymentSchedule.class, PaymentRelativeTo.class);
    /**
     * The meta-property for the {@code paymentOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentOffset = DirectMetaProperty.ofImmutable(
        this, "paymentOffset", PaymentSchedule.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code compoundingMethod} property.
     */
    private final MetaProperty<CompoundingMethod> compoundingMethod = DirectMetaProperty.ofImmutable(
        this, "compoundingMethod", PaymentSchedule.class, CompoundingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentFrequency",
        "initialPaymentAccrualPeriods",
        "finalPaymentAccrualPeriods",
        "paymentRelativeTo",
        "paymentOffset",
        "compoundingMethod");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case 437971173:  // initialPaymentAccrualPeriods
          return initialPaymentAccrualPeriods;
        case -336449037:  // finalPaymentAccrualPeriods
          return finalPaymentAccrualPeriods;
        case -1357627123:  // paymentRelativeTo
          return paymentRelativeTo;
        case 1303406137:  // paymentOffset
          return paymentOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PaymentSchedule.Builder builder() {
      return new PaymentSchedule.Builder();
    }

    @Override
    public Class<? extends PaymentSchedule> beanType() {
      return PaymentSchedule.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code paymentFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> paymentFrequency() {
      return paymentFrequency;
    }

    /**
     * The meta-property for the {@code initialPaymentAccrualPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> initialPaymentAccrualPeriods() {
      return initialPaymentAccrualPeriods;
    }

    /**
     * The meta-property for the {@code finalPaymentAccrualPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> finalPaymentAccrualPeriods() {
      return finalPaymentAccrualPeriods;
    }

    /**
     * The meta-property for the {@code paymentRelativeTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentRelativeTo> paymentRelativeTo() {
      return paymentRelativeTo;
    }

    /**
     * The meta-property for the {@code paymentOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> paymentOffset() {
      return paymentOffset;
    }

    /**
     * The meta-property for the {@code compoundingMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CompoundingMethod> compoundingMethod() {
      return compoundingMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return ((PaymentSchedule) bean).getPaymentFrequency();
        case 437971173:  // initialPaymentAccrualPeriods
          return ((PaymentSchedule) bean).getInitialPaymentAccrualPeriods();
        case -336449037:  // finalPaymentAccrualPeriods
          return ((PaymentSchedule) bean).getFinalPaymentAccrualPeriods();
        case -1357627123:  // paymentRelativeTo
          return ((PaymentSchedule) bean).getPaymentRelativeTo();
        case 1303406137:  // paymentOffset
          return ((PaymentSchedule) bean).getPaymentOffset();
        case -1376171496:  // compoundingMethod
          return ((PaymentSchedule) bean).getCompoundingMethod();
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
   * The bean-builder for {@code PaymentSchedule}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PaymentSchedule> {

    private Frequency paymentFrequency;
    private Integer initialPaymentAccrualPeriods;
    private Integer finalPaymentAccrualPeriods;
    private PaymentRelativeTo paymentRelativeTo;
    private DaysAdjustment paymentOffset;
    private CompoundingMethod compoundingMethod;

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
    private Builder(PaymentSchedule beanToCopy) {
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
      this.initialPaymentAccrualPeriods = beanToCopy.getInitialPaymentAccrualPeriods();
      this.finalPaymentAccrualPeriods = beanToCopy.getFinalPaymentAccrualPeriods();
      this.paymentRelativeTo = beanToCopy.getPaymentRelativeTo();
      this.paymentOffset = beanToCopy.getPaymentOffset();
      this.compoundingMethod = beanToCopy.getCompoundingMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case 437971173:  // initialPaymentAccrualPeriods
          return initialPaymentAccrualPeriods;
        case -336449037:  // finalPaymentAccrualPeriods
          return finalPaymentAccrualPeriods;
        case -1357627123:  // paymentRelativeTo
          return paymentRelativeTo;
        case 1303406137:  // paymentOffset
          return paymentOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          this.paymentFrequency = (Frequency) newValue;
          break;
        case 437971173:  // initialPaymentAccrualPeriods
          this.initialPaymentAccrualPeriods = (Integer) newValue;
          break;
        case -336449037:  // finalPaymentAccrualPeriods
          this.finalPaymentAccrualPeriods = (Integer) newValue;
          break;
        case -1357627123:  // paymentRelativeTo
          this.paymentRelativeTo = (PaymentRelativeTo) newValue;
          break;
        case 1303406137:  // paymentOffset
          this.paymentOffset = (DaysAdjustment) newValue;
          break;
        case -1376171496:  // compoundingMethod
          this.compoundingMethod = (CompoundingMethod) newValue;
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
    public PaymentSchedule build() {
      return new PaymentSchedule(
          paymentFrequency,
          initialPaymentAccrualPeriods,
          finalPaymentAccrualPeriods,
          paymentRelativeTo,
          paymentOffset,
          compoundingMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code paymentFrequency} property in the builder.
     * @param paymentFrequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
      this.paymentFrequency = paymentFrequency;
      return this;
    }

    /**
     * Sets the {@code initialPaymentAccrualPeriods} property in the builder.
     * @param initialPaymentAccrualPeriods  the new value
     * @return this, for chaining, not null
     */
    public Builder initialPaymentAccrualPeriods(Integer initialPaymentAccrualPeriods) {
      this.initialPaymentAccrualPeriods = initialPaymentAccrualPeriods;
      return this;
    }

    /**
     * Sets the {@code finalPaymentAccrualPeriods} property in the builder.
     * @param finalPaymentAccrualPeriods  the new value
     * @return this, for chaining, not null
     */
    public Builder finalPaymentAccrualPeriods(Integer finalPaymentAccrualPeriods) {
      this.finalPaymentAccrualPeriods = finalPaymentAccrualPeriods;
      return this;
    }

    /**
     * Sets the {@code paymentRelativeTo} property in the builder.
     * @param paymentRelativeTo  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentRelativeTo(PaymentRelativeTo paymentRelativeTo) {
      JodaBeanUtils.notNull(paymentRelativeTo, "paymentRelativeTo");
      this.paymentRelativeTo = paymentRelativeTo;
      return this;
    }

    /**
     * Sets the {@code paymentOffset} property in the builder.
     * @param paymentOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentOffset(DaysAdjustment paymentOffset) {
      JodaBeanUtils.notNull(paymentOffset, "paymentOffset");
      this.paymentOffset = paymentOffset;
      return this;
    }

    /**
     * Sets the {@code compoundingMethod} property in the builder.
     * @param compoundingMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder compoundingMethod(CompoundingMethod compoundingMethod) {
      JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
      this.compoundingMethod = compoundingMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("PaymentSchedule.Builder{");
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("initialPaymentAccrualPeriods").append('=').append(JodaBeanUtils.toString(initialPaymentAccrualPeriods)).append(',').append(' ');
      buf.append("finalPaymentAccrualPeriods").append('=').append(JodaBeanUtils.toString(finalPaymentAccrualPeriods)).append(',').append(' ');
      buf.append("paymentRelativeTo").append('=').append(JodaBeanUtils.toString(paymentRelativeTo)).append(',').append(' ');
      buf.append("paymentOffset").append('=').append(JodaBeanUtils.toString(paymentOffset)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
