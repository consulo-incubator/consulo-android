package org.jetbrains.android;

import com.intellij.openapi.module.impl.scopes.LibraryScopeBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.ResolveScopeProvider;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.android.augment.AndroidInternalRClass;
import org.jetbrains.android.sdk.AndroidSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.must.android.module.extension.AndroidModuleExtension;
import org.mustbe.consulo.module.extension.ModuleExtensionHelper;

import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidSdkResolveScopeProvider extends ResolveScopeProvider {

  @Nullable
  @Override
  public GlobalSearchScope getResolveScope(@NotNull VirtualFile file, Project project) {
    if (!ModuleExtensionHelper.getInstance(project).hasModuleExtension(AndroidModuleExtension.class)) return null;

    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    List<OrderEntry> orderEntriesForFile = index.getOrderEntriesForFile(file);
    for (OrderEntry orderEntry : orderEntriesForFile) {
      if(orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
        Sdk sdk = ((ModuleExtensionWithSdkOrderEntry)orderEntry).getSdk();
        if(sdk != null && sdk.getSdkType() instanceof AndroidSdkType) {
          return new MyJdkScope(project, (ModuleExtensionWithSdkOrderEntry)orderEntry, index.isInLibrarySource(file));
        }
      }
    }
    return null;
  }

  public static class MyJdkScope extends LibraryScopeBase {
    private final Sdk mySdk;
    private final boolean myIncludeSource;

    private MyJdkScope(Project project, @NotNull ModuleExtensionWithSdkOrderEntry entry, boolean includeSource) {
      super(project,
            entry.getFiles(OrderRootType.CLASSES),
            includeSource ? entry.getFiles(OrderRootType.SOURCES) : VirtualFile.EMPTY_ARRAY);
      myIncludeSource = includeSource;
      mySdk = entry.getSdk();
    }

    @Override
    public boolean isForceSearchingInLibrarySources() {
      return myIncludeSource;
    }

    @Override
    public int compare(@NotNull VirtualFile file1, @NotNull VirtualFile file2) {
      final boolean inSources1 = myIndex.isInLibrarySource(file1);

      if (inSources1 != myIndex.isInLibrarySource(file2)) {
        //Consider class A implements Runnable in project source.
        //Super class Object for class A is found in cls, super interface Runnable is found in cls as well (class A resolve scope is simple modules scope with dependencies).
        //Super class of cls Runnable isn't explicitly specified in the cls psi, so PsiClassImplUtil.getSuperClass/getSuperTypes return java.lang.Object
        //found via file's resolve scope (this one). So for the two hierarchies to meet in one place we should return cls Object here.
        //By default this resolve scope prefers sdk sources, so we need to override this behavior for Object.

        //The problem doesn't arise for other super class references inside Android sdk cls (e.g. A extends B implements C, where B implements C and both are inside android sdk),
        //because these references (C) are explicit and resolved via ClsJavaCodeReferenceElementImpl#resolveClassPreferringMyJar which returns cls classes despite custom Android sdk scope.
        if (!CommonClassNames.JAVA_LANG_OBJECT_SHORT.equals(file1.getNameWithoutExtension())) {
          return inSources1 ? 1 : -1;
        }
      }
      return super.compare(file1, file2);
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return super.contains(file) || AndroidInternalRClass.isAndroidInternalR(file, mySdk);
    }
  }
}
