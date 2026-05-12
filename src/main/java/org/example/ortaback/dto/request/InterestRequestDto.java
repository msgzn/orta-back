package org.example.ortaback.dto.request;

import jakarta.validation.constraints.NotNull;

public record InterestRequestDto(

        @NotNull Long categoryId
) {}
