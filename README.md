# Gossip-10

This project contains the implementation of the Gossip module of them team Gossip-10.

## Installation Requirements

This project was written in Java using Gradle as the tool project and dependency management.
Please install these tools accordingly.

## Building

The project can be built by issuing the following command:
```
gradle :classes :testClasses
```

## Testing

Tests can be executed by issuing the following command:
```
gradle :test
```

Test coverage can be collected by issuing the following command:
```
gradle jacocoTestReport
```

After the execution, code coverage results are available to view when opening the file
`./build/reports/jacoco/test/html/index.html` in your favorite browser.

## Running

The program can be run through gradle as well using the following command. Command line
arguments can be supplied via the optional `--args` argument of the gradle utility:
```
gradle :run --args='...'
```

When supplying the `--help` command line argument, the program prints the help menu:
```
Usage: gossip [options] [command] [command options]
  Options:
    -h, --help
      Shows the help menu.
  Commands:
    run      Runs the gossip application (Default).
      Usage: run [options]
        Options:
        * -c, --configuration
            Path string to the Windows INI configuration file.
          -s, --storage
            The location of the identity storage folder. (Default 
            './identities'). 

    generate      Generate identities for the gossip module.
      Usage: generate [options]
        Options:
          --address
            The remote address expected from all the generated identities 
            (Default: '127.0.0.1').
            Default: 127.0.0.1
          -a, --amount
            The amount of entities to generate.
            Default: 1
        * --start-port
            The port to use for the identities. Increments by one for every 
            additional identity (Default: 7000).
            Default: 7000
          -s, --storage
            The location of the identity storage folder. (Default 
            './identities'). 

    import      Imports a .pem public key file as an identity.
      Usage: import [options]
        Options:
        * --address
            The remote address expected from the identity.
        * -i, --input
            The input .pem file.
        * --port
            The remote port expected from the identity.
          -s, --storage
            The location of the identity storage folder. (Default 
            './identities'). 
```

There are three subcommands available which are all described in the following sections.

The program currently runs in `DEBUG` mode by default, to make it easier to see what happens under the hood!

### run

`run` is the default subcommand (meaning it is executed when no explicit sub command is provided).
It expects the `-c` parameter supplied with the INI configuration file and then starts the gossip application.
Everything about Gossip itself it described in great detail in the end-term report.

### generate

The `generate` subcommand can be used to generate identities in an identity storage folder (by default `./identities`).
An identity consist of the public part of the hostkey, hostname and a port to connect to the remote peer.

Refer to the help menu for an explanation on the required command line parameters.

### import

The `import` subcommand uses a keypair stored inside a provided `.pem` file to import it as a new identity in our
identity storage folder. The file can either contain a `RSA PUBLIC KEY` or a `RSA PRIVATE KEY` (e.g. the host key file).

Refer to the help menu for an explanation on the required command line parameters.

### Further utilities

The utility currently also provides a `generateHostKey.sh` script which can be used to easily generate host keys for testing purposes.
