package com.product.infrastructure.web.exception;

import com.product.infrastructure.web.exception.ApiError;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.product.domain.exception.ProductException;


@Provider
public class GlobalExceptionHandler implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException ex) {

        // 404 - Not Found
        if (ex instanceof ProductException e) {
            return build(
                    Response.Status.NOT_FOUND,
                    ErrorCode.INVENTORY_NOT_FOUND.getCode(),
                    e.getMessage());
        }

        // 409 - Business rule violation
        if (ex instanceof ProductException e) {
            return build(
                    Response.Status.CONFLICT,
                    ErrorCode.INSUFFICIENT_RESOURCES.getCode(),
                    e.getMessage());
        }

        // 406 - Business rule violation
        if (ex instanceof ProductException e) {
            return build(
                    Response.Status.NOT_ACCEPTABLE,
                    ErrorCode.INSUFFICIENT_RESOURCES_AVAILABLE.getCode(),
                    e.getMessage());
        }

        // 406 - Business rule violation
        if (ex instanceof ProductException e) {
            return build(
                    Response.Status.NOT_ACCEPTABLE,
                    ErrorCode.REDIS_RESOURCES_AVAILABLE.getCode(),
                    e.getMessage());
        }

        if (ex instanceof ProductException e) {
            return build(
                    Response.Status.NOT_ACCEPTABLE,
                    ErrorCode.HISTORICAL_RESOURCES_AVAILABLE.getCode(),
                    e.getMessage());
        }

        // 500 - unexpected errors
        return build(
                Response.Status.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.getCode(),
                ex.getMessage());
    }

    private Response build(Response.Status status, String code, String message) {
        return Response.status(status)
                .entity(new ApiError(code, message))
                .build();
    }
}