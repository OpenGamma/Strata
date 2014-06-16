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
public class ViewInputsSerializer {

  private final ViewInputs _viewInputs;

  /**
   * Create a serializer for the specified view inputs instance.
   *
   * @param viewInputs the view inputs to create a serializer for
   */
  public ViewInputsSerializer(ViewInputs viewInputs) {
    _viewInputs = ArgumentChecker.notNull(viewInputs, "viewInputs");
  }

  /**
   * Serialize the view inputs to the specified output stream.
   *
   * @param outputStream the output stream to serialize to
   */
  public void serialize(OutputStream outputStream) {

    try (Writer writer = new OutputStreamWriter(outputStream)) {
      FudgeContext ctx = OpenGammaFudgeContext.getInstance();

      FormattingXmlStreamWriter xmlStreamWriter = FormattingXmlStreamWriter.builder(writer)
          .indent(true)
          .build();
      FudgeXMLStreamWriter streamWriter = new FudgeXMLStreamWriter(ctx, xmlStreamWriter);
      FudgeMsgWriter fudgeMsgWriter = new FudgeMsgWriter(streamWriter);
      MutableFudgeMsg msg = (new FudgeSerializer(ctx)).objectToFudgeMsg(_viewInputs);
      FudgeSerializer.addClassHeader(msg, _viewInputs.getClass());
      fudgeMsgWriter.writeMessage(msg);
      fudgeMsgWriter.flush();
      writer.close();
    } catch (IOException e) {
      throw new OpenGammaRuntimeException("Error whilst serializing view inputs", e);
    }
  }
}
