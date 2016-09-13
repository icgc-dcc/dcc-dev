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
package org.icgc.dcc.dev.server.portal;

import static lombok.AccessLevel.PRIVATE;

import java.util.Map;

import org.icgc.dcc.dev.server.jira.JiraTicket;

import com.github.slugify.Slugify;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * Encapsulates how portal fields should be updated.
 */
@NoArgsConstructor(access = PRIVATE)
final class PortalUpdates {

  @SneakyThrows
  public static String newSlug(String newSlug, String currentSlug, String newTitle, String currentTitle,
      String prTitle) {
    return new Slugify().slugify(newValue(newSlug, currentSlug, prTitle));
  }

  public static String newTitle(String newTitle, String currentTitle, String prTitle) {
    return newValue(newTitle, currentTitle, prTitle);
  }

  public static String newDescription(String newDescription, String currentDescription, String prDescription) {
    return newValue(newDescription, currentDescription, prDescription);
  }

  public static String newTicketKey(String newTicketKey, String currentTicketKey, JiraTicket currentTicket) {
    return newValue(newTicketKey, currentTicketKey, currentTicket != null ? currentTicket.getKey() : null);
  }

  public static Map<String, String> newConfig(Map<String, String> newConfig, Map<String, String> currentConfig) {
    return newValue(newConfig, currentConfig);
  }
  
  @SafeVarargs
  private static <T> T newValue(T... values) {
    for (T value : values)
      if (value != null) return value;
    
    return null;
  }

}
