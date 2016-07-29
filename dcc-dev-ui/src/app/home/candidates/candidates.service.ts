import { Injectable } from '@angular/core';
import { Http } from '@angular/http';

@Injectable()
export class Candidates {
  value = 'Angular 2';

  constructor(public http: Http) {

  }

  getData = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/candidates')
      .map(res => res.json());
  }

}
