import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';

@Injectable()
export class TimeService {
  now: Observable<Date>;

  constructor() {
    this.now = Observable.interval(1000).map(x => new Date()).share();
  }
}
