/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.io.Serializable;
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Recombining trinomial tree data.
 * <p>
 * This includes state values and transition probabilities for all of the nodes,
 * as well as discount factors and time (time from valuation date) for individual time steps.
 */
@BeanDefinition(builderScope = "private")
public final class RecombiningTrinomialTreeData implements ImmutableBean, Serializable {

  /**
   * The state value.
   * <p>
   * The {@code (i,j)} component of this matrix represents the underlying asset price at the {@code j}-th lowest node 
   * at the {@code i}-th time layer.
   */
  @PropertyDefinition
  private final DoubleMatrix stateValue;
  /**
   * The transition probability.
   * <p>
   * The {@code i}-th element of the list represents the transition probability values for the nodes 
   * at the {@code i}-th time layer.
   * The matrix is {@code (2*i+1)} times {@code 3}, and its {@code j}-th row involves [0] down probability, 
   * [1] middle probability and [2] up probability for the {@code j}-th lowest node.
   */
  @PropertyDefinition
  private final ImmutableList<DoubleMatrix> transitionProbability;
  /**
   * The discount factor.
   * <p>
   * The {@code i}-th element is the discount factor between the {@code i}-th layer and the {@code (i+1)}-th layer.
   */
  @PropertyDefinition
  private final DoubleArray discountFactor;
  /**
   * The time.
   * <p>
   * The {@code i}-th element is the year fraction between the {@code 0}-th time layer and the {@code i}-th layer.
   */
  @PropertyDefinition
  private final DoubleArray time;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param stateValue  the state value
   * @param transitionProbability  the transition probability
   * @param discountFactor  the discount factor
   * @param time  the time
   * @return the instance
   */
  public static RecombiningTrinomialTreeData of(
      DoubleMatrix stateValue,
      List<DoubleMatrix> transitionProbability,
      DoubleArray discountFactor,
      DoubleArray time) {

    int nSteps = discountFactor.size();
    ArgChecker.isTrue(stateValue.rowCount() == nSteps + 1, "the number of rows of stateValue must be (nSteps + 1)");
    ArgChecker.isTrue(transitionProbability.size() == nSteps, "the size of transitionProbability list must be nSteps");
    ArgChecker.isTrue(time.size() == nSteps + 1, "the size of time must be (nSteps + 1)");
    for (int i = 0; i < nSteps; ++i) {
      ArgChecker.isTrue(stateValue.row(i).size() == 2 * i + 1,
          "the i-th row of stateValue must have the size (2 * i + 1)");
      ArgChecker.isTrue(transitionProbability.get(i).rowCount() == 2 * i + 1,
          "the i-th element of transitionProbability list must have (2 * i + 1) rows");
      ArgChecker.isTrue(transitionProbability.get(i).columnCount() == 3,
          "the i-th element of transitionProbability list must have 3 columns");
    }
    ArgChecker.isTrue(stateValue.row(nSteps).size() == 2 * nSteps + 1);
    return new RecombiningTrinomialTreeData(stateValue, transitionProbability, discountFactor, time);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the number of time steps.
   * 
   * @return the number of time steps
   */
  public int getNumberOfSteps() {
    return transitionProbability.size();
  }

  /**
   * Obtains the state values at the {@code i}-th time layer
   * 
   * @param i  the layer
   * @return the state values
   */
  public DoubleArray getStateValueAtLayer(int i) {
    return stateValue.row(i);
  }

  /**
   * Obtains the transition probability values at the {@code i}-th time layer
   * 
   * @param i  the layer
   * @return the transition probability
   */
  public DoubleMatrix getProbabilityAtLayer(int i) {
    return transitionProbability.get(i);
  }

  /**
   * Obtains discount factor between the {@code i}-th layer to the {@code (i+1)}-th layer.
   * 
   * @param i  the layer
   * @return the discount factor
   */
  public double getDiscountFactorAtLayer(int i) {
    return discountFactor.get(i);
  }

  /**
   * Obtains the spot.
   * 
   * @return the spot
   */
  public double getSpot() {
    return stateValue.get(0, 0);
  }

  /**
   * Obtains the time for the {@code i}-th layer.
   * <p>
   * The time is the year fraction between the {@code 0}-th layer and the {@code i}-th layer.
   * 
   * @param i  the layer
   * @return the time
   */
  public double getTime(int i) {
    return time.get(i);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RecombiningTrinomialTreeData}.
   * @return the meta-bean, not null
   */
  public static RecombiningTrinomialTreeData.Meta meta() {
    return RecombiningTrinomialTreeData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RecombiningTrinomialTreeData.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private RecombiningTrinomialTreeData(
      DoubleMatrix stateValue,
      List<DoubleMatrix> transitionProbability,
      DoubleArray discountFactor,
      DoubleArray time) {
    this.stateValue = stateValue;
    this.transitionProbability = (transitionProbability != null ? ImmutableList.copyOf(transitionProbability) : null);
    this.discountFactor = discountFactor;
    this.time = time;
  }

  @Override
  public RecombiningTrinomialTreeData.Meta metaBean() {
    return RecombiningTrinomialTreeData.Meta.INSTANCE;
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
   * Gets the state value.
   * <p>
   * The {@code (i,j)} component of this matrix represents the underlying asset price at the {@code j}-th lowest node
   * at the {@code i}-th time layer.
   * @return the value of the property
   */
  public DoubleMatrix getStateValue() {
    return stateValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the transition probability.
   * <p>
   * The {@code i}-th element of the list represents the transition probability values for the nodes
   * at the {@code i}-th time layer.
   * The matrix is {@code (2*i+1)} times {@code 3}, and its {@code j}-th row involves [0] down probability,
   * [1] middle probability and [2] up probability for the {@code j}-th lowest node.
   * @return the value of the property
   */
  public ImmutableList<DoubleMatrix> getTransitionProbability() {
    return transitionProbability;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factor.
   * <p>
   * The {@code i}-th element is the discount factor between the {@code i}-th layer and the {@code (i+1)}-th layer.
   * @return the value of the property
   */
  public DoubleArray getDiscountFactor() {
    return discountFactor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time.
   * <p>
   * The {@code i}-th element is the year fraction between the {@code 0}-th time layer and the {@code i}-th layer.
   * @return the value of the property
   */
  public DoubleArray getTime() {
    return time;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RecombiningTrinomialTreeData other = (RecombiningTrinomialTreeData) obj;
      return JodaBeanUtils.equal(stateValue, other.stateValue) &&
          JodaBeanUtils.equal(transitionProbability, other.transitionProbability) &&
          JodaBeanUtils.equal(discountFactor, other.discountFactor) &&
          JodaBeanUtils.equal(time, other.time);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(stateValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(transitionProbability);
    hash = hash * 31 + JodaBeanUtils.hashCode(discountFactor);
    hash = hash * 31 + JodaBeanUtils.hashCode(time);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("RecombiningTrinomialTreeData{");
    buf.append("stateValue").append('=').append(stateValue).append(',').append(' ');
    buf.append("transitionProbability").append('=').append(transitionProbability).append(',').append(' ');
    buf.append("discountFactor").append('=').append(discountFactor).append(',').append(' ');
    buf.append("time").append('=').append(JodaBeanUtils.toString(time));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RecombiningTrinomialTreeData}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code stateValue} property.
     */
    private final MetaProperty<DoubleMatrix> stateValue = DirectMetaProperty.ofImmutable(
        this, "stateValue", RecombiningTrinomialTreeData.class, DoubleMatrix.class);
    /**
     * The meta-property for the {@code transitionProbability} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<DoubleMatrix>> transitionProbability = DirectMetaProperty.ofImmutable(
        this, "transitionProbability", RecombiningTrinomialTreeData.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code discountFactor} property.
     */
    private final MetaProperty<DoubleArray> discountFactor = DirectMetaProperty.ofImmutable(
        this, "discountFactor", RecombiningTrinomialTreeData.class, DoubleArray.class);
    /**
     * The meta-property for the {@code time} property.
     */
    private final MetaProperty<DoubleArray> time = DirectMetaProperty.ofImmutable(
        this, "time", RecombiningTrinomialTreeData.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "stateValue",
        "transitionProbability",
        "discountFactor",
        "time");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -236449952:  // stateValue
          return stateValue;
        case 734501792:  // transitionProbability
          return transitionProbability;
        case -557144592:  // discountFactor
          return discountFactor;
        case 3560141:  // time
          return time;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends RecombiningTrinomialTreeData> builder() {
      return new RecombiningTrinomialTreeData.Builder();
    }

    @Override
    public Class<? extends RecombiningTrinomialTreeData> beanType() {
      return RecombiningTrinomialTreeData.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code stateValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleMatrix> stateValue() {
      return stateValue;
    }

    /**
     * The meta-property for the {@code transitionProbability} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<DoubleMatrix>> transitionProbability() {
      return transitionProbability;
    }

    /**
     * The meta-property for the {@code discountFactor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> discountFactor() {
      return discountFactor;
    }

    /**
     * The meta-property for the {@code time} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> time() {
      return time;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -236449952:  // stateValue
          return ((RecombiningTrinomialTreeData) bean).getStateValue();
        case 734501792:  // transitionProbability
          return ((RecombiningTrinomialTreeData) bean).getTransitionProbability();
        case -557144592:  // discountFactor
          return ((RecombiningTrinomialTreeData) bean).getDiscountFactor();
        case 3560141:  // time
          return ((RecombiningTrinomialTreeData) bean).getTime();
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
   * The bean-builder for {@code RecombiningTrinomialTreeData}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<RecombiningTrinomialTreeData> {

    private DoubleMatrix stateValue;
    private List<DoubleMatrix> transitionProbability;
    private DoubleArray discountFactor;
    private DoubleArray time;

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
        case -236449952:  // stateValue
          return stateValue;
        case 734501792:  // transitionProbability
          return transitionProbability;
        case -557144592:  // discountFactor
          return discountFactor;
        case 3560141:  // time
          return time;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -236449952:  // stateValue
          this.stateValue = (DoubleMatrix) newValue;
          break;
        case 734501792:  // transitionProbability
          this.transitionProbability = (List<DoubleMatrix>) newValue;
          break;
        case -557144592:  // discountFactor
          this.discountFactor = (DoubleArray) newValue;
          break;
        case 3560141:  // time
          this.time = (DoubleArray) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public RecombiningTrinomialTreeData build() {
      return new RecombiningTrinomialTreeData(
          stateValue,
          transitionProbability,
          discountFactor,
          time);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("RecombiningTrinomialTreeData.Builder{");
      buf.append("stateValue").append('=').append(JodaBeanUtils.toString(stateValue)).append(',').append(' ');
      buf.append("transitionProbability").append('=').append(JodaBeanUtils.toString(transitionProbability)).append(',').append(' ');
      buf.append("discountFactor").append('=').append(JodaBeanUtils.toString(discountFactor)).append(',').append(' ');
      buf.append("time").append('=').append(JodaBeanUtils.toString(time));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
