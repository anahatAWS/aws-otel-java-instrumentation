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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static software.amazon.opentelemetry.javaagent.instrumentation.awssdk_v2_2.AwsExperimentalAttributes.GEN_AI_SYSTEM;
import static software.amazon.opentelemetry.javaagent.instrumentation.awssdk_v2_2.AwsSdkRequestType.BEDROCKRUNTIME;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

public class PatchedAwsSdkInstrumentationModule extends InstrumentationModule {

  public PatchedAwsSdkInstrumentationModule() {
    super("aws-sdk-adot", "aws-sdk-2.2-adot");
  }

  @Override
  public int order() {
    // Ensure this runs after OTel
    return 1;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("software.amazon.awssdk.core.interceptor.ExecutionInterceptor");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new AwsSdkInterceptorInstrumentation());
  }

  public static class AwsSdkInterceptorInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      // Target the upstream OTel interceptor
      return named("io.opentelemetry.instrumentation.awssdk.v2_2.TracingExecutionInterceptor");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          isMethod()
              .and(named("beforeExecution"))
              .and(
                  takesArgument(
                      0, named("software.amazon.awssdk.core.interceptor.Context$BeforeExecution")))
              .and(
                  takesArgument(
                      1, named("software.amazon.awssdk.core.interceptor.ExecutionAttributes"))),
          this.getClass().getName() + "$BeforeExecutionAdvice");
    }

    @SuppressWarnings("unused")
    public static class BeforeExecutionAdvice {
      @Advice.OnMethodExit
      public static void onExit(
          @Advice.Argument(0) Context.BeforeExecution context,
          @Advice.Argument(1) ExecutionAttributes executionAttributes) {
        FieldMapper fieldMapper = new FieldMapper();
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {

          // Check for Bedrock requests and add GEN_AI_SYSTEM attribute
          SdkRequest request = context.request();
          if (request != null) {
            AwsSdkRequest awsSdkRequest = AwsSdkRequest.ofSdkRequest(request);
            if (awsSdkRequest != null && awsSdkRequest.type() == BEDROCKRUNTIME) {
              currentSpan.setAttribute(GEN_AI_SYSTEM, "aws.bedrock");
              // Apply field mappings for Bedrock
              fieldMapper.mapToAttributes(request, awsSdkRequest, currentSpan);
            }
          }
        }
      }
    }
  }
}
