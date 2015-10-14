/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.sdk;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.text.StringUtil;
import icons.AndroidIcons;
import org.consulo.lombok.annotations.LazyInstance;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidSdkType extends SdkType {
  @NotNull
  @LazyInstance
  public static AndroidSdkType getInstance() {
    return EP_NAME.findExtension(AndroidSdkType.class);
  }

  @NonNls
  public static final String DEFAULT_EXTERNAL_DOCUMENTATION_URL = "http://developer.android.com/reference/";

  private static final String SOURCE_FILE = "source.properties";

  public AndroidSdkType() {
    super("ANDROID_SDK");
  }

  @Override
  public boolean isValidSdkHome(@Nullable String path) {
    if (StringUtil.isEmpty(path)) {
      return false;
    }
    File sourceFile = new File(path, SOURCE_FILE);
    return sourceFile.exists();
  }

  @Nullable
  @Override
  public String getVersionString(String sdkHome) {
    Map<Object, Object> properties = loadProperties(sdkHome);
    Object apiLevel = properties.get("AndroidVersion.ApiLevel");
    return apiLevel != null ? apiLevel.toString() : null;
  }

  @Override
  @NotNull
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    Map<Object, Object> properties = loadProperties(sdkHome);
    Object desc = properties.get("Pkg.Desc");
    return desc != null ? desc.toString() : "Android SDK";
  }

  private static Map<Object, Object> loadProperties(@NotNull String home) {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(new File(home, SOURCE_FILE)));
      return properties;
    }
    catch (IOException e) {
      return Collections.emptyMap();
    }
  }

  /*@Override
  public boolean setupSdkPaths(@NotNull Sdk sdk, @NotNull SdkModel sdkModel) {
    final List<String> javaSdks = Lists.newArrayList();
    final Sdk[] sdks = sdkModel.getSdks();
    for (Sdk jdk : sdks) {
      if (Jdks.isApplicableJdk(jdk)) {
        javaSdks.add(jdk.getName());
      }
    }

    if (javaSdks.isEmpty()) {
      Messages.showErrorDialog(AndroidBundle.message("no.jdk.for.android.found.error"), "No Java SDK Found");
      return false;
    }

    MessageBuildingSdkLog log = new MessageBuildingSdkLog();
    AndroidSdkData sdkData = getSdkData(sdk);

    if (sdkData == null) {
      String errorMessage = !log.getErrorMessage().isEmpty() ? log.getErrorMessage() : AndroidBundle.message("cannot.parse.sdk.error");
      Messages.showErrorDialog(errorMessage, "SDK Parsing Error");
      return false;
    }

    IAndroidTarget[] targets = sdkData.getTargets();

    if (targets.length == 0) {
      if (Messages.showOkCancelDialog(AndroidBundle.message("no.android.targets.error"), CommonBundle.getErrorTitle(),
                                      "Open SDK Manager", Messages.CANCEL_BUTTON, Messages.getErrorIcon()) == Messages.OK) {
        RunAndroidSdkManagerAction.runSpecificSdkManager(null, sdkData.getLocation());
      }
      return false;
    }

    String[] targetNames = new String[targets.length];

    String newestPlatform = null;
    AndroidVersion version = null;

    for (int i = 0; i < targets.length; i++) {
      IAndroidTarget target = targets[i];
      String targetName = getTargetPresentableName(target);
      targetNames[i] = targetName;
      if (target.isPlatform() && (version == null || target.getVersion().compareTo(version) > 0)) {
        newestPlatform = targetName;
        version = target.getVersion();
      }
    }

    AndroidNewSdkDialog dialog = new AndroidNewSdkDialog(null, javaSdks, javaSdks.get(0), Arrays.asList(targetNames),
                                                         newestPlatform != null ? newestPlatform : targetNames[0]);
    if (!dialog.showAndGet()) {
      return false;
    }
    String name = javaSdks.get(dialog.getSelectedJavaSdkIndex());
    Sdk jdk = sdkModel.findSdk(name);
    IAndroidTarget target = targets[dialog.getSelectedTargetIndex()];
    String sdkName = chooseNameForNewLibrary(target);
    setUpSdk(sdk, sdkName, sdks, target, jdk, true);

    return true;
  } */

  @Override
  @NotNull
  public String getPresentableName() {
    return AndroidBundle.message("android.sdk.presentable.name");
  }

  @Override
  @NotNull
  public Icon getIcon() {
    return AndroidIcons.Android;
  }

  @Override
  @NotNull
  public String getDefaultDocumentationUrl(@NotNull Sdk sdk) {
    return DEFAULT_EXTERNAL_DOCUMENTATION_URL;
  }

  @Override
  public boolean isRootTypeApplicable(OrderRootType type) {
    return JavaSdk.getInstance().isRootTypeApplicable(type);
  }
}
