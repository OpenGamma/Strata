/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;

/**
 * Provides the definition of how to calibrate an ISDA compliant curve for credit.
 * <p>
 * An ISDA compliant curve is built from a number of parameters and described by metadata.
 * Calibration is based on a list of {@link IsdaCreditCurveNode} instances, one for each parameter,
 * that specify the underlying instruments.
 */
@BeanDefinition(builderScope = "private")
public final class IsdaCreditCurveDefinition
    implements ImmutableBean, Serializable {

  /**
   * The curve name.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveName name;
  /**
   * The curve currency. 
   * <p>
   * The resultant curve will be used for discounting based on this currency. 
   * This is typically the same as the currency of the curve node instruments in {@code curveNodes}. 
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The curve valuation date.
   * <p>
   * The date on which the resultant curve is used for pricing. 
   * This date is not necessarily the same as the {@code valuationDate} of {@code MarketData} 
   * on which the market data was snapped.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate curveValuationDate;
  /**
   * The day count.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The curve nodes.
   * <p>
   * The nodes are used to find the par rates and calibrate the curve.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends IsdaCreditCurveNode>")
  private final ImmutableList<IsdaCreditCurveNode> curveNodes;
  /**
   * The flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean computeJacobian;
  /**
   * The flag indicating if the node trade should be stored or not.
   * <p>
   * This property is used only for credit curve calibration.
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean storeNodeTrade;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param name  the name
   * @param currency  the currency
   * @param curveValuationDate  the curve valuation date
   * @param dayCount  the day count
   * @param curveNodes  the curve nodes
   * @param computeJacobian  the Jacobian flag
   * @param storeNodeTrade  the node trade flag
   * @return the instance
   */
  public static IsdaCreditCurveDefinition of(
      CurveName name,
      Currency currency,
      LocalDate curveValuationDate,
      DayCount dayCount,
      List<? extends IsdaCreditCurveNode> curveNodes,
      boolean computeJacobian,
      boolean storeNodeTrade) {

    return new IsdaCreditCurveDefinition(
        name,
        currency,
        curveValuationDate,
        dayCount,
        curveNodes,
        computeJacobian,
        storeNodeTrade);
  }

  /**
   * Creates the curve from an array of parameter values.
   * <p>
   * The meaning of the parameters is determined by the implementation.
   * The size of the array must match the {@linkplain #getParameterCount() count of parameters}.
   * 
   * @param valuationDate  the valuation date
   * @param metadata  the curve metadata
   * @param parameters  the array of parameters
   * @return the curve
   */
  /**
   * Creates the ISDA compliant curve.
   * <p>
   * The parameter metadata is not stored in the metadata of the curve.
   * 
   * @param yearFractions  the year fraction values
   * @param zeroRates  the zero rate values
   * @return the curve
   */
  public InterpolatedNodalCurve curve(DoubleArray yearFractions, DoubleArray zeroRates) {
    CurveMetadata baseMetadata = Curves.zeroRates(name, dayCount);
    return InterpolatedNodalCurve.of(
        baseMetadata,
        yearFractions,
        zeroRates,
        CurveInterpolators.PRODUCT_LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.PRODUCT_LINEAR);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IsdaCreditCurveDefinition}.
   * @return the meta-bean, not null
   */
  public static IsdaCreditCurveDefinition.Meta meta() {
    return IsdaCreditCurveDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IsdaCreditCurveDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IsdaCreditCurveDefinition(
      CurveName name,
      Currency currency,
      LocalDate curveValuationDate,
      DayCount dayCount,
      List<? extends IsdaCreditCurveNode> curveNodes,
      boolean computeJacobian,
      boolean storeNodeTrade) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(curveValuationDate, "curveValuationDate");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(curveNodes, "curveNodes");
    JodaBeanUtils.notNull(computeJacobian, "computeJacobian");
    JodaBeanUtils.notNull(storeNodeTrade, "storeNodeTrade");
    this.name = name;
    this.currency = currency;
    this.curveValuationDate = curveValuationDate;
    this.dayCount = dayCount;
    this.curveNodes = ImmutableList.copyOf(curveNodes);
    this.computeJacobian = computeJacobian;
    this.storeNodeTrade = storeNodeTrade;
  }

  @Override
  public IsdaCreditCurveDefinition.Meta metaBean() {
    return IsdaCreditCurveDefinition.Meta.INSTANCE;
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
   * Gets the curve name.
   * @return the value of the property, not null
   */
  public CurveName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the curve currency.
   * <p>
   * The resultant curve will be used for discounting based on this currency.
   * This is typically the same as the currency of the curve node instruments in {@code curveNodes}.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the curve valuation date.
   * <p>
   * The date on which the resultant curve is used for pricing.
   * This date is not necessarily the same as the {@code valuationDate} of {@code MarketData}
   * on which the market data was snapped.
   * @return the value of the property, not null
   */
  public LocalDate getCurveValuationDate() {
    return curveValuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the curve nodes.
   * <p>
   * The nodes are used to find the par rates and calibrate the curve.
   * @return the value of the property, not null
   */
  public ImmutableList<IsdaCreditCurveNode> getCurveNodes() {
    return curveNodes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   * @return the value of the property, not null
   */
  public boolean isComputeJacobian() {
    return computeJacobian;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating if the node trade should be stored or not.
   * <p>
   * This property is used only for credit curve calibration.
   * @return the value of the property, not null
   */
  public boolean isStoreNodeTrade() {
    return storeNodeTrade;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IsdaCreditCurveDefinition other = (IsdaCreditCurveDefinition) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(curveValuationDate, other.curveValuationDate) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(curveNodes, other.curveNodes) &&
          (computeJacobian == other.computeJacobian) &&
          (storeNodeTrade == other.storeNodeTrade);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(curveValuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(curveNodes);
    hash = hash * 31 + JodaBeanUtils.hashCode(computeJacobian);
    hash = hash * 31 + JodaBeanUtils.hashCode(storeNodeTrade);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("IsdaCreditCurveDefinition{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("curveValuationDate").append('=').append(curveValuationDate).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("curveNodes").append('=').append(curveNodes).append(',').append(' ');
    buf.append("computeJacobian").append('=').append(computeJacobian).append(',').append(' ');
    buf.append("storeNodeTrade").append('=').append(JodaBeanUtils.toString(storeNodeTrade));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IsdaCreditCurveDefinition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<CurveName> name = DirectMetaProperty.ofImmutable(
        this, "name", IsdaCreditCurveDefinition.class, CurveName.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IsdaCreditCurveDefinition.class, Currency.class);
    /**
     * The meta-property for the {@code curveValuationDate} property.
     */
    private final MetaProperty<LocalDate> curveValuationDate = DirectMetaProperty.ofImmutable(
        this, "curveValuationDate", IsdaCreditCurveDefinition.class, LocalDate.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", IsdaCreditCurveDefinition.class, DayCount.class);
    /**
     * The meta-property for the {@code curveNodes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<IsdaCreditCurveNode>> curveNodes = DirectMetaProperty.ofImmutable(
        this, "curveNodes", IsdaCreditCurveDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code computeJacobian} property.
     */
    private final MetaProperty<Boolean> computeJacobian = DirectMetaProperty.ofImmutable(
        this, "computeJacobian", IsdaCreditCurveDefinition.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code storeNodeTrade} property.
     */
    private final MetaProperty<Boolean> storeNodeTrade = DirectMetaProperty.ofImmutable(
        this, "storeNodeTrade", IsdaCreditCurveDefinition.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currency",
        "curveValuationDate",
        "dayCount",
        "curveNodes",
        "computeJacobian",
        "storeNodeTrade");

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
        case 318917792:  // curveValuationDate
          return curveValuationDate;
        case 1905311443:  // dayCount
          return dayCount;
        case -1863622910:  // curveNodes
          return curveNodes;
        case -1730091410:  // computeJacobian
          return computeJacobian;
        case 561141921:  // storeNodeTrade
          return storeNodeTrade;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IsdaCreditCurveDefinition> builder() {
      return new IsdaCreditCurveDefinition.Builder();
    }

    @Override
    public Class<? extends IsdaCreditCurveDefinition> beanType() {
      return IsdaCreditCurveDefinition.class;
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
    public MetaProperty<CurveName> name() {
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
     * The meta-property for the {@code curveValuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> curveValuationDate() {
      return curveValuationDate;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code curveNodes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<IsdaCreditCurveNode>> curveNodes() {
      return curveNodes;
    }

    /**
     * The meta-property for the {@code computeJacobian} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> computeJacobian() {
      return computeJacobian;
    }

    /**
     * The meta-property for the {@code storeNodeTrade} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> storeNodeTrade() {
      return storeNodeTrade;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((IsdaCreditCurveDefinition) bean).getName();
        case 575402001:  // currency
          return ((IsdaCreditCurveDefinition) bean).getCurrency();
        case 318917792:  // curveValuationDate
          return ((IsdaCreditCurveDefinition) bean).getCurveValuationDate();
        case 1905311443:  // dayCount
          return ((IsdaCreditCurveDefinition) bean).getDayCount();
        case -1863622910:  // curveNodes
          return ((IsdaCreditCurveDefinition) bean).getCurveNodes();
        case -1730091410:  // computeJacobian
          return ((IsdaCreditCurveDefinition) bean).isComputeJacobian();
        case 561141921:  // storeNodeTrade
          return ((IsdaCreditCurveDefinition) bean).isStoreNodeTrade();
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
   * The bean-builder for {@code IsdaCreditCurveDefinition}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IsdaCreditCurveDefinition> {

    private CurveName name;
    private Currency currency;
    private LocalDate curveValuationDate;
    private DayCount dayCount;
    private List<? extends IsdaCreditCurveNode> curveNodes = ImmutableList.of();
    private boolean computeJacobian;
    private boolean storeNodeTrade;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 575402001:  // currency
          return currency;
        case 318917792:  // curveValuationDate
          return curveValuationDate;
        case 1905311443:  // dayCount
          return dayCount;
        case -1863622910:  // curveNodes
          return curveNodes;
        case -1730091410:  // computeJacobian
          return computeJacobian;
        case 561141921:  // storeNodeTrade
          return storeNodeTrade;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (CurveName) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 318917792:  // curveValuationDate
          this.curveValuationDate = (LocalDate) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1863622910:  // curveNodes
          this.curveNodes = (List<? extends IsdaCreditCurveNode>) newValue;
          break;
        case -1730091410:  // computeJacobian
          this.computeJacobian = (Boolean) newValue;
          break;
        case 561141921:  // storeNodeTrade
          this.storeNodeTrade = (Boolean) newValue;
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
    public IsdaCreditCurveDefinition build() {
      return new IsdaCreditCurveDefinition(
          name,
          currency,
          curveValuationDate,
          dayCount,
          curveNodes,
          computeJacobian,
          storeNodeTrade);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("IsdaCreditCurveDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("curveValuationDate").append('=').append(JodaBeanUtils.toString(curveValuationDate)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("curveNodes").append('=').append(JodaBeanUtils.toString(curveNodes)).append(',').append(' ');
      buf.append("computeJacobian").append('=').append(JodaBeanUtils.toString(computeJacobian)).append(',').append(' ');
      buf.append("storeNodeTrade").append('=').append(JodaBeanUtils.toString(storeNodeTrade));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
