/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

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

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.date.HolidayCalendar;
import com.opengamma.basics.date.HolidayCalendars;
import com.opengamma.basics.date.Tenor;
import com.opengamma.basics.date.TenorAdjustment;
import com.opengamma.collect.ArgChecker;

/**
 * An IBOR-like index, such as Libor or Euribor.
 * <p>
 * An index represented by this class relates to inter-bank lending for periods
 * from one day to one year. They are typically calculated and published as the
 * trimmed arithmetic mean of estimated rates contributed by banks.
 * <p>
 * The index is defined by four dates.
 * The fixing date is the date on which the index is to be observed.
 * The publication date is the date on which the fixed rate is actually published.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 */
@BeanDefinition(cacheHashCode = true)
public final class IborIndex
    implements RateIndex, ImmutableBean, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1;

  /**
   * The currency of the index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The index name, such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String name;
  /**
   * The calendar that the fixing date follows.
   * <p>
   * The fixing date is when the rate is determined.
   */
  @PropertyDefinition(validate = "notNull")
  private final HolidayCalendar fixingCalendar;
  /**
   * The adjustment applied to the fixing date to obtain the effective date.
   * <p>
   * The effective date is the start date of the indexed deposit.
   * In most cases, the effective date is 0 or 2 days after the fixing date.
   * This data structure allows the complex rules of some indices to be represented.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment effectiveDateOffset;
  /**
   * The adjustment applied to the effective date to obtain the maturity date.
   * <p>
   * The maturity date is the end date of the indexed deposit and is relative to the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   */
  @PropertyDefinition(validate = "notNull")
  private final TenorAdjustment maturityDateOffset;
  /**
   * The day count convention.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code IborIndex} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  public static IborIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return RateIndices.ENUM_LOOKUP.lookup(uniqueName, IborIndex.class);
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

  //-------------------------------------------------------------------------
  /**
   * Gets the type of the index, which is tenor.
   * 
   * @return the index type
   */
  @Override
  public RateIndexType getType() {
    return RateIndexType.TENOR;
  }

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
   * Calculates the publication date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   * <p>
   * An IBOR-like index is always published on the same day as the fixing date.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the publication date
   */
  @Override
  public LocalDate calculatePublicationFromFixing(LocalDate fixingDate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    return fixingCalendar.nextOrSame(fixingDate);
  }

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
  public LocalDate calculateFixingFromEffective(LocalDate effectiveDate)  {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    LocalDate effectiveBusinessDay = effectiveDateCalendar().nextOrSame(effectiveDate);
    LocalDate fixingDate = effectiveBusinessDay;
    while (effectiveDateOffset.adjust(fixingDate).isAfter(effectiveBusinessDay) || fixingCalendar.isHoliday(fixingDate)) {
      fixingDate = fixingDate.minusDays(1);
    }
    return fixingDate;
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
    HolidayCalendar cal = effectiveDateOffset.getAdjustment().getCalendar();
    if (cal == HolidayCalendars.NO_HOLIDAYS) {
      cal = effectiveDateOffset.getCalendar();
      if (cal == HolidayCalendars.NO_HOLIDAYS) {
        cal = fixingCalendar;
      }
    }
    return cal;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborIndex}.
   * @return the meta-bean, not null
   */
  public static IborIndex.Meta meta() {
    return IborIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborIndex.Meta.INSTANCE);
  }

  /**
   * The cached hash code, using the racy single-check idiom.
   */
  private int cachedHashCode;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborIndex.Builder builder() {
    return new IborIndex.Builder();
  }

  private IborIndex(
      Currency currency,
      String name,
      HolidayCalendar fixingCalendar,
      DaysAdjustment effectiveDateOffset,
      TenorAdjustment maturityDateOffset,
      DayCount dayCount) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notEmpty(name, "name");
    JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
    JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
    JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.currency = currency;
    this.name = name;
    this.fixingCalendar = fixingCalendar;
    this.effectiveDateOffset = effectiveDateOffset;
    this.maturityDateOffset = maturityDateOffset;
    this.dayCount = dayCount;
  }

  @Override
  public IborIndex.Meta metaBean() {
    return IborIndex.Meta.INSTANCE;
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
   * Gets the currency of the index.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
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
   * Gets the calendar that the fixing date follows.
   * <p>
   * The fixing date is when the rate is determined.
   * @return the value of the property, not null
   */
  public HolidayCalendar getFixingCalendar() {
    return fixingCalendar;
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

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborIndex other = (IborIndex) obj;
      return JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getFixingCalendar(), other.getFixingCalendar()) &&
          JodaBeanUtils.equal(getEffectiveDateOffset(), other.getEffectiveDateOffset()) &&
          JodaBeanUtils.equal(getMaturityDateOffset(), other.getMaturityDateOffset()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = cachedHashCode;
    if (hash == 0) {
      hash = getClass().hashCode();
      hash += hash * 31 + JodaBeanUtils.hashCode(getCurrency());
      hash += hash * 31 + JodaBeanUtils.hashCode(getName());
      hash += hash * 31 + JodaBeanUtils.hashCode(getFixingCalendar());
      hash += hash * 31 + JodaBeanUtils.hashCode(getEffectiveDateOffset());
      hash += hash * 31 + JodaBeanUtils.hashCode(getMaturityDateOffset());
      hash += hash * 31 + JodaBeanUtils.hashCode(getDayCount());
      cachedHashCode = hash;
    }
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborIndex}.
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
        this, "currency", IborIndex.class, Currency.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", IborIndex.class, String.class);
    /**
     * The meta-property for the {@code fixingCalendar} property.
     */
    private final MetaProperty<HolidayCalendar> fixingCalendar = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", IborIndex.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> effectiveDateOffset = DirectMetaProperty.ofImmutable(
        this, "effectiveDateOffset", IborIndex.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code maturityDateOffset} property.
     */
    private final MetaProperty<TenorAdjustment> maturityDateOffset = DirectMetaProperty.ofImmutable(
        this, "maturityDateOffset", IborIndex.class, TenorAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", IborIndex.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "name",
        "fixingCalendar",
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
        case 575402001:  // currency
          return currency;
        case 3373707:  // name
          return name;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
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
    public IborIndex.Builder builder() {
      return new IborIndex.Builder();
    }

    @Override
    public Class<? extends IborIndex> beanType() {
      return IborIndex.class;
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
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code fixingCalendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> fixingCalendar() {
      return fixingCalendar;
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
        case 575402001:  // currency
          return ((IborIndex) bean).getCurrency();
        case 3373707:  // name
          return ((IborIndex) bean).getName();
        case 394230283:  // fixingCalendar
          return ((IborIndex) bean).getFixingCalendar();
        case 1571923688:  // effectiveDateOffset
          return ((IborIndex) bean).getEffectiveDateOffset();
        case 1574797394:  // maturityDateOffset
          return ((IborIndex) bean).getMaturityDateOffset();
        case 1905311443:  // dayCount
          return ((IborIndex) bean).getDayCount();
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
   * The bean-builder for {@code IborIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborIndex> {

    private Currency currency;
    private String name;
    private HolidayCalendar fixingCalendar;
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
    private Builder(IborIndex beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.name = beanToCopy.getName();
      this.fixingCalendar = beanToCopy.getFixingCalendar();
      this.effectiveDateOffset = beanToCopy.getEffectiveDateOffset();
      this.maturityDateOffset = beanToCopy.getMaturityDateOffset();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 3373707:  // name
          return name;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
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
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 394230283:  // fixingCalendar
          this.fixingCalendar = (HolidayCalendar) newValue;
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
    public IborIndex build() {
      return new IborIndex(
          currency,
          name,
          fixingCalendar,
          effectiveDateOffset,
          maturityDateOffset,
          dayCount);
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
     * Sets the {@code name} property in the builder.
     * @param name  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notEmpty(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the {@code fixingCalendar} property in the builder.
     * @param fixingCalendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingCalendar(HolidayCalendar fixingCalendar) {
      JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
      this.fixingCalendar = fixingCalendar;
      return this;
    }

    /**
     * Sets the {@code effectiveDateOffset} property in the builder.
     * @param effectiveDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveDateOffset(DaysAdjustment effectiveDateOffset) {
      JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
      this.effectiveDateOffset = effectiveDateOffset;
      return this;
    }

    /**
     * Sets the {@code maturityDateOffset} property in the builder.
     * @param maturityDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder maturityDateOffset(TenorAdjustment maturityDateOffset) {
      JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset");
      this.maturityDateOffset = maturityDateOffset;
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

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("IborIndex.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(fixingCalendar)).append(',').append(' ');
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
