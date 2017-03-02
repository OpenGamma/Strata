/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.IborRateSwapLegConvention;

/**
 * A CMS leg of a constant maturity swap (CMS) product.
 * <p>
 * This defines a single CMS leg for CMS or CMS cap/floor.
 * The CMS leg of CMS periodically pays coupons based on swap rate, which is the observed
 * value of a {@linkplain SwapIndex swap index}.
 * A CMS cap/floor instruments are defined as a set of call/put options on successive swap
 * rates, creating CMS caplets/floorlets.
 * <p>
 * The periodic payments in the resolved leg are CMS coupons, CMS caplets or
 * CMS floorlets depending on the data in this leg.
 * The {@code capSchedule} field is used to represent strike values of individual caplets,
 * whereas {@code floorSchedule} is used to represent strike values of individual floorlets.
 * Thus at least one of {@code capSchedule} and {@code floorSchedule} must be empty.
 * If both the fields are absent, the periodic payments in this leg are CMS coupons.
 */
@BeanDefinition
public final class CmsLeg
    implements Resolvable<ResolvedCmsLeg>, ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative swap rates can result in a payment in the opposite direction
   * to that implied by this indicator.
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
   * The offset of payment from the base calculation period date.
   * <p>
   * The offset is applied to the adjusted end date of each payment period.
   * Offset can be based on calendar days or business days.
   * <p>
   * When building, this will default to the payment offset of the swap convention in the swap index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment paymentDateOffset;
  /**
   * The currency of the leg associated with the notional.
   * <p>
   * This is the currency of the leg and the currency that swap rate calculation is made in.
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
   * The swap index.
   * <p>
   * The swap rate to be paid is the observed value of this index.
   */
  @PropertyDefinition(validate = "notNull")
  private final SwapIndex index;
  /**
   * The base date that each fixing is made relative to, defaulted to 'PeriodStart'.
   * <p>
   * The fixing date is relative to either the start or end of each period.
   */
  @PropertyDefinition(validate = "notNull")
  private final FixingRelativeTo fixingRelativeTo;
  /**
   * The offset of the fixing date from each adjusted reset date.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * <p>
   * When building, this will default to the fixing offset of the swap convention in the swap index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment fixingDateOffset;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the swap convention in the swap index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
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
    builder.fixingRelativeTo = FixingRelativeTo.PERIOD_START;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      IborRateSwapLegConvention iborLeg = builder.index.getTemplate().getConvention().getFloatingLeg();
      if (builder.fixingDateOffset == null) {
        builder.fixingDateOffset = iborLeg.getFixingDateOffset();
      }
      if (builder.dayCount == null) {
        builder.dayCount = iborLeg.getDayCount();
      }
      if (builder.paymentDateOffset == null) {
        builder.paymentDateOffset = iborLeg.getPaymentDateOffset();
      }
      if (builder.currency == null) {
        builder.currency = iborLeg.getCurrency();
      }
    }
  }

  @ImmutableConstructor
  private CmsLeg(
      PayReceive payReceive,
      PeriodicSchedule paymentSchedule,
      DaysAdjustment paymentDateOffset,
      Currency currency,
      ValueSchedule notional,
      SwapIndex index,
      FixingRelativeTo fixingRelativeTo,
      DaysAdjustment fixingDateOffset,
      DayCount dayCount,
      ValueSchedule capSchedule,
      ValueSchedule floorSchedule) {

    this.payReceive = ArgChecker.notNull(payReceive, "payReceive");
    this.paymentSchedule = ArgChecker.notNull(paymentSchedule, "paymentSchedule");
    this.paymentDateOffset = paymentDateOffset;
    this.currency = currency;
    this.notional = ArgChecker.notNull(notional, "notional");
    this.index = ArgChecker.notNull(index, "index");
    this.fixingRelativeTo = fixingRelativeTo;
    this.fixingDateOffset = fixingDateOffset;
    this.dayCount = dayCount;
    this.capSchedule = capSchedule;
    this.floorSchedule = floorSchedule;
    ArgChecker.isTrue(!this.getPaymentSchedule().getStubConvention().isPresent() ||
        this.getPaymentSchedule().getStubConvention().get().equals(StubConvention.NONE), "Stub period is not allowed");
    ArgChecker.isFalse(this.getCapSchedule().isPresent() && this.getFloorSchedule().isPresent(),
        "At least one of cap schedule and floor schedule should be empty");
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
   * Gets the underlying Ibor index that the leg is based on.
   * 
   * @return the index
   */
  public IborIndex getUnderlyingIndex() {
    return index.getTemplate().getConvention().getFloatingLeg().getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedCmsLeg resolve(ReferenceData refData) {
    Schedule adjustedSchedule = paymentSchedule.createSchedule(refData);
    DoubleArray cap = getCapSchedule().isPresent() ? capSchedule.resolveValues(adjustedSchedule) : null;
    DoubleArray floor = getFloorSchedule().isPresent() ? floorSchedule.resolveValues(adjustedSchedule) : null;
    DoubleArray notionals = notional.resolveValues(adjustedSchedule);
    DateAdjuster fixingDateAdjuster = fixingDateOffset.resolve(refData);
    DateAdjuster paymentDateAdjuster = paymentDateOffset.resolve(refData);
    ImmutableList.Builder<CmsPeriod> cmsPeriodsBuild = ImmutableList.builder();
    for (int i = 0; i < adjustedSchedule.size(); i++) {
      SchedulePeriod period = adjustedSchedule.getPeriod(i);
      LocalDate fixingDate = fixingDateAdjuster.adjust(
          (fixingRelativeTo.equals(FixingRelativeTo.PERIOD_START)) ? period.getStartDate() : period.getEndDate());
      LocalDate paymentDate = paymentDateAdjuster.adjust(period.getEndDate());
      double signedNotional = payReceive.normalize(notionals.get(i));
      cmsPeriodsBuild.add(CmsPeriod.builder()
          .currency(currency)
          .notional(signedNotional)
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .unadjustedStartDate(period.getUnadjustedStartDate())
          .unadjustedEndDate(period.getUnadjustedEndDate())
          .yearFraction(period.yearFraction(dayCount, adjustedSchedule))
          .paymentDate(paymentDate)
          .fixingDate(fixingDate)
          .caplet(cap != null ? cap.get(i) : null)
          .floorlet(floor != null ? floor.get(i) : null)
          .dayCount(dayCount)
          .index(index)
          .underlyingSwap(createUnderlyingSwap(fixingDate, refData))
          .build());
    }
    return ResolvedCmsLeg.builder()
        .cmsPeriods(cmsPeriodsBuild.build())
        .payReceive(payReceive)
        .build();
  }

  // creates and resolves the underlying swap
  private ResolvedSwap createUnderlyingSwap(LocalDate fixingDate, ReferenceData refData) {
    FixedIborSwapConvention conv = index.getTemplate().getConvention();
    LocalDate effectiveDate = conv.calculateSpotDateFromTradeDate(fixingDate, refData);
    LocalDate maturityDate = effectiveDate.plus(index.getTemplate().getTenor());
    Swap swap = conv.toTrade(fixingDate, effectiveDate, maturityDate, BuySell.BUY, 1d, 1d).getProduct();
    return swap.resolve(refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CmsLeg}.
   * @return the meta-bean, not null
   */
  public static CmsLeg.Meta meta() {
    return CmsLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CmsLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CmsLeg.Builder builder() {
    return new CmsLeg.Builder();
  }

  @Override
  public CmsLeg.Meta metaBean() {
    return CmsLeg.Meta.INSTANCE;
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
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative swap rates can result in a payment in the opposite direction
   * to that implied by this indicator.
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
   * Gets the offset of payment from the base calculation period date.
   * <p>
   * The offset is applied to the adjusted end date of each payment period.
   * Offset can be based on calendar days or business days.
   * <p>
   * When building, this will default to the payment offset of the swap convention in the swap index if not specified.
   * @return the value of the property, not null
   */
  public DaysAdjustment getPaymentDateOffset() {
    return paymentDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the leg associated with the notional.
   * <p>
   * This is the currency of the leg and the currency that swap rate calculation is made in.
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
   * Gets the swap index.
   * <p>
   * The swap rate to be paid is the observed value of this index.
   * @return the value of the property, not null
   */
  public SwapIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base date that each fixing is made relative to, defaulted to 'PeriodStart'.
   * <p>
   * The fixing date is relative to either the start or end of each period.
   * @return the value of the property, not null
   */
  public FixingRelativeTo getFixingRelativeTo() {
    return fixingRelativeTo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the fixing date from each adjusted reset date.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * <p>
   * When building, this will default to the fixing offset of the swap convention in the swap index if not specified.
   * @return the value of the property, not null
   */
  public DaysAdjustment getFixingDateOffset() {
    return fixingDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the swap convention in the swap index if not specified.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
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
      CmsLeg other = (CmsLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(paymentSchedule, other.paymentSchedule) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fixingRelativeTo, other.fixingRelativeTo) &&
          JodaBeanUtils.equal(fixingDateOffset, other.fixingDateOffset) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingRelativeTo);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(capSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(floorSchedule);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(384);
    buf.append("CmsLeg{");
    buf.append("payReceive").append('=').append(payReceive).append(',').append(' ');
    buf.append("paymentSchedule").append('=').append(paymentSchedule).append(',').append(' ');
    buf.append("paymentDateOffset").append('=').append(paymentDateOffset).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("fixingRelativeTo").append('=').append(fixingRelativeTo).append(',').append(' ');
    buf.append("fixingDateOffset").append('=').append(fixingDateOffset).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("capSchedule").append('=').append(capSchedule).append(',').append(' ');
    buf.append("floorSchedule").append('=').append(JodaBeanUtils.toString(floorSchedule));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CmsLeg}.
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
        this, "payReceive", CmsLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code paymentSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> paymentSchedule = DirectMetaProperty.ofImmutable(
        this, "paymentSchedule", CmsLeg.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", CmsLeg.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CmsLeg.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<ValueSchedule> notional = DirectMetaProperty.ofImmutable(
        this, "notional", CmsLeg.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<SwapIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", CmsLeg.class, SwapIndex.class);
    /**
     * The meta-property for the {@code fixingRelativeTo} property.
     */
    private final MetaProperty<FixingRelativeTo> fixingRelativeTo = DirectMetaProperty.ofImmutable(
        this, "fixingRelativeTo", CmsLeg.class, FixingRelativeTo.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", CmsLeg.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", CmsLeg.class, DayCount.class);
    /**
     * The meta-property for the {@code capSchedule} property.
     */
    private final MetaProperty<ValueSchedule> capSchedule = DirectMetaProperty.ofImmutable(
        this, "capSchedule", CmsLeg.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code floorSchedule} property.
     */
    private final MetaProperty<ValueSchedule> floorSchedule = DirectMetaProperty.ofImmutable(
        this, "floorSchedule", CmsLeg.class, ValueSchedule.class);
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
        "index",
        "fixingRelativeTo",
        "fixingDateOffset",
        "dayCount",
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
        case 100346066:  // index
          return index;
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
        case -596212599:  // capSchedule
          return capSchedule;
        case -1562227005:  // floorSchedule
          return floorSchedule;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CmsLeg.Builder builder() {
      return new CmsLeg.Builder();
    }

    @Override
    public Class<? extends CmsLeg> beanType() {
      return CmsLeg.class;
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
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fixingRelativeTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixingRelativeTo> fixingRelativeTo() {
      return fixingRelativeTo;
    }

    /**
     * The meta-property for the {@code fixingDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingDateOffset() {
      return fixingDateOffset;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
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
          return ((CmsLeg) bean).getPayReceive();
        case -1499086147:  // paymentSchedule
          return ((CmsLeg) bean).getPaymentSchedule();
        case -716438393:  // paymentDateOffset
          return ((CmsLeg) bean).getPaymentDateOffset();
        case 575402001:  // currency
          return ((CmsLeg) bean).getCurrency();
        case 1585636160:  // notional
          return ((CmsLeg) bean).getNotional();
        case 100346066:  // index
          return ((CmsLeg) bean).getIndex();
        case 232554996:  // fixingRelativeTo
          return ((CmsLeg) bean).getFixingRelativeTo();
        case 873743726:  // fixingDateOffset
          return ((CmsLeg) bean).getFixingDateOffset();
        case 1905311443:  // dayCount
          return ((CmsLeg) bean).getDayCount();
        case -596212599:  // capSchedule
          return ((CmsLeg) bean).capSchedule;
        case -1562227005:  // floorSchedule
          return ((CmsLeg) bean).floorSchedule;
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
   * The bean-builder for {@code CmsLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CmsLeg> {

    private PayReceive payReceive;
    private PeriodicSchedule paymentSchedule;
    private DaysAdjustment paymentDateOffset;
    private Currency currency;
    private ValueSchedule notional;
    private SwapIndex index;
    private FixingRelativeTo fixingRelativeTo;
    private DaysAdjustment fixingDateOffset;
    private DayCount dayCount;
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
    private Builder(CmsLeg beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.paymentSchedule = beanToCopy.getPaymentSchedule();
      this.paymentDateOffset = beanToCopy.getPaymentDateOffset();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.index = beanToCopy.getIndex();
      this.fixingRelativeTo = beanToCopy.getFixingRelativeTo();
      this.fixingDateOffset = beanToCopy.getFixingDateOffset();
      this.dayCount = beanToCopy.getDayCount();
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
        case 100346066:  // index
          return index;
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
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
        case 100346066:  // index
          this.index = (SwapIndex) newValue;
          break;
        case 232554996:  // fixingRelativeTo
          this.fixingRelativeTo = (FixingRelativeTo) newValue;
          break;
        case 873743726:  // fixingDateOffset
          this.fixingDateOffset = (DaysAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
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
    public CmsLeg build() {
      preBuild(this);
      return new CmsLeg(
          payReceive,
          paymentSchedule,
          paymentDateOffset,
          currency,
          notional,
          index,
          fixingRelativeTo,
          fixingDateOffset,
          dayCount,
          capSchedule,
          floorSchedule);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
     * Note that negative swap rates can result in a payment in the opposite direction
     * to that implied by this indicator.
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
     * Sets the offset of payment from the base calculation period date.
     * <p>
     * The offset is applied to the adjusted end date of each payment period.
     * Offset can be based on calendar days or business days.
     * <p>
     * When building, this will default to the payment offset of the swap convention in the swap index if not specified.
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
     * This is the currency of the leg and the currency that swap rate calculation is made in.
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
     * Sets the swap index.
     * <p>
     * The swap rate to be paid is the observed value of this index.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(SwapIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the base date that each fixing is made relative to, defaulted to 'PeriodStart'.
     * <p>
     * The fixing date is relative to either the start or end of each period.
     * @param fixingRelativeTo  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingRelativeTo(FixingRelativeTo fixingRelativeTo) {
      JodaBeanUtils.notNull(fixingRelativeTo, "fixingRelativeTo");
      this.fixingRelativeTo = fixingRelativeTo;
      return this;
    }

    /**
     * Sets the offset of the fixing date from each adjusted reset date.
     * <p>
     * The offset is applied to the base date specified by {@code fixingRelativeTo}.
     * The offset is typically a negative number of business days.
     * <p>
     * When building, this will default to the fixing offset of the swap convention in the swap index if not specified.
     * @param fixingDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDateOffset(DaysAdjustment fixingDateOffset) {
      JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
      this.fixingDateOffset = fixingDateOffset;
      return this;
    }

    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * <p>
     * When building, this will default to the day count of the swap convention in the swap index if not specified.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
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
      StringBuilder buf = new StringBuilder(384);
      buf.append("CmsLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingRelativeTo").append('=').append(JodaBeanUtils.toString(fixingRelativeTo)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("capSchedule").append('=').append(JodaBeanUtils.toString(capSchedule)).append(',').append(' ');
      buf.append("floorSchedule").append('=').append(JodaBeanUtils.toString(floorSchedule));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
