package org.jetbrains.android.util;

import com.intellij.ProjectTopics;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.reference.SoftReference;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NotNull;
import org.must.android.module.extension.AndroidModuleExtension;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidDependenciesCache {
  private static final Key<SoftReference<AndroidDependenciesCache>> KEY = Key.create("ANDROID_DEPENDENCIES_CACHE");
  private final Module myModule;

  private final Ref<List<WeakReference<AndroidModuleExtension>>> myAllDependencies = Ref.create();
  private final Ref<List<WeakReference<AndroidModuleExtension>>> myAllLibraryDependencies = Ref.create();

  private AndroidDependenciesCache(@NotNull Module module) {
    myModule = module;

    module.getProject().getMessageBus().connect(module).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        dropCache();
      }
    });
  }

  private synchronized void dropCache() {
    myAllDependencies.set(null);
    myAllLibraryDependencies.set(null);
  }

  @NotNull
  public static AndroidDependenciesCache getInstance(@NotNull Module module) {
    AndroidDependenciesCache cache = SoftReference.dereference(module.getUserData(KEY));

    if (cache == null) {
      cache = new AndroidDependenciesCache(module);
      module.putUserData(KEY, new SoftReference<AndroidDependenciesCache>(cache));
    }
    return cache;
  }

  @NotNull
  public synchronized List<AndroidModuleExtension> getAllAndroidDependencies(boolean androidLibrariesOnly) {
    return getAllAndroidDependencies(myModule, androidLibrariesOnly, getListRef(androidLibrariesOnly));
  }

  @NotNull
  private Ref<List<WeakReference<AndroidModuleExtension>>> getListRef(boolean androidLibrariesOnly) {
    return androidLibrariesOnly ? myAllLibraryDependencies : myAllDependencies;
  }

  @NotNull
  private static List<AndroidModuleExtension> getAllAndroidDependencies(@NotNull Module module,
                                                              boolean androidLibrariesOnly,
                                                              Ref<List<WeakReference<AndroidModuleExtension>>> listRef) {
    List<WeakReference<AndroidModuleExtension>> refs = listRef.get();

    if (refs == null) {
      final List<AndroidModuleExtension> facets = new ArrayList<AndroidModuleExtension>();
      collectAllAndroidDependencies(module, androidLibrariesOnly, facets, new HashSet<AndroidModuleExtension>());

      refs = ContainerUtil.map(ContainerUtil.reverse(facets), new Function<AndroidModuleExtension, WeakReference<AndroidModuleExtension>>() {
        @Override
        public WeakReference<AndroidModuleExtension> fun(AndroidModuleExtension facet) {
          return new WeakReference<AndroidModuleExtension>(facet);
        }
      });
      listRef.set(refs);
    }
    return dereference(refs);
  }

  @NotNull
  private static List<AndroidModuleExtension> dereference(@NotNull List<WeakReference<AndroidModuleExtension>> refs) {
    return ContainerUtil.mapNotNull(refs, new Function<WeakReference<AndroidModuleExtension>, AndroidModuleExtension>() {
      @Override
      public AndroidModuleExtension fun(WeakReference<AndroidModuleExtension> ref) {
        return ref.get();
      }
    });
  }

  private static void collectAllAndroidDependencies(Module module,
                                                    boolean androidLibrariesOnly,
                                                    List<AndroidModuleExtension> result,
                                                    Set<AndroidModuleExtension> visited) {
    final OrderEntry[] entries = ModuleRootManager.getInstance(module).getOrderEntries();
    // loop in the inverse order to resolve dependencies on the libraries, so that if a library
    // is required by two higher level libraries it can be inserted in the correct place

    for (int i = entries.length - 1; i >= 0; i--) {
      final OrderEntry orderEntry = entries[i];
      if (orderEntry instanceof ModuleOrderEntry) {
        final ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)orderEntry;

        if (moduleOrderEntry.getScope() == DependencyScope.COMPILE) {
          final Module depModule = moduleOrderEntry.getModule();

          if (depModule != null) {
            final AndroidModuleExtension depFacet = ModuleUtilCore.getExtension(depModule, AndroidModuleExtension.class);

            if (depFacet != null &&
                (!androidLibrariesOnly || depFacet.isLibraryProject()) &&
                visited.add(depFacet)) {
              final List<WeakReference<AndroidModuleExtension>> cachedDepDeps =
                getInstance(depModule).getListRef(androidLibrariesOnly).get();

              if (cachedDepDeps != null) {
                final List<AndroidModuleExtension> depDeps = dereference(cachedDepDeps);

                for (AndroidModuleExtension depDepFacet : depDeps) {
                  if (visited.add(depDepFacet)) {
                    result.add(depDepFacet);
                  }
                }
              }
              else {
                collectAllAndroidDependencies(depModule, androidLibrariesOnly, result, visited);
              }
              result.add(depFacet);
            }
          }
        }
      }
    }
  }
}
