package com.github.ksewen.uuid.enums;

import com.github.ksewen.uuid.benchmark.KUIDBenchmark;
import com.github.ksewen.uuid.benchmark.UUIDBenchmark;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ksewen
 * @date 2019-12-1216:21
 */
public enum CaseType {
  UUID(UUIDBenchmark.class.getSimpleName()),
  KUID(KUIDBenchmark.class.getSimpleName());

  private final String type;

  CaseType(String type) {
    this.type = type;
  }

  public static CaseType forName(String name) {
    for (CaseType value : values()) {
      if (StringUtils.equalsIgnoreCase(value.name(), name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("type not exist");
  }

  public String getType() {
    return this.type;
  }
}
