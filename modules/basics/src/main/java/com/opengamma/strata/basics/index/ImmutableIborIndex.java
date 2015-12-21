/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An Ibor index implementation based on an immutable set of rules.
 * <p>
 * A standard immutable implementation of {@link IborIndex} that defines the currency
 * and the rules for converting from fixing to effective and maturity.
 * <p>
 * In most cases, applications should refer to indices by name, using {@link IborIndex#of(String)}.
 * The named index will typically be resolved to an instance of this class.
 * As such, it is recommended to use the {@code IborIndex} interface in application
 * code rather than directly referring to this class.
 */
@BeanDefinition
public final class ImmutableIborIndex
    implements IborIndex, ImmutableBean, Serializable {

  /**
   * The index name, such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String name;
  /**
   * The currency of the index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The calendar that determines which dates are fixing dates.
   * <p>
   * The fixing date is when the rate is determined.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final HolidayCalendar fixingCalendar;
  /**
   * The adjustment applied to the effective date to obtain the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * In most cases, the fixing date is 0 or 2 days before the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment fixingDateOffset;
  /**
   * The adjustment applied to the fixing date to obtain the effective date.
   * <p>
   * The effective date is the start date of the indexed deposit.
   * In most cases, the effective date is 0 or 2 days after the fixing date.
   * This data structure allows the complex rules of some indices to be represented.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment effectiveDateOffset;
  /**
   * The adjustment applied to the effective date to obtain the maturity date.
   * <p>
   * The maturity date is the end date of the indexed deposit and is relative to the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TenorAdjustment maturityDateOffset;
  /**
   * The day count convention.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  /**
   * Gets the tenor of the index.
   * 
   * @return the tenor
   */
  @Override
  public Tenor getTenor() {
    return maturityDateOffset.getTenor();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  @Override
  public LocalDate calculateEffectiveFromFixing(LocalDate fixingDate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    LocalDate fixingBusinessDay = fixingCalendar.nextOrSame(fixingDate);
    return effectiveDateOffset.adjust(fixingBusinessDay);
  }

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  @Override
  public LocalDate calculateFixingFromEffective(LocalDate effectiveDate) {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    LocalDate effectiveBusinessDay = effectiveDateCalendar().nextOrSame(effectiveDate);
    return fixingDateOffset.adjust(effectiveBusinessDay);
  }

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  @Override
  public LocalDate calculateMaturityFromEffective(LocalDate effectiveDate) {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    LocalDate effectiveBusinessDay = effectiveDateCalendar().nextOrSame(effectiveDate);
    return maturityDateOffset.adjust(effectiveBusinessDay);
  }

  // finds the calendar of the effective date
  private HolidayCalendar effectiveDateCalendar() {
    HolidayCalendar cal = effectiveDateOffset.getEffectiveResultCalendar();
    if (cal == HolidayCalendars.NO_HOLIDAYS) {
      cal = fixingCalendar;
    }
    return cal;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ImmutableIborIndex) {
      return name.equals(((ImmutableIborIndex) obj).name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the index.
   * 
   * @return the name of the index
   */
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableIborIndex}.
   * @return the meta-bean, not null
   */
  public static ImmutableIborIndex.Meta meta() {
    return ImmutableIborIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableIborIndex.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableIborIndex.Builder builder() {
    return new ImmutableIborIndex.Builder();
  }

  private ImmutableIborIndex(
      String name,
      Currency currency,
      HolidayCalendar fixingCalendar,
      DaysAdjustment fixingDateOffset,
      DaysAdjustment effectiveDateOffset,
      TenorAdjustment maturityDateOffset,
      DayCount dayCount) {
    JodaBeanUtils.notEmpty(name, "name");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
    JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
    JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
    JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.name = name;
    this.currency = currency;
    this.fixingCalendar = fixingCalendar;
    this.fixingDateOffset = fixingDateOffset;
    this.effectiveDateOffset = effectiveDateOffset;
    this.maturityDateOffset = maturityDateOffset;
    this.dayCount = dayCount;
  }

  @Override
  public ImmutableIborIndex.Meta metaBean() {
    return ImmutableIborIndex.Meta.INSTANCE;
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
   * Gets the index name, such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not empty
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the index.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calendar that determines which dates are fixing dates.
   * <p>
   * The fixing date is when the rate is determined.
   * @return the value of the property, not null
   */
  @Override
  public HolidayCalendar getFixingCalendar() {
    return fixingCalendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the effective date to obtain the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * In most cases, the fixing date is 0 or 2 days before the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * @return the value of the property, not null
   */
  @Override
  public DaysAdjustment getFixingDateOffset() {
    return fixingDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the fixing date to obtain the effective date.
   * <p>
   * The effective date is the start date of the indexed deposit.
   * In most cases, the effective date is 0 or 2 days after the fixing date.
   * This data structure allows the complex rules of some indices to be represented.
   * @return the value of the property, not null
   */
  @Override
  public DaysAdjustment getEffectiveDateOffset() {
    return effectiveDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the effective date to obtain the maturity date.
   * <p>
   * The maturity date is the end date of the indexed deposit and is relative to the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * @return the value of the property, not null
   */
  @Override
  public TenorAdjustment getMaturityDateOffset() {
    return maturityDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableIborIndex}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableIborIndex.class, String.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ImmutableIborIndex.class, Currency.class);
    /**
     * The meta-property for the {@code fixingCalendar} property.
     */
    private final MetaProperty<HolidayCalendar> fixingCalendar = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", ImmutableIborIndex.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", ImmutableIborIndex.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> effectiveDateOffset = DirectMetaProperty.ofImmutable(
        this, "effectiveDateOffset", ImmutableIborIndex.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code maturityDateOffset} property.
     */
    private final MetaProperty<TenorAdjustment> maturityDateOffset = DirectMetaProperty.ofImmutable(
        this, "maturityDateOffset", ImmutableIborIndex.class, TenorAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutableIborIndex.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currency",
        "fixingCalendar",
        "fixingDateOffset",
        "effectiveDateOffset",
        "maturityDateOffset",
        "dayCount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 575402001:  // currency
          return currency;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1571923688:  // effectiveDateOffset
          return effectiveDateOffset;
        case 1574797394:  // maturityDateOffset
          return maturityDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableIborIndex.Builder builder() {
      return new ImmutableIborIndex.Builder();
    }

    @Override
    public Class<? extends ImmutableIborIndex> beanType() {
      return ImmutableIborIndex.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code fixingCalendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> fixingCalendar() {
      return fixingCalendar;
    }

    /**
     * The meta-property for the {@code fixingDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingDateOffset() {
      return fixingDateOffset;
    }

    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> effectiveDateOffset() {
      return effectiveDateOffset;
    }

    /**
     * The meta-property for the {@code maturityDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TenorAdjustment> maturityDateOffset() {
      return maturityDateOffset;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((ImmutableIborIndex) bean).getName();
        case 575402001:  // currency
          return ((ImmutableIborIndex) bean).getCurrency();
        case 394230283:  // fixingCalendar
          return ((ImmutableIborIndex) bean).getFixingCalendar();
        case 873743726:  // fixingDateOffset
          return ((ImmutableIborIndex) bean).getFixingDateOffset();
        case 1571923688:  // effectiveDateOffset
          return ((ImmutableIborIndex) bean).getEffectiveDateOffset();
        case 1574797394:  // maturityDateOffset
          return ((ImmutableIborIndex) bean).getMaturityDateOffset();
        case 1905311443:  // dayCount
          return ((ImmutableIborIndex) bean).getDayCount();
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
   * The bean-builder for {@code ImmutableIborIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableIborIndex> {

    private String name;
    private Currency currency;
    private HolidayCalendar fixingCalendar;
    private DaysAdjustment fixingDateOffset;
    private DaysAdjustment effectiveDateOffset;
    private TenorAdjustment maturityDateOffset;
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableIborIndex beanToCopy) {
      this.name = beanToCopy.getName();
      this.currency = beanToCopy.getCurrency();
      this.fixingCalendar = beanToCopy.getFixingCalendar();
      this.fixingDateOffset = beanToCopy.getFixingDateOffset();
      this.effectiveDateOffset = beanToCopy.getEffectiveDateOffset();
      this.maturityDateOffset = beanToCopy.getMaturityDateOffset();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 575402001:  // currency
          return currency;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1571923688:  // effectiveDateOffset
          return effectiveDateOffset;
        case 1574797394:  // maturityDateOffset
          return maturityDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 394230283:  // fixingCalendar
          this.fixingCalendar = (HolidayCalendar) newValue;
          break;
        case 873743726:  // fixingDateOffset
          this.fixingDateOffset = (DaysAdjustment) newValue;
          break;
        case 1571923688:  // effectiveDateOffset
          this.effectiveDateOffset = (DaysAdjustment) newValue;
          break;
        case 1574797394:  // maturityDateOffset
          this.maturityDateOffset = (TenorAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
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
    public ImmutableIborIndex build() {
      return new ImmutableIborIndex(
          name,
          currency,
          fixingCalendar,
          fixingDateOffset,
          effectiveDateOffset,
          maturityDateOffset,
          dayCount);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index name, such as 'GBP-LIBOR-3M'.
     * @param name  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notEmpty(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the currency of the index.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the calendar that determines which dates are fixing dates.
     * <p>
     * The fixing date is when the rate is determined.
     * @param fixingCalendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingCalendar(HolidayCalendar fixingCalendar) {
      JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
      this.fixingCalendar = fixingCalendar;
      return this;
    }

    /**
     * Sets the adjustment applied to the effective date to obtain the fixing date.
     * <p>
     * The fixing date is the date on which the index is to be observed.
     * In most cases, the fixing date is 0 or 2 days before the effective date.
     * This data structure allows the complex rules of some indices to be represented.
     * @param fixingDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDateOffset(DaysAdjustment fixingDateOffset) {
      JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
      this.fixingDateOffset = fixingDateOffset;
      return this;
    }

    /**
     * Sets the adjustment applied to the fixing date to obtain the effective date.
     * <p>
     * The effective date is the start date of the indexed deposit.
     * In most cases, the effective date is 0 or 2 days after the fixing date.
     * This data structure allows the complex rules of some indices to be represented.
     * @param effectiveDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveDateOffset(DaysAdjustment effectiveDateOffset) {
      JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
      this.effectiveDateOffset = effectiveDateOffset;
      return this;
    }

    /**
     * Sets the adjustment applied to the effective date to obtain the maturity date.
     * <p>
     * The maturity date is the end date of the indexed deposit and is relative to the effective date.
     * This data structure allows the complex rules of some indices to be represented.
     * @param maturityDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder maturityDateOffset(TenorAdjustment maturityDateOffset) {
      JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset");
      this.maturityDateOffset = maturityDateOffset;
      return this;
    }

    /**
     * Sets the day count convention.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("ImmutableIborIndex.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(fixingCalendar)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
      buf.append("effectiveDateOffset").append('=').append(JodaBeanUtils.toString(effectiveDateOffset)).append(',').append(' ');
      buf.append("maturityDateOffset").append('=').append(JodaBeanUtils.toString(maturityDateOffset)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
