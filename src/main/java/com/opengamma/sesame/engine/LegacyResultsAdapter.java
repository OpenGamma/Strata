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
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 *
 */
/* package */ class LegacyResultsAdapter {

  private final Map<ObjectId, Integer> _idToIndex;
  private final Map<ColumnSpec, Integer> _colToIndex;
  private final CompiledViewDefinition _compiledViewDef;
  private final List<String> _columnNames;

  /* package */ LegacyResultsAdapter(CompiledViewDefinition compiledViewDef) {
    _compiledViewDef = ArgumentChecker.notNull(compiledViewDef, "compiledViewDef");
    _idToIndex = rowIndices(compiledViewDef.getPortfolio().getRootNode());

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
    Map<ColumnSpec, Integer> colToIndex = Maps.newHashMapWithExpectedSize(columns.size());
    _columnNames = Lists.newArrayListWithCapacity(columns.size());
    int colIndex = 0;
    for (ColumnSpec column : columns) {
      colToIndex.put(column, colIndex++);
      _columnNames.add(column._header);
    }
    _colToIndex = colToIndex;
  }

  private static Map<ObjectId, Integer> rowIndices(PortfolioNode node) {
    PortfolioMapperFunction<List<ObjectId>> mapperFn = new PortfolioMapperFunction<List<ObjectId>>() {
      @Override
      public List<ObjectId> apply(PortfolioNode node) {
        return Collections.singletonList(node.getUniqueId().getObjectId());
      }

      @Override
      public List<ObjectId> apply(PortfolioNode parent, Position position) {
        List<ObjectId> ids = Lists.newArrayList(position.getUniqueId().getObjectId());
        for (Trade trade : position.getTrades()) {
          ids.add(trade.getUniqueId().getObjectId());
        }
        return ids;
      }
    };
    List<ObjectId> ids = PortfolioMapper.flatMap(node, mapperFn);
    Map<ObjectId, Integer> idsToIndex = Maps.newHashMapWithExpectedSize(ids.size());
    int rowIndex = 0;
    for (ObjectId id : ids) {
      idsToIndex.put(id, rowIndex++);
    }
    return idsToIndex;
  }

  /* package */ Results adapt(ViewResultModel resultModel) {
    Results.Builder builder = Results.builder(_columnNames);
    for (ViewResultEntry entry : resultModel.getAllResults()) {
      String calcConfigName = entry.getCalculationConfiguration();
      ComputedValueResult value = entry.getComputedValue();
      ValueSpecification valueSpec = value.getSpecification();
      CompiledViewCalculationConfiguration calcConfig = _compiledViewDef.getCompiledCalculationConfiguration(calcConfigName);
      Set<ValueRequirement> valueReqs = calcConfig.getTerminalOutputSpecifications().get(valueSpec);
      for (ValueRequirement valueReq : valueReqs) {
        ColumnSpec colSpec = new ColumnSpec(calcConfigName, valueReq.getValueName(), valueSpec.getProperties());
        Integer colIndex = _colToIndex.get(colSpec);
        Integer rowIndex = _idToIndex.get(valueReq.getTargetReference().getSpecification().getUniqueId().getObjectId());
        builder.add(rowIndex, colIndex, valueSpec.getTargetSpecification().getUniqueId(), value.getValue(), null);
      }
    }
    return builder.build();
  }

  // header is deliberately ignored for the purposes of equals and hashCode
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

    @Override
    public int hashCode() {
      return Objects.hash(_calcConfigName, _valueName, _valueProperties);
    }

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
