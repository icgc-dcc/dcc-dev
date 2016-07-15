package org.icgc.dcc.dev.jenkins;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.MavenBuild;
import com.offbytwo.jenkins.model.MavenJobWithDetails;

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
    return builds().map(this::convert).collect(toList());
  }

  @SneakyThrows
  public JenkinsBuild getBuild(String buildNumber) {
    val value = Integer.valueOf(buildNumber);
    val defaultValue = new JenkinsBuild().setNumber(value);
    
    return builds().filter(b -> b.getNumber() == value).findFirst().map(this::convert).orElse(defaultValue);
  }

  @SneakyThrows
  private MavenJobWithDetails getJob() {
    return jenkins.getMavenJob(jobName);
  }

  private Stream<MavenBuild> builds() {
    return getJob().getBuilds().stream();
  }

  @SneakyThrows
  private JenkinsBuild convert(MavenBuild build) {
    return new JenkinsBuild()
        .setNumber(build.getNumber())
        .setQueueId(build.getQueueId())
        .setUrl(build.getUrl())
        .setTimestamp(build.details().getTimestamp());
  }

}
