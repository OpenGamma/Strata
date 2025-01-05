/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.RateAccrualPeriod;

/**
 * An overnight rate in arrears cap/floor leg of a cap/floor product.
 * <p>
 * This defines a single cap/floor leg for an overnight rate in arrears cap/floor product.
 * The cap/floor instruments are defined as a set of call/put options on successive compounded overnight index rates.
 * <p>
 * The periodic payments in the resolved leg are caplets or floorlets depending on the data in this leg.
 * The {@code capSchedule} field is used to represent strike values of individual caplets,
 * whereas {@code floorSchedule} is used to represent strike values of individual floorlets.
 * Either {@code capSchedule} or {@code floorSchedule} must be present, and not both.
 */
@BeanDefinition
public final class OvernightInArrearsCapFloorLeg
    implements Resolvable<ResolvedOvernightInArrearsCapFloorLeg>, ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   */
  @PropertyDefinition(validate = "notNull")
  private final PayReceive payReceive;
  /**
   * The periodic payment schedule.
   * <p>
   * This is used to define the periodic payment periods.
   * These are used directly or indirectly to determine other dates in the leg.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule paymentSchedule;
  /**
   * The offset of payment from the base calculation period date, defaulted to 'None'.
   * <p>
   * The offset is applied to the adjusted end date of each payment period.
   * Offset can be based on calendar days or business days.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment paymentDateOffset;
  /**
   * The currency of the leg associated with the notional.
   * <p>
   * This is the currency of the leg and the currency that payoff calculation is made in.
   * The amounts of the notional are expressed in terms of this currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount, must be non-negative.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueSchedule notional;
  /**
   * The interest rate accrual calculation.
   * <p>
   * The interest rate accrual is based on overnight index.
   * The overnight rate accrual method should be compounded.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightRateCalculation calculation;
  /**
   * The cap schedule, optional.
   * <p>
   * This defines the strike value of a cap as an initial value and a list of adjustments.
   * Thus individual caplets may have different strike values.
   * The cap rate is only allowed to change at payment period boundaries.
   * <p>
   * If the product is not a cap, the cap schedule will be absent.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule capSchedule;
  /**
   * The floor schedule, optional.
   * <p>
   * This defines the strike value of a floor as an initial value and a list of adjustments.
   * Thus individual floorlets may have different strike values.
   * The floor rate is only allowed to change at payment period boundaries.
   * <p>
   * If the product is not a floor, the floor schedule will be absent.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule floorSchedule;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.paymentDateOffset(DaysAdjustment.NONE);
  }

  @ImmutableConstructor
  private OvernightInArrearsCapFloorLeg(
      PayReceive payReceive,
      PeriodicSchedule paymentSchedule,
      DaysAdjustment paymentDateOffset,
      Currency currency,
      ValueSchedule notional,
      OvernightRateCalculation calculation,
      ValueSchedule capSchedule,
      ValueSchedule floorSchedule) {

    this.payReceive = ArgChecker.notNull(payReceive, "payReceive");
    this.paymentSchedule = ArgChecker.notNull(paymentSchedule, "paymentSchedule");
    this.paymentDateOffset = ArgChecker.notNull(paymentDateOffset, "paymentDateOffset");
    this.currency = currency != null ? currency : calculation.getIndex().getCurrency();
    this.notional = notional;
    this.calculation = ArgChecker.notNull(calculation, "calculation");
    this.capSchedule = capSchedule;
    this.floorSchedule = floorSchedule;
    ArgChecker.isTrue(!this.getPaymentSchedule().getStubConvention().isPresent() ||
        this.getPaymentSchedule().getStubConvention().get().equals(StubConvention.NONE), "Stub period is not allowed");
    ArgChecker.isFalse(this.getCapSchedule().isPresent() == this.getFloorSchedule().isPresent(),
        "One of cap schedule and floor schedule should be empty");
    ArgChecker.isTrue(this.getCalculation().getAccrualMethod().equals(OvernightAccrualMethod.COMPOUNDED),
        "Overnight accrual method should be compounded");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   *
   * @return the start date of the leg
   */
  public AdjustableDate getStartDate() {
    return paymentSchedule.calculatedStartDate();
  }

  /**
   * Gets the accrual end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the termination date.
   *
   * @return the end date of the leg
   */
  public AdjustableDate getEndDate() {
    return paymentSchedule.calculatedEndDate();
  }

  /**
   * Gets the overnight index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   *
   * @return the overnight index
   */
  public OvernightIndex getIndex() {
    return calculation.getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedOvernightInArrearsCapFloorLeg resolve(ReferenceData refData) {
    Schedule adjustedSchedule = paymentSchedule.createSchedule(refData);
    DoubleArray cap = getCapSchedule().isPresent() ? capSchedule.resolveValues(adjustedSchedule) : null;
    DoubleArray floor = getFloorSchedule().isPresent() ? floorSchedule.resolveValues(adjustedSchedule) : null;
    DoubleArray notionals = notional.resolveValues(adjustedSchedule);
    List<RateAccrualPeriod> accrualPeriods = calculation.createAccrualPeriods(adjustedSchedule, adjustedSchedule, refData);
    DateAdjuster paymentDateAdjuster = paymentDateOffset.resolve(refData);
    ImmutableList.Builder<OvernightInArrearsCapletFloorletPeriod> periodsBuild = ImmutableList.builder();
    for (int i = 0; i < adjustedSchedule.size(); i++) {
      SchedulePeriod period = adjustedSchedule.getPeriod(i);
      LocalDate paymentDate = paymentDateAdjuster.adjust(period.getEndDate());
      double signedNotional = payReceive.normalize(notionals.get(i));
      periodsBuild.add(OvernightInArrearsCapletFloorletPeriod.builder()
          .unadjustedStartDate(period.getUnadjustedStartDate())
          .unadjustedEndDate(period.getUnadjustedEndDate())
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .overnightRate((OvernightCompoundedRateComputation) accrualPeriods.get(i).getRateComputation()) // arg checked
          .paymentDate(paymentDate)
          .notional(signedNotional)
          .currency(currency)
          .yearFraction(period.yearFraction(calculation.getDayCount(), adjustedSchedule))
          .caplet(cap != null ? cap.get(i) : null)
          .floorlet(floor != null ? floor.get(i) : null)
          .build());
    }
    return ResolvedOvernightInArrearsCapFloorLeg.builder()
        .capletFloorletPeriods(periodsBuild.build())
        .payReceive(payReceive)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapFloorLeg}.
   * @return the meta-bean, not null
   */
  public static OvernightInArrearsCapFloorLeg.Meta meta() {
    return OvernightInArrearsCapFloorLeg.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightInArrearsCapFloorLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightInArrearsCapFloorLeg.Builder builder() {
    return new OvernightInArrearsCapFloorLeg.Builder();
  }

  @Override
  public OvernightInArrearsCapFloorLeg.Meta metaBean() {
    return OvernightInArrearsCapFloorLeg.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * @return the value of the property, not null
   */
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic payment schedule.
   * <p>
   * This is used to define the periodic payment periods.
   * These are used directly or indirectly to determine other dates in the leg.
   * @return the value of the property, not null
   */
  public PeriodicSchedule getPaymentSchedule() {
    return paymentSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of payment from the base calculation period date, defaulted to 'None'.
   * <p>
   * The offset is applied to the adjusted end date of each payment period.
   * Offset can be based on calendar days or business days.
   * @return the value of the property, not null
   */
  public DaysAdjustment getPaymentDateOffset() {
    return paymentDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the leg associated with the notional.
   * <p>
   * This is the currency of the leg and the currency that payoff calculation is made in.
   * The amounts of the notional are expressed in terms of this currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, must be non-negative.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property, not null
   */
  public ValueSchedule getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interest rate accrual calculation.
   * <p>
   * The interest rate accrual is based on overnight index.
   * The overnight rate accrual method should be compounded.
   * @return the value of the property, not null
   */
  public OvernightRateCalculation getCalculation() {
    return calculation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cap schedule, optional.
   * <p>
   * This defines the strike value of a cap as an initial value and a list of adjustments.
   * Thus individual caplets may have different strike values.
   * The cap rate is only allowed to change at payment period boundaries.
   * <p>
   * If the product is not a cap, the cap schedule will be absent.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getCapSchedule() {
    return Optional.ofNullable(capSchedule);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the floor schedule, optional.
   * <p>
   * This defines the strike value of a floor as an initial value and a list of adjustments.
   * Thus individual floorlets may have different strike values.
   * The floor rate is only allowed to change at payment period boundaries.
   * <p>
   * If the product is not a floor, the floor schedule will be absent.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getFloorSchedule() {
    return Optional.ofNullable(floorSchedule);
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
      OvernightInArrearsCapFloorLeg other = (OvernightInArrearsCapFloorLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(paymentSchedule, other.paymentSchedule) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(calculation, other.calculation) &&
          JodaBeanUtils.equal(capSchedule, other.capSchedule) &&
          JodaBeanUtils.equal(floorSchedule, other.floorSchedule);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(calculation);
    hash = hash * 31 + JodaBeanUtils.hashCode(capSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(floorSchedule);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("OvernightInArrearsCapFloorLeg{");
    buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
    buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
    buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
    buf.append("calculation").append('=').append(JodaBeanUtils.toString(calculation)).append(',').append(' ');
    buf.append("capSchedule").append('=').append(JodaBeanUtils.toString(capSchedule)).append(',').append(' ');
    buf.append("floorSchedule").append('=').append(JodaBeanUtils.toString(floorSchedule));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapFloorLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", OvernightInArrearsCapFloorLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code paymentSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> paymentSchedule = DirectMetaProperty.ofImmutable(
        this, "paymentSchedule", OvernightInArrearsCapFloorLeg.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", OvernightInArrearsCapFloorLeg.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", OvernightInArrearsCapFloorLeg.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<ValueSchedule> notional = DirectMetaProperty.ofImmutable(
        this, "notional", OvernightInArrearsCapFloorLeg.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code calculation} property.
     */
    private final MetaProperty<OvernightRateCalculation> calculation = DirectMetaProperty.ofImmutable(
        this, "calculation", OvernightInArrearsCapFloorLeg.class, OvernightRateCalculation.class);
    /**
     * The meta-property for the {@code capSchedule} property.
     */
    private final MetaProperty<ValueSchedule> capSchedule = DirectMetaProperty.ofImmutable(
        this, "capSchedule", OvernightInArrearsCapFloorLeg.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code floorSchedule} property.
     */
    private final MetaProperty<ValueSchedule> floorSchedule = DirectMetaProperty.ofImmutable(
        this, "floorSchedule", OvernightInArrearsCapFloorLeg.class, ValueSchedule.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "paymentSchedule",
        "paymentDateOffset",
        "currency",
        "notional",
        "calculation",
        "capSchedule",
        "floorSchedule");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -934682935:  // calculation
          return calculation;
        case -596212599:  // capSchedule
          return capSchedule;
        case -1562227005:  // floorSchedule
          return floorSchedule;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightInArrearsCapFloorLeg.Builder builder() {
      return new OvernightInArrearsCapFloorLeg.Builder();
    }

    @Override
    public Class<? extends OvernightInArrearsCapFloorLeg> beanType() {
      return OvernightInArrearsCapFloorLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code payReceive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PayReceive> payReceive() {
      return payReceive;
    }

    /**
     * The meta-property for the {@code paymentSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> paymentSchedule() {
      return paymentSchedule;
    }

    /**
     * The meta-property for the {@code paymentDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> paymentDateOffset() {
      return paymentDateOffset;
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
    public MetaProperty<ValueSchedule> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code calculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightRateCalculation> calculation() {
      return calculation;
    }

    /**
     * The meta-property for the {@code capSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> capSchedule() {
      return capSchedule;
    }

    /**
     * The meta-property for the {@code floorSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> floorSchedule() {
      return floorSchedule;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((OvernightInArrearsCapFloorLeg) bean).getPayReceive();
        case -1499086147:  // paymentSchedule
          return ((OvernightInArrearsCapFloorLeg) bean).getPaymentSchedule();
        case -716438393:  // paymentDateOffset
          return ((OvernightInArrearsCapFloorLeg) bean).getPaymentDateOffset();
        case 575402001:  // currency
          return ((OvernightInArrearsCapFloorLeg) bean).getCurrency();
        case 1585636160:  // notional
          return ((OvernightInArrearsCapFloorLeg) bean).getNotional();
        case -934682935:  // calculation
          return ((OvernightInArrearsCapFloorLeg) bean).getCalculation();
        case -596212599:  // capSchedule
          return ((OvernightInArrearsCapFloorLeg) bean).capSchedule;
        case -1562227005:  // floorSchedule
          return ((OvernightInArrearsCapFloorLeg) bean).floorSchedule;
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
   * The bean-builder for {@code OvernightInArrearsCapFloorLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightInArrearsCapFloorLeg> {

    private PayReceive payReceive;
    private PeriodicSchedule paymentSchedule;
    private DaysAdjustment paymentDateOffset;
    private Currency currency;
    private ValueSchedule notional;
    private OvernightRateCalculation calculation;
    private ValueSchedule capSchedule;
    private ValueSchedule floorSchedule;

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
    private Builder(OvernightInArrearsCapFloorLeg beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.paymentSchedule = beanToCopy.getPaymentSchedule();
      this.paymentDateOffset = beanToCopy.getPaymentDateOffset();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.calculation = beanToCopy.getCalculation();
      this.capSchedule = beanToCopy.capSchedule;
      this.floorSchedule = beanToCopy.floorSchedule;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -934682935:  // calculation
          return calculation;
        case -596212599:  // capSchedule
          return capSchedule;
        case -1562227005:  // floorSchedule
          return floorSchedule;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          this.payReceive = (PayReceive) newValue;
          break;
        case -1499086147:  // paymentSchedule
          this.paymentSchedule = (PeriodicSchedule) newValue;
          break;
        case -716438393:  // paymentDateOffset
          this.paymentDateOffset = (DaysAdjustment) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (ValueSchedule) newValue;
          break;
        case -934682935:  // calculation
          this.calculation = (OvernightRateCalculation) newValue;
          break;
        case -596212599:  // capSchedule
          this.capSchedule = (ValueSchedule) newValue;
          break;
        case -1562227005:  // floorSchedule
          this.floorSchedule = (ValueSchedule) newValue;
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
    public OvernightInArrearsCapFloorLeg build() {
      return new OvernightInArrearsCapFloorLeg(
          payReceive,
          paymentSchedule,
          paymentDateOffset,
          currency,
          notional,
          calculation,
          capSchedule,
          floorSchedule);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
     * @param payReceive  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payReceive(PayReceive payReceive) {
      JodaBeanUtils.notNull(payReceive, "payReceive");
      this.payReceive = payReceive;
      return this;
    }

    /**
     * Sets the periodic payment schedule.
     * <p>
     * This is used to define the periodic payment periods.
     * These are used directly or indirectly to determine other dates in the leg.
     * @param paymentSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentSchedule(PeriodicSchedule paymentSchedule) {
      JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
      this.paymentSchedule = paymentSchedule;
      return this;
    }

    /**
     * Sets the offset of payment from the base calculation period date, defaulted to 'None'.
     * <p>
     * The offset is applied to the adjusted end date of each payment period.
     * Offset can be based on calendar days or business days.
     * @param paymentDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDateOffset(DaysAdjustment paymentDateOffset) {
      JodaBeanUtils.notNull(paymentDateOffset, "paymentDateOffset");
      this.paymentDateOffset = paymentDateOffset;
      return this;
    }

    /**
     * Sets the currency of the leg associated with the notional.
     * <p>
     * This is the currency of the leg and the currency that payoff calculation is made in.
     * The amounts of the notional are expressed in terms of this currency.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount, must be non-negative.
     * <p>
     * The notional amount applicable during the period.
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(ValueSchedule notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the interest rate accrual calculation.
     * <p>
     * The interest rate accrual is based on overnight index.
     * The overnight rate accrual method should be compounded.
     * @param calculation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calculation(OvernightRateCalculation calculation) {
      JodaBeanUtils.notNull(calculation, "calculation");
      this.calculation = calculation;
      return this;
    }

    /**
     * Sets the cap schedule, optional.
     * <p>
     * This defines the strike value of a cap as an initial value and a list of adjustments.
     * Thus individual caplets may have different strike values.
     * The cap rate is only allowed to change at payment period boundaries.
     * <p>
     * If the product is not a cap, the cap schedule will be absent.
     * @param capSchedule  the new value
     * @return this, for chaining, not null
     */
    public Builder capSchedule(ValueSchedule capSchedule) {
      this.capSchedule = capSchedule;
      return this;
    }

    /**
     * Sets the floor schedule, optional.
     * <p>
     * This defines the strike value of a floor as an initial value and a list of adjustments.
     * Thus individual floorlets may have different strike values.
     * The floor rate is only allowed to change at payment period boundaries.
     * <p>
     * If the product is not a floor, the floor schedule will be absent.
     * @param floorSchedule  the new value
     * @return this, for chaining, not null
     */
    public Builder floorSchedule(ValueSchedule floorSchedule) {
      this.floorSchedule = floorSchedule;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("OvernightInArrearsCapFloorLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("calculation").append('=').append(JodaBeanUtils.toString(calculation)).append(',').append(' ');
      buf.append("capSchedule").append('=').append(JodaBeanUtils.toString(capSchedule)).append(',').append(' ');
      buf.append("floorSchedule").append('=').append(JodaBeanUtils.toString(floorSchedule));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
