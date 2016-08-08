import { Component, Input } from '@angular/core';

import { AppState } from '../app.service';
import { XLarge } from './x-large';

import { Http } from '@angular/http';
import { PortalControls } from '../portal-controls';

interface PullRequest {
  avatarUrl: String;
  branch: String;
  description: String;
  head: String;
  number: Number;
  title: String;
  url: String;
  user: String;
}

interface Candidate {
  artifact: String;
  build: {
    commitId: String,
    number: Number,
    prNumber: Number,
    timestamp: Number,
    url: String
  };
  pr: PullRequest;
};

interface Portal {
  autoDeploy: Boolean,
  autoRemove: Boolean,
  description: String,
  id: String,
  properties: any,
  state: String, // maybe enum?
  systemProperties: {
    'management.port': String,
    'server.port': String
  },
  target: Candidate,
  ticket: any,
  url: String
};

@Component({
  // The selector is what angular internally uses
  // for `document.querySelectorAll(selector)` in our index.html
  // where, in this case, selector is the string 'home'
  selector: 'home',  // <home></home>
  // We need to tell Angular's Dependency Injection which providers are in our app.
  providers: [],
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
  // Set our default values
  localState = { value: '' };
  candidates: Array<Candidate>;
  portals: Array<Portal>;
  // TypeScript public modifiers
  constructor(public appState: AppState, public http: Http) {

  }

  ngOnInit() {
    console.log('hello `Home` component');
    this.fetchCandidates().subscribe(data => this.candidates = data);
    this.fetchPortals().subscribe(data => this.portals = data);
  }

  fetchCandidates = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/candidates')
      .map(res => res.json());
  }

  fetchPortals = () => {
    return this.http.get('http://dev.dcc.icgc.org:9000/portals')
      .map(res => res.json());
  }

  getCandidatePortal = (candidate: Candidate) => {
    return this.portals.find(p => p.target.pr.number === candidate.pr.number);
  }

  submitState(value) {
    console.log('submitState', value);
    this.appState.set('value', value);
    this.localState.value = '';
  }

}
