import { Component, Input } from '@angular/core';

import { Http, Headers, URLSearchParams } from '@angular/http';

// TODO: move PortalControls into own folder
@Component({
  selector: 'portal-controls',
  templateUrl: './portal-controls.html'
})
export class PortalControls {
  @Input()
  portal: Object;
  @Input()
  prNumber: String;

  constructor (public http: Http) {}

  // TODO: this component should only send signals up, and not make the actual http request
  // The signals would trigger reqeusts and then model updates
  start = () => {
    return this.http.post('http://dev.dcc.icgc.org:9000/portals', `prNumber=${this.prNumber}`, {
      headers: new Headers({
        'Content-Type': 'application/x-www-form-urlencoded'
      })
    })
      .map(res => res.json())
      .subscribe(res => this.portal = res);
  };

  stop = () => {};
  restart = () => {};
  delete = () => {
    return this.http.delete(`http://dev.dcc.icgc.org:9000/portals/${this.portal.id}`)
      .subscribe(() => this.portal = undefined);
  };
}
