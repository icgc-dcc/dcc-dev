import { Component, Input, OnChanges, SimpleChange } from '@angular/core';
import { PortalService } from '../portal-service';
import { get, map, zipObject, without } from 'lodash';

@Component({
  selector: 'portal-options-editor',
  template: ``
})
export class PortalOptionsEditor {
  @Input()
  title: string;

  @Input()
  slug: string;

  @Input()
  description: string;

  @Input()
  autoDeploy: boolean = false;

  @Input()
  autoRemove: boolean = false;

  @Input()
  configEntries: Array<any> = [{name: '', value: ''}];

  constructor () {}

  get serializedConfig() {
    const entries = this.configEntries.filter(x => x.name && x.value);
    return JSON.stringify(zipObject(
      entries.map(x => x.name),
      entries.map(x => x.value)
    ));
  }

  get optionsValue() {
    return {
      config: this.serializedConfig,
      autoDeploy: this.autoDeploy
    };
  }

  addConfigEntry = () => {
    this.configEntries.push({ name: '', value: '' });
  };

  removeConfigEntry = (entry) => {
    this.configEntries = without(this.configEntries, entry);
  };
}

@Component({
  selector: 'portal-controls',
  templateUrl: './portal-controls.html',
  styleUrls: [ './portal-controls.style.css' ],
})
export class PortalControls {
  @Input()
  portal: any;

  @Input()
  pr: any;

  @Input()
  build: any;

  isProcessing: Boolean;
  autoDeploy: Boolean = false;

  configEntries: Array<any> = [{name: '', value: ''}];
  get serializedConfig() {
    const entries = this.configEntries.filter(x => x.name && x.value);
    return JSON.stringify(zipObject(
      entries.map(x => x.name),
      entries.map(x => x.value)
    ));
  }

  ngOnChanges(changes: {[propKey: string]: SimpleChange}) {
    // const configChanges = get(changes, 'portal.currentValue.config');
    // // TODO: may want to somehow check if someone is currently editing the config
    // // prevents an endpoint pull triggered by someone else
    // // causing currently being edited configs to be reset 
    // if (configChanges && Object.keys(configChanges).length) {
    //   // this.config = configChanges;
    //   this.configEntries = map(configChanges, (value, key) => ({name: key, value}));
    // }
    const autoDeploy = get<Boolean>(changes, 'portal.currentValue.autoDeploy');
    this.autoDeploy = autoDeploy;

  }

  // TODO: rename..
  logsFromRestEndpoint: any = {};
  logsFromWebsocket = [];

  get logsFromWebsocketAfterLogsFromRestEndpoint() {
    const demarcation = this.logsFromRestEndpoint.timestamp || 0;
    return this.logsFromWebsocket.filter(log => log.timestamp > demarcation);
  }

  constructor (public portalService: PortalService) {}

  get portalOptions() {
    return {
      config: this.serializedConfig,
      autoDeploy: this.autoDeploy
    };
  }

  start = () => {
    this.isProcessing = true;
    console.log(this.portalOptions, this.autoDeploy);
    return this.portalService.createPortal(this.pr.number, this.portalOptions);
  };

  delete = () => {
    this.isProcessing = true;
    return this.portalService.deletePortal(this.portal.id);
  };

  update = () => {
    this.isProcessing = true;
    return this.portalService.updatePortal(this.portal.id, this.portalOptions);
  }

  requestLogs = () => {
    this.portalService.fetchPortalLog(this.portal.id)
      .subscribe( data => this.logsFromRestEndpoint = data );

    this.portalService.subscribePortalLog(this.portal.id, (message) => {
      this.logsFromWebsocket.push(JSON.parse(message.body));
    });
  };

  get transformedUrl() {
    return 'https://dev.dcc.icgc.org:9000/portals/' + this.portal.id;
  }
}
