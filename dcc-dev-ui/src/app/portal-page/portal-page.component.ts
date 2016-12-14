import { Component, Input, OnChanges, SimpleChange, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { Http } from '@angular/http';
import { ActivatedRoute, Params } from '@angular/router';
import { get, map, zipObject, without } from 'lodash';
import * as moment from 'moment';

import { PortalService } from '../portal-service';
import { PortalControls } from '../portal-controls';
import { PullRequest, Candidate, Portal }  from '../interfaces';


@Component({
  selector: 'portal-page',
  template: `
    <portal-controls
      *ngIf="portal"
      [portal]="portal"
      [artifact]="portal.target.artifact"
      [build]="portal.target.build"
      [pr]="portal.target.pr"
      [ticket]="portal.target.ticket"
    ></portal-controls>
  `,
  providers: [ PortalService ],
  directives: [ PortalControls ],
})
export class PortalPage implements OnInit {
  portal: Portal;

  constructor (
    private portalService: PortalService,
    private route: ActivatedRoute
    ) {}
  
  ngOnInit() {
    this.route.params
      .switchMap((params: Params) => this.portalService.fetchPortal(params['portalId']))
      .subscribe((portal: Portal) => this.portal = portal);
  }
}
