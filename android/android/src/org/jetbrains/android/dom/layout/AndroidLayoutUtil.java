package org.jetbrains.android.dom.layout;

import org.jetbrains.android.dom.AndroidDomExtender;
import org.jetbrains.android.dom.AndroidDomUtil;
import org.jetbrains.annotations.NotNull;
import org.must.android.module.extension.AndroidModuleExtension;

import java.util.ArrayList;
import java.util.List;

import static com.android.SdkConstants.*;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidLayoutUtil {
  private AndroidLayoutUtil() {
  }

  @NotNull
  public static List<String> getPossibleRoots(@NotNull AndroidModuleExtension facet) {
    final List<String> result = new ArrayList<String>();
    result.add(VIEW_TAG);
    result.add(VIEW_MERGE);
    result.add(VIEW_FRAGMENT);
    result.addAll(AndroidDomUtil.removeUnambiguousNames(AndroidDomExtender.getViewClassMap(facet)));
    result.remove(VIEW);
    result.add(TAG_LAYOUT);
    return result;
  }
}
