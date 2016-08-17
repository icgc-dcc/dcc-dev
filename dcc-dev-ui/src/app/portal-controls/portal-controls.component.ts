import { Component, Input } from '@angular/core';
import { PortalService } from '../portal-service';

// TODO: move PortalControls into own folder
@Component({
  selector: 'portal-controls',
  templateUrl: './portal-controls.html',
})
export class PortalControls {
  @Input()
  portal: any;
  @Input()
  prNumber: String;
  isProcessing: Boolean;

  // TODO: rename..
  logsFromRest = {};
  logsFromWebsocket = [];


  constructor (public portalService: PortalService) {}

  start = () => {
    this.isProcessing = true;
    return this.portalService.createPortal(this.prNumber);
  };
  stop = () => {};
  restart = () => {};
  delete = () => {
    this.isProcessing = true;
    return this.portalService.deletePortal(this.portal.id);
  };

  requestLogs = () => {
    this.portalService.fetchPortalLog(this.portal.id)
      .subscribe( data => this.logsFromRest = data );

    this.portalService.subscribePortalLog(this.portal.id, (message) => {
      this.logsFromWebsocket.push(JSON.parse(message.body));
    });
  }

  get transformedUrl() {
    return 'https://dev.dcc.icgc.org:' + this.portal.url.split(':')[2];
  }

  get logs() {
    return this.portalService.logsMap[this.portal.id];
  }
}
