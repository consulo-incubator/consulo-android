package org.jetbrains.android;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.android.dom.converters.OnClickConverter;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.must.android.module.extension.AndroidModuleExtension;

import static com.android.SdkConstants.*;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidImplicitUsagesProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(PsiElement element) {
    if (element instanceof PsiField) {
      return isImplicitFieldUsage((PsiField)element);
    }
    else if (element instanceof PsiParameter) {
      return isImplicitParameterUsage((PsiParameter)element);
    }
    else if (element instanceof PsiMethod) {
      PsiMethod method = (PsiMethod)element;
      return method.isConstructor() && isImplicitConstructorUsage(method);
    }
    return false;
  }

  private static boolean isImplicitParameterUsage(@NotNull PsiParameter parameter) {
    if (ModuleUtilCore.getExtension(parameter, AndroidModuleExtension.class) == null) {
      return false;
    }
    final PsiMethod method = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);

    if (method == null ||
        !OnClickConverter.CONVERTER_FOR_LAYOUT.checkSignature(method) &&
        !OnClickConverter.CONVERTER_FOR_MENU.checkSignature(method)) {
      return false;
    }
    final PsiClass aClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);

    if (aClass == null) {
      return false;
    }
    final PsiClass activityBaseClass = JavaPsiFacade.getInstance(aClass.getProject()).
      findClass(AndroidUtils.ACTIVITY_BASE_CLASS_NAME, parameter.getResolveScope());

    if (activityBaseClass == null) {
      return false;
    }
    return aClass.isInheritor(activityBaseClass, true);
  }

  private static boolean isImplicitFieldUsage(@NotNull PsiField field) {
    if (!"CREATOR".equals(field.getName())) {
      return false;
    }
    final PsiModifierList modifierList = field.getModifierList();

    if (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.STATIC)) {
      return false;
    }
    final PsiClass aClass = field.getContainingClass();
    return aClass != null && InheritanceUtil.isInheritor(aClass, "android.os.Parcelable");
  }

  @Override
  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(PsiElement element) {
    if (!(element instanceof PsiField)) {
      return false;
    }
    final AndroidModuleExtension facet = ModuleUtilCore.getExtension(element, AndroidModuleExtension.class);

    if (facet == null) {
      return false;
    }
    final PsiField field = (PsiField)element;
    final PsiModifierList modifierList = field.getModifierList();

    if (modifierList == null) {
      return false;
    }
    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
        final PsiAnnotationMemberValue value = pair.getValue();

        if (isResourceReference(value)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isResourceReference(@Nullable PsiAnnotationMemberValue value) {
    if (!(value instanceof PsiReferenceExpression)) {
      return false;
    }
    PsiReferenceExpression exp = (PsiReferenceExpression)value;
    String refName = exp.getReferenceName();

    if (refName == null || refName.length() == 0) {
      return false;
    }
    PsiExpression qExp = exp.getQualifierExpression();

    if (!(qExp instanceof PsiReferenceExpression)) {
      return false;
    }
    exp = (PsiReferenceExpression)qExp;
    refName = exp.getReferenceName();

    if (refName == null || refName.length() == 0) {
      return false;
    }
    qExp = exp.getQualifierExpression();

    if (!(qExp instanceof PsiReferenceExpression)) {
      return false;
    }
    exp = (PsiReferenceExpression)qExp;
    return AndroidUtils.R_CLASS_NAME.equals(exp.getReferenceName());
  }

  public boolean isImplicitConstructorUsage(PsiMethod method) {
    if (!method.isConstructor()) {
      return false;
    }

    if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
      return false;
    }

    PsiParameterList parameterList = method.getParameterList();
    int parameterCount = parameterList.getParametersCount();
    if (parameterCount == 0) {
      // Some Android classes need default constructors, and are invoked by inflaters
      final PsiClass aClass = method.getContainingClass();
      if (aClass != null) {
        if (InheritanceUtil.isInheritor(aClass, CLASS_FRAGMENT)
          || InheritanceUtil.isInheritor(aClass, CLASS_V4_FRAGMENT)
          || InheritanceUtil.isInheritor(aClass, CLASS_BACKUP_AGENT)) {
          // Activity, Service, ContentProvider and BroadcastReceiver should also be treated as having implicit usages,
          // but for some reason that's already the case (they are not marked as unused constructors currently;
          // perhaps due to the XML DOM bindings?
          return true;
        }
      }
      return false;
    }

    // Look for View constructors; these are of one of these forms:
    //   View(android.content.Context context)
    //   View(android.content.Context context, android.util.AttributeSet attrs)
    //   View(android.content.Context context, android.util.AttributeSet attrs, int defStyle)
    // Also check for
    //   ActionProvider(android.content.Context context)
    if (parameterCount < 1 || parameterCount > 3) {
      return false;
    }

    PsiParameter[] parameters = parameterList.getParameters();
    PsiType type = parameters[0].getType();
    if (!(type instanceof PsiClassReferenceType)) {
      return false;
    }
    PsiClassReferenceType classType = (PsiClassReferenceType)type;
    PsiClass resolvedParameter = classType.resolve();
    if (resolvedParameter == null || !CLASS_CONTEXT.equals(resolvedParameter.getQualifiedName())) {
      return false;
    }

    if (parameterCount > 1) {
      type = parameters[1].getType();
      if (!(type instanceof PsiClassReferenceType)) {
        return false;
      }
      classType = (PsiClassReferenceType)type;
      resolvedParameter = classType.resolve();
      if (resolvedParameter == null || !CLASS_ATTRIBUTE_SET.equals(resolvedParameter.getQualifiedName())) {
        return false;
      }
      if (parameterCount > 2) {
        type = parameters[2].getType();
        if (!PsiType.INT.equals(type)) {
          return false;
        }
      }
    }

    final PsiClass aClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
    if (aClass == null) {
      return false;
    }

    PsiClass viewBaseClass = JavaPsiFacade.getInstance(aClass.getProject()).findClass(CLASS_VIEW, method.getResolveScope());
    if (viewBaseClass == null) {
      return false;
    }
    return aClass.isInheritor(viewBaseClass, true) || parameterCount == 1 && InheritanceUtil.isInheritor(aClass, CLASS_ACTION_PROVIDER);
  }
}
