package com.github.rodbate.uid.web.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * User: jiangsongsong
 * Date: 2018/12/29
 * Time: 14:18
 */
@Getter
@Setter
public class ListIdBySecondaryIdsRequest {
    private String secondaryIdName;
    @NotNull
    private List<String> secondaryIdValues;
}
