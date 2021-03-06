import { Component, Input, OnInit } from '@angular/core';
import { get, map, zipObject, without, pick } from 'lodash';
import { PortalService } from '../../portal-service';

@Component({
  selector: 'portal-options',
  templateUrl: './portal-options.html',
  styleUrls: [ './portal-options.style.scss' ],
})
export class PortalOptions implements OnInit {
  @Input()
  title: string;

  @Input()
  description: string;

  @Input()
  autoDeploy: boolean = true;

  @Input()
  autoRefresh: boolean = true;

  @Input()
  autoRemove: boolean = true;

  @Input()
  ticketKey: string;

  @Input()
  config: any = {};

  @Input()
  configEntries: Array<any> = [{name: '', value: ''}];

  @Input()
  onChange: Function;

  @Input()
  shouldShowAdvanced: boolean = false;

  constructor (public portalService: PortalService) {}

  ngOnInit() {
    this.configEntries = map(this.config, (value, name) => ({name, value}));
  }

  change = () => {
    this.onChange && this.onChange(this.optionsValue);
  };

  get serializedConfig() {
    const entries = this.configEntries.filter(x => x.name && x.value);
    return JSON.stringify(zipObject(
      entries.map(x => x.name),
      entries.map(x => x.value)
    ));
  }

  get optionsValue() {
    return Object.assign({},
      pick(this, 'title', 'description', 'autoDeploy', 'autoRefresh', 'autoRemove'),
      { config: this.serializedConfig });
  }

  addConfigEntry = () => {
    this.configEntries.push({ name: '', value: '' });
  };

  removeConfigEntry = (entry) => {
    this.configEntries = without(this.configEntries, entry);
  };

}
