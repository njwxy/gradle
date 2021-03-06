/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.file;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.file.DefaultFilePropertyFactory.DefaultDirectoryVar;
import org.gradle.api.internal.file.DefaultFilePropertyFactory.FixedDirectory;
import org.gradle.api.internal.file.DefaultFilePropertyFactory.FixedFile;
import org.gradle.api.internal.file.collections.MinimalFileSet;
import org.gradle.api.internal.provider.AbstractMappingProvider;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.provider.Provider;
import org.gradle.internal.deprecation.DeprecationLogger;

import java.io.File;

public class DefaultProjectLayout implements ProjectLayout, TaskFileVarFactory {
    private final FixedDirectory projectDir;
    private final DefaultDirectoryVar buildDir;
    private final TaskDependencyFactory taskDependencyFactory;
    private final FileCollectionFactory fileCollectionFactory;

    public DefaultProjectLayout(File projectDir, FileResolver resolver, TaskDependencyFactory taskDependencyFactory, FileCollectionFactory fileCollectionFactory) {
        this.taskDependencyFactory = taskDependencyFactory;
        this.fileCollectionFactory = fileCollectionFactory;
        this.projectDir = new FixedDirectory(projectDir, resolver, fileCollectionFactory);
        this.buildDir = new DefaultDirectoryVar(resolver, fileCollectionFactory, Project.DEFAULT_BUILD_DIR_NAME);
    }

    @Override
    public Directory getProjectDirectory() {
        return projectDir;
    }

    @Override
    public DirectoryProperty getBuildDirectory() {
        return buildDir;
    }

    @Override
    public ConfigurableFileCollection newInputFileCollection(Task consumer) {
        return new CachingTaskInputFileCollection(projectDir.fileResolver, projectDir.fileResolver.getPatternSetFactory(), taskDependencyFactory);
    }

    @Override
    public FileCollection newCalculatedInputFileCollection(Task consumer, MinimalFileSet calculatedFiles, FileCollection... inputs) {
        return new CalculatedTaskInputFileCollection(consumer.getPath(), calculatedFiles, inputs);
    }

    @Override
    public Provider<RegularFile> file(Provider<File> provider) {
        return new AbstractMappingProvider<RegularFile, File>(RegularFile.class, Providers.internal(provider)) {
            @Override
            protected RegularFile mapValue(File file) {
                return new FixedFile(projectDir.fileResolver.resolve(file));
            }
        };
    }

    @Override
    public Provider<Directory> dir(Provider<File> provider) {
        return new AbstractMappingProvider<Directory, File>(Directory.class, Providers.internal(provider)) {
            @Override
            protected Directory mapValue(File file) {
                return new FixedDirectory(projectDir.fileResolver.resolve(file), projectDir.fileResolver, fileCollectionFactory);
            }
        };
    }

    @Override
    public FileCollection files(Object... paths) {
        return fileCollectionFactory.resolving(paths);
    }

    @Override
    public ConfigurableFileCollection configurableFiles(Object... files) {
        DeprecationLogger.deprecateMethod("ProjectLayout.configurableFiles()").replaceWith("ObjectFactory.fileCollection()").nagUser();
        return fileCollectionFactory.configurableFiles().from(files);
    }

    /**
     * A temporary home. Should be on the public API somewhere
     */
    public void setBuildDirectory(Object value) {
        buildDir.resolveAndSet(value);
    }
}
