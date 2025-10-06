#!/bin/sh

webpack
scp dist/bundle.js root@ifgapcar.local:/etc/ifgapcar/public/dist/bundle.js
scp public/index.html root@ifgapcar.local:/etc/ifgapcar/public/index.html
