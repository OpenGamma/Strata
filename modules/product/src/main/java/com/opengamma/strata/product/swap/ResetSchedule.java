/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Defines the schedule of fixing dates relative to the accrual periods.
 * <p>
 * This defines the data necessary to create a schedule of reset periods.
 * Most accrual periods only contain a single reset period.
 * This schedule is used when there is more than one reset period in each accrual
 * period, or where the rules around the reset period are unusual.
 * <p>
 * The rate will be observed once for each reset period.
 * If an accrual period contains more than one reset period then an averaging
 * method will be used to combine the floating rates.
 * <p>
 * This class defines reset periods using a periodic frequency.
 * The frequency must match or be smaller than the accrual periodic frequency.
 * The reset schedule is calculated forwards, potentially with a short stub at the end.
 */
@BeanDefinition
public final class ResetSchedule
    implements ImmutableBean, Serializable {

  /**
   * The periodic frequency of reset dates.
   * <p>
   * Reset dates will be calculated within each accrual period based on unadjusted dates.
   * The frequency must be the same as, or smaller than, the accrual periodic frequency.
   * When calculating the reset dates, the roll convention of the accrual periods will be used.
   * Once the unadjusted date calculation is complete, the business day adjustment specified
   * here will be used.
   * <p>
   * Averaging applies if the reset frequency does not equal the accrual frequency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency resetFrequency;
  /**
   * The business day adjustment to apply to each reset date.
   * <p>
   * This adjustment is applied to each reset date to ensure it is a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The rate reset method, defaulted to 'Unweighted'.
   * <p>
   * This is used when more than one fixing contributes to the accrual period.
   * <p>
   * Averaging may be weighted by the number of days that the fixing is applicable for.
   * The number of days is based on the reset period, not the period between two fixing dates.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2a.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborRateResetMethod resetMethod;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.resetMethod(IborRateResetMethod.UNWEIGHTED);
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves this schedule using the specified reference data.
   * <p>
   * Calling this method binds the reference data and roll convention, returning a
   * function that can convert a {@code SchedulePeriod} to an {@code FxReset}.
   * <p>
   * The reset schedule is created within the bounds of the specified accrual period.
   * The reset frequency is added repeatedly to the unadjusted start date of the period
   * in order to generate the schedule, potentially leaving a short final stub.
   * The dates are adjusted using the specified roll convention and the business
   * day adjustment of this class.
   * 
   * @param rollConvention  the applicable roll convention
   * @param refData  the reference data to use when resolving
   * @return the reset schedule
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if the schedule is invalid
   */
  Function<SchedulePeriod, Schedule> createSchedule(RollConvention rollConvention, ReferenceData refData) {
    return accrualPeriod -> accrualPeriod.subSchedule(
        resetFrequency, rollConvention, StubConvention.SHORT_FINAL, businessDayAdjustment).createSchedule(refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResetSchedule}.
   * @return the meta-bean, not null
   */
  public static ResetSchedule.Meta meta() {
    return ResetSchedule.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResetSchedule.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResetSchedule.Builder builder() {
    return new ResetSchedule.Builder();
  }

  private ResetSchedule(
      Frequency resetFrequency,
      BusinessDayAdjustment businessDayAdjustment,
      IborRateResetMethod resetMethod) {
    JodaBeanUtils.notNull(resetFrequency, "resetFrequency");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(resetMethod, "resetMethod");
    this.resetFrequency = resetFrequency;
    this.businessDayAdjustment = businessDayAdjustment;
    this.resetMethod = resetMethod;
  }

  @Override
  public ResetSchedule.Meta metaBean() {
    return ResetSchedule.Meta.INSTANCE;
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
   * Gets the periodic frequency of reset dates.
   * <p>
   * Reset dates will be calculated within each accrual period based on unadjusted dates.
   * The frequency must be the same as, or smaller than, the accrual periodic frequency.
   * When calculating the reset dates, the roll convention of the accrual periods will be used.
   * Once the unadjusted date calculation is complete, the business day adjustment specified
   * here will be used.
   * <p>
   * Averaging applies if the reset frequency does not equal the accrual frequency.
   * @return the value of the property, not null
   */
  public Frequency getResetFrequency() {
    return resetFrequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to each reset date.
   * <p>
   * This adjustment is applied to each reset date to ensure it is a valid business day.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate reset method, defaulted to 'Unweighted'.
   * <p>
   * This is used when more than one fixing contributes to the accrual period.
   * <p>
   * Averaging may be weighted by the number of days that the fixing is applicable for.
   * The number of days is based on the reset period, not the period between two fixing dates.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2a.
   * @return the value of the property, not null
   */
  public IborRateResetMethod getResetMethod() {
    return resetMethod;
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
      ResetSchedule other = (ResetSchedule) obj;
      return JodaBeanUtils.equal(resetFrequency, other.resetFrequency) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(resetMethod, other.resetMethod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(resetFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(resetMethod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ResetSchedule{");
    buf.append("resetFrequency").append('=').append(resetFrequency).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("resetMethod").append('=').append(JodaBeanUtils.toString(resetMethod));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResetSchedule}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code resetFrequency} property.
     */
    private final MetaProperty<Frequency> resetFrequency = DirectMetaProperty.ofImmutable(
        this, "resetFrequency", ResetSchedule.class, Frequency.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", ResetSchedule.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code resetMethod} property.
     */
    private final MetaProperty<IborRateResetMethod> resetMethod = DirectMetaProperty.ofImmutable(
        this, "resetMethod", ResetSchedule.class, IborRateResetMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "resetFrequency",
        "businessDayAdjustment",
        "resetMethod");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 101322957:  // resetFrequency
          return resetFrequency;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -958176496:  // resetMethod
          return resetMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResetSchedule.Builder builder() {
      return new ResetSchedule.Builder();
    }

    @Override
    public Class<? extends ResetSchedule> beanType() {
      return ResetSchedule.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code resetFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> resetFrequency() {
      return resetFrequency;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code resetMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateResetMethod> resetMethod() {
      return resetMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 101322957:  // resetFrequency
          return ((ResetSchedule) bean).getResetFrequency();
        case -1065319863:  // businessDayAdjustment
          return ((ResetSchedule) bean).getBusinessDayAdjustment();
        case -958176496:  // resetMethod
          return ((ResetSchedule) bean).getResetMethod();
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
   * The bean-builder for {@code ResetSchedule}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResetSchedule> {

    private Frequency resetFrequency;
    private BusinessDayAdjustment businessDayAdjustment;
    private IborRateResetMethod resetMethod;

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
    private Builder(ResetSchedule beanToCopy) {
      this.resetFrequency = beanToCopy.getResetFrequency();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.resetMethod = beanToCopy.getResetMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 101322957:  // resetFrequency
          return resetFrequency;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -958176496:  // resetMethod
          return resetMethod;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 101322957:  // resetFrequency
          this.resetFrequency = (Frequency) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -958176496:  // resetMethod
          this.resetMethod = (IborRateResetMethod) newValue;
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
    public ResetSchedule build() {
      return new ResetSchedule(
          resetFrequency,
          businessDayAdjustment,
          resetMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the periodic frequency of reset dates.
     * <p>
     * Reset dates will be calculated within each accrual period based on unadjusted dates.
     * The frequency must be the same as, or smaller than, the accrual periodic frequency.
     * When calculating the reset dates, the roll convention of the accrual periods will be used.
     * Once the unadjusted date calculation is complete, the business day adjustment specified
     * here will be used.
     * <p>
     * Averaging applies if the reset frequency does not equal the accrual frequency.
     * @param resetFrequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder resetFrequency(Frequency resetFrequency) {
      JodaBeanUtils.notNull(resetFrequency, "resetFrequency");
      this.resetFrequency = resetFrequency;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to each reset date.
     * <p>
     * This adjustment is applied to each reset date to ensure it is a valid business day.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the rate reset method, defaulted to 'Unweighted'.
     * <p>
     * This is used when more than one fixing contributes to the accrual period.
     * <p>
     * Averaging may be weighted by the number of days that the fixing is applicable for.
     * The number of days is based on the reset period, not the period between two fixing dates.
     * <p>
     * Defined by the 2006 ISDA definitions article 6.2a.
     * @param resetMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder resetMethod(IborRateResetMethod resetMethod) {
      JodaBeanUtils.notNull(resetMethod, "resetMethod");
      this.resetMethod = resetMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ResetSchedule.Builder{");
      buf.append("resetFrequency").append('=').append(JodaBeanUtils.toString(resetFrequency)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("resetMethod").append('=').append(JodaBeanUtils.toString(resetMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
