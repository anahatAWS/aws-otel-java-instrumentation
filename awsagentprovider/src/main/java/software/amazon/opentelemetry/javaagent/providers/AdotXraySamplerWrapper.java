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

package software.amazon.opentelemetry.javaagent.providers;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

public class AdotXraySamplerWrapper implements Sampler {
  // New semantic convention attributes
  private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");
  private static final AttributeKey<String> URL_FULL = AttributeKey.stringKey("url.full");
  private static final AttributeKey<String> HTTP_REQUEST_METHOD =
      AttributeKey.stringKey("http.request.method");

  // Old semantic convention attributes (from contrib)
  private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");
  private static final AttributeKey<String> HTTP_URL = AttributeKey.stringKey("http.url");
  private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");

  private final Sampler delegate;

  public AdotXraySamplerWrapper(Sampler delegate) {
    this.delegate = delegate;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    // Convert attributes to format X-Ray sampler expects
    AttributesBuilder builder = Attributes.builder();

    // Copy all existing attributes
    builder.putAll(attributes);

    // Handle URL path conversion
    String urlPath = attributes.get(URL_PATH);
    if (urlPath != null) {
      builder.put(HTTP_TARGET, urlPath);
    }

    // Handle URL full conversion
    String urlFull = attributes.get(URL_FULL);
    if (urlFull != null) {
      builder.put(HTTP_URL, urlFull);

      // Extract path from full URL if URL_PATH not present
      if (urlPath == null) {
        int schemeEndIndex = urlFull.indexOf("://");
        if (schemeEndIndex > 0) {
          int pathIndex = urlFull.indexOf('/', schemeEndIndex + "://".length());
          if (pathIndex < 0) {
            builder.put(HTTP_TARGET, "/");
          } else {
            builder.put(HTTP_TARGET, urlFull.substring(pathIndex));
          }
        }
      }
    }

    // Handle HTTP method conversion
    String httpMethod = attributes.get(HTTP_REQUEST_METHOD);
    if (httpMethod != null) {
      builder.put(HTTP_METHOD, httpMethod);
    }

    // Delegate to original X-Ray sampler with converted attributes
    return delegate.shouldSample(
        parentContext, traceId, name, spanKind, builder.build(), parentLinks);
  }

  @Override
  public String getDescription() {
    return String.format("AdotXraySampler{%s}", delegate.getDescription());
  }
}
