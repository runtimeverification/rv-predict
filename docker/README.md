# Docker for RV-Predict

## Build the docker image 

```bash 
cd rv-predict # git repo root directory 
docker build -t rv-predict-ubuntu -f ./docker/Dockerfile ./docker
```

## Run a docker container in bash

```bash
docker run -it rv-predict-ubuntu
```

## References

* [ How To Install and Use Docker on Ubuntu 16.04](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04)
* [ Docker Explained: Using Dockerfiles to Automate Building of Images ](https://www.digitalocean.com/community/tutorials/docker-explained-using-dockerfiles-to-automate-building-of-images)