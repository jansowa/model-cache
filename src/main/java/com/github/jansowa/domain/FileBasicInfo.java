package com.github.jansowa.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class FileBasicInfo {
    String name;
    String filePath;
    String extension;
    String url;
    Date creationTime;
    Date lastModifiedTime;
}
