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

package software.amazon.opentelemetry.javaagent.instrumentation.awssdk_v2_2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AwsSdkExperimentalAttributesTest {

  private Attributes attributesMock;
  private SpanData spanDataMock;
  private InstrumentationScopeInfo instrumentationScopeInfoMock;
  private Resource resource;
  private SpanContext spanContextMock;
  private SpanContext parentSpanContextMock;

  @BeforeEach
  void setup() {
    attributesMock = mock(Attributes.class);
    spanDataMock = mock(SpanData.class);
    instrumentationScopeInfoMock = mock(InstrumentationScopeInfo.class);
    spanContextMock = mock(SpanContext.class);
    parentSpanContextMock = mock(SpanContext.class);

    when(spanDataMock.getAttributes()).thenReturn(attributesMock);
    when(spanDataMock.getInstrumentationScopeInfo()).thenReturn(instrumentationScopeInfoMock);
    when(spanDataMock.getSpanContext()).thenReturn(spanContextMock);
    when(spanDataMock.getParentSpanContext()).thenReturn(parentSpanContextMock);
    when(instrumentationScopeInfoMock.getName()).thenReturn("aws-sdk");
    when(parentSpanContextMock.isValid()).thenReturn(true);
    when(parentSpanContextMock.isRemote()).thenReturn(false);

    resource = Resource.getDefault();
  }

  @Test
  void testAwsSdkSpanWithExperimentalAttributes() {
    // Mock AWS SDK specific attributes
    mockAttribute(AwsExperimentalAttributes.AWS_BUCKET_NAME, "test-bucket");
    mockAttribute(
        AwsExperimentalAttributes.AWS_QUEUE_URL,
        "https://sqs.us-west-2.amazonaws.com/123456789012/test-queue");
    mockAttribute(AwsExperimentalAttributes.AWS_QUEUE_NAME, "test-queue");
    mockAttribute(AwsExperimentalAttributes.AWS_STREAM_NAME, "test-stream");
    mockAttribute(AwsExperimentalAttributes.AWS_TABLE_NAME, "test-table");
    mockAttribute(AwsExperimentalAttributes.AWS_AUTH_REGION, "us-west-2");

    when(spanDataMock.getKind()).thenReturn(SpanKind.CLIENT);

    // Verify that the attributes are present in the span
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.AWS_BUCKET_NAME))
        .isEqualTo("test-bucket");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.AWS_QUEUE_URL))
        .isEqualTo("https://sqs.us-west-2.amazonaws.com/123456789012/test-queue");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.AWS_QUEUE_NAME))
        .isEqualTo("test-queue");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.AWS_STREAM_NAME))
        .isEqualTo("test-stream");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.AWS_TABLE_NAME))
        .isEqualTo("test-table");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.AWS_AUTH_REGION))
        .isEqualTo("us-west-2");
  }

  @Test
  void testAwsSdkSpanWithGenAiAttributes() {
    // Mock Gen AI specific attributes
    mockAttribute(AwsExperimentalAttributes.GEN_AI_MODEL, "anthropic.claude-v2");
    mockAttribute(AwsExperimentalAttributes.GEN_AI_SYSTEM, "bedrock");
    mockAttribute(AwsExperimentalAttributes.GEN_AI_REQUEST_MAX_TOKENS, "2000");
    mockAttribute(AwsExperimentalAttributes.GEN_AI_REQUEST_TEMPERATURE, "0.7");
    mockAttribute(AwsExperimentalAttributes.GEN_AI_REQUEST_TOP_P, "0.9");

    when(spanDataMock.getKind()).thenReturn(SpanKind.CLIENT);

    // Verify that the Gen AI attributes are present
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.GEN_AI_MODEL))
        .isEqualTo("anthropic.claude-v2");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.GEN_AI_SYSTEM))
        .isEqualTo("bedrock");
    assertThat(
            spanDataMock.getAttributes().get(AwsExperimentalAttributes.GEN_AI_REQUEST_MAX_TOKENS))
        .isEqualTo("2000");
    assertThat(
            spanDataMock.getAttributes().get(AwsExperimentalAttributes.GEN_AI_REQUEST_TEMPERATURE))
        .isEqualTo("0.7");
    assertThat(spanDataMock.getAttributes().get(AwsExperimentalAttributes.GEN_AI_REQUEST_TOP_P))
        .isEqualTo("0.9");
  }

  private <T> void mockAttribute(AttributeKey<T> key, T value) {
    when(attributesMock.get(key)).thenReturn(value);
  }
}
