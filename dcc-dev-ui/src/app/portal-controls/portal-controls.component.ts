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

  constructor (public portalService: PortalService) {}

  // TODO: this component should only send signals up, and not make the actual http request
  // The signals would trigger reqeusts and then model updates
  start = () => {
    return this.portalService.createPortal(this.prNumber);
  };
  stop = () => {};
  restart = () => {};
  delete = () => {
    return this.portalService.deletePortal(this.portal.id);
  };

  get transformedUrl() {
    return 'https://dev.dcc.icgc.org:' + this.portal.url.split(':')[2];
  }
}
