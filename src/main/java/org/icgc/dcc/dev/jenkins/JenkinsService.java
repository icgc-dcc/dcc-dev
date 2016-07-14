package org.icgc.dcc.dev.jenkins;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JenkinsService {

  @Value("${jenkins.jobName}")
  String jobName;
  @Autowired
  JenkinsServer jenkins;
  
  @Scheduled(cron = "${jenkins.cron}")
  public void poll() {
    val build = convert(getJob().getLastStableBuild());
    log.info("Build: {}", build);
  }

  @SneakyThrows
  public List<JenkinsBuild> getBuilds() {
    return getJob().getAllBuilds().stream().map(this::convert).collect(toList());
  }

  @SneakyThrows
  private JobWithDetails getJob() {
    return jenkins.getJob(jobName);
  }

  @SneakyThrows
  private JenkinsBuild convert(Build build) {
    return new JenkinsBuild()
        .setNumber(build.getNumber())
        .setQueueId(build.getQueueId())
        .setUrl(build.getUrl())
        .setTimestamp(build.details().getTimestamp());
  }

}
