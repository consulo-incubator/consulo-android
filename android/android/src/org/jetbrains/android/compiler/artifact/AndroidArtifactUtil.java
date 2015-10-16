package org.jetbrains.android.compiler.artifact;

import com.intellij.CommonBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.Processor;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.must.android.module.extension.AndroidModuleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidArtifactUtil {
  private AndroidArtifactUtil() {
  }

  public static boolean containsAndroidPackage(ArtifactEditorContext editorContext, Artifact artifact) {
    return !ArtifactUtil
      .processPackagingElements(artifact, AndroidFinalPackageElementType.getInstance(), new Processor<AndroidFinalPackageElement>() {
        @Override
        public boolean process(AndroidFinalPackageElement e) {
          return false;
        }
      }, editorContext, true);
  }

  public static boolean isRelatedArtifact(@NotNull Artifact artifact, @NotNull Module module) {
    if (!(artifact.getArtifactType() instanceof AndroidApplicationArtifactType)) {
      return false;
    }
    final AndroidModuleExtension facet = getPackagedFacet(module.getProject(), artifact);
    return facet != null && module.equals(facet.getModule());
  }

  @Nullable
  public static AndroidModuleExtension getPackagedFacet(Project project, Artifact artifact) {
    final Ref<AndroidFinalPackageElement> elementRef = Ref.create(null);
    final PackagingElementResolvingContext resolvingContext = ArtifactManager.getInstance(project).getResolvingContext();
    ArtifactUtil
      .processPackagingElements(artifact, AndroidFinalPackageElementType.getInstance(), new Processor<AndroidFinalPackageElement>() {
        @Override
        public boolean process(AndroidFinalPackageElement e) {
          elementRef.set(e);
          return false;
        }
      }, resolvingContext, true);
    final AndroidFinalPackageElement element = elementRef.get();
    return element != null ? element.getFacet() : null;
  }

  @Nullable
  public static AndroidModuleExtension chooseAndroidApplicationModule(@NotNull Project project,
                                                            @NotNull List<AndroidModuleExtension> facets) {
    final Map<Module, AndroidModuleExtension> map = new HashMap<Module, AndroidModuleExtension>();

    for (AndroidModuleExtension facet : facets) {
      map.put(facet.getModule(), facet);
    }
    String message = "Selected Android application module will be included in the created artifact with all dependencies";

    final ChooseModulesDialog dialog = new ChooseModulesDialog(project, new ArrayList<Module>(map.keySet()), "Select Module", message);
    dialog.setSingleSelectionMode();
    dialog.show();
    final List<Module> selected = dialog.getChosenElements();
    if (selected.isEmpty()) {
      return null;
    }
    assert selected.size() == 1;
    final Module module = selected.get(0);
    final String moduleName = module.getName();

    final AndroidModuleExtension facet = map.get(module);
    if (facet == null) {
      message = "Cannot find Android facet for module " + moduleName;
      Messages.showErrorDialog(project, message, CommonBundle.getErrorTitle());
      return null;
    }
    return facet;
  }
}
