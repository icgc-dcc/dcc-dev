/// <reference path="globals/angular-protractor/index.d.ts" />
/// <reference path="globals/core-js/index.d.ts" />
/// <reference path="globals/hammerjs/index.d.ts" />
/// <reference path="globals/jasmine/index.d.ts" />
/// <reference path="globals/node/index.d.ts" />
/// <reference path="globals/selenium-webdriver/index.d.ts" />
/// <reference path="globals/source-map/index.d.ts" />
/// <reference path="globals/uglify-js/index.d.ts" />
/// <reference path="globals/webpack/index.d.ts" />
/// <reference path="modules/lodash/index.d.ts" />

declare module "sockjs-client" {
    var x: any;
    export = x;
}

declare module "stompjs/lib/stomp.js" {
    var x: any;
    export default x;
    export var Stomp;
}