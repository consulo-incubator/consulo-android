package org.must.android.module.extension;

import com.android.builder.model.SourceProvider;
import com.android.prefs.AndroidLocation;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.tools.idea.configurations.ConfigurationManager;
import com.android.tools.idea.databinding.DataBindingUtil;
import com.android.tools.idea.gradle.IdeaAndroidProject;
import com.android.tools.idea.model.AndroidModuleInfo;
import com.android.tools.idea.rendering.AppResourceRepository;
import com.android.tools.idea.rendering.LocalResourceRepository;
import com.android.tools.idea.rendering.ProjectResourceRepository;
import com.android.tools.idea.rendering.RenderService;
import com.android.utils.ILogger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AvdsNotSupportedException;
import org.jetbrains.android.facet.ClassMapConstructor;
import org.jetbrains.android.facet.IdeaSourceProvider;
import org.jetbrains.android.facet.ResourceFolderManager;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.jetbrains.android.resourceManagers.ResourceManager;
import org.jetbrains.android.resourceManagers.SystemResourceManager;
import org.jetbrains.android.sdk.AndroidPlatform;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.android.model.impl.JpsAndroidModuleProperties;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14.10.2015
 */
public interface AndroidModuleExtension<T extends AndroidModuleExtension<T>> extends ModuleExtensionWithSdk<T> {
  @Nullable
  AndroidPlatform getAndroidPlatform();

  @Nullable
  IAndroidTarget getAndroidTarget();

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

  @NotNull
  SourceProvider getMainSourceProvider();

  @Nullable
  PsiClass getLightRClass();

  /**
   * This returns the primary resource directory; the default location to place
   * newly created resources etc.  This method is marked deprecated since we should
   * be gradually adding in UI to allow users to choose specific resource folders
   * among the available flavors (see {@link #getFlavorSourceProviders()} etc).
   *
   * @return the primary resource dir, if any
   */
  @Deprecated
  @Nullable
  VirtualFile getPrimaryResourceDir();

  /**
   * Returns the source providers for the available flavors, which will never be null for a Gradle based
   * Android project, and always null for a legacy Android project
   *
   * @return the flavor source providers or null in legacy projects
   */
  @Nullable
  List<SourceProvider> getFlavorSourceProviders();

  /**
   * Returns the source provider specific to the flavor combination, if any.
   *
   * @return the source provider or {@code null}.
   */
  @Nullable
  SourceProvider getMultiFlavorSourceProvider();

  /**
   * Returns the source provider for the current build type, which will never be null for a Gradle based
   * Android project, and always null for a legacy Android project
   *
   * @return the build type source set or null
   */
  @Nullable
  SourceProvider getBuildTypeSourceProvider();

  /**
   * Returns the source provider specific to the variant, if any.
   *
   * @return the source provider or null
   */
  @Nullable
  SourceProvider getVariantSourceProvider();

  @Contract("true -> !null")
  @Nullable
  AppResourceRepository getAppResources(boolean createIfNecessary);

  /**
   * Returns the light BR class for this facet if it is aready set.
   *
   * @return The BR class for this facet, if exists
   * @see DataBindingUtil#getOrCreateBrClassFor(AndroidModuleExtension)
   */
  DataBindingUtil.LightBrClass getLightBrClass();

  /**
   * Set by {@linkplain DataBindingUtil} the first time we need it.
   *
   * @param lightBrClass
   * @see DataBindingUtil#getOrCreateBrClassFor(AndroidModuleExtension)
   */
  public void setLightBrClass(DataBindingUtil.LightBrClass lightBrClass);

  boolean isDataBindingEnabled();

  /**
   * Called by the {@linkplain DataBindingUtil} to update whether this facet includes data binding or not.
   *
   * @param dataBindingEnabled True if Facet includes data binding, false otherwise.
   */
  void setDataBindingEnabled(boolean dataBindingEnabled);

  @Contract("true -> !null")
  @Nullable
  LocalResourceRepository getModuleResources(boolean createIfNecessary);

  // TODO: correctly support classes from external non-platform jars
  @NotNull
  Map<String, PsiClass> getClassMap(@NotNull final String className, @NotNull final ClassMapConstructor constructor);

  @Nullable
  ResourceManager getResourceManager(@Nullable String resourcePackage);

  @Nullable
  ResourceManager getResourceManager(@Nullable String resourcePackage, @Nullable PsiElement contextElement);

  @NotNull
  ConfigurationManager getConfigurationManager();

  @Contract("true -> !null")
  @Nullable
  ConfigurationManager getConfigurationManager(boolean createIfNecessary);

  @Contract("true -> !null")
  @Nullable
  ProjectResourceRepository getProjectResources(boolean createIfNecessary);

  /**
   * Returns all resource directories, in the overlay order
   *
   * @return a list of all resource directories
   */
  @NotNull
  List<VirtualFile> getAllResourceDirectories();

  ResourceFolderManager getResourceFolderManager();

  @Nullable
  AvdManager getAvdManagerSilently();

  @NotNull
  AndroidModuleInfo getAndroidModuleInfo();

  @NotNull
  RenderService getRenderService();

  void refreshResources();

  @NotNull
  AvdInfo[] getAllAvds();

  @NotNull
  AvdInfo[] getValidCompatibleAvds();

  void launchEmulator(@Nullable String avdName, @NotNull String commands);

  @NotNull
  AvdManager getAvdManager(ILogger log) throws AvdsNotSupportedException, AndroidLocation.AndroidLocationException;
}
