/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.core.position.PortfolioNode;
import com.opengamma.core.position.Position;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.PortfolioMapper;
import com.opengamma.core.position.impl.PortfolioMapperFunction;
import com.opengamma.engine.value.ComputedValueResult;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.ViewCalculationConfiguration;
import com.opengamma.engine.view.ViewResultEntry;
import com.opengamma.engine.view.ViewResultModel;
import com.opengamma.engine.view.compilation.CompiledViewCalculationConfiguration;
import com.opengamma.engine.view.compilation.CompiledViewDefinition;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 *
 */
/* package */ class LegacyResultsAdapter {

  private final Map<ObjectId, Integer> _idToIndex;
  private final Map<ColumnSpec, Integer> _colToIndex;
  private final CompiledViewDefinition _compiledViewDef;
  private final List<String> _columnNames;
  private final List<UniqueIdentifiable> _inputs;

  /* package */ LegacyResultsAdapter(CompiledViewDefinition compiledViewDef) {
    _compiledViewDef = ArgumentChecker.notNull(compiledViewDef, "compiledViewDef");

    PortfolioMapperFunction<List<UniqueIdentifiable>> mapperFn = new PortfolioMapperFunction<List<UniqueIdentifiable>>() {
      @Override
      public List<UniqueIdentifiable> apply(PortfolioNode node) {
        return Collections.emptyList();
      }

      @Override
      public List<UniqueIdentifiable> apply(PortfolioNode parent, Position position) {
        List<UniqueIdentifiable> targets = Lists.<UniqueIdentifiable>newArrayList(position);
        for (Trade trade : position.getTrades()) {
          targets.add(trade);
        }
        return targets;
      }
    };
    _inputs = PortfolioMapper.flatMap(compiledViewDef.getPortfolio().getRootNode(), mapperFn);
    _idToIndex = Maps.newHashMapWithExpectedSize(_inputs.size());
    int rowIndex = 0;
    for (UniqueIdentifiable target : _inputs) {
      _idToIndex.put(target.getUniqueId().getObjectId(), rowIndex++);
    }

    //---------------------------------------------------

    Collection<ViewCalculationConfiguration> calcConfigs = compiledViewDef.getViewDefinition().getAllCalculationConfigurations();
    Set<ColumnSpec> columns = Sets.newLinkedHashSet();
    for (ViewCalculationConfiguration calcConfig : calcConfigs) {
      for (ViewCalculationConfiguration.Column column : calcConfig.getColumns()) {
        columns.add(new ColumnSpec(calcConfig.getName(), column.getValueName(), column.getProperties(), column.getHeader()));
      }
      for (Pair<String, ValueProperties> output : calcConfig.getAllPortfolioRequirements()) {
        String header;
        if (calcConfigs.size() == 1) {
          // if there's only 1 calc config then use the value name as the column header
          header = output.getFirst();
        } else {
          // if there are multiple calc configs need to include the calc config name
          header = calcConfig.getName() + "/" + output.getFirst();
        }
        columns.add(new ColumnSpec(calcConfig.getName(), output.getFirst(), output.getSecond(), header));
      }
    }
    _colToIndex = Maps.newHashMapWithExpectedSize(columns.size());
    _columnNames = Lists.newArrayListWithCapacity(columns.size());
    int colIndex = 0;
    for (ColumnSpec column : columns) {
      _colToIndex.put(column, colIndex++);
      _columnNames.add(column._header);
    }
  }

  /* package */ Results adapt(ViewResultModel resultModel) {
    ResultBuilder builder = Results.builder(_inputs, _columnNames);
    for (ViewResultEntry entry : resultModel.getAllResults()) {
      String calcConfigName = entry.getCalculationConfiguration();
      ComputedValueResult value = entry.getComputedValue();
      ValueSpecification valueSpec = value.getSpecification();
      CompiledViewCalculationConfiguration calcConfig = _compiledViewDef.getCompiledCalculationConfiguration(calcConfigName);
      Set<ValueRequirement> valueReqs = calcConfig.getTerminalOutputSpecifications().get(valueSpec);
      for (ValueRequirement valueReq : valueReqs) {
        ColumnSpec colSpec = new ColumnSpec(calcConfigName, valueReq.getValueName(), valueReq.getConstraints());
        Integer colIndex = _colToIndex.get(colSpec);
        Integer rowIndex = _idToIndex.get(valueReq.getTargetReference().getSpecification().getUniqueId().getObjectId());
        Result<Object> result;
        // TODO I'm not sure this is right, null might be considered a successful result but it's not allowed ATM
        if (value.getValue() != null) {
          result = Result.success(value.getValue());
        } else {
          result = Result.failure(FailureStatus.CALCULATION_FAILED, "Null result");
        }
        builder.add(rowIndex, colIndex, result, null);
      }
    }
    return builder.build();
  }

  private static class ColumnSpec {

    /** Name of the calculation configuration that produces the column data. */
    private final String _calcConfigName;
    /** Value name of the column's data. */
    private final String _valueName;
    /** Value properties used when calculating the column's data. */
    private final ValueProperties _valueProperties;
    /** Column header. */
    private final String _header;

    private ColumnSpec(String calcConfigName, String valueName, ValueProperties valueProperties, String header) {
      _calcConfigName = calcConfigName;
      _valueName = valueName;
      _valueProperties = valueProperties;
      _header = header;
    }

    public ColumnSpec(String calcConfigName, String valueName, ValueProperties properties) {
      this(calcConfigName, valueName, properties, null);
    }

    // header is deliberately ignored for the purposes of equals and hashCode
    @Override
    public int hashCode() {
      return Objects.hash(_calcConfigName, _valueName, _valueProperties);
    }

    // header is deliberately ignored for the purposes of equals and hashCode
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ColumnSpec other = (ColumnSpec) obj;
      return
          Objects.equals(this._calcConfigName, other._calcConfigName) &&
          Objects.equals(this._valueName, other._valueName) &&
          Objects.equals(this._valueProperties, other._valueProperties);
    }
  }
}
