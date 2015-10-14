/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.databinding;

import com.google.common.collect.Maps;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.consulo.psi.PsiPackageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.must.android.module.extension.AndroidModuleExtension;
import org.mustbe.consulo.java.module.extension.JavaModuleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps data binding information related to a project
 */
public class DataBindingProjectComponent implements ProjectComponent, ModificationTracker {
  final CachedValue<AndroidModuleExtension[]> myDataBindingEnabledModules;
  final Project myProject;
  private AtomicLong myModificationCount = new AtomicLong(0);
  private Map<String, PsiJavaPackage> myDataBindingPsiPackages = Maps.newConcurrentMap();

  public DataBindingProjectComponent(final Project project) {
    myProject = project;
    myDataBindingEnabledModules = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<AndroidModuleExtension[]>() {
      @Nullable
      @Override
      public Result<AndroidModuleExtension[]> compute() {
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        List<AndroidModuleExtension> facets = new ArrayList<AndroidModuleExtension>();
        for (Module module : modules) {
          AndroidModuleExtension facet = ModuleUtilCore.getExtension(module, AndroidModuleExtension.class);
          if (facet == null) {
            continue;
          }
          if (facet.isDataBindingEnabled()) {
            facets.add(facet);
          }
        }
        myModificationCount.incrementAndGet();
        return Result.create(facets.toArray(new AndroidModuleExtension[facets.size()]), DataBindingUtil.DATA_BINDING_ENABLED_TRACKER, ModuleManager.getInstance(project));
      }
    }, false);
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public boolean hasAnyDataBindingEnabledFacet() {
    return getDataBindingEnabledFacets().length > 0;
  }

  public AndroidModuleExtension[] getDataBindingEnabledFacets() {
    return myDataBindingEnabledModules.getValue();
  }

  @Override
  public void projectOpened() {

  }

  @Override
  public void projectClosed() {

  }

  @Override
  public void initComponent() {

  }

  @Override
  public void disposeComponent() {

  }

  @NotNull
  @Override
  public String getComponentName() {
    return "data binding project component";
  }

  @Override
  public long getModificationCount() {
    return myModificationCount.longValue();
  }

  /**
   * Returns a {@linkplain PsiJavaPackage} instance for the given package name.
   * <p>
   * If it does not exist in the cache, a new one is created.
   *
   * @param packageName The qualified package name
   * @return A {@linkplain PsiJavaPackage} that represents the given qualified name
   */
  public synchronized PsiJavaPackage getOrCreateDataBindingPsiPackage(String packageName) {
    PsiJavaPackage pkg = myDataBindingPsiPackages.get(packageName);
    if (pkg == null) {
      pkg = new PsiPackageImpl(PsiManager.getInstance(myProject), PsiPackageManager.getInstance(myProject), JavaModuleExtension.class, packageName) {
        @Override
        public boolean isValid() {
          return true;
        }
      };
      myDataBindingPsiPackages.put(packageName, pkg);
    }
    return pkg;
  }
}
