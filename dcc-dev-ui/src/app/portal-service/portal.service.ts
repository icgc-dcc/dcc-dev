import { Injectable } from '@angular/core';
import { Http } from '@angular/http';

@Injectable()
export class Candidates {
  value = 'Angular 2';

  constructor(public http: Http) {

  }

  getCandidates = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/api/candidates')
      .map(res => res.json());
  }

}
