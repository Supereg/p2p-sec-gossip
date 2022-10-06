# Gossip-10

This project contains the implementation of the Gossip module of them team Gossip-10.

## Installation Requirements

This project was written in Java using Gradle as the tool project and dependency management.
Please install these tools accordingly.

## Building

The project can be built by issuing the following command:
```
gradle :assemble
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

**Running with Gradle:**  
The program can be run through gradle as well using the following command. Command line
arguments can be supplied via the optional `--args` argument of the gradle utility:
```
gradle :run --args='...'
```

**Running the JAR file:**  
Alternatively, the program can be run without gradle executing the actual JAR file via two options.

FatJar (added after the submission to ease the grading process of the instructors): You can run the program using 
a single JAR file (see [UberJar](https://stackoverflow.com/a/11947093) for more info). This was made possible with a
slight modification to the gradle build configuration. The FatJar/UberJar file is located at `./build/libs/Gossip-10-all.jar`.  
You can execute the JAR file (after executing the build command) by invoking the JVM: `java -jar ./build/libs/Gossip-10-all.jar`.

By default, gradle assembles the application by having every dependency packaged in its separate (e.g., preserving original code signing) JAR file.
To execute, gradle provides two script files (Windows and UNIX systems) to run Gossip and load the dependencies
from the individual dependency JAR files.
To do this, unpack the ZIP file located at `./build/distributions/Gossip-10.zip` after executing `gradle :assemble`.
On UNIX based systems, you can now simply run `./build/distributions/Gossip-10/bin/Gossip-10` appending command line
arguments as usual.

**Command Line Interface:**

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


## Build your own network

This project contains a default setup for a gossip p2p network of size 3.
Meaning there is a `./identities` PeerIdentityStorage folder with three pre-generated peer identities.
The respective hostkeys are stored in the `./Documents` folder.
That folder also contains three different config INI files.
Two configuring a gossip instance with degree 1 and one configuring a gossip instance with degree 2.

In order to start all peers execute the following commands each in their own terminal:
```
gradle run --args='run -c ./Documents/config1.ini'
gradle run --args='run -c ./Documents/config2.ini'
gradle run --args='run -c ./Documents/config3.ini'
```
After few seconds the network will stabilize and all nodes should be connected to each other.

Now you can use the python-based mock implementations for the gossip clients (the `gossip_client.py` script).
We execute the following two commands to listen for notifications on instance 2 and 3:
```
python3 gossip_client.py -d 127.0.0.1 -p 7003 --notify
python3 gossip_client.py -d 127.0.0.1 -p 7005 --notify
```

Now we can connect to instance 1 and send a notification.
```
python3 gossip_client.py -d 127.0.0.1 -p 7001 --announce
```

The other clients should have received the announced data and properly validated it.

### Extending the network

This chapter guides you through all the steps you have to take to extend your test setup with more nodes.

First of all we have to generate a new hostkey to identify our new gossip instance:
```
./generateHostKey.sh > ./Documents/hostkeyX.pem
```

We create a new `configX.ini` and configure the new instance accordingly.
Most notably, we set the new p2p_address port to 7006 and the api_address port to 7007.
We may adjust the other configuration parameters.

Now we import the new identity in our shared `./identities` folder.
Note, for simplicity we don't use a separate .pem file that only contains the hostkeys public key.
You should do that in production to not expose your private key!
Also, in this test setup, we use a shared `./identities` folder for simplicity.
In production, you have to do this import step several times for each distributed peer.
```
gradle run --args='import -i ./hostkeyX.pem --address 127.0.0.1 --port 7006'
```
Note that we used the port that we defined above in the import command.

Now we can start our new gossip instance as follows:
```
gradle run --args='run -c ./Documents/configX.ini'
```

The other instances must be restarted in order to refresh the list of known identities (more specifically a running instance
will be able to accept incoming connections from a peer that was placed in the `./identities` folder after application start.
However, the `GossipConnectionDispatcher` isn't updated and thus the application will never initiate a connection to this peer itself.).
