package com.github.rodbate.uid.web.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * User: jiangsongsong
 * Date: 2018/12/29
 * Time: 14:51
 */
@Getter
@Setter
public class LinkSecondaryIdToCookieIdRequest {
    @NotBlank
    private String cookieId;
    @NotNull
    private List<SecondaryIdEntry> secondaryIds;

    @Getter
    @Setter
    public static class SecondaryIdEntry {
        @NotBlank
        private String secondaryIdName;
        @NotBlank
        private String secondaryIdValue;
    }
}
