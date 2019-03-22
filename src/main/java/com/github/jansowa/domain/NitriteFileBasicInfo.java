package com.github.jansowa.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.objects.Id;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NitriteFileBasicInfo {
    String name;
    @Id
    String filePath;
    String extension;
    String url;
    Date creationTime;
    Date lastUsageTime;
}
