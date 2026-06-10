#!/bin/bash

mvn clean verify sonar:sonar \
  -Dsonar.projectKey=Kakak-Enterprises-Dev-Board_kakak-backend_aa62c8c0-5169-44a8-80ab-67f6d7dfb943 \
  -Dsonar.projectName=kakak-backend \
  -Dsonar.host.url=https://sonar.orque-ims.xyz \
  -Dsonar.token=sqp_e97ff7d7779394d0136737eba1986746db436616