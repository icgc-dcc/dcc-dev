import { Component, Input, OnChanges, SimpleChange, OnInit } from '@angular/core';
import { Http } from '@angular/http';
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
export class PortalControls implements OnInit {
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
  buildCommitData = {};
  prHeadData = {};

  get logsFromWebsocketAfterLogsFromRestEndpoint() {
    const demarcation = this.logsFromRestEndpoint.timestamp || 0;
    return this.logsFromWebsocket.filter(log => log.timestamp > demarcation);
  }

  constructor (
    public portalService: PortalService,
    public timeService: TimeService,
    public http: Http
    ) {}

  ngOnInit () {
    this.build && this.updateBuildCommitData();
    this.pr && this.updatePRHeadData();
  }

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

  fetchCommitData = (commitId) => {
    return this.http.get(`https://api.github.com/repos/icgc-dcc/dcc-portal/git/commits/${commitId}`)
      .map(res => res.json());
  }

  fetchPRData = (prNumber) => {
    return this.http.get(`https://api.github.com/repos/icgc-dcc/dcc-portal/pulls/${prNumber}`)
      .map(res => res.json());
  }

  updateBuildCommitData = () => {
    this.fetchCommitData(this.build.commitId)
      .subscribe( data => this.buildCommitData = data );
  }

  updatePRHeadData = () => {
    this.fetchPRData(this.pr.number)
      .subscribe(data => {
        this.fetchCommitData(data.head.sha)
        .subscribe(commitData => this.prHeadData = commitData );
      });
  }

  get formattedLastUpdateTime() {
    return this.portal && moment(this.timeService.now).from(moment(this.portal.updated, 'x'));
  }
}
