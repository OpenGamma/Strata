/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Convention;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.common.ImmLogic;
import com.opengamma.strata.finance.credit.common.RedCode;
import com.opengamma.strata.finance.credit.fee.FeeLeg;
import com.opengamma.strata.finance.credit.fee.PeriodicPayments;
import com.opengamma.strata.finance.credit.general.GeneralTerms;
import com.opengamma.strata.finance.credit.general.reference.SeniorityLevel;
import com.opengamma.strata.finance.credit.protection.ProtectionTerms;
import com.opengamma.strata.finance.credit.protection.RestructuringClause;
import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@BeanDefinition
public final class StandardSingleNameCdsConvention
    implements Convention, ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull")
  private final Currency currency;

  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;

  @PropertyDefinition(validate = "notNull")
  private final BusinessDayConvention dayConvention;

  @PropertyDefinition(validate = "notNull")
  private final Frequency paymentFrequency;

  @PropertyDefinition(validate = "notNull")
  private final RollConvention rollConvention;

  @PropertyDefinition(validate = "notNull")
  private final boolean payAccOnDefault;

  @PropertyDefinition(validate = "notNull")
  private final HolidayCalendar calendar;

  @PropertyDefinition(get = "field")
  private final StubConvention stubConvention;

  @PropertyDefinition(validate = "notNull")
  private final int stepIn;

  @PropertyDefinition(validate = "notNull")
  private final int settleLag;


  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    //   ArgChecker.isTrue(fixedLeg.getCurrency().equals(floatingLeg.getCurrency()), "Conventions must have same currency");
  }

  //-------------------------------------------------------------------------

  public CdsTrade toTrade(
      StandardId id,
      LocalDate tradeDate,
      Period period,
      BuySell buySell,
      double notional,
      double coupon,
      RedCode referenceEntityId,
      String referenceEntityName,
      SeniorityLevel seniorityLevel,
      RestructuringClause restructuringClause
  ) {
//    ArgChecker.inOrderOrEqual(tradeDate, startDate, "tradeDate", "startDate");
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(
        dayConvention,
        calendar
    );
    LocalDate unadjustedStartDate = calcUnadjustedAccrualStartDate(tradeDate);

    // Standard maturity dates are unadjusted – always Mar/Jun/Sep/Dec 20th.
    LocalDate unadjustedEndDate = calcUnadjustedMaturityDate(unadjustedStartDate, period);

    LocalDate stepInDate = businessDayAdjustment.adjust(tradeDate.plusDays(stepIn));
    LocalDate settleDate = businessDayAdjustment.adjust(tradeDate.plusDays(settleLag));

    PeriodicSchedule periodicSchedule = PeriodicSchedule.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(paymentFrequency)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConvention)
        .rollConvention(rollConvention)
        .build();

    LocalDate adjustedStartDate = periodicSchedule.getAdjustedStartDate();
    LocalDate adjustedEndDate = periodicSchedule.getAdjustedEndDate();

    return CdsTrade.of(
        TradeInfo
            .builder()
            .id(id)
            .tradeDate(tradeDate)
            .settlementDate(settleDate)
            .build(),
        Cds.of(
            GeneralTerms.singleName(
                adjustedStartDate,
                adjustedEndDate,
                buySell,
                businessDayAdjustment,
                referenceEntityId,
                referenceEntityName,
                currency,
                seniorityLevel
            ),
            FeeLeg.of(
                PeriodicPayments.of(
                    periodicSchedule,
                    CurrencyAmount.of(currency, notional),
                    coupon,
                    dayCount
                )
            ),
            ProtectionTerms.of(
                CurrencyAmount.of(currency, notional),
                restructuringClause
            )
        )
    );
  }

  private static LocalDate calcUnadjustedAccrualStartDate(LocalDate tradeDate) {
    return ImmLogic.getPrevIMMDate(tradeDate);
  }

  /**
   * Standard maturity dates are unadjusted – always Mar/Jun/Sep/Dec 20th.
   * Example: As of Feb09, the 1y standard CDS contract would protect the buyer through Sat 20Mar10.
   * @param unadjustedAccrualStartDate beginning of protection
   * @param period length of the CDS
   * @return
   */
  private static LocalDate calcUnadjustedMaturityDate(LocalDate unadjustedAccrualStartDate, Period period) {
    return unadjustedAccrualStartDate.plus(period);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code StandardSingleNameCdsConvention}.
   * @return the meta-bean, not null
   */
  public static StandardSingleNameCdsConvention.Meta meta() {
    return StandardSingleNameCdsConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(StandardSingleNameCdsConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static StandardSingleNameCdsConvention.Builder builder() {
    return new StandardSingleNameCdsConvention.Builder();
  }

  private StandardSingleNameCdsConvention(
      Currency currency,
      DayCount dayCount,
      BusinessDayConvention dayConvention,
      Frequency paymentFrequency,
      RollConvention rollConvention,
      boolean payAccOnDefault,
      HolidayCalendar calendar,
      StubConvention stubConvention,
      int stepIn,
      int settleLag) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(dayConvention, "dayConvention");
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    JodaBeanUtils.notNull(rollConvention, "rollConvention");
    JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
    JodaBeanUtils.notNull(calendar, "calendar");
    JodaBeanUtils.notNull(stepIn, "stepIn");
    JodaBeanUtils.notNull(settleLag, "settleLag");
    this.currency = currency;
    this.dayCount = dayCount;
    this.dayConvention = dayConvention;
    this.paymentFrequency = paymentFrequency;
    this.rollConvention = rollConvention;
    this.payAccOnDefault = payAccOnDefault;
    this.calendar = calendar;
    this.stubConvention = stubConvention;
    this.stepIn = stepIn;
    this.settleLag = settleLag;
    validate();
  }

  @Override
  public StandardSingleNameCdsConvention.Meta metaBean() {
    return StandardSingleNameCdsConvention.Meta.INSTANCE;
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
   * Gets the currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the dayCount.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the dayConvention.
   * @return the value of the property, not null
   */
  public BusinessDayConvention getDayConvention() {
    return dayConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the paymentFrequency.
   * @return the value of the property, not null
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rollConvention.
   * @return the value of the property, not null
   */
  public RollConvention getRollConvention() {
    return rollConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payAccOnDefault.
   * @return the value of the property, not null
   */
  public boolean isPayAccOnDefault() {
    return payAccOnDefault;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calendar.
   * @return the value of the property, not null
   */
  public HolidayCalendar getCalendar() {
    return calendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the stepIn.
   * @return the value of the property, not null
   */
  public int getStepIn() {
    return stepIn;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the settleLag.
   * @return the value of the property, not null
   */
  public int getSettleLag() {
    return settleLag;
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
      StandardSingleNameCdsConvention other = (StandardSingleNameCdsConvention) obj;
      return JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getDayConvention(), other.getDayConvention()) &&
          JodaBeanUtils.equal(getPaymentFrequency(), other.getPaymentFrequency()) &&
          JodaBeanUtils.equal(getRollConvention(), other.getRollConvention()) &&
          (isPayAccOnDefault() == other.isPayAccOnDefault()) &&
          JodaBeanUtils.equal(getCalendar(), other.getCalendar()) &&
          JodaBeanUtils.equal(stubConvention, other.stubConvention) &&
          (getStepIn() == other.getStepIn()) &&
          (getSettleLag() == other.getSettleLag());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentFrequency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRollConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(isPayAccOnDefault());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCalendar());
    hash = hash * 31 + JodaBeanUtils.hashCode(stubConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(getStepIn());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSettleLag());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("StandardSingleNameCdsConvention{");
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("dayConvention").append('=').append(getDayConvention()).append(',').append(' ');
    buf.append("paymentFrequency").append('=').append(getPaymentFrequency()).append(',').append(' ');
    buf.append("rollConvention").append('=').append(getRollConvention()).append(',').append(' ');
    buf.append("payAccOnDefault").append('=').append(isPayAccOnDefault()).append(',').append(' ');
    buf.append("calendar").append('=').append(getCalendar()).append(',').append(' ');
    buf.append("stubConvention").append('=').append(stubConvention).append(',').append(' ');
    buf.append("stepIn").append('=').append(getStepIn()).append(',').append(' ');
    buf.append("settleLag").append('=').append(JodaBeanUtils.toString(getSettleLag()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code StandardSingleNameCdsConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", StandardSingleNameCdsConvention.class, Currency.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", StandardSingleNameCdsConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code dayConvention} property.
     */
    private final MetaProperty<BusinessDayConvention> dayConvention = DirectMetaProperty.ofImmutable(
        this, "dayConvention", StandardSingleNameCdsConvention.class, BusinessDayConvention.class);
    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", StandardSingleNameCdsConvention.class, Frequency.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", StandardSingleNameCdsConvention.class, RollConvention.class);
    /**
     * The meta-property for the {@code payAccOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccOnDefault", StandardSingleNameCdsConvention.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code calendar} property.
     */
    private final MetaProperty<HolidayCalendar> calendar = DirectMetaProperty.ofImmutable(
        this, "calendar", StandardSingleNameCdsConvention.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", StandardSingleNameCdsConvention.class, StubConvention.class);
    /**
     * The meta-property for the {@code stepIn} property.
     */
    private final MetaProperty<Integer> stepIn = DirectMetaProperty.ofImmutable(
        this, "stepIn", StandardSingleNameCdsConvention.class, Integer.TYPE);
    /**
     * The meta-property for the {@code settleLag} property.
     */
    private final MetaProperty<Integer> settleLag = DirectMetaProperty.ofImmutable(
        this, "settleLag", StandardSingleNameCdsConvention.class, Integer.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "dayCount",
        "dayConvention",
        "paymentFrequency",
        "rollConvention",
        "payAccOnDefault",
        "calendar",
        "stubConvention",
        "stepIn",
        "settleLag");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1905311443:  // dayCount
          return dayCount;
        case 1710876717:  // dayConvention
          return dayConvention;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -10223666:  // rollConvention
          return rollConvention;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
        case -178324674:  // calendar
          return calendar;
        case -31408449:  // stubConvention
          return stubConvention;
        case -892367599:  // stepIn
          return stepIn;
        case 1526370375:  // settleLag
          return settleLag;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public StandardSingleNameCdsConvention.Builder builder() {
      return new StandardSingleNameCdsConvention.Builder();
    }

    @Override
    public Class<? extends StandardSingleNameCdsConvention> beanType() {
      return StandardSingleNameCdsConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code dayConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayConvention> dayConvention() {
      return dayConvention;
    }

    /**
     * The meta-property for the {@code paymentFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> paymentFrequency() {
      return paymentFrequency;
    }

    /**
     * The meta-property for the {@code rollConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RollConvention> rollConvention() {
      return rollConvention;
    }

    /**
     * The meta-property for the {@code payAccOnDefault} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> payAccOnDefault() {
      return payAccOnDefault;
    }

    /**
     * The meta-property for the {@code calendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> calendar() {
      return calendar;
    }

    /**
     * The meta-property for the {@code stubConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StubConvention> stubConvention() {
      return stubConvention;
    }

    /**
     * The meta-property for the {@code stepIn} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> stepIn() {
      return stepIn;
    }

    /**
     * The meta-property for the {@code settleLag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> settleLag() {
      return settleLag;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((StandardSingleNameCdsConvention) bean).getCurrency();
        case 1905311443:  // dayCount
          return ((StandardSingleNameCdsConvention) bean).getDayCount();
        case 1710876717:  // dayConvention
          return ((StandardSingleNameCdsConvention) bean).getDayConvention();
        case 863656438:  // paymentFrequency
          return ((StandardSingleNameCdsConvention) bean).getPaymentFrequency();
        case -10223666:  // rollConvention
          return ((StandardSingleNameCdsConvention) bean).getRollConvention();
        case -988493655:  // payAccOnDefault
          return ((StandardSingleNameCdsConvention) bean).isPayAccOnDefault();
        case -178324674:  // calendar
          return ((StandardSingleNameCdsConvention) bean).getCalendar();
        case -31408449:  // stubConvention
          return ((StandardSingleNameCdsConvention) bean).stubConvention;
        case -892367599:  // stepIn
          return ((StandardSingleNameCdsConvention) bean).getStepIn();
        case 1526370375:  // settleLag
          return ((StandardSingleNameCdsConvention) bean).getSettleLag();
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
   * The bean-builder for {@code StandardSingleNameCdsConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<StandardSingleNameCdsConvention> {

    private Currency currency;
    private DayCount dayCount;
    private BusinessDayConvention dayConvention;
    private Frequency paymentFrequency;
    private RollConvention rollConvention;
    private boolean payAccOnDefault;
    private HolidayCalendar calendar;
    private StubConvention stubConvention;
    private int stepIn;
    private int settleLag;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(StandardSingleNameCdsConvention beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.dayCount = beanToCopy.getDayCount();
      this.dayConvention = beanToCopy.getDayConvention();
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
      this.rollConvention = beanToCopy.getRollConvention();
      this.payAccOnDefault = beanToCopy.isPayAccOnDefault();
      this.calendar = beanToCopy.getCalendar();
      this.stubConvention = beanToCopy.stubConvention;
      this.stepIn = beanToCopy.getStepIn();
      this.settleLag = beanToCopy.getSettleLag();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1905311443:  // dayCount
          return dayCount;
        case 1710876717:  // dayConvention
          return dayConvention;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -10223666:  // rollConvention
          return rollConvention;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
        case -178324674:  // calendar
          return calendar;
        case -31408449:  // stubConvention
          return stubConvention;
        case -892367599:  // stepIn
          return stepIn;
        case 1526370375:  // settleLag
          return settleLag;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 1710876717:  // dayConvention
          this.dayConvention = (BusinessDayConvention) newValue;
          break;
        case 863656438:  // paymentFrequency
          this.paymentFrequency = (Frequency) newValue;
          break;
        case -10223666:  // rollConvention
          this.rollConvention = (RollConvention) newValue;
          break;
        case -988493655:  // payAccOnDefault
          this.payAccOnDefault = (Boolean) newValue;
          break;
        case -178324674:  // calendar
          this.calendar = (HolidayCalendar) newValue;
          break;
        case -31408449:  // stubConvention
          this.stubConvention = (StubConvention) newValue;
          break;
        case -892367599:  // stepIn
          this.stepIn = (Integer) newValue;
          break;
        case 1526370375:  // settleLag
          this.settleLag = (Integer) newValue;
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
    public StandardSingleNameCdsConvention build() {
      return new StandardSingleNameCdsConvention(
          currency,
          dayCount,
          dayConvention,
          paymentFrequency,
          rollConvention,
          payAccOnDefault,
          calendar,
          stubConvention,
          stepIn,
          settleLag);
    }

    //-----------------------------------------------------------------------
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
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the {@code dayConvention} property in the builder.
     * @param dayConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayConvention(BusinessDayConvention dayConvention) {
      JodaBeanUtils.notNull(dayConvention, "dayConvention");
      this.dayConvention = dayConvention;
      return this;
    }

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
     * Sets the {@code rollConvention} property in the builder.
     * @param rollConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rollConvention(RollConvention rollConvention) {
      JodaBeanUtils.notNull(rollConvention, "rollConvention");
      this.rollConvention = rollConvention;
      return this;
    }

    /**
     * Sets the {@code payAccOnDefault} property in the builder.
     * @param payAccOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccOnDefault(boolean payAccOnDefault) {
      JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
      this.payAccOnDefault = payAccOnDefault;
      return this;
    }

    /**
     * Sets the {@code calendar} property in the builder.
     * @param calendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calendar(HolidayCalendar calendar) {
      JodaBeanUtils.notNull(calendar, "calendar");
      this.calendar = calendar;
      return this;
    }

    /**
     * Sets the {@code stubConvention} property in the builder.
     * @param stubConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      this.stubConvention = stubConvention;
      return this;
    }

    /**
     * Sets the {@code stepIn} property in the builder.
     * @param stepIn  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder stepIn(int stepIn) {
      JodaBeanUtils.notNull(stepIn, "stepIn");
      this.stepIn = stepIn;
      return this;
    }

    /**
     * Sets the {@code settleLag} property in the builder.
     * @param settleLag  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settleLag(int settleLag) {
      JodaBeanUtils.notNull(settleLag, "settleLag");
      this.settleLag = settleLag;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("StandardSingleNameCdsConvention.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("dayConvention").append('=').append(JodaBeanUtils.toString(dayConvention)).append(',').append(' ');
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(payAccOnDefault)).append(',').append(' ');
      buf.append("calendar").append('=').append(JodaBeanUtils.toString(calendar)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("stepIn").append('=').append(JodaBeanUtils.toString(stepIn)).append(',').append(' ');
      buf.append("settleLag").append('=').append(JodaBeanUtils.toString(settleLag));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
