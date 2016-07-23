package org.icgc.dcc.dev.jenkins;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JenkinsBuild {

  /**
   * Build number.
   * <p>
   * Primary key.
   */
  int number;

  Integer queueId;
  String url;
  Long timestamp;

}