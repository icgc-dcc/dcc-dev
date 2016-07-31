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

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.StringReader;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Portal module configuration for portal infrastructure.
 */
@Slf4j
@EnableAsync
@Configuration
public class PortalConfig {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${template.dir}")
  File templateDir;

  /**
   * Dependencies.
   */
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalService service;

  @Bean
  @SneakyThrows
  public Properties config(@Value("${config}") String text) {
    val config = new Properties();
    config.load(new StringReader(text));

    return config;
  }

  @PostConstruct
  public void init() {
    if (!workspaceDir.exists()) {
      log.info("Creating workspace...");
      checkState(workspaceDir.mkdirs(), "Could not create workspace dir %s", workspaceDir);
    }

    if (!templateDir.exists()) {
      log.info("Creating template...");
      deployer.setup();
    }
  }

  @Async
  @EventListener
  public void start(ApplicationReadyEvent event) {
    log.info("**** Started!");

    val portals = service.list();
    if (portals.isEmpty()) {
      return;
    }

    log.info("Restarting portals...");
    for (val portal : portals) {
      service.restart(portal.getId());
    }
  }

}
