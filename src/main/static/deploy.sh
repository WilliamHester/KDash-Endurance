#!/bin/sh

npm run build
rsync -av --progress build/* root@ifgapcar.local:/etc/ifgapcar/public/

