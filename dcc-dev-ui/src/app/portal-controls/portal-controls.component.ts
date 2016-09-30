import { Component, Input, OnChanges, SimpleChange } from '@angular/core';
import { PortalService } from '../portal-service';
import { TimeService } from './time-service';
import { PortalOptions } from './portal-options/portal-options.component';
import { get, map, zipObject, without } from 'lodash';
import * as moment from 'moment';

@Component({
  selector: 'portal-controls',
  templateUrl: './portal-controls.html',
  styleUrls: [ './portal-controls.style.scss' ],
  directives: [
    PortalOptions
  ],
  providers: [TimeService]
})
export class PortalControls {
  @Input()
  portal: any;

  @Input()
  pr: any;

  @Input()
  artifact: any;

  @Input()
  build: any;

  @Input()
  ticket: any;

  isProcessing: Boolean;
  portalOptions: any = {};
  shouldShowAdvanced = false;

  // TODO: rename..
  logsFromRestEndpoint: any = {};
  logsFromWebsocket = [];

  get logsFromWebsocketAfterLogsFromRestEndpoint() {
    const demarcation = this.logsFromRestEndpoint.timestamp || 0;
    return this.logsFromWebsocket.filter(log => log.timestamp > demarcation);
  }

  constructor (public portalService: PortalService, public timeService: TimeService) {}

  start = () => {
    this.isProcessing = true;
    return this.portalService.createPortal(this.pr.number, this.portalOptions);
  };

  delete = () => {
    this.isProcessing = true;
    return this.portalService.deletePortal(this.portal.id);
  };

  update = () => {
    this.isProcessing = true;
    return this.portalService.updatePortal(this.portal.id, this.portalOptions);
  }

  requestLogs = () => {
    this.portalService.fetchPortalLog(this.portal.id)
      .subscribe( data => this.logsFromRestEndpoint = data );

    this.portalService.subscribePortalLog(this.portal.id, (message) => {
      this.logsFromWebsocket.push(JSON.parse(message.body));
    });
  };

  handlePortalOptionsChange = (options) => {
    this.portalOptions = options;
  }

  get formattedLastUpdateTime() {
    return this.portal && moment(moment(this.portal.updated, 'x')).from(this.timeService.now);
  }
}
