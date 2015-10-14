package org.jetbrains.android.dom;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.must.android.module.extension.AndroidModuleExtension;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidXmlCharFilter extends CharFilter {
  @Nullable
  @Override
  public Result acceptChar(char c, int prefixLength, Lookup lookup) {
    if (c != '|') {
      return null;
    }
    final PsiFile file = lookup.getPsiFile();
    return file != null && ModuleUtilCore.getExtension(file, AndroidModuleExtension.class) != null ? Result.ADD_TO_PREFIX : null;
  }
}
