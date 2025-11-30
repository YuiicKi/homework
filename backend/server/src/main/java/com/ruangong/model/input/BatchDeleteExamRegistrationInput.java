package com.ruangong.model.input;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchDeleteExamRegistrationInput {

    @NotEmpty
    private List<Long> registrationIds;

    public List<Long> getRegistrationIds() {
        return registrationIds;
    }

    public void setRegistrationIds(List<Long> registrationIds) {
        this.registrationIds = registrationIds;
    }
}
