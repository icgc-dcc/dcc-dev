import { Component, Input } from '@angular/core';

import { AppState } from '../app.service';
import { XLarge } from './x-large';

import { Http } from '@angular/http';

import { PortalService } from '../portal-service';
import { PortalControls } from '../portal-controls';

import { PullRequest, Candidate, Portal }  from '../interfaces';
import { includes } from 'lodash';

@Component({
  // The selector is what angular internally uses
  // for `document.querySelectorAll(selector)` in our index.html
  // where, in this case, selector is the string 'home'
  selector: 'home',  // <home></home>
  // We need to tell Angular's Dependency Injection which providers are in our app.
  providers: [ PortalService ],
  // We need to tell Angular's compiler which directives are in our template.
  // Doing so will allow Angular to attach our behavior to an element
  directives: [
    XLarge,
    PortalControls
  ],
  // We need to tell Angular's compiler which custom pipes are in our template.
  pipes: [ ],
  // Our list of styles in our component. We may add more to compose many styles together
  styleUrls: [ './home.style.css' ],
  // Every Angular template is first compiled by the browser before Angular runs it's compiler
  templateUrl: './home.template.html'
})
export class Home {
  // TypeScript public modifiers
  constructor(public portalService: PortalService) {

  }

  ngOnInit() {
    console.log('hello `Home` component');
  }

  get candidates(): Array<Candidate> {
    return this.portalService.candidates;
  }

  get portals(): Array<Portal> {
    return this.portalService.portals;
  }

  get portalsWithoutPRs(): Array<Portal> {
    const prNumbers = this.candidates.map(candidate => candidate.pr.number);
    return this.portals.filter((portal: Portal) => !includes(prNumbers, portal.target.pr.number));
  }

  getCandidatePortal = (candidate: Candidate) => {
    return this.portals.find(p => p.target.pr.number === candidate.pr.number);
  }
}
