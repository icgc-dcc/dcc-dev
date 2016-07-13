package org.icgc.dcc.dev.slack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import lombok.SneakyThrows;

@Service
public class SlackService {

  @Autowired
  Slack slack;

  @SneakyThrows
  public void notify(SlackMessage message) {
    slack.push(message);
  }

}
