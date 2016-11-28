/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.common.BuySell;

/**
 * A credit default swap (CDS), resolved for pricing.
 * <p>
 * This is the resolved form of {@link Cds} and is the primary input to the pricers.
 * Applications will typically create a {@code ResolvedCds} from a {@code Cds}
 * using {@link Cds#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedCds} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedCds
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * Whether the CDS is buy or sell.
   * <p>
   * A value of 'Buy' implies that the fee leg payments are being paid, and protection is being bought.
   * A value of 'Sell' implies that the fee leg payments are being received, and protection is being sold.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySellProtection;
  /**
   * The primary currency.
   * <p>
   * This is the currency of the CDS and the currency that payments are made in.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount used to calculate fee payments.
   */
  @PropertyDefinition(validate = "notNull")
  private final double notional;
  /**
   * The coupon used to calculate fee payments.
   */
  @PropertyDefinition(validate = "notNull")
  private final double coupon;
  /**
   * The date that the CDS nominally starts in terms of premium payments.
   * <p>
   * The number of days in the first period, and thus the amount of the first premium payment,
   * is counted from this date.
   * <p>
   * This should be adjusted according business day and holidays.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The date that the contract expires and protection ends.
   * <p>
   * Any default after this date does not trigger a payment.
   * The protection ends at the end of day.
   * <p>
   * This is an adjusted date and can fall on a holiday or weekend.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The business day adjustment to apply to the start and end dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The reference against which protection applies.
   * <p>
   * For a single-name CDS, this contains information on the entity/issue
   * For a CDS index, this contains information about the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ReferenceInformation referenceInformation;
  /**
   * Whether the accrued premium is paid in the event of a default.
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean payAccruedOnDefault;
  /**
   * The nominal period between premium payments, such as 3 months or 6 months.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period paymentInterval;
  /**
   * The stub convention to use.
   * <p>
   * This may be 'ShortInitial', 'LongInitial', 'ShortFinal', or 'LongFinal'.
   * The values 'None' and 'Both' are not allowed.
   */
  @PropertyDefinition(validate = "notNull")
  private final StubConvention stubConvention;
  /**
   * The day count convention to be used for calculating the accrual.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount accrualDayCount;
  /**
   * The upfront fee amount, optional.
   */
  @PropertyDefinition(get = "optional")
  private final Double upfrontFeeAmount;
  /**
   * The upfront fee date, optional.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate upfrontFeePaymentDate;

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedCds}.
   * @return the meta-bean, not null
   */
  public static ResolvedCds.Meta meta() {
    return ResolvedCds.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedCds.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedCds.Builder builder() {
    return new ResolvedCds.Builder();
  }

  private ResolvedCds(
      BuySell buySellProtection,
      Currency currency,
      double notional,
      double coupon,
      LocalDate startDate,
      LocalDate endDate,
      BusinessDayAdjustment businessDayAdjustment,
      ReferenceInformation referenceInformation,
      boolean payAccruedOnDefault,
      Period paymentInterval,
      StubConvention stubConvention,
      DayCount accrualDayCount,
      Double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate) {
    JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(notional, "notional");
    JodaBeanUtils.notNull(coupon, "coupon");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(referenceInformation, "referenceInformation");
    JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
    JodaBeanUtils.notNull(paymentInterval, "paymentInterval");
    JodaBeanUtils.notNull(stubConvention, "stubConvention");
    JodaBeanUtils.notNull(accrualDayCount, "accrualDayCount");
    this.buySellProtection = buySellProtection;
    this.currency = currency;
    this.notional = notional;
    this.coupon = coupon;
    this.startDate = startDate;
    this.endDate = endDate;
    this.businessDayAdjustment = businessDayAdjustment;
    this.referenceInformation = referenceInformation;
    this.payAccruedOnDefault = payAccruedOnDefault;
    this.paymentInterval = paymentInterval;
    this.stubConvention = stubConvention;
    this.accrualDayCount = accrualDayCount;
    this.upfrontFeeAmount = upfrontFeeAmount;
    this.upfrontFeePaymentDate = upfrontFeePaymentDate;
  }

  @Override
  public ResolvedCds.Meta metaBean() {
    return ResolvedCds.Meta.INSTANCE;
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
   * Gets whether the CDS is buy or sell.
   * <p>
   * A value of 'Buy' implies that the fee leg payments are being paid, and protection is being bought.
   * A value of 'Sell' implies that the fee leg payments are being received, and protection is being sold.
   * @return the value of the property, not null
   */
  public BuySell getBuySellProtection() {
    return buySellProtection;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency.
   * <p>
   * This is the currency of the CDS and the currency that payments are made in.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount used to calculate fee payments.
   * @return the value of the property, not null
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the coupon used to calculate fee payments.
   * @return the value of the property, not null
   */
  public double getCoupon() {
    return coupon;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the CDS nominally starts in terms of premium payments.
   * <p>
   * The number of days in the first period, and thus the amount of the first premium payment,
   * is counted from this date.
   * <p>
   * This should be adjusted according business day and holidays.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the contract expires and protection ends.
   * <p>
   * Any default after this date does not trigger a payment.
   * The protection ends at the end of day.
   * <p>
   * This is an adjusted date and can fall on a holiday or weekend.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end dates.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference against which protection applies.
   * <p>
   * For a single-name CDS, this contains information on the entity/issue
   * For a CDS index, this contains information about the index.
   * @return the value of the property, not null
   */
  public ReferenceInformation getReferenceInformation() {
    return referenceInformation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the accrued premium is paid in the event of a default.
   * @return the value of the property, not null
   */
  public boolean isPayAccruedOnDefault() {
    return payAccruedOnDefault;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nominal period between premium payments, such as 3 months or 6 months.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * @return the value of the property, not null
   */
  public Period getPaymentInterval() {
    return paymentInterval;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the stub convention to use.
   * <p>
   * This may be 'ShortInitial', 'LongInitial', 'ShortFinal', or 'LongFinal'.
   * The values 'None' and 'Both' are not allowed.
   * @return the value of the property, not null
   */
  public StubConvention getStubConvention() {
    return stubConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention to be used for calculating the accrual.
   * @return the value of the property, not null
   */
  public DayCount getAccrualDayCount() {
    return accrualDayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the upfront fee amount, optional.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getUpfrontFeeAmount() {
    return upfrontFeeAmount != null ? OptionalDouble.of(upfrontFeeAmount) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the upfront fee date, optional.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getUpfrontFeePaymentDate() {
    return Optional.ofNullable(upfrontFeePaymentDate);
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
      ResolvedCds other = (ResolvedCds) obj;
      return JodaBeanUtils.equal(buySellProtection, other.buySellProtection) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(coupon, other.coupon) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(referenceInformation, other.referenceInformation) &&
          (payAccruedOnDefault == other.payAccruedOnDefault) &&
          JodaBeanUtils.equal(paymentInterval, other.paymentInterval) &&
          JodaBeanUtils.equal(stubConvention, other.stubConvention) &&
          JodaBeanUtils.equal(accrualDayCount, other.accrualDayCount) &&
          JodaBeanUtils.equal(upfrontFeeAmount, other.upfrontFeeAmount) &&
          JodaBeanUtils.equal(upfrontFeePaymentDate, other.upfrontFeePaymentDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(buySellProtection);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(coupon);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceInformation);
    hash = hash * 31 + JodaBeanUtils.hashCode(payAccruedOnDefault);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentInterval);
    hash = hash * 31 + JodaBeanUtils.hashCode(stubConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualDayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(upfrontFeeAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(upfrontFeePaymentDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(480);
    buf.append("ResolvedCds{");
    buf.append("buySellProtection").append('=').append(buySellProtection).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("coupon").append('=').append(coupon).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("referenceInformation").append('=').append(referenceInformation).append(',').append(' ');
    buf.append("payAccruedOnDefault").append('=').append(payAccruedOnDefault).append(',').append(' ');
    buf.append("paymentInterval").append('=').append(paymentInterval).append(',').append(' ');
    buf.append("stubConvention").append('=').append(stubConvention).append(',').append(' ');
    buf.append("accrualDayCount").append('=').append(accrualDayCount).append(',').append(' ');
    buf.append("upfrontFeeAmount").append('=').append(upfrontFeeAmount).append(',').append(' ');
    buf.append("upfrontFeePaymentDate").append('=').append(JodaBeanUtils.toString(upfrontFeePaymentDate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedCds}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code buySellProtection} property.
     */
    private final MetaProperty<BuySell> buySellProtection = DirectMetaProperty.ofImmutable(
        this, "buySellProtection", ResolvedCds.class, BuySell.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ResolvedCds.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", ResolvedCds.class, Double.TYPE);
    /**
     * The meta-property for the {@code coupon} property.
     */
    private final MetaProperty<Double> coupon = DirectMetaProperty.ofImmutable(
        this, "coupon", ResolvedCds.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", ResolvedCds.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", ResolvedCds.class, LocalDate.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", ResolvedCds.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code referenceInformation} property.
     */
    private final MetaProperty<ReferenceInformation> referenceInformation = DirectMetaProperty.ofImmutable(
        this, "referenceInformation", ResolvedCds.class, ReferenceInformation.class);
    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccruedOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccruedOnDefault", ResolvedCds.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code paymentInterval} property.
     */
    private final MetaProperty<Period> paymentInterval = DirectMetaProperty.ofImmutable(
        this, "paymentInterval", ResolvedCds.class, Period.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", ResolvedCds.class, StubConvention.class);
    /**
     * The meta-property for the {@code accrualDayCount} property.
     */
    private final MetaProperty<DayCount> accrualDayCount = DirectMetaProperty.ofImmutable(
        this, "accrualDayCount", ResolvedCds.class, DayCount.class);
    /**
     * The meta-property for the {@code upfrontFeeAmount} property.
     */
    private final MetaProperty<Double> upfrontFeeAmount = DirectMetaProperty.ofImmutable(
        this, "upfrontFeeAmount", ResolvedCds.class, Double.class);
    /**
     * The meta-property for the {@code upfrontFeePaymentDate} property.
     */
    private final MetaProperty<LocalDate> upfrontFeePaymentDate = DirectMetaProperty.ofImmutable(
        this, "upfrontFeePaymentDate", ResolvedCds.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySellProtection",
        "currency",
        "notional",
        "coupon",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "referenceInformation",
        "payAccruedOnDefault",
        "paymentInterval",
        "stubConvention",
        "accrualDayCount",
        "upfrontFeeAmount",
        "upfrontFeePaymentDate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          return buySellProtection;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1354573786:  // coupon
          return coupon;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -2117930783:  // referenceInformation
          return referenceInformation;
        case -43782841:  // payAccruedOnDefault
          return payAccruedOnDefault;
        case -230746901:  // paymentInterval
          return paymentInterval;
        case -31408449:  // stubConvention
          return stubConvention;
        case -1387075166:  // accrualDayCount
          return accrualDayCount;
        case 2020904624:  // upfrontFeeAmount
          return upfrontFeeAmount;
        case 508500860:  // upfrontFeePaymentDate
          return upfrontFeePaymentDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedCds.Builder builder() {
      return new ResolvedCds.Builder();
    }

    @Override
    public Class<? extends ResolvedCds> beanType() {
      return ResolvedCds.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code buySellProtection} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySellProtection() {
      return buySellProtection;
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
     * The meta-property for the {@code coupon} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> coupon() {
      return coupon;
    }

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
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code referenceInformation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ReferenceInformation> referenceInformation() {
      return referenceInformation;
    }

    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> payAccruedOnDefault() {
      return payAccruedOnDefault;
    }

    /**
     * The meta-property for the {@code paymentInterval} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> paymentInterval() {
      return paymentInterval;
    }

    /**
     * The meta-property for the {@code stubConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StubConvention> stubConvention() {
      return stubConvention;
    }

    /**
     * The meta-property for the {@code accrualDayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> accrualDayCount() {
      return accrualDayCount;
    }

    /**
     * The meta-property for the {@code upfrontFeeAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> upfrontFeeAmount() {
      return upfrontFeeAmount;
    }

    /**
     * The meta-property for the {@code upfrontFeePaymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> upfrontFeePaymentDate() {
      return upfrontFeePaymentDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          return ((ResolvedCds) bean).getBuySellProtection();
        case 575402001:  // currency
          return ((ResolvedCds) bean).getCurrency();
        case 1585636160:  // notional
          return ((ResolvedCds) bean).getNotional();
        case -1354573786:  // coupon
          return ((ResolvedCds) bean).getCoupon();
        case -2129778896:  // startDate
          return ((ResolvedCds) bean).getStartDate();
        case -1607727319:  // endDate
          return ((ResolvedCds) bean).getEndDate();
        case -1065319863:  // businessDayAdjustment
          return ((ResolvedCds) bean).getBusinessDayAdjustment();
        case -2117930783:  // referenceInformation
          return ((ResolvedCds) bean).getReferenceInformation();
        case -43782841:  // payAccruedOnDefault
          return ((ResolvedCds) bean).isPayAccruedOnDefault();
        case -230746901:  // paymentInterval
          return ((ResolvedCds) bean).getPaymentInterval();
        case -31408449:  // stubConvention
          return ((ResolvedCds) bean).getStubConvention();
        case -1387075166:  // accrualDayCount
          return ((ResolvedCds) bean).getAccrualDayCount();
        case 2020904624:  // upfrontFeeAmount
          return ((ResolvedCds) bean).upfrontFeeAmount;
        case 508500860:  // upfrontFeePaymentDate
          return ((ResolvedCds) bean).upfrontFeePaymentDate;
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
   * The bean-builder for {@code ResolvedCds}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedCds> {

    private BuySell buySellProtection;
    private Currency currency;
    private double notional;
    private double coupon;
    private LocalDate startDate;
    private LocalDate endDate;
    private BusinessDayAdjustment businessDayAdjustment;
    private ReferenceInformation referenceInformation;
    private boolean payAccruedOnDefault;
    private Period paymentInterval;
    private StubConvention stubConvention;
    private DayCount accrualDayCount;
    private Double upfrontFeeAmount;
    private LocalDate upfrontFeePaymentDate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedCds beanToCopy) {
      this.buySellProtection = beanToCopy.getBuySellProtection();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.coupon = beanToCopy.getCoupon();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.referenceInformation = beanToCopy.getReferenceInformation();
      this.payAccruedOnDefault = beanToCopy.isPayAccruedOnDefault();
      this.paymentInterval = beanToCopy.getPaymentInterval();
      this.stubConvention = beanToCopy.getStubConvention();
      this.accrualDayCount = beanToCopy.getAccrualDayCount();
      this.upfrontFeeAmount = beanToCopy.upfrontFeeAmount;
      this.upfrontFeePaymentDate = beanToCopy.upfrontFeePaymentDate;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          return buySellProtection;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1354573786:  // coupon
          return coupon;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -2117930783:  // referenceInformation
          return referenceInformation;
        case -43782841:  // payAccruedOnDefault
          return payAccruedOnDefault;
        case -230746901:  // paymentInterval
          return paymentInterval;
        case -31408449:  // stubConvention
          return stubConvention;
        case -1387075166:  // accrualDayCount
          return accrualDayCount;
        case 2020904624:  // upfrontFeeAmount
          return upfrontFeeAmount;
        case 508500860:  // upfrontFeePaymentDate
          return upfrontFeePaymentDate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          this.buySellProtection = (BuySell) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1354573786:  // coupon
          this.coupon = (Double) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -2117930783:  // referenceInformation
          this.referenceInformation = (ReferenceInformation) newValue;
          break;
        case -43782841:  // payAccruedOnDefault
          this.payAccruedOnDefault = (Boolean) newValue;
          break;
        case -230746901:  // paymentInterval
          this.paymentInterval = (Period) newValue;
          break;
        case -31408449:  // stubConvention
          this.stubConvention = (StubConvention) newValue;
          break;
        case -1387075166:  // accrualDayCount
          this.accrualDayCount = (DayCount) newValue;
          break;
        case 2020904624:  // upfrontFeeAmount
          this.upfrontFeeAmount = (Double) newValue;
          break;
        case 508500860:  // upfrontFeePaymentDate
          this.upfrontFeePaymentDate = (LocalDate) newValue;
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
    public ResolvedCds build() {
      return new ResolvedCds(
          buySellProtection,
          currency,
          notional,
          coupon,
          startDate,
          endDate,
          businessDayAdjustment,
          referenceInformation,
          payAccruedOnDefault,
          paymentInterval,
          stubConvention,
          accrualDayCount,
          upfrontFeeAmount,
          upfrontFeePaymentDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the CDS is buy or sell.
     * <p>
     * A value of 'Buy' implies that the fee leg payments are being paid, and protection is being bought.
     * A value of 'Sell' implies that the fee leg payments are being received, and protection is being sold.
     * @param buySellProtection  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySellProtection(BuySell buySellProtection) {
      JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
      this.buySellProtection = buySellProtection;
      return this;
    }

    /**
     * Sets the primary currency.
     * <p>
     * This is the currency of the CDS and the currency that payments are made in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount used to calculate fee payments.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the coupon used to calculate fee payments.
     * @param coupon  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder coupon(double coupon) {
      JodaBeanUtils.notNull(coupon, "coupon");
      this.coupon = coupon;
      return this;
    }

    /**
     * Sets the date that the CDS nominally starts in terms of premium payments.
     * <p>
     * The number of days in the first period, and thus the amount of the first premium payment,
     * is counted from this date.
     * <p>
     * This should be adjusted according business day and holidays.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the date that the contract expires and protection ends.
     * <p>
     * Any default after this date does not trigger a payment.
     * The protection ends at the end of day.
     * <p>
     * This is an adjusted date and can fall on a holiday or weekend.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end dates.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the reference against which protection applies.
     * <p>
     * For a single-name CDS, this contains information on the entity/issue
     * For a CDS index, this contains information about the index.
     * @param referenceInformation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceInformation(ReferenceInformation referenceInformation) {
      JodaBeanUtils.notNull(referenceInformation, "referenceInformation");
      this.referenceInformation = referenceInformation;
      return this;
    }

    /**
     * Sets whether the accrued premium is paid in the event of a default.
     * @param payAccruedOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccruedOnDefault(boolean payAccruedOnDefault) {
      JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
      this.payAccruedOnDefault = payAccruedOnDefault;
      return this;
    }

    /**
     * Sets the nominal period between premium payments, such as 3 months or 6 months.
     * <p>
     * Regular payments will be made at the specified periodic frequency.
     * @param paymentInterval  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentInterval(Period paymentInterval) {
      JodaBeanUtils.notNull(paymentInterval, "paymentInterval");
      this.paymentInterval = paymentInterval;
      return this;
    }

    /**
     * Sets the stub convention to use.
     * <p>
     * This may be 'ShortInitial', 'LongInitial', 'ShortFinal', or 'LongFinal'.
     * The values 'None' and 'Both' are not allowed.
     * @param stubConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      JodaBeanUtils.notNull(stubConvention, "stubConvention");
      this.stubConvention = stubConvention;
      return this;
    }

    /**
     * Sets the day count convention to be used for calculating the accrual.
     * @param accrualDayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualDayCount(DayCount accrualDayCount) {
      JodaBeanUtils.notNull(accrualDayCount, "accrualDayCount");
      this.accrualDayCount = accrualDayCount;
      return this;
    }

    /**
     * Sets the upfront fee amount, optional.
     * @param upfrontFeeAmount  the new value
     * @return this, for chaining, not null
     */
    public Builder upfrontFeeAmount(Double upfrontFeeAmount) {
      this.upfrontFeeAmount = upfrontFeeAmount;
      return this;
    }

    /**
     * Sets the upfront fee date, optional.
     * @param upfrontFeePaymentDate  the new value
     * @return this, for chaining, not null
     */
    public Builder upfrontFeePaymentDate(LocalDate upfrontFeePaymentDate) {
      this.upfrontFeePaymentDate = upfrontFeePaymentDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(480);
      buf.append("ResolvedCds.Builder{");
      buf.append("buySellProtection").append('=').append(JodaBeanUtils.toString(buySellProtection)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("coupon").append('=').append(JodaBeanUtils.toString(coupon)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("referenceInformation").append('=').append(JodaBeanUtils.toString(referenceInformation)).append(',').append(' ');
      buf.append("payAccruedOnDefault").append('=').append(JodaBeanUtils.toString(payAccruedOnDefault)).append(',').append(' ');
      buf.append("paymentInterval").append('=').append(JodaBeanUtils.toString(paymentInterval)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("accrualDayCount").append('=').append(JodaBeanUtils.toString(accrualDayCount)).append(',').append(' ');
      buf.append("upfrontFeeAmount").append('=').append(JodaBeanUtils.toString(upfrontFeeAmount)).append(',').append(' ');
      buf.append("upfrontFeePaymentDate").append('=').append(JodaBeanUtils.toString(upfrontFeePaymentDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
