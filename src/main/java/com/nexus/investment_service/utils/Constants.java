package com.nexus.investment_service.utils;

public class Constants {

    /**
     * The base URL for the external User Microservice (which handles Funder/Investor user details).
     * Replace "http://localhost:8081" with the actual service discovery name or host/port
     * when deploying to a containerized environment (e.g., Kubernetes, Eureka).
     */
    public static final String USER_SERVICE_BASE_URL = "http://localhost:3000/api/v1/users";


    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_FUNDED = "FUNDED";
    public static final String STATUS_CLOSED = "CLOSED";


}