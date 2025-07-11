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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.AddLayerVersionPermissionRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class AwsSdkV2IntegrationTest {

  private static ByteArrayOutputStream logCapture;
  private static PrintStream originalErr;

  private static final AttributeKey<String> AWS_BUCKET_NAME =
      AttributeKey.stringKey("aws.bucket.name");
  private static final AttributeKey<String> AWS_QUEUE_URL = AttributeKey.stringKey("aws.queue.url");
  private static final AttributeKey<String> AWS_TABLE_NAME =
      AttributeKey.stringKey("aws.table.name");
  private static final AttributeKey<String> AWS_LAMBDA_NAME =
      AttributeKey.stringKey("aws.lambda.function.name");
  private static final AttributeKey<String> AWS_SNS_TOPIC_ARN =
      AttributeKey.stringKey("aws.sns.topic.arn");
  private static final AttributeKey<String> GEN_AI_SYSTEM = AttributeKey.stringKey("gen_ai.system");
  private static final AttributeKey<String> GEN_AI_MODEL =
      AttributeKey.stringKey("gen_ai.request.model");

  public static void main(String[] args) throws Exception {
    System.out.println("Starting Self-Asserting AWS SDK Integration Test...");

    // Capture stderr to intercept span logs
    setupLogCapture();

    Tracer tracer = GlobalOpenTelemetry.getTracer("test");
    Span parentSpan = tracer.spanBuilder("test-parent").startSpan();

    try {
      testS3Attributes();
      testSqsAttributes();
      testDynamoDbAttributes();
      testLambdaAttributes();
      testSnsAttributes();
      testBedrockAttributes();

    } finally {
      parentSpan.end();
    }

    Thread.sleep(3000);

    // Verify spans from captured logs
    verifySpansFromLogs();

    System.out.println("\n✅ All tests passed! SPI implementation working correctly.");
  }

  private static void setupLogCapture() {
    logCapture = new ByteArrayOutputStream();
    originalErr = System.err;
    System.setErr(new PrintStream(logCapture));
  }

  private static void testS3Attributes() {
    System.out.println("\n=== Testing S3 Attributes ===");
    try {
      S3Client client =
          S3Client.builder()
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .build();

      GetObjectRequest request =
          GetObjectRequest.builder().bucket("test-bucket").key("test-key").build();

      client.getObject(request);
    } catch (Exception e) {
      System.out.println("Expected S3 exception: " + e.getClass().getSimpleName());
    }
  }

  private static void testSqsAttributes() {
    System.out.println("\n=== Testing SQS Attributes ===");
    try {
      SqsClient client =
          SqsClient.builder()
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .build();

      SendMessageRequest request =
          SendMessageRequest.builder()
              .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue")
              .messageBody("test message")
              .build();

      client.sendMessage(request);
    } catch (Exception e) {
      System.out.println("Expected SQS exception: " + e.getClass().getSimpleName());
    }
  }

  private static void testDynamoDbAttributes() {
    System.out.println("\n=== Testing DynamoDB Attributes ===");
    try {
      DynamoDbClient client =
          DynamoDbClient.builder()
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .build();

      GetItemRequest request = GetItemRequest.builder().tableName("test-table").build();

      client.getItem(request);
    } catch (Exception e) {
      System.out.println("Expected DynamoDB exception: " + e.getClass().getSimpleName());
    }
  }

  private static void verifySpansFromLogs() {
    System.out.println("\n=== Verifying Spans from Logs ===");

    // Restore stderr and get captured logs
    System.setErr(originalErr);
    String logs = logCapture.toString();

    assertThat(logs.contains(GEN_AI_MODEL.toString())).isTrue();
    assertThat(logs.contains(AWS_BUCKET_NAME.toString())).isTrue();
    assertThat(logs.contains(AWS_QUEUE_URL.toString())).isTrue();
    assertThat(logs.contains(AWS_TABLE_NAME.toString())).isTrue();
    assertThat(logs.contains(AWS_LAMBDA_NAME.toString())).isTrue();
    assertThat(logs.contains(AWS_SNS_TOPIC_ARN.toString())).isTrue();
    assertThat(logs.contains(GEN_AI_SYSTEM.toString())).isTrue();
  }

  private static void testLambdaAttributes() {
    System.out.println("\n=== Testing Lambda Attributes ===");
    try {
      LambdaClient client =
          LambdaClient.builder()
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .build();

      // Test regular invoke
      InvokeRequest invokeRequest = InvokeRequest.builder().functionName("test-function").build();
      client.invoke(invokeRequest);

      // Test request with UUID field
      AddLayerVersionPermissionRequest permissionRequest =
          AddLayerVersionPermissionRequest.builder()
              .layerName("test-layer")
              .versionNumber(1L)
              .statementId("test-statement")
              .action("lambda:GetLayerVersion")
              .principal("123456789012")
              .build();
      client.addLayerVersionPermission(permissionRequest);
    } catch (Exception e) {
      System.out.println("Expected Lambda exception: " + e.getClass().getSimpleName());
    }
  }

  private static void testSnsAttributes() {
    System.out.println("\n=== Testing SNS Attributes ===");
    try {
      SnsClient client =
          SnsClient.builder()
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .build();

      PublishRequest request =
          PublishRequest.builder()
              .topicArn("arn:aws:sns:us-east-1:123456789012:test-topic")
              .message("test message")
              .build();

      client.publish(request);
    } catch (Exception e) {
      System.out.println("Expected SNS exception: " + e.getClass().getSimpleName());
    }
  }

  private static void testBedrockAttributes() {
    System.out.println("\n=== Testing Bedrock Attributes ===");
    try {
      BedrockRuntimeClient client =
          BedrockRuntimeClient.builder()
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .build();

      String requestBody =
          "{\"max_tokens\": 100, \"temperature\": 0.7, \"model\": \"anthropic.claude-v2\"}";
      InvokeModelRequest request =
          InvokeModelRequest.builder()
              .modelId("anthropic.claude-v2")
              .body(SdkBytes.fromUtf8String(requestBody))
              .build();

      client.invokeModel(request);
    } catch (Exception e) {
      System.out.println("Expected Bedrock exception: " + e.getClass().getSimpleName());
    }
  }
}
