#!/bin/zsh

# use the script like the following: `./generateHostKey.sh > outfile.pem`

openssl genrsa -out privatekey.pem 4096
openssl rsa -in privatekey.pem -out publickey.pem -pubout -outform PEM

cat privatekey.pem publickey.pem

rm privatekey.pem publickey.pem
