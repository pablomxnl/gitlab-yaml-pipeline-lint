package org.ideplugins.ci_pipeline_lint.testing;

import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class TempDirProjectImpl extends TempDirTestFixtureImpl {

    private Path root;

    public TempDirProjectImpl() {
    }

    public TempDirProjectImpl(Path root) {
        this.root = root;
    }

    public void setRoot(Path newRoot) {
        this.root = newRoot;
    }

    @Override
    protected @Nullable Path getTempHome() {
        return root;
    }
}
