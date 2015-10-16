package org.jetbrains.android.compiler.artifact;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import icons.AndroidIcons;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.must.android.module.extension.AndroidModuleExtension;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidFinalPackageElementType extends PackagingElementType<AndroidFinalPackageElement> {
  @NonNls public static final String TYPE_ID = "android-final-package";

  protected AndroidFinalPackageElementType() {
    super(TYPE_ID, "Android Final Package");
  }

  public static AndroidFinalPackageElementType getInstance() {
    return getInstance(AndroidFinalPackageElementType.class);
  }

  @Override
  public Icon getIcon() {
    return AndroidIcons.Android;
  }

  @Override
  public boolean isAvailableForAdd(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return getAndroidApplicationFacets(context, context.getModulesProvider().getModules()).size() > 0 &&
           !AndroidArtifactUtil.containsAndroidPackage(context, artifact);
  }

  @NotNull
  private static List<AndroidModuleExtension> getAndroidApplicationFacets(@NotNull ArtifactEditorContext context, @NotNull Module[] modules) {
    final List<AndroidModuleExtension> result = new ArrayList<AndroidModuleExtension>();
    for (Module module : modules) {
      for (AndroidModuleExtension facet : context.getFacetsProvider().getFacetsByType(module, AndroidFacet.ID)) {
        if (!facet.isLibraryProject()) {
          result.add(facet);
        }
      }
    }
    return result;
  }

  @NotNull
  @Override
  public List<? extends PackagingElement<?>> chooseAndCreate(@NotNull ArtifactEditorContext context,
                                                             @NotNull Artifact artifact,
                                                             @NotNull CompositePackagingElement<?> parent) {
    final List<AndroidModuleExtension> facets = getAndroidApplicationFacets(context, context.getModulesProvider().getModules());

    final AndroidModuleExtension facet = AndroidArtifactUtil.chooseAndroidApplicationModule(context.getProject(), facets);
    if (facet == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new AndroidFinalPackageElement(context.getProject(), facet));
  }

  @NotNull
  @Override
  public AndroidFinalPackageElement createEmpty(@NotNull Project project) {
    return new AndroidFinalPackageElement(project, null);
  }
}
