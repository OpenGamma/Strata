/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A term deposit.
 * <p>
 * A term deposit is a financial instrument that provides a fixed rate of interest on
 * an amount for a specific term.
 * For example, investing GBP 1,000 for 3 months at a 1% interest rate.
 * <p>
 * The instrument has two payments, one at the start date and one at the end date.
 * For example, investing  GBP 1,000 for 3 months implies an initial payment to the counterparty
 * of GBP 1,000 and a final payment from the counterparty of GBP 1,000 plus interest.
 * The sign of the principal will be positive in this case, as the final payment is being received.
 */
@BeanDefinition
public class TermDeposit
    implements TermDepositProduct, ImmutableBean, Serializable {

  /**
   * Whether the term deposit is 'Buy' or 'Sell'.
   * <p>
   * A value of 'Buy' implies that one pays the principal at the start date then receives the principal plus 
   * fixed interest at the end date. A value of 'Sell' implies that the principal is paid to the counterparty 
   * at the start date then the principal plus fixed interest is received from the counterparty at the end date.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;
  /**
   * The start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * This date must be after the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The principal amount, positive if receiving the interest, negative if paying the interest.
   * <p>
   * The term deposit has two payments, one at the start date and one at the end date.
   * The principal is signed based on the payment at the end date, when the interest is generated.
   * A positive amount indicates the end date payment is to be received.
   * A negative amount indicates the end date payment is to be paid.
   * <p>
   * Note that a negative interest rate can mean that the end date payment is less than
   * the start date payment.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount principal;
  /**
   * The fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   */
  @PropertyDefinition
  private final double rate;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency of the deposit.
   * 
   * @return the currency of the deposit
   */
  public Currency getCurrency() {
    return principal.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this term deposit, trivially returning {@code this}.
   * 
   * @return this
   */
  @Override
  public ExpandedTermDeposit expand() {
    LocalDate start = getBusinessDayAdjustment().orElse(BusinessDayAdjustment.NONE).adjust(startDate);
    LocalDate end = getBusinessDayAdjustment().orElse(BusinessDayAdjustment.NONE).adjust(endDate);
    double yearFraction = dayCount.yearFraction(start, end);
    return ExpandedTermDeposit.builder()
        .startDate(start)
        .endDate(end)
        .yearFraction(yearFraction)
        .currency(principal.getCurrency())
        .principal(buySell.normalize(principal.getAmount()))
        .rate(rate)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TermDeposit}.
   * @return the meta-bean, not null
   */
  public static TermDeposit.Meta meta() {
    return TermDeposit.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(TermDeposit.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static TermDeposit.Builder builder() {
    return new TermDeposit.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected TermDeposit(TermDeposit.Builder builder) {
    JodaBeanUtils.notNull(builder.buySell, "buySell");
    JodaBeanUtils.notNull(builder.startDate, "startDate");
    JodaBeanUtils.notNull(builder.endDate, "endDate");
    JodaBeanUtils.notNull(builder.dayCount, "dayCount");
    JodaBeanUtils.notNull(builder.principal, "principal");
    this.buySell = builder.buySell;
    this.startDate = builder.startDate;
    this.endDate = builder.endDate;
    this.businessDayAdjustment = builder.businessDayAdjustment;
    this.dayCount = builder.dayCount;
    this.principal = builder.principal;
    this.rate = builder.rate;
    validate();
  }

  @Override
  public TermDeposit.Meta metaBean() {
    return TermDeposit.Meta.INSTANCE;
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
   * Gets whether the term deposit is 'Buy' or 'Sell'.
   * <p>
   * A value of 'Buy' implies that one pays the principal at the start date then receives the principal plus
   * fixed interest at the end date. A value of 'Sell' implies that the principal is paid to the counterparty
   * at the start date then the principal plus fixed interest is received from the counterparty at the end date.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * This date must be after the start date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getBusinessDayAdjustment() {
    return Optional.ofNullable(businessDayAdjustment);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the principal amount, positive if receiving the interest, negative if paying the interest.
   * <p>
   * The term deposit has two payments, one at the start date and one at the end date.
   * The principal is signed based on the payment at the end date, when the interest is generated.
   * A positive amount indicates the end date payment is to be received.
   * A negative amount indicates the end date payment is to be paid.
   * <p>
   * Note that a negative interest rate can mean that the end date payment is less than
   * the start date payment.
   * @return the value of the property, not null
   */
  public CurrencyAmount getPrincipal() {
    return principal;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   * @return the value of the property
   */
  public double getRate() {
    return rate;
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
      TermDeposit other = (TermDeposit) obj;
      return JodaBeanUtils.equal(getBuySell(), other.getBuySell()) &&
          JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getPrincipal(), other.getPrincipal()) &&
          JodaBeanUtils.equal(getRate(), other.getRate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getBuySell());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPrincipal());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("TermDeposit{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("buySell").append('=').append(JodaBeanUtils.toString(getBuySell())).append(',').append(' ');
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(getStartDate())).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(getEndDate())).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount())).append(',').append(' ');
    buf.append("principal").append('=').append(JodaBeanUtils.toString(getPrincipal())).append(',').append(' ');
    buf.append("rate").append('=').append(JodaBeanUtils.toString(getRate())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TermDeposit}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code buySell} property.
     */
    private final MetaProperty<BuySell> buySell = DirectMetaProperty.ofImmutable(
        this, "buySell", TermDeposit.class, BuySell.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", TermDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", TermDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", TermDeposit.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", TermDeposit.class, DayCount.class);
    /**
     * The meta-property for the {@code principal} property.
     */
    private final MetaProperty<CurrencyAmount> principal = DirectMetaProperty.ofImmutable(
        this, "principal", TermDeposit.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code rate} property.
     */
    private final MetaProperty<Double> rate = DirectMetaProperty.ofImmutable(
        this, "rate", TermDeposit.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySell",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "dayCount",
        "principal",
        "rate");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case -1812041682:  // principal
          return principal;
        case 3493088:  // rate
          return rate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public TermDeposit.Builder builder() {
      return new TermDeposit.Builder();
    }

    @Override
    public Class<? extends TermDeposit> beanType() {
      return TermDeposit.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code buySell} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BuySell> buySell() {
      return buySell;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code principal} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CurrencyAmount> principal() {
      return principal;
    }

    /**
     * The meta-property for the {@code rate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> rate() {
      return rate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return ((TermDeposit) bean).getBuySell();
        case -2129778896:  // startDate
          return ((TermDeposit) bean).getStartDate();
        case -1607727319:  // endDate
          return ((TermDeposit) bean).getEndDate();
        case -1065319863:  // businessDayAdjustment
          return ((TermDeposit) bean).businessDayAdjustment;
        case 1905311443:  // dayCount
          return ((TermDeposit) bean).getDayCount();
        case -1812041682:  // principal
          return ((TermDeposit) bean).getPrincipal();
        case 3493088:  // rate
          return ((TermDeposit) bean).getRate();
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
   * The bean-builder for {@code TermDeposit}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<TermDeposit> {

    private BuySell buySell;
    private LocalDate startDate;
    private LocalDate endDate;
    private BusinessDayAdjustment businessDayAdjustment;
    private DayCount dayCount;
    private CurrencyAmount principal;
    private double rate;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(TermDeposit beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.dayCount = beanToCopy.getDayCount();
      this.principal = beanToCopy.getPrincipal();
      this.rate = beanToCopy.getRate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case -1812041682:  // principal
          return principal;
        case 3493088:  // rate
          return rate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          this.buySell = (BuySell) newValue;
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
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1812041682:  // principal
          this.principal = (CurrencyAmount) newValue;
          break;
        case 3493088:  // rate
          this.rate = (Double) newValue;
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
    public TermDeposit build() {
      return new TermDeposit(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code buySell} property in the builder.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

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
     * Sets the {@code businessDayAdjustment} property in the builder.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
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
     * Sets the {@code principal} property in the builder.
     * @param principal  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder principal(CurrencyAmount principal) {
      JodaBeanUtils.notNull(principal, "principal");
      this.principal = principal;
      return this;
    }

    /**
     * Sets the {@code rate} property in the builder.
     * @param rate  the new value
     * @return this, for chaining, not null
     */
    public Builder rate(double rate) {
      this.rate = rate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("TermDeposit.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("principal").append('=').append(JodaBeanUtils.toString(principal)).append(',').append(' ');
      buf.append("rate").append('=').append(JodaBeanUtils.toString(rate)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
