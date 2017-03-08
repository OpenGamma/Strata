/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.collect.ArgChecker;

/**
 * The variant of an exchange traded derivative (ETD).
 * <p>
 * Most ETDs are monthly, where there is one expiry date per month, but a few are issued weekly or daily.
 * <p>
 * A special category of ETD are <i>flex</i> futures and options.
 * These have additional contract flexibility, with a settlement type and option type.
 */
@BeanDefinition(builderScope = "private", metaScope = "private")
public final class EtdVariant
    implements ImmutableBean, Serializable {

  /**
   * The standard Monthly type.
   */
  public static final EtdVariant MONTHLY = new EtdVariant(EtdExpiryType.MONTHLY, null, null, null);

  /**
   * The type of ETD - Monthly, Weekly or Daily.
   * <p>
   * Flex Futures and Options are always Daily.
   */
  @PropertyDefinition(validate = "notNull")
  private final EtdExpiryType type;
  /**
   * The optional date code, populated for Weekly and Daily.
   * <p>
   * This will be the week number for Weekly and the day-of-week for Daily.
   */
  @PropertyDefinition(get = "optional")
  private final Integer dateCode;
  /**
   * The optional settlement type, such as 'Cash' or 'Physical', populated for Flex Futures and Flex Options.
   */
  @PropertyDefinition(get = "optional")
  private final EtdSettlementType settlementType;
  /**
   * The optional option type, 'American' or 'European', populated for Flex Options.
   */
  @PropertyDefinition(get = "optional")
  private final EtdOptionType optionType;
  /**
   * The short code.
   */
  private final String code;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * The standard monthly ETD.
   * 
   * @return the variant
   */
  public static EtdVariant ofMonthly() {
    return MONTHLY;
  }

  /**
   * The standard weekly ETD.
   * 
   * @param week  the week number
   * @return the variant
   */
  public static EtdVariant ofWeekly(int week) {
    return new EtdVariant(EtdExpiryType.WEEKLY, week, null, null);
  }

  /**
   * The standard daily ETD.
   * 
   * @param dayOfMonth  the day-of-month
   * @return the variant
   */
  public static EtdVariant ofDaily(int dayOfMonth) {
    return new EtdVariant(EtdExpiryType.DAILY, dayOfMonth, null, null);
  }

  /**
   * The standard monthly ETD.
   * 
   * @param dayOfMonth  the day-of-month
   * @param settlementType  the settlement type
   * @return the variant
   */
  public static EtdVariant ofFlexFuture(int dayOfMonth, EtdSettlementType settlementType) {
    return new EtdVariant(EtdExpiryType.DAILY, dayOfMonth, settlementType, null);
  }

  /**
   * The standard monthly ETD.
   * 
   * @param dayOfMonth  the day-of-month
   * @param settlementType  the settlement type
   * @param optionType  the option type
   * @return the variant
   */
  public static EtdVariant ofFlexOption(int dayOfMonth, EtdSettlementType settlementType, EtdOptionType optionType) {
    return new EtdVariant(EtdExpiryType.DAILY, dayOfMonth, settlementType, optionType);
  }

  @ImmutableConstructor
  private EtdVariant(
      EtdExpiryType type,
      Integer dateCode,
      EtdSettlementType settlementType,
      EtdOptionType optionType) {

    this.type = ArgChecker.notNull(type, "type");
    this.dateCode = dateCode;
    this.settlementType = settlementType;
    this.optionType = optionType;
    if (type == EtdExpiryType.MONTHLY) {
      ArgChecker.isTrue(dateCode == null, "Monthly variant must have no dateCode");
      ArgChecker.isTrue(settlementType == null, "Monthly variant must have no settlementType");
      ArgChecker.isTrue(optionType == null, "Monthly variant must have no optionType");
      this.code = "";
    } else if (type == EtdExpiryType.WEEKLY) {
      ArgChecker.notNull(dateCode, "dateCode");
      ArgChecker.isTrue(dateCode >= 1 && dateCode <= 5, "Week must be from 1 to 5");
      ArgChecker.isTrue(settlementType == null, "Weekly variant must have no settlementType");
      ArgChecker.isTrue(optionType == null, "Weekly variant must have no optionType");
      this.code = "W" + dateCode;
    } else {  // DAILY and Flex
      ArgChecker.notNull(dateCode, "dateCode");
      ArgChecker.isTrue(dateCode >= 1 && dateCode <= 31, "Day-of-week must be from 1 to 31");
      ArgChecker.isFalse(settlementType == null && optionType != null,
          "Flex Option must have both settlementType and optionType");
      String dateCodeStr = dateCode < 10 ? "0" + dateCode : Integer.toString(dateCode);
      String settlementCode = settlementType != null ? settlementType.getCode() : "";
      String optionCode = optionType != null ? optionType.getCode() : "";
      this.code = dateCodeStr + settlementCode + optionCode;
    }
  }

  // resolve after deserialization
  private Object readResolve() {
    return new EtdVariant(type, dateCode, settlementType, optionType);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the variant is a Flex Future or Flex Option.
   * 
   * @return true if this is a Flex Future or Flex Option
   */
  public boolean isFlex() {
    return settlementType != null;
  }

  /**
   * Gets the short code that describes the variant.
   * <p>
   * This is an empty string for Monthly, the week number prefixed by 'W' for Weekly,
   * the day number for daily, with a suffix of the settlement type and option type codes.
   * 
   * @return the short code
   */
  public String getCode() {
    return code;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code EtdVariant}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return EtdVariant.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(EtdVariant.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public MetaBean metaBean() {
    return EtdVariant.Meta.INSTANCE;
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
   * Gets the type of ETD - Monthly, Weekly or Daily.
   * <p>
   * Flex Futures and Options are always Daily.
   * @return the value of the property, not null
   */
  public EtdExpiryType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional date code, populated for Weekly and Daily.
   * <p>
   * This will be the week number for Weekly and the day-of-week for Daily.
   * @return the optional value of the property, not null
   */
  public OptionalInt getDateCode() {
    return dateCode != null ? OptionalInt.of(dateCode) : OptionalInt.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional settlement type, such as 'Cash' or 'Physical', populated for Flex Futures and Flex Options.
   * @return the optional value of the property, not null
   */
  public Optional<EtdSettlementType> getSettlementType() {
    return Optional.ofNullable(settlementType);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional option type, 'American' or 'European', populated for Flex Options.
   * @return the optional value of the property, not null
   */
  public Optional<EtdOptionType> getOptionType() {
    return Optional.ofNullable(optionType);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      EtdVariant other = (EtdVariant) obj;
      return JodaBeanUtils.equal(type, other.type) &&
          JodaBeanUtils.equal(dateCode, other.dateCode) &&
          JodaBeanUtils.equal(settlementType, other.settlementType) &&
          JodaBeanUtils.equal(optionType, other.optionType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(type);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateCode);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementType);
    hash = hash * 31 + JodaBeanUtils.hashCode(optionType);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("EtdVariant{");
    buf.append("type").append('=').append(type).append(',').append(' ');
    buf.append("dateCode").append('=').append(dateCode).append(',').append(' ');
    buf.append("settlementType").append('=').append(settlementType).append(',').append(' ');
    buf.append("optionType").append('=').append(JodaBeanUtils.toString(optionType));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code EtdVariant}.
   */
  private static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<EtdExpiryType> type = DirectMetaProperty.ofImmutable(
        this, "type", EtdVariant.class, EtdExpiryType.class);
    /**
     * The meta-property for the {@code dateCode} property.
     */
    private final MetaProperty<Integer> dateCode = DirectMetaProperty.ofImmutable(
        this, "dateCode", EtdVariant.class, Integer.class);
    /**
     * The meta-property for the {@code settlementType} property.
     */
    private final MetaProperty<EtdSettlementType> settlementType = DirectMetaProperty.ofImmutable(
        this, "settlementType", EtdVariant.class, EtdSettlementType.class);
    /**
     * The meta-property for the {@code optionType} property.
     */
    private final MetaProperty<EtdOptionType> optionType = DirectMetaProperty.ofImmutable(
        this, "optionType", EtdVariant.class, EtdOptionType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "type",
        "dateCode",
        "settlementType",
        "optionType");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case 1792248507:  // dateCode
          return dateCode;
        case -295448573:  // settlementType
          return settlementType;
        case 1373587791:  // optionType
          return optionType;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends EtdVariant> builder() {
      return new EtdVariant.Builder();
    }

    @Override
    public Class<? extends EtdVariant> beanType() {
      return EtdVariant.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return ((EtdVariant) bean).getType();
        case 1792248507:  // dateCode
          return ((EtdVariant) bean).dateCode;
        case -295448573:  // settlementType
          return ((EtdVariant) bean).settlementType;
        case 1373587791:  // optionType
          return ((EtdVariant) bean).optionType;
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
   * The bean-builder for {@code EtdVariant}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<EtdVariant> {

    private EtdExpiryType type;
    private Integer dateCode;
    private EtdSettlementType settlementType;
    private EtdOptionType optionType;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case 1792248507:  // dateCode
          return dateCode;
        case -295448573:  // settlementType
          return settlementType;
        case 1373587791:  // optionType
          return optionType;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          this.type = (EtdExpiryType) newValue;
          break;
        case 1792248507:  // dateCode
          this.dateCode = (Integer) newValue;
          break;
        case -295448573:  // settlementType
          this.settlementType = (EtdSettlementType) newValue;
          break;
        case 1373587791:  // optionType
          this.optionType = (EtdOptionType) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public EtdVariant build() {
      return new EtdVariant(
          type,
          dateCode,
          settlementType,
          optionType);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("EtdVariant.Builder{");
      buf.append("type").append('=').append(JodaBeanUtils.toString(type)).append(',').append(' ');
      buf.append("dateCode").append('=').append(JodaBeanUtils.toString(dateCode)).append(',').append(' ');
      buf.append("settlementType").append('=').append(JodaBeanUtils.toString(settlementType)).append(',').append(' ');
      buf.append("optionType").append('=').append(JodaBeanUtils.toString(optionType));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
