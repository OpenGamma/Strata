/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.MutableFudgeMsg;
import org.fudgemsg.mapping.FudgeSerializer;
import org.fudgemsg.wire.FudgeMsgWriter;
import org.fudgemsg.wire.xml.FudgeXMLStreamWriter;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.xml.FormattingXmlStreamWriter;

/**
 * Responsible for serializing a ViewInputs object so that it
 * can be restored at a later time. Primarily a wrapper to
 * hide underlying fudge serialization constructs.
 */
public class ViewResultsSerializer {

  /**
   * The inputs used for the view.
   */
  private final ViewInputs _viewInputs;

  /**
   * The outputs produced by the view.
   */
  private final ViewOutputs _viewOutputs;

  /**
   * Create a serializer for the specified view inputs instance.
   *
   * @param results the view inputs to create a serializer for
   */
  public ViewResultsSerializer(Results results) {
    ArgumentChecker.notNull(results, "results");
    _viewInputs = results.getViewInputs();
    _viewOutputs = ViewOutputs.builder()
        .columnNames(results.getColumnNames())
        .nonPortfolioResults(results.getNonPortfolioResults())
        .rows(results.getRows())
        .build();
  }

  /**
   * Serialize the view inputs to the specified output stream.
   *
   * @param outputStream the output stream to serialize to
   */
  public void serializeViewInputs(OutputStream outputStream) {
    serialize(outputStream, _viewInputs);
  }

  /**
   * Serialize the view outputs to the specified output stream.
   *
   * @param outputStream the output stream to serialize to
   */
  public void serializeViewOutputs(OutputStream outputStream) {
    serialize(outputStream, _viewOutputs);
  }

  private void serialize(OutputStream outputStream, Object object) {
    try (Writer writer = new OutputStreamWriter(outputStream)) {
      FudgeContext ctx = OpenGammaFudgeContext.getInstance();

      FormattingXmlStreamWriter xmlStreamWriter = FormattingXmlStreamWriter.builder(writer)
          .indent(true)
          .build();
      FudgeXMLStreamWriter streamWriter = new FudgeXMLStreamWriter(ctx, xmlStreamWriter);
      FudgeMsgWriter fudgeMsgWriter = new FudgeMsgWriter(streamWriter);
      MutableFudgeMsg msg = (new FudgeSerializer(ctx)).objectToFudgeMsg(object);
      FudgeSerializer.addClassHeader(msg, object.getClass());
      fudgeMsgWriter.writeMessage(msg);
      fudgeMsgWriter.flush();
      writer.close();
    } catch (IOException e) {
      throw new OpenGammaRuntimeException("Error whilst serializing", e);
    }
  }
}
