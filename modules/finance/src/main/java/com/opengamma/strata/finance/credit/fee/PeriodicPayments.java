/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.fee;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
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

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * http://www.fpml.org/spec/fpml-5-7-2-wd-2/html/pretrade/schemaDocumentation/schemas/fpml-cd-5-7_xsd/complexTypes/FeeLeg/periodicPayment.html
 * <p>
 * Specifies a periodic schedule of fixed amounts that are payable by the buyer to the seller on
 * the fixed rate payer payment dates. The fixed amount to be paid on each payment date can be
 * specified in terms of a known currency amount or as an amount calculated on a formula basis
 * by reference to a per annum fixed rate. The applicable business day convention and business
 * day for adjusting any fixed rate payer payment date if it would otherwise fall on a day that
 * is not a business day are those specified in the dateAdjustments element within the
 * generalTerms component.
 */
@BeanDefinition
public final class PeriodicPayments
    implements ImmutableBean, Serializable {

  /**
   * The time interval between regular fixed rate payer payment dates.
   */
  @PropertyDefinition(validate = "notNull")
  final Frequency paymentFrequency;

  /**
   * The start date of the initial calculation period if such date is not equal to the trade’s effective date.
   * It must only be specified if it is not equal to the effective date.
   * The applicable business day convention and business day are those specified in the dateAdjustments
   * element within the generalTerms component
   * (or in a transaction supplement FpML representation defined within
   * the referenced general terms confirmation agreement).
   */
//  @PropertyDefinition(validate = "notNull")
//  final LocalDate firstPeriodStartDate;

  /**
   * The first unadjusted fixed rate payer payment date. The applicable business day convention and business
   * day are those specified in the dateAdjustments element within the generalTerms component
   * (or in a transaction supplement FpML representation defined within the referenced general terms
   * confirmation agreement). ISDA 2003 Term: Fixed Rate Payer Payment Date
   */
//  @PropertyDefinition(validate = "notNull")
//  final LocalDate firstPaymentDate;

  /**
   * The last regular unadjusted fixed rate payer payment date. The applicable business day convention and
   * business day are those specified in the dateAdjustments element within the generalTerms component
   * (or in a transaction supplement FpML representation defined within the referenced general terms confirmation
   * agreement). This element should only be included if there is a final payment stub, i.e. where the last regular
   * unadjusted fixed rate payer payment date is not equal to the scheduled termination date.
   * ISDA 2003 Term: Fixed Rate Payer Payment Date
   */
//  @PropertyDefinition(validate = "notNull")
//  final LocalDate lastRegularPaymentDate;
  /**
   * Stub convention to use when the maturity date does not land precisely on an imm date
   * and a stab period is created
   */
  @PropertyDefinition(validate = "notNull")
  final StubConvention stubConvention;

  /**
   * Roll convention used to calculate payment schedule
   */
  @PropertyDefinition(validate = "notNull")
  final RollConvention rollConvention;

  /**
   * This element contains all the terms relevant to calculating a fixed amount where the fixed amount is
   * calculated by reference to a per annum fixed rate. There is no corresponding ISDA 2003 Term.
   * The equivalent is Sec 5.1 "Calculation of Fixed Amount" but this in itself is not a defined Term.
   */
  @PropertyDefinition(validate = "notNull")
  final FixedAmountCalculation fixedAmountCalculation;

  public static PeriodicPayments of(
      Frequency paymentFrequency,
      StubConvention stubConvention,
      RollConvention rollConvention,
      FixedAmountCalculation fixedAmountCalculation
  ) {
    return new PeriodicPayments(
        paymentFrequency,
        stubConvention,
        rollConvention,
        fixedAmountCalculation
    );
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PeriodicPayments}.
   * @return the meta-bean, not null
   */
  public static PeriodicPayments.Meta meta() {
    return PeriodicPayments.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PeriodicPayments.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PeriodicPayments.Builder builder() {
    return new PeriodicPayments.Builder();
  }

  private PeriodicPayments(
      Frequency paymentFrequency,
      StubConvention stubConvention,
      RollConvention rollConvention,
      FixedAmountCalculation fixedAmountCalculation) {
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    JodaBeanUtils.notNull(stubConvention, "stubConvention");
    JodaBeanUtils.notNull(rollConvention, "rollConvention");
    JodaBeanUtils.notNull(fixedAmountCalculation, "fixedAmountCalculation");
    this.paymentFrequency = paymentFrequency;
    this.stubConvention = stubConvention;
    this.rollConvention = rollConvention;
    this.fixedAmountCalculation = fixedAmountCalculation;
  }

  @Override
  public PeriodicPayments.Meta metaBean() {
    return PeriodicPayments.Meta.INSTANCE;
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
   * Gets the time interval between regular fixed rate payer payment dates.
   * @return the value of the property, not null
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets stub convention to use when the maturity date does not land precisely on an imm date
   * and a stab period is created
   * @return the value of the property, not null
   */
  public StubConvention getStubConvention() {
    return stubConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets roll convention used to calculate payment schedule
   * @return the value of the property, not null
   */
  public RollConvention getRollConvention() {
    return rollConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets this element contains all the terms relevant to calculating a fixed amount where the fixed amount is
   * calculated by reference to a per annum fixed rate. There is no corresponding ISDA 2003 Term.
   * The equivalent is Sec 5.1 "Calculation of Fixed Amount" but this in itself is not a defined Term.
   * @return the value of the property, not null
   */
  public FixedAmountCalculation getFixedAmountCalculation() {
    return fixedAmountCalculation;
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
      PeriodicPayments other = (PeriodicPayments) obj;
      return JodaBeanUtils.equal(getPaymentFrequency(), other.getPaymentFrequency()) &&
          JodaBeanUtils.equal(getStubConvention(), other.getStubConvention()) &&
          JodaBeanUtils.equal(getRollConvention(), other.getRollConvention()) &&
          JodaBeanUtils.equal(getFixedAmountCalculation(), other.getFixedAmountCalculation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentFrequency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStubConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRollConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixedAmountCalculation());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("PeriodicPayments{");
    buf.append("paymentFrequency").append('=').append(getPaymentFrequency()).append(',').append(' ');
    buf.append("stubConvention").append('=').append(getStubConvention()).append(',').append(' ');
    buf.append("rollConvention").append('=').append(getRollConvention()).append(',').append(' ');
    buf.append("fixedAmountCalculation").append('=').append(JodaBeanUtils.toString(getFixedAmountCalculation()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PeriodicPayments}.
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
        this, "paymentFrequency", PeriodicPayments.class, Frequency.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", PeriodicPayments.class, StubConvention.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", PeriodicPayments.class, RollConvention.class);
    /**
     * The meta-property for the {@code fixedAmountCalculation} property.
     */
    private final MetaProperty<FixedAmountCalculation> fixedAmountCalculation = DirectMetaProperty.ofImmutable(
        this, "fixedAmountCalculation", PeriodicPayments.class, FixedAmountCalculation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentFrequency",
        "stubConvention",
        "rollConvention",
        "fixedAmountCalculation");

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
        case -31408449:  // stubConvention
          return stubConvention;
        case -10223666:  // rollConvention
          return rollConvention;
        case 542293565:  // fixedAmountCalculation
          return fixedAmountCalculation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PeriodicPayments.Builder builder() {
      return new PeriodicPayments.Builder();
    }

    @Override
    public Class<? extends PeriodicPayments> beanType() {
      return PeriodicPayments.class;
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
     * The meta-property for the {@code stubConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StubConvention> stubConvention() {
      return stubConvention;
    }

    /**
     * The meta-property for the {@code rollConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RollConvention> rollConvention() {
      return rollConvention;
    }

    /**
     * The meta-property for the {@code fixedAmountCalculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedAmountCalculation> fixedAmountCalculation() {
      return fixedAmountCalculation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return ((PeriodicPayments) bean).getPaymentFrequency();
        case -31408449:  // stubConvention
          return ((PeriodicPayments) bean).getStubConvention();
        case -10223666:  // rollConvention
          return ((PeriodicPayments) bean).getRollConvention();
        case 542293565:  // fixedAmountCalculation
          return ((PeriodicPayments) bean).getFixedAmountCalculation();
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
   * The bean-builder for {@code PeriodicPayments}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PeriodicPayments> {

    private Frequency paymentFrequency;
    private StubConvention stubConvention;
    private RollConvention rollConvention;
    private FixedAmountCalculation fixedAmountCalculation;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PeriodicPayments beanToCopy) {
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
      this.stubConvention = beanToCopy.getStubConvention();
      this.rollConvention = beanToCopy.getRollConvention();
      this.fixedAmountCalculation = beanToCopy.getFixedAmountCalculation();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -31408449:  // stubConvention
          return stubConvention;
        case -10223666:  // rollConvention
          return rollConvention;
        case 542293565:  // fixedAmountCalculation
          return fixedAmountCalculation;
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
        case -31408449:  // stubConvention
          this.stubConvention = (StubConvention) newValue;
          break;
        case -10223666:  // rollConvention
          this.rollConvention = (RollConvention) newValue;
          break;
        case 542293565:  // fixedAmountCalculation
          this.fixedAmountCalculation = (FixedAmountCalculation) newValue;
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
    public PeriodicPayments build() {
      return new PeriodicPayments(
          paymentFrequency,
          stubConvention,
          rollConvention,
          fixedAmountCalculation);
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
     * Sets the {@code stubConvention} property in the builder.
     * @param stubConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      JodaBeanUtils.notNull(stubConvention, "stubConvention");
      this.stubConvention = stubConvention;
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
     * Sets the {@code fixedAmountCalculation} property in the builder.
     * @param fixedAmountCalculation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixedAmountCalculation(FixedAmountCalculation fixedAmountCalculation) {
      JodaBeanUtils.notNull(fixedAmountCalculation, "fixedAmountCalculation");
      this.fixedAmountCalculation = fixedAmountCalculation;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("PeriodicPayments.Builder{");
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("fixedAmountCalculation").append('=').append(JodaBeanUtils.toString(fixedAmountCalculation));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
