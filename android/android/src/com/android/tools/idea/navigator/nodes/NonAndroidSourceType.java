/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.navigator.nodes;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;
import org.mustbe.consulo.roots.impl.ProductionContentFolderTypeProvider;
import org.mustbe.consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import org.mustbe.consulo.roots.impl.TestContentFolderTypeProvider;
import org.mustbe.consulo.roots.impl.TestResourceContentFolderTypeProvider;

import javax.swing.*;

public enum NonAndroidSourceType {
  JAVA(ProductionContentFolderTypeProvider.getInstance(), "java", AllIcons.Modules.SourceRoot),
  TESTS(TestContentFolderTypeProvider.getInstance(), "tests", AllIcons.Modules.SourceRoot),
  RESOURCES(ProductionResourceContentFolderTypeProvider.getInstance(), "resources", AllIcons.Modules.ResourcesRoot),
  TEST_RESOURCES(TestResourceContentFolderTypeProvider.getInstance(), "test-resources", AllIcons.Modules.TestResourcesRoot);

  @NotNull public final ContentFolderTypeProvider rootType;
  @NotNull public final String presentableName;
  @NotNull public final Icon icon;

  NonAndroidSourceType(ContentFolderTypeProvider rootType, String name, Icon icon) {
    this.rootType = rootType;
    this.presentableName = name;
    this.icon = icon;
  }
}
