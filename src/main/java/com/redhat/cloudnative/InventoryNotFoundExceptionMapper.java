package com.redhat.cloudnative;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InventoryNotFoundExceptionMapper implements ExceptionMapper<InventoryNotFoundException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(InventoryNotFoundException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(Response.Status.NOT_FOUND.getStatusCode())
                .error("Not Found")
                .message(exception.getMessage())
                .path(uriInfo.getRequestUri().getPath())
                .build();

        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                .build();
    }
}