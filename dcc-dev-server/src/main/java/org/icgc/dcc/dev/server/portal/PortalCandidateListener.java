package org.icgc.dcc.dev.server.portal;

import static com.google.common.collect.Iterables.getLast;

import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.message.Messages.GithubPrsMessage;
import org.icgc.dcc.dev.server.message.Messages.JenkinsBuildsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener that handles upstream portal candidate events.
 */
@Slf4j
@Component
public class PortalCandidateListener {

  @Autowired
  PortalService portals;

  @EventListener
  public void handle(JenkinsBuildsMessage message) {
    val prBuilds = Multimaps.index(message.getBuilds(), JenkinsBuild::getPrNumber);

    for (val portal : portals.list()) {
      val prNumber = portal.getTarget().getPr().getNumber();
      val portalBuilds = prBuilds.get(prNumber);
      val latestBuild = getLast(portalBuilds);

      val buildNumber = portal.getTarget().getBuild().getNumber();
      if (latestBuild.getNumber() <= buildNumber) continue;

      log.info("Build update found for portal {}:  {}", portal.getId(), latestBuild);
      if (!portal.isAutoDeploy()) continue;

      log.info("Auto deploying portal {}: {}", portal.getId(), latestBuild);
      portal.getTarget().setBuild(latestBuild);
      portals.update(portal);
    }
  }

  @EventListener
  public void handle(GithubPrsMessage message) {
    val openPrNumbers = Maps.uniqueIndex(message.getPrs(), GithubPr::getNumber);

    for (val portal : portals.list()) {
      val prNumber = portal.getTarget().getPr().getNumber();
      val prOpen = openPrNumbers.containsKey(prNumber);
      if (prOpen) continue;

      log.info("Closed PR found for portal {}", portal.getId());
      if (!portal.isAutoRemove()) continue;

      log.info("Auto removing portal {}", portal.getId());
      portals.remove(portal.getId());
    }
  }

}
