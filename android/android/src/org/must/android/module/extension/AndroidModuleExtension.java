package org.must.android.module.extension;

import com.android.tools.idea.gradle.IdeaAndroidProject;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.IdeaSourceProvider;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.jetbrains.android.resourceManagers.SystemResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.android.model.impl.JpsAndroidModuleProperties;

/**
 * @author VISTALL
 * @since 14.10.2015
 */
public interface AndroidModuleExtension<T extends AndroidModuleExtension<T>> extends ModuleExtensionWithSdk<T> {
  @Nullable
  IdeaAndroidProject getIdeaAndroidProject();

  @NotNull
  LocalResourceManager getLocalResourceManager();

  @Nullable
  SystemResourceManager getSystemResourceManager();

  @Nullable
  SystemResourceManager getSystemResourceManager(boolean publicOnly);

  @Nullable
  Manifest getManifest();

  boolean isGradleProject();

  boolean isLibraryProject();

  @NotNull
  JpsAndroidModuleProperties getProperties();

  @NotNull
  IdeaSourceProvider getMainIdeaSourceProvider();
}
