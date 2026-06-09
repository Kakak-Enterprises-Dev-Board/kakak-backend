#!/bin/bash

mvn clean verify sonar:sonar \
-Dsonar.projectKey=Kakak \
-Dsonar.host.url=http://localhost:9000 \
-Dsonar.token=sqp_bab5d0f5a19cfead6b3036ad987535fdac7d2bd0