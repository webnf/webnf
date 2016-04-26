var injectTapEventPlugin = require("react-tap-event-plugin");
injectTapEventPlugin();
window.React = require("react");
window.ReactDOM = require("react-dom");
window.ReactMUI = require("material-ui");
window.ReactMUI_styles = require("material-ui/styles");
// TODO separate bundle for SVG icons
// window.ReactMUI_SvgIcons = require("material-ui/svg-icons");
window.ReactAddons_createFragment = require("react-addons-create-fragment");
module.exports = {};
