package org.icgc.dcc.dev.jenkins;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JenkinsBuild {

  int number;
  int queueId;
  String url;
  long timestamp;

}