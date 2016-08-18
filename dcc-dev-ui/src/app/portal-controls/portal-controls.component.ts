import { Component, Input, OnChanges, SimpleChange } from '@angular/core';
import { PortalService } from '../portal-service';
import { get, map, zipObject, without } from 'lodash';

@Component({
  selector: 'portal-controls',
  templateUrl: './portal-controls.html',
  styleUrls: [ './portal-controls.style.css' ],
})
export class PortalControls {
  @Input()
  portal: any;
  @Input()
  prNumber: String;
  isProcessing: Boolean;

  configEntries: Array<any> = [{name: '', value: ''}];
  get serializedConfig() {
    const entries = this.configEntries.filter(x => x.name && x.value);
    return JSON.stringify(zipObject(
      entries.map(x => x.name),
      entries.map(x => x.value)
    ));
  }

  // TODO: rename..
  logsFromRestEndpoint = {};
  logsFromWebsocket = [];

  get logsFromWebsocketAfterLogsFromRestEndpoint() {
    const demarcation = this.logsFromRestEndpoint.timestamp || 0;
    return this.logsFromWebsocket.filter(log => log.timestamp > demarcation);
  }

  constructor (public portalService: PortalService) {}

  ngOnChanges(changes: {[propKey: string]: SimpleChange}) {
    const configChanges = get(changes, 'portal.currentValue.config');
    // TODO: may want to somehow check if someone is currently editing the config
    // prevents an endpoint pull triggered by someone else
    // causing currently being edited configs to be reset 
    if (configChanges && Object.keys(configChanges).length) {
      // this.config = configChanges;
      this.configEntries = map(configChanges, (value, key) => ({name: key, value}));
    }
  }

  start = () => {
    this.isProcessing = true;
    return this.portalService.createPortal(this.prNumber, {config: this.serializedConfig});
  };

  delete = () => {
    this.isProcessing = true;
    return this.portalService.deletePortal(this.portal.id);
  };

  update = () => {
    this.isProcessing = true;
    return this.portalService.updatePortal(this.portal.id, {config: this.serializedConfig});
  }

  requestLogs = () => {
    this.portalService.fetchPortalLog(this.portal.id)
      .subscribe( data => this.logsFromRestEndpoint = data );

    this.portalService.subscribePortalLog(this.portal.id, (message) => {
      this.logsFromWebsocket.push(JSON.parse(message.body));
    });
  };

  addConfigEntry = () => {
    this.configEntries.push({name: '', value: ''});
  };
  
  removeConfigEntry = (entry) => {
    this.configEntries = without(this.configEntries, entry);
  };

  get transformedUrl() {
    return 'https://dev.dcc.icgc.org:' + this.portal.url.split(':')[2];
  }
}
