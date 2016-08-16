import { Injectable } from '@angular/core';
import { Http, Headers, URLSearchParams } from '@angular/http';
import { PullRequest, Candidate, Portal }  from '../interfaces';

@Injectable()
export class PortalService {
  candidates: Array<Candidate> = [];
  portals: Array<Portal> = [];

  constructor(public http: Http) {
    this.fetchCandidates().subscribe( data => this.candidates = data );
    this.fetchPortals().subscribe( data => this.portals = data );
  }

  fetchCandidates = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/api/candidates')
      .map(res => res.json());
  }

  fetchPortals = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/api/portals')
      .map(res => res.json());
  }

  createPortal = (prNumber) => {
    return this.http.post('http://dev.dcc.icgc.org:9000/api/portals', `prNumber=${prNumber}`, {
      headers: new Headers({
        'Content-Type': 'application/x-www-form-urlencoded'
      })
    })
      .map(res => res.json())
      .subscribe(res => this.portals = [...this.portals, res]);
  }

  deletePortal = (portalId) => {
    return this.http.delete(`http://dev.dcc.icgc.org:9000/api/portals/${portalId}`)
      .subscribe(() => this.portals = this.portals.filter( p => p.id !== portalId));
  }


}
