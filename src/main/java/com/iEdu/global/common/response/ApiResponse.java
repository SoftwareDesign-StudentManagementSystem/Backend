package com.iEdu.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iEdu.global.exception.ReturnCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private ReturnCode returnCode;
    private String returnMessage;
    private T data;
    private IEduPage<T> iEduPage;

    public static <T> ApiResponse of(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.returnCode = ReturnCode.SUCCESS;
        response.returnMessage = ReturnCode.SUCCESS.getMessage();
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> of(IEduPage<T> iEduPage) {
        ApiResponse<T> response = new ApiResponse<>();
        response.returnCode = ReturnCode.SUCCESS;
        response.returnMessage = ReturnCode.SUCCESS.getMessage();
        response.iEduPage = iEduPage;
        return response;
    }

    public static <T> ApiResponse<T> of(ReturnCode returnCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.returnCode = returnCode;
        response.returnMessage = returnCode.getMessage();
        return response;
    }
}
