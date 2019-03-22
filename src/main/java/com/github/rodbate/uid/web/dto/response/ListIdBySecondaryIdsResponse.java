package com.github.rodbate.uid.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * User: jiangsongsong
 * Date: 2018/12/29
 * Time: 14:20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListIdBySecondaryIdsResponse {
    private List<Entry> ids;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String secondaryId;
        private String id;
    }
}
