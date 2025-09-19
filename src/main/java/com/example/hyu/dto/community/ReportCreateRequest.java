package com.example.hyu.dto.community;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest (
        @NotNull
        Reason reason,

        @Size(max=2000)
        String description,

        @Size(max = 1000)
        String attachmentUrl
){
    public enum Reason { SPAM, ABUSE, SUICIDE, VIOLENCE, OTHER }
}
