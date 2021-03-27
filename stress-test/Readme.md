# Stress test for test course application

## Prerequisites

* install rust toolchain https://www.rust-lang.org/tools/install
* install docker https://docs.docker.com/engine/install/
* install lets task runner https://lets-cli.org/docs/getting_started

### Lets command guide

* `run` - launch full flow
* `build` - build rust (on Your machine) and python (docker image)
* `clean` - remove previous experiment data
* `test` - run all predefined tests
* `report` - run HTML report generation
* `test-<duration>-<rps>` (check all predefined) re-run one of experiments
