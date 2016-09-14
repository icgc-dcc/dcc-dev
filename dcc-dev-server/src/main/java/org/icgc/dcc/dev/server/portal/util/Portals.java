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
package org.icgc.dcc.dev.server.portal.util;

import static lombok.AccessLevel.PRIVATE;

import org.icgc.dcc.dev.server.portal.Portal;

import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Portal utilities.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Portals {

  /**
   * Defines an external URL when the portal is behind a reverse proxy / load balancer. E.g. shortUrl resource uses it
   * for generation of valid URLs.
   */
  public static final String WEB_BASE_URL_PROPERTY = "web.baseUrl";

  /**
   * Main web port property name of a running instance.
   */
  public static final String SERVER_PORT_PROPERTY = "server.port";

  /**
   * Admin web port property name of a running instance.
   */
  public static final String MANAGEMENT_PORT_PROPERTY = "management.port";

  public static String getServerPort(@NonNull Portal portal) {
    return portal.getSystemConfig().get(Portals.SERVER_PORT_PROPERTY);
  }

}
