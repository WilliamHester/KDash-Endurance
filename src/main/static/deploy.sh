#!/bin/sh

webpack
scp dist/bundle.js williamhester@macmini.local:/Users/williamhester/kdash-live/public/dist
scp public/index.html macmini.local:~/kdash-live/public/
