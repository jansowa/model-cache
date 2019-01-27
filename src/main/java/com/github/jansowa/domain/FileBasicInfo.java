package com.github.jansowa.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.Serializable;
import java.util.Date;

@Value
@Builder
public class FileBasicInfo implements Serializable {
    String name;
    @Wither String filePath;
    String extension;
    String url;
    Date creationTime;
    @Wither Date lastUsageTime;
}
