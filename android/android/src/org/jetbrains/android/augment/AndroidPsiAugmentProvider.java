package org.jetbrains.android.augment;

import com.android.resources.ResourceType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.util.containers.HashSet;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidPsiAugmentProvider extends PsiAugmentProvider {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.augment.AndroidPsiAugmentProvider");

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type) {
    if ((type != PsiClass.class && type != PsiField.class) ||
        !(element instanceof PsiExtensibleClass)) {
      return Collections.emptyList();
    }
    final PsiExtensibleClass aClass = (PsiExtensibleClass)element;
    final String className = aClass.getName();
    final boolean rClassAugment = AndroidUtils.R_CLASS_NAME.equals(className)
                                  && type == PsiClass.class;

    if (DumbService.isDumb(element.getProject())) {
      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because of dumb mode");
      }
      return Collections.emptyList();
    }

    final AndroidFacet facet = AndroidFacet.getInstance(element);
    if (facet == null) {
      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because no facet");
      }
      return Collections.emptyList();
    }

    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) {
      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because of no containing file");
      }
      return Collections.emptyList();
    }

    if (type == PsiClass.class) {
      if (AndroidUtils.R_CLASS_NAME.equals(className) &&
          AndroidResourceUtil.isRJavaFile(facet, containingFile)) {
        final Set<String> existingInnerClasses = getOwnInnerClasses(aClass);
        final Set<String> types = ResourceReferenceConverter.getResourceTypesInCurrentModule(facet);
        final List<Psi> result = new ArrayList<Psi>();

        for (String resType : types) {
          if (!existingInnerClasses.contains(resType)) {
            final AndroidLightClass resClass = new ResourceTypeClass(facet, resType, aClass);
            result.add((Psi)resClass);
          }
        }
        if (rClassAugment) {
          LOG.debug("R_CLASS_AUGMENT: " + result.size() + " classes added");
        }
        return result;
      }
      else if (AndroidUtils.MANIFEST_CLASS_NAME.equals(className) &&
               AndroidResourceUtil.isManifestJavaFile(facet, containingFile)) {
        return Arrays.asList((Psi)new PermissionClass(facet, aClass),
                             (Psi)new PermissionGroupClass(facet, aClass));
      }

      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because containing file is not actual R.java file");
      }
    }
    else if (type == PsiField.class && !(aClass instanceof AndroidLightClass)) {
      // extend existing inner classes, not provided by this augment (ex. they can be generated by maven)
      final PsiClass parentClass = aClass.getContainingClass();

      if (parentClass != null &&
          AndroidUtils.R_CLASS_NAME.equals(parentClass.getName()) &&
          AndroidResourceUtil.isRJavaFile(facet, containingFile)) {
        final String resClassName = aClass.getName();

        if (resClassName != null && ResourceType.getEnum(resClassName) != null) {
          final Set<String> existingFields = getOwnFields(aClass);
          final PsiField[] newFields = ResourceTypeClass.buildLocalResourceFields(facet, resClassName, aClass);
          final List<Psi> result = new ArrayList<Psi>();

          for (PsiField field : newFields) {
            if (!existingFields.contains(field.getName())) {
              result.add((Psi)field);
            }
          }
          return result;
        }
      }
    }
    return Collections.emptyList();
  }

  @NotNull
  private static Set<String> getOwnInnerClasses(@NotNull PsiExtensibleClass aClass) {
    final Set<String> result = new HashSet<String>();

    for (PsiClass innerClass : aClass.getOwnInnerClasses()) {
      result.add(innerClass.getName());
    }
    return result;
  }

  @NotNull
  private static Set<String> getOwnFields(@NotNull PsiExtensibleClass aClass) {
    final Set<String> result = new HashSet<String>();

    for (PsiField field : aClass.getOwnFields()) {
      result.add(field.getName());
    }
    return result;
  }
}