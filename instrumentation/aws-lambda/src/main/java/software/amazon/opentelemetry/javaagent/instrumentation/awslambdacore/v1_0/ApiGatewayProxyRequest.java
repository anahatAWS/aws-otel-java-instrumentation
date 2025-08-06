/*
 * Copyright Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.opentelemetry.javaagent.instrumentation.awslambdacore.v1_0;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.api.GlobalOpenTelemetry;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public abstract class ApiGatewayProxyRequest {

  private static final Logger logger = Logger.getLogger(ApiGatewayProxyRequest.class.getName());

  // TODO: We should create a RequestFactory type of class instead of evaluating this for every
  // request.
  private static boolean noHttpPropagationNeeded() {
    Collection<String> fields =
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().fields();
    return fields.isEmpty() || xrayPropagationFieldsOnly(fields);
  }

  private static boolean xrayPropagationFieldsOnly(Collection<String> fields) {
    // ugly but faster than typical convert-to-set-and-check-contains-only
    return (fields.size() == 1)
        && ParentContextExtractor.AWS_TRACE_HEADER_PROPAGATOR_KEY.equalsIgnoreCase(
            fields.iterator().next());
  }

  public static ApiGatewayProxyRequest forStream(InputStream source) {
    if (noHttpPropagationNeeded()) {
      return new NoopRequest(source);
    }

    if (source.markSupported()) {
      return new MarkableApiGatewayProxyRequest(source);
    }
    // It is known that the Lambda runtime passes ByteArrayInputStream's to functions, so gracefully
    // handle this without propagating and revisit if getting user reports that expectations
    // changed.
    logger.log(
        WARNING,
        "HTTP propagation enabled but could not extract HTTP headers."
            + " This is a bug in the OpenTelemetry AWS Lambda instrumentation."
            + " Type of request stream {0}",
        source.getClass());
    return new NoopRequest(source);
  }

  @Nullable
  public Map<String, String> getHeaders() throws IOException {
    // Map<String, String> headers = ofStream(freshStream());
    return Collections.emptyMap();
  }

  public abstract InputStream freshStream() throws IOException;

  private static class NoopRequest extends ApiGatewayProxyRequest {

    private final InputStream stream;

    private NoopRequest(InputStream stream) {
      this.stream = stream;
    }

    @Override
    public InputStream freshStream() {
      return stream;
    }

    @Override
    public Map<String, String> getHeaders() {
      return Collections.emptyMap();
    }
  }

  private static class MarkableApiGatewayProxyRequest extends ApiGatewayProxyRequest {

    private final InputStream inputStream;

    private MarkableApiGatewayProxyRequest(InputStream inputStream) {
      this.inputStream = inputStream;
      inputStream.mark(Integer.MAX_VALUE);
    }

    @Override
    public InputStream freshStream() throws IOException {
      inputStream.reset();
      inputStream.mark(Integer.MAX_VALUE);
      return inputStream;
    }
  }
}
