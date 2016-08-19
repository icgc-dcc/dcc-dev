import { Component, Input } from '@angular/core';
import { get, map, zipObject, without, pick } from 'lodash';

@Component({
  selector: 'portal-options',
  templateUrl: './portal-options.html',
})
export class PortalOptions {
  @Input()
  title: string;

  @Input()
  description: string;

  @Input()
  autoDeploy: boolean = true;

  @Input()
  autoRemove: boolean = false;

  @Input()
  config: any = {};

  @Input()
  configEntries: Array<any> = [{name: '', value: ''}];

  @Input()
  onChange: Function;

  @Input()
  shouldShowConfig: boolean = false;

  constructor () {}

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
    // return {
    //   title: this.title,
    //   description: this.description,
    //   autoDeploy: this.autoDeploy,
    //   autoRemove: this.autoRemove,
    //   config: this.serializedConfig,
    // };
    return Object.assign({},
      pick(this, 'title', 'description', 'autoDeploy'),
      { config: this.serializedConfig });
  }

  addConfigEntry = () => {
    this.configEntries.push({ name: '', value: '' });
  };

  removeConfigEntry = (entry) => {
    this.configEntries = without(this.configEntries, entry);
  };
}