// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.ant.dom;

import com.intellij.lang.ant.AntFilesProvider;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 */
public abstract class AntDomFilesProviderImpl extends AntDomElement implements AntFilesProvider{
  private volatile List<File> myCachedFiles;

  @Attribute("defaultexcludes")
  public abstract GenericAttributeValue<String> getDefaultExcludes();
  @Attribute("casesensitive")
  public abstract GenericAttributeValue<String> getCaseSensitive();


  @Override
  public final @NotNull List<File> getFiles(Set<AntFilesProvider> processed) {
    if (processed.contains(this)) {
      return Collections.emptyList();
    }
    List<File> result = myCachedFiles;
    if (result == null) {
      myCachedFiles = result = getFilesImpl(processed);
    }
    return result;
  }

  private List<File> getFilesImpl(final Set<AntFilesProvider> processed) {
    processed.add(this);
    try {
      final AntDomElement referenced = getRefId().getValue();
      if (referenced instanceof AntFilesProvider) {
        return ((AntFilesProvider)referenced).getFiles(processed);
      }
      return getFiles(getAntPattern(), processed);
    }
    finally {
      processed.remove(this);
    }
  }

  protected @Nullable AntDomPattern getAntPattern() {
    return AntDomPattern.create(this, shouldHonorDefaultExcludes(), matchPatternsCaseSensitive());
  }

  protected @NotNull List<File> getFiles(@Nullable AntDomPattern pattern, final Set<AntFilesProvider> processed) {
    return Collections.emptyList();
  }


  private boolean shouldHonorDefaultExcludes() {
    final @NonNls String value = getDefaultExcludes().getRawText();
    return value == null || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
  }

  private boolean matchPatternsCaseSensitive() {
    final @NonNls String value = getCaseSensitive().getRawText();
    return value == null || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
  }

  protected @Nullable File getCanonicalFile(final String path) {
    if (path == null) {
      return null;
    }
    try {
      final File file = new File(path);
      if (file.isAbsolute()) {
        return file.getCanonicalFile();
      }
      final String baseDir = getContextAntProject().getProjectBasedirPath();
      if (baseDir == null) {
        return null;
      }
      return new File(FileUtil.toCanonicalPath(new File(baseDir, path).getPath()));
    }
    catch (IOException e) {
      return null;
    }
  }

}
