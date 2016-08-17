import { Injectable, ApplicationRef, NgZone } from '@angular/core';
import { Http, Headers, URLSearchParams } from '@angular/http';
import { PullRequest, Candidate, Portal }  from '../interfaces';
import * as SockJS from 'sockjs-client';
import { Stomp } from 'stompjs/lib/stomp.js';

const socket = new SockJS('http://dev.dcc.icgc.org:9000/messages');
const stompClient = Stomp.over(socket);

@Injectable()
export class PortalService {
  candidates: Array<Candidate> = [];
  portals: Array<Portal> = [];

  constructor(
    public http: Http,
    private ref: ApplicationRef,
    private _ngZone: NgZone
  ) {

    this.updateCandidates();
    this.updatePortals();

    stompClient.connect({}, (frame) => {
      console.log('Connected: ' + frame);

      const handlePortalMessage = (message) => {
        const {portalId, type} = JSON.parse(message.body);
        if (type !== 'EXECUTION') {
          this._ngZone.run( () => {
            this.updatePortals();
            this.updateCandidates();
          });
        }
      }

      // stompClient.subscribe('/topic/portal/state', onState.bind(null, '/state'));
      // stompClient.subscribe('/topic/portal/execute', onState.bind(null, '/execute'));
      // stompClient.subscribe('/topic/logs/1', onState.bind(null, '/logs'));
      // stompClient.subscribe('/topic/builds', onState.bind(null, '/builds'));
      stompClient.subscribe('/topic/portal', handlePortalMessage);
      // stompClient.subscribe('/topic/portal/state', onState);
      // stompClient.subscribe('/topic/portal/execute', onState);
      // stompClient.subscribe('/topic/logs/1', onState);
      // stompClient.subscribe('/topic/builds', onState);
    });
  }

  

  createPortal = (prNumber) => {
    return this.http.post('http://dev.dcc.icgc.org:9000/api/portals', `prNumber=${prNumber}`, {
      headers: new Headers({
        'Content-Type': 'application/x-www-form-urlencoded'
      })
    })
      .map(res => res.json())
      .subscribe();
  }

  deletePortal = (portalId) => {
    return this.http.delete(`http://dev.dcc.icgc.org:9000/api/portals/${portalId}`)
      .subscribe();
  }

  private fetchCandidates = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/api/candidates')
      .map(res => res.json());
  }

  private fetchPortals = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/api/portals')
      .map(res => res.json());
  }

  private updatePortals = () => {
    return this.fetchPortals().subscribe( data => this.portals = data );
  }

  private updateCandidates = () => {
    return this.fetchCandidates().subscribe( data => this.candidates = data);
  }

}
