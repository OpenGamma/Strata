/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.ObjIntPair;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioPerturbation;
import com.opengamma.strata.market.ShiftType;

/**
 * A perturbation that applies different shifts to specific points in a parameterized data.
 * <p>
 * Examples of parameterized data include curve, option volatilities and model parameters.
 * <p>
 * This class contains a set of shifts, each one associated with a different parameter in the data.
 * Each shift has an associated key that is matched against the parameterized data.
 * In order for this to work the parameterized data must have matching and unique parameter metadata.
 * <p>
 * When matching the shift to the parameterized data, either the identifier or label parameter may be used.
 * A shift is not applied if there is no point on the parameterized data with a matching identifier.
 *
 * @see ParameterMetadata#getIdentifier()
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class PointShifts
    implements ScenarioPerturbation<ParameterizedData>, ImmutableBean, Serializable {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(PointShifts.class);

  /**
   * The type of shift applied to the parameters.
   */
  @PropertyDefinition(validate = "notNull")
  private final ShiftType shiftType;

  /**
   * The shift to apply to the rates.
   * <p>
   * There is one row in the matrix for each scenario and one column for each parameter in the data.
   * Node indices are found using {@code nodeIndices}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleMatrix shifts;

  /**
   * Indices of each parameter, keyed by an object identifying the node.
   * <p>
   * The key is typically the node {@linkplain ParameterMetadata#getIdentifier() identifier}.
   * The key may also be the node {@linkplain ParameterMetadata#getLabel() label}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Object, Integer> nodeIndices;

  //-------------------------------------------------------------------------
  /**
   * Returns a new mutable builder for building instances of {@code ParameterizedDataPointShifts}.
   *
   * @param shiftType  the type of shift to apply to the rates
   * @return a new mutable builder for building instances of {@code ParameterizedDataPointShifts}
   */
  public static PointShiftsBuilder builder(ShiftType shiftType) {
    return new PointShiftsBuilder(shiftType);
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Creates a new set of point shifts.
   *
   * @param shiftType  the type of the shift, absolute or relative
   * @param shifts  the shifts, with one row per scenario and one column per parameter
   * @param nodeIdentifiers  the node identifiers corresponding to the columns in the matrix of shifts
   */
  PointShifts(
      ShiftType shiftType,
      DoubleMatrix shifts,
      List<Object> nodeIdentifiers) {

    this(shiftType, shifts, buildNodeMap(nodeIdentifiers));
  }

  private static Map<Object, Integer> buildNodeMap(List<Object> nodeIdentifiers) {
    return Guavate.zipWithIndex(nodeIdentifiers.stream())
        .collect(toImmutableMap(ObjIntPair::getFirst, ObjIntPair::getSecond));
  }

  //-------------------------------------------------------------------------

  @Override
  public MarketDataBox<ParameterizedData> applyTo(
      MarketDataBox<ParameterizedData> marketData,
      ReferenceData refData) {

    log.debug("Applying {} point shift to ParameterizedData '{}'", shiftType,
        marketData.getValue(0).toString());
    return marketData.mapWithIndex(
        shifts.rowCount(),
        (prams, scenarioIndex) -> applyShifts(scenarioIndex, prams));
  }

  private ParameterizedData applyShifts(int scenarioIndex, ParameterizedData prams) {
    return prams.withPerturbation((index, value, meta) -> {
      Double shiftAmount = shiftForNode(scenarioIndex, meta);
      return shiftType.applyShift(value, shiftAmount);
    });
  }

  @Override
  public int getScenarioCount() {
    return shifts.rowCount();
  }

  private double shiftForNode(int scenarioIndex, ParameterMetadata meta) {
    Integer nodeIndex = nodeIndices.get(meta.getIdentifier());

    if (nodeIndex != null) {
      return shifts.get(scenarioIndex, nodeIndex);
    }
    nodeIndex = nodeIndices.get(meta.getLabel());

    if (nodeIndex != null) {
      return shifts.get(scenarioIndex, nodeIndex);
    }
    return 0;
  }

  @Override
  public Class<ParameterizedData> getMarketDataType() {
    return ParameterizedData.class;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code PointShifts}.
   * @return the meta-bean, not null
   */
  public static PointShifts.Meta meta() {
    return PointShifts.Meta.INSTANCE;
  }

  static {
    MetaBean.register(PointShifts.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param shiftType  the value of the property, not null
   * @param shifts  the value of the property, not null
   * @param nodeIndices  the value of the property, not null
   */
  PointShifts(
      ShiftType shiftType,
      DoubleMatrix shifts,
      Map<Object, Integer> nodeIndices) {
    JodaBeanUtils.notNull(shiftType, "shiftType");
    JodaBeanUtils.notNull(shifts, "shifts");
    JodaBeanUtils.notNull(nodeIndices, "nodeIndices");
    this.shiftType = shiftType;
    this.shifts = shifts;
    this.nodeIndices = ImmutableMap.copyOf(nodeIndices);
  }

  @Override
  public PointShifts.Meta metaBean() {
    return PointShifts.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of shift applied to the parameters.
   * @return the value of the property, not null
   */
  public ShiftType getShiftType() {
    return shiftType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift to apply to the rates.
   * <p>
   * There is one row in the matrix for each scenario and one column for each parameter in the data.
   * Node indices are found using {@code nodeIndices}.
   * @return the value of the property, not null
   */
  public DoubleMatrix getShifts() {
    return shifts;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets indices of each parameter, keyed by an object identifying the node.
   * <p>
   * The key is typically the node {@linkplain ParameterMetadata#getIdentifier() identifier}.
   * The key may also be the node {@linkplain ParameterMetadata#getLabel() label}.
   * @return the value of the property, not null
   */
  public ImmutableMap<Object, Integer> getNodeIndices() {
    return nodeIndices;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PointShifts other = (PointShifts) obj;
      return JodaBeanUtils.equal(shiftType, other.shiftType) &&
          JodaBeanUtils.equal(shifts, other.shifts) &&
          JodaBeanUtils.equal(nodeIndices, other.nodeIndices);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftType);
    hash = hash * 31 + JodaBeanUtils.hashCode(shifts);
    hash = hash * 31 + JodaBeanUtils.hashCode(nodeIndices);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("PointShifts{");
    buf.append("shiftType").append('=').append(shiftType).append(',').append(' ');
    buf.append("shifts").append('=').append(shifts).append(',').append(' ');
    buf.append("nodeIndices").append('=').append(JodaBeanUtils.toString(nodeIndices));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PointShifts}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code shiftType} property.
     */
    private final MetaProperty<ShiftType> shiftType = DirectMetaProperty.ofImmutable(
        this, "shiftType", PointShifts.class, ShiftType.class);
    /**
     * The meta-property for the {@code shifts} property.
     */
    private final MetaProperty<DoubleMatrix> shifts = DirectMetaProperty.ofImmutable(
        this, "shifts", PointShifts.class, DoubleMatrix.class);
    /**
     * The meta-property for the {@code nodeIndices} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Object, Integer>> nodeIndices = DirectMetaProperty.ofImmutable(
        this, "nodeIndices", PointShifts.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "shiftType",
        "shifts",
        "nodeIndices");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 893345500:  // shiftType
          return shiftType;
        case -903338959:  // shifts
          return shifts;
        case -1547874491:  // nodeIndices
          return nodeIndices;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PointShifts> builder() {
      return new PointShifts.Builder();
    }

    @Override
    public Class<? extends PointShifts> beanType() {
      return PointShifts.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code shiftType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ShiftType> shiftType() {
      return shiftType;
    }

    /**
     * The meta-property for the {@code shifts} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleMatrix> shifts() {
      return shifts;
    }

    /**
     * The meta-property for the {@code nodeIndices} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Object, Integer>> nodeIndices() {
      return nodeIndices;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 893345500:  // shiftType
          return ((PointShifts) bean).getShiftType();
        case -903338959:  // shifts
          return ((PointShifts) bean).getShifts();
        case -1547874491:  // nodeIndices
          return ((PointShifts) bean).getNodeIndices();
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
   * The bean-builder for {@code PointShifts}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<PointShifts> {

    private ShiftType shiftType;
    private DoubleMatrix shifts;
    private Map<Object, Integer> nodeIndices = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 893345500:  // shiftType
          return shiftType;
        case -903338959:  // shifts
          return shifts;
        case -1547874491:  // nodeIndices
          return nodeIndices;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 893345500:  // shiftType
          this.shiftType = (ShiftType) newValue;
          break;
        case -903338959:  // shifts
          this.shifts = (DoubleMatrix) newValue;
          break;
        case -1547874491:  // nodeIndices
          this.nodeIndices = (Map<Object, Integer>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public PointShifts build() {
      return new PointShifts(
          shiftType,
          shifts,
          nodeIndices);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("PointShifts.Builder{");
      buf.append("shiftType").append('=').append(JodaBeanUtils.toString(shiftType)).append(',').append(' ');
      buf.append("shifts").append('=').append(JodaBeanUtils.toString(shifts)).append(',').append(' ');
      buf.append("nodeIndices").append('=').append(JodaBeanUtils.toString(nodeIndices));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
