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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;

@Slf4j
@Service
public class JiraService {

  /**
   * Constants.
   */
  private static final String STATUS_FIELD_NAME = "status";
  private static final String TEST_STATUS = "Ready for testing";

  /**
   * Dependencies.
   */
  @Autowired
  JiraClient jira;

  public JiraTicket getTicket(@NonNull String key) {
    val issue = getIssue(key);

    return new JiraTicket()
        .setKey(key)
        .setTitle(issue.getSummary())
        .setStatus(issue.getStatus().getName())
        .setAssignee(issue.getAssignee().getName())
        .setUrl(issue.getUrl());
  }

  @SneakyThrows
  public void updateTicket(@NonNull String key, String comment) {
    val issue = getIssue(key);
    if (issue == null) {
      log.warn("Cannot find ticket {}", key);
      return;
    }
    
    if (!issue.getStatus().getName().equals(TEST_STATUS)) {
      issue.update().field(STATUS_FIELD_NAME, TEST_STATUS);
    }

    issue.addComment(comment);
  }

  @SneakyThrows
  private Issue getIssue(String key) {
    return jira.getIssue(key);
  }

}
