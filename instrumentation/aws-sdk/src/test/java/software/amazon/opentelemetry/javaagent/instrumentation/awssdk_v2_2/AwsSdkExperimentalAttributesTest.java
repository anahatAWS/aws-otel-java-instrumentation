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

import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class AwsSdkExperimentalAttributesTest {

  @RegisterExtension static final OpenTelemetryExtension testing = OpenTelemetryExtension.create();

  @Test
  void testS3ExperimentalAttributes() {
    // Create S3 client
    S3Client s3 =
        S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();

    // Execute S3 operation
    String bucketName = "test-bucket";
    try {
      s3.listObjects(builder -> builder.bucket(bucketName));
    } catch (Exception e) {
      // Expected exception in test environment
    }

    // Print all spans for debugging
    testing
        .getSpans()
        .forEach(
            span -> {
              System.out.println("Span: " + span.getName());
              System.out.println("Attributes: " + span.getAttributes());
            });
  }
}
