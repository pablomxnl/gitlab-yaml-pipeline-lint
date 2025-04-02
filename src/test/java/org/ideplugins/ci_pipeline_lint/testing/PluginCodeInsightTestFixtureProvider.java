package org.ideplugins.ci_pipeline_lint.testing;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.TestApplicationManager;
import com.intellij.testFramework.fixtures.*;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import org.junit.jupiter.api.extension.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class PluginCodeInsightTestFixtureProvider
        implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    private static final String DEFAULT_TEST_DATA_PATH = "build";
    private static CodeInsightTestFixture codeInsightTestFixture;

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return CodeInsightTestFixture.class.isAssignableFrom(
                parameterContext.getParameter().getType());
    }

    private void initFixture(final String testDataPath, final Class<?> tempDir, ExtensionContext extensionContext) throws Exception {
        String projectName = extensionContext.getRequiredTestMethod().getName() + UUID.randomUUID();
        Path projectPath = Path.of(System.getProperty("java.io.tmpdir", "/tmp"), projectName);
        Class<?> tmpDirFixtureClazz = Class.forName(tempDir.getName());
        TempDirTestFixture tempDirFixture = (TempDirTestFixture)
                Arrays.stream(tmpDirFixtureClazz.getConstructors()).filter(p -> p.getParameterCount() == 0).findFirst().get().newInstance();
        final LightProjectDescriptor projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR;
        final IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder =
                (tempDirFixture instanceof LightTempDirTestFixtureImpl) ? factory.createLightFixtureBuilder(projectName) :
                        factory.createFixtureBuilder(projectName, projectPath, false);

        if (tempDirFixture instanceof TempDirProjectImpl) {
            ((TempDirProjectImpl) tempDirFixture).setRoot(projectPath);
        }

        codeInsightTestFixture = factory.createCodeInsightFixture(fixtureBuilder.getFixture(), tempDirFixture);
        codeInsightTestFixture.setTestDataPath(testDataPath);
        codeInsightTestFixture.setUp();
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (codeInsightTestFixture == null) {
            throw new ParameterResolutionException("CodeInsightTestFixture is missing.");
        }

        return codeInsightTestFixture;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            if (codeInsightTestFixture != null) {
                codeInsightTestFixture.tearDown();
            }
        } catch (Exception e) {
            context.publishReportEntry("Falied", "Failed to teardown JavaCodeInsightTestFixture.");
        }
        TestApplicationManager.getInstance().setDataProvider(null);
        codeInsightTestFixture = null;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Optional<PluginTestDataPath> pluginTestDataPathAnnotation =
                extensionContext
                        .getTestClass()
                        .flatMap(testClass -> findAnnotation(testClass, PluginTestDataPath.class));


        String testDataPath =
                pluginTestDataPathAnnotation.map(PluginTestDataPath::value).orElse(DEFAULT_TEST_DATA_PATH);

        Optional<PluginTest> pluginAnnotation = Optional.ofNullable(extensionContext.getRequiredTestClass().getAnnotation(PluginTest.class));
        Class<?> fixtureClass = pluginAnnotation.map(PluginTest::fixture).orElseThrow();
        initFixture(testDataPath, fixtureClass, extensionContext);
    }


}
