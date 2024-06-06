# Disaster recovery frontend 

This has two components.

## Builder
This processes incoming update messages and adds file information to a sqlite database.

## Webapp
This reads the database to allow users to search and download the files.

## Running

```shell
sbt assembly
docker build -t webapp -f Dockerfile-webapp .
docker build -t webapp -f Dockerfile-builder .
docker run --name webapp -p 8080:8080 -v /host/path/to/db/file:/root/database  -v /host/path/to/ocfl/repo:/root/repo -e DATABASE_PATH=/root/database/assets webapp 
docker build -f Dockerfile-builder -t builder . && docker run --name builder -v /host/path/to/db/file:/root/database -v /host/path/to/ocfl/repo:/root/repo -v /host/path/to/ocfl/work:/root/work -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN -e OCFL_WORK_DIR=/root/work -e OCFL_REPO_DIR=/root/repo -e QUEUE_URL=https://sqs.eu-west-2.amazonaws.com/1234567890/update-queue -e DATABASE_PATH=/root/database/assets builder
```

This is the result of a spike but we may re-use this code in future.