import { Injectable, ApplicationRef, NgZone } from '@angular/core';
import { Http, Headers, URLSearchParams } from '@angular/http';
import { PullRequest, Candidate, Portal }  from '../interfaces';
import * as SockJS from 'sockjs-client';
import { Stomp } from 'stompjs/lib/stomp.js';

const API_ROOT = 'https://dev.dcc.icgc.org:9000/api';
const socket = new SockJS(`https://dev.dcc.icgc.org:9000/messages`);
const stompClient = Stomp.over(socket);

@Injectable()
export class PortalService {
  candidates: Array<Candidate> = [];
  portals: Array<Portal> = [];
  logsMap: any = {};

  constructor(
    public http: Http,
    private ref: ApplicationRef,
    private _ngZone: NgZone
  ) {

    this.updateCandidates();
    this.updatePortals();

    stompClient.connect({}, (frame) => {
      console.log('Connected: ' + frame);


      // stompClient.subscribe('/topic/portal/state', onState.bind(null, '/state'));
      // stompClient.subscribe('/topic/portal/execute', onState.bind(null, '/execute'));
      // stompClient.subscribe('/topic/logs/1', onState.bind(null, '/logs'));
      // stompClient.subscribe('/topic/builds', onState.bind(null, '/builds'));
      stompClient.subscribe('/topic/portal', this.handlePortalStateMessage);
      // stompClient.subscribe('/topic/portal/state', onState);
      // stompClient.subscribe('/topic/portal/execute', onState);
      // stompClient.subscribe('/topic/logs/1', this.handlePortalLogMessage);
      // stompClient.subscribe('/topic/builds', onState);
    });
  }

  createPortal = (prNumber) => {
    return this.http.post(`${API_ROOT}/portals`, `prNumber=${prNumber}`, {
      headers: new Headers({
        'Content-Type': 'application/x-www-form-urlencoded'
      })
    })
      .subscribe();
  }

  deletePortal = (portalId) => {
    return this.http.delete(`${API_ROOT}/portals/${portalId}`)
      .subscribe();
  }

  fetchPortalLog = (portalId) => {
    return this.http.get(`${API_ROOT}/portals/${portalId}/log`)
      .map(res => ({
        timestamp: Number(res.headers.get('X-Server-Timestamp')),
        content: res.text(),
      }));
  };

  // TODO: make this an observable
  subscribePortalLog = (portalId, cb) => {
    // stompClient.subscribe(`/topic/logs/${portalId}`, this.handlePortalLogMessage);
    stompClient.subscribe(`/topic/logs/${portalId}`, (message) => {
      this._ngZone.run(() => cb(message));
    });
  };

  // private handlePortalLogMessage = (message) => {
  //   console.log(message);
  // };

  private handlePortalStateMessage = (message) => {
    const {portalId, type} = JSON.parse(message.body);
    if (type !== 'EXECUTION') {
      this._ngZone.run(() => {
        this.updatePortals();
        this.updateCandidates();
      });
    }
  };

  private fetchCandidates = () => {
    return this.http.get(`${API_ROOT}/candidates`)
      .map(res => res.json());
  }

  private fetchPortals = () => {
    return this.http.get(`${API_ROOT}/portals`)
      .map(res => res.json());
  }

  private updatePortals = () => {
    return this.fetchPortals().subscribe( data => this.portals = data );
  }

  private updateCandidates = () => {
    return this.fetchCandidates().subscribe( data => this.candidates = data);
  }

}
