var webpack = require("webpack");
module.exports = [{
    entry: "./browser-mui.js",
    externals: {
        "react": "React",
        "react-dom": "ReactDOM",
        "react-addons-create-fragment": "React.addons.createFragment",
        "react-addons-pure-render-mixin": "React.addons.PureRenderMixin",
        "react-addons-transition-group": "React.addons.TransitionGroup",
        "react-addons-update": "React.addons.update"
    },
    output: {
        path: "../target/classes/webnf/mui",
        filename: "material-ui.bundle.js"
    }
}, {
    entry: "./browser-mui.js",
    externals: {
        "react": "React",
        "react-dom": "ReactDOM",
        "react-addons-create-fragment": "React.addons.createFragment",
        "react-addons-pure-render-mixin": "React.addons.PureRenderMixin",
        "react-addons-transition-group": "React.addons.TransitionGroup",
        "react-addons-update": "React.addons.update"
    },
    output: {
        path: "../target/classes/webnf/mui",
        filename: "material-ui.bundle.min.js"
    },
    plugins: [
        new webpack.DefinePlugin({
            'process.env': {
                NODE_ENV: JSON.stringify('production')
            }
        }),
        new webpack.optimize.UglifyJsPlugin({
            output: {
                comments: false
            },
            compress: {
                warnings: false
            }
        }),
        new webpack.optimize.OccurenceOrderPlugin(),
        new webpack.optimize.DedupePlugin()
    ]
}];
