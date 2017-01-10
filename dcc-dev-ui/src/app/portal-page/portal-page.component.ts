import { Component, Input, OnChanges, SimpleChange, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { Http } from '@angular/http';
import { ActivatedRoute, Params } from '@angular/router';
import { get, map, zipObject, without } from 'lodash';
import * as moment from 'moment';
import * as _ from 'lodash';

import { PortalService } from '../portal-service';
import { PortalControls } from '../portal-controls';
import { PullRequest, Candidate, Portal, JiraComment }  from '../interfaces';

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
    <div
      *ngIf="_.get(portal, 'target.ticket')"
      class="jira-comments"
    >
      <div class="comment-controls">
        <label>
          <input type="checkbox" [(ngModel)]="shouldHideAutomatedComments">
          hide automated comments
        </label>
      </div>
      <div
        class="comment"
        *ngIf="isFetchingComments"
      >
        <i class="comment__avatar fa fa-spinner fa-spin"></i>
      </div>

      <div
        class="comment"
        *ngIf="!isFetchingComments && (!jiraComments || !jiraComments.length)"
      >
        No comments
      </div>

      <div
        class="comment"
        *ngFor="let comment of _.orderBy(_.filter(jiraComments, commentFilter), 'createdDate', 'desc')"
      >
        <span class="comment__avatar">
          <img src="{{comment.author.avatarUrls['24x24']}}"/>
        </span>

        <div class="comment__right-column">
          <div class="comment__body"> {{ comment.body }} </div>

          <div class="comment__info">
            <div class="comment__info__item">{{ comment.author.displayName }}</div>
            <div class="comment__info__item">
              <a
                target="blank"
                [href]="'https://jira.oicr.on.ca/browse/' + portal.target.ticket.key +'?focusedCommentId=' + comment.id"
              >
                {{ moment(comment.createdDate).fromNow() }}
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: [ './portal-page.style.scss' ],
  providers: [ PortalService ],
  directives: [ PortalControls ],
})
export class PortalPage implements OnInit {
  isFetchingComments: Boolean = false;
  portal: Portal;
  jiraComments: [JiraComment];
  moment = moment;
  _ = _;

  shouldHideAutomatedComments = true;

  constructor (
    private portalService: PortalService,
    private route: ActivatedRoute,
    public http: Http
    ) {}
  
  ngOnInit() {
    this.route.params
      .switchMap((params: Params) => this.portalService.fetchPortal(params['portalId']))
      .subscribe((portal: Portal) => {
        this.portal = portal;
        if (portal.target.ticket)
        this.fetchJiraComments();
      });
  }

  commentFilter = (comment) => {
    const isAutomatedComment = (comment) => ['DCC Jira Automation'].includes(comment.author.displayName);
    return _.every([
      this.shouldHideAutomatedComments ? !isAutomatedComment(comment) : true,
    ]);
  };

  fetchJiraComments = () => {
    this.isFetchingComments = true;
    this.portalService.fetchJiraComments(this.portal)
      .subscribe(
        (data) => {
          this.jiraComments = data;
          this.isFetchingComments = false;
        },
        (err) => err.status === 404 && setTimeout(this.fetchJiraComments, 3000)
        )
  }
}
