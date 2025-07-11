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

import static org.mockito.Mockito.*;

import io.opentelemetry.api.trace.Span;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;

public class AwsSdkAttributesInjectionTest {

  @Test
  void testS3ExperimentalAttributes() {
    FieldMapper fieldMapper = new FieldMapper();
    Span mockSpan = mock(Span.class);
    SdkRequest mockRequest = mock(SdkRequest.class);

    when(mockRequest.getValueForField("Bucket", Object.class))
        .thenReturn(Optional.of("test-bucket"));

    fieldMapper.mapToAttributes(mockRequest, AwsSdkRequest.S3Request, mockSpan);

    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.AWS_BUCKET_NAME.getKey()), eq("test-bucket"));
  }

  @Test
  void testSqsExperimentalAttributes() {
    FieldMapper fieldMapper = new FieldMapper();
    Span mockSpan = mock(Span.class);
    SdkRequest mockRequest = mock(SdkRequest.class);

    String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";
    when(mockRequest.getValueForField("QueueUrl", Object.class)).thenReturn(Optional.of(queueUrl));

    fieldMapper.mapToAttributes(mockRequest, AwsSdkRequest.SqsRequest, mockSpan);

    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.AWS_QUEUE_URL.getKey()), eq(queueUrl));
    // Queue name is extracted from URL by the serializer, not a separate field
  }

  @Test
  void testDynamoDbExperimentalAttributes() {
    FieldMapper fieldMapper = new FieldMapper();
    Span mockSpan = mock(Span.class);
    SdkRequest mockRequest = mock(SdkRequest.class);

    when(mockRequest.getValueForField("TableName", Object.class))
        .thenReturn(Optional.of("test-table"));

    fieldMapper.mapToAttributes(mockRequest, AwsSdkRequest.DynamoDbRequest, mockSpan);

    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.AWS_TABLE_NAME.getKey()), eq("test-table"));
  }

  @Test
  void testLambdaExperimentalAttributes() {
    FieldMapper fieldMapper = new FieldMapper();
    Span mockSpan = mock(Span.class);
    SdkRequest mockRequest = mock(SdkRequest.class);

    when(mockRequest.getValueForField("FunctionName", Object.class))
        .thenReturn(Optional.of("test-function"));

    fieldMapper.mapToAttributes(mockRequest, AwsSdkRequest.LambdaRequest, mockSpan);

    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.AWS_LAMBDA_NAME.getKey()), eq("test-function"));
  }

  @Test
  void testSnsExperimentalAttributes() {
    FieldMapper fieldMapper = new FieldMapper();
    Span mockSpan = mock(Span.class);
    SdkRequest mockRequest = mock(SdkRequest.class);

    String topicArn = "arn:aws:sns:us-east-1:123456789012:test-topic";
    when(mockRequest.getValueForField("TopicArn", Object.class)).thenReturn(Optional.of(topicArn));

    fieldMapper.mapToAttributes(mockRequest, AwsSdkRequest.SnsRequest, mockSpan);

    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.AWS_SNS_TOPIC_ARN.getKey()), eq(topicArn));
  }

  @Test
  void testBedrockExperimentalAttributes() {
    FieldMapper fieldMapper = new FieldMapper();
    Span mockSpan = mock(Span.class);
    SdkRequest mockRequest = mock(SdkRequest.class);

    String modelId = "anthropic.claude-v2";
    SdkBytes requestBody = SdkBytes.fromUtf8String("{\"max_tokens\": 100, \"temperature\": 0.7}");

    when(mockRequest.getValueForField("modelId", Object.class)).thenReturn(Optional.of(modelId));
    when(mockRequest.getValueForField("body", Object.class)).thenReturn(Optional.of(requestBody));

    fieldMapper.mapToAttributes(mockRequest, AwsSdkRequest.BedrockRuntimeRequest, mockSpan);

    verify(mockSpan).setAttribute(eq(AwsExperimentalAttributes.GEN_AI_MODEL.getKey()), eq(modelId));
    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.GEN_AI_REQUEST_MAX_TOKENS.getKey()), eq("100"));
    verify(mockSpan)
        .setAttribute(eq(AwsExperimentalAttributes.GEN_AI_REQUEST_TEMPERATURE.getKey()), eq("0.7"));
  }
}
