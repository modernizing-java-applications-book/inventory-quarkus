package com.redhat.cloudnative;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidInventoryExceptionMapper implements ExceptionMapper<InvalidInventoryException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidInventoryException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(Response.Status.BAD_REQUEST.getStatusCode())
                .error("Bad Request")
                .message(exception.getMessage())
                .path(uriInfo.getRequestUri().getPath())
                .build();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                .build();
    }
}