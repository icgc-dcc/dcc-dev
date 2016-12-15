/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.dev.server.jira;

import static com.google.common.base.Strings.isNullOrEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;

/**
 * JIRA façade service.
 */
@Slf4j
@Service
public class JiraService {

  /**
   * Constants.
   */
  static final String STATUS_FIELD_NAME = "status";
  static final String STATUS_READY_FOR_TESTING = "Ready for testing";

  /**
   * Configuration.
   */
  @Value("${jira.update}")
  boolean update;

  /**
   * Dependencies.
   */
  @Autowired
  JiraClient jira;

  @Synchronized
  public JiraTicket getTicket(@NonNull String key) {
    val issue = getIssue(key);
    if (issue == null) return null;

    return new JiraTicket()
        .setKey(key)
        .setTitle(issue.getSummary())
        .setStatus(issue.getStatus().getName())
        .setAssignee(issue.getAssignee().getName())
        .setUrl(issue.getUrl());
  }

  @Synchronized
  @SneakyThrows
  public void updateTicket(@NonNull String key, String comment, boolean status) {
    if (!update) {
      log.debug("Updates disabled. Skipping update of ticket {}", key);
      return;
    }

    val issue = getIssue(key);
    if (issue == null) {
      log.warn("Cannot find ticket {}", key);
      return;
    }

    val testing = STATUS_READY_FOR_TESTING;
    val notTesting = !issue.getStatus().getName().equals(testing);
    if (status && notTesting) {
      log.info("Setting status to '{}'", testing);
      issue.update().field(STATUS_FIELD_NAME, testing);
    }

    if (!isNullOrEmpty(comment)) {
      issue.addComment(comment);
    }
  }

  @SneakyThrows
  private Issue getIssue(String key) {
    try {
      return jira.getIssue(key);
    } catch (Exception e) {
      log.error("Could not get issue for key: '{}': {}", key, e.getMessage());
      return null;
    }
  }

}
