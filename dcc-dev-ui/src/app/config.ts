export const API_ROOT = window.location.hostname === 'local.dcc.icgc.org'
  ? 'https://dev.dcc.icgc.org'
  : '';

export const REST_ROOT = `${this.API_ROOT}/api`;

export const WEBSOCKET_ROOT = `${this.API_ROOT}/messages`;