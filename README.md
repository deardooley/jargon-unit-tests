# Jargon/IRODS 4.x Unit Tests
-------

This repository contains tests to validate behavioral changes and edge case behavior of the [Jargon](https://github.com/DICE-UNC/jargon) SDK against the 4.x branch of IRODS. 

## Running the tests

Tests should be run using the included `docker-compose.yml` file. In doing so, the build and test environment is consistent and as all tests run against a linked IRODS 4 container accessible only from the test container. 

```
$ docker-compose -f docker-compose.test.yml up
``` 

## Configuration

The connection parameters of the IRODS server used to test is specified in the container environment. To change this value, edit the `docker-compose.test.yml` by changing the values of the `IRODS_HOST`, `IRODS_PORT`, `IRODS_USERNAME`, `IRODS_PASSWORD`, `IRODS_RESOURCE`, and `IRODS_ZONE` variables.

## Output

You may view test results in two ways. First, by watching the output of the container. The output of the maven build and test actions will be written to stdout when as container runs. Here you will see every test case and result as they occur.

If you are automating these tests, then you can also check the results offline by looking in the `target/surefire-reports` directory of the repository. The repository directory is volume mounted into the container at runtime, so you can check the standard maven surefire location for the test results.

