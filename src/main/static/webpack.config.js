const path = require("path");
const webpack = require("webpack");

module.exports = {
  entry: "./src/spa.jsx",
  mode: "development",
  module: {
    rules: [
      {
        test: /\.(js|jsx|ts|tsx)$/,
        exclude: /(node_modules|bower_components)/,
        use: [
          {
            loader: "babel-loader",
            options: { presets: ["@babel/env", "@babel/preset-react"] }
          },
          {
            loader: "ts-loader",
          },
        ],
      },
      {
        test: /\.css$/,
        use: ["style-loader", "css-loader"],
      },
    ]
  },
  resolve: { extensions: [".ts", ".tsx", ".js", ".jsx"] },
  output: {
    path: path.resolve(__dirname, "dist/"),
    publicPath: "/dist/",
    filename: "bundle.js"
  },
  devServer: {
    static: path.join(__dirname, "public/"),
    port: 3000,
    historyApiFallback: true,
    // publicPath: "http://localhost:3000/dist/",
    // hotOnly: true
  },
  plugins: [new webpack.HotModuleReplacementPlugin()]
};
