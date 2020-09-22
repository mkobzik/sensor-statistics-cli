# sensor-statistics-cli
![build status](https://github.com/mkobzik/sensor-statistics-cli/workflows/build/badge.svg?branch=master)

Command line tool that calculates statistics from humidity sensor data.

## Building
```bash
sbt assembly
```
## Usage
```bash
$ java -jar sensor-statistics-cli.jar --help                                                                     
Usage: sensor-statistics-cli <report_directory_path>

Calculate statistics from humidity sensor data

Options and flags:
    --help
        Display this help text.
    --version, -v
        Print the version number and exit.
```
