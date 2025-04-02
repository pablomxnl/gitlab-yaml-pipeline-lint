package org.ideplugins.ci_pipeline_lint.testing;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ExtendWith(PluginCodeInsightTestFixtureProvider.class)
@PluginTestDataPath("build/resources/test")
@Retention(RetentionPolicy.RUNTIME)

@Target(ElementType.TYPE)
public @interface PluginTest {
    Class<?> fixture() default TempDirProjectImpl.class;
}
